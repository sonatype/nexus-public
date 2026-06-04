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
import { Badge, Box, Button, Callout, Card, Flex, Grid, Heading, Text } from '@radix-ui/themes';
import { AlertTriangle, ExternalLink, Shield } from 'lucide-react';

import type { MaliciousPackagesDataSnapshot } from './useMaliciousPackagesData';
import type { TabId } from './types';

interface OverviewTabProps {
  data: MaliciousPackagesDataSnapshot;
  hasFirewall: boolean;
  detectCount: number;
  hardenGapCount: number;
  protectedRepoCount: number;
  totalRepoCount: number;
  onNavigate: (tab: TabId) => void;
}

function MaliciousVsVulnerableEducation() {
  return (
    <Grid columns="2" gap="4">
      <Card style={{ borderColor: 'var(--red-6)', borderWidth: 1, borderStyle: 'solid' }}>
        <Flex direction="column" gap="2">
          <Heading size="3" color="red">Malicious Packages (This Page)</Heading>
          <Text size="2" weight="bold">Purpose-built to attack you.</Text>
          <Text size="2">
            Typosquats, backdoors, credential stealers targeting developers and CI/CD pipelines.
          </Text>
          <Callout.Root color="red" size="1">
            <Callout.Text>
              <Text size="1" weight="bold">Executes on install.</Text>{' '}
              <Text size="1">
                By the time your CI/CD SCA scanner flags it, the attack has already run.{' '}
                <code>npm install</code> = game over.
              </Text>
            </Callout.Text>
          </Callout.Root>
          <Text size="1" weight="bold" color="red">
            Response: Delete immediately. Investigate blast radius. Activate incident management.
          </Text>
        </Flex>
      </Card>
      <Card style={{ borderColor: 'var(--blue-6)', borderWidth: 1, borderStyle: 'solid' }}>
        <Flex direction="column" gap="2">
          <Heading size="3" color="blue">Vulnerable Components (Health Check)</Heading>
          <Text size="2" weight="bold">Legitimate software with security bugs.</Text>
          <Text size="2">
            CVEs, misconfigurations — the authors did not intend harm.
          </Text>
          <Callout.Root color="blue" size="1">
            <Callout.Text>
              <Text size="1" weight="bold">Found by scanning.</Text>{' '}
              <Text size="1">SCA tools in CI/CD catch these. You have time to plan upgrades.</Text>
            </Callout.Text>
          </Callout.Root>
          <Text size="1" weight="bold" color="blue">
            Response: Upgrade to patched version. Prioritize by severity.
          </Text>
        </Flex>
      </Card>
    </Grid>
  );
}

