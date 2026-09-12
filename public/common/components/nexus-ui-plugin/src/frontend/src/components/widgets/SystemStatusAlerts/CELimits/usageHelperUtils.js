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

/**
 * Shared, ExtJS-free logic for the CE usage/throttling helpers.
 *
 * These pieces are pure (or React-only) and are consumed by the per-plugin
 * UsageHelper modules in nexus-ui-plugin and nexus-coreui-plugin. Keeping the
 * decision logic and metric parsing in one place ensures fixes (e.g. NEXUS-53215)
 * apply to every copy at once. The thin hooks that read ExtJS state stay local
 * to each plugin's UsageHelper.
 */
import React from 'react';
import {indexBy, pathOr, prop} from 'ramda';

// CE throttle status values
export const OVER_LIMITS = 'Over limits';
export const NEAR_LIMITS = '75% usage';
export const UNDER_LIMITS = 'Under limits';

// CE threshold constants
export const CE_REQUESTS_HARD_THRESHOLD = 100000;
export const CE_COMPONENTS_HARD_THRESHOLD = 40000;

// Length of the CE over-limit grace period, in days.
const GRACE_PERIOD_DAYS = 45;

// localStorage keys for Test Hub scenarios
export const STORAGE_KEY_CE_THROTTLING_STATUS = 'SONATYPE_TEST_CE_THROTTLING_STATUS';
export const STORAGE_KEY_CE_GRACE_PERIOD_ENDS = 'SONATYPE_TEST_CE_GRACE_PERIOD_ENDS';
export const STORAGE_KEY_CE_COMPONENTS = 'SONATYPE_TEST_CE_COMPONENTS';
export const STORAGE_KEY_CE_REQUESTS = 'SONATYPE_TEST_CE_REQUESTS';

// sessionStorage flag set by SonatypeTestHub when a CE scenario is active.
// Absent on fresh browser sessions so stale localStorage values from prior
// test runs are not applied on first login.
export function isTestSessionActive() {
  return typeof sessionStorage !== 'undefined' &&
    sessionStorage.getItem('SONATYPE_TEST_CE_SESSION') === '1';
}

export function getMetricData(usage, metricName) {
  const isActive = isTestSessionActive();
  const testComponents = isActive && typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY_CE_COMPONENTS);
  const testRequests = isActive && typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY_CE_REQUESTS);

  // If test overrides are set, use them for metric values
  let data = usage?.find(m => m.metricName === metricName) ?? {};
  if (testComponents || testRequests) {
    if (metricName === 'component_total_count' && testComponents) {
      data = {
        ...data,
        metricValue: parseInt(testComponents, 10),
        thresholds: [{thresholdName: 'HARD_THRESHOLD', thresholdValue: CE_COMPONENTS_HARD_THRESHOLD}],
        aggregates: [{name: 'component_total_count', value: parseInt(testComponents, 10), period: 'peak_recorded_count_30d'}],
      };
    } else if (metricName === 'peak_requests_per_day' && testRequests) {
      data = {
        ...data,
        metricValue: parseInt(testRequests, 10),
        thresholds: [{thresholdName: 'HARD_THRESHOLD', thresholdValue: CE_REQUESTS_HARD_THRESHOLD}],
        aggregates: [{name: 'content_request_count', value: parseInt(testRequests, 10), period: 'peak_recorded_count_30d'}],
      };
    }
  }

  const {aggregates, thresholds, metricValue = 0} = data;
  // Handle null/undefined values from API after session timeout by providing empty arrays
  const safeThresholds = thresholds ?? [];
  const safeAggregates = aggregates ?? [];
  const thresholdValue = pathOr(0, ['HARD_THRESHOLD', 'thresholdValue'], indexBy(prop('thresholdName'), safeThresholds));
  const highestRecordedCount = pathOr(0, ['peak_recorded_count_30d', 'value'], indexBy(prop('period'), safeAggregates));
  return {metricValue, thresholdValue, highestRecordedCount, aggregates: safeAggregates};
}

/**
 * Pure CE throttling-status decision.
 *
 * Given the raw throttling status, the number of days until the grace period
 * ends (negative once it has ended, 0 on the final day), and whether the
 * current user is an admin, returns the UI-facing throttling state.
 *
 * Grace period boundary: diffInDays === 0 means today is the exact end date,
 * which is still considered "in grace" (the last day of the grace period).
 * Only diffInDays < 0 is considered "after grace period".
 *
 * @param {string} throttlingStatus - raw status (OVER_LIMITS / NEAR_LIMITS / UNDER_LIMITS)
 * @param {number} diffInDays - days until grace period ends
 * @param {boolean} isAdmin - whether the current user is an administrator
 * @returns {string} the resolved throttling state, one of: 'NEAR_LIMITS_NON_ADMIN',
 *   'NEAR_LIMITS_NEVER_IN_GRACE', 'OVER_LIMITS_IN_GRACE', 'BELOW_LIMITS_IN_GRACE',
 *   'OVER_LIMITS_GRACE_PERIOD_ENDED', 'BELOW_LIMITS_GRACE_PERIOD_ENDED',
 *   'NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED', or 'NO_THROTTLING' (the default)
 */
export function resolveThrottlingStatus(throttlingStatus, diffInDays, isAdmin) {
  const duringGracePeriod = diffInDays <= GRACE_PERIOD_DAYS && diffInDays >= 0;
  const afterGracePeriod = diffInDays < 0;

  // Note: a grace period end date more than GRACE_PERIOD_DAYS in the future is
  // neither "during" nor "after" grace, so any status paired with it (including
  // OVER_LIMITS) falls through to NO_THROTTLING and shows no alert. The backend
  // caps the grace window at GRACE_PERIOD_DAYS, so this is not an expected state;
  // treating it as no-alert is the safe default rather than a silent over-limit.

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

  // Defensive diagnostic: an OVER_LIMITS status that matches none of the branches
  // above means the grace period end date is more than GRACE_PERIOD_DAYS in the
  // future (neither "during" nor "after" grace). The backend caps the grace window
  // at GRACE_PERIOD_DAYS, so this should be unreachable; log it so the silent
  // no-alert fall-through is diagnosable in production rather than invisible.
  if (throttlingStatus === OVER_LIMITS) {
    console.warn(
      `resolveThrottlingStatus: OVER_LIMITS fell through to NO_THROTTLING (diffInDays=${diffInDays}). ` +
      `Grace period end date is beyond the expected ${GRACE_PERIOD_DAYS}-day window; suppressing alert.`
    );
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
