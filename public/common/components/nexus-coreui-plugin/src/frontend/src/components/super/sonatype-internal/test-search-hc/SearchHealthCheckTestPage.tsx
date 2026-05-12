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
 * SONATYPE INTERNAL — NOT INCLUDED IN PRODUCTION BUILDS
 *
 * SearchHealthCheckTestPage — every permutation of HealthCheckCell in a browse-grid layout.
 * Click any cell to see the corresponding Health Check modal.
 *
 * Access: http://localhost:8081/?debug#preview/test-search-hc
 */

import React from 'react';
import { Box, Flex, Text, Heading, Card, Badge, Button, Table } from '@radix-ui/themes';
import { FlaskConical, ExternalLink } from 'lucide-react';
import { HealthCheckCell, HealthCheckStatus } from '../../../shared/security/HealthCheckCell';
import type { SecurityRepositoryInfo } from '../../../shared/security/security.types';

import './SearchHealthCheckTestPage.scss';

// ─────────────────────────────────────────────────────────────────────────────
// Repo fixtures
// ─────────────────────────────────────────────────────────────────────────────

const PROXY_MAVEN: SecurityRepositoryInfo = { name: 'maven-central', type: 'proxy', format: 'maven2' };
const PROXY_NPM: SecurityRepositoryInfo = { name: 'npm-proxy', type: 'proxy', format: 'npm' };
const PROXY_NUGET: SecurityRepositoryInfo = { name: 'nuget-proxy', type: 'proxy', format: 'nuget' };
const PROXY_DOCKER: SecurityRepositoryInfo = { name: 'docker-proxy', type: 'proxy', format: 'docker' }; // not supported for HC
const PROXY_HELM: SecurityRepositoryInfo = { name: 'helm-proxy', type: 'proxy', format: 'helm' };      // not supported for HC
const HOSTED_MAVEN: SecurityRepositoryInfo = { name: 'maven-releases', type: 'hosted', format: 'maven2' };
const GROUP_MAVEN: SecurityRepositoryInfo = { name: 'maven-public', type: 'group', format: 'maven2' };

const REPORT_URL = 'https://clm.sonatype.com/assets/index.html#/reports/maven-central/current';

// ─────────────────────────────────────────────────────────────────────────────
// Permutations
// ─────────────────────────────────────────────────────────────────────────────

interface HCPermutation {
  id: string;
  label: string;
  description: string;
  repository: SecurityRepositoryInfo;
  healthStatus?: HealthCheckStatus;
  group?: string;
}

