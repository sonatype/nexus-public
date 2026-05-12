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

import React, { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import PropTypes from 'prop-types';
import { Box, Text, Tooltip } from '@radix-ui/themes';
import { AlertCircle, ChevronDown, ChevronRight } from 'lucide-react';

const CHIP_TRUNCATE_LENGTH = 35;

function HighlightMatch({ text, query }) {
  if (!query) return <>{text}</>;
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx === -1) return <>{text}</>;
  return (
    <>
      {text.slice(0, idx)}
      <mark className="settings-combobox__match">{text.slice(idx, idx + query.length)}</mark>
      {text.slice(idx + query.length)}
    </>
  );
}

import './SettingsCombobox.scss';

/**
 * Detect if dark mode is active by checking the document root attribute
 */
function useIsDarkMode() {
  const [isDark, setIsDark] = useState(
    () => document.documentElement.getAttribute('data-theme') === 'dark'
  );
  useEffect(() => {
    const observer = new MutationObserver(() => {
      setIsDark(document.documentElement.getAttribute('data-theme') === 'dark');
    });
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    return () => observer.disconnect();
  }, []);
  return isDark;
}

/**
 * SettingsCombobox - Text input with dropdown suggestions
 *
 * Single mode: free-form text input with suggestion dropdown.
 * Multiple mode: searchable multi-select with removable chips and grouped results.
 *
 * @example Single mode
 * <SettingsCombobox name="domain" label="Domain" value={val} onChange={setVal} options={opts} />
 *
 * @example Multiple mode (GitHub label picker UX)
 * <SettingsCombobox
 *   name="privileges"
 *   label="Privileges"
 *   multiple
 *   selectedValues={selected}
 *   onMultiChange={setSelected}
 *   options={opts}
 *   groupBy={(opt) => opt.value.split('-').slice(0,3).join('-')}
 *   placeholder="Search privileges..."
 * />
 */
