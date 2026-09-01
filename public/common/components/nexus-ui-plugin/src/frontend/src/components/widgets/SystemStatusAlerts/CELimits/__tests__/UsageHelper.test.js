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
import {helperFunctions} from '../UsageHelper';

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(() => ({getValue: jest.fn()})),
    useState: jest.fn(),
    useUser: jest.fn(),
  },
}));

const {ExtJS} = jest.requireMock('../../../../../interface/ExtJS');

const {
  getMetricData,
  useThrottlingStatus,
  useThrottlingStatusValue,
  useGracePeriodEndsDate,
  buildLearnMoreUrl,
  OVER_LIMITS,
  NEAR_LIMITS,
  UNDER_LIMITS,
} = helperFunctions;

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
});

describe('useThrottlingStatus', () => {
  // Today's date is fixed relative — past = afterGrace, near-future ≤ 45d = duringGrace
  function futureDate(daysFromNow) {
    const d = new Date();
    d.setDate(d.getDate() + daysFromNow);
    return d;
  }

  function setup({throttlingStatus, graceDaysFromNow, isAdmin = true}) {
    // useState is called twice: first for throttlingStatus, second for gracePeriodDate
    ExtJS.useState
      .mockReturnValueOnce(throttlingStatus)
      .mockReturnValueOnce(graceDaysFromNow != null ? futureDate(graceDaysFromNow) : new Date('2030-01-01'));
    ExtJS.useUser.mockReturnValue({administrator: isAdmin});
  }

  it('returns NO_THROTTLING when throttlingStatus is null', () => {
    setup({throttlingStatus: null, graceDaysFromNow: 365});
    expect(useThrottlingStatus()).toBe('NO_THROTTLING');
  });

  it('returns NEAR_LIMITS_NON_ADMIN when near limits and not admin', () => {
    setup({throttlingStatus: NEAR_LIMITS, graceDaysFromNow: 365, isAdmin: false});
    expect(useThrottlingStatus()).toBe('NEAR_LIMITS_NON_ADMIN');
  });

  it('returns NEAR_LIMITS_NEVER_IN_GRACE when near limits, not in grace, not after grace, admin', () => {
    setup({throttlingStatus: NEAR_LIMITS, graceDaysFromNow: 365, isAdmin: true});
    expect(useThrottlingStatus()).toBe('NEAR_LIMITS_NEVER_IN_GRACE');
  });

  it('returns OVER_LIMITS_IN_GRACE when over limits during grace period as admin', () => {
    setup({throttlingStatus: OVER_LIMITS, graceDaysFromNow: 15, isAdmin: true});
    expect(useThrottlingStatus()).toBe('OVER_LIMITS_IN_GRACE');
  });

  it('returns BELOW_LIMITS_IN_GRACE when under limits during grace period as admin', () => {
    setup({throttlingStatus: UNDER_LIMITS, graceDaysFromNow: 10, isAdmin: true});
    expect(useThrottlingStatus()).toBe('BELOW_LIMITS_IN_GRACE');
  });

  it('returns OVER_LIMITS_GRACE_PERIOD_ENDED when over limits after grace as admin', () => {
    setup({throttlingStatus: OVER_LIMITS, graceDaysFromNow: -10, isAdmin: true});
    expect(useThrottlingStatus()).toBe('OVER_LIMITS_GRACE_PERIOD_ENDED');
  });

  it('returns NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED when over limits after grace as non-admin', () => {
    setup({throttlingStatus: OVER_LIMITS, graceDaysFromNow: -10, isAdmin: false});
    expect(useThrottlingStatus()).toBe('NON_ADMIN_OVER_LIMITS_GRACE_PERIOD_ENDED');
  });

  it('returns BELOW_LIMITS_GRACE_PERIOD_ENDED when under limits after grace as admin', () => {
    setup({throttlingStatus: UNDER_LIMITS, graceDaysFromNow: -5, isAdmin: true});
    expect(useThrottlingStatus()).toBe('BELOW_LIMITS_GRACE_PERIOD_ENDED');
  });

  it('treats diffInDays === 0 (grace ends today) as in-grace for OVER_LIMITS', () => {
    // When today is exactly the grace period end date, it's still considered "in grace"
    setup({throttlingStatus: OVER_LIMITS, graceDaysFromNow: 0, isAdmin: true});
    expect(useThrottlingStatus()).toBe('OVER_LIMITS_IN_GRACE');
  });

  it('treats diffInDays === 0 (grace ends today) as in-grace for UNDER_LIMITS', () => {
    // When today is exactly the grace period end date, it's still considered "in grace"
    setup({throttlingStatus: UNDER_LIMITS, graceDaysFromNow: 0, isAdmin: true});
    expect(useThrottlingStatus()).toBe('BELOW_LIMITS_IN_GRACE');
  });
});

