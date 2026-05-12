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
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { Server } from 'lucide-react';

import { SettingsFormSection } from '../../../shared/form';
import { NodesList } from './NodesList';
import { NodesPageProps } from './types';

import './NodesPage.scss';

/**
 * NodesPage - Main Nodes management page for Preview UI
 *
 * Displays cluster nodes in the system.
 */
export function NodesPage({ className }: NodesPageProps) {
  return (
    <Box className={`nodes-page ${className || ''}`.trim()}>
      {/* Header */}
      <Flex align="center" gap="3" className="nodes-page__header">
        <Server size={24} className="nodes-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">Nodes</Heading>
          <Text size="2" className="nodes-page__description">
            View cluster nodes in this Nexus Repository instance
          </Text>
        </Box>
      </Flex>

      {/* Content */}
      <Box className="nodes-page__content">
        {/* Nodes List Section */}
        <SettingsFormSection title="Cluster Nodes">
          <Text size="2" className="nodes-page__section-description">
            Nodes currently participating in this Nexus Repository cluster.
          </Text>
          <Box className="nodes-page__nodes-list">
            <NodesList />
          </Box>
        </SettingsFormSection>
      </Box>
    </Box>
  );
}

export default NodesPage;
