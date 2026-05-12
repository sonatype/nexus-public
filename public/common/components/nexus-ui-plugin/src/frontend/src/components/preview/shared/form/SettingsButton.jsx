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
import { Loader2 } from 'lucide-react';

import './SettingsButton.scss';

/**
 * SettingsButton - Consistent button styling for settings forms
 * 
 * @example
 * <SettingsButton variant="primary" onClick={handleSave} loading={isSaving}>
 *   Save
 * </SettingsButton>
 * 
 * <SettingsButton variant="secondary" onClick={handleCancel}>
 *   Cancel
 * </SettingsButton>
 * 
 * <SettingsButton variant="danger" onClick={handleDelete}>
 *   Delete
 * </SettingsButton>
 */
export function SettingsButton({
  children,
  variant = 'secondary',
  type = 'button',
  onClick,
  disabled = false,
  loading = false,
  icon: Icon,
  size = 'medium',
  className = '',
  testId,
  ...rest
}) {
  const isDisabled = disabled || loading;
  
  // Auto-generate testId from type if not provided
  const dataTestId = testId || (type === 'submit' ? 'button-submit' : undefined);

  // Filter out React elements from children (they should be passed as icon prop instead)
  // This prevents errors when icons are accidentally passed as children
  const textChildren = React.Children.toArray(children).filter(child => {
    // Keep strings, numbers, and text nodes
    if (typeof child === 'string' || typeof child === 'number') {
      return true;
    }
    // Filter out React elements (they should use icon prop)
    if (React.isValidElement(child)) {
      console.warn('SettingsButton: Icon elements should be passed via the "icon" prop, not as children. Ignoring:', child);
      return false;
    }
    return true;
  });

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      className={`settings-button settings-button--${variant} settings-button--${size} ${className}`.trim()}
      data-testid={dataTestId}
      aria-busy={loading}
      {...rest}
    >
      {loading ? (
        <Loader2 size={16} className="settings-button__spinner" />
      ) : Icon ? (
        <Icon size={16} className="settings-button__icon" />
      ) : null}
      {textChildren.length > 0 && (
        <span className="settings-button__text">{textChildren}</span>
      )}
    </button>
  );
}

SettingsButton.propTypes = {
  /** Button label (text only - icons should use icon prop) */
  children: PropTypes.node,
  /** Button style variant */
  variant: PropTypes.oneOf(['primary', 'secondary', 'danger', 'ghost']),
  /** HTML button type */
  type: PropTypes.oneOf(['button', 'submit', 'reset']),
  /** Click handler */
  onClick: PropTypes.func,
  /** Disable button */
  disabled: PropTypes.bool,
  /** Show loading spinner */
  loading: PropTypes.bool,
  /** Optional icon component (Lucide icon) */
  icon: PropTypes.elementType,
  /** Button size */
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  /** Additional CSS class */
  className: PropTypes.string,
  /** Test ID for E2E testing (auto-generated as 'button-submit' for submit buttons) */
  testId: PropTypes.string,
};

export default SettingsButton;

