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

import { act, renderHook, waitFor } from '@testing-library/react';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
  parseApiError: (err: unknown) => ({
    status: 0,
    message: err instanceof Error ? err.message : String(err),
  }),
}));

import { restClient } from '../../../../../../../interface/api';
import { useRepositoryCdnList } from '../useRepositoryCdnList';

const mockedGet = restClient.get as jest.Mock;
const mockedPost = restClient.post as jest.Mock;

const BULK_URL = '/service/rest/internal/ui/repositories/global-distribution';

const detailsResponse = [
  {
    name: 'maven-releases',
    type: 'hosted',
    format: 'maven2',
    blobStoreName: 'default',
    globalDistribution: false,
  },
  {
    name: 'maven-central',
    type: 'proxy',
    format: 'maven2',
    blobStoreName: 'default',
    globalDistribution: true,
  },
  {
    name: 'maven-public',
    type: 'group',
    format: 'maven2',
  },
];

describe('useRepositoryCdnList', () => {
  beforeEach(() => {
    mockedGet.mockReset();
    mockedPost.mockReset();
  });

  it('includes every repository type on load (hosted, proxy, group)', async () => {
    mockedGet.mockResolvedValueOnce(detailsResponse);

    const { result } = renderHook(() => useRepositoryCdnList());

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.rows.map((r) => r.name)).toEqual([
      'maven-releases',
      'maven-central',
      'maven-public',
    ]);
    expect(result.current.rows[0]).toMatchObject({
      id: 'maven-releases',
      type: 'hosted',
      format: 'maven2',
      blobStoreName: 'default',
      globalDistribution: false,
    });
    // Group rows have no blob store; blobStoreName maps to ''.
    expect(result.current.rows[2]).toMatchObject({
      id: 'maven-public',
      type: 'group',
      blobStoreName: '',
      globalDistribution: false,
    });
    expect(result.current.error).toBeNull();
  });

  it('surfaces the error message when the details endpoint fails', async () => {
    mockedGet.mockRejectedValueOnce(new Error('boom'));

    const { result } = renderHook(() => useRepositoryCdnList());

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.error).toBe('boom');
    expect(result.current.rows).toEqual([]);
  });

  it('treats an empty details response as an empty list', async () => {
    mockedGet.mockResolvedValueOnce([]);
    const { result } = renderHook(() => useRepositoryCdnList());
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.rows).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('applyCdnState with no ids is a no-op', async () => {
    mockedGet.mockResolvedValueOnce(detailsResponse);
    const { result } = renderHook(() => useRepositoryCdnList());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    let outcome: Awaited<ReturnType<typeof result.current.applyCdnState>> | undefined;
    await act(async () => {
      outcome = await result.current.applyCdnState([], true);
    });

    expect(outcome).toEqual({ updated: [], failed: [] });
    expect(mockedPost).not.toHaveBeenCalled();
  });

  it('POSTs a single bulk request and returns updated ids', async () => {
    mockedGet.mockResolvedValueOnce(detailsResponse);
    const { result } = renderHook(() => useRepositoryCdnList());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    mockedPost.mockResolvedValueOnce({
      updated: ['maven-releases', 'maven-central'],
      failed: [],
    });

    let outcome: Awaited<ReturnType<typeof result.current.applyCdnState>> | undefined;
    await act(async () => {
      outcome = await result.current.applyCdnState(
        ['maven-releases', 'maven-central'],
        true,
      );
    });

    expect(outcome).toEqual({
      updated: ['maven-releases', 'maven-central'],
      failed: [],
    });
    expect(mockedPost).toHaveBeenCalledTimes(1);
    expect(mockedPost).toHaveBeenCalledWith(BULK_URL, {
      repositoryNames: ['maven-releases', 'maven-central'],
      globalDistribution: true,
    });
  });

  it('maps partial-failure response name->id and preserves both lists', async () => {
    mockedGet.mockResolvedValueOnce(detailsResponse);
    const { result } = renderHook(() => useRepositoryCdnList());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    mockedPost.mockResolvedValueOnce({
      updated: ['maven-releases'],
      failed: [{ name: 'maven-central', message: 'validation failed' }],
    });

    let outcome: Awaited<ReturnType<typeof result.current.applyCdnState>> | undefined;
    await act(async () => {
      outcome = await result.current.applyCdnState(
        ['maven-releases', 'maven-central'],
        true,
      );
    });

    expect(outcome?.updated).toEqual(['maven-releases']);
    expect(outcome?.failed).toEqual([
      { id: 'maven-central', message: 'validation failed' },
    ]);
  });

  it('marks every selected repo as failed when the bulk endpoint itself errors', async () => {
    mockedGet.mockResolvedValueOnce(detailsResponse);
    const { result } = renderHook(() => useRepositoryCdnList());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    mockedPost.mockRejectedValueOnce(new Error('403 Forbidden'));

    let outcome: Awaited<ReturnType<typeof result.current.applyCdnState>> | undefined;
    await act(async () => {
      outcome = await result.current.applyCdnState(
        ['maven-releases', 'maven-central'],
        true,
      );
    });

    expect(outcome?.updated).toEqual([]);
    expect(outcome?.failed).toEqual([
      { id: 'maven-releases', message: '403 Forbidden' },
      { id: 'maven-central', message: '403 Forbidden' },
    ]);
  });

  it('reconciles local rows with the applied value on success', async () => {
    mockedGet.mockResolvedValueOnce(detailsResponse);
    const { result } = renderHook(() => useRepositoryCdnList());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    mockedPost.mockResolvedValueOnce({
      updated: ['maven-releases'],
      failed: [],
    });

    await act(async () => {
      await result.current.applyCdnState(['maven-releases'], true);
    });

    const updated = result.current.rows.find((r) => r.id === 'maven-releases');
    expect(updated?.globalDistribution).toBe(true);
  });
});
