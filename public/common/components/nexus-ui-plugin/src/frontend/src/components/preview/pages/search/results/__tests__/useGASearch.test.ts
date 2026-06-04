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
import { useGASearch } from '../useGASearch';
import { searchMavenGA } from '../../core/searchApi';

// Mock the real API (hook uses USE_REAL_API = true)
jest.mock('../../core/searchApi', () => ({
  searchMavenGA: jest.fn(),
}));

const mockSearchMavenGA = searchMavenGA as jest.MockedFunction<typeof searchMavenGA>;

const mockResults = [
  {
    id: 'maven:org.apache.commons:commons-lang3',
    gaId: 'maven:org.apache.commons:commons-lang3',
    format: 'maven',
    displayName: 'commons-lang3',
    namespace: 'org.apache.commons',
    name: 'commons-lang3',
    latestVersion: '3.14.0',
    versionsCount: 47,
    repositoriesCount: 2,
    repositories: ['maven-central'],
    lastUpdated: '2024-01-15T10:30:00Z',
  },
  {
    id: 'maven:com.google.guava:guava',
    gaId: 'maven:com.google.guava:guava',
    format: 'maven',
    displayName: 'guava',
    namespace: 'com.google.guava',
    name: 'guava',
    latestVersion: '33.0.0-jre',
    versionsCount: 156,
    repositoriesCount: 3,
    repositories: ['maven-central'],
    lastUpdated: '2024-02-01T14:22:00Z',
  },
];

describe('useGASearch', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockSearchMavenGA.mockResolvedValue({
      items: mockResults,
      totalCount: mockResults.length,
      continuationToken: undefined,
    });
  });

  describe('initial state', () => {
    it('starts with empty results', () => {
      const { result } = renderHook(() => useGASearch({}));
      
      expect(result.current.state.results).toEqual([]);
      expect(result.current.state.loading).toBe(false);
      expect(result.current.state.error).toBeFalsy();
    });

    it('accepts initial query', () => {
      const { result } = renderHook(() => useGASearch({ query: 'commons' }));
      
      // Hook should store initial query
      expect(result.current.state.results).toEqual([]);
    });
  });

  describe('search', () => {
    it('sets loading state while searching', async () => {
      const { result } = renderHook(() => useGASearch({}));
      
      act(() => {
        result.current.search({ query: 'commons' });
      });
      
      // Should be loading immediately after search call
      expect(result.current.state.loading).toBe(true);
      
      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });
    });

    it('returns results after search', async () => {
      const { result } = renderHook(() => useGASearch({}));
      
      await act(async () => {
        await result.current.search({ query: 'commons' });
      });
      
      await waitFor(() => {
        expect(result.current.state.results.length).toBeGreaterThan(0);
      });
    });

    it('handles search error', async () => {
      mockSearchMavenGA.mockRejectedValue(new Error('Network error'));
      
      const { result } = renderHook(() => useGASearch({}));
      
      await act(async () => {
        await result.current.search({ query: 'commons' });
      });
      
      await waitFor(() => {
        expect(result.current.state.error).toBeTruthy();
        expect(result.current.state.loading).toBe(false);
      });
    });
  });

  describe('pagination', () => {
    it('hasMore is true when continuation token exists', async () => {
      mockSearchMavenGA.mockResolvedValue({
        items: mockResults,
        continuationToken: 'token123',
        total: 100,
      });
      
      const { result } = renderHook(() => useGASearch({}));
      
      await act(async () => {
        await result.current.search({ query: 'commons' });
      });
      
      await waitFor(() => {
        expect(result.current.hasMore).toBe(true);
      });
    });

    it('loadMore appends results without reshuffling', async () => {
      const firstPage = [mockResults[0]];
      const secondPage = [mockResults[1]];
      
      mockSearchMavenGA
        .mockResolvedValueOnce({
          items: firstPage,
          continuationToken: 'token123',
          total: 2,
        })
        .mockResolvedValueOnce({
          items: secondPage,
          continuationToken: null,
          total: 2,
        });
      
      const { result } = renderHook(() => useGASearch({}));
      
      // First search
      await act(async () => {
        await result.current.search({ query: 'test' });
      });
      
      await waitFor(() => {
        expect(result.current.state.results).toHaveLength(1);
      });
      
      // Load more
      await act(async () => {
        await result.current.loadMore();
      });
      
      await waitFor(() => {
        expect(result.current.state.results).toHaveLength(2);
        // First result should still be first (deterministic ordering)
        expect(result.current.state.results[0].gaId).toBe(firstPage[0].gaId);
        expect(result.current.state.results[1].gaId).toBe(secondPage[0].gaId);
      });
    });
  });

  describe('sorting', () => {
    it('setSort updates sort state', async () => {
      const { result } = renderHook(() => useGASearch({}));
      
      act(() => {
        result.current.setSort('name', 'asc');
      });
      
      expect(result.current.state.sort).toBe('name');
      expect(result.current.state.sortDirection).toBe('asc');
    });
  });

  describe('clear', () => {
    it('clears results and resets state', async () => {
      const { result } = renderHook(() => useGASearch({}));
      
      // First, perform a search
      await act(async () => {
        await result.current.search({ query: 'commons' });
      });
      
      await waitFor(() => {
        expect(result.current.state.results.length).toBeGreaterThan(0);
      });
      
      // Then clear
      act(() => {
        result.current.clear();
      });
      
      expect(result.current.state.results).toEqual([]);
      expect(result.current.state.error).toBeFalsy();
    });
  });
});

