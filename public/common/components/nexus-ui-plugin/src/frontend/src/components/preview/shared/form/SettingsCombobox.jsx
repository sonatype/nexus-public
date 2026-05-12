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

import React, { useState, useCallback, useRef, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Box, Text } from '@radix-ui/themes';
import { AlertCircle, ChevronDown } from 'lucide-react';

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
 * Allows free-form text input while providing suggestions from a predefined list.
 * User can either select from suggestions or type a custom value.
 * 
 * @example
 * <SettingsCombobox
 *   name="domain"
 *   label="Domain"
 *   value={domain}
 *   onChange={setDomain}
 *   options={[
 *     { value: 'users', label: 'users' },
 *     { value: 'roles', label: 'roles' },
 *     { value: 'privileges', label: 'privileges' },
 *   ]}
 *   helpText="Application domain (e.g., users, roles)"
 *   allowCustom
 * />
 */
export function SettingsCombobox({
  name,
  label,
  value = '',
  onChange,
  onBlur,
  options = [],
  placeholder = 'Select or type...',
  helpText = '',
  error = '',
  required = false,
  disabled = false,
  className = '',
  allowCustom = true,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState(value);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const [isTyping, setIsTyping] = useState(false);
  const [openUpward, setOpenUpward] = useState(false);
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const containerRef = useRef(null);
  const isDarkMode = useIsDarkMode();

  const inputId = `settings-combobox-${name}`;
  const listboxId = `settings-combobox-list-${name}`;
  const helpId = `settings-combobox-help-${name}`;
  const errorId = `settings-combobox-error-${name}`;

  // Sync input value with external value
  useEffect(() => {
    setInputValue(value);
  }, [value]);

  // Filter options based on input - only filter while actively typing,
  // show all options when dropdown is opened by focus/click
  const filteredOptions = isTyping
    ? options.filter(option =>
        option.value.toLowerCase().includes(inputValue.toLowerCase()) ||
        option.label.toLowerCase().includes(inputValue.toLowerCase())
      )
    : options;

  const handleInputChange = useCallback((e) => {
    const newValue = e.target.value;
    setInputValue(newValue);
    setIsOpen(true);
    setIsTyping(true);
    setHighlightedIndex(-1);

    if (allowCustom && onChange) {
      onChange(newValue);
    }
  }, [onChange, allowCustom]);

  const handleSelectOption = useCallback((option) => {
    setInputValue(option.value);
    setIsOpen(false);
    setIsTyping(false);
    setHighlightedIndex(-1);
    if (onChange) {
      onChange(option.value);
    }
  }, [onChange]);

  const handleInputBlur = useCallback((e) => {
    // Delay close to allow click on option
    setTimeout(() => {
      setIsOpen(false);
      setHighlightedIndex(-1);
    }, 150);

    if (onBlur) {
      onBlur(e);
    }
  }, [onBlur]);

  const handleInputFocus = useCallback(() => {
    // Calculate if dropdown should open upward
    if (containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      const spaceBelow = window.innerHeight - rect.bottom;
      const dropdownHeight = 220; // max-height (200px) + padding
      setOpenUpward(spaceBelow < dropdownHeight && rect.top > dropdownHeight);
    }
    setIsOpen(true);
    setIsTyping(false); // Show all options when re-focusing
  }, []);

  const handleKeyDown = useCallback((e) => {
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
          prev < filteredOptions.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex(prev =>
          prev > 0 ? prev - 1 : filteredOptions.length - 1
        );
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < filteredOptions.length) {
          handleSelectOption(filteredOptions[highlightedIndex]);
        } else if (filteredOptions.length === 1) {
          handleSelectOption(filteredOptions[0]);
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
  }, [isOpen, filteredOptions, highlightedIndex, handleSelectOption]);

  // Scroll highlighted option into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const highlightedEl = listRef.current.children[highlightedIndex];
      if (highlightedEl) {
        highlightedEl.scrollIntoView({ block: 'nearest' });
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

  return (
    <Box
      ref={containerRef}
      className={`settings-combobox ${error ? 'settings-combobox--error' : ''} ${className}`.trim()}
    >
      {label && (
        <label htmlFor={inputId} className="settings-combobox__label">
          {label}
          {required && <span className="settings-combobox__required">*</span>}
        </label>
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
          onBlur={handleInputBlur}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
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
        {isOpen && filteredOptions.length > 0 && (
          <ul
            ref={listRef}
            id={listboxId}
            role="listbox"
            className={`settings-combobox__listbox ${openUpward ? 'settings-combobox__listbox--upward' : ''}`}
            style={{
              backgroundColor: isDarkMode ? '#1c2128' : '#ffffff',
              borderColor: isDarkMode ? '#444c56' : '#d1d5db',
              color: isDarkMode ? '#e6edf3' : '#1f2937',
              boxShadow: isDarkMode
                ? '0 4px 12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1)'
                : '0 4px 12px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.05)',
              ...(openUpward ? { bottom: '100%', top: 'auto', marginBottom: '4px', marginTop: 0 } : {}),
            }}
          >
            {filteredOptions.map((option, index) => (
              <li
                key={option.value}
                id={`${listboxId}-option-${index}`}
                role="option"
                aria-selected={highlightedIndex === index}
                onClick={() => handleSelectOption(option)}
                className={`settings-combobox__option ${highlightedIndex === index ? 'settings-combobox__option--highlighted' : ''} ${option.value === value ? 'settings-combobox__option--selected' : ''}`}
              >
                {option.label}
              </li>
            ))}
          </ul>
        )}
      </Box>
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-combobox__help">
          {helpText}
        </Text>
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
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text displayed above input */
  label: PropTypes.string,
  /** Current value */
  value: PropTypes.string,
  /** Change handler (receives value) */
  onChange: PropTypes.func,
  /** Blur handler */
  onBlur: PropTypes.func,
  /** Array of options { value, label } */
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    })
  ),
  /** Placeholder text */
  placeholder: PropTypes.string,
  /** Help text displayed below input */
  helpText: PropTypes.string,
  /** Error message */
  error: PropTypes.string,
  /** Mark field as required */
  required: PropTypes.bool,
  /** Disable input */
  disabled: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Allow custom values not in options */
  allowCustom: PropTypes.bool,
};

export default SettingsCombobox;
