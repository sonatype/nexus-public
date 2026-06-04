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

import React, { useMemo } from 'react';
import { Box, Button, Callout, Card, Flex, Grid, Heading, Skeleton, Text } from '@radix-ui/themes';
import { Info, ShieldCheck } from 'lucide-react';
import { ExtJS } from '../../../../interface/ExtJS';
import type { ProtectDataSnapshot } from './useProtectData';

const LEARN_ABOUT_FIREWALL_HREF = 'https://links.sonatype.com/nexus-repository-firewall';

function buildFirewallDashboardUrl(iqBaseUrl: string | undefined): string | null {
  if (!iqBaseUrl?.trim()) return null;
  return `${iqBaseUrl.replace(/\/+$/, '')}/assets/index.html#/firewall/dashboard/`;
}

export interface ProtectOverviewProps {
  protectData: ProtectDataSnapshot;
  onGoToQuickConfig: () => void;
}

export default function ProtectOverview({ protectData, onGoToQuickConfig }: ProtectOverviewProps) {
  const {
    repos,
    loading,
    hcSummary,
    iqAudit,
    hasFirewall,
    hasIqConnection,
    hcInstanceEnabled,
    canUpdateHealthCheck,
    iqCapabilities,
  } = protectData;

  const cleanupStats = useMemo(() => {
    const active = repos.filter((r) => r.taskEnabled).length;
    const pendingMalware = repos.reduce((s, r) => s + r.malwareCount, 0);
    return { active, total: repos.length, pendingMalware };
  }, [repos]);

  if (loading) {
    return (
      <Box p="4" data-testid="protect-overview-skeleton" aria-busy="true" aria-label="Loading Protect overview">
        <Grid columns={{ initial: '1', sm: '3' }} gap="4">
          {[0, 1, 2].map((i) => (
            <Card key={i}>
              <Flex direction="column" gap="2" p="3">
                <Skeleton height={20} width="40%" />
                <Skeleton height={36} width="55%" />
                <Skeleton height={16} width="90%" />
                <Skeleton height={16} width="70%" />
              </Flex>
            </Card>
          ))}
        </Grid>
      </Box>
    );
  }

  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;
  const fwCounts = iqAudit.counts;

  const firewallLicense = iqCapabilities?.hasFirewall === true;
  const showFirewallUpsell = iqCapabilities != null && !firewallLicense;
  const firewallDashboardUrl = buildFirewallDashboardUrl(iqCapabilities?.url);

  let quarantineProxySummary: { quarantine: number; total: number } | null = null;
  const auditEligibleTotal = fwCounts
    ? fwCounts.reposProtected + fwCounts.reposInAudit + fwCounts.reposUnprotected
    : null;
  if (fwCounts && auditEligibleTotal !== null && auditEligibleTotal > 0) {
    quarantineProxySummary = { quarantine: fwCounts.reposProtected, total: auditEligibleTotal };
  }

  return (
    <Box p="4">
      {showFirewallUpsell && (
        <Callout.Root color="blue" mb="4" data-testid="protect-overview-firewall-upsell">
          <Callout.Icon>
            <Info size={16} aria-hidden />
          </Callout.Icon>
          {/* Callout.Text renders as <p>; block children need a Box wrapper (see GASecurityTab). */}
          <Box className="rt-CalloutText">
            <Flex direction="column" gap="2">
              <Text weight="bold" size="3">
                Upgrade your protection with Sonatype Repository Firewall
              </Text>
              <Text size="2" color="gray">
                Get real-time quarantine, automated malware remediation, and Sonatype's industry-leading open source
                intelligence.
              </Text>
              <Box>
                <Button size="2" variant="solid" asChild>
                  <a href={LEARN_ABOUT_FIREWALL_HREF} target="_blank" rel="noopener noreferrer">
                    Learn About Firewall
                  </a>
                </Button>
              </Box>
            </Flex>
          </Box>
        </Callout.Root>
      )}

      {firewallLicense && (
        <Card mb="4" data-testid="protect-overview-firewall-active">
          <Flex direction={{ initial: 'column', sm: 'row' }} align={{ sm: 'center' }} justify="between" gap="3" p="3">
            <Flex align="center" gap="3" wrap="wrap">
              <Flex align="center" gap="2">
                <Box
                  style={{
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    background: 'var(--green-9)',
                    flexShrink: 0,
                  }}
                  aria-hidden
                />
                <ShieldCheck size={20} color="var(--green-9)" aria-hidden />
                <Text weight="bold" size="3">
                  Firewall Active
                </Text>
              </Flex>
              {hasIqConnection && quarantineProxySummary && (
                <Text size="2" color="gray">
                  Quarantine enabled on {quarantineProxySummary.quarantine}/{quarantineProxySummary.total} proxies
                </Text>
              )}
              {hasIqConnection &&
                fwCounts &&
                auditEligibleTotal === 0 &&
                !iqAudit.error && (
                  <Text size="2" color="gray">
                    No firewall-eligible proxy repositories in IQ audit.
                  </Text>
                )}
              {hasIqConnection && !fwCounts && !iqAudit.error && (
                <Text size="2" color="gray">
                  Loading proxy coverage…
                </Text>
              )}
              {hasIqConnection && iqAudit.error && (
                <Text size="2" color="gray">
                  Could not load quarantine coverage.
                </Text>
              )}
              {!hasIqConnection && (
                <Text size="2" color="gray">
                  Connect IQ Server to manage quarantine on proxy repositories.
                </Text>
              )}
            </Flex>
            {firewallDashboardUrl ? (
              <Button size="2" variant="soft" asChild>
                <a href={firewallDashboardUrl} target="_blank" rel="noopener noreferrer">
                  View Firewall Dashboard
                </a>
              </Button>
            ) : (
              <Button size="2" variant="soft" asChild>
                <a href="#preview/admin/iq">IQ Server settings</a>
              </Button>
            )}
          </Flex>
        </Card>
      )}

      <Grid columns={{ initial: '1', sm: '3' }} gap="4">
        <Card>
          <Flex direction="column" gap="2" p="3">
            <Heading size="3">Health Check</Heading>
            {hcSummary.error ? (
              <Text size="2" color="gray">
                {hcSummary.error}
              </Text>
            ) : (
              <>
                <Text size="5" weight="bold">
                  {hcSummary.enabledCount} / {hcSummary.totalProxyCount}
                </Text>
                <Text size="2" color="gray">
                  eligible proxy repositories with Health Check enabled
                </Text>
                {hcSummary.unsupportedFormatProxyCount > 0 && (
                  <Text size="1" color="gray">
                    {hcSummary.unsupportedFormatProxyCount} other proxy{' '}
                    {hcSummary.unsupportedFormatProxyCount === 1 ? 'repository does' : 'repositories do'} not support
                    Health Check (format).
                  </Text>
                )}
                {(hcSummary.totalSecurityIssues > 0 || hcSummary.totalLicenseIssues > 0) && (
                  <Text size="2" color="gray">
                    {hcSummary.totalSecurityIssues > 0 &&
                      `${hcSummary.totalSecurityIssues.toLocaleString()} security issue${hcSummary.totalSecurityIssues !== 1 ? 's' : ''}`}
                    {hcSummary.totalSecurityIssues > 0 && hcSummary.totalLicenseIssues > 0 ? ' · ' : ''}
                    {hcSummary.totalLicenseIssues > 0 &&
                      `${hcSummary.totalLicenseIssues.toLocaleString()} license issue${hcSummary.totalLicenseIssues !== 1 ? 's' : ''}`}
                  </Text>
                )}
                {!canUpdateHealthCheck && (
                  <Text size="1" color="gray">
                    You do not have permission to change Health Check settings.
                  </Text>
                )}
                {canUpdateHealthCheck && !hcInstanceEnabled && (
                  <>
                    <Text size="2" color="amber">
                      {isCloud
                        ? 'Health Check capability is disabled — contact your administrator to enable scanning.'
                        : 'Health Check capability is disabled — enable it in System Capabilities to turn on scanning.'}
                    </Text>
                    {!isCloud && (
                      <Button size="1" variant="soft" asChild>
                        <a href="#preview/admin/system/capabilities">Open Capabilities</a>
                      </Button>
                    )}
                  </>
                )}
              </>
            )}
          </Flex>
        </Card>

        <Card>
          <Flex direction="column" gap="2" p="3">
            <Heading size="3">Firewall Protection</Heading>
            {!firewallLicense ? (
              <>
                <Text size="2" color="gray">
                  Repository Firewall provides real-time quarantine and automated malware remediation
                  for your proxy repositories.
                </Text>
                <Text size="1" color="gray">
                  A Firewall license and IQ Server connection are required.
                </Text>
              </>
            ) : !hasIqConnection ? (
              <>
                <Text size="2" color="amber">
                  {isCloud
                    ? 'IQ Server is not connected. Contact your administrator to enable Firewall for this tenant.'
                    : 'IQ Server is not connected. Connect IQ to see firewall protection levels.'}
                </Text>
                {!isCloud && (
                  <Button size="1" variant="soft" asChild>
                    <a href="#preview/admin/iq">Connect IQ</a>
                  </Button>
                )}
              </>
            ) : iqAudit.error ? (
              <Text size="2" color="gray">
                {iqAudit.error.message}
              </Text>
            ) : fwCounts ? (
              <>
                <Text size="2">
                  {fwCounts.reposProtected} Quarantine · {fwCounts.reposInAudit} Audit · {fwCounts.reposUnprotected}{' '}
                  Unprotected
                </Text>
                <Text size="1" color="gray">
                  Across supported proxy repositories
                </Text>
              </>
            ) : (
              <Text size="2" color="gray">
                Loading firewall summary…
              </Text>
            )}
          </Flex>
        </Card>

        <Card>
          <Flex direction="column" gap="2" p="3">
            <Heading size="3">OSS Malware Cleanup</Heading>
            {!firewallLicense ? (
              <>
                <Text size="2" color="gray">
                  Automated malware cleanup tasks require a Firewall license and IQ Server connection.
                </Text>
                <Text size="1" color="gray">
                  Use Repository Health Check to detect malware. Firewall enables automated cleanup.
                </Text>
              </>
            ) : (
              <>
                <Text size="5" weight="bold">
                  {cleanupStats.active} / {cleanupStats.total}
                </Text>
                <Text size="2" color="gray">
                  repos with OSS malware cleanup task active
                </Text>
                <Text size="1" color="gray">
                  {cleanupStats.pendingMalware} pending malware component
                  {cleanupStats.pendingMalware === 1 ? '' : 's'} across repos
                </Text>
              </>
            )}
          </Flex>
        </Card>
      </Grid>

      <Flex justify="center" mt="6">
        <Button size="3" variant="solid" onClick={onGoToQuickConfig}>
          Go to Quick Config →
        </Button>
      </Flex>
    </Box>
  );
}
