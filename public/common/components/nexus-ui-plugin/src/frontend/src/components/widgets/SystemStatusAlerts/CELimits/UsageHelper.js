/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {indexBy, pathOr, prop} from 'ramda';

const OVER_LIMITS = 'Over limits';
const NEAR_LIMITS = '75% usage';
const UNDER_LIMITS = 'Under limits';

function getMetricData(usage, metricName) {
  const data = usage?.find(m => m.metricName === metricName) ?? {};
  const { aggregates = [], thresholds = [], metricValue = 0 } = data;
  // Handle null values from API after session timeout by providing empty arrays
  const safeThresholds = thresholds ?? [];
  const safeAggregates = aggregates ?? [];
  const thresholdValue = pathOr(0, ['HARD_THRESHOLD', 'thresholdValue'], indexBy(prop('thresholdName'), safeThresholds));
  const highestRecordedCount = pathOr(0, ['peak_recorded_count_30d', 'value'], indexBy(prop('period'), safeAggregates));
  return { metricValue, thresholdValue, highestRecordedCount, aggregates: safeAggregates };
}

function addProductParams() {
  const nodeId = ExtJS.state().getValue('nexus.node.id');
  const usage = ExtJS.state().getValue('contentUsageEvaluationResult', []);
  const { metricValue: peakRequestsMetricValue, thresholdValue: peakRequestsThresholdValue, highestRecordedCount: highestRecordedCountPeakRequests } = getMetricData(usage, "peak_requests_per_day");
  const { metricValue: componentTotalMetricValue, thresholdValue: componentTotalThresholdValue, highestRecordedCount: highestRecordedCountComponentTotal } = getMetricData(usage, "component_total_count");
  const malwareCount = ExtJS.state().getValue('nexus.malware.count')?.totalCount || 0;

  const params =
      {
        nodeId: nodeId,
        componentCountLimit: componentTotalThresholdValue,
        componentCountMax: highestRecordedCountComponentTotal,
        componentCount: componentTotalMetricValue,
        requestsPer24HoursLimit: peakRequestsThresholdValue,
        requestsPer24HoursMax: highestRecordedCountPeakRequests,
        requestsPer24HoursCount: peakRequestsMetricValue,
        malwareCount: malwareCount
      };
  return new URLSearchParams(params).toString();
}

function useViewPurchaseALicenseUrl() {
  return `http://links.sonatype.com/products/nxrm3/ce/purchase-license?${addProductParams()}`;
}

function useViewLearnMoreUrl() {
  if (useThrottlingStatus() === 'OVER_LIMITS_GRACE_PERIOD_ENDED') {
    return `http://links.sonatype.com/products/nxrm3/ce/learn-more-limits-enforced?${addProductParams()}`;
  }
  return `http://links.sonatype.com/products/nxrm3/ce/learn-more?${addProductParams()}`;
}

function useGracePeriodEndsDate() {
  return new Date(ExtJS.state().getValue('nexus.community.gracePeriodEnds'));
}

function useThrottlingStatusValue () {
  return ExtJS.state().getValue('nexus.community.throttlingStatus');
}

function useGracePeriodEndDate() {
  const gracePeriodEnds = ExtJS.useState(useGracePeriodEndsDate);
  return gracePeriodEnds.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
}

function useDaysUntilGracePeriodEnds() {
  const gracePeriodEnds = ExtJS.useState(useGracePeriodEndsDate);
  const now = new Date();

  const diffInMs = gracePeriodEnds ?
    Date.UTC(gracePeriodEnds.getFullYear(), gracePeriodEnds.getMonth(), gracePeriodEnds.getDate()) -
    Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) : 0;
  const diffInDays = diffInMs / (1000 * 60 * 60 * 24);
  return diffInDays;
}

function useThrottlingStatus() {
  const throttlingStatus = ExtJS.useState(useThrottlingStatusValue);
  const diffInDays = useDaysUntilGracePeriodEnds();
  const duringGracePeriod = diffInDays <= 45 && diffInDays >= 0;
  const afterGracePeriod = diffInDays < 0;

  const isAdmin = ExtJS.useUser()?.administrator;

  if (throttlingStatus === NEAR_LIMITS && !isAdmin) {
    return 'NEAR_LIMITS_NON_ADMIN';
  } else if (throttlingStatus === NEAR_LIMITS && !duringGracePeriod && !afterGracePeriod) {
    return 'NEAR_LIMITS_NEVER_IN_GRACE';
  } else if (throttlingStatus === OVER_LIMITS && duringGracePeriod && isAdmin) {
    return 'OVER_LIMITS_IN_GRACE';
  } else if ((throttlingStatus === UNDER_LIMITS || throttlingStatus === NEAR_LIMITS) && duringGracePeriod && isAdmin) {
    return 'BELOW_LIMITS_IN_GRACE';
  } else if (throttlingStatus === OVER_LIMITS && !duringGracePeriod && afterGracePeriod && isAdmin) {
    return 'OVER_LIMITS_GRACE_PERIOD_ENDED';
  } else if ((throttlingStatus === NEAR_LIMITS || throttlingStatus === UNDER_LIMITS) && !duringGracePeriod && afterGracePeriod && isAdmin) {
    return 'BELOW_LIMITS_GRACE_PERIOD_ENDED';
  } else if (throttlingStatus === OVER_LIMITS && !duringGracePeriod && afterGracePeriod && !isAdmin) {
    return 'NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED';
  }
  return 'NO_THROTTLING';
}

export const helperFunctions = {
  useViewLearnMoreUrl,
  useViewPurchaseALicenseUrl,
  useGracePeriodEndDate,
  useThrottlingStatus,
  useGracePeriodEndsDate,
  useThrottlingStatusValue,
  getMetricData,
  OVER_LIMITS,
  NEAR_LIMITS,
  UNDER_LIMITS,
  useDaysUntilGracePeriodEnds,
};
