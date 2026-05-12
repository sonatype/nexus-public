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
import { Button } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';

import './SettingsButton.scss';

/** Map SettingsButton variant → Radix Button variant */
const RADIX_VARIANT = {
  primary: 'solid',
  secondary: 'outline',
  danger: 'solid',
  ghost: 'ghost',
};

/** Map SettingsButton variant → Radix color (undefined = accent default) */
const RADIX_COLOR = {
  primary: undefined,
  secondary: undefined,
  danger: 'red',
  ghost: undefined,
};

/** Map SettingsButton size → Radix size token */
const RADIX_SIZE = {
  small: '1',
  medium: '2',
  large: '3',
};

/**
 * SettingsButton - Consistent button styling for settings forms.
 * Implemented with Radix UI Button for design-system consistency.
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
  const dataTestId = testId || (type === 'submit' ? 'button-submit' : undefined);

  return (
    <Button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      variant={RADIX_VARIANT[variant] || 'outline'}
      color={RADIX_COLOR[variant]}
      size={RADIX_SIZE[size] || '2'}
      className={`settings-button settings-button--${variant} ${className}`.trim()}
      data-testid={dataTestId}
      data-variant={variant}
      data-size={size}
      aria-busy={loading}
      {...rest}
    >
      {loading ? (
        <Loader2 size={16} data-testid="settings-button-spinner" />
      ) : Icon ? (
        <Icon size={16} data-testid="settings-button-icon" />
      ) : null}
      {children}
    </Button>
  );
}

SettingsButton.propTypes = {
  /** Button label */
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

