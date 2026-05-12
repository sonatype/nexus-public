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

import React, { useState, useEffect, useMemo } from 'react';
import { Box, Flex, Text, Tabs, Badge } from '@radix-ui/themes';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { 
  Shield, 
  Key, 
  Filter, 
  Users, 
  Info,
  ArrowLeft,
  Loader2,
  AlertTriangle
} from 'lucide-react';

import { 
  PageHeader, 
  LoadingState, 
  ErrorState, 
  EntityTable,
  EmptyState,
  TableColumn,
  StatusBadge,
  useToast,
  MetadataGrid,
} from '../../../../shared';
import { SettingsFormSection } from '../../../shared/form';
import { useRoleTree } from './useRoleTree';
import { RoleExplorerTree } from './RoleExplorerTree';
import { CalculatedPermissions } from './CalculatedPermissions';
import { useSecurityEntityModal } from '../SecurityEntityModalContext';
import { useRolesApi } from './useRolesApi';
import { useUsersApi } from '../users/useUsersApi';
import { Role, formatRoleSourceDisplay } from './types';
import { User, DEFAULT_SOURCE } from '../users/types';
import { apiHubHref } from '../apiHubLinks';

import './RoleProfilePage.scss';

interface RoleProfilePageProps {
  roleName: string;
  onBack: () => void;
  /** When true, cross-entity links (users) render as plain text — used in full-screen modal */
  embedMode?: boolean;
}

/**
 * RoleProfilePage - Dedicated read-only operational dashboard for a Role.
 * Follows the "Profile Pattern" established for Repositories.
 */
