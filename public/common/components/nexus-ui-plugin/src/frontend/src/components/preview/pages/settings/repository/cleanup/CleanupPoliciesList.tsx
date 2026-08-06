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
import { Box, TextField } from '@radix-ui/themes';
import { Search, Trash2 } from 'lucide-react';

import {
  EntityTable,
  EmptyState,
  HelpSection,
  type TableColumn,
} from '../../../../shared';
import { useCleanupPoliciesApi } from './useCleanupPoliciesApi';
import { CleanupPolicy } from './types';

import './CleanupPoliciesList.scss';

interface CleanupPoliciesListProps {
  onSelect: (name: string) => void;
  onCreate: () => void;
}

/**
 * CleanupPoliciesList - Sortable, filterable table of cleanup policies
 * Uses shared components: EntityTable, EmptyState, HelpSection
 */
export function CleanupPoliciesList({ onSelect, onCreate }: CleanupPoliciesListProps) {
  const [policies, setPolicies] = useState<CleanupPolicy[]>([]);
  const [filter, setFilter] = useState('');
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const { fetchCleanupPolicies } = useCleanupPoliciesApi();

  // Load policies on mount
  const loadPolicies = useCallback(() => {
    setIsLoading(true);
    setLoadError(null);
    fetchCleanupPolicies()
      .then(setPolicies)
      .catch((err) => setLoadError(err.message))
      .finally(() => setIsLoading(false));
  }, [fetchCleanupPolicies]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  // Filter and sort policies
  const filteredPolicies = useMemo(() => {
    let result = policies;

    // Apply filter
    if (filter) {
      const lowerFilter = filter.toLowerCase();
      result = result.filter(
        (p) =>
          p.name.toLowerCase().includes(lowerFilter) ||
          p.format.toLowerCase().includes(lowerFilter) ||
          (p.notes?.toLowerCase().includes(lowerFilter))
      );
    }

    // Apply sort
    if (sortBy && sortDirection) {
      result = [...result].sort((a, b) => {
        const aVal = (a[sortBy as keyof CleanupPolicy] || '').toString().toLowerCase();
        const bVal = (b[sortBy as keyof CleanupPolicy] || '').toString().toLowerCase();
        const cmp = aVal.localeCompare(bVal);
        return sortDirection === 'asc' ? cmp : -cmp;
      });
    }

    return result;
  }, [policies, filter, sortBy, sortDirection]);

  // Handle sort toggle
  const handleSort = useCallback((column: string) => {
    if (sortBy === column) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
  }, [sortBy]);

  // Table columns
  const columns: TableColumn<CleanupPolicy>[] = useMemo(() => [
    {
      id: 'name',
      header: 'Name',
      accessor: 'name',
      sortable: true,
    },
    {
      id: 'format',
      header: 'Format',
      accessor: 'format',
      sortable: true,
    },
    {
      id: 'notes',
      header: 'Description',
      accessor: (policy) => policy.notes || '—',
      sortable: true,
    },
  ], []);

  // Empty state component
  const emptyState = useMemo(() => {
    if (filter) {
      return (
        <EmptyState
          icon={Trash2}
          title="No Matching Policies"
          description={`No cleanup policies match "${filter}"`}
          size="small"
        />
      );
    }
    return (
      <EmptyState
        icon={Trash2}
        title="No Cleanup Policies"
        description="Create your first cleanup policy to automatically remove unused components."
        action={{
          label: 'Create Cleanup Policy',
          onClick: onCreate,
        }}
        secondaryAction={{
          label: 'Learn more about cleanup policies',
          href: 'http://links.sonatype.com/products/nxrm3/docs/cleanup-policy',
        }}
      />
    );
  }, [filter, onCreate]);

  return (
    <Box className="cleanup-policies-list">
      {/* Help Section */}
      <HelpSection
        title="What is a cleanup policy?"
        content="Cleanup policies can be used to remove content from your repositories. These policies will execute at the configured frequency. Once created, a cleanup policy must be assigned to a repository from the repository configuration screen."
        docLink={{
          label: 'Learn more',
          href: 'http://links.sonatype.com/products/nxrm3/docs/cleanup-policy',
        }}
      />

      {/* Search/Filter */}
      <Box className="cleanup-policies-list__toolbar">
        <TextField.Root
          placeholder="Filter by name, format, or description..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="cleanup-policies-list__search"
        >
          <TextField.Slot>
            <Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </Box>

      {/* Table */}
      <EntityTable<CleanupPolicy>
        data={filteredPolicies}
        columns={columns}
        getRowKey={(policy) => policy.name}
        getRowTestId={(policy) => `policy-row-${policy.name}`}
        onRowClick={(policy) => onSelect(policy.name)}
        loading={isLoading}
        loadingMessage="Loading cleanup policies..."
        error={loadError || undefined}
        onRetry={loadPolicies}
        emptyState={emptyState}
        sortBy={sortBy}
        sortDirection={sortDirection}
        onSort={handleSort}
        ariaLabel="Cleanup policies table"
        className="cleanup-policies-list__table"
      />
    </Box>
  );
}

export default CleanupPoliciesList;
