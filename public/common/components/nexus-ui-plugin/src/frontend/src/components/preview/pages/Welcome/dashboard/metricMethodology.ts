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

export type MetricType =
  | 'storage'
  | 'egress'
  | 'totalComponents'
  | 'peakRequestsPerDay'
  | 'peakRequestsPerMonth';

export interface MetricMethodologyContent {
  title: string;
  chartSection: string;
  whenCalculated: string;
  headerSection: string;
}

const METHODOLOGY: Record<MetricType, MetricMethodologyContent> = {
  storage: {
    title: 'Storage',
    chartSection:
      'The chart shows your instance’s peak storage over the last 12 months. Each point is the maximum blob store size observed during that month (measured daily at midnight).',
    whenCalculated:
      'Storage metrics are finalized at the end of each month. The aggregation task runs hourly, so the current month may show partial or zero data until the month is complete.',
    headerSection:
      'The header number is the latest month’s peak storage (in GB). If monthly metrics have no data yet (e.g. new instance or current month incomplete), we fall back to the current total size of all blob stores — that’s a live snapshot when you load the dashboard.',
  },
  egress: {
    title: 'Egress',
    chartSection:
      'The chart shows data transferred (bytes sent in HTTP responses to repository requests) over the last 12 months. Each point is the sum of all egress for that month.',
    whenCalculated:
      'Egress metrics are finalized at the end of each month. The aggregation task runs hourly. The current month often shows zero until it’s complete.',
    headerSection:
      "The header number is the latest month’s total egress (in GB). If the current month is incomplete, we use the last complete month’s value. ",
  },
  totalComponents: {
    title: 'Total Components',
    chartSection:
      'The chart shows total component count over the last 12 months. Each point comes from the monthly-metrics API.',
    whenCalculated:
      'Content usage evaluation runs hourly (task "Metric aggregation" in Settings → System → Tasks). The displayed value reflects the last evaluation run, not a live counter.',
    headerSection:
      'The header number is the current total count of components across all repositories. It’s computed by the content usage evaluator and updated when the hourly aggregation task runs.',
  },
  peakRequestsPerDay: {
    title: 'Peak Requests/Day',
    chartSection:
      'The chart shows the maximum number of repository requests in a single day for each of the last 12 months.',
    whenCalculated:
      'Content usage evaluation runs hourly. The peak is derived from aggregated request counts; data is finalized at the end of each month.',
    headerSection:
      'The header number is the highest single-day request count in the evaluated period. It’s updated when the hourly metric aggregation task runs (Settings → System → Tasks).',
  },
  peakRequestsPerMonth: {
    title: 'Peak Requests/Month',
    chartSection:
      'The chart shows the total repository requests per month over the last 12 months. Each point is the sum of all requests in that month.',
    whenCalculated:
      'Content usage evaluation runs hourly. Request counts are aggregated minute → hour → day → month. Monthly values are finalized at month end.',
    headerSection:
      'The header number is the highest monthly request count in the evaluated period. Updated by the hourly metric aggregation task.',
  },
};

export function getMetricMethodology(
  type: MetricType,
  isEgressTbd?: boolean
): MetricMethodologyContent {
  const base = METHODOLOGY[type];
  if (type === 'egress' && isEgressTbd) {
    return {
      ...base,
      headerSection:
        'TBD means your instance is new or has no egress data yet. Egress data will appear after the first full month of repository usage. The chart will populate as monthly metrics are finalized.',
    };
  }
  return base;
}
