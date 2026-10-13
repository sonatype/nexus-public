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

// Model for the Preview UI Roles page. Owns route-derived view mode, role fetch lifecycle,
// error classification, retry, wizard step, form validity, delete dialog, and refresh key.
// The ViewModel hook (useRolesPage) translates hashchange events into ROUTE_CHANGE events
// and projects state.context / state.matches(...) into a clean API for the view.
//
// Injected services / callbacks so the machine stays React-free (see architecture.md line 121):
//   - findRole: async fetcher for role details by id
//   - deleteRole: async mutation to delete a role by id
//   - onDeleted: side effect (toast + navigation) invoked when a delete succeeds
//   - onNavigateAway: side effect invoked when the machine enters notFound or forbidden

export const BASE_PATH = 'preview/admin/security/roles';

export type ViewMode = 'list' | 'create' | 'detail';

export interface RouteState {
  viewMode: ViewMode;
  roleId: string | null;
}

export type RoleLoadErrorType = 'notFound' | 'forbidden' | 'transient';

export interface RolesPageContext {
  routeState: RouteState;
  role: Role | null;
  errorMessage: string | null;
  errorType: RoleLoadErrorType | null;
  refreshKey: number;
  deleteDialogOpen: boolean;
}

export type RolesPageEvent =
  | { type: 'ROUTE_CHANGE'; routeState: RouteState }
  | { type: 'RETRY' }
  | { type: 'OPEN_DELETE_DIALOG' }
  | { type: 'CANCEL_DELETE' }
  | { type: 'CONFIRM_DELETE' }
  | { type: 'INCREMENT_REFRESH' };

export interface RolesPageMachineOptions {
  initialRouteState: RouteState;
  findRole: (roleId: string) => Promise<Role | null>;
  deleteRole: (roleId: string) => Promise<void>;
  onDeleted: (roleName: string) => void;
  onNavigateAway: () => void;
}

// Public helper so the hook can synchronously derive the initial route from window.location.hash
// (parseRoute is deterministic; both hook and machine can call it independently).
export function parseRoute(hash: string): RouteState {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');
  const roleIndex = parts.indexOf('roles');

  if (roleIndex === -1) return { viewMode: 'list', roleId: null };

  const pathAfterRoles = parts.slice(roleIndex + 1);
  if (pathAfterRoles.length === 0) return { viewMode: 'list', roleId: null };

  if (pathAfterRoles[0] === 'create') {
    return { viewMode: 'create', roleId: null };
  }

  return { viewMode: 'detail', roleId: decodeURIComponent(pathAfterRoles[0]) };
}

function initialTopStateFor(routeState: RouteState): 'listView' | 'createView' | 'detailView' {
  if (routeState.viewMode === 'create') return 'createView';
  if (routeState.viewMode === 'detail' && routeState.roleId) return 'detailView';
  return 'listView';
}

