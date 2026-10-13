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

/**
 * NEXUS-54498 — Browse repository list: fix filtering UX.
 *
 * Covers the request lifecycle of the server-side repository list hook:
 *   AC2 — a filter change must produce exactly one request (not one per trigger).
 *   AC3 — overlapping filter changes must settle on the latest filter state,
 *         regardless of the order in which the responses arrive.
 *   AC4 — multi-select formats stay comma-joined (server-side OR), unchanged.
 */

import { renderHook, act } from '@testing-library/react';

const mockGet = jest.fn();

jest.mock('axios', () => ({
  get: (...args: unknown[]) => mockGet(...args),
}));

jest.mock('../../../../config/featureFlags', () => ({
  isMockMode: () => false,
}));

import { useRepositoryListServer } from '../useRepositoryListServer';

const page = (names: string[]) => ({
  data: {
    data: names.map((name) => ({ name, format: 'maven2', type: 'hosted', url: `/${name}` })),
    totalCount: names.length,
    totalPages: 1,
  },
});

/** URLs the hook requested, percent-decoding so assertions read like the wire format. */
const requestedUrls = (): string[] => mockGet.mock.calls.map(([url]) => decodeURIComponent(url as string));

/** Let pending promises settle and any timer-based work run. */
const settle = async () => {
  await act(async () => {
    jest.advanceTimersByTime(1000);
    await Promise.resolve();
  });
};

