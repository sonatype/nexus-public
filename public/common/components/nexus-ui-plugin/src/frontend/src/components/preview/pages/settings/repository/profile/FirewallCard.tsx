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

import React, { useState, useEffect, useRef } from 'react';
import { Box, Card, Flex, Text, Badge, Button, Tooltip, Grid, Separator } from '@radix-ui/themes';
import { Shield, ShieldCheck, Info, ExternalLink, ShieldOff } from 'lucide-react';
import type { FirewallData, IqCapabilities, MalwareCleanupSummary } from './types';
import { useFirewallEnable, fetchIqAuditStatus } from '../../../../shared/security/useFirewallEnable';
import {
  setMalwareRemediatorEnabledForRepository,
  type MalwareRemediatorMode,
} from '../../../../shared/security/malwareRemediatorTask';
import { useToast } from '../../../../shared/Toast';

export interface FirewallCardProps {
  repositoryName: string;
  firewall: FirewallData | null;
  malwareCleanupSummary: MalwareCleanupSummary | null;
  iqCapabilities: IqCapabilities | null;
  isSupported: boolean;
  refresh: () => void;
  onSelectTab?: (tab: string) => void;
}

type ProtectionLevel = 'unprotected' | 'audit' | 'quarantine';

const RADIX_COLOR: Record<ProtectionLevel, 'red' | 'amber' | 'green'> = {
  unprotected: 'red',
  audit: 'amber',
  quarantine: 'green',
};

const CSS_COLOR: Record<ProtectionLevel, string> = {
  unprotected: 'var(--red-9)',
  audit: 'var(--amber-9)',
  quarantine: 'var(--green-9)',
};

const LABELS: Record<ProtectionLevel, string> = {
  unprotected: 'Unprotected',
  audit: 'Audit',
  quarantine: 'Quarantine',
};

const DESCRIPTIONS: Record<ProtectionLevel, string> = {
  unprotected: 'No protection - components are not evaluated',
  audit: 'Evaluate components, log violations',
  quarantine: 'Evaluate and block risky components',
};

const MALWARE_TASK_LABELS: Record<MalwareRemediatorMode, string> = {
  disabled: 'Disabled',
  audit: 'Audit',
  delete: 'Delete',
};

const MALWARE_TASK_DESCRIPTIONS: Record<MalwareRemediatorMode, string> = {
  disabled: 'No scheduled scanning for malicious packages',
  audit: 'Scan and record malicious packages but do not remove them',
  delete: 'Scan, record, and automatically remove malicious packages (Recommended)',
};

const MALWARE_TASK_COLORS: Record<MalwareRemediatorMode, string> = {
  disabled: 'var(--gray-9)',
  audit: 'var(--amber-9)',
  delete: 'var(--green-9)',
};

function deriveProtectionLevel(firewall: FirewallData | null): ProtectionLevel {
  if (!firewall) return 'unprotected';
  if (firewall.quarantineEnabled) return 'quarantine';
  const msg = firewall.message?.toLowerCase() ?? '';
  if (msg.includes('quarantine')) return 'quarantine';
  if (msg.includes('audit')) return 'audit';
  if (msg.includes('enabled')) return 'audit';
  if (firewall.enabled) return 'audit';
  return 'unprotected';
}

