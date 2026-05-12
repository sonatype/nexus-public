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

import React, { useState, useMemo } from 'react';
import { Box, Flex, Text, Tabs, Badge } from '@radix-ui/themes';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { Key, Shield, Users, Info, ArrowLeft, Pencil, Check } from 'lucide-react';

import {
  PageHeader,
  LoadingState,
  ErrorState,
  EntityTable,
  EmptyState,
  TableColumn,
  StatusBadge,
  MetadataGrid,
} from '../../../../shared';
import { SettingsFormSection, SettingsButton } from '../../../shared/form';
import { usePrivilegeProfile } from './usePrivilegeProfile';
import { useSecurityEntityModal } from '../SecurityEntityModalContext';
import { usePrivilegesApi } from './usePrivilegesApi';
import { getPrivilegeTypeLabel, getActionsForPrivilegeType, PRIVILEGE_TYPES } from './types';
import { Role, formatRoleSourceDisplay } from '../roles/types';
import { User, DEFAULT_SOURCE, getSourceLabel } from '../users/types';
import { apiHubHref } from '../apiHubLinks';

import './PrivilegeProfilePage.scss';

interface PrivilegeProfilePageProps {
  privilegeId: string;
  onBack: () => void;
  onEdit?: () => void;
  canEdit?: boolean;
  /** When true, cross-entity links (roles, users) render as plain text — used in full-screen modal */
  embedMode?: boolean;
}

/**
 * PrivilegeProfilePage - Read-only profile for a Privilege.
 * Tabs: Overview, Roles Using This, Users With Access.
 * Per SECURITY-CROSS-NAVIGATION-DESIGN: cross-module links open in new tab.
 */