describe('useRepositoryListServer (NEXUS-54498)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    mockGet.mockReset();
    mockGet.mockResolvedValue(page(['maven-central']));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('issues a single request on mount', async () => {
    renderHook(() => useRepositoryListServer({ pageSize: 40 }));
    await settle();

    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(requestedUrls()[0]).toContain('pageSize=40');
  });

  describe('AC2 — one request per applied filter change', () => {
    it('does not duplicate the request when the name filter is applied', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();
      mockGet.mockClear();

      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'abc' });
      });
      await settle();

      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(requestedUrls()[0]).toContain('nameFilter=abc');
    });

    it('does not duplicate the request when a checkbox filter is applied', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();
      mockGet.mockClear();

      await act(async () => {
        result.current.setFilterParams({ formats: 'maven2' });
      });
      await settle();

      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(requestedUrls()[0]).toContain('formats=maven2');
    });

    it('resets to the first page when a filter changes', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();

      await act(async () => {
        result.current.setFilterParams({ page: 3 });
      });
      await settle();
      mockGet.mockClear();

      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'abc' });
      });
      await settle();

      expect(requestedUrls()[0]).toContain('page=1');
      expect(result.current.page).toBe(1);
    });
  });

  describe('AC3 — overlapping filter changes settle on the latest filter state', () => {
    it('keeps the newest response when an older one resolves last', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();
      mockGet.mockClear();

      // Request A: name filter only. Held open so it can resolve *after* request B.
      let resolveA: (value: unknown) => void = () => {};
      mockGet.mockImplementationOnce(() => new Promise((resolve) => { resolveA = resolve; }));
      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'mav' });
      });

      // Request B: name filter + format. Issued while A is still in flight.
      let resolveB: (value: unknown) => void = () => {};
      mockGet.mockImplementationOnce(() => new Promise((resolve) => { resolveB = resolve; }));
      await act(async () => {
        result.current.setFilterParams({ formats: 'maven2' });
      });

      expect(requestedUrls()[1]).toContain('nameFilter=mav');
      expect(requestedUrls()[1]).toContain('formats=maven2');

      // B (newest) comes back first, then the stale A.
      await act(async () => {
        resolveB(page(['maven-central', 'maven-releases']));
        await Promise.resolve();
      });
      await act(async () => {
        resolveA(page(['maven-central', 'maven-releases', 'maven-snapshots', 'npm-proxy']));
        await Promise.resolve();
      });
      await settle();

      // The stale response must not overwrite the newest one, and the count must
      // correspond to the set actually displayed.
      expect(result.current.repositories.map((r) => r.name)).toEqual([
        'maven-central',
        'maven-releases',
      ]);
      expect(result.current.totalCount).toBe(2);
    });

    it('clears loading once the newest response has been applied', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();

      let resolveA: (value: unknown) => void = () => {};
      mockGet.mockImplementationOnce(() => new Promise((resolve) => { resolveA = resolve; }));
      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'mav' });
      });
      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolveA(page(['maven-central']));
        await Promise.resolve();
      });
      await settle();

      expect(result.current.loading).toBe(false);
    });
  });

  describe('AC3 — retry participates in the same latest-request-wins guard', () => {
    it('keeps the retry response when the request it retried resolves last', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();

      // A filter change that will resolve *after* the retry fired.
      let resolveFilterChange: (value: unknown) => void = () => {};
      mockGet.mockReturnValueOnce(new Promise((resolve) => {
        resolveFilterChange = resolve;
      }));
      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'mav' });
      });

      let resolveRetry: (value: unknown) => void = () => {};
      mockGet.mockReturnValueOnce(new Promise((resolve) => {
        resolveRetry = resolve;
      }));
      await act(async () => {
        result.current.retry();
      });

      // Retry resolves first, then the older in-flight request.
      await act(async () => {
        resolveRetry(page(['retry-result']));
        await Promise.resolve();
      });
      await act(async () => {
        resolveFilterChange(page(['stale-result']));
        await Promise.resolve();
      });
      await settle();

      // retry() goes through fetchRepositories, so it claims the newest request id and the
      // late-arriving earlier response is discarded rather than overwriting it.
      expect(result.current.repositories.map((repo) => repo.name)).toEqual(['retry-result']);
      expect(result.current.loading).toBe(false);
    });
  });

  describe('AC1 — request lifecycle is reported separately from result count', () => {
    it('reports hasLoadedOnce only once the first response has settled', async () => {
      let resolveFirst: (value: unknown) => void = () => {};
      mockGet.mockReturnValueOnce(new Promise((resolve) => {
        resolveFirst = resolve;
      }));

      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));

      // In flight: consumers must treat this as a first load, not a refetch.
      expect(result.current.loading).toBe(true);
      expect(result.current.hasLoadedOnce).toBe(false);

      await act(async () => {
        resolveFirst(page(['maven-central']));
        await Promise.resolve();
      });
      await settle();

      expect(result.current.hasLoadedOnce).toBe(true);
    });

    it('keeps hasLoadedOnce true when a later filter matches nothing', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();
      expect(result.current.hasLoadedOnce).toBe(true);

      mockGet.mockResolvedValue(page([]));
      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'zzzznomatch' });
      });
      await settle();

      // An empty result is still a settled result: the next filter change is a refetch, so the
      // table must keep its empty state rather than falling back to first-load skeletons.
      expect(result.current.repositories).toHaveLength(0);
      expect(result.current.hasLoadedOnce).toBe(true);
    });

    it('keeps hasLoadedOnce true when a later request fails', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();

      mockGet.mockRejectedValue(new Error('boom'));
      await act(async () => {
        result.current.setFilterParams({ nameFilter: 'anything' });
      });
      await settle();

      expect(result.current.error).toBeTruthy();
      expect(result.current.hasLoadedOnce).toBe(true);
    });
  });

  describe('AC4 — Format filter stays an OR across selected formats', () => {
    it('sends every selected format comma-joined in a single formats param', async () => {
      const { result } = renderHook(() => useRepositoryListServer({ pageSize: 40 }));
      await settle();
      mockGet.mockClear();

      await act(async () => {
        result.current.setFilterParams({ formats: 'maven2,npm' });
      });
      await settle();

      const url = requestedUrls()[0];
      expect(url).toContain('formats=maven2,npm');
      // A single formats param — not repeated keys, and not narrowed to one format.
      expect(url.match(/formats=/g)).toHaveLength(1);
    });
  });
});
