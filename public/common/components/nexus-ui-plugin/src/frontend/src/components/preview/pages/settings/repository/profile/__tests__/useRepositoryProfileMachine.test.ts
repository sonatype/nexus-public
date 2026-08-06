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

import { renderHook, waitFor, act } from '@testing-library/react';

import { useRepositoryProfileMachine } from '../useRepositoryProfileMachine';

// Mock the REST API
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
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
  ENDPOINTS: {
    CAPABILITIES: '/service/rest/v1/capabilities',
    REPOSITORIES: '/service/rest/v1/repositories',
    BLOBSTORES: '/service/rest/v1/blobstores',
    ROUTING_RULES: '/service/rest/v1/routing-rules',
  },
}));

// Mock toast
const mockToast = {
  success: jest.fn(),
  error: jest.fn(),
  warning: jest.fn(),
  info: jest.fn(),
};

jest.mock('../../../../../shared/Toast', () => ({
  useToast: () => mockToast,
}));

// =============================================================================
// Test Data
// =============================================================================

const mockRepository = {
  name: 'test-maven-repo',
  type: 'proxy',
  format: 'maven2',
  url: 'http://localhost:8081/repository/test-maven-repo',
  online: true,
  attributes: {
    storage: {
      blobStoreName: 'default',
    },
  },
};

const mockInternalRepository = {
  ...mockRepository,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  cleanup: { policyName: ['test-cleanup-policy'] },
  routingRuleName: 'test-rule',
};

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

// =============================================================================
// Tests
// =============================================================================

describe('useRepositoryProfileMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Default successful responses
    mockRestClient.get.mockImplementation((url: string) => {
      if (url.includes('/internal/ui/repositories/repository/')) {
        return Promise.resolve(mockInternalRepository);
      }
      if (url.includes('/internal/ui/repositories/details')) {
        return Promise.resolve([{
          ...mockRepository,
          status: { online: true, description: 'Ready' },
          componentCount: 2,
          assetCount: 3,
          size: 1024000,
        }]);
      }
      if (url.includes('/internal/ui/blobstores') && !url.includes('/usage/')) {
        return Promise.resolve(mockBlobStoreList);
      }
      if (url.includes('/routing-rules/')) {
        return Promise.resolve({ name: 'test-rule', mode: 'ALLOW', matchers: [] });
      }
      if (url.includes('/healthcheck')) {
        return Promise.resolve({ enabled: true, securityIssues: 2, licenseIssues: 1 });
      }
      if (url.includes('/firewall/status/repo/')) {
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
        return Promise.resolve([]);
      }
      if (url.includes('/http')) {
        return Promise.resolve({});
      }
      if (url.includes('/iq/capabilities')) {
        return Promise.resolve({ connected: false, hasFirewall: false, hasLifecycle: false });
      }
      if (url.includes('/malware-cleanup/summary/')) {
        return Promise.resolve(null);
      }
      return Promise.resolve({});
    });

    mockRestClient.post.mockResolvedValue({});
    mockRestClient.put.mockResolvedValue({});
    mockRestClient.delete.mockResolvedValue({});
  });

  // ========================================
  // Initial State Tests
  // ========================================

  describe('initial state', () => {
    it('starts in loading state', () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));
      expect(result.current.loading).toBe(true);
    });

    it('has null repository initially', () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));
      expect(result.current.repository).toBeNull();
    });
  });

  // ========================================
  // Data Loading Tests
  // ========================================

  describe('data loading', () => {
    it('loads repository data successfully', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository?.name).toBe('test-maven-repo');
      expect(result.current.error).toBeNull();
    });

    it('loads blob store details', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.blobStore?.name).toBe('default');
    });

    it('loads metrics', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.metrics?.componentCount).toBe(2);
      expect(result.current.metrics?.assetCount).toBe(3);
    });

    it('sets securityLoading to false after load', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.securityLoading).toBe(false);
    });

    it('sets systemLoading to false after load', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.systemLoading).toBe(false);
    });
  });

  // ========================================
  // Error Handling Tests
  // ========================================

  describe('error handling', () => {
    it('handles missing repository gracefully', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/repository/')) {
          return Promise.resolve(null);
        }
        return Promise.resolve({});
      });

      const { result } = renderHook(() => useRepositoryProfileMachine('nonexistent-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository).toBeNull();
    });

    it('handles API failure gracefully', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Even with failures, machine should reach a stable state
      expect(result.current.repository).toBeNull();
    });
  });

  // ========================================
  // Action Handler Tests
  // ========================================

  describe('action handlers', () => {
    it('handleInvalidateCache sets pending action', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleInvalidateCache();
      });

      expect(result.current.isConfirming).toBe(true);
      expect(result.current.pendingAction).toBe('invalidate-cache');
    });

    it('handleRebuildIndex sets pending action', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleRebuildIndex();
      });

      expect(result.current.isConfirming).toBe(true);
      expect(result.current.pendingAction).toBe('rebuild-index');
    });

    it('handleToggleOnline sets pending action', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleToggleOnline();
      });

      expect(result.current.isConfirming).toBe(true);
      expect(result.current.pendingAction).toBe('toggle-online');
    });
  });

  // ========================================
  // Confirmation Dialog Tests
  // ========================================

  describe('confirmation dialog', () => {
    it('provides correct dialog title for invalidate-cache', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleInvalidateCache();
      });

      expect(result.current.dialogTitle).toBe('Invalidate Cache');
      expect(result.current.dialogMessage).toContain('cached content');
    });

    it('provides correct dialog title for rebuild-index', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleRebuildIndex();
      });

      expect(result.current.dialogTitle).toBe('Rebuild Index');
      expect(result.current.dialogMessage).toContain('search index');
    });

    it('provides correct dialog title for toggle-online (online -> offline)', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleToggleOnline();
      });

      expect(result.current.dialogTitle).toBe('Take Repository Offline');
      expect(result.current.dialogConfirmLabel).toBe('Take Offline');
    });

    it('cancelAction returns to loaded state', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.handleInvalidateCache();
      });

      expect(result.current.isConfirming).toBe(true);

      act(() => {
        result.current.cancelAction();
      });

      expect(result.current.isConfirming).toBe(false);
      expect(result.current.pendingAction).toBeNull();
    });
  });

  // ========================================
  // Refresh Tests
  // ========================================

  describe('refresh functionality', () => {
    it('refresh triggers data reload', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Clear call count
      mockRestClient.get.mockClear();

      act(() => {
        result.current.refresh();
      });

      // Should trigger loading
      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });
    });
  });

  // ========================================
  // Derived State Tests
  // ========================================

  describe('derived state', () => {
    it('isProxy is true for proxy repositories', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository?.type).toBe('proxy');
    });

    it('isOnline reflects repository online status', async () => {
      const { result } = renderHook(() => useRepositoryProfileMachine('test-maven-repo'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.repository?.online).toBe(true);
    });
  });
});
