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
import { Box, Flex, Text } from '@radix-ui/themes';
import { CheckCircle, XCircle, Loader2, Link2 } from 'lucide-react';

export type ConnectionStatus = 'idle' | 'testing' | 'connected' | 'failed';

export interface ConnectionIndicatorProps {
  status: ConnectionStatus;
  message?: string;
  className?: string;
}

/**
 * ConnectionIndicator - Shows IQ Server connection status at the top of the page
 *
 * States:
 * - idle: No indicator shown (settings not configured)
 * - testing: Shows "Testing connection..." with spinner
 * - connected: Shows "Connected to IQ Server" with checkmark and optional version
 * - failed: Shows "Connection failed: [message]" with X icon
 */
export function ConnectionIndicator({ status, message, className }: ConnectionIndicatorProps) {
  if (status === 'idle') {
    return null;
  }

  const getStatusContent = () => {
    switch (status) {
      case 'testing':
        return (
          <Flex align="center" gap="2" className="connection-indicator__content connection-indicator__content--testing">
            <Loader2 size={16} className="connection-indicator__spinner" aria-hidden="true" />
            <Text size="2">Testing connection...</Text>
          </Flex>
        );
      case 'connected':
        return (
          <Flex align="center" gap="2" className="connection-indicator__content connection-indicator__content--connected">
            <CheckCircle size={16} aria-hidden="true" />
            <Text size="2">{message || 'Connected to IQ Server'}</Text>
          </Flex>
        );
      case 'failed':
        return (
          <Flex align="center" gap="2" className="connection-indicator__content connection-indicator__content--failed">
            <XCircle size={16} aria-hidden="true" />
            <Text size="2">{message || 'Connection failed'}</Text>
          </Flex>
        );
      default:
        return null;
    }
  };

  return (
    <Box
      className={`connection-indicator connection-indicator--${status} ${className || ''}`.trim()}
      data-testid="connection-indicator"
      data-status={status}
      role="status"
      aria-live="polite"
    >
      <Flex align="center" gap="2">
        <Link2 size={14} className="connection-indicator__icon" aria-hidden="true" />
        {getStatusContent()}
      </Flex>
    </Box>
  );
}

export default ConnectionIndicator;
