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
 * SettingsNotAvailablePage —placeholder page for settings not yet migrated to the new UI
 *
 * Displays a centered message explaining that a feature is still being prepared
 * for the new UI and should be accessed through the classic administration panel.
 */

import React from 'react';
import { Flex, Heading, Text, Button } from '@radix-ui/themes';
import { Construction, ArrowLeft } from 'lucide-react';

export interface SettingsNotAvailablePageProps {
  featureName: string;
}

export default function SettingsNotAvailablePage({ featureName }: SettingsNotAvailablePageProps) {
  return (
    <Flex
      direction="column"
      align="center"
      justify="center"
      style={{ minHeight: '100vh', padding: 'var(--space-6)', background: 'var(--color-background)' }}
    >
      <Flex
        direction="column"
        align="center"
        gap="4"
        style={{ maxWidth: '500px', textAlign: 'center' }}
      >
        <Construction
          size={64}
          strokeWidth={1.5}
          style={{ color: 'var(--gray-8)', marginBottom: 'var(--space-2)' }}
        />
        <Heading size="7">Not available in preview</Heading>
        <div style={{ width: '60px', height: '3px', background: 'var(--accent-9)', borderRadius: 'var(--radius-1)', margin: 'var(--space-1) 0' }} />
        <Text size="3" color="gray">
          {featureName} is still being prepared for the Nexus One UI.
          Access it through the classic UI administration panel for now.
        </Text>
        <Button variant="solid" size="3" mt="2" asChild>
          <a href="#preview/admin/settings" style={{ color: 'var(--accent-contrast)', textDecoration: 'none' }}>
            <ArrowLeft size={16} />
            Back to Settings
          </a>
        </Button>
      </Flex>
    </Flex>
  );
}
