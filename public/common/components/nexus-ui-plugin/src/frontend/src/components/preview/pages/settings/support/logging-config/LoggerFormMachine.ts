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

import { assign, createMachine } from 'xstate';
import { restClient, parseApiError } from '../../../../../../interface/api';
import { Logger, LogLevel, LOGGING_CONFIG_API } from './types';

export interface LoggerFormContext {
  name: string;
  level: LogLevel;
  originalLevel: LogLevel;
  isCreate: boolean;
  loggerName: string;
  error: string | null;
}

type LoggerFormEvent =
  | { type: 'SET_NAME'; value: string }
  | { type: 'SET_LEVEL'; value: LogLevel }
  | { type: 'SUBMIT' }
  | { type: 'RETRY' }
  | { type: 'done.invoke.fetchLogger'; data: Logger }
  | { type: 'error.platform.fetchLogger'; data: Error }
  | { type: 'done.invoke.saveLogger' }
  | { type: 'error.platform.saveLogger'; data: Error };

// Typed event aliases used in assign actions below
type FetchDoneEvent = Extract<LoggerFormEvent, { type: 'done.invoke.fetchLogger' }>;
type FetchErrorEvent = Extract<LoggerFormEvent, { type: 'error.platform.fetchLogger' }>;
type SaveErrorEvent = Extract<LoggerFormEvent, { type: 'error.platform.saveLogger' }>;
type SetNameEvent = Extract<LoggerFormEvent, { type: 'SET_NAME' }>;
type SetLevelEvent = Extract<LoggerFormEvent, { type: 'SET_LEVEL' }>;

export const loggerFormMachine = createMachine<LoggerFormContext, LoggerFormEvent>(
  {
    id: 'loggerForm',
    initial: 'init',
    predictableActionArguments: true,
    context: {
      name: '',
      level: 'INFO',
      originalLevel: 'INFO',
      isCreate: false,
      loggerName: '',
      error: null,
    },
    states: {
      init: {
        always: [
          { target: 'loading', cond: 'isEdit' },
          { target: 'form' },
        ],
      },
      loading: {
        invoke: {
          id: 'fetchLogger',
          src: 'fetchLogger',
          onDone: {
            target: 'form',
            actions: 'setLoggerData',
          },
          onError: {
            target: 'fetchError',
            actions: 'setError',
          },
        },
      },
      form: {
        on: {
          SET_NAME: { actions: 'setName' },
          SET_LEVEL: { actions: 'setLevel' },
          SUBMIT: {
            target: 'saving',
            cond: 'isFormValid',
            actions: 'clearError',
          },
        },
      },
      saving: {
        invoke: {
          id: 'saveLogger',
          src: 'saveLogger',
          onDone: {
            target: 'success',
          },
          onError: {
            target: 'form',
            actions: 'setSaveError',
          },
        },
      },
      fetchError: {
        on: {
          RETRY: 'loading',
        },
      },
      success: {
        type: 'final',
      },
    },
  },
  {
    guards: {
      isEdit: (context) => !context.isCreate,
      // For create mode: name must be non-empty. For edit mode: level must differ from the loaded value.
      isFormValid: (context) =>
        context.isCreate ? context.name.trim().length > 0 : context.level !== context.originalLevel,
    },
    actions: {
      setLoggerData: assign({
        name: (_, event) => (event as FetchDoneEvent).data?.name ?? '',
        level: (_, event) => (event as FetchDoneEvent).data?.level ?? 'INFO',
        originalLevel: (_, event) => (event as FetchDoneEvent).data?.level ?? 'INFO',
        error: null,
      }),
      setName: assign({
        name: (_, event) => (event as SetNameEvent).value,
      }),
      setLevel: assign({
        level: (_, event) => (event as SetLevelEvent).value,
      }),
      setError: assign({
        error: (_, event) => (event as FetchErrorEvent).data?.message ?? 'Failed to load logger',
      }),
      setSaveError: assign({
        error: (_, event) => (event as SaveErrorEvent).data?.message ?? 'Failed to save logger',
      }),
      clearError: assign({
        error: null,
      }),
    },
    services: {
      fetchLogger: async (context) => {
        try {
          const data = await restClient.get<Logger>(LOGGING_CONFIG_API.GET(context.loggerName));
          return data;
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to load logger');
        }
      },
      saveLogger: async (context) => {
        try {
          await restClient.put(LOGGING_CONFIG_API.UPDATE(context.name), { level: context.level });
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to save logger');
        }
      },
    },
  }
);