export function createRolesPageMachine(options: RolesPageMachineOptions) {
  const { initialRouteState, findRole, deleteRole, onDeleted, onNavigateAway } = options;

  return createMachine<RolesPageContext, RolesPageEvent>(
    {
      id: 'rolesPage',
      initial: initialTopStateFor(initialRouteState),
      context: {
        routeState: initialRouteState,
        role: null,
        errorMessage: null,
        errorType: null,
        refreshKey: 0,
        deleteDialogOpen: false,
      },
      on: {
        ROUTE_CHANGE: [
          {
            target: '.listView',
            cond: 'isListRoute',
            actions: 'assignRouteAndReset',
          },
          {
            target: '.createView',
            cond: 'isCreateRoute',
            actions: 'assignRouteAndReset',
          },
          {
            target: '.detailView',
            actions: 'assignRouteAndReset',
          },
        ],
        OPEN_DELETE_DIALOG: { actions: 'openDeleteDialog' },
        CANCEL_DELETE: { actions: 'closeDeleteDialog' },
        INCREMENT_REFRESH: { actions: 'incrementRefreshKey' },
      },
      states: {
        listView: {
          entry: 'clearDetailContext',
        },
        createView: {
          entry: 'clearDetailContext',
        },
        detailView: {
          initial: 'loading',
          states: {
            loading: {
              entry: 'clearRoleAndError',
              invoke: {
                src: 'fetchRole',
                onDone: [
                  { target: 'notFound', cond: 'isNotFound' },
                  { target: 'loaded', actions: 'assignRole' },
                ],
                onError: [
                  { target: 'forbidden', cond: 'isForbidden' },
                  { target: 'transientError', actions: 'assignTransientError' },
                ],
              },
            },
            loaded: {
              on: {
                CONFIRM_DELETE: 'deleting',
              },
            },
            notFound: {
              entry: ['assignNotFoundError', 'triggerNavigateAway'],
            },
            forbidden: {
              entry: ['assignForbiddenError', 'triggerNavigateAway'],
            },
            transientError: {
              on: { RETRY: 'loading' },
            },
            deleting: {
              entry: 'closeDeleteDialog',
              invoke: {
                src: 'deleteRole',
                onDone: {
                  target: 'loaded',
                  actions: ['triggerDeletedNotification', 'incrementRefreshKey'],
                },
                onError: {
                  target: 'loaded',
                  actions: 'assignDeleteError',
                },
              },
            },
          },
        },
      },
    },
    {
      services: {
        fetchRole: (context) => findRole(context.routeState.roleId ?? ''),
        deleteRole: (context) => deleteRole(context.role?.id ?? ''),
      },
      guards: {
        isListRoute: (_context, event) =>
          (event as { routeState: RouteState }).routeState.viewMode === 'list',
        isCreateRoute: (_context, event) =>
          (event as { routeState: RouteState }).routeState.viewMode === 'create',
        isNotFound: (_context, event) => (event as { data: Role | null }).data === null,
        isForbidden: (_context, event) => {
          const err = (event as { data?: unknown }).data;
          return (err as { status?: number } | null | undefined)?.status === 403;
        },
      },
      actions: {
        assignRouteAndReset: assign<RolesPageContext, RolesPageEvent>({
          routeState: (_context, event) => (event as { routeState: RouteState }).routeState,
          errorMessage: (_context) => null,
          errorType: (_context) => null,
        }),
        clearDetailContext: assign<RolesPageContext, RolesPageEvent>({
          role: (_context) => null,
          errorMessage: (_context) => null,
          errorType: (_context) => null,
        }),
        clearRoleAndError: assign<RolesPageContext, RolesPageEvent>({
          role: (_context) => null,
          errorMessage: (_context) => null,
          errorType: (_context) => null,
        }),
        assignRole: assign<RolesPageContext, RolesPageEvent>({
          role: (_context, event) => (event as { data: Role }).data,
          errorMessage: (_context) => null,
          errorType: (_context) => null,
        }),
        assignNotFoundError: assign<RolesPageContext, RolesPageEvent>({
          errorMessage: (_context) => 'Role not found',
          errorType: (_context) => 'notFound',
        }),
        assignForbiddenError: assign<RolesPageContext, RolesPageEvent>({
          errorMessage: (_context) => 'You do not have permission to view this role',
          errorType: (_context) => 'forbidden',
        }),
        assignTransientError: assign<RolesPageContext, RolesPageEvent>({
          errorMessage: (_context, event) => {
            const err = (event as { data?: unknown }).data;
            return (
              (err as { message?: string } | null | undefined)?.message ?? 'Failed to load role'
            );
          },
          errorType: (_context) => 'transient',
        }),
        assignDeleteError: assign<RolesPageContext, RolesPageEvent>({
          errorMessage: (_context, event) => {
            const err = (event as { data?: unknown }).data;
            return (
              (err as { message?: string } | null | undefined)?.message ??
              'Failed to delete role'
            );
          },
        }),
        openDeleteDialog: assign<RolesPageContext, RolesPageEvent>({
          deleteDialogOpen: (_context) => true,
        }),
        closeDeleteDialog: assign<RolesPageContext, RolesPageEvent>({
          deleteDialogOpen: (_context) => false,
        }),
        incrementRefreshKey: assign<RolesPageContext, RolesPageEvent>({
          refreshKey: (context) => context.refreshKey + 1,
        }),
        triggerNavigateAway: () => {
          onNavigateAway();
        },
        triggerDeletedNotification: (context) => {
          onDeleted(context.role?.name ?? '');
        },
      },
    },
  );
}
