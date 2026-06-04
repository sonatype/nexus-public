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
import Axios from 'axios';
import { useRepositories, clearRepositoriesCache } from '../useRepositories';

jest.mock('axios');

const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useRepositories', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Clear cache before each test to ensure fresh state
    clearRepositoriesCache();
  });

  it('fetches all repositories when no format is provided', async () => {
    mockedAxios.get.mockResolvedValueOnce({
      data: [
        { name: 'maven-central', format: 'maven2', type: 'proxy' },
        { name: 'npm-hosted', format: 'npm', type: 'hosted' },
      ],
    });

    const { result } = renderHook(() => useRepositories());

    // Initially loading
    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.repositories).toContain('maven-central');
    expect(result.current.repositories).toContain('npm-hosted');
    expect(result.current.availableFormats).toEqual(new Set(['maven2', 'npm']));
    expect(result.current.formatCounts).toEqual({ maven2: 1, npm: 1 });
    expect(result.current.error).toBeUndefined();
    // Hook fetches all repositories (no format filter in URL)
    expect(mockedAxios.get).toHaveBeenCalledWith('/service/rest/v1/repositories');
  });

  it('filters repositories by format client-side', async () => {
    mockedAxios.get.mockResolvedValueOnce({
      data: [
        { name: 'maven-central', format: 'maven2', type: 'proxy' },
        { name: 'npm-hosted', format: 'npm', type: 'hosted' },
      ],
    });

    const { result } = renderHook(() => useRepositories('maven2'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    // Should only return maven repositories (filtered client-side)
    expect(result.current.repositories).toContain('maven-central');
    expect(result.current.repositories).not.toContain('npm-hosted');
    // API is called without format param (fetches all, filters client-side)
    expect(mockedAxios.get).toHaveBeenCalledWith('/service/rest/v1/repositories');
  });

  it('handles errors gracefully', async () => {
    mockedAxios.get.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useRepositories());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.repositories).toEqual([]);
    expect(result.current.error).toBe('Network error');
  });

  it('handles non-Error exceptions', async () => {
    mockedAxios.get.mockRejectedValueOnce('Unknown error');

    const { result } = renderHook(() => useRepositories());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe('Failed to fetch repositories');
  });

  it('filters by format when format changes', async () => {
    mockedAxios.get.mockResolvedValueOnce({
      data: [
        { name: 'maven-central', format: 'maven2', type: 'proxy' },
        { name: 'npm-proxy', format: 'npm', type: 'proxy' },
      ],
    });

    const { result, rerender } = renderHook(
      ({ format }) => useRepositories(format),
      { initialProps: { format: 'maven2' as string | undefined } }
    );

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.repositories).toContain('maven-central');
    expect(result.current.repositories).not.toContain('npm-proxy');

    // Change format - should re-filter client-side (no new API call due to caching)
    rerender({ format: 'npm' });

    expect(result.current.repositories).toContain('npm-proxy');
    expect(result.current.repositories).not.toContain('maven-central');
  });
});

