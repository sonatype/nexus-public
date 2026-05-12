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

import './SettingsToggle.scss';

/**
 * SettingsToggle - Switch toggle for boolean settings
 * 
 * @example
 * <SettingsToggle
 *   name="anonymousAccess"
 *   label="Allow anonymous access"
 *   checked={anonymousAccess}
 *   onChange={setAnonymousAccess}
 *   description="Allow users to browse repositories without logging in"
 * />
 */
export function SettingsToggle({
  name,
  label,
  checked = false,
  onChange,
  description = '',
  disabled = false,
  className = '',
}) {
  const toggleId = `settings-toggle-${name}`;
  const descriptionId = `settings-toggle-desc-${name}`;

  const handleChange = () => {
    if (!disabled && onChange) {
      onChange(!checked);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleChange();
    }
  };

  return (
    <Box className={`settings-toggle ${disabled ? 'settings-toggle--disabled' : ''} ${className}`.trim()}>
      <div className="settings-toggle__container">
        <button
          id={toggleId}
          type="button"
          role="switch"
          aria-checked={checked}
          aria-describedby={description ? descriptionId : undefined}
          disabled={disabled}
          onClick={handleChange}
          onKeyDown={handleKeyDown}
          data-testid={`toggle-${name}`}
          className={`settings-toggle__switch ${checked ? 'settings-toggle__switch--checked' : ''}`}
        >
          <span className="settings-toggle__thumb" />
        </button>
        <label htmlFor={toggleId} className="settings-toggle__content" onClick={handleChange}>
          <span className="settings-toggle__label">{label}</span>
          {description && (
            <Text as="span" size="1" id={descriptionId} className="settings-toggle__description">
              {description}
            </Text>
          )}
        </label>
      </div>
    </Box>
  );
}

SettingsToggle.propTypes = {
  /** Field name */
  name: PropTypes.string.isRequired,
  /** Label text */
  label: PropTypes.string.isRequired,
  /** Toggle state */
  checked: PropTypes.bool,
  /** Change handler (receives new state) */
  onChange: PropTypes.func,
  /** Description text below label */
  description: PropTypes.string,
  /** Disable toggle */
  disabled: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
};

export default SettingsToggle;

