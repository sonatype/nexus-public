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
 * Malicious Packages Page — Preview UI (Nexus One style).
 *
 * REST-only replacement for the Default UI MalwareRemediation.jsx.
 * Two visual states: active remediation (malware > 0) and no malware detected.
 * Mirrors the Classic UI per-repo table layout.
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Container,
  Flex,
  Text,
  Heading,
  Card,
  Button,
  Callout,
  Badge,
  Table,
} from '@radix-ui/themes';
import {
  AlertTriangle,
  CheckCircle,
  Download,
  ExternalLink,
  Info,
} from 'lucide-react';
import { restClient, ENDPOINTS } from '../../../interface/api';
import { fetchMalwareRemediatorTasks } from '../shared/security/malwareRemediatorTask';

const CSV_URL = ENDPOINTS.MALICIOUS_RISK_CSV;

const LINKS = {
  MALWARE_RISK_DOCS: 'https://help.sonatype.com/en/malware-risk.html',
  TASKS_DOCS: 'https://help.sonatype.com/en/tasks.html',
  REMOVAL_GUIDE: 'https://links.sonatype.com/nexus-repository-firewall/malware-risk/guide-to-removing-malware',
  PROTECT_REPOS: 'https://links.sonatype.com/nexus-repository-firewall/malware-risk/malware-risk',
  VULN_VS_MALWARE: 'https://links.sonatype.com/nexus-repository-firewall/malware-risk/vulnerabilities-and-malware',
  FIREWALL_DASHBOARD: 'https://links.sonatype.com/nexus-repository-firewall/malware-risk/firewall-dashboard',
};

interface MalwareCountsResponse {
  totalCount: number;
  counts: Record<string, number>;
  hdsAvailable: boolean;
  hcEnabledRepos: string[];
}

interface RemediateData {
  malwareCount: number;
  countsByRepo: Record<string, number>;
  tasksCount: number;
  proxyRepoCount: number;
  coveredRepoCount: number;
  loading: boolean;
  error: string | null;
}

function getTestMalwareCount(): number {
  try {
    return parseInt(localStorage.getItem('SONATYPE_TEST_MALWARE_BANNER') || '0', 10) || 0;
  } catch {
    return 0;
  }
}

