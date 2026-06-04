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
 * SettingsSection — Section wrapper with heading for Settings Hub
 *
 * Displays:
 * - Section heading (size="5")
 * - Grid of SettingsCard components with gap="3"
 */

import React from 'react';
import { Box, Flex, Heading } from '@radix-ui/themes';
import SettingsCard from './SettingsCard';
import type { SettingsSection as SettingsSectionType } from './types';

export interface SettingsSectionProps {
  section: SettingsSectionType;
}

export default function SettingsSection({ section }: SettingsSectionProps) {
  return (
    <Box>
      <Heading as="h2" size="5" mb="3">{section.label}</Heading>
      <Flex direction="column" gap="3">
        {section.cards.map((card) => (
          <SettingsCard key={card.id} card={card} />
        ))}
      </Flex>
    </Box>
  );
}
