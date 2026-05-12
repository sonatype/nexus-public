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
import { Box, Flex, Text, Tabs, Card, Button, Separator, Heading, Spinner, ScrollArea, Grid, Badge, Callout, Tooltip } from '@radix-ui/themes';
import { useRouter, useCurrentStateAndParams } from '@uirouter/react';
import {
  RefreshCw,
  FolderTree,
  Package,
  Circle,
} from 'lucide-react';

import {
  PageHeader,
  ErrorState,
} from '../../../../shared';
import { FORMAT_LABELS, TYPE_LABELS } from '@/components/super/settings/repository/repositories/types';
import { Breadcrumbs } from '@/components/super/search/details/Breadcrumbs';
import { useRepositoryProfile } from './hooks/useRepositoryProfile';
import { RepositoryStructureTree } from '@/components/super/settings/repository/repositories/RepositoryStructureTree';
import { RepositoryGroupUsageTab } from '@/components/super/settings/repository/repositories/RepositoryGroupUsageTab';
import { RepositoryTab } from './tabs/RepositoryTab';
import { AccessSecurityTab } from './tabs/AccessSecurityTab';
import { SystemTab } from './tabs/SystemTab';
import { UsageTab } from './tabs/UsageTab';
import { RepositoryAuditTab } from './tabs/RepositoryAuditTab';
import { InstanceConfigTab } from './tabs/InstanceConfigTab';
import { HealthCheckCard } from './HealthCheckCard';
import { FirewallCard } from './FirewallCard';
import { useFirewallTier } from '@/components/shared/security/firewallTier';
import { restClient, ENDPOINTS } from '@/utils/api';
import { useToast } from '../../../../shared/Toast';

// =============================================================================
// Types
// =============================================================================

interface RepositoryProfilePageProps {
  repositoryName: string;
  /** Context determines back navigation: 'browse' goes to Browse tree, 'settings' goes to repo list */
  context?: 'browse' | 'settings';
}

// Simplified tab structure
type TabId = 'repository' | 'structure' | 'membership' | 'usage' | 'audit' | 'security' | 'system' | 'instance-config';

const TABS: Array<{ id: TabId; label: string }> = [
  { id: 'repository', label: 'Repository' },
  { id: 'structure', label: 'Structure' },
  { id: 'membership', label: 'Group Membership' },
  { id: 'usage', label: 'Usage' },
  { id: 'audit', label: 'Audit' },
  { id: 'security', label: 'Access & Security' },
  { id: 'system', label: 'System' },
  { id: 'instance-config', label: 'Instance Config' },
];

// =============================================================================
// Component
// =============================================================================

/**
 * RepositoryProfilePage - Read-only operational dashboard for a repository
 */
export function RepositoryProfilePage({ repositoryName, context = 'settings' }: RepositoryProfilePageProps): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();
  const initialTab = (params?.tab as TabId) || 'repository';
  const [activeTab, setActiveTab] = useState<TabId>(initialTab);
  const firewallTier = useFirewallTier();
  const toast = useToast();

  // Sync tab state with URL on initial load if tab param is missing
  useEffect(() => {
    if (!params?.tab) {
      handleSelectTab('repository');
    }
  }, [params?.tab, handleSelectTab]);

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
    refresh,
  } = useRepositoryProfile(repositoryName);

  // Handlers for Health Check Card
  const handleToggleRepoHealthCheck = useCallback(async (enabled: boolean) => {
    try {
      if (enabled) {
        await restClient.post(`/service/rest/v1/repositories/${encodeURIComponent(repositoryName)}/health-check`);
      } else {
        await restClient.delete(`/service/rest/v1/repositories/${encodeURIComponent(repositoryName)}/health-check`);
      }
      toast.success(`Health Check ${enabled ? 'enabled' : 'disabled'} for ${repositoryName}`);
      refresh();
    } catch (err) {
      toast.error(`Failed to update Health Check: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  }, [repositoryName, refresh, toast]);

  const handleToggleInstanceHealthCheck = useCallback(async (enabled: boolean, useTrustStore: boolean) => {
    try {
      // Find the healthcheck capability if it exists
      const healthCheckCapability = capabilities.find(c => c.type === 'healthcheck');
      
      if (healthCheckCapability) {
        await restClient.put(`${ENDPOINTS.CAPABILITIES}/${healthCheckCapability.id}`, {
          enabled,
          properties: {
            ...healthCheckCapability.properties,
            useTrustStore: String(useTrustStore),
          }
        });
      } else if (enabled) {
        await restClient.post(ENDPOINTS.CAPABILITIES, {
          type: 'healthcheck',
          enabled: true,
          properties: {
            configuredForAll: 'true',
            useTrustStore: String(useTrustStore),
          }
        });
      }
      toast.success(`Instance Health Check ${enabled ? 'enabled' : 'disabled'}`);
      refresh();
    } catch (err) {
      toast.error('Failed to update Instance Health Check');
    }
  }, [capabilities, refresh, toast]);

  // Context-aware back navigation
  const isBrowseContext = context === 'browse';
  const backLabel = isBrowseContext ? 'Back to Browse' : 'Back to Repositories';

  // Navigation handlers
  const handleBack = useCallback(() => {
    if (isBrowseContext) {
      router.stateService.go('preview.browse.browse');
    } else {
      router.stateService.go('preview.admin.repository.repositories.list');
    }
  }, [isBrowseContext, router]);

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

  const handleViewInBrowse = useCallback(() => {
    router.stateService.go('preview.browse.browse.repo', { repoName: repositoryName });
  }, [router, repositoryName]);

  const handleSelectTab = useCallback((tab: TabId) => {
    setActiveTab(tab);
    const stateName = context === 'browse' ? 'preview.browse.repository-profile' : 'preview.admin.repository.repositories.profile';
    const tabParams = { repositoryName, tab };
    router.stateService.go(stateName, tabParams, { notify: false, location: 'replace' });
  }, [router, repositoryName, context]);

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
                    }}>{repository.url}</code>
                  </Text>
                </Flex>
              </Flex>
              <Flex gap="2" align="center">
                <Button variant="soft" size="2" onClick={handleViewInBrowse}>
                  <FolderTree size={16} />
                  Browse Repository
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
                  onToggleRepo={handleToggleRepoHealthCheck}
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
    </ScrollArea>
  );
}

export default RepositoryProfilePage;
