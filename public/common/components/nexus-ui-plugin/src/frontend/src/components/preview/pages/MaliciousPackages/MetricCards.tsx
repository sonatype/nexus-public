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
import { Flex, Card, Text, Heading } from '@radix-ui/themes';

import { getFindingStatus, type MaliciousFinding } from './types';

interface MetricCardsProps {
  activeFindings: MaliciousFinding[];
  malwareCount: number;
  countsByRepo: Record<string, number>;
}

function computeFirstDetected(findings: MaliciousFinding[]): string {
  let oldest: string | null = null;
  for (const f of findings) {
    if (f.firstDetectedAt && (!oldest || f.firstDetectedAt < oldest)) {
      oldest = f.firstDetectedAt;
    }
  }
  if (!oldest) return 'N/A';
  return new Date(oldest).toLocaleDateString();
}

export function MetricCards({ activeFindings, malwareCount, countsByRepo }: MetricCardsProps): React.ReactElement {
  const pendingCount = activeFindings.filter((f) => getFindingStatus(f) === 'pending').length;
  const affectedRepoCount = Object.keys(countsByRepo).length;
  const firstDetected = computeFirstDetected(activeFindings);

  return (
    <Flex gap="3" wrap="wrap">
      <Card style={{ flex: 1, minWidth: 140 }}>
        <Flex direction="column" align="center" gap="1" p="2">
          <Heading size="8" color="red" trim="both">
            {pendingCount}
          </Heading>
          <Text size="2" color="gray">Pending Findings</Text>
        </Flex>
      </Card>

      <Card style={{ flex: 1, minWidth: 140 }}>
        <Flex direction="column" align="center" gap="1" p="2">
          <Heading size="8" trim="both">
            {affectedRepoCount}
          </Heading>
          <Text size="2" color="gray">Affected Repos</Text>
        </Flex>
      </Card>

      <Card style={{ flex: 1, minWidth: 140 }}>
        <Flex direction="column" align="center" gap="1" p="2">
          <Heading size="8" color="orange" trim="both">
            {malwareCount}
          </Heading>
          <Text size="2" color="gray">RHC Malware</Text>
        </Flex>
      </Card>

      <Card style={{ flex: 1, minWidth: 140 }}>
        <Flex direction="column" align="center" gap="1" p="2">
          <Heading size="8" trim="both">
            {firstDetected}
          </Heading>
          <Text size="2" color="gray">First Detected</Text>
        </Flex>
      </Card>
    </Flex>
  );
}
