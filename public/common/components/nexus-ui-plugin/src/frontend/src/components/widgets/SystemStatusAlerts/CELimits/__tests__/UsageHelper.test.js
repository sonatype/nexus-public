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

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: jest.fn(() => ({getValue: jest.fn()})),
    useState: jest.fn(),
    useUser: jest.fn(),
  },
}));

const {ExtJS} = jest.requireMock('@sonatype/nexus-ui-plugin');

const {
  getMetricData,
  useThrottlingStatus,
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
});

describe('exported constants', () => {
  it('exports OVER_LIMITS', () => expect(OVER_LIMITS).toBeDefined());
  it('exports NEAR_LIMITS', () => expect(NEAR_LIMITS).toBeDefined());
  it('exports UNDER_LIMITS', () => expect(UNDER_LIMITS).toBeDefined());
});
