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

import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { helperFunctions } from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';
import type { InstanceTotals } from './simplified.types';

const { getMetricData } = helperFunctions;

/**
 * Metric names from the contentUsageEvaluationResult API.
 * These are the same metric names used in UsageCenter.jsx
 * 
 * IMPORTANT: Some metrics have different names for PostgreSQL vs H2 databases.
 * The hook must check isPostgresql to use the correct metric name.
 */
const METRIC_NAMES = {
  // Same for both H2 and PostgreSQL
  totalComponents: 'component_total_count',
  // Same for both H2 and PostgreSQL  
  peakRequestsPerMonth: 'highestMonthlyRequestMetrics',
  // Different for H2 vs PostgreSQL!
  peakRequestsPerDay: {
    h2: 'peak_requests_per_day',
    postgresql: 'peak_requests_per_day_30d',
  },
} as const;

interface UseInstanceTotalsResult {
  data: InstanceTotals | null;
  loading: boolean;
}

/**
 * Hook to get instance-wide usage metrics from ExtJS state.
 * 
 * Uses the same data source as UsageCenter.jsx:
 * - ExtJS.state().getValue('contentUsageEvaluationResult', [])
 * 
 * IMPORTANT: This hook must check the database type (PostgreSQL vs H2) to use
 * the correct metric names, matching the behavior in UsageCenter.jsx.
 * 
 * @returns Instance totals data and loading state
 */
export function useInstanceTotals(): UseInstanceTotalsResult {
  // Get usage data from ExtJS state (same as UsageCenter)
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult', []);
  
  // Check database type to use correct metric names (same logic as UsageCenter Card component)
  const isPostgres = ExtJS.state().getValue('datastore.isPostgresql');

  // If no data available, return null
  if (!usage || usage.length === 0) {
    return { data: null, loading: false };
  }

  // Extract metrics using the same getMetricData helper as UsageCenter
  // Use PostgreSQL-specific metric names when running on PostgreSQL
  const peakRequestsPerDayMetricName = isPostgres 
    ? METRIC_NAMES.peakRequestsPerDay.postgresql 
    : METRIC_NAMES.peakRequestsPerDay.h2;
    
  const { metricValue: totalComponents } = getMetricData(usage, METRIC_NAMES.totalComponents);
  const { metricValue: peakRequestsPerDay } = getMetricData(usage, peakRequestsPerDayMetricName);
  const { metricValue: peakRequestsPerMonth } = getMetricData(usage, METRIC_NAMES.peakRequestsPerMonth);

  return {
    data: {
      totalComponents,
      peakRequestsPerDay,
      peakRequestsPerMonth,
    },
    loading: false,
  };
}

export default useInstanceTotals;

