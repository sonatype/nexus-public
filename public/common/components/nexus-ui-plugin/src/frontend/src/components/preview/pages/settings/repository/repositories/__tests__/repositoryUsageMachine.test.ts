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

import { createRepositoryUsageMachine } from '../repositoryUsageMachine';

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
  ENDPOINTS: {},
}));

const mockMetricsData = [
  {
    name: 'test-maven-repo',
    componentCount: 10,
    assetCount: 25,
    size: 1024000,
  },
];

const mockGroupRepository = {
  name: 'test-group',
  type: 'group',
  format: 'maven2',
  group: {
    memberNames: ['repo1', 'repo2'],
  },
};

const mockAllRepositories = [
  { name: 'group1', type: 'group', format: 'maven2' },
  { name: 'group2', type: 'group', format: 'maven2' },
  { name: 'test-hosted', type: 'hosted', format: 'maven2' },
];

async function runUntil(
  machine: ReturnType<typeof createRepositoryUsageMachine>,
  predicate: (state: ReturnType<typeof machine.transition>) => boolean,
  onLoaded?: (service: ReturnType<typeof interpret>) => void,
  timeoutMs = 5000,
): Promise<ReturnType<typeof machine.transition>> {
  return new Promise((resolve, reject) => {
    const service = interpret(machine);
    const timer = setTimeout(() => {
      service.stop();
      reject(new Error(`runUntil timed out after ${timeoutMs}ms waiting for predicate to hold; last state: ${JSON.stringify(service.state?.value)}`));
    }, timeoutMs);
    service.onTransition((state) => {
      if (predicate(state)) {
        clearTimeout(timer);
        service.stop();
        resolve(state);
        return;
      }
      onLoaded?.(service);
    });
    service.start();
  });
}

