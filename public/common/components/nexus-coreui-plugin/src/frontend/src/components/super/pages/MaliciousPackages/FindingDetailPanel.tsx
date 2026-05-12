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
import { Grid, Flex, Text, Code, Badge, Heading } from '@radix-ui/themes';

import type { MaliciousFinding } from './types';

interface FindingDetailPanelProps {
  finding: MaliciousFinding;
}

function getThreatBadge(threatLevel: number | null): React.ReactElement | null {
  if (threatLevel === null) return null;
  if (threatLevel >= 9) return <Badge color="red">Critical ({threatLevel})</Badge>;
  if (threatLevel >= 7) return <Badge color="orange">Severe ({threatLevel})</Badge>;
  if (threatLevel >= 4) return <Badge color="yellow">Moderate ({threatLevel})</Badge>;
  return <Badge color="gray">Low ({threatLevel})</Badge>;
}

function formatTimestamp(ts: string | null): string {
  if (!ts) return 'N/A';
  return new Date(ts).toLocaleString();
}

export function FindingDetailPanel({ finding }: FindingDetailPanelProps): React.ReactElement {
  const hasIntel = finding.componentName !== null;

  return (
    <Grid columns="2" gap="5" p="4" style={{ background: 'var(--gray-2)', borderRadius: 'var(--radius-2)' }}>
      <Flex direction="column" gap="3">
        <Heading size="3">Threat Intelligence</Heading>

        {hasIntel ? (
          <Flex direction="column" gap="2">
            <Flex gap="2" align="center">
              <Text size="2" weight="medium">Component:</Text>
              <Text size="2">
                {finding.componentName}@{finding.componentVersion ?? 'unknown'}
              </Text>
            </Flex>

            <Flex gap="2" align="center">
              <Text size="2" weight="medium">Format:</Text>
              <Text size="2">{finding.componentFormat ?? finding.format}</Text>
            </Flex>

            <Flex gap="2" align="center">
              <Text size="2" weight="medium">Threat Level:</Text>
              {getThreatBadge(finding.threatLevel)}
            </Flex>

            {finding.threatSummary && (
              <Flex direction="column" gap="1">
                <Text size="2" weight="medium">Summary:</Text>
                <Text size="2" color="gray">{finding.threatSummary}</Text>
              </Flex>
            )}

            {finding.threatReference && (
              <Flex gap="2" align="center">
                <Text size="2" weight="medium">Reference:</Text>
                <Text size="2" asChild>
                  <a href={finding.threatReference} target="_blank" rel="noopener noreferrer">
                    {finding.threatReference}
                  </a>
                </Text>
              </Flex>
            )}

            {finding.policyName && (
              <Flex gap="2" align="center">
                <Text size="2" weight="medium">Policy:</Text>
                <Text size="2">{finding.policyName}</Text>
              </Flex>
            )}
          </Flex>
        ) : (
          <Text size="2" color="orange">
            Threat intel not available — re-scan to enrich
          </Text>
        )}
      </Flex>

      <Flex direction="column" gap="3">
        <Heading size="3">Provenance</Heading>

        <Flex direction="column" gap="2">
          <Flex gap="2" align="center">
            <Text size="2" weight="medium">Asset Path:</Text>
            <Text size="2">{finding.path}</Text>
          </Flex>

          {finding.hash && (
            <Flex gap="2" align="center">
              <Text size="2" weight="medium">SHA-1:</Text>
              <Code size="2">{finding.hash}</Code>
            </Flex>
          )}

          <Flex gap="2" align="center">
            <Text size="2" weight="medium">Proxied By:</Text>
            <Text size="2">
              {finding.createdBy ?? 'N/A'}
              {finding.createdByIp && ` (${finding.createdByIp})`}
            </Text>
          </Flex>

          <Flex gap="2" align="center">
            <Text size="2" weight="medium">Cached:</Text>
            <Text size="2">{formatTimestamp(finding.recordedTime)}</Text>
          </Flex>

          {finding.deletedTime && (
            <Flex direction="column" gap="1" mt="2" p="2" style={{ background: 'var(--red-2)', borderRadius: 'var(--radius-2)' }}>
              <Text size="2" weight="medium" color="red">Deletion Record</Text>
              <Text size="2">Deleted: {formatTimestamp(finding.deletedTime)}</Text>
              {finding.deletedBy && <Text size="2">By: {finding.deletedBy}</Text>}
              {finding.deletionMethod && <Text size="2">Method: {finding.deletionMethod}</Text>}
            </Flex>
          )}
        </Flex>
      </Flex>
    </Grid>
  );
}