function NoFirewallOverview({ data, onNavigate }: { data: MaliciousPackagesDataSnapshot; onNavigate: (tab: TabId) => void }) {
  const hasMalware = data.malwareCount > 0 || data.activeFindings.some((f) => !f.deletedTime && !f.acknowledgedAt);

  return (
    <Flex direction="column" gap="4" data-testid="overview-no-firewall">
      <Callout.Root color="amber" data-testid="firewall-not-enabled-warning">
        <Callout.Icon><AlertTriangle size={16} /></Callout.Icon>
        <Callout.Text>
          <Text weight="bold">Your repositories are not protected from malicious packages, zero-day attacks, or risky dependencies.</Text>{' '}
          Keep bad code out of your repository. Repository Firewall blocks malicious and vulnerable components
          at the moment they are requested — before they ever enter your repo. No manual cleanup, no surprises
          in builds, just trusted dependencies from the start.{' '}
          <a href="https://links.sonatype.com/nexus-repository-firewall" target="_blank" rel="noopener noreferrer" style={{ fontWeight: 600 }}>
            Learn more about Sonatype Firewall
          </a>
        </Callout.Text>
      </Callout.Root>
      {hasMalware && (
        <Callout.Root color="amber">
          <Callout.Icon><AlertTriangle size={16} /></Callout.Icon>
          <Callout.Text>
            <Text weight="bold">Malicious packages detected in your repositories.</Text>{' '}
            These are not the same as vulnerable components. Malicious packages are purpose-built attacks that execute
            on download — before any scanner can catch them.
          </Callout.Text>
        </Callout.Root>
      )}

      <MaliciousVsVulnerableEducation />

      <Card>
        <Flex direction="column" gap="3">
          <Heading size="4">What To Do Now</Heading>
          <Text size="2" color="gray">Without Repository Firewall, remediation is manual. Follow these steps for each finding:</Text>
          <Flex direction="column" gap="3">
            <Flex gap="3" align="start">
              <Badge size="2" color="blue" variant="solid" radius="full" style={{ minWidth: 28, minHeight: 28, justifyContent: 'center' }}>1</Badge>
              <Box>
                <Text size="2" weight="bold">Enable Detection</Text>
                <Text as="p" size="2" color="gray">
                  Go to the{' '}
                  <Text weight="bold" style={{ cursor: 'pointer', textDecoration: 'underline' }} onClick={() => onNavigate('detect')}>Detect tab</Text>.
                  Enable Repository Health Check on all proxy repositories to identify malicious package signatures.
                </Text>
              </Box>
            </Flex>
            <Flex gap="3" align="start">
              <Badge size="2" color="blue" variant="solid" radius="full" style={{ minWidth: 28, minHeight: 28, justifyContent: 'center' }}>2</Badge>
              <Box>
                <Text size="2" weight="bold">Review Each Finding in Sonatype Guide</Text>
                <Text as="p" size="2" color="gray">
                  Go to the{' '}
                  <Text weight="bold" style={{ cursor: 'pointer', textDecoration: 'underline' }} onClick={() => onNavigate('remediate')}>Remediate tab</Text>.
                  For each malicious package, open in Sonatype Guide to understand the threat, blast radius, and impact.
                </Text>
              </Box>
            </Flex>
            <Flex gap="3" align="start">
              <Badge size="2" color="red" variant="solid" radius="full" style={{ minWidth: 28, minHeight: 28, justifyContent: 'center' }}>3</Badge>
              <Box>
                <Text size="2" weight="bold">Delete the Component (Recommended)</Text>
                <Text as="p" size="2" color="gray">
                  Delete the malicious package from your repository. If you cannot delete immediately, Accept Risk with justification.
                </Text>
              </Box>
            </Flex>
            <Flex gap="3" align="start">
              <Badge size="2" color="red" variant="solid" radius="full" style={{ minWidth: 28, minHeight: 28, justifyContent: 'center' }}>4</Badge>
              <Box>
                <Text size="2" weight="bold">Activate Breach Incident Management</Text>
                <Text as="p" size="2" color="gray">
                  Follow your organization's incident response process. Determine if the malicious package executed, what data was exposed, and who is affected.
                </Text>
              </Box>
            </Flex>
          </Flex>
        </Flex>
      </Card>

      <Card style={{ background: 'var(--violet-2)', borderColor: 'var(--violet-6)', borderWidth: 1, borderStyle: 'solid' }}>
        <Flex direction="column" gap="2">
          <Flex align="center" gap="2">
            <Shield size={18} color="var(--violet-9)" />
            <Heading size="3" color="violet">Want automated protection?</Heading>
          </Flex>
          <Text size="2" color="violet">
            <Text weight="bold">Repository Firewall</Text> blocks malicious packages at the proxy before they reach your
            developers — automated detection, quarantine, and remediation. No manual cleanup needed.
          </Text>
          <Button size="2" variant="outline" color="violet" asChild style={{ alignSelf: 'flex-start' }}>
            <a href="https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall" target="_blank" rel="noopener noreferrer">
              Learn about Repository Firewall <ExternalLink size={14} />
            </a>
          </Button>
        </Flex>
      </Card>
    </Flex>
  );
}

