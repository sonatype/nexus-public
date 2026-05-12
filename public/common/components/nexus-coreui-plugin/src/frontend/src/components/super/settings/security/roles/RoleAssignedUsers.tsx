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
import { Box, Flex, Text, Callout } from '@radix-ui/themes';
import { Info, User as UserIcon } from 'lucide-react';
import { LoadingState, ErrorState, EntityTable, TableColumn } from '../../../../shared';
import { useUsersApi } from '../../security/users/useUsersApi';
import { User } from '../../security/users/types';

interface RoleAssignedUsersProps {
  roleId: string;
}

/**
 * RoleAssignedUsers - Displays users who have the specified role assigned.
 */
export function RoleAssignedUsers({ roleId }: RoleAssignedUsersProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { fetchUsers } = useUsersApi();

  const loadUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      // Fetch all users and filter locally by roleId
      // Note: This matches current UI logic for usage tabs
      const allUsers = await fetchUsers();
      const assignedUsers = allUsers.filter(user => user.roles?.includes(roleId));
      setUsers(assignedUsers);
    } catch (err: any) {
      setError(err.message || 'Failed to load assigned users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [roleId]);

  const columns: TableColumn<User>[] = useMemo(() => [
    {
      id: 'userId',
      header: 'User ID',
      accessor: (user) => (
        <Flex align="center" gap="2">
          <UserIcon size={16} color="var(--blue-9)" />
          <Text weight="medium">{user.userId}</Text>
        </Flex>
      ),
      sortable: true,
    },
    {
      id: 'name',
      header: 'Name',
      accessor: (user) => `${user.firstName} ${user.lastName}`,
      sortable: true,
    },
    {
      id: 'email',
      header: 'Email',
      accessor: (user) => user.emailAddress || '—',
      sortable: true,
    },
    {
      id: 'source',
      header: 'Source',
      accessor: (user) => user.source,
      sortable: true,
    }
  ], []);

  if (loading) return <LoadingState message="Checking assigned users..." />;
  if (error) return <ErrorState message={error} onRetry={loadUsers} />;

  return (
    <Box p="4" className="role-assigned-users">
      <Callout.Root color="blue" mb="4" size="1">
        <Callout.Icon>
          <Info size={16} />
        </Callout.Icon>
        <Callout.Text>
          Showing users who are directly assigned the role <strong>{roleId}</strong>.
        </Callout.Text>
      </Callout.Root>

      {users.length > 0 ? (
        <EntityTable<User>
          data={users}
          columns={columns}
          getRowKey={(user) => user.userId}
          ariaLabel="Assigned users list"
        />
      ) : (
        <Flex 
          direction="column" 
          align="center" 
          justify="center" 
          p="6" 
          style={{ background: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }}
        >
          <Text color="gray" size="2">No users are currently assigned this role.</Text>
        </Flex>
      )}
    </Box>
  );
}

export default RoleAssignedUsers;
