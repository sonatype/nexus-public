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
  Key,
  Users,
  User,
  Ghost,
  CheckCircle,
  XCircle,
  Loader2,
} from 'lucide-react';
import { ClassicSettingsLink } from './classicSettingsLink';
import type {
  RepositoryProfileData,
  PrivilegeInfo,
  RoleInfo,
  UserWithAccess,
  AnonymousAccess,
} from '../types';

// =============================================================================
// Types
// =============================================================================

interface AccessSecurityTabProps {
  repository: RepositoryProfileData;
  privileges: PrivilegeInfo[];
  roles: RoleInfo[];
  users: UserWithAccess[];
  anonymousAccess: AnonymousAccess | null;
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
// Component
// =============================================================================

/**
 * AccessSecurityTab - Shows who has access to this repository
 *
 * Maps to Settings → Security menu:
 * - Privileges: Repository-specific privileges
 * - Roles with Access: Roles that include these privileges
 * - Users with Access: Users with these roles
 * - Anonymous Access: Can anonymous read/browse this repo
 */
export function AccessSecurityTab({
  repository,
  privileges,
  roles,
  users,
  anonymousAccess,
  loading,
}: AccessSecurityTabProps): JSX.Element {
  if (loading) {
    return (
      <Box className="profile-empty-state">
        <Loader2 size={48} className="profile-empty-state__icon profile-empty-state__icon--spinning" />
        <Text className="profile-empty-state__title">Loading Access Data...</Text>
        <Text className="profile-empty-state__message">
          Fetching privileges, roles, and users with access to this repository.
        </Text>
      </Box>
    );
  }

  return (
    <Box>
      {/* Privileges Section */}
      <ProfileSection
        title="Privileges"
        icon={Key}
        editPath="preview/admin/security/privileges"
      >
        {privileges.length > 0 ? (
          <Flex direction="column" gap="2">
            {privileges.map((privilege) => (
              <Box key={privilege.name} className="profile-section__row profile-section__row--privilege">
                <Flex align="center" gap="2">
                  <code className="profile-section__value--code">{privilege.name}</code>
                  {privilege.description && (
                    <Text size="1" color="gray">({privilege.description})</Text>
                  )}
                </Flex>
                <Badge size="1" color={privilege.actions?.includes('*') ? 'green' : 'blue'}>
                  {privilege.actions?.join(', ') || 'Read'}
                </Badge>
              </Box>
            ))}
          </Flex>
        ) : (
          <Text color="gray" size="2">
            No specific privileges found for this repository.
            Default privileges may still apply.
          </Text>
        )}
      </ProfileSection>

      {/* Roles with Access */}
      <ProfileSection
        title="Roles with Access"
        icon={Users}
        editPath="preview/admin/security/roles"
      >
        {roles.length > 0 ? (
          <Table.Root>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Role</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Privileges</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Users</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {roles.map((role) => (
                <Table.Row key={role.id}>
                  <Table.Cell>
                    <Text weight="medium">{role.name}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="1" color="gray">
                      {role.privilegeCount ?? 0} privileges
                    </Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Badge size="1">{role.userCount ?? 0}</Badge>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        ) : (
          <Text color="gray" size="2">
            No roles found with explicit access to this repository.
          </Text>
        )}
      </ProfileSection>

      {/* Users with Access */}
      <ProfileSection
        title="Users with Access"
        icon={User}
        editPath="preview/admin/security/users"
      >
        {users.length > 0 ? (
          <Table.Root>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>User</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Via Role</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Permissions</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {users.slice(0, 10).map((user) => (
                <Table.Row key={user.userId}>
                  <Table.Cell>
                    <Text weight="medium">{user.userId}</Text>
                    {user.firstName && (
                      <Text size="1" color="gray"> ({user.firstName} {user.lastName})</Text>
                    )}
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="1">{user.roles?.join(', ') || '—'}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Badge size="1" color={user.permissions?.includes('Full Access') ? 'green' : 'blue'}>
                      {user.permissions?.join(', ') || 'Read'}
                    </Badge>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        ) : (
          <Text color="gray" size="2">
            No users found with explicit access to this repository.
          </Text>
        )}
        {users.length > 10 && (
          <Text size="1" color="gray" mt="2">
            ... and {users.length - 10} more users
          </Text>
        )}
      </ProfileSection>

      {/* Anonymous Access */}
      <ProfileSection
        title="Anonymous Access"
        icon={Ghost}
        editPath="preview/admin/security/anonymous"
      >
        {anonymousAccess ? (
          <Flex direction="column" gap="2">
            <Box className="profile-section__row">
              <Text className="profile-section__label">Status</Text>
              <Flex align="center" gap="1">
                {anonymousAccess.enabled ? (
                  <>
                    <CheckCircle size={14} color="var(--green-11)" />
                    <Text className="profile-section__value">Enabled</Text>
                  </>
                ) : (
                  <>
                    <XCircle size={14} color="var(--red-11)" />
                    <Text className="profile-section__value">Disabled</Text>
                  </>
                )}
              </Flex>
            </Box>
            {anonymousAccess.enabled && (
              <>
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Can Read</Text>
                  <Text className="profile-section__value">
                    {anonymousAccess.canRead ? 'Yes' : 'No'}
                  </Text>
                </Box>
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Can Browse</Text>
                  <Text className="profile-section__value">
                    {anonymousAccess.canBrowse ? 'Yes' : 'No'}
                  </Text>
                </Box>
                <Box className="profile-section__row">
                  <Text className="profile-section__label">Realm</Text>
                  <Text className="profile-section__value">{anonymousAccess.realm || '—'}</Text>
                </Box>
              </>
            )}
          </Flex>
        ) : (
          <Text color="gray" size="2">
            Anonymous access settings not available.
          </Text>
        )}
      </ProfileSection>
    </Box>
  );
}

export default AccessSecurityTab;


