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
   * @param config [optional] a function used to change the config of the machine
   * @return {StateMachine<any, any, AnyEventObject>}
   */
  static buildFormMachine({id, initial = 'loading', config = (config) => config, options = (options) => options}) {
    const DEFAULT_CONFIG = {
      id,
      initial,

      context: {
        isPristine: true,
        isTouched: false,
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
            SAVE: 'saving',
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
              target: initial,
              actions: ['clearSaveError', 'logSaveSuccess', 'onSaveSuccess']
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
          data: ({pristineData}) => pristineData
        }),

        update: assign({
          data: ({data}, event) => ({...data, ...event.data}),
          isTouched: () => true
        }),

        setIsPristine: assign({
          isPristine: ({data, pristineData}) => Object.keys(pristineData).every(
              key => pristineData[key] === data[key])
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
}