function FirewallOverview({
  data, detectCount, hardenGapCount, protectedRepoCount, totalRepoCount, onNavigate,
}: {
  data: MaliciousPackagesDataSnapshot;
  detectCount: number;
  hardenGapCount: number;
  protectedRepoCount: number;
  totalRepoCount: number;
  onNavigate: (tab: TabId) => void;
}) {
  const pendingCount = data.activeFindings.filter((f) => !f.deletedTime && !f.acknowledgedAt).length;

  return (
    <Flex direction="column" gap="4" data-testid="overview-firewall">
      <Grid columns="3" gap="3">
        <Card style={{ borderColor: pendingCount > 0 ? 'var(--red-6)' : 'var(--green-6)', borderWidth: 1, borderStyle: 'solid', textAlign: 'center' }}>
          <Flex direction="column" align="center" gap="1" py="2">
            <Text size="7" weight="bold" color={pendingCount > 0 ? 'red' : 'green'}>{pendingCount}</Text>
            <Text size="1" weight="medium">Malicious Packages</Text>
            <Text size="1" color="gray">{pendingCount > 0 ? 'Pending remediation' : 'All clear'}</Text>
          </Flex>
        </Card>
        <Card style={{ borderColor: hardenGapCount > 0 ? 'var(--amber-6)' : 'var(--green-6)', borderWidth: 1, borderStyle: 'solid', textAlign: 'center' }}>
          <Flex direction="column" align="center" gap="1" py="2">
            <Text size="7" weight="bold" color={hardenGapCount > 0 ? 'amber' : 'green'}>{hardenGapCount}</Text>
            <Text size="1" weight="medium">Repos Need Hardening</Text>
            <Text size="1" color="gray">{hardenGapCount > 0 ? 'Protection gaps detected' : 'Fully hardened'}</Text>
          </Flex>
        </Card>
        <Card style={{ borderColor: 'var(--green-6)', borderWidth: 1, borderStyle: 'solid', textAlign: 'center' }}>
          <Flex direction="column" align="center" gap="1" py="2">
            <Text size="7" weight="bold" color="green">{protectedRepoCount}</Text>
            <Text size="1" weight="medium">Repos Fully Protected</Text>
            <Text size="1" color="gray">Firewall quarantine active</Text>
          </Flex>
        </Card>
      </Grid>

      <MaliciousVsVulnerableEducation />

      <Card>
        <Flex direction="column" gap="3">
          <Flex align="center" gap="2">
            <Heading size="4">Your Protection Workflow</Heading>
            <Badge color="green" variant="soft">Firewall Active</Badge>
          </Flex>
          <Text size="2" color="gray">You have Repository Firewall — the best automated tools for malicious package protection.</Text>
          <Grid columns="3" gap="3">
            <Card>
              <Flex direction="column" gap="2">
                <Badge color="blue" variant="solid" size="1" style={{ alignSelf: 'flex-start' }}>DETECT</Badge>
                <Text size="2" weight="bold">Identify threats</Text>
                <Text size="1" color="gray">RHC scans for malicious signatures. Deep Scan identifies exact packages.</Text>
                {detectCount > 0 ? (
                  <Button size="1" variant="soft" color="red" onClick={() => onNavigate('detect')}>
                    {detectCount} {detectCount === 1 ? 'repo needs' : 'repos need'} attention
                  </Button>
                ) : (
                  <Text size="1" color="green">All clear — no threats detected</Text>
                )}
              </Flex>
            </Card>
            <Card>
              <Flex direction="column" gap="2">
                <Badge color="red" variant="solid" size="1" style={{ alignSelf: 'flex-start' }}>REMEDIATE</Badge>
                <Text size="2" weight="bold">Clean it up</Text>
                <Text size="1" color="gray">Review in Sonatype Guide. Delete (recommended) or Accept Risk. Activate incident response.</Text>
                {pendingCount > 0 ? (
                  <Button size="1" variant="soft" color="red" onClick={() => onNavigate('remediate')}>
                    {pendingCount} packages pending
                  </Button>
                ) : (
                  <Text size="1" color="green">No packages pending</Text>
                )}
              </Flex>
            </Card>
            <Card>
              <Flex direction="column" gap="2">
                <Badge color="amber" variant="solid" size="1" style={{ alignSelf: 'flex-start' }}>HARDEN</Badge>
                <Text size="2" weight="bold">Prevent re-infection</Text>
                <Text size="1" color="gray">Enable Quarantine mode on all repos. Audit mode logs threats but <Text weight="bold">still lets malicious packages through</Text>.</Text>
                {hardenGapCount > 0 ? (
                  <Button size="1" variant="soft" color="amber" onClick={() => onNavigate('harden')}>
                    {hardenGapCount} repos have gaps
                  </Button>
                ) : (
                  <Text size="1" color="green">All repos hardened</Text>
                )}
              </Flex>
            </Card>
          </Grid>
        </Flex>
      </Card>

      <Callout.Root color="amber">
        <Callout.Icon><AlertTriangle size={16} /></Callout.Icon>
        <Callout.Text>
          <Text weight="bold">Audit mode does not stop malicious packages.</Text>{' '}
          Repositories in Audit mode log malicious packages but still serve them to developers.
          Only Quarantine mode blocks downloads. Sonatype's detection has a {'<'}0.01% false positive rate —
          Quarantine mode will not disrupt your developers.{' '}
          <Text weight="bold" style={{ cursor: 'pointer', textDecoration: 'underline' }} onClick={() => onNavigate('harden')}>
            Switch repos to Quarantine in the Harden tab →
          </Text>
        </Callout.Text>
      </Callout.Root>
    </Flex>
  );
}

export function OverviewTab({
  data, hasFirewall, detectCount, hardenGapCount, protectedRepoCount, totalRepoCount, onNavigate,
}: OverviewTabProps): React.ReactElement {
  if (!hasFirewall) {
    return <NoFirewallOverview data={data} onNavigate={onNavigate} />;
  }
  return (
    <FirewallOverview
      data={data}
      detectCount={detectCount}
      hardenGapCount={hardenGapCount}
      protectedRepoCount={protectedRepoCount}
      totalRepoCount={totalRepoCount}
      onNavigate={onNavigate}
    />
  );
}
