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

import React from 'react';
import { Box, Flex, Text, Badge, Table } from '@radix-ui/themes';
import {
  Clock,
  Zap,
  Globe,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Loader2,
} from 'lucide-react';
import { ClassicSettingsLink } from './classicSettingsLink';
import type {
  RepositoryProfileData,
  TaskInfo,
  CapabilityInfo,
  HttpSettingsInfo,
} from '../types';

// =============================================================================
// Types
// =============================================================================

interface SystemTabProps {
  repository: RepositoryProfileData;
  tasks: TaskInfo[];
  capabilities: CapabilityInfo[];
  httpSettings: HttpSettingsInfo | null;
  loading?: boolean;
}

// =============================================================================
// Helper Components
// =============================================================================

interface ProfileSectionProps {
  title: string;
  icon: React.ElementType;
  editPath?: string;
  children: React.ReactNode;
}

function ProfileSection({ title, icon: Icon, editPath, children }: ProfileSectionProps): JSX.Element {
  return (
    <Box className="profile-section__card" mb="4">
      <Flex align="center" justify="between" mb="4" className="profile-section__header">
        <Flex align="center" gap="2">
          <Icon size={18} />
          <Text weight="bold">{title}</Text>
        </Flex>
        {editPath && (
          <ClassicSettingsLink previewPath={editPath} label="View" />
        )}
      </Flex>
      {children}
    </Box>
  );
}

// =============================================================================
// Helper Functions
// =============================================================================

function getStatusBadge(status: string): JSX.Element {
  switch (status?.toLowerCase()) {
    case 'ok':
    case 'success':
    case 'completed':
      return <Badge color="green" size="1"><CheckCircle size={12} /> Success</Badge>;
    case 'running':
    case 'waiting':
      return <Badge color="blue" size="1"><Loader2 size={12} /> Running</Badge>;
    case 'failed':
    case 'error':
      return <Badge color="red" size="1"><XCircle size={12} /> Failed</Badge>;
    case 'blocked':
    case 'waiting_to_run':
      return <Badge color="yellow" size="1"><AlertTriangle size={12} /> Waiting</Badge>;
    default:
      return <Badge color="gray" size="1">{status || '—'}</Badge>;
  }
}

function formatRelativeTime(dateString: string | undefined): string {
  if (!dateString) return '—';
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 60) return `${diffMins} min ago`;
  if (diffHours < 24) return `${diffHours} hours ago`;
  return `${diffDays} days ago`;
}

// =============================================================================
// Component
// =============================================================================

/**
 * SystemTab - Shows system-level settings affecting this repository
 *
 * Maps to Settings → System menu:
 * - Scheduled Tasks: Tasks that affect this repository
 * - Capabilities: Capabilities affecting this repo (Firewall, Health Check, etc.)
 * - HTTP Settings: Proxy, User Agent (if relevant for proxy repos)
 */
export function SystemTab({
  repository,
  tasks,
  capabilities,
  httpSettings,
  loading,
}: SystemTabProps): JSX.Element {
  if (loading) {
    return (
      <Box className="profile-empty-state">
        <Loader2 size={48} className="profile-empty-state__icon profile-empty-state__icon--spinning" />
        <Text className="profile-empty-state__title">Loading System Data...</Text>
        <Text className="profile-empty-state__message">
          Fetching tasks, capabilities, and HTTP settings.
        </Text>
      </Box>
    );
  }

  return (
    <Box>
      {/* Scheduled Tasks Section */}
      <ProfileSection
        title="Scheduled Tasks"
        icon={Clock}
        editPath="preview/admin/system/tasks"
      >
        {tasks.length > 0 ? (
          <Table.Root>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Task</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Schedule</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Last Run</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {tasks.map((task) => (
                <Table.Row key={task.id}>
                  <Table.Cell>
                    <Text weight="medium">{task.name}</Text>
                    <Text size="1" color="gray" as="div">{task.type}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{task.schedule || 'Manual'}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{formatRelativeTime(task.lastRun)}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    {getStatusBadge(task.lastRunResult || '')}
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        ) : (
          <Text color="gray" size="2">
            No scheduled tasks found that specifically target this repository.
          </Text>
        )}
      </ProfileSection>

      {/* Capabilities Section */}
      <ProfileSection
        title="Capabilities"
        icon={Zap}
        editPath="preview/admin/system/capabilities"
      >
        {capabilities.length > 0 ? (
          <Table.Root>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Capability</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Affects This Repo</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {capabilities.map((capability) => (
                <Table.Row key={capability.id}>
                  <Table.Cell>
                    <Text weight="medium">{capability.type}</Text>
                    {capability.notes && (
                      <Text size="1" color="gray" as="div">{capability.notes}</Text>
                    )}
                  </Table.Cell>
                  <Table.Cell>
                    <Badge color={capability.enabled ? 'green' : 'gray'} size="1">
                      {capability.enabled ? 'Active' : 'Disabled'}
                    </Badge>
                  </Table.Cell>
                  <Table.Cell>
                    {capability.affectsRepo ? (
                      <Flex align="center" gap="1">
                        <CheckCircle size={14} color="var(--green-11)" />
                        <Text size="2">{capability.affectsReason || 'Yes'}</Text>
                      </Flex>
                    ) : (
                      <Text size="2" color="gray">No</Text>
                    )}
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        ) : (
          <Text color="gray" size="2">
            No capabilities found that affect this repository.
          </Text>
        )}
      </ProfileSection>

      {/* HTTP Settings Section (for proxy repositories) */}
      {repository.type === 'proxy' && (
        <ProfileSection
          title="HTTP Settings"
          icon={Globe}
          editPath="preview/admin/system/http"
        >
          {httpSettings ? (
            <Flex direction="column" gap="2">
              <Box className="profile-section__row">
                <Text className="profile-section__label">User Agent Suffix</Text>
                <Text className="profile-section__value">
                  {httpSettings.userAgentSuffix || '—'}
                </Text>
              </Box>
              <Box className="profile-section__row">
                <Text className="profile-section__label">HTTP Proxy</Text>
                <Text className="profile-section__value">
                  {httpSettings.httpProxy ? (
                    <code className="profile-section__value--code">
                      {httpSettings.httpProxy.host}:{httpSettings.httpProxy.port}
                    </code>
                  ) : (
                    'Not configured'
                  )}
                </Text>
              </Box>
              <Box className="profile-section__row">
                <Text className="profile-section__label">HTTPS Proxy</Text>
                <Text className="profile-section__value">
                  {httpSettings.httpsProxy ? (
                    <code className="profile-section__value--code">
                      {httpSettings.httpsProxy.host}:{httpSettings.httpsProxy.port}
                    </code>
                  ) : (
                    'Not configured'
                  )}
                </Text>
              </Box>
              {httpSettings.nonProxyHosts && httpSettings.nonProxyHosts.length > 0 && (
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Non-Proxy Hosts</Text>
                  <Text className="profile-section__value">
                    {httpSettings.nonProxyHosts.join(', ')}
                  </Text>
                </Box>
              )}
            </Flex>
          ) : (
            <Text color="gray" size="2">
              HTTP settings not available. Using system defaults.
            </Text>
          )}
        </ProfileSection>
      )}
    </Box>
  );
}

export default SystemTab;