export function RoleProfilePage({ roleName, onBack, embedMode = false }: RoleProfilePageProps) {
  const [role, setRole] = useState<Role | null>(null);
  const [roleLoading, setRoleLoading] = useState(true);
  const [roleError, setRoleError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [assignedUsers, setAssignedUsers] = useState<User[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [treeExpandedNodes, setTreeExpandedNodes] = useState<Set<string>>(() => new Set([roleName]));
  const [treeSearchTerm, setTreeSearchTerm] = useState('');

  useEffect(() => {
    setTreeExpandedNodes(new Set([roleName]));
  }, [roleName]);

  const { findRole } = useRolesApi();
  const { fetchUsers } = useUsersApi();
  const { tree, effectivePrivileges, loading: treeLoading, toggleExpand, expandAll, collapseAll } = useRoleTree(roleName, {
    searchTerm: treeSearchTerm,
    initialExpandedNodes: treeExpandedNodes,
    onExpandedNodesChange: setTreeExpandedNodes,
  });
  const toast = useToast();

  useEffect(() => {
    const loadRole = async () => {
      setRoleLoading(true);
      try {
        const data = await findRole(roleName);
        setRole(data);
      } catch (err: any) {
        setRoleError(err.message || 'Failed to load role');
      } finally {
        setRoleLoading(false);
      }
    };

    loadRole();
  }, [roleName, findRole]);

  useEffect(() => {
    if (activeTab === 'users' && assignedUsers.length === 0) {
      const loadUsers = async () => {
        setUsersLoading(true);
        try {
          // Fetch all users and filter by role
          // Note: In a real high-scale environment, we'd want a backend endpoint
          // for this, but for now we follow the existing pattern.
          const allUsers = await fetchUsers();
          const filtered = allUsers.filter(u => u.roles.includes(roleName));
          setAssignedUsers(filtered);
        } catch (err: any) {
          toast.error(`Failed to load assigned users: ${err.message}`);
        } finally {
          setUsersLoading(false);
        }
      };
      loadUsers();
    }
  }, [activeTab, roleName, fetchUsers, assignedUsers.length, toast]);

  const { openEntity } = useSecurityEntityModal();

  const userColumns: TableColumn<User>[] = useMemo(() => [
    {
      id: 'userId',
      header: 'User ID',
      accessor: (user) =>
        embedMode ? (
          <Text size="2" weight="medium" as="span">{user.userId}</Text>
        ) : (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              openEntity('user', user.userId, user.source || DEFAULT_SOURCE);
            }}
            style={{
              background: 'none',
              border: 'none',
              padding: 0,
              font: 'inherit',
              color: 'var(--blue-11)',
              textDecoration: 'underline',
              cursor: 'pointer',
            }}
            aria-label={`Open user profile: ${user.userId}`}
          >
            <Text size="2" weight="medium" as="span">{user.userId}</Text>
          </button>
        ),
      sortable: true,
    },
    {
      id: 'name',
      header: 'Name',
      accessor: (user) =>
        embedMode ? (
          <Text size="2" as="span">{user.firstName} {user.lastName}</Text>
        ) : (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              openEntity('user', user.userId, user.source || DEFAULT_SOURCE);
            }}
            style={{
              background: 'none',
              border: 'none',
              padding: 0,
              font: 'inherit',
              color: 'var(--blue-11)',
              textDecoration: 'underline',
              cursor: 'pointer',
            }}
            aria-label={`Open user profile: ${user.firstName} ${user.lastName}`}
          >
            <Text size="2" as="span">{user.firstName} {user.lastName}</Text>
          </button>
        ),
      sortable: true,
    },
    {
      id: 'status',
      header: 'Status',
      accessor: (user) => (
        <StatusBadge status={user.status === 'active' ? 'online' : 'offline'} size="small" />
      ),
      sortable: true,
    },
    {
      id: 'source',
      header: 'Source',
      accessor: (user) => <Text size="2">{user.source}</Text>,
      sortable: true,
    }
  ], [embedMode, openEntity]);

  if (roleLoading) return <LoadingState message="Loading role profile..." />;
  if (roleError) return <ErrorState message={roleError} onRetry={() => window.location.reload()} />;
  if (!role) return <ErrorState message="Role not found" />;

  const canLinkApiHub = ExtJS.checkPermission('nexus:settings:read');

  return (
    <Box className="role-profile-page">
      <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
        <Tabs.List className="role-profile-page__tabs-list">
          <Tabs.Trigger value="overview">
            <Flex align="center" gap="1">
              <Info size={14} />
              <Text size="2">Overview</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="explorer">
            <Flex align="center" gap="1">
              <Shield size={14} />
              <Text size="2">Security Tree</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="effective">
            <Flex align="center" gap="1">
              <Key size={14} />
              <Text size="2">Effective Permissions</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="users">
            <Flex align="center" gap="1">
              <Users size={14} />
              <Text size="2">Assigned Users</Text>
            </Flex>
          </Tabs.Trigger>
        </Tabs.List>

        <Box pt="4" className="role-profile-page__content">
          <Tabs.Content value="overview">
            <SettingsFormSection title="Role Metadata" defaultOpen>
              <MetadataGrid
                items={[
                  {
                    label: 'ID',
                    value: (
                      <Text size="2" style={{ fontFamily: 'var(--font-mono)' }}>
                        {role.id}
                      </Text>
                    ),
                  },
                  { label: 'Name', value: role.name },
                  {
                    label: 'Source',
                    value: (
                      <Badge color="blue" variant="soft">
                        {formatRoleSourceDisplay(role.source)}
                      </Badge>
                    ),
                  },
                  { label: 'Description', value: role.description },
                ]}
              />
            </SettingsFormSection>
            {canLinkApiHub && !embedMode && (
              <Box mt="3">
                <Text size="2">
                  <a href={apiHubHref({ role: role.id })} className="role-profile-page__api-hub-link">
                    View API endpoints this role can access
                  </a>{' '}
                  <Text size="1" color="gray" as="span">
                    (administrator only; opens with <Text as="span" style={{ fontFamily: 'var(--font-mono)' }}>?role=</Text>{' '}
                    deep link)
                  </Text>
                </Text>
              </Box>
            )}
          </Tabs.Content>

          <Tabs.Content value="explorer">
            <Box className="role-profile-page__pane role-profile-page__pane--tree">
              <RoleExplorerTree 
                tree={tree} 
                loading={treeLoading} 
                onToggleExpand={toggleExpand} 
                onExpandAll={expandAll}
                onCollapseAll={collapseAll}
                onSearchChange={setTreeSearchTerm}
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="effective">
            <Box className="role-profile-page__pane">
              <CalculatedPermissions
                privileges={effectivePrivileges}
                loading={treeLoading}
                linksDisabled={embedMode}
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="users">
            <Box className="role-profile-page__pane">
              <EntityTable<User>
                data={assignedUsers}
                columns={userColumns}
                getRowKey={(u) => u.userId}
                loading={usersLoading}
                loadingMessage="Loading assigned users..."
                ariaLabel="Assigned users list"
                emptyState={
                  <EmptyState
                    title="No users assigned"
                    description="No users currently hold this role directly."
                    icon={Users}
                  />
                }
              />
            </Box>
          </Tabs.Content>
        </Box>
      </Tabs.Root>
    </Box>
  );
}

export default RoleProfilePage;
