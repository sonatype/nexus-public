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
import {assign, Machine} from 'xstate';
import Axios from 'axios'
import {Utils} from '@sonatype/nexus-ui-plugin';

export default Machine(
    {
      initial: 'loading',

      context: {
        data: [],
        pristineData: [],
        sortField: 'name',
        sortDirection: Utils.ASC,
        error: ''
      },

      states: {
        loading: {
          invoke: {
            src: 'fetchMetricHealth',
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
        loaded: {
          entry: ['sortData'],
          on: {
            SORT_BY_NAME: {
              target: 'loaded',
              actions: ['setSortByName']
            },
            SORT_BY_MESSAGE: {
              target: 'loaded',
              actions: ['setSortByMessage']
            },
            SORT_BY_ERROR: {
              target: 'loaded',
              actions: ['setSortByError']
            }
          }
        },
        error: {}
      }
    },
    {
      actions: {
        setData: assign({
          data: (_, event) => Object.entries(event.data.data).map(metric => Object.assign({name: metric[0]}, metric[1])),
          pristineData: (_, event) => Object.entries(event.data.data).map(metric => Object.assign({name: metric[0]}, metric[1]))
        }),
        setError: assign({
          error: (_, event) => event.data.message
        }),
        setSortByName: assign({
          sortField: 'name',
          sortDirection: Utils.nextSortDirection('name')
        }),
        setSortByMessage: assign({
          sortField: 'message',
          sortDirection: Utils.nextSortDirection('message')
        }),
        setSortByError: assign({
          sortField: 'error',
          sortDirection: Utils.nextSortDirection('error')
        }),
        sortData: assign({
          data: Utils.sortDataByFieldAndDirection
        }),
      },
      services: {
        fetchMetricHealth: () => Axios.get('/service/rest/internal/ui/status-check')
      }
    }
);
