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
import { createCleanupPolicyFormMachine, type CleanupPolicyMachineContext } from './cleanupPolicyFormMachine';
import { CleanupPolicy, CleanupPolicyFormData, FormatCriteria } from './types';

export interface UseCleanupPolicyFormOptions {
  policyName?: string;
  policy?: CleanupPolicy | null;
  formatCriteria: FormatCriteria[];
  onSave?: (data: CleanupPolicyFormData) => Promise<void>;
  onCancel: () => void;
  createPolicy: (data: CleanupPolicyFormData) => Promise<CleanupPolicy>;
  updatePolicy: (name: string, data: CleanupPolicyFormData) => Promise<CleanupPolicy>;
  /**
   * Optional getter for the repository attachment override. Called at save
   * time so the form can read the latest selection state. Threaded into the
   * save payload because the underlying XState machine does not track
   * `repositories`.
   *   - returns `undefined` -> field omitted (backend preserves existing attachments)
   *   - returns `[]`        -> field present as empty list (backend clears all attachments)
   *   - returns `[a, b]`    -> field present as exact set
   */
  getRepositories?: () => string[] | undefined;
}

export interface UseCleanupPolicyFormReturn {
  form: ReturnType<typeof useForm>;
  policy: CleanupPolicy | null;
  isCreate: boolean;
  criteriaEnabled: CleanupPolicyMachineContext['criteriaEnabled'];
  changeFormat: (format: string) => void;
  toggleCriteria: (criteria: string, enabled: boolean) => void;
  changeReleaseType: (releaseType: string) => void;
}

/**
 * Custom hook for managing CleanupPolicyForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine tracks format-specific criteria
 * availability and criteria enable/disable states.
 *
 * This hook also handles save operations and toast notifications.
 */
export function useCleanupPolicyForm({
  policyName,
  policy,
  formatCriteria,
  onSave,
  onCancel,
  createPolicy,
  updatePolicy,
  getRepositories,
}: UseCleanupPolicyFormOptions): UseCleanupPolicyFormReturn {
  const toast = useToast();
  const isCreate = !(policyName || policy);

  // Create the form machine - memoized based on policyName, policy, and formatCriteria
  const machine = useMemo(
    () => createCleanupPolicyFormMachine(policyName, policy, formatCriteria),
    [policyName, policy, formatCriteria]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: {
        data: CleanupPolicyFormData;
        policy: CleanupPolicy | null;
        criteriaEnabled: CleanupPolicyMachineContext['criteriaEnabled'];
      }) => {
        try {
          // Clean data: null out disabled criteria before saving
          const saveData: CleanupPolicyFormData = {
            ...ctx.data,
            criteriaLastBlobUpdated: ctx.criteriaEnabled.lastBlobUpdated
              ? ctx.data.criteriaLastBlobUpdated
              : null,
            criteriaLastDownloaded: ctx.criteriaEnabled.lastDownloaded
              ? ctx.data.criteriaLastDownloaded
              : null,
            criteriaAssetRegex: ctx.criteriaEnabled.assetRegex
              ? ctx.data.criteriaAssetRegex
              : null,
            retain: ctx.criteriaEnabled.retain ? ctx.data.retain : null,
            sortBy: ctx.criteriaEnabled.retain ? ctx.data.sortBy : null,
            // Include repositories only when caller opted in. `undefined` means
            // "preserve existing attachments" per the backend contract.
            ...((): { repositories?: string[] } => {
              const repos = getRepositories?.();
              return repos !== undefined ? { repositories: repos } : {};
            })(),
          };

          if (isCreate) {
            await createPolicy(saveData);
            toast.success(`Cleanup policy "${saveData.name}" created successfully`);
          } else {
            const name = policy?.name || policyName || saveData.name;
            await updatePolicy(name, saveData);
            toast.success(`Cleanup policy "${saveData.name}" updated successfully`);
          }
          if (onSave) {
            await onSave(ctx.data);
          }
          onCancel();
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // Access the extended context
  const context = (form.state as {
    context: {
      policy: CleanupPolicy | null;
      criteriaEnabled: CleanupPolicyMachineContext['criteriaEnabled'];
    };
  }).context;

  /**
   * Change the selected format (resets all criteria)
   */
  const changeFormat = useCallback(
    (format: string) => {
      form.send({ type: 'FORMAT_CHANGE', value: format } as any);
    },
    [form]
  );

  /**
   * Toggle a criteria checkbox on/off
   */
  const toggleCriteria = useCallback(
    (criteria: string, enabled: boolean) => {
      form.send({ type: 'TOGGLE_CRITERIA', criteria, enabled } as any);
    },
    [form]
  );

  /**
   * Change the release type (may disable retain criteria)
   */
  const changeReleaseType = useCallback(
    (releaseType: string) => {
      form.send({ type: 'RELEASE_TYPE_CHANGE', value: releaseType } as any);
    },
    [form]
  );

  return {
    form,
    policy: context.policy,
    isCreate,
    criteriaEnabled: context.criteriaEnabled,
    changeFormat,
    toggleCriteria,
    changeReleaseType,
  };
}
