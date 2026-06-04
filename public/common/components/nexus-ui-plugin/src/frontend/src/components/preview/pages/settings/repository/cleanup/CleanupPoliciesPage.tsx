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

import React, { useState, useEffect, useCallback } from 'react';
import { Box } from '@radix-ui/themes';
import { Plus, ArrowLeft } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { useToast, LoadingState, PageHeader } from '../../../../shared';
import { SettingsButton, SettingsAlert, ConfirmDialog } from '../../../../shared/form';
import { CleanupPoliciesList } from './CleanupPoliciesList';
import { CleanupPolicyForm } from './CleanupPolicyForm';
import { useCleanupPoliciesApi } from './useCleanupPoliciesApi';
import { CleanupPolicy, CleanupPolicyFormData, FormatCriteria } from './types';

import './CleanupPoliciesPage.scss';

// Base path for cleanup policies URLs
const BASE_PATH = 'preview/admin/repository/cleanup-policies';

/**
 * URL-based routing patterns:
 * - /cleanup-policies           → List page
 * - /cleanup-policies/create    → Create form
 * - /cleanup-policies/{name}    → Edit form
 */
type ViewMode = 'list' | 'create' | 'detail';

interface RouteState {
  viewMode: ViewMode;
  policyName: string | null;
}

/**
 * Parse the URL hash to determine the current route state
 */
function parseRoute(hash: string): RouteState {
  // Remove leading # and any query params (like ?debug)
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');

  const policiesIndex = parts.indexOf('cleanup-policies');
  if (policiesIndex === -1) {
    return { viewMode: 'list', policyName: null };
  }

  const pathAfterPolicies = parts.slice(policiesIndex + 1);

  // /cleanup-policies → list
  if (pathAfterPolicies.length === 0 || pathAfterPolicies[0] === '') {
    return { viewMode: 'list', policyName: null };
  }

  // /cleanup-policies/create → create form
  if (pathAfterPolicies[0] === 'create') {
    return { viewMode: 'create', policyName: null };
  }

  // /cleanup-policies/{name} → detail/edit
  return {
    viewMode: 'detail',
    policyName: decodeURIComponent(pathAfterPolicies[0]),
  };
}

/**
 * Navigate to a new route by updating the URL hash
 */
function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * CleanupPoliciesPage - Main Cleanup Policies management page for Preview UI
 *
 * Uses URL-based routing for testability:
 * - Direct URL access to any view
 * - Browser back/forward support
 * - Bookmarkable URLs
 *
 * URL patterns:
 * - #preview/admin/repository/cleanup-policies           → List
 * - #preview/admin/repository/cleanup-policies/create    → Create form
 * - #preview/admin/repository/cleanup-policies/{name}    → Edit form
 */
