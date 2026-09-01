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

import React, { useMemo, useState } from 'react';
import { Box, Flex, Heading, Spinner, Table, Text, TextField } from '@radix-ui/themes';

import { getSourceLabel } from '../../../security/users/types';
import { useEndpointAccess } from '../hooks/useEndpointAccess';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';

const ROLES_PROFILE_BASE = 'preview/admin/security/roles';
const USERS_BASE = 'preview/admin/security/users';

function roleProfileHash(roleId: string): string {
  return `#${ROLES_PROFILE_BASE}/${encodeURIComponent(roleId)}/profile`;
}

function userProfileHash(userId: string, source: string): string {
  return `#${USERS_BASE}/${encodeURIComponent(userId)}/${encodeURIComponent(source)}/profile`;
}

export interface WhoHasAccessTabProps {
  row: MergedApiEndpoint;
  /** When false, skip loading security data (tab not visible) */
  active: boolean;
}

export function WhoHasAccessTab({ row, active }: WhoHasAccessTabProps) {
  const { loading, error, qualifyingRoles, usersWithAccess, noMappedPermissions } = useEndpointAccess(
    row,
    active
  );
  const [userFilter, setUserFilter] = useState('');

  const filteredUsers = useMemo(() => {
    const q = userFilter.trim().toLowerCase();
    if (!q) {
      return usersWithAccess;
    }
    return usersWithAccess.filter(({ user: u }) => {
      const blob = `${u.userId} ${u.firstName} ${u.lastName} ${u.emailAddress}`.toLowerCase();
      return blob.includes(q);
    });
  }, [usersWithAccess, userFilter]);

  const roleLabelById = useMemo(() => {
    const m = new Map<string, string>();
    for (const { role } of qualifyingRoles) {
      m.set(role.id, role.name);
    }
    return m;
  }, [qualifyingRoles]);

  const anonymous = row.permission && !row.permission.authenticated;

  return (
    <Box className="api-who-has-access-tab" data-testid="api-who-has-access-tab">
      {anonymous && (
        <Box mb="3" p="2" className="api-who-has-access-tab__banner">
          <Text size="2">
            This endpoint allows anonymous access when anonymous is enabled in Settings → Security → Anonymous Access.
          </Text>
        </Box>
      )}

      {noMappedPermissions && (
        <Text size="2" color="gray" mb="3" as="p">
          No permission strings are mapped for this operation in the registry. Access may be enforced in code or
          undocumented.
        </Text>
      )}

      {error && (
        <Text size="2" color="red" mb="3">
          {error}
        </Text>
      )}

      {loading && (
        <Flex align="center" gap="2" py="4">
          <Spinner />
          <Text size="2">Loading roles and users…</Text>
        </Flex>
      )}

      {!((loading || error ) || noMappedPermissions ) && qualifyingRoles.length === 0 && (
        <Text size="2" color="gray" mb="3">
          No roles contain the required privileges for this endpoint.
        </Text>
      )}

      {!(loading || noMappedPermissions ) && qualifyingRoles.length > 0 && (
        <>
          <Heading as="h3" size="3" weight="medium" mb="2">
            Roles
          </Heading>
          <Text size="1" color="gray" mb="2">
            Roles whose privileges satisfy the required permission{row.permission?.permissions?.length === 1 ? '' : 's'}{' '}
            ({row.permission?.permissions?.[0]?.logical === 'OR' ? 'ANY' : 'ALL'}).
          </Text>
          <Table.Root variant="surface" mb="4">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Role</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Users</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Notes</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {qualifyingRoles.map(({ role, userCount }) => (
                <Table.Row key={role.id}>
                  <Table.Cell>
                    <a href={roleProfileHash(role.id)} className="api-who-has-access-tab__link">
                      {role.name}
                    </a>
                    <Text size="1" color="gray" as="div">
                      {role.id}
                    </Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{userCount}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    {role.readOnly && (
                      <Text size="1" color="gray">
                        Read-only role (e.g. external mapping). Change mappings in the source realm configuration.
                      </Text>
                    )}
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>

          <Flex align="center" justify="between" mb="2" wrap="wrap" gap="2">
            <Heading as="h3" size="3" weight="medium">
              Users
            </Heading>
            <Box style={{ minWidth: 200, flex: '1 1 200px' }}>
              <TextField.Root
                placeholder="Filter users…"
                value={userFilter}
                onChange={(e) => setUserFilter(e.target.value)}
                aria-label="Filter users with access"
              />
            </Box>
          </Flex>

          {filteredUsers.length === 0 ? (
            <Text size="2" color="gray">
              No users currently have access to this endpoint.
            </Text>
          ) : (
            <Table.Root variant="surface">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell>User</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Source</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Granting roles</Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {filteredUsers.map(({ user, grantingRoleIds }) => (
                  <Table.Row key={`${user.userId}-${user.source}`}>
                    <Table.Cell>
                      <a href={userProfileHash(user.userId, user.source)} className="api-who-has-access-tab__link">
                        {user.userId}
                      </a>
                      <Text size="1" color="gray" as="div">
                        {user.firstName} {user.lastName}
                      </Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2">{getSourceLabel(user.source)}</Text>
                      {user.readOnly && (
                        <Text size="1" color="gray" as="div">
                          External (read-only in Nexus)
                        </Text>
                      )}
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2">
                        {grantingRoleIds.map((rid, i) => (
                          <span key={rid}>
                            {i > 0 ? ', ' : ''}
                            <a href={roleProfileHash(rid)} className="api-who-has-access-tab__link">
                              {roleLabelById.get(rid) || rid}
                            </a>
                          </span>
                        ))}
                      </Text>
                    </Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          )}
        </>
      )}
    </Box>
  );
}
