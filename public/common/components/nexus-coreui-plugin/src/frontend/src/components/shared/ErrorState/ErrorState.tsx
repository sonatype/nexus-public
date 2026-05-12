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
import { Box, Button, Callout, Flex, Text } from '@radix-ui/themes';
import { AlertCircle, RefreshCw } from 'lucide-react';

import './ErrorState.scss';

export interface ErrorStateProps {
  /** Error title (defaults to "Something went wrong") */
  title?: string;
  /** Error message */
  message: string;
  /** Optional retry callback */
  onRetry?: () => void;
  /** Retry button text */
  retryText?: string;
  /** Size variant */
  size?: 'small' | 'medium' | 'large';
  /** Use inline callout style */
  variant?: 'inline' | 'centered';
  /** Custom class name */
  className?: string;
}

/**
 * ErrorState displays an error message with optional retry button.
 *
 * Features:
 * - Clear error messaging
 * - Optional retry functionality
 * - Two display variants: inline callout or centered block
 * - Accessible error indication
 *
 * @example
 * ```tsx
 * <ErrorState
 *   title="Failed to load repositories"
 *   message="Network connection lost. Please check your connection."
 *   onRetry={() => refetch()}
 * />
 *
 * <ErrorState
 *   variant="inline"
 *   message="Invalid form data"
 * />
 * ```
 */
export function ErrorState({
  title = 'Something went wrong',
  message,
  onRetry,
  retryText = 'Try again',
  size = 'medium',
  variant = 'centered',
  className = '',
}: ErrorStateProps): JSX.Element {
  const sizeClass = `error-state--${size}`;

  // Inline variant uses Radix Callout
  if (variant === 'inline') {
    return (
      <Box className={`error-state error-state--inline ${className}`} data-testid="error-state">
        <Callout.Root color="red" role="alert">
          <Callout.Icon>
            <AlertCircle size={16} />
          </Callout.Icon>
          <Callout.Text>
            {title !== 'Something went wrong' && <strong>{title}: </strong>}
            {message}
            {onRetry && (
              <Button
                variant="ghost"
                size="1"
                onClick={onRetry}
                className="error-state__inline-retry"
              >
                <RefreshCw size={14} />
                {retryText}
              </Button>
            )}
          </Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  // Centered variant
  return (
    <Flex
      direction="column"
      align="center"
      justify="center"
      gap="4"
      className={`error-state ${sizeClass} ${className}`}
      data-testid="error-state"
      role="alert"
    >
      <Box className="error-state__icon">
        <AlertCircle size={size === 'large' ? 48 : size === 'small' ? 24 : 32} />
      </Box>

      <Text
        as="h3"
        size={size === 'large' ? '5' : size === 'small' ? '2' : '4'}
        weight="medium"
        className="error-state__title"
      >
        {title}
      </Text>

      <Text
        size={size === 'small' ? '1' : '2'}
        color="gray"
        align="center"
        className="error-state__message"
      >
        {message}
      </Text>

      {onRetry && (
        <Button
          variant="outline"
          size={size === 'small' ? '1' : '2'}
          onClick={onRetry}
          className="error-state__retry"
        >
          <RefreshCw size={14} />
          {retryText}
        </Button>
      )}
    </Flex>
  );
}

export default ErrorState;


