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
import { Box, Flex, Text, Card, Grid, Separator, Badge, Tooltip } from '@radix-ui/themes';
import {
  HardDrive,
  FileBox,
  Database,
  ShieldCheck,
  Activity,
  Copy,
  Check,
  Info,
} from 'lucide-react';
import { ensureTrailingSlash } from '../../../../../../../utils/url';
import type {
  RepositoryProfileData,
  RepositoryMetrics,
  HealthCheckData,
  FirewallData,
  MalwareCleanupSummary,
  BlobStoreInfo,
} from '../types';

export interface UsageTabProps {
  repository: RepositoryProfileData;
  metrics: RepositoryMetrics | null;
  healthCheck: HealthCheckData | null;
  firewall: FirewallData | null;
  malwareCleanupSummary: MalwareCleanupSummary | null;
  blobStore: BlobStoreInfo | null;
}

function formatBytes(bytes: number | undefined | null): string {
  if (bytes === undefined || bytes === null || bytes < 0) return '\u2014';
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

function formatTimestamp(epoch: number | undefined): string | null {
  if (!epoch) return null;
  try {
    const ms = epoch < 1e12 ? epoch * 1000 : epoch;
    return new Date(ms).toLocaleString(undefined, {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: 'numeric', minute: '2-digit',
    });
  } catch {
    return null;
  }
}

function deriveProtectionLabel(fw: FirewallData): { label: string; color: 'green' | 'amber' | 'red' } {
  if (fw.quarantineEnabled) return { label: 'Quarantine', color: 'green' };
  const msg = fw.message?.toLowerCase() ?? '';
  if (msg.includes('quarantine')) return { label: 'Quarantine', color: 'green' };
  if (msg.includes('audit') || msg.includes('enabled') || fw.enabled) return { label: 'Audit', color: 'amber' };
  return { label: 'Unprotected', color: 'red' };
}

function StatValue({ value, fallback = '\u2014' }: { value: number | undefined | null; fallback?: string }) {
  const display = value != null && value >= 0 ? value.toLocaleString() : fallback;
  return <Text size="5" weight="bold">{display}</Text>;
}

function StatLabel({ children }: { children: React.ReactNode }) {
  return <Text size="1" color="gray">{children}</Text>;
}

function InfoTip({ content }: { content: string }) {
  return (
    <Tooltip content={content}>
      <Box style={{ cursor: 'help', display: 'inline-flex', alignItems: 'center' }}>
        <Info size={12} color="var(--gray-8)" />
      </Box>
    </Tooltip>
  );
}

function CopyableUrl({ url }: { url: string }) {
  const [copied, setCopied] = React.useState(false);
  const urlWithSlash = ensureTrailingSlash(url);
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(urlWithSlash);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch { /* clipboard not available */ }
  };
  return (
    <Flex align="center" gap="2" style={{ minWidth: 0 }}>
      <Text size="2" style={{ wordBreak: 'break-all', fontFamily: 'var(--code-font-family)', flex: 1 }}>{urlWithSlash}</Text>
      <Tooltip content={copied ? 'Copied' : 'Copy URL'}>
        <Box onClick={handleCopy} style={{ cursor: 'pointer', flexShrink: 0, display: 'inline-flex' }}>
          {copied ? <Check size={14} color="var(--green-9)" /> : <Copy size={14} color="var(--gray-8)" />}
        </Box>
      </Tooltip>
    </Flex>
  );
}

