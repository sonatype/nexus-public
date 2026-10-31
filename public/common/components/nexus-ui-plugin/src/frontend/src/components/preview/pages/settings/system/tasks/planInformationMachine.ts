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
import { restClient } from '../../../../../../interface/api';

const PLAN_URL = '/service/rest/v1/plan';

/** Plan states that count as "active" for the aggregate (parity with Classic Tasks.js). */
export const ACTIVE_PLAN_STATES = ['PLANNED', 'EXECUTE', 'EXECUTING', 'EXECUTED'];

export interface ReconcilePlanXO {
  id?: number;
  repository?: string;
  blobStore?: string;
  state: string;
  configuration?: Record<string, string>;
}

interface PlanPage {
  items?: ReconcilePlanXO[];
  continuationToken?: string | null;
}

export interface PlanInformationContext {
  planCount: number;
  blobStoreCount: number;
  repositoryCount: number;
  startDate: string | null;
  endDate: string | null;
  error: string | null;
  /** True when MAX_PAGES was hit with a remaining continuationToken — counts may be incomplete. */
  truncated: boolean;
}

interface FetchPlansResult {
  items: ReconcilePlanXO[];
  truncated: boolean;
}

type PlanInformationEvent =
  | { type: 'RETRY' }
  | { type: 'done.invoke.fetchPlans'; data: FetchPlansResult }
  | { type: 'error.platform.fetchPlans'; data: Error };

const parseTime = (value: string | undefined): number | null => {
  if (!value) return null;
  const t = new Date(value).getTime();
  return Number.isNaN(t) ? null : t;
};

/** Aggregate active plans into the display context. Pure — exported for direct unit testing. */
export function aggregatePlans(plans: ReconcilePlanXO[]): Omit<PlanInformationContext, 'error' | 'truncated'> {
  const active = plans.filter((p) => ACTIVE_PLAN_STATES.includes(p.state));
  // Classic parity (Tasks.js updateReconciliationPlanInformation): count +1 per plan that has a
  // non-empty, non-"undefined" value — NOT the number of distinct values. Two plans sharing the
  // same blobStore name contribute 2 to the count, matching the ExtJS implementation exactly.
  const isValidValue = (v: string | undefined): boolean => !!v && v !== 'undefined';
  const blobStoreCount = active.filter((p) => isValidValue(p.blobStore)).length;
  const repositoryCount = active.filter((p) => isValidValue(p.repository)).length;

  let startDate: string | null = null;
  let endDate: string | null = null;
  let minStart = Infinity;
  let maxEnd = -Infinity;
  for (const p of active) {
    const s = parseTime(p.configuration?.planStartDate);
    const e = parseTime(p.configuration?.planEndDate);
    if (s !== null && s < minStart) { minStart = s; startDate = p.configuration?.planStartDate ?? null; }
    if (e !== null && e > maxEnd) { maxEnd = e; endDate = p.configuration?.planEndDate ?? null; }
  }
  return {
    planCount: active.length,
    blobStoreCount,
    repositoryCount,
    startDate,
    endDate,
  };
}

export const planInformationMachine = createMachine<PlanInformationContext, PlanInformationEvent>(
  {
    id: 'planInformation',
    initial: 'loading',
    context: {
      planCount: 0,
      blobStoreCount: 0,
      repositoryCount: 0,
      startDate: null,
      endDate: null,
      error: null,
      truncated: false,
    },
    states: {
      loading: {
        invoke: {
          id: 'fetchPlans',
          src: 'fetchPlans',
          onDone: { target: 'loaded', actions: 'setPlanInfo' },
          onError: { target: 'error', actions: 'setError' },
        },
      },
      loaded: {},
      error: {
        on: { RETRY: { target: 'loading', actions: 'clearError' } },
      },
    },
  },
  {
    actions: {
      setPlanInfo: assign((_ctx, event) => {
        if (event.type !== 'done.invoke.fetchPlans') return {};
        const { items, truncated } = event.data;
        return { ...aggregatePlans(items ?? []), error: null, truncated };
      }),
      setError: assign((_ctx, event) => {
        if (event.type !== 'error.platform.fetchPlans') return {};
        return { error: event.data?.message ?? 'Failed to load plan information' };
      }),
      clearError: assign({ error: null }),
    },
    services: {
      // Follow continuationToken so the aggregate covers every active plan, not just the first page.
      // MAX_PAGES bounds the loop so a malformed or repeating token can never spin forever / exhaust
      // memory — 100 pages is far beyond any realistic active-plan count for this read-only widget.
      fetchPlans: async (): Promise<FetchPlansResult> => {
        const all: ReconcilePlanXO[] = [];
        const MAX_PAGES = 100;
        let token: string | null | undefined;
        let pages = 0;
        do {
          const url = token ? `${PLAN_URL}?continuationToken=${encodeURIComponent(token)}` : PLAN_URL;
          const page = await restClient.get<PlanPage>(url);
          all.push(...(page?.items ?? []));
          token = page?.continuationToken;
          pages += 1;
        } while (token && pages < MAX_PAGES);
        // If we hit the page cap while a token still exists, the aggregate is incomplete.
        return { items: all, truncated: pages >= MAX_PAGES && !!token };
      },
    },
  },
);
