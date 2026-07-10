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
import { assign, createMachine } from 'xstate';

import Axios from 'axios';
import { isPermissionError } from './UsageInsightsUtils';

export default createMachine(
  {
    id: 'LicensingHistoricalUsageMachine',
    initial: 'loading',

    context: {
      data: null,
      pristineData: null,
      loadError: null,
      isPermissionError: false
    },

    states: {
      loading: {
        invoke: {
          src: 'fetchData',
          onDone: {
            target: 'loaded',
            actions: ['setData', 'clearError']
          },
          onError: {
            target: 'loadError',
            actions: assign({
              loadError: (context, event) => event.data,
              isPermissionError: (context, event) => isPermissionError(event.data)
            })
          }
        }
      },
      loaded: {},
      loadError: {
        on: {
          RETRY: {
            target: 'loading',
            actions: 'clearError'
          }
        }
      }
    }
  },
  {
    actions: {
      setData: assign({
        data: (_, { data }) => data.data,
        pristineData: (_, { data }) => data.data
      }),
      clearError: assign({
        loadError: () => null,
        isPermissionError: () => false
      })
    },
    services: {
      fetchData: () => Axios.get('service/rest/v1/monthly-metrics')
    }
  }
);
