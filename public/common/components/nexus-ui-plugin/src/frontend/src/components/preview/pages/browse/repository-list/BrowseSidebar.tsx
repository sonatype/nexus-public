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

import React, { useCallback, useMemo } from 'react';
import { ChevronDown, ChevronRight, Filter, RotateCcw } from 'lucide-react';
import { Checkbox, Text, Flex } from '@radix-ui/themes';

import './BrowseSidebar.scss';

/**
 * Filter values for Browse (multi-select).
 */
export interface BrowseFilters {
  formats: string[];
  types: string[];
  statuses: string[];
}

/**
 * Available filter options computed from repositories.
 */
export interface FilterOptions {
  formats: Array<{ value: string; label: string; count: number }>;
  types: Array<{ value: string; label: string; count: number }>;
  statuses: Array<{ value: string; label: string; count: number }>;
}

export interface BrowseSidebarProps {
  /** Current filter values */
  filters: BrowseFilters;
  /** Available filter options */
  options: FilterOptions;
  /** Callback when a filter changes */
  onFilterChange: (filters: BrowseFilters) => void;
  /** Callback to reset all filters */
  onReset: () => void;
  /** Whether filters are disabled */
  disabled?: boolean;
}

// =============================================================================
// COLLAPSIBLE FILTER SECTION
// =============================================================================

interface FilterSectionProps {
  title: string;
  children: React.ReactNode;
  defaultExpanded?: boolean;
  count?: number;
}

function FilterSection({
  title,
  children,
  defaultExpanded = true,
  count,
}: FilterSectionProps): JSX.Element {
  const [expanded, setExpanded] = React.useState(defaultExpanded);

  return (
    <div className="filter-section">
      <button
        type="button"
        className="filter-section__header"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
      >
        {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
        <span className="filter-section__title">{title}</span>
        {count !== undefined && count > 0 && (
          <span className="filter-section__count">({count})</span>
        )}
      </button>
      {expanded && <div className="filter-section__content">{children}</div>}
    </div>
  );
}

// =============================================================================
// CHECKBOX FILTER LIST
// =============================================================================

interface CheckboxFilterProps {
  options: Array<{ value: string; label: string; count: number }>;
  selected: string[];
  onChange: (values: string[]) => void;
  disabled?: boolean;
}

function CheckboxFilter({
  options,
  selected,
  onChange,
  disabled,
}: CheckboxFilterProps): JSX.Element {
  const handleChange = useCallback(
    (value: string, checked: boolean) => {
      if (checked) {
        // Add to selection
        onChange([...selected, value]);
      } else {
        // Remove from selection
        onChange(selected.filter((v) => v !== value));
      }
    },
    [selected, onChange]
  );

  return (
    <div className="checkbox-filter">
      {options.map((option) => {
        const isSelected = selected.includes(option.value);
        return (
          <label
            key={option.value}
            className={`checkbox-filter__item ${isSelected ? 'checkbox-filter__item--selected' : ''}`}
          >
            <Checkbox
              checked={isSelected}
              onCheckedChange={(checked) => handleChange(option.value, checked === true)}
              disabled={disabled}
            />
            <span className="checkbox-filter__label">{option.label}</span>
            <span className="checkbox-filter__count">{option.count}</span>
          </label>
        );
      })}
    </div>
  );
}

// =============================================================================
// MAIN SIDEBAR COMPONENT
// =============================================================================

export function BrowseSidebar({
  filters,
  options,
  onFilterChange,
  onReset,
  disabled = false,
}: BrowseSidebarProps): JSX.Element {
  // Count active filters (total selected items across all filter categories)
  const activeFilterCount = useMemo(() => {
    return filters.formats.length + filters.types.length + filters.statuses.length;
  }, [filters]);

  // Handle format change (multi-select)
  const handleFormatChange = useCallback(
    (values: string[]) => {
      onFilterChange({ ...filters, formats: values });
    },
    [filters, onFilterChange]
  );

  // Handle type change (multi-select)
  const handleTypeChange = useCallback(
    (values: string[]) => {
      onFilterChange({ ...filters, types: values });
    },
    [filters, onFilterChange]
  );

  // Handle status change (multi-select)
  const handleStatusChange = useCallback(
    (values: string[]) => {
      onFilterChange({ ...filters, statuses: values });
    },
    [filters, onFilterChange]
  );

  return (
    <aside className="browse-sidebar">
      {/* Header with clear all button */}
      <div className="browse-sidebar__header">
        <div className="browse-sidebar__title">
          <Filter size={16} />
          <span>Filters</span>
          {activeFilterCount > 0 && (
            <span className="browse-sidebar__count">{activeFilterCount}</span>
          )}
        </div>
        {activeFilterCount > 0 && (
          <button
            type="button"
            className="browse-sidebar__reset"
            onClick={onReset}
            disabled={disabled}
            title="Clear all filters"
          >
            <RotateCcw size={14} />
            Clear all
          </button>
        )}
      </div>

      <div className="browse-sidebar__content">
        <FilterSection
          title="Format"
          defaultExpanded
          count={filters.formats.length > 0 ? filters.formats.length : undefined}
        >
          <CheckboxFilter
            options={options.formats}
            selected={filters.formats}
            onChange={handleFormatChange}
            disabled={disabled}
          />
        </FilterSection>

        {/* Type Filter */}
        <FilterSection
          title="Type"
          defaultExpanded
          count={filters.types.length > 0 ? filters.types.length : undefined}
        >
          <CheckboxFilter
            options={options.types}
            selected={filters.types}
            onChange={handleTypeChange}
            disabled={disabled}
          />
        </FilterSection>

        {/* Status Filter */}
        <FilterSection
          title="Status"
          defaultExpanded
          count={filters.statuses.length > 0 ? filters.statuses.length : undefined}
        >
          <CheckboxFilter
            options={options.statuses}
            selected={filters.statuses}
            onChange={handleStatusChange}
            disabled={disabled}
          />
        </FilterSection>
      </div>

      {/* Footer hint */}
      <div className="browse-sidebar__footer">
        <Text size="1" color="gray">
          Filters apply automatically
        </Text>
      </div>
    </aside>
  );
}

export default BrowseSidebar;

