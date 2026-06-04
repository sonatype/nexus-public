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
import { useHelmSearch } from '../useHelmSearch';

jest.mock('axios');
const mockAxios = Axios as jest.Mocked<typeof Axios>;

const makeRawItem = (name: string, version: string, appVersion?: string, description?: string, icon?: string) => ({
  id: `helm:${name}:${version}`,
  repository: 'helm-hosted',
  format: 'helm',
  group: null,
  name,
  version,
  assets: [
    {
      id: `asset-${name}`,
      path: `/${name}-${version}.tgz`,
      downloadUrl: `/repository/helm-hosted/${name}-${version}.tgz`,
      attributes: {
        helm: { appVersion, description, icon },
      },
    },
  ],
});

const defaultItems = [
  makeRawItem('nginx', '1.2.0', '1.24.0', 'NGINX web server', 'https://example.com/icon.svg'),
  makeRawItem('redis', '17.0.0', '7.2.0'),
];

beforeEach(() => {
  jest.clearAllMocks();
  mockAxios.get.mockResolvedValue({
    data: { items: defaultItems, continuationToken: undefined },
  });
});

describe('useHelmSearch', () => {
  it('initializes with empty state', () => {
    const { result } = renderHook(() => useHelmSearch());

    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.results).toHaveLength(0);
    expect(result.current.hasMore).toBe(false);
    expect(result.current.state.error).toBeUndefined();
  });

  it('search() sets loading, calls API, then populates results', async () => {
    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({ name: 'nginx' });
    });

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    expect(result.current.state.results).toHaveLength(2);
    expect(result.current.state.results[0].name).toBe('nginx');
    expect(mockAxios.get).toHaveBeenCalledTimes(1);
  });

  it('search() with Error instance uses error.message', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Network error'));
    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({});
    });

    await waitFor(() => {
      expect(result.current.state.error).toBe('Network error');
    });
    expect(result.current.state.loading).toBe(false);
  });

  it('search() with non-Error thrown uses fallback string', async () => {
    mockAxios.get.mockRejectedValueOnce('some string error');
    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({});
    });

    await waitFor(() => {
      expect(result.current.state.error).toBe('Search failed');
    });
  });

  it('loadMore() does nothing when no continuationToken', async () => {
    const { result } = renderHook(() => useHelmSearch());

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
    const { result } = renderHook(() => useHelmSearch());

    // Start search without awaiting — hook stays in loading=true
    act(() => { result.current.search({}); });

    // loadMore should be a no-op while loading=true
    await act(async () => { await result.current.loadMore(); });
    expect(mockAxios.get).toHaveBeenCalledTimes(1); // only the search call, not a second loadMore call

    resolveSearch();
  });

  it('loadMore() appends results when continuationToken present', async () => {
    const firstPage = [makeRawItem('nginx', '1.2.0')];
    const secondPage = [makeRawItem('redis', '17.0.0')];

    mockAxios.get
      .mockResolvedValueOnce({ data: { items: firstPage, continuationToken: 'token-1' } })
      .mockResolvedValueOnce({ data: { items: secondPage, continuationToken: undefined } });

    const { result } = renderHook(() => useHelmSearch());

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

  it('loadMore() handles error with Error instance', async () => {
    mockAxios.get
      .mockResolvedValueOnce({ data: { items: defaultItems, continuationToken: 'token-1' } })
      .mockRejectedValueOnce(new Error('Load more failed'));

    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));

    await act(async () => {
      await result.current.loadMore();
    });
    await waitFor(() => expect(result.current.state.error).toBe('Load more failed'));
  });

  it('loadMore() handles non-Error thrown', async () => {
    mockAxios.get
      .mockResolvedValueOnce({ data: { items: defaultItems, continuationToken: 'token-1' } })
      .mockRejectedValueOnce({ code: 500 });

    const { result } = renderHook(() => useHelmSearch());

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
    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({ name: 'nginx' });
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
      data: { items: defaultItems, continuationToken: 'next-page' },
    });
    const { result } = renderHook(() => useHelmSearch());

    expect(result.current.hasMore).toBe(false);

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.hasMore).toBe(true));
  });

  it('aggregation update path: same chart name increments versionsCount and updates latestVersion', async () => {
    const items = [
      makeRawItem('nginx', '1.0.0', '1.20.0', 'Old desc', 'https://icon.svg'),
      makeRawItem('nginx', '2.0.0', '1.24.0', 'New desc'),
    ];
    mockAxios.get.mockResolvedValue({ data: { items, continuationToken: undefined } });

    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(1));

    const chart = result.current.state.results[0];
    expect(chart.versionsCount).toBe(2);
    expect(chart.latestVersion).toBe('2.0.0');
  });

  it('aggregation create path: new chart name creates entry with versionsCount 1', async () => {
    const items = [makeRawItem('prometheus', '25.0.0', '2.48.0', 'Monitoring', 'https://prom.svg')];
    mockAxios.get.mockResolvedValue({ data: { items, continuationToken: undefined } });

    const { result } = renderHook(() => useHelmSearch());

    await act(async () => {
      await result.current.search({});
    });
    await waitFor(() => expect(result.current.state.results).toHaveLength(1));

    const chart = result.current.state.results[0];
    expect(chart.versionsCount).toBe(1);
    expect(chart.name).toBe('prometheus');
    expect(chart.appVersion).toBe('2.48.0');
    expect(chart.description).toBe('Monitoring');
    expect(chart.icon).toBe('https://prom.svg');
  });
});
