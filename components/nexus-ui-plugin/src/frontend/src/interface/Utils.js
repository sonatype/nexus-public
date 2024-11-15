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
import {useRef, useState} from 'react';
import {assign, createMachine} from "xstate";
import ExtJS from "./ExtJS";
import UIStrings from "../constants/UIStrings";
import {__, any, hasPath, is, join, map, path, pathOr, whereEq} from 'ramda';
import fileSize from 'file-size';

const FIELD_ID = 'FIELD ';
const PARAMETER_ID = 'PARAMETER ';

/**
 * @since 3.22
 * @deprecated - prefer to use other Util classes such as FormUtils and ValidationUtils
 */
export default class Utils {
  static urlFromPath(path) {
    return NX.app.relativePath + path;
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

  static timeoutPromise(time) {
    return new Promise(resolve => {
      setTimeout(resolve, time);
    });
  }

  /**
   * @param constructors a series of constructor functions
   * @param value a value to test
   * @return true if value is an instance of any of the types named
   * by the constructors. NOTE: if value is a primitive, it will be boxed
   * before checking. That is, isInstanceof([String], 'foo') returns true
   */
  static isInstanceOfAny(constructors, value) {
    return any(is(__, value), constructors);
  }

  static isInRange({value, min = -Infinity, max = Infinity, allowDecimals = true}) {
    if (value === null || value === undefined) {
      return null;
    }

    if (typeof value === 'string' && this.isBlank(value)) {
        return null;
    }

    const number = Number(value);
    if (isNaN(number)) {
      return UIStrings.ERROR.NAN
    }

    if (!allowDecimals && typeof value === 'string' && !/^-?[0-9]*$/.test(value)) {
      return UIStrings.ERROR.DECIMAL;
    }

    if (min > number) {
      return UIStrings.ERROR.MIN(min);
    }
    else if (max < number) {
      return UIStrings.ERROR.MAX(max);
    }
    else {
      return null;
    }
  }

  /**
   * Match the regex from components/nexus-validation/src/main/java/org/sonatype/nexus/validation/constraint/NamePatternConstants.java
   */
  static isName(name) {
    return name.match(/^[a-zA-Z0-9\-]{1}[a-zA-Z0-9_\-\.]*$/);
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
  static buildFormMachine({
                            id,
                            initial = 'loading',
                            stateAfterSave = 'loaded',
                            config = (config) => config,
                            options = (options) => options
                          })
  {
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
        data: {},
        pristineData: {}
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
          entry: ['validate', 'setDirtyFlag', 'setIsPristine', 'onLoadedEntry'],

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
            },
            CONFIRM_DELETE: {
              target: 'confirmDelete',
              cond: 'canDelete'
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
          on: {
            RETRY: {
              target: 'loading'
            }
          }
        },

        confirmDelete: {
          invoke: {
            src: 'confirmDelete',
            onDone: 'delete',
            onError: 'loaded'
          }
        },
        delete: {
          invoke: {
            src: 'delete',
            onDone: {
              target: 'loaded',
              actions: 'onDeleteSuccess'
            },
            onError: {
              target: 'loaded',
              actions: 'onDeleteError'
            }
          }
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
        postProcessData: () => {
        },
        setDirtyFlag: ({isPristine}) => ExtJS.setDirtyStatus(id, !isPristine),
        clearDirtyFlag: () => ExtJS.setDirtyStatus(id, false),

        setSaveError: assign({
          saveErrorData: ({data}) => data,
          saveError: (_, event) => {
            const data = event.data?.response?.data;
            if (data instanceof String) {
              return data;
            }
            else if (data instanceof Array) {
              return data.find(({id}) => id === '*')?.message;
            }
            return null;
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
          console.log(`Save Error: ${event.data?.message}`);
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
          isPristine: ({data, pristineData}) => whereEq(pristineData)(data)
        }),

        setSavedData: assign({
          pristineData: ({data}) => data,
          isTouched: () => ({})
        }),

        onLoadedEntry: () => {
          /* hook for users to override on entry to the loaded state */
        }
      },

      guards: {
        canSave: ({isPristine, validationErrors}) => {
          const isValid = !Utils.isInvalid(validationErrors);
          return !isPristine && isValid;
        },
        canDelete: () => false
      },

      services: {
        confirmDelete: () => Promise.reject('unimplemented'),
        delete: () => Promise.reject('unimplemented'),
        fetchData: () => Promise.resolve({data: {}})
      }
    };

    return createMachine(config(DEFAULT_CONFIG), options(DEFAULT_OPTIONS));
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

    return Boolean(Object.values(errors).find(error => {
      if (error === null || error === undefined) {
        return false;
      }
      else if (error.length > 0) {
        return true;
      }
      else {
        return this.isInvalid(error);
      }
    }));
  }

  /**
   * Generate common props for form fields
   * @param name
   * @param current a form machine generated by buildFormMachine
   * @param defaultValue if the machine did not provide any data
   * @return {{name: *, validationErrors: (*|[]), isPristine: boolean, value: (*|string)}}
   */
  static fieldProps(name, current, defaultValue = '', typeConversion = String) {
    const {data = {}, isTouched = {}, validationErrors = {}, saveErrors = {}, saveErrorData = {}} = current.context;

    if (!Array.isArray(name)) {
      name = [name];
    }

    let errors = null;
    if (path(name, isTouched) && path(name, validationErrors)) {
      errors = path(name, validationErrors);
    }
    else if (path(name, saveErrors) && path(name, saveErrorData) === path(name, data)) {
      errors = path(name, saveErrors);
    }

    return {
      id: join('.', name),
      name: join('.', name),
      value: typeConversion(pathOr(defaultValue, name, data)),
      isPristine: hasPath(name, isTouched) ? !path(name, isTouched) : true,
      validatable: true,
      validationErrors: errors || null
    };
  }

  /**
   * Generate common props for checkbox fields
   * @param name
   * @param current a form machine generated by buildFormMachine
   * @param defaultValue if the machine did not provide a value for the checkbox (defaults to false)
   * @return {{name: string, isChecked: boolean}}
   */
  static checkboxProps(name, current, defaultValue = false) {
    const {data = {}} = current.context;

    if (!Array.isArray(name)) {
      name = [name];
    }

    return {
      checkboxId: String(join('.', name)),
      name: String(join('.', name)),
      isChecked: Boolean(pathOr(defaultValue, name, data))
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
   * Convert a size in bytes to a human readable string
   * @param bytes
   * @return {*}
   */
  static bytesToString(bytes) {
    if (bytes < 0) {
      return UIStrings.UNAVAILABLE;
    }
    return fileSize(bytes).human('si');
  }

  /**
   * Provide a random id to be used to tie a label to a field.
   * This function should be avoided when possible and should be replaced with one from the RSC when available.
   */
  static getRandomId(prefix) {
    const typedArray = new Uint8Array(new ArrayBuffer(8));

    crypto.getRandomValues(typedArray);

    const randomHexString = join('', map(x => x.toString(16), Array.from(typedArray)));

    return `${prefix}-${randomHexString}`;
  }

  /**
   * Get a random id to use to tie a label to a field. This hook will be stable across re-renders.
   * This function should be avoided when possible and should be replaced with one from the RSC when available.
   */
  static useRandomId(prefix, explicitId) {
    const idBox = useRef();

    if (explicitId != null) {
      return explicitId;
    }
    else {
      if (idBox.current == null) {
        idBox.current = Utils.getRandomId(prefix);
      }

      return idBox.current;
    }
  }

  /**
   * Returns debounced function and cancel method, e.g, for using to implement 
   * delayed mouse hover handler
   * @param fun function to debounce
   * @param arg debounced function's argument
   * @param delay delay time in milliseconds
   * @return {[debounced: function, cancel: function]}
   */
   static useDebounce(fun, args, delay) {
    const [task, setTask] = useState(null);
    const debounced = () => setTask(setTimeout(() => fun(args), delay));
    const cancel = () => clearTimeout(task);
    return [debounced, cancel];
  }
}
