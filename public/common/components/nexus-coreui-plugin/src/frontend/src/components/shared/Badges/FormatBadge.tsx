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
import { Flex, Text } from '@radix-ui/themes';
import { FormatIcon } from '../../super/settings/repository/repositories/components/FormatIcon';
import { FORMAT_LABELS } from '../../super/settings/repository/repositories/types';

import './Badges.scss';

export interface FormatBadgeProps {
  /** Repository format (e.g., 'maven2', 'npm', 'docker') */
  format: string;
  /** Visual variant */
  variant?: 'inline' | 'tile';
  /** Icon size */
  size?: number;
  /** Whether to show the icon */
  showIcon?: boolean;
  /** Whether to show the text label */
  showLabel?: boolean;
  /** Label text color (e.g. 'blue' for link-style) */
  labelColor?: 'gray' | 'blue';
  /** Custom class name */
  className?: string;
}

/**
 * FormatBadge - A standardized badge for displaying technology formats (Maven, npm, etc.)
 * using official brand logos.
 */
export function FormatBadge({
  format,
  variant = 'inline',
  size = 16,
  showIcon = true,
  showLabel = true,
  labelColor,
  className = '',
}: FormatBadgeProps): JSX.Element {
  const displayLabel = FORMAT_LABELS[format] || format;

  if (variant === 'tile') {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="2"
        className={`format-badge format-badge--tile ${className}`}
      >
        {showIcon && <FormatIcon format={format} size={size} />}
        {showLabel && (
          <Text size="1" weight="medium" color={labelColor} className="format-badge__label">
            {displayLabel}
          </Text>
        )}
      </Flex>
    );
  }

  return (
    <Flex
      align="center"
      gap="2"
      display="inline-flex"
      className={`format-badge format-badge--inline ${className}`}
    >
      {showIcon && <FormatIcon format={format} size={size} />}
      {showLabel && (
        <Text size="2" weight="medium" color={labelColor} className="format-badge__label">
          {displayLabel}
        </Text>
      )}
    </Flex>
  );
}

export default FormatBadge;
