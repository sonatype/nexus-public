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

import { useRepositoryMetrics } from '../useRepositoryMetrics';

const mockFetchRepositoryMetricsDetail = jest.fn();

jest.mock('../fetchRepositoryMetricsDetail', () => ({
  fetchRepositoryMetricsDetail: (...args: unknown[]) => mockFetchRepositoryMetricsDetail(...args),
}));

describe('useRepositoryMetrics', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches via the shared /details/filtered helper with the given repository name', async () => {
    mockFetchRepositoryMetricsDetail.mockResolvedValue({
      name: 'test-repo',
      componentCount: 10,
      assetCount: 25,
      size: 1024000,
    });

    const { result } = renderHook(() => useRepositoryMetrics('test-repo'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(mockFetchRepositoryMetricsDetail).toHaveBeenCalledWith('test-repo');
    expect(result.current.data).toEqual({ componentCount: 10, assetCount: 25, totalSize: 1024000 });
    expect(result.current.error).toBeNull();
  });

  it('coerces missing metric fields to 0 (unlike repositoryUsageMachine, which preserves undefined)', async () => {
    mockFetchRepositoryMetricsDetail.mockResolvedValue({ name: 'test-repo' });

    const { result } = renderHook(() => useRepositoryMetrics('test-repo'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual({ componentCount: 0, assetCount: 0, totalSize: 0 });
  });

  it('surfaces an error when the fetch fails', async () => {
    mockFetchRepositoryMetricsDetail.mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useRepositoryMetrics('test-repo'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('boom');
    expect(result.current.data).toBeNull();
  });
});
