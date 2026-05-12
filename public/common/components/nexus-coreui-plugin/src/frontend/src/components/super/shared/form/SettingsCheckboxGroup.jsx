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

import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Box, Text } from '@radix-ui/themes';
import { AlertCircle, Check } from 'lucide-react';

import './SettingsCheckboxGroup.scss';

/**
 * SettingsCheckboxGroup - Multiple checkbox selection with label and error handling
 * 
 * Converts an array of selected values to/from a comma-separated string for compatibility
 * with existing form data structures.
 * 
 * @example
 * <SettingsCheckboxGroup
 *   name="actions"
 *   label="Actions"
 *   value="read,update,delete"
 *   onChange={(value) => handleChange('actions', value)}
 *   options={[
 *     { value: 'browse', label: 'Browse' },
 *     { value: 'read', label: 'Read' },
 *     { value: 'edit', label: 'Edit' },
 *     { value: 'delete', label: 'Delete' },
 *   ]}
 *   helpText="Select the actions allowed for this privilege"
 *   required
 * />
 */
export function SettingsCheckboxGroup({
  name,
  label,
  value = '',
  onChange,
  options = [],
  helpText = '',
  error = '',
  required = false,
  disabled = false,
  className = '',
  layout = 'horizontal',
}) {
  const groupId = `settings-checkbox-group-${name}`;
  const helpId = `settings-checkbox-group-help-${name}`;
  const errorId = `settings-checkbox-group-error-${name}`;

  // Parse comma-separated string into array
  const selectedValues = value ? value.split(',').map(v => v.trim()).filter(Boolean) : [];

  const handleCheckboxChange = useCallback((optionValue, checked) => {
    if (!onChange) return;

    let newValues;
    if (checked) {
      // Add to selection
      newValues = [...selectedValues, optionValue];
    } else {
      // Remove from selection
      newValues = selectedValues.filter(v => v !== optionValue);
    }

    // Convert back to comma-separated string
    onChange(newValues.join(','));
  }, [onChange, selectedValues]);

  const isChecked = (optionValue) => selectedValues.includes(optionValue);

  return (
    <Box 
      className={`settings-checkbox-group ${error ? 'settings-checkbox-group--error' : ''} ${className}`.trim()}
      role="group"
      aria-labelledby={label ? `${groupId}-label` : undefined}
    >
      {label && (
        <Text as="label" id={`${groupId}-label`} className="settings-checkbox-group__label">
          {label}
          {required && <span className="settings-checkbox-group__required">*</span>}
        </Text>
      )}
      <Box 
        className={`settings-checkbox-group__options settings-checkbox-group__options--${layout}`}
        aria-describedby={`${helpText ? helpId : ''} ${error ? errorId : ''}`.trim() || undefined}
      >
        {options.map((option) => {
          const inputId = `${groupId}-${option.value}`;
          const checked = isChecked(option.value);
          const optionDisabled = disabled || option.disabled;

          return (
            <label 
              key={option.value} 
              htmlFor={inputId}
              className={`settings-checkbox-group__option ${optionDisabled ? 'settings-checkbox-group__option--disabled' : ''}`}
            >
              <span className={`settings-checkbox-group__box ${checked ? 'settings-checkbox-group__box--checked' : ''}`}>
                <input
                  id={inputId}
                  type="checkbox"
                  name={`${name}[]`}
                  value={option.value}
                  checked={checked}
                  onChange={(e) => handleCheckboxChange(option.value, e.target.checked)}
                  disabled={optionDisabled}
                  data-testid={`checkbox-${name}-${option.value}`}
                  className="settings-checkbox-group__input"
                />
                {checked && <Check size={14} className="settings-checkbox-group__icon" />}
              </span>
              <span className="settings-checkbox-group__option-label">{option.label}</span>
            </label>
          );
        })}
      </Box>
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-checkbox-group__help">
          {helpText}
        </Text>
      )}
      {error && (
        <Text as="p" size="1" id={errorId} className="settings-checkbox-group__error-text">
          <AlertCircle size={14} />
          {error}
        </Text>
      )}
    </Box>
  );
}

SettingsCheckboxGroup.propTypes = {
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text displayed above checkboxes */
  label: PropTypes.string,
  /** Current value as comma-separated string (e.g., "read,update,delete") */
  value: PropTypes.string,
  /** Change handler (receives comma-separated string) */
  onChange: PropTypes.func,
  /** Array of options { value, label, disabled? } */
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      disabled: PropTypes.bool,
    })
  ).isRequired,
  /** Help text displayed below checkboxes */
  helpText: PropTypes.string,
  /** Error message */
  error: PropTypes.string,
  /** Mark field as required */
  required: PropTypes.bool,
  /** Disable all checkboxes */
  disabled: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Layout direction: 'horizontal' or 'vertical' */
  layout: PropTypes.oneOf(['horizontal', 'vertical']),
};

export default SettingsCheckboxGroup;
