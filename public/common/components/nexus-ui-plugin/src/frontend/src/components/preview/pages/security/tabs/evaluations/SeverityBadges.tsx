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
import { Badge, Flex } from '@radix-ui/themes';

interface Props {
  critical: number;
  severe: number;
  moderate: number;
  quarantined: number;
}

/**
 * Renders the four severity indicators — Critical / Severe / Moderate — plus a Quarantined count.
 * Deliberately matches IQ's bucket set (Critical / Severe / Moderate).
 */
export function SeverityBadges({ critical, severe, moderate, quarantined }: Props) {
  return (
    <Flex gap="1" wrap="wrap" align="center">
      <Badge
        size="1"
        radius="full"
        color={critical > 0 ? 'red' : 'gray'}
        variant={critical > 0 ? 'solid' : 'soft'}
        title={`${critical} Critical`}
        style={{ minWidth: 35, justifyContent: 'center' }}
      >
        {critical}
      </Badge>
      <Badge
        size="1"
        radius="full"
        color={severe > 0 ? 'orange' : 'gray'}
        variant={severe > 0 ? 'solid' : 'soft'}
        title={`${severe} Severe`}
        style={{ minWidth: 35, justifyContent: 'center' }}
      >
        {severe}
      </Badge>
      <Badge
        size="1"
        radius="full"
        color={moderate > 0 ? 'yellow' : 'gray'}
        variant={moderate > 0 ? 'solid' : 'soft'}
        title={`${moderate} Moderate`}
        style={{ minWidth: 35, justifyContent: 'center' }}
      >
        {moderate}
      </Badge>
      <Badge
        size="1"
        radius="full"
        color={quarantined > 0 ? 'blue' : 'gray'}
        variant={quarantined > 0 ? 'solid' : 'soft'}
        title={`${quarantined} Quarantined`}
        style={{ minWidth: 35, justifyContent: 'center' }}
      >
        {quarantined}
      </Badge>
    </Flex>
  );
}
