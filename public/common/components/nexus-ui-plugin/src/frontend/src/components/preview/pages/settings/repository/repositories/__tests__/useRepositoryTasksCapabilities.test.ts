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
import { useRepositoryTasksCapabilities } from '../useRepositoryTasksCapabilities';

const mockRestClient = {
  get: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.message || 'An error occurred',
  })),
}));

const TASKS_URL = '/service/rest/v1/tasks';
const CAPABILITIES_URL = '/service/rest/v1/capabilities';

const mockTaskForRepo = {
  id: 't1',
  name: 'Cleanup my-repo',
  typeId: 'repository.cleanup',
  schedule: 'daily',
  lastRun: '2025-01-01T00:00:00Z',
  lastRunResult: 'ok',
  nextRun: '2025-01-02T00:00:00Z',
  currentState: 'WAITING',
  properties: { repositoryName: 'my-repo' },
};

const mockTaskForOtherRepo = {
  id: 't2',
  name: 'Cleanup other',
  typeId: 'repository.cleanup',
  properties: { repositoryName: 'other-repo' },
};

const mockCapabilityForRepo = {
  id: 'c1',
  type: 'firewall.audit',
  enabled: true,
  notes: 'PCCS',
  properties: { repository: 'my-repo' },
};

const mockCapabilityWithRepoNameKey = {
  id: 'c2',
  type: 'healthcheck',
  enabled: true,
  properties: { repositoryName: 'my-repo' },
};

const mockInstanceWideCapability = {
  id: 'c3',
  type: 'logging',
  enabled: true,
  properties: {},
};

const mockCapabilityForOtherRepo = {
  id: 'c4',
  type: 'other',
  enabled: false,
  properties: { repository: 'other-repo' },
};

function mockTasksResponse(items: unknown[]): void {
  mockRestClient.get.mockImplementation((url: string) => {
    if (url === TASKS_URL) return Promise.resolve({ items });
    if (url === CAPABILITIES_URL) return Promise.resolve([]);
    return Promise.reject(new Error(`Unexpected URL: ${url}`));
  });
}

function mockCapabilitiesResponse(items: unknown[]): void {
  mockRestClient.get.mockImplementation((url: string) => {
    if (url === TASKS_URL) return Promise.resolve({ items: [] });
    if (url === CAPABILITIES_URL) return Promise.resolve(items);
    return Promise.reject(new Error(`Unexpected URL: ${url}`));
  });
}

function mockBothResponses(tasks: unknown[], capabilities: unknown[]): void {
  mockRestClient.get.mockImplementation((url: string) => {
    if (url === TASKS_URL) return Promise.resolve({ items: tasks });
    if (url === CAPABILITIES_URL) return Promise.resolve(capabilities);
    return Promise.reject(new Error(`Unexpected URL: ${url}`));
  });
}

