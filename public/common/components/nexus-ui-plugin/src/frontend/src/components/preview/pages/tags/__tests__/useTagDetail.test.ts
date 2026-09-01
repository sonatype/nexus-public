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

import { renderHook, act, waitFor } from '@testing-library/react';

import { useTagDetail } from '../hooks/useTagDetail';
import * as tagsApi from '../tags.api';
import { mockTagDetail } from './mockData';

// Mock the API
jest.mock('../tags.api');
const mockedFetchTagDetail = tagsApi.fetchTagDetail as jest.MockedFunction<
  typeof tagsApi.fetchTagDetail
>;

// The tagDetailMachine also loads the component list and total count directly
// through restClient during its initial load, so mock that too.
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    delete: jest.fn(),
  },
}));

import { restClient } from '../../../../../interface/api';

const mockRestClientGet = restClient.get as jest.MockedFunction<typeof restClient.get>;

describe('useTagDetail', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default: component search + total-count lookups resolve with an empty page.
    mockRestClientGet.mockResolvedValue({ items: [] });
  });

  it('should fetch tag detail on mount', async () => {
    mockedFetchTagDetail.mockResolvedValue(mockTagDetail);

    const { result } = renderHook(() => useTagDetail('release-1.0'));

    // Initially loading
    expect(result.current.state.loading).toBe(true);
    expect(result.current.state.tagDetail).toBeNull();
    expect(result.current.state.error).toBeNull();

    // Wait for data to load
    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(mockedFetchTagDetail).toHaveBeenCalledTimes(1);
    expect(mockedFetchTagDetail).toHaveBeenCalledWith('release-1.0');
    expect(result.current.state.tagDetail).toEqual(mockTagDetail);
    expect(result.current.state.error).toBeNull();
  });

  it('should handle fetch error', async () => {
    const errorMessage = 'Tag not found';
    mockedFetchTagDetail.mockRejectedValue(new Error(errorMessage));

    const { result } = renderHook(() => useTagDetail('nonexistent-tag'));

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBe(errorMessage);
    expect(result.current.state.tagDetail).toBeNull();
  });

  it('should handle non-Error rejection', async () => {
    mockedFetchTagDetail.mockRejectedValue('Unknown error');

    const { result } = renderHook(() => useTagDetail('some-tag'));

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBe('Failed to load tag details');
    expect(result.current.state.tagDetail).toBeNull();
  });

  it('should retry fetching after error', async () => {
    mockedFetchTagDetail.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useTagDetail('release-1.0'));

    await waitFor(() => {
      expect(result.current.state.error).toBe('Network error');
    });

    expect(result.current.state.tagDetail).toBeNull();

    // Now mock successful response
    mockedFetchTagDetail.mockResolvedValueOnce(mockTagDetail);

    // Retry
    act(() => {
      result.current.actions.retry();
    });

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBeNull();
    expect(result.current.state.tagDetail).toEqual(mockTagDetail);
    expect(mockedFetchTagDetail).toHaveBeenCalledTimes(2);
  });

  it('should refetch when tagName changes', async () => {
    mockedFetchTagDetail.mockResolvedValue(mockTagDetail);

    const { result, rerender } = renderHook(
      ({ tagName }) => useTagDetail(tagName),
      { initialProps: { tagName: 'release-1.0' } }
    );

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(mockedFetchTagDetail).toHaveBeenCalledWith('release-1.0');

    // Change tagName
    const newTagDetail = {
      ...mockTagDetail,
      name: 'staging',
    };
    mockedFetchTagDetail.mockResolvedValue(newTagDetail);

    rerender({ tagName: 'staging' });

    await waitFor(() => {
      expect(result.current.state.tagDetail?.name).toBe('staging');
    });

    expect(mockedFetchTagDetail).toHaveBeenCalledWith('staging');
    expect(mockedFetchTagDetail).toHaveBeenCalledTimes(2);
  });

  it('should handle empty tag name', async () => {
    const { result } = renderHook(() => useTagDetail(''));

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBe('Tag name is required');
    expect(result.current.state.tagDetail).toBeNull();
    expect(mockedFetchTagDetail).not.toHaveBeenCalled();
  });

  it('should return tag detail with attributes', async () => {
    const tagWithAttributes = {
      ...mockTagDetail,
      attributes: {
        env: 'production',
        version: '1.0.0',
        buildId: '142',
        deployDate: '2026-01-15',
      },
    };
    mockedFetchTagDetail.mockResolvedValue(tagWithAttributes);

    const { result } = renderHook(() => useTagDetail('release-1.0'));

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.tagDetail?.attributes).toEqual({
      env: 'production',
      version: '1.0.0',
      buildId: '142',
      deployDate: '2026-01-15',
    });
  });

  it('should return tag detail with empty attributes', async () => {
    const tagWithEmptyAttributes = {
      ...mockTagDetail,
      attributes: {},
    };
    mockedFetchTagDetail.mockResolvedValue(tagWithEmptyAttributes);

    const { result } = renderHook(() => useTagDetail('empty-tag'));

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.tagDetail?.attributes).toEqual({});
  });

  it('should set loading to true during fetch', async () => {
    let resolvePromise: (value: typeof mockTagDetail) => void;
    const fetchPromise = new Promise<typeof mockTagDetail>((resolve) => {
      resolvePromise = resolve;
    });
    mockedFetchTagDetail.mockReturnValue(fetchPromise);

    const { result } = renderHook(() => useTagDetail('release-1.0'));

    // Should be loading
    expect(result.current.state.loading).toBe(true);

    // Resolve the promise
    act(() => {
      resolvePromise!(mockTagDetail);
    });

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.tagDetail).toEqual(mockTagDetail);
  });

  it('should clear error when retry succeeds', async () => {
    mockedFetchTagDetail.mockRejectedValueOnce(new Error('First attempt failed'));

    const { result } = renderHook(() => useTagDetail('release-1.0'));

    await waitFor(() => {
      expect(result.current.state.error).toBe('First attempt failed');
    });

    // Successful retry
    mockedFetchTagDetail.mockResolvedValueOnce(mockTagDetail);

    act(() => {
      result.current.actions.retry();
    });

    // During retry, loading should be true
    expect(result.current.state.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBeNull();
    expect(result.current.state.tagDetail).toEqual(mockTagDetail);
  });
});

