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

import { assign, createMachine } from 'xstate';
import { restClient, ENDPOINTS } from '../../../../interface/api';
import type { SecurityStatusData } from './security.types';

/** Each request makes a live IQ Server call, so fetch a page in small batches instead of all at once. */
export const FIREWALL_COUNTS_CHUNK_SIZE = 4;

export interface FirewallCountsContext {
  /** Repositories on the current page whose firewall is enabled */
  targets: string[];
  /** Per-repository counts fetched so far; kept across pages so paging does not refetch */
  counts: Record<string, SecurityStatusData>;
  /** Repositories whose count request failed; other repositories are unaffected */
  failures: string[];
}

type FirewallCountsEvent =
  | { type: 'SET_TARGETS'; names: string[] }
  | { type: 'REFRESH' }
  | { type: 'COUNT_LOADED'; name: string; data: SecurityStatusData }
  | { type: 'COUNT_FAILED'; name: string }
  | { type: 'HALT' }
  | { type: 'POOL_DONE' };

/** A response carrying an errorMessage means IQ could not answer, which counts as a failure. */
export async function fetchRepoCounts(name: string): Promise<SecurityStatusData> {
  const data = await restClient.get<SecurityStatusData>(ENDPOINTS.FIREWALL_STATUS_REPO(name));
  if (!data || data.errorMessage) {
    throw new Error(data?.errorMessage ?? `No firewall counts returned for ${name}`);
  }
  return data;
}

const isPending = (ctx: FirewallCountsContext, name: string) =>
  !ctx.counts[name] && !ctx.failures.includes(name);

/**
 * Model for the inline Firewall violation counts in the repository list. Counts are fetched only
 * for the targeted (enabled, current-page) repositories, in batches of FIREWALL_COUNTS_CHUNK_SIZE.
 * A failed repository is recorded on its own and never blocks the others. The machine halts only
 * when a whole batch fails, which means IQ Server is down; REFRESH resumes it.
 */
export const firewallCountsMachine = createMachine<FirewallCountsContext, FirewallCountsEvent>(
  {
    id: 'firewallCounts',
    predictableActionArguments: true,
    initial: 'idle',
    context: { targets: [], counts: {}, failures: [] },
    states: {
      idle: {
        on: {
          SET_TARGETS: [
            { cond: 'eventHasPending', target: 'fetching', actions: 'setTargets' },
            { actions: 'setTargets' },
          ],
          REFRESH: [
            { cond: 'hasUnloaded', target: 'fetching', actions: 'clearFailures' },
            { actions: 'clearFailures' },
          ],
        },
      },
      fetching: {
        invoke: { id: 'countsLoader', src: 'countsLoader' },
        on: {
          COUNT_LOADED: { actions: 'storeCount' },
          COUNT_FAILED: { actions: 'storeFailure' },
          POOL_DONE: 'idle',
          HALT: 'halted',
          SET_TARGETS: [
            { cond: 'eventHasPending', target: 'fetching', actions: 'setTargets' },
            { target: 'idle', actions: 'setTargets' },
          ],
        },
      },
      halted: {
        on: {
          SET_TARGETS: { actions: 'setTargets' },
          REFRESH: [
            { cond: 'hasUnloaded', target: 'fetching', actions: 'clearFailures' },
            { target: 'idle', actions: 'clearFailures' },
          ],
        },
      },
    },
  },
  {
    guards: {
      hasUnloaded: (ctx) => ctx.targets.some((name) => !ctx.counts[name]),
      eventHasPending: (ctx, event) =>
        event.type === 'SET_TARGETS' && event.names.some((name) => isPending(ctx, name)),
    },
    actions: {
      setTargets: assign({
        targets: (_ctx, event) => (event.type === 'SET_TARGETS' ? event.names : []),
      }),
      storeCount: assign({
        counts: (ctx, event) =>
          event.type === 'COUNT_LOADED' ? { ...ctx.counts, [event.name]: event.data } : ctx.counts,
      }),
      storeFailure: assign({
        failures: (ctx, event) =>
          event.type === 'COUNT_FAILED' && !ctx.failures.includes(event.name)
            ? [...ctx.failures, event.name]
            : ctx.failures,
      }),
      clearFailures: assign({ failures: () => [] }),
    },
    services: {
      countsLoader: (ctx) => (send) => {
        let cancelled = false;
        const pending = ctx.targets.filter((name) => isPending(ctx, name));
        (async () => {
          for (let start = 0; start < pending.length && !cancelled; start += FIREWALL_COUNTS_CHUNK_SIZE) {
            const chunk = pending.slice(start, start + FIREWALL_COUNTS_CHUNK_SIZE);
            const results = await Promise.allSettled(chunk.map(fetchRepoCounts));
            if (cancelled) return;
            let failed = 0;
            results.forEach((result, index) => {
              if (result.status === 'fulfilled') {
                send({ type: 'COUNT_LOADED', name: chunk[index], data: result.value });
              } else {
                failed++;
                send({ type: 'COUNT_FAILED', name: chunk[index] });
              }
            });
            if (chunk.length > 1 && failed === chunk.length) {
              send({ type: 'HALT' });
              return;
            }
          }
          if (!cancelled) send({ type: 'POOL_DONE' });
        })();
        return () => {
          cancelled = true;
        };
      },
    },
  },
);
