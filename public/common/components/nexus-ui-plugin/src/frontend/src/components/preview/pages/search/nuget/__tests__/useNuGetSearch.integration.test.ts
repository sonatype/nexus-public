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
import { useNuGetSearch } from '../useNuGetSearch';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useNuGetSearch Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('API endpoint calls', () => {
    it('calls correct API endpoint with format=nuget', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft.Json' });
      });

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('/service/rest/v1/search');
        expect(calledUrl).toContain('format=nuget');
        expect(calledUrl).toContain('q=Newtonsoft.Json');
      });
    });

    it('includes nuget.id parameter when packageId is provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ packageId: 'Microsoft.Extensions.Logging' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('nuget.id=Microsoft.Extensions.Logging');
      });
    });

    it('includes version parameter when provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ packageId: 'Newtonsoft.Json', version: '13.0.3' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('version=13.0.3');
      });
    });

    it('includes continuationToken for pagination', async () => {
      mockedAxios.get
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
            ],
            continuationToken: 'token123',
          },
        })
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '2', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.2', assets: [] },
            ],
            continuationToken: null,
          },
        });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.hasMore).toBe(true);
      });

      await act(async () => {
        await result.current.loadMore();
      });

      await waitFor(() => {
        const secondCallUrl = mockedAxios.get.mock.calls[1][0];
        expect(secondCallUrl).toContain('continuationToken=token123');
      });
    });
  });

  describe('response transformation', () => {
    it('aggregates results by package ID', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
            { id: '2', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.2', assets: [] },
            { id: '3', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.1', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        // Should aggregate to 1 result with versionsCount = 3
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].packageId).toBe('Newtonsoft.Json');
        expect(result.current.state.results[0].versionsCount).toBe(3);
      });
    });

    it('creates correct displayName from packageId', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Microsoft.Extensions.Logging', version: '8.0.0', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Microsoft.Extensions' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].displayName).toBe('Microsoft.Extensions.Logging');
        expect(result.current.state.results[0].packageId).toBe('Microsoft.Extensions.Logging');
      });
    });

    it('tracks latest version correctly', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '12.0.1', assets: [] },
            { id: '2', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
            { id: '3', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.1', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].latestVersion).toBe('13.0.3');
      });
    });

    it('maps asset lastModified to lastUpdated', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            {
              id: '1', repository: 'nuget-proxy', format: 'nuget', group: null,
              name: 'Newtonsoft.Json', version: '13.0.3',
              assets: [{ id: 'a1', path: '/x', downloadUrl: '/dl', lastModified: '2024-06-15T10:00:00.000+00:00' }],
            },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].lastUpdated).toBe('2024-06-15T10:00:00.000+00:00');
      });
    });

    it('uses empty string for lastUpdated when assets array is empty', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Serilog', version: '3.1.1', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Serilog' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].lastUpdated).toBe('');
      });
    });

    it('uses empty string for lastUpdated when asset has no lastModified', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            {
              id: '1', repository: 'nuget-proxy', format: 'nuget', group: null,
              name: 'AutoMapper', version: '12.0.1',
              assets: [{ id: 'a1', path: '/x', downloadUrl: '/dl' }],
            },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'AutoMapper' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].lastUpdated).toBe('');
      });
    });

    it('keeps the most recent lastModified across multiple versions', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            {
              id: '1', repository: 'nuget-proxy', format: 'nuget', group: null,
              name: 'Newtonsoft.Json', version: '12.0.1',
              assets: [{ id: 'a1', path: '/x', downloadUrl: '/dl', lastModified: '2023-01-01T00:00:00.000+00:00' }],
            },
            {
              id: '2', repository: 'nuget-proxy', format: 'nuget', group: null,
              name: 'Newtonsoft.Json', version: '13.0.3',
              assets: [{ id: 'a2', path: '/y', downloadUrl: '/dl', lastModified: '2024-06-15T10:00:00.000+00:00' }],
            },
            {
              id: '3', repository: 'nuget-proxy', format: 'nuget', group: null,
              name: 'Newtonsoft.Json', version: '11.0.2',
              assets: [{ id: 'a3', path: '/z', downloadUrl: '/dl', lastModified: '2022-03-20T00:00:00.000+00:00' }],
            },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].lastUpdated).toBe('2024-06-15T10:00:00.000+00:00');
      });
    });

    it('picks the most recent lastModified across multiple assets for a single version', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            {
              id: '1', repository: 'nuget-proxy', format: 'nuget', group: null,
              name: 'Newtonsoft.Json', version: '13.0.3',
              assets: [
                { id: 'a1', path: '/x.nupkg', downloadUrl: '/dl/nupkg', lastModified: '2024-01-01T00:00:00.000Z' },
                { id: 'a2', path: '/x.nuspec', downloadUrl: '/dl/nuspec', lastModified: '2024-06-15T10:00:00.000Z' },
                { id: 'a3', path: '/x.sha512', downloadUrl: '/dl/sha', lastModified: '2023-12-01T00:00:00.000Z' },
              ],
            },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].lastUpdated).toBe('2024-06-15T10:00:00.000Z');
      });
    });

    it('handles case-insensitive package aggregation', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
            { id: '2', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'newtonsoft.json', version: '13.0.2', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        // Should aggregate case-insensitively
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].versionsCount).toBe(2);
      });
    });
  });

  describe('error handling', () => {
    it('handles API errors gracefully', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.error).toBe('Network error');
        expect(result.current.state.loading).toBe(false);
        expect(result.current.state.results).toEqual([]);
      });
    });

    it('handles empty results', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'nonexistent-package-xyz' });
      });

      await waitFor(() => {
        expect(result.current.state.results).toEqual([]);
        expect(result.current.state.totalCount).toBe(0);
        expect(result.current.state.error).toBeUndefined();
      });
    });
  });

  describe('sorting', () => {
    it('setSort updates sort state', async () => {
      const { result } = renderHook(() => useNuGetSearch());

      act(() => {
        result.current.setSort('downloads', 'desc');
      });

      expect(result.current.state.sort).toBe('downloads');
      expect(result.current.state.sortDirection).toBe('desc');
    });

    it('setSort triggers re-search when results exist', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
      });

      act(() => {
        result.current.setSort('recent', 'asc');
      });

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledTimes(2);
      });
    });
  });

  describe('state management', () => {
    it('clears previous results on new search', async () => {
      mockedAxios.get
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
            ],
            continuationToken: null,
          },
        })
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '2', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Serilog', version: '3.0.0', assets: [] },
            ],
            continuationToken: null,
          },
        });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].packageId).toBe('Newtonsoft.Json');
      });

      await act(async () => {
        await result.current.search({ query: 'Serilog' });
      });

      await waitFor(() => {
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].packageId).toBe('Serilog');
      });
    });

    it('clear() resets all state', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'nuget-proxy', format: 'nuget', group: null, name: 'Newtonsoft.Json', version: '13.0.3', assets: [] },
          ],
          continuationToken: 'token123',
        },
      });

      const { result } = renderHook(() => useNuGetSearch());

      await act(async () => {
        await result.current.search({ query: 'Newtonsoft' });
      });

      await waitFor(() => {
        expect(result.current.state.results).toHaveLength(1);
      });

      act(() => {
        result.current.clear();
      });

      expect(result.current.state.results).toEqual([]);
      expect(result.current.state.totalCount).toBe(0);
      expect(result.current.state.continuationToken).toBeUndefined();
      expect(result.current.state.query).toBe('');
      expect(result.current.hasMore).toBe(false);
    });
  });
});


