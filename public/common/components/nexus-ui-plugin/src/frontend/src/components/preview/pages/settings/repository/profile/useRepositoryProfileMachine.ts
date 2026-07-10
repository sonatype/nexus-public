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

import { useMemo, useCallback } from 'react';
import { useMachine } from '@xstate/react';

import { createRepositoryProfileMachine } from './repositoryProfileMachine';
import type { RepositoryProfileContext } from './repositoryProfileMachine';
import { ACTION_METADATA, type RepositoryAction } from './types';

// =============================================================================
// Types
// =============================================================================

export interface UseRepositoryProfileMachineReturn {
  // Repository data
  repository: RepositoryProfileContext['repository'];
  blobStore: RepositoryProfileContext['blobStore'];
  cleanupPolicies: RepositoryProfileContext['cleanupPolicies'];
  routingRule: RepositoryProfileContext['routingRule'];
  healthCheck: RepositoryProfileContext['healthCheck'];
  firewall: RepositoryProfileContext['firewall'];
  malwareCleanupSummary: RepositoryProfileContext['malwareCleanupSummary'];
  iqCapabilities: RepositoryProfileContext['iqCapabilities'];
  metrics: RepositoryProfileContext['metrics'];
  privileges: RepositoryProfileContext['privileges'];
  roles: RepositoryProfileContext['roles'];
  users: RepositoryProfileContext['users'];
  anonymousAccess: RepositoryProfileContext['anonymousAccess'];
  tasks: RepositoryProfileContext['tasks'];
  capabilities: RepositoryProfileContext['capabilities'];
  httpSettings: RepositoryProfileContext['httpSettings'];

  // Loading states
  loading: boolean;
  securityLoading: boolean;
  systemLoading: boolean;

  // Error states
  error: string | null;
  actionError: string | null;

  // Actions
  refresh: () => void;
  retry: () => void;

  // Action handlers
  handleInvalidateCache: () => void;
  handleRebuildIndex: () => void;
  handleToggleOnline: () => void;
  handleToggleHealthCheck: (enabled: boolean) => void;
  handleToggleInstanceHealthCheck: (enabled: boolean, useTrustStore: boolean) => void;

  // Confirmation dialog
  confirmAction: () => void;
  cancelAction: () => void;
  isConfirming: boolean;
  isExecuting: boolean;
  pendingAction: RepositoryAction | null;
  dialogTitle: string;
  dialogMessage: string;
  dialogConfirmLabel: string;
  dialogVariant: 'warning' | 'danger';

  // Execution state
  pendingActionData: {
    isProxy: boolean;
    isOnline: boolean;
    repositoryName: string;
  };
}

// =============================================================================
// Hook
// =============================================================================

/**
 * useRepositoryProfileMachine
 *
 * React hook that integrates the repository profile XState machine with components.
 * Provides:
 * - All repository data from machine context
 * - Loading and error states
 * - Action handlers for repository operations
 * - Confirmation dialog state derived from machine
 *
 * Loading phases are derived from the machine's parallel sub-states rather than
 * boolean flags in context, ensuring they reset correctly on REFRESH:
 *   securityLoading = matches({ loading: { security: 'fetching' } })
 *   systemLoading   = matches({ loading: { system: 'fetching' } })
 *
 * The pending action is derived from the current state name rather than a
 * context field, so it is always in sync with the machine state:
 *   confirmingInvalidateCache / executingInvalidateCache → 'invalidate-cache'
 *   confirmingRebuildIndex    / executingRebuildIndex    → 'rebuild-index'
 *   confirmingToggleOnline    / executingToggleOnline    → 'toggle-online'
 */
