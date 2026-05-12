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

import React, { useCallback, useMemo, useState } from 'react';
import { Button, Callout, Flex, Text } from '@radix-ui/themes';
import { AlertTriangle, CheckCircle } from 'lucide-react';

import { FindingsTable } from './FindingsTable';
import type { MaliciousPackagesDataSnapshot } from './useMaliciousPackagesData';

interface RemediateTabProps {
  data: MaliciousPackagesDataSnapshot;
  onNavigateToDetect: () => void;
  repoFilter?: string | null;
  onClearRepoFilter?: () => void;
}

function timeScopePhrase(dateRangeLabel: string): string {
  if (dateRangeLabel === 'all time') {
    return 'over all time';
  }
  return `in the last ${dateRangeLabel}`;
}

export function RemediateTab({ data, onNavigateToDetect, repoFilter, onClearRepoFilter }: RemediateTabProps): React.ReactElement {
  const {
    activeFindings,
    remediateFindings,
    remediateRepository,
    acknowledge,
    bulkAcknowledge,
    fetchFindings,
    malwareCount,
    proxyRepos,
    tasks,
  } = data;
  const [dateRangeLabel, setDateRangeLabel] = useState('30 days');

  const pendingCount = useMemo(
    () => activeFindings.filter((f) => !f.deletedTime && !f.acknowledgedAt).length,
    [activeFindings]
  );
  const remediatedInRange = useMemo(
    () => activeFindings.filter((f) => f.deletedTime != null).length,
    [activeFindings]
  );
  const riskAcceptedInRange = useMemo(
    () => activeFindings.filter((f) => f.acknowledgedAt != null).length,
    [activeFindings]
  );
  const criticalCount = useMemo(
    () =>
      activeFindings.filter(
        (f) => !f.deletedTime && !f.acknowledgedAt && f.threatLevel !== null && f.threatLevel >= 9
      ).length,
    [activeFindings]
  );
  const hasCriticalThreat = criticalCount > 0;
  const blindSpotCount = useMemo(
    () => proxyRepos.filter((r) => r.rhcSupported && !r.rhcEnabled).length,
    [proxyRepos]
  );
  const hasBlindSpots = blindSpotCount > 0;

  const scope = timeScopePhrase(dateRangeLabel);

  const detectLink = useCallback(
    (label: string) => (
      <Button
        variant="ghost"
        size="1"
        style={{
          display: 'inline',
          padding: 0,
          height: 'auto',
          fontWeight: 'bold',
          textDecoration: 'underline',
          cursor: 'pointer',
        }}
        onClick={onNavigateToDetect}
      >
        {label}
      </Button>
    ),
    [onNavigateToDetect]
  );

  // State machine: first match wins (priority 5 > 4 > 3 > 6 > 2 > 1)
  let banner: React.ReactElement;

  if (pendingCount > 0 && hasCriticalThreat) {
    banner = (
      <Callout.Root color="red" data-testid="remediate-banner-critical">
        <Callout.Icon>
          <AlertTriangle size={16} />
        </Callout.Icon>
        <Callout.Text>
          CRITICAL: {pendingCount} malicious {pendingCount === 1 ? 'package requires' : 'packages require'} immediate
          action. {criticalCount} {criticalCount === 1 ? 'is' : 'are'} rated Critical threat level.
          {' '}
          <Text weight="bold">
            → Review each component in Sonatype Guide. Delete the package (recommended) or Accept Risk. Then activate your
            Breach Incident Management process.
          </Text>
        </Callout.Text>
      </Callout.Root>
    );
  } else if (pendingCount > 0) {
    banner = (
      <Callout.Root color="red" data-testid="remediate-banner-pending">
        <Callout.Icon>
          <AlertTriangle size={16} />
        </Callout.Icon>
        <Callout.Text>
          {pendingCount} malicious {pendingCount === 1 ? 'package requires' : 'packages require'} remediation.
          {' '}
          <Text weight="bold">
            → For each finding: open in Sonatype Guide to assess impact, then Delete (recommended) or Accept Risk.
          </Text>
        </Callout.Text>
      </Callout.Root>
    );
  } else if (malwareCount > 0) {
    banner = (
      <Callout.Root color="red" data-testid="remediate-banner-signatures-pending">
        <Callout.Icon>
          <AlertTriangle size={16} />
        </Callout.Icon>
        <Callout.Text>
          {malwareCount} malicious package {malwareCount === 1 ? 'signature' : 'signatures'} detected but not yet
          identified.
          {' '}
          <Text weight="bold">
            → Go to the {detectLink('Detect tab')} and run Deep Scan on flagged repositories to identify the exact
            packages before you can remediate.
          </Text>
        </Callout.Text>
      </Callout.Root>
    );
  } else if (malwareCount === 0 && pendingCount === 0 && remediatedInRange === 0 && riskAcceptedInRange === 0 && hasBlindSpots) {
    banner = (
      <Callout.Root color="amber" data-testid="remediate-banner-blind-spots">
        <Callout.Icon>
          <AlertTriangle size={16} />
        </Callout.Icon>
        <Callout.Text>
          No malicious packages found in monitored repositories, but{' '}
          <Text weight="bold">
            {blindSpotCount} {blindSpotCount === 1 ? 'repository is' : 'repositories are'} unmonitored
          </Text>{' '}
          — threats may exist undetected.
          {' '}
          <Text weight="bold">
            → Enable detection on all repositories in the {detectLink('Detect tab')}. You cannot protect what you cannot
            see.
          </Text>
        </Callout.Text>
      </Callout.Root>
    );
  } else if (remediatedInRange > 0 || riskAcceptedInRange > 0) {
    const riskPart = riskAcceptedInRange > 0 ? ` and ${riskAcceptedInRange} risk-accepted` : '';
    banner = (
      <Callout.Root color="green" data-testid="remediate-banner-clear-history">
        <Callout.Icon>
          <CheckCircle size={16} />
        </Callout.Icon>
        <Callout.Text>
          No malicious packages pending remediation. {remediatedInRange} malicious{' '}
          {remediatedInRange === 1 ? 'package was' : 'packages were'} removed
          {riskPart} {scope}.
          {' '}
          <Text weight="bold">
            Ensure all repositories have Quarantine enabled to block future threats.
          </Text>
        </Callout.Text>
      </Callout.Root>
    );
  } else {
    banner = (
      <Callout.Root color="green" data-testid="remediate-banner-all-clear">
        <Callout.Icon>
          <CheckCircle size={16} />
        </Callout.Icon>
        <Callout.Text>
          No malicious packages pending remediation. No malicious packages have been identified or removed from your
          proxies {scope}.
          {' '}
          <Text weight="bold">
            Ensure all repositories have Quarantine enabled to block future threats.
          </Text>
        </Callout.Text>
      </Callout.Root>
    );
  }

  return (
    <Flex direction="column" gap="4">
      {banner}
      <FindingsTable
        onRemediateFindings={remediateFindings}
        onRemediateRepository={remediateRepository}
        onAcknowledge={acknowledge}
        onBulkAcknowledge={bulkAcknowledge}
        fetchFindings={fetchFindings}
        signatureCount={malwareCount}
        tasks={tasks}
        onDateRangeChange={setDateRangeLabel}
        repoFilter={repoFilter}
        onClearRepoFilter={onClearRepoFilter}
      />
    </Flex>
  );
}
