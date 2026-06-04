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
import { createUsersFormMachine } from './usersFormMachine';
import { User, UserFormData, DEFAULT_SOURCE } from './types';

export interface UseUsersFormOptions {
  userId?: string;
  userSource?: string; // Source/realm of the user being edited
  user?: User; // Pre-loaded user to avoid reloading
  onSave?: (data: UserFormData) => Promise<void>;
  onCancel: () => void;
  createUser: (data: UserFormData) => Promise<void>;
  updateUser: (userId: string, data: UserFormData, source: string) => Promise<void>;
  changePassword?: (userId: string, newPassword: string) => Promise<void>;
}

export interface UseUsersFormReturn {
  form: ReturnType<typeof useForm>;
  user: User | null;
  isCreate: boolean;
}

/**
 * Custom hook for managing UserForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads both the user being edited
 * (if userId provided) and reference data (roles, sources).
 *
 * Supports source variant sub-states: local users have full form fields
 * including password, while external users have a reduced form.
 *
 * This hook also handles save operations and toast notifications.
 */
export function useUsersForm({
  userId,
  userSource = DEFAULT_SOURCE,
  user,
  onSave,
  onCancel,
  createUser,
  updateUser,
  changePassword,
}: UseUsersFormOptions): UseUsersFormReturn {
  const toast = useToast();
  const isCreate = !userId && !user;

  // Create the form machine - memoized by userId/userSource only. Do NOT include
  // user in deps: when UsersPage's fetch completes and user arrives, recreating
  // the machine would reset the form and lose edits. The machine fetches via
  // userId/userSource in its load service.
  const machine = useMemo(
    () => createUsersFormMachine(userId, userSource, user),
    [userId, userSource]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: UserFormData; user: User | null }) => {
        try {
          // Use preloaded user, ctx.user from load, or fallback to form data
          const userToUpdate = user || ctx.user;

          if (isCreate) {
            await createUser(ctx.data);
            toast.success(`User "${ctx.data.userId}" created successfully`);
          } else if (userToUpdate) {
            const source = userToUpdate.source || userSource;
            await updateUser(userToUpdate.userId, ctx.data, source);

            if (changePassword && ctx.data.password) {
              await changePassword(userToUpdate.userId, ctx.data.password);
            }

            toast.success(`User "${ctx.data.userId}" updated successfully`);
          } else if (ctx.data?.userId) {
            // Fallback: form has userId from load; use it when user object missing
            const source = ctx.data.source || userSource;
            await updateUser(ctx.data.userId, ctx.data, source);

            if (changePassword && ctx.data.password) {
              await changePassword(ctx.data.userId, ctx.data.password);
            }

            toast.success(`User "${ctx.data.userId}" updated successfully`);
          } else {
            throw new Error(
              'User data not loaded. Please go back and try again.'
            );
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

  // Access the raw state to get the extended context with reference data and user
  const context = (form.state as { context: { user: User | null } }).context;
  const loadedUser = context.user;

  return {
    form,
    user: loadedUser,
    isCreate,
  };
}
