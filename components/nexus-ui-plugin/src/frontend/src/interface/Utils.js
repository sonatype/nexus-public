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

/**
 * @since 3.22
 */
export default class Utils {
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
   * @param loading [optional] the initial state of the machine
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
        validationErrors: {},
      },

      states: {
        loading: {
          invoke: {
            id: 'fetchData',
            src: 'fetchData',
            onDone: {
              target: 'loaded',
              actions: ['setData', 'validate']
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
              actions: ['reset']
            }
          },

          exit: 'clearSaveError'
        },

        saving: {
          invoke: {
            src: 'saveData',
            onDone: {
              target: stateAfterSave,
              actions: ['clearSaveError', 'setSavedData', 'logSaveSuccess', 'onSaveSuccess']
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
      }
    };

    const DEFAULT_OPTIONS = {
      actions: {
        setData: assign({
          data: (_, event) => event.data?.data,
          pristineData: (_, event) => event.data?.data
        }),

        setDirtyFlag: ({isPristine}) => ExtJS.setDirtyStatus(id, !isPristine),
        clearDirtyFlag: () => ExtJS.setDirtyStatus(id, false),

        setSaveError: assign({
          saveError: (_, event) => event.data?.response?.data
        }),
        clearSaveError: assign({
          saveError: () => undefined
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
              key => pristineData[key] === data[key])
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
    const {data, isTouched, validationErrors} = current.context;
    return {
      name,
      value: (data && data[name]) || defaultValue,
      isPristine: name in isTouched ? !isTouched[name] : true,
      validatable: true,
      validationErrors: (name in isTouched) ? validationErrors[name] : []
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
}
