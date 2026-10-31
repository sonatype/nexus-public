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
import { Box, Flex, Text, Tabs, Badge, Card, Heading } from '@radix-ui/themes';
import { ExtJS } from '../../../../../../interface/ExtJS';
import {
  User as UserIcon,
  Shield,
  Key,
  Info,
  Pencil,
} from 'lucide-react';

import {
  LoadingState,
  ErrorState,
  EntityTable,
  EmptyState,
  TableColumn,
  MetadataGrid,
  PageHeader,
  StatusBadge,
} from '../../../../shared';
import { SettingsFormSection, SettingsButton } from '../../../../shared/form';
import { RoleExplorerTree } from '../roles/RoleExplorerTree';
import { useSecurityEntityModal } from '../SecurityEntityModalContext';
import { useUserTree } from './useUserTree';
import { useUserEffectivePrivileges, type EffectivePrivilege } from './useUserEffectivePrivileges';
import { useUsersApi } from './useUsersApi';
import {
  User,
  DEFAULT_SOURCE,
  getSourceLabel,
} from './types';
import { Role } from '../roles/types';
import { apiHubHref } from '../apiHubLinks';

import './UserProfilePage.scss';

interface UserProfilePageProps {
  userId: string;
  userSource: string;
  onBack: () => void;
  onEdit?: () => void;
  canEdit?: boolean;
  /** When true, cross-entity links (roles, privileges) render as plain text — used in full-screen modal */
  embedMode?: boolean;
}

const ROLES_BASE = 'preview/admin/security/roles';
const PRIVILEGES_BASE = 'preview/admin/security/privileges';

function _roleProfileUrl(roleId: string): string {
  return `#${ROLES_BASE}/${encodeURIComponent(roleId)}/profile`;
}

function _privilegeProfileUrl(privilegeId: string): string {
  return `#${PRIVILEGES_BASE}/${encodeURIComponent(privilegeId)}/profile`;
}

/**
 * UserProfilePage - Read-only operational dashboard for a User.
 * Tabs: Overview, Roles, Privileges, Security Tree.
 * Follows Role Profile pattern.
 */
