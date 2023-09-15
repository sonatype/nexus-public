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
import Axios from "axios";
import {APIConstants} from '@sonatype/nexus-ui-plugin';

const {USAGE_METRICS} = APIConstants.REST.INTERNAL;

const UsageMetricsMachine = createMachine({
  id: 'UsageMetricsMachine',
  initial: 'loading',

  context: {
    data: {},
    loadError: null
  },

  states: {
    loading: {
      invoke: {
        src: 'fetchData',
        onDone: {
          target: 'loaded',
          actions: 'setData'
        },
        onError: {
          target: 'error',
          actions: 'setError'
        }
      }
    },
    loaded: {},
    error: {
      on: {
        RETRY: {
          target: 'loading',
          actions: 'clearError'
        }
      }
    }
  }
}, {
  actions: {
    setData: assign({
      data: (_, {data: {data: {usage}}}) => {
        return {
          totalComponents: usage[0].component_total_count ?? 0,
          uniqueLogins: usage[0].unique_users_last_30d ?? 0,
          peakRequestsPerMin: usage[0].request_rates?.peak_requests_per_minute_1d ?? 0,
          peakRequestsPerDay: usage[0].request_rates?.peak_requests_per_day_30d ?? 0
        }
      }
    }),

    setError: assign({
      loadError: (_, event) => event.data.message
    }),

    clearError: assign({
      loadError: () => null
    })
  },
  services: {
    fetchData: () => Axios.get(USAGE_METRICS)
  }
});

export default UsageMetricsMachine;
