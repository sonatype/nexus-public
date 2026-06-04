/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark
 * of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * Firewall config tab for Edit repository form – proxy repos only.
 * Same 3-button treatment as Malware Defense: None | Audit | Quarantine with outline on selected.
 * Includes Repository Health Check section (enable/disable).
 */
import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text, Card, Button, Separator } from '@radix-ui/themes';
import { Shield, ShieldCheck, ExternalLink } from 'lucide-react';
import {
  useFirewallEnable,
  fetchIqAuditStatus,
  type IqAuditStatus,
} from '../../../../shared/security/useFirewallEnable';
import { ProtectionLevelSelector, type ProtectionLevel } from './ProtectionLevelSelector';
import { useRepositoriesApi } from './useRepositoriesApi';
import { restClient } from '../../../../../../interface/api';

export interface RepositoryFirewallConfigTabProps {
  repositoryName: string;
  /** When false, show cross-sell instead of Enable buttons */
  hasFirewallLicense?: boolean;
  /** Called after successful enable (parent can refresh) */
  onEnableSuccess?: () => void;
  /** Show the Firewall section (default true) */
  showFirewall?: boolean;
  /** Show the Health Check section (default true) */
  showHealthCheck?: boolean;
}

interface HealthCheckStatus {
  enabled?: boolean;
  analyzing?: boolean;
}

export function RepositoryFirewallConfigTab({
  repositoryName,
  hasFirewallLicense = true,
  onEnableSuccess,
  showFirewall = true,
  showHealthCheck = true,
}: RepositoryFirewallConfigTabProps): JSX.Element {
  const [status, setStatus] = useState<IqAuditStatus | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [healthCheck, setHealthCheck] = useState<HealthCheckStatus | null>(null);
  const [healthCheckLoading, setHealthCheckLoading] = useState(true);
  const { enableAudit, enableQuarantine, disable, loading, error } = useFirewallEnable(repositoryName);
  const { enableHealthCheck } = useRepositoriesApi();

  const fetchStatus = useCallback(async () => {
    if (!hasFirewallLicense) {
      setStatus(null);
      setLoadingStatus(false);
      return;
    }
    setLoadingStatus(true);
    try {
      const data = await fetchIqAuditStatus(repositoryName);
      setStatus(data);
    } catch {
      setStatus(null);
    } finally {
      setLoadingStatus(false);
    }
  }, [repositoryName, hasFirewallLicense]);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  useEffect(() => {
    let cancelled = false;
    setHealthCheckLoading(true);
    restClient
      .get<HealthCheckStatus>(`/service/rest/v1/repositories/${encodeURIComponent(repositoryName)}/health-check`)
      .then((data) => {
        if (!cancelled) setHealthCheck(data ?? null);
      })
      .catch(() => {
        if (!cancelled) setHealthCheck(null);
      })
      .finally(() => {
        if (!cancelled) setHealthCheckLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [repositoryName]);

  const protectionLevel: ProtectionLevel =
    status?.enabled && status?.enabledQuarantine
      ? 'quarantine'
      : status?.enabled
        ? 'audit'
        : 'none';

  const handleProtectionChange = async (level: ProtectionLevel) => {
    const base = { repositoryName, enabled: false, enabledQuarantine: false };
    try {
      if (level === 'none') await disable(() => { setStatus((p) => ({ ...(p ?? base), enabled: false, enabledQuarantine: false })); onEnableSuccess?.(); });
      else if (level === 'audit') await enableAudit(() => { setStatus((p) => ({ ...(p ?? base), enabled: true, enabledQuarantine: false })); onEnableSuccess?.(); });
      else await enableQuarantine(() => { setStatus((p) => ({ ...(p ?? base), enabled: true, enabledQuarantine: true })); onEnableSuccess?.(); });
    } catch {
      // Error shown inline via useFirewallEnable error state
    }
  };

  const handleEnableHealthCheck = async () => {
    try {
      await enableHealthCheck(repositoryName);
      setHealthCheck((p) => (p ? { ...p, enabled: true } : { enabled: true }));
      onEnableSuccess?.();
    } catch {
      // Best effort
    }
  };

  if (loadingStatus) {
    return (
      <Box p="4">
        <Text size="2" color="gray">
          Loading Firewall status…
        </Text>
      </Box>
    );
  }

  return (
    <Box p="4" className="repository-firewall-config-tab">
      {showFirewall && (
        <Card size="2">
          <Flex direction="column" gap="4">
            <Flex align="center" gap="3">
              <Shield size={24} color="var(--orange-9)" aria-hidden />
              <Box>
                <Text size="4" weight="bold" as="div" mb="1">
                  Repository Firewall
                </Text>
                <Text size="2" color="gray" as="div">
                  Protect this proxy repository by enabling Firewall to audit or block policy violations and malware.
                </Text>
              </Box>
            </Flex>

            {error && (
              <Text size="2" color="red">
                {error}
              </Text>
            )}

            {hasFirewallLicense ? (
              <Box>
                <Text size="2" weight="medium" mb="2" as="p">
                  Protection level
                </Text>
                <ProtectionLevelSelector
                  value={protectionLevel}
                  onChange={handleProtectionChange}
                  disabled={loading}
                  size="2"
                />
                <Text size="1" color="gray" mt="2">
                  Audit trail: Configuration change history will be available in a future release.
                </Text>
              </Box>
            ) : (
              <Flex direction="column" gap="3">
                <Text size="2" color="gray">
                  Protect this repository with Repository Firewall. Requires a Firewall license.
                </Text>
                <Flex gap="2" wrap="wrap">
                  <Button variant="ghost" size="2" asChild>
                    <a
                      href="https://help.sonatype.com/iqserver/product-information/repository-firewall"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <ExternalLink size={14} />
                      Learn more
                    </a>
                  </Button>
                  <Button variant="ghost" size="2" asChild>
                    <a href="http://links.sonatype.com/contact" target="_blank" rel="noopener noreferrer">
                      Contact sales
                    </a>
                  </Button>
                </Flex>
              </Flex>
            )}
          </Flex>
        </Card>
      )}

      {showFirewall && showHealthCheck && <Separator size="4" my="4" />}

      {showHealthCheck && (
        <Card size="2">
          <Flex direction="column" gap="4">
            <Flex align="center" gap="3">
              <ShieldCheck size={24} color="var(--blue-9)" aria-hidden />
              <Box>
                <Text size="4" weight="bold">
                  Repository Health Check
                </Text>
                <Text size="2" color="gray">
                  Repository Health Check analyzes components for security vulnerabilities and license issues.
                </Text>
              </Box>
            </Flex>

            {healthCheckLoading ? (
              <Text size="2" color="gray">
                Loading…
              </Text>
            ) : healthCheck?.enabled ? (
              <Flex align="center" gap="2">
                <Text size="2" weight="medium">
                  {healthCheck.analyzing ? 'Analyzing' : 'Enabled'}
                </Text>
              </Flex>
            ) : (
              <Flex gap="2" wrap="wrap">
                <Button
                  type="button"
                  variant="solid"
                  color="blue"
                  size="2"
                  onClick={handleEnableHealthCheck}
                >
                  Enable Health Check
                </Button>
                <Button type="button" variant="soft" size="2" disabled>
                  None
                </Button>
              </Flex>
            )}
            <Text size="1" color="gray" mt="2">
              Audit trail: Configuration change history will be available in a future release.
            </Text>
          </Flex>
        </Card>
      )}
    </Box>
  );
}

export default RepositoryFirewallConfigTab;
