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

import './SettingsTextArea.scss';

/**
 * SettingsTextArea - Multi-line text input
 * 
 * @example
 * <SettingsTextArea
 *   name="nonProxyHosts"
 *   label="Non-Proxy Hosts"
 *   value={nonProxyHosts}
 *   onChange={setNonProxyHosts}
 *   rows={5}
 *   helpText="One host pattern per line. Use * as wildcard."
 * />
 */
export function SettingsTextArea({
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
  rows = 4,
  maxLength,
  className = '',
  inputRef,
  onBlur,
  onFocus,
  monospace = false,
}) {
  const textareaId = `settings-textarea-${name}`;
  const helpId = `settings-help-${name}`;
  const errorId = `settings-error-${name}`;

  const handleChange = (e) => {
    if (onChange) {
      onChange(e.target.value, e);
    }
  };

  return (
    <Box className={`settings-textarea ${error ? 'settings-textarea--error' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={textareaId} className="settings-textarea__label">
          {label}
          {required && <span className="settings-textarea__required">*</span>}
        </label>
      )}
      {helpText && !error && (
        <Text as="p" size="1" id={helpId} className="settings-textarea__help">
          {helpText}
        </Text>
      )}
      <textarea
        ref={inputRef}
        id={textareaId}
        name={name}
        value={value}
        onChange={handleChange}
        onBlur={onBlur}
        onFocus={onFocus}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        required={required}
        rows={rows}
        maxLength={maxLength}
        aria-describedby={`${helpText ? helpId : ''} ${error ? errorId : ''}`.trim() || undefined}
        aria-invalid={!!error}
        data-testid={`textarea-${name}`}
        className={`settings-textarea__input ${monospace ? 'settings-textarea__input--mono' : ''}`}
      />
      {error && (
        <Text as="p" size="1" id={errorId} className="settings-textarea__error">
          <AlertCircle size={14} />
          {error}
        </Text>
      )}
    </Box>
  );
}

SettingsTextArea.propTypes = {
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text displayed above textarea */
  label: PropTypes.string,
  /** Current input value */
  value: PropTypes.string,
  /** Change handler (receives value and event) */
  onChange: PropTypes.func,
  /** Placeholder text */
  placeholder: PropTypes.string,
  /** Help text displayed between label and textarea */
  helpText: PropTypes.string,
  /** Error message displayed below textarea */
  error: PropTypes.string,
  /** Mark field as required */
  required: PropTypes.bool,
  /** Disable textarea */
  disabled: PropTypes.bool,
  /** Make textarea read-only */
  readOnly: PropTypes.bool,
  /** Number of visible text lines */
  rows: PropTypes.number,
  /** Maximum character length */
  maxLength: PropTypes.number,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Ref for textarea element */
  inputRef: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
  ]),
  /** Blur handler */
  onBlur: PropTypes.func,
  /** Focus handler */
  onFocus: PropTypes.func,
  /** Use monospace font */
  monospace: PropTypes.bool,
};

export default SettingsTextArea;

