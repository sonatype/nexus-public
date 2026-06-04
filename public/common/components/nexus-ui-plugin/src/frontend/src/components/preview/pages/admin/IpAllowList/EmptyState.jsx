/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import React from 'react';
import { Box, Flex, Text, Button } from '@radix-ui/themes';
import './EmptyState.scss';

/**
 * Empty state component following NexusOne design patterns
 *
 * Categories:
 * 1. Data-Driven: No CTA needed (system-generated empty state)
 * 2. User-Action: User can undo/adjust (e.g., Reset Filters)
 * 3. First-Time Creation: Encourage first action with primary CTA
 *
 * Guidelines:
 * - No icons in empty states
 * - Title: Text size="3" weight="medium"
 * - Description: Text size="1" color="gray"
 * - Text: Title Case (First Letter Of Each Word Capitalized)
 * - CTA buttons: Button size="1" variant="solid"
 *
 * @param {string} heading - Main heading text (Title Case)
 * @param {string} description - Descriptive text below heading
 * @param {Array} actions - Array of action objects with label, variant, onClick, testId
 * @param {string} className - Additional CSS classes
 */
export function EmptyState({
  heading,
  description,
  actions = [],
  className = ''
}) {
  return (
    <Box className={`empty-state ${className}`}>
      <Text
        as="h3"
        size="3"
        weight="medium"
        className="empty-state__heading"
      >
        {heading}
      </Text>
      {description && (
        <Text
          as="p"
          size="1"
          color="gray"
          className="empty-state__description"
        >
          {description}
        </Text>
      )}
      {actions && actions.length > 0 && (
        <Flex gap="2" className="empty-state__actions" justify="center">
          {actions.map((action, index) => (
            <Button
              key={index}
              size="2"
              variant={action.variant || 'solid'}
              onClick={action.onClick}
              data-testid={action.testId}
            >
              {action.label}
            </Button>
          ))}
        </Flex>
      )}
    </Box>
  );
}
