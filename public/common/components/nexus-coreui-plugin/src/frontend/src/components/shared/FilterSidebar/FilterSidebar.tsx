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

import React, { useCallback, useMemo, useState } from 'react';
import { ChevronDown, ChevronRight, Filter, RotateCcw } from 'lucide-react';
import { Checkbox, Flex, Text, TextField, Select } from '@radix-ui/themes';

import './FilterSidebar.scss';

/**
 * Filter option representing a selectable item.
 */
export interface FilterOption {
  /** Unique value for the filter */
  value: string;
  /** Display label */
  label: string;
  /** Optional count of items matching this filter */
  count?: number;
  /** Optional icon to display next to label */
  icon?: React.ReactNode;
}

/**
 * Filter section configuration.
 */
export interface FilterSection {
  /** Unique identifier for the section */
  id: string;
  /** Section label displayed in header */
  label: string;
  /** Type of filter control */
  type: 'checkbox' | 'select' | 'text';
  /** Available options for checkbox/select types */
  options?: FilterOption[];
  /** Currently selected value(s) */
  value: string | string[];
  /** Whether section is expanded by default */
  defaultExpanded?: boolean;
}

export interface FilterSidebarProps {
  /** Filter sections to display */
  sections: FilterSection[];
  /** Callback when a filter value changes */
  onFilterChange: (sectionId: string, value: string | string[]) => void;
  /** Callback to reset all filters */
  onClear: () => void;
  /** Whether filters are disabled */
  disabled?: boolean;
  /** Optional title for the sidebar */
  title?: string;
  /** Optional footer text */
  footerText?: string;
  /** Custom class name */
  className?: string;
}

// =============================================================================
// COLLAPSIBLE FILTER SECTION COMPONENT
// =============================================================================

interface FilterSectionHeaderProps {
  title: string;
  expanded: boolean;
  activeCount?: number;
  onClick: () => void;
}

function FilterSectionHeader({
  title,
  expanded,
  activeCount,
  onClick,
}: FilterSectionHeaderProps): JSX.Element {
  return (
    <button
      type="button"
      className="filter-section__header"
      onClick={onClick}
      aria-expanded={expanded}
    >
      {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
      <span className="filter-section__title">{title}</span>
      {activeCount !== undefined && activeCount > 0 && (
        <span className="filter-section__count">({activeCount})</span>
      )}
    </button>
  );
}

// =============================================================================
// CHECKBOX FILTER COMPONENT
// =============================================================================

interface CheckboxFilterListProps {
  options: FilterOption[];
  selected: string[];
  onChange: (values: string[]) => void;
  disabled?: boolean;
}

function CheckboxFilterList({
  options,
  selected,
  onChange,
  disabled,
}: CheckboxFilterListProps): JSX.Element {
  const handleChange = useCallback(
    (value: string, checked: boolean) => {
      if (checked) {
        onChange([...selected, value]);
      } else {
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
            <Flex align="center" gap="2" style={{ flex: 1 }}>
              {option.icon && <span className="checkbox-filter__icon">{option.icon}</span>}
              <span className="checkbox-filter__label">{option.label}</span>
            </Flex>
            {option.count !== undefined && (
              <span className="checkbox-filter__count">{option.count}</span>
            )}
          </label>
        );
      })}
    </div>
  );
}

// =============================================================================
// TEXT FILTER COMPONENT
// =============================================================================

