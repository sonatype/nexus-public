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
 * SearchFirewallTestPage — every permutation of FirewallCell in a browse-grid layout.
 * Click any cell to see the corresponding modal.
 *
 * Access: http://localhost:8081/?debug#preview/test-search-fw
 */

import React from 'react';
import { Box, Flex, Text, Heading, Card, Badge, Button, Table } from '@radix-ui/themes';
import { FlaskConical, ExternalLink } from 'lucide-react';
import { FirewallCell } from '../../../shared/security/FirewallCell';
import type { SecurityStatusData, SecurityRepositoryInfo } from '../../../shared/security/security.types';

import './SearchFirewallTestPage.scss';

// ─────────────────────────────────────────────────────────────────────────────
// Repo fixtures
// ─────────────────────────────────────────────────────────────────────────────

const PROXY_MAVEN: SecurityRepositoryInfo = { name: 'maven-central', type: 'proxy', format: 'maven2' };
const PROXY_NPM: SecurityRepositoryInfo = { name: 'npm-proxy', type: 'proxy', format: 'npm' };
const PROXY_NUGET: SecurityRepositoryInfo = { name: 'nuget-proxy', type: 'proxy', format: 'nuget' };
const PROXY_TERRAFORM: SecurityRepositoryInfo = { name: 'terraform-proxy', type: 'proxy', format: 'terraform' }; // unsupported
const PROXY_HELM: SecurityRepositoryInfo = { name: 'helm-proxy', type: 'proxy', format: 'helm' };              // unsupported
const HOSTED_MAVEN: SecurityRepositoryInfo = { name: 'maven-releases', type: 'hosted', format: 'maven2' };
const GROUP_MAVEN: SecurityRepositoryInfo = { name: 'maven-public', type: 'group', format: 'maven2' };

const REPORT_URL = 'https://smart-repo-non-production.iq.saas.sonatype.dev/ui/links/firewall/repositories/report/maven-central';

// ─────────────────────────────────────────────────────────────────────────────
// FirewallStatus fixtures — all meaningful permutations
// ─────────────────────────────────────────────────────────────────────────────

interface FirewallPermutation {
  id: string;
  label: string;
  description: string;
  repository: SecurityRepositoryInfo;
  firewallStatus?: SecurityStatusData;
  hasFirewallLicense?: boolean;
  proxyProtectionSummary?: { totalProxy: number; protectedProxy: number };
  group?: string;
}