export function PrivilegeProfilePage({
  privilegeId,
  onBack,
  onEdit,
  canEdit = false,
  embedMode = false,
}: PrivilegeProfilePageProps) {
  const [activeTab, setActiveTab] = useState('overview');

  const { privilege, rolesUsing, usersWithAccess, loading, error } =
    usePrivilegeProfile(privilegeId);
  const { openEntity } = useSecurityEntityModal();

  const roleColumns: TableColumn<Role>[] = useMemo(
    () => [
      {
        id: 'name',
        header: 'Name',
        accessor: (role) =>
          embedMode ? (
            <Text size="2" weight="medium" as="span">{role.name}</Text>
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
              <Text size="2" weight="medium" as="span">{role.name}</Text>
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
            {formatRoleSourceDisplay(role.source)}
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

  const userColumns: TableColumn<User>[] = useMemo(
    () => [
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
          <StatusBadge
            status={user.status === 'active' ? 'online' : 'offline'}
            size="small"
          />
        ),
        sortable: true,
      },
      {
        id: 'source',
        header: 'Source',
        accessor: (user) => (
          <Text size="2">{getSourceLabel(user.source || DEFAULT_SOURCE)}</Text>
        ),
        sortable: true,
      },
    ],
    [embedMode, openEntity]
  );

  if (loading && !privilege) return <LoadingState message="Loading privilege profile..." />;
  if (error) return <ErrorState message={error} onRetry={() => window.location.reload()} />;
  if (!privilege) return <ErrorState message="Privilege not found" />;

  const canLinkApiHub = ExtJS.checkPermission('nexus:settings:read');

  return (
    <Box className="privilege-profile-page" data-testid="privilege-profile-page">
      <PageHeader
        icon={Key}
        title={privilege.name}
        description={`${getPrivilegeTypeLabel(privilege.type)}${privilege.readOnly ? ' (Read Only)' : ''}`}
        breadcrumbs={[
          { label: 'Privileges', onClick: onBack },
          { label: privilege.name },
        ]}
        actions={
          !embedMode ? (
            <Flex gap="2">
              {canEdit && onEdit && (
                <SettingsButton variant="primary" onClick={onEdit} icon={Pencil}>
                  Edit Privilege
                </SettingsButton>
              )}
              <SettingsButton variant="ghost" onClick={onBack} icon={ArrowLeft}>
                Back to List
              </SettingsButton>
            </Flex>
          ) : undefined
        }
      />

      <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
        <Tabs.List className="privilege-profile-page__tabs-list">
          <Tabs.Trigger value="overview">
            <Flex align="center" gap="1">
              <Info size={14} />
              <Text size="2">Overview</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="roles">
            <Flex align="center" gap="1">
              <Shield size={14} />
              <Text size="2">Roles Using This</Text>
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="users">
            <Flex align="center" gap="1">
              <Users size={14} />
              <Text size="2">Users With Access</Text>
            </Flex>
          </Tabs.Trigger>
        </Tabs.List>

        <Box pt="4" className="privilege-profile-page__content">
          <Tabs.Content value="overview">
            <SettingsFormSection title="Privilege Metadata" defaultOpen>
              <MetadataGrid
                items={[
                  { label: 'Name', value: privilege.name },
                  { label: 'Type', value: getPrivilegeTypeLabel(privilege.type) },
                  { label: 'Description', value: privilege.description || '—' },
                  ...(privilege.readOnly
                    ? [
                        {
                          label: 'Read Only',
                          value: (
                            <Badge color="gray" variant="soft">
                              Read Only (system)
                            </Badge>
                          ),
                        },
                      ]
                    : []),
                ]}
              />
            </SettingsFormSection>
            <SettingsFormSection title="Configuration" defaultOpen>
              <MetadataGrid
                items={[
                  // Type-specific fields
                  ...(privilege.type === PRIVILEGE_TYPES.WILDCARD
                    ? [
                        {
                          label: 'Privilege String',
                          value: (
                            <Text size="2" style={{ fontFamily: 'var(--font-mono)' }}>
                              {privilege.properties?.pattern || '—'}
                            </Text>
                          ),
                        },
                      ]
                    : []),
                  ...(privilege.type === PRIVILEGE_TYPES.APPLICATION
                    ? [
                        { label: 'Domain', value: privilege.properties?.domain || '—' },
                      ]
                    : []),
                  ...(privilege.type === PRIVILEGE_TYPES.REPOSITORY_VIEW ||
                  privilege.type === PRIVILEGE_TYPES.REPOSITORY_ADMIN
                    ? [
                        { label: 'Format', value: privilege.properties?.format || '—' },
                        { label: 'Repository', value: privilege.properties?.repository || '—' },
                      ]
                    : []),
                  ...(privilege.type === PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR
                    ? [
                        { label: 'Content Selector', value: privilege.properties?.contentSelector || '—' },
                        { label: 'Repository', value: privilege.properties?.repository || '—' },
                      ]
                    : []),
                  ...(privilege.type === PRIVILEGE_TYPES.SCRIPT
                    ? [
                        { label: 'Script Name', value: privilege.properties?.name || '—' },
                      ]
                    : []),
                  // Actions (if applicable)
                  ...(privilege.properties?.actions
                    ? [
                        {
                          label: 'Actions',
                          value: (() => {
                            const actions = privilege.properties.actions.split(',').map((a) => a.trim());
                            const availableActions = getActionsForPrivilegeType(privilege.type);
                            if (availableActions.length === 0) return '—';

                            return (
                              <Flex gap="2" wrap="wrap">
                                {availableActions.map((option) => {
                                  const isChecked = actions.includes(option.value);
                                  return (
                                    <Flex
                                      key={option.value}
                                      align="center"
                                      gap="2"
                                      style={{
                                        opacity: isChecked ? 1 : 0.4,
                                        padding: '4px 8px',
                                        borderRadius: '4px',
                                        backgroundColor: isChecked ? 'var(--accent-3)' : 'var(--gray-3)',
                                      }}
                                    >
                                      <Box
                                        style={{
                                          width: '16px',
                                          height: '16px',
                                          borderRadius: '3px',
                                          border: '1px solid var(--gray-7)',
                                          backgroundColor: isChecked ? 'var(--accent-9)' : 'transparent',
                                          display: 'flex',
                                          alignItems: 'center',
                                          justifyContent: 'center',
                                        }}
                                      >
                                        {isChecked && <Check size={12} color="white" />}
                                      </Box>
                                      <Text size="2">{option.label}</Text>
                                    </Flex>
                                  );
                                })}
                              </Flex>
                            );
                          })(),
                        },
                      ]
                    : []),
                ]}
              />
            </SettingsFormSection>
            {canLinkApiHub && !embedMode && privilege.permission && (
              <Box mt="3">
                <Text size="2">
                  <a
                    href={apiHubHref({ permission: privilege.permission })}
                    className="privilege-profile-page__api-hub-link"
                  >
                    View API endpoints requiring this permission
                  </a>{' '}
                  <Text size="1" color="gray" as="span">
                    (opens API hub filtered by <Text as="span" style={{ fontFamily: 'var(--font-mono)' }}>?permission=</Text>)
                  </Text>
                </Text>
              </Box>
            )}
          </Tabs.Content>

          <Tabs.Content value="roles">
            <Box className="privilege-profile-page__pane">
              <EntityTable<Role>
                data={rolesUsing}
                columns={roleColumns}
                getRowKey={(r) => r.id}
                loading={loading}
                loadingMessage="Computing roles..."
                ariaLabel="Roles that grant this privilege"
                emptyState={
                  <EmptyState
                    title="No roles use this privilege"
                    description="No roles currently grant this privilege."
                    icon={Shield}
                  />
                }
              />
            </Box>
          </Tabs.Content>

          <Tabs.Content value="users">
            <Box className="privilege-profile-page__pane">
              <EntityTable<User>
                data={usersWithAccess}
                columns={userColumns}
                getRowKey={(u) => u.userId}
                loading={loading}
                loadingMessage="Computing users..."
                ariaLabel="Users who have this privilege"
                emptyState={
                  <EmptyState
                    title="No users have this privilege"
                    description="No users have this privilege through their assigned roles."
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

export default PrivilegeProfilePage;
