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

import { useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { useQuickActionsData, type RepoWithProtection } from '../MalwareRisk/useQuickActionsData';
import { useHealthCheckSummary, type HealthCheckSummary } from '../Welcome/useHealthCheckSummary';
import { useIqAudit, type IqAuditCounts } from '../MalwareRisk/useIqAudit';
import {
  isIqServerEnabled,
  canReadFirewallStatus,
  canUpdateHealthCheck,
} from '../browse/repository-list/useRepositoryList';
import { isFirewallSupportedFormat } from '../../../../utils/firewallFormats';
import { isHealthCheckSupportedFormat } from '../../../../utils/healthCheckFormats';
import { protectDataMachine, type ProtectIqCapabilities } from './protectDataMachine';

// Re-export ProtectIqCapabilities so consumers can import it from here.
export type { ProtectIqCapabilities } from './protectDataMachine';

export interface ProtectFilterCounts {
  formats: Map<string, number>;
  protection: Map<string, number>;
  healthCheck: { enabled: number; disabled: number; unsupported: number };
  cleanup: { delete: number; audit: number; off: number };
}

function buildFilterCounts(repos: RepoWithProtection[]): ProtectFilterCounts {
  const formats = new Map<string, number>();
  const protection = new Map<string, number>();
  let hcOn = 0;
  let hcOff = 0;
  let hcUnsupported = 0;
  let cleanupDelete = 0;
  let cleanupAudit = 0;
  let cleanupOff = 0;

  repos.forEach((r) => {
    formats.set(r.format, (formats.get(r.format) ?? 0) + 1);
    const p = isFirewallSupportedFormat(r.format) ? r.protection : 'unsupported';
    protection.set(p, (protection.get(p) ?? 0) + 1);
    if (!isHealthCheckSupportedFormat(r.format)) {
      hcUnsupported++;
    } else if (r.rhcEnabled) {
      hcOn++;
    } else {
      hcOff++;
    }
    if (r.taskEnabled && r.taskCleanupEnabled) {
      cleanupDelete++;
    } else if (r.taskEnabled) {
      cleanupAudit++;
    } else {
      cleanupOff++;
    }
  });

  return {
    formats,
    protection,
    healthCheck: { enabled: hcOn, disabled: hcOff, unsupported: hcUnsupported },
    cleanup: { delete: cleanupDelete, audit: cleanupAudit, off: cleanupOff },
  };
}

/** Single snapshot for Protect hub + tabs (one fetch pipeline, no duplicate IQ/HC calls). */
export interface ProtectDataSnapshot {
  repos: RepoWithProtection[];
  refetch: () => void;
  loading: boolean;
  /** Only repository table / quick-actions failures block the Quick Config error banner. */
  error: string | null;
  filterCounts: ProtectFilterCounts;
  hasFirewall: boolean;
  hasIqConnection: boolean;
  canUpdateHealthCheck: boolean;
  iqAudit: { counts: IqAuditCounts | null; loading: boolean; error: Error | null };
  hcSummary: HealthCheckSummary;
  hcInstanceEnabled: boolean;
  lastAnalyzedByRepo: Map<string, number | null>;
  /** From GET /service/rest/v1/iq/capabilities; null if the request failed or not yet loaded. */
  iqCapabilities: ProtectIqCapabilities | null;
}

export function useProtectData(): ProtectDataSnapshot {
  /** Same gate as preview Browse repository list firewall column (useRepositoryList.fetchFirewallSummary). */
  const iqConnected = isIqServerEnabled();
  const canReadFirewall = canReadFirewallStatus();
  const canHealthCheck = canUpdateHealthCheck();
  const firewallActionsAvailable = iqConnected && canReadFirewall;

  const { repos, loading: qaLoading, error: qaError, refetch } = useQuickActionsData(firewallActionsAvailable);
  const hcSummary = useHealthCheckSummary();
  /** Browse loads IQ audit when IQ is connected only — not nexus:iq-violation-summary:read (see BrowsePage). */
  const iqAudit = useIqAudit(iqConnected);

  const [state] = useMachine(protectDataMachine);
  const iqCapabilities = state.context.iqCapabilities;
  const hcInstanceEnabled = state.context.hcInstanceEnabled;
  const iqCapabilitiesLoading = state.matches('capabilities.loading');

  const lastAnalyzedByRepo = useMemo(() => {
    const m = new Map<string, number | null>();
    hcSummary.repos.forEach((r) => {
      m.set(r.repositoryName, r.lastAnalyzedDate ?? null);
    });
    return m;
  }, [hcSummary.repos]);

  const filterCounts = useMemo(() => buildFilterCounts(repos), [repos]);

  const loading = qaLoading || hcSummary.loading || iqAudit.loading || iqCapabilitiesLoading;
  const error = qaError;

  return {
    repos,
    refetch,
    loading,
    error,
    filterCounts,
    /** IQ connected and user may read firewall summary — matches preview #browse / repository-profile. */
    hasFirewall: firewallActionsAvailable,
    hasIqConnection: iqConnected,
    canUpdateHealthCheck: canHealthCheck,
    iqAudit,
    hcSummary,
    hcInstanceEnabled,
    lastAnalyzedByRepo,
    iqCapabilities,
  };
}
