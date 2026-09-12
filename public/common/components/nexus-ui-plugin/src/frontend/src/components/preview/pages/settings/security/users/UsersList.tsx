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

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Badge, Box, Flex, Text, TextField } from '@radix-ui/themes';
import { Search, Users, Plus, UserPlus } from 'lucide-react';

import {
  EntityTable,
  FilterSidebar,
  EmptyState,
  HelpSection,
  type TableColumn,
  type FilterSection,
} from '../../../../shared';

import { useUsersApi } from './useUsersApi';
import {
  User,
  SortDirection,
  UserSortField,
  DEFAULT_SOURCE,
  UsersListProps,
} from './types';

import './UsersList.scss';

/**
 * UsersList - Displays users using SUPER UI/UX standard
 *
 * Features:
 * - FilterSidebar with Source and Status filters
 * - EntityTable with sortable columns
 * - Search functionality
 * - Loading, error, and empty states
 */
const CLOUD_SOURCE = 'OAuth2';

export function UsersList({
  onSelect,
  getRowAriaLabel,
  onCreate,
  isCloud = false,
}: UsersListProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [filter, setFilter] = useState('');
  const [debouncedFilter, setDebouncedFilter] = useState('');
  const [sourceFilter, setSourceFilter] = useState<string[]>([]);
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [sortField, setSortField] = useState<UserSortField>('userId');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [loadingUsers, setLoadingUsers] = useState(true);

  const { error, setError, fetchUsers } = useUsersApi();

  const activeSource = isCloud ? CLOUD_SOURCE : undefined;

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedFilter(filter), 300);
    return () => clearTimeout(timer);
  }, [filter]);

  // The filter must be applied server-side; per-source result caps
  // (100 for LDAP, 1000 for SAML) mean client-side filtering can only
  // narrow the first page and would silently miss matches beyond it.
  useEffect(() => {
    let cancelled = false;
    const loadUsers = async () => {
      setLoadingUsers(true);
      try {
        const data = await fetchUsers(debouncedFilter, activeSource);
        if (!cancelled) {
          setUsers(data);
        }
      } catch (err: any) {
        if (!cancelled) {
          setError(err.message);
        }
      } finally {
        if (!cancelled) {
          setLoadingUsers(false);
        }
      }
    };

    loadUsers();
    return () => {
      cancelled = true;
    };
  }, [fetchUsers, setError, activeSource, debouncedFilter]);

  // Get unique sources with counts for filter
  const sourceOptions = useMemo(() => {
    const sourceCounts = new Map<string, number>();

    users.forEach((user) => {
      const source = user.source || DEFAULT_SOURCE;
      sourceCounts.set(source, (sourceCounts.get(source) || 0) + 1);
    });

    return Array.from(sourceCounts.entries())
      .sort(([a], [b]) => {
        // Put "Local" first
        if (a === DEFAULT_SOURCE) return -1;
        if (b === DEFAULT_SOURCE) return 1;
        return a.localeCompare(b);
      })
      .map(([value, count]) => ({
        value,
        label: value === DEFAULT_SOURCE ? 'Local' : value,
        count,
      }));
  }, [users]);

  // Get status options with counts
  const statusOptions = useMemo(() => {
    const statusCounts = new Map<string, number>();

    users.forEach((user) => {
      const status = user.status === 'active' ? 'active' : 'inactive';
      statusCounts.set(status, (statusCounts.get(status) || 0) + 1);
    });

    return [
      {
        value: 'active',
        label: 'Active',
        count: statusCounts.get('active') || 0,
      },
      {
        value: 'inactive',
        label: 'Inactive',
        count: statusCounts.get('inactive') || 0,
      },
    ];
  }, [users]);

  // Client-side facets applied on top of the server-filtered result set.
  // The text filter itself is applied server-side (see the load effect above)
  // so we only refine by source and status here.
  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      if (sourceFilter.length > 0) {
        const userSource = user.source || DEFAULT_SOURCE;
        if (!sourceFilter.includes(userSource)) {
          return false;
        }
      }

      if (statusFilter.length > 0) {
        const userStatus = user.status === 'active' ? 'active' : 'inactive';
        if (!statusFilter.includes(userStatus)) {
          return false;
        }
      }

      return true;
    });
  }, [users, sourceFilter, statusFilter]);

  // Sort users
  const sortedUsers = useMemo(() => {
    if (!sortDirection) return filteredUsers;

    return [...filteredUsers].sort((a, b) => {
      let aVal: string = '';
      let bVal: string = '';

      switch (sortField) {
        case 'userId':
          aVal = a.userId || '';
          bVal = b.userId || '';
          break;
        case 'firstName':
          aVal = a.firstName || '';
          bVal = b.firstName || '';
          break;
        case 'lastName':
          aVal = a.lastName || '';
          bVal = b.lastName || '';
          break;
        case 'email':
          aVal = a.emailAddress || a.email || '';
          bVal = b.emailAddress || b.email || '';
          break;
        case 'source':
          aVal = a.source || '';
          bVal = b.source || '';
          break;
        case 'status':
          aVal = a.status || '';
          bVal = b.status || '';
          break;
      }

      const comparison = aVal.toLowerCase().localeCompare(bVal.toLowerCase());
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [filteredUsers, sortField, sortDirection]);

  const handleSort = useCallback((columnId: string) => {
    const field = columnId as UserSortField;
    if (sortField === field) {
      if (sortDirection === 'asc') {
        setSortDirection('desc');
      } else if (sortDirection === 'desc') {
        setSortDirection(null);
      } else {
        setSortDirection('asc');
      }
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  }, [sortField, sortDirection]);

  const handleRowClick = useCallback((user: User) => {
    onSelect(user.userId, user.source);
  }, [onSelect]);

  const handleFilterChange = useCallback((sectionId: string, value: string | string[]) => {
    if (sectionId === 'source') {
      setSourceFilter(value as string[]);
    } else if (sectionId === 'status') {
      setStatusFilter(value as string[]);
    } else if (sectionId === 'search') {
      setFilter(value as string);
    }
  }, []);

  const handleClearFilters = useCallback(() => {
    setFilter('');
    // Sync the debounced filter so Clear is immediate rather than waiting
    // 300ms for the debounce to fire — this is a discrete action, not typing.
    setDebouncedFilter('');
    setSourceFilter([]);
    setStatusFilter([]);
  }, []);

  const handleRetry = useCallback(async () => {
    setError(null);
    setLoadingUsers(true);
    try {
      const data = await fetchUsers(debouncedFilter, activeSource);
      setUsers(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoadingUsers(false);
    }
  }, [fetchUsers, setError, activeSource, debouncedFilter]);

  // Filter sidebar sections
  const filterSections: FilterSection[] = useMemo(() => [
    ...(!isCloud ? [{
      id: 'source',
      label: 'Source',
      type: 'checkbox' as const,
      options: sourceOptions,
      value: sourceFilter,
      defaultExpanded: true,
    }] : []),
    {
      id: 'status',
      label: 'Status',
      type: 'checkbox' as const,
      options: statusOptions,
      value: statusFilter,
      defaultExpanded: true,
    },
  ], [isCloud, sourceOptions, statusOptions, sourceFilter, statusFilter]);

  // Table columns
  const columns: TableColumn<User>[] = useMemo(() => [
    {
      id: 'userId',
      header: 'User ID',
      accessor: (user) => user.userId,
      sortable: true,
      width: '150px',
    },
    {
      id: 'firstName',
      header: 'First Name',
      accessor: (user) => user.firstName || '—',
      sortable: true,
      width: '150px',
    },
    {
      id: 'lastName',
      header: 'Last Name',
      accessor: (user) => user.lastName || '—',
      sortable: true,
      width: '150px',
    },
    {
      id: 'email',
      header: 'Email',
      accessor: (user) => user.emailAddress || user.email || '—',
      sortable: true,
      width: '200px',
    },
    {
      id: 'source',
      header: 'Source',
      accessor: (user) => {
        const source = user.source || DEFAULT_SOURCE;
        return source === DEFAULT_SOURCE ? 'Local' : source;
      },
      sortable: true,
      width: '120px',
    },
    {
      id: 'status',
      header: 'Status',
      accessor: (user) => {
        const isActive = user.status === 'active';
        return (
          <Badge variant="soft" color={isActive ? 'green' : 'gray'} size="1">
            {isActive ? 'Active' : 'Inactive'}
          </Badge>
        );
      },
      sortable: true,
      width: '120px',
    },
  ], []);

  // Empty state
  const emptyState = useMemo(() => {
    const hasFilters = filter || sourceFilter.length > 0 || statusFilter.length > 0;

    if (hasFilters) {
      return (
        <EmptyState
          icon={Users}
          title="No Matching Users"
          description="No users match your current filters. Try adjusting your filter criteria."
          action={{
            label: 'Clear Filters',
            onClick: handleClearFilters,
          }}
        />
      );
    }

    return (
      <EmptyState
        icon={Users}
        title="No Users"
        description={isCloud ? 'Invite users to start managing access.' : 'Create your first local user to start managing access.'}
        action={onCreate ? {
          label: isCloud ? 'Invite User' : 'Create Local User',
          onClick: onCreate,
          icon: isCloud ? UserPlus : Plus,
        } : undefined}
        secondaryAction={{
          label: 'Learn more about users',
          href: 'http://links.sonatype.com/products/nxrm3/docs/users',
        }}
        tip="Users are assigned roles which grant them access to Nexus Repository features."
      />
    );
  }, [filter, sourceFilter, statusFilter, handleClearFilters, onCreate, isCloud]);

  return (
    <Flex className="users-list" gap="4" data-testid="users-list">
      {/* Filter Sidebar */}
      <FilterSidebar
        sections={filterSections}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
        disabled={loadingUsers}
        className="users-list__sidebar"
      />

      {/* Main Content */}
      <Box className="users-list__main">
        {/* Search Bar */}
        <Box className="users-list__search-container">
          <TextField.Root
            placeholder="Search by user ID..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="users-list__search-input"
            data-testid="users-search"
            data-analytics-id="nxrm-user-filter"
            aria-label="Search users by user ID"
          >
            <TextField.Slot>
              <Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        {/* Table */}
        <EntityTable<User>
          data={sortedUsers}
          columns={columns}
          getRowKey={(user) => `${user.userId}-${user.source || DEFAULT_SOURCE}`}
          onRowClick={handleRowClick}
          getRowAriaLabel={getRowAriaLabel}
          loading={loadingUsers}
          loadingMessage="Loading users..."
          error={error || undefined}
          onRetry={handleRetry}
          emptyState={emptyState}
          sortBy={sortField}
          sortDirection={sortDirection || undefined}
          onSort={handleSort}
          showRowArrow={false}
          clickable={true}
          ariaLabel="Users list"
          className="users-list__table"
        />

        {/* Summary */}
        {!loadingUsers && !error && sortedUsers.length > 0 && (
          <Box className="users-list__summary">
            <Text size="2" color="gray">
              Showing {sortedUsers.length} users
            </Text>
          </Box>
        )}

        {/* Help Section */}
        <HelpSection
          title="About Users"
          content="Users represent individuals or systems that access Nexus Repository. Each user is assigned roles which grant them specific permissions. Local users are created and managed within Nexus, while external users are authenticated through LDAP, SAML, or other configured authentication sources."
          docLink={{
            label: 'View Documentation',
            href: 'http://links.sonatype.com/products/nxrm3/docs/users',
          }}
          className="users-list__help"
        />
      </Box>
    </Flex>
  );
}

export default UsersList;
