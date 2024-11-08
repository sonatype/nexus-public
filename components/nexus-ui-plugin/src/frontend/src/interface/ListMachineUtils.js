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
import {actions, assign, createMachine, send} from 'xstate';

import APIConstants from '../constants/APIConstants';

const {SORT_DIRECTIONS: {ASC, DESC}} = APIConstants;

/**
 * @since 3.38
 */
export default class ListMachineUtils {
  /**
   * Constant for ascending sorts
   * @since 3.29
   */
  static get ASC() {
    return ASC;
  }

  /**
   * Constant for descending sorts
   * @since 3.29
   */
  static get DESC() {
    return DESC;
  }

  /**
   * Builds a new xstate machine used to handle item lists.
   *
   * ListMachineUtils.buildListMachine({
   *   id: 'MyMachine',
   *   sortableFields: ['name']
   * }).withConfig({
   *     filterData: myMachine.assign({
   *       data: ({filter, data, pristineData}, _) => pristineData.filter(({name}) =>
   *           name.toLowerCase().indexOf(filter.toLowerCase()) !== -1
   *       )
   *     })
   *   },
   *   services: {
   *     fetchData: () => axios.get(url)
   *   }
   * });
   *
   * @param {string} id [required] - a unique identifier for this machine
   * @param {string} [initial=loading] [optional] - The initial state to start in
   * @param {string} [sortField=name] [optional] - The field to sort on
   * @param {string} [sortDirection="asc", "desc"] [optional] - Sort direction, default to "asc"
   * @param {string[]} [sortableFields=[]] [optional] - A list of fields to sort
   * @param {boolean} [apiSorting=false] [optional] - Set to "true" to use API sorting
   * @param {boolean} [apiFiltering=false] [optional] - Set to "true" to use API filtering
   * @param config [optional] a function used to change the config of the machine
   * @param options [optional] a function used to change the options of the machine
   * @return {StateMachine<any, any, AnyEventObject>}
   */
  static buildListMachine({
                            id,
                            initial = 'loading',
                            sortField = 'name',
                            sortDirection = ListMachineUtils.ASC,
                            sortableFields = [],
                            apiSorting = false,
                            apiFiltering = false,
                            config = (config) => config,
                            options = (options) => options
                          })
  {
    const sortEvents = {};
    const sortActions = {};
    sortableFields.forEach(field => {
      const eventName = `SORT_BY_${field.replace(/[A-Z]/g, c => '_' + c).toUpperCase()}`;
      const actionName = `setSortBy${field}`;
      sortEvents[eventName] = {
        target: apiSorting ? 'loading' : 'loaded',
        actions: [actionName],
      };

      sortActions[actionName] = assign({
        sortField: field,
        sortDirection: ListMachineUtils.nextSortDirection(field)
      });
    });

    const loadedEntry = [];
    if (!apiFiltering) loadedEntry.push('filterData');
    if (!apiSorting) loadedEntry.push('sortData');

    let filterActions = ['setFilter'];
    if (apiFiltering) {
      filterActions = filterActions.concat(['debounceApiFilter', 'apiFilter'])
    }

    const DEFAULT_CONFIG = {
      id,
      initial: initial,

      context: {
        data: [],
        pristineData: [],
        sortField,
        sortDirection,
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
                src: 'fetchData',
                onDone: {
                  target: '#loaded',
                  actions: ['clearError','setData']
                },
                onError: {
                  target: '#error',
                  actions: ['setError']
                }
              }
            }
          }
        },
        loaded: {
          id: 'loaded',
          entry: loadedEntry,
          on: {
            ...sortEvents,
            FILTER: {
              target: 'loaded',
              actions: filterActions,
            },
            API_FILTER: {
              target: 'loading',
            },
            SET_DATA: {
              target: 'loaded',
              actions: ['setData']
            },
            LOAD: {
              target: 'loading'
            }
          }
        },
        error: {
          id: 'error',
          on: {
            FILTER: {
              target: 'loaded',
              actions: ['setFilter']
            }
          }
        }
      }
    };

    const DEFAULT_OPTIONS = {
      actions: {
        ...sortActions,

        setData: assign({
          data: (_, {data}) => data.data,
          pristineData: (_, {data}) => data.data,
        }),

        setError: assign({
          error: (_, event) => {
            const message = event.data?.message;
            return message ? `${message}` : null;
          },
        }),

        clearError: assign({
          error: ''
        }),

        setFilter: assign({
          filter: (_, {filter}) => filter
        }),

        debounceApiFilter: actions.cancel('debounced-filter'),
        apiFilter: send({
          type: 'API_FILTER',
          id: 'debounced-filter',
          delay: APIConstants.DEBOUNCE_DELAY,
        }),

        clearFilter: assign({
          filter: () => ''
        }),

        filterData: () => {
        },

        sortData: assign({
          data: ListMachineUtils.sortDataByFieldAndDirection({useLowerCaseSorting: true})
        })
      },

      services: {
        fetchData: () => Promise.resolve({data: []})
      }
    };

    return createMachine(config(DEFAULT_CONFIG), options(DEFAULT_OPTIONS));
  }

  /**
   * @return the data sorted by the field and direction
   */
  static sortDataByFieldAndDirection({useLowerCaseSorting}) {
    return ({sortField, sortDirection, data}) => (data.slice().sort((a, b) => {
      const dir = sortDirection === ListMachineUtils.ASC ? 1 : -1;
      const left = a[sortField];
      const right = b[sortField];

      function isString(val) {
        if (val === null) {
          return true;
        }
        return typeof val === 'string';
      }

      if (left === right) {
        return 0;
      }
      if (typeof (left) === 'object' && typeof (right) === 'object') {
        return JSON.stringify(a[sortField]).toLowerCase() > JSON.stringify(b[sortField]).toLowerCase() ? dir : -dir;
      }
      else if (isString(left) && isString(right)) {
        if (useLowerCaseSorting) {
          return (left || "").toLowerCase() > (right || "").toLowerCase() ? dir : -dir;
        }
        else {
          return (left || "") > (right || "") ? dir : -dir;
        }
      }
      else {
        return left > right ? dir : -dir;
      }
    }));
  }

  /**
   * @param fieldName
   * @return a function that can be used with assign to set the next sort direction based on the current context
   */
  static nextSortDirection(fieldName) {
    return ({sortField, sortDirection}) => {
      if (sortField === fieldName && sortDirection === this.ASC) {
        return this.DESC;
      }
      return this.ASC;
    };
  }

  /**
   * Determine the sort direction for use with the NxTable columns
   * @param fieldName
   * @param context {sortField, sortDirection}
   * @return {null | 'asc' | 'desc'}
   */
  static getSortDirection(fieldName, {sortField, sortDirection}) {
    if (sortField === fieldName) {
      return sortDirection;
    }
    else {
      return null;
    }
  }

  /**
   * @param {string[]} values [required] An array of field values
   * @param {string} filter [required]
   * @return {boolean}
   */
  static hasAnyMatches(values, filter) {
    if (!filter) {
      return true;
    }
    return Boolean(values.find(value =>
        Boolean(value) && value.toString().toLowerCase().indexOf(filter.toLowerCase()) !== -1
    ));
  }
}
