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
 * HealthCheckStatusCard - Dashboard banner for Repository Health Check (RHC) status.
 *
 * RHC is available to 100% of instances - it's enabled PER PROXY REPO, not globally.
 *
 * Shows:
 * - "X of Y proxy repos protected by Health Check"
 * - Color coding based on percentage:
 *   - RED: <50% repos have RHC enabled
 *   - YELLOW: 50-99% repos have RHC enabled
 *   - GREEN: 100% repos have RHC enabled
 * - CTA: "Enable on N remaining repos" links to Protect module
 *
 * This card is RHC-only. Firewall data is in FirewallStatusCard (MalwareStatusCard).
 */

import React from 'react';
import { Box, Flex, Text, Card, Button, Skeleton } from '@radix-ui/themes';
import { ShieldCheck, ShieldAlert, Shield } from 'lucide-react';
import { useHealthCheckSummary } from './useHealthCheckSummary';

import './HealthCheckStatusCard.scss';

const PROTECT_HREF = '#preview/browse/malwarerisk';
const BROWSE_HREF = '#preview/browse';

function RhcIcon({ color }: { color: 'green' | 'red' | 'amber' | 'gray' }) {
  const size = 22;
  if (color === 'green') return <ShieldCheck size={size} color="var(--green-9)" />;
  if (color === 'red') return <ShieldAlert size={size} color="var(--red-9)" />;
  if (color === 'amber') return <Shield size={size} color="var(--amber-9)" />;
  return <Shield size={size} color="var(--gray-8)" />;
}

/**
 * Determine color based on RHC coverage percentage.
 * - RED: <50% repos have RHC enabled
 * - YELLOW (amber): 50-99% repos have RHC enabled
 * - GREEN: 100% repos have RHC enabled
 * - GRAY: No proxy repos exist
 */
function getColorByPercentage(enabledCount: number, totalCount: number): 'green' | 'red' | 'amber' | 'gray' {
  if (totalCount === 0) return 'gray';
  const percentage = (enabledCount / totalCount) * 100;
  if (percentage >= 100) return 'green';
  if (percentage >= 50) return 'amber';
  return 'red';
}

export default function HealthCheckStatusCard() {
  const rhc = useHealthCheckSummary();

  if (rhc.loading) {
    return (
      <Card className="nxrm-health-check-status-card" size="1" aria-busy="true" aria-label="Loading Health Check">
        <Flex align="center" justify="between" gap="3">
          <Flex align="center" gap="3" style={{ flex: 1, minWidth: 0 }}>
            <Skeleton width={22} height={22} style={{ borderRadius: 4 }} />
            <Box className="nxrm-health-check-status-card__content" style={{ flex: 1, minWidth: 0 }}>
              <Skeleton width={140} height={20} mb="2" />
              <Skeleton width="min(100%, 320px)" height={16} />
            </Box>
          </Flex>
          <Flex gap="2" wrap="wrap" justify="end">
            <Skeleton width={96} height={28} />
            <Skeleton width={120} height={28} />
          </Flex>
        </Flex>
      </Card>
    );
  }

  if (rhc.error) {
    return (
      <Card className="nxrm-health-check-status-card" size="1">
        <Flex align="center" justify="between" gap="3" wrap="wrap">
          <Text size="2" color="red">
            {rhc.error}
          </Text>
          <Button variant="soft" size="1" onClick={() => rhc.refetch()}>
            Retry
          </Button>
        </Flex>
      </Card>
    );
  }

  // Calculate coverage percentage
  const percentage = rhc.totalProxyCount > 0
    ? Math.round((rhc.enabledCount / rhc.totalProxyCount) * 100)
    : 0;
  const remainingRepos = rhc.totalProxyCount - rhc.enabledCount;

  // Derive color based on percentage
  const color = getColorByPercentage(rhc.enabledCount, rhc.totalProxyCount);

  // Build status text
  let statusText = '';
  let detailLine = '';

  if (rhc.totalProxyCount === 0) {
    statusText = 'No proxy repositories';
    detailLine = 'Create proxy repos to enable Health Check scanning';
  } else {
    statusText = `${rhc.enabledCount} of ${rhc.totalProxyCount} eligible proxy repos have Health Check`;
    if (rhc.totalSecurityIssues > 0 || rhc.totalLicenseIssues > 0) {
      const parts: string[] = [];
      if (rhc.totalSecurityIssues > 0) {
        parts.push(`${rhc.totalSecurityIssues.toLocaleString()} security issue${rhc.totalSecurityIssues !== 1 ? 's' : ''}`);
      }
      if (rhc.totalLicenseIssues > 0) {
        parts.push(`${rhc.totalLicenseIssues.toLocaleString()} license issue${rhc.totalLicenseIssues !== 1 ? 's' : ''}`);
      }
      detailLine = parts.join(' · ');
    } else if (rhc.enabledCount === rhc.totalProxyCount) {
      detailLine = 'All eligible repos scanning · No issues detected';
    } else {
      detailLine = `${percentage}% coverage`;
    }
  }

  // CTA button text
  const ctaText = remainingRepos > 0
    ? `Enable on ${remainingRepos} remaining repo${remainingRepos !== 1 ? 's' : ''}`
    : 'Manage Protection';

  return (
    <Card
      className="nxrm-health-check-status-card"
      size="1"
      style={{
        borderLeft: `4px solid var(--${color === 'amber' ? 'amber' : color}-9)`,
      }}
    >
      <Flex align="center" justify="between" gap="3">
        <Flex align="center" gap="3">
          <RhcIcon color={color} />
          <Box className="nxrm-health-check-status-card__content">
            <Flex align="center" gap="2" mb="1">
              <Text size="3" weight="medium">Health Check</Text>
            </Flex>
            <Text size="2" color="gray" className="nxrm-health-check-status-card__lines">
              {statusText}
              {detailLine && ` · ${detailLine}`}
            </Text>
          </Box>
        </Flex>
        <Flex gap="2" wrap="wrap" justify="end">
          {rhc.enabledCount > 0 && (
            <Button
              variant="ghost"
              size="1"
              onClick={() => {
                window.location.hash = BROWSE_HREF;
              }}
            >
              View in Browse
            </Button>
          )}
          {rhc.totalProxyCount > 0 && (
            <Button
              variant="soft"
              size="1"
              onClick={() => {
                window.location.hash = PROTECT_HREF;
              }}
            >
              {ctaText}
            </Button>
          )}
        </Flex>
      </Flex>
    </Card>
  );
}
