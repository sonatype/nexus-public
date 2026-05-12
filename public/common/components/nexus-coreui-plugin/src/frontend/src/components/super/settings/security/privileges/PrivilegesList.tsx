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
import { Box, Flex, Text, TextField, IconButton, Tooltip } from '@radix-ui/themes';
import { Search, Key, Plus, Lock, Eye, Pencil, Trash2 } from 'lucide-react';

import {
  EntityTable,
  FilterSidebar,
  EmptyState,
  HelpSection,
  type TableColumn,
  type FilterSection,
} from '../../../../shared';

import { usePrivilegeList } from './usePrivilegeList';
import {
  Privilege,
  PrivilegesListProps,
  getPrivilegeTypeLabel,
  isReadOnlyPrivilege,
} from './types';

import './PrivilegesList.scss';

/**
 * PrivilegesList - Displays privileges using SUPER UI/UX standard
 * Now uses XState for state management
 *
 * Features:
 * - FilterSidebar with Type filter
 * - EntityTable with sortable columns
 * - Search functionality
 * - Loading, error, and empty states
 */
export function PrivilegesList({ onSelect, onEdit, onDelete, onCreate, canEdit = true, canDelete = true }: PrivilegesListProps) {
  // Use XState list hook
  const {
    data: privileges,
    loading,
    error,
    filters,
    setFilters,
    sortField,
    sortDirection,
    setSort,
    typeCounts,
    readOnlyCounts,
    handleFilterChange,
    handleRowClick,
    handleCreate,
  } = usePrivilegeList({ onRowClick: onSelect, onCreate });

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 25;

  // Get unique types with counts for filter sidebar
  const typeOptions = useMemo(() => {
    return typeCounts.map(([value, count]) => ({
      value,
      label: getPrivilegeTypeLabel(value),
      count,
    }));
  }, [typeCounts]);

  const handleClearFilters = () => {
    setFilters({ filter: '', typeFilter: [], readOnlyFilter: [] });
    setCurrentPage(1);
  };

  // Calculate paginated data
  const totalPages = Math.ceil(privileges.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedPrivileges = privileges.slice(startIndex, endIndex);

  // Reset to page 1 when filters change
  React.useEffect(() => {
    setCurrentPage(1);
  }, [filters.filter, filters.typeFilter, filters.readOnlyFilter]);

  // Read-only filter options (Locked = system/read-only, Unlocked = editable)
  const readOnlyOptions = useMemo(
    () => [
      { value: 'locked', label: 'Locked', count: readOnlyCounts.locked },
      { value: 'unlocked', label: 'Unlocked', count: readOnlyCounts.unlocked },
    ],
    [readOnlyCounts]
  );

  // Filter sections for sidebar
  const filterSections = useMemo<FilterSection[]>(() => {
    return [
      {
        id: 'readOnly',
        label: 'Status',
        type: 'checkbox',
        options: readOnlyOptions,
        value: filters.readOnlyFilter ?? [],
      },
      {
        id: 'type',
        label: 'Type',
        type: 'checkbox',
        options: typeOptions,
        value: filters.typeFilter,
      },
    ];
  }, [typeOptions, readOnlyOptions, filters.typeFilter, filters.readOnlyFilter]);

  // Table columns
  const columns = useMemo<TableColumn<Privilege>[]>(() => [
    {
      id: 'name',
      header: 'Name',
      accessor: (priv) => (
        <Flex align="center" gap="2">
          {priv.readOnly && (
            <Lock size={14} className="privileges-list__readonly-icon" />
          )}
          <Text>{priv.name}</Text>
        </Flex>
      ),
      sortable: true,
      width: '200px',
    },
    {
      id: 'description',
      header: 'Description',
      accessor: (priv) => priv.description || '—',
      sortable: true,
      width: '300px',
    },
    {
      id: 'type',
      header: 'Type',
      accessor: (priv) => getPrivilegeTypeLabel(priv.type),
      sortable: true,
      width: '150px',
    },
    {
      id: 'permission',
      header: 'Permission',
      accessor: (priv) => (
        <code className="privileges-list__permission-code">{priv.permission}</code>
      ),
      sortable: true,
      width: '250px',
    },
    {
      id: 'actions',
      header: '',
      accessor: (priv) => (
        <Flex gap="2" justify="end">
          <Tooltip content="View Profile">
            <IconButton
              variant="ghost"
              size="1"
              onClick={(e) => { e.stopPropagation(); onSelect(priv.name); }}
              aria-label={`View profile: ${priv.name}`}
            >
              <Eye size={14} />
            </IconButton>
          </Tooltip>
          {canEdit && !isReadOnlyPrivilege(priv) && (
            <Tooltip content="Edit">
              <IconButton
                variant="ghost"
                size="1"
                onClick={(e) => {
                  e.stopPropagation();
                  (onEdit || onSelect)(priv.name);
                }}
                aria-label={`Edit ${priv.name}`}
              >
                <Pencil size={14} />
              </IconButton>
            </Tooltip>
          )}
          {canDelete && onDelete && !isReadOnlyPrivilege(priv) && (
            <Tooltip content="Delete">
              <IconButton
                variant="ghost"
                size="1"
                onClick={(e) => { e.stopPropagation(); onDelete(priv.name, priv.name); }}
                aria-label={`Delete ${priv.name}`}
              >
                <Trash2 size={14} />
              </IconButton>
            </Tooltip>
          )}
        </Flex>
      ),
      width: '100px',
    },
  ], [onSelect, onEdit, onDelete, canEdit, canDelete]);

  // Empty state
  const emptyState = useMemo(() => {
    const readOnlyFilter = filters.readOnlyFilter ?? [];
    const hasFilters = filters.filter || filters.typeFilter.length > 0 || readOnlyFilter.length > 0;

    if (hasFilters) {
      return (
        <EmptyState
          icon={Key}
          title="No Matching Privileges"
          description="No privileges match your current filters. Try adjusting your filter criteria."
          action={{
            label: 'Clear Filters',
            onClick: handleClearFilters,
          }}
        />
      );
    }

    return (
      <EmptyState
        icon={Key}
        title="No Privileges"
        description="Create your first privilege to start defining access controls."
        action={onCreate ? {
          label: 'Create Privilege',
          onClick: handleCreate,
          icon: Plus,
        } : undefined}
        secondaryAction={{
          label: 'Learn more about privileges',
          href: 'http://links.sonatype.com/products/nxrm3/docs/privileges',
        }}
        tip="Privileges define granular permissions that can be assigned to roles."
      />
    );
  }, [filters, handleClearFilters, onCreate, handleCreate]);

  return (
    <Flex className="privileges-list" gap="4" data-testid="privileges-list">
      {/* Filter Sidebar */}
      <FilterSidebar
        sections={filterSections}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
        disabled={loading}
        className="privileges-list__sidebar"
      />

      {/* Main Content */}
      <Box className="privileges-list__main">
        {/* Search Bar */}
        <Box className="privileges-list__search-container">
          <TextField.Root
            placeholder="Search privileges by name, description, or permission..."
            value={filters.filter}
            onChange={(e) => setFilters({ filter: e.target.value })}
            className="privileges-list__search-input"
            data-testid="privileges-search"
            data-analytics-id="nxrm-privilege-filter"
          >
            <TextField.Slot>
              <Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        {/* Table */}
        <EntityTable<Privilege>
          data={paginatedPrivileges}
          columns={columns}
          getRowKey={(priv) => priv.id || priv.name}
          onRowClick={handleRowClick}
          loading={loading}
          loadingMessage="Loading privileges..."
          error={error || undefined}
          onRetry={() => window.location.reload()}
          emptyState={emptyState}
          sortBy={sortField}
          sortDirection={sortDirection === 'asc' ? 'asc' : sortDirection === 'desc' ? 'desc' : undefined}
          onSort={setSort}
          showRowArrow={false}
          clickable={true}
          ariaLabel="Privileges list"
          className="privileges-list__table"
        />

        {/* Pagination & Summary */}
        {!loading && !error && privileges.length > 0 && (
          <Flex justify="between" align="center" className="privileges-list__footer">
            <Text size="2" color="gray">
              Showing {startIndex + 1}-{Math.min(endIndex, privileges.length)} of {privileges.length} privileges
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
          title="What is a Privilege?"
          content="A privilege is a granular permission that controls access to a specific operation or resource in Nexus Repository. Privileges are assigned to roles, which are then assigned to users to define what actions they can perform."
          docLink={{
            label: 'View Documentation',
            href: 'http://links.sonatype.com/products/nxrm3/docs/privileges',
          }}
          className="privileges-list__help"
        />
      </Box>
    </Flex>
  );
}

export default PrivilegesList;
