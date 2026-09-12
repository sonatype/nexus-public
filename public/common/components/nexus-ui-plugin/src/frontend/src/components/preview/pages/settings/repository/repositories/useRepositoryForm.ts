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

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useCurrentStateAndParams } from '@uirouter/react';
import { useForm } from '../../../../../../interface/form';
import { useToast, useIsCloud, useHasFirewallLicense } from '../../../../shared';
import { useRepositoriesApi } from './useRepositoriesApi';
import { createRepositoryFormMachine, validateRepository } from './repositoryFormMachine';
import {
  Repository,
  RepositoryFormData,
  RepositoryType,
  BlobStore,
  RoutingRule,
  CleanupPolicy,
  RepositoryReference,
  hasFormErrors,
} from './types';

interface RepositoryFormMachineContext {
  repository: Repository | null;
  pristineData: RepositoryFormData | undefined;
  blobStores: BlobStore[];
  routingRules: RoutingRule[];
  cleanupPolicies: CleanupPolicy[];
  memberRepositories: RepositoryReference[];
}

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
  onSave?: (data: RepositoryFormData) => Promise<undefined | { skipNavigate?: boolean }>;
  /** Callback to navigate back / cancel the form */
  onCancel: () => void;
  /** When true, save only calls onSave (advance wizard) without creating/updating */
  advanceOnly?: boolean;
  /** Ref for exposing submit function to parent (wizard integration) */
  onSubmitRef?: React.MutableRefObject<(() => void) | null>;
  /** Callback to report whether form is valid for wizard advancement */
  onCanAdvanceChange?: (canAdvance: boolean) => void;
  /** Callback to report whether the form has unsaved edits (NEXUS-54349) */
  onDirtyChange?: (isDirty: boolean) => void;
}

export type RepositoryFormTab =
  | 'summary'
  | 'settings'
  | 'firewall'
  | 'health-check'
  | 'evaluation'
  | 'audit'
  | 'tasks-capabilities';

export interface UseRepositoryFormReturn {
  /** Form state and helpers from useForm */
  form: ReturnType<typeof useForm>;
  /** Loaded repository data (null for create mode) */
  repository: Repository | null;
  /** Whether this is a create (true) or edit (false) form */
  isCreate: boolean;

  /** Whether the current environment has a Firewall/CLM license */
  hasFirewallLicense: boolean;
  /** Whether running in cloud mode */
  isCloud: boolean;

  /** Currently active tab (edit mode) */
  activeTab: RepositoryFormTab;
  /** Set the active tab */
  setActiveTab: (tab: RepositoryFormTab) => void;
  /** Whether the proxy remote URL has changed from its original value */
  originChangeWarning: boolean;
  /** Set the origin change warning state (called by HttpClientFacet) */
  setOriginChangeWarning: (warning: boolean) => void;

  /** Form data cast to RepositoryFormData */
  formData: RepositoryFormData;
  /** Pristine data (original values before edits) */
  pristineData: RepositoryFormData | undefined;
  /** Validation errors from the machine */
  errors: Record<string, string | undefined>;

  /** Reference data: available blob stores */
  blobStores: BlobStore[];
  /** Reference data: available routing rules */
  routingRules: RoutingRule[];
  /** Reference data: available cleanup policies */
  cleanupPolicies: CleanupPolicy[];
  /** Reference data: available member repositories (for group type) */
  memberRepositories: RepositoryReference[];

  /** Update one or more top-level form fields */
  handleChange: (updates: Partial<RepositoryFormData>) => void;
  /** Update a nested form field (e.g., storage, proxy, httpClient) */
  handleNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
}

/**
 * Custom hook for managing RepositoryForm state and logic.
 *
 * This is the Layer 2 (Integration) hook that:
 * - Initializes the XState form machine
 * - Provides all derived state (isCloud, hasFirewallLicense, reference data)
 * - Manages UI state (activeTab, originChangeWarning)
 * - Handles wizard integration (onSubmitRef, onCanAdvanceChange)
 * - Exposes bridge functions for facet components (handleChange, handleNestedChange)
 *
 * The component (Layer 3) should only consume this hook's return value and render JSX.
 */
