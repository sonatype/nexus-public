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
  fetchPccsSupportedFormats,
  type IqAuditStatus,
} from '../../../../shared/security/useFirewallEnable';
import { ProtectionLevelSelector, type ProtectionLevel } from './ProtectionLevelSelector';
import { useRepositoriesApi } from './useRepositoriesApi';
import { restClient, ENDPOINTS } from '../../../../../../interface/api';

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
  /**
   * Repository format (e.g. 'maven2', 'npm', 'pypi'). Used to decide whether to offer the
   * PCCS protection level. Optional for backward compatibility — when omitted, PCCS is hidden
   * (matching the pre-PCCS behaviour). Future callers should always pass this through.
   */
  format?: string;
}

interface HealthCheckStatus {
  enabled?: boolean;
  analyzing?: boolean;
}

interface HealthCheckListEntry extends HealthCheckStatus {
  repositoryName: string;
}

export function RepositoryFirewallConfigTab({
  repositoryName,
  hasFirewallLicense = true,
  onEnableSuccess,
  showFirewall = true,
  showHealthCheck = true,
  format,
}: RepositoryFirewallConfigTabProps): JSX.Element {
  const [status, setStatus] = useState<IqAuditStatus | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [healthCheck, setHealthCheck] = useState<HealthCheckStatus | null>(null);
  const [healthCheckLoading, setHealthCheckLoading] = useState(true);
  const [pccsSupported, setPccsSupported] = useState(false);
  const { enableAudit, enableQuarantine, enablePccs, disable, loading, error } =
    useFirewallEnable(repositoryName);
  const { enableHealthCheck, disableHealthCheck } = useRepositoriesApi();

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

  // Resolve whether this repository's format supports PCCS. We do not gate on
  // `hasFirewallLicense` here — the format-capabilities query is a cheap server lookup that
  // is independent of license state, and gating PCCS visibility on format alone matches the
  // legacy ExtJS form behaviour.
  useEffect(() => {
    if (!format) {
      setPccsSupported(false);
      return;
    }
    let cancelled = false;
    fetchPccsSupportedFormats().then((formats) => {
      if (!cancelled) {
        setPccsSupported(formats.includes(format));
      }
    });
    return () => {
      cancelled = true;
    };
  }, [format]);

  useEffect(() => {
    if (!showHealthCheck) {
      setHealthCheck(null);
      setHealthCheckLoading(false);
      return;
    }
    let cancelled = false;
    setHealthCheckLoading(true);
    restClient
      .get<HealthCheckListEntry[]>(ENDPOINTS.HEALTH_CHECK)
      .then((data) => {
        if (cancelled) return;
        const entry = Array.isArray(data)
          ? data.find((r) => r.repositoryName === repositoryName)
          : undefined;
        setHealthCheck(entry ?? null);
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
  }, [repositoryName, showHealthCheck]);

  // Map the 4-mode firewall state down to the protection level the selector understands.
  // We deliberately read `status.mode` (post-migration full state) when available and only
  // fall back to the boolean view for legacy callers — otherwise QUARANTINE and PCCS would
  // both render as 'quarantine'.
  const protectionLevel: ProtectionLevel =
    status?.mode === 'PCCS'
      ? 'pccs'
      : status?.mode === 'QUARANTINE'
        ? 'quarantine'
        : status?.mode === 'AUDIT'
          ? 'audit'
          : status?.enabled && status?.enabledQuarantine
            ? 'quarantine'
            : status?.enabled
              ? 'audit'
              : 'none';

  const handleProtectionChange = async (level: ProtectionLevel) => {
    // Optimistically update the local status so the selector reflects the chosen button
    // before the next fetch round-trips. The server-side mode is the source of truth on
    // subsequent reads.
    const applyOptimistic = (mode: 'DISABLED' | 'AUDIT' | 'QUARANTINE' | 'PCCS') => {
      setStatus((prev) => ({
        repositoryName: prev?.repositoryName ?? repositoryName,
        enabled: mode !== 'DISABLED',
        enabledQuarantine: mode === 'QUARANTINE' || mode === 'PCCS',
        mode,
      }));
      onEnableSuccess?.();
    };
    try {
      if (level === 'none') await disable(() => applyOptimistic('DISABLED'));
      else if (level === 'audit') await enableAudit(() => applyOptimistic('AUDIT'));
      else if (level === 'quarantine') await enableQuarantine(() => applyOptimistic('QUARANTINE'));
      else await enablePccs(() => applyOptimistic('PCCS'));
    } catch {
      // Error shown inline via useFirewallEnable error state
    }
  };

  const handleEnableHealthCheck = async () => {
    try {
      await enableHealthCheck(repositoryName);
      setHealthCheck((p) => (p ? { ...p, enabled: true, analyzing: true } : { enabled: true, analyzing: true }));
      onEnableSuccess?.();
    } catch {
      // Best effort
    }
  };

  const handleDisableHealthCheck = async () => {
    try {
      await disableHealthCheck(repositoryName);
      setHealthCheck((p) => (p ? { ...p, enabled: false, analyzing: false } : { enabled: false }));
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
                  pccsSupported={pccsSupported}
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
                      href="https://links.sonatype.com/nexus-repository-firewall"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <ExternalLink size={14} />
                      Learn more
                    </a>
                  </Button>
                  <Button variant="ghost" size="2" asChild>
                    <a href="https://links.sonatype.com/contact-sales" target="_blank" rel="noopener noreferrer">
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
                <Text size="4" weight="bold" as="div" mb="1">
                  Repository Health Check
                </Text>
                <Text size="2" color="gray" as="div">
                  Repository Health Check analyzes components for security vulnerabilities and license issues.
                </Text>
              </Box>
            </Flex>

            {healthCheckLoading ? (
              <Text size="2" color="gray">
                Loading…
              </Text>
            ) : healthCheck?.enabled ? (
              <Flex direction="column" gap="3">
                <Flex align="center" gap="2">
                  <Box
                    style={{
                      width: 8, height: 8, borderRadius: '50%',
                      backgroundColor: healthCheck.analyzing ? 'var(--orange-9)' : 'var(--green-9)',
                    }}
                  />
                  <Text size="2" weight="medium" color={healthCheck.analyzing ? 'orange' : 'green'}>
                    {healthCheck.analyzing ? 'Analyzing…' : 'Enabled'}
                  </Text>
                </Flex>
                <Flex gap="2">
                  <Button
                    type="button"
                    variant="soft"
                    color="red"
                    size="2"
                    onClick={handleDisableHealthCheck}
                  >
                    Disable Health Check
                  </Button>
                </Flex>
              </Flex>
            ) : (
              <Flex direction="column" gap="3">
                <Flex align="center" gap="2">
                  <Box
                    style={{
                      width: 8, height: 8, borderRadius: '50%',
                      backgroundColor: 'var(--gray-8)',
                    }}
                  />
                  <Text size="2" weight="medium" color="gray">
                    Disabled
                  </Text>
                </Flex>
                <Flex gap="2">
                  <Button
                    type="button"
                    variant="solid"
                    color="blue"
                    size="2"
                    onClick={handleEnableHealthCheck}
                  >
                    Enable Health Check
                  </Button>
                </Flex>
              </Flex>
            )}
          </Flex>
        </Card>
      )}
    </Box>
  );
}

export default RepositoryFirewallConfigTab;
