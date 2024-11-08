/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import Axios from 'axios';
import {assign, createMachine} from 'xstate';
import {useMachine} from '@xstate/react';
import {mergeDeepRight} from 'ramda';
import UIStrings from '../constants/UIStrings';
import APIConstants from '../constants/APIConstants';
import {useState} from "react";

const {EXT: {URL, SMALL_PAGE_SIZE}, SORT_DIRECTIONS: {ASC}} = APIConstants;

export default class ExtAPIUtils {
  static useExtMachine(action, method, options = {}) {
    const defaultResult = options.defaultResult || [];

    const [machine] = useState(createMachine({
      id: `ExtMachine(${action}, ${method})`,

      initial: options.initial || 'loaded',

      states: {
        loading: {
          invoke: {
            src: 'fetch',
            onDone: {
              target: 'loaded',
              actions: ['setData']
            },
            onError: {
              target: 'error',
              actions: ['setError']
            }
          }
        },
        loaded: {},
        error: {}
      },

      on: {
        LOAD: {
          target: 'loading'
        }
      }
    }, {
      actions: {
        setData: assign({
          data: (_, {data}) => data
        }),

        setError: assign({
          error: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN
        })
      },
      services: {
        fetch: async (_, {options = null}) => {
          const response = await this.extAPIRequest(action, method, options);
          this.checkForError(response);
          return this.extractResult(response, defaultResult);
        }
      }
    }));

    return useMachine(machine, mergeDeepRight({
      context: {
        data: defaultResult
      },
      devTools: true
    }, options));
  }

  static extractResult(response, defaultResult) {
    const extDirectResponse = response?.data;
    return extDirectResponse?.result?.data || defaultResult;
  }

  static checkForError(response) {
    const {data} = response;
    if (data.type === 'exception') {
      throw Error(data.message);
    }
    if (!data.result.success) {
      if (data.result.message) {
        throw Error(data.result.message);
      } else if (data.result.errors) {
        throw Error(JSON.stringify(data.result.errors));
      } else {
        throw Error('Unknown error');
      }
    }
  }

  static checkForErrorAndExtract(response) {
    ExtAPIUtils.checkForError(response);
    return ExtAPIUtils.extractResult(response);
  }

  /**
   * @param {Object} options [required] - The request data configuration
   * @param {*} options.data [optional] - The request data
   * @param {number} [options.page=1] [optional] - The request page number
   * @param {number} [options.start=0] [optional] - The request start position
   * @param {number} [options.limit=25] [optional] - The request page size
   * @param {string} options.sortField [optional] - The request sort field
   * @param {string="asc", "desc"} [options.sortDirection=asc] [optional] - The request sort direction
   * @param {string} options.filterValue [optional] - The request filter value
   * @param {string} [options.filterField=filter] [optional] - The request filter field
   * @param {string} options.query [optional] - The request query
   * @return {Array}
   */
  static createData(options) {

    if (!options) return null;

    const {
      data,
      page = 1,
      start = 0,
      limit = SMALL_PAGE_SIZE,
      sortField,
      sortDirection = ASC,
      filter,
      filterValue,
      filterField = 'filter',
      query,
    } = options;

    if (data) {
      return data;
    }

    let requestData = {page, limit, start};

    if (sortField) {
      requestData.sort = [{
        property: sortField,
        direction: sortDirection.toUpperCase(),
      }];
    }

    if (filter) {
      requestData.filter = filter;
    } else if (filterValue) {
      requestData.filter = [{
        property: filterField,
        value: filterValue,
      }];
    }

    if (query) {
      requestData.query = query;
    }

    return [requestData];
  }

  /**
   * @param {string} action [required] the ExtJS action (example: coreui_Bundle, coreui_AnonymousSettings etc.)
   * @param {string} method [required] the method for the request (example: read, write etc.)
   * @param {Object} options [optional] - The request options
   * @param {number} tid [optional] - The request ID
   * @return {Object}
   */
  static createRequestBody(action, method, options = null, tid = 1) {
    const requestData = this.createData(options);
    return {action, method, data: requestData, type: 'rpc', tid};
  }

  /**
   * @param {string} action [required] the ExtJS action (example: coreui_Bundle, coreui_AnonymousSettings etc.)
   * @param {string} method [required] the method for the request (example: read, write etc.)
   * @param {Object} options [optional] - The request options
   * @return {Promise}
   */
  static extAPIRequest(action, method, options = null) {
    this.setupTokenInterceptors();
    return Axios.post(URL, this.createRequestBody(action, method, options));
  }

  /**
   * @param {Object[]} requests [required] - The list of requests
   * @param {string} requests[].action [required] - The ExtJS action
   * @param {string} requests[].method [required] - The method for the request
   * @param {Object} requests[].options [optional] - The request data configuration
   * @return {Promise}
   */
  static extAPIBulkRequest(requests) {
    this.setupTokenInterceptors();
    const data = requests.map(({action, method, options}, index) => {
      return this.createRequestBody(action, method, options, index + 1);
    });
    return Axios.post(URL, data);
  }

  static setupTokenInterceptors() {
    if (!this.interceptorSet && process.env.NODE_ENV !== 'test') {
      Axios.interceptors.request.use(
          function(config) {
            const csrfToken = (document.cookie.match('(^|; )NX-ANTI-CSRF-TOKEN=([^;]*)') || 0)[2];
            if (csrfToken) {
              config.headers['NX-ANTI-CSRF-TOKEN'] = csrfToken;
            }
            return config;
          }
      );
      this.interceptorSet = true;
    }
  }
}
