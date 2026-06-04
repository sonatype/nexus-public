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
import { renderHook, waitFor } from '@testing-library/react';
import { useUsageHistory } from '../useUsageHistory';

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../interface/api', () => ({
  restClient: {
    get: (...args) => mockRestClient.get(...args),
    post: (...args) => mockRestClient.post(...args),
    put: (...args) => mockRestClient.put(...args),
    delete: (...args) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.error || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
  isNotFoundError: jest.fn((apiError) => apiError?.status === 404),
  isPermissionError: jest.fn((apiError) => apiError?.status === 403),
}));

describe('useUsageHistory', () => {
  const mockRequestsDaily = {
    metric: 'requests',
    period: 'daily',
    data: [
      { date: '2026-01-17', value: 100 },
      { date: '2026-01-18', value: 150 },
    ]
  };

  const mockRequestsMonthly = {
    metric: 'requests',
    period: 'monthly',
    data: [
      { date: '2025-12', value: 1000 },
      { date: '2026-01', value: 1200 },
    ]
  };

  const mockComponentsDaily = {
    metric: 'components',
    period: 'daily',
    data: [
      { date: '2026-01-17', value: 50000 },
      { date: '2026-01-18', value: 51000 },
    ]
  };

  const mockComponentsMonthly = {
    metric: 'components',
    period: 'monthly',
    data: [
      { date: '2025-12', value: 45000 },
      { date: '2026-01', value: 51000 },
    ]
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockRestClient.get.mockImplementation((url) => {
      if (url.includes('metric=requests') && url.includes('period=daily')) {
        return Promise.resolve(mockRequestsDaily);
      }
      if (url.includes('metric=requests') && url.includes('period=monthly')) {
        return Promise.resolve(mockRequestsMonthly);
      }
      if (url.includes('metric=components') && url.includes('period=daily')) {
        return Promise.resolve(mockComponentsDaily);
      }
      if (url.includes('metric=components') && url.includes('period=monthly')) {
        return Promise.resolve(mockComponentsMonthly);
      }
      return Promise.reject({ message: 'Unknown endpoint' });
    });
  });

  it('fetches all metrics on mount', async () => {
    const { result } = renderHook(() => useUsageHistory());

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockRestClient.get).toHaveBeenCalledTimes(4);
    expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/usage-history?metric=requests&period=daily');
    expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/usage-history?metric=requests&period=monthly');
    expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/usage-history?metric=components&period=daily');
    expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/usage-history?metric=components&period=monthly');
  });

  it('returns all fetched data', async () => {
    const { result } = renderHook(() => useUsageHistory());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.requestsDaily).toEqual(mockRequestsDaily.data);
    expect(result.current.requestsMonthly).toEqual(mockRequestsMonthly.data);
    expect(result.current.componentsDaily).toEqual(mockComponentsDaily.data);
    expect(result.current.componentsMonthly).toEqual(mockComponentsMonthly.data);
    expect(result.current.error).toBeNull();
  });

  it('handles API errors', async () => {
    const errorMessage = 'Failed to fetch';
    mockRestClient.get.mockRejectedValue({
      response: { status: 500, data: { error: errorMessage } },
    });

    const { result } = renderHook(() => useUsageHistory());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe(errorMessage);
  });

  it('handles 404 gracefully (e.g. cloud deployment)', async () => {
    mockRestClient.get.mockRejectedValue({
      response: { status: 404 },
    });

    const { result } = renderHook(() => useUsageHistory());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.requestsDaily).toEqual([]);
    expect(result.current.requestsMonthly).toEqual([]);
    expect(result.current.componentsDaily).toEqual([]);
    expect(result.current.componentsMonthly).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('provides refresh function', async () => {
    const { result } = renderHook(() => useUsageHistory());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(typeof result.current.refresh).toBe('function');

    // Clear the mock call count
    mockRestClient.get.mockClear();

    // Call refresh
    result.current.refresh();

    await waitFor(() => {
      expect(mockRestClient.get).toHaveBeenCalledTimes(4);
    });
  });

  it('handles missing data gracefully', async () => {
    mockRestClient.get.mockResolvedValue({});

    const { result } = renderHook(() => useUsageHistory());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.requestsDaily).toEqual([]);
    expect(result.current.requestsMonthly).toEqual([]);
    expect(result.current.componentsDaily).toEqual([]);
    expect(result.current.componentsMonthly).toEqual([]);
  });
});

