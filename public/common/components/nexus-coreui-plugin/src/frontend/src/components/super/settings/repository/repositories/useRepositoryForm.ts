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
import { useForm } from '@sonatype/nexus-ui-plugin';
import { useToast } from '../../../../shared';
import { createRepositoryFormMachine } from './repositoryFormMachine';
import { Repository, RepositoryFormData, RepositoryType } from './types';

export interface UseRepositoryFormOptions {
  /** Repository name for edit mode; undefined for create mode */
  repositoryName?: string;
  /** Pre-loaded repository to avoid re-fetching */
  repository?: Repository;
  /** Format for the repository (e.g., 'maven2', 'npm', 'docker') */
  format: string;
  /** Repository type for create mode (defaults to 'hosted') */
  repositoryType?: RepositoryType;
  /** Optional callback after save completes. Return { skipNavigate: true } to prevent automatic navigation. */
  onSave?: (data: RepositoryFormData) => Promise<void | { skipNavigate?: boolean }>;
  /** Callback to navigate back / cancel the form */
  onCancel: () => void;
  /** API function to create a new repository */
  createRepository: (data: RepositoryFormData) => Promise<void>;
  /** API function to update an existing repository */
  updateRepository: (name: string, data: RepositoryFormData) => Promise<void>;
  /** When true, save only calls onSave (advance wizard) without creating/updating */
  advanceOnly?: boolean;
}

export interface UseRepositoryFormReturn {
  /** Form state and helpers from useForm */
  form: ReturnType<typeof useForm>;
  /** Loaded repository data (null for create mode) */
  repository: Repository | null;
  /** Whether this is a create (true) or edit (false) form */
  isCreate: boolean;
}

/**
 * Custom hook for managing RepositoryForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking,
 * unsaved changes warnings, and type variant sub-states for hosted/proxy/group.
 *
 * The machine handles:
 * - Loading the repository (if editing) and reference data (blob stores, routing rules, etc.)
 * - TYPE_CHANGE transitions between hosted/proxy/group with field resets
 * - Validation per repository type (proxy requires remoteUrl, group requires members)
 * - Format as a context field that affects validation, not a sub-state
 *
 * @example
 * ```tsx
 * const { form, repository, isCreate } = useRepositoryForm({
 *   format: 'maven2',
 *   repositoryType: 'hosted',
 *   onCancel: () => navigate('/admin/repository/repositories'),
 *   createRepository: api.createRepository,
 *   updateRepository: api.updateRepository,
 * });
 *
 * if (form.isLoading) return <Spinner />;
 *
 * // Change repository type (triggers sub-state transition)
 * form.send({ type: 'TYPE_CHANGE', value: 'proxy' });
 * ```
 */
export function useRepositoryForm({
  repositoryName,
  repository,
  format,
  repositoryType = 'hosted',
  onSave,
  onCancel,
  createRepository,
  updateRepository,
  advanceOnly = false,
}: UseRepositoryFormOptions): UseRepositoryFormReturn {
  const toast = useToast();
  const isCreate = !repositoryName && !repository;

  // Create the form machine - memoized based on key identifiers
  const machine = useMemo(
    () =>
      createRepositoryFormMachine({
        repositoryName,
        preloadedRepository: repository,
        format,
        repositoryType,
      }),
    [repositoryName, repository, format, repositoryType]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: RepositoryFormData; repository: Repository | null }) => {
        try {
          if (advanceOnly) {
            // Wizard advance: validate passed, just notify parent (no create/update)
            if (onSave) {
              await onSave(ctx.data);
            }
            return;
          }
          // Use the preloaded repository if available, otherwise use ctx.repository
          const repoToUpdate = repository || ctx.repository;

          if (isCreate) {
            await createRepository(ctx.data);
            toast.success(`Repository "${ctx.data.name}" created successfully`);
          } else if (repoToUpdate) {
            await updateRepository(repoToUpdate.name, ctx.data);
            toast.success(`Repository "${ctx.data.name}" updated successfully`);
          }
          // Call the provided onSave callback if needed
          let skipNavigate = false;
          if (onSave) {
            const result = await onSave(ctx.data);
            skipNavigate = !!(result && typeof result === 'object' && result.skipNavigate);
          }
          // Navigate back after successful save (unless onSave requested skip)
          if (!skipNavigate) {
            onCancel();
          }
        } catch (err) {
          // Raw API functions throw errors, form machine will handle them
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // Access the raw state to get the extended context with reference data
  const context = (form.state as { context: { repository: Repository | null } }).context;
  const loadedRepository = context.repository;

  return {
    form,
    repository: loadedRepository,
    isCreate,
  };
}
