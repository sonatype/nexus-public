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
import { useDockerSearch } from '../useDockerSearch';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useDockerSearch Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('API endpoint calls', () => {
    it('calls correct API endpoint with format=docker', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ query: 'nginx' });
      });

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('/service/rest/v1/search');
        expect(calledUrl).toContain('format=docker');
        expect(calledUrl).toContain('q=nginx');
      });
    });

    it('includes docker.imageName parameter when imageName is provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'library/nginx' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('docker.imageName=library%2Fnginx');
      });
    });

    it('includes docker.imageTag parameter when tag is provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx', tag: 'alpine' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('docker.imageTag=alpine');
      });
    });

    it('includes docker.contentDigest parameter when digest is provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ digest: 'sha256:abc123' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('docker.contentDigest=sha256%3Aabc123');
      });
    });

    it('includes repository parameter when provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx', repository: 'docker-proxy' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('repository=docker-proxy');
      });
    });

    it('includes continuationToken for pagination', async () => {
      mockedAxios.get
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
            ],
            continuationToken: 'token123',
          },
        })
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '2', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'alpine', assets: [] },
            ],
            continuationToken: null,
          },
        });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
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
    it('aggregates results by image name', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
            { id: '2', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'alpine', assets: [] },
            { id: '3', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: '1.25.0', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
      });

      await waitFor(() => {
        // Should aggregate to 1 result with tagsCount = 3
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].imageName).toBe('nginx');
        expect(result.current.state.results[0].tagsCount).toBe(3);
      });
    });

    it('creates unique ID per repository and image combination', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
            { id: '2', repository: 'docker-hosted', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
      });

      await waitFor(() => {
        // Should have 2 results because different repositories
        expect(result.current.state.results).toHaveLength(2);
        expect(result.current.state.results[0].id).not.toBe(result.current.state.results[1].id);
      });
    });

    it('tracks latest tag correctly', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: '1.24.0', assets: [] },
            { id: '2', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
            { id: '3', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: '1.25.0', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
      });

      await waitFor(() => {
        // "latest" should be the latest (string comparison)
        expect(result.current.state.results[0].latestTag).toBe('latest');
      });
    });

    it('uses version as default latestTag when provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'alpine', version: '3.18', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'alpine' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].latestTag).toBe('3.18');
      });
    });

    it('preserves repository information in results', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-hosted', format: 'docker', group: null, name: 'myapp', version: 'v1.0.0', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'myapp' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].repository).toBe('docker-hosted');
      });
    });
  });

  describe('error handling', () => {
    it('handles API errors gracefully', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
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

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nonexistent-image-xyz' });
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
      const { result } = renderHook(() => useDockerSearch());

      act(() => {
        result.current.setSort('lastUpdated', 'desc');
      });

      expect(result.current.state.sort).toBe('lastUpdated');
      expect(result.current.state.sortDirection).toBe('desc');
    });

    it('setSort triggers re-search when results exist', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
      });

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
      });

      act(() => {
        result.current.setSort('name', 'asc');
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
              { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
            ],
            continuationToken: null,
          },
        })
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '2', repository: 'docker-proxy', format: 'docker', group: null, name: 'alpine', version: 'latest', assets: [] },
            ],
            continuationToken: null,
          },
        });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].imageName).toBe('nginx');
      });

      await act(async () => {
        await result.current.search({ imageName: 'alpine' });
      });

      await waitFor(() => {
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].imageName).toBe('alpine');
      });
    });

    it('clear() resets all state', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
          ],
          continuationToken: 'token123',
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
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

    it('accepts initial parameters', async () => {
      const { result } = renderHook(() => useDockerSearch({
        query: 'nginx',
        sort: 'name',
        sortDirection: 'asc',
      }));

      expect(result.current.state.query).toBe('nginx');
      expect(result.current.state.sort).toBe('name');
      expect(result.current.state.sortDirection).toBe('asc');
    });
  });

  describe('loading states', () => {
    it('sets loading true while searching', async () => {
      let resolvePromise: (value: unknown) => void;
      const delayedPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockedAxios.get.mockReturnValue(delayedPromise as Promise<unknown>);

      const { result } = renderHook(() => useDockerSearch());

      act(() => {
        result.current.search({ imageName: 'nginx' });
      });

      expect(result.current.state.loading).toBe(true);

      await act(async () => {
        resolvePromise!({
          data: {
            items: [],
            continuationToken: null,
          },
        });
      });

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });
    });

    it('does not allow loadMore when already loading', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'docker-proxy', format: 'docker', group: null, name: 'nginx', version: 'latest', assets: [] },
          ],
          continuationToken: 'token123',
        },
      });

      const { result } = renderHook(() => useDockerSearch());

      await act(async () => {
        await result.current.search({ imageName: 'nginx' });
      });

      await waitFor(() => {
        expect(result.current.hasMore).toBe(true);
      });

      // Simulate loading state
      let resolvePromise: (value: unknown) => void;
      const delayedPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockedAxios.get.mockReturnValue(delayedPromise as Promise<unknown>);

      // Start loading
      act(() => {
        result.current.loadMore();
      });

      // Try to call again while loading
      await act(async () => {
        await result.current.loadMore();
      });

      // Should only have been called once for the loadMore
      expect(mockedAxios.get).toHaveBeenCalledTimes(2); // 1 for search, 1 for loadMore

      // Cleanup
      await act(async () => {
        resolvePromise!({
          data: {
            items: [],
            continuationToken: null,
          },
        });
      });
    });
  });
});


