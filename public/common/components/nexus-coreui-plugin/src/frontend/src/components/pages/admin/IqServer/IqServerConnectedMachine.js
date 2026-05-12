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
import Axios from 'axios';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

const IQ_CONFIG_URL = 'service/rest/v1/iq';
const VERIFY_CONNECTION_URL = 'service/rest/v1/iq/verify-connection';

function parseLicensedSolutions(iqConfig) {
  if (!iqConfig) {
    return {
      lifecycle: false,
      firewall: false
    };
  }

  const solutions = iqConfig.licensedSolutions || [];
  const solutionIds = solutions.map(s => s.id);

  return {
    lifecycle: solutionIds.includes('lifecycle'),
    firewall: solutionIds.includes('firewall')
  };
}

export default createMachine(
  {
    id: 'IqServerConnectedMachine',
    initial: 'loading',

    context: {
      data: null,
      pristineData: null,
      iqServerUrl: '',
      error: null,
      connectionStatus: null
    },

    states: {
      loading: {
        invoke: {
          src: 'fetchData',
          onDone: {
            target: 'verifyingConnection',
            actions: ['setData']
          },
          onError: {
            target: 'loaded',
            actions: ['setError', 'logLoadError']
          }
        }
      },

      verifyingConnection: {
        invoke: {
          src: 'verifyConnection',
          onDone: {
            target: 'loaded',
            actions: ['setConnectionSuccess']
          },
          onError: {
            target: 'loaded',
            actions: ['setConnectionError']
          }
        }
      },

      loaded: {}
    }
  },
  {
    actions: {
      setData: assign((_, {data}) => {
        const parsed = parseLicensedSolutions(data);
        return {
          data: parsed,
          pristineData: parsed,
          iqServerUrl: data?.url || '',
          rawData: data,
          error: null
        };
      }),

      setError: assign({
        data: () => ({
          lifecycle: false,
          firewall: false
        }),
        pristineData: () => ({
          lifecycle: false,
          firewall: false
        }),
        error: (_, event) => event.data,
        iqServerUrl: () => ''
      }),

      logLoadError: (_, event) => {
        console.error('Failed to load license information', event.data?.message);
      },

      setConnectionSuccess: assign({
        connectionStatus: () => 'connected'
      }),

      setConnectionError: assign({
        connectionStatus: () => 'error'
      })
    },

    services: {
      fetchData: async () => {
        const iqConfig = await Axios.get(IQ_CONFIG_URL);
        return iqConfig.data;
      },
      verifyConnection: async () => {
        // Verify connection regardless of enabled status
        // The Connected page shows actual connection health, not whether IQ is enabled
        return Axios.post(VERIFY_CONNECTION_URL);
      }
    }
  }
);
