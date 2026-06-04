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
import { Box, Flex, Text, Button } from '@radix-ui/themes';
import { Plus, Search, X } from 'lucide-react';
import type { CustomFilter } from './custom.types';
import { CustomFilterRow } from './CustomFilterRow';

export interface CustomSearchBuilderProps {
  filters: readonly CustomFilter[];
  onUpdateFilter: (id: string, updates: Partial<CustomFilter>) => void;
  onRemoveFilter: (id: string) => void;
  onAddFilter: () => void;
  onSearch: () => void;
  onClear: () => void;
  loading?: boolean;
  hasFilters?: boolean;
}

export function CustomSearchBuilder({
  filters,
  onUpdateFilter,
  onRemoveFilter,
  onAddFilter,
  onSearch,
  onClear,
  loading = false,
  hasFilters = false,
}: CustomSearchBuilderProps): JSX.Element {
  const handleKeyDown = (e: React.KeyboardEvent): void => {
    if (e.key === 'Enter' && !loading) {
      onSearch();
    }
  };

  return (
    <Box p="4" style={{ backgroundColor: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }} onKeyDown={handleKeyDown}>
      <Flex direction="column" gap="4">
        <Box>
          <Text size="3" weight="medium" mb="1">Search Criteria</Text>
          <Text size="2" color="gray">
            Build your search query by adding filter criteria. All filters are combined with AND logic.
          </Text>
        </Box>

        <Flex direction="column" gap="2">
          {filters.map((filter) => (
            <CustomFilterRow
              key={filter.id}
              filter={filter}
              onUpdate={onUpdateFilter}
              onRemove={onRemoveFilter}
              isOnlyFilter={filters.length === 1}
              disabled={loading}
            />
          ))}
        </Flex>

        <Flex justify="start">
          <Button variant="ghost" onClick={onAddFilter} disabled={loading}>
            <Plus size={16} />
            Add Filter
          </Button>
        </Flex>

        <Flex justify="end" gap="2">
          {hasFilters && (
            <Button variant="ghost" onClick={onClear} disabled={loading}>
              <X size={14} />
              Clear All
            </Button>
          )}
          <Button onClick={onSearch} disabled={loading}>
            <Search size={16} />
            {loading ? 'Searching...' : 'Search'}
          </Button>
        </Flex>
      </Flex>
    </Box>
  );
}

export default CustomSearchBuilder;
