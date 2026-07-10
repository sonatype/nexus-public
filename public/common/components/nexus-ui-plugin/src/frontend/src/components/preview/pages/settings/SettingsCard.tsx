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

/**
 * SettingsCard — Small variant card for Settings Hub
 *
 * Displays a layout with:
 * - Name (bold text) with optional Coming Soon badge
 * - Description (gray text, wraps naturally)
 * - View button
 *
 * The entire card is clickable and navigates to the settings page.
 */

import React from 'react';
import { Badge, Box, Flex, Text, Button, Card } from '@radix-ui/themes';
import { isFeatureEnabled } from '../../config/featureFlags';
import type { SettingCard } from './types';

export interface SettingsCardProps {
  card: SettingCard;
}

export default function SettingsCard({ card }: SettingsCardProps) {
  const href = card.fullHash ?? `#preview/admin/${card.path}`;
  const isComingSoon = card.featureKey ? !isFeatureEnabled(card.featureKey) : false;

  return (
    <Card asChild style={{ cursor: 'pointer' }}>
      <a href={href}>
        <Box p="3">
          <Flex align="start" justify="between" gap="4">
            <Box style={{ flex: 1, minWidth: 0 }}>
              <Flex align="center" gap="2">
                <Text size="2" weight="medium">{card.label}</Text>
                {isComingSoon && (
                  <Badge color="blue" size="1" variant="soft">
                    Coming Soon
                  </Badge>
                )}
              </Flex>
              <Text as="p" size="2" color="gray" style={{ margin: 0 }}>
                {card.description}
              </Text>
            </Box>
            <Button variant="surface" size="1">View</Button>
          </Flex>
        </Box>
      </a>
    </Card>
  );
}
