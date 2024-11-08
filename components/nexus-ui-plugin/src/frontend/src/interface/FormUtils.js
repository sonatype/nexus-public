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
import ExtJS from './ExtJS';
import UIStrings from '../constants/UIStrings';
import Utils from './Utils';
import {any, dissocPath, hasPath, join, lensPath, path, pathOr, set, whereEq} from 'ramda';

const FIELD_ID = 'FIELD ';
const PARAMETER_ID = 'PARAMETER ';
const HELPER_BEAN = 'HelperBean.';

const SUBMITTING = false;
const SUBMITTED = true;
const IDLE = null;

/**
 * @since 3.31
 */
export default class FormUtils {
  /**
   * Builds a new xstate machine used to handle forms.
   * Typically the validation action, fetchData service, and saveData service should be implemented in withConfig.
   *
   * FormUtils.buildFormMachine({id: 'MyMachine'}).withConfig({
   *   actions: {
   *     validate: assign({
   *       validationErrors: ({data}) => ({
   *         field: FormUtils.isBlank(data.field) ? UIStrings.ERROR.FIELD_REQUIRED : null
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
  }) {
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
              actions: ['clearLoadError', 'setData', 'postProcessData', 'validate']
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
            },
            DELETE_DATA_PROPERTY: {
              target: 'loaded',
              actions: 'deleteDataProperty'
            },
            ADD_DATA_PROPERTY: {
              target: 'loaded',
              actions: 'addDataProperty'
            }
          }
        },

        saving: {
          entry: 'clearSaveError',

          invoke: {
            src: 'saveData',
            onDone: {
              target: stateAfterSave,
              actions: [
                'clearDirtyFlag',
                'clearSaveError',
                'setSavedData',
                'logSaveSuccess',
                'onSaveSuccess'
              ]
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
              target: 'ended',
              actions: ['clearDirtyFlag', 'logDeleteSuccess']
            },
            onError: {
              target: 'loaded',
              actions: 'onDeleteError'
            }
          }
        },
        ended: {
          type: 'final',
          entry: 'onDeleteSuccess'
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
          saveError: (_, event) => FormUtils.extractSaveErrorMessage(event),
          saveErrors: (_, event) => {
            const data = event.data?.response?.data;
            if (data instanceof Array) {
              let saveErrors = {};
              data.forEach(({id, message}) => {
                id = id.replace(FIELD_ID, '');
                id = id.replace(PARAMETER_ID, '');
                id = id.replace(HELPER_BEAN, '');
                saveErrors[id] = message;
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
          if (event.data?.message) {
            console.log(`Save Error: ${event.data?.message}`);
          }
        },
        logSaveSuccess: () => ExtJS.showSuccessMessage(UIStrings.SAVE_SUCCESS),
        logDeleteSuccess: () => {},

        setLoadError: assign({
          loadError: (_, event) => {
            const message = event.data?.message;
            return message ? `${message}` : null;
          }
        }),
        logLoadError: (_, event) => {
          if (event.data?.message) {
            console.log(`Load Error: ${event.data?.message}`);
          }
          ExtJS.showErrorMessage(UIStrings.ERROR.LOAD_ERROR);
        },
        clearLoadError: assign({
          loadError: null,
        }),

        onSaveSuccess: () => {},

        reset: assign({
          data: ({pristineData}) => pristineData,
          isTouched: () => ({})
        }),

        deleteDataProperty: assign({
          data: ({data}, {path}) => dissocPath(path.split('.'), data)
        }),

        addDataProperty: assign({
          data: ({data}, {path, value}) => {
            const pth = path.split('.');
            return hasPath(pth, data) ? data : set(lensPath(pth), value, data);
          }
        }),

        update: assign({
          data: FormUtils.updateFormDataDefaultAction,
          isTouched: ({isTouched}, event) => {
            if (event.name) {
              const pthArray = event.name instanceof Array ? event.name : event.name.split('.');
              return set(lensPath(pthArray), true, isTouched);
            }

            const result = {...isTouched};
            Object.keys(event.data).forEach((key) => (result[key] = true));
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
          const isValid = !FormUtils.isInvalid(validationErrors);
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
   * Handles form data updates according to the received event
   * @param data to update
   * @param event that triggered the update
   * @returns {data: object} updated data
   */
  static updateFormDataDefaultAction({data}, event) {
    if (event.name) {
      const pthArray = event.name instanceof Array ? event.name : event.name.split('.');
      return set(lensPath(pthArray), event.value, data);
    }
    else if (event.data) {
      return {
        ...data,
        ...event.data
      };
    }
    else {
      console.error('update event must have a name and value or a data object', event);
    }
  }

