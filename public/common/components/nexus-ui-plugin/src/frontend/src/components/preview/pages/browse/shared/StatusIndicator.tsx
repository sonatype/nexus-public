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
import { Flex, Text, Tooltip } from '@radix-ui/themes';
import { Circle } from 'lucide-react';
import type { StatusIndicatorProps } from '../browse.types';

/**
 * Renders an online/offline status indicator.
 *
 * @example
 * <StatusIndicator status={{ online: true }} />
 * <StatusIndicator status={{ online: false, description: "Connection failed" }} showLabel />
 */
export function StatusIndicator({ status, showLabel = false }: StatusIndicatorProps): JSX.Element {
  const isOnline = status?.online ?? false;
  const color = isOnline ? 'var(--green-9)' : 'var(--red-9)';
  const label = isOnline ? 'Online' : 'Offline';
  const tooltipContent = status?.description || label;

  const indicator = (
    <Flex align="center" gap="1" data-testid="status-indicator">
      <span data-testid="status-indicator-circle" style={{ display: 'inline-flex' }}>
        <Circle size={8} fill={color} color={color} aria-hidden="true" />
      </span>
      {showLabel && (
        <Text size="1" color="gray">
          {label}
        </Text>
      )}
    </Flex>
  );

  // Wrap in tooltip if there's a description
  if (status?.description) {
    return (
      <Tooltip content={tooltipContent}>
        {indicator}
      </Tooltip>
    );
  }

  return indicator;
}

export default StatusIndicator;
