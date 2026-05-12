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

const mockGet = jest.fn();
const mockPost = jest.fn();

jest.mock('@/utils/api', () => {
  return {
    __esModule: true,
    restClient: {
      get: jest.fn(function() { return mockGet.apply(null, arguments); }),
      post: jest.fn(function() { return mockPost.apply(null, arguments); }),
    },
    ENDPOINTS: {
      MALWARE_COUNTS: '/service/rest/internal/ui/malware/counts',
      MALICIOUS_RISK_ACTIVE_FINDINGS: '/service/rest/v1/malicious-risk/active-findings',
      MALICIOUS_RISK_HISTORY: '/service/rest/v1/malicious-risk/history',
      MALICIOUS_RISK_ACKNOWLEDGE: '/service/rest/v1/malicious-risk/acknowledge',
      MALICIOUS_RISK_DELETE_FINDING: '/service/rest/v1/malicious-risk/delete-finding',
      IQ_CAPABILITIES: '/service/rest/v1/iq/capabilities',
      REPOSITORIES: '/service/rest/v1/repositories',
      HEALTH_CHECK_SUMMARY: '/service/rest/internal/ui/healthcheck/summary',
    },
  };
});

import {
  getRepoMalwareRemediatorBusyStatus,
  isSchedulerTaskRunningState,
  parseTaskCurrentState,
  useMaliciousPackagesData,
  type TaskInfo,
} from '../useMaliciousPackagesData';

const MOCK_MALWARE_COUNTS = {
  totalCount: 3,
  counts: { 'maven-proxy': 2, 'npm-proxy': 1 },
  hdsAvailable: true,
  hcEnabledRepos: ['maven-proxy'],
  detectInitiatedRepos: {},
};

const MOCK_ACTIVE_FINDINGS = [
  {
    id: 1,
    repositoryName: 'maven-proxy',
    assetId: 'abc123',
    path: '/com/evil/malware/1.0/malware-1.0.jar',
    format: 'maven2',
    recordedTime: '2026-03-01T10:00:00Z',
    deletedTime: null,
    deletedBy: null,
    deletionMethod: null,
    acknowledgedAt: null,
    acknowledgedBy: null,
    acknowledgedReason: null,
    firstDetectedAt: '2026-03-01T10:00:00Z',
    hash: 'sha256:deadbeef',
    createdBy: 'user1',
    createdByIp: '10.0.0.1',
    componentName: 'malware',
    componentVersion: '1.0',
    componentFormat: 'maven2',
    threatLevel: 10,
    threatSummary: 'Known malware',
    threatReference: 'https://example.com/threat/1',
    policyName: 'Malware-Policy',
  },
  {
    id: 2,
    repositoryName: 'npm-proxy',
    assetId: 'def456',
    path: '/-/npm-evil-1.0.0.tgz',
    format: 'npm',
    recordedTime: '2026-03-02T12:00:00Z',
    deletedTime: null,
    deletedBy: null,
    deletionMethod: null,
    acknowledgedAt: null,
    acknowledgedBy: null,
    acknowledgedReason: null,
    firstDetectedAt: '2026-03-02T12:00:00Z',
    hash: 'sha256:cafebabe',
    createdBy: 'user2',
    createdByIp: '10.0.0.2',
    componentName: 'npm-evil',
    componentVersion: '1.0.0',
    componentFormat: 'npm',
    threatLevel: 8,
    threatSummary: 'Suspicious package',
    threatReference: 'https://example.com/threat/2',
    policyName: 'Malware-Policy',
  },
];

const MOCK_IQ_CAPABILITIES = {
  connected: true,
  hasFirewall: true,
  hasLifecycle: false,
  url: 'https://iq.example.com',
};

const MOCK_REPOSITORIES = [
  { name: 'maven-proxy', type: 'proxy', format: 'maven2' },
  { name: 'npm-proxy', type: 'proxy', format: 'npm' },
  { name: 'maven-releases', type: 'hosted', format: 'maven2' },
];

