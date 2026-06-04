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

import { useMemo } from 'react';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createRolesFormMachine } from './rolesFormMachine';
import { Role, RoleFormData } from './types';

export interface UseRolesFormOptions {
  roleId?: string;
  role?: Role;
  onCancel: () => void;
  onComplete?: () => void;
  createRole: (data: RoleFormData) => Promise<void>;
  updateRole: (data: RoleFormData) => Promise<void>;
}

export interface UseRolesFormReturn {
  form: ReturnType<typeof useForm>;
  role: Role | null;
  isCreate: boolean;
}

/**
 * Custom hook for managing RoleForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads both the role being edited
 * (if roleId provided) and reference data (privileges, roles).
 *
 * This hook also handles save operations and toast notifications.
 */
export function useRolesForm({
  roleId,
  role,
  onCancel,
  onComplete,
  createRole,
  updateRole,
}: UseRolesFormOptions): UseRolesFormReturn {
  const toast = useToast();
  const isCreate = !roleId && !role;

  const machine = useMemo(
    () => createRolesFormMachine(roleId, role),
    [roleId, role]
  );

  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: RoleFormData; role: Role | null }) => {
        try {
          const roleToUpdate = role || ctx.role;

          const hasPrivilegeOrRole =
            (ctx.data.privileges?.length ?? 0) > 0 || (ctx.data.roles?.length ?? 0) > 0;
          if (!hasPrivilegeOrRole) {
            throw new Error('Select at least one privilege or contained role');
          }

          if (isCreate) {
            await createRole(ctx.data);
            toast.success(`Role "${ctx.data.name}" created successfully`);
          } else if (roleToUpdate) {
            await updateRole(ctx.data);
            toast.success(`Role "${ctx.data.name}" updated successfully`);
          }
          if (onComplete) {
            onComplete();
          }
          onCancel();
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // Access the raw state to get the extended context with reference data and role
  const context = (form.state as { context: { role: Role | null } }).context;
  const loadedRole = context.role;

  return {
    form,
    role: loadedRole,
    isCreate,
  };
}
