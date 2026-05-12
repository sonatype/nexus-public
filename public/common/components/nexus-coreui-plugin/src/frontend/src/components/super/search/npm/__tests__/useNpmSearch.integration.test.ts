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
import { useNpmSearch } from '../useNpmSearch';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useNpmSearch Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('API endpoint calls', () => {
    it('calls correct API endpoint with format=npm', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
      });

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('/service/rest/v1/search');
        expect(calledUrl).toContain('format=npm');
        expect(calledUrl).toContain('q=lodash');
      });
    });

    it('includes npm.scope parameter when scope is provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ scope: '@angular', name: 'core' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('npm.scope=angular');
        expect(calledUrl).toContain('q=core');
      });
    });

    it('strips @ prefix from scope for API call', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ scope: '@types' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('npm.scope=types');
        expect(calledUrl).not.toContain('npm.scope=@types');
      });
    });

    it('includes repository parameter when provided', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'react', repository: 'npm-proxy' });
      });

      await waitFor(() => {
        const calledUrl = mockedAxios.get.mock.calls[0][0];
        expect(calledUrl).toContain('repository=npm-proxy');
      });
    });

    it('includes continuationToken for pagination', async () => {
      mockedAxios.get
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '1', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.21', assets: [] },
            ],
            continuationToken: 'token123',
          },
        })
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '2', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.20', assets: [] },
            ],
            continuationToken: null,
          },
        });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
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
    it('aggregates results by package name', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.21', assets: [] },
            { id: '2', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.20', assets: [] },
            { id: '3', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.19', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
      });

      await waitFor(() => {
        // Should aggregate to 1 result with versionsCount = 3
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].name).toBe('lodash');
        expect(result.current.state.results[0].versionsCount).toBe(3);
      });
    });

    it('creates scoped display name for scoped packages', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'npm-proxy', format: 'npm', group: 'angular', name: 'core', version: '17.0.0', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ scope: '@angular' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].displayName).toBe('@angular/core');
        expect(result.current.state.results[0].scope).toBe('angular');
      });
    });

    it('tracks latest version correctly', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.19', assets: [] },
            { id: '2', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.21', assets: [] },
            { id: '3', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.20', assets: [] },
          ],
          continuationToken: null,
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].latestVersion).toBe('4.17.21');
      });
    });
  });

  describe('error handling', () => {
    it('handles API errors gracefully', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
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

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'nonexistent-package-xyz' });
      });

      await waitFor(() => {
        expect(result.current.state.results).toEqual([]);
        expect(result.current.state.totalCount).toBe(0);
        expect(result.current.state.error).toBeUndefined();
      });
    });
  });

  describe('state management', () => {
    it('clears previous results on new search', async () => {
      mockedAxios.get
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '1', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.21', assets: [] },
            ],
            continuationToken: null,
          },
        })
        .mockResolvedValueOnce({
          data: {
            items: [
              { id: '2', repository: 'npm-proxy', format: 'npm', group: null, name: 'react', version: '18.0.0', assets: [] },
            ],
            continuationToken: null,
          },
        });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
      });

      await waitFor(() => {
        expect(result.current.state.results[0].name).toBe('lodash');
      });

      await act(async () => {
        await result.current.search({ name: 'react' });
      });

      await waitFor(() => {
        expect(result.current.state.results).toHaveLength(1);
        expect(result.current.state.results[0].name).toBe('react');
      });
    });

    it('clear() resets all state', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          items: [
            { id: '1', repository: 'npm-proxy', format: 'npm', group: null, name: 'lodash', version: '4.17.21', assets: [] },
          ],
          continuationToken: 'token123',
        },
      });

      const { result } = renderHook(() => useNpmSearch());

      await act(async () => {
        await result.current.search({ name: 'lodash' });
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
      expect(result.current.hasMore).toBe(false);
    });
  });
});


