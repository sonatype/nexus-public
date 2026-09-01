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
import {renderHook} from '@testing-library/react';

import {
  getMetricData,
  resolveThrottlingStatus,
  isTestSessionActive,
  useTestOverrideDetection,
  OVER_LIMITS,
  NEAR_LIMITS,
  UNDER_LIMITS,
  CE_REQUESTS_HARD_THRESHOLD,
  CE_COMPONENTS_HARD_THRESHOLD,
  STORAGE_KEY_CE_THROTTLING_STATUS,
  STORAGE_KEY_CE_GRACE_PERIOD_ENDS,
  STORAGE_KEY_CE_COMPONENTS,
  STORAGE_KEY_CE_REQUESTS,
} from '../usageHelperUtils';

describe('usageHelperUtils constants', () => {
  it('exports the throttle-status labels', () => {
    expect(OVER_LIMITS).toBe('Over limits');
    expect(NEAR_LIMITS).toBe('75% usage');
    expect(UNDER_LIMITS).toBe('Under limits');
  });

  it('exports the hard-threshold values', () => {
    expect(CE_REQUESTS_HARD_THRESHOLD).toBe(100000);
    expect(CE_COMPONENTS_HARD_THRESHOLD).toBe(40000);
  });

  it('exports the Test Hub localStorage keys', () => {
    expect(STORAGE_KEY_CE_THROTTLING_STATUS).toBe('SONATYPE_TEST_CE_THROTTLING_STATUS');
    expect(STORAGE_KEY_CE_GRACE_PERIOD_ENDS).toBe('SONATYPE_TEST_CE_GRACE_PERIOD_ENDS');
    expect(STORAGE_KEY_CE_COMPONENTS).toBe('SONATYPE_TEST_CE_COMPONENTS');
    expect(STORAGE_KEY_CE_REQUESTS).toBe('SONATYPE_TEST_CE_REQUESTS');
  });
});

describe('isTestSessionActive', () => {
  afterEach(() => sessionStorage.clear());

  it('is false when the session flag is absent', () => {
    expect(isTestSessionActive()).toBe(false);
  });

  it('is true when the session flag equals "1"', () => {
    sessionStorage.setItem('SONATYPE_TEST_CE_SESSION', '1');
    expect(isTestSessionActive()).toBe(true);
  });

  it('is false when the session flag has any other value', () => {
    sessionStorage.setItem('SONATYPE_TEST_CE_SESSION', 'true');
    expect(isTestSessionActive()).toBe(false);
  });
});

describe('getMetricData', () => {
  it('returns zeros when usage is undefined', () => {
    const result = getMetricData(undefined, 'peak_requests_per_day');
    expect(result.metricValue).toBe(0);
    expect(result.thresholdValue).toBe(0);
    expect(result.highestRecordedCount).toBe(0);
    expect(result.aggregates).toEqual([]);
  });

  it('returns zeros when metric is not found in the usage array', () => {
    const result = getMetricData([], 'missing_metric');
    expect(result.metricValue).toBe(0);
    expect(result.thresholdValue).toBe(0);
  });

  it('returns the metricValue from a matching entry', () => {
    const usage = [{metricName: 'peak_requests_per_day', metricValue: 42, thresholds: [], aggregates: []}];
    expect(getMetricData(usage, 'peak_requests_per_day').metricValue).toBe(42);
  });

  it('extracts thresholdValue from the HARD_THRESHOLD entry', () => {
    const usage = [{
      metricName: 'test',
      metricValue: 0,
      thresholds: [{thresholdName: 'HARD_THRESHOLD', thresholdValue: 500}],
      aggregates: [],
    }];
    expect(getMetricData(usage, 'test').thresholdValue).toBe(500);
  });

  it('extracts highestRecordedCount from peak_recorded_count_30d', () => {
    const usage = [{
      metricName: 'test',
      metricValue: 0,
      thresholds: [],
      aggregates: [{period: 'peak_recorded_count_30d', value: 999}],
    }];
    expect(getMetricData(usage, 'test').highestRecordedCount).toBe(999);
  });

  it('handles null thresholds and aggregates gracefully', () => {
    const usage = [{metricName: 'test', metricValue: 7, thresholds: null, aggregates: null}];
    const result = getMetricData(usage, 'test');
    expect(result.metricValue).toBe(7);
    expect(result.thresholdValue).toBe(0);
    expect(result.highestRecordedCount).toBe(0);
  });

  describe('localStorage test overrides', () => {
    beforeEach(() => sessionStorage.setItem('SONATYPE_TEST_CE_SESSION', '1'));
    afterEach(() => {
      localStorage.clear();
      sessionStorage.clear();
    });

    it('overrides component_total_count when SONATYPE_TEST_CE_COMPONENTS is set', () => {
      localStorage.setItem('SONATYPE_TEST_CE_COMPONENTS', '35000');
      const result = getMetricData([], 'component_total_count');
      expect(result.metricValue).toBe(35000);
      expect(result.thresholdValue).toBe(40000);
      expect(result.highestRecordedCount).toBe(35000);
    });

    it('overrides peak_requests_per_day when SONATYPE_TEST_CE_REQUESTS is set', () => {
      localStorage.setItem('SONATYPE_TEST_CE_REQUESTS', '80000');
      const result = getMetricData([], 'peak_requests_per_day');
      expect(result.metricValue).toBe(80000);
      expect(result.thresholdValue).toBe(100000);
      expect(result.highestRecordedCount).toBe(80000);
    });

    it('does not override component metric when only the requests key is set', () => {
      localStorage.setItem('SONATYPE_TEST_CE_REQUESTS', '80000');
      const usage = [{metricName: 'component_total_count', metricValue: 100, thresholds: [], aggregates: []}];
      expect(getMetricData(usage, 'component_total_count').metricValue).toBe(100);
    });

    it('ignores localStorage overrides when the session flag is not set (fresh login)', () => {
      sessionStorage.removeItem('SONATYPE_TEST_CE_SESSION');
      localStorage.setItem('SONATYPE_TEST_CE_COMPONENTS', '35000');
      const usage = [{metricName: 'component_total_count', metricValue: 100, thresholds: [], aggregates: []}];
      expect(getMetricData(usage, 'component_total_count').metricValue).toBe(100);
    });
  });
});