const PERMUTATIONS: FirewallPermutation[] = [
  // ── No data / loading ──────────────────────────────────────────────────────
  {
    id: 'no-data',
    label: 'No status yet',
    description: 'firewallStatus=undefined — shows loading/waiting state (spinner or empty).',
    repository: PROXY_MAVEN,
    firewallStatus: undefined,
    group: 'Proxy — Loading / No Data',
  },

  // ── Unsupported formats ────────────────────────────────────────────────────
  {
    id: 'unsupported-terraform',
    label: 'Terraform proxy (unsupported)',
    description: 'format=terraform is not supported by Firewall. Shows "Not supported" badge.',
    repository: PROXY_TERRAFORM,
    group: 'Proxy — Unsupported Format',
  },
  {
    id: 'unsupported-helm',
    label: 'Helm proxy (unsupported)',
    description: 'format=helm is not supported. Shows "Not supported" badge.',
    repository: PROXY_HELM,
    group: 'Proxy — Unsupported Format',
  },

  // ── Non-proxy repos ────────────────────────────────────────────────────────
  {
    id: 'hosted-all-protected',
    label: 'Hosted — all proxies protected',
    description: 'Hosted repo with Firewall license + all proxies protected. Shows "—" + green modal.',
    repository: HOSTED_MAVEN,
    hasFirewallLicense: true,
    proxyProtectionSummary: { totalProxy: 10, protectedProxy: 10 },
    group: 'Hosted / Group (N/A)',
  },
  {
    id: 'hosted-some-unprotected',
    label: 'Hosted — some proxies unprotected',
    description: 'Hosted repo, some proxies not protected. Shows "—" + amber modal.',
    repository: HOSTED_MAVEN,
    hasFirewallLicense: true,
    proxyProtectionSummary: { totalProxy: 10, protectedProxy: 6 },
    group: 'Hosted / Group (N/A)',
  },
  {
    id: 'hosted-no-license',
    label: 'Hosted — no Firewall license',
    description: 'Hosted repo, no license. Shows "—" + red modal with upsell.',
    repository: HOSTED_MAVEN,
    hasFirewallLicense: false,
    group: 'Hosted / Group (N/A)',
  },
  {
    id: 'group-all-protected',
    label: 'Group — all proxies protected',
    description: 'Group repo, fully protected.',
    repository: GROUP_MAVEN,
    hasFirewallLicense: true,
    proxyProtectionSummary: { totalProxy: 5, protectedProxy: 5 },
    group: 'Hosted / Group (N/A)',
  },

  // ── Clean proxy repos ──────────────────────────────────────────────────────
  {
    id: 'clean-no-report',
    label: 'Clean — no issues, no report URL',
    description: '0 violations, 0 quarantined, no reportUrl. Shows green badge.',
    repository: PROXY_MAVEN,
    firewallStatus: { repositoryName: 'maven-central', affectedComponentCount: 0, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 0 },
    group: 'Proxy — Clean',
  },
  {
    id: 'clean-with-report',
    label: 'Clean — with report URL',
    description: '0 violations but reportUrl present. Shows green badge + clickable.',
    repository: PROXY_MAVEN,
    firewallStatus: { repositoryName: 'maven-central', affectedComponentCount: 0, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 0, reportUrl: REPORT_URL },
    group: 'Proxy — Clean',
  },

  // ── Violations only (quarantine=0) ─────────────────────────────────────────
  {
    id: 'critical-only',
    label: 'Critical violations only',
    description: 'criticalComponentCount=25, rest 0. Shows red cell.',
    repository: PROXY_MAVEN,
    firewallStatus: { repositoryName: 'maven-central', affectedComponentCount: 25, criticalComponentCount: 25, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 0, reportUrl: REPORT_URL },
    group: 'Proxy — Violations (not quarantined)',
  },
  {
    id: 'severe-only',
    label: 'Severe violations only',
    description: 'severeComponentCount=12.',
    repository: PROXY_NPM,
    firewallStatus: { repositoryName: 'npm-proxy', affectedComponentCount: 12, criticalComponentCount: 0, severeComponentCount: 12, moderateComponentCount: 0, quarantinedComponentCount: 0, reportUrl: REPORT_URL },
    group: 'Proxy — Violations (not quarantined)',
  },
  {
    id: 'moderate-only',
    label: 'Moderate violations only',
    description: 'moderateComponentCount=7.',
    repository: PROXY_NUGET,
    firewallStatus: { repositoryName: 'nuget-proxy', affectedComponentCount: 7, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 7, quarantinedComponentCount: 0, reportUrl: REPORT_URL },
    group: 'Proxy — Violations (not quarantined)',
  },
  {
    id: 'mixed-violations',
    label: 'Critical + Severe + Moderate',
    description: 'All three severity buckets populated.',
    repository: PROXY_MAVEN,
    firewallStatus: { repositoryName: 'maven-central', affectedComponentCount: 44, criticalComponentCount: 20, severeComponentCount: 15, moderateComponentCount: 9, quarantinedComponentCount: 0, reportUrl: REPORT_URL },
    group: 'Proxy — Violations (not quarantined)',
  },

  // ── Quarantined ────────────────────────────────────────────────────────────
  {
    id: 'quarantined-only',
    label: 'Quarantined only (no violations)',
    description: 'quarantinedComponentCount=13, no policy violations. Shows quarantined badge.',
    repository: PROXY_NPM,
    firewallStatus: { repositoryName: 'npm-proxy', affectedComponentCount: 13, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 13 },
    group: 'Proxy — Quarantined',
  },
  {
    id: 'quarantined-plus-violations',
    label: 'Quarantined + violations',
    description: 'Both quarantined and policy violations present.',
    repository: PROXY_NPM,
    firewallStatus: { repositoryName: 'npm-proxy', affectedComponentCount: 95, criticalComponentCount: 82, severeComponentCount: 11, moderateComponentCount: 1, quarantinedComponentCount: 13, reportUrl: REPORT_URL },
    group: 'Proxy — Quarantined',
  },
  {
    id: 'quarantined-no-report',
    label: 'Quarantined — no report URL',
    description: 'Quarantined but no reportUrl available.',
    repository: PROXY_NPM,
    firewallStatus: { repositoryName: 'npm-proxy', affectedComponentCount: 5, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 5 },
    group: 'Proxy — Quarantined',
  },

  // ── Error ──────────────────────────────────────────────────────────────────
  {
    id: 'error-iq-offline',
    label: 'Error — IQ Server unreachable',
    description: 'errorMessage set — IQ connection failure.',
    repository: PROXY_MAVEN,
    firewallStatus: { repositoryName: 'maven-central', affectedComponentCount: 0, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 0, errorMessage: 'Unable to connect to IQ Server: Connection refused' },
    group: 'Proxy — Error States',
  },
  {
    id: 'error-audit-running',
    label: 'Error — Audit in progress',
    description: 'Shows "Audit in progress" message from IQ.',
    repository: PROXY_MAVEN,
    firewallStatus: { repositoryName: 'maven-central', affectedComponentCount: 0, criticalComponentCount: 0, severeComponentCount: 0, moderateComponentCount: 0, quarantinedComponentCount: 0, errorMessage: 'Audit in progress' },
    group: 'Proxy — Error States',
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// Group the permutations
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

export default function SearchFirewallTestPage() {
  return (
    <Box className="search-fw-test-page" p="4">
      <Flex align="center" gap="3" mb="4">
        <FlaskConical size={24} color="var(--amber-9)" />
        <Box>
          <Heading size="5">Search / Browse — Firewall Cell Test Harness</Heading>
          <Text size="2" color="amber" weight="medium">
            Sonatype Internal · {PERMUTATIONS.length} permutations
          </Text>
        </Box>
      </Flex>

      <Card mb="5" className="search-fw-test-page__live-section">
        <Flex justify="between" align="center">
          <Box>
            <Heading size="4" mb="1">Live Browse Page</Heading>
            <Text size="2" color="gray">Opens the real browse grid with live Firewall data.</Text>
          </Box>
          <Button variant="solid" color="amber" size="2" asChild style={{ flexShrink: 0 }}>
            <a href={LIVE_URL} target="_blank" rel="noopener noreferrer">
              <ExternalLink size={14} /> Open Browse
            </a>
          </Button>
        </Flex>
      </Card>

      <Text size="2" color="gray" mb="4" as="p">
        Each row shows a FirewallCell as it appears in the browse grid. Click any cell to open the
        corresponding modal. Cells are rendered with mocked props — no server calls made.
      </Text>

      {Array.from(GROUPED.entries()).map(([group, items]) => (
        <Box key={group} mb="5">
          <Heading size="3" mb="3" className="search-fw-test-page__group-heading">{group}</Heading>
          <Card>
            <Table.Root variant="surface" size="1">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell style={{ width: 200 }}>Label</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ width: 220 }}>Repository</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ width: 120 }}>Firewall Cell</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {items.map((p) => (
                  <Table.Row key={p.id} data-testid={`fw-row-${p.id}`}>
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
                      <Box className="search-fw-test-page__cell-wrapper">
                        <FirewallCell
                          repository={p.repository}
                          firewallStatus={p.firewallStatus}
                          hasFirewallLicense={p.hasFirewallLicense ?? true}
                          proxyProtectionSummary={p.proxyProtectionSummary}
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
