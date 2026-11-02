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
import { assign, createMachine } from 'xstate';
import LoginPageStrings from '../../../constants/LoginPageStrings';
import ValidationUtils from '../../../interface/ValidationUtils';
import ExtJS from '../../../interface/ExtJS';
import { parseRetryAfter } from '../../../utils/loginUtils';

const BLANK_FIELD_ERROR = ' ';

/**
 * Extracts the error payload from an XState v4 invoke rejection. Depending on the shape of
 * the rejected promise/observable, the error can land on `event.data` or `event.value`.
 */
function getError(event) {
  return event.data || event.value;
}

const localLoginMachine = createMachine(
  {
    id: 'LoginFormMachine',
    predictableActionArguments: true,
    initial: 'editing',
    context: {
      data: { username: '', password: '' },
      validationErrors: {},
      saveError: undefined,
      saveErrors: {},
      saveErrorData: {},
      rateLimitWarning: false,
      retryAfterSeconds: null,
      skipValidation: false
    },
    states: {
      editing: {
        entry: 'validate',
        on: {
          UPDATE: {
            target: 'editing',
            actions: 'update',
            internal: false
          },
          SUBMIT: {
            target: 'authenticating',
            cond: 'isValid'
          },
          CLEAR_SAVE_ERROR: {
            actions: 'clearSaveError',
            internal: true
          }
        }
      },

      authenticating: {
        entry: 'clearSaveError',
        invoke: {
          src: 'saveData',
          onDone: {
            target: 'editing',
            actions: 'onSaveSuccess'
          },
          onError: [
            {
              cond: 'isRateLimited',
              target: 'rateLimited',
              actions: ['setRateLimitState', 'logSaveError']
            },
            {
              target: 'editing',
              actions: ['setSaveError', 'logSaveError']
            }
          ]
        }
      },

      rateLimited: {
        invoke: {
          src: () => (send) => {
            const id = setInterval(() => send('TICK'), 1000);
            return () => clearInterval(id);
          }
        },
        on: {
          TICK: [
            {
              cond: 'shouldContinueCountdown',
              actions: 'decrementRetryAfter'
            },
            {
              target: 'editing',
              actions: 'clearRateLimit'
            }
          ]
        }
      }
    }
  },
  {
    actions: {
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

      update: assign({
        data: ({ data }, event) => ({ ...data, [event.name]: event.value })
      }),

      setRateLimitState: assign({
        rateLimitWarning: () => true,
        retryAfterSeconds: (_, event) => {
          const error = getError(event);
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

      decrementRetryAfter: assign({
        retryAfterSeconds: ({ retryAfterSeconds }) => retryAfterSeconds - 1
      }),

      setSaveError: assign({
        saveErrorData: ({ data }) => ({ ...(data ?? {}) }),
        saveError: (_, event) => {
          const error = getError(event);
          const status = error?.response?.status;

          if (status === 403) {
            return LoginPageStrings.ERRORS.WRONG_CREDENTIALS;
          }
          return status === 0
            ? LoginPageStrings.ERRORS.CONNECTION_FAILED
            : error?.response?.data?.message || LoginPageStrings.ERRORS.AUTHENTICATION_FAILED;
        },
        saveErrors: (_, event) => {
          const error = getError(event);
          if (error?.response?.status === 403) {
            return {
              username: BLANK_FIELD_ERROR,
              password: BLANK_FIELD_ERROR
            };
          }
          return {};
        },
        data: ({ data }, event) => {
          const error = getError(event);
          if (error?.response?.status === 403) {
            return {
              ...data,
              password: ''
            };
          }
          return data;
        },
        validationErrors: ({ validationErrors }, event) => {
          const error = getError(event);
          if (error?.response?.status === 403) {
            return {};
          }
          return validationErrors;
        },
        skipValidation: (_, event) => {
          const error = getError(event);
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

      onSaveSuccess: () => {
        // Intentionally left blank for the component to override (needs router access)
      },
      logSaveError: () => {
        // Intentionally left blank for the component to override (reports non-403/429 errors)
      }
    },

    guards: {
      isValid: ({ validationErrors }) => !validationErrors?.username && !validationErrors?.password,
      isRateLimited: (_, event) => getError(event)?.response?.status === 429,
      shouldContinueCountdown: ({ retryAfterSeconds }) => retryAfterSeconds !== null && retryAfterSeconds > 1
    },

    services: {
      saveData: ({ data }) =>
        ExtJS.requestSession(data.username, data.password).then((result) => ({
          response: result.response,
          username: data.username
        }))
    }
  }
);

export default localLoginMachine;
