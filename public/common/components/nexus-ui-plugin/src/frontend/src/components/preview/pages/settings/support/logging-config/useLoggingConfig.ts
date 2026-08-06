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

import { useCallback, useEffect, useRef } from 'react';
import { useMachine } from '@xstate/react';
import { useToast } from '../../../../shared';
import { loggingConfigMachine } from './LoggingConfigMachine';

export type ViewMode = 'list' | 'create' | 'detail';

export interface UseLoggingConfigReturn {
  viewMode: ViewMode;
  selectedLogger: string | null;
  deleteDialogOpen: boolean;
  resetAllDialogOpen: boolean;
  isDeleting: boolean;
  isResettingAll: boolean;
  error: string | null;
  refreshKey: number;
  handleSelectLogger: (name: string) => void;
  handleCreate: () => void;
  handleBack: () => void;
  handleSave: () => void;
  handleDeleteClick: () => void;
  handleDeleteConfirm: () => void;
  handleCancelDelete: () => void;
  handleResetAll: () => void;
  handleResetAllConfirm: () => void;
  handleCancelResetAll: () => void;
  clearError: () => void;
}

export function useLoggingConfig(): UseLoggingConfigReturn {
  const toast = useToast();
  const [state, send] = useMachine(loggingConfigMachine);

  const prevStateRef = useRef(state);

  // Show toast notifications on successful delete/reset operations.
  // `toast` is in deps so a reference change is picked up; extra runs are inert because
  // the prevState/state comparison only emits toasts on the specific transitions below.
  useEffect(() => {
    const prevState = prevStateRef.current;

    if (prevState.matches('deleting') && state.matches('list')) {
      // Read selectedLogger from prevState.context — by the time we reach 'list', the machine's
      // clearSelectedLogger action has already set it to null in the current state.
      const loggerName = prevState.context.selectedLogger;
      toast.success(`Logger override removed for "${loggerName}"`);
    }

    if (prevState.matches('resettingAll') && state.matches('list')) {
      toast.success('All loggers reset to default levels');
    }

    prevStateRef.current = state;
  }, [state, toast]);

  // Derive view mode from machine state
  const viewMode: ViewMode = state.matches('creating')
    ? 'create'
    : state.matches('editing') || state.matches('confirmDelete') || state.matches('deleting')
    ? 'detail'
    : 'list';

  const deleteDialogOpen = state.matches('confirmDelete') || state.matches('deleting');
  const resetAllDialogOpen = state.matches('confirmResetAll') || state.matches('resettingAll');
  const isDeleting = state.matches('deleting');
  const isResettingAll = state.matches('resettingAll');

  const handleSelectLogger = useCallback(
    (name: string) => {
      send({ type: 'SELECT', name });
    },
    [send]
  );

  const handleCreate = useCallback(() => {
    send({ type: 'CREATE' });
  }, [send]);

  const handleBack = useCallback(() => {
    send({ type: 'BACK' });
  }, [send]);

  const handleSave = useCallback(() => {
    send({ type: 'SAVE' });
  }, [send]);

  const handleDeleteClick = useCallback(() => {
    send({ type: 'DELETE_CLICK' });
  }, [send]);

  const handleDeleteConfirm = useCallback(() => {
    send({ type: 'CONFIRM_DELETE' });
  }, [send]);

  const handleCancelDelete = useCallback(() => {
    send({ type: 'CANCEL_DELETE' });
  }, [send]);

  const handleResetAll = useCallback(() => {
    send({ type: 'RESET_ALL_CLICK' });
  }, [send]);

  const handleResetAllConfirm = useCallback(() => {
    send({ type: 'CONFIRM_RESET_ALL' });
  }, [send]);

  const handleCancelResetAll = useCallback(() => {
    send({ type: 'CANCEL_RESET_ALL' });
  }, [send]);

  const clearError = useCallback(() => {
    send({ type: 'CLEAR_ERROR' });
  }, [send]);

  return {
    viewMode,
    selectedLogger: state.context.selectedLogger,
    deleteDialogOpen,
    resetAllDialogOpen,
    isDeleting,
    isResettingAll,
    error: state.context.error,
    refreshKey: state.context.refreshKey,
    handleSelectLogger,
    handleCreate,
    handleBack,
    handleSave,
    handleDeleteClick,
    handleDeleteConfirm,
    handleCancelDelete,
    handleResetAll,
    handleResetAllConfirm,
    handleCancelResetAll,
    clearError,
  };
}
