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
import {
  Badge,
  Box,
  Flex,
  Text as RadixText,
  Button,
  Checkbox,
  Select,
} from '@radix-ui/themes';
import { ChevronDown, ChevronUp, ChevronLeft, RefreshCw } from 'lucide-react';

import { FORMAT_LABELS } from '../../../../components/super/settings/repository/repositories/types';
import type { SortColumn, SortDirection } from '../upload.types';

interface FilterOption {
  value: string;
  label: string;
  count: number;
}

const VISIBLE_OPTIONS_COUNT = 7;

const SORT_FIELD_OPTIONS: { value: SortColumn; label: string }[] = [
  { value: 'name', label: 'Name' },
  { value: 'format', label: 'Format' },
];

const SORT_DIRECTION_OPTIONS: { value: 'asc' | 'desc'; label: string }[] = [
  { value: 'asc', label: 'Ascending' },
  { value: 'desc', label: 'Descending' },
];

export interface UploadFilterSidebarProps {
  formatOptions: FilterOption[];
  selectedFormats: string[];
  sortColumn: SortColumn | null;
  sortDirection: SortDirection;
  onFormatsChange: (values: string[]) => void;
  onSortChange: (column: SortColumn | null, direction: 'asc' | 'desc' | null) => void;
  onResetFilters: () => void;
  disabled?: boolean;
}

function FilterSection({
  label,
  options,
  selected,
  onChange,
  disabled,
}: {
  label: string;
  options: FilterOption[];
  selected: string[];
  onChange: (values: string[]) => void;
  disabled?: boolean;
}): JSX.Element {
  const [showAll, setShowAll] = useState(false);
  const visibleOptions = showAll ? options : options.slice(0, VISIBLE_OPTIONS_COUNT);
  const hasMore = options.length > VISIBLE_OPTIONS_COUNT;

  const toggle = useCallback(
    (value: string) => {
      const next = selected.includes(value)
        ? selected.filter((v) => v !== value)
        : [...selected, value];
      onChange(next.length > 0 ? next : []);
    },
    [selected, onChange]
  );

  const clear = useCallback(() => onChange([]), [onChange]);

  return (
    <Box mt="1">
      <Flex align="center" justify="between" mb="3">
        <RadixText size="2" weight="bold">
          {label}
        </RadixText>
      </Flex>
      <Flex direction="column" gap="1">
        {selected.length > 0 && (
          <Flex
            align="center"
            gap="2"
            style={{ cursor: 'pointer' }}
            onClick={clear}
          >
            <ChevronLeft size={14} color="var(--blue-11)" />
            <RadixText size="2" color="blue" style={{ fontWeight: '500' }}>
              Clear
            </RadixText>
          </Flex>
        )}
        {visibleOptions.map((opt) => (
          <Flex key={opt.value} align="center" gap="2">
            <Checkbox
              checked={selected.includes(opt.value)}
              onCheckedChange={() => toggle(opt.value)}
              disabled={disabled}
            />
            <RadixText size="2">{FORMAT_LABELS[opt.value] || opt.label}</RadixText>
            <Badge size="1" color="gray" variant="soft" radius="full">
              {opt.count}
            </Badge>
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
            <RadixText size="2">{showAll ? 'See less' : 'See more'}</RadixText>
          </Button>
        )}
      </Flex>
    </Box>
  );
}

/**
 * UploadFilterSidebar - Filter bar for upload repository list.
 * Matches Browse layout with Format filter and Sort dropdowns.
 */
export function UploadFilterSidebar({
  formatOptions,
  selectedFormats,
  sortColumn,
  sortDirection,
  onFormatsChange,
  onSortChange,
  onResetFilters,
  disabled = false,
}: UploadFilterSidebarProps): JSX.Element {
  const sortField = sortColumn ?? 'name';
  const sortDir = sortDirection ?? 'asc';

  const handleSortFieldChange = useCallback(
    (v: string) => onSortChange(v as SortColumn, sortDir as 'asc' | 'desc'),
    [onSortChange, sortDir]
  );

  const handleSortDirectionChange = useCallback(
    (v: string) => onSortChange(sortColumn, v as 'asc' | 'desc'),
    [onSortChange, sortColumn]
  );

  return (
    <Box p="1" pt="4">
      <Flex align="center" justify="start" mb="4">
        <Button
          variant="outline"
          color="gray"
          size="2"
          onClick={onResetFilters}
          disabled={disabled}
        >
          <RefreshCw size={12} />
          Reset filters
        </Button>
      </Flex>

      <Flex direction="column" gap="4">
        <Box mt="1">
          <Flex mb="2" align="center" gap="2">
            <Box asChild style={{ fontWeight: 600, fontSize: 'var(--font-size-2)' }}>
              <span>Sort</span>
            </Box>
          </Flex>
          <Flex direction="column" gap="2">
            <Select.Root
              value={sortField}
              onValueChange={handleSortFieldChange}
              size="2"
              disabled={disabled}
            >
              <Select.Trigger />
              <Select.Content>
                {SORT_FIELD_OPTIONS.map((o) => (
                  <Select.Item key={o.value} value={o.value}>
                    {o.label}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
            <Select.Root
              value={sortDir}
              onValueChange={handleSortDirectionChange}
              size="2"
              disabled={disabled}
            >
              <Select.Trigger />
              <Select.Content>
                {SORT_DIRECTION_OPTIONS.map((o) => (
                  <Select.Item key={o.value} value={o.value}>
                    {o.label}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Flex>
        </Box>

        <FilterSection
          label="Format"
          options={formatOptions}
          selected={selectedFormats}
          onChange={onFormatsChange}
          disabled={disabled}
        />
      </Flex>
    </Box>
  );
}

export default UploadFilterSidebar;
