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

export default class ExtAPIUtils {
  static useExtMachine(action, method, options = {}) {
    const defaultResult = options.defaultResult || [];

    const machine = createMachine({
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
          error: (_, event) => event?.message || UIStrings.ERROR.UNKNOWN
        })
      },
      services: {
        fetch: async (_, {data = null}) => {
          const response = await this.extAPIRequest(action, method, data);
          return this.extractResult(response, defaultResult);
        }
      }
    });

    return useMachine(machine, mergeDeepRight({
      context: {
        data: defaultResult
      },
      devTools: true
    }, options));
  }

  static extractResult(response, defaultResult) {
    const extDirectResponse = response.data;
    return extDirectResponse.result.data || defaultResult;
  }

  static createRequestBody(action, method, data = null, tid = 1) {
    return {action, method, data, type: 'rpc', tid};
  };

  /**
   * @param {string} action [required] the ExtJS action (example: coreui_Bundle, coreui_AnonymousSettings etc.)
   * @param {string} method [required] the method for the request (example: read, write etc.)
   * @param {string, Object, Array} data [optional] - The request data
   * @return {Promise}
   */
  static extAPIRequest(action, method, data = null) {
    return Axios.post(APIConstants.EXT.URL, this.createRequestBody(action, method, data));
  }

  /**
   * @param {Object[]} requests [required] - The list of requests
   * @param {string} requests[].action [required] - The ExtJS action
   * @param {string} requests[].method [required] - The method for the request
   * @param {string, Object, Array} requests[].data [optional] - The request data
   * @return {Promise}
   */
  static extAPIBulkRequest(requests) {
    const data = requests.map((request, index) => {
      return this.createRequestBody(request.action, request.method, request.data, index + 1);
    });
    return Axios.post(APIConstants.EXT.URL, data);
  }
}
