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

jest.mock('../../../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
  ENDPOINTS: { SEARCH_REPOSITORIES: '/service/rest/v1/search/repositories' },
}));
jest.mock('../../../../config/featureFlags', () => ({ isMockMode: () => false }));

import { restClient, ENDPOINTS } from '../../../../../../interface/api';
import { useGARepositoriesForVersion } from '../useGARepositoriesForVersion';

const mockGet = restClient.get as jest.Mock;

const RESULT = {
  items: [{ repositoryName: 'r-a', type: 'hosted', versionCount: 2 }],
  totalCount: 1,
};

beforeEach(() => {
  mockGet.mockReset();
});

describe('useGARepositoriesForVersion', () => {
  it('returns empty state with no selectedVersion and does not fetch', async () => {
    const { result } = renderHook(() =>
      useGARepositoriesForVersion({ gaId: 'maven:g:n', selectedVersion: null }),
    );

    expect(result.current.rows).toEqual([]);
    expect(result.current.totalCount).toBe(0);
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(mockGet).not.toHaveBeenCalled();
  });

  it('fetches and populates rows when a version is selected', async () => {
    mockGet.mockResolvedValueOnce(RESULT);

    const { result } = renderHook(() =>
      useGARepositoriesForVersion({ gaId: 'maven:g:n', selectedVersion: '1.0' }),
    );

    await waitFor(() => expect(result.current.rows.length).toBe(1));
    expect(result.current.totalCount).toBe(1);
    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(mockGet.mock.calls[0][0]).toContain(ENDPOINTS.SEARCH_REPOSITORIES);
    expect(mockGet.mock.calls[0][0]).toContain('format=maven');
    expect(mockGet.mock.calls[0][0]).toContain('namespace=g');
    expect(mockGet.mock.calls[0][0]).toContain('name=n');
    expect(mockGet.mock.calls[0][0]).toContain('version=1.0');
  });

  it('does not refetch when switching back to a previously loaded version', async () => {
    mockGet.mockResolvedValueOnce(RESULT);
    mockGet.mockResolvedValueOnce({ items: [], totalCount: 0 });

    const { result, rerender } = renderHook(
      ({ v }: { v: string | null }) =>
        useGARepositoriesForVersion({ gaId: 'maven:g:n', selectedVersion: v }),
      { initialProps: { v: '1.0' } },
    );
    await waitFor(() => expect(result.current.rows.length).toBe(1));
    expect(mockGet).toHaveBeenCalledTimes(1);

    rerender({ v: '2.0' });
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));

    rerender({ v: '1.0' });
    // No third call — cache hit
    expect(mockGet).toHaveBeenCalledTimes(2);
    expect(result.current.rows.length).toBe(1);
  });

  it('invalidates cache when gaId changes', async () => {
    mockGet.mockResolvedValue(RESULT);

    const { rerender } = renderHook(
      ({ g }: { g: string }) =>
        useGARepositoriesForVersion({ gaId: g, selectedVersion: '1.0' }),
      { initialProps: { g: 'maven:g:n' } },
    );
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(1));

    rerender({ g: 'maven:g:other' });
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));
  });

  it('surfaces fetch errors and clears them on refresh', async () => {
    mockGet.mockRejectedValueOnce(new Error('nope'));
    mockGet.mockResolvedValueOnce(RESULT);

    const { result } = renderHook(() =>
      useGARepositoriesForVersion({ gaId: 'maven:g:n', selectedVersion: '1.0' }),
    );

    await waitFor(() => expect(result.current.error).not.toBeNull());
    expect(result.current.error).toContain('nope');

    result.current.refresh();
    await waitFor(() => expect(result.current.rows.length).toBe(1));
    expect(result.current.error).toBeNull();
  });
});
