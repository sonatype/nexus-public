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
 * FirewallProSummaryTab - Summary page for Firewall Pro (Yellowfin) protection.
 *
 * This tab is shown when the repository is protected by Firewall Pro (Yellowfin)
 * instead of Firewall Enterprise (IQ Server). It displays:
 * - Protection status (protected via Yellowfin proxy)
 * - Malware blocked count (from in-memory cache, TBD: Yellowfin API)
 * - Recent blocked requests
 *
 * Note: Metrics are currently stored in an in-memory cache. Future work will
 * integrate with Yellowfin API to fetch real metrics (see bead f8pj).
 */

import React, { useState, useEffect } from 'react';
import { Box, Flex, Text, Card, Badge, Button, Table, Callout, Separator } from '@radix-ui/themes';
import {
  Shield,
  ShieldCheck,
  ShieldAlert,
  ShieldOff,
  AlertTriangle,
  ExternalLink,
  RefreshCw,
  Clock,
  Ban,
  Info,
} from 'lucide-react';

import { useFirewallTier, isFirewallProSupportedFormat } from '../../../../../shared/security/firewallTier';
import { LoadingState, ErrorState } from '../../../../../shared';

import './FirewallProSummaryTab.scss';

export interface FirewallProSummaryTabProps {
  repositoryName: string;
  repositoryFormat?: string;
}

interface YellowfinMetrics {
  isProtected: boolean;
  proxyUrl: string | null;
  totalRequests: number;
  blockedRequests: number;
  lastBlockedAt: string | null;
  recentBlocks: Array<{
    timestamp: string;
    purl: string;
    reason: string;
    clientIp: string;
  }>;
}

/**
 * Get Yellowfin metrics from localStorage cache.
 * TBD: Replace with Yellowfin API call (bead f8pj).
 */
function getYellowfinMetrics(repositoryName: string): YellowfinMetrics {
  const defaultMetrics: YellowfinMetrics = {
    isProtected: false,
    proxyUrl: null,
    totalRequests: 0,
    blockedRequests: 0,
    lastBlockedAt: null,
    recentBlocks: [],
  };

  try {
    const configStr = localStorage.getItem('YELLOWFIN_CONFIG');
    if (!configStr) return defaultMetrics;

    const config = JSON.parse(configStr);
    if (!config.enabled || !config.url) return defaultMetrics;

    // Check if this repo is configured to use Yellowfin
    const repoConfig = config.repositories?.[repositoryName];
    if (!repoConfig?.enabled) return defaultMetrics;

    // Get metrics from cache (TBD: replace with API call)
    const metricsStr = localStorage.getItem(`YELLOWFIN_METRICS_${repositoryName}`);
    const metrics = metricsStr ? JSON.parse(metricsStr) : {};

    return {
      isProtected: true,
      proxyUrl: config.url,
      totalRequests: metrics.totalRequests ?? 0,
      blockedRequests: metrics.blockedRequests ?? 0,
      lastBlockedAt: metrics.lastBlockedAt ?? null,
      recentBlocks: metrics.recentBlocks ?? [],
    };
  } catch {
    return defaultMetrics;
  }
}

function MetricCard({
  label,
  value,
  icon: Icon,
  color = 'gray',
}: {
  label: string;
  value: string | number;
  icon: React.ComponentType<{ size?: number }>;
  color?: 'gray' | 'green' | 'red' | 'amber';
}) {
  return (
    <Card className="firewall-pro-summary__metric-card">
      <Flex align="center" gap="3">
        <Box className={`firewall-pro-summary__metric-icon firewall-pro-summary__metric-icon--${color}`}>
          <Icon size={20} />
        </Box>
        <Box>
          <Text size="6" weight="bold" className="firewall-pro-summary__metric-value">
            {typeof value === 'number' ? value.toLocaleString() : value}
          </Text>
          <Text size="2" color="gray">
            {label}
          </Text>
        </Box>
      </Flex>
    </Card>
  );
}

