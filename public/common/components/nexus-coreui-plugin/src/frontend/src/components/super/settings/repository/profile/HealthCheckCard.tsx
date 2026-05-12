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

import React, { useState, useCallback, useEffect } from 'react';
import { Box, Card, Flex, Text, Badge, Button, Switch, Tooltip, Grid, Separator } from '@radix-ui/themes';
import { ShieldCheck, ShieldAlert, Info, ExternalLink, Shield, CheckCircle2, Skull } from 'lucide-react';
import type { HealthCheckData, CapabilityInfo } from './hooks/useRepositoryProfile';

const REPORT_NOT_AVAILABLE =
  'Health Check report not yet available. Reports are generated periodically after enabling Health Check.';

function isSameOriginReportUrl(url: string): boolean {
  try {
    const resolved = new URL(url, window.location.href);
    return resolved.origin === window.location.origin;
  } catch {
    return false;
  }
}

export interface HealthCheckCardProps {
  repositoryName: string;
  healthCheck: HealthCheckData | null;
  capabilities: CapabilityInfo[];
  onToggleRepo: (enabled: boolean) => void;
  onToggleInstance: (enabled: boolean, useTrustStore: boolean) => void;
  isSupported: boolean;
}

function parseLastAnalyzed(healthCheck: HealthCheckData | null): string | null {
  if (!healthCheck) return null;

  // Try the direct lastAnalyzedDate field first
  if (healthCheck.lastAnalyzedDate && healthCheck.lastAnalyzedDate > 0) {
    return new Date(healthCheck.lastAnalyzedDate).toLocaleString(undefined, {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: 'numeric', minute: '2-digit',
    });
  }

  // Fallback: extract hex timestamp from detailUrl (e.g. .../19d12c0705b/details.html)
  if (healthCheck.detailUrl) {
    const match = healthCheck.detailUrl.match(/\/([0-9a-f]{8,})\//i);
    if (match) {
      try {
        const ts = parseInt(match[1], 16);
        if (ts > 1000000000000) {
          return new Date(ts).toLocaleString(undefined, {
            month: 'short', day: 'numeric', year: 'numeric',
            hour: 'numeric', minute: '2-digit',
          });
        }
      } catch { /* ignore */ }
    }
  }

  return null;
}

export function HealthCheckCard({
  repositoryName,
  healthCheck,
  capabilities,
  onToggleRepo,
  onToggleInstance,
  isSupported,
}: HealthCheckCardProps): JSX.Element {
  const [reportOpenError, setReportOpenError] = useState<string | null>(null);

  const healthCheckCapability = capabilities.find(c => c.type === 'healthcheck');
  const isInstanceEnabled = healthCheckCapability?.enabled ?? false;
  const isRepoEnabled = healthCheck?.enabled ?? false;

  const hcSecurity = healthCheck?.securityIssueCount ?? 0;
  const hcLicense = healthCheck?.licenseIssueCount ?? 0;
  const malwareCount = healthCheck?.malwareCount;
  const hasMalwareCount = malwareCount !== null && malwareCount !== undefined;

  const lastAnalyzed = parseLastAnalyzed(healthCheck);

  useEffect(() => {
    setReportOpenError(null);
  }, [healthCheck?.detailUrl]);

  const openFullReport = useCallback(async () => {
    const url = healthCheck?.detailUrl;
    if (!url) {
      return;
    }
    setReportOpenError(null);
    if (!isSameOriginReportUrl(url)) {
      window.open(url, '_blank', 'noopener,noreferrer');
      return;
    }
    try {
      const res = await fetch(url, { method: 'HEAD', credentials: 'include', redirect: 'follow' });
      if (res.status === 404) {
        setReportOpenError(REPORT_NOT_AVAILABLE);
        return;
      }
    } catch {
      /* same-origin fetch failed; still try opening */
    }
    window.open(url, '_blank', 'noopener,noreferrer');
  }, [healthCheck?.detailUrl]);

  const infoIcon = (content: string) => (
    <Tooltip content={content}>
      <Box style={{ cursor: 'help', display: 'inline-flex', alignItems: 'center' }}>
        <Info size={14} color="var(--gray-8)" />
      </Box>
    </Tooltip>
  );

  if (!isSupported) {
    return (
      <Card size="2" style={{ flex: 1 }}>
        <Flex align="center" gap="3" height="100%">
          <Shield size={24} color="var(--gray-7)" />
          <Box>
            <Flex align="center" gap="2">
              <Text size="2" weight="bold" color="gray">Health Check</Text>
              {infoIcon("Repository Health Check is not supported for this repository format.")}
            </Flex>
            <Text size="2" color="gray">Not Supported</Text>
          </Box>
        </Flex>
      </Card>
    );
  }

  if (!isInstanceEnabled) {
    return (
      <Card size="2" style={{ flex: 1 }}>
        <Flex direction="column" gap="3">
          <Flex align="center" justify="between">
            <Flex align="center" gap="2">
              <ShieldAlert size={20} color="var(--yellow-9)" />
              <Text weight="bold">Health Check</Text>
              {infoIcon("Repository Health Check analyzes your repositories for security vulnerabilities and license risks.")}
            </Flex>
            <Badge color="yellow" variant="soft">Instance Disabled</Badge>
          </Flex>
          <Text size="2" color="gray">Health Check is disabled for this Nexus instance.</Text>
          <Button
            size="1"
            variant="soft"
            color="gray"
            onClick={() => onToggleInstance(true, false)}
          >
            Enable Instance-wide
          </Button>
        </Flex>
      </Card>
    );
  }

  return (
    <Card size="2" style={{ flex: 1 }}>
      <Flex direction="column" gap="3">
        {/* ---- Header ---- */}
        <Flex align="center" justify="between">
          <Flex align="center" gap="2">
            <ShieldCheck size={20} color={isRepoEnabled ? "var(--green-9)" : "var(--gray-9)"} />
            <Text weight="bold">Health Check</Text>
            {infoIcon("Repository Health Check analyzes your repositories for security vulnerabilities and license risks.")}
          </Flex>
          <Badge color={isRepoEnabled ? "green" : "gray"} variant="soft">
            {isRepoEnabled ? "Enabled" : "Disabled"}
          </Badge>
        </Flex>

        {isInstanceEnabled && !isRepoEnabled && (
          <Text size="2" color="gray">Enable Health Check to scan for malware.</Text>
        )}

        {isRepoEnabled && (
          <>
            {healthCheck?.analyzing ? (
              <>
                <Flex align="center" gap="2" p="2" style={{ backgroundColor: 'var(--blue-2)', borderRadius: '4px' }}>
                  <Info size={14} color="var(--blue-9)" />
                  <Text size="2" color="blue">Analyzing components...</Text>
                </Flex>
                <Flex align="center" justify="between" p="2" style={{ backgroundColor: 'var(--gray-2)', borderRadius: '4px' }}>
                  <Text size="2" weight="medium">Malicious Components</Text>
                  <Text size="2" color="blue">Analyzing...</Text>
                </Flex>
              </>
            ) : (
              <>
                {/* ---- Security + License counts ---- */}
                <Grid columns="2" gap="2">
                  <Flex direction="column" align="center" p="2" style={{ backgroundColor: 'var(--red-2)', borderRadius: '4px' }}>
                    <Text size="1" color="red">Security Vulnerabilities</Text>
                    <Text size="4" weight="bold" color="red">{hcSecurity.toLocaleString()}</Text>
                  </Flex>
                  <Flex direction="column" align="center" p="2" style={{ backgroundColor: 'var(--amber-2)', borderRadius: '4px' }}>
                    <Text size="1" color="amber">License Issues</Text>
                    <Text size="4" weight="bold" color="amber">{hcLicense.toLocaleString()}</Text>
                  </Flex>
                </Grid>

                {/* ---- Malicious components (HDS); omit row when count unknown (null) ---- */}
                {hasMalwareCount ? (
                  <Flex
                    align="center"
                    justify="between"
                    p="2"
                    style={{
                      backgroundColor: malwareCount > 0 ? 'var(--red-2)' : 'var(--green-2)',
                      borderRadius: '4px',
                    }}
                  >
                    <Flex align="center" gap="2">
                      {malwareCount > 0 ? (
                        <Skull size={16} color="var(--red-11)" aria-hidden />
                      ) : (
                        <CheckCircle2 size={16} color="var(--green-11)" aria-hidden />
                      )}
                      <Text size="2" weight="medium" color={malwareCount > 0 ? 'red' : 'green'}>
                        Malicious Components
                      </Text>
                    </Flex>
                    <Badge color={malwareCount > 0 ? 'red' : 'green'} size="2" variant="soft">
                      {malwareCount.toLocaleString()}
                    </Badge>
                  </Flex>
                ) : null}
              </>
            )}

            {/* ---- Last Analyzed timestamp ---- */}
            <Flex align="center" gap="1">
              <Text size="1" color="gray">Last analyzed: {lastAnalyzed ?? 'Not Available'}</Text>
              {infoIcon("Analysis runs automatically every 6 hours")}
            </Flex>

            {/* ---- View Full Report ---- */}
            <Flex direction="column" gap="1" align="start">
              <Button
                variant="soft"
                size="1"
                disabled={!healthCheck?.detailUrl}
                onClick={() => void openFullReport()}
              >
                View Full Report <ExternalLink size={14} />
              </Button>
              <Text size="1" color="gray">Report may take up to 6 hours to generate after enabling.</Text>
              {reportOpenError && (
                <Text size="2" color="amber" style={{ maxWidth: '100%' }}>{reportOpenError}</Text>
              )}
            </Flex>
          </>
        )}

        <Separator size="4" />

        {/* ---- On/Off toggle at the bottom ---- */}
        <Flex justify="between" align="center">
          <Flex align="center" gap="2">
            <Switch
              size="1"
              checked={isRepoEnabled}
              onCheckedChange={(checked) => onToggleRepo(checked)}
            />
            <Text size="1" color="gray">{isRepoEnabled ? 'Active' : 'Off'}</Text>
          </Flex>
        </Flex>
      </Flex>
    </Card>
  );
}
