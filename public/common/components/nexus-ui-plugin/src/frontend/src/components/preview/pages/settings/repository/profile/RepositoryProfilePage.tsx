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

import React, { useState, useCallback, useEffect, useMemo } from 'react';
import { Box, Flex, Text, Tabs, Button, Separator, Heading, Spinner, ScrollArea, Grid, Badge, Callout, Tooltip } from '@radix-ui/themes';
import { useRouter, useCurrentStateAndParams } from '@uirouter/react';
import {
  RefreshCw,
  FolderTree,
  Package,
  Circle,
  RotateCcw,
  Database,
  Power,
  Trash2,
} from 'lucide-react';

import {
  DeleteConfirmationModal,
} from '../../../../shared';
import { useRepositoriesApi } from '../repositories/useRepositoriesApi';
import { ensureTrailingSlash } from '../../../../../../utils/url';
import { FORMAT_LABELS, TYPE_LABELS } from '../repositories/types';
import { Breadcrumbs } from '../../../search/details/Breadcrumbs';
import { RepositoryStructureTree } from '../repositories/RepositoryStructureTree';
import { RepositoryGroupUsageTab } from '../repositories/RepositoryGroupUsageTab';
import { RepositoryTab } from './tabs/RepositoryTab';
import { AccessSecurityTab } from './tabs/AccessSecurityTab';
import { SystemTab } from './tabs/SystemTab';
import { UsageTab } from './tabs/UsageTab';
import { RepositoryAuditTab } from './tabs/RepositoryAuditTab';
import { InstanceConfigTab } from './tabs/InstanceConfigTab';
import { HealthCheckCard } from './HealthCheckCard';
import { FirewallCard } from './FirewallCard';
import { useFirewallTier } from '../../../../shared/security/firewallTier';
import { useToast } from '../../../../shared/Toast';
import { ConfirmDialog } from '../../../../shared/ConfirmDialog';
import { useRepositoryProfileMachine } from './useRepositoryProfileMachine';
import { TABS, type TabId } from './types';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';

// =============================================================================
// Types
// =============================================================================

interface RepositoryProfilePageProps {
  repositoryName: string;
  /** Context determines back navigation: 'browse' goes to Browse tree, 'settings' goes to repo list */
  context?: 'browse' | 'settings';
}

// =============================================================================
// Component
// =============================================================================

/**
 * RepositoryProfilePage - Read-only operational dashboard for a repository
 *
 * This component follows the three-layer architecture:
 * - Layer 1: repositoryProfileMachine.ts - XState machine for business logic
 * - Layer 2: useRepositoryProfileMachine.ts - Hook for React integration
 * - Layer 3: This component - Pure presentation only
 */