interface TextFilterProps {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

function TextFilter({
  value,
  onChange,
  disabled,
  placeholder = 'Type to filter...',
}: TextFilterProps): JSX.Element {
  return (
    <TextField.Root
      className="text-filter"
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
    />
  );
}

// =============================================================================
// SELECT FILTER COMPONENT
// =============================================================================

/** Radix Select.Item must not use an empty string value; map "" to this internally. */
const SELECT_EMPTY_VALUE_PLACEHOLDER = '__filter_sidebar_empty__';

interface SelectFilterProps {
  options: FilterOption[];
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

function SelectFilter({
  options,
  value,
  onChange,
  disabled,
}: SelectFilterProps): JSX.Element {
  const radixValue =
    value === '' ? SELECT_EMPTY_VALUE_PLACEHOLDER : value;
  const handleChange = (v: string) => {
    onChange(v === SELECT_EMPTY_VALUE_PLACEHOLDER ? '' : v);
  };
  return (
    <Select.Root
      value={radixValue}
      onValueChange={handleChange}
      disabled={disabled}
      size="2"
    >
      <Select.Trigger style={{ width: '100%' }} />
      <Select.Content position="popper" side="bottom" avoidCollisions={false} sideOffset={4}>
        {options.map((opt, idx) => {
          const itemValue =
            opt.value === '' ? SELECT_EMPTY_VALUE_PLACEHOLDER : opt.value;
          return (
            <Select.Item key={`${itemValue}-${idx}`} value={itemValue}>
              {opt.label}
            </Select.Item>
          );
        })}
      </Select.Content>
    </Select.Root>
  );
}

// =============================================================================
// MAIN FILTER SIDEBAR COMPONENT
// =============================================================================

/**
 * FilterSidebar provides a reusable sidebar component for filtering lists.
 *
 * Features:
 * - Collapsible filter sections
 * - Checkbox filters with counts
 * - Text search filters
 * - Clear all button
 * - Active filter count display
 * - Dark mode support
 *
 * @example
 * ```tsx
 * <FilterSidebar
 *   sections={[
 *     {
 *       id: 'format',
 *       label: 'Format',
 *       type: 'checkbox',
 *       options: [
 *         { value: 'maven2', label: 'Maven', count: 12 },
 *         { value: 'npm', label: 'NPM', count: 8 },
 *       ],
 *       value: selectedFormats,
 *     },
 *   ]}
 *   onFilterChange={(sectionId, value) => handleFilterChange(sectionId, value)}
 *   onClear={() => resetFilters()}
 * />
 * ```
 */
export function FilterSidebar({
  sections,
  onFilterChange,
  onClear,
  disabled = false,
  title = 'Filters',
  footerText = 'Filters apply automatically',
  className = '',
}: FilterSidebarProps): JSX.Element {
  // Track expanded state for each section
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>(() => {
    const initial: Record<string, boolean> = {};
    sections.forEach((section) => {
      initial[section.id] = section.defaultExpanded ?? true;
    });
    return initial;
  });

  // Count total active filters
  const activeFilterCount = useMemo(() => {
    return sections.reduce((count, section) => {
      if (Array.isArray(section.value)) {
        return count + section.value.length;
      }
      return count + (section.value ? 1 : 0);
    }, 0);
  }, [sections]);

  // Toggle section expansion
  const toggleSection = useCallback((sectionId: string) => {
    setExpandedSections((prev) => ({
      ...prev,
      [sectionId]: !prev[sectionId],
    }));
  }, []);

  // Get active count for a section
  const getSectionActiveCount = (section: FilterSection): number | undefined => {
    if (Array.isArray(section.value) && section.value.length > 0) {
      return section.value.length;
    }
    return undefined;
  };

  return (
    <aside className={`filter-sidebar ${className}`} data-testid="filter-sidebar">
      {/* Header */}
      <div className="filter-sidebar__header">
        <div className="filter-sidebar__title">
          <Filter size={16} aria-hidden="true" />
          <span>{title}</span>
          {activeFilterCount > 0 && (
            <span className="filter-sidebar__count">{activeFilterCount}</span>
          )}
        </div>
        {activeFilterCount > 0 && (
          <button
            type="button"
            className="filter-sidebar__reset"
            onClick={onClear}
            disabled={disabled}
            title="Clear all filters"
          >
            <RotateCcw size={14} aria-hidden="true" />
            Clear all
          </button>
        )}
      </div>

      {/* Filter Sections */}
      <div className="filter-sidebar__content">
        {sections.map((section) => {
          const isExpanded = expandedSections[section.id] ?? true;
          const activeCount = getSectionActiveCount(section);

          return (
            <div key={section.id} className="filter-section">
              <FilterSectionHeader
                title={section.label}
                expanded={isExpanded}
                activeCount={activeCount}
                onClick={() => toggleSection(section.id)}
              />
              {isExpanded && (
                <div className="filter-section__content">
                  {section.type === 'checkbox' && section.options && (
                    <CheckboxFilterList
                      options={section.options}
                      selected={Array.isArray(section.value) ? section.value : []}
                      onChange={(values) => onFilterChange(section.id, values)}
                      disabled={disabled}
                    />
                  )}
                  {section.type === 'text' && (
                    <TextFilter
                      value={typeof section.value === 'string' ? section.value : ''}
                      onChange={(value) => onFilterChange(section.id, value)}
                      disabled={disabled}
                    />
                  )}
                  {section.type === 'select' && section.options && (
                    <SelectFilter
                      options={section.options}
                      value={typeof section.value === 'string' ? section.value : ''}
                      onChange={(value) => onFilterChange(section.id, value)}
                      disabled={disabled}
                    />
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Footer */}
      {footerText && (
        <div className="filter-sidebar__footer">
          <Text size="1" color="gray">
            {footerText}
          </Text>
        </div>
      )}
    </aside>
  );
}

export default FilterSidebar;


