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

export type RevealTokenType = 'user' | 'nuget';

export interface RevealTokenServices<TStatus, TToken> {
  /**
   * Optional — loads token metadata (enabled? has token? expiration?). When omitted, the
   * machine skips the loading state and starts in idle with status = null. NuGet uses
   * this shape because it has no status endpoint; UserToken always provides it.
   */
  checkTokenStatus?: () => Promise<TStatus>;
  /** Reveals the existing token value. Auth is expected to happen inside this service. */
  fetchToken: () => Promise<TToken>;
  /** Generates a new token value. Optional — UserToken uses it, NuGet does not. */
  generateToken?: () => Promise<TToken>;
  /** Deletes the token. Auth happens inside the service. */
  deleteToken: () => Promise<void>;
}

export interface RevealTokenContext<TStatus, TToken> {
  tokenType: RevealTokenType;
  status: TStatus | null;
  tokenValue: TToken | null;
  loadError: string | null;
  actionError: string | null;
}

export type RevealTokenEvent =
  | { type: 'REVEAL' }
  | { type: 'GENERATE' }
  | { type: 'DELETE' }
  | { type: 'HIDE' }
  | { type: 'CLEAR_ERROR' }
  | { type: 'REFRESH' }
  | { type: 'RETRY' };

export interface CreateRevealTokenMachineOptions<TStatus, TToken> {
  id?: string;
  tokenType: RevealTokenType;
  services: RevealTokenServices<TStatus, TToken>;
}

function extractMessage(payload: unknown, fallback: string): string {
  if (payload instanceof Error) {
    return payload.message || fallback;
  }
  if (typeof payload === 'string' && payload) {
    return payload;
  }
  return fallback;
}

/**
 * Shared state machine for the UserToken and NuGetApiKey pages.
 *
 * The two pages differ only in endpoints, whether they generate tokens, and whether
 * they have a status endpoint. All of that is injected via `services` and the machine
 * treats the status + token values as opaque payloads. Hooks handle toast wiring,
 * modals, and page-state derivation.
 */
export function createRevealTokenMachine<TStatus, TToken>(
  options: CreateRevealTokenMachineOptions<TStatus, TToken>
) {
  const { id = 'reveal-token', tokenType, services } = options;
  const hasStatus = Boolean(services.checkTokenStatus);
  const hasGenerate = Boolean(services.generateToken);

  return createMachine<RevealTokenContext<TStatus, TToken>, RevealTokenEvent>(
    {
      id,
      predictableActionArguments: true,
      initial: hasStatus ? 'loading' : 'idle',
      context: {
        tokenType,
        status: null,
        tokenValue: null,
        loadError: null,
        actionError: null,
      },
      states: {
        ...(hasStatus && {
          loading: {
            entry: 'clearLoadError',
            invoke: {
              src: 'checkTokenStatus',
              onDone: {
                target: 'idle',
                actions: 'setStatus',
              },
              onError: {
                target: 'loadError',
                actions: 'setLoadError',
              },
            },
          },
          loadError: {
            on: {
              RETRY: 'loading',
            },
          },
        }),

        idle: {
          on: {
            REVEAL: 'revealing',
            ...(hasGenerate && { GENERATE: 'generating' }),
            DELETE: 'deleting',
            ...(hasStatus && { REFRESH: 'loading' }),
            CLEAR_ERROR: {
              actions: 'clearActionError',
            },
          },
        },

        revealing: {
          entry: ['clearActionError', 'clearTokenValue'],
          invoke: {
            src: 'fetchToken',
            onDone: {
              target: 'revealed',
              actions: 'setTokenValue',
            },
            onError: {
              target: 'idle',
              actions: 'setActionError',
            },
          },
        },

        revealed: {
          on: {
            HIDE: {
              target: 'idle',
              actions: 'clearTokenValue',
            },
            DELETE: 'deleting',
          },
        },

        ...(hasGenerate && {
          generating: {
            entry: ['clearActionError', 'clearTokenValue'],
            invoke: {
              src: 'generateToken',
              onDone: {
                target: hasStatus ? 'reloadingAfterGenerate' : 'revealed',
                actions: 'setTokenValue',
              },
              onError: {
                target: 'idle',
                actions: 'setActionError',
              },
            },
          },
        }),

        ...(hasStatus && hasGenerate && {
          // Refresh status after successful generate so the idle-state UI reflects the
          // new token, then continue to revealed so the reveal modal opens with the
          // newly-generated value.
          reloadingAfterGenerate: {
            invoke: {
              src: 'checkTokenStatus',
              onDone: {
                target: 'revealed',
                actions: 'setStatus',
              },
              onError: {
                target: 'revealed',
                actions: 'setLoadError',
              },
            },
          },
        }),

        deleting: {
          entry: 'clearActionError',
          invoke: {
            src: 'deleteToken',
            onDone: {
              target: hasStatus ? 'loading' : 'idle',
              actions: 'onDeleteSuccess',
            },
            onError: {
              target: 'idle',
              actions: 'setActionError',
            },
          },
        },
      },
    },
    {
      actions: {
        clearLoadError: assign({ loadError: (_ctx) => null }),
        setLoadError: assign({
          loadError: (_ctx, event) => {
            const invokeEvent = event as unknown as { data: unknown };
            return extractMessage(invokeEvent.data, 'Failed to load token status.');
          },
        }),
        setStatus: assign({
          status: (_ctx, event) => {
            const invokeEvent = event as unknown as { data: TStatus };
            return invokeEvent.data;
          },
        }),
        setTokenValue: assign({
          tokenValue: (_ctx, event) => {
            const invokeEvent = event as unknown as { data: TToken };
            return invokeEvent.data;
          },
        }),
        clearTokenValue: assign({ tokenValue: (_ctx) => null }),
        clearActionError: assign({ actionError: (_ctx) => null }),
        setActionError: assign({
          actionError: (_ctx, event) => {
            const invokeEvent = event as unknown as { data: unknown };
            return extractMessage(invokeEvent.data, 'Action failed.');
          },
        }),
        onDeleteSuccess: assign({
          tokenValue: (_ctx) => null,
          status: (_ctx) => null,
        }),
      },
      services: {
        checkTokenStatus: async () => {
          if (!services.checkTokenStatus) {
            throw new Error('checkTokenStatus is not configured');
          }
          return services.checkTokenStatus();
        },
        fetchToken: async () => services.fetchToken(),
        generateToken: async () => {
          if (!services.generateToken) {
            throw new Error('generateToken is not configured');
          }
          return services.generateToken();
        },
        deleteToken: async () => services.deleteToken(),
      },
    }
  );
}
