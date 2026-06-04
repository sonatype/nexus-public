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

import React, { useCallback, useRef, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';

/** Debounce delay for select filter changes (ms) */
const SELECT_FILTER_DEBOUNCE_MS = 200;

export interface SelectFilterOption {
  /** Option value */
  value: string;
  /** Display label */
  label: string;
}

export interface SelectFilterProps {
  /** Filter ID */
  id: string;
  /** Display label */
  label: string;
  /** Current value */
  value: string;
  /** Callback when value changes */
  onChange: (id: string, value: string) => void;
  /** Callback to trigger search after selection (for immediate filter application) */
  onSearch?: () => void;
  /** Available options */
  options: readonly SelectFilterOption[];
  /** Placeholder text for empty state */
  placeholder?: string;
  /** Whether the filter is disabled */
  disabled?: boolean;
}

/**
 * Dropdown select filter component for the search sidebar.
 * Auto-applies filter with 200ms debounce when selection changes.
 */
export function SelectFilter({
  id,
  label,
  value,
  onChange,
  onSearch,
  options,
  placeholder = 'Select...',
  disabled = false,
}: SelectFilterProps): JSX.Element {
  // Track debounce timer
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clean up debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      // Update UI state immediately
      onChange(id, e.target.value);
      
      // Cancel any pending debounced search
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
      
      // Schedule debounced search
      if (onSearch) {
        debounceRef.current = setTimeout(() => {
          onSearch();
        }, SELECT_FILTER_DEBOUNCE_MS);
      }
    },
    [id, onChange, onSearch]
  );

  return (
    <div className="select-filter">
      <label className="select-filter__label" htmlFor={`filter-${id}`}>
        {label}
      </label>
      <div className="select-filter__input-wrapper">
        <select
          id={`filter-${id}`}
          className="select-filter__select"
          value={value}
          onChange={handleChange}
          disabled={disabled}
        >
          <option value="">{placeholder}</option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <span className="select-filter__icon">
          <ChevronDown size={14} />
        </span>
      </div>
    </div>
  );
}

export default SelectFilter;


