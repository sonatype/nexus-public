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
import {assign, Machine} from "xstate";
import ExtJS from "./ExtJS";
import UIStrings from "../constants/UIStrings";
import {equals} from 'ramda';

const FIELD_ID = 'FIELD ';
const PARAMETER_ID = 'PARAMETER ';

/**
 * @since 3.22
 */
export default class Utils {
  /**
   * Constant for ascending sorts
   * @since 3.next
   */
  static get ASC() {
    return 'asc';
  }

  /**
   * Constant for descending sorts
   * @since 3.next
   */
  static get DESC() {
    return 'desc';
  }

  static urlFromPath(path) {
    return NX.app.baseUrl + path;
  }

  static isBlank(str) {
    return (!str || /^\s*$/.test(str));
  }

  static notBlank(str) {
    return !Utils.isBlank(str);
  }

  static isUri(str) {
    return str && /^[a-z]*:.+$/i.test(str);
  }

  static notUri(str) {
    return !Utils.isUri(str);
  }

  static isInRange({value, min = -Infinity, max = Infinity}) {
    if (isNaN(value)) {
      return UIStrings.ERROR.NAN
    }

    const number = parseInt(value, 10);
    if (min > number) {
      return UIStrings.ERROR.MIN(min);
    }
    else if (max < number) {
      return UIStrings.ERROR.MAX(max);
    }

    return null;
  }

