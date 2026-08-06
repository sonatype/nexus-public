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
import { useFilteredTags } from '../hooks/useFilteredTags';

// Mock the REST API from the relative path that the source imports from
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
  },
}));

import { restClient } from '../../../../../interface/api';

// Get mock reference
const mockGet = restClient.get as jest.MockedFunction<typeof restClient.get>;

describe('useFilteredTags', () => {
  const mockTagsResponse = {
    items: [
      {
        name: 'tag-1',
        attributes: { key: 'value' },
        firstCreated: '2024-01-01T00:00:00.000Z',
        lastUpdated: '2024-01-15T00:00:00.000Z',
        componentCount: 10,
      },
      {
        name: 'tag-2',
        attributes: null,
        firstCreated: '2024-02-01T00:00:00.000Z',
        lastUpdated: '2024-02-15T00:00:00.000Z',
        componentCount: 5,
      },
    ],
    totalCount: 2,
    continuationToken: null,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // restClient.get returns data directly (not wrapped in {data: ...})
    mockGet.mockResolvedValue(mockTagsResponse);
  });

  describe('initial state', () => {
    it('starts with loading true', () => {
      const { result } = renderHook(() => useFilteredTags());

      expect(result.current.loading).toBe(true);
    });

    it('has empty tags initially', () => {
      const { result } = renderHook(() => useFilteredTags());

      expect(result.current.tags).toEqual([]);
    });

    it('has default filters', () => {
      const { result } = renderHook(() => useFilteredTags());

      expect(result.current.filters).toEqual({
        nameFilter: '',
        componentCountRanges: [],
        activityDays: [],
      });
    });

    it('has default sort settings', () => {
      const { result } = renderHook(() => useFilteredTags());

      expect(result.current.sortField).toBe('name');
      expect(result.current.sortDirection).toBe('asc');
    });

    it('has default pagination settings', () => {
      const { result } = renderHook(() => useFilteredTags());

      expect(result.current.currentPage).toBe(0);
      expect(result.current.pageSize).toBe(20);
    });
  });

  describe('data fetching', () => {
    it('fetches tags on mount', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(mockGet).toHaveBeenCalledWith(
        expect.stringContaining('/service/rest/internal/ui/tags/filtered')
      );
    });

    it('populates tags from response', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.tags).toHaveLength(2);
      });

      expect(result.current.tags[0].name).toBe('tag-1');
      expect(result.current.tags[1].name).toBe('tag-2');
    });

    it('sets totalItems from response', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.totalItems).toBe(2);
      });
    });

    it('handles fetch error', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // fetchTagsFiltered propagates the underlying error message; the machine
      // surfaces it verbatim when the rejection is an Error instance.
      expect(result.current.error).toBe('Network error');
      expect(result.current.tags).toEqual([]);
    });
  });

  describe('setFilters', () => {
    it('updates filters and resets page to 0', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Change page first
      act(() => {
        result.current.setPage(2);
      });

      expect(result.current.currentPage).toBe(2);

      // Let the fetch triggered by SET_PAGE settle so the machine returns to
      // `ready` and can accept the next event.
      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Set filters should reset page
      act(() => {
        result.current.setFilters({
          nameFilter: 'test',
          componentCountRanges: ['1-10'],
          activityDays: [7],
        });
      });

      expect(result.current.filters.nameFilter).toBe('test');
      expect(result.current.filters.componentCountRanges).toEqual(['1-10']);
      expect(result.current.filters.activityDays).toEqual([7]);
      expect(result.current.currentPage).toBe(0);
    });

    it('includes filters in API request', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      mockGet.mockClear();

      act(() => {
        result.current.setFilters({
          nameFilter: 'myTag',
          componentCountRanges: ['10-50', '50-100'],
          activityDays: [30, 90],
        });
      });

      await waitFor(() => {
        expect(mockGet).toHaveBeenCalled();
      });

      const url = mockGet.mock.calls[0][0];
      expect(url).toContain('nameFilter=myTag');
      expect(url).toContain('componentCountRanges=10-50');
      expect(url).toContain('componentCountRanges=50-100');
      expect(url).toContain('activityDays=30');
      expect(url).toContain('activityDays=90');
    });
  });

  describe('toggleSort', () => {
    it('changes sort field when clicking different field', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.toggleSort('componentCount');
      });

      expect(result.current.sortField).toBe('componentCount');
      expect(result.current.sortDirection).toBe('asc');
    });

    it('toggles direction when clicking same field', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Initial: name asc
      expect(result.current.sortField).toBe('name');
      expect(result.current.sortDirection).toBe('asc');

      // Click same field -> toggle to desc
      act(() => {
        result.current.toggleSort('name');
      });

      expect(result.current.sortField).toBe('name');
      expect(result.current.sortDirection).toBe('desc');

      // Let the refetch settle before sending the next TOGGLE_SORT.
      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Click again -> toggle back to asc
      act(() => {
        result.current.toggleSort('name');
      });

      expect(result.current.sortDirection).toBe('asc');
    });

    it('resets page to 0 when sorting', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.setPage(3);
      });

      expect(result.current.currentPage).toBe(3);

      // Let the fetch triggered by SET_PAGE settle before sorting.
      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.toggleSort('lastUpdated');
      });

      expect(result.current.currentPage).toBe(0);
    });
  });

  describe('setPage', () => {
    it('updates current page', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.setPage(5);
      });

      expect(result.current.currentPage).toBe(5);
    });
  });

  describe('setPageSize', () => {
    it('updates page size and resets to page 0', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.setPage(2);
      });

      // Let the fetch triggered by SET_PAGE settle before changing page size.
      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.setPageSize(50);
      });

      expect(result.current.pageSize).toBe(50);
      expect(result.current.currentPage).toBe(0);
    });
  });

  describe('retry', () => {
    it('refetches data when called', async () => {
      mockGet.mockRejectedValueOnce(new Error('Initial error'));

      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.error).toBeTruthy();
      });

      // Setup successful response for retry
      mockGet.mockResolvedValueOnce(mockTagsResponse);

      act(() => {
        result.current.retry();
      });

      await waitFor(() => {
        expect(result.current.error).toBeNull();
        expect(result.current.tags).toHaveLength(2);
      });
    });
  });

  describe('API URL construction', () => {
    it('includes all query parameters', async () => {
      const { result } = renderHook(() => useFilteredTags());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      const url = mockGet.mock.calls[0][0];
      expect(url).toContain('sortField=name');
      expect(url).toContain('sortDirection=asc');
      expect(url).toContain('page=0');
      expect(url).toContain('pageSize=20');
    });
  });
});
