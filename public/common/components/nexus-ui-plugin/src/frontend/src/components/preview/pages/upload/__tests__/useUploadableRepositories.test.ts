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

import { useUploadableRepositories } from '../hooks/useUploadableRepositories';

// Mock the REST API from the relative path that the source imports from
// Note: jest.mock is hoisted, so we use jest.fn() inside the factory
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
  isNotFoundError: jest.fn((apiError: { status?: number }) => apiError?.status === 404),
}));

import { restClient } from '../../../../../interface/api';

// Get mock reference
const mockGet = restClient.get as jest.MockedFunction<typeof restClient.get>;

// The hook issues this many parallel REST calls per fetch (repositories + upload-specs).
// Centralized so the count assertions don't hard-code an implementation detail.
const CALLS_PER_FETCH = 2;

describe('useUploadableRepositories', () => {
  const mockRepositories = [
    {
      name: 'maven-releases',
      type: 'hosted',
      format: 'maven2',
      url: 'http://localhost:8081/repository/maven-releases/',
      status: { online: true },
      versionPolicy: 'RELEASE',
    },
    {
      name: 'maven-snapshots',
      type: 'hosted',
      format: 'maven2',
      url: 'http://localhost:8081/repository/maven-snapshots/',
      status: { online: true },
      versionPolicy: 'SNAPSHOT',
    },
    {
      name: 'npm-hosted',
      type: 'hosted',
      format: 'npm',
      url: 'http://localhost:8081/repository/npm-hosted/',
      status: { online: true },
    },
    {
      name: 'npm-proxy',
      type: 'proxy',
      format: 'npm',
      url: 'http://localhost:8081/repository/npm-proxy/',
      status: { online: true },
    },
    {
      name: 'docker-hosted',
      type: 'hosted',
      format: 'docker',
      url: 'http://localhost:8081/repository/docker-hosted/',
      status: { online: true },
    },
    {
      name: 'raw-offline',
      type: 'hosted',
      format: 'raw',
      url: 'http://localhost:8081/repository/raw-offline/',
      status: { online: false },
    },
  ];

  const mockUploadDefinitions = [
    { format: 'maven2', uiUpload: true, multipleUpload: true },
    { format: 'npm', uiUpload: true, multipleUpload: false },
    { format: 'docker', uiUpload: false, multipleUpload: false },
    { format: 'raw', uiUpload: true, multipleUpload: true },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    // Default mock implementation: return repositories for first call, upload definitions for second
    mockGet.mockImplementation((url: string) => {
      if (url.includes('/repositories/details')) {
        return Promise.resolve(mockRepositories);
      }
      if (url === '/service/rest/v1/formats/upload-specs') {
        return Promise.resolve(mockUploadDefinitions);
      }
      return Promise.resolve([]);
    });
  });

  describe('Initial State', () => {
    it('starts in loading state', () => {
      const { result } = renderHook(() => useUploadableRepositories());

      expect(result.current.loading).toBe(true);
      expect(result.current.repositories).toEqual([]);
      expect(result.current.error).toBeNull();
    });
  });

  describe('Data Fetching', () => {
    it('fetches repositories and upload definitions', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Two parallel REST calls
      expect(mockGet).toHaveBeenCalledTimes(CALLS_PER_FETCH);
    });

    it('filters to only hosted repositories', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Should not include npm-proxy (type: proxy)
      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).not.toContain('npm-proxy');
    });

    it('filters out Maven SNAPSHOT repositories', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Should not include maven-snapshots
      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).not.toContain('maven-snapshots');
    });

    it('filters out offline repositories', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Should not include raw-offline
      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).not.toContain('raw-offline');
    });

    it('filters to only formats that support UI upload', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Should not include docker-hosted (uiUpload: false)
      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).not.toContain('docker-hosted');
    });

    it('includes valid uploadable repositories', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).toContain('maven-releases');
      expect(repoNames).toContain('npm-hosted');
    });
  });

  describe('Error Handling', () => {
    it('sets error state when fetch fails', async () => {
      mockGet.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBe('Network error');
      expect(result.current.repositories).toEqual([]);
    });

    it('surfaces an error when upload-specs cannot be loaded', async () => {
      mockGet.mockImplementation((url: string) => {
        if (url.includes('/repositories/details')) {
          return Promise.resolve(mockRepositories);
        }
        if (url === '/service/rest/v1/formats/upload-specs') {
          return Promise.reject({ response: { status: 404 } });
        }
        return Promise.resolve([]);
      });

      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).not.toBeNull();
      expect(result.current.repositories).toEqual([]);
    });

    it('falls back to public repos endpoint when internal endpoint returns 404 (cloud deployment)', async () => {
      const publicRepos = [
        { name: 'maven-releases', type: 'hosted', format: 'maven2', url: 'http://localhost:8081/repository/maven-releases/', online: true },
        // Excluded by type (proxy, not hosted).
        { name: 'npm-proxy', type: 'proxy', format: 'npm', url: 'http://localhost:8081/repository/npm-proxy/', online: true },
        // Hosted + online but its format has no upload definition — exercises the format-filtering path.
        { name: 'pypi-hosted', type: 'hosted', format: 'pypi', url: 'http://localhost:8081/repository/pypi-hosted/', online: true },
      ];

      mockGet.mockImplementation((url: string) => {
        if (url.includes('/repositories/details')) {
          return Promise.reject({ response: { status: 404 } });
        }
        if (url === '/service/rest/v1/repositories') {
          return Promise.resolve(publicRepos);
        }
        if (url === '/service/rest/v1/formats/upload-specs') {
          return Promise.resolve(mockUploadDefinitions);
        }
        return Promise.resolve([]);
      });

      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBeNull();

      // Only maven-releases survives: it is hosted, online, and its format (maven2) is present in
      // the resolved upload definitions. npm-proxy is dropped by type, pypi-hosted by format.
      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).toEqual(['maven-releases']);
    });
  });

  describe('Filtering', () => {
    it('filters repositories by name', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleFilterChange('maven');
      });

      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).toContain('maven-releases');
      expect(repoNames).not.toContain('npm-hosted');
    });

    it('filters repositories by format', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleFilterChange('npm');
      });

      const repoNames = result.current.repositories.map((r) => r.name);
      expect(repoNames).toContain('npm-hosted');
      expect(repoNames).not.toContain('maven-releases');
    });

    it('clears filter with clearFilter', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleFilterChange('maven');
      });

      expect(result.current.filterText).toBe('maven');

      act(() => {
        result.current.clearFilter();
      });

      expect(result.current.filterText).toBe('');
    });
  });

  describe('Sorting', () => {
    it('sorts ascending on first click', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleSort('name');
      });

      expect(result.current.sortColumn).toBe('name');
      expect(result.current.sortDirection).toBe('asc');
    });

    it('sorts descending on second click', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleSort('name');
      });

      act(() => {
        result.current.handleSort('name');
      });

      expect(result.current.sortColumn).toBe('name');
      expect(result.current.sortDirection).toBe('desc');
    });

    it('clears sort on third click', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleSort('name');
      });

      act(() => {
        result.current.handleSort('name');
      });

      act(() => {
        result.current.handleSort('name');
      });

      expect(result.current.sortColumn).toBeNull();
      expect(result.current.sortDirection).toBeNull();
    });

    it('resets to ascending when clicking different column', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleSort('name');
      });

      act(() => {
        result.current.handleSort('format');
      });

      expect(result.current.sortColumn).toBe('format');
      expect(result.current.sortDirection).toBe('asc');
    });

    it('sorts repositories by name ascending', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleSort('name');
      });

      const names = result.current.repositories.map((r) => r.name);
      const sortedNames = [...names].sort();
      expect(names).toEqual(sortedNames);
    });

    it('sorts repositories by name descending', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleSort('name');
      });

      act(() => {
        result.current.handleSort('name');
      });

      const names = result.current.repositories.map((r) => r.name);
      const sortedNames = [...names].sort().reverse();
      expect(names).toEqual(sortedNames);
    });
  });

  describe('Refetch', () => {
    it('can refetch data', async () => {
      const { result } = renderHook(() => useUploadableRepositories());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(mockGet).toHaveBeenCalledTimes(CALLS_PER_FETCH);

      await act(async () => {
        await result.current.refetch();
      });

      // Refetch repeats the same set of parallel calls.
      expect(mockGet).toHaveBeenCalledTimes(CALLS_PER_FETCH * 2);
    });
  });
});
