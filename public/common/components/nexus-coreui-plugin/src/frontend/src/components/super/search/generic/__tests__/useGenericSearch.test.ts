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
import Axios from 'axios';
import { useGenericSearch } from '../useGenericSearch';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useGenericSearch', () => {
  const mockApiResponse = {
    data: {
      items: [
        {
          id: '1',
          repository: 'maven-central',
          format: 'maven2',
          group: 'org.apache.commons',
          name: 'commons-lang3',
          version: '3.14.0',
          assets: [
            {
              id: 'asset-1',
              path: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
              downloadUrl: 'http://localhost/repository/maven-central/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
            },
          ],
        },
        {
          id: '2',
          repository: 'npm-proxy',
          format: 'npm',
          group: null,
          name: 'lodash',
          version: '4.17.21',
          assets: [],
        },
      ],
      continuationToken: 'next-page-token',
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedAxios.get.mockResolvedValue(mockApiResponse);
  });

  it('initializes with empty state', () => {
    const { result } = renderHook(() => useGenericSearch());

    expect(result.current.state.results).toEqual([]);
    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.error).toBeUndefined();
    expect(result.current.state.totalCount).toBe(0);
    expect(result.current.hasMore).toBe(false);
  });

  it('initializes with provided filters', () => {
    const initialFilters = { q: 'spring', format: 'maven2' };
    const { result } = renderHook(() => useGenericSearch(initialFilters));

    expect(result.current.state.filters).toEqual(initialFilters);
  });

  it('executes search and updates state', async () => {
    const { result } = renderHook(() => useGenericSearch());

    act(() => {
      result.current.search({ q: 'commons' });
    });

    // Should be loading
    expect(result.current.state.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Should have results
    expect(result.current.state.results.length).toBe(2);
    expect(result.current.state.results[0].name).toBe('commons-lang3');
    expect(result.current.state.results[1].name).toBe('lodash');
  });

  it('transforms API response correctly', async () => {
    const { result } = renderHook(() => useGenericSearch());

    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    const firstResult = result.current.state.results[0];
    expect(firstResult.id).toBe('maven2:org.apache.commons:commons-lang3:3.14.0');
    expect(firstResult.displayName).toBe('org.apache.commons:commons-lang3');
    expect(firstResult.format).toBe('maven2');

    const secondResult = result.current.state.results[1];
    expect(secondResult.id).toBe('npm:lodash:4.17.21');
    expect(secondResult.displayName).toBe('lodash');
  });

  it('sets hasMore when continuationToken is present', async () => {
    const { result } = renderHook(() => useGenericSearch());

    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    expect(result.current.hasMore).toBe(true);
    expect(result.current.state.continuationToken).toBe('next-page-token');
  });

  it('calls API with correct query parameters', async () => {
    const { result } = renderHook(() => useGenericSearch());

    await act(async () => {
      await result.current.search({
        q: 'spring',
        format: 'maven2',
        repository: 'maven-central',
        group: 'org.springframework',
        name: 'spring-core',
        version: '6.0.0',
      });
    });

    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
    const calledUrl = mockedAxios.get.mock.calls[0][0];
    expect(calledUrl).toContain('q=spring');
    expect(calledUrl).toContain('format=maven2');
    expect(calledUrl).toContain('repository=maven-central');
    expect(calledUrl).toContain('group=org.springframework');
    expect(calledUrl).toContain('name=spring-core');
    expect(calledUrl).toContain('version=6.0.0');
  });

  it('handles API errors', async () => {
    mockedAxios.get.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useGenericSearch());

    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.error).toBe('Network error');
    expect(result.current.state.results).toEqual([]);
  });

  it('clears state when clear is called', async () => {
    const { result } = renderHook(() => useGenericSearch());

    // First do a search
    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    expect(result.current.state.results.length).toBeGreaterThan(0);

    // Then clear
    act(() => {
      result.current.clear();
    });

    expect(result.current.state.results).toEqual([]);
    expect(result.current.state.totalCount).toBe(0);
    expect(result.current.state.filters).toEqual({});
    expect(result.current.hasMore).toBe(false);
  });

  it('loads more results with continuation token', async () => {
    const moreResults = {
      data: {
        items: [
          {
            id: '3',
            repository: 'docker-hosted',
            format: 'docker',
            group: null,
            name: 'nginx',
            version: 'latest',
            assets: [],
          },
        ],
        continuationToken: undefined,
      },
    };

    mockedAxios.get
      .mockResolvedValueOnce(mockApiResponse)
      .mockResolvedValueOnce(moreResults);

    const { result } = renderHook(() => useGenericSearch());

    // Initial search
    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    expect(result.current.state.results.length).toBe(2);
    expect(result.current.hasMore).toBe(true);

    // Load more
    await act(async () => {
      await result.current.loadMore();
    });

    expect(result.current.state.results.length).toBe(3);
    expect(result.current.state.results[2].name).toBe('nginx');
    expect(result.current.hasMore).toBe(false);
  });

  it('does not load more when no continuation token', async () => {
    mockedAxios.get.mockResolvedValueOnce({
      data: { items: [], continuationToken: undefined },
    });

    const { result } = renderHook(() => useGenericSearch());

    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    expect(result.current.hasMore).toBe(false);

    // Try to load more - should not make API call
    await act(async () => {
      await result.current.loadMore();
    });

    // Only the initial search call should have been made
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });

  it('does not load more while already loading', async () => {
    // Make the API slow
    mockedAxios.get.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockApiResponse), 100))
    );

    const { result } = renderHook(() => useGenericSearch());

    // Start search but don't await
    act(() => {
      result.current.search({ q: 'test' });
    });

    expect(result.current.state.loading).toBe(true);

    // Try to load more while loading
    await act(async () => {
      await result.current.loadMore();
    });

    // Should only have one API call
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });

  it('updates sort settings', () => {
    const { result } = renderHook(() => useGenericSearch());

    expect(result.current.state.sort).toBe('relevance');
    expect(result.current.state.sortDirection).toBe('desc');

    act(() => {
      result.current.setSort('lastUpdated', 'asc');
    });

    expect(result.current.state.sort).toBe('lastUpdated');
    expect(result.current.state.sortDirection).toBe('asc');
  });

  it('re-searches when sort changes and has active filters', async () => {
    const { result } = renderHook(() => useGenericSearch());

    // First search
    await act(async () => {
      await result.current.search({ q: 'test' });
    });

    expect(mockedAxios.get).toHaveBeenCalledTimes(1);

    // Change sort
    await act(async () => {
      result.current.setSort('name', 'asc');
    });

    // Should trigger a new search
    await waitFor(() => {
      expect(mockedAxios.get).toHaveBeenCalledTimes(2);
    });
  });

  it('does not re-search on sort change when no active filters', () => {
    const { result } = renderHook(() => useGenericSearch());

    act(() => {
      result.current.setSort('name', 'asc');
    });

    // No API call should be made
    expect(mockedAxios.get).not.toHaveBeenCalled();
  });
});