  /**
   * Builds a new xstate machine used to handle forms.
   * Typically the validation action, fetchData service, and saveData service should be implemented in withConfig.
   *
   * Utils.buildFormMachine({id: 'MyMachine'}).withConfig({
   *   actions: {
   *     validate: assign({
   *       validationErrors: ({data}) => ({
   *         field: Utils.isBlank(data.field) ? UIStrings.ERROR.FIELD_REQUIRED : null
   *       })
   *     })
   *   },
   *   services: {
   *     fetchData: () => axios.get(url),
   *     saveData: ({data}) => axios.put(url, data)
   *   }
   * });
   *
   * @param id [required] a unique identifier for this machine
   * @param initial [optional] the initial state to start in, defaults to 'loading'
   * @param stateAfterSave [optional] the state to put the machine in after successful save, default of 'loaded'
   * @param config [optional] a function used to change the config of the machine
   * @return {StateMachine<any, any, AnyEventObject>}
   */
  static buildFormMachine({id, initial = 'loading', stateAfterSave = 'loaded', config = (config) => config, options = (options) => options}) {
    const DEFAULT_CONFIG = {
      id,
      initial,

      context: {
        isPristine: true,
        isTouched: {},
        loadError: null,
        saveError: null,
        saveErrorData: {},
        validationErrors: {},
      },

      states: {
        loading: {
          invoke: {
            id: 'fetchData',
            src: 'fetchData',
            onDone: {
              target: 'loaded',
              actions: ['setData', 'postProcessData', 'validate']
            },
            onError: {
              target: 'loadError',
              actions: ['setLoadError', 'logLoadError']
            }
          }
        },

        loaded: {
          entry: ['validate', 'setDirtyFlag', 'setIsPristine'],

          on: {
            UPDATE: {
              target: 'loaded',
              actions: ['update'],
              internal: false
            },
            SAVE: {
              target: 'saving',
              cond: 'canSave'
            },
            RESET: {
              target: 'loaded',
              actions: ['reset', 'clearSaveError']
            }
          }
        },

        saving: {
          entry: 'clearSaveError',

          invoke: {
            src: 'saveData',
            onDone: {
              target: stateAfterSave,
              actions: ['clearDirtyFlag', 'clearSaveError', 'setSavedData', 'logSaveSuccess', 'onSaveSuccess']
            },

            onError: {
              target: 'loaded',
              actions: ['setSaveError', 'logSaveError']
            }
          }
        },

        loadError: {
          type: 'final'
        }
      },
      on: {
        RETRY: {
          target: initial
        }
      }
    };

    const DEFAULT_OPTIONS = {
      actions: {
        setData: assign({
          data: (_, event) => event.data?.data,
          pristineData: (_, event) => event.data?.data
        }),
        postProcessData: () => {},
        setDirtyFlag: ({isPristine}) => ExtJS.setDirtyStatus(id, !isPristine),
        clearDirtyFlag: () => ExtJS.setDirtyStatus(id, false),

        setSaveError: assign({
          saveErrorData: ({data}) => data,
          saveError: (_, event) => {
            const data = event.data?.response?.data;
            return data instanceof String ? data : null;
          },
          saveErrors: (_, event) => {
            const data = event.data?.response?.data;
            if (data instanceof Array) {
              let saveErrors = {};
              data.forEach(({id, message}) => {
                if (id.startsWith(FIELD_ID)) {
                  saveErrors[id.replace(FIELD_ID, '')] = message;
                }
                else if (id.startsWith(PARAMETER_ID)) {
                  saveErrors[id.replace(PARAMETER_ID, '')] = message;
                }
                else {
                  saveErrors[id] = message;
                }
              });
              return saveErrors;
            }
          }
        }),
        clearSaveError: assign({
          saveErrorData: () => ({}),
          saveError: () => undefined,
          saveErrors: () => ({})
        }),
        logSaveError: (_, event) => {
          console.log(`Load Error: ${event.data?.message}`);
          ExtJS.showErrorMessage(UIStrings.ERROR.SAVE_ERROR)
        },
        logSaveSuccess: () => ExtJS.showSuccessMessage(UIStrings.SAVE_SUCCESS),

        setLoadError: assign({
          loadError: (_, event) => event.data?.message
        }),
        logLoadError: (_, event) => {
          console.log(`Load Error: ${event.data?.message}`);
          ExtJS.showErrorMessage(UIStrings.ERROR.LOAD_ERROR)
        },

        onSaveSuccess: () => {
        },

        reset: assign({
          data: ({pristineData}) => pristineData,
          isTouched: () => ({})
        }),

        update: assign({
          data: ({data}, event) => ({...data, ...event.data}),
          isTouched: ({isTouched}, event) => {
            const result = {...isTouched};
            Object.keys(event.data).forEach(key => result[key] = true);
            return result;
          }
        }),

        setIsPristine: assign({
          isPristine: ({data, pristineData}) => Object.keys(pristineData).every(
              key => equals(pristineData[key], data[key]))
        }),

        setSavedData: assign({
          pristineData: ({data}) => data,
          isTouched: () => ({})
        })
      },

      guards: {
        canSave: ({isPristine, validationErrors}) => {
          const isValid = !Utils.isInvalid(validationErrors);
          return !isPristine && isValid;
        }
      },

      services: {
        fetchData: () => Promise.resolve({data: {}})
      }
    };

    return Machine(config(DEFAULT_CONFIG), options(DEFAULT_OPTIONS));
  }

