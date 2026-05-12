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

import { useCallback, useEffect } from 'react';
import { useMachine } from '@xstate/react';
import { path } from 'ramda';
import type { StateMachine } from 'xstate';

import type {
  FormContext,
  FormEvent,
  UseFormReturn,
  FieldProps,
  CheckboxProps,
  SelectProps,
} from './types';
import { hasValidationErrors, toPathArray } from './utils';
import {
  useUnsavedChangesWarning,
  clearDirtyState,
} from '../../components/preview/shared/hooks/useUnsavedChangesWarning';

/**
 * React hook for using a form machine created by createFormMachine.
 *
 * Provides:
 * - Automatic dirty state tracking for navigation warnings (uses machine.id)
 * - Field helpers for binding to form inputs
 * - Status flags for loading, saving, etc.
 * - Actions for submit, reset, delete, cancel
 *
 * Accepts the same options as useMachine for action/guard/service overrides.
 *
 * @example
 * ```tsx
 * function UserForm({ user, onClose }) {
 *   const form = useForm(userFormMachine, {
 *     actions: { onCancel: onClose },
 *   });
 *
 *   if (form.isLoading) return <Spinner />;
 *
 *   return (
 *     <form onSubmit={(e) => { e.preventDefault(); form.submit(); }}>
 *       <TextInput {...form.field('name')} label="Name" />
 *       <button type="submit">Save</button>
 *       <button type="button" onClick={form.requestCancel}>Cancel</button>
 *     </form>
 *   );
 * }
 * ```
 */
export function useForm<TData>(
  machine: StateMachine<FormContext<TData>, any, FormEvent>,
  options?: Parameters<typeof useMachine>[1]
): UseFormReturn<TData> {
  // Use machine.id for dirty state tracking
  const formId = machine.id;

  const [state, send] = useMachine(machine, options);
  const { context } = state;

  // ============================================
  // Dirty State Management
  // ============================================

  // Auto-register for navigation warnings when form is dirty
  useUnsavedChangesWarning(!context.isPristine, formId);

  // Clear dirty state on successful save or delete
  useEffect(() => {
    if (state.matches('saved') || state.matches('deleted')) {
      clearDirtyState(formId);
    }
  }, [state.value, formId]);

  // Cleanup on unmount - only if pristine (router handles dirty case)
  useEffect(() => {
    return () => {
      // Component is unmounting - if we're pristine, ensure we're cleaned up
      // If dirty, the router will handle showing the warning and cleanup
      clearDirtyState(formId);
    };
  }, [formId]);

  // ============================================
  // Field Helpers
  // ============================================

  /**
   * Get props for a text input field
   */
  const field = useCallback(
    (name: string): FieldProps => {
      const pathArray = toPathArray(name);
      const value = path(pathArray, context.data);
      const touched = path(pathArray, context.touched);
      const error = context.validationErrors[name];

      return {
        name,
        value: value != null ? String(value) : '',
        error: touched ? error ?? undefined : undefined,
        onChange: (newValue: string) => send({ type: 'UPDATE', name, value: newValue }),
        onBlur: () => send({ type: 'BLUR', name }),
      };
    },
    [context.data, context.touched, context.validationErrors, send]
  );

  /**
   * Get props for a checkbox field
   */
  const checkbox = useCallback(
    (name: string): CheckboxProps => {
      const pathArray = toPathArray(name);
      const value = path(pathArray, context.data);
      const touched = path(pathArray, context.touched);
      const error = context.validationErrors[name];

      return {
        name,
        checked: Boolean(value),
        error: touched ? error ?? undefined : undefined,
        onChange: (checked: boolean) => send({ type: 'UPDATE', name, value: checked }),
      };
    },
    [context.data, context.touched, context.validationErrors, send]
  );

  /**
   * Get props for a select field
   */
  const select = useCallback(
    (name: string): SelectProps => {
      const pathArray = toPathArray(name);
      const value = path(pathArray, context.data);
      const touched = path(pathArray, context.touched);
      const error = context.validationErrors[name];

      return {
        name,
        value: value != null ? String(value) : '',
        error: touched ? error ?? undefined : undefined,
        onChange: (newValue: string) => send({ type: 'UPDATE', name, value: newValue }),
        onBlur: () => send({ type: 'BLUR', name }),
      };
    },
    [context.data, context.touched, context.validationErrors, send]
  );

  // ============================================
  // Actions
  // ============================================

  const submit = useCallback(() => {
    send({ type: 'SUBMIT' });
  }, [send]);

  const reset = useCallback(() => {
    send({ type: 'RESET' });
  }, [send]);

  const retry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  const requestDelete = useCallback(() => {
    send({ type: 'DELETE' });
  }, [send]);

  const confirmDelete = useCallback(() => {
    send({ type: 'CONFIRM_DELETE' });
  }, [send]);

  const cancelDelete = useCallback(() => {
    send({ type: 'CANCEL_DELETE' });
  }, [send]);

  const requestCancel = useCallback(() => {
    send({ type: 'CANCEL' });
  }, [send]);

  const confirmCancel = useCallback(() => {
    send({ type: 'CONFIRM_CANCEL' });
  }, [send]);

  const stay = useCallback(() => {
    send({ type: 'STAY' });
  }, [send]);

  // ============================================
  // Return
  // ============================================

  return {
    // State
    data: context.data,
    isPristine: context.isPristine,
    touched: context.touched,

    // Status flags
    isLoading: state.matches('loading'),
    isSaving: state.matches('saving'),
    isDeleting: state.matches('deleting'),
    hasLoadError: state.matches('loadError'),
    hasValidationErrors: hasValidationErrors(context.validationErrors),
    isConfirmingDelete: state.matches('confirmingDelete'),
    isConfirmingCancel: state.matches('confirmingCancel'),
    isCancelled: state.matches('cancelled'),
    isComplete: state.matches('saved') || state.matches('deleted'),

    // Errors
    loadError: context.loadError,
    saveError: context.saveError,
    deleteError: context.deleteError,
    validationErrors: context.validationErrors,

    // Field helpers
    field,
    checkbox,
    select,

    // Actions
    submit,
    reset,
    retry,
    requestDelete,
    confirmDelete,
    cancelDelete,
    requestCancel,
    confirmCancel,
    stay,

    // Raw access (escape hatch)
    state,
    send,
  };
}
