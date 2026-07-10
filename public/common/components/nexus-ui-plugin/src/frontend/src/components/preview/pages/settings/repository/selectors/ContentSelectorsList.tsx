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
import { Box, Text, TextField } from '@radix-ui/themes';
import { Search, Layers, Plus } from 'lucide-react';

import {
  EntityTable,
  EmptyState,
  HelpSection,
  LoadingState,
  ErrorState,
  type TableColumn,
} from '../../../../shared';
import { useContentSelectorsApi } from './useContentSelectorsApi';
import { ContentSelector } from './types';

import './ContentSelectorsList.scss';

interface ContentSelectorsListProps {
  onSelect: (name: string) => void;
  onCreate: () => void;
}

/**
 * ContentSelectorsList - Sortable, filterable table of content selectors
 * Uses shared EntityTable component for consistent UX
 */
export function ContentSelectorsList({ onSelect, onCreate }: ContentSelectorsListProps) {
  const [selectors, setSelectors] = useState<ContentSelector[]>([]);
  const [filter, setFilter] = useState('');
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const { fetchContentSelectors } = useContentSelectorsApi();

  // Load selectors on mount
  useEffect(() => {
    setIsLoading(true);
    setLoadError(null);
    fetchContentSelectors()
      .then(setSelectors)
      .catch((err) => setLoadError(err.message))
      .finally(() => setIsLoading(false));
  }, [fetchContentSelectors]);

  // Define table columns
  const columns: TableColumn<ContentSelector>[] = useMemo(() => [
    {
      id: 'name',
      header: 'Name',
      accessor: (item) => <Text weight="medium">{item.name}</Text>,
      sortable: true,
    },
    {
      id: 'type',
      header: 'Type',
      accessor: (item) => item.type?.toUpperCase() || '',
      sortable: true,
    },
    {
      id: 'description',
      header: 'Description',
      accessor: (item) => (
        <Text className="content-selectors-list__description">
          {item.description || '—'}
        </Text>
      ),
      sortable: true,
    },
    {
      id: 'expression',
      header: 'Expression',
      accessor: (item) => (
        <Text className="content-selectors-list__expression">
          {item.expression || '—'}
        </Text>
      ),
      sortable: true,
    },
  ], []);

  // Handle sort changes
  const handleSort = useCallback((column: string) => {
    if (sortBy === column) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
  }, [sortBy]);

  // Filter and sort selectors
  const filteredSelectors = useMemo(() => {
    let result = selectors;

    // Apply filter
    if (filter) {
      const lowerFilter = filter.toLowerCase();
      result = result.filter(
        (s) =>
          s.name.toLowerCase().includes(lowerFilter) ||
          (s.description && s.description.toLowerCase().includes(lowerFilter)) ||
          (s.expression && s.expression.toLowerCase().includes(lowerFilter))
      );
    }

    // Apply sort
    if (sortBy && sortDirection) {
      result = [...result].sort((a, b) => {
        const aVal = (a[sortBy as keyof ContentSelector] || '').toString().toLowerCase();
        const bVal = (b[sortBy as keyof ContentSelector] || '').toString().toLowerCase();
        const cmp = aVal.localeCompare(bVal);
        return sortDirection === 'asc' ? cmp : -cmp;
      });
    }

    return result;
  }, [selectors, filter, sortBy, sortDirection]);

  // Retry loading on error
  const handleRetry = useCallback(() => {
    setIsLoading(true);
    setLoadError(null);
    fetchContentSelectors()
      .then(setSelectors)
      .catch((err) => setLoadError(err.message))
      .finally(() => setIsLoading(false));
  }, [fetchContentSelectors]);

  // Show loading state
  if (isLoading) {
    return (
      <Box className="content-selectors-list">
        <LoadingState message="Loading content selectors..." />
      </Box>
    );
  }

  // Show error state
  if (loadError) {
    return (
      <Box className="content-selectors-list">
        <ErrorState
          title="Failed to Load Content Selectors"
          message={loadError}
          onRetry={handleRetry}
        />
      </Box>
    );
  }

  // Determine empty state based on filter
  const renderEmptyState = () => {
    if (filter) {
      // No results for filter
      return (
        <EmptyState
          icon={Search}
          title="No Matching Content Selectors"
          description={`No content selectors match "${filter}". Try a different search term.`}
          size="small"
        />
      );
    }

    // No selectors at all
    return (
      <EmptyState
        icon={Layers}
        title="No Content Selectors"
        description="Create your first content selector to define what content users can access."
        action={{
          label: 'Create Selector',
          onClick: onCreate,
          icon: Plus,
        }}
        secondaryAction={{
          label: 'Learn more about Content Selectors',
          href: 'http://links.sonatype.com/products/nxrm3/docs/content-selector',
        }}
      />
    );
  };

  return (
    <Box className="content-selectors-list">
      {/* Help Section */}
      <HelpSection
        title="What is a content selector?"
        content="Content selectors provide a means for you to select specific content from your repositories. Repository content is evaluated against expressions written in CSEL (Content Selector Expression Language)."
        docLink={{
          label: 'Learn more',
          href: 'http://links.sonatype.com/products/nxrm3/docs/content-selector',
        }}
        className="content-selectors-list__help"
      />

      {/* Search Filter */}
      <Box className="content-selectors-list__toolbar">
        <TextField.Root
          placeholder="Filter by name or description..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="content-selectors-list__search"
          data-analytics-id="nxrm-content-selector-filter"
        >
          <TextField.Slot>
            <Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </Box>

      {/* Entity Table */}
      <EntityTable<ContentSelector>
        data={filteredSelectors}
        columns={columns}
        getRowKey={(item) => item.name}
        getRowTestId={(item) => `selector-row-${item.name}`}
        onRowClick={(item) => onSelect(item.name)}
        sortBy={sortBy}
        sortDirection={sortDirection}
        onSort={handleSort}
        emptyState={renderEmptyState()}
        ariaLabel="Content selectors table"
        className="content-selectors-list__table"
      />
    </Box>
  );
}

export default ContentSelectorsList;
