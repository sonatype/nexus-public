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

import { ExtAPIUtils, ExtJS, APIConstants } from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const action = APIConstants.EXT.OUTREACH.ACTION,
    outreachStatusMethod = APIConstants.EXT.OUTREACH.METHODS.READ_STATUS,
    proxyDownloadNumbersMethod = APIConstants.EXT.OUTREACH.METHODS.GET_PROXY_DOWNLOAD_NUMBERS;

const welcomeMachine = createMachine({
  id: 'WelcomeMachine',
  predictableActionArguments: true,

  initial: 'loaded',

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
    fetch: async () => {
      const user = ExtJS.state().getUser(),
          edition = ExtJS.state().getValue('status')?.edition,
          isAdmin = user?.administrator,
          requiresOutreach = edition === 'OSS' || !!user,
          outreachStatusRequest = { action, method: outreachStatusMethod },
          proxyDownloadNumbersRequest = { action, method: proxyDownloadNumbersMethod },
          requests = [outreachStatusRequest, proxyDownloadNumbersRequest],

          bulkResponse = await ExtAPIUtils.extAPIBulkRequest(requests),
          outreachStatusResponse = bulkResponse.data.find(({ method }) => method === outreachStatusRequest.method),
          proxyDownloadNumbersResponse = bulkResponse.data
              .find(({ method }) => method === proxyDownloadNumbersRequest.method),

          // The ExtAPIUtils expect this extra layer of object
          wrappedOutreachStatusResponse = { data: outreachStatusResponse },
          wrappedProxyDownloadNumbersResponse = { data: proxyDownloadNumbersResponse };

      ExtAPIUtils.checkForError(wrappedOutreachStatusResponse);

      // the outreach response includes a `data` property that is a long hexadecimal string (when the iframe should
      // be enabled) or null when the iframe should be disabled
      const showOutreachIframe = requiresOutreach &&
              Boolean(outreachStatusResponse?.result?.success) && outreachStatusResponse?.result?.data !== null,
          proxyDownloadNumberParams = ExtAPIUtils.extractResult(wrappedProxyDownloadNumbersResponse);

      return {
        showOutreachIframe,
        proxyDownloadNumberParams,
      };
    }
  }
});

export default welcomeMachine;
