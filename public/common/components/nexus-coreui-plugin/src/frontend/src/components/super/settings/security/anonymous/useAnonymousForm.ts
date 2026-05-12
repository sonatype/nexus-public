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

import { useMemo, useCallback, useState } from 'react';
import { useForm, restClient, APIConstants } from '@sonatype/nexus-ui-plugin';
import { useToast } from '../../../../shared';
import { createAnonymousFormMachine, AnonymousFormData } from './anonymousFormMachine';
import type { RealmType, AnonymousSettings } from './types';

const { REST } = APIConstants;

/**
 * Extended context type for anonymous form
 */
interface AnonymousExtendedContext {
  realmTypes: RealmType[];
  pristineData?: AnonymousFormData;
}

/**
 * Options for useAnonymousForm hook
 */
export interface UseAnonymousFormOptions {
  onSave?: (data: AnonymousFormData) => Promise<void>;
  messages?: {
    saveSuccess?: (enabled: boolean) => string;
  };
}

/**
 * Custom hook for managing AnonymousForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. Handles the "disable anonymous access"
 * confirmation flow.
 */
export function useAnonymousForm(options: UseAnonymousFormOptions = {}) {
  const { onSave, messages } = options;
  const toast = useToast();
  const [showDisableConfirm, setShowDisableConfirm] = useState(false);
  const [pendingData, setPendingData] = useState<AnonymousFormData | null>(null);

  // Create the form machine - stable across renders
  const machine = useMemo(() => createAnonymousFormMachine(), []);

  // Use the form machine with save service override
  const form = useForm(machine, {
    services: {
      save: async (ctx: { data: AnonymousFormData }) => {
        try {
          const payload = {
            enabled: ctx.data.enabled,
            userId: ctx.data.userId.trim(),
            realmName: ctx.data.realmName,
          };
          await restClient.put<AnonymousSettings>(REST.INTERNAL.ANONYMOUS_SETTINGS, payload);

          const message = messages?.saveSuccess
            ? messages.saveSuccess(ctx.data.enabled)
            : ctx.data.enabled
              ? 'Anonymous access settings saved successfully'
              : 'Anonymous access disabled';
          toast.success(message);

          if (onSave) {
            await onSave(ctx.data);
          }
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // Access the raw state context for reference data
  const context = (form.state as { context: AnonymousExtendedContext }).context;

  // Get pristine data to check if we're disabling
  const pristineData = context.pristineData;
  const isDisabling = pristineData?.enabled && !form.data.enabled;

  // Handle submit - may need confirmation if disabling anonymous access
  const handleSubmit = useCallback(() => {
    if (isDisabling) {
      setPendingData({ ...form.data });
      setShowDisableConfirm(true);
    } else {
      form.submit();
    }
  }, [form, isDisabling]);

  // Confirm disable
  const handleConfirmDisable = useCallback(() => {
    setShowDisableConfirm(false);
    form.submit();
    setPendingData(null);
  }, [form]);

  // Cancel disable
  const handleCancelDisable = useCallback(() => {
    setShowDisableConfirm(false);
    setPendingData(null);
  }, []);

  return {
    // Form state
    formData: form.data,
    errors: form.validationErrors,
    touched: form.touched,

    // Computed state
    isPristine: form.isPristine,
    isSaving: form.isSaving,
    hasValidationErrors: form.hasValidationErrors,

    // Loading states
    isLoading: form.isLoading,
    loadError: form.loadError,

    // Reference data
    realmTypes: context.realmTypes || [],

    // Field helpers
    field: form.field,
    checkbox: form.checkbox,

    // Handlers
    handleChange: (field: string, value: unknown) =>
      form.send({ type: 'UPDATE', name: field, value }),
    handleBlur: (field: string) => form.send({ type: 'BLUR', name: field }),
    handleSubmit,
    handleDiscard: form.reset,
    handleRetry: form.retry,

    // Disable confirmation
    showDisableConfirm,
    handleConfirmDisable,
    handleCancelDisable,

    // Cancel dialog state
    cancelDialogOpen: form.isConfirmingCancel,
    handleCancelConfirm: form.confirmCancel,
    handleStay: form.stay,
  };
}
