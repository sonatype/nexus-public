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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Box, IconButton, Text, TextField } from '@radix-ui/themes';
import { Eye, EyeOff, AlertCircle } from 'lucide-react';

import './SettingsPasswordInput.scss';

/**
 * SettingsPasswordInput - Password input with visibility toggle
 *
 * @example
 * <SettingsPasswordInput
 *   name="password"
 *   label="SMTP Password"
 *   value={password}
 *   onChange={setPassword}
 *   helpText="Password for SMTP authentication"
 * />
 */
export function SettingsPasswordInput({
  name,
  label,
  value,
  onChange,
  placeholder = '',
  helpText = '',
  error = '',
  required = false,
  disabled = false,
  autoComplete = 'new-password',
  className = '',
  inputRef,
  onBlur,
  onFocus,
  showToggle = true,
}) {
  const [showPassword, setShowPassword] = useState(false);

  const inputId = `settings-input-${name}`;
  const helpId = `settings-help-${name}`;
  const errorId = `settings-error-${name}`;

  const handleChange = (e) => {
    if (onChange) {
      onChange(e.target.value, e);
    }
  };

  const toggleVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <Box className={`settings-password-input ${error ? 'settings-password-input--error' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={inputId} className="settings-password-input__label">
          {label}
          {required && <span className="settings-password-input__required">*</span>}
        </label>
      )}
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-password-input__help">
          {helpText}
        </Text>
      )}
      <TextField.Root
        ref={inputRef}
        id={inputId}
        name={name}
        type={showPassword ? 'text' : 'password'}
        value={value}
        onChange={handleChange}
        onBlur={onBlur}
        onFocus={onFocus}
        placeholder={placeholder}
        disabled={disabled}
        required={required}
        autoComplete={autoComplete}
        aria-describedby={`${helpText ? helpId : ''} ${error ? errorId : ''}`.trim() || undefined}
        aria-invalid={!!error}
        data-testid={`password-${name}`}
        className="settings-password-input__input"
        size="2"
      >
        {showToggle && value && (
          <TextField.Slot side="right">
            <IconButton
              type="button"
              variant="ghost"
              color="gray"
              size="1"
              className="settings-password-input__toggle"
              onClick={toggleVisibility}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              data-testid={`password-toggle-${name}`}
              tabIndex={-1}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </IconButton>
          </TextField.Slot>
        )}
      </TextField.Root>
      {error && (
        <Text as="p" size="1" id={errorId} className="settings-password-input__error">
          <AlertCircle size={14} />
          {error}
        </Text>
      )}
    </Box>
  );
}

SettingsPasswordInput.propTypes = {
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text displayed above input */
  label: PropTypes.string,
  /** Current input value */
  value: PropTypes.string,
  /** Change handler (receives value and event) */
  onChange: PropTypes.func,
  /** Placeholder text */
  placeholder: PropTypes.string,
  /** Help text displayed between label and input */
  helpText: PropTypes.string,
  /** Error message displayed below input (replaces help text when present) */
  error: PropTypes.string,
  /** Mark field as required */
  required: PropTypes.bool,
  /** Disable input */
  disabled: PropTypes.bool,
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
  /** Show/hide visibility toggle button */
  showToggle: PropTypes.bool,
};

export default SettingsPasswordInput;
