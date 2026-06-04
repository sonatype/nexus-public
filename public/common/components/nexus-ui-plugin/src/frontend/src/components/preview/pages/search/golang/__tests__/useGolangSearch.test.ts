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
import { useGolangSearch } from '../useGolangSearch';

jest.mock('axios');
const mockAxios = Axios as jest.Mocked<typeof Axios>;

const makeRawItem = (name: string, version: string) => ({
  id: `go:${name}:${version}`,
  repository: 'go-proxy',
  format: 'go',
  group: null,
  name,
  version,
  assets: [
    {
      id: `asset-${name}`,
      path: `/${name}/@v/${version}.info`,
      downloadUrl: `/repository/go-proxy/${name}/@v/${version}.info`,
    },
  ],
});

const defaultItems = [
  makeRawItem('github.com/gin-gonic/gin', 'v1.9.1'),
  makeRawItem('github.com/stretchr/testify', 'v1.8.4'),
];

beforeEach(() => {
  jest.clearAllMocks();
  mockAxios.get.mockResolvedValue({
    data: { items: defaultItems, continuationToken: undefined },
  });
});

describe('useGolangSearch', () => {
  it('initializes with empty state', () => {
    const { result } = renderHook(() => useGolangSearch());

    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.results).toHaveLength(0);
    expect(result.current.hasMore).toBe(false);
    expect(result.current.state.error).toBeUndefined();
  });

  it('search() populates results on success', async () => {
    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({ module: 'gin' });
    });

    await waitFor(() => expect(result.current.state.loading).toBe(false));
    expect(result.current.state.results).toHaveLength(2);
    expect(mockAxios.get).toHaveBeenCalledTimes(1);
  });

  it('search() with Error instance uses error.message', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Network error'));
    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
    });

    await waitFor(() => expect(result.current.state.error).toBe('Network error'));
    expect(result.current.state.loading).toBe(false);
  });

  it('search() with non-Error thrown uses fallback string', async () => {
    mockAxios.get.mockRejectedValueOnce('string error');
    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
    });

    await waitFor(() => expect(result.current.state.error).toBe('Search failed'));
  });

  it('loadMore() does nothing when no continuationToken', async () => {
    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.loadMore();
    });

    expect(mockAxios.get).not.toHaveBeenCalled();
  });

  it('loadMore() does nothing when already loading', async () => {
    let resolveSearch!: () => void;
    mockAxios.get.mockReturnValueOnce(
      new Promise((res) => {
        resolveSearch = () => res({ data: { items: defaultItems, continuationToken: 'tok' } });
      })
    );
    const { result } = renderHook(() => useGolangSearch());

    // Start search without awaiting — hook stays in loading=true
    act(() => { result.current.search({}); });

    // loadMore should be a no-op while loading=true
    await act(async () => { await result.current.loadMore(); });
    expect(mockAxios.get).toHaveBeenCalledTimes(1); // only the search call, not a second loadMore call

    resolveSearch();
  });

  it('loadMore() appends results when continuationToken present', async () => {
    const firstPage = [makeRawItem('github.com/gin-gonic/gin', 'v1.9.1')];
    const secondPage = [makeRawItem('github.com/gorilla/mux', 'v1.8.1')];

    mockAxios.get
      .mockResolvedValueOnce({ data: { items: firstPage, continuationToken: 'token-1' } })
      .mockResolvedValueOnce({ data: { items: secondPage, continuationToken: undefined } });

    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
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

    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
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

    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));

    await act(async () => {
      await result.current.loadMore();
    });
    await waitFor(() => expect(result.current.state.error).toBe('Failed to load more'));
  });

  it('clear() resets all state to initial', async () => {
    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({ module: 'gin' });
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
    const { result } = renderHook(() => useGolangSearch());

    expect(result.current.hasMore).toBe(false);

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));
  });

  it('aggregation update path: same module increments versionsCount', async () => {
    const items = [
      makeRawItem('github.com/gin-gonic/gin', 'v1.8.0'),
      makeRawItem('github.com/gin-gonic/gin', 'v1.9.1'),
    ];
    mockAxios.get.mockResolvedValue({ data: { items, continuationToken: undefined } });

    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(1));

    const module = result.current.state.results[0];
    expect(module.versionsCount).toBe(2);
    expect(module.latestVersion).toBe('v1.9.1');
  });

  it('aggregation create path: new module creates entry with versionsCount 1', async () => {
    const items = [makeRawItem('golang.org/x/net', 'v0.20.0')];
    mockAxios.get.mockResolvedValue({ data: { items, continuationToken: undefined } });

    const { result } = renderHook(() => useGolangSearch());

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(1));

    const module = result.current.state.results[0];
    expect(module.versionsCount).toBe(1);
    expect(module.module).toBe('golang.org/x/net');
    expect(module.latestVersion).toBe('v0.20.0');
  });
});
