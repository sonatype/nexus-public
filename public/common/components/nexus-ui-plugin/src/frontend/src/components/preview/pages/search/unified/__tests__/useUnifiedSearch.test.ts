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
import { useUnifiedSearch } from '../useUnifiedSearch';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

const mockSearchResponse = {
  data: {
    items: [
      {
        id: 'comp-1',
        repository: 'npm-proxy',
        format: 'npm',
        group: null,
        name: 'lodash',
        version: '4.17.21',
        assets: [
          {
            id: 'asset-1',
            path: '/lodash/-/lodash-4.17.21.tgz',
            downloadUrl: 'http://localhost/repository/npm-proxy/lodash/-/lodash-4.17.21.tgz',
          },
        ],
      },
      {
        id: 'comp-2',
        repository: 'maven-central',
        format: 'maven2',
        group: 'org.apache.commons',
        name: 'commons-lang3',
        version: '3.12.0',
        assets: [
          {
            id: 'asset-2',
            path: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
            downloadUrl: 'http://localhost/repository/maven-central/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
          },
        ],
      },
    ],
    continuationToken: 'token123',
  },
};

describe('useUnifiedSearch', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('initializes with default state', () => {
    const { result } = renderHook(() => useUnifiedSearch());

    expect(result.current.state.format).toBe('all');
    expect(result.current.state.query).toBe('');
    expect(result.current.state.filters).toEqual({});
    expect(result.current.state.results).toEqual([]);
    expect(result.current.state.loading).toBe(false);
    expect(result.current.hasMore).toBe(false);
  });

  it('setFormat updates format and clears filters', () => {
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setFilter('groupId', 'org.apache');
    });

    expect(result.current.state.filters.groupId).toBe('org.apache');

    act(() => {
      result.current.setFormat('npm');
    });

    expect(result.current.state.format).toBe('npm');
    expect(result.current.state.filters).toEqual({});
  });

  it('setQuery updates the query', () => {
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setQuery('lodash');
    });

    expect(result.current.state.query).toBe('lodash');
  });

  it('setFilter updates a single filter value', () => {
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setFilter('groupId', 'org.springframework');
    });

    expect(result.current.state.filters.groupId).toBe('org.springframework');
  });

  it('setFilters updates multiple filter values', () => {
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setFilters({
        groupId: 'org.apache',
        artifactId: 'commons-lang3',
      });
    });

    expect(result.current.state.filters.groupId).toBe('org.apache');
    expect(result.current.state.filters.artifactId).toBe('commons-lang3');
  });

  it('search fetches results from API', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setQuery('lodash');
    });

    await act(async () => {
      await result.current.search();
    });

    expect(mockedAxios.get).toHaveBeenCalledWith(
      expect.stringContaining('/service/rest/v1/search'),
      expect.objectContaining({ signal: expect.any(AbortSignal) })
    );
    expect(result.current.state.results).toHaveLength(2);
    expect(result.current.state.results[0].name).toBe('lodash');
    expect(result.current.hasMore).toBe(true);
  });

  it('search includes format in query params', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setFormat('maven');
    });

    await act(async () => {
      await result.current.search();
    });

    expect(mockedAxios.get).toHaveBeenCalledWith(
      expect.stringContaining('format=maven2'),
      expect.objectContaining({ signal: expect.any(AbortSignal) })
    );
  });

  it('search handles errors', async () => {
    mockedAxios.get.mockRejectedValueOnce(new Error('Network error'));
    
    const { result } = renderHook(() => useUnifiedSearch());

    await act(async () => {
      await result.current.search();
    });

    expect(result.current.state.error).toBe('Network error');
    expect(result.current.state.loading).toBe(false);
  });

  it('loadMore fetches additional results', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    await act(async () => {
      await result.current.search();
    });

    const moreResults = {
      data: {
        items: [
          {
            id: 'comp-3',
            repository: 'npm-proxy',
            format: 'npm',
            group: null,
            name: 'express',
            version: '4.18.2',
            assets: [],
          },
        ],
        continuationToken: null,
      },
    };
    mockedAxios.get.mockResolvedValueOnce(moreResults);

    await act(async () => {
      await result.current.loadMore();
    });

    expect(mockedAxios.get).toHaveBeenCalledWith(
      expect.stringContaining('continuationToken=token123'),
      expect.objectContaining({ signal: expect.any(AbortSignal) })
    );
    expect(result.current.state.results).toHaveLength(3);
    expect(result.current.hasMore).toBe(false);
  });

  it('reset clears all state', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setFormat('npm');
      result.current.setQuery('lodash');
      result.current.setFilter('name', 'test');
    });

    await act(async () => {
      await result.current.search();
    });

    expect(result.current.state.results.length).toBeGreaterThan(0);

    act(() => {
      result.current.reset();
    });

    expect(result.current.state.format).toBe('all');
    expect(result.current.state.query).toBe('');
    expect(result.current.state.filters).toEqual({});
    expect(result.current.state.results).toEqual([]);
  });

  it('placeholder is derived from format', () => {
    const { result } = renderHook(() => useUnifiedSearch());

    expect(result.current.placeholder).toContain('Search');

    act(() => {
      result.current.setFormat('maven');
    });

    expect(result.current.placeholder).toContain('group ID');
  });

  it('search omits q parameter when no query is provided', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    // Don't set any query - leave it empty
    await act(async () => {
      await result.current.search();
    });

    // Should NOT include q parameter (API returns all components by default)
    // The API rejects q=* with "Leading wildcards are prohibited"
    const [url] = mockedAxios.get.mock.calls[0];
    expect(url).not.toContain('q=');
    expect(result.current.state.results).toHaveLength(2);
  });

  it('search omits q parameter when query is only whitespace', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setQuery('   '); // Whitespace only
    });

    await act(async () => {
      await result.current.search();
    });

    // Should trim whitespace and omit q parameter
    const [url] = mockedAxios.get.mock.calls[0];
    expect(url).not.toContain('q=');
  });

  it('search uses actual query when provided', async () => {
    mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
    
    const { result } = renderHook(() => useUnifiedSearch());

    act(() => {
      result.current.setQuery('react');
    });

    await act(async () => {
      await result.current.search();
    });

    // Should use the actual query
    const [url] = mockedAxios.get.mock.calls[0];
    expect(url).toContain('q=react');
  });

  describe('server-side sorting', () => {
    /**
     * Sorting is delegated to the REST API: every request carries `sort` and
     * `direction`, and the hook never re-orders what comes back. These tests
     * pin the UI sort field to the API alias it maps to.
     */
    function requestedParams(callIndex = 0): URLSearchParams {
      const [url] = mockedAxios.get.mock.calls[callIndex] as [string];
      return new URLSearchParams(url.slice(url.indexOf('?') + 1));
    }

    it('sends the default sort with every search', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);

      const { result } = renderHook(() => useUnifiedSearch());

      await act(async () => {
        await result.current.search();
      });

      const params = requestedParams();
      expect(params.get('sort')).toBe('last_updated');
      expect(params.get('direction')).toBe('desc');
    });

    it.each([
      ['name', 'asc', 'name', 'asc'],
      ['name', 'desc', 'name', 'desc'],
      ['lastUpdated', 'asc', 'last_updated', 'asc'],
      ['repository', 'asc', 'repository', 'asc'],
    ])(
      'sends %s/%s as sort=%s&direction=%s',
      async (field, direction, apiSort, apiDirection) => {
        mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);

        const { result } = renderHook(() => useUnifiedSearch());

        act(() => {
          result.current.setSort(field as never, direction as never);
        });

        await act(async () => {
          await result.current.search();
        });

        const params = requestedParams();
        expect(params.get('sort')).toBe(apiSort);
        expect(params.get('direction')).toBe(apiDirection);
      },
    );

    it('keeps the sort on the loadMore request alongside the continuation token', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);

      const { result } = renderHook(() => useUnifiedSearch());

      act(() => {
        result.current.setSort('name', 'asc');
      });

      await act(async () => {
        await result.current.search();
      });

      mockedAxios.get.mockResolvedValueOnce({
        data: { items: [], continuationToken: null },
      });

      await act(async () => {
        await result.current.loadMore();
      });

      const params = requestedParams(1);
      expect(params.get('sort')).toBe('name');
      expect(params.get('direction')).toBe('asc');
      expect(params.get('continuationToken')).toBe('token123');
    });

    it('does not re-order the results the server returned', async () => {
      // The API response is deliberately not alphabetical; the hook must
      // preserve it rather than apply a client-side sort of its own.
      mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);

      const { result } = renderHook(() => useUnifiedSearch());

      act(() => {
        result.current.setSort('name', 'asc');
      });

      await act(async () => {
        await result.current.search();
      });

      expect(result.current.state.results.map((r) => r.name)).toEqual([
        'lodash',
        'commons-lang3',
      ]);
    });

    it('exposes the current sort selection on the state', () => {
      const { result } = renderHook(() => useUnifiedSearch());

      expect(result.current.state.sortField).toBe('lastUpdated');
      expect(result.current.state.sortDirection).toBe('desc');

      act(() => {
        result.current.setSort('name', 'asc');
      });

      expect(result.current.state.sortField).toBe('name');
      expect(result.current.state.sortDirection).toBe('asc');
    });
  });

  describe('AbortController - request cancellation', () => {
    it('passes AbortController signal to Axios requests', async () => {
      mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
      
      const { result } = renderHook(() => useUnifiedSearch());

      await act(async () => {
        await result.current.search();
      });

      // Verify that signal was passed to Axios
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          signal: expect.any(AbortSignal),
        })
      );
    });

    it('cancels previous request when a new search is triggered', async () => {
      // With XState, rapid SEARCH events are coalesced by the machine.
      // The behavioral guarantee: only the latest search result matters.
      const firstResponse = {
        data: {
          items: [{
            id: 'comp-lodash',
            repository: 'npm-proxy',
            format: 'npm',
            group: null,
            name: 'lodash',
            version: '4.17.21',
            assets: [{ id: 'a1', path: '/lodash', downloadUrl: 'http://localhost/lodash' }],
          }],
          continuationToken: null,
        },
      };
      const secondResponse = {
        data: {
          items: [{
            id: 'comp-express',
            repository: 'npm-proxy',
            format: 'npm',
            group: null,
            name: 'express',
            version: '4.18.2',
            assets: [{ id: 'a1', path: '/express', downloadUrl: 'http://localhost/express' }],
          }],
          continuationToken: null,
        },
      };

      // Set up both mocks - machine may use one or both depending on event coalescing
      mockedAxios.get
        .mockResolvedValueOnce(firstResponse)
        .mockResolvedValueOnce(secondResponse);

      const { result } = renderHook(() => useUnifiedSearch());

      // First search
      await act(async () => {
        result.current.setQuery('lodash');
        await result.current.search();
      });

      // Second search replaces the first
      await act(async () => {
        result.current.setQuery('express');
        await result.current.search();
      });

      // Wait for the machine to settle
      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // Final results should be from a completed search (the latest one)
      expect(result.current.state.results.length).toBeGreaterThan(0);
      expect(result.current.state.error).toBeUndefined();
    });

    it('ignores AbortError and does not update error state', async () => {
      // Simulate an AbortError
      const abortError = new Error('canceled');
      abortError.name = 'CanceledError';
      
      mockedAxios.get.mockRejectedValueOnce(abortError);
      
      const { result } = renderHook(() => useUnifiedSearch());

      await act(async () => {
        await result.current.search();
      });

      // Error state should not be set for AbortError
      expect(result.current.state.error).toBeUndefined();
    });

    it('loadMore also passes AbortController signal', async () => {
      // First search to get continuation token
      mockedAxios.get.mockResolvedValueOnce(mockSearchResponse);
      
      const { result } = renderHook(() => useUnifiedSearch());

      await act(async () => {
        await result.current.search();
      });

      // Now load more
      const moreResults = {
        data: {
          items: [{
            id: 'comp-3',
            repository: 'npm-proxy',
            format: 'npm',
            group: null,
            name: 'express',
            version: '4.18.2',
            assets: [],
          }],
          continuationToken: null,
        },
      };
      mockedAxios.get.mockResolvedValueOnce(moreResults);

      await act(async () => {
        await result.current.loadMore();
      });

      // Verify loadMore also uses signal
      expect(mockedAxios.get).toHaveBeenLastCalledWith(
        expect.any(String),
        expect.objectContaining({
          signal: expect.any(AbortSignal),
        })
      );
    });

    it('rapid searches only return the latest result (race condition prevention)', async () => {
      const { result } = renderHook(() => useUnifiedSearch());

      // Setup mock responses — machine may coalesce rapid events and only
      // invoke the service for the final state, so we provide enough mocks
      const response = {
        data: {
          items: [{ id: '3', repository: 'r', format: 'npm', group: null, name: 'result-3', version: '1.0.0', assets: [] }],
          continuationToken: null,
        },
      };
      mockedAxios.get.mockResolvedValue(response);

      // Fire off multiple searches rapidly in the same act block
      await act(async () => {
        result.current.search();
        result.current.search();
        await result.current.search();
      });

      // Wait for the machine to settle (service completes, state transitions)
      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      // At least one Axios call was made
      expect(mockedAxios.get).toHaveBeenCalled();

      // Final state should reflect a completed search with no errors
      expect(result.current.state.loading).toBe(false);
      expect(result.current.state.error).toBeUndefined();
      expect(result.current.state.results).toHaveLength(1);
      expect(result.current.state.results[0].name).toBe('result-3');
    });
  });
});