function setupDefaultMocks() {
  mockGet.mockImplementation((url: string) => {
    if (url.includes('malware/counts')) return Promise.resolve(MOCK_MALWARE_COUNTS);
    if (url.includes('active-findings')) return Promise.resolve(MOCK_ACTIVE_FINDINGS);
    if (url.includes('iq/capabilities')) return Promise.resolve(MOCK_IQ_CAPABILITIES);
    if (url.includes('healthcheck/summary')) return Promise.resolve([]);
    if (url.includes('repositories')) return Promise.resolve(MOCK_REPOSITORIES);
    if (url.includes('history')) return Promise.resolve([]);
    return Promise.resolve(null);
  });
  mockPost.mockResolvedValue(undefined);
}

describe('useMaliciousPackagesData', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupDefaultMocks();
  });

  it('starts in a loading state', () => {
    const { result } = renderHook(() => useMaliciousPackagesData());
    expect(result.current.loading).toBe(true);
  });

  it('populates state from REST responses', async () => {
    const { result } = renderHook(() => useMaliciousPackagesData());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.activeFindings).toHaveLength(2);
    expect(result.current.malwareCount).toBe(2);
    expect(result.current.countsByRepo).toEqual({ 'maven-proxy': 2 });
    expect(result.current.hasFirewall).toBe(true);
    expect(result.current.hcEnabledRepos).toEqual(['maven-proxy']);
    expect(result.current.totalProxyRepoCount).toBe(2);
    expect(result.current.error).toBeNull();
  });

  it('acknowledge calls the correct endpoint and refetches', async () => {
    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.acknowledge(1, 'false positive');
    });

    expect(mockPost).toHaveBeenCalledWith(
      '/service/rest/v1/malicious-risk/acknowledge/1',
      { reason: 'false positive' }
    );
    expect(mockGet).toHaveBeenCalledWith('/service/rest/internal/ui/malware/counts');
  });

  it('deleteFinding calls the correct endpoint and refetches', async () => {
    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.deleteFinding(2);
    });

    expect(mockPost).toHaveBeenCalledWith(
      '/service/rest/v1/malicious-risk/delete-finding/2'
    );
  });

  it('bulkDelete calls delete endpoint for each ID', async () => {
    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.bulkDelete([1, 2]);
    });

    expect(mockPost).toHaveBeenCalledWith('/service/rest/v1/malicious-risk/delete-finding/1');
    expect(mockPost).toHaveBeenCalledWith('/service/rest/v1/malicious-risk/delete-finding/2');
  });

  it('bulkAcknowledge calls acknowledge endpoint for each ID', async () => {
    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.bulkAcknowledge([1, 2], 'accepted risk');
    });

    expect(mockPost).toHaveBeenCalledWith(
      '/service/rest/v1/malicious-risk/acknowledge/1',
      { reason: 'accepted risk' }
    );
    expect(mockPost).toHaveBeenCalledWith(
      '/service/rest/v1/malicious-risk/acknowledge/2',
      { reason: 'accepted risk' }
    );
  });

  it('sets error state on fetch failure', async () => {
    mockGet.mockImplementation(() => {
      throw new Error('Network error');
    });

    const { result } = renderHook(() => useMaliciousPackagesData());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe('Network error');
    expect(result.current.activeFindings).toEqual([]);
  });

  it('fetchHistory populates historyFindings', async () => {
    const historyData = [
      {
        ...MOCK_ACTIVE_FINDINGS[0],
        id: 10,
        deletedTime: '2026-03-05T10:00:00Z',
        deletedBy: 'admin',
        deletionMethod: 'manual',
      },
    ];
    mockGet.mockImplementation((url: string) => {
      if (url.includes('history')) return Promise.resolve(historyData);
      if (url.includes('malware/counts')) return Promise.resolve(MOCK_MALWARE_COUNTS);
      if (url.includes('active-findings')) return Promise.resolve(MOCK_ACTIVE_FINDINGS);
      if (url.includes('iq/capabilities')) return Promise.resolve(MOCK_IQ_CAPABILITIES);
      if (url.includes('healthcheck/summary')) return Promise.resolve([]);
      if (url.includes('repositories')) return Promise.resolve(MOCK_REPOSITORIES);
      return Promise.resolve(null);
    });

    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.fetchHistory(50, 0);
    });

    expect(result.current.historyFindings).toHaveLength(1);
    expect(result.current.historyFindings[0].id).toBe(10);
  });

  it('sets hasFirewall false when IQ capabilities fail', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url.includes('iq/capabilities')) return Promise.reject(new Error('fail'));
      if (url.includes('malware/counts')) return Promise.resolve(MOCK_MALWARE_COUNTS);
      if (url.includes('active-findings')) return Promise.resolve(MOCK_ACTIVE_FINDINGS);
      if (url.includes('healthcheck/summary')) return Promise.resolve([]);
      if (url.includes('repositories')) return Promise.resolve(MOCK_REPOSITORIES);
      return Promise.resolve(null);
    });

    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.hasFirewall).toBe(false);
  });

  it('provides a refetch function', async () => {
    const { result } = renderHook(() => useMaliciousPackagesData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(typeof result.current.refetch).toBe('function');
    const initialCallCount = mockGet.mock.calls.length;

    await act(async () => {
      result.current.refetch();
    });
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(mockGet.mock.calls.length).toBeGreaterThan(initialCallCount);
  });
});

