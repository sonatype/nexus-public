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
import Axios from 'axios';
import { useRawSearch } from '../useRawSearch';

jest.mock('axios');
const mockAxios = Axios as jest.Mocked<typeof Axios>;

const makeRawItem = (name: string) => ({
  id: `raw-${name}`,
  repository: 'raw-hosted',
  format: 'raw',
  group: null,
  name,
  version: '',
  assets: [
    {
      id: `asset-${name}`,
      path: `/files/${name}.txt`,
      downloadUrl: `/repository/raw-hosted/files/${name}.txt`,
      contentType: 'text/plain',
      fileSize: 1024,
      lastModified: '2024-01-15T12:00:00Z',
    },
  ],
});

const defaultItems = [makeRawItem('readme'), makeRawItem('config')];

beforeEach(() => {
  jest.clearAllMocks();
  mockAxios.get.mockResolvedValue({
    data: { items: defaultItems, continuationToken: undefined },
  });
});

describe('useRawSearch', () => {
  it('initializes with empty state', () => {
    const { result } = renderHook(() => useRawSearch());

    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.results).toHaveLength(0);
    expect(result.current.hasMore).toBe(false);
    expect(result.current.state.error).toBeUndefined();
  });

  it('setFilters() updates filter state', () => {
    const { result } = renderHook(() => useRawSearch());

    act(() => {
      result.current.setFilters({ keyword: 'readme' });
    });

    expect(result.current.state.filters.keyword).toBe('readme');
  });

  it('search() uses current filters and populates results', async () => {
    const { result } = renderHook(() => useRawSearch());

    act(() => {
      result.current.setFilters({ keyword: 'config' });
    });

    await act(async () => {
      await result.current.search();
    });

    await waitFor(() => expect(result.current.state.loading).toBe(false));
    expect(result.current.state.results).toHaveLength(2);
    expect(mockAxios.get).toHaveBeenCalledTimes(1);
  });

  it('search() with Error instance uses error.message', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Network error'));
    const { result } = renderHook(() => useRawSearch());

    await act(async () => {
      await result.current.search();
    });

    await waitFor(() => expect(result.current.state.error).toBe('Network error'));
    expect(result.current.state.loading).toBe(false);
  });

  it('search() with non-Error thrown uses fallback string', async () => {
    mockAxios.get.mockRejectedValueOnce('string error');
    const { result } = renderHook(() => useRawSearch());

    await act(async () => {
      await result.current.search();
    });

    await waitFor(() => expect(result.current.state.error).toBe('Search failed'));
  });

  it('loadMore() does nothing when no continuationToken', async () => {
    const { result } = renderHook(() => useRawSearch());

    await act(async () => {
      await result.current.loadMore();
    });

    expect(mockAxios.get).not.toHaveBeenCalled();
  });

  it('loadMore() appends results when continuationToken present', async () => {
    const firstPage = [makeRawItem('readme')];
    const secondPage = [makeRawItem('config')];

    mockAxios.get
      .mockResolvedValueOnce({ data: { items: firstPage, continuationToken: 'token-1' } })
      .mockResolvedValueOnce({ data: { items: secondPage, continuationToken: undefined } });

    const { result } = renderHook(() => useRawSearch());

    await act(async () => {
      await result.current.search();
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(1));
    expect(result.current.hasMore).toBe(true);

    await act(async () => {
      await result.current.loadMore();
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(2));
    expect(result.current.hasMore).toBe(false);
  });

  it('loadMore() handles Error instance', async () => {
    mockAxios.get
      .mockResolvedValueOnce({ data: { items: defaultItems, continuationToken: 'token-1' } })
      .mockRejectedValueOnce(new Error('Load failed'));

    const { result } = renderHook(() => useRawSearch());

    await act(async () => {
      await result.current.search();
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));

    await act(async () => {
      await result.current.loadMore();
    });
    await waitFor(() => expect(result.current.state.error).toBe('Load failed'));
  });

  it('loadMore() handles non-Error thrown', async () => {
    mockAxios.get
      .mockResolvedValueOnce({ data: { items: defaultItems, continuationToken: 'token-1' } })
      .mockRejectedValueOnce({ code: 500 });

    const { result } = renderHook(() => useRawSearch());

    await act(async () => {
      await result.current.search();
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));

    await act(async () => {
      await result.current.loadMore();
    });
    await waitFor(() => expect(result.current.state.error).toBe('Failed to load more'));
  });

  it('clear() resets all state to initial', async () => {
    const { result } = renderHook(() => useRawSearch());

    act(() => {
      result.current.setFilters({ keyword: 'readme' });
    });
    await act(async () => {
      await result.current.search();
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(2));

    act(() => {
      result.current.clear();
    });

    expect(result.current.state.results).toHaveLength(0);
    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.error).toBeUndefined();
    expect(result.current.hasMore).toBe(false);
  });

  it('hasMore reflects presence of continuationToken', async () => {
    mockAxios.get.mockResolvedValue({
      data: { items: defaultItems, continuationToken: 'next' },
    });
    const { result } = renderHook(() => useRawSearch());

    expect(result.current.hasMore).toBe(false);

    await act(async () => {
      await result.current.search();
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));
  });
});
