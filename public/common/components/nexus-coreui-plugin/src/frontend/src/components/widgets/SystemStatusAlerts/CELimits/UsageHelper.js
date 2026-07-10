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
import React from 'react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {indexBy, pathOr, prop} from 'ramda';

// CE throttle status values
export const OVER_LIMITS = 'Over limits';
export const NEAR_LIMITS = '75% usage';
export const UNDER_LIMITS = 'Under limits';

// CE threshold constants
export const CE_REQUESTS_HARD_THRESHOLD = 100000;
export const CE_COMPONENTS_HARD_THRESHOLD = 40000;

// localStorage keys for Test Hub scenarios
export const STORAGE_KEY_CE_THROTTLING_STATUS = 'SONATYPE_TEST_CE_THROTTLING_STATUS';
export const STORAGE_KEY_CE_GRACE_PERIOD_ENDS = 'SONATYPE_TEST_CE_GRACE_PERIOD_ENDS';
export const STORAGE_KEY_CE_COMPONENTS = 'SONATYPE_TEST_CE_COMPONENTS';
export const STORAGE_KEY_CE_REQUESTS = 'SONATYPE_TEST_CE_REQUESTS';

function getMetricData(usage, metricName) {
  // Check for test override values (used by Test Hub scenarios)
  // Only use localStorage if it has truthy values (Test Hub explicitly sets values)
  const testComponents = typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_TEST_CE_COMPONENTS');
  const testRequests = typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_TEST_CE_REQUESTS');

  // If test overrides are set, use them for metric values
  let data = usage?.find(m => m.metricName === metricName) ?? {};
  if (testComponents || testRequests) {
    if (metricName === 'component_total_count' && testComponents) {
      data = {
        ...data,
        metricValue: parseInt(testComponents, 10),
        thresholds: [{ thresholdName: 'HARD_THRESHOLD', thresholdValue: CE_COMPONENTS_HARD_THRESHOLD }],
        aggregates: [{ name: 'component_total_count', value: parseInt(testComponents, 10), period: 'peak_recorded_count_30d' }]
      };
    } else if (metricName === 'peak_requests_per_day' && testRequests) {
      data = {
        ...data,
        metricValue: parseInt(testRequests, 10),
        thresholds: [{ thresholdName: 'HARD_THRESHOLD', thresholdValue: CE_REQUESTS_HARD_THRESHOLD }],
        aggregates: [{ name: 'content_request_count', value: parseInt(testRequests, 10), period: 'peak_recorded_count_30d' }]
      };
    }
  }

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
  // Check for test override first (used by Test Hub scenarios)
  // Only use localStorage if it has a truthy value (Test Hub explicitly sets values)
  const testGracePeriod = typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_TEST_CE_GRACE_PERIOD_ENDS');
  if (testGracePeriod) {
    return new Date(testGracePeriod);
  }
  return new Date(ExtJS.state().getValue('nexus.community.gracePeriodEnds'));
}

function useThrottlingStatusValue () {
  // Check for test override first (used by Test Hub scenarios)
  // Only use localStorage if it has a truthy value (Test Hub explicitly sets values)
  const testOverride = typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_TEST_CE_THROTTLING_STATUS');
  if (testOverride) {
    return testOverride;
  }
  return ExtJS.state().getValue('nexus.community.throttlingStatus');
}

function useEdition() {
  return ExtJS.useState(() => ExtJS.state().getEdition());
}

function useCommunityEdition() {
  // Always call useEdition() to maintain hook ordering (React Rules of Hooks)
  const edition = useEdition();

  // Check for test override after hook call (used by Test Hub scenarios)
  // Only use localStorage if it has a truthy value (Test Hub explicitly sets values)
  const testOverride = typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_TEST_CE_THROTTLING_STATUS');
  if (testOverride) {
    return true; // Test scenarios always simulate CE mode
  }

  return edition === 'COMMUNITY';
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

/**
 * Custom hook to force re-render when specified localStorage keys change.
 * Used by Test Hub scenarios to enable live preview of different CE states.
 *
 * @param {string[]} storageKeys - Array of localStorage keys to watch
 * @returns {void} - Triggers re-render when any watched key changes
 */
export function useTestOverrideDetection(storageKeys) {
  const [, forceUpdate] = React.useReducer(x => x + 1, 0);

  React.useEffect(() => {
    const handleStorageChange = (e) => {
      if (storageKeys.includes(e.key)) {
        forceUpdate();
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [storageKeys]);
}

export const helperFunctions = {
  useViewLearnMoreUrl,
  useViewPurchaseALicenseUrl,
  useGracePeriodEndDate,
  useThrottlingStatus,
  useGracePeriodEndsDate,
  useThrottlingStatusValue,
  getMetricData,
  useDaysUntilGracePeriodEnds,
  useEdition,
  useCommunityEdition,
  OVER_LIMITS,
  NEAR_LIMITS,
  UNDER_LIMITS,
};