export function useRepositoryForm({
  repositoryName,
  repository,
  format,
  repositoryType = 'hosted',
  onSave,
  onCancel,
  advanceOnly = false,
  onSubmitRef,
  onCanAdvanceChange,
  onDirtyChange,
}: UseRepositoryFormOptions): UseRepositoryFormReturn {
  const toast = useToast();
  const { createRepository, updateRepository } = useRepositoriesApi();
  const isCreate = !repositoryName;

  // ============================================
  // Derived state from ExtJS (session-scoped)
  // ============================================

  const isCloud = useIsCloud();
  const hasFirewallLicense = useHasFirewallLicense();

  // ============================================
  // UI state (presentation-adjacent, managed in hook layer)
  // ============================================

  // ?tab from router state supports deep-links (e.g. evaluation tab from Hosted Repo Eval surface); dynamic: true lets it change without remount.
  const { params: routerParams } = useCurrentStateAndParams();
  const TAB_IDS: readonly RepositoryFormTab[] = ['summary', 'settings', 'firewall', 'health-check', 'evaluation', 'audit', 'tasks-capabilities'];
  const initialTab: RepositoryFormTab = (() => {
    if (isCreate) return 'summary';
    const requested = routerParams?.tab as string | undefined | null;
    return requested && (TAB_IDS as readonly string[]).includes(requested)
      ? (requested as RepositoryFormTab)
      : 'settings';
  })();
  const [activeTab, setActiveTab] = useState<RepositoryFormTab>(initialTab);
  const [originChangeWarning, setOriginChangeWarning] = useState(false);

  // ============================================
  // Form machine setup
  // ============================================

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

  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: RepositoryFormData; repository: Repository | null }) => {
        try {
          if (advanceOnly) {
            if (onSave) {
              await onSave(ctx.data);
            }
            return;
          }
          const repoToUpdate = repository || ctx.repository;

          if (isCreate) {
            await createRepository(ctx.data);
            toast.success(`Repository "${ctx.data.name}" created successfully`);
          } else if (repoToUpdate) {
            await updateRepository(repoToUpdate.name, ctx.data);
            toast.success(`Repository "${ctx.data.name}" updated successfully`);
          }
          let skipNavigate = false;
          if (onSave) {
            const result = await onSave(ctx.data);
            skipNavigate = !!(result && typeof result === 'object' && result.skipNavigate);
          }
          if (!skipNavigate) {
            onCancel();
          }
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // ============================================
  // Derived data from machine context
  // ============================================

  const context = form.state.context as RepositoryFormMachineContext;
  const loadedRepository = context.repository;
  const formData = form.data as RepositoryFormData;
  const pristineData = context.pristineData;
  const blobStores = context.blobStores || [];
  const routingRules = context.routingRules || [];
  const cleanupPolicies = context.cleanupPolicies || [];
  const memberRepositories = context.memberRepositories || [];
  const errors = form.validationErrors || {};

  // ============================================
  // Bridge functions for facet components
  // ============================================

  const handleChange = useCallback((updates: Partial<RepositoryFormData>) => {
    Object.entries(updates).forEach(([key, value]) => {
      form.send({ type: 'UPDATE', name: key, value });
    });
  }, [form]);

  const handleNestedChange = useCallback(<K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => {
    const current = (formData[key] ?? {}) as Partial<RepositoryFormData[K]>;
    form.send({ type: 'UPDATE', name: key as string, value: { ...current, ...updates } });
  }, [form, formData]);

  // ============================================
  // Wizard integration
  // ============================================

  const { isLoading, isSaving } = form;

  useEffect(() => {
    if (onSubmitRef) {
      onSubmitRef.current = () => {
        if (!isLoading && !isSaving) {
          if (advanceOnly && onSave) {
            const validationErrors = validateRepository(form.data as RepositoryFormData, { isCloud });
            if (!hasFormErrors(validationErrors)) {
              onSave(form.data as RepositoryFormData);
            } else {
              form.send('SUBMIT');
            }
          } else {
            form.send('SUBMIT');
          }
        }
      };
      return () => { onSubmitRef.current = null; };
    }
  }, [onSubmitRef, isLoading, isSaving, advanceOnly, onSave, isCloud, form]);

  useEffect(() => {
    if (!onCanAdvanceChange) return;
    if (form.isLoading) {
      onCanAdvanceChange(false);
      return;
    }
    if (form.data) {
      const validationErrors = validateRepository(form.data as RepositoryFormData, { isCloud });
      onCanAdvanceChange(!hasFormErrors(validationErrors));
    }
  }, [form.data, form.isLoading, onCanAdvanceChange, isCloud]);

  // NEXUS-54349: report the machine's pristine state to the wizard so it can
  // skip the "Unsaved Changes" dialog on Cancel when no field has been edited.
  useEffect(() => {
    onDirtyChange?.(!form.isPristine);
  }, [form.isPristine, onDirtyChange]);

  // ============================================
  // Return
  // ============================================

  return {
    form,
    repository: loadedRepository,
    isCreate,

    hasFirewallLicense,
    isCloud,

    activeTab,
    setActiveTab,
    originChangeWarning,
    setOriginChangeWarning,

    formData,
    pristineData,
    errors,

    blobStores,
    routingRules,
    cleanupPolicies,
    memberRepositories,

    handleChange,
    handleNestedChange,
  };
}
