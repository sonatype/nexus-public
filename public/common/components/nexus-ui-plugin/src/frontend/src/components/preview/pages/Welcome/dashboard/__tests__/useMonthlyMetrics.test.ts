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

import { renderHook, waitFor } from '@testing-library/react';
import { useMonthlyMetrics, formatBytesToGB } from '../useMonthlyMetrics';

jest.mock('../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(() => ({
      getValue: jest.fn((key: string, defaultValue?: unknown) => {
        if (key === 'isCloud') return true;
        return defaultValue ?? undefined;
      }),
    })),
  },
}));

const mockGet = jest.fn();

jest.mock('../../../../../../interface/api', () => {
  const actual = jest.requireActual('../../../../../../interface/api');
  return {
    ...actual,
    restClient: {
      get: (...args: unknown[]) => mockGet(...args),
    },
  };
});

describe('useMonthlyMetrics', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns self-hosted format (peakStorage, responseSize)', async () => {
    const selfHostedData = [
      {
        metricDate: '2024-02-01',
        peakStorage: 1073741824,
        responseSize: 536870912,
        requestCount: 100,
        componentCount: 50,
      },
    ];
    mockGet.mockResolvedValue(selfHostedData);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBe(1073741824);
    expect(result.current.responseSize).toBe(536870912);
    expect(result.current.error).toBeNull();
    expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/monthly-metrics');
  });

  it('returns Cloud format (storage, egress)', async () => {
    const cloudData = [
      {
        metricDate: '2024-02-01',
        storage: 2147483648,
        egress: 1073741824,
        requestCount: 200,
        componentCount: 100,
      },
    ];
    mockGet.mockResolvedValue(cloudData);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBe(2147483648);
    expect(result.current.responseSize).toBe(1073741824);
    expect(result.current.error).toBeNull();
  });

  it('handles Cloud "N/A" strings for storage and egress', async () => {
    const cloudNaData = [
      {
        metricDate: '2024-02-01',
        storage: 'N/A',
        egress: 'N/A',
        requestCount: 0,
        componentCount: 0,
      },
    ];
    mockGet.mockResolvedValue(cloudNaData);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBeNull();
    expect(result.current.responseSize).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('uses latest record (first element, API returns newest first)', async () => {
    const multipleMonths = [
      { metricDate: '2024-02-01', peakStorage: 1000, responseSize: 2000 },
      { metricDate: '2024-01-01', peakStorage: 500, responseSize: 1000 },
    ];
    mockGet.mockResolvedValue(multipleMonths);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBe(1000);
    expect(result.current.responseSize).toBe(2000);
  });

  it('returns 12-month history for charts (oldest first)', async () => {
    const multipleMonths = [
      { metricDate: '2024-02-01', peakStorage: 2000, responseSize: 4000, requestCount: 20, componentCount: 10 },
      { metricDate: '2024-01-01', peakStorage: 1000, responseSize: 2000, requestCount: 10, componentCount: 5 },
    ];
    mockGet.mockResolvedValue(multipleMonths);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.history.storage).toEqual([
      { date: '2024-01-01', value: 1000 },
      { date: '2024-02-01', value: 2000 },
    ]);
    expect(result.current.history.egress).toEqual([
      { date: '2024-01-01', value: 2000 },
      { date: '2024-02-01', value: 4000 },
    ]);
    expect(result.current.history.requests).toEqual([
      { date: '2024-01-01', value: 10 },
      { date: '2024-02-01', value: 20 },
    ]);
    expect(result.current.history.components).toEqual([
      { date: '2024-01-01', value: 5 },
      { date: '2024-02-01', value: 10 },
    ]);
  });

  it('handles empty array with null values', async () => {
    mockGet.mockResolvedValue([]);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBeNull();
    expect(result.current.responseSize).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('handles 404 gracefully (API may not exist in all editions)', async () => {
    const axios404 = Object.assign(new Error('Not Found'), {
      response: { status: 404 },
      isAxiosError: true,
    });
    mockGet.mockRejectedValue(axios404);

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBeNull();
    expect(result.current.responseSize).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('sets error on non-404 fetch failure', async () => {
    mockGet.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBeNull();
    expect(result.current.responseSize).toBeNull();
    expect(result.current.error).toBe('Network error');
  });

  it('handles non-array response defensively', async () => {
    mockGet.mockResolvedValue({ data: [] });

    const { result } = renderHook(() => useMonthlyMetrics());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.peakStorage).toBeNull();
    expect(result.current.responseSize).toBeNull();
    expect(result.current.error).toBeNull();
  });
});

describe('formatBytesToGB', () => {
  it('returns "N/A" for null or undefined', () => {
    expect(formatBytesToGB(null)).toBe('N/A');
    expect(formatBytesToGB(undefined)).toBe('N/A');
  });

  it('returns "N/A" for 0', () => {
    expect(formatBytesToGB(0)).toBe('N/A');
  });

  it('formats bytes as GB with 2 decimal places', () => {
    expect(formatBytesToGB(1e9)).toBe('1.00 GB');
    expect(formatBytesToGB(1073741824)).toBe('1.07 GB');
    expect(formatBytesToGB(536870912)).toBe('0.54 GB');
  });

  it('returns "0.00 GB" for 0 when allowZero is true', () => {
    expect(formatBytesToGB(0, true)).toBe('0.00 GB');
  });
});
