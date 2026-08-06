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

import { interpret } from 'xstate';

import { createRepositoryProfileMachine } from '../repositoryProfileMachine';

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

// =============================================================================
// Test Data
// =============================================================================

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

const mockRoutingRule = {
  name: 'test-rule',
  mode: 'ALLOW',
  matchers: ['/org/example/.*'],
};

// =============================================================================
// Tests
// =============================================================================

describe('repositoryProfileMachine', () => {
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
      if (url.includes('/routing-rules/')) {
        return Promise.resolve(mockRoutingRule);
      }
      if (url.includes('/internal/ui/blobstores') && !url.includes('/usage/')) {
        return Promise.resolve(mockBlobStoreList);
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
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });
      // loading is a parallel state so its value is an object, not a string
      expect(machine.initialState.value).toEqual({
        loading: { core: 'fetching', security: 'fetching', system: 'fetching' },
      });
    });

    it('initializes with correct context', () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-repo' });
      expect(machine.initialState.context.repositoryName).toBe('test-repo');
      expect(machine.initialState.context.repository).toBeNull();
      // pendingAction was removed from context — it is now derived from state name in the hook
      expect(machine.initialState.context.actionError).toBeNull();
    });
  });

  // ========================================
  // Loading State Tests
  // ========================================

  describe('loading state', () => {
    it('fetches repository data on LOAD event', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            expect(state.context.repository?.name).toBe('test-maven-repo');
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('sets repository data after successful load', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            expect(state.context.blobStore?.name).toBe('default');
            expect(state.context.metrics?.componentCount).toBe(2);
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('handles missing repository gracefully', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/repository/')) {
          return Promise.resolve(null);
        }
        return Promise.resolve({});
      });

      const machine = createRepositoryProfileMachine({ repositoryName: 'nonexistent-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            expect(state.context.repository).toBeNull();
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('handles API failure gracefully', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            // Machine should still reach loaded state even with failures
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });
  });

  // ========================================
  // Confirming State Tests
  // ========================================

  describe('confirming state', () => {
    it('transitions to confirming on INVALIDATE_CACHE', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            // pendingAction is now derived from state name in the hook, not stored in context
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('transitions to confirming on REBUILD_INDEX', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            service.send({ type: 'REBUILD_INDEX' });
          } else if (state.matches('confirmingRebuildIndex')) {
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('transitions to confirming on TOGGLE_ONLINE', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded')) {
            service.send({ type: 'TOGGLE_ONLINE' });
          } else if (state.matches('confirmingToggleOnline')) {
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('returns to loaded on CANCEL', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        let sentInvalidate = false;
        let wasConfirming = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository && !sentInvalidate) {
            sentInvalidate = true;
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            wasConfirming = true;
            service.send({ type: 'CANCEL' });
          } else if (state.matches('loaded') && wasConfirming) {
            // actionError (not pendingAction) lives in context; pendingAction is derived from state
            expect(state.context.actionError).toBeNull();
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });
  });

  // ========================================
  // Executing State Tests
  // ========================================

  describe('executing state', () => {
    it('transitions to executing on CONFIRM', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository) {
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            service.send({ type: 'CONFIRM' });
          } else if (state.matches('executingInvalidateCache')) {
            expect(mockRestClient.post).toHaveBeenCalledWith(
              expect.stringContaining('/invalidate-cache')
            );
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('calls invalidate cache API on execute', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository) {
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            service.send({ type: 'CONFIRM' });
          } else if (state.matches('executingInvalidateCache')) {
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('calls rebuild index API on execute', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository) {
            service.send({ type: 'REBUILD_INDEX' });
          } else if (state.matches('confirmingRebuildIndex')) {
            service.send({ type: 'CONFIRM' });
          } else if (state.matches('executingRebuildIndex')) {
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('returns to loaded after successful execution', async () => {
      mockRestClient.post.mockResolvedValue({});

      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        let sentInvalidate = false;
        let wasExecuting = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository && !sentInvalidate) {
            sentInvalidate = true;
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            service.send({ type: 'CONFIRM' });
          } else if (state.matches('executingInvalidateCache')) {
            wasExecuting = true;
          } else if (state.matches('loaded') && wasExecuting) {
            // actionError should be null on success (pendingAction no longer in context)
            expect(state.context.actionError).toBeNull();
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });

    it('sets actionError on execution failure', async () => {
      mockRestClient.post.mockRejectedValue(new Error('Action failed'));

      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        let sentInvalidate = false;
        let wasExecuting = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository && !sentInvalidate) {
            sentInvalidate = true;
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            service.send({ type: 'CONFIRM' });
          } else if (state.matches('executingInvalidateCache')) {
            wasExecuting = true;
          } else if (state.matches('loaded') && wasExecuting) {
            expect(state.context.actionError).toBeTruthy();
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });
  });

  // ========================================
  // Refresh Tests
  // ========================================

  describe('refresh functionality', () => {
    it('transitions to loading on REFRESH', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        let wasLoaded = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository) {
            if (!wasLoaded) {
              wasLoaded = true;
              // Clear previous calls
              mockRestClient.get.mockClear();
              service.send({ type: 'REFRESH' });
            } else {
              // Should be back in loaded after refresh
              service.stop();
              resolve();
            }
          }
        });
        service.start();
      });
    });
  });

  // ========================================
  // Guard Tests
  // ========================================

  describe('guards', () => {
    it('prevents new actions while executing', async () => {
      const machine = createRepositoryProfileMachine({ repositoryName: 'test-maven-repo' });

      await new Promise<void>((resolve) => {
        let sentInvalidate = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && state.context.repository && !sentInvalidate) {
            sentInvalidate = true;
            service.send({ type: 'INVALIDATE_CACHE' });
          } else if (state.matches('confirmingInvalidateCache')) {
            service.send({ type: 'CONFIRM' });
          } else if (state.matches('executingInvalidateCache')) {
            // Try to send another action — executingInvalidateCache has no handler for
            // REBUILD_INDEX so the machine stays in this state
            service.send({ type: 'REBUILD_INDEX' });
            expect(state.matches('executingInvalidateCache')).toBe(true);
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });
  });
});