const PERMUTATIONS: HCPermutation[] = [
  // ── Not applicable ─────────────────────────────────────────────────────────
  {
    id: 'hosted-repo',
    label: 'Hosted repo',
    description: 'Hosted repos are not applicable for Health Check — shows "—".',
    repository: HOSTED_MAVEN,
    group: 'Not Applicable',
  },
  {
    id: 'group-repo',
    label: 'Group repo',
    description: 'Group repos are not applicable — shows "—".',
    repository: GROUP_MAVEN,
    group: 'Not Applicable',
  },
  {
    id: 'unsupported-docker',
    label: 'Docker proxy (unsupported)',
    description: 'Docker format is not supported by Health Check — shows "Not supported" tooltip.',
    repository: PROXY_DOCKER,
    group: 'Not Applicable',
  },
  {
    id: 'unsupported-helm',
    label: 'Helm proxy (unsupported)',
    description: 'Helm format not supported.',
    repository: PROXY_HELM,
    group: 'Not Applicable',
  },

  // ── Not yet analyzed ───────────────────────────────────────────────────────
  {
    id: 'no-status',
    label: 'No status (not analyzed)',
    description: 'healthStatus=undefined — shows Analyze button.',
    repository: PROXY_MAVEN,
    healthStatus: undefined,
    group: 'Not Yet Analyzed',
  },
  {
    id: 'not-enabled',
    label: 'Not enabled',
    description: 'healthStatus present but enabled=false — shows Analyze button.',
    repository: PROXY_NPM,
    healthStatus: { enabled: false },
    group: 'Not Yet Analyzed',
  },

  // ── Analyzing / in-progress ────────────────────────────────────────────────
  {
    id: 'analyzing',
    label: 'Analyzing in progress',
    description: 'analyzing=true — shows spinner/loading state.',
    repository: PROXY_MAVEN,
    healthStatus: { enabled: true, analyzing: true },
    group: 'In Progress',
  },

  // ── Clean ──────────────────────────────────────────────────────────────────
  {
    id: 'clean-zero',
    label: 'Clean — 0 security, 0 license',
    description: 'enabled + no issues at all. Shows green 0/0.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 0,
      licenseIssueCount: 0,
      results: { criticalCount: 0, severeCount: 0, moderateCount: 0, totalCount: 0 },
      reportDate: '2026-03-01',
      reportAge: '4 days ago',
    },
    group: 'Enabled — Clean',
  },
  {
    id: 'clean-with-report',
    label: 'Clean — with report URL',
    description: 'Clean repo with a detailedReport URL. Cell is clickable.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 0,
      licenseIssueCount: 0,
      results: { criticalCount: 0, severeCount: 0, moderateCount: 0, totalCount: 0 },
      detailedReport: REPORT_URL,
      reportDate: '2026-03-01',
      reportAge: '4 days ago',
    },
    group: 'Enabled — Clean',
  },

  // ── Security issues only ───────────────────────────────────────────────────
  {
    id: 'security-critical-only',
    label: 'Security — critical only',
    description: 'criticalCount=8, no license issues.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 8,
      licenseIssueCount: 0,
      results: { criticalCount: 8, severeCount: 0, moderateCount: 0, totalCount: 8 },
      detailedReport: REPORT_URL,
      reportDate: '2026-03-01',
    },
    group: 'Enabled — Security Issues',
  },
  {
    id: 'security-severe-only',
    label: 'Security — severe only',
    description: 'severeCount=15, no critical or license.',
    repository: PROXY_NPM,
    healthStatus: {
      enabled: true,
      securityIssueCount: 15,
      licenseIssueCount: 0,
      results: { criticalCount: 0, severeCount: 15, moderateCount: 0, totalCount: 15 },
      detailedReport: REPORT_URL,
    },
    group: 'Enabled — Security Issues',
  },
  {
    id: 'security-moderate-only',
    label: 'Security — moderate only',
    description: 'moderateCount=3.',
    repository: PROXY_NUGET,
    healthStatus: {
      enabled: true,
      securityIssueCount: 3,
      licenseIssueCount: 0,
      results: { criticalCount: 0, severeCount: 0, moderateCount: 3, totalCount: 3 },
    },
    group: 'Enabled — Security Issues',
  },
  {
    id: 'security-mixed',
    label: 'Security — critical + severe + moderate',
    description: 'All three severity buckets populated. Large counts.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 2758,
      licenseIssueCount: 0,
      results: { criticalCount: 2113, severeCount: 645, moderateCount: 47, totalCount: 2758 },
      detailedReport: REPORT_URL,
      reportDate: '2026-03-04',
      reportAge: '1 day ago',
    },
    group: 'Enabled — Security Issues',
  },

  // ── License issues only ────────────────────────────────────────────────────
  {
    id: 'license-only',
    label: 'License issues only',
    description: 'No security CVEs but license problems. licenseIssueCount=12.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 0,
      licenseIssueCount: 12,
      licenseCopyleftCount: 5,
      licenseNonStandardCount: 4,
      licenseNotProvidedCount: 3,
      results: { criticalCount: 0, severeCount: 0, moderateCount: 0, totalCount: 12 },
      detailedReport: REPORT_URL,
    },
    group: 'Enabled — License Issues',
  },
  {
    id: 'license-copyleft-only',
    label: 'License — copyleft only',
    description: 'Only copyleft license issues.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 0,
      licenseIssueCount: 8,
      licenseCopyleftCount: 8,
      results: { criticalCount: 0, severeCount: 0, moderateCount: 0, totalCount: 8 },
    },
    group: 'Enabled — License Issues',
  },

  // ── Both security and license ──────────────────────────────────────────────
  {
    id: 'both-security-license',
    label: 'Security + License issues',
    description: 'Both security CVEs and license problems.',
    repository: PROXY_MAVEN,
    healthStatus: {
      enabled: true,
      securityIssueCount: 609,
      licenseIssueCount: 47,
      licenseCopyleftCount: 20,
      licenseNonStandardCount: 15,
      licenseNotProvidedCount: 12,
      results: { criticalCount: 2047, severeCount: 609, moderateCount: 13, totalCount: 2656 },
      detailedReport: REPORT_URL,
      reportDate: '2026-03-04',
    },
    group: 'Enabled — Security + License',
  },
  {
    id: 'both-small-counts',
    label: 'Security + License — small counts',
    description: 'Small numbers: 2 security, 1 license.',
    repository: PROXY_NPM,
    healthStatus: {
      enabled: true,
      securityIssueCount: 2,
      licenseIssueCount: 1,
      results: { criticalCount: 2, severeCount: 0, moderateCount: 0, totalCount: 3 },
      detailedReport: REPORT_URL,
    },
    group: 'Enabled — Security + License',
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// Group
// ─────────────────────────────────────────────────────────────────────────────

function groupBy<T>(items: T[], key: (item: T) => string): Map<string, T[]> {
  const map = new Map<string, T[]>();
  items.forEach((item) => {
    const k = key(item);
    const existing = map.get(k) ?? [];
    existing.push(item);
    map.set(k, existing);
  });
  return map;
}

const GROUPED = groupBy(PERMUTATIONS, (p) => p.group ?? 'Other');

const LIVE_URL = `${window.location.origin}${window.location.pathname}?debug#preview/browse`;

export default function SearchHealthCheckTestPage() {
  return (
    <Box className="search-hc-test-page" p="4">
      <Flex align="center" gap="3" mb="4">
        <FlaskConical size={24} color="var(--amber-9)" />
        <Box>
          <Heading size="5">Search / Browse — Health Check Cell Test Harness</Heading>
          <Text size="2" color="amber" weight="medium">
            Sonatype Internal · {PERMUTATIONS.length} permutations
          </Text>
        </Box>
      </Flex>

      <Card mb="5" className="search-hc-test-page__live-section">
        <Flex justify="between" align="center">
          <Box>
            <Heading size="4" mb="1">Live Browse Page</Heading>
            <Text size="2" color="gray">Opens the real browse grid with live Health Check data.</Text>
          </Box>
          <Button variant="solid" color="amber" size="2" asChild style={{ flexShrink: 0 }}>
            <a href={LIVE_URL} target="_blank" rel="noopener noreferrer">
              <ExternalLink size={14} /> Open Browse
            </a>
          </Button>
        </Flex>
      </Card>

      <Text size="2" color="gray" mb="4" as="p">
        Each row shows a HealthCheckCell as it appears in the browse grid. Click any active cell to
        open the Health Check modal. All data is mocked — no server calls.
      </Text>

      {Array.from(GROUPED.entries()).map(([group, items]) => (
        <Box key={group} mb="5">
          <Heading size="3" mb="3" className="search-hc-test-page__group-heading">{group}</Heading>
          <Card>
            <Table.Root variant="surface" size="1">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell style={{ width: 220 }}>Label</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ width: 200 }}>Repository</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ width: 140 }}>HC Cell</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {items.map((p) => (
                  <Table.Row key={p.id} data-testid={`hc-row-${p.id}`}>
                    <Table.Cell>
                      <Flex direction="column" gap="1">
                        <Text size="2" weight="medium">{p.label}</Text>
                        <Badge variant="outline" color="gray" size="1">{p.id}</Badge>
                      </Flex>
                    </Table.Cell>
                    <Table.Cell>
                      <Flex direction="column" gap="1">
                        <Text size="2">{p.repository.name}</Text>
                        <Flex gap="1">
                          <Badge variant="soft" color="gray" size="1">{p.repository.type}</Badge>
                          <Badge variant="soft" color="gray" size="1">{p.repository.format}</Badge>
                        </Flex>
                      </Flex>
                    </Table.Cell>
                    <Table.Cell>
                      <Box className="search-hc-test-page__cell-wrapper">
                        <HealthCheckCell
                          repository={p.repository}
                          healthStatus={p.healthStatus}
                        />
                      </Box>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2" color="gray">{p.description}</Text>
                    </Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          </Card>
        </Box>
      ))}
    </Box>
  );
}