export function useRepositoryProfileMachine(repositoryName: string): UseRepositoryProfileMachineReturn {
  // Create machine instance
  const machine = useMemo(
    () => createRepositoryProfileMachine({ repositoryName }),
    [repositoryName]
  );

  const [state, send] = useMachine(machine);

  const { context, matches } = state;

  // ========================================
  // Loading State (derived from parallel sub-states)
  // ========================================

  const loading = matches('loading');

  // These use object syntax to match nested parallel state nodes (XState v4)
  const securityLoading = matches({ loading: { security: 'fetching' } });
  const systemLoading = matches({ loading: { system: 'fetching' } });

  // ========================================
  // Confirming / Executing State
  // ========================================

  const isConfirming =
    matches('confirmingInvalidateCache') ||
    matches('confirmingRebuildIndex') ||
    matches('confirmingToggleOnline');

  const isExecuting =
    matches('executingInvalidateCache') ||
    matches('executingRebuildIndex') ||
    matches('executingToggleOnline') ||
    matches('executingToggleHealthCheck') ||
    matches('executingToggleInstanceHealthCheck');

  // Derive pending action from state rather than context — always in sync
  const pendingAction: RepositoryAction | null =
    matches('confirmingInvalidateCache') || matches('executingInvalidateCache')
      ? 'invalidate-cache'
      : matches('confirmingRebuildIndex') || matches('executingRebuildIndex')
        ? 'rebuild-index'
        : matches('confirmingToggleOnline') || matches('executingToggleOnline')
          ? 'toggle-online'
          : null;

  // ========================================
  // Dialog Content (derived from pending action)
  // ========================================

  const actionMetadata = pendingAction ? ACTION_METADATA[pendingAction] : null;

  let dialogTitle = '';
  let dialogMessage = '';
  let dialogConfirmLabel = 'Continue';
  let dialogVariant: 'warning' | 'danger' = 'warning';

  if (actionMetadata) {
    dialogTitle = actionMetadata.title;
    dialogMessage = actionMetadata.message;
    dialogConfirmLabel = actionMetadata.confirmLabel;
    dialogVariant = actionMetadata.variant;

    // Customize toggle-online dialog copy based on current online state
    if (pendingAction === 'toggle-online' && context.repository) {
      const isOnline = context.repository.online;
      dialogTitle = isOnline ? 'Take Repository Offline' : 'Bring Repository Online';
      dialogMessage = isOnline
        ? `Taking "${repositoryName}" offline will prevent users from accessing this repository. Existing requests will fail. Continue?`
        : `Bringing "${repositoryName}" online will allow users to access this repository again. Continue?`;
      dialogConfirmLabel = isOnline ? 'Take Offline' : 'Bring Online';
    }
  }

  // ========================================
  // Action Handlers
  // ========================================

  const refresh = useCallback(() => {
    send({ type: 'REFRESH' });
  }, [send]);

  // retry is a semantic alias for refresh — both trigger re-entry into loading
  const retry = useCallback(() => {
    send({ type: 'REFRESH' });
  }, [send]);

  const handleInvalidateCache = useCallback(() => {
    send({ type: 'INVALIDATE_CACHE' });
  }, [send]);

  const handleRebuildIndex = useCallback(() => {
    send({ type: 'REBUILD_INDEX' });
  }, [send]);

  const handleToggleOnline = useCallback(() => {
    send({ type: 'TOGGLE_ONLINE' });
  }, [send]);

  const handleToggleHealthCheck = useCallback((enabled: boolean) => {
    send({ type: 'TOGGLE_HEALTH_CHECK', enabled });
  }, [send]);

  const handleToggleInstanceHealthCheck = useCallback((enabled: boolean, useTrustStore: boolean) => {
    send({ type: 'TOGGLE_INSTANCE_HEALTH_CHECK', enabled, useTrustStore });
  }, [send]);

  const confirmAction = useCallback(() => {
    send({ type: 'CONFIRM' });
  }, [send]);

  const cancelAction = useCallback(() => {
    send({ type: 'CANCEL' });
  }, [send]);

  // ========================================
  // Return
  // ========================================

  return {
    // Repository data
    repository: context.repository,
    blobStore: context.blobStore,
    cleanupPolicies: context.cleanupPolicies,
    routingRule: context.routingRule,
    healthCheck: context.healthCheck,
    firewall: context.firewall,
    malwareCleanupSummary: context.malwareCleanupSummary,
    iqCapabilities: context.iqCapabilities,
    metrics: context.metrics,
    privileges: context.privileges,
    roles: context.roles,
    users: context.users,
    anonymousAccess: context.anonymousAccess,
    tasks: context.tasks,
    capabilities: context.capabilities,
    httpSettings: context.httpSettings,

    // Loading states
    loading,
    securityLoading,
    systemLoading,

    // Error states
    error: context.loadError,
    actionError: context.actionError,

    // Actions
    refresh,
    retry,

    // Action handlers
    handleInvalidateCache,
    handleRebuildIndex,
    handleToggleOnline,
    handleToggleHealthCheck,
    handleToggleInstanceHealthCheck,

    // Confirmation dialog
    confirmAction,
    cancelAction,
    isConfirming,
    isExecuting,
    pendingAction,
    dialogTitle,
    dialogMessage,
    dialogConfirmLabel,
    dialogVariant,

    // Execution state
    pendingActionData: {
      isProxy: context.repository?.type === 'proxy',
      isOnline: context.repository?.online ?? true,
      repositoryName: context.repositoryName,
    },
  };
}

export default useRepositoryProfileMachine;
