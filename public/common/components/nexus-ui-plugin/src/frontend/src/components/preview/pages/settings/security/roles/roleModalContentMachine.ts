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
import type { Role } from './types';

// Model for the SecurityEntityModalContext role drill-down. Owns the fetch lifecycle and
// error classification so the RoleModalContent view + useRoleModalContent hook can stay
// purely presentational and projection-only (per architecture.md line 44, 54, 260).
//
// findRole is injected as a machine option because the machine cannot import React hooks;
// the hook wires in the real useRolesApi().findRole at construction time.

export type RoleModalContentContext = {
  roleName: string;
  role: Role | null;
  error: string | null;
};

export type RoleModalContentEvent = { type: 'RETRY' };

export type RoleModalContentState =
  | 'loading'
  | 'loaded'
  | 'notFound'
  | 'error';

export interface RoleModalContentMachineOptions {
  roleName: string;
  findRole: (roleId: string) => Promise<Role | null>;
}

export function createRoleModalContentMachine(options: RoleModalContentMachineOptions) {
  const { roleName, findRole } = options;

  return createMachine<RoleModalContentContext, RoleModalContentEvent>(
    {
      id: 'roleModalContent',
      initial: 'loading',
      context: {
        roleName,
        role: null,
        error: null,
      },
      states: {
        loading: {
          entry: 'clearRole',
          invoke: {
            src: 'fetchRole',
            onDone: [
              { target: 'notFound', cond: 'isNotFound' },
              { target: 'loaded', actions: 'assignRole' },
            ],
            onError: {
              target: 'error',
              actions: 'assignError',
            },
          },
        },
        loaded: {},
        notFound: {
          entry: 'assignNotFoundMessage',
        },
        error: {
          on: { RETRY: 'loading' },
        },
      },
    },
    {
      services: {
        fetchRole: (context) => findRole(context.roleName),
      },
      guards: {
        isNotFound: (_context, event) => event.data === null,
      },
      actions: {
        clearRole: assign<RoleModalContentContext, RoleModalContentEvent>({
          role: (_context) => null,
          error: (_context) => null,
        }),
        assignRole: assign<RoleModalContentContext, RoleModalContentEvent>({
          role: (_context, event) => (event as { data: Role }).data,
          error: (_context) => null,
        }),
        assignNotFoundMessage: assign<RoleModalContentContext, RoleModalContentEvent>({
          role: (_context) => null,
          error: (_context) => 'Role not found',
        }),
        assignError: assign<RoleModalContentContext, RoleModalContentEvent>({
          role: (_context) => null,
          error: (_context, event) => {
            const err = (event as { data?: unknown }).data;
            const message = (err as { message?: string } | null | undefined)?.message;
            // Log with the original error preserved for production debugging.
            console.error('Failed to load role:', err);
            return message ?? 'Failed to load role';
          },
        }),
      },
    },
  );
}
