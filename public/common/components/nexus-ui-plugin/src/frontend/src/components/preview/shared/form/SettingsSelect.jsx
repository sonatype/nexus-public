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

import React from 'react';
import PropTypes from 'prop-types';
import { Box, Text, Select } from '@radix-ui/themes';
import { AlertCircle } from 'lucide-react';

import './SettingsSelect.scss';

// Special value to represent "None" selection since Radix doesn't allow empty strings
const NONE_VALUE = '__NONE__';

/**
 * SettingsSelect - Dropdown select using Radix UI for consistent theming
 * 
 * @example
 * <SettingsSelect
 *   name="ssl"
 *   label="SSL/TLS Options"
 *   value={sslOption}
 *   onChange={setSslOption}
 *   options={[
 *     { value: 'none', label: 'None' },
 *     { value: 'starttls', label: 'STARTTLS' },
 *     { value: 'ssl', label: 'SSL/TLS' },
 *   ]}
 * />
 */
export function SettingsSelect({
  name,
  label,
  value,
  onChange,
  options = [],
  placeholder = 'Select an option',
  helpText = '',
  error = '',
  required = false,
  disabled = false,
  className = '',
}) {
  const selectId = `settings-select-${name}`;
  const helpId = `settings-help-${name}`;
  const errorId = `settings-error-${name}`;

  // Convert empty string or undefined value to our special NONE_VALUE for Radix compatibility
  const normalizedValue = value === '' || value === undefined ? NONE_VALUE : value;

  // Convert options with empty string values to use NONE_VALUE
  const normalizedOptions = options.map((option) => ({
    ...option,
    value: option.value === '' ? NONE_VALUE : option.value,
  }));

  const handleChange = (newValue) => {
    if (onChange) {
      // Convert NONE_VALUE back to empty string for the parent component
      onChange(newValue === NONE_VALUE ? '' : newValue);
    }
  };

  return (
    <Box className={`settings-select ${error ? 'settings-select--error' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={selectId} className="settings-select__label">
          {label}
          {required && <span className="settings-select__required" aria-hidden="true">*</span>}
        </label>
      )}
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-select__help">
          {helpText}
        </Text>
      )}
      <Box className="settings-select__wrapper">
        <Select.Root
          value={normalizedValue}
          onValueChange={handleChange}
          disabled={disabled}
          name={name}
        >
          <Select.Trigger
            id={selectId}
            className="settings-select__trigger"
            placeholder={placeholder}
            aria-describedby={`${helpText ? helpId : ''} ${error ? errorId : ''}`.trim() || undefined}
            aria-invalid={!!error}
            data-testid={`select-${name}`}
          />
          <Select.Content className="settings-select__content" position="popper" sideOffset={4}>
            {normalizedOptions.map((option) => (
              <Select.Item
                key={option.value}
                value={option.value}
                disabled={option.disabled}
                className="settings-select__item"
              >
                {option.label}
              </Select.Item>
            ))}
          </Select.Content>
        </Select.Root>
      </Box>
      {error && (
        <Text as="p" size="1" id={errorId} className="settings-select__error-text">
          <AlertCircle size={14} aria-hidden="true" />
          {error}
        </Text>
      )}
    </Box>
  );
}

SettingsSelect.propTypes = {
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text displayed above select */
  label: PropTypes.string,
  /** Current selected value */
  value: PropTypes.string,
  /** Change handler (receives value) */
  onChange: PropTypes.func,
  /** Array of options { value, label, disabled? } */
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      disabled: PropTypes.bool,
    })
  ),
  /** Placeholder text for empty selection */
  placeholder: PropTypes.string,
  /** Help text displayed between label and select */
  helpText: PropTypes.string,
  /** Error message displayed below select */
  error: PropTypes.string,
  /** Mark field as required */
  required: PropTypes.bool,
  /** Disable select */
  disabled: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
};

export default SettingsSelect;