export function SettingsCombobox({
  name,
  label,
  value = '',
  onChange,
  onBlur,
  options = [],
  placeholder,
  helpText = '',
  error = '',
  required = false,
  disabled = false,
  className = '',
  allowCustom = true,
  multiple = false,
  selectedValues = [],
  onMultiChange,
  groupBy,
  chipLimit = 50,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState(multiple ? '' : value);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const [collapsedGroups, setCollapsedGroups] = useState(new Set());
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const containerRef = useRef(null);
  const isSelectingRef = useRef(false);
  const isDarkMode = useIsDarkMode();

  const toggleGroupCollapse = useCallback((group) => {
    setCollapsedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(group)) next.delete(group);
      else next.add(group);
      return next;
    });
  }, []);

  const effectivePlaceholder = placeholder || (multiple ? 'Search...' : 'Select or type...');
  const inputId = `settings-combobox-${name}`;
  const listboxId = `settings-combobox-list-${name}`;
  const helpId = `settings-combobox-help-${name}`;
  const errorId = `settings-combobox-error-${name}`;

  // Sync input value with external value (single mode only)
  // Skip sync when user is actively selecting to avoid overwriting their selection
  useEffect(() => {
    if (!multiple && !isSelectingRef.current) {
      setInputValue(value);
    }
  }, [value, multiple]); // Don't include inputValue - we only want to sync when the prop changes, not when user types

  // Build selected set for O(1) lookups
  const selectedSet = useMemo(
    () => new Set(multiple ? selectedValues : []),
    [multiple, selectedValues]
  );

  // Filter options based on input text
  const filteredOptions = useMemo(() => {
    const query = inputValue.toLowerCase();
    return options.filter(option => {
      if (multiple && selectedSet.has(option.value)) return false;
      if (!query) return true;
      return (
        option.value.toLowerCase().includes(query) ||
        option.label.toLowerCase().includes(query) ||
        (option.description && option.description.toLowerCase().includes(query))
      );
    });
  }, [options, inputValue, multiple, selectedSet]);

  // Group filtered options when groupBy is provided
  const groupedOptions = useMemo(() => {
    if (!groupBy) return null;
    const groups = new Map();
    for (const opt of filteredOptions) {
      const key = groupBy(opt);
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(opt);
    }
    return groups;
  }, [filteredOptions, groupBy]);

  // Flat list for keyboard navigation when grouped
  const flatFilteredOptions = filteredOptions;

  // Multi-mode: toggle an option in selectedValues
  const toggleOption = useCallback((optionValue) => {
    if (!onMultiChange) return;
    const next = selectedSet.has(optionValue)
      ? selectedValues.filter((v) => v !== optionValue)
      : [...selectedValues, optionValue];
    onMultiChange(next);
  }, [selectedValues, selectedSet, onMultiChange]);

  const removeChip = useCallback((val) => {
    if (!onMultiChange) return;
    onMultiChange(selectedValues.filter((v) => v !== val));
  }, [selectedValues, onMultiChange]);

  // Look up label for a value
  const optionLabelMap = useMemo(() => {
    const m = new Map();
    for (const o of options) m.set(o.value, o.label);
    return m;
  }, [options]);

  const handleInputChange = useCallback((e) => {
    const newValue = e.target.value;
    setInputValue(newValue);
    setIsOpen(true);
    setHighlightedIndex(-1);

    if (!multiple && allowCustom && onChange) {
      onChange(newValue);
    }
  }, [onChange, allowCustom, multiple]);

  const handleSelectOption = useCallback((option) => {
    if (multiple) {
      toggleOption(option.value);
      setInputValue('');
      inputRef.current?.focus();
    } else {
      // Mark that we're selecting to prevent external value sync from overwriting
      isSelectingRef.current = true;
      setInputValue(option.value);
      setIsOpen(false);
      setHighlightedIndex(-1);
      if (onChange) onChange(option.value);
      // Reset the flag after a short delay to allow parent to update
      setTimeout(() => {
        isSelectingRef.current = false;
      }, 100);
    }
  }, [onChange, multiple, toggleOption]);

  const handleInputBlur = useCallback((e) => {
    // Capture isSelecting state immediately (not after timeout)
    // to correctly detect if blur happened during option selection
    const wasSelecting = isSelectingRef.current;

    // Delay closing and validation to allow selection to complete
    setTimeout(() => {
      setIsOpen(false);
      setHighlightedIndex(-1);
      // Trigger validation after dropdown closes and form state updates
      // Skip if user was actively selecting when blur occurred
      if (onBlur && !wasSelecting) {
        onBlur(e);
      }
    }, 200);
  }, [onBlur]);

  const handleInputFocus = useCallback(() => {
    setIsOpen(true);
  }, []);

  const handleInputClick = useCallback(() => {
    if (!multiple && inputRef.current && inputValue) {
      inputRef.current.select();
    }
    setIsOpen(true);
  }, [inputValue, multiple]);

  const handleKeyDown = useCallback((e) => {
    if (multiple && e.key === 'Backspace' && !inputValue && selectedValues.length > 0) {
      removeChip(selectedValues[selectedValues.length - 1]);
      return;
    }

    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        setIsOpen(true);
        e.preventDefault();
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex(prev =>
          prev < flatFilteredOptions.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex(prev =>
          prev > 0 ? prev - 1 : flatFilteredOptions.length - 1
        );
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < flatFilteredOptions.length) {
          handleSelectOption(flatFilteredOptions[highlightedIndex]);
        } else if (!multiple && flatFilteredOptions.length === 1) {
          handleSelectOption(flatFilteredOptions[0]);
        }
        break;
      case 'Escape':
        setIsOpen(false);
        setHighlightedIndex(-1);
        break;
      case 'Tab':
        setIsOpen(false);
        setHighlightedIndex(-1);
        break;
      default:
        break;
    }
  }, [isOpen, flatFilteredOptions, highlightedIndex, handleSelectOption, multiple, inputValue, selectedValues, removeChip]);

  // Scroll highlighted option into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[role="option"]');
      if (items[highlightedIndex]) {
        items[highlightedIndex].scrollIntoView({ block: 'nearest' });
      }
    }
  }, [highlightedIndex]);

  // Close on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const totalAvailable = multiple ? options.length - selectedSet.size : options.length;

  const renderOptionLabel = (option) => (
    <span className="settings-combobox__option-content">
      <span className="settings-combobox__option-label">
        <HighlightMatch text={option.label} query={inputValue} />
      </span>
      {option.description && (
        <span className="settings-combobox__option-desc">
          <HighlightMatch text={option.description} query={inputValue} />
        </span>
      )}
    </span>
  );

  const renderDropdownItems = () => {
    if (groupedOptions) {
      let flatIdx = 0;
      const elements = [];
      for (const [group, items] of groupedOptions) {
        const isCollapsed = collapsedGroups.has(group);
        elements.push(
          <li
            key={`group-${group}`}
            className="settings-combobox__group-header"
            role="presentation"
            onClick={() => toggleGroupCollapse(group)}
          >
            {isCollapsed ? <ChevronRight size={12} /> : <ChevronDown size={12} />}
            {group} ({items.length})
          </li>
        );
        if (!isCollapsed) {
          for (const option of items) {
            const idx = flatIdx++;
            const isSelected = selectedSet.has(option.value);
            elements.push(
              <li
                key={option.value}
                id={`${listboxId}-option-${idx}`}
                role="option"
                aria-selected={isSelected}
                onClick={() => handleSelectOption(option)}
                className={`settings-combobox__option ${highlightedIndex === idx ? 'settings-combobox__option--highlighted' : ''} ${isSelected ? 'settings-combobox__option--selected' : ''}`}
              >
                {multiple && (
                  <span className="settings-combobox__check" aria-hidden="true">
                    {isSelected ? '✓' : ''}
                  </span>
                )}
                {renderOptionLabel(option)}
              </li>
            );
          }
        } else {
          flatIdx += items.length;
        }
      }
      return elements;
    }

    return flatFilteredOptions.map((option, index) => {
      const isSelected = selectedSet.has(option.value);
      return (
        <li
          key={option.value}
          id={`${listboxId}-option-${index}`}
          role="option"
          aria-selected={multiple ? isSelected : highlightedIndex === index}
          onClick={() => handleSelectOption(option)}
          className={`settings-combobox__option ${highlightedIndex === index ? 'settings-combobox__option--highlighted' : ''} ${isSelected || (!multiple && option.value === value) ? 'settings-combobox__option--selected' : ''}`}
        >
          {multiple && (
            <span className="settings-combobox__check" aria-hidden="true">
              {isSelected ? '✓' : ''}
            </span>
          )}
          {renderOptionLabel(option)}
        </li>
      );
    });
  };

  const showChips = multiple && selectedValues.length > 0;
  const chipValues = selectedValues.length > chipLimit
    ? selectedValues.slice(0, chipLimit) : selectedValues;
  const overflowCount = selectedValues.length - chipValues.length;

  return (
    <Box
      ref={containerRef}
      className={`settings-combobox ${multiple ? 'settings-combobox--multiple' : ''} ${error ? 'settings-combobox--error' : ''} ${className}`.trim()}
    >
      {label && (
        <label htmlFor={inputId} className="settings-combobox__label">
          {label}
          {required && <span className="settings-combobox__required">*</span>}
          {multiple && selectedValues.length > 0 && (
            <span className="settings-combobox__count">{selectedValues.length} selected</span>
          )}
        </label>
      )}
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-combobox__help">
          {helpText}
        </Text>
      )}
      <Box className="settings-combobox__wrapper">
        <input
          ref={inputRef}
          id={inputId}
          type="text"
          name={name}
          value={inputValue}
          onChange={handleInputChange}
          onFocus={handleInputFocus}
          onClick={handleInputClick}
          onBlur={handleInputBlur}
          onKeyDown={handleKeyDown}
          placeholder={effectivePlaceholder}
          disabled={disabled}
          aria-describedby={`${helpText ? helpId : ''} ${error ? errorId : ''}`.trim() || undefined}
          aria-invalid={!!error}
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          aria-controls={listboxId}
          aria-activedescendant={highlightedIndex >= 0 ? `${listboxId}-option-${highlightedIndex}` : undefined}
          role="combobox"
          autoComplete="off"
          data-testid={`combobox-${name}`}
          className="settings-combobox__input"
        />
        <ChevronDown
          size={16}
          className={`settings-combobox__chevron ${isOpen ? 'settings-combobox__chevron--open' : ''}`}
          aria-hidden="true"
        />
        {isOpen && (flatFilteredOptions.length > 0 || inputValue) && (
          <ul
            ref={listRef}
            id={listboxId}
            role="listbox"
            aria-multiselectable={multiple || undefined}
            className="settings-combobox__listbox"
            style={{
              backgroundColor: isDarkMode ? '#1c2128' : '#ffffff',
              borderColor: isDarkMode ? '#444c56' : '#d1d5db',
              color: isDarkMode ? '#e6edf3' : '#1f2937',
              boxShadow: isDarkMode
                ? '0 4px 12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1)'
                : '0 4px 12px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.05)',
            }}
          >
            {multiple && (
              <li className="settings-combobox__counter" role="status" aria-live="polite">
                {flatFilteredOptions.length} of {totalAvailable} matching
                {selectedValues.length > 0 && ` · ${selectedValues.length} selected`}
              </li>
            )}
            {flatFilteredOptions.length > 0 ? renderDropdownItems() : (
              <li className="settings-combobox__empty" role="option" aria-disabled="true">
                No results matching &ldquo;{inputValue}&rdquo;
              </li>
            )}
          </ul>
        )}
      </Box>
      {showChips && (
        <div className="settings-combobox__chips" data-testid={`combobox-chips-${name}`}>
          {chipValues.map((val) => {
            const fullLabel = optionLabelMap.get(val) || val;
            const needsTruncation = fullLabel.length > CHIP_TRUNCATE_LENGTH;
            const displayLabel = needsTruncation
              ? fullLabel.slice(0, CHIP_TRUNCATE_LENGTH - 3) + '...'
              : fullLabel;
            const chip = (
              <span key={val} className="settings-combobox__chip">
                <span className="settings-combobox__chip-label">{displayLabel}</span>
                {!disabled && (
                  <button
                    type="button"
                    className="settings-combobox__chip-remove"
                    onClick={() => removeChip(val)}
                    aria-label={`Remove ${fullLabel}`}
                    tabIndex={-1}
                  >
                    ×
                  </button>
                )}
              </span>
            );
            return needsTruncation ? (
              <Tooltip key={val} content={fullLabel}>{chip}</Tooltip>
            ) : chip;
          })}
          {overflowCount > 0 && (
            <span className="settings-combobox__chip settings-combobox__chip--overflow">
              +{overflowCount} more
            </span>
          )}
        </div>
      )}
      {error && (
        <Text as="p" size="1" id={errorId} className="settings-combobox__error-text">
          <AlertCircle size={14} />
          {error}
        </Text>
      )}
    </Box>
  );
}

SettingsCombobox.propTypes = {
  name: PropTypes.string.isRequired,
  label: PropTypes.string,
  value: PropTypes.string,
  onChange: PropTypes.func,
  onBlur: PropTypes.func,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      description: PropTypes.string,
    })
  ),
  placeholder: PropTypes.string,
  helpText: PropTypes.string,
  error: PropTypes.string,
  required: PropTypes.bool,
  disabled: PropTypes.bool,
  className: PropTypes.string,
  allowCustom: PropTypes.bool,
  /** Enable multi-select mode with chips */
  multiple: PropTypes.bool,
  /** Selected values array (multiple mode) */
  selectedValues: PropTypes.arrayOf(PropTypes.string),
  /** Change handler for multiple mode (receives string[]) */
  onMultiChange: PropTypes.func,
  /** Group options by category: (option) => groupLabel */
  groupBy: PropTypes.func,
  /** Max chips to render before showing "+N more" */
  chipLimit: PropTypes.number,
};

export default SettingsCombobox;
