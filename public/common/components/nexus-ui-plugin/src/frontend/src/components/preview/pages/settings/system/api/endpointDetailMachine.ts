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

export type EndpointDetailTab = 'try' | 'who' | 'grant';

export interface EndpointDetailContext {
  hasSecurityDirectoryRead: boolean;
  hasGrantAccess: boolean;
}

export type EndpointDetailEvent =
  | { type: 'SELECT_TAB'; tab: EndpointDetailTab }
  | { type: 'PERMISSIONS_UPDATED'; hasSecurityDirectoryRead: boolean; hasGrantAccess: boolean };

export interface CreateEndpointDetailMachineOptions {
  hasSecurityDirectoryRead: boolean;
  hasGrantAccess: boolean;
}

/**
 * Presentation-state machine for {@link ../EndpointDetail}. The active tab lives here so
 * the view can stay purely declarative; SELECT_TAB is guarded so callers cannot switch to
 * a hidden tab, and PERMISSIONS_UPDATED re-evaluates the current state so a revoked
 * permission bounces the user back to `try` without a race between render and effect.
 */
export function createEndpointDetailMachine(options: CreateEndpointDetailMachineOptions) {
  return createMachine<EndpointDetailContext, EndpointDetailEvent>(
    {
      id: 'endpoint-detail',
      predictableActionArguments: true,
      initial: 'try',
      context: {
        hasSecurityDirectoryRead: options.hasSecurityDirectoryRead,
        hasGrantAccess: options.hasGrantAccess,
      },
      on: {
        PERMISSIONS_UPDATED: { actions: 'assignPermissions' },
      },
      states: {
        try: {
          on: {
            SELECT_TAB: [
              { target: 'who', cond: 'canSelectWho' },
              { target: 'grant', cond: 'canSelectGrant' },
            ],
          },
        },
        who: {
          always: [{ target: 'try', cond: 'lostSecurityRead' }],
          on: {
            SELECT_TAB: [
              { target: 'try', cond: 'selectingTry' },
              { target: 'grant', cond: 'canSelectGrant' },
            ],
          },
        },
        grant: {
          always: [{ target: 'try', cond: 'lostGrantAccess' }],
          on: {
            SELECT_TAB: [
              { target: 'try', cond: 'selectingTry' },
              { target: 'who', cond: 'canSelectWho' },
            ],
          },
        },
      },
    },
    {
      actions: {
        assignPermissions: assign((_ctx, event) => {
          const e = event as Extract<EndpointDetailEvent, { type: 'PERMISSIONS_UPDATED' }>;
          return {
            hasSecurityDirectoryRead: e.hasSecurityDirectoryRead,
            hasGrantAccess: e.hasGrantAccess,
          };
        }),
      },
      guards: {
        selectingTry: (_ctx, event) =>
          (event as Extract<EndpointDetailEvent, { type: 'SELECT_TAB' }>).tab === 'try',
        canSelectWho: (ctx, event) =>
          (event as Extract<EndpointDetailEvent, { type: 'SELECT_TAB' }>).tab === 'who' &&
          ctx.hasSecurityDirectoryRead,
        canSelectGrant: (ctx, event) =>
          (event as Extract<EndpointDetailEvent, { type: 'SELECT_TAB' }>).tab === 'grant' &&
          ctx.hasGrantAccess,
        lostSecurityRead: (ctx) => !ctx.hasSecurityDirectoryRead,
        lostGrantAccess: (ctx) => !ctx.hasGrantAccess,
      },
    }
  );
}