  /**
   * Extract api error message if possible
   * @param event
   * @return {string|null}
   */
  static extractSaveErrorMessage(event) {
    const data = event.data?.response?.data || event.data?.message || event.data;
    let error = null;
    if (typeof data === 'string') {
      error = data;
    } else if (typeof data === 'object' && data.id === '*') {
      error = data.message
    }
    return error;
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

    return Boolean(
      Object.values(errors).find((error) => {
        if (error === null || error === undefined) {
          return false;
        }
        else if (Array.isArray(error)) {
          return error.some(e => this.isInvalid(e));
        }
        else if (error.length > 0) {
          return true;
        }
        else {
          return this.isInvalid(error);
        }
      })
    );
  }

  /**
   * Generate the standard props for NxForms.
   *
   * @param state {{context: {isPristine: boolean, loadError: string, saveError: string, validationErrors: any}, matches: function}}
   * @param send {function}
   * @returns {{submitError, submitMaskMessage: string, doLoad: doLoad, loadError, onSubmit: onSubmit, validationErrors: (string|null), submitBtnText: string, loading, submitMaskState: boolean}}
   */
  static formProps(state, send) {
    const {data = {}, saveErrorData = {}, isPristine, loadError, saveError, saveErrors, validationErrors} = state.context;
    const isInvalid = FormUtils.isInvalid(validationErrors);
    const loading = state.matches('loading');
    const saveTooltip = FormUtils.saveTooltip({isPristine, isInvalid});
    const submitBtnText = UIStrings.SETTINGS.SAVE_BUTTON_LABEL;
    const submitMaskMessage = UIStrings.SAVING;
    const submitMaskState = FormUtils.submitMaskState(state);

    const doLoad = () => send({type: 'RETRY'});
    const onSubmit = () => send({type: 'SAVE'});

    let submitError = saveError;
    if (!submitError && FormUtils.isInvalid(saveErrors)) {
      submitError = UIStrings.ERROR.SAVE_ERROR;
    }
    if (!whereEq(data, saveErrorData)) {
      submitError = null;
    }

    return {
      doLoad,
      loading,
      loadError,
      onSubmit,
      submitBtnText,
      submitError,
      submitMaskMessage,
      submitMaskState,
      validationErrors: saveTooltip
    }
  }

  /**
   * Generate common props for form fields
   * @param name
   * @param current a form machine generated by buildFormMachine
   * @param defaultValue if the machine did not provide any data
   * @return {{name: *, validationErrors: (*|[]), isPristine: boolean, value: (*|string)}}
   */
  static fieldProps(name, current, defaultValue = '') {
    const {
      data = {},
      isTouched = {},
      validationErrors = {},
      saveErrors = {},
      saveErrorData = {}
    } = current.context;

    if (!Array.isArray(name)) {
      name = name.split('.');
    }

    const saveError = path(name, saveErrors);
    const savedValue = path(name, saveErrorData);
    const currentValue = path(name, data);
    const validationError = path(name, validationErrors);

    let errors = null;
    if (Boolean(savedValue) && saveError && currentValue === savedValue) {
      errors = saveError;
    }
    else if (Boolean(validationError)) {
      errors = validationError;
    }

    return {
      id: join('.', name),
      name: join('.', name),
      value: String(pathOr(defaultValue, name, data)),
      isPristine: hasPath(name, isTouched) ? !path(name, isTouched) : true,
      validatable: true,
      validationErrors: errors || null
    };
  }

