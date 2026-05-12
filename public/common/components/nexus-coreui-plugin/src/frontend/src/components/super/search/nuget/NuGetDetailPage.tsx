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

import React, { useEffect, useState } from 'react';
import { 
  Box, 
  Card, 
  Flex, 
  Heading, 
  Text, 
  Badge, 
  Tabs, 
  Spinner, 
  Callout,
  Button,
  ScrollArea,
  Table,
  Link,
  Code,
  Separator,
} from '@radix-ui/themes';
import { ArrowLeft, Package, ExternalLink, Download, CheckCircle } from 'lucide-react';

import type { NuGetDetail, NuGetVersion } from './nuget.types';
import { mockNuGetDetailApi } from './mockData';

interface NuGetDetailPageProps {
  packageId: string;
  onBack?: () => void;
}

/**
 * NuGet package detail page.
 * Shows package overview, versions, and installation instructions.
 */
export function NuGetDetailPage({ packageId, onBack }: NuGetDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<NuGetDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedVersion, setSelectedVersion] = useState<string | null>(null);

  useEffect(() => {
    async function loadDetail() {
      setLoading(true);
      setError(null);
      
      try {
        const data = await mockNuGetDetailApi(packageId);
        setDetail(data);
        if (data.versions.length > 0) {
          setSelectedVersion(data.versions[0].version);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load package');
      } finally {
        setLoading(false);
      }
    }

    loadDetail();
  }, [packageId]);

  const handleBackClick = () => {
    if (onBack) {
      onBack();
    } else {
      window.location.hash = '#preview/browse/search/nuget';
    }
  };

  if (loading) {
    return (
      <Box p="6">
        <Flex justify="center" align="center" style={{ minHeight: '400px' }}>
          <Flex direction="column" align="center" gap="3">
            <Spinner size="3" />
            <Text color="gray">Loading package details...</Text>
          </Flex>
        </Flex>
      </Box>
    );
  }

  if (error || !detail) {
    return (
      <Box p="6">
        <Button variant="ghost" onClick={handleBackClick} mb="4">
          <ArrowLeft size={16} />
          Back to Search
        </Button>
        <Callout.Root color="red">
          <Callout.Icon>
            <Package size={16} />
          </Callout.Icon>
          <Callout.Text>{error || 'Package not found'}</Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  const latestVersion = detail.versions[0]?.version || '';

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Back */}
          <Button variant="ghost" onClick={handleBackClick} style={{ alignSelf: 'flex-start' }}>
            <ArrowLeft size={16} />
            Back to Search
          </Button>

          {/* Header */}
          <Card>
            <Flex justify="between" align="start" wrap="wrap" gap="3">
              <Flex direction="column" gap="1">
                <Flex align="center" gap="2">
                  <Package size={24} />
                  <Heading size="6">{detail.displayName}</Heading>
                </Flex>
                {detail.authors && (
                  <Text size="2" color="gray">by {detail.authors.join(', ')}</Text>
                )}
              </Flex>
              <Badge color="blue" size="2">{latestVersion}</Badge>
            </Flex>
          </Card>

          {/* Tabs */}
          <Card>
            <Tabs.Root defaultValue="overview">
              <Tabs.List>
                <Tabs.Trigger value="overview">Overview</Tabs.Trigger>
                <Tabs.Trigger value="versions">Versions ({detail.versions.length})</Tabs.Trigger>
                <Tabs.Trigger value="install">Install</Tabs.Trigger>
              </Tabs.List>

              <Box pt="4">
                {/* Overview Tab */}
                <Tabs.Content value="overview">
                  <Flex direction="column" gap="4">
                    {detail.description && (
                      <Box>
                        <Heading size="3" mb="2">Description</Heading>
                        <Text>{detail.description}</Text>
                      </Box>
                    )}

                    <Separator size="4" />

                    <Flex gap="6" wrap="wrap">
                      {detail.projectUrl && (
                        <Box>
                          <Text size="1" color="gray" mb="1">Project URL</Text>
                          <Link href={detail.projectUrl} target="_blank">
                            <Flex align="center" gap="1">
                              {detail.projectUrl}
                              <ExternalLink size={12} />
                            </Flex>
                          </Link>
                        </Box>
                      )}

                      {detail.license && (
                        <Box>
                          <Text size="1" color="gray" mb="1">License</Text>
                          <Text>{detail.license}</Text>
                        </Box>
                      )}
                    </Flex>

                    {detail.tags && detail.tags.length > 0 && (
                      <Box>
                        <Text size="1" color="gray" mb="2">Tags</Text>
                        <Flex gap="1" wrap="wrap">
                          {detail.tags.map((tag) => (
                            <Badge key={tag} variant="soft">{tag}</Badge>
                          ))}
                        </Flex>
                      </Box>
                    )}
                  </Flex>
                </Tabs.Content>

                {/* Versions Tab */}
                <Tabs.Content value="versions">
                  <Table.Root>
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Version</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Downloads</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Published</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Frameworks</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {detail.versions.map((v) => (
                        <VersionRow 
                          key={v.version} 
                          version={v} 
                          isLatest={v.version === latestVersion}
                          isSelected={v.version === selectedVersion}
                          onSelect={() => setSelectedVersion(v.version)}
                        />
                      ))}
                    </Table.Body>
                  </Table.Root>
                </Tabs.Content>

                {/* Install Tab */}
                <Tabs.Content value="install">
                  <Flex direction="column" gap="4">
                    <Box>
                      <Heading size="3" mb="3">Package Manager Console</Heading>
                      <Card>
                        <Code size="2" style={{ display: 'block' }}>
                          Install-Package {detail.packageId} -Version {selectedVersion || latestVersion}
                        </Code>
                      </Card>
                    </Box>

                    <Box>
                      <Heading size="3" mb="3">.NET CLI</Heading>
                      <Card>
                        <Code size="2" style={{ display: 'block' }}>
                          dotnet add package {detail.packageId} --version {selectedVersion || latestVersion}
                        </Code>
                      </Card>
                    </Box>

                    <Box>
                      <Heading size="3" mb="3">PackageReference</Heading>
                      <Card>
                        <Code size="2" style={{ display: 'block', whiteSpace: 'pre' }}>
{`<PackageReference Include="${detail.packageId}" Version="${selectedVersion || latestVersion}" />`}
                        </Code>
                      </Card>
                    </Box>
                  </Flex>
                </Tabs.Content>
              </Box>
            </Tabs.Root>
          </Card>
        </Flex>
      </Box>
    </ScrollArea>
  );
}

