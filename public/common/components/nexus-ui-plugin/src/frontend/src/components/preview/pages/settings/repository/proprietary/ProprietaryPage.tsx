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
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Box, Text, } from '@radix-ui/themes';
import { Lock, } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  PageHeader,
  LoadingState,
  ErrorState,
  HelpSection,
  useToast,
} from '../../../../shared';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsTransferList,
} from '../../../../shared/form';
import { useProprietaryApi } from './useProprietaryApi';
import { RepositoryReference } from './types';

import './ProprietaryPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * ProprietaryPage - Proprietary Repositories settings page for Preview UI
 *
 * Allows administrators to mark hosted repositories as containing proprietary components.
 */
export function ProprietaryPage() {
  const [possibleRepos, setPossibleRepos] = useState<RepositoryReference[]>([]);
  const [enabledRepos, setEnabledRepos] = useState<string[]>([]);
  const [initialEnabledRepos, setInitialEnabledRepos] = useState<string[]>([]);
  const [loadingData, setLoadingData] = useState(true);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    fetchSettings,
    fetchPossibleRepositories,
    updateSettings,
  } = useProprietaryApi();

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');

  // Load data on mount
  const loadData = useCallback(async () => {
    setLoadingData(true);
    setError(null);
    try {
      const [settings, repos] = await Promise.all([
        fetchSettings(),
        fetchPossibleRepositories(),
      ]);
      setPossibleRepos(repos);
      setEnabledRepos(settings.enabledRepositories);
      setInitialEnabledRepos(settings.enabledRepositories);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoadingData(false);
    }
  }, [fetchSettings, fetchPossibleRepositories, setError]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Check if form has changes
  const isDirty = useMemo(() => {
    if (enabledRepos.length !== initialEnabledRepos.length) return true;
    const sortedCurrent = [...enabledRepos].sort();
    const sortedInitial = [...initialEnabledRepos].sort();
    return sortedCurrent.some((repo, idx) => repo !== sortedInitial[idx]);
  }, [enabledRepos, initialEnabledRepos]);

  // Convert to transfer list items - use 'name' as that's what SettingsTransferList expects
  const availableItems = useMemo(() => {
    return possibleRepos.map(repo => ({
      id: repo.id,
      name: repo.name,
    }));
  }, [possibleRepos]);

  // Convert selected IDs to full objects for consistency with SettingsTransferList
  const selectedItemObjects = useMemo(() => {
    return enabledRepos.map(id => {
      const repo = possibleRepos.find(r => r.id === id);
      return { id, name: repo?.name || id };
    });
  }, [enabledRepos, possibleRepos]);

  // Handle transfer list changes - extract IDs from the selected items
  const handleTransferChange = useCallback((selectedItems: Array<{ id: string; name: string }>) => {
    const ids = selectedItems.map(item => item.id);
    setEnabledRepos(ids);
  }, []);

  const handleSave = useCallback(async () => {
      const result = await updateSettings(enabledRepos);
      setInitialEnabledRepos(result.enabledRepositories);
      setEnabledRepos(result.enabledRepositories);
      toast.success('Proprietary repositories settings saved successfully');
  }, [enabledRepos, updateSettings, toast]);

  const handleDiscard = useCallback(() => {
    setEnabledRepos(initialEnabledRepos);
  }, [initialEnabledRepos]);

  // Read-only view for users without update permission
  const renderReadOnly = () => (
    <Box className="proprietary-page__read-only">
      <Text size="2" className="proprietary-page__read-only-label">
        Enabled Proprietary Repositories
      </Text>
      {enabledRepos.length > 0 ? (
        <Box className="proprietary-page__read-only-list">
          {enabledRepos.map(repo => (
            <Text key={repo} size="2" className="proprietary-page__read-only-item">
              {repo}
            </Text>
          ))}
        </Box>
      ) : (
        <Text size="2" className="proprietary-page__read-only-empty">
          No proprietary repositories configured
        </Text>
      )}
    </Box>
  );

  // Show loading state
  if (loadingData) {
    return (
      <Box className="proprietary-page">
        <PageHeader
          icon={Lock}
          title="Proprietary Repositories"
          description="Configure which repositories contain proprietary components"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Proprietary Repositories' }
          ]}
        />
        <LoadingState message="Loading settings..." />
      </Box>
    );
  }

  // Show error state with retry
  if (error && !possibleRepos.length) {
    return (
      <Box className="proprietary-page">
        <PageHeader
          icon={Lock}
          title="Proprietary Repositories"
          description="Configure which repositories contain proprietary components"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Proprietary Repositories' }
          ]}
        />
        <ErrorState
          title="Failed to Load"
          message={error}
          onRetry={loadData}
        />
      </Box>
    );
  }

  return (
    <Box className="proprietary-page">
      <PageHeader
        icon={Lock}
        title="Proprietary Repositories"
        description="Configure which repositories contain proprietary components"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Proprietary Repositories' }
        ]}
      />

      {/* Error Alert */}
      {error && (
        <Box className="proprietary-page__alerts">
          <ErrorState
            variant="inline"
            title="Error"
            message={error}
            onRetry={() => setError(null)}
            retryText="Dismiss"
          />
        </Box>
      )}

      {/* Content */}
      <Box className="proprietary-page__content">
        <SettingsForm 
          testId="proprietary-form"
          onSubmit={handleSave} 
          onCancel={handleDiscard} 
          showHeader={false} 
          dirty={isDirty}
          cancelDisabled={!isDirty}
        >
          {/* Help Section - Using standard HelpSection component */}
          <HelpSection
            title="About Proprietary Components"
            content="Marking a repository as containing proprietary components enables enhanced firewall and policy enforcement for components in that repository. This is useful for repositories that contain your organization's internal or licensed components."
            docLink={{
              label: "Learn more",
              href: "https://help.sonatype.com/en/proprietary-component-configuration.html",
            }}
          />

          <SettingsFormSection title="Repository Selection">
            {canUpdate ? (
              <SettingsTransferList
                name="proprietaryRepositories"
                testId="proprietary-repos"
                availableItems={availableItems}
                selectedItems={selectedItemObjects}
                onChange={handleTransferChange}
                availableLabel="Generic Hosted Repositories"
                selectedLabel="Proprietary Hosted Repositories"
                disabled={loading}
              />
            ) : (
              renderReadOnly()
            )}
          </SettingsFormSection>

          {/* Actions are in the sticky header bar via SettingsForm */}
        </SettingsForm>
      </Box>
    </Box>
  );
}

export default ProprietaryPage;
