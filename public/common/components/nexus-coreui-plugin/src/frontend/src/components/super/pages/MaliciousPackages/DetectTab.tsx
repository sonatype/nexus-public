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

import React, { useEffect, useMemo, useRef } from 'react';
import { Callout, Text } from '@radix-ui/themes';
import { CheckCircle } from 'lucide-react';

import { DetectTable } from './DetectTable';
import { useToast } from '../../../../nosc';
import type { MaliciousPackagesDataSnapshot } from './useMaliciousPackagesData';
import { isSchedulerTaskRunningState } from './useMaliciousPackagesData';

interface DetectTabProps {
  data: MaliciousPackagesDataSnapshot;
  onIdentify: (repoName: string, signatureCount: number) => void;
  onNavigateToRemediate: (repoName: string) => void;
}

export function DetectTab({ data, onIdentify, onNavigateToRemediate }: DetectTabProps): React.ReactElement {
  const toast = useToast();
  const {
    proxyRepos,
    hcEnabledRepos,
    countsByRepo,
    rhcScans,
    tasks,
    activeFindings,
    enableRhc,
    malwareCount,
    identifyFailures,
  } = data;

  const toastRef = useRef(toast);
  toastRef.current = toast;

  const prevScanPhases = useRef<Map<string, string>>(new Map());
  const prevTaskStates = useRef<Map<string, string>>(new Map());

  useEffect(() => {
    const prev = prevScanPhases.current;
    const next = new Map<string, string>();

    for (const [name, scan] of rhcScans) {
      next.set(name, scan.phase);
      if (prev.get(name) === 'scanning' && scan.phase === 'completed' && (countsByRepo[name] ?? 0) === 0) {
        toastRef.current.success(`Scan complete — no malicious signatures found in "${name}"`);
      }
    }

    prevScanPhases.current = next;
  }, [rhcScans, countsByRepo]);

  useEffect(() => {
    const prev = prevTaskStates.current;
    const next = new Map<string, string>();
    const findingsByRepo = new Map<string, number>();
    for (const f of activeFindings) {
      findingsByRepo.set(f.repositoryName, (findingsByRepo.get(f.repositoryName) ?? 0) + 1);
    }

    for (const task of tasks) {
      const state = task.currentState ?? 'UNKNOWN';
      next.set(task.id, state);
      const prevState = prev.get(task.id);
      if (
        isSchedulerTaskRunningState(prevState) &&
        !isSchedulerTaskRunningState(state)
      ) {
        const repoFindings = findingsByRepo.get(task.repositoryName) ?? 0;
        if (state === 'WAITING' && repoFindings === 0) {
          toastRef.current.success(`Deep Scan complete — no malicious packages found in "${task.repositoryName}"`);
        }
      }
    }

    prevTaskStates.current = next;
  }, [tasks, activeFindings]);

  const hasBlindSpots = useMemo(
    () => proxyRepos.some((r) => r.rhcSupported && !r.rhcEnabled),
    [proxyRepos]
  );

  const hasActiveScans = rhcScans.size > 0;
  const allRepositoriesClear = !hasBlindSpots && !hasActiveScans && malwareCount === 0;

  if (allRepositoriesClear) {
    return (
      <Callout.Root color="green" data-testid="detect-tab-all-clear">
        <Callout.Icon>
          <CheckCircle size={16} />
        </Callout.Icon>
        <Callout.Text>
          All monitored repositories scanned — no malicious package signatures detected.{' '}
          <Text weight="bold">→ Continue to the Harden tab to ensure all repositories are protected.</Text>
        </Callout.Text>
      </Callout.Root>
    );
  }

  return (
    <DetectTable
      proxyRepos={proxyRepos}
      hcEnabledRepos={hcEnabledRepos}
      countsByRepo={countsByRepo}
      rhcScans={rhcScans}
      tasks={tasks}
      activeFindings={activeFindings}
      identifyFailures={identifyFailures}
      onEnableRhc={enableRhc}
      onIdentify={onIdentify}
      onNavigateToRemediate={onNavigateToRemediate}
    />
  );
}
