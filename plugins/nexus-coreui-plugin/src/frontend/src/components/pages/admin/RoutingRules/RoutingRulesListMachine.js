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
import Axios from 'axios';
import {Utils} from '@sonatype/nexus-ui-plugin';

export default Machine(
    {
      id: 'RoutingRulesListMachine',

      initial: 'loading',

      context: {
        data: [],
        pristineData: [],
        sortField: 'name',
        sortDirection: Utils.ASC,
        filter: '',
        error: ''
      },

      states: {
        loading: {
          id: 'loading',
          initial: 'fetch',
          states: {
            'fetch': {
              invoke: {
                src: 'fetch',
                onDone: {
                  target: '#loaded',
                  actions: ['setData']
                },
                onError: {
                  target: '#error',
                  actions: ['setError']
                }
              }
            },
            'resetting': {
              invoke: {
                src: 'reset',
                onDone: 'fetch',
                onError: {
                  target: 'fetch',
                  actions: ['showResetError']
                }
              }
            }
          }
        },
        loaded: {
          id: 'loaded',
          initial: 'editing',
          entry: ['filterData', 'sortData'],
          states: {
            editing: {
              on: {
                RESET: 'confirmReset'
              }
            },
            confirmReset: {
              invoke: {
                src: 'confirmReset',
                onDone: '#loading.resetting',
                onError: 'editing'
              }
            }
          },
          on: {
            SORT_BY_NAME: {
              target: 'loaded',
              actions: ['setSortByName']
            },
            SORT_BY_DESCRIPTION: {
              target: 'loaded',
              actions: ['setSortByDescription']
            },
            SORT_BY_USED_BY: {
              target: 'loaded',
              actions: ['setSortByUsedBy']
            },
            FILTER: {
              target: 'loaded',
              actions: ['setFilter']
            }
          }
        },
        error: {
          id: 'error'
        },

        preview: {}
      },
      on: {
        PREVIEW: {
          target: 'preview'
        }
      }
    },
    {
      actions: {
        setData: assign({
          data: ({sortField, sortDirection}, {data}) => data.data,
          pristineData: ({sortField, sortDirection}, {data}) => data.data
        }),

        setError: assign({
          error: (_, event) => event.data.message
        }),

        setSortByName: assign({
          sortField: 'name',
          sortDirection: Utils.nextSortDirection('name')
        }),
        setSortByDescription: assign({
          sortField: 'description',
          sortDirection: Utils.nextSortDirection('description')
        }),
        setSortByUsedBy: assign({
          sortField: 'assignedRepositoryCount',
          sortDirection: Utils.nextSortDirection('assignedRepositoryCount')
        }),

        setFilter: assign({
          filter: (_, {filter}) => filter
        }),

        filterData: assign({
          data: ({filter, data, pristineData}, _) => pristineData.filter(
              ({name, description}) => name.toLowerCase().indexOf(filter.toLowerCase()) !== -1 ||
                  description.toLowerCase().indexOf(filter.toLowerCase()) !== -1)
        }),

        sortData: assign({
          data: Utils.sortDataByFieldAndDirection
        }),
      },
      services: {
        fetch: () => Axios.get('/service/rest/internal/ui/routing-rules?includeRepositoryNames=true'),
      }
    }
);
