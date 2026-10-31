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

import {
  useRepositoryType,
  invalidateRepositoryTypeCache,
  REPOSITORY_TYPE_TTL_MS,
  __resetRepositoryTypeCache,
} from '../useRepositoryType';
import { fetchBrowsableRepositories } from '../browse.api';

jest.mock('../browse.api', () => {
  const actual = jest.requireActual('../browse.api');
  return { ...actual, fetchBrowsableRepositories: jest.fn() };
});

const mockFetchRepositories = fetchBrowsableRepositories as jest.MockedFunction<
  typeof fetchBrowsableRepositories
>;

const REPOSITORIES = [
  { id: 'maven-releases', name: 'maven-releases', format: 'maven2', type: 'hosted' },
  { id: 'maven-central', name: 'maven-central', format: 'maven2', type: 'proxy' },
  { id: 'maven-public', name: 'maven-public', format: 'maven2', type: 'group' },
];

describe('useRepositoryType (delete gating)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    __resetRepositoryTypeCache();
    mockFetchRepositories.mockResolvedValue(REPOSITORIES as never);
  });

  it('returns undefined before the repository list has resolved', () => {
    const { result } = renderHook(() => useRepositoryType('maven-releases'));

    expect(result.current).toBeUndefined();
  });

  it.each([
    ['maven-releases', 'hosted'],
    ['maven-central', 'proxy'],
    ['maven-public', 'group'],
  ])('resolves %s to type %s', async (name, type) => {
    const { result } = renderHook(() => useRepositoryType(name));

    await waitFor(() => expect(result.current).toBe(type));
  });

  it('does not fetch when no repository is selected', () => {
    const { result } = renderHook(() => useRepositoryType(undefined));

    expect(result.current).toBeUndefined();
    expect(mockFetchRepositories).not.toHaveBeenCalled();
  });

  it('clears the previous type immediately when the selection changes, so a stale repository never lends its type to the new one', async () => {
    const { result, rerender } = renderHook(({ name }) => useRepositoryType(name), {
      initialProps: { name: 'maven-releases' as string | undefined },
    });
    await waitFor(() => expect(result.current).toBe('hosted'));

    rerender({ name: 'maven-public' });

    // Resolves to the group type; must never be observed as 'hosted' for the group repo.
    await waitFor(() => expect(result.current).toBe('group'));
  });

  it('returns undefined for a repository missing from the list', async () => {
    const { result } = renderHook(() => useRepositoryType('does-not-exist'));

    await waitFor(() => expect(mockFetchRepositories).toHaveBeenCalled());
    expect(result.current).toBeUndefined();
  });

  it('returns undefined when the repository list fails to load', async () => {
    mockFetchRepositories.mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useRepositoryType('maven-releases'));

    await waitFor(() => expect(mockFetchRepositories).toHaveBeenCalled());
    expect(result.current).toBeUndefined();
  });

  it('caches the repository list across hook instances', async () => {
    const first = renderHook(() => useRepositoryType('maven-releases'));
    await waitFor(() => expect(first.result.current).toBe('hosted'));

    const second = renderHook(() => useRepositoryType('maven-central'));
    await waitFor(() => expect(second.result.current).toBe('proxy'));

    expect(mockFetchRepositories).toHaveBeenCalledTimes(1);
  });

  /**
   * The repository list is session-scoped configuration, not an
   * immutable fact. A repository created, removed, or retyped while the Browse
   * page is open must change the Delete gating without an application reload,
   * so a successful response must never be retained as permanently
   * authoritative.
   */
  describe('cache invalidation', () => {
    let now: number;
    let nowSpy: jest.SpyInstance<number, []>;

    beforeEach(() => {
      now = 1_700_000_000_000;
      nowSpy = jest.spyOn(Date, 'now').mockImplementation(() => now);
    });

    afterEach(() => {
      nowSpy.mockRestore();
    });

    it('resolves a repository created after the list was first cached', async () => {
      const first = renderHook(() => useRepositoryType('raw-hosted'));
      await waitFor(() => expect(mockFetchRepositories).toHaveBeenCalled());
      expect(first.result.current).toBeUndefined();

      mockFetchRepositories.mockResolvedValue([
        ...REPOSITORIES,
        { id: 'raw-hosted', name: 'raw-hosted', format: 'raw', type: 'hosted' },
      ] as never);

      const second = renderHook(() => useRepositoryType('raw-hosted'));

      await waitFor(() => expect(second.result.current).toBe('hosted'));
    });

    it('picks up a type change once the cached list has aged out', async () => {
      const first = renderHook(() => useRepositoryType('maven-releases'));
      await waitFor(() => expect(first.result.current).toBe('hosted'));

      mockFetchRepositories.mockResolvedValue(
        REPOSITORIES.map((r) =>
          r.name === 'maven-releases' ? { ...r, type: 'group' } : r,
        ) as never,
      );

      // Still fresh: no refetch, so no observable change yet.
      const stillFresh = renderHook(() => useRepositoryType('maven-releases'));
      await waitFor(() => expect(stillFresh.result.current).toBe('hosted'));
      expect(mockFetchRepositories).toHaveBeenCalledTimes(1);

      now += REPOSITORY_TYPE_TTL_MS + 1;

      const afterTtl = renderHook(() => useRepositoryType('maven-releases'));
      await waitFor(() => expect(afterTtl.result.current).toBe('group'));
    });

    it('stops resolving a repository that was removed, once the list has aged out', async () => {
      const first = renderHook(() => useRepositoryType('maven-central'));
      await waitFor(() => expect(first.result.current).toBe('proxy'));

      mockFetchRepositories.mockResolvedValue(
        REPOSITORIES.filter((r) => r.name !== 'maven-central') as never,
      );
      now += REPOSITORY_TYPE_TTL_MS + 1;

      const afterTtl = renderHook(() => useRepositoryType('maven-central'));
      await waitFor(() => expect(mockFetchRepositories).toHaveBeenCalledTimes(2));
      expect(afterTtl.result.current).toBeUndefined();
    });

    it('refetches immediately when the cache is explicitly invalidated', async () => {
      const first = renderHook(() => useRepositoryType('maven-releases'));
      await waitFor(() => expect(first.result.current).toBe('hosted'));

      mockFetchRepositories.mockResolvedValue(
        REPOSITORIES.map((r) =>
          r.name === 'maven-releases' ? { ...r, type: 'proxy' } : r,
        ) as never,
      );
      invalidateRepositoryTypeCache();

      const second = renderHook(() => useRepositoryType('maven-releases'));

      await waitFor(() => expect(second.result.current).toBe('proxy'));
    });

    it('does not let a request that started before an invalidation become the cache', async () => {
      // The invalidation exists so an explicit refresh re-reads the type. A
      // response already in flight when it happens was read from the server
      // *before* the change, so republishing it would restore the stale type and
      // give it a fresh timestamp — silently defeating the refresh for a full TTL.
      let releaseFirst: (value: unknown) => void = () => {};
      mockFetchRepositories.mockReturnValueOnce(
        new Promise((resolve) => {
          releaseFirst = resolve;
        }) as never,
      );

      const first = renderHook(() => useRepositoryType('maven-releases'));
      await waitFor(() => expect(mockFetchRepositories).toHaveBeenCalledTimes(1));

      invalidateRepositoryTypeCache();
      await act(async () => {
        releaseFirst(REPOSITORIES);
      });

      mockFetchRepositories.mockResolvedValue(
        REPOSITORIES.map((r) =>
          r.name === 'maven-releases' ? { ...r, type: 'proxy' } : r,
        ) as never,
      );

      const second = renderHook(() => useRepositoryType('maven-releases'));

      await waitFor(() => expect(second.result.current).toBe('proxy'));
      expect(mockFetchRepositories).toHaveBeenCalledTimes(2);
      // The in-flight response still answered its own caller; it just never
      // became the shared snapshot.
      expect(first.result.current).toBe('hosted');
    });

    it('shares a single in-flight request between concurrent hooks', async () => {
      const a = renderHook(() => useRepositoryType('maven-releases'));
      const b = renderHook(() => useRepositoryType('maven-central'));

      await waitFor(() => expect(a.result.current).toBe('hosted'));
      await waitFor(() => expect(b.result.current).toBe('proxy'));
      expect(mockFetchRepositories).toHaveBeenCalledTimes(1);
    });
  });
});
