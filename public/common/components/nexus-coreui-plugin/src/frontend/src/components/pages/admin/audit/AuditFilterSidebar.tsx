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

import React, { useMemo } from 'react';
import { Box, Flex, Text, Button, Select, Checkbox, TextField } from '@radix-ui/themes';
import { RefreshCw } from 'lucide-react';

import type { AuditFilters, AuditCategory } from '@sonatype/nexus-ui-plugin';
import {
  CATEGORY_LABELS,
  COMMON_EVENT_TYPES,
  COMMON_DOMAINS,
} from '@sonatype/nexus-ui-plugin';
import { FilterSidebar, type FilterSection } from '../../../shared/FilterSidebar';
import type { Repository } from '../../super/settings/repository/repositories/types';
import './AuditFilterSidebar.scss';

const CATEGORY_OPTIONS: Array<{ value: AuditCategory; label: string }> = [
  { value: 'security', label: CATEGORY_LABELS.security },
  { value: 'repository', label: CATEGORY_LABELS.repository },
  { value: 'configuration', label: CATEGORY_LABELS.configuration },
  { value: 'protection', label: CATEGORY_LABELS.protection },
];

const DATE_RANGE_OPTIONS = [
  { value: 'last-24-hours', label: 'Last 24 hours' },
  { value: 'last-7-days', label: 'Last 7 days' },
  { value: 'last-30-days', label: 'Last 30 days' },
  { value: 'last-90-days', label: 'Last 90 days' },
];

const EVENT_TYPE_OPTIONS = [
  ...COMMON_EVENT_TYPES.map((type) => ({
    value: type,
    label: type.charAt(0).toUpperCase() + type.slice(1),
  })),
  { value: 'automatic-malware-removed', label: 'Malware Cleaned' },
];

export interface AuditFilterSidebarProps {
  filters: AuditFilters;
  repositories: Repository[];
  onCategoryToggle: (category: AuditCategory) => void;
  onEventTypeToggle: (eventType: string) => void;
  onInitiatorChange: (initiator: string) => void;
  onRepositoryNameChange: (repositoryName: string) => void;
  onRepositoryTypeChange: (repositoryType: string) => void;
  onDateRangeChange: (range: AuditFilters['dateRange']) => void;
  onClearAllFilters: () => void;
  disabled?: boolean;
}

export function AuditFilterSidebar({
  filters,
  repositories,
  onCategoryToggle,
  onEventTypeToggle,
  onInitiatorChange,
  onRepositoryNameChange,
  onRepositoryTypeChange,
  onDateRangeChange,
  onClearAllFilters,
  disabled = false,
}: AuditFilterSidebarProps): JSX.Element {
  const formatOptions = useMemo(() => {
    const formats = new Set<string>();
    repositories.forEach((repo) => {
      if (repo.format) formats.add(repo.format);
    });
    return Array.from(formats)
      .sort()
      .map((f) => ({ value: f, label: f.toUpperCase() }));
  }, [repositories]);

  const repoOptions = useMemo(() => {
    return repositories
      .slice()
      .sort((a, b) => a.name.localeCompare(b.name))
      .map((repo) => ({ value: repo.name, label: repo.name }));
  }, [repositories]);

  return (
    <Box p="1" pt="0">
      <Button
        variant="outline"
        color="gray"
        size="2"
        mb="4"
        onClick={onClearAllFilters}
        disabled={disabled}
      >
        <RefreshCw size={12} />
        Reset filters
      </Button>

      <Flex direction="column" gap="4">
        {/* Date Range */}
        <Box>
          <Text as="p" size="2" weight="bold" mb="2">
            Date Range
          </Text>
          <Select.Root
            size="2"
            value={filters.dateRange || 'last-30-days'}
            onValueChange={(value) => onDateRangeChange(value as AuditFilters['dateRange'])}
            disabled={disabled}
          >
            <Select.Trigger className="audit-filter-sidebar__select-trigger" />
            <Select.Content>
              {DATE_RANGE_OPTIONS.map((opt) => (
                <Select.Item key={opt.value} value={opt.value}>
                  {opt.label}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Box>

        {/* Categories */}
        <Box>
          <Text as="p" size="2" weight="bold" mb="2">
            Categories
          </Text>
          <Flex direction="column">
            {CATEGORY_OPTIONS.map(({ value, label }) => (
              <Flex key={value} align="center" gap="2" mb="1">
                <Checkbox
                  id={`category-${value}`}
                  checked={filters.categories.includes(value)}
                  onCheckedChange={() => onCategoryToggle(value)}
                  disabled={disabled}
                  size="2"
                />
                <Text as="label" size="2" htmlFor={`category-${value}`}>
                  {label}
                </Text>
              </Flex>
            ))}
          </Flex>
        </Box>

        {/* Format */}
        <Box>
          <Text as="p" size="2" weight="bold" mb="2">
            Format
          </Text>
          <Select.Root
            size="2"
            value={filters.repositoryType || '__all__'}
            onValueChange={(value) => onRepositoryTypeChange(value === '__all__' ? '' : value)}
            disabled={disabled}
          >
            <Select.Trigger className="audit-filter-sidebar__select-trigger" />
            <Select.Content>
              <Select.Item value="__all__">All Formats</Select.Item>
              {formatOptions.map((opt) => (
                <Select.Item key={opt.value} value={opt.value}>
                  {opt.label}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Box>

        {/* Repository */}
        <Box>
          <Text as="p" size="2" weight="bold" mb="2">
            Repository
          </Text>
          <Select.Root
            size="2"
            value={filters.repositoryName || '__all__'}
            onValueChange={(value) => onRepositoryNameChange(value === '__all__' ? '' : value)}
            disabled={disabled}
          >
            <Select.Trigger className="audit-filter-sidebar__select-trigger" />
            <Select.Content>
              <Select.Item value="__all__">All Repositories</Select.Item>
              {repoOptions.map((opt) => (
                <Select.Item key={opt.value} value={opt.value}>
                  {opt.label}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Box>

        {/* Event Types */}
        <Box>
          <Text as="p" size="2" weight="bold" mb="2">
            Event Types
          </Text>
          <Flex direction="column">
            {EVENT_TYPE_OPTIONS.map(({ value, label }) => (
              <Flex key={value} align="center" gap="2" mb="1">
                <Checkbox
                  id={`eventtype-${value}`}
                  checked={filters.eventTypes.includes(value)}
                  onCheckedChange={() => onEventTypeToggle(value)}
                  disabled={disabled}
                  size="2"
                />
                <Text as="label" size="2" htmlFor={`eventtype-${value}`}>
                  {label}
                </Text>
              </Flex>
            ))}
          </Flex>
        </Box>

        {/* Initiator */}
        <Box>
          <Text as="p" size="2" weight="bold" mb="2">
            Initiator
          </Text>
          <TextField.Root
            placeholder="Filter by initiator..."
            value={filters.initiator || ''}
            onChange={(e) => onInitiatorChange(e.target.value)}
            disabled={disabled}
            size="2"
          />
        </Box>
      </Flex>
    </Box>
  );
}
