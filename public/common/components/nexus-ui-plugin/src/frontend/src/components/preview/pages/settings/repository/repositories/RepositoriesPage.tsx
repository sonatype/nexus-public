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

import { PageHeader, useToast } from '../../../../shared';
import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Box, Button, Flex, Link } from '@radix-ui/themes';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ArrowLeft, Database, Plus } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsAlert, WizardForm } from '../../../../shared/form';
import { clearDirtyState } from '../../../../shared/hooks/useUnsavedChangesWarning';

import { DeleteConfirmationModal } from '../../../../shared/modals/DeleteConfirmationModal';
import { ConfirmDialog } from '../../../../shared/ConfirmDialog';
import { RepositoriesList } from './RepositoriesList';
import { RepositoryTypeSelector } from './RepositoryTypeSelector';
import { RepositoryForm } from './RepositoryForm';
import { RepositoryFirewallStep } from './RepositoryFirewallStep';
import { RepositoryRHCStep } from './RepositoryRHCStep';
import { FormatIcon } from '../repositories/components/FormatIcon';
import {
  enableFirewallAudit,
  enableFirewallPccs,
  enableFirewallQuarantine,
} from '../../../../shared/security/useFirewallEnable';
import { useRepositoriesApi } from './useRepositoriesApi';
import { Repository, Recipe, RepositoryFormData, RepositoryType } from './types';

import './RepositoriesPage.scss';

function hasFirewallLicense(): boolean {
  try {
    const clm = ExtJS.state()?.getValue?.('clm');
    return !!(clm?.enabled ?? clm?.hasFirewall);
  } catch {
    return false;
  }
}

// Base path for repository URLs
const BASE_PATH = 'preview/admin/repository/repositories';

/**
 * URL-based routing patterns:
 * - /repositories           → List page
 * - /repositories/create    → Type selector
 * - /repositories/create/{format}/{type} → Create form
 * - /repositories/{name}    → Edit form
 */
type ViewMode = 'list' | 'select-type' | 'create' | 'edit';

interface RouteState {
  viewMode: ViewMode;
  repoName: string | null;
  format: string | null;
  type: string | null;
}

// Confirmation state for repository action buttons (rebuild index,
// invalidate cache, toggle online). Each shares the same ConfirmDialog so
// we hold the pending action in a single piece of state.
type PendingAction =
  | { kind: 'rebuild-index' }
  | { kind: 'invalidate-cache' }
  | { kind: 'toggle-online'; nextOnline: boolean };

function parseRoute(hash: string): RouteState {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');
  const repoIndex = parts.indexOf('repositories');
  if (repoIndex === -1) return { viewMode: 'list', repoName: null, format: null, type: null };

  const pathAfterRepos = parts.slice(repoIndex + 1);
  if (pathAfterRepos.length === 0) return { viewMode: 'list', repoName: null, format: null, type: null };

  if (pathAfterRepos[0] === 'create') {
    if (pathAfterRepos.length === 1) return { viewMode: 'select-type', repoName: null, format: null, type: null };
    if (pathAfterRepos.length >= 3) {
      return {
        viewMode: 'create',
        repoName: null,
        format: decodeURIComponent(pathAfterRepos[1]),
        type: decodeURIComponent(pathAfterRepos[2]),
      };
    }
    return { viewMode: 'select-type', repoName: null, format: null, type: null };
  }

  if (pathAfterRepos[0] && pathAfterRepos[0] !== 'profile') {
    if (pathAfterRepos.length >= 2 && pathAfterRepos[1] === 'profile') {
      return { viewMode: 'list', repoName: null, format: null, type: null };
    }
    return { viewMode: 'edit', repoName: decodeURIComponent(pathAfterRepos[0]), format: null, type: null };
  }

  return { viewMode: 'list', repoName: null, format: null, type: null };
}

function navigateTo(path: string) {
  window.location.hash = path;
}

