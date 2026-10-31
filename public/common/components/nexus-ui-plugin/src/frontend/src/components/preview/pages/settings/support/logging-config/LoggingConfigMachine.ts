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
import { LOGGING_CONFIG_API } from './types';

export interface LoggingConfigContext {
  selectedLogger: string | null;
  error: string | null;
  refreshKey: number;
}

type LoggingConfigEvent =
  | { type: 'SELECT'; name: string }
  | { type: 'CREATE' }
  | { type: 'BACK' }
  | { type: 'SAVE' }
  | { type: 'DELETE_CLICK' }
  | { type: 'CONFIRM_DELETE' }
  | { type: 'CANCEL_DELETE' }
  | { type: 'RESET_ALL_CLICK' }
  | { type: 'CONFIRM_RESET_ALL' }
  | { type: 'CANCEL_RESET_ALL' }
  | { type: 'CLEAR_ERROR' }
  | { type: 'done.invoke.deleteLogger' }
  | { type: 'error.platform.deleteLogger'; data: Error }
  | { type: 'done.invoke.resetAll' }
  | { type: 'error.platform.resetAll'; data: Error };

// Typed event aliases used in assign actions below
type SelectEvent = Extract<LoggingConfigEvent, { type: 'SELECT' }>;
type ServiceErrorEvent = Extract<LoggingConfigEvent, { type: 'error.platform.deleteLogger' | 'error.platform.resetAll' }>;

export const loggingConfigMachine = createMachine<LoggingConfigContext, LoggingConfigEvent>(
  {
    id: 'loggingConfig',
    initial: 'list',
    predictableActionArguments: true,
    context: {
      selectedLogger: null,
      error: null,
      refreshKey: 0,
    },
    states: {
      list: {
        on: {
          SELECT: {
            target: 'editing',
            actions: 'setSelectedLogger',
          },
          CREATE: 'creating',
          RESET_ALL_CLICK: 'confirmResetAll',
          CLEAR_ERROR: { actions: 'clearError' },
        },
      },
      creating: {
        on: {
          BACK: {
            target: 'list',
            actions: 'clearSelectedLogger',
          },
          SAVE: {
            target: 'list',
            actions: ['clearSelectedLogger', 'incrementRefreshKey'],
          },
          CLEAR_ERROR: { actions: 'clearError' },
        },
      },
      editing: {
        on: {
          BACK: {
            target: 'list',
            actions: 'clearSelectedLogger',
          },
          SAVE: {
            target: 'list',
            actions: ['clearSelectedLogger', 'incrementRefreshKey'],
          },
          DELETE_CLICK: 'confirmDelete',
          CLEAR_ERROR: { actions: 'clearError' },
        },
      },
      confirmDelete: {
        on: {
          CONFIRM_DELETE: 'deleting',
          CANCEL_DELETE: 'editing',
        },
      },
      deleting: {
        invoke: {
          id: 'deleteLogger',
          src: 'deleteLogger',
          onDone: {
            target: 'list',
            actions: ['clearSelectedLogger', 'incrementRefreshKey', 'clearError'],
          },
          onError: {
            target: 'editing',
            actions: 'setError',
          },
        },
      },
      confirmResetAll: {
        on: {
          CONFIRM_RESET_ALL: 'resettingAll',
          CANCEL_RESET_ALL: 'list',
        },
      },
      resettingAll: {
        invoke: {
          id: 'resetAll',
          src: 'resetAll',
          onDone: {
            target: 'list',
            actions: ['incrementRefreshKey', 'clearError'],
          },
          onError: {
            target: 'list',
            actions: 'setError',
          },
        },
      },
    },
  },
  {
    actions: {
      setSelectedLogger: assign({
        selectedLogger: (_, event) => (event as SelectEvent).name,
        error: null,
      }),
      clearSelectedLogger: assign({
        selectedLogger: null,
      }),
      incrementRefreshKey: assign({
        refreshKey: (context) => context.refreshKey + 1,
      }),
      setError: assign({
        error: (_, event) => (event as ServiceErrorEvent).data?.message ?? 'An error occurred',
      }),
      clearError: assign({
        error: null,
      }),
    },
    services: {
      deleteLogger: async (context) => {
        // Explicit guard rather than non-null assertion: today `deleting` is only reachable via
        // CONFIRM_DELETE from `confirmDelete` (which is only reachable from `editing` where
        // selectedLogger is set), but a future direct transition to `deleting` would otherwise
        // pass null to the API silently.
        if (!context.selectedLogger) {
          throw new Error('No logger selected for deletion');
        }
        try {
          // Nexus represents "delete logger override" as POST to /reset — there is no DELETE endpoint.
          // The RESET call removes the custom level and reverts the logger to its inherited default.
          await restClient.post(LOGGING_CONFIG_API.RESET(context.selectedLogger));
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to delete logger override');
        }
      },
      resetAll: async () => {
        try {
          await restClient.post(LOGGING_CONFIG_API.RESET_ALL);
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to reset all loggers');
        }
      },
    },
  }
);
