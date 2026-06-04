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

import { renderHook, waitFor } from '@testing-library/react';

import { useRepositoryProfile } from '../hooks/useRepositoryProfile';

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
    status: err?.response?.status,
  })),
}));

describe('useRepositoryProfile', () => {
  const mockRepository = {
    name: 'test-maven-repo',
    type: 'hosted',
    format: 'maven2',
    url: 'http://localhost:8081/repository/test-maven-repo',
    online: true,
    attributes: {
      storage: {
        blobStoreName: 'default',
      },
      cleanup: {
        policyName: ['test-cleanup-policy'],
      },
    },
    routingRuleId: 'test-rule',
  };

  const mockBlobStore = {
    name: 'default',
    type: 'File',
    path: '/nexus-data/blobs/default',
    totalSizeInBytes: 1024000,
    availableSpaceInBytes: 512000,
    blobCount: 100,
  };

  // Internal UI list format (fetchBlobStore uses this endpoint and finds by name)
  const mockBlobStoreList = [
    {
      name: 'default',
      typeId: 'file',
      typeName: 'File',
      path: '/nexus-data/blobs/default',
      unavailable: false,
      blobCount: 100,
      totalSizeInBytes: 1024000,
      availableSpaceInBytes: 512000,
    },
  ];

  const mockRoutingRule = {
    name: 'test-rule',
    mode: 'ALLOW',
    matchers: ['/org/example/.*'],
  };

  const mockHealthCheck = {
    'test-maven-repo': {
      enabled: true,
      securityIssues: 2,
      licenseIssues: 1,
      lastScan: '2024-01-15T10:00:00Z',
    },
  };

  // Internal UI repository endpoint returns full config including storage.blobStoreName
  const mockInternalRepository = {
    ...mockRepository,
    storage: { blobStoreName: 'default', strictContentTypeValidation: true },
    cleanup: { policyName: ['test-cleanup-policy'] },
    routingRuleName: 'test-rule',
  };

  beforeEach(() => {
    jest.clearAllMocks();

    // Sprint 15: All mocks use REST GET (no more ExtDirect POST mocks)
    mockRestClient.get.mockImplementation((url) => {
      // Internal UI repository (full config with storage) - primary source
      if (url.includes('/internal/ui/repositories/repository/')) {
        return Promise.resolve(mockInternalRepository);
      }
      // Repository status enrichment
      if (url.includes('/internal/ui/repositories/details')) {
        return Promise.resolve([{ ...mockRepository, status: { online: true, description: 'Ready' }, componentCount: 2, assetCount: 3, size: 1024000 }]);
      }
      // Routing rule (was ExtDirect coreui_RoutingRules.read)
      if (url.includes('/routing-rules/')) {
        return Promise.resolve(mockRoutingRule);
      }
      // Health check (was ExtDirect healthcheck_Status.read)
      if (url.includes('/health-check')) {
        return Promise.resolve({ enabled: true, securityIssues: 2, licenseIssues: 1 });
      }
      if (url.includes('/internal/ui/blobstores') && !url.includes('/usage/')) {
        return Promise.resolve(mockBlobStoreList);
      }
      if (url.includes('/search')) {
        return Promise.resolve({
          items: [
            { id: '1', assets: [{ id: 'a1' }, { id: 'a2' }] },
            { id: '2', assets: [{ id: 'a3' }] },
          ],
        });
      }
      if (url.includes('/iq/quarantine')) {
        return Promise.resolve({ enabled: false });
      }
      if (url.includes('/security/privileges')) {
        return Promise.resolve([]);
      }
      if (url.includes('/security/roles')) {
        return Promise.resolve([]);
      }
      if (url.includes('/security/users')) {
        return Promise.resolve([]);
      }
      if (url.includes('/security/anonymous')) {
        return Promise.resolve({ enabled: false });
      }
      if (url.includes('/tasks')) {
        return Promise.resolve({ items: [] });
      }
      if (url.includes('/capabilities')) {
        return Promise.resolve({ items: [] });
      }
      if (url.includes('/system/http')) {
        return Promise.resolve({});
      }
      return Promise.resolve({});
    });
  });

  describe('initial loading state', () => {
    it('starts in loading state', () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      expect(result.current.loading).toBe(true);
    });
  });

  describe('successful data fetching', () => {
    it('fetches repository data successfully', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository).toMatchObject(mockRepository);
      expect(result.current.error).toBeNull();
    });

    it('fetches blob store details', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.blobStore).toMatchObject(mockBlobStore);
    });

    it('extracts cleanup policies from repository attributes', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.cleanupPolicies).toEqual([]);
    });

    it('fetches routing rule when repository has one', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.routingRule).toEqual(mockRoutingRule);
    });

    it('calculates metrics from search results', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.metrics).toEqual({
        componentCount: 2,
        assetCount: 3,
        totalSize: 1024000,
        downloadsPerMonth: 0,
        uploadsPerMonth: 0,
      });
    });
  });

  describe('error handling', () => {
    it('sets repository to null when repository is not found', async () => {
      mockRestClient.get.mockImplementation((url) => {
        if (url.includes('/internal/ui/repositories/repository/')) {
          return Promise.resolve(null);
        }
        return Promise.resolve({});
      });

      const { result } = renderHook(() => useRepositoryProfile('nonexistent-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository).toBeNull();
    });

    it('sets repository to null when API call fails', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository).toBeNull();
    });

    it('handles missing repository name gracefully', async () => {
      mockRestClient.get.mockImplementation((url) => {
        if (url.includes('/internal/ui/repositories/repository/')) {
          return Promise.resolve(null);
        }
        return Promise.resolve({});
      });

      const { result } = renderHook(() => useRepositoryProfile(''));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository).toBeNull();
    });
  });

  describe('refresh functionality', () => {
    it('reloads all data when refresh is called', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Clear GET mock call count to verify refresh triggers new calls
      const callCountBefore = mockRestClient.get.mock.calls.length;

      await result.current.refresh();

      // Verify additional API GET calls were made during refresh
      expect(mockRestClient.get.mock.calls.length).toBeGreaterThan(callCountBefore);
    });
  });

  describe('security data loading', () => {
    it('sets securityLoading to true initially', () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      expect(result.current.securityLoading).toBe(true);
    });

    it('completes security loading after fetch', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.securityLoading).toBe(false);
      });
    });
  });

  describe('system data loading', () => {
    it('sets systemLoading to true initially', () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      expect(result.current.systemLoading).toBe(true);
    });

    it('completes system loading after fetch', async () => {
      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.systemLoading).toBe(false);
      });
    });
  });

  describe('API failure resilience', () => {
    it('continues loading when blob store API fails', async () => {
      mockRestClient.get.mockImplementation((url) => {
        if (url.includes('/internal/ui/repositories/repository/')) {
          return Promise.resolve(mockInternalRepository);
        }
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.resolve([]);
        }
        if (url.includes('/internal/ui/blobstores') && !url.includes('/usage/')) {
          return Promise.reject(new Error('Blob store not found'));
        }
        return Promise.resolve({});
      });

      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Repository should still load even if blob store fails
      expect(result.current.repository).toMatchObject(mockRepository);
      expect(result.current.blobStore).toBeNull();
    });

    it('continues loading when firewall API fails', async () => {
      mockRestClient.get.mockImplementation((url) => {
        if (url.includes('/internal/ui/repositories/repository/')) {
          return Promise.resolve(mockInternalRepository);
        }
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.resolve([]);
        }
        if (url.includes('/firewall/status/repo/')) {
          return Promise.reject(new Error('IQ Server not available'));
        }
        if (url.includes('/internal/ui/blobstores') && !url.includes('/usage/')) {
          return Promise.resolve(mockBlobStoreList);
        }
        return Promise.resolve({});
      });

      const { result } = renderHook(() => useRepositoryProfile('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository).toMatchObject(mockRepository);
      expect(result.current.firewall).toBeNull();
    });
  });
});


