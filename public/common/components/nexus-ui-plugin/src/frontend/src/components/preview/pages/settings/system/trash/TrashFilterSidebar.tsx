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

import React, { useState, useCallback } from 'react';
import { Badge, Box, Button, Checkbox, Flex, Text } from '@radix-ui/themes';
import { ChevronDown, ChevronLeft, ChevronUp, RefreshCw } from 'lucide-react';

import {
  DeletedItemsFilterState,
  DELETED_ITEM_TYPE_LABELS,
  FacetsResponse,
} from './types';

interface FilterOption {
  value: string;
  label: string;
  count: number;
}

export interface TrashFilterSidebarProps {
  facets: FacetsResponse | null;
  facetsLoading: boolean;
  filterState: DeletedItemsFilterState;
  onFilterChange: (sectionId: keyof DeletedItemsFilterState, values: string[]) => void;
  onReset: () => void;
  disabled?: boolean;
}

const VISIBLE_OPTIONS_COUNT = 7;

function FilterSection({
  label,
  options,
  selected,
  onChange,
  disabled,
  loading,
}: {
  label: string;
  options: FilterOption[];
  selected: string[];
  onChange: (values: string[]) => void;
  disabled?: boolean;
  loading?: boolean;
}): JSX.Element {
  const [showAll, setShowAll] = useState(false);
  const visibleOptions = showAll ? options : options.slice(0, VISIBLE_OPTIONS_COUNT);
  const hasMore = options.length > VISIBLE_OPTIONS_COUNT;

  const toggle = useCallback(
    (value: string) => {
      const next = selected.includes(value)
        ? selected.filter((v) => v !== value)
        : [...selected, value];
      onChange(next);
    },
    [selected, onChange]
  );

  const clear = useCallback(() => onChange([]), [onChange]);

  // Empty sections should still render their heading while loading
  if (!loading && options.length === 0) return <></>;

  return (
    <Box mt="1">
      <Flex align="center" justify="between" mb="3">
        <Text size="2" weight="bold">
          {label}
        </Text>
      </Flex>
      <Flex direction="column" gap="1">
        {selected.length > 0 && (
          <Button
            variant="ghost"
            color="blue"
            size="1"
            onClick={clear}
            style={{ alignSelf: 'flex-start', gap: '4px' }}
          >
            <ChevronLeft size={14} />
            Clear
          </Button>
        )}
        {visibleOptions.map((opt) => (
          <Flex
            key={opt.value}
            align="center"
            gap="2"
            asChild
          >
            <label style={{ cursor: disabled ? 'default' : 'pointer' }}>
              <Checkbox
                checked={selected.includes(opt.value)}
                onCheckedChange={() => toggle(opt.value)}
                disabled={disabled}
              />
              <Text size="2">{opt.label}</Text>
              <Badge size="1" color="gray" variant="soft" radius="full">
                {opt.count}
              </Badge>
            </label>
          </Flex>
        ))}
        {hasMore && (
          <Button
            variant="ghost"
            color="blue"
            size="1"
            onClick={() => setShowAll(!showAll)}
            style={{ alignSelf: 'flex-start' }}
          >
            {showAll ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
            <Text size="2">{showAll ? 'See less' : 'See more'}</Text>
          </Button>
        )}
      </Flex>
    </Box>
  );
}

export function TrashFilterSidebar({
  facets,
  facetsLoading,
  filterState,
  onFilterChange,
  onReset,
  disabled = false,
}: TrashFilterSidebarProps): JSX.Element {
  const entityTypeOptions: FilterOption[] =
    facets
      ? Object.entries(facets.entityTypes).map(([value, count]) => ({
          value,
          label: DELETED_ITEM_TYPE_LABELS[value as keyof typeof DELETED_ITEM_TYPE_LABELS] ?? value,
          count,
        }))
      : [];

  const formatOptions: FilterOption[] =
    facets
      ? Object.entries(facets.formats).map(([value, count]) => ({
          value,
          label: value,
          count,
        }))
      : [];

  const deletedByOptions: FilterOption[] =
    facets
      ? facets.deletedBy.map(({ name, count, displayName }) => ({
          // Value stays the raw principal so filtering matches the stored deleted_by;
          // label prefers the resolved display name when available.
          value: name,
          label: displayName || name,
          count,
        }))
      : [];

  // Nothing to reset when every filter section is empty; disable the button so it isn't a no-op affordance.
  const hasActiveFilters = Object.values(filterState).some(
    (values) => Array.isArray(values) && values.length > 0
  );

  return (
    <Box p="1" pt="4" data-testid="filter-sidebar">
      <Flex align="center" justify="start" mb="4">
        <Button
          variant="outline"
          color="gray"
          size="2"
          onClick={onReset}
          disabled={disabled || !hasActiveFilters}
        >
          <RefreshCw size={12} />
          Reset filters
        </Button>
      </Flex>

      <Flex direction="column" gap="4">
        <FilterSection
          label="Entity Type"
          options={entityTypeOptions}
          selected={filterState.entityTypes}
          onChange={(v) => onFilterChange('entityTypes', v)}
          disabled={disabled}
          loading={facetsLoading}
        />
        <FilterSection
          label="Format"
          options={formatOptions}
          selected={filterState.formats}
          onChange={(v) => onFilterChange('formats', v)}
          disabled={disabled}
          loading={facetsLoading}
        />
        <FilterSection
          label="Deleted By"
          options={deletedByOptions}
          selected={filterState.deletedBy}
          onChange={(v) => onFilterChange('deletedBy', v)}
          disabled={disabled}
          loading={facetsLoading}
        />
      </Flex>
    </Box>
  );
}