describe('buildLearnMoreUrl', () => {
  beforeEach(() => {
    ExtJS.state.mockReturnValue({
      getValue: jest.fn((key) => {
        if (key === 'nexus.node.id') return 'node-1';
        if (key === 'nexus.malware.count') return {totalCount: 3};
        return undefined;
      }),
    });
  });

  it('returns the limits-enforced learn-more URL when the grace period has ended', () => {
    const url = buildLearnMoreUrl('OVER_LIMITS_GRACE_PERIOD_ENDED');
    expect(url).toMatch(/^http:\/\/links\.sonatype\.com\/products\/nxrm3\/ce\/learn-more-limits-enforced\?/);
    expect(url).toContain('nodeId=node-1');
    expect(url).toContain('malwareCount=3');
  });

  it('returns the default learn-more URL for any other throttling status', () => {
    const url = buildLearnMoreUrl('NEAR_LIMITS_NEVER_IN_GRACE');
    expect(url).toMatch(/^http:\/\/links\.sonatype\.com\/products\/nxrm3\/ce\/learn-more\?/);
  });

  it('returns the default learn-more URL when throttlingStatus is undefined', () => {
    const url = buildLearnMoreUrl(undefined);
    expect(url).toMatch(/^http:\/\/links\.sonatype\.com\/products\/nxrm3\/ce\/learn-more\?/);
  });
});

describe('localStorage test overrides', () => {
  beforeEach(() => {
    // Simulate an active TestHub session so UsageHelper reads localStorage
    sessionStorage.setItem('SONATYPE_TEST_CE_SESSION', '1');
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('getMetricData', () => {
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

    it('does not override when no localStorage keys are set', () => {
      const usage = [{metricName: 'component_total_count', metricValue: 100, thresholds: [], aggregates: []}];
      expect(getMetricData(usage, 'component_total_count').metricValue).toBe(100);
    });

    it('does not override component metric when only requests key is set', () => {
      localStorage.setItem('SONATYPE_TEST_CE_REQUESTS', '80000');
      const usage = [{metricName: 'component_total_count', metricValue: 100, thresholds: [], aggregates: []}];
      expect(getMetricData(usage, 'component_total_count').metricValue).toBe(100);
    });

    it('ignores localStorage overrides when SONATYPE_TEST_CE_SESSION is not set (fresh login)', () => {
      sessionStorage.removeItem('SONATYPE_TEST_CE_SESSION');
      localStorage.setItem('SONATYPE_TEST_CE_COMPONENTS', '35000');
      const usage = [{metricName: 'component_total_count', metricValue: 100, thresholds: [], aggregates: []}];
      expect(getMetricData(usage, 'component_total_count').metricValue).toBe(100);
    });
  });

  describe('useThrottlingStatusValue', () => {
    beforeEach(() => {
      ExtJS.state.mockReturnValue({getValue: jest.fn()});
    });

    it('returns localStorage override when SONATYPE_TEST_CE_THROTTLING_STATUS is set', () => {
      localStorage.setItem('SONATYPE_TEST_CE_THROTTLING_STATUS', '75% usage');
      expect(useThrottlingStatusValue()).toBe('75% usage');
    });

    it('falls back to server state when no override is set', () => {
      ExtJS.state.mockReturnValue({getValue: jest.fn().mockReturnValue('Over limits')});
      expect(useThrottlingStatusValue()).toBe('Over limits');
    });

    it('ignores localStorage override when SONATYPE_TEST_CE_SESSION is not set (fresh login)', () => {
      sessionStorage.removeItem('SONATYPE_TEST_CE_SESSION');
      localStorage.setItem('SONATYPE_TEST_CE_THROTTLING_STATUS', '75% usage');
      ExtJS.state.mockReturnValue({getValue: jest.fn().mockReturnValue('Under limits')});
      expect(useThrottlingStatusValue()).toBe('Under limits');
    });
  });

  describe('useGracePeriodEndsDate', () => {
    beforeEach(() => {
      ExtJS.state.mockReturnValue({getValue: jest.fn()});
    });

    it('returns parsed date from SONATYPE_TEST_CE_GRACE_PERIOD_ENDS when set', () => {
      localStorage.setItem('SONATYPE_TEST_CE_GRACE_PERIOD_ENDS', '2025-12-31T00:00:00Z');
      const result = useGracePeriodEndsDate();
      expect(result).toBeInstanceOf(Date);
      expect(result.getFullYear()).toBe(2025);
      expect(result.getMonth()).toBe(11);
    });

    it('falls back to server state when no override is set', () => {
      ExtJS.state.mockReturnValue({getValue: jest.fn().mockReturnValue('2026-06-01T00:00:00Z')});
      const result = useGracePeriodEndsDate();
      expect(result).toBeInstanceOf(Date);
    });

    it('ignores localStorage override when SONATYPE_TEST_CE_SESSION is not set (fresh login)', () => {
      sessionStorage.removeItem('SONATYPE_TEST_CE_SESSION');
      localStorage.setItem('SONATYPE_TEST_CE_GRACE_PERIOD_ENDS', '2025-12-31T00:00:00Z');
      ExtJS.state.mockReturnValue({getValue: jest.fn().mockReturnValue(null)});
      expect(useGracePeriodEndsDate()).toBeInstanceOf(Date);
      // Should be the server's null-derived date, not 2025
      const result = useGracePeriodEndsDate();
      expect(result.getFullYear()).not.toBe(2025);
    });
  });
});

describe('exported constants', () => {
  it('exports OVER_LIMITS', () => expect(OVER_LIMITS).toBeDefined());
  it('exports NEAR_LIMITS', () => expect(NEAR_LIMITS).toBeDefined());
  it('exports UNDER_LIMITS', () => expect(UNDER_LIMITS).toBeDefined());
});
