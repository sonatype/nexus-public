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
import { MonthlyMetric } from './types';
import { USAGE_STRINGS } from './usageStrings';

export interface UsageContext {
  metrics: MonthlyMetric[];
  loadError: string | null;
  isPermissionError: boolean;
}

type UsageEvent =
  | { type: 'RETRY' }
  | { type: 'done.invoke.loadMetrics'; data: MonthlyMetric[] }
  | { type: 'error.platform.loadMetrics'; data: unknown };

async function loadMetrics(): Promise<MonthlyMetric[]> {
  const data = await restClient.get<MonthlyMetric[]>('service/rest/v1/monthly-metrics');
  return data ?? [];
}

export function createUsageMachine() {
  return createMachine<UsageContext, UsageEvent>(
    {
      id: 'usage',
      initial: 'loading',
      context: { metrics: [], loadError: null, isPermissionError: false },
      states: {
        loading: {
          entry: 'clearError',
          invoke: {
            id: 'loadMetrics',
            src: 'loadMetrics',
            onDone: { target: 'loaded', actions: 'setMetrics' },
            onError: { target: 'loadError', actions: 'setLoadError' },
          },
        },
        loaded: { on: { RETRY: 'loading' } },
        loadError: { on: { RETRY: 'loading' } },
      },
    },
    {
      actions: {
        setMetrics: assign((_ctx, event) => {
          const e = event as Extract<UsageEvent, { type: 'done.invoke.loadMetrics' }>;
          return {
            metrics: e.data ?? [],
            loadError: null,
            isPermissionError: false,
          };
        }),
        setLoadError: assign((_ctx, event) => {
          const e = event as Extract<UsageEvent, { type: 'error.platform.loadMetrics' }>;
          const apiError = parseApiError(e.data);
          return {
            loadError: isPermissionError(apiError)
              ? USAGE_STRINGS.PERMISSION_ERROR
              : apiError.message || USAGE_STRINGS.LOAD_ERROR,
            isPermissionError: isPermissionError(apiError),
          };
        }),
        clearError: assign({ loadError: (_c: UsageContext) => null, isPermissionError: (_c: UsageContext) => false }),
      },
      services: { loadMetrics: () => loadMetrics() },
    },
  );
}