describe('useRepositoryTasksCapabilities', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shouldFetchTasksAndCapabilitiesInParallel', async () => {
    mockBothResponses([mockTaskForRepo], [mockCapabilityForRepo]);

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(mockRestClient.get).toHaveBeenCalledWith(TASKS_URL);
    expect(mockRestClient.get).toHaveBeenCalledWith(CAPABILITIES_URL);
    expect(result.current.tasks).toHaveLength(1);
    expect(result.current.capabilities).toHaveLength(1);
    expect(result.current.error).toBeNull();
  });

  it('shouldFilterTasksByRepositoryName', async () => {
    mockTasksResponse([mockTaskForRepo, mockTaskForOtherRepo]);

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.tasks).toHaveLength(1);
    expect(result.current.tasks[0].id).toBe('t1');
    expect(result.current.tasks[0].type).toBe('repository.cleanup');
  });

  it('shouldFilterCapabilitiesByExplicitRepositoryMatchOnly', async () => {
    mockCapabilitiesResponse([
      mockCapabilityForRepo,
      mockCapabilityWithRepoNameKey,
      mockInstanceWideCapability,
      mockCapabilityForOtherRepo,
    ]);

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    const ids = result.current.capabilities.map((c) => c.id).sort();
    expect(ids).toEqual(['c1', 'c2']);
  });

  it('shouldExposeLoadingStateUntilBothCallsResolve', async () => {
    let resolveTasks!: (v: { items: unknown[] }) => void;
    let resolveCapabilities!: (v: unknown[]) => void;
    mockRestClient.get.mockImplementation((url: string) => {
      if (url === TASKS_URL) return new Promise((r) => { resolveTasks = r; });
      if (url === CAPABILITIES_URL) return new Promise((r) => { resolveCapabilities = r; });
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));
    expect(result.current.loading).toBe(true);

    await act(async () => {
      resolveTasks({ items: [] });
    });
    expect(result.current.loading).toBe(true);

    await act(async () => {
      resolveCapabilities([]);
    });
    await waitFor(() => expect(result.current.loading).toBe(false));
  });

  it('shouldExposeErrorWhenTasksCallFails', async () => {
    mockRestClient.get.mockImplementation((url: string) => {
      if (url === TASKS_URL) return Promise.reject(new Error('Tasks blew up'));
      if (url === CAPABILITIES_URL) return Promise.resolve([]);
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('Tasks blew up');
    expect(result.current.tasks).toEqual([]);
    expect(result.current.capabilities).toEqual([]);
  });

  it('shouldExposeErrorWhenCapabilitiesCallFails', async () => {
    mockRestClient.get.mockImplementation((url: string) => {
      if (url === TASKS_URL) return Promise.resolve({ items: [] });
      if (url === CAPABILITIES_URL) return Promise.reject(new Error('Caps down'));
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('Caps down');
  });

  it('shouldSupportManualRefetch', async () => {
    mockBothResponses([], []);

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));
    await waitFor(() => expect(result.current.loading).toBe(false));

    const callsBefore = mockRestClient.get.mock.calls.length;

    await act(async () => {
      result.current.refetch();
    });
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(mockRestClient.get.mock.calls.length).toBeGreaterThan(callsBefore);
  });

  it('shouldReturnEmptyArraysWhenTasksResponseHasNoItems', async () => {
    mockRestClient.get.mockImplementation((url: string) => {
      if (url === TASKS_URL) return Promise.resolve({});
      if (url === CAPABILITIES_URL) return Promise.resolve([]);
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.tasks).toEqual([]);
    expect(result.current.capabilities).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('shouldReturnEmptyCapabilitiesWhenCapabilitiesResponseIsNotAnArray', async () => {
    mockRestClient.get.mockImplementation((url: string) => {
      if (url === TASKS_URL) return Promise.resolve({ items: [] });
      if (url === CAPABILITIES_URL) return Promise.resolve(null);
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { result } = renderHook(() => useRepositoryTasksCapabilities('my-repo'));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.capabilities).toEqual([]);
  });

  describe('permission-aware fetching', () => {
    it('shouldSkipTasksFetchWhenCanReadTasksIsFalse', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url === CAPABILITIES_URL) return Promise.resolve([mockCapabilityForRepo]);
        return Promise.reject(new Error(`Unexpected URL: ${url}`));
      });

      const { result } = renderHook(() =>
        useRepositoryTasksCapabilities('my-repo', { canReadTasks: false })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(mockRestClient.get).not.toHaveBeenCalledWith(TASKS_URL);
      expect(mockRestClient.get).toHaveBeenCalledWith(CAPABILITIES_URL);
      expect(result.current.tasks).toEqual([]);
      expect(result.current.capabilities).toHaveLength(1);
    });

    it('shouldSkipCapabilitiesFetchWhenCanReadCapabilitiesIsFalse', async () => {
      mockRestClient.get.mockImplementation((url: string) => {
        if (url === TASKS_URL) return Promise.resolve({ items: [mockTaskForRepo] });
        return Promise.reject(new Error(`Unexpected URL: ${url}`));
      });

      const { result } = renderHook(() =>
        useRepositoryTasksCapabilities('my-repo', { canReadCapabilities: false })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(mockRestClient.get).toHaveBeenCalledWith(TASKS_URL);
      expect(mockRestClient.get).not.toHaveBeenCalledWith(CAPABILITIES_URL);
      expect(result.current.tasks).toHaveLength(1);
      expect(result.current.capabilities).toEqual([]);
    });

    it('shouldSkipBothFetchesWhenBothFlagsAreFalse', async () => {
      const { result } = renderHook(() =>
        useRepositoryTasksCapabilities('my-repo', {
          canReadTasks: false,
          canReadCapabilities: false,
        })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(mockRestClient.get).not.toHaveBeenCalled();
      expect(result.current.tasks).toEqual([]);
      expect(result.current.capabilities).toEqual([]);
      expect(result.current.error).toBeNull();
    });
  });
});