export function UsageTab({
  repository,
  metrics,
  healthCheck,
  firewall,
  malwareCleanupSummary,
  blobStore,
}: UsageTabProps): JSX.Element {
  if (!metrics) {
    return (
      <Flex direction="column" align="center" justify="center" gap="3" py="9">
        <Activity size={48} color="var(--gray-7)" />
        <Text size="3" weight="medium" color="gray">Usage Metrics Unavailable</Text>
        <Text size="2" color="gray">Metrics are not yet available for this repository.</Text>
      </Flex>
    );
  }

  const hasHealthCheck = healthCheck != null;
  const hasFirewall = firewall?.enabled;
  const hasSecurityData = hasHealthCheck || hasFirewall;
  const remoteUrl = repository.attributes?.proxy?.remoteUrl;
  const blobStoreName = repository.attributes?.storage?.blobStoreName;

  const repoSize = metrics.totalSize || 0;
  const assetCount = metrics.assetCount || blobStore?.blobCount || 0;
  const metricsUnavailable = repoSize === 0 && assetCount === 0;

  return (
    <Flex direction="column" gap="5">
      {/* ================================================================
          Section 1: Storage Metrics
          ================================================================ */}
      <Grid columns="3" gap="4">
        <Card size="2">
          <Flex direction="column" gap="2" align="center" py="2">
            <HardDrive size={22} color="var(--accent-9)" />
            {repoSize > 0 ? (
              <StatValue value={undefined} fallback={formatBytes(repoSize)} />
            ) : (
              <Flex align="center" gap="1">
                <Text size="5" weight="bold" color="gray">{'\u2014'}</Text>
                <InfoTip content="Size not yet calculated. The repository metrics task runs periodically." />
              </Flex>
            )}
            <StatLabel>Repository Size</StatLabel>
          </Flex>
        </Card>

        <Card size="2">
          <Flex direction="column" gap="2" align="center" py="2">
            <FileBox size={22} color="var(--accent-9)" />
            {assetCount > 0 ? (
              <StatValue value={assetCount} />
            ) : metricsUnavailable ? (
              <Flex align="center" gap="1">
                <Text size="5" weight="bold" color="gray">{'\u2014'}</Text>
                <InfoTip content="Asset count not yet calculated. The repository metrics task runs periodically." />
              </Flex>
            ) : (
              <StatValue value={0} />
            )}
            <StatLabel>Total Assets</StatLabel>
          </Flex>
        </Card>

        <Card size="2">
          <Flex direction="column" gap="2" align="center" py="2">
            <Database size={22} color="var(--accent-9)" />
            <Text size="5" weight="bold">{blobStoreName ?? '\u2014'}</Text>
            <StatLabel>
              {blobStore?.availableSpaceInBytes != null
                ? `${formatBytes(blobStore.availableSpaceInBytes)} available`
                : 'Blob Store'}
            </StatLabel>
          </Flex>
        </Card>
      </Grid>

      {/* ================================================================
          Section 2: Security Posture (only when data exists)
          ================================================================ */}
      {hasSecurityData && (
        <Card size="2">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <ShieldCheck size={18} color="var(--accent-9)" />
              <Text size="3" weight="bold">Security Posture</Text>
            </Flex>
            <Separator size="4" />

            <Grid columns={{ initial: '1', sm: hasHealthCheck && hasFirewall ? '2' : '1' }} gap="5">
              {/* Health Check side */}
              {hasHealthCheck && (
                <Flex direction="column" gap="3">
                  <Flex align="center" gap="2">
                    <Text size="2" weight="medium">Health Check</Text>
                    <Badge
                      size="1"
                      variant="soft"
                      color={healthCheck.enabled ? 'green' : 'gray'}
                    >
                      {healthCheck.enabled ? 'Enabled' : 'Disabled'}
                    </Badge>
                  </Flex>
                  <Grid columns="2" gap="3">
                    <Box>
                      <StatValue value={healthCheck.securityIssueCount} />
                      <StatLabel>Security Issues</StatLabel>
                    </Box>
                    <Box>
                      <StatValue value={healthCheck.licenseIssueCount} />
                      <StatLabel>License Issues</StatLabel>
                    </Box>
                  </Grid>
                  {(() => {
                    const ts = formatTimestamp(healthCheck.lastAnalyzedDate);
                    return ts ? (
                      <Text size="1" color="gray">Last analyzed: {ts}</Text>
                    ) : (
                      <Text size="1" color="gray">Last analyzed: Not Available</Text>
                    );
                  })()}
                </Flex>
              )}

              {/* Firewall side */}
              {hasFirewall && (
                <Flex direction="column" gap="3"
                  style={hasHealthCheck ? { borderLeft: '1px solid var(--gray-4)', paddingLeft: 'var(--space-5)' } : undefined}
                >
                  <Flex align="center" gap="2">
                    <Text size="2" weight="medium">Firewall</Text>
                    {(() => {
                      const { label, color } = deriveProtectionLabel(firewall);
                      return <Badge size="1" variant="soft" color={color}>{label}</Badge>;
                    })()}
                  </Flex>
                  <Grid columns="2" gap="3">
                    <Box>
                      <StatValue value={firewall.affectedComponentCount} />
                      <StatLabel>Affected Components</StatLabel>
                    </Box>
                    <Box>
                      <StatValue value={firewall.quarantinedComponentCount} />
                      <StatLabel>Quarantined</StatLabel>
                    </Box>
                    <Box>
                      <Flex gap="1" align="baseline">
                        <StatValue value={firewall.criticalComponentCount} />
                        <Text size="1" color="red">critical</Text>
                      </Flex>
                    </Box>
                    <Box>
                      <Flex gap="1" align="baseline">
                        <StatValue value={firewall.severeComponentCount} />
                        <Text size="1" color="orange">severe</Text>
                      </Flex>
                    </Box>
                  </Grid>
                  {malwareCleanupSummary && (
                    <Flex align="center" gap="3">
                      <Text size="1" color="gray">
                        Malicious Packages: {malwareCleanupSummary.taskEnabled ? (malwareCleanupSummary.taskCleanupEnabled ? 'Delete' : 'Audit') : 'Off'}
                      </Text>
                      {malwareCleanupSummary.scrubbedCount > 0 && (
                        <Text size="1" color="gray">{malwareCleanupSummary.scrubbedCount} scrubbed</Text>
                      )}
                      {malwareCleanupSummary.pendingCount > 0 && (
                        <Text size="1" color="red">{malwareCleanupSummary.pendingCount} pending</Text>
                      )}
                    </Flex>
                  )}
                </Flex>
              )}
            </Grid>
          </Flex>
        </Card>
      )}

      {/* ================================================================
          Section 3: Repository Configuration
          ================================================================ */}
      <Card size="2">
        <Flex direction="column" gap="3">
          <Text size="3" weight="bold">Repository Configuration</Text>
          <Separator size="4" />
          <Grid columns="2" gap="4" rows="auto">
            <Box>
              <Text size="1" color="gray" as="div" mb="1">Format</Text>
              <Text size="2" weight="medium">{repository.format}</Text>
            </Box>
            <Box>
              <Text size="1" color="gray" as="div" mb="1">Type</Text>
              <Text size="2" weight="medium" style={{ textTransform: 'capitalize' }}>{repository.type}</Text>
            </Box>
            <Box style={{ gridColumn: 'span 2' }}>
              <Text size="1" color="gray" as="div" mb="1">Repository URL</Text>
              <CopyableUrl url={repository.url} />
            </Box>
            {remoteUrl && (
              <Box style={{ gridColumn: 'span 2' }}>
                <Text size="1" color="gray" as="div" mb="1">Remote URL (Proxy)</Text>
                <CopyableUrl url={remoteUrl} />
              </Box>
            )}
            <Box>
              <Text size="1" color="gray" as="div" mb="1">Status</Text>
              <Badge color={repository.online ? 'green' : 'red'} variant="soft">
                {repository.online ? 'Online' : 'Offline'}
              </Badge>
            </Box>
            {blobStoreName && (
              <Box>
                <Text size="1" color="gray" as="div" mb="1">Blob Store</Text>
                <Text size="2" weight="medium">{blobStoreName}</Text>
              </Box>
            )}
          </Grid>
        </Flex>
      </Card>
    </Flex>
  );
}

export default UsageTab;