describe('repositoryUsageMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockRestClient.get.mockImplementation((url: string) => {
      if (url.includes('/internal/ui/repositories/details')) {
        return Promise.resolve(mockMetricsData);
      }
      if (url.includes('/v1/repositories/')) {
        if (url.includes('test-group')) {
          return Promise.resolve(mockGroupRepository);
        }
        return Promise.resolve({ name: 'test-repo', type: 'hosted', format: 'maven2' });
      }
      if (url.includes('/internal/ui/repositories') && !url.includes('/details')) {
        return Promise.resolve(mockAllRepositories);
      }
      return Promise.resolve({});
    });
  });

  describe('initial state', () => {
    it('starts in loading state (auto-fetches on mount)', () => {
      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });
      expect(machine.initialState.value).toBe('loading');
    });

    it('initializes with correct context', () => {
      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-repo',
        repositoryType: 'hosted',
      });
      expect(machine.initialState.context.repositoryName).toBe('test-repo');
      expect(machine.initialState.context.repositoryType).toBe('hosted');
      expect(machine.initialState.context.metrics).toBeNull();
      expect(machine.initialState.context.groupMembers).toEqual([]);
      expect(machine.initialState.context.whereUsed).toEqual([]);
      expect(machine.initialState.context.error).toBeNull();
      expect(machine.initialState.context.membershipError).toBeNull();
    });
  });

  describe('loading -> loaded', () => {
    it('fetches metrics data', async () => {
      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.metrics?.componentCount).toBe(10);
      expect(state.context.metrics?.assetCount).toBe(25);
      expect(state.context.metrics?.totalSize).toBe(1024000);
    });

    it('fetches group members for group repository', async () => {
      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-group',
        repositoryType: 'group',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.groupMembers).toContain('repo1');
      expect(state.context.groupMembers).toContain('repo2');
    });

    it('fetches where-used for hosted repository', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.resolve(mockMetricsData);
        }
        if (url.includes('/v1/repositories/')) {
          if (url.includes('group1')) {
            return Promise.resolve({
              name: 'group1',
              type: 'group',
              format: 'maven2',
              group: { memberNames: ['test-maven-repo', 'other-repo'] },
            });
          }
          if (url.includes('group2')) {
            return Promise.resolve({
              name: 'group2',
              type: 'group',
              format: 'maven2',
              group: { memberNames: ['other-repo'] },
            });
          }
          return Promise.resolve({ name: 'test-repo', type: 'hosted', format: 'maven2' });
        }
        if (url.includes('/internal/ui/repositories') && !url.includes('/details')) {
          return Promise.resolve(mockAllRepositories);
        }
        return Promise.resolve({});
      });

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.whereUsed).toContain('group1');
      expect(state.context.whereUsed).not.toContain('group2');
    });

    it('preserves undefined metric fields returned by API (does not coerce to 0)', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.resolve([{ name: 'test-maven-repo' }]);
        }
        if (url.includes('/internal/ui/repositories') && !url.includes('/details')) {
          return Promise.resolve([]);
        }
        return Promise.resolve({});
      });

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.metrics?.componentCount).toBeUndefined();
      expect(state.context.metrics?.assetCount).toBeUndefined();
      expect(state.context.metrics?.totalSize).toBeUndefined();
    });
  });

  describe('partial-failure handling (Promise.allSettled)', () => {
    it('surfaces metrics + membership error when membership fetch fails (hosted)', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.resolve(mockMetricsData);
        }
        return Promise.reject(new Error('Where-used fetch failed'));
      });

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.metrics?.componentCount).toBe(10);
      expect(state.context.whereUsed).toEqual([]);
      expect(state.context.membershipError).toBeTruthy();
      expect(state.context.error).toBeNull();
    });

    it('surfaces metrics + membership error when group-members fetch fails (group)', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.resolve([{ name: 'test-group', componentCount: 10, assetCount: 25, size: 1024000 }]);
        }
        return Promise.reject(new Error('Group members fetch failed'));
      });

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-group',
        repositoryType: 'group',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.metrics?.componentCount).toBe(10);
      expect(state.context.groupMembers).toEqual([]);
      expect(state.context.membershipError).toBeTruthy();
      expect(state.context.error).toBeNull();
    });

    it('renders membership + metrics-error when metrics fetch fails', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url.includes('/internal/ui/repositories/details')) {
          return Promise.reject(new Error('Metrics fetch failed'));
        }
        if (url.includes('/v1/repositories/')) {
          if (url.includes('test-group')) return Promise.resolve(mockGroupRepository);
          return Promise.resolve({ name: 'test-repo', type: 'hosted', format: 'maven2' });
        }
        return Promise.resolve([]);
      });

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-group',
        repositoryType: 'group',
      });
      const state = await runUntil(machine, (s) => s.matches('loaded'));
      expect(state.context.metrics).toBeNull();
      expect(state.context.error).toBeTruthy();
      expect(state.context.groupMembers).toEqual(['repo1', 'repo2']);
      expect(state.context.membershipError).toBeNull();
    });

    it('transitions to error only when BOTH metrics and membership fail', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });
      const state = await runUntil(machine, (s) => s.matches('error'));
      expect(state.context.error).toBeTruthy();
      expect(state.context.membershipError).toBeTruthy();
    });
  });

  describe('refresh functionality', () => {
    it('transitions to loading on REFRESH from loaded state', async () => {
      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });

      await new Promise<void>((resolve) => {
        let sawFirstLoaded = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('loaded') && !sawFirstLoaded) {
            sawFirstLoaded = true;
            mockRestClient.get.mockClear();
            service.send({ type: 'REFRESH' });
          } else if (state.matches('loaded') && sawFirstLoaded) {
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });
  });

  describe('retry functionality', () => {
    it('transitions to loading on RETRY from error state', async () => {
      let callCount = 0;
      mockRestClient.get.mockImplementation(() => {
        callCount++;
        if (callCount <= 2) {
          return Promise.reject(new Error('Network error'));
        }
        return Promise.resolve(mockMetricsData);
      });

      const machine = createRepositoryUsageMachine({
        repositoryName: 'test-maven-repo',
        repositoryType: 'hosted',
      });

      await new Promise<void>((resolve) => {
        let sawError = false;
        const service = interpret(machine).onTransition((state) => {
          if (state.matches('error') && !sawError) {
            sawError = true;
            service.send({ type: 'RETRY' });
          } else if (state.matches('loaded')) {
            expect(state.context.metrics?.componentCount).toBe(10);
            service.stop();
            resolve();
          }
        });
        service.start();
      });
    });
  });
});
