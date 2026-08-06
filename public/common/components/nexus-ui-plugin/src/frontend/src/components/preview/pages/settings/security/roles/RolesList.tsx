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
import { Box, Flex, Text, TextField, IconButton, Tooltip } from '@radix-ui/themes';
import { Search, Lock, Shield, Plus, Pencil, Trash2, Eye } from 'lucide-react';

import {
  EntityTable,
  FilterSidebar,
  EmptyState,
  HelpSection,
  type TableColumn,
  type FilterSection,
} from '../../../../shared';

import { useRolesApi } from './useRolesApi';
import {
  Role,
  SortDirection,
  RoleSortField,
  RolesListProps,
  isReadOnlyRole,
  ROLES_PAGE_SIZE,
} from './types';

import './RolesList.scss';

type ManagementType = 'user' | 'system';

/**
 * RolesList - Displays roles using SUPER UI/UX standard
 */
export function RolesList({ onSelect, onDelete, onCreate, canDelete = true }: RolesListProps) {
  const [roles, setRoles] = useState<Role[]>([]);
  const [filter, setFilter] = useState('');
  const [managementFilter, setManagementFilter] = useState<ManagementType[]>([]);
  const [sortField, setSortField] = useState<RoleSortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [loadingRoles, setLoadingRoles] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);

  const { error, setError, fetchRoles } = useRolesApi();

  // Load all roles (API uses ?source=default to exclude raw external roles)
  useEffect(() => {
    const loadRoles = async () => {
      setLoadingRoles(true);
      try {
        const data = await fetchRoles();
        setRoles(data);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoadingRoles(false);
      }
    };

    loadRoles();
  }, [fetchRoles, setError]);

  // Get management options for filter
  const managementOptions = useMemo(() => {
    const managementCounts = { user: 0, system: 0 };

    roles.forEach((role) => {
      if (role.readOnly) {
        managementCounts.system++;
      } else {
        managementCounts.user++;
      }
    });

    return [
      { value: 'user', label: 'User-Managed', count: managementCounts.user },
      { value: 'system', label: 'System-Managed', count: managementCounts.system },
    ].filter(opt => opt.count > 0) as Array<{ value: ManagementType; label: string; count: number }>;
  }, [roles]);

  // Filter roles
  const filteredRoles = useMemo(() => {
    return roles.filter((role) => {
      // Text filter
      if (filter) {
        const searchLower = filter.toLowerCase();
        const matchesName = role.name?.toLowerCase().includes(searchLower);
        const matchesId = role.id?.toLowerCase().includes(searchLower);
        const matchesDescription = role.description?.toLowerCase().includes(searchLower);
        if (!((matchesName || matchesId ) || matchesDescription)) {
          return false;
        }
      }

      // Management filter (User-Managed, System-Managed)
      if (managementFilter.length > 0) {
        const isSystem = role.readOnly;
        const isUser = !role.readOnly;
        if (managementFilter.includes('system') && !isSystem) {
          if (!managementFilter.includes('user')) return false;
        }
        if (managementFilter.includes('user') && !isUser) {
          if (!managementFilter.includes('system')) return false;
        }
      }

      return true;
    });
  }, [roles, filter, managementFilter]);

  // Sort roles
  const sortedRoles = useMemo(() => {
    if (!sortDirection) return filteredRoles;

    return [...filteredRoles].sort((a, b) => {
      let aVal: string = '';
      let bVal: string = '';

      switch (sortField) {
        case 'name':
          aVal = a.name || '';
          bVal = b.name || '';
          break;
        case 'id':
          aVal = a.id || '';
          bVal = b.id || '';
          break;
        case 'description':
          aVal = a.description || '';
          bVal = b.description || '';
          break;
        case 'source':
          aVal = a.source || '';
          bVal = b.source || '';
          break;
      }

      const comparison = aVal.toLowerCase().localeCompare(bVal.toLowerCase());
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [filteredRoles, sortField, sortDirection]);

  const handleSort = useCallback((columnId: string) => {
    const field = columnId as RoleSortField;
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

  const handleRowClick = useCallback((role: Role) => {
    onSelect(role.id, 'profile');
  }, [onSelect]);

  const handleFilterChange = useCallback((sectionId: string, value: string | string[]) => {
    if (sectionId === 'management') {
      setManagementFilter(value as ManagementType[]);
    } else if (sectionId === 'search') {
      setFilter(value as string);
    }
  }, []);

  const handleClearFilters = useCallback(() => {
    setFilter('');
    setManagementFilter([]);
    setCurrentPage(1);
  }, []);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, []);

  // Calculate paginated data
  const totalPages = Math.ceil(sortedRoles.length / ROLES_PAGE_SIZE);
  const startIndex = (currentPage - 1) * ROLES_PAGE_SIZE;
  const endIndex = startIndex + ROLES_PAGE_SIZE;
  const paginatedRoles = sortedRoles.slice(startIndex, endIndex);

  const handleRetry = useCallback(async () => {
    setError(null);
    setLoadingRoles(true);
    try {
      const data = await fetchRoles();
      setRoles(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoadingRoles(false);
    }
  }, [fetchRoles, setError]);

  // Filter sidebar sections
  const filterSections: FilterSection[] = useMemo(() => [
    {
      id: 'management',
      label: 'Management',
      type: 'checkbox',
      options: managementOptions,
      value: managementFilter,
      defaultExpanded: true,
    },
  ], [managementOptions, managementFilter]);

  // Table columns
  const columns: TableColumn<Role>[] = useMemo(() => [
    {
      id: 'name',
      header: 'Name',
      accessor: (role) => (
        <Flex align="center" gap="2">
          <Text weight="medium">{role.name}</Text>
          {isReadOnlyRole(role) && (
            <Lock size={12} className="roles-list__readonly-icon" title="Read Only" />
          )}
        </Flex>
      ),
      sortable: true,
      width: '200px',
    },
    {
      id: 'id',
      header: 'ID',
      accessor: (role) => role.id,
      sortable: true,
      width: '150px',
    },
    {
      id: 'description',
      header: 'Description',
      accessor: (role) => role.description || '—',
      sortable: true,
      width: '300px',
    },
    {
      id: 'actions',
      header: '',
      accessor: (role) => (
        <Flex gap="2" justify="end">
          {/* View button for all roles */}
          <Tooltip content="View Role">
            <IconButton
              variant="ghost"
              size="1"
              onClick={(e) => { e.stopPropagation(); onSelect(role.id, 'profile'); }}
              aria-label={`View ${role.name}`}
            >
              <Eye size={14} />
            </IconButton>
          </Tooltip>
          {/* Edit and Delete only for non-read-only roles */}
          {!isReadOnlyRole(role) && (
            <>
              <Tooltip content="Edit Role">
                <IconButton
                  variant="ghost"
                  size="1"
                  onClick={(e) => { e.stopPropagation(); onSelect(role.id, 'edit'); }}
                  aria-label={`Edit ${role.name}`}
                >
                  <Pencil size={14} />
                </IconButton>
              </Tooltip>
              {canDelete && onDelete && (
                <Tooltip content="Delete Role">
                  <IconButton
                    variant="ghost"
                    size="1"
                    onClick={(e) => { e.stopPropagation(); onDelete(role.id, role.name); }}
                    aria-label={`Delete ${role.name}`}
                  >
                    <Trash2 size={14} />
                  </IconButton>
                </Tooltip>
              )}
            </>
          )}
        </Flex>
      ),
      width: '120px',
    },
  ], [onSelect, onDelete, canDelete]);

  // Empty state
  const emptyState = useMemo(() => {
    const hasFilters = filter || managementFilter.length > 0;

    if (hasFilters) {
      return (
        <EmptyState
          icon={Shield}
          title="No Matching Roles"
          description="No roles match your current filters. Try adjusting your filter criteria."
          action={{
            label: 'Clear Filters',
            onClick: handleClearFilters,
          }}
        />
      );
    }

    return (
      <EmptyState
        icon={Shield}
        title="No Roles"
        description="Create your first role to group privileges together."
        action={onCreate ? {
          label: 'Create Role',
          onClick: onCreate,
          icon: Plus,
        } : undefined}
        secondaryAction={{
          label: 'Learn more about roles',
          href: 'http://links.sonatype.com/products/nxrm3/docs/roles',
        }}
        tip="Roles combine multiple privileges and can be assigned to users."
      />
    );
  }, [filter, managementFilter, handleClearFilters, onCreate]);

  return (
    <Flex className="roles-list" gap="4" data-testid="roles-list">
      {/* Filter Sidebar */}
      <FilterSidebar
        sections={filterSections}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
        disabled={loadingRoles}
        className="roles-list__sidebar"
      />

      {/* Main Content */}
      <Box className="roles-list__main">
        {/* Search Bar */}
        <Box className="roles-list__search-container">
          <TextField.Root
            placeholder="Search roles by name, ID, or description..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="roles-list__search-input"
            data-testid="roles-search"
          >
            <TextField.Slot>
              <Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        {/* Table */}
        <EntityTable<Role>
          data={paginatedRoles}
          columns={columns}
          getRowKey={(role) => role.id}
          onRowClick={handleRowClick}
          loading={loadingRoles}
          loadingMessage="Loading roles..."
          error={error || undefined}
          onRetry={handleRetry}
          emptyState={emptyState}
          sortBy={sortField}
          sortDirection={sortDirection || undefined}
          onSort={handleSort}
          showRowArrow={false}
          clickable={true}
          ariaLabel="Roles list"
          className="roles-list__table"
        />

        {/* Pagination & Summary */}
        {!(loadingRoles || error ) && sortedRoles.length > 0 && (
          <Flex justify="between" align="center" className="roles-list__footer">
            <Text size="2" color="gray">
              Showing {startIndex + 1}-{Math.min(endIndex, sortedRoles.length)} of {sortedRoles.length} roles
            </Text>
            {totalPages > 1 && (
              <Flex gap="2" align="center">
                <IconButton
                  variant="soft"
                  size="1"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(currentPage - 1)}
                  aria-label="Previous page"
                >
                  ←
                </IconButton>
                <Text size="2" color="gray">
                  Page {currentPage} of {totalPages}
                </Text>
                <IconButton
                  variant="soft"
                  size="1"
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage(currentPage + 1)}
                  aria-label="Next page"
                >
                  →
                </IconButton>
              </Flex>
            )}
          </Flex>
        )}

        {/* Help Section */}
        <HelpSection
          title="About Roles"
          content="Roles are collections of privileges that define a set of permissions. By assigning roles to users, you can control what actions they can perform in Nexus Repository. Roles can also contain other roles to create hierarchical permission structures."
          docLink={{
            label: 'View Documentation',
            href: 'http://links.sonatype.com/products/nxrm3/docs/roles',
          }}
          className="roles-list__help"
        />
      </Box>
    </Flex>
  );
}

export default RolesList;