export function FirewallCard({
  repositoryName,
  firewall,
  malwareCleanupSummary,
  iqCapabilities,
  isSupported,
  refresh,
  onSelectTab,
}: FirewallCardProps): JSX.Element {
  const toast = useToast();
  const { enableAudit, enableQuarantine, disable, loading: firewallLoading } = useFirewallEnable(repositoryName);
  const [taskLoading, setTaskLoading] = useState(false);

  const derivedLevel = deriveProtectionLevel(firewall);
  const [optimisticLevel, setOptimisticLevel] = useState<ProtectionLevel | null>(null);
  const protectionLevel = optimisticLevel ?? derivedLevel;
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clear optimistic override once the server-derived level catches up
  useEffect(() => {
    if (optimisticLevel && derivedLevel === optimisticLevel) {
      setOptimisticLevel(null);
    }
  }, [derivedLevel, optimisticLevel]);

  // Cleanup timer on unmount
  useEffect(() => {
    return () => {
      if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    };
  }, []);

  const handleProtectionChange = async (level: ProtectionLevel) => {
    if (level === protectionLevel || firewallLoading) return;
    try {
      if (level === 'unprotected') await disable();
      else if (level === 'audit') await enableAudit();
      else if (level === 'quarantine') await enableQuarantine();

      setOptimisticLevel(level);
      toast.success(`Firewall set to ${LABELS[level]} for ${repositoryName}`);

      // Schedule periodic refreshes so the server-derived level eventually
      // catches up and the useEffect above clears the optimistic override.
      const scheduleRefresh = (delay: number, remaining: number) => {
        if (remaining <= 0) return;
        refreshTimerRef.current = setTimeout(async () => {
          refresh();
          scheduleRefresh(delay, remaining - 1);
        }, delay);
      };
      scheduleRefresh(5000, 4);
    } catch (err) {
      setOptimisticLevel(null);
      toast.error(`Failed to update firewall: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  };

  const currentTaskMode: MalwareRemediatorMode =
    !malwareCleanupSummary?.taskEnabled
      ? 'disabled'
      : malwareCleanupSummary?.taskCleanupEnabled
        ? 'delete'
        : 'audit';

  const handleMalwareTaskModeChange = async (mode: MalwareRemediatorMode) => {
    if (mode === currentTaskMode || taskLoading) return;
    setTaskLoading(true);
    try {
      await setMalwareRemediatorEnabledForRepository(repositoryName, mode);
      const label = MALWARE_TASK_LABELS[mode];
      toast.success(`Malicious Packages set to ${label} for ${repositoryName}`);
      refresh();
    } catch (err) {
      console.error('Malicious Packages mode change error:', err);
      toast.error(`Failed to update Malicious Packages task: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setTaskLoading(false);
    }
  };

  const infoIcon = (content: string) => (
    <Tooltip content={content}>
      <Box style={{ cursor: 'help', display: 'inline-flex', alignItems: 'center' }}>
        <Info size={14} color="var(--gray-8)" />
      </Box>
    </Tooltip>
  );

  const isConnected = iqCapabilities?.connected;
  const hasFirewallLicense = iqCapabilities?.hasFirewall;
  const hasFirewallAccess = isConnected && hasFirewallLicense;

  if (!isSupported) {
    return (
      <Card size="2" style={{ flex: 1 }}>
        <Flex align="center" gap="3" height="100%">
          <ShieldOff size={24} color="var(--gray-7)" />
          <Box>
            <Flex align="center" gap="2">
              <Text size="2" weight="bold" color="gray">Firewall</Text>
              {infoIcon("Repository Firewall is not supported for this repository format.")}
            </Flex>
            <Text size="2" color="gray">Format not Supported</Text>
          </Box>
        </Flex>
      </Card>
    );
  }

  if (!hasFirewallAccess) {
    return (
      <Card size="2" style={{ flex: 1 }}>
        <Flex direction="column" gap="3">
          <Flex align="center" justify="between">
            <Flex align="center" gap="2">
              <Shield size={20} color="var(--gray-9)" />
              <Text weight="bold">Firewall</Text>
            </Flex>
            <Badge color="gray" variant="soft">No Firewall Access</Badge>
          </Flex>
          <Text size="2" color="gray">Repository Firewall protection is not available.</Text>
          <Button
            variant="soft"
            size="1"
            onClick={() => window.open('https://links.sonatype.com/nexus-repository-firewall', '_blank')}
            style={{ width: 'fit-content' }}
          >
            Learn More <ExternalLink size={14} />
          </Button>
        </Flex>
      </Card>
    );
  }

  const critical = firewall?.criticalComponentCount ?? 0;
  const severe = firewall?.severeComponentCount ?? 0;
  const moderate = firewall?.moderateComponentCount ?? 0;
  const affected = firewall?.affectedComponentCount ?? 0;
  const quarantined = firewall?.quarantinedComponentCount ?? 0;
  const totalViolations = critical + severe + moderate;
  const scrubbed = malwareCleanupSummary?.scrubbedCount ?? 0;
  const pending = malwareCleanupSummary?.pendingCount ?? 0;

  return (
    <Card size="2" style={{ flex: 1 }}>
      <Flex direction="column" gap="3">
        {/* ---- Header ---- */}
        <Flex align="center" justify="between">
          <Flex align="center" gap="2">
            <ShieldCheck size={20} color={CSS_COLOR[protectionLevel]} />
            <Text weight="bold">Firewall</Text>
            {infoIcon("Nexus Repository Firewall stops malicious and risky open source from entering your software supply chain.")}
          </Flex>
          <Badge color={RADIX_COLOR[protectionLevel]} variant="soft">
            {LABELS[protectionLevel]}
          </Badge>
        </Flex>

        {/* ---- Violation counts: Critical / Severe / Moderate ---- */}
        <Grid columns="3" gap="2">
          <Flex direction="column" align="center" p="2" style={{ backgroundColor: 'var(--red-2)', borderRadius: '4px' }}>
            <Text size="1" color="red">Critical</Text>
            <Text size="4" weight="bold" color="red">{critical.toLocaleString()}</Text>
          </Flex>
          <Flex direction="column" align="center" p="2" style={{ backgroundColor: 'var(--orange-2)', borderRadius: '4px' }}>
            <Text size="1" color="orange">Severe</Text>
            <Text size="4" weight="bold" color="orange">{severe.toLocaleString()}</Text>
          </Flex>
          <Flex direction="column" align="center" p="2" style={{ backgroundColor: 'var(--yellow-2)', borderRadius: '4px' }}>
            <Text size="1" color="yellow">Moderate</Text>
            <Text size="4" weight="bold" color="yellow">{moderate.toLocaleString()}</Text>
          </Flex>
        </Grid>

        {/* ---- Components + quarantine summary ---- */}
        <Text size="1" color="gray">
          {totalViolations.toLocaleString()} violations &middot; {affected.toLocaleString()} components affected
          {quarantined > 0 && ` · ${quarantined.toLocaleString()} quarantined`}
        </Text>

        {firewall?.errorMessage && (
          <Text size="2" color="red">{firewall.errorMessage}</Text>
        )}

        {/* ---- Firewall Dashboard link ---- */}
        {firewall?.reportUrl && (
          <Button
            variant="soft"
            size="1"
            onClick={() => window.open(firewall.reportUrl, '_blank')}
          >
            Firewall Dashboard <ExternalLink size={14} />
          </Button>
        )}

        <Separator size="4" />

        {/* ---- Protection Level (compact row) ---- */}
        <Box>
          <Flex align="center" gap="1" mb="2">
            <Text size="1" color="gray">Protection Level</Text>
            {infoIcon("Controls how Firewall handles policy violations for incoming components.")}
          </Flex>
          <Flex gap="2">
            {(['unprotected', 'audit', 'quarantine'] as const).map((level) => {
              const selected = protectionLevel === level;
              const color = CSS_COLOR[level];
              return (
                <Tooltip key={level} content={DESCRIPTIONS[level]}>
                  <Flex
                    align="center"
                    gap="1"
                    px="2"
                    py="1"
                    style={{
                      borderRadius: '6px',
                      border: `1.5px solid ${selected ? color : 'var(--gray-5)'}`,
                      backgroundColor: selected ? `${color}15` : 'transparent',
                      cursor: firewallLoading ? 'not-allowed' : 'pointer',
                      opacity: firewallLoading ? 0.6 : 1,
                      transition: 'all 0.15s ease',
                      flex: 1,
                      justifyContent: 'center',
                    }}
                    onClick={() => handleProtectionChange(level)}
                  >
                    <Box
                      style={{
                        width: 12,
                        height: 12,
                        borderRadius: '50%',
                        border: `2px solid ${selected ? color : 'var(--gray-7)'}`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      {selected && (
                        <Box style={{ width: 6, height: 6, borderRadius: '50%', backgroundColor: color }} />
                      )}
                    </Box>
                    <Text size="1" weight={selected ? 'bold' : 'regular'} style={{ color: selected ? color : undefined }}>
                      {LABELS[level]}
                    </Text>
                  </Flex>
                </Tooltip>
              );
            })}
          </Flex>
        </Box>

        {/* ---- Malicious Packages (3-option radio) ---- */}
        <Box>
          <Flex align="center" gap="1" mb="2">
            <Text size="1" color="gray">Malicious Packages</Text>
            {infoIcon("Scheduled task that scans for malicious packages. Disabled = off. Audit = scan and record only. Delete = scan and remove (Recommended).")}
          </Flex>
          <Flex gap="2" mb="2">
            {(['disabled', 'audit', 'delete'] as const).map((mode) => {
              const selected = currentTaskMode === mode;
              const color = MALWARE_TASK_COLORS[mode];
              return (
                <Tooltip key={mode} content={MALWARE_TASK_DESCRIPTIONS[mode]}>
                  <Flex
                    align="center"
                    gap="1"
                    px="2"
                    py="1"
                    style={{
                      borderRadius: '6px',
                      border: `1.5px solid ${selected ? color : 'var(--gray-5)'}`,
                      backgroundColor: selected ? `${color}15` : 'transparent',
                      cursor: taskLoading ? 'not-allowed' : 'pointer',
                      opacity: taskLoading ? 0.6 : 1,
                      transition: 'all 0.15s ease',
                      flex: 1,
                      justifyContent: 'center',
                    }}
                    onClick={() => handleMalwareTaskModeChange(mode)}
                  >
                    <Box
                      style={{
                        width: 12,
                        height: 12,
                        borderRadius: '50%',
                        border: `2px solid ${selected ? color : 'var(--gray-7)'}`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      {selected && (
                        <Box style={{ width: 6, height: 6, borderRadius: '50%', backgroundColor: color }} />
                      )}
                    </Box>
                    <Text size="1" weight={selected ? 'bold' : 'regular'} style={{ color: selected ? color : undefined }}>
                      {MALWARE_TASK_LABELS[mode]}
                    </Text>
                  </Flex>
                </Tooltip>
              );
            })}
          </Flex>
          <Flex gap="3" justify="end">
            <Text size="1" color="gray">{scrubbed} scrubbed</Text>
            {pending > 0 && <Text size="1" color="red">{pending} pending</Text>}
          </Flex>
        </Box>
      </Flex>
    </Card>
  );
}