export function CleanupPoliciesPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [policy, setPolicy] = useState<CleanupPolicy | null>(null);
  const [formatCriteria, setFormatCriteria] = useState<FormatCriteria[]>([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    fetchCleanupPolicy,
    fetchFormatCriteria,
    deleteCleanupPolicy,
  } = useCleanupPoliciesApi();

  const canCreate = ExtJS.checkPermission('nexus:settings:update');
  const canDelete = ExtJS.checkPermission('nexus:settings:update');

  // Listen for hash changes (browser back/forward, direct URL navigation)
  useEffect(() => {
    const handleHashChange = () => {
      const newState = parseRoute(window.location.hash);
      setRouteState(newState);
      setError(null);
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [setError]);

  // Load format criteria on mount
  useEffect(() => {
    fetchFormatCriteria()
      .then(setFormatCriteria)
      .catch((err) => console.error('Failed to load format criteria:', err));
  }, [fetchFormatCriteria]);

  // Load policy details when editing
  useEffect(() => {
    if (routeState.viewMode === 'detail' && routeState.policyName) {
      fetchCleanupPolicy(routeState.policyName)
        .then((policyData) => {
          if (policyData) {
            setPolicy(policyData);
          } else {
            setError('Cleanup policy not found');
            navigateTo(BASE_PATH);
          }
        })
        .catch((err) => {
          setError(err.message);
          navigateTo(BASE_PATH);
        });
    } else {
      setPolicy(null);
    }
  }, [routeState.viewMode, routeState.policyName, fetchCleanupPolicy, setError]);

  // Navigation handlers - update URL instead of local state
  const handleSelectPolicy = useCallback((name: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(name)}`);
  }, []);

  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  // Called by form after successful save - form's useCleanupPolicyForm handles API call + toast
  const handleSave = useCallback(async (_data: CleanupPolicyFormData) => {
    setRefreshKey((k) => k + 1);
  }, []);

  const handleDelete = useCallback(() => {
    setDeleteDialogOpen(true);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!policy) return;

    try {
      await deleteCleanupPolicy(policy.name);
      toast.success(`Cleanup policy "${policy.name}" deleted successfully`);
      setRefreshKey((k) => k + 1);
      setRouteState({ viewMode: 'list', policyName: null });
      setDeleteDialogOpen(false);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Operation failed';
      toast.error(message);
    }
  }, [policy, deleteCleanupPolicy, toast]);

  // Render header based on view mode
  const renderHeader = () => {
    if (routeState.viewMode === 'list') {
      return (
        <PageHeader
          title="Cleanup Policies"
          description="Manage component removal configuration"
          actions={canCreate && (
            <SettingsButton
              variant="primary"
              onClick={handleCreate}
              icon={Plus}
              testId="create-policy-button"
              data-analytics-id="nxrm-cleanup-policy-create"
            >
              Create Cleanup Policy
            </SettingsButton>
          )}
        />
      );
    }

    const title = routeState.viewMode === 'create'
      ? 'Create Cleanup Policy'
      : policy
        ? `Edit ${policy.name}`
        : 'Cleanup Policy Details';

    const description = policy && routeState.viewMode === 'detail'
      ? `Format: ${policy.format}`
      : undefined;

    return (
      <PageHeader
        title={title}
        description={description}
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Cleanup Policies' }
        ]}
      />
    );
  };

  // Show loading when in detail mode but policy not yet loaded
  if (routeState.viewMode === 'detail' && routeState.policyName && !policy) {
    return (
      <Box
        className="cleanup-policies-page"
        data-testid="cleanup-policies-page"
        data-view="loading"
      >
        {renderHeader()}
        <Box p="4">
          <LoadingState message="Loading cleanup policy details..." />
        </Box>
      </Box>
    );
  }

  return (
    <Box
      className="cleanup-policies-page"
      data-testid="cleanup-policies-page"
      data-view={routeState.viewMode}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      <Box mb="4">
        {renderHeader()}
      </Box>

      {/* Error Alert */}
      {error && (
        <Box className="cleanup-policies-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="cleanup-policies-page__content">
        {routeState.viewMode === 'list' && (
          <CleanupPoliciesList
            key={refreshKey}
            onSelect={handleSelectPolicy}
            onCreate={handleCreate}
          />
        )}

        {routeState.viewMode === 'create' && (
          <CleanupPolicyForm
            isCreate={true}
            formatCriteria={formatCriteria}
            onSave={handleSave}
            onCancel={handleBack}
            loading={loading}
          />
        )}

        {routeState.viewMode === 'detail' && policy && (
          <CleanupPolicyForm
            policy={policy}
            isCreate={false}
            formatCriteria={formatCriteria}
            canDelete={canDelete}
            onSave={handleSave}
            onDelete={handleDelete}
            onCancel={handleBack}
            loading={loading}
          />
        )}
      </Box>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        testId="delete-cleanup-policy-dialog"
        onOpenChange={setDeleteDialogOpen}
        title="Delete Cleanup Policy?"
        message={
          policy && policy.inUseCount > 0
            ? `This Cleanup Policy is used by ${policy.inUseCount} ${policy.inUseCount === 1 ? 'repository' : 'repositories'}. Repositories using this policy will no longer automatically clean up assets. This action cannot be undone.`
            : 'Repositories using this policy will no longer automatically clean up assets. This action cannot be undone.'
        }
        entityName={policy?.name}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
}

export default CleanupPoliciesPage;
