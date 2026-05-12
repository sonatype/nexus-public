/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import { renderHook, waitFor, act } from '@testing-library/react';
import { useRepositoriesByFormat } from '../useRepositoriesByFormat';

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
    get: (...args) => mockRestClient.get(...args),
    post: (...args) => mockRestClient.post(...args),
    put: (...args) => mockRestClient.put(...args),
    delete: (...args) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

const emptyMalwareResponse = {
  counts: {},
  totalCount: 0,
  hdsAvailable: true,
  hcEnabledRepos: [],
};

describe('useRepositoriesByFormat', () => {
  let reposPayload;

  const mockRepositories = [
    { name: 'maven-central', format: 'maven2', type: 'proxy' },
    { name: 'maven-releases', format: 'maven2', type: 'hosted' },
    { name: 'maven-snapshots', format: 'maven2', type: 'hosted' },
    { name: 'maven-public', format: 'maven2', type: 'group' },
    { name: 'npm-proxy', format: 'npm', type: 'proxy' },
    { name: 'npm-hosted', format: 'npm', type: 'hosted' },
    { name: 'docker-hub', format: 'docker', type: 'proxy' },
    { name: 'pypi-proxy', format: 'pypi', type: 'proxy' },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    reposPayload = mockRepositories;
    mockRestClient.get.mockImplementation((url) => {
      if (String(url).includes('malware/counts')) {
        return Promise.resolve(emptyMalwareResponse);
      }
      return Promise.resolve(reposPayload);
    });
  });

  it('fetches repositories on mount', async () => {
    const { result } = renderHook(() => useRepositoriesByFormat());

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/repositories');
    expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/malware/counts');
  });

  it('groups repositories by format', async () => {
    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Should have 4 formats: maven2, npm, docker, pypi
    expect(result.current.data.length).toBe(4);

    // Find maven2 row
    const mavenRow = result.current.data.find((r) => r.formatCode === 'maven2');
    expect(mavenRow).toBeDefined();
    expect(mavenRow.proxyCount).toBe(1);
    expect(mavenRow.hostedCount).toBe(2);
    expect(mavenRow.groupCount).toBe(1);

    // Find npm row
    const npmRow = result.current.data.find((r) => r.formatCode === 'npm');
    expect(npmRow).toBeDefined();
    expect(npmRow.proxyCount).toBe(1);
    expect(npmRow.hostedCount).toBe(1);
    expect(npmRow.groupCount).toBe(0);
  });

  it('sorts results by total repo count descending', async () => {
    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // maven2 has 4 repos, npm has 2, docker has 1, pypi has 1
    expect(result.current.data[0].formatCode).toBe('maven2');
    expect(result.current.data[1].formatCode).toBe('npm');
  });

  it('includes total and status counts for each format', async () => {
    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const mavenRow = result.current.data.find((r) => r.formatCode === 'maven2');
    expect(mavenRow).toBeDefined();

    // Check that simplified real-data fields are included
    expect(mavenRow.totalCount).toBeDefined();
    expect(typeof mavenRow.totalCount).toBe('number');
    expect(mavenRow.totalCount).toBe(4); // 1 proxy + 2 hosted + 1 group
    expect(mavenRow.onlineCount).toBeDefined();
    expect(mavenRow.offlineCount).toBeDefined();
  });

  it('sets error on API failure', async () => {
    const errorMessage = 'Network error';
    mockRestClient.get.mockImplementation((url) => {
      if (String(url).includes('malware/counts')) {
        return Promise.resolve(emptyMalwareResponse);
      }
      return Promise.reject({ message: errorMessage });
    });

    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe(errorMessage);
    expect(result.current.data).toEqual([]);
  });

  it('returns empty array when no repositories', async () => {
    reposPayload = [];

    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual([]);
  });

  it('provides refetch function', async () => {
    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(typeof result.current.refetch).toBe('function');

    reposPayload = [];

    act(() => {
      result.current.refetch();
    });

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockRestClient.get).toHaveBeenCalledTimes(4);
  });

  it('converts format display names correctly', async () => {
    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const mavenRow = result.current.data.find((r) => r.formatCode === 'maven2');
    expect(mavenRow.format).toBe('Maven');

    const npmRow = result.current.data.find((r) => r.formatCode === 'npm');
    expect(npmRow.format).toBe('npm');

    const dockerRow = result.current.data.find((r) => r.formatCode === 'docker');
    expect(dockerRow.format).toBe('Docker');
  });

  it('handles case-insensitive format grouping', async () => {
    const mixedCaseRepos = [
      { name: 'repo1', format: 'Maven2', type: 'proxy' },
      { name: 'repo2', format: 'MAVEN2', type: 'hosted' },
      { name: 'repo3', format: 'maven2', type: 'group' },
    ];
    reposPayload = mixedCaseRepos;

    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // All should be grouped under one format
    expect(result.current.data.length).toBe(1);
    expect(result.current.data[0].proxyCount + result.current.data[0].hostedCount + result.current.data[0].groupCount).toBe(3);
  });

  it('merges malware counts and HC-enabled proxy counts per format', async () => {
    mockRestClient.get.mockImplementation((url) => {
      if (String(url).includes('malware/counts')) {
        return Promise.resolve({
          counts: { 'maven-central': 7, 'npm-proxy': 2 },
          totalCount: 9,
          hdsAvailable: true,
          hcEnabledRepos: ['maven-central', 'npm-proxy'],
        });
      }
      return Promise.resolve(mockRepositories);
    });

    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const mavenRow = result.current.data.find((r) => r.formatCode === 'maven2');
    expect(mavenRow.malwareCountsAvailable).toBe(true);
    expect(mavenRow.malwareCount).toBe(7);
    expect(mavenRow.hcEnabledProxyCount).toBe(1);

    const npmRow = result.current.data.find((r) => r.formatCode === 'npm');
    expect(npmRow.malwareCount).toBe(2);
    expect(npmRow.hcEnabledProxyCount).toBe(1);
  });

  it('omits malware aggregates when malware counts endpoint fails', async () => {
    mockRestClient.get.mockImplementation((url) => {
      if (String(url).includes('malware/counts')) {
        return Promise.reject({ response: { status: 404 } });
      }
      return Promise.resolve(mockRepositories);
    });

    const { result } = renderHook(() => useRepositoriesByFormat());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const mavenRow = result.current.data.find((r) => r.formatCode === 'maven2');
    expect(mavenRow.malwareCountsAvailable).toBe(false);
    expect(mavenRow.malwareCount).toBeUndefined();
  });
});

