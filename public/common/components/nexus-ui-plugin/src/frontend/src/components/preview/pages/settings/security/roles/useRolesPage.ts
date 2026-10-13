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

import { useCallback, useEffect, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';
import { useToast } from '../../../../shared';
import { useRolesApi } from './useRolesApi';
import type { Role } from './types';
import {
  BASE_PATH,
  createRolesPageMachine,
  parseRoute,
  type RouteState,
  type RoleLoadErrorType,
} from './rolesPageMachine';

// Re-export helpers used by RolesPage view for navigation labels/paths.
export { BASE_PATH, parseRoute };
export type { RouteState, RoleLoadErrorType };

export function navigateTo(path: string): void {
  window.location.hash = path;
}

// ViewModel (thin projection) over rolesPageMachine. Holds no state of its own; subscribes
// to hashchange, translates it into ROUTE_CHANGE events, and projects state.matches(...) /
// state.context into a clean API for the RolesPage view.

export interface UseRolesPageResult {
  // Route
  routeState: RouteState;

  // Detail state
  role: Role | null;
  loading: boolean;
  error: string | null;
  roleLoadErrorType: RoleLoadErrorType | null;
  refreshKey: number;

  // Permissions
  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  roleCanEdit: boolean;
  roleCanDelete: boolean;

  // Delete dialog
  deleteDialogOpen: boolean;
  setDeleteDialogOpen: (open: boolean) => void;

  // Actions
  setError: (msg: string | null) => void;
  handleCreate: () => void;
  handleBack: () => void;
  handleComplete: () => void;
  handleDelete: () => void;
  handleDeleteConfirm: () => void;
  retryRoleLoad: () => void;
}

export function useRolesPage(): UseRolesPageResult {
  const toast = useToast();
  const { loading: apiLoading, error: apiError, setError: setApiError, findRole, deleteRole } = useRolesApi();

  // Semantic-command callbacks handed to the machine so it can trigger side effects
  // without importing React (kept out of the Model per architecture.md line 121).
  const onDeleted = useCallback((roleName: string) => {
    toast.success(`Role "${roleName}" deleted successfully`);
    navigateTo(BASE_PATH);
  }, [toast]);

  const onNavigateAway = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  const machine = useMemo(
    () =>
      createRolesPageMachine({
        initialRouteState: parseRoute(window.location.hash),
        findRole,
        deleteRole,
        onDeleted,
        onNavigateAway,
      }),
    // findRole/deleteRole are useCallback([]) inside useRolesApi so they are stable;
    // onDeleted/onNavigateAway are stable via useCallback here. The machine is created
    // once for the lifetime of the page.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  const [state, send] = useMachine(machine);

  // Bridge browser hashchange events into machine events. This is the only effect the
  // ViewModel needs: it translates DOM signals into machine commands (per architecture.md
  // line 133 "Provide navigation callbacks" - ViewModel responsibility).
  useEffect(() => {
    const handleHashChange = () => {
      send({ type: 'ROUTE_CHANGE', routeState: parseRoute(window.location.hash) });
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [send]);

  // Reactive permission checks (NEXUS-54212 pattern). ExtJS.usePermission with a hasUser
  // dependency re-evaluates once the user and their permissions arrive.
  const hasUser = ExtJS.useUser() ?? false;
  const canCreate = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.ROLES.CREATE),
    [hasUser],
  );
  const canUpdate = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.ROLES.UPDATE),
    [hasUser],
  );
  const canDelete = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.ROLES.DELETE),
    [hasUser],
  );

  // Semantic commands (view calls these; hook wraps machine sends)
  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  const handleComplete = useCallback(() => {
    send({ type: 'INCREMENT_REFRESH' });
    navigateTo(BASE_PATH);
  }, [send]);

  const handleDelete = useCallback(() => send({ type: 'OPEN_DELETE_DIALOG' }), [send]);
  const handleDeleteConfirm = useCallback(() => send({ type: 'CONFIRM_DELETE' }), [send]);
  const setDeleteDialogOpen = useCallback(
    (open: boolean) => send({ type: open ? 'OPEN_DELETE_DIALOG' : 'CANCEL_DELETE' }),
    [send],
  );
  const retryRoleLoad = useCallback(() => send({ type: 'RETRY' }), [send]);
  const setError = useCallback((msg: string | null) => setApiError(msg), [setApiError]);

  const { role, errorMessage, errorType, refreshKey, deleteDialogOpen, routeState } =
    state.context;

  // Projected loading: true whenever the machine is in a fetch/delete state or when the
  // underlying useRolesApi flag reports an ambient operation (kept for backward-compat
  // with the previous hook's `loading` semantics).
  const detailLoading =
    state.matches({ detailView: 'loading' }) || state.matches({ detailView: 'deleting' });
  const loading = detailLoading || apiLoading;

  const roleCanEdit = !!(canUpdate && role && !role.readOnly);
  const roleCanDelete = !!(canDelete && role && !role.readOnly);

  // Compose page-level error surface. Priority: transient-role-load error > any API error.
  const error = errorMessage ?? apiError ?? null;

  return {
    routeState,
    role,
    loading,
    error,
    roleLoadErrorType: errorType,
    refreshKey,
    canCreate,
    canUpdate,
    canDelete,
    roleCanEdit,
    roleCanDelete,
    deleteDialogOpen,
    setDeleteDialogOpen,
    setError,
    handleCreate,
    handleBack,
    handleComplete,
    handleDelete,
    handleDeleteConfirm,
    retryRoleLoad,
  };
}
