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

import React, { useCallback, useState } from 'react';
import { Flex, Spinner, Switch, Table, Text, Tooltip } from '@radix-ui/themes';
import { CheckCircle } from 'lucide-react';
import { Flex, IconButton, Spinner, Switch, Table, Text, Tooltip } from '@radix-ui/themes';
import { CircleHelp } from 'lucide-react';
import { restClient, ENDPOINTS } from '@/utils/api';
import {
  disableFirewall,
  enableFirewallAudit,
  enableFirewallQuarantine,
} from '../../../shared/security/useFirewallEnable';
import { useToast } from '../../../shared';
import type { RepoWithProtection, ProtectionLevel } from '../MalwareRisk/useQuickActionsData';
import { isFirewallSupportedFormat } from '@/utils/firewallFormats';
import { isHealthCheckSupportedFormat } from '@/utils/healthCheckFormats';
import { FormatIcon } from '../../settings/repository/repositories/components/FormatIcon';
import {
  setMalwareRemediatorEnabledForRepository,
  type MalwareRemediatorMode,
} from '../../../shared/security/malwareRemediatorTask';

const MALWARE_MODE_LABELS: Record<MalwareRemediatorMode, string> = {
  disabled: 'Off',
  audit: 'Audit',
  delete: 'Delete',
};

export interface ProtectRepoRowProps {
  repo: RepoWithProtection;
  hasFirewallLicense: boolean;
  hasIqConnection: boolean;
  /** Matches browse list: nexus:healthcheck:update */
  canUpdateHealthCheck: boolean;
  hcInstanceEnabled: boolean;
  onRefetch: () => void;
  onRepoChanged?: (repoName: string) => void;
  hardened?: boolean;
}

