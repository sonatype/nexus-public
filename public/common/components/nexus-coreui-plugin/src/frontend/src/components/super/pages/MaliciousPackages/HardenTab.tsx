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

import React, { useMemo, useRef, useState, useCallback, useEffect } from 'react';
import { Box, Button, Callout, Flex, Heading, Text } from '@radix-ui/themes';
import { AlertTriangle, CheckCircle, ExternalLink } from 'lucide-react';

import ProtectQuickConfig from '../Protect/ProtectQuickConfig';
import type { ProtectDataSnapshot } from '../Protect/useProtectData';
import '../Protect/Protect.scss';
import { isFirewallSupportedFormat } from '@/utils/firewallFormats';
import { isHealthCheckSupportedFormat } from '@/utils/healthCheckFormats';

interface HardenTabProps {
  protectData: ProtectDataSnapshot;
}

function hasProtectionGap(r: { format: string; rhcEnabled: boolean; protection: string; taskEnabled: boolean }, hasFirewall: boolean): boolean {
  const hcGap = isHealthCheckSupportedFormat(r.format) && !r.rhcEnabled;
  const fwGap = isFirewallSupportedFormat(r.format) && hasFirewall && r.protection !== 'quarantine';
  const taskGap = isFirewallSupportedFormat(r.format) && hasFirewall && !r.taskEnabled;
  return hcGap || fwGap || taskGap;
}

export function computeHardenCount(protectData: ProtectDataSnapshot): number {
  const { repos, hasFirewall } = protectData;
  return repos.filter((r) => hasProtectionGap(r, hasFirewall)).length;
}

export function HardenTab({ protectData }: HardenTabProps): React.ReactElement {
  const gapsOnlyData = useMemo<ProtectDataSnapshot>(() => {
    const gapRepos = protectData.repos.filter((r) => hasProtectionGap(r, protectData.hasFirewall));
    return { ...protectData, repos: gapRepos };
  }, [protectData]);

  const userTouchedRef = useRef<Set<string>>(new Set());
  const [hardenedRepos, setHardenedRepos] = useState<Set<string>>(new Set());
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  const handleRepoChanged = useCallback((repoName: string) => {
    userTouchedRef.current.add(repoName);
  }, []);

  const gapNamesSet = useMemo(
    () => new Set(gapsOnlyData.repos.map((r) => r.name)),
    [gapsOnlyData.repos]
  );

  useEffect(() => {
    const touched = userTouchedRef.current;
    if (touched.size === 0) return;

    const newlyHardened: string[] = [];
    for (const name of touched) {
      if (!gapNamesSet.has(name)) {
        newlyHardened.push(name);
      }
    }

    if (newlyHardened.length === 0) return;

    for (const name of newlyHardened) {
      touched.delete(name);
    }

    setHardenedRepos((prev) => {
      const next = new Set(prev);
      for (const n of newlyHardened) next.add(n);
      return next;
    });

    for (const name of newlyHardened) {
      if (timersRef.current.has(name)) clearTimeout(timersRef.current.get(name));
      const timer = setTimeout(() => {
        setHardenedRepos((prev) => {
          const next = new Set(prev);
          next.delete(name);
          return next;
        });
        timersRef.current.delete(name);
      }, 4000);
      timersRef.current.set(name, timer);
    }
  }, [gapNamesSet]);

  useEffect(() => {
    const timers = timersRef.current;
    return () => { for (const t of timers.values()) clearTimeout(t); };
  }, []);

  const displayData = useMemo<ProtectDataSnapshot>(() => {
    if (hardenedRepos.size === 0) return gapsOnlyData;
    const hardenedRepoObjects = protectData.repos.filter((r) => hardenedRepos.has(r.name));
    const combined = [...gapsOnlyData.repos, ...hardenedRepoObjects];
    combined.sort((a, b) => a.name.localeCompare(b.name));
    return { ...protectData, repos: combined };
  }, [gapsOnlyData, hardenedRepos, protectData]);

  const unprotectedCount = gapsOnlyData.repos.length;
  const totalRepos = protectData.repos.length;

  return (
    <Flex direction="column" gap="4">
      {unprotectedCount === 0 ? (
        <Callout.Root color="green" data-testid="harden-tab-all-protected">
          <Callout.Icon>
            <CheckCircle size={16} />
          </Callout.Icon>
          <Callout.Text>
            All {totalRepos} proxy repositories fully hardened — Health Check, Firewall Quarantine, and Auto Remediation
            active.
            {' '}
            <Text weight="bold">
              → New repositories will appear here automatically when added. Review periodically as your environment
              grows.
            </Text>
          </Callout.Text>
        </Callout.Root>
      ) : (
        <Callout.Root color="amber" data-testid="harden-tab-gaps">
          <Callout.Icon>
            <AlertTriangle size={16} />
          </Callout.Icon>
          <Callout.Text>
            {unprotectedCount} of {totalRepos} proxy repositories have protection gaps.
            {' '}
            <Text weight="bold">
              → Enable Quarantine on each repository below. Audit mode logs threats but still serves malicious packages to
              developers.
            </Text>
          </Callout.Text>
        </Callout.Root>
      )}
      <ProtectQuickConfig
        protectData={displayData}
        onRepoChanged={handleRepoChanged}
        hardenedRepos={hardenedRepos}
      />
      <Box mt="4">
        <Heading size="3">Audit Trail</Heading>
        <Text size="2" color="gray" mt="2">
          Review detailed audit trails for all malicious package remediation actions.
        </Text>
        <Button size="2" variant="soft" mt="2" onClick={() => { window.location.hash = '#admin/system/audit'; }}>
          Open Audit Module <ExternalLink size={14} />
        </Button>
      </Box>
    </Flex>
  );
}
