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

import { useCleanupPoliciesApi } from '../useCleanupPoliciesApi';
import { CLEANUP_POLICY_API } from '../types';

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
  ExtJS: {
    state: () => ({
      getValue: (key: string) => {
        if (key === 'datastore.isPostgresql') return true;
        if (key === 'nexus.cleanup.preview.enabled') return true;
        if (key === 'nexus.cleanup.maven2Retain') return true;
        if (key === 'nexus.cleanup.dockerRetain') return true;
        return false;
      },
    }),
    urlOf: (path: string) => path,
  },
}));

describe('useCleanupPoliciesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchCleanupPolicies', () => {
    it('fetches cleanup policies successfully', async () => {
      const mockPolicies = [
        { name: 'policy1', format: 'maven2', notes: 'Test' },
        { name: 'policy2', format: 'npm', notes: '' },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockPolicies);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      const policies = await act(async () => result.current.fetchCleanupPolicies());

      expect(mockRestClient.get).toHaveBeenCalledWith(CLEANUP_POLICY_API.BASE_URL);
      expect(policies).toEqual(mockPolicies);
    });

    it('handles fetch error', async () => {
      mockRestClient.get.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useCleanupPoliciesApi());

      await expect(result.current.fetchCleanupPolicies()).rejects.toThrow('Network error');
    });
  });

  describe('fetchCleanupPolicy', () => {
    it('fetches a single cleanup policy successfully', async () => {
      const mockPolicy = { name: 'test-policy', format: 'maven2', notes: 'Test' };
      mockRestClient.get.mockResolvedValueOnce(mockPolicy);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      const policy = await act(async () => result.current.fetchCleanupPolicy('test-policy'));

      expect(mockRestClient.get).toHaveBeenCalledWith(`${CLEANUP_POLICY_API.BASE_URL}/test-policy`);
      expect(policy).toEqual(mockPolicy);
    });
  });

  describe('fetchFormatCriteria', () => {
    it('fetches format criteria successfully', async () => {
      const mockCriteria = [
        { id: 'maven2', name: 'Maven2', availableCriteria: ['lastBlobUpdated'] },
        { id: 'npm', name: 'npm', availableCriteria: ['lastDownloaded'] },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockCriteria);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      const criteria = await act(async () => result.current.fetchFormatCriteria());

      expect(mockRestClient.get).toHaveBeenCalledWith(CLEANUP_POLICY_API.CRITERIA_FORMATS_URL);
      expect(criteria).toEqual(mockCriteria);
    });
  });

  describe('createCleanupPolicy', () => {
    it('creates a cleanup policy successfully', async () => {
      const mockPolicy = {
        name: 'new-policy',
        format: 'maven2',
        notes: 'New policy',
        criteriaLastBlobUpdated: 30,
        criteriaLastDownloaded: null,
        criteriaReleaseType: null,
        criteriaAssetRegex: null,
        retain: null,
        sortBy: null,
      };
      mockRestClient.post.mockResolvedValueOnce(mockPolicy);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      let createdPolicy;
      await act(async () => {
        createdPolicy = await result.current.createCleanupPolicy(mockPolicy);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(CLEANUP_POLICY_API.BASE_URL, expect.any(Object));
      expect(createdPolicy).toEqual(mockPolicy);
    });

    it('sets error on create failure', async () => {
      mockRestClient.post.mockRejectedValueOnce({
        response: { data: { message: 'Policy already exists' } },
      });

      const { result } = renderHook(() => useCleanupPoliciesApi());

      await act(async () => {
        try {
          await result.current.createCleanupPolicy({
            name: 'existing-policy',
            format: 'maven2',
            notes: '',
            criteriaLastBlobUpdated: null,
            criteriaLastDownloaded: null,
            criteriaReleaseType: null,
            criteriaAssetRegex: null,
            retain: null,
            sortBy: null,
          });
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Policy already exists');
    });
  });

  describe('updateCleanupPolicy', () => {
    it('updates a cleanup policy successfully', async () => {
      const mockPolicy = {
        name: 'test-policy',
        format: 'maven2',
        notes: 'Updated',
        criteriaLastBlobUpdated: 60,
        criteriaLastDownloaded: null,
        criteriaReleaseType: null,
        criteriaAssetRegex: null,
        retain: null,
        sortBy: null,
      };
      mockRestClient.put.mockResolvedValueOnce(mockPolicy);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      await act(async () => {
        await result.current.updateCleanupPolicy('test-policy', mockPolicy);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        `${CLEANUP_POLICY_API.BASE_URL}/test-policy`,
        expect.any(Object)
      );
    });
  });

  describe('deleteCleanupPolicy', () => {
    it('deletes a cleanup policy successfully', async () => {
      mockRestClient.delete.mockResolvedValueOnce({});

      const { result } = renderHook(() => useCleanupPoliciesApi());

      await act(async () => {
        await result.current.deleteCleanupPolicy('test-policy');
      });

      expect(mockRestClient.delete).toHaveBeenCalledWith(`${CLEANUP_POLICY_API.BASE_URL}/test-policy`);
    });

    it('sets error on delete failure', async () => {
      mockRestClient.delete.mockRejectedValueOnce({
        response: { data: { message: 'Policy is in use' } },
      });

      const { result } = renderHook(() => useCleanupPoliciesApi());

      await act(async () => {
        try {
          await result.current.deleteCleanupPolicy('test-policy');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Policy is in use');
    });
  });

  describe('fetchRepositories', () => {
    it('fetches repositories for a format', async () => {
      const mockRepos = [
        { id: 'repo1', name: 'Repository 1' },
        { id: 'repo2', name: 'Repository 2' },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockRepos);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      const repos = await act(async () => result.current.fetchRepositories('maven2'));

      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/internal/ui/repositories?format=maven2'
      );
      expect(repos).toEqual(mockRepos);
    });
  });

  describe('previewCleanupPolicy', () => {
    it('previews cleanup policy results with POST request', async () => {
      const mockResults = {
        results: [{ name: 'component1', group: 'com.example', version: '1.0.0', repository: 'repo1' }],
        total: 1,
      };
      mockRestClient.post.mockResolvedValueOnce(mockResults);

      const { result } = renderHook(() => useCleanupPoliciesApi());

      const preview = await act(async () =>
        result.current.previewCleanupPolicy('repo1', {
          name: 'test',
          format: 'maven2',
          notes: '',
          criteriaLastBlobUpdated: 30,
          criteriaLastDownloaded: null,
          criteriaReleaseType: null,
          criteriaAssetRegex: null,
          retain: null,
          sortBy: null,
        })
      );

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/internal/cleanup-policies/preview/components',
        expect.objectContaining({
          repository: 'repo1',
          criteriaLastBlobUpdated: 30,
        })
      );
      expect(preview.components).toEqual(mockResults.results);
      expect(preview.total).toBe(1);
    });
  });

  describe('getDryRunCsvUrl', () => {
    it('generates correct CSV download URL', () => {
      const { result } = renderHook(() => useCleanupPoliciesApi());

      const url = result.current.getDryRunCsvUrl('repo1', {
        name: 'test',
        format: 'maven2',
        notes: '',
        criteriaLastBlobUpdated: 30,
        criteriaLastDownloaded: null,
        criteriaReleaseType: null,
        criteriaAssetRegex: null,
        retain: null,
        sortBy: null,
      });

      expect(url).toContain('repository=repo1');
      expect(url).toContain('criteriaLastBlobUpdated=30');
    });
  });

  describe('isPreviewEnabled', () => {
    it('returns true when PostgreSQL and preview is enabled', () => {
      const { result } = renderHook(() => useCleanupPoliciesApi());

      expect(result.current.isPreviewEnabled()).toBe(true);
    });
  });

  describe('isRetainEnabled', () => {
    it('returns true for maven2 format', () => {
      const { result } = renderHook(() => useCleanupPoliciesApi());

      expect(result.current.isRetainEnabled('maven2')).toBe(true);
    });

    it('returns true for docker format', () => {
      const { result } = renderHook(() => useCleanupPoliciesApi());

      expect(result.current.isRetainEnabled('docker')).toBe(true);
    });
  });
});