  /**
   * Builds a new xstate machine used to handle item lists.
   *
   * Utils.buildListMachine({
   *   id: 'MyMachine',
   *   config: (config) => ({
   *     ...config,
   *     states: {
   *       ...config.states,
   *       loaded: {
   *         ...config.states.loaded,
   *         on: {
   *           ...config.states.loaded.on,
   *           SORT_BY_NAME: {
   *             target: 'loaded',
   *             actions: ['setSortByName']
   *           }
   *         }
   *       }
   *     }
   *   }),
   *   config: (config) => ({
   *     actions: {
   *       setSortByName: assign({
   *         sortField: 'name',
   *         sortDirection: Utils.nextSortDirection('name')
   *       }),
   *       filterData: assign({
   *         data: ({filter, data, pristineData}, _) =>
   *         pristineData.filter(({name}) => name.toLowerCase().indexOf(filter.toLowerCase()) !== -1)
   *       })
   *     },
   *     services: {
   *       fetchData: () => axios.get(url)
   *     }
   *   })
   * });
   *
   * @param id [required] a unique identifier for this machine
   * @param sortField [optional] field to sort on, defaults to 'name'
   * @param initial [optional] the initial state to start in, defaults to 'loading'
   * @param config [optional] a function used to change the config of the machine
   * @param options [optional] a function used to change the options of the machine
   * @return {StateMachine<any, any, AnyEventObject>}
   */
  static buildListMachine({id, initial = 'loading', sortField = 'name', config = (config) => config, options = (options) => options}) {
    const DEFAULT_CONFIG = {
      id,
      initial: initial,

      context: {
        data: [],
        pristineData: [],
        sortField: sortField,
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
                src: 'fetchData',
                onDone: {
                  target: '#loaded',
                  actions: ['setData']
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
          entry: ['filterData', 'sortData'],
          on: {
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
    };

    const DEFAULT_OPTIONS = {
      actions: {
        setData: assign({
          data: (_, {data}) => data.data,
          pristineData: (_, {data}) => data.data
        }),

        setError: assign({
          error: (_, event) => event.data.message
        }),

        setFilter: assign({
          filter: (_, {filter}) => filter
        }),

        clearFilter: assign({
          filter: () => ''
        }),

        filterData: () => {},

        sortData: assign({
          data: Utils.sortDataByFieldAndDirection
        })
      },

      services: {
        fetchData: () => Promise.resolve({data: {}})
      }
    };

    return Machine(config(DEFAULT_CONFIG), options(DEFAULT_OPTIONS));
  }

  /**
   * Check if the errors object returned contains any error messages
   * @param errors {Object | null | undefined}
   * @return {boolean} true if there are any error messages
   */
  static isInvalid(errors) {
    if (errors === null || errors === undefined) {
      return false;
    }
    return Object.values(errors).findIndex((e) => e !== null && e !== undefined && e.length > 0) !== -1;
  }

  /**
   * Generate common props for form fields
   * @param name
   * @param current a form machine generated by buildFormMachine
   * @param defaultValue if the machine did not provide any data
   * @return {{name: *, validationErrors: (*|[]), isPristine: boolean, value: (*|string)}}
   */
  static fieldProps(name, current, defaultValue = '') {
    const {data = {}, isTouched = {}, validationErrors = {}, saveErrors = {}, saveErrorData = {}} = current.context;
    let errors = null;
    if (name in isTouched && validationErrors[name]) {
      errors = validationErrors[name];
    }
    else if (saveErrors[name] && saveErrorData[name] === data[name]) {
      errors = saveErrors[name];
    }
    return {
      name,
      value: (data && data[name]) || defaultValue,
      isPristine: name in isTouched ? !isTouched[name] : true,
      validatable: true,
      validationErrors: errors
    };
  }

  /**
   * @param isPristine
   * @param isInvalid
   */
  static saveTooltip({isPristine, isInvalid}) {
    if (isPristine) {
      return UIStrings.PRISTINE_TOOLTIP;
    }
    else if (isInvalid) {
      return UIStrings.INVALID_TOOLTIP;
    }
  }

  static discardTooltip({isPristine}) {
    if (isPristine) {
      return UIStrings.PRISTINE_TOOLTIP;
    }
  }

  /**
   * @since 3.next
   * @param fieldName
   * @return a function that can be used with assign to set the next sort direction based on the current context
   */
  static nextSortDirection(fieldName) {
    return ({sortField, sortDirection}) => {
      if (sortField !== fieldName) {
        return this.ASC;
      }
      else if (sortDirection === this.ASC) {
        return this.DESC
      }
      else {
        return this.ASC;
      }
    }
  }

  /**
   * @since 3.next
   * @return the data sorted by the field and direction
   */
  static sortDataByFieldAndDirection({sortField, sortDirection, data}) {
    return (data.slice().sort((a, b) => {
      const dir = sortDirection === Utils.ASC ? 1 : -1;
      return a[sortField] > b[sortField] ? dir : -dir;
    }));
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
}