export function RepositoryProfilePage({ repositoryName, context = 'settings' }: RepositoryProfilePageProps): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();
  const initialTab = (params?.tab as TabId) || 'repository';
  const [activeTab, setActiveTab] = useState<TabId>(initialTab);
  const _firewallTier = useFirewallTier();
  const toast = useToast();

  // Delete-modal state. We use the list page's DeleteConfirmationModal directly
  // (rather than threading through the state machine) so the profile page owns
  // its own ephemeral UI without coupling to repositoryProfileMachine — which
  // is currently scoped to per-repo actions (cache, index, online, health check).
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState<boolean>(false);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);
  const { deleteRepository } = useRepositoriesApi();

  // Get all state and handlers from the machine hook
  const {
    repository,
    blobStore,
    cleanupPolicies,
    routingRule,
    healthCheck,
    firewall,
    malwareCleanupSummary,
    metrics,
    privileges,
    roles,
    users,
    anonymousAccess,
    tasks,
    capabilities,
    iqCapabilities,
    httpSettings,
    loading,
    securityLoading,
    systemLoading,
    error,
    actionError,
    refresh,
    retry,
    handleInvalidateCache,
    handleRebuildIndex,
    handleToggleOnline,
    handleToggleHealthCheck,
    handleToggleInstanceHealthCheck,
    confirmAction,
    cancelAction,
    isConfirming,
    isExecuting,
    pendingAction,
    dialogTitle,
    dialogMessage,
    dialogConfirmLabel,
    dialogVariant,
  } = useRepositoryProfileMachine(repositoryName);

  // Hide mutating actions for users without the matching repository-admin
  // permission (NEXUS-54212). Edit gates cache/index/online; delete gates removal.
  // coreui never mounts a <PermissionsProvider>, so context usePermission returns false for
  // everyone; use the provider-independent ExtJS.usePermission.
  const hasUser = ExtJS.useUser() ?? false;
  const canEditRepository = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.REPOSITORY_ADMIN.EDIT),
    [hasUser],
  );
  const canDeleteRepository = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.REPOSITORY_ADMIN.DELETE),
    [hasUser],
  );

  // Context-aware back navigation
  const isBrowseContext = context === 'browse';
  const _backLabel = isBrowseContext ? 'Back to Browse' : 'Back to Repositories';

  // Navigation handlers — defined before any useEffect that references them to avoid TDZ errors
  const handleBack = useCallback(() => {
    if (isBrowseContext) {
      router.stateService.go('preview.browse.browse');
    } else {
      router.stateService.go('preview.admin.repository.repositories.list');
    }
  }, [isBrowseContext, router]);

  const handleSelectTab = useCallback((tab: TabId) => {
    setActiveTab(tab);
    const stateName = context === 'browse' ? 'preview.browse.repository-profile' : 'preview.admin.repository.repositories.profile';
    const tabParams = { repositoryName, tab };
    router.stateService.go(stateName, tabParams, { notify: false, location: 'replace' });
  }, [router, repositoryName, context]);

  const handleViewInBrowse = useCallback(() => {
    router.stateService.go('preview.browse.browse.repo', { repoName: repositoryName });
  }, [router, repositoryName]);

  const handleOpenDeleteModal = useCallback(() => {
    setIsDeleteModalOpen(true);
  }, []);

  const handleConfirmDelete = useCallback(async () => {
    setIsDeleting(true);
    try {
      await deleteRepository(repositoryName);
      toast.success(`Repository "${repositoryName}" deleted successfully`);
      setIsDeleteModalOpen(false);
      // After a successful delete the repo no longer exists, so navigate back
      // to the list (settings context) or browse view (browse context).
      handleBack();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      toast.error('Failed to delete repository', message);
    } finally {
      setIsDeleting(false);
    }
  }, [deleteRepository, repositoryName, toast, handleBack]);

  // Breadcrumb items based on context
  const breadcrumbItems = useMemo(() => {
    if (isBrowseContext) {
      return [
        { label: 'Browse', onClick: handleBack },
        { label: repositoryName }
      ];
    } else {
      return [
        { label: 'Settings', onClick: () => router.stateService.go('preview.admin') },
        { label: 'Repositories', onClick: handleBack },
        { label: repositoryName }
      ];
    }
  }, [isBrowseContext, repositoryName, handleBack, router]);

  // Sync tab state with URL on initial load if tab param is missing
  useEffect(() => {
    if (!params?.tab) {
      handleSelectTab('repository');
    }
  }, [params?.tab, handleSelectTab]);

  // Surface action errors (invalidate cache / rebuild index / toggle online failures) via toast
  useEffect(() => {
    if (actionError) {
      toast.error('Action failed', actionError);
    }
  }, [actionError, toast]);

  // Loading state
  if (loading) {
    return (
      <Box p="6">
        <Flex justify="center" align="center" style={{ minHeight: '400px' }}>
          <Flex direction="column" align="center" gap="3">
            <Spinner size="3" />
            <Text color="gray">{`Loading ${repositoryName}...`}</Text>
          </Flex>
        </Flex>
      </Box>
    );
  }

  // Error state
  if (error || !repository) {
    return (
      <Box p="6">
        <Breadcrumbs items={breadcrumbItems} />
        <Callout.Root color="red">
          <Callout.Icon>
            <Package size={16} />
          </Callout.Icon>
          <Callout.Text>{error || `Repository "${repositoryName}" not found`}</Callout.Text>
        </Callout.Root>
        <Box mt="4">
          <Button onClick={retry}>Retry</Button>
        </Box>
      </Box>
    );
  }

  const isOnline = repository.status?.online ?? repository.online ?? true;
  const isGroup = repository.type === 'group';
  const isProxy = repository.type === 'proxy';

  // Filter tabs based on repository type
  const visibleTabs = TABS.filter(tab => {
    if (tab.id === 'structure') return isGroup;
    if (tab.id === 'membership') return !isGroup;
    return true;
  });

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box px="4" pt="0" pb="6">
        <Box maxWidth="1280px" mx="auto" width="100%" pb="9">
          {/* Breadcrumbs */}
          <Breadcrumbs items={breadcrumbItems} />

          {/* Repository Header */}
          <Box p="4" mb="4">
            <Grid columns={{ initial: '1', md: '1fr auto' }} gap="6" align="start">
              <Flex direction="column" gap="3">
                <Heading size="6">
                  {repository.name}
                </Heading>
                <Flex align="center" gap="3" wrap="wrap">
                  {/* Status Badge */}
                  <Tooltip content={`Repository is ${isOnline ? 'online' : 'offline'}`}>
                    <Badge variant="soft" color="gray" size="2">
                      <Flex align="center" gap="1">
                        <Circle
                          size={8}
                          fill={isOnline ? 'var(--green-9)' : 'var(--gray-9)'}
                          color={isOnline ? 'var(--green-9)' : 'var(--gray-9)'}
                        />
                        {isOnline ? 'Online' : 'Offline'}
                      </Flex>
                    </Badge>
                  </Tooltip>

                  {/* Type Badge */}
                  <Tooltip content={`Repository type: ${TYPE_LABELS[repository.type] || repository.type}`}>
                    <Badge variant="soft" color="gray" size="2">
                      {TYPE_LABELS[repository.type] || repository.type}
                    </Badge>
                  </Tooltip>

                  {/* Format Badge */}
                  <Tooltip content={`Format: ${FORMAT_LABELS[repository.format] || repository.format}`}>
                    <Badge variant="soft" color="gray" size="2">
                      {FORMAT_LABELS[repository.format] || repository.format}
                    </Badge>
                  </Tooltip>

                  <Separator orientation="vertical" />
                  <Text size="2" color="gray">
                    URL: <code style={{
                      fontFamily: 'var(--font-mono, monospace)',
                      fontSize: '12px',
                      padding: '2px 6px',
                      background: 'var(--gray-a3)',
                      borderRadius: '4px'
                    }}>{ensureTrailingSlash(repository.url)}</code>
                  </Text>
                </Flex>
              </Flex>
              <Flex gap="2" align="center" wrap="wrap">
                <Button variant="soft" size="2" onClick={handleViewInBrowse}>
                  <FolderTree size={16} />
                  Browse Repository
                </Button>
                {canEditRepository && isProxy && (
                  <Button
                    variant="soft"
                    size="2"
                    onClick={handleInvalidateCache}
                    disabled={isExecuting || isDeleting}
                    title="Invalidate Cache"
                  >
                    <RotateCcw size={16} />
                    Invalidate Cache
                  </Button>
                )}
                {canEditRepository && (
                  <Button
                    variant="soft"
                    size="2"
                    onClick={handleRebuildIndex}
                    disabled={isExecuting || isDeleting}
                    title="Rebuild Index"
                  >
                    <Database size={16} />
                    Rebuild Index
                  </Button>
                )}
                {canEditRepository && (
                  <Button
                    variant="soft"
                    size="2"
                    onClick={handleToggleOnline}
                    disabled={isExecuting || isDeleting}
                    color={isOnline ? 'green' : 'red'}
                    title={isOnline ? 'Take Offline' : 'Bring Online'}
                  >
                    <Power size={16} />
                    {isOnline ? 'Online' : 'Offline'}
                  </Button>
                )}
                <Button
                  variant="soft"
                  size="2"
                  color="red"
                  onClick={handleOpenDeleteModal}
                  disabled={!canDeleteRepository || isExecuting || isDeleting}
                  title="Delete Repository"
                  data-testid="repository-profile-delete-button"
                >
                  <Trash2 size={16} />
                  Delete
                </Button>
                <Button variant="ghost" size="2" onClick={refresh} title="Refresh">
                  <RefreshCw size={16} />
                </Button>
              </Flex>
            </Grid>
          </Box>

          {/* Interactive Cards (Focus Section) - Only for Proxy repositories */}
          {isProxy && (
            <Box px="4" mb="6">
              <Grid columns={{ initial: '1', md: '2' }} gap="4">
                <HealthCheckCard
                  repositoryName={repository.name}
                  healthCheck={healthCheck}
                  capabilities={capabilities}
                  onToggleRepo={handleToggleHealthCheck}
                  onToggleInstance={handleToggleInstanceHealthCheck}
                  isSupported={isProxy}
                />
                <FirewallCard
                  repositoryName={repository.name}
                  firewall={firewall}
                  malwareCleanupSummary={malwareCleanupSummary}
                  iqCapabilities={iqCapabilities}
                  isSupported={isProxy}
                  refresh={refresh}
                  onSelectTab={(tab) => handleSelectTab(tab as TabId)}
                />
              </Grid>
            </Box>
          )}

          {/* Tabs */}
          <Tabs.Root value={activeTab} onValueChange={(v) => handleSelectTab(v as TabId)}>
            <Box p="4" pt="2" pb="2">
              <Tabs.List size="2" style={{ borderBottom: 'none' }}>
                {visibleTabs.map((tab) => (
                  <Tabs.Trigger key={tab.id} value={tab.id}>
                    {tab.label}
                  </Tabs.Trigger>
                ))}
              </Tabs.List>
            </Box>

            <Box p="4">
              <Tabs.Content value="repository">
                <RepositoryTab
                  repository={repository}
                  blobStore={blobStore}
                  cleanupPolicies={cleanupPolicies}
                  routingRule={routingRule}
                />
              </Tabs.Content>

              <Tabs.Content value="structure">
                <RepositoryStructureTree repositoryName={repository.name} />
              </Tabs.Content>

              <Tabs.Content value="membership">
                <RepositoryGroupUsageTab repositoryName={repository.name} />
              </Tabs.Content>

              <Tabs.Content value="usage">
                <UsageTab
                  repository={repository}
                  metrics={metrics}
                  healthCheck={healthCheck}
                  firewall={firewall}
                  malwareCleanupSummary={malwareCleanupSummary}
                  blobStore={blobStore}
                />
              </Tabs.Content>

              <Tabs.Content value="audit">
                <RepositoryAuditTab
                  repositoryName={repository.name}
                />
              </Tabs.Content>

              <Tabs.Content value="security">
                <AccessSecurityTab
                  repository={repository}
                  privileges={privileges}
                  roles={roles}
                  users={users}
                  anonymousAccess={anonymousAccess}
                  loading={securityLoading}
                />
              </Tabs.Content>

              <Tabs.Content value="system">
                <SystemTab
                  repository={repository}
                  tasks={tasks}
                  capabilities={capabilities}
                  httpSettings={httpSettings}
                  loading={systemLoading}
                />
              </Tabs.Content>

              <Tabs.Content value="instance-config">
                <InstanceConfigTab
                  iqCapabilities={iqCapabilities}
                  capabilities={capabilities}
                />
              </Tabs.Content>
            </Box>
          </Tabs.Root>
        </Box>
      </Box>

      {/* Confirmation Dialog - rendered based on machine state */}
      <ConfirmDialog
        open={isConfirming}
        onOpenChange={(open) => { if (!open) cancelAction(); }}
        title={dialogTitle}
        message={dialogMessage}
        confirmLabel={dialogConfirmLabel}
        variant={dialogVariant}
        onConfirm={confirmAction}
      />

      {/* Delete confirmation modal — owned by the profile page (not the machine) */}
      <DeleteConfirmationModal
        open={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        entityName={repository.name}
        entityType="repository"
        loading={isDeleting}
      />
    </ScrollArea>
  );
}

export default RepositoryProfilePage;
