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
import { renderHook, act } from '@testing-library/react';

import { useInstanceTotals } from '../useInstanceTotals';
import { ExtJS } from '../../../../../../interface/ExtJS';

// Mock ExtJS module — the hook polls ExtJS.state().getValue() directly
jest.mock('../../../../../../interface/ExtJS', () => {
  const mockStore = {
    usageData: [] as unknown,
    isPostgres: false as unknown,
  };

  return {
    ExtJS: {
      state: jest.fn(() => ({
        getValue: jest.fn((key: string) => {
          if (key === 'contentUsageEvaluationResult') {
            return mockStore.usageData;
          }
          if (key === 'datastore.isPostgresql') {
            return mockStore.isPostgres;
          }
          return null;
        }),
      })),
      __mockStore: mockStore,
    },
  };
});

// Access the mock store for setting test data
const mockStore = (ExtJS as unknown as { __mockStore: { usageData: unknown; isPostgres: unknown } }).__mockStore;

interface UsageEntry {
  metricName: string;
  metricValue?: number | null;
}

function setExtJsState(usage: UsageEntry[] | null | undefined, isPostgres: boolean) {
  mockStore.usageData = usage;
  mockStore.isPostgres = isPostgres;
}

const FULL_H2_USAGE: UsageEntry[] = [
  { metricName: 'component_total_count', metricValue: 42 },
  { metricName: 'peak_requests_per_day', metricValue: 7 },
];

const FULL_PG_USAGE: UsageEntry[] = [
  { metricName: 'component_total_count', metricValue: 42 },
  { metricName: 'peak_requests_per_day_30d', metricValue: 9 },
];

beforeEach(() => {
  mockStore.usageData = [];
  mockStore.isPostgres = false;
  jest.useFakeTimers();
});

afterEach(() => {
  jest.useRealTimers();
});

describe('useInstanceTotals', () => {
  it('returns loading=true with null data when usage is empty on mount', () => {
    setExtJsState([], false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current).toEqual({ data: null, loading: true });
  });

  it('returns loading=true with null data when usage is undefined', () => {
    setExtJsState(undefined, false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current).toEqual({ data: null, loading: true });
  });

  it('returns loading=true when only some metrics are present', () => {
    setExtJsState(
      [{ metricName: 'peak_requests_per_day', metricValue: 5 }],
      false
    );
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current).toEqual({ data: null, loading: true });
  });

  it('resolves loading after poll interval when data arrives', () => {
    setExtJsState([], false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current.loading).toBe(true);

    // Simulate data arriving before next poll
    setExtJsState(FULL_H2_USAGE, false);

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(result.current).toEqual({
      data: {
        totalComponents: 42,
        peakRequestsPerDay: 7,
        peakRequestsPerMonth: 0,
        totalComponentsLimit: 0,
        peakRequestsPerDayLimit: 0,
      },
      loading: false,
    });
  });

  it('returns loading=false with full data when both H2 metrics are present on mount', () => {
    setExtJsState(FULL_H2_USAGE, false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current).toEqual({
      data: {
        totalComponents: 42,
        peakRequestsPerDay: 7,
        peakRequestsPerMonth: 0,
        totalComponentsLimit: 0,
        peakRequestsPerDayLimit: 0,
      },
      loading: false,
    });
  });

  it('uses postgresql metric name when isPostgres=true', () => {
    setExtJsState(FULL_PG_USAGE, true);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current.loading).toBe(false);
    expect(result.current.data?.peakRequestsPerDay).toBe(9);
  });

  it('preserves a genuine zero distinct from missing', () => {
    const usageWithGenuineZero: UsageEntry[] = [
      { metricName: 'component_total_count', metricValue: 0 },
      { metricName: 'peak_requests_per_day', metricValue: 0 },
    ];
    setExtJsState(usageWithGenuineZero, false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current).toEqual({
      data: {
        totalComponents: 0,
        peakRequestsPerDay: 0,
        peakRequestsPerMonth: 0,
        totalComponentsLimit: 0,
        peakRequestsPerDayLimit: 0,
      },
      loading: false,
    });
  });

  it('treats metricValue null as missing (loading)', () => {
    const usageWithNullValue: UsageEntry[] = [
      { metricName: 'component_total_count', metricValue: null },
      { metricName: 'peak_requests_per_day', metricValue: 7 },
    ];
    setExtJsState(usageWithNullValue, false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current).toEqual({ data: null, loading: true });
  });

  it('keeps loading=false on subsequent poll even if state momentarily empties', () => {
    setExtJsState(FULL_H2_USAGE, false);
    const { result } = renderHook(() => useInstanceTotals());
    expect(result.current.loading).toBe(false);

    // Simulate state transiently clearing (e.g. during a refresh)
    setExtJsState([], false);

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(result.current.loading).toBe(false);
    expect(result.current.data).toEqual({
      totalComponents: 0,
      peakRequestsPerDay: 0,
      peakRequestsPerMonth: 0,
      totalComponentsLimit: 0,
      peakRequestsPerDayLimit: 0,
    });
  });
});