  /**
   * Generate common props for form fields
   * @param name
   * @param current a form machine generated by buildFormMachine
   * @param defaultValue if the machine did not provide any data
   * @return {{name: *, isPristine: boolean, value: (*|string)}}
   */
  static selectProps(name, current, defaultValue = '') {
    const {
      data = {},
      isTouched = {}
    } = current.context;

    if (!Array.isArray(name)) {
      name = name.split('.');
    }

    return {
      id: join('.', name),
      name: join('.', name),
      value: String(pathOr(defaultValue, name, data)),
      isPristine: hasPath(name, isTouched) ? !path(name, isTouched) : true
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
      name = name.split('.');
    }

    return {
      checkboxId: String(join('.', name)),
      name: String(join('.', name)),
      isChecked: Boolean(pathOr(defaultValue, name, data))
    };
  }

  /**
   * Generate common props for file uploads
   * @param name
   * @param current a form machine generated by buildFormMachine
   * @return {{name: *, isPristine: boolean, files: FileList|null}}
   */
  static fileUploadProps(name, current) {
    const {
      data = {},
      isTouched = {}
    } = current.context;

    if (!Array.isArray(name)) {
      name = name.split('.');
    }

    return {
      id: join('.', name),
      name: join('.', name),
      files: pathOr(null, name, data),
      isPristine: hasPath(name, isTouched) ? !path(name, isTouched) : true,
    };
  }

  /**
   * Generate a function that will send a standard form UPDATE message to a machine
   * @param name of the field to update
   * @param send - a function that sends events to the machine
   * @returns {(function(*): void)|*}
   */
  static handleUpdate(name, send, type = 'UPDATE') {
    return (value) => {
      if (value instanceof Set) {
        value = Array.from(value);
      }
      send({type, name, value});
    };
  }

  /**
   * Removes trailing spaces for a given input value on blur event.
   * @param name of the field to update
   * @param send - a function that sends events to the machine.
   * @returns {(function(event): void)|*}
   */
  static trimOnBlur(name, send) {
    return (event) => {
      const value = event.target.value.trim();
      this.handleUpdate(name, send)(value);
    }
  }

  /**
   * @param isPristine
   * @param isInvalid
   * @return {string|null} the tooltip explaining why the save button is disabled
   */
  static saveTooltip({isPristine, isInvalid}) {
    if (isPristine) {
      return UIStrings.PRISTINE_TOOLTIP;
    } else if (isInvalid) {
      return UIStrings.INVALID_TOOLTIP;
    }
    return null;
  }

  static getValidationErrorsMessage(state) {
    const {isPristine, validationErrors} = state.context;
    const isInvalid = this.isInvalid(validationErrors);
    return this.saveTooltip({isPristine, isInvalid});
  }

  static discardTooltip({isPristine}) {
    if (isPristine) {
      return UIStrings.PRISTINE_TOOLTIP;
    }
  }

  /**
   * @param enabled [required]
   * @return {string} read only checkbox status label
   */
  static readOnlyCheckboxValueLabel(enabled) {
    return enabled
      ? UIStrings.SETTINGS.READ_ONLY.CHECKBOX.ENABLED
      : UIStrings.SETTINGS.READ_ONLY.CHECKBOX.DISABLED;
  }

  /**
   * Determine the submit mask state. False when submitting, true when submitted, and null otherwise.
   *
   * @param state [required] a machine state
   * @param submittingStates [optional] {string[]} an array of submitting states
   * @returns {null|boolean}
   */
  static submitMaskState(state, submittingStates = ['saving']) {
    if (this.isInState(state, submittingStates)) {
      return SUBMITTING;
    }
    else if (state.matches('saved')) {
      return SUBMITTED;
    }
    else {
      return IDLE;
    }
  }

  /**
   * Returns true if the machine is in one of the given states, and false otherwise.
   *
   * @param state [required] a machine state
   * @param states [required] {string[]} a states to check
   * @returns {boolean}
   */
  static isInState(state, states) {
    return any(it => state.matches(it), states);
  }
}
