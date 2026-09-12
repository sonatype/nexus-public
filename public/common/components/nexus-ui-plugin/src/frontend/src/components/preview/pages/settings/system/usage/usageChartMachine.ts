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
import { restClient, parseApiError, isPermissionError } from '../../../../../../interface/api';
// Pure helpers reused from the legacy module (no React, no state).
import { getMonthOptions, getDateRange, KEY_EGRESS, KEY_STORAGE } from '../../../../../pages/admin/Usage/UsageInsightsUtils';
import { ChartDataPoint, MonthOption } from './types';
import { USAGE_STRINGS } from './usageStrings';

interface DailyPoint { date: string; bytes: number | string }
interface DailyResponse { data: DailyPoint[] }

export interface UsageChartContext {
  combinedData: ChartDataPoint[];
  monthOptions: MonthOption[];
  selectedMonth: MonthOption | null;
  dateFrom: string;
  dateTo: string;
  loadError: string | null;
  isPermissionError: boolean;
}

type UsageChartEvent =
  | { type: 'SELECT_MONTH'; month: MonthOption }
  | { type: 'RETRY' }
  | { type: 'done.invoke.fetchAll'; data: [DailyResponse, DailyResponse] }
  | { type: 'error.platform.fetchAll'; data: unknown };

function combine(egress: DailyPoint[], storage: DailyPoint[]): ChartDataPoint[] {
  const map = new Map<string, ChartDataPoint>();
  const put = (item: DailyPoint, key: 'egress' | 'storage') => {
    const point = map.get(item.date) ?? { metricDate: item.date, egress: 0, storage: 0 };
    const bytes = Number(item.bytes);
    point[key] = Number.isFinite(bytes) ? bytes : 0;
    map.set(item.date, point);
  };
  // egress/storage are always arrays here (setData defaults them before calling).
  egress.forEach((i) => put(i, KEY_EGRESS as 'egress'));
  storage.forEach((i) => put(i, KEY_STORAGE as 'storage'));
  // Guard against malformed dates: an invalid date yields NaN, which would make
  // the comparator non-deterministic. Treat unparseable dates as epoch 0.
  const toTime = (d: string) => {
    const t = new Date(d).getTime();
    return Number.isNaN(t) ? 0 : t;
  };
  return Array.from(map.values()).sort((a, b) => toTime(a.metricDate) - toTime(b.metricDate));
}

export function createUsageChartMachine() {
  return createMachine<UsageChartContext, UsageChartEvent>(
    {
      id: 'usageChart',
      initial: 'loading',
      context: {
        combinedData: [],
        monthOptions: [],
        selectedMonth: null,
        dateFrom: '',
        dateTo: '',
        loadError: null,
        isPermissionError: false,
      },
      states: {
        loading: {
          // clearError runs on success (onDone), not on entry, so an existing
          // error/permission banner stays visible during a RETRY re-fetch
          // instead of flickering off and back on.
          entry: 'ensureMonthSelection',
          invoke: {
            id: 'fetchAll',
            src: 'fetchAll',
            onDone: { target: 'loaded', actions: ['setData', 'clearError'] },
            onError: { target: 'loadError', actions: 'setError' },
          },
        },
        loaded: {
          on: {
            SELECT_MONTH: { target: 'loading', actions: 'selectMonth' },
            RETRY: 'loading',
          },
        },
        loadError: {
          on: {
            RETRY: 'loading',
            // The month Select stays interactive in the error UI: picking a
            // different month must re-fetch its range, not be silently dropped.
            SELECT_MONTH: { target: 'loading', actions: 'selectMonth' },
          },
        },
      },
    },
    {
      actions: {
        ensureMonthSelection: assign((ctx) => {
          if (ctx.monthOptions.length > 0) return {};
          const monthOptions = getMonthOptions() as MonthOption[];
          const selectedMonth = monthOptions[0] ?? null;
          const range = selectedMonth ? selectedMonth.value : getDateRange(new Date());
          return { monthOptions, selectedMonth, dateFrom: range.dateFrom, dateTo: range.dateTo };
        }),
        selectMonth: assign((_ctx, event) => {
          const { month } = event as Extract<UsageChartEvent, { type: 'SELECT_MONTH' }>;
          return { selectedMonth: month, dateFrom: month.value.dateFrom, dateTo: month.value.dateTo, combinedData: [] };
        }),
        setData: assign((_ctx, event) => {
          const e = event as Extract<UsageChartEvent, { type: 'done.invoke.fetchAll' }>;
          const [egressResp, storageResp] = e.data;
          return { combinedData: combine(egressResp?.data ?? [], storageResp?.data ?? []) };
        }),
        setError: assign((_ctx, event) => {
          const e = event as Extract<UsageChartEvent, { type: 'error.platform.fetchAll' }>;
          const apiError = parseApiError(e.data);
          return {
            loadError: isPermissionError(apiError) ? USAGE_STRINGS.PERMISSION_ERROR : apiError.message || USAGE_STRINGS.LOAD_ERROR,
            isPermissionError: isPermissionError(apiError),
          };
        }),
        clearError: assign({ loadError: (_c: UsageChartContext) => null, isPermissionError: (_c: UsageChartContext) => false }),
      },
      services: {
        fetchAll: (ctx) => {
          const params = { dateFrom: ctx.dateFrom, dateTo: ctx.dateTo };
          return Promise.all([
            restClient.get<DailyResponse>('service/rest/v1/daily-metrics/egress', { params }),
            restClient.get<DailyResponse>('service/rest/v1/daily-metrics/storage', { params }),
          ]);
        },
      },
    },
  );
}
