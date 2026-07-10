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
import { Check } from 'lucide-react';

import './SettingsCheckbox.scss';

/**
 * SettingsCheckbox - Checkbox with label and optional description
 * 
 * @example
 * <SettingsCheckbox
 *   name="enabled"
 *   label="Enable email server"
 *   checked={enabled}
 *   onChange={setEnabled}
 *   description="Allow the system to send email notifications"
 * />
 */
export function SettingsCheckbox({
  name,
  label,
  checked = false,
  onChange,
  description = '',
  disabled = false,
  className = '',
  analyticsId,
}) {
  const inputId = `settings-checkbox-${name}`;
  const descriptionId = `settings-checkbox-desc-${name}`;

  const handleChange = (e) => {
    if (onChange) {
      onChange(e.target.checked, e);
    }
  };

  return (
    <Box className={`settings-checkbox ${disabled ? 'settings-checkbox--disabled' : ''} ${className}`.trim()}>
      <label htmlFor={inputId} className="settings-checkbox__container">
        <span className={`settings-checkbox__box ${checked ? 'settings-checkbox__box--checked' : ''}`}>
          <input
            id={inputId}
            name={name}
            type="checkbox"
            checked={checked}
            onChange={handleChange}
            disabled={disabled}
            aria-describedby={description ? descriptionId : undefined}
            data-testid={`checkbox-${name}`}
            {...(analyticsId ? { 'data-analytics-id': analyticsId } : {})}
            className="settings-checkbox__input"
          />
          {checked && <Check size={14} className="settings-checkbox__icon" />}
        </span>
        <span className="settings-checkbox__content">
          <span className="settings-checkbox__label">{label}</span>
          {description && (
            <Text as="span" size="1" id={descriptionId} className="settings-checkbox__description">
              {description}
            </Text>
          )}
        </span>
      </label>
    </Box>
  );
}

SettingsCheckbox.propTypes = {
  /** Field name for form submission */
  name: PropTypes.string.isRequired,
  /** Label text */
  label: PropTypes.string.isRequired,
  /** Checked state */
  checked: PropTypes.bool,
  /** Change handler (receives checked state and event) */
  onChange: PropTypes.func,
  /** Description text below label */
  description: PropTypes.string,
  /** Disable checkbox */
  disabled: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Analytics ID for the checkbox input */
  analyticsId: PropTypes.string,
};

export default SettingsCheckbox;

