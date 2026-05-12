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
import { Box, Button, Flex, Text } from '@radix-ui/themes';
import { ExternalLink, LucideIcon } from 'lucide-react';

import './EmptyState.scss';

export interface EmptyStateAction {
  /** Button label */
  label: string;
  /** Click handler */
  onClick: () => void;
  /** Optional icon component */
  icon?: LucideIcon;
}

export interface EmptyStateSecondaryAction {
  /** Link label */
  label: string;
  /** Link URL */
  href: string;
}

export interface EmptyStateProps {
  /** Icon component to display */
  icon: LucideIcon;
  /** Title text */
  title: string;
  /** Description text */
  description: string;
  /** Primary action button */
  action?: EmptyStateAction;
  /** Secondary action link */
  secondaryAction?: EmptyStateSecondaryAction;
  /** Optional tip text */
  tip?: string;
  /** Custom class name */
  className?: string;
  /** Size variant */
  size?: 'small' | 'medium' | 'large';
}

/**
 * EmptyState displays a placeholder when no data is available.
 *
 * Features:
 * - Large icon display
 * - Title and description text
 * - Optional primary action button
 * - Optional secondary link
 * - Optional helpful tip
 *
 * @example
 * ```tsx
 * <EmptyState
 *   icon={Package}
 *   title="No Repositories Yet"
 *   description="Create your first repository to start storing and managing your components."
 *   action={{
 *     label: "Create Repository",
 *     onClick: () => handleCreate(),
 *     icon: Plus,
 *   }}
 *   secondaryAction={{
 *     label: "Learn more",
 *     href: "https://help.sonatype.com/...",
 *   }}
 *   tip="Start with a proxy repository to cache artifacts from Maven Central."
 * />
 * ```
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  secondaryAction,
  tip,
  className = '',
  size = 'medium',
}: EmptyStateProps): JSX.Element {
  const iconSize = size === 'large' ? 64 : size === 'small' ? 32 : 48;
  const sizeClass = `empty-state--${size}`;

  return (
    <Flex
      direction="column"
      align="center"
      justify="center"
      gap="4"
      className={`empty-state ${sizeClass} ${className}`}
      data-testid="empty-state"
    >
      {/* Icon */}
      <Box className="empty-state__icon">
        <Icon size={iconSize} strokeWidth={1.5} aria-hidden="true" />
      </Box>

      {/* Title */}
      <Text
        as="h3"
        size={size === 'large' ? '6' : size === 'small' ? '3' : '5'}
        weight="medium"
        className="empty-state__title"
      >
        {title}
      </Text>

      {/* Description */}
      <Text
        as="p"
        size={size === 'small' ? '1' : '2'}
        color="gray"
        align="center"
        className="empty-state__description"
      >
        {description}
      </Text>

      {/* Primary Action */}
      {action && (
        <Button
          size={size === 'small' ? '2' : '3'}
          variant="solid"
          onClick={action.onClick}
          className="empty-state__action"
        >
          {action.icon && <action.icon size={16} aria-hidden="true" />}
          {action.label}
        </Button>
      )}

      {/* Secondary Action */}
      {secondaryAction && (
        <a
          href={secondaryAction.href}
          target="_blank"
          rel="noopener noreferrer"
          className="empty-state__secondary-action"
        >
          {secondaryAction.label}
          <ExternalLink size={14} aria-hidden="true" />
        </a>
      )}

      {/* Tip */}
      {tip && (
        <Box className="empty-state__tip">
          <Text size="1" color="gray">
            💡 {tip}
          </Text>
        </Box>
      )}
    </Flex>
  );
}

export default EmptyState;


