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

import { ReactNode } from 'react';
import { assign, createMachine } from 'xstate';
import type { CreateTokenForm, RoleOption, ServiceAccountToken } from './types';

export interface CreatedTokenResult {
  token: ServiceAccountToken;
  rawToken: string;
}

export interface ServiceAccountTokensLoadResult {
  tokens: ServiceAccountToken[];
  roles: RoleOption[];
  rolesError: ReactNode | null;
}

export interface ServiceAccountTokensServices {
  loadAll: () => Promise<ServiceAccountTokensLoadResult>;
  createToken: (form: CreateTokenForm) => Promise<CreatedTokenResult>;
  revokeToken: (tokenId: string) => Promise<void>;
}

export interface ServiceAccountTokensContext {
  tokens: ServiceAccountToken[];
  roles: RoleOption[];
  rolesError: ReactNode | null;
  loadError: string | null;
  createError: string | null;
  revokeError: string | null;
  pendingCreate: CreateTokenForm | null;
  pendingRevokeId: string | null;
  /**
   * Result of the most-recent successful createToken invocation. The hook resolves its
   * awaited `createToken(...)` promise from this value.
   */
  lastCreated: CreatedTokenResult | null;
  /**
   * Increments each time a create or revoke command is dispatched. Used by the hook to
   * pair a specific SUBMIT_CREATE / REVOKE call with the state transition that reports
   * its outcome, so concurrent commands can't cross-resolve each other's promises.
   */
  commandId: number;
}

export type ServiceAccountTokensEvent =
  | { type: 'REFRESH' }
  | { type: 'RETRY' }
  | { type: 'SUBMIT_CREATE'; form: CreateTokenForm; commandId: number }
  | { type: 'REVOKE'; tokenId: string; commandId: number }
  | { type: 'CLEAR_ERROR' };

const INITIAL_CONTEXT: ServiceAccountTokensContext = {
  tokens: [],
  roles: [],
  rolesError: null,
  loadError: null,
  createError: null,
  revokeError: null,
  pendingCreate: null,
  pendingRevokeId: null,
  lastCreated: null,
  commandId: 0,
};

/**
 * Machine for the Service Account Tokens page. Handles list load, create submit, revoke,
 * and their error recovery paths as explicit states. The hook layer wraps SUBMIT_CREATE
 * and REVOKE with promise-based helpers so the page can `await createToken(...)` and
 * chain a toast + reveal after success — matching the previous hook's surface.
 */
export function createServiceAccountTokensMachine(services: ServiceAccountTokensServices) {
  return createMachine<ServiceAccountTokensContext, ServiceAccountTokensEvent>(
    {
      id: 'service-account-tokens',
      predictableActionArguments: true,
      initial: 'loading',
      context: INITIAL_CONTEXT,
      states: {
        loading: {
          entry: 'clearLoadError',
          invoke: {
            src: 'loadAll',
            onDone: {
              target: 'idle',
              actions: 'setLoaded',
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
            REFRESH: 'loading',
            CLEAR_ERROR: {
              actions: 'clearLoadError',
            },
          },
        },
        idle: {
          on: {
            REFRESH: 'loading',
            SUBMIT_CREATE: {
              target: 'submittingCreate',
              actions: ['setPendingCreate', 'bumpCommandId'],
            },
            REVOKE: {
              target: 'revoking',
              actions: ['setPendingRevoke', 'bumpCommandId'],
            },
            CLEAR_ERROR: {
              actions: ['clearLoadError', 'clearCreateError', 'clearRevokeError'],
            },
          },
        },
        submittingCreate: {
          entry: 'clearCreateError',
          invoke: {
            src: 'createToken',
            onDone: {
              target: 'loading',
              actions: 'setLastCreated',
            },
            onError: {
              target: 'idle',
              actions: 'setCreateError',
            },
          },
        },
        revoking: {
          entry: 'clearRevokeError',
          invoke: {
            src: 'revokeToken',
            onDone: {
              target: 'loading',
              actions: 'clearPendingRevoke',
            },
            onError: {
              target: 'idle',
              actions: 'setRevokeError',
            },
          },
        },
      },
    },
    {
      actions: {
        setLoaded: assign((_ctx, event) => {
          const invoke = event as unknown as { data: ServiceAccountTokensLoadResult };
          return {
            tokens: invoke.data.tokens,
            roles: invoke.data.roles,
            rolesError: invoke.data.rolesError,
            loadError: null,
          };
        }),
        setLoadError: assign({
          loadError: (_ctx, event) => {
            const invoke = event as unknown as { data: unknown };
            if (invoke.data instanceof Error) return invoke.data.message;
            if (typeof invoke.data === 'string') return invoke.data;
            return 'Failed to load service-account tokens.';
          },
        }),
        clearLoadError: assign({ loadError: (_ctx) => null }),

        bumpCommandId: assign({ commandId: (ctx) => ctx.commandId + 1 }),

        setPendingCreate: assign({
          pendingCreate: (_ctx, event) => {
            const submitEvent = event as { type: 'SUBMIT_CREATE'; form: CreateTokenForm };
            return submitEvent.form;
          },
          lastCreated: (_ctx) => null,
        }),
        setLastCreated: assign((_ctx, event) => {
          const invoke = event as unknown as { data: CreatedTokenResult };
          return {
            lastCreated: invoke.data,
            pendingCreate: null,
            createError: null,
          };
        }),
        setCreateError: assign({
          createError: (_ctx, event) => {
            const invoke = event as unknown as { data: unknown };
            if (invoke.data instanceof Error) return invoke.data.message;
            if (typeof invoke.data === 'string') return invoke.data;
            return 'Failed to create service-account token.';
          },
          pendingCreate: (_ctx) => null,
        }),
        clearCreateError: assign({ createError: (_ctx) => null }),

        setPendingRevoke: assign({
          pendingRevokeId: (_ctx, event) => {
            const revokeEvent = event as { type: 'REVOKE'; tokenId: string };
            return revokeEvent.tokenId;
          },
        }),
        clearPendingRevoke: assign({ pendingRevokeId: (_ctx) => null }),
        setRevokeError: assign({
          revokeError: (_ctx, event) => {
            const invoke = event as unknown as { data: unknown };
            if (invoke.data instanceof Error) return invoke.data.message;
            if (typeof invoke.data === 'string') return invoke.data;
            return 'Failed to revoke service-account token.';
          },
          pendingRevokeId: (_ctx) => null,
        }),
        clearRevokeError: assign({ revokeError: (_ctx) => null }),
      },
      services: {
        loadAll: async () => services.loadAll(),
        createToken: async (ctx) => {
          if (!ctx.pendingCreate) {
            throw new Error('No pending create form');
          }
          return services.createToken(ctx.pendingCreate);
        },
        revokeToken: async (ctx) => {
          if (!ctx.pendingRevokeId) {
            throw new Error('No pending revoke id');
          }
          return services.revokeToken(ctx.pendingRevokeId);
        },
      },
    }
  );
}