describe('parseTaskCurrentState', () => {
  it('splits RUNNING state with progress suffix from REST TaskXO', () => {
    expect(parseTaskCurrentState('RUNNING: 17%')).toEqual({ state: 'RUNNING', progress: '17%' });
  });

  it('returns bare state when no progress', () => {
    expect(parseTaskCurrentState('RUNNING')).toEqual({ state: 'RUNNING', progress: null });
    expect(parseTaskCurrentState('WAITING')).toEqual({ state: 'WAITING', progress: null });
  });

  it('handles null and empty', () => {
    expect(parseTaskCurrentState(null)).toEqual({ state: null, progress: null });
    expect(parseTaskCurrentState('')).toEqual({ state: null, progress: null });
  });
});

describe('getRepoMalwareRemediatorBusyStatus', () => {
  const baseTask = (overrides: Partial<TaskInfo>): TaskInfo => ({
    id: 't1',
    name: 'Malicious Packages - r',
    repositoryName: 'npm-proxy',
    mode: 'delete',
    enabled: true,
    lastRun: null,
    lastRunResult: null,
    nextRun: null,
    currentState: 'RUNNING',
    progress: null,
    ...overrides,
  });

  it('returns running when a task for that repo is in a running group state', () => {
    expect(
      getRepoMalwareRemediatorBusyStatus([baseTask({ currentState: 'RUNNING' })], 'npm-proxy'),
    ).toBe('running');
  });

  it('returns null when state is WAITING (idle, not busy)', () => {
    expect(
      getRepoMalwareRemediatorBusyStatus([baseTask({ currentState: 'WAITING' })], 'npm-proxy'),
    ).toBeNull();
  });

  it('returns null for a different repository', () => {
    expect(
      getRepoMalwareRemediatorBusyStatus([baseTask({ currentState: 'RUNNING' })], 'other-repo'),
    ).toBeNull();
  });

  it('returns null when task is finished', () => {
    expect(
      getRepoMalwareRemediatorBusyStatus([baseTask({ currentState: 'OK' })], 'npm-proxy'),
    ).toBeNull();
  });
});

describe('isSchedulerTaskRunningState', () => {
  it('is true for scheduler running group states', () => {
    expect(isSchedulerTaskRunningState('RUNNING')).toBe(true);
    expect(isSchedulerTaskRunningState('RUNNING_STARTING')).toBe(true);
    expect(isSchedulerTaskRunningState('RUNNING_BLOCKED')).toBe(true);
    expect(isSchedulerTaskRunningState('RUNNING_CANCELED')).toBe(true);
  });

  it('is false for terminal and waiting states', () => {
    expect(isSchedulerTaskRunningState('WAITING')).toBe(false);
    expect(isSchedulerTaskRunningState('OK')).toBe(false);
    expect(isSchedulerTaskRunningState(null)).toBe(false);
  });
});
