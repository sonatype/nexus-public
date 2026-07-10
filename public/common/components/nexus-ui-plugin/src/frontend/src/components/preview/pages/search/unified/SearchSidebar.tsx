/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Flex, Text, TextField, Button } from '@radix-ui/themes';
import { RefreshCw } from 'lucide-react';

import { FormatSearchDropdown } from './FormatSearchDropdown';
import { RepositorySearchDropdown } from './RepositorySearchDropdown';
import type { SearchFormat } from './unified.types';
import { getFiltersForFormat } from './searchFilters';

import './SearchSidebar.scss';

export interface SearchSidebarProps {
  /** Currently selected format (single) */
  selectedFormat: SearchFormat | '';
  /** Callback when format selection changes */
  onFormatChange: (format: SearchFormat | '') => void;
  /** Callback to trigger search */
  onSearch?: () => void;
  /** Set of formats that have at least one accessible repository */
  availableFormats?: Set<string>;
  /** Whether filters are disabled */
  disabled?: boolean;
  /** Current filter values */
  filters: Record<string, string>;
  /** Callback when a filter changes */
  onFilterChange: (filterId: string, value: string) => void;
  /** Callback to reset all filters */
  onReset: () => void;
  /** Available repositories for dropdown */
  repositories?: readonly string[];
  /** When true, sidebar is in mobile drawer */
  inDrawer?: boolean;
}

const DEBOUNCE_MS = 300;

export const SearchSidebar = React.memo(function SearchSidebar({
  selectedFormat,
  onFormatChange,
  onSearch,
  availableFormats,
  disabled = false,
  filters,
  onFilterChange,
  onReset,
  repositories = [],
  inDrawer = false,
}: SearchSidebarProps): JSX.Element {
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Track the focused input element so focus can be restored after disabled toggles
  const focusedInputRef = useRef<HTMLElement | null>(null);
  const prevDisabledRef = useRef(disabled);

  // Local text values — decoupled from parent state so typing never depends on a
  // round-trip through the search machine. Cleared only when onReset fires.
  const [localValues, setLocalValues] = useState<Record<string, string>>({});

  // Restore focus after disabled transitions false→true→false (search in-flight)
  useEffect(() => {
    const wasDisabled = prevDisabledRef.current;
    prevDisabledRef.current = disabled;
    if (wasDisabled && !disabled && focusedInputRef.current) {
      focusedInputRef.current.focus();
    }
  }, [disabled]);

  const handleFormatChange = useCallback(
    (format: SearchFormat | '') => {
      onFormatChange(format);
      if (onSearch) setTimeout(onSearch, 200);
    },
    [onFormatChange, onSearch],
  );

  const handleRepositoryChange = useCallback(
    (value: string) => {
      onFilterChange('repository', value);
      if (onSearch) setTimeout(onSearch, 200);
    },
    [onFilterChange, onSearch],
  );

  const handleReset = useCallback(() => {
    setLocalValues({});
    onFormatChange('');
    onFilterChange('repository', '');
    onReset();
    if (onSearch) setTimeout(onSearch, 0);
  }, [onFormatChange, onFilterChange, onReset, onSearch]);

  const handleTextFilterChange = useCallback(
    (filterId: string, value: string) => {
      setLocalValues((prev) => ({ ...prev, [filterId]: value }));
      onFilterChange(filterId, value);
      if (onSearch) {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
          debounceRef.current = null;
          onSearch();
        }, DEBOUNCE_MS);
      }
    },
    [onFilterChange, onSearch],
  );

  const handleSelectFilterChange = useCallback(
    (filterId: string, value: string) => {
      onFilterChange(filterId, value);
      if (onSearch) setTimeout(onSearch, 200);
    },
    [onFilterChange, onSearch],
  );

  const repositoryValue = filters['repository'] || '';

  // Format-specific filters (exclude repository and nameOrVersion — both rendered elsewhere)
  const formatSpecificFilters = useMemo(() => {
    if (!selectedFormat || selectedFormat === 'all') return [];
    return getFiltersForFormat(selectedFormat).filter(
      (f) => f.id !== 'repository' && f.id !== 'nameOrVersion',
    );
  }, [selectedFormat]);

  return (
    <aside
      className={`search-sidebar ${inDrawer ? 'search-sidebar--drawer' : ''}`}
    >
      <Box p="1" pt="1">
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            onClick={handleReset}
            disabled={disabled}
          >
            <RefreshCw size={12} />
            Reset filters
          </Button>
        </Flex>

        <Flex direction="column" gap="4">
          {/* Format - dropdown */}
          <Box mt="1">
            <Flex align="center" justify="between" mb="3">
              <Text size="2" weight="bold">
                Format
              </Text>
            </Flex>
            <FormatSearchDropdown
              value={selectedFormat}
              onChange={handleFormatChange}
              availableFormats={availableFormats}
              disabled={disabled}
              placeholder="All formats"
            />
          </Box>

          {/* Repository - searchable dropdown */}
          <Box mt="1">
            <Flex align="center" justify="between" mb="3">
              <Text size="2" weight="bold">
                Repository
              </Text>
            </Flex>
            <RepositorySearchDropdown
              value={repositoryValue}
              onChange={handleRepositoryChange}
              repositories={repositories}
              disabled={disabled}
              placeholder="All repositories"
            />
          </Box>

          {/* Format-specific filters (when format is selected) */}
          {formatSpecificFilters.length > 0 && (
            <Box mt="1">
              <Flex align="center" justify="between" mb="3">
                <Text size="2" weight="bold">
                  Filters
                </Text>
              </Flex>
              <Flex direction="column" gap="3">
                {formatSpecificFilters.map((filter) =>
                  filter.type === 'text' ? (
                    <Box key={filter.id}>
                      <Text as="label" size="2" htmlFor={`filter-${filter.id}`}>
                        {filter.label}
                      </Text>
                      <TextField.Root
                        id={`filter-${filter.id}`}
                        placeholder={filter.placeholder}
                        value={localValues[filter.id] ?? filters[filter.id] ?? ''}
                        onChange={(e) => handleTextFilterChange(filter.id, e.target.value)}
                        onFocus={(e) => { focusedInputRef.current = e.currentTarget; }}
                        onBlur={() => { if (!disabled) focusedInputRef.current = null; }}
                        disabled={disabled}
                        size="2"
                        mt="1"
                      />
                    </Box>
                  ) : filter.type === 'select' && filter.options ? (
                    <Box key={filter.id}>
                      <Text as="label" size="2" htmlFor={`filter-${filter.id}`}>
                        {filter.label}
                      </Text>
                      <select
                        id={`filter-${filter.id}`}
                        value={filters[filter.id] || ''}
                        onChange={(e) => handleSelectFilterChange(filter.id, e.target.value)}
                        disabled={disabled}
                        style={{
                          width: '100%',
                          padding: '8px 12px',
                          marginTop: '4px',
                          fontSize: '14px',
                          border: '1px solid var(--gray-6)',
                          borderRadius: '6px',
                          backgroundColor: 'var(--color-surface)',
                        }}
                      >
                        <option value="">{filter.placeholder || 'All'}</option>
                        {filter.options.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </select>
                    </Box>
                  ) : null,
                )}
              </Flex>
            </Box>
          )}
        </Flex>
      </Box>
    </aside>
  );
});

export default SearchSidebar;
