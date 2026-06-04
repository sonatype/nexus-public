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
import { ExtJS } from '../../../../interface/ExtJS';
import {
  useInstanceTotals,
  useMonthlyMetrics,
  useInstanceStorage,
  formatBytesToGB,
} from './dashboard';

export interface MonthlyMetricsFormatted {
  peakStorageGB?: string;
  responseSizeGB: string;
  isEgressTbd: boolean;
}

export function useUsageMetricsTabData() {
  const isCloud: boolean = ExtJS.state().getValue('isCloud', false);
  const instanceTotals = useInstanceTotals();
  const monthlyMetrics = useMonthlyMetrics();
  const instanceStorage = useInstanceStorage();

  let monthlyMetricsFormatted: MonthlyMetricsFormatted | undefined;

  if (!monthlyMetrics.loading) {
    const storageBytes: number | null =
      (monthlyMetrics.peakStorage != null && monthlyMetrics.peakStorage > 0
        ? monthlyMetrics.peakStorage
        : null) ?? (instanceStorage.currentStorageBytes ?? null);

    const latestEgress = monthlyMetrics.responseSize ?? 0;
    const lastKnownEgress =
      monthlyMetrics.history?.egress?.slice().reverse().find((p: {value: number}) => p.value > 0)?.value ?? 0;
    const egressBytes = latestEgress > 0 ? latestEgress : lastKnownEgress;
    const hasAnyEgressHistory = (monthlyMetrics.history?.egress ?? []).some((p: {value: number}) => p.value > 0);
    const isEgressTbd = egressBytes === 0 && !hasAnyEgressHistory;

    const peakStorageGB =
      storageBytes != null && storageBytes > 0 ? formatBytesToGB(storageBytes) : undefined;

    const out: MonthlyMetricsFormatted = {
      responseSizeGB: isEgressTbd ? 'TBD' : formatBytesToGB(egressBytes, true),
      isEgressTbd,
    };
    if (peakStorageGB !== undefined) {
      out.peakStorageGB = peakStorageGB;
    }
    monthlyMetricsFormatted = out;
  }

  return {isCloud, instanceTotals, monthlyMetrics, monthlyMetricsFormatted};
}
