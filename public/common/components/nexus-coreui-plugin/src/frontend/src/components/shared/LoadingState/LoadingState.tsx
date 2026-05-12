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
import { Flex, Spinner, Text } from '@radix-ui/themes';

import './LoadingState.scss';

export interface LoadingStateProps {
  /** Optional message to display */
  message?: string;
  /** Size variant */
  size?: 'small' | 'medium' | 'large';
  /** Whether to display inline (no centering) */
  inline?: boolean;
  /** Custom class name */
  className?: string;
}

/**
 * LoadingState displays a spinner with optional message.
 *
 * Features:
 * - Three size variants
 * - Optional descriptive message
 * - Inline or centered display
 * - Accessible loading indication
 *
 * @example
 * ```tsx
 * <LoadingState message="Loading repositories..." />
 * <LoadingState size="small" inline />
 * <LoadingState size="large" message="Please wait..." />
 * ```
 */
export function LoadingState({
  message,
  size = 'medium',
  inline = false,
  className = '',
}: LoadingStateProps): JSX.Element {
  const spinnerSize = size === 'large' ? '3' : size === 'small' ? '1' : '2';
  const textSize = size === 'large' ? '3' : size === 'small' ? '1' : '2';
  const sizeClass = `loading-state--${size}`;
  const inlineClass = inline ? 'loading-state--inline' : '';

  return (
    <Flex
      direction="column"
      align="center"
      justify="center"
      gap="3"
      className={`loading-state ${sizeClass} ${inlineClass} ${className}`}
      data-testid="loading-state"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <Spinner size={spinnerSize} />
      {message && (
        <Text size={textSize} color="gray" className="loading-state__message">
          {message}
        </Text>
      )}
      {/* Screen reader text */}
      <span className="sr-only">Loading{message ? `: ${message}` : ''}</span>
    </Flex>
  );
}

export default LoadingState;


