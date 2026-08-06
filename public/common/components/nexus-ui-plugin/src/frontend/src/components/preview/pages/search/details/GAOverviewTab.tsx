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

import React, { useCallback, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Code,
  Flex,
  Grid,
  Heading,
  Link,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { Copy, ExternalLink } from 'lucide-react';

import type { GADetail } from '../core';

interface GAOverviewTabProps {
  detail: GADetail;
  selectedVersion: string | null;
}

/**
 * GAOverviewTab - Overview information for a GA.
 * Matches prototype layout: cards in grid, License, Description, Project URL, Usage snippets.
 */
function formatDate(dateString: string | undefined): string {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return '—';
  }
}

export function GAOverviewTab({ detail, selectedVersion }: GAOverviewTabProps) {
  const { projectUrl, gaId, repositories } = detail;
  const [copiedType, setCopiedType] = useState<'maven' | 'gradle' | null>(null);

  const parts = gaId.split(':');
  const groupId = parts.length >= 3 ? parts[1] : '';
  const artifactId = parts.length >= 3 ? parts[2] : detail.displayName;
  const ver = selectedVersion || detail.versions[0]?.version || 'VERSION';

  const selectedVersionObj = detail.versions.find((v) => v.version === ver);
  const lastUpdated = selectedVersionObj?.lastUpdated ?? detail.versions[0]?.lastUpdated;
  const ecosystemLabel = detail.format === 'maven' ? 'Maven' : detail.format;
  const repositoryNames = repositories?.map((r) => r.name).join(', ') || '—';

  const mavenSnippet = `<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>${ver}</version>
</dependency>`;

  const gradleSnippet = `implementation '${groupId}:${artifactId}:${ver}'`;

  const handleCopyMaven = useCallback(() => {
    navigator.clipboard.writeText(mavenSnippet);
    setCopiedType('maven');
    setTimeout(() => setCopiedType(null), 2000);
  }, [mavenSnippet]);

  const handleCopyGradle = useCallback(() => {
    navigator.clipboard.writeText(gradleSnippet);
    setCopiedType('gradle');
    setTimeout(() => setCopiedType(null), 2000);
  }, [gradleSnippet]);

  return (
    <Flex direction="column" gap="6">
      {/* First Row - License+Description combined, Project URL (same width as before) */}
      <Grid columns={{ initial: '1', md: '2fr 1fr' }} gap="4">
        {/* Dependencies Card */}
        <Card size="1">
          <Box p="4">
            <Heading size="4" mb="4">Dependencies</Heading>
            <Flex direction="column" gap="4">
              <Box>
                <Text size="1" color="gray" weight="medium" mb="2">Maven</Text>
                <Box
                  p="4"
                  style={{
                    backgroundColor: 'var(--gray-2)',
                    borderRadius: '6px',
                  }}
                >
                  <Flex align="center" justify="between" gap="2">
                    <Code
                      size="2"
                      color="gray"
                      variant="ghost"
                      style={{
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-all',
                        flex: 1,
                        overflowX: 'auto',
                      }}
                    >
                      {mavenSnippet}
                    </Code>
                    <Tooltip content={copiedType === 'maven' ? 'Copied!' : 'Copy'}>
                      <Button variant="soft" color="blue" size="2" onClick={handleCopyMaven}>
                        <Copy size={14} />
                      </Button>
                    </Tooltip>
                  </Flex>
                </Box>
              </Box>
              <Box>
                <Text size="1" color="gray" weight="medium" mb="2">Gradle</Text>
                <Box
                  p="4"
                  style={{
                    backgroundColor: 'var(--gray-2)',
                    borderRadius: '6px',
                  }}
                >
                  <Flex align="center" justify="between" gap="2">
                    <Code
                      size="2"
                      color="gray"
                      variant="ghost"
                      style={{
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-all',
                        flex: 1,
                      }}
                    >
                      {gradleSnippet}
                    </Code>
                    <Tooltip content={copiedType === 'gradle' ? 'Copied!' : 'Copy'}>
                      <Button variant="soft" color="blue" size="2" onClick={handleCopyGradle}>
                        <Copy size={14} />
                      </Button>
                    </Tooltip>
                  </Flex>
                </Box>
              </Box>
            </Flex>
          </Box>
        </Card>

        {/* Component Details Card - same width as before (1/3) */}
        <Card size="1">
          <Box p="4">
            <Heading size="4" mb="3">Component Details</Heading>
            <Table.Root variant="ghost" size="1">
              <Table.Body>
                <Table.Row>
                  <Table.Cell>
                    <Text size="2" color="gray">Last Updated</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2" weight="medium">{formatDate(lastUpdated)}</Text>
                  </Table.Cell>
                </Table.Row>
                <Table.Row>
                  <Table.Cell>
                    <Text size="2" color="gray">Ecosystem</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2" weight="medium">{ecosystemLabel}</Text>
                  </Table.Cell>
                </Table.Row>
                <Table.Row>
                  <Table.Cell>
                    <Text size="2" color="gray">Group</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2" weight="medium">{groupId || '—'}</Text>
                  </Table.Cell>
                </Table.Row>
                <Table.Row>
                  <Table.Cell>
                    <Text size="2" color="gray">Repository</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2" weight="medium" style={{ fontFamily: 'monospace', fontSize: '11px' }}>
                      {repositoryNames}
                    </Text>
                  </Table.Cell>
                </Table.Row>
                {projectUrl && (
                  <Table.Row>
                    <Table.Cell>
                      <Text size="2" color="gray">Project URL</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Link href={projectUrl} target="_blank" rel="noopener noreferrer" style={{ textDecoration: 'none' }}>
                        <Text size="2" weight="medium" style={{ color: 'var(--blue-11)' }}>
                          {projectUrl}
                        </Text>
                        <ExternalLink size={12} style={{ marginLeft: 4, verticalAlign: 'middle', display: 'inline' }} />
                      </Link>
                    </Table.Cell>
                  </Table.Row>
                )}
              </Table.Body>
            </Table.Root>
          </Box>
        </Card>
      </Grid>
    </Flex>
  );
}

export default GAOverviewTab;
