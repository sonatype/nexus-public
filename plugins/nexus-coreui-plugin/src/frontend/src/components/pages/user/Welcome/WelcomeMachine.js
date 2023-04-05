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
import {assign, createMachine} from 'xstate';
import { isNil } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { ExtAPIUtils, ExtJS, APIConstants } from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const action = APIConstants.EXT.OUTREACH.ACTION,
    outreachStatusMethod = APIConstants.EXT.OUTREACH.METHODS.READ_STATUS,
    proxyDownloadNumbersMethod = APIConstants.EXT.OUTREACH.METHODS.GET_PROXY_DOWNLOAD_NUMBERS,
    log4jMethod = APIConstants.EXT.OUTREACH.METHODS.IS_LOG4J_CAPABILITY_ACTIVE;

const welcomeMachine = createMachine({
  id: 'WelcomeMachine',

  initial: 'loaded',

  states: {
    loading: {
      invoke: {
        src: 'fetch',
        onDone: {
          target: 'loaded',
          actions: ['setData', 'clearLog4jError']
        },
        onError: {
          target: 'error',
          actions: ['setError']
        }
      }
    },
    loaded: {},
    error: {},
    enablingLog4j: {
      invoke: {
        src: 'enableLog4j',
        onDone: {
          target: 'redirectingToLog4j'
        },
        onError: {
          target: 'log4jError',
          actions: ['setLog4jError', 'clearShowLog4jAlert']
        }
      }
    },
    redirectingToLog4j: {
      type: 'final',
      invoke: {
        src: 'redirectToLog4j'
      }
    },
    log4jError: {}
  },

  on: {
    LOAD: {
      target: 'loading'
    },
    ENABLE_LOG4J: {
      target: 'enablingLog4j'
    }
  }
}, {
  actions: {
    setData: assign({
      data: (_, {data}) => data
    }),

    setError: assign({
      error: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN
    }),

    setLog4jError: assign({
      log4jError: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN
    }),

    clearLog4jError: assign({ log4jError: null }),

    clearShowLog4jAlert: assign({
      data: ({data}) => ({ ...data, showLog4jAlert: false })
    })
  },
  services: {
    fetch: async () => {
      const isAdmin = ExtJS.state().getUser()?.administrator,
          outreachStatusRequest = { action, method: outreachStatusMethod },
          proxyDownloadNumbersRequest = { action, method: proxyDownloadNumbersMethod },
          log4jRequest = { action, method: log4jMethod },
          requests = [outreachStatusRequest, proxyDownloadNumbersRequest].concat(isAdmin ? log4jRequest : []),

          bulkResponse = await ExtAPIUtils.extAPIBulkRequest(requests),
          outreachStatusResponse = bulkResponse.data.find(({ method }) => method === outreachStatusRequest.method),
          log4jResponse = bulkResponse.data.find(({ method }) => method === log4jRequest.method),
          proxyDownloadNumbersResponse = bulkResponse.data
              .find(({ method }) => method === proxyDownloadNumbersRequest.method),

          // The ExtAPIUtils expect this extra layer of object
          wrappedOutreachStatusResponse = { data: outreachStatusResponse },
          wrappedLog4jResponse = { data: log4jResponse },
          wrappedProxyDownloadNumbersResponse = { data: proxyDownloadNumbersResponse };

      ExtAPIUtils.checkForError(wrappedOutreachStatusResponse);
      if (isAdmin) {
        ExtAPIUtils.checkForError(wrappedLog4jResponse);
      }

      // the outreach response includes a `data` property that is a long hexadecimal string (when the iframe should
      // be enabled) or null when the iframe should be disabled
      const showOutreachIframe =
              Boolean(outreachStatusResponse?.result?.success) && outreachStatusResponse?.result?.data !== null,
          isAvailableLog4jDisclaimer = isAdmin ? ExtAPIUtils.extractResult(wrappedLog4jResponse) : null,
          proxyDownloadNumberParams = ExtAPIUtils.extractResult(wrappedProxyDownloadNumbersResponse);

      return {
        // API response is "true"/"false". We want to parse it and then invert it because we show the
        // alert whenever the capability is _not_ installed and enabled
        showLog4jAlert: isNil(isAvailableLog4jDisclaimer) ? false : !JSON.parse(isAvailableLog4jDisclaimer),
        showOutreachIframe,
        proxyDownloadNumberParams
      };
    },
    enableLog4j: async () => {
      const response = await ExtAPIUtils.extAPIRequest('outreach_Outreach', 'setLog4JVisualizerEnabled', {
            data: [true]
          }),
          state = ExtJS.state(),
          stateStore = Ext.getApplication().getStore('State'),
          isCapabilityEnabled = () => state.getValue('vulnerabilityCapabilityState', {enabled: false}).enabled;

      ExtAPIUtils.checkForError(response);

      if (isCapabilityEnabled()) {
        return true;
      }
      else {
        return new Promise((resolve, reject) => {
          function handleChange() {
            if (isCapabilityEnabled()) {
              listener.destroy();
              resolve();
            }
          }

          const uiSettings = state.getValue('uiSettings') ?? {},

              // NOTE: measured in seconds
              { requestTimeout = 60, statusIntervalAuthenticated = 5 } = uiSettings;

          const listener = stateStore.on('datachanged', handleChange, undefined, {
            destroyable: true
          });

          // A timeout on waiting for the capability to be enabled
          setTimeout(() => {
            listener.destroy();
            reject({ message: 'Timeout while enabling capability' });
          }, 1000 * (2 * statusIntervalAuthenticated + requestTimeout));
        });
      }
    },
    redirectToLog4j: () => {
      setTimeout(() => {
        window.location.hash = '#admin/repository/insightfrontend';
      }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
    }
  }
});

export default welcomeMachine;