function VersionRow({ 
  version, 
  isLatest, 
  isSelected,
  onSelect,
}: { 
  version: NuGetVersion; 
  isLatest: boolean;
  isSelected: boolean;
  onSelect: () => void;
}): JSX.Element {
  return (
    <Table.Row 
      onClick={onSelect}
      style={{ 
        cursor: 'pointer',
        backgroundColor: isSelected ? 'var(--accent-3)' : undefined,
      }}
    >
      <Table.Cell>
        <Flex align="center" gap="2">
          <Text weight="medium">{version.version}</Text>
          {isLatest && (
            <Badge color="green" size="1">
              <Flex align="center" gap="1">
                <CheckCircle size={10} />
                Latest
              </Flex>
            </Badge>
          )}
          {version.isPrerelease && (
            <Badge color="amber" size="1">Prerelease</Badge>
          )}
        </Flex>
      </Table.Cell>
      <Table.Cell>
        {version.downloads ? (
          <Flex align="center" gap="1">
            <Download size={12} />
            <Text size="2">{formatNumber(version.downloads)}</Text>
          </Flex>
        ) : (
          <Text color="gray">-</Text>
        )}
      </Table.Cell>
      <Table.Cell>
        <Text size="2" color="gray">
          {new Date(version.published).toLocaleDateString()}
        </Text>
      </Table.Cell>
      <Table.Cell>
        {version.targetFrameworks && version.targetFrameworks.length > 0 ? (
          <Flex gap="1" wrap="wrap">
            {version.targetFrameworks.slice(0, 3).map((fw) => (
              <Badge key={fw} size="1" variant="outline">{fw}</Badge>
            ))}
            {version.targetFrameworks.length > 3 && (
              <Badge size="1" variant="outline">+{version.targetFrameworks.length - 3}</Badge>
            )}
          </Flex>
        ) : (
          <Text color="gray">-</Text>
        )}
      </Table.Cell>
    </Table.Row>
  );
}

function formatNumber(n: number): string {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`;
  return String(n);
}

export default NuGetDetailPage;

