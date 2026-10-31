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

import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import { useToast } from '../../../../shared';
import { loggerFormMachine } from './LoggerFormMachine';
import { LogLevel } from './types';

interface UseLoggerFormOptions {
  loggerName?: string | null;
  isCreate?: boolean;
  onSave: () => void;
  onCancel: () => void;
}

export interface UseLoggerFormReturn {
  name: string;
  level: LogLevel;
  isDirty: boolean;
  isLoading: boolean;
  isSaving: boolean;
  error: string | null;
  setName: (value: string) => void;
  setLevel: (value: string) => void;
  handleSubmit: (e?: React.FormEvent) => void;
}

export function useLoggerForm({
  loggerName,
  isCreate = false,
  onSave,
  onCancel,
}: UseLoggerFormOptions): UseLoggerFormReturn {
  const toast = useToast();

  const machine = useMemo(
    () =>
      loggerFormMachine.withContext({
        ...loggerFormMachine.context,
        isCreate,
        loggerName: loggerName ?? '',
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isCreate, loggerName]
  );

  const [state, send] = useMachine(machine);
  const { name, level, originalLevel, error } = state.context;

  const isLoading = state.matches('loading');
  const isSaving = state.matches('saving');

  const isDirty = isCreate ? name.trim().length > 0 : level !== originalLevel;

  // Latest-value refs: the success effect must fire exactly once when the machine transitions
  // to the 'success' final state, but must read the current onSave/isCreate/toast values.
  // Adding these to the effect's dep array would cause onSave() to fire again if the parent
  // re-renders while the machine is already in 'success' (a final state that never leaves).
  const onSaveRef = useRef(onSave);
  const isCreateRef = useRef(isCreate);
  const toastRef = useRef(toast);
  useEffect(() => {
    onSaveRef.current = onSave;
    isCreateRef.current = isCreate;
    toastRef.current = toast;
  });

  // When machine reaches success, call onSave with toast notification.
  useEffect(() => {
    if (state.matches('success')) {
      const { name: ctxName, level: ctxLevel } = state.context;
      const successMessage = isCreateRef.current
        ? `Logger "${ctxName}" created successfully`
        : `Logger "${ctxName}" updated to ${ctxLevel}`;
      toastRef.current.success(successMessage);
      onSaveRef.current();
    }
  }, [state.value]); // eslint-disable-line react-hooks/exhaustive-deps

  const setName = useCallback(
    (value: string) => {
      send({ type: 'SET_NAME', value });
    },
    [send]
  );

  const setLevel = useCallback(
    (value: string) => {
      send({ type: 'SET_LEVEL', value: value as LogLevel });
    },
    [send]
  );

  const handleSubmit = useCallback(
    (e?: React.FormEvent) => {
      if (e) e.preventDefault();
      if (!isDirty) return;
      send({ type: 'SUBMIT' });
    },
    [isDirty, send]
  );

  return {
    name,
    level,
    isDirty,
    isLoading,
    isSaving,
    error,
    setName,
    setLevel,
    handleSubmit,
  };
}
