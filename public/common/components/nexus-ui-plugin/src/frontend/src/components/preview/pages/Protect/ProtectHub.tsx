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

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Box, Tabs } from '@radix-ui/themes';
import { ExtJS } from '../../../../interface/ExtJS';
import MalwareBanner from '../../shared/security/MalwareBanner';
import ProtectOverview from './ProtectOverview';
import ProtectQuickConfig from './ProtectQuickConfig';
import { useProtectData } from './useProtectData';
import { canReadFirewallStatus } from '../browse/repository-list/useRepositoryList';
import UIStrings from '../../constants/UIStrings';
import './Protect.scss';

const MALWARE_MANAGEMENT_TASKS_COUNT = UIStrings.MALICIOUS_RISK.MALWARE_MANAGEMENT_TASKS_COUNT;

export type ProtectTabValue = 'overview' | 'quick-config';

const VALID_TABS: ProtectTabValue[] = ['overview', 'quick-config'];

function getTabFromHash(): ProtectTabValue | null {
  const hash = window.location.hash;
  const match = hash.match(/[?&]tab=([^&]*)/);
  const tab = match?.[1];
  return tab && VALID_TABS.includes(tab as ProtectTabValue) ? (tab as ProtectTabValue) : null;
}

function setTabInHash(tab: ProtectTabValue) {
  const hash = window.location.hash;
  const cleaned = hash.replace(/([?&])tab=[^&]*/g, '$1').replace(/[?&]$/, '').replace(/\?&/, '?');
  const separator = cleaned.includes('?') ? '&' : '?';
  window.location.hash = `${cleaned}${separator}tab=${tab}`;
}

function useMalwareCount() {
  const malwareData = ExtJS.useState(() => ExtJS.state()?.getValue?.('nexus.malware.count'));
  return malwareData?.totalCount ?? 0;
}

function useMalwareTasksCount() {
  return ExtJS.useState(() => ExtJS.state()?.getValue?.(MALWARE_MANAGEMENT_TASKS_COUNT)) ?? 0;
}

/**
 * Match preview Browse / repository-profile: IQ connected + nexus:iq-violation-summary:read.
 * (Do not require clm.hasFirewall; that hid firewall data when IQ was otherwise usable.)
 */
function useFirewallAuditEnabled() {
  const clm = ExtJS.useState(() => ExtJS.state()?.getValue?.('clm'));
  const iqOn = !!(clm?.enabled);
  if (!iqOn) {
    return false;
  }
  try {
    return canReadFirewallStatus();
  } catch {
    return false;
  }
}

type PageState = 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 'H';

function derivePageState(
  firewallAuditEnabled: boolean,
  malwareCount: number,
  tasksCount: number,
  iqAuditPartial: boolean,
  hasAnyProtection: boolean
): PageState {
  const hasMalware = malwareCount > 0;
  const implemented = tasksCount > 0 || hasAnyProtection;

  if (!firewallAuditEnabled) {
    return hasMalware ? 'B' : 'A';
  }
  if (!implemented) {
    return hasMalware ? 'D' : 'C';
  }
  if (iqAuditPartial) {
    return hasMalware ? 'F' : 'E';
  }
  return hasMalware ? 'H' : 'G';
}

export default function ProtectHub() {
  const protectData = useProtectData();
  const malwareCount = useMalwareCount();
  const tasksCount = useMalwareTasksCount();
  const firewallAuditEnabled = useFirewallAuditEnabled();
  const iqAuditPartial = protectData.iqAudit.counts?.isPartial ?? false;
  const protectedRepoCount =
    (protectData.iqAudit.counts?.reposProtected ?? 0) +
    (protectData.iqAudit.counts?.reposInAudit ?? 0);

  const state = useMemo(
    () =>
      derivePageState(
        firewallAuditEnabled,
        malwareCount,
        tasksCount,
        iqAuditPartial,
        protectedRepoCount > 0
      ),
    [firewallAuditEnabled, malwareCount, tasksCount, iqAuditPartial, protectedRepoCount]
  );

  const defaultTab: ProtectTabValue =
    state === 'D' || state === 'F' || state === 'H' ? 'quick-config' : 'overview';

  const [activeTab, setActiveTabState] = useState<ProtectTabValue>(
    () => getTabFromHash() ?? defaultTab
  );

  useEffect(() => {
    const handleHashChange = () => {
      const tabFromHash = getTabFromHash();
      if (tabFromHash && tabFromHash !== activeTab) {
        setActiveTabState(tabFromHash);
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [activeTab]);

  const setActiveTab = useCallback((tab: ProtectTabValue) => {
    setActiveTabState(tab);
    setTabInHash(tab);
  }, []);

  return (
    <Box className="nxrm-protect-hub">
      <MalwareBanner nonDismissible />
      <Tabs.Root
        className="nxrm-protect-hub__tabs"
        value={activeTab}
        onValueChange={(v) => setActiveTab(v as ProtectTabValue)}
      >
        <Tabs.List>
          <Tabs.Trigger value="overview">Overview</Tabs.Trigger>
          <Tabs.Trigger value="quick-config">Quick Config</Tabs.Trigger>
        </Tabs.List>
        <Tabs.Content value="overview">
          <ProtectOverview
            protectData={protectData}
            onGoToQuickConfig={() => setActiveTab('quick-config')}
          />
        </Tabs.Content>
        <Tabs.Content value="quick-config">
          <ProtectQuickConfig protectData={protectData} />
        </Tabs.Content>
      </Tabs.Root>
    </Box>
  );
}
