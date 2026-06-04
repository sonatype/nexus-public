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
import { assign } from 'xstate';
import LoginPageStrings from '../../../constants/LoginPageStrings';
import FormUtils from '../../../interface/FormUtils';
import ValidationUtils from '../../../interface/ValidationUtils';
import ExtJS from '../../../interface/ExtJS';
import {parseRetryAfter} from '../../../utils/loginUtils';

const BLANK_FIELD_ERROR = ' ';

const localLoginMachine = FormUtils.buildFormMachine({
  id: 'LoginFormMachine',
  initial: 'loaded',
  stateAfterSave: 'loaded',
  config: (config) => {
    const loadedState = config.states.loaded || {};
    const loadedOn = loadedState.on || {};

    const newConfig = {
      ...config,
      context: {
        ...config.context,
        data: { username: '', password: '' },
        pristineData: { username: '', password: '' },
        skipValidation: false,
        rateLimitWarning: false,
        retryAfterSeconds: null
      },
      states: {
        ...config.states,
        loaded: {
          ...loadedState,
          on: {
            ...loadedOn,
            CLEAR_SAVE_ERROR: {
              actions: ['clearSaveError'],
              internal: true
            }
          }
        },
        saving: {
          ...config.states.saving,
          invoke: {
            ...config.states.saving.invoke,
            onError: [
              {
                cond: (_, event) => (event.data || event.value)?.response?.status === 429,
                target: 'rateLimited',
                actions: ['setRateLimitState', 'logSaveError', 'onSaveError']
              },
              {
                target: 'loaded',
                actions: ['setSaveError', 'logSaveError', 'onSaveError']
              }
            ]
          }
        },
        rateLimited: {
          invoke: {
            src: (_ctx, _e) => (send) => {
              const id = setInterval(() => send('TICK'), 1000);
              return () => clearInterval(id);
            }
          },
          on: {
            TICK: [
              {
                cond: (ctx) => ctx.retryAfterSeconds !== null && ctx.retryAfterSeconds > 1,
                actions: assign({ retryAfterSeconds: (ctx) => ctx.retryAfterSeconds - 1 })
              },
              {
                target: 'loaded',
                actions: 'clearRateLimit'
              }
            ]
          }
        }
      }
    };
    return newConfig;
  },
  options: (options) => {
    return {
      ...options,
      actions: {
        ...options.actions,
        validate: assign({
          validationErrors: ({ data, skipValidation }) => {
            if (skipValidation) {
              return {};
            }
            return {
              username: ValidationUtils.isBlank(data?.username) ? LoginPageStrings.ERRORS.USERNAME_REQUIRED : undefined,
              password: ValidationUtils.isBlank(data?.password) ? LoginPageStrings.ERRORS.PASSWORD_REQUIRED : undefined
            };
          },
          skipValidation: () => false
        }),
        setDirtyFlag: () => {},
        clearDirtyFlag: () => {},
        setRateLimitState: assign({
          rateLimitWarning: () => true,
          retryAfterSeconds: (_, event) => {
            const error = event.data || event.value;
            const headers = error?.response?.headers || {};
            return parseRetryAfter(headers['retry-after']);
          },
          saveError: () => undefined,
          saveErrors: () => ({})
        }),
        clearRateLimit: assign({
          rateLimitWarning: () => false,
          retryAfterSeconds: () => null
        }),
        setSaveError: assign({
          saveErrorData: ({ data }) => ({ ...(data ?? {}) }),
          saveError: (_, event) => {
            const error = event.data || event.value;
            const status = error?.response?.status;

            if (status === 403) {
              return LoginPageStrings.ERRORS.WRONG_CREDENTIALS;
            }
            return status === 0
              ? LoginPageStrings.ERRORS.CONNECTION_FAILED
              : error?.response?.data?.message || LoginPageStrings.ERRORS.AUTHENTICATION_FAILED;
          },
          saveErrors: (_, event) => {
            const error = event.data || event.value;
            if (error?.response?.status === 403) {
              return {
                username: BLANK_FIELD_ERROR,
                password: BLANK_FIELD_ERROR
              };
            }
            return {};
          },
          data: ({ data }, event) => {
            const error = event.data || event.value;
            if (error?.response?.status === 403) {
              return {
                ...data,
                password: ''
              };
            }
            return data;
          },
          validationErrors: ({ validationErrors }, event) => {
            const error = event.data || event.value;
            if (error?.response?.status === 403) {
              return {};
            }
            return validationErrors;
          },
          skipValidation: (_, event) => {
            const error = event.data || event.value;
            return error?.response?.status === 403;
          }
        }),
        clearSaveError: assign({
          saveErrorData: () => ({}),
          saveError: () => undefined,
          saveErrors: () => ({}),
          rateLimitWarning: () => false,
          retryAfterSeconds: () => null,
          skipValidation: () => true
        }),
        logSaveSuccess: () => {
          // Intentionally left blank - suppress default success toast for login flow
        },
        logSaveError: () => {
          // Intentionally left blank - error state is handled by setSaveError action
        },
        onSaveError: () => {
          // Intentionally left blank - form should be left in loaded state
        }
      },
      services: {
        ...options.services,
        saveData: ({ data }) => {
          return ExtJS.requestSession(data.username, data.password)
            .then((result) => {
              return {
                response: result.response,
                username: data.username
              };
            });
        }
      }
    };
  }
});

export default localLoginMachine;
