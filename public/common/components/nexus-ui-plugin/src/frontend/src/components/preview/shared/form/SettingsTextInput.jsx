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
import { Box, Text } from '@radix-ui/themes';
import { AlertCircle } from 'lucide-react';

import './SettingsTextInput.scss';

/**
 * SettingsTextInput - Single-line text input with label, help text, and validation
 * 
 * @example
 * <SettingsTextInput
 *   name="hostname"
 *   label="SMTP Hostname"
 *   value={hostname}
 *   onChange={setHostname}
 *   placeholder="smtp.example.com"
 *   helpText="The hostname of your SMTP server"
 *   required
 * />
 */
export function SettingsTextInput({
  name,
  label,
  value,
  onChange,
  placeholder = '',
  helpText = '',
  error = '',
  required = false,
  disabled = false,
  readOnly = false,
  type = 'text',
  maxLength,
  min,
  max,
  step,
  autoComplete,
  className = '',
  inputRef,
  onBlur,
  onFocus,
  onKeyDown,
}) {
  const inputId = `settings-input-${name}`;
  const helpId = `settings-help-${name}`;
  const errorId = `settings-error-${name}`;

  const handleChange = (e) => {
    if (onChange) {
      onChange(e.target.value, e);
    }
  };

  return (
    <Box className={`settings-text-input ${error ? 'settings-text-input--error' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={inputId} className="settings-text-input__label">
          {label}
          {required && <span className="settings-text-input__required">*</span>}
        </label>
      )}
      <input
        ref={inputRef}
        id={inputId}
        name={name}
        type={type}
        value={value}
        onChange={handleChange}
        onBlur={onBlur}
        onFocus={onFocus}
        onKeyDown={onKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        required={required}
        maxLength={maxLength}
        min={min}
        max={max}
        step={step}
        autoComplete={autoComplete}
        aria-describedby={`${helpText ? helpId : ''} ${error ? errorId : ''}`.trim() || undefined}
        aria-invalid={!!error}
        data-testid={`input-${name}`}
        className="settings-text-input__input"
      />
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-text-input__help">
          {helpText}
        </Text>
      )}
      {error && (
        <Text as="p" size="1" id={errorId} className="settings-text-input__error">
          <AlertCircle size={14} />
          {error}
        </Text>
      )}
    </Box>
  );
}

SettingsTextInput.propTypes = {
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text displayed above input */
  label: PropTypes.string,
  /** Current input value */
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  /** Change handler (receives value and event) */
  onChange: PropTypes.func,
  /** Placeholder text */
  placeholder: PropTypes.string,
  /** Help text displayed below input */
  helpText: PropTypes.string,
  /** Error message (replaces help text when present) */
  error: PropTypes.string,
  /** Mark field as required */
  required: PropTypes.bool,
  /** Disable input */
  disabled: PropTypes.bool,
  /** Make input read-only */
  readOnly: PropTypes.bool,
  /** Input type (text, email, number, url, tel) */
  type: PropTypes.oneOf(['text', 'email', 'number', 'url', 'tel']),
  /** Maximum character length */
  maxLength: PropTypes.number,
  /** Minimum value (for number type) */
  min: PropTypes.number,
  /** Maximum value (for number type) */
  max: PropTypes.number,
  /** Step value (for number type) */
  step: PropTypes.number,
  /** Autocomplete attribute */
  autoComplete: PropTypes.string,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Ref for input element */
  inputRef: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
  ]),
  /** Blur handler */
  onBlur: PropTypes.func,
  /** Focus handler */
  onFocus: PropTypes.func,
  /** Key down handler */
  onKeyDown: PropTypes.func,
};

export default SettingsTextInput;