function useRemediateData(): RemediateData & { refetch: () => void } {
  const [data, setData] = useState<Omit<RemediateData, 'loading' | 'error'>>({
    malwareCount: 0,
    countsByRepo: {},
    tasksCount: 0,
    proxyRepoCount: 0,
    coveredRepoCount: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [malwareCounts, tasks, repos] = await Promise.all([
        restClient.get<MalwareCountsResponse>(ENDPOINTS.MALWARE_COUNTS),
        fetchMalwareRemediatorTasks(),
        restClient.get<Array<{ name: string; type: string }>>(ENDPOINTS.REPOSITORIES),
      ]);

      const proxyRepos = (repos ?? []).filter((r) => r.type === 'proxy');
      const taskRepoNames = new Set(
        tasks.map((t) => t.properties?.repositoryName).filter(Boolean),
      );
      const hasAllRepo = taskRepoNames.has('all');
      const coveredCount = hasAllRepo
        ? proxyRepos.length
        : proxyRepos.filter((r) => taskRepoNames.has(r.name)).length;

      const testCount = getTestMalwareCount();
      const apiCount = malwareCounts?.totalCount ?? 0;

      setData({
        malwareCount: testCount || apiCount,
        countsByRepo: malwareCounts?.counts ?? {},
        tasksCount: tasks.length,
        proxyRepoCount: proxyRepos.length,
        coveredRepoCount: coveredCount,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refetch();
  }, [refetch]);

  return { ...data, loading, error, refetch };
}

function ExternalLinkInline({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
      {children} <ExternalLink size={12} />
    </a>
  );
}

function RepoBreakdownTable({ countsByRepo }: { countsByRepo: Record<string, number> }) {
  const entries = Object.entries(countsByRepo)
    .filter(([, count]) => count > 0)
    .sort(([, a], [, b]) => b - a);

  if (entries.length === 0) return null;

  return (
    <Box mb="5">
      <Heading size="3" mb="3">Per-Repository Breakdown</Heading>
      <Table.Root variant="surface">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="center">Malware Count</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {entries.map(([repo, count]) => (
            <Table.Row key={repo}>
              <Table.Cell>{repo}</Table.Cell>
              <Table.Cell justify="center" style={{ fontVariantNumeric: 'tabular-nums' }}>{count.toLocaleString()}</Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default function RemediatePage() {
  const {
    malwareCount,
    countsByRepo,
    tasksCount,
    loading,
    error,
    refetch,
  } = useRemediateData();
  const hasMalware = malwareCount > 0;

  if (loading) {
    return (
      <Container size="4" px="5" py="6">
        <Card>
          <Flex direction="column" gap="3" align="center" py="6">
            <Text size="2" color="gray">Loading malicious packages data...</Text>
          </Flex>
        </Card>
      </Container>
    );
  }

  if (error) {
    return (
      <Container size="4" px="5" py="6">
        <Callout.Root color="red" variant="surface">
          <Callout.Icon><AlertTriangle size={20} /></Callout.Icon>
          <Box className="rt-CalloutText">
            <Flex direction="column" gap="2">
              <Text weight="bold" size="3">Failed to load data</Text>
              <Text size="2">{error}</Text>
              <Button variant="soft" size="1" mt="2" onClick={refetch}>Retry</Button>
            </Flex>
          </Box>
        </Callout.Root>
      </Container>
    );
  }

  return (
    <Container size="4" px="5" py="6">
      {/* Page header */}
      <Flex align="center" gap="2" mb="1">
        <AlertTriangle size={24} color="var(--amber-9)" />
        <Heading size="5">Malicious Packages</Heading>
      </Flex>
      <Text size="2" color="gray" as="p" mb="5">
        Open source malware is cached in the proxy repositories on your Nexus Repository.
      </Text>

      {/* Status banner -- single count */}
      {hasMalware ? (
        <Callout.Root color="red" variant="surface" mb="5">
          <Callout.Icon><AlertTriangle size={20} /></Callout.Icon>
          <Box className="rt-CalloutText">
            <Flex direction="column" gap="2">
              <Text weight="bold" size="4">
                {malwareCount.toLocaleString()} Malicious {malwareCount === 1 ? 'Package' : 'Packages'} Detected
              </Text>
              <Text size="2">
                Sonatype has identified malicious packages in your repositories.
                Use the CSV below to identify which components are malicious and remediate.
              </Text>
            </Flex>
          </Box>
        </Callout.Root>
      ) : (
        <Callout.Root color="green" variant="surface" mb="5">
          <Callout.Icon><CheckCircle size={20} /></Callout.Icon>
          <Box className="rt-CalloutText">
            <Flex direction="column" gap="2">
              <Text weight="bold" size="4">No Malicious Packages Detected</Text>
              <Text size="2">
                No malicious components have been identified at this time.
                Repository Health Check continuously monitors your proxy repositories for known malware.
                Schedule Malware Removal Tasks to automatically clean any detected threats.
              </Text>
            </Flex>
          </Box>
        </Callout.Root>
      )}

      {/* Per-repo table (mirrors Classic UI) */}
      {hasMalware && <RepoBreakdownTable countsByRepo={countsByRepo} />}

      {/* Two-column: Steps + Education */}
      <Flex gap="5" direction={{ initial: 'column', sm: 'row' }}>
        {/* Remediation steps */}
        <Box style={{ flex: 2 }}>
          <Card>
            <Heading size="4" mb="3">Steps to Identify and Address Malware</Heading>
            <Box asChild pl="4">
              <ol style={{ margin: 0, display: 'flex', flexDirection: 'column', gap: 16 }}>
                <li>
                  <Text size="2" as="p">
                    Create and run Automatic Malware Management tasks on your proxy repositories.{' '}
                    <ExternalLinkInline href={LINKS.TASKS_DOCS}>
                      Learn about maintenance tasks
                    </ExternalLinkInline>
                  </Text>
                  <Flex gap="2" mt="2" wrap="wrap" align="center">
                    <Badge
                      variant="soft"
                      color={tasksCount > 0 ? 'blue' : 'orange'}
                      size="2"
                    >
                      Current tasks configured: {tasksCount}
                    </Badge>
                  </Flex>
                </li>
                <li>
                  <Text size="2" as="p" mb="2">
                    Download the CSV file using the link below to review the components flagged by
                    Sonatype as containing malware.
                  </Text>
                  {hasMalware ? (
                    <Button asChild variant="outline" size="2">
                      <a href={CSV_URL} download style={{ textDecoration: 'none' }}>
                        <Download size={14} /> Download CSV
                      </a>
                    </Button>
                  ) : (
                    <Button variant="outline" size="2" disabled>
                      <Download size={14} /> No components to export
                    </Button>
                  )}
                </li>
                <li>
                  <Text size="2" as="p">
                    Search your proxy repository to remove the components.{' '}
                    <ExternalLinkInline href={LINKS.REMOVAL_GUIDE}>
                      Guide to removing malware
                    </ExternalLinkInline>
                  </Text>
                </li>
                <li>
                  <Text size="2" as="p">
                    Learn how to protect your repository to keep developers from downloading
                    Malware again.{' '}
                    <ExternalLinkInline href={LINKS.PROTECT_REPOS}>
                      How to protect your repository from malware
                    </ExternalLinkInline>
                  </Text>
                </li>
              </ol>
            </Box>
            <Box mt="4">
              <ExternalLinkInline href={LINKS.FIREWALL_DASHBOARD}>
                <Text size="2">View Firewall Dashboard</Text>
              </ExternalLinkInline>
            </Box>
          </Card>
        </Box>

        {/* Education card */}
        <Box style={{ flex: 1 }}>
          <Callout.Root color="blue" variant="surface" style={{ height: '100%' }}>
            <Callout.Icon><Info size={20} /></Callout.Icon>
            <Box className="rt-CalloutText">
              <Flex direction="column" gap="2">
                <Text weight="bold" size="3">What is Open Source Malware?</Text>
                <Text size="2">
                  Open Source Malware in proxy repositories poses a critical risk to the integrity
                  of the software supply chain, introducing malware such as credential harvesting,
                  data exfiltration, backdoor, file system corruption leads to compromised
                  applications, data breaches, and regulatory non-compliance.
                </Text>
                <Text size="2">
                  Remediation requires immediate removal of infected components, identifying
                  impacted dependencies, and Developers must be informed of the threat and
                  prevented from accessing to compromised artifacts.
                </Text>
                <Box>
                  <ExternalLinkInline href={LINKS.VULN_VS_MALWARE}>
                    <Text size="2">Differentiating Software Vulnerabilities and Malware</Text>
                  </ExternalLinkInline>
                </Box>
              </Flex>
            </Box>
          </Callout.Root>
        </Box>
      </Flex>

      {/* Incident Response Workflow */}
      {hasMalware && (
        <Card mt="5">
          <Heading size="4" mb="3">Incident Response Workflow</Heading>
          <Box asChild pl="4">
            <ol style={{ margin: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
              <li><Text size="2"><strong>Identify</strong> — Use the per-repository breakdown to locate repositories containing malware.</Text></li>
              <li><Text size="2"><strong>Clean</strong> — Run automated remediation or manually remove malicious components.</Text></li>
              <li><Text size="2"><strong>Audit</strong> — Review who accessed the malicious components and when.</Text></li>
              <li><Text size="2"><strong>Protect</strong> — Enable quarantine and automated remediation on all proxy repositories.</Text></li>
              <li><Text size="2"><strong>Educate</strong> — Install Sonatype Guide for shift-left protection for your developers.</Text></li>
            </ol>
          </Box>
        </Card>
      )}
    </Container>
  );
}
