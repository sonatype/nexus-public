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
  Text,
  Button,
  Checkbox,
  Select,
} from '@radix-ui/themes';
import { ChevronDown, ChevronUp, ChevronLeft, RefreshCw } from 'lucide-react';

interface FilterOption {
  value: string;
  label: string;
  count: number;
}

const VISIBLE_OPTIONS_COUNT = 7;

const SORT_FIELD_OPTIONS = [
  { value: 'name', label: 'Name' },
  { value: 'format', label: 'Format' },
  { value: 'type', label: 'Type' },
  { value: 'status', label: 'Status' },
];

const SORT_DIRECTION_OPTIONS = [
  { value: 'asc', label: 'Ascending' },
  { value: 'desc', label: 'Descending' },
];

export interface BrowseFilterSidebarProps {
  /** Format options */
  formatOptions: FilterOption[];
  /** Type options */
  typeOptions: FilterOption[];
  /** Status options */
  statusOptions: FilterOption[];
  /** Protection options (optional) */
  protectionOptions?: FilterOption[];
  /** Health check options (optional) */
  healthCheckOptions?: FilterOption[];
  /** Selected format values */
  selectedFormats: string[];
  /** Selected type values */
  selectedTypes: string[];
  /** Selected status values */
  selectedStatuses: string[];
  /** Selected protection values */
  selectedProtection: string[];
  /** Selected health check values */
  selectedHealthCheck: string[];
  /** Sort field */
  sortField: string;
  /** Sort direction */
  sortDirection: 'asc' | 'desc';
  /** Callback when format filter changes */
  onFormatsChange: (values: string[]) => void;
  /** Callback when type filter changes */
  onTypesChange: (values: string[]) => void;
  /** Callback when status filter changes */
  onStatusesChange: (values: string[]) => void;
  /** Callback when protection filter changes */
  onProtectionChange?: (values: string[]) => void;
  /** Callback when health check filter changes */
  onHealthCheckChange?: (values: string[]) => void;
  /** Callback when sort changes */
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  /** Callback to reset all filters */
  onResetFilters: () => void;
  /** Whether filters are disabled */
  disabled?: boolean;
  /** Hide Clear button */
  hideClearButton?: boolean;
}

/**
 * Filter section following the seaworthy-ux-lab pattern:
 * - Bold section title
 * - Clear link (ChevronLeft + "Clear" in blue) when items selected
 * - Checkbox + label + Badge count per option
 * - See more/See less when > 7 items
 */
function FilterSection({
  label,
  options,
  selected,
  onChange,
  disabled,
  analyticsId,
}: {
  label: string;
  options: FilterOption[];
  selected: string[];
  onChange: (values: string[]) => void;
  disabled?: boolean;
  analyticsId?: string;
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
        <Text size="2" weight="bold">
          {label}
        </Text>
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
            <Text size="2" color="blue" style={{ fontWeight: '500' }}>
              Clear
            </Text>
          </Flex>
        )}
        {visibleOptions.map((opt) => (
          <Flex key={opt.value} align="center" gap="2">
            <Checkbox
              checked={selected.includes(opt.value)}
              onCheckedChange={() => toggle(opt.value)}
              disabled={disabled}
              aria-label={opt.label}
              data-analytics-id={analyticsId ? `${analyticsId}-${opt.value}` : undefined}
            />
            <Text size="2">{opt.label}</Text>
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
            <Text size="2">{showAll ? 'See less' : 'See more'}</Text>
          </Button>
        )}
      </Flex>
    </Box>
  );
}

/**
 * BrowseFilterSidebar following the seaworthy-ux-lab filter pattern.
 * Uses Nexus endpoints: /service/rest/internal/ui/repositories/details/filtered
 * for filter options (computed from allReposForFilters in BrowsePage).
 */
export function BrowseFilterSidebar({
  formatOptions,
  typeOptions,
  statusOptions,
  protectionOptions = [],
  healthCheckOptions = [],
  selectedFormats,
  selectedTypes,
  selectedStatuses,
  selectedProtection,
  selectedHealthCheck,
  sortField,
  sortDirection,
  onFormatsChange,
  onTypesChange,
  onStatusesChange,
  onProtectionChange,
  onHealthCheckChange,
  onSortChange,
  onResetFilters,
  disabled = false,
  hideClearButton = false,
}: BrowseFilterSidebarProps): JSX.Element {
  // Sort field - when changed, keep current direction
  const handleSortFieldChange = useCallback(
    (v: string) => onSortChange(v, sortDirection),
    [onSortChange, sortDirection]
  );

  // Sort direction - when changed, keep current field
  const handleSortDirectionChange = useCallback(
    (v: string) => onSortChange(sortField, v as 'asc' | 'desc'),
    [onSortChange, sortField]
  );

  return (
    <Box p="1" pt="4">
      {!hideClearButton && (
        <Flex align="center" justify="start" mb="4">
          <Button
            variant="outline"
            color="gray"
            size="2"
            onClick={onResetFilters}
            data-analytics-id="nxrm-repository-filter-reset"
          >
            <RefreshCw size={12} />
            Reset filters
          </Button>
        </Flex>
      )}

      <Flex direction="column" gap="4">
        {/* Sort */}
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
              value={sortDirection}
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

        {/* Format */}
        <FilterSection
          label="Format"
          options={formatOptions}
          selected={selectedFormats}
          onChange={onFormatsChange}
          disabled={disabled}
          analyticsId="nxrm-repository-filter-format"
        />

        {/* Type */}
        <FilterSection
          label="Type"
          options={typeOptions}
          selected={selectedTypes}
          onChange={onTypesChange}
          disabled={disabled}
          analyticsId="nxrm-repository-filter-type"
        />

        {/* Status */}
        <FilterSection
          label="Status"
          options={statusOptions}
          selected={selectedStatuses}
          onChange={onStatusesChange}
          disabled={disabled}
          analyticsId="nxrm-repository-filter-status"
        />

        {/* Protection (optional) */}
        {protectionOptions.length > 0 && onProtectionChange && (
          <FilterSection
            label="Protection"
            options={protectionOptions}
            selected={selectedProtection}
            onChange={onProtectionChange}
            disabled={disabled}
            analyticsId="nxrm-repository-filter-protection"
          />
        )}

        {/* Health Check (optional) */}
        {healthCheckOptions.length > 0 && onHealthCheckChange && (
          <FilterSection
            label="Health Check"
            options={healthCheckOptions}
            selected={selectedHealthCheck}
            onChange={onHealthCheckChange}
            disabled={disabled}
            analyticsId="nxrm-repository-filter-healthcheck"
          />
        )}
      </Flex>
    </Box>
  );
}
