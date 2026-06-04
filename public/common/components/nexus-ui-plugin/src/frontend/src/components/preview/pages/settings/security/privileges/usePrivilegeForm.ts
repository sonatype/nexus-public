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
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createPrivilegeFormMachine } from './privilegeFormMachine';
import { Privilege, PrivilegeFormData } from './types';

export interface UsePrivilegeFormOptions {
  privilegeId?: string;
  privilege?: Privilege; // Pre-loaded privilege to avoid reloading
  typeId?: string; // Initial type ID for creation
  onSave?: (data: PrivilegeFormData) => Promise<void>;
  onCancel: () => void;
  createPrivilege: (data: PrivilegeFormData) => Promise<void>;
  updatePrivilege: (data: PrivilegeFormData & { id: string; version: string }) => Promise<void>;
}

export interface UsePrivilegeFormReturn {
  form: ReturnType<typeof useForm>;
  privilege: Privilege | null;
  isCreate: boolean;
}

/**
 * Custom hook for managing PrivilegeForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads both the privilege being edited
 * (if privilegeId provided) and reference data (privilege types, repositories, etc.).
 *
 * This hook also handles save operations and toast notifications.
 */
export function usePrivilegeForm({
  privilegeId,
  privilege,
  typeId,
  onSave,
  onCancel,
  createPrivilege,
  updatePrivilege,
}: UsePrivilegeFormOptions): UsePrivilegeFormReturn {
  const toast = useToast();
  const isCreate = !privilegeId && !privilege;

  // Create the form machine - memoized based on privilegeId and privilege
  const machine = useMemo(
    () => createPrivilegeFormMachine(privilegeId, privilege, typeId),
    [privilegeId, privilege, typeId]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: PrivilegeFormData; privilege: Privilege | null }) => {
        try {
          // Use the preloaded privilege if available, otherwise use ctx.privilege
          const privilegeToUpdate = privilege || ctx.privilege;

          if (isCreate) {
            await createPrivilege(ctx.data);
            toast.success(`Privilege "${ctx.data.name}" created successfully`);
          } else if (privilegeToUpdate) {
            const updateData = {
              ...ctx.data,
              id: privilegeToUpdate.id,
              version: privilegeToUpdate.version,
            };
            await updatePrivilege(updateData);
            toast.success(`Privilege "${ctx.data.name}" updated successfully`);
          }
          // Call the provided onSave callback if needed
          if (onSave) {
            await onSave(ctx.data);
          }
          // Navigate back after successful save
          onCancel();
        } catch (err) {
          // Raw API functions throw errors, form machine will handle them
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // Access the raw state to get the extended context with reference data and privilege
  const context = (form.state as { context: { privilege: Privilege | null } }).context;
  const loadedPrivilege = context.privilege;

  /**
   * Helper for checkbox group fields (handles comma-separated strings)
   */
  const checkboxGroup = useCallback((name: string) => {
    const value = form.field(name).value;
    return {
      name,
      value,
      onChange: (newValue: string) => form.send({ type: 'UPDATE', name, value: newValue }),
      error: form.field(name).error,
    };
  }, [form]);

  return {
    form: {
      ...form,
      checkboxGroup,
    },
    privilege: loadedPrivilege,
    isCreate,
  };
}