export function UserProfilePage({
  userId,
  userSource,
  onBack,
  onEdit,
  canEdit = false,
  embedMode = false,
}: UserProfilePageProps) {
  const [user, setUser] = useState<User | null>(null);
  const [userLoading, setUserLoading] = useState(true);
  const [userError, setUserError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('overview');

  const { fetchUser } = useUsersApi();
  const {
    tree,
    loading: treeLoading,
    error: _treeError,
    toggleExpand,
    expandAll,
    collapseAll,
    setSearchTerm,
  } = useUserTree(user);
  const {
    privileges,
    roleMap,
    loading: privilegesLoading,
  } = useUserEffectivePrivileges(user);

  useEffect(() => {
    const load = async () => {
      setUserLoading(true);
      setUserError(null);
      try {
        const data = await fetchUser(userId, userSource || DEFAULT_SOURCE);
        setUser(data);
      } catch (err: unknown) {
        setUserError(err instanceof Error ? err.message : 'Failed to load user');
      } finally {
        setUserLoading(false);
      }
    };
    load();
  }, [userId, userSource, fetchUser]);

  const { openEntity } = useSecurityEntityModal();
  const canLinkApiHub = ExtJS.checkPermission('nexus:settings:read');

  const rolesTableData = useMemo(() => {
    if (!user?.roles) return [];
    return user.roles
      .map((roleId) => roleMap.get(roleId))
      .filter((r): r is Role => !!r);
  }, [user?.roles, roleMap]);

  const roleColumns: TableColumn<Role>[] = useMemo(
    () => [
      {
        id: 'name',
        header: 'Name',
        accessor: (role) =>
          embedMode ? (
            <Text size="2" as="span">{role.name}</Text>
          ) : (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                openEntity('role', role.id);
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
              aria-label={`Open role profile: ${role.name}`}
            >
              <Text size="2" as="span">{role.name}</Text>
            </button>
          ),
        sortable: true,
      },
      {
        id: 'id',
        header: 'ID',
        accessor: (role) => <Text size="2">{role.id}</Text>,
        sortable: true,
      },
      {
        id: 'source',
        header: 'Source',
        accessor: (role) => (
          <Badge color="blue" variant="soft">
            {role.source || 'Default'}
          </Badge>
        ),
        sortable: true,
      },
      {
        id: 'description',
        header: 'Description',
        accessor: (role) => <Text size="2">{role.description || '—'}</Text>,
        sortable: false,
      },
    ],
    [embedMode, openEntity]
  );

  const privilegeColumns: TableColumn<EffectivePrivilege>[] = useMemo(
    () => [
      {
        id: 'name',
        header: 'Name',
        accessor: (p) => (
          <Flex align="center" gap="2">
            <Key size={14} color="var(--amber-9)" aria-hidden="true" />
            {embedMode ? (
              <Text size="2" weight="medium" as="span">{p.name}</Text>
            ) : (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  openEntity('privilege', p.id);
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
                aria-label={`Open privilege profile: ${p.name}`}
              >
                <Text size="2" weight="medium" as="span">{p.name}</Text>
              </button>
            )}
          </Flex>
        ),
        sortable: true,
      },
      {
        id: 'permission',
        header: 'Permission',
        accessor: (p) => (
          <Text size="1" color="gray" style={{ fontFamily: 'var(--font-mono)' }}>
            {p.permission}
          </Text>
        ),
        sortable: true,
      },
      {
        id: 'type',
        header: 'Type',
        accessor: (p) => <Text size="2">{p.type || '—'}</Text>,
        sortable: true,
      },
      {
        id: 'repository',
        header: 'Repository',
        accessor: (p) => (
          <Text size="2">{p.properties?.repository || '—'}</Text>
        ),
        sortable: true,
      },
      {
        id: 'actions',
        header: 'Actions',
        accessor: (p) => (
          <Text size="2">{p.properties?.actions || '—'}</Text>
        ),
        sortable: true,
      },
      {
        id: 'grantedBy',
        header: 'Granted by',
        accessor: (p) => (
          <Flex wrap="wrap" gap="1">
            {(p.grantedByRoleIds || []).map((roleId, idx) => {
              const role = roleMap.get(roleId);
              const label = role?.name || roleId;
              const isLast = idx === (p.grantedByRoleIds?.length ?? 1) - 1;
              return (
                <React.Fragment key={roleId}>
                  {embedMode ? (
                    <Text size="1" as="span">{label}</Text>
                  ) : (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        openEntity('role', roleId);
                      }}
                      style={{
                        background: 'none',
                        border: 'none',
                        padding: 0,
                        font: 'inherit',
                        color: 'var(--blue-11)',
                        textDecoration: 'underline',
                        fontSize: 'var(--font-size-1)',
                        cursor: 'pointer',
                      }}
                      aria-label={`Open role profile: ${label}`}
                    >
                      {label}
                    </button>
                  )}
                  {!isLast && <Text size="1">{', '}</Text>}
                </React.Fragment>
              );
            })}
          </Flex>
        ),
        sortable: false,
      },
    ],
    [embedMode, openEntity, roleMap]
  );

  if (userLoading) return <LoadingState message="Loading user profile..." />;
  if (userError) return <ErrorState message={userError} onRetry={() => window.location.reload()} />;
  if (!user) return <ErrorState message="User not found" />;

  const sourceLabel = getSourceLabel(user.source || DEFAULT_SOURCE);
  const isActive = user.status === 'active';
  const statusLabel = isActive ? 'Active' : 'Disabled';

  return (
    <Box className="user-profile-page" data-testid="user-profile-page">
      <PageHeader
        title={user.userId}
        headingAs={embedMode ? 'h2' : 'h1'}
        breadcrumbs={[
          { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
          { label: 'Users', onClick: onBack },
          { label: user.userId }
        ]}
        actions={!embedMode ? (
          <Flex gap="2">
            {canEdit && onEdit && (
              <SettingsButton variant="primary" onClick={onEdit} icon={Pencil}>
                Edit User
              </SettingsButton>
            )}
          </Flex>
        ) : undefined}
      />
      <Card className="user-profile-page__hero">
        <Flex gap="4" align="center">
          <Box className="user-profile-page__hero-icon">
            <UserIcon size={40} aria-hidden="true" />
          </Box>
          <Box className="user-profile-page__hero-content">
            <Flex align="center" gap="3" className="user-profile-page__title-row">
              <Heading as="h2" size="5" weight="bold" className="user-profile-page__title">
                {user.userId}
              </Heading>
              <StatusBadge
                status={isActive ? 'online' : 'offline'}
                size="small"
                label={isActive ? 'Active' : 'Disabled'}
              />
              <Badge color="blue" variant="soft">
                {sourceLabel}
              </Badge>
            </Flex>
          </Box>
        </Flex>
      </Card>

      <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
        <Tabs.List className="user-profile-page__tabs-list">
          <Tabs.Trigger value="overview">
            <Flex align="center" gap="1">
              <Info size={14} aria-hidden="true" />
              <Text size="2">Overview</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="roles">
            <Flex align="center" gap="1">
              <Shield size={14} aria-hidden="true" />
              <Text size="2">Roles</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="privileges">
            <Flex align="center" gap="1">
              <Key size={14} aria-hidden="true" />
              <Text size="2">Privileges</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="tree">
            <Flex align="center" gap="1">
              <Shield size={14} aria-hidden="true" />
              <Text size="2">Security Tree</Text>
            </Flex>
          </Tabs.Trigger>
        </Tabs.List>

        <Box pt="4" className="user-profile-page__content">
          <Tabs.Content value="overview">
            <SettingsFormSection title="User Metadata" defaultOpen>
              <MetadataGrid
                items={[
                  {
                    label: 'User ID',
                    value: (
                      <Text size="2" style={{ fontFamily: 'var(--font-mono)' }}>
                        {user.userId}
                      </Text>
                    ),
                  },
                  { label: 'First Name', value: user.firstName },
                  { label: 'Last Name', value: user.lastName },
                  { label: 'Email', value: user.emailAddress || user.email },
                  { label: 'Status', value: statusLabel },
                  {
                    label: 'Source',
                    value: (
                      <Badge color="blue" variant="soft">
                        {sourceLabel}
                      </Badge>
                    ),
                  },
                ]}
              />
            </SettingsFormSection>
          </Tabs.Content>

          <Tabs.Content value="roles">
            <Box className="user-profile-page__pane">
              <EntityTable<Role>
                data={rolesTableData}
                columns={roleColumns}
                getRowKey={(r) => r.id}
                loading={privilegesLoading && user.roles!.length > 0}
                loadingMessage="Loading roles..."
                ariaLabel="Roles assigned to user"
                emptyState={
                  <EmptyState
                    title="No roles assigned"
                    description="This user has no roles."
                    icon={Shield}
                  />
                }
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="privileges">
            <Box className="user-profile-page__pane">
              {canLinkApiHub && !embedMode && (
                <Box mb="3">
                  <Text size="2">
                    <a href={apiHubHref({ user: userId })} className="user-profile-page__api-hub-link">
                      View API access for this user
                    </a>{' '}
                    <Text size="1" color="gray" as="span">
                      (opens API hub with <Text as="span" style={{ fontFamily: 'var(--font-mono)' }}>?user=</Text> deep
                      link)
                    </Text>
                  </Text>
                </Box>
              )}
              <EntityTable<EffectivePrivilege>
                data={privileges}
                columns={privilegeColumns}
                getRowKey={(p) => p.id}
                loading={privilegesLoading}
                loadingMessage="Calculating effective privileges..."
                ariaLabel="Effective privileges"
                emptyState={
                  <EmptyState
                    title="No privileges"
                    description="This user has no effective privileges."
                    icon={Key}
                  />
                }
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="tree">
            <Card className="user-profile-page__pane user-profile-page__pane--tree">
              {user.roles?.length ? (
                <RoleExplorerTree
                  tree={tree}
                  loading={treeLoading}
                  onToggleExpand={toggleExpand}
                  onExpandAll={expandAll}
                  onCollapseAll={collapseAll}
                  onSearchChange={setSearchTerm}
                />
              ) : (
                <Box className="user-profile-page__empty-tree">
                  <Text size="2" color="gray">
                    This user has no roles assigned. Assign roles in the Edit
                    form to see the security tree.
                  </Text>
                </Box>
              )}
            </Card>
          </Tabs.Content>
        </Box>
      </Tabs.Root>
    </Box>
  );
}

export default UserProfilePage;
