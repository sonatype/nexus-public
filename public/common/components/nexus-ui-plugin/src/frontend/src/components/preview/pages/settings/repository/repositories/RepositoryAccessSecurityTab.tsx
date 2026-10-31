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
import {
  Box,
  Flex,
  Text,
  Badge,
  Button,
  Card,
  Spinner,
  Table,
  Separator,
} from '@radix-ui/themes';
import {
  Key,
  Users as UsersIcon,
  User,
  Ghost,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import ExtJS from '../../../../../../interface/ExtJS';
import { useRepositoryAccessSecurity } from './useRepositoryAccessSecurity';

const USERS_DISPLAY_LIMIT = 10;

interface RepositoryAccessSecurityTabProps {
  repositoryName: string;
  repositoryFormat?: string;
}

/**
 * RepositoryAccessSecurityTab - Settings-page tab that surfaces
 * repo-scoped privileges, roles, users, and anonymous-access status.
 * Self-fetching; mounts only when the tab is opened (Radix Tabs unmounts
 * inactive content), so the security REST calls do not block Settings-page
 * first paint.
 *
 * Each section is gated on its own read permission so users can see just
 * the slice they have access to (mirrors the ratified Tasks & Capabilities
 * pattern). The corresponding fetch is skipped when the perm is missing.
 */
export function RepositoryAccessSecurityTab({
  repositoryName,
  repositoryFormat,
}: RepositoryAccessSecurityTabProps): JSX.Element {
  const canReadPrivileges = ExtJS.checkPermission('nexus:privileges:read');
  const canReadRoles = ExtJS.checkPermission('nexus:roles:read');
  const canReadUsers = ExtJS.checkPermission('nexus:users:read');
  const canReadAnonymous = ExtJS.checkPermission('nexus:settings:read');

  const { privileges, roles, users, anonymousAccess, loading, error, refetch } =
    useRepositoryAccessSecurity(repositoryName, {
      canReadPrivileges,
      canReadRoles,
      canReadUsers,
      canReadAnonymous,
      repositoryFormat,
    });

  if (loading) {
    return (
      <Flex justify="center" align="center" py="9" data-testid="access-security-loading">
        <Spinner size="3" />
      </Flex>
    );
  }

  if (error) {
    return (
      <Card size="2">
        <Flex direction="column" align="center" gap="3" py="6">
          <Text color="red" size="2">
            Failed to load access and security data.
          </Text>
          <Text size="1" color="gray">
            {error}
          </Text>
          <Button variant="outline" size="2" onClick={refetch}>
            Retry
          </Button>
        </Flex>
      </Card>
    );
  }

  const extraUsers = Math.max(0, users.length - USERS_DISPLAY_LIMIT);

  return (
    <Flex direction="column" gap="4">
      {canReadPrivileges && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <Key size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">
                Repository Privileges
              </Text>
              <Badge variant="soft" color="gray" size="1">
                {privileges.length} privilege{privileges.length === 1 ? '' : 's'}
              </Badge>
            </Flex>

            <Separator size="4" />

            {privileges.length > 0 ? (
              <Flex direction="column" gap="2">
                {privileges.map((privilege) => (
                  <Flex
                    key={privilege.name}
                    align="center"
                    justify="between"
                    gap="2"
                    wrap="wrap"
                  >
                    <Flex align="center" gap="2">
                      <Text size="1" weight="medium">
                        <code>{privilege.name}</code>
                      </Text>
                      {privilege.description && (
                        <Text size="1" color="gray">
                          ({privilege.description})
                        </Text>
                      )}
                    </Flex>
                    <Badge
                      size="1"
                      color={privilege.actions?.includes('*') ? 'green' : 'blue'}
                    >
                      {privilege.actions?.join(', ') || 'Read'}
                    </Badge>
                  </Flex>
                ))}
              </Flex>
            ) : (
              <Box py="4">
                <Text size="2" color="gray">
                  No privileges target this repository.
                </Text>
              </Box>
            )}
          </Flex>
        </Card>
      )}

      {canReadRoles && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <UsersIcon size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">
                Roles with Access
              </Text>
              <Badge variant="soft" color="gray" size="1">
                {roles.length} role{roles.length === 1 ? '' : 's'}
              </Badge>
            </Flex>

            <Separator size="4" />

            {roles.length > 0 ? (
              <Table.Root variant="surface" size="1">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Role</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Privileges</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {roles.map((role) => (
                    <Table.Row key={role.id}>
                      <Table.Cell>
                        <Text size="1" weight="medium">
                          {role.name}
                        </Text>
                        {role.description && (
                          <Text size="1" color="gray" as="div">
                            {role.description}
                          </Text>
                        )}
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="1" color="gray">
                          {role.privileges.length} privilege
                          {role.privileges.length === 1 ? '' : 's'}
                        </Text>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            ) : (
              <Box py="4">
                <Text size="2" color="gray">
                  No roles reference the privileges for this repository.
                </Text>
              </Box>
            )}
          </Flex>
        </Card>
      )}

      {canReadUsers && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <User size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">
                Users with Access
              </Text>
              <Badge variant="soft" color="gray" size="1">
                {users.length} user{users.length === 1 ? '' : 's'}
              </Badge>
            </Flex>

            <Separator size="4" />

            {users.length > 0 ? (
              <>
                <Table.Root variant="surface" size="1">
                  <Table.Header>
                    <Table.Row>
                      <Table.ColumnHeaderCell>User</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Via Role</Table.ColumnHeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {users.slice(0, USERS_DISPLAY_LIMIT).map((user) => (
                      <Table.Row key={user.userId}>
                        <Table.Cell>
                          <Text size="1" weight="medium">
                            {user.userId}
                          </Text>
                          {(user.firstName || user.lastName) && (
                            <Text size="1" color="gray">
                              {' '}
                              ({[user.firstName, user.lastName].filter(Boolean).join(' ')})
                            </Text>
                          )}
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="1">{user.roles?.join(', ') || '—'}</Text>
                        </Table.Cell>
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table.Root>
                {extraUsers > 0 && (
                  <Text size="1" color="gray">
                    ... and {extraUsers} more user{extraUsers === 1 ? '' : 's'}
                  </Text>
                )}
              </>
            ) : (
              <Box py="4">
                <Text size="2" color="gray">
                  No users have access via the roles for this repository.
                </Text>
              </Box>
            )}
          </Flex>
        </Card>
      )}

      {canReadAnonymous && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <Ghost size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">
                Anonymous Access
              </Text>
              {anonymousAccess && (
                <Badge
                  color={anonymousAccess.enabled ? 'green' : 'gray'}
                  size="1"
                >
                  {anonymousAccess.enabled ? (
                    <>
                      <CheckCircle size={12} /> Enabled
                    </>
                  ) : (
                    <>
                      <XCircle size={12} /> Disabled
                    </>
                  )}
                </Badge>
              )}
            </Flex>

            <Separator size="4" />

            {anonymousAccess ? (
              <Flex direction="column" gap="2">
                {anonymousAccess.enabled && (
                  <>
                    <Flex align="center" justify="between">
                      <Text size="1" color="gray">
                        User ID
                      </Text>
                      <Text size="1">{anonymousAccess.userId || '—'}</Text>
                    </Flex>
                    <Flex align="center" justify="between">
                      <Text size="1" color="gray">
                        Realm
                      </Text>
                      <Text size="1">
                        {anonymousAccess.realmName || anonymousAccess.realm || '—'}
                      </Text>
                    </Flex>
                  </>
                )}
                {!anonymousAccess.enabled && (
                  <Text size="2" color="gray">
                    Anonymous users cannot access this repository.
                  </Text>
                )}
              </Flex>
            ) : (
              <Box py="4">
                <Text size="2" color="gray">
                  Anonymous access settings are not available on this instance.
                </Text>
              </Box>
            )}
          </Flex>
        </Card>
      )}
    </Flex>
  );
}

export default RepositoryAccessSecurityTab;
