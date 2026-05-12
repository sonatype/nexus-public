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
import { Box, Button, Checkbox, Flex, Text, Select, TextField } from '@radix-ui/themes';
import { RefreshCw } from 'lucide-react';

import type { AuditFilters, AuditCategory } from './audit.types';
import { CATEGORY_LABELS, COMMON_EVENT_TYPES, COMMON_DOMAINS } from './audit.constants';
import { FilterSidebar, type FilterSection } from '../../../shared/FilterSidebar';

const CATEGORY_OPTIONS: Array<{ value: string; label: string }> = [
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

const EVENT_TYPE_OPTIONS = COMMON_EVENT_TYPES.map((type) => ({
  value: type,
  label: type.charAt(0).toUpperCase() + type.slice(1),
}));

export interface AuditFilterSidebarProps {
  filters: AuditFilters;
  repositories: any[];
  onCategoryToggle: (category: AuditCategory) => void;
  onDomainToggle: (domain: string) => void;
  onEventTypeToggle: (eventType: string) => void;
  onInitiatorChange: (initiator: string) => void;
  onRepositoryNameChange: (repositoryName: string) => void;
  onRepositoryTypeChange: (repositoryType: string) => void;
  onDateRangeChange: (range: AuditFilters['dateRange']) => void;
  onClearAllFilters: () => void;
  hasActiveFilters: boolean;
  disabled?: boolean;
}

/**
 * Filter sidebar for Audit Log page.
 * Provides filtering by category, domain, event type, initiator, and date range.
 * Uses the standard FilterSidebar component for consistency with the Search module.
 */
export function AuditFilterSidebar({
  filters,
  repositories,
  onCategoryToggle,
  onDomainToggle,
  onEventTypeToggle,
  onInitiatorChange,
  onRepositoryNameChange,
  onRepositoryTypeChange,
  onDateRangeChange,
  onClearAllFilters,
  hasActiveFilters,
  disabled = false,
}: AuditFilterSidebarProps): JSX.Element {
  // Extract unique formats from repositories
  const formatOptions = useMemo(() => {
    const formats = new Set<string>();
    repositories.forEach(repo => {
      if (repo.format) formats.add(repo.format);
    });
    return Array.from(formats).sort().map(f => ({
      value: f,
      label: f.toUpperCase(),
    }));
  }, [repositories]);

  // Map repositories to options
  const repoOptions = useMemo(() => {
    return repositories
      .slice()
      .sort((a, b) => a.name.localeCompare(b.name))
      .map(repo => ({
        value: repo.name,
        label: repo.name,
      }));
  }, [repositories]);

  const sections: FilterSection[] = [
    {
      id: 'dateRange',
      label: 'Date Range',
      type: 'select',
      options: DATE_RANGE_OPTIONS,
      value: filters.dateRange,
      defaultExpanded: true,
    },
    {
      id: 'categories',
      label: 'Categories',
      type: 'checkbox',
      options: CATEGORY_OPTIONS,
      value: filters.categories,
      defaultExpanded: true,
    },
    {
      id: 'repositoryType',
      label: 'Format',
      type: 'select',
      options: [
        { value: '', label: 'All Formats' },
        ...formatOptions
      ],
      value: filters.repositoryType || '',
      defaultExpanded: true,
    },
    {
      id: 'repositoryName',
      label: 'Repository',
      type: 'select',
      options: [
        { value: '', label: 'All Repositories' },
        ...repoOptions
      ],
      value: filters.repositoryName || '',
      defaultExpanded: true,
    },
    {
      id: 'eventTypes',
      label: 'Event Types',
      type: 'checkbox',
      options: [
        ...EVENT_TYPE_OPTIONS,
        { value: 'automatic-malware-removed', label: 'Malware Cleaned' }
      ],
      value: filters.eventTypes,
      defaultExpanded: false,
    },
    {
      id: 'initiator',
      label: 'Initiator',
      type: 'text',
      value: filters.initiator || '',
      defaultExpanded: false,
    }
  ];

  const handleFilterChange = (sectionId: string, value: string | string[]) => {
    switch (sectionId) {
      case 'categories':
        const newCats = value as AuditCategory[];
        const currentCats = filters.categories;
        const added = newCats.find(c => !currentCats.includes(c));
        const removed = currentCats.find(c => !newCats.includes(c));
        if (added) onCategoryToggle(added);
        else if (removed) onCategoryToggle(removed);
        break;
      case 'eventTypes':
        const newTypes = value as string[];
        const currentTypes = filters.eventTypes;
        const addedType = newTypes.find(t => !currentTypes.includes(t));
        const removedType = currentTypes.find(t => !newTypes.includes(t));
        if (addedType) onEventTypeToggle(addedType);
        else if (removedType) onEventTypeToggle(removedType);
        break;
      case 'dateRange':
        onDateRangeChange(value as AuditFilters['dateRange']);
        break;
      case 'repositoryType':
        onRepositoryTypeChange(value as string);
        break;
      case 'repositoryName':
        onRepositoryNameChange(value as string);
        break;
      case 'initiator':
        onInitiatorChange(value as string);
        break;
    }
  };

  return (
    <FilterSidebar
      title="Audit Filters"
      sections={sections}
      onFilterChange={handleFilterChange}
      onClear={onClearAllFilters}
      disabled={disabled}
      className="audit-filter-sidebar"
    />
  );
}
