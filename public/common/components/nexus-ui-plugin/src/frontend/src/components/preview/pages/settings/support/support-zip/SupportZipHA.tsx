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

import { SupportZipForm } from './SupportZipForm';
import { SupportZipParams } from './types';

import './SupportZipHA.scss';

interface SupportZipHAProps {
  params: SupportZipParams;
  onParamChange: (name: keyof SupportZipParams, value: boolean | number) => void;
  onSubmit: () => void;
  onSubmitAll: () => void;
  disabled?: boolean;
}

/**
 * SupportZipHA - HA (clustered) support ZIP interface
 *
 * Displays support ZIP form with options for single node or all nodes.
 */
export function SupportZipHA({
  params,
  onParamChange,
  onSubmit,
  onSubmitAll,
  disabled = false,
}: SupportZipHAProps) {
  return (
    <Box className="support-zip-ha" data-testid="support-zip-ha">
      {/* HA Header */}
      <Flex align="center" gap="2" mb="4" className="support-zip-ha__header">
        <Server size={20} className="support-zip-ha__icon" />
        <Heading as="h3" size="4" weight="medium">
          High Availability Mode
        </Heading>
      </Flex>

      <Text size="2" color="gray" mb="4">
        You are running in a clustered environment. You can create a support ZIP for this node only
        or for all nodes in the cluster.
      </Text>

      {/* Form */}
      <SupportZipForm
        params={params}
        onParamChange={onParamChange}
        onSubmit={onSubmit}
        onSubmitAll={onSubmitAll}
        isHa={true}
        disabled={disabled}
      />
    </Box>
  );
}

export default SupportZipHA;


