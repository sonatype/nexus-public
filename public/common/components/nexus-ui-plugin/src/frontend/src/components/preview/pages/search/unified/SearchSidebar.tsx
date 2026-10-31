/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Flex, Text, Button } from '@radix-ui/themes';
import { RefreshCw } from 'lucide-react';

import { FormatSearchDropdown } from './FormatSearchDropdown';
import { RepositorySearchDropdown } from './RepositorySearchDropdown';
import type { SearchFormat } from './unified.types';
import {
  getFiltersForFormat,
  getAliasLabelsForFormat,
  getAliasPlaceholdersForFormat,
} from './searchFilters';
import { TextFilter, SelectFilter } from './filters';

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

/**
 * Adapter that maps `readonly string[]` filter options into the
 * `{ value, label }` shape SelectFilter expects, memoized on the source
 * options array so the mapped list keeps a stable reference across parent
 * renders.  Without this, every parent render (e.g. a keystroke in another
 * text filter) would hand SelectFilter a fresh `options` prop and defeat any
 * downstream memoization.
 */
const SelectFilterAdapter = React.memo(function SelectFilterAdapter(props: {
  id: string;
  label: string;
  options?: readonly string[];
  value: string;
  onChange: (id: string, value: string) => void;
  onSearch?: () => void;
  disabled?: boolean;
}): JSX.Element {
  const { options, ...rest } = props;
  const mappedOptions = useMemo(
    () => (options ?? []).map((opt) => ({ value: opt, label: opt })),
    [options],
  );
  return <SelectFilter {...rest} options={mappedOptions} />;
});

/**
 * Debounce delay for sidebar filter changes (ms).
 *
 * 300 ms rather than `APIConstants.DEBOUNCE_DELAY`: these filters narrow an already-rendered
 * result set, so the list is expected to react closely to typing.
 *
 * This debounces a *callback* (`onSearch`), not a value: Reset fires it immediately, and format,
 * repository and select changes schedule it outside the text-filter window. Neither is expressible
 * with the shared `useDebounced` hook, so this keeps its own timer ref. See
 * `private/developer-documentation/frontend/debouncing.md`.
 */
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
  // Track the focused input element so focus can be restored after disabled toggles
  const focusedInputRef = useRef<HTMLElement | null>(null);
  // Track which filter id is currently focused, so external filter changes
  // (e.g. browser back/forward) don't yank text out from under an active edit.
  const focusedFilterIdRef = useRef<string | null>(null);
  const prevDisabledRef = useRef(disabled);

  // Local text values — decoupled from parent state so typing never depends on a
  // round-trip through the search machine. Cleared only when onReset fires.
  const [localValues, setLocalValues] = useState<Record<string, string>>({});

  // Reconcile local text values when the filters prop changes from an external
  // source (browser back/forward, deep-link rehydration, format reset). Without
  // this, localValues (which takes precedence in the input) would keep showing
  // stale values even though results and the URL have changed.
  //
  // We only drop a local override when the *prop value for that key actually
  // transitioned* (prev !== next). This distinguishes a genuine external change
  // (e.g. back button: artifactId 'a' -> 'b') from the common case where the
  // parent re-renders with a still-unchanged filters value while the user is
  // mid-type (prop stays '' because the machine hasn't been told yet). The
  // currently-focused filter is always skipped so typing is never disrupted.
  const prevFiltersRef = useRef(filters);
  useEffect(() => {
    const prevFilters = prevFiltersRef.current;
    prevFiltersRef.current = filters;

    setLocalValues((prev) => {
      let changed = false;
      const next = { ...prev };
      for (const id of Object.keys(prev)) {
        if (id === focusedFilterIdRef.current) {
          continue;
        }
        const prevPropValue = prevFilters[id] ?? '';
        const nextPropValue = filters[id] ?? '';
        // Only reconcile when the authoritative prop value itself changed AND
        // it no longer matches the local override.
        if (prevPropValue !== nextPropValue && nextPropValue !== prev[id]) {
          delete next[id];
          changed = true;
        }
      }
      return changed ? next : prev;
    });
  }, [filters]);

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
      // Debounce and onSearch are delegated to TextFilter via the debounceMs prop
    },
    [onFilterChange],
  );

  // SelectFilter debounces its own onSearch call; the sidebar only needs to
  // relay the value change, so onFilterChange is passed to it directly.

  const handleInputFocus = useCallback((id: string, el: HTMLInputElement) => {
    focusedInputRef.current = el;
    focusedFilterIdRef.current = id;
  }, []);

  const handleInputBlur = useCallback(
    (_id: string) => {
      if (!disabled) focusedInputRef.current = null;
      focusedFilterIdRef.current = null;
    },
    [disabled],
  );

  const repositoryValue = filters.repository || '';

  // Filters to render + alias vocabulary (label + placeholder) for the current
  // format.  Bundled into a single memo so React only compares one dep pair.
  // Repository and nameOrVersion are rendered elsewhere (header / results
  // header), so they are excluded from the sidebar list.
  const { filtersToRender, aliasLabels, aliasPlaceholders } = useMemo(() => {
    const format = (selectedFormat || 'all') as SearchFormat;
    return {
      filtersToRender: getFiltersForFormat(format).filter(
        (f) => f.id !== 'repository' && f.id !== 'nameOrVersion',
      ),
      aliasLabels: getAliasLabelsForFormat(format) ?? {},
      aliasPlaceholders: getAliasPlaceholdersForFormat(format) ?? {},
    };
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

          {/* Filters — generic group/name/version always shown; aliases applied per format */}
          {filtersToRender.length > 0 && (
            <Box mt="1">
              <Flex align="center" justify="between" mb="3">
                <Text size="2" weight="bold">
                  Filters
                </Text>
              </Flex>
              <Flex direction="column" gap="3">
                {filtersToRender.map((filter) => {
                  // Relabel generic group/name/version when the current format declares an alias.
                  const isGeneric =
                    filter.id === 'group' || filter.id === 'name' || filter.id === 'version';
                  const aliasKey = isGeneric
                    ? (filter.id as 'group' | 'name' | 'version')
                    : undefined;
                  const label = aliasKey ? aliasLabels[aliasKey] ?? filter.label : filter.label;
                  const placeholder = aliasKey
                    ? aliasPlaceholders[aliasKey] ?? filter.placeholder
                    : filter.placeholder;

                  if (filter.type === 'text') {
                    return (
                      <TextFilter
                        key={filter.id}
                        id={filter.id}
                        label={label}
                        placeholder={placeholder}
                        value={localValues[filter.id] ?? filters[filter.id] ?? ''}
                        onChange={handleTextFilterChange}
                        onSearch={onSearch}
                        debounceMs={DEBOUNCE_MS}
                        disabled={disabled}
                        onInputFocus={handleInputFocus}
                        onInputBlur={handleInputBlur}
                      />
                    );
                  }
                  if (filter.type === 'select') {
                    return (
                      <SelectFilterAdapter
                        key={filter.id}
                        id={filter.id}
                        label={label}
                        options={filter.options}
                        value={filters[filter.id] ?? ''}
                        onChange={onFilterChange}
                        onSearch={onSearch}
                        disabled={disabled}
                      />
                    );
                  }
                  return null;
                })}
              </Flex>
            </Box>
          )}
        </Flex>
      </Box>
    </aside>
  );
});

export default SearchSidebar;
