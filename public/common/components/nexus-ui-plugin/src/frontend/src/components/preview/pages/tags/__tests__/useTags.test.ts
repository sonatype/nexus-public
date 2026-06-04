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

import { useTags } from '../hooks/useTags';
import * as tagsApi from '../tags.api';
import { mockTags, generateManyTags } from './mockData';

// Mock the API
jest.mock('../tags.api');
const mockedFetchTags = tagsApi.fetchTags as jest.MockedFunction<typeof tagsApi.fetchTags>;

describe('useTags', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should fetch tags on mount', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    // Initially loading
    expect(result.current.state.loading).toBe(true);

    // Wait for data to load
    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(mockedFetchTags).toHaveBeenCalledTimes(1);
    expect(result.current.state.tags).toHaveLength(mockTags.length);
    expect(result.current.state.error).toBeNull();
  });

  it('should handle fetch error', async () => {
    const errorMessage = 'Network error';
    mockedFetchTags.mockRejectedValue(new Error(errorMessage));

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBe(errorMessage);
    expect(result.current.state.tags).toHaveLength(0);
  });

  it('should filter tags by name', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Filter by 'release'
    act(() => {
      result.current.actions.setFilter('release');
    });

    expect(result.current.state.filter).toBe('release');
    expect(result.current.state.tags).toHaveLength(1);
    expect(result.current.state.tags[0].id).toBe('release-1.0');

    // Filter by 'beta'
    act(() => {
      result.current.actions.setFilter('beta');
    });

    expect(result.current.state.tags).toHaveLength(1);
    expect(result.current.state.tags[0].id).toBe('beta-2.0');

    // Clear filter
    act(() => {
      result.current.actions.setFilter('');
    });

    expect(result.current.state.tags).toHaveLength(mockTags.length);
  });

  it('should reset to first page when filtering', async () => {
    const manyTags = generateManyTags(50);
    mockedFetchTags.mockResolvedValue(manyTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Go to page 2
    act(() => {
      result.current.actions.setPage(1);
    });

    expect(result.current.state.currentPage).toBe(1);

    // Filter - should reset to page 0
    act(() => {
      result.current.actions.setFilter('tag-001');
    });

    expect(result.current.state.currentPage).toBe(0);
  });

  it('should sort tags by different fields', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Default sort is by id ascending
    expect(result.current.state.sortField).toBe('id');
    expect(result.current.state.sortDirection).toBe('asc');
    expect(result.current.state.tags[0].id).toBe('alpha-test');

    // Sort by firstCreatedTime
    act(() => {
      result.current.actions.setSort('firstCreatedTime', 'asc');
    });

    expect(result.current.state.sortField).toBe('firstCreatedTime');
    expect(result.current.state.tags[0].id).toBe('alpha-test'); // Earliest

    // Sort by lastUpdatedTime descending
    act(() => {
      result.current.actions.setSort('lastUpdatedTime', 'desc');
    });

    expect(result.current.state.sortField).toBe('lastUpdatedTime');
    expect(result.current.state.sortDirection).toBe('desc');
    expect(result.current.state.tags[0].id).toBe('staging'); // Most recent
  });

  it('should toggle sort direction when clicking same field', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Initial: id ascending
    expect(result.current.state.sortDirection).toBe('asc');

    // Toggle id - should become descending
    act(() => {
      result.current.actions.toggleSort('id');
    });

    expect(result.current.state.sortDirection).toBe('desc');
    expect(result.current.state.tags[0].id).toBe('staging'); // Last alphabetically

    // Toggle id again - should become ascending
    act(() => {
      result.current.actions.toggleSort('id');
    });

    expect(result.current.state.sortDirection).toBe('asc');
  });

  it('should switch to new field with ascending when toggling different field', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Toggle id to descending first
    act(() => {
      result.current.actions.toggleSort('id');
    });

    expect(result.current.state.sortDirection).toBe('desc');

    // Toggle to firstCreatedTime - should reset to ascending
    act(() => {
      result.current.actions.toggleSort('firstCreatedTime');
    });

    expect(result.current.state.sortField).toBe('firstCreatedTime');
    expect(result.current.state.sortDirection).toBe('asc');
  });

  it('should handle pagination', async () => {
    const manyTags = generateManyTags(50);
    mockedFetchTags.mockResolvedValue(manyTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // First page
    expect(result.current.state.currentPage).toBe(0);
    expect(result.current.state.tags.length).toBeLessThanOrEqual(result.current.state.pageSize);

    // Go to next page
    act(() => {
      result.current.actions.setPage(1);
    });

    expect(result.current.state.currentPage).toBe(1);
  });

  it('should retry fetching after error', async () => {
    mockedFetchTags.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.error).toBe('Network error');
    });

    // Now mock successful response
    mockedFetchTags.mockResolvedValueOnce(mockTags);

    // Retry
    act(() => {
      result.current.actions.retry();
    });

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.error).toBeNull();
    expect(result.current.state.tags).toHaveLength(mockTags.length);
  });

  it('should calculate totalItems correctly', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.totalItems).toBe(mockTags.length);

    // Filter to reduce count
    act(() => {
      result.current.actions.setFilter('release');
    });

    expect(result.current.state.totalItems).toBe(1);
  });

  it('should handle empty tags array', async () => {
    mockedFetchTags.mockResolvedValue([]);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.tags).toHaveLength(0);
    expect(result.current.state.totalItems).toBe(0);
    expect(result.current.state.error).toBeNull();
  });

  it('should handle case-insensitive filtering', async () => {
    mockedFetchTags.mockResolvedValue(mockTags);

    const { result } = renderHook(() => useTags());

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Filter with uppercase
    act(() => {
      result.current.actions.setFilter('RELEASE');
    });

    expect(result.current.state.tags).toHaveLength(1);
    expect(result.current.state.tags[0].id).toBe('release-1.0');
  });
});

