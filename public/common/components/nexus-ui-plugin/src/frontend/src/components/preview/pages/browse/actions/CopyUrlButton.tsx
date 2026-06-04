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

import React, { useState, useCallback, useRef, useEffect } from 'react';
import { IconButton, Tooltip } from '@radix-ui/themes';
import { Copy, Check } from 'lucide-react';
import { ExtJS } from '../../../../../interface/ExtJS';
import { useToast } from '../../../shared';

import { ACTION_STRINGS, type CopyUrlButtonProps } from './actions.types';

import './CopyUrlButton.scss';

/**
 * Duration in milliseconds to show the success checkmark.
 */
const SUCCESS_FEEDBACK_DURATION = 2000;

/**
 * Map button size prop to icon size in pixels.
 */
const ICON_SIZES: Record<string, number> = {
  small: 14,
  medium: 16,
  large: 18,
};

/**
 * CopyUrlButton provides a one-click copy-to-clipboard action with visual feedback.
 *
 * Features:
 * - Copies URL to clipboard on click
 * - Shows success checkmark icon briefly after copy
 * - Displays ExtJS toast notification on success/error
 * - Accessible with keyboard support
 * - Tooltip showing action description
 *
 * @example
 * ```tsx
 * <CopyUrlButton
 *   url="https://example.com/repository/artifact.jar"
 *   tooltipText="Copy download URL"
 *   successMessage="Download URL copied!"
 * />
 * ```
 */
export function CopyUrlButton({
  url,
  tooltipText = ACTION_STRINGS.copyUrl.tooltipText,
  successMessage = ACTION_STRINGS.copyUrl.successMessage,
  size = 'medium',
  className = '',
  disabled = false,
}: CopyUrlButtonProps): JSX.Element {
  const [copied, setCopied] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const toast = useToast();

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  const handleCopy = useCallback(
    async (event: React.MouseEvent) => {
      // Prevent event from propagating to parent clickable elements (e.g., table rows)
      event.stopPropagation();

      if (disabled || !url) {
        return;
      }

      try {
        await navigator.clipboard.writeText(url);
        setCopied(true);
        toast.success(successMessage);

        // Clear any existing timeout
        if (timeoutRef.current) {
          clearTimeout(timeoutRef.current);
        }

        // Reset the copied state after feedback duration
        timeoutRef.current = setTimeout(() => {
          setCopied(false);
        }, SUCCESS_FEEDBACK_DURATION);
      } catch (error) {
        // Fallback for browsers that don't support clipboard API
        console.error('Failed to copy URL:', error);
        toast.error(ACTION_STRINGS.copyUrl.errorMessage);
      }
    },
    [url, disabled, successMessage]
  );

  const iconSize = ICON_SIZES[size] || ICON_SIZES.medium;
  const buttonClasses = [
    'nxrm-copy-url-button',
    copied && 'nxrm-copy-url-button--copied',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <Tooltip content={copied ? 'Copied!' : tooltipText}>
      <IconButton
        size="1"
        variant="ghost"
        color={copied ? 'green' : 'gray'}
        className={buttonClasses}
        onClick={handleCopy}
        disabled={disabled}
        aria-label={tooltipText}
      >
        {copied ? (
          <Check size={iconSize} className="nxrm-copy-url-button__icon nxrm-copy-url-button__icon--success" />
        ) : (
          <Copy size={iconSize} className="nxrm-copy-url-button__icon" />
        )}
      </IconButton>
    </Tooltip>
  );
}

export default CopyUrlButton;