describe('resolveThrottlingStatus', () => {
  const IN_GRACE = 15;
  const AFTER_GRACE = -10;
  const NEVER_IN_GRACE = 365;

  it('returns NO_THROTTLING when the status is null', () => {
    expect(resolveThrottlingStatus(null, NEVER_IN_GRACE, true)).toBe('NO_THROTTLING');
  });

  it('returns NEAR_LIMITS_NON_ADMIN when near limits and not admin', () => {
    expect(resolveThrottlingStatus(NEAR_LIMITS, NEVER_IN_GRACE, false)).toBe('NEAR_LIMITS_NON_ADMIN');
  });

  it('returns NEAR_LIMITS_NEVER_IN_GRACE when near limits, never in grace, admin', () => {
    expect(resolveThrottlingStatus(NEAR_LIMITS, NEVER_IN_GRACE, true)).toBe('NEAR_LIMITS_NEVER_IN_GRACE');
  });

  it('returns OVER_LIMITS_IN_GRACE when over limits during grace as admin', () => {
    expect(resolveThrottlingStatus(OVER_LIMITS, IN_GRACE, true)).toBe('OVER_LIMITS_IN_GRACE');
  });

  it('returns BELOW_LIMITS_IN_GRACE when under limits during grace as admin', () => {
    expect(resolveThrottlingStatus(UNDER_LIMITS, IN_GRACE, true)).toBe('BELOW_LIMITS_IN_GRACE');
  });

  it('returns OVER_LIMITS_GRACE_PERIOD_ENDED when over limits after grace as admin', () => {
    expect(resolveThrottlingStatus(OVER_LIMITS, AFTER_GRACE, true)).toBe('OVER_LIMITS_GRACE_PERIOD_ENDED');
  });

  it('returns BELOW_LIMITS_GRACE_PERIOD_ENDED when under limits after grace as admin', () => {
    expect(resolveThrottlingStatus(UNDER_LIMITS, AFTER_GRACE, true)).toBe('BELOW_LIMITS_GRACE_PERIOD_ENDED');
  });

  it('returns NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED when over limits after grace as non-admin', () => {
    expect(resolveThrottlingStatus(OVER_LIMITS, AFTER_GRACE, false)).toBe('NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED');
  });

  it('treats diffInDays === 0 (grace ends today) as in-grace for OVER_LIMITS', () => {
    expect(resolveThrottlingStatus(OVER_LIMITS, 0, true)).toBe('OVER_LIMITS_IN_GRACE');
  });

  it('treats diffInDays === 0 (grace ends today) as in-grace for UNDER_LIMITS', () => {
    expect(resolveThrottlingStatus(UNDER_LIMITS, 0, true)).toBe('BELOW_LIMITS_IN_GRACE');
  });

  it('returns NO_THROTTLING and warns when OVER_LIMITS falls through (grace end beyond the window)', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    try {
      expect(resolveThrottlingStatus(OVER_LIMITS, NEVER_IN_GRACE, true)).toBe('NO_THROTTLING');
      expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('OVER_LIMITS fell through to NO_THROTTLING'));
    } finally {
      warnSpy.mockRestore();
    }
  });
});

describe('useTestOverrideDetection', () => {
  it('subscribes to storage events on mount and unsubscribes on unmount', () => {
    const addSpy = jest.spyOn(window, 'addEventListener');
    const removeSpy = jest.spyOn(window, 'removeEventListener');

    const {unmount} = renderHook(() => useTestOverrideDetection([STORAGE_KEY_CE_THROTTLING_STATUS]));
    expect(addSpy).toHaveBeenCalledWith('storage', expect.any(Function));

    unmount();
    expect(removeSpy).toHaveBeenCalledWith('storage', expect.any(Function));

    addSpy.mockRestore();
    removeSpy.mockRestore();
  });
});
