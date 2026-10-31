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
import { LOGS_API } from './types';

export interface LogViewerContext {
  filename: string;
  logContent: string;
  refreshPeriod: number;
  logSize: number;
  mark: string;
  error: string | null;
}

type LogViewerEvent =
  | { type: 'SET_REFRESH_PERIOD'; value: number }
  | { type: 'SET_LOG_SIZE'; value: number }
  | { type: 'SET_MARK'; value: string }
  | { type: 'INSERT_MARK' }
  | { type: 'RETRY' }
  | { type: 'REFRESH' }
  | { type: 'done.invoke.fetchContent'; data: string }
  | { type: 'error.platform.fetchContent'; data: Error }
  | { type: 'done.invoke.refreshContent'; data: string }
  | { type: 'error.platform.refreshContent'; data: Error }
  | { type: 'done.invoke.insertMark' }
  | { type: 'error.platform.insertMark'; data: Error };

// Typed event aliases used in assign actions below
type ContentDoneEvent = Extract<LogViewerEvent, { type: 'done.invoke.fetchContent' | 'done.invoke.refreshContent' }>;
type ContentErrorEvent = Extract<LogViewerEvent, { type: 'error.platform.fetchContent' | 'error.platform.refreshContent' | 'error.platform.insertMark' }>;
type SetRefreshPeriodEvent = Extract<LogViewerEvent, { type: 'SET_REFRESH_PERIOD' }>;
type SetLogSizeEvent = Extract<LogViewerEvent, { type: 'SET_LOG_SIZE' }>;
type SetMarkEvent = Extract<LogViewerEvent, { type: 'SET_MARK' }>;

export const logViewerMachine = createMachine<LogViewerContext, LogViewerEvent>(
  {
    id: 'logViewer',
    initial: 'loading',
    predictableActionArguments: true,
    context: {
      filename: '',
      logContent: '',
      refreshPeriod: 0,
      logSize: 25,
      mark: '',
      error: null,
    },
    states: {
      loading: {
        invoke: {
          id: 'fetchContent',
          src: 'fetchContent',
          onDone: {
            target: 'loaded',
            actions: 'setContent',
          },
          onError: {
            target: 'error',
            actions: 'setError',
          },
        },
      },
      loaded: {
        // Sub-states isolate the auto-refresh timer: it only exists in `polling`, so when
        // refreshPeriod is 0 no timer is scheduled (idle) — avoiding a wasted 0ms timer
        // with a redundant guard. SET_REFRESH_PERIOD re-enters via `.check`, which cancels
        // any pending timer and re-evaluates the delay against the new refreshPeriod.
        initial: 'check',
        on: {
          RETRY: 'loading',
          REFRESH: 'refreshing',
          SET_REFRESH_PERIOD: {
            target: '.check',
            actions: 'setRefreshPeriod',
          },
          SET_LOG_SIZE: {
            target: 'loading',
            actions: 'setLogSize',
          },
          SET_MARK: { actions: 'setMark' },
          INSERT_MARK: 'insertingMark',
        },
        states: {
          check: {
            always: [
              { target: 'polling', cond: 'hasRefreshPeriod' },
              { target: 'idle' },
            ],
          },
          idle: {},
          polling: {
            after: {
              REFRESH_DELAY: '#logViewer.refreshing',
            },
          },
        },
      },
      refreshing: {
        invoke: {
          // Reuses fetchContent service — same endpoint, different error recovery path
          // (refresh error returns to loaded to preserve existing content; initial load error goes to error state)
          id: 'refreshContent',
          src: 'fetchContent',
          onDone: {
            target: 'loaded',
            actions: 'setContent',
          },
          onError: {
            // On refresh error, go back to loaded (keep existing content)
            target: 'loaded',
            actions: 'setError',
          },
        },
        on: {
          SET_REFRESH_PERIOD: { actions: 'setRefreshPeriod' },
          SET_MARK: { actions: 'setMark' },
        },
      },
      insertingMark: {
        invoke: {
          id: 'insertMark',
          src: 'insertMark',
          onDone: {
            // After inserting mark, reload to show it
            target: 'loading',
            actions: 'clearMark',
          },
          onError: {
            target: 'loaded',
            actions: 'setError',
          },
        },
      },
      error: {
        on: {
          RETRY: 'loading',
          SET_REFRESH_PERIOD: { actions: 'setRefreshPeriod' },
          SET_LOG_SIZE: { actions: 'setLogSize' },
        },
      },
    },
  },
  {
    guards: {
      hasRefreshPeriod: (context) => context.refreshPeriod > 0,
    },
    delays: {
      REFRESH_DELAY: (context) => context.refreshPeriod * 1000,
    },
    actions: {
      setContent: assign({
        logContent: (_, event) => (event as ContentDoneEvent).data ?? '',
        error: null,
      }),
      setError: assign({
        error: (_, event) => (event as ContentErrorEvent).data?.message ?? 'Failed to load log content',
      }),
      setRefreshPeriod: assign({
        refreshPeriod: (_, event) => (event as SetRefreshPeriodEvent).value,
      }),
      setLogSize: assign({
        logSize: (_, event) => (event as SetLogSizeEvent).value,
      }),
      setMark: assign({
        mark: (_, event) => (event as SetMarkEvent).value,
      }),
      clearMark: assign({
        mark: '',
        error: null,
      }),
    },
    services: {
      fetchContent: async (context) => {
        try {
          const bytesCount = context.logSize * -1024;
          const data = await restClient.get<string>(LOGS_API.VIEW(context.filename), {
            params: { bytesCount },
            headers: { Accept: 'text/plain' },
          });
          return data || '';
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to load log content');
        }
      },
      insertMark: async (context) => {
        try {
          await restClient.post(LOGS_API.MARK, context.mark || 'MARK', {
            headers: { 'Content-Type': 'text/plain' },
          });
        } catch (err: any) {
          const apiError = parseApiError(err);
          throw new Error(apiError.message || 'Failed to insert mark');
        }
      },
    },
  },
);