export function FirewallProSummaryTab({
  repositoryName,
  repositoryFormat,
}: FirewallProSummaryTabProps): JSX.Element {
  const firewallTier = useFirewallTier();
  const [metrics, setMetrics] = useState<YellowfinMetrics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    // Simulate async load (TBD: replace with actual API call)
    const timer = setTimeout(() => {
      setMetrics(getYellowfinMetrics(repositoryName));
      setLoading(false);
    }, 300);
    return () => clearTimeout(timer);
  }, [repositoryName]);

  const handleRefresh = () => {
    setLoading(true);
    setTimeout(() => {
      setMetrics(getYellowfinMetrics(repositoryName));
      setLoading(false);
    }, 300);
  };

  // Check if format is supported by Firewall Pro
  if (repositoryFormat && !isFirewallProSupportedFormat(repositoryFormat)) {
    return (
      <Box className="firewall-pro-summary" data-testid="firewall-pro-summary-unsupported">
        <Flex direction="column" align="center" justify="center" gap="4" py="9">
          <ShieldOff size={48} color="var(--gray-8)" />
          <Text size="5" weight="medium" color="gray">
            Firewall Pro Not Available
          </Text>
          <Text size="2" color="gray" align="center" style={{ maxWidth: 400 }}>
            Firewall Pro (Yellowfin) only supports npm, nuget, pypi, and maven2 formats.
            The {repositoryFormat} format is not supported.
          </Text>
          <Button
            variant="soft"
            onClick={() => window.open('https://links.sonatype.com/firewall-pro-formats', '_blank')}
          >
            <ExternalLink size={14} />
            Learn More
          </Button>
        </Flex>
      </Box>
    );
  }

  // Show upgrade prompt if not on Firewall Pro
  if (firewallTier.tier === 'none') {
    return (
      <Box className="firewall-pro-summary" data-testid="firewall-pro-summary-upgrade">
        <Flex direction="column" align="center" justify="center" gap="4" py="9">
          <Shield size={48} color="var(--gray-8)" />
          <Text size="5" weight="medium" color="gray">
            Firewall Protection Not Configured
          </Text>
          <Text size="2" color="gray" align="center" style={{ maxWidth: 400 }}>
            Keep bad code out of your repository. Repository Firewall blocks malicious and
            vulnerable components at the moment they're requested—before they ever enter your repo.
          </Text>
          <Flex gap="3">
            <Button
              variant="soft"
              onClick={() => window.location.hash = '#preview/admin/iq'}
            >
              Configure Firewall
            </Button>
            <Button
              variant="ghost"
              onClick={() => window.open('https://links.sonatype.com/firewall-pro', '_blank')}
            >
              <ExternalLink size={14} />
              Learn More
            </Button>
          </Flex>
        </Flex>
      </Box>
    );
  }

  // Show Enterprise redirect if on Enterprise tier
  if (firewallTier.tier === 'enterprise') {
    return (
      <Box className="firewall-pro-summary" data-testid="firewall-pro-summary-enterprise">
        <Callout.Root color="blue">
          <Callout.Icon>
            <Info size={16} />
          </Callout.Icon>
          <Callout.Text>
            This repository is protected by <strong>Firewall Enterprise</strong> via IQ Server.
            View the full Firewall Report for detailed policy violations and quarantine status.
          </Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  if (loading) {
    return (
      <Box className="firewall-pro-summary">
        <LoadingState message="Loading Firewall Pro metrics..." />
      </Box>
    );
  }

  if (!metrics) {
    return (
      <Box className="firewall-pro-summary">
        <ErrorState
          title="Unable to Load Metrics"
          message="Could not load Firewall Pro metrics for this repository."
          icon={AlertTriangle}
          action={{ label: 'Retry', onClick: handleRefresh }}
        />
      </Box>
    );
  }

  return (
    <Box className="firewall-pro-summary" data-testid="firewall-pro-summary">
      {/* Header */}
      <Flex justify="between" align="center" mb="4">
        <Flex align="center" gap="3">
          <ShieldCheck size={24} color="var(--green-9)" />
          <Box>
            <Text size="5" weight="bold">Firewall Pro Protection</Text>
            <Text size="2" color="gray">Malware protection via Yellowfin proxy</Text>
          </Box>
        </Flex>
        <Button variant="ghost" size="2" onClick={handleRefresh}>
          <RefreshCw size={16} />
          Refresh
        </Button>
      </Flex>

      {/* TBD Notice */}
      <Callout.Root color="amber" mb="4">
        <Callout.Icon>
          <AlertTriangle size={16} />
        </Callout.Icon>
        <Callout.Text>
          <strong>TBD:</strong> Metrics are currently stored locally. Future versions will fetch
          real-time data from the Yellowfin API.
        </Callout.Text>
      </Callout.Root>

      {/* Status Card */}
      <Card mb="4">
        <Flex justify="between" align="center" p="3">
          <Flex align="center" gap="3">
            {metrics.isProtected ? (
              <Badge color="green" size="2">
                <ShieldCheck size={14} />
                Protected
              </Badge>
            ) : (
              <Badge color="gray" size="2">
                <ShieldOff size={14} />
                Not Protected
              </Badge>
            )}
            {metrics.proxyUrl && (
              <Text size="2" color="gray">
                Proxy: <code>{metrics.proxyUrl}</code>
              </Text>
            )}
          </Flex>
          {!metrics.isProtected && (
            <Button
              variant="soft"
              size="2"
              onClick={() => window.location.hash = '#preview/admin/iq'}
            >
              Enable Protection
            </Button>
          )}
        </Flex>
      </Card>

      {/* Metrics Grid */}
      <Flex gap="4" mb="5" wrap="wrap">
        <MetricCard
          label="Total Requests"
          value={metrics.totalRequests}
          icon={RefreshCw}
          color="gray"
        />
        <MetricCard
          label="Malware Blocked"
          value={metrics.blockedRequests}
          icon={Ban}
          color={metrics.blockedRequests > 0 ? 'red' : 'green'}
        />
        <MetricCard
          label="Last Blocked"
          value={metrics.lastBlockedAt ? new Date(metrics.lastBlockedAt).toLocaleDateString() : 'Never'}
          icon={Clock}
          color="gray"
        />
      </Flex>

      <Separator size="4" mb="4" />

      {/* Recent Blocks Table */}
      <Box>
        <Text size="4" weight="medium" mb="3">
          Recent Blocked Requests
        </Text>

        {metrics.recentBlocks.length === 0 ? (
          <Card>
            <Flex direction="column" align="center" justify="center" gap="3" py="6">
              <ShieldCheck size={32} color="var(--green-9)" />
              <Text size="3" color="gray">No malware blocked yet</Text>
              <Text size="2" color="gray">
                All requests to this repository have been clean.
              </Text>
            </Flex>
          </Card>
        ) : (
          <Table.Root variant="surface">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Timestamp</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Package</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Reason</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Client IP</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {metrics.recentBlocks.map((block, idx) => (
                <Table.Row key={idx}>
                  <Table.Cell>
                    <Text size="2">{new Date(block.timestamp).toLocaleString()}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <code>{block.purl}</code>
                  </Table.Cell>
                  <Table.Cell>
                    <Badge color="red" size="1">{block.reason}</Badge>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2" color="gray">{block.clientIp}</Text>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        )}
      </Box>
    </Box>
  );
}

export default FirewallProSummaryTab;
