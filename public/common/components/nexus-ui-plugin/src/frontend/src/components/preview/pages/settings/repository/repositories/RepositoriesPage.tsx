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

import { ErrorState, PageHeader, type PageHeaderProps, useToast } from '../../../../shared';
import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Box, Button, Flex, Spinner, Text as RadixText } from '@radix-ui/themes';
import { ArrowLeft, ChevronRight, Database, Plus } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsAlert, SettingsButton, WizardForm } from '../../../../shared/form';

import { DeleteConfirmationModal } from '../../../../shared/modals/DeleteConfirmationModal';
import { RepositoriesList } from './RepositoriesList';
import { RepositoryTypeSelector } from './RepositoryTypeSelector';
import { RepositoryForm } from './RepositoryForm';
import { RepositoryFirewallStep } from './RepositoryFirewallStep';
import { RepositoryRHCStep } from './RepositoryRHCStep';
import { FormatIcon } from '../repositories/components/FormatIcon';
import {
  enableFirewallAudit,
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
  const [fetchingRepository, setFetchingRepository] = useState(false);

  const toast = useToast();
  const {
    loading,
    error,
    setError,
    fetchRepository,
    createRepository,
    updateRepository,
    deleteRepository,
    enableHealthCheck,
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
  const handleBackToTypeSelect = useCallback(() => navigateTo(`${BASE_PATH}/create`), []);

  const [canAdvanceFromStep0, setCanAdvanceFromStep0] = useState(false);
  const [canAdvanceFromStep1, setCanAdvanceFromStep1] = useState(false);
  const [canAdvanceFromStep2, setCanAdvanceFromStep2] = useState(false);
  const [pendingRecipe, setPendingRecipe] = useState<Recipe | null>(null);
  const [postCreateStep, setPostCreateStep] = useState<'firewall' | 'rhc' | null>(null);
  const [firewallChoice, setFirewallChoice] = useState<'none' | 'audit' | 'quarantine' | null>(null);
  const [rhcChoice, setRhcChoice] = useState<'enable' | 'none' | null>(null);
  const savedFormDataRef = useRef<RepositoryFormData | null>(null);

  const isProxyRecipe = selectedRecipe?.type === 'proxy';
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
      setFirewallChoice(null);
      setRhcChoice(null);
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
      if (pendingRecipe && pendingRecipe.type) {
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
        if (hasFw && (firewallChoice === 'audit' || firewallChoice === 'quarantine')) {
          try {
            if (firewallChoice === 'audit') await enableFirewallAudit(repoName);
            else await enableFirewallQuarantine(repoName);
          } catch {
            // Firewall enable is best-effort during create; admin can configure from repo edit
          }
        }
        if (rhcChoice === 'enable') {
          try {
            await enableHealthCheck(repoName);
          } catch {
            // Health Check enable is best-effort; analysis will run on next scheduled task
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
  }, [postCreateStep, firewallChoice, rhcChoice, createRepository, enableHealthCheck, finishCreate, toast]);

  const handleRecipeSelectionChange = useCallback((canAdvance: boolean, recipe: Recipe | null) => {
    if (recipe && !recipe.type) {
      // Format selected - auto-advance to type selection
      setCanAdvanceFromStep0(true);
      setSelectedFormat(recipe.format);
      setPendingRecipe(recipe);
      setInternalWizardStep(1);
    } else if (recipe && recipe.type) {
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
      finishCreate();
      return { skipNavigate: true };
    },
    [selectedRecipe, routeState.viewMode, postCreateStep, finishCreate, toast]
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

  const getHeaderProps = () => {
    switch (routeState.viewMode) {
      case 'list': return { icon: Database, title: 'Repositories', description: 'Manage hosted, proxy, and group repositories', actions: canCreate ? <Button variant="solid" onClick={handleCreate}><Plus size={16} /> Create Repository</Button> : undefined };
      case 'select-type': return { icon: Database, title: 'Create Repository', description: 'Configure your new repository settings' };
      case 'create': return { icon: selectedRecipe ? <FormatIcon format={selectedRecipe.format} type={selectedRecipe.type} size={24} /> : Database, title: `Create ${(selectedRecipe?.format || '').replace(/2$/, '')} (${selectedRecipe?.type || ''}) Repository`, description: 'Configure settings' };
      case 'edit': return { icon: repository ? <FormatIcon format={repository.format} type={repository.type} size={24} /> : Database, title: repository ? `Edit ${repository.name}` : 'Details', description: repository ? `${repository.format} • ${repository.type} • ${repository.status?.online ? 'Online' : 'Offline'}` : 'Loading...', actions: <Button variant="ghost" onClick={handleBack}><ArrowLeft size={16} /> Back</Button> };
      default: return { icon: Database, title: 'Repositories', description: 'Manage repositories' };
    }
  };

  const headerProps = getHeaderProps();

  return (
    <Box className="repositories-page" data-testid="repositories-page" data-view={routeState.viewMode}>
      <PageHeader icon={headerProps.icon} title={headerProps.title} description={headerProps.description} actions={headerProps.actions} 
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Repositories' }
          ]}
/>
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
            completeLabel="Create Repository"
            onStepSubmitOverride={(step) =>
              step === 2
                ? () => {
                    repoFormSubmitRef.current?.();
                  }
                : undefined
            }
            dirty={createStep >= 2}
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
            noDirtyTracking={createStep < 2}
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
                  advanceOnly={isProxyRecipe && createStep === 2}
                  onCanAdvanceChange={createStep === 2 ? setCanAdvanceFromStep2 : undefined}
                />
              </Box>
            )}
            {isProxyRecipe && postCreateStep === 'firewall' && createStep === 3 && (
              <RepositoryFirewallStep
                mode="deferred"
                value={firewallChoice ?? 'none'}
                onChoice={setFirewallChoice}
                hasFirewallLicense={hasFirewallLicense()}
              />
            )}
            {isProxyRecipe && postCreateStep === 'rhc' && createStep === 4 && (
              <RepositoryRHCStep
                mode="deferred"
                value={rhcChoice ?? 'none'}
                onChoice={setRhcChoice}
              />
            )}
          </WizardForm>
        )}
        {routeState.viewMode === 'edit' && editRecipe && <RepositoryForm repository={repository} recipe={editRecipe} isCreate={false} onSave={handleSave} onCancel={handleBack} onDelete={canDelete ? handleDelete : undefined} loading={loading} />}
      </Box>
      <DeleteConfirmationModal
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleDeleteConfirm}
        entityName={repository?.name || ''}
        entityType="repository"
        loading={isDeleting}
      />
    </Box>
  );
}

export default RepositoriesPage;
