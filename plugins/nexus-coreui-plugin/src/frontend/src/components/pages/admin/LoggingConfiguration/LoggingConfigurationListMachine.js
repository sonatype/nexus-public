/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {assign, Machine} from 'xstate';
import Axios from 'axios';

import {ExtJS} from 'nexus-ui-plugin';

import UIStrings from "../../../../constants/UIStrings";

export const ASC = 1;
export const DESC = -1;

export default Machine(
    {
      initial: 'loading',

      context: {
        data: [],
        pristineData: [],
        sortField: 'name',
        sortDirection: ASC,
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
                src: 'fetchLoggingConfigurations',
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
            SORT_BY_LEVEL: {
              target: 'loaded',
              actions: ['setSortByLevel']
            },
            FILTER: {
              target: 'loaded',
              actions: ['setFilter']
            }
          }
        },
        error: {
          id: 'error'
        }
      }
    },
    {
      actions: {
        setData: assign({
          data: ({sortField, sortDirection}, {data}) => data.data.map(({name, level}) => ({name, level})),
          pristineData: ({sortField, sortDirection}, {data}) => data.data.map(({name, level}) => ({name, level}))
        }),

        setError: assign({
          error: (_, event) => event.data.message
        }),

        setSortByName: assign({
          sortField: 'name',
          sortDirection: ({sortField, sortDirection}) => {
            if (sortField !== 'name') {
              return ASC;
            }
            else {
              return sortDirection === ASC ? DESC : ASC
            }
          }
        }),

        setSortByLevel: assign({
          sortField: 'level',
          sortDirection: ({sortField, sortDirection}) => {
            if (sortField !== 'level') {
              return ASC;
            }
            else {
              return sortDirection === ASC ? DESC : ASC
            }
          }
        }),

        setFilter: assign({
          filter: (_, {filter}) => filter
        }),

        filterData: assign({
          data: ({filter, data, pristineData}, _) => pristineData.filter(
              ({name}) => name.toLowerCase().indexOf(filter.toLowerCase()) !== -1
          )
        }),

        sortData: assign({
          data: ({sortField, sortDirection, data}) => (data.slice().sort((a, b) => {
            return a[sortField] > b[sortField] ? sortDirection : -sortDirection;
          }))
        }),

        showResetError: (_, {data}) => {
          console.error(data.response);
          ExtJS.showErrorMessage(UIStrings.LOGGING.MESSAGES.RESET_ERROR)
        }
      },
      services: {
        fetchLoggingConfigurations: () => Axios.get('/service/rest/internal/ui/loggingConfiguration'),
        confirmReset: () => ExtJS.requestConfirmation({
          title: UIStrings.LOGGING.CONFIRM_RESET_ALL.TITLE,
          message: UIStrings.LOGGING.CONFIRM_RESET_ALL.MESSAGE,
          yesButtonText: UIStrings.LOGGING.CONFIRM_RESET_ALL.CONFIRM_BUTTON,
          noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL
        }),
        reset: () => Axios.post('/service/rest/internal/ui/loggingConfiguration/reset')
      }
    }
);
