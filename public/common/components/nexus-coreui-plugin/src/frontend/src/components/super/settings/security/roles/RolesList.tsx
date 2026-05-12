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
import { Search, Lock, Shield, Plus, Pencil, Trash2 } from 'lucide-react';

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
  isExternalRole,
  formatRoleSourceDisplay,
} from './types';

import './RolesList.scss';

type RoleType = 'user' | 'system' | 'external';

/**
 * RolesList - Displays roles using SUPER UI/UX standard
 */
export function RolesList({ onSelect, onDelete, onCreate, canDelete = true }: RolesListProps) {
  const [roles, setRoles] = useState<Role[]>([]);
  const [filter, setFilter] = useState('');
  const [sourceFilter, setSourceFilter] = useState<string[]>([]);
  const [typeFilter, setTypeFilter] = useState<RoleType[]>([]);
  const [sortField, setSortField] = useState<RoleSortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [loadingRoles, setLoadingRoles] = useState(true);

  const { error, setError, fetchRoles } = useRolesApi();

  // Load all roles
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

  // Get unique sources with counts for filter
  const { sourceOptions, typeOptions } = useMemo(() => {
    const sourceCounts = new Map<string, number>();
    const typeCounts = { user: 0, system: 0, external: 0 };

    roles.forEach((role) => {
      const source = role.source || 'Default';
      sourceCounts.set(source, (sourceCounts.get(source) || 0) + 1);

      if (isExternalRole(role.source)) {
        typeCounts.external++;
      } else if (role.readOnly) {
        typeCounts.system++;
      } else {
        typeCounts.user++;
      }
    });

    return {
      sourceOptions: Array.from(sourceCounts.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([value, count]) => ({
          value,
          label: value === 'default' ? 'Default' : value,
          count,
        })),
      typeOptions: [
        { value: 'user', label: 'User-Managed', count: typeCounts.user },
        { value: 'system', label: 'System-Managed', count: typeCounts.system },
        { value: 'external', label: 'External', count: typeCounts.external },
      ].filter(opt => opt.count > 0)
    };
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
        if (!matchesName && !matchesId && !matchesDescription) {
          return false;
        }
      }

      // Source filter
      if (sourceFilter.length > 0) {
        const roleSource = role.source || 'default';
        if (!sourceFilter.includes(roleSource)) {
          return false;
        }
      }

      // Role Type filter
      if (typeFilter.length > 0) {
        const isExternal = isExternalRole(role.source);
        const isSystem = !isExternal && role.readOnly;
        const isUser = !isExternal && !role.readOnly;

        if (typeFilter.includes('external') && isExternal) return true;
        if (typeFilter.includes('system') && isSystem) return true;
        if (typeFilter.includes('user') && isUser) return true;
        
        return false;
      }

      return true;
    });
  }, [roles, filter, sourceFilter, typeFilter]);

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
    if (sectionId === 'source') {
      setSourceFilter(value as string[]);
    } else if (sectionId === 'type') {
      setTypeFilter(value as RoleType[]);
    } else if (sectionId === 'search') {
      setFilter(value as string);
    }
  }, []);

  const handleClearFilters = useCallback(() => {
    setFilter('');
    setSourceFilter([]);
    setTypeFilter([]);
  }, []);

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
      id: 'type',
      label: 'Role Type',
      type: 'checkbox',
      options: typeOptions,
      value: typeFilter,
      defaultExpanded: true,
    },
    {
      id: 'source',
      label: 'Source',
      type: 'checkbox',
      options: sourceOptions,
      value: sourceFilter,
      defaultExpanded: false,
    },
  ], [typeOptions, sourceOptions, typeFilter, sourceFilter]);

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
      id: 'source',
      header: 'Source',
      accessor: (role) => formatRoleSourceDisplay(role.source),
      sortable: true,
      width: '120px',
    },
    {
      id: 'actions',
      header: '',
      accessor: (role) => (
        <Flex gap="2" justify="end">
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
      width: '100px',
    },
  ], [onSelect, onDelete, canDelete]);

  // Empty state
  const emptyState = useMemo(() => {
    const hasFilters = filter || sourceFilter.length > 0;

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
  }, [filter, sourceFilter, handleClearFilters, onCreate]);

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
          data={sortedRoles}
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

        {/* Summary */}
        {!loadingRoles && !error && sortedRoles.length > 0 && (
          <Box className="roles-list__summary">
            <Text size="2" color="gray">
              Showing {sortedRoles.length} of {roles.length} roles
            </Text>
          </Box>
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
