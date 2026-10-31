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

import { renderHook, act } from '@testing-library/react';
import { useProprietaryApi } from '../useProprietaryApi';

// Mock the REST API at the path the source uses
// Variables used in mock must have 'mock' prefix for hoisting
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
  })),
}));

const PROPRIETARY_CONTENT_URL = '/service/rest/internal/proprietary-content';

const mockAllRepos = [
  { name: 'maven-releases', format: 'maven2', type: 'hosted' },
  { name: 'maven-snapshots', format: 'maven2', type: 'hosted' },
  { name: 'npm-hosted', format: 'npm', type: 'hosted' },
  { name: 'maven-central', format: 'maven2', type: 'proxy' },
];

describe('useProprietaryApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchSettings', () => {
    it('should fetch settings successfully via REST', async () => {
      mockRestClient.get.mockResolvedValueOnce(['maven-releases', 'npm-hosted']);

      const { result } = renderHook(() => useProprietaryApi());

      let settings: any;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings.enabledRepositories).toEqual(['maven-releases', 'npm-hosted']);
      expect(mockRestClient.get).toHaveBeenCalledWith(PROPRIETARY_CONTENT_URL);
    });

    it('should handle empty response', async () => {
      mockRestClient.get.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useProprietaryApi());

      let settings: any;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings.enabledRepositories).toEqual([]);
    });

    it('should handle null response', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useProprietaryApi());

      let settings: any;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings.enabledRepositories).toEqual([]);
    });

    it('should throw error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useProprietaryApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Network error');
      consoleSpy.mockRestore();
    });
  });

  describe('fetchPossibleRepositories', () => {
    it('should fetch and filter hosted repositories', async () => {
      mockRestClient.get.mockResolvedValueOnce(mockAllRepos);

      const { result } = renderHook(() => useProprietaryApi());

      let repos: any;
      await act(async () => {
        repos = await result.current.fetchPossibleRepositories();
      });

      expect(repos).toEqual([
        { id: 'maven-releases', name: 'maven-releases' },
        { id: 'maven-snapshots', name: 'maven-snapshots' },
        { id: 'npm-hosted', name: 'npm-hosted' },
      ]);
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/repositories');
    });

    it('should handle empty response', async () => {
      mockRestClient.get.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useProprietaryApi());

      let repos: any;
      await act(async () => {
        repos = await result.current.fetchPossibleRepositories();
      });

      expect(repos).toEqual([]);
    });

    it('should handle null response', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useProprietaryApi());

      let repos: any;
      await act(async () => {
        repos = await result.current.fetchPossibleRepositories();
      });

      expect(repos).toEqual([]);
    });

    it('should throw error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useProprietaryApi());

      await expect(result.current.fetchPossibleRepositories()).rejects.toThrow('Network error');
      consoleSpy.mockRestore();
    });
  });

  describe('updateSettings', () => {
    it('should update settings successfully via REST', async () => {
      // updateSettings POSTs to proprietary-content, then fetchSettings GETs
      mockRestClient.post.mockResolvedValueOnce(undefined);
      mockRestClient.get.mockResolvedValueOnce(['maven-releases', 'npm-hosted']);

      const possibleRepos = [
        { id: 'maven-releases', name: 'maven-releases' },
        { id: 'maven-snapshots', name: 'maven-snapshots' },
        { id: 'npm-hosted', name: 'npm-hosted' },
      ];

      const { result } = renderHook(() => useProprietaryApi());

      let settings: any;
      await act(async () => {
        settings = await result.current.updateSettings(['maven-releases', 'npm-hosted'], possibleRepos);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        PROPRIETARY_CONTENT_URL,
        {
          proprietary: ['maven-releases', 'npm-hosted'],
          nonProprietary: ['maven-snapshots'],
        }
      );
      expect(settings.enabledRepositories).toEqual(['maven-releases', 'npm-hosted']);
      expect(result.current.loading).toBe(false);
    });

    it('should set error on failure', async () => {
      mockRestClient.post.mockRejectedValueOnce({ message: 'Update failed' });

      const { result } = renderHook(() => useProprietaryApi());

      await act(async () => {
        try {
          await result.current.updateSettings(['maven-releases']);
        } catch (_e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Update failed');
      expect(result.current.loading).toBe(false);
    });

    it('should set loading state during update', async () => {
      let resolvePost: (value: unknown) => void;
      mockRestClient.post.mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePost = resolve;
        })
      );

      const { result } = renderHook(() => useProprietaryApi());

      act(() => {
        result.current.updateSettings(['maven-releases']);
      });

      expect(result.current.loading).toBe(true);

      // Resolve the update call, then mock the fetchSettings call
      mockRestClient.get.mockResolvedValueOnce(['maven-releases']);
      await act(async () => {
        resolvePost!(undefined);
      });

      expect(result.current.loading).toBe(false);
    });
  });

  describe('setError', () => {
    it('should allow setting and clearing error', () => {
      const { result } = renderHook(() => useProprietaryApi());

      act(() => {
        result.current.setError('Test error');
      });

      expect(result.current.error).toBe('Test error');

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});