export function RepositoriesPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [repository, setRepository] = useState<Repository | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [selectedFormat, setSelectedFormat] = useState<string | null>(null);
  const [internalWizardStep, setInternalWizardStep] = useState(0);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [_fetchingRepository, _setFetchingRepository] = useState(false);

  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [isExecutingAction, setIsExecutingAction] = useState(false);

  const toast = useToast();
  const router = useRouter();
  const { params: routerParams } = useCurrentStateAndParams();
  const {
    loading,
    error,
    setError,
    fetchRepository,
    createRepository,
    deleteRepository,
    invalidateCache,
    rebuildIndex,
    setRepositoryOnline,
    enableHealthCheck,
    disableHealthCheck,
    fetchHealthCheckCapabilityEnabled,
  } = useRepositoriesApi();

  const canCreate = ExtJS.checkPermission('nexus:repository-admin:*:*:add');
  const canUpdate = ExtJS.checkPermission('nexus:repository-admin:*:*:edit');
  const canDelete = ExtJS.checkPermission('nexus:repository-admin:*:*:delete');

  useEffect(() => {
    const handleHashChange = () => {
      const newState = parseRoute(window.location.hash);
      setRouteState(newState);
      setError(null);
      // Synchronize internal wizard step with route
      if (newState.viewMode === 'create' && newState.format && newState.type) {
        setInternalWizardStep(2);
        setSelectedFormat(newState.format);
      } else if (newState.viewMode === 'select-type') {
        // Stay on Step 0 or 1 based on selectedFormat
      } else {
        setInternalWizardStep(0);
        setSelectedFormat(null);
        setPostCreateStep(null);
        setFirewallChoice(null);
        setRhcChoice(null);
        setCanAdvanceFromStep2(false);
        setIsRepoFormDirty(false);
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [setError]);

  const selectedRecipe = useMemo<Recipe | null>(() => {
    if (routeState.format && routeState.type) {
      return { format: routeState.format, type: routeState.type as RepositoryType, name: `${routeState.format}-${routeState.type}` };
    }
    return null;
  }, [routeState.format, routeState.type]);

  const editRecipe = useMemo<Recipe | null>(() => {
    if (repository) {
      return { format: repository.format, type: repository.type as RepositoryType, name: `${repository.format}-${repository.type}` };
    }
    return null;
  }, [repository]);

  useEffect(() => {
    if (routeState.viewMode === 'edit' && routeState.repoName) {
      fetchRepository(routeState.repoName).then(setRepository).catch(err => { setError(err.message); navigateTo(BASE_PATH); });
    } else {
      setRepository(null);
    }
  }, [routeState.viewMode, routeState.repoName, fetchRepository, setError]);

  const handleSelectRepository = useCallback((name: string) => navigateTo(`${BASE_PATH}/${encodeURIComponent(name)}`), []);
  const handleCreate = useCallback(() => navigateTo(`${BASE_PATH}/create`), []);
  const handleBack = useCallback(() => navigateTo(BASE_PATH), []);
  const _handleBackToTypeSelect = useCallback(() => navigateTo(`${BASE_PATH}/create`), []);

  const [canAdvanceFromStep0, setCanAdvanceFromStep0] = useState(false);
  const [canAdvanceFromStep1, setCanAdvanceFromStep1] = useState(false);
  const [canAdvanceFromStep2, setCanAdvanceFromStep2] = useState(false);
  // NEXUS-54349: mirrors the RepositoryForm machine's dirty state so Cancel on
  // an untouched config step navigates away without the "Unsaved Changes" dialog.
  const [isRepoFormDirty, setIsRepoFormDirty] = useState(false);
  const [pendingRecipe, setPendingRecipe] = useState<Recipe | null>(null);
  const [postCreateStep, setPostCreateStep] = useState<'firewall' | 'rhc' | null>(null);
  const [firewallChoice, setFirewallChoice] = useState<'none' | 'audit' | 'quarantine' | 'pccs' | null>(null);
  const [rhcChoice, setRhcChoice] = useState<'enable' | 'none' | null>(null);
  const [rhcCapabilityEnabled, setRhcCapabilityEnabled] = useState<boolean | null>(null);
  const rhcCapabilityFetchedRef = useRef(false);
  const savedFormDataRef = useRef<RepositoryFormData | null>(null);

  const isProxyRecipe = selectedRecipe?.type === 'proxy';

  useEffect(() => {
    if (!isProxyRecipe) {
      setRhcCapabilityEnabled(null);
      rhcCapabilityFetchedRef.current = false;
      return;
    }
    if (rhcCapabilityFetchedRef.current) return;
    rhcCapabilityFetchedRef.current = true;
    fetchHealthCheckCapabilityEnabled().then(setRhcCapabilityEnabled);
  }, [isProxyRecipe, fetchHealthCheckCapabilityEnabled]);

  const wizardSteps = useMemo(
    () => [
      { id: 'format', label: 'Select Format' },
      { id: 'type', label: 'Select Type' },
      { id: 'config', label: 'Configure' },
      { id: 'firewall', label: 'Enable Firewall' },
      { id: 'rhc', label: 'Enable RHC' },
    ],
    []
  );

  const createStep = useMemo(() => {
    if (postCreateStep === 'rhc') return 4;
    if (postCreateStep === 'firewall') return 3;
    if (routeState.viewMode === 'create' && selectedRecipe) return 2;
    return internalWizardStep;
  }, [routeState.viewMode, selectedRecipe, internalWizardStep, postCreateStep]);

  const finishCreate = useCallback(() => {
    setRefreshKey(k => k + 1);
    setPostCreateStep(null);
    setFirewallChoice(null);
    setRhcChoice(null);
    savedFormDataRef.current = null;
    setRouteState({ viewMode: 'list', repoName: null, format: null, type: null });
    navigateTo(BASE_PATH);
  }, []);

  const handleWizardStepChange = useCallback((step: number) => {
    if (postCreateStep && step < 3) {
      setPostCreateStep(null);
      return;
    }
    if (postCreateStep === 'rhc' && step === 3) {
      setPostCreateStep('firewall');
      return;
    }
    if (step === 0) {
      setSelectedFormat(null);
      setInternalWizardStep(0);
      setCanAdvanceFromStep0(false);
      setCanAdvanceFromStep1(false);
      navigateTo(`${BASE_PATH}/create`);
    } else if (step === 1) {
      // Preserve format when going back to type selection
      const currentFormat = selectedRecipe?.format || pendingRecipe?.format;
      if (currentFormat) {
        setSelectedFormat(currentFormat);
        setPendingRecipe({ format: currentFormat, name: currentFormat });
      }
      setInternalWizardStep(1);
      setCanAdvanceFromStep1(false);
      navigateTo(`${BASE_PATH}/create`);
    } else if (step === 2) {
      if (pendingRecipe?.type) {
        navigateTo(`${BASE_PATH}/create/${encodeURIComponent(pendingRecipe.format)}/${encodeURIComponent(pendingRecipe.type)}`);
      }
    } else if (step === 3 && selectedRecipe) {
      if (selectedRecipe.type === 'proxy') {
        setPostCreateStep('firewall');
      } else {
        repoFormSubmitRef.current?.();
      }
    } else if (step === 4) {
      setPostCreateStep('rhc');
    }
  }, [pendingRecipe, selectedRecipe, postCreateStep]);

  const repoFormSubmitRef = useRef<(() => void) | null>(null);

  const handleFinalSubmit = useCallback(async () => {
    if (postCreateStep === 'rhc' && savedFormDataRef.current) {
      try {
        await createRepository(savedFormDataRef.current);
        const repoName = savedFormDataRef.current.name;
        const hasFw = hasFirewallLicense();
        if (hasFw && firewallChoice && firewallChoice !== 'none') {
          try {
            if (firewallChoice === 'audit') await enableFirewallAudit(repoName);
            else if (firewallChoice === 'quarantine') await enableFirewallQuarantine(repoName);
            else await enableFirewallPccs(repoName);
          } catch {
            // Firewall enable is best-effort during create; admin can configure from repo edit
          }
        }
        if (rhcChoice === 'enable') {
          try {
            await enableHealthCheck(repoName);
          } catch (err) {
            const errorMessage = err instanceof Error ? err.message : 'Failed to enable Health Check';
            toast.warning(`Health Check could not be enabled for "${repoName}": ${errorMessage}`);
          }
        } else {
          // The backend may auto-enable HC for new proxy repos; explicitly disable to enforce
          // the user's choice (or the default) of having HC off after creation.
          try {
            await disableHealthCheck(repoName);
          } catch (err) {
            console.warn(`Could not disable auto-enabled Health Check for "${repoName}":`, err);
          }
        }
        toast.success(`Repository "${repoName}" created successfully`);
        finishCreate();
      } catch (err) {
        toast.error(err instanceof Error ? err.message : 'Failed to create repository');
        throw err;
      }
    } else if (repoFormSubmitRef.current) {
      repoFormSubmitRef.current();
    }
  }, [postCreateStep, firewallChoice, rhcChoice, createRepository, enableHealthCheck, disableHealthCheck, finishCreate, toast]);

  const handleRecipeSelectionChange = useCallback((canAdvance: boolean, recipe: Recipe | null) => {
    if (recipe && !recipe.type) {
      // Format selected - auto-advance to type selection
      setCanAdvanceFromStep0(true);
      setSelectedFormat(recipe.format);
      setPendingRecipe(recipe);
      setInternalWizardStep(1);
    } else if (recipe?.type) {
      // Type selected - auto-advance to configuration
      setCanAdvanceFromStep1(true);
      setPendingRecipe(recipe);
      navigateTo(`${BASE_PATH}/create/${encodeURIComponent(recipe.format)}/${encodeURIComponent(recipe.type)}`);
    } else {
      setCanAdvanceFromStep0(false);
      setCanAdvanceFromStep1(false);
    }
  }, []);

  const handleSave = useCallback(
    async (data: RepositoryFormData) => {
      // Edit mode: updateRepository already PUT — skip create branch to avoid duplicate POST rejection.
      if (routeState.viewMode === 'edit') {
        finishCreate();
        return { skipNavigate: true };
      }

      if (
        selectedRecipe?.type === 'proxy' &&
        (routeState.viewMode === 'create' || routeState.viewMode === 'select-type') &&
        postCreateStep === 'rhc'
      ) {
        savedFormDataRef.current = data;
        return { skipNavigate: true };
      }
      if (
        selectedRecipe?.type === 'proxy' &&
        (routeState.viewMode === 'create' || routeState.viewMode === 'select-type') &&
        postCreateStep === null
      ) {
        savedFormDataRef.current = data;
        setPostCreateStep('firewall');
        return { skipNavigate: true };
      }
      // Non-proxy repos: create directly from wizard (form machine stays in editing
      // so the form remains editable if creation fails)
      try {
        await createRepository(data);
        toast.success(`Repository "${data.name}" created successfully`);
        finishCreate();
      } catch (err) {
        toast.error(err instanceof Error ? err.message : 'Failed to create repository');
      }
      return { skipNavigate: true };
    },
    [selectedRecipe, routeState.viewMode, postCreateStep, finishCreate, toast, createRepository]
  );

  const handleDelete = useCallback(() => {
    setDeleteModalOpen(true);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!repository) return;
    setIsDeleting(true);
    try {
      await deleteRepository(repository.name);
      toast.success(`Repository "${repository.name}" deleted successfully`);
      setRefreshKey(k => k + 1);
      setDeleteModalOpen(false);
      handleBack();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsDeleting(false);
    }
  }, [repository, deleteRepository, toast, handleBack, setError]);

  // Action handlers: stage pending action → ConfirmDialog → handleConfirmAction; refetch after toggle keeps pristineData in sync.
  const handleRebuildIndex = useCallback(() => {
    if (!repository) return;
    setPendingAction({ kind: 'rebuild-index' });
  }, [repository]);

  const handleInvalidateCache = useCallback(() => {
    if (!repository) return;
    setPendingAction({ kind: 'invalidate-cache' });
  }, [repository]);

  const handleToggleOnline = useCallback((nextOnline: boolean) => {
    if (!repository) return;
    setPendingAction({ kind: 'toggle-online', nextOnline });
  }, [repository]);

  const handleBrowseRepository = useCallback(() => {
    if (!repository) return;
    router.stateService.go('preview.browse.browse.repo', { repoName: repository.name });
  }, [repository, router]);

  const cancelPendingAction = useCallback(() => {
    if (isExecutingAction) return;
    setPendingAction(null);
  }, [isExecutingAction]);

  const handleConfirmAction = useCallback(async () => {
    if (!repository || !pendingAction) return;
    const repoName = repository.name;
    setIsExecutingAction(true);
    try {
      if (pendingAction.kind === 'rebuild-index') {
        await rebuildIndex(repoName);
        toast.success(`Repository index rebuild started for "${repoName}"`);
      } else if (pendingAction.kind === 'invalidate-cache') {
        await invalidateCache(repoName);
        toast.success(`Repository caches invalidated for "${repoName}"`);
      } else {
        await setRepositoryOnline(
          repoName,
          repository.format,
          repository.type,
          pendingAction.nextOnline
        );
        toast.success(
          pendingAction.nextOnline
            ? `Repository "${repoName}" is now online`
            : `Repository "${repoName}" is now offline`
        );
        // Refetch so pristineData.online (and the Online checkbox) reflect the
        // saved state. Failure to refetch here would leave the toggle button
        // labelled with the previous state until the user reloads.
        const refreshed = await fetchRepository(repoName);
        if (refreshed) {
          setRepository(refreshed);
        }
      }
      setPendingAction(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      toast.error('Action failed', message);
    } finally {
      setIsExecutingAction(false);
    }
  }, [repository, pendingAction, rebuildIndex, invalidateCache, setRepositoryOnline, fetchRepository, toast]);

  // Dialog copy derived from the pending action — keeps the JSX block lean and
  // ensures the copy stays consistent with the action that's actually about to
  // run.
  const actionDialogProps = useMemo(() => {
    if (!pendingAction || !repository) {
      return { title: '', message: '', confirmLabel: 'Confirm', variant: 'warning' as const };
    }
    const name = repository.name;
    if (pendingAction.kind === 'rebuild-index') {
      return {
        title: 'Rebuild Repository Index',
        message: `Rebuild the search index for repository "${name}"? This may take some time depending on repository size.`,
        confirmLabel: 'Rebuild Index',
        variant: 'warning' as const,
      };
    }
    if (pendingAction.kind === 'invalidate-cache') {
      return {
        title: 'Invalidate Repository Cache',
        message: `Invalidate cached metadata and content for repository "${name}"? Subsequent requests will refetch from the upstream source.`,
        confirmLabel: 'Invalidate Cache',
        variant: 'warning' as const,
      };
    }
    // toggle-online
    const isGoingOffline = !pendingAction.nextOnline;
    return {
      title: isGoingOffline ? 'Disable System Status' : 'Enable System Status',
      message: isGoingOffline
        ? `Take repository "${name}" offline? Clients will no longer be able to read from or write to this repository until it is brought back online.`
        : `Bring repository "${name}" online? Clients will be able to access it again.`,
      confirmLabel: isGoingOffline ? 'Take Offline' : 'Bring Online',
      variant: 'warning' as const,
    };
  }, [pendingAction, repository]);

  const getHeaderProps = () => {
    switch (routeState.viewMode) {
      case 'list': return { icon: Database, title: 'Repositories', description: 'Manage hosted, proxy, and group repositories', actions: canCreate ? <Button variant="solid" onClick={handleCreate}><Plus size={16} /> Create Repository</Button> : undefined };
      case 'select-type': return { icon: Database, title: 'Create Repository', description: 'Configure your new repository settings' };
      case 'create': return { icon: selectedRecipe ? <FormatIcon format={selectedRecipe.format} type={selectedRecipe.type} size={24} /> : Database, title: `Create ${(selectedRecipe?.format || '').replace(/2$/, '')} (${selectedRecipe?.type || ''}) Repository`, description: 'Configure settings' };
      case 'edit': return { icon: repository ? <FormatIcon format={repository.format} type={repository.type} size={24} /> : Database, title: repository ? `Edit ${repository.name}` : 'Details', description: repository ? `${repository.format} • ${repository.type} • ${repository.status?.online ? 'Online' : 'Offline'}` : 'Loading...' };
      default: return { icon: Database, title: 'Repositories', description: 'Manage repositories' };
    }
  };

  const getBreadcrumbs = () => {
    const base = [
      { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
      { label: 'Repositories', onClick: handleBack },
    ];
    switch (routeState.viewMode) {
      case 'list': return [
        { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
        { label: 'Repositories' }
      ];
      case 'select-type': return [...base, { label: 'Create' }];
      case 'create': return [...base, { label: 'Create' }];
      case 'edit': return [...base, { label: repository?.name || 'Loading...' }];
      default: return base;
    }
  };

  const headerProps = getHeaderProps();

  const EVAL_HUB_STATE = 'preview.admin.iqHostedReposEval';
  const showEvalBackLink = routeState.viewMode === 'edit' && routerParams?.tab === 'evaluation';
  const evalHubHref =
    router.stateService.href(EVAL_HUB_STATE, {}) ?? '#preview/admin/iq/hosted-repos-eval';

  return (
    <Box className="repositories-page" data-testid="repositories-page" data-view={routeState.viewMode}>
      <PageHeader icon={headerProps.icon} title={headerProps.title} description={headerProps.description} actions={headerProps.actions}
          breadcrumbs={getBreadcrumbs()}
/>
      {showEvalBackLink && (
        <Box className="repositories-page__eval-back" mb="2">
          <Link
            href={evalHubHref}
            size="2"
            weight="medium"
            data-testid="repositories-page-eval-back-link"
            data-analytics-id="nxrm-repository-eval-back-link"
          >
            <Flex align="center" gap="1" as="span">
              <ArrowLeft size={14} aria-hidden />
              <span>Hosted Repository Evaluation</span>
            </Flex>
          </Link>
        </Box>
      )}
      {error && <Box className="repositories-page__alerts"><SettingsAlert type="error" onClose={() => setError(null)}>{error}</SettingsAlert></Box>}
      <Box className="repositories-page__content">
        {routeState.viewMode === 'list' && <RepositoriesList key={refreshKey} onSelect={handleSelectRepository} onCreate={handleCreate} onDelete={canDelete ? async (n) => { await deleteRepository(n); setRefreshKey(k => k + 1); } : undefined} />}
        {(routeState.viewMode === 'select-type' || routeState.viewMode === 'create') && (
          <WizardForm
            steps={wizardSteps}
            currentStep={createStep}
            onStepChange={handleWizardStepChange}
            onComplete={handleFinalSubmit}
            onCancel={() => {
              setSelectedFormat(null);
              setPostCreateStep(null);
              setFirewallChoice(null);
              setRhcChoice(null);
              handleBack();
            }}
            externalDirtyTracking
            onDiscardConfirm={() => {
              clearDirtyState('repository-form-new');
              setSelectedFormat(null);
              setPostCreateStep(null);
              setFirewallChoice(null);
              setRhcChoice(null);
              handleBack();
            }}
            completeLabel="Create Repository"
            onStepSubmitOverride={(step) =>
              step === 2
                ? () => {
                    repoFormSubmitRef.current?.();
                  }
                : undefined
            }
            dirty={createStep > 2 || (createStep === 2 && isRepoFormDirty)}
            canAdvance={
              createStep === 0
                ? canAdvanceFromStep0
                : createStep === 1
                  ? canAdvanceFromStep1
                  : createStep === 2
                    ? canAdvanceFromStep2
                    : true
            }
            loading={loading && createStep === 2}
            noDirtyTracking={createStep < 2 || (createStep === 2 && !isRepoFormDirty)}
            hideSubmitButton={createStep === 0 || createStep === 1}
            hideStepTitle={createStep === 0 || createStep === 1}
          >
            {createStep === 0 && <RepositoryTypeSelector onSelect={() => {}} onCancel={handleBack} hideActions mode="format" onSelectionChange={handleRecipeSelectionChange} />}
            {createStep === 1 && <RepositoryTypeSelector onSelect={() => {}} onCancel={handleBack} hideActions mode="type" selectedFormat={selectedFormat} onSelectionChange={handleRecipeSelectionChange} />}
            {selectedRecipe && createStep >= 2 && (
              <Box style={{ display: createStep === 2 ? 'block' : 'none' }}>
                <RepositoryForm
                  recipe={selectedRecipe}
                  isCreate
                  onSave={handleSave}
                  onCancel={handleBack}
                  hideActions
                  onSubmitRef={repoFormSubmitRef}
                  advanceOnly
                  onCanAdvanceChange={createStep === 2 ? setCanAdvanceFromStep2 : undefined}
                  onDirtyChange={setIsRepoFormDirty}
                />
              </Box>
            )}
            {isProxyRecipe && postCreateStep === 'firewall' && createStep === 3 && (
              <RepositoryFirewallStep
                mode="deferred"
                value={firewallChoice ?? 'none'}
                onChoice={setFirewallChoice}
                hasFirewallLicense={hasFirewallLicense()}
                format={selectedRecipe?.format}
              />
            )}
            {isProxyRecipe && postCreateStep === 'rhc' && createStep === 4 && (
              <RepositoryRHCStep
                mode="deferred"
                value={rhcChoice ?? 'none'}
                onChoice={setRhcChoice}
                capabilityEnabled={rhcCapabilityEnabled ?? false}
              />
            )}
          </WizardForm>
        )}
        {routeState.viewMode === 'edit' && editRecipe && (
          <RepositoryForm
            repository={repository}
            recipe={editRecipe}
            isCreate={false}
            onSave={handleSave}
            onCancel={handleBack}
            onDelete={canDelete ? handleDelete : undefined}
            onRebuildIndex={canUpdate ? handleRebuildIndex : undefined}
            onInvalidateCache={canUpdate ? handleInvalidateCache : undefined}
            onToggleOnline={canUpdate ? handleToggleOnline : undefined}
            onBrowseRepository={handleBrowseRepository}
            isActionInFlight={isExecutingAction}
            loading={loading}
          />
        )}
      </Box>
      <DeleteConfirmationModal
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleDeleteConfirm}
        entityName={repository?.name || ''}
        entityType="repository"
        loading={isDeleting}
      />
      <ConfirmDialog
        open={pendingAction !== null}
        onOpenChange={(open) => { if (!open) cancelPendingAction(); }}
        title={actionDialogProps.title}
        message={actionDialogProps.message}
        confirmLabel={actionDialogProps.confirmLabel}
        variant={actionDialogProps.variant}
        onConfirm={handleConfirmAction}
      />
    </Box>
  );
}

export default RepositoriesPage;
