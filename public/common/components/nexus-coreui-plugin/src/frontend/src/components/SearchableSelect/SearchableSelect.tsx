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

import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { ChevronDown, Search, Check, X } from 'lucide-react';

import './SearchableSelect.scss';

export interface SelectOption {
  value: string;
  label: string;
  icon?: React.ReactNode;
}

export interface SearchableSelectProps {
  /** Array of options to display */
  options: SelectOption[];
  /** Currently selected value */
  value: string;
  /** Callback when selection changes */
  onChange: (value: string) => void;
  /** Placeholder text when nothing selected */
  placeholder?: string;
  /** Search input placeholder */
  searchPlaceholder?: string;
  /** Whether the select is disabled */
  disabled?: boolean;
  /** Optional className for styling */
  className?: string;
  /** Label for the "all" option (e.g., "All Formats", "All repositories") */
  allOptionLabel?: string;
}

/**
 * SearchableSelect - A dropdown with search/filter capability.
 * 
 * Features:
 * - Type to filter options
 * - Keyboard navigation (Arrow keys, Enter, Escape)
 * - Click outside to close
 * - Accessible with ARIA attributes
 */
export function SearchableSelect({
  options,
  value,
  onChange,
  placeholder = 'Select...',
  searchPlaceholder = 'Search...',
  disabled = false,
  className = '',
  allOptionLabel,
}: SearchableSelectProps): JSX.Element {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  // Build full options list (with "All" option if provided)
  const fullOptions = useMemo(() => {
    if (allOptionLabel) {
      return [{ value: '', label: allOptionLabel }, ...options];
    }
    return options;
  }, [options, allOptionLabel]);

  // Filter options based on search query
  const filteredOptions = useMemo(() => {
    if (!searchQuery.trim()) {
      return fullOptions;
    }
    const query = searchQuery.toLowerCase();
    return fullOptions.filter(
      (opt) => opt.label.toLowerCase().includes(query) || opt.value.toLowerCase().includes(query)
    );
  }, [fullOptions, searchQuery]);

  // Get display label and icon for current value
  const selectedOption = useMemo(() => {
    return fullOptions.find((opt) => opt.value === value);
  }, [fullOptions, value]);

  const displayLabel = selectedOption?.label || placeholder;
  const displayIcon = selectedOption?.icon;

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setSearchQuery('');
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Focus search input when dropdown opens
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  // Scroll highlighted option into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const highlightedElement = listRef.current.children[highlightedIndex] as HTMLElement;
      if (highlightedElement) {
        highlightedElement.scrollIntoView({ block: 'nearest' });
      }
    }
  }, [highlightedIndex]);

  // Handle option selection
  const handleSelect = useCallback(
    (optionValue: string) => {
      onChange(optionValue);
      setIsOpen(false);
      setSearchQuery('');
      setHighlightedIndex(-1);
    },
    [onChange]
  );

  // Handle keyboard navigation
  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (disabled) return;

      switch (event.key) {
        case 'Enter':
          event.preventDefault();
          if (!isOpen) {
            setIsOpen(true);
          } else if (highlightedIndex >= 0 && filteredOptions[highlightedIndex]) {
            handleSelect(filteredOptions[highlightedIndex].value);
          }
          break;
        case 'Escape':
          event.preventDefault();
          setIsOpen(false);
          setSearchQuery('');
          setHighlightedIndex(-1);
          break;
        case 'ArrowDown':
          event.preventDefault();
          if (!isOpen) {
            setIsOpen(true);
          } else {
            setHighlightedIndex((prev) =>
              prev < filteredOptions.length - 1 ? prev + 1 : prev
            );
          }
          break;
        case 'ArrowUp':
          event.preventDefault();
          if (isOpen) {
            setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : 0));
          }
          break;
        case 'Tab':
          setIsOpen(false);
          setSearchQuery('');
          break;
      }
    },
    [disabled, isOpen, highlightedIndex, filteredOptions, handleSelect]
  );

  // Toggle dropdown
  const handleToggle = useCallback(() => {
    if (!disabled) {
      setIsOpen((prev) => !prev);
      if (isOpen) {
        setSearchQuery('');
      }
    }
  }, [disabled, isOpen]);

  // Clear selection
  const handleClear = useCallback(
    (event: React.MouseEvent) => {
      event.stopPropagation();
      onChange('');
      setIsOpen(false);
    },
    [onChange]
  );

  return (
    <div
      ref={containerRef}
      className={`searchable-select ${className} ${disabled ? 'searchable-select--disabled' : ''} ${isOpen ? 'searchable-select--open' : ''}`}
      onKeyDown={handleKeyDown}
    >
      {/* Trigger button */}
      <button
        type="button"
        className="searchable-select__trigger"
        onClick={handleToggle}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <div 
          className={`searchable-select__value ${!value ? 'searchable-select__value--placeholder' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          {displayIcon && <span className="searchable-select__trigger-icon">{displayIcon}</span>}
          <span>{displayLabel}</span>
        </div>
        <span className="searchable-select__icons">
          {value && !disabled && (
            <span
              className="searchable-select__clear"
              onClick={handleClear}
              role="button"
              aria-label="Clear selection"
            >
              <X size={14} />
            </span>
          )}
          <ChevronDown size={16} className={`searchable-select__chevron ${isOpen ? 'searchable-select__chevron--open' : ''}`} />
        </span>
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div className="searchable-select__dropdown">
          {/* Search input */}
          <div className="searchable-select__search">
            <Search size={14} className="searchable-select__search-icon" />
            <input
              ref={inputRef}
              type="text"
              className="searchable-select__search-input"
              placeholder={searchPlaceholder}
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setHighlightedIndex(0);
              }}
              aria-label="Search options"
            />
          </div>

          {/* Options list */}
          <ul
            ref={listRef}
            className="searchable-select__list"
            role="listbox"
            aria-label="Options"
          >
            {filteredOptions.length === 0 ? (
              <li className="searchable-select__empty">No matches found</li>
            ) : (
              filteredOptions.map((option, index) => (
                <li
                  key={option.value || '__all__'}
                  className={`searchable-select__option ${option.value === value ? 'searchable-select__option--selected' : ''} ${index === highlightedIndex ? 'searchable-select__option--highlighted' : ''}`}
                  onClick={() => handleSelect(option.value)}
                  onMouseEnter={() => setHighlightedIndex(index)}
                  role="option"
                  aria-selected={option.value === value}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1 }}>
                    {option.icon && <span className="searchable-select__option-icon">{option.icon}</span>}
                    <span className="searchable-select__option-label">{option.label}</span>
                  </div>
                  {option.value === value && (
                    <Check size={14} className="searchable-select__option-check" />
                  )}
                </li>
              ))
            )}
          </ul>
        </div>
      )}
    </div>
  );
}

export default SearchableSelect;
