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
import { Button, Dialog, Flex, Text } from '@radix-ui/themes';
import { restClient, ENDPOINTS } from '../../../../interface/api';
import {
  setMalwareRemediatorEnabledForRepository,
  fetchMalwareRemediatorTasks,
} from '../../shared/security/malwareRemediatorTask';
import { enableFirewallQuarantine } from '../../shared/security/useFirewallEnable';
import { useToast } from '../../shared';
import type { RepoWithProtection } from '../MalwareRisk/useQuickActionsData';
import { isFirewallSupportedFormat } from '../../../../utils/firewallFormats';

export type ProtectBulkAction = 'healthcheck' | 'firewall' | 'cleanup';

type ProgressCallback = (completed: number, total: number) => void;

async function runSequentially<T>(
  items: T[],
  fn: (item: T, index: number) => Promise<unknown>,
  onProgress?: ProgressCallback,
) {
  const results: PromiseSettledResult<unknown>[] = [];
  for (let i = 0; i < items.length; i++) {
    const result = await Promise.allSettled([fn(items[i], i)]);
    results.push(...result);
    onProgress?.(i + 1, items.length);
  }
  return results;
}

export interface ProtectBulkActionModalProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  action: ProtectBulkAction | null;
  candidates: RepoWithProtection[];
  onComplete: () => void;
}

export default function ProtectBulkActionModal({
  open,
  onOpenChange,
  action,
  candidates,
  onComplete,
}: ProtectBulkActionModalProps) {
  const toast = useToast();
  const [running, setRunning] = useState(false);
  const [progress, setProgress] = useState({ done: 0, total: 0 });

  const title =
    action === 'healthcheck'
      ? 'Enable Health Check on visible repositories'
      : action === 'firewall'
        ? 'Enable Firewall (Quarantine) on visible repositories'
        : action === 'cleanup'
          ? 'Enable Auto Remediation (Delete) on visible repositories'
          : '';

  const handleConfirm = useCallback(async () => {
    if (!action || candidates.length === 0) return;
    setRunning(true);
    setProgress({ done: 0, total: candidates.length });
    const onProgress: ProgressCallback = (done, total) => setProgress({ done, total });
    try {
      let results: PromiseSettledResult<unknown>[] = [];
      if (action === 'healthcheck') {
        results = await runSequentially(candidates, async (r) => {
          await restClient.post(ENDPOINTS.HEALTH_CHECK_ANALYZE(r.name), {});
        }, onProgress);
      } else if (action === 'firewall') {
        const supported = candidates.filter((r) => isFirewallSupportedFormat(r.format));
        setProgress({ done: 0, total: supported.length });
        results = await runSequentially(supported, async (r) => {
          await enableFirewallQuarantine(r.name);
        }, onProgress);
      } else if (action === 'cleanup') {
        const supported = candidates.filter((r) => isFirewallSupportedFormat(r.format));
        setProgress({ done: 0, total: supported.length });
        const existingTasks = await fetchMalwareRemediatorTasks();
        const baseCount = existingTasks.length;
        results = await runSequentially(supported, async (r, index) => {
          await setMalwareRemediatorEnabledForRepository(r.name, true, baseCount + index);
        }, onProgress);
      }

      const ok = results.filter((r) => r.status === 'fulfilled').length;
      const fail = results.length - ok;
      if (fail === 0) {
        toast.success(`Completed: ${ok} succeeded`);
      } else {
        toast.error(`Partial failure: ${ok} succeeded, ${fail} failed`);
      }
      onComplete();
      onOpenChange(false);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : String(e));
    } finally {
      setRunning(false);
      setProgress({ done: 0, total: 0 });
    }
  }, [action, candidates, onComplete, onOpenChange, toast]);

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Content style={{ maxWidth: 420 }} aria-describedby={undefined}>
        <Dialog.Title>{title}</Dialog.Title>
        <Text size="2" mt="2">
          Applies only to repositories currently shown in the table (search box and Filters sidebar). This will
          update {candidates.length} repositor{candidates.length === 1 ? 'y' : 'ies'}.
        </Text>
        {running && (
          <Text size="1" color="gray" mt="2">
            Updating {progress.done} / {progress.total}…
          </Text>
        )}
        <Flex gap="3" justify="end" mt="4">
          <Button variant="soft" onClick={() => onOpenChange(false)} disabled={running}>
            Cancel
          </Button>
          <Button onClick={() => void handleConfirm()} disabled={running || candidates.length === 0}>
            Confirm
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