export default function ProtectRepoRow({
  repo,
  hasFirewallLicense,
  hasIqConnection,
  canUpdateHealthCheck,
  hcInstanceEnabled,
  onRefetch,
  onRepoChanged,
  hardened,
}: ProtectRepoRowProps) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  const supported = isFirewallSupportedFormat(repo.format);
  const hcFormatSupported = isHealthCheckSupportedFormat(repo.format);
  const iqReady = hasIqConnection && hasFirewallLicense;

  const applyProtection = useCallback(
    async (level: ProtectionLevel) => {
      if (!iqReady || !supported) return;
      setBusy(true);
      try {
        if (level === 'none') await disableFirewall(repo.name);
        else if (level === 'audit') await enableFirewallAudit(repo.name);
        else await enableFirewallQuarantine(repo.name);
        onRepoChanged?.(repo.name);
        await onRefetch();
        const label =
          level === 'none' ? 'None' : level === 'audit' ? 'Audit' : 'Quarantine';
        toast.success(`Firewall protection set to ${label} for "${repo.name}"`);
      } catch (e) {
        toast.error(e instanceof Error ? e.message : String(e));
      } finally {
        setBusy(false);
      }
    },
    [iqReady, onRefetch, repo.name, supported, toast]
  );

  const toggleHc = useCallback(
    async (enabled: boolean) => {
      if (!hcInstanceEnabled) return;
      setBusy(true);
      try {
        if (enabled) {
          await restClient.post(ENDPOINTS.HEALTH_CHECK_ANALYZE(repo.name), {});
        } else {
          await restClient.delete(ENDPOINTS.REPOSITORY_HEALTH_CHECK(repo.name));
        }
        onRepoChanged?.(repo.name);
        await onRefetch();
        toast.success(
          enabled
            ? `Health Check enabled for "${repo.name}"`
            : `Health Check disabled for "${repo.name}"`
        );
      } catch (e) {
        toast.error(e instanceof Error ? e.message : String(e));
      } finally {
        setBusy(false);
      }
    },
    [hcInstanceEnabled, onRefetch, repo.name, toast]
  );

  const currentMalwareMode: MalwareRemediatorMode =
    !repo.taskEnabled ? 'disabled' : repo.taskCleanupEnabled ? 'delete' : 'audit';

  const applyMalwareMode = useCallback(
    async (mode: MalwareRemediatorMode) => {
      if (!iqReady || !supported || mode === currentMalwareMode) return;
      setBusy(true);
      try {
        await setMalwareRemediatorEnabledForRepository(repo.name, mode);
        onRepoChanged?.(repo.name);
        await onRefetch();
        toast.success(`Auto Remediation set to ${MALWARE_MODE_LABELS[mode]} for "${repo.name}"`);
      } catch (e) {
        toast.error(e instanceof Error ? e.message : String(e));
      } finally {
        setBusy(false);
      }
    },
    [currentMalwareMode, iqReady, onRefetch, repo.name, supported, toast]
  );

  if (hardened) {
    return (
      <Table.Row data-testid={`protect-repo-row-${repo.name}`} className="protect-repo-row--hardened">
        <Table.Cell colSpan={hasFirewallLicense ? 5 : 3}>
          <Flex align="center" gap="2">
            <CheckCircle size={16} color="var(--green-9)" />
            <Text size="2" color="green" weight="medium">
              {repo.name} — Fully hardened
            </Text>
          </Flex>
        </Table.Cell>
      </Table.Row>
    );
  }

  return (
    <Table.Row
      data-testid={`protect-repo-row-${repo.name}`}
      aria-busy={busy}
    >
          <Table.Cell>
            <Flex align="center" gap="2">
              <FormatIcon format={repo.format} size={18} />
              <Text size="2" weight="medium">
                {repo.name}
              </Text>
            </Flex>
          </Table.Cell>
          <Table.Cell>
            <Text size="1" color="gray">
              {repo.format}
            </Text>
          </Table.Cell>
          <Table.Cell style={{ textAlign: 'center' }}>
            {!hcFormatSupported || !repo.rhcSupported ? (
              <Tooltip content={
                !hcFormatSupported
                  ? `Health Check is not available for ${repo.format} repositories. Supported formats: Maven, npm, NuGet, PyPI, and others.`
                  : 'Health Check only supports Maven proxy repositories with a RELEASE version policy. SNAPSHOT and MIXED policies are not supported.'
              }>
                <Flex align="center" justify="center" gap="1" style={{ cursor: 'help' }}>
                  <Text size="2" color="gray">Not supported</Text>
                </Flex>
              </Tooltip>
            ) : !canUpdateHealthCheck ? (
              <Tooltip content="You do not have permission to manage Repository Health Check">
                <Text size="2" color="gray">
                  —
                </Text>
              </Tooltip>
            ) : !hcInstanceEnabled ? (
              <Tooltip content="Enable the Health Check capability for the instance">
                <Text size="2" color="gray">
                  —
                </Text>
              </Tooltip>
            ) : busy ? (
              <Spinner size="1" />
            ) : (
              <Flex align="center" justify="center" gap="2">
                <Switch checked={repo.rhcEnabled} onCheckedChange={(v) => void toggleHc(v)} disabled={busy} />
              </Flex>
            )}
          </Table.Cell>
          {hasFirewallLicense && (
            <Table.Cell style={{ textAlign: 'center' }}>
              {!supported ? (
                <Text size="2" color="gray">
                  Not supported
                </Text>
              ) : !hasIqConnection ? (
                <Tooltip content="Connect IQ Server to manage firewall protection">
                  <Text size="2" color="gray">
                    —
                  </Text>
                </Tooltip>
              ) : busy ? (
                <Spinner size="1" />
              ) : (
                <Flex gap="1" wrap="wrap" justify="center">
                  {(['none', 'audit', 'quarantine'] as const).map((level) => (
                    <label key={level} style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                      <input
                        type="radio"
                        name={`fw-${repo.name}`}
                        checked={repo.protection === level}
                        onChange={() => void applyProtection(level)}
                        disabled={busy}
                      />
                      <Text size="1" color={level === 'none' && repo.protection === 'none' ? 'red' : undefined}>
                        {level === 'none' ? 'None' : level === 'audit' ? 'Audit' : 'Quar.'}
                      </Text>
                    </label>
                  ))}
                </Flex>
              )}
            </Table.Cell>
          )}
          {hasFirewallLicense && (
            <Table.Cell style={{ textAlign: 'center' }}>
              {!supported ? (
                <Text size="2" color="gray">
                  Not supported
                </Text>
              ) : !hasIqConnection ? (
                <Tooltip content="Connect IQ Server to enable Auto Remediation">
                  <Text size="2" color="gray">
                    —
                  </Text>
                </Tooltip>
              ) : busy ? (
                <Spinner size="1" />
              ) : (
                <Flex gap="1" wrap="wrap" justify="center">
                  {(['disabled', 'audit', 'delete'] as const).map((mode) => (
                    <label key={mode} style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                      <input
                        type="radio"
                        name={`mp-${repo.name}`}
                        checked={currentMalwareMode === mode}
                        onChange={() => void applyMalwareMode(mode)}
                        disabled={busy}
                      />
                      <Text size="1" color={mode === 'disabled' && currentMalwareMode === 'disabled' ? 'red' : undefined}>
                        {MALWARE_MODE_LABELS[mode]}
                      </Text>
                    </label>
                  ))}
                </Flex>
              )}
            </Table.Cell>
          )}
    </Table.Row>
  );
}
