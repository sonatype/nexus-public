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

import { interpret } from 'xstate';

import { cacheKeyOf, createComponentVersionsMachine } from '../componentVersionsMachine';
import type { ComponentVersionsPage } from '../../core/search.types';

const page = (overrides: Partial<ComponentVersionsPage> = {}): ComponentVersionsPage => ({
  items: [{ version: '1.0.0', lastUpdated: '2026-01-01T00:00:00Z', repositories: ['releases'] }],
  total: 100,
  page: 0,
  size: 20,
  ...overrides,
});

const flushMicrotasks = async (iterations = 20) => {
  for (let i = 0; i < iterations; i++) {
    await Promise.resolve();
  }
};

const start = (loadPage: jest.Mock) => {
  const service = interpret(
    createComponentVersionsMachine('maven2:org.test:artifact').withConfig({
      services: { loadPage },
    }),
  );
  service.start();
  return service;
};

describe('componentVersionsMachine', () => {
  let service: ReturnType<typeof start> | undefined;

  afterEach(() => {
    if (service) {
      try {
        service.stop();
      } catch {
        // ignore
      }
      service = undefined;
    }
  });

  it('loads the first page eagerly and records the total', async () => {
    const loadPage = jest.fn().mockResolvedValue(page());
    service = start(loadPage);
    await flushMicrotasks();

    expect(loadPage).toHaveBeenCalledTimes(1);
    const ctx = service.getSnapshot().context;
    expect(ctx.cache[cacheKeyOf(ctx)].total).toBe(100);
    expect(ctx.cache[cacheKeyOf(ctx)].pages[0]).toHaveLength(1);
    expect(service.getSnapshot().matches('idle')).toBe(true);
  });

  it('does not refetch a page already in the cache', async () => {
    const loadPage = jest.fn().mockImplementation((ctx: { page: number }) =>
      Promise.resolve(page({ page: ctx.page })),
    );
    service = start(loadPage);
    await flushMicrotasks();
    expect(loadPage).toHaveBeenCalledTimes(1);

    service.send({ type: 'SET_PAGE', page: 1 });
    await flushMicrotasks();
    expect(loadPage).toHaveBeenCalledTimes(2);

    service.send({ type: 'SET_PAGE', page: 0 });
    await flushMicrotasks();
    expect(loadPage).toHaveBeenCalledTimes(2); // page 0 was already cached
    expect(service.getSnapshot().context.page).toBe(0);
  });

  it('starts a new cache entry when sort changes, dropping the previous one', async () => {
    const loadPage = jest.fn().mockResolvedValue(page());
    service = start(loadPage);
    await flushMicrotasks();
    const firstKey = cacheKeyOf(service.getSnapshot().context);

    service.send({ type: 'SET_SORT', sort: 'lastUpdated', direction: 'asc' });
    await flushMicrotasks();

    const ctx = service.getSnapshot().context;
    const secondKey = cacheKeyOf(ctx);
    expect(secondKey).not.toBe(firstKey);
    expect(loadPage).toHaveBeenCalledTimes(2);
    expect(ctx.cache[firstKey]).toBeUndefined();
    expect(ctx.cache[secondKey]).toBeDefined();
  });

  it('resets to page 0 and drops the cache when the version filter changes', async () => {
    const loadPage = jest.fn().mockImplementation((ctx: { page: number }) =>
      Promise.resolve(page({ page: ctx.page })),
    );
    service = start(loadPage);
    await flushMicrotasks();

    service.send({ type: 'SET_PAGE', page: 3 });
    await flushMicrotasks();
    expect(service.getSnapshot().context.page).toBe(3);

    service.send({ type: 'SET_VERSION_FILTER', versionFilter: '2.1' });
    await flushMicrotasks();

    const ctx = service.getSnapshot().context;
    expect(ctx.page).toBe(0);
    expect(ctx.versionFilter).toBe('2.1');
    expect(Object.keys(ctx.cache)).toHaveLength(1);
  });

  it('resets to page 0 and drops the cache when size changes', async () => {
    const loadPage = jest.fn().mockResolvedValue(page());
    service = start(loadPage);
    await flushMicrotasks();

    service.send({ type: 'SET_PAGE', page: 2 });
    await flushMicrotasks();
    service.send({ type: 'SET_SIZE', size: 50 });
    await flushMicrotasks();

    const ctx = service.getSnapshot().context;
    expect(ctx.page).toBe(0);
    expect(ctx.size).toBe(50);
  });

  it('ignores SET_PAGE for the page already in context, even mid-flight', async () => {
    // TablePagination dispatches SET_SIZE and SET_PAGE(page 0) from one synchronous handler,
    // so this pair arrives while the SET_SIZE fetch is still in flight and the cache — which
    // pageIsCached consults — has just been dropped. Only pageIsUnchanged can suppress it.
    const loadPage = jest.fn().mockImplementation((ctx: { page: number }) =>
      Promise.resolve(page({ page: ctx.page })),
    );
    service = start(loadPage);
    await flushMicrotasks();

    service.send({ type: 'SET_PAGE', page: 2 });
    await flushMicrotasks();
    expect(loadPage).toHaveBeenCalledTimes(2);

    service.send({ type: 'SET_SIZE', size: 50 });
    service.send({ type: 'SET_PAGE', page: 0 });
    await flushMicrotasks();

    expect(loadPage).toHaveBeenCalledTimes(3);
    expect(loadPage.mock.calls[2][0].size).toBe(50);
    expect(loadPage.mock.calls[2][0].page).toBe(0);
    expect(service.getSnapshot().matches('idle')).toBe(true);
  });

  it('surfaces an error and recovers on RETRY', async () => {
    const loadPage = jest
      .fn()
      .mockRejectedValueOnce(new Error('boom'))
      .mockResolvedValue(page());
    service = start(loadPage);
    await flushMicrotasks();

    expect(service.getSnapshot().context.error).toBeTruthy();
    expect(service.getSnapshot().matches('failed')).toBe(true);

    service.send({ type: 'RETRY' });
    await flushMicrotasks();

    const ctx = service.getSnapshot().context;
    expect(ctx.error).toBeNull();
    expect(ctx.cache[cacheKeyOf(ctx)].total).toBe(100);
    expect(service.getSnapshot().matches('idle')).toBe(true);
  });

  /**
   * newestVersion and totalVersions feed the page header, the copy-path target, the Overview
   * tab's default version, and the Versions tab badge — none of which may follow the user's
   * interaction with the versions table. Each test below drives one such interaction and asserts
   * the latched values survive it while the page cache legitimately moves underneath.
   */
  describe('newestVersion and totalVersions', () => {
    const version = (v: string) => ({
      version: v,
      lastUpdated: '2026-01-01T00:00:00Z',
      repositories: ['releases'],
    });

    it('latches both from the eager, default-ordered first page', async () => {
      const loadPage = jest.fn().mockResolvedValue(page({ items: [version('9.0.0')], total: 100 }));
      service = start(loadPage);
      await flushMicrotasks();

      const ctx = service.getSnapshot().context;
      expect(ctx.newestVersion).toBe('9.0.0');
      expect(ctx.totalVersions).toBe(100);
    });

    it('keeps the newest version when the user sorts ascending', async () => {
      const loadPage = jest
        .fn()
        .mockResolvedValueOnce(page({ items: [version('9.0.0')] }))
        .mockResolvedValue(page({ items: [version('0.1.0')] }));
      service = start(loadPage);
      await flushMicrotasks();

      service.send({ type: 'SET_SORT', sort: 'version', direction: 'asc' });
      await flushMicrotasks();

      const ctx = service.getSnapshot().context;
      // The visible page now starts at the oldest version — which is exactly why the header
      // cannot read it.
      expect(ctx.cache[cacheKeyOf(ctx)].pages[0][0].version).toBe('0.1.0');
      expect(ctx.newestVersion).toBe('9.0.0');
    });

    it('keeps the newest version when the user pages forward', async () => {
      const loadPage = jest.fn().mockImplementation((ctx: { page: number }) =>
        Promise.resolve(page({ items: [version(ctx.page === 0 ? '9.0.0' : '4.0.0')], page: ctx.page })),
      );
      service = start(loadPage);
      await flushMicrotasks();

      service.send({ type: 'SET_PAGE', page: 1 });
      await flushMicrotasks();

      const ctx = service.getSnapshot().context;
      expect(ctx.cache[cacheKeyOf(ctx)].pages[1][0].version).toBe('4.0.0');
      expect(ctx.newestVersion).toBe('9.0.0');
    });

    it('keeps both when a version filter narrows the list', async () => {
      const loadPage = jest
        .fn()
        .mockResolvedValueOnce(page({ items: [version('9.0.0')], total: 100 }))
        .mockResolvedValue(page({ items: [version('2.1.4')], total: 3 }));
      service = start(loadPage);
      await flushMicrotasks();

      service.send({ type: 'SET_VERSION_FILTER', versionFilter: '2.1' });
      await flushMicrotasks();

      const ctx = service.getSnapshot().context;
      // The tab's own total is the match count; the badge's is not.
      expect(ctx.cache[cacheKeyOf(ctx)].total).toBe(3);
      expect(ctx.totalVersions).toBe(100);
      expect(ctx.newestVersion).toBe('9.0.0');
    });

    it('re-latches on a page-size change, which re-fetches page 0 of the same ordering', async () => {
      const loadPage = jest
        .fn()
        .mockResolvedValueOnce(page({ items: [version('9.0.0')], total: 100 }))
        .mockResolvedValue(page({ items: [version('9.1.0')], total: 101, size: 50 }));
      service = start(loadPage);
      await flushMicrotasks();

      service.send({ type: 'SET_SIZE', size: 50 });
      await flushMicrotasks();

      const ctx = service.getSnapshot().context;
      expect(ctx.newestVersion).toBe('9.1.0');
      expect(ctx.totalVersions).toBe(101);
    });

    it('leaves the newest version null when the component has no versions', async () => {
      const loadPage = jest.fn().mockResolvedValue(page({ items: [], total: 0 }));
      service = start(loadPage);
      await flushMicrotasks();

      expect(service.getSnapshot().context.newestVersion).toBeNull();
    });
  });

  it('cacheKeyOf ignores the selected version entirely (it is not part of context)', () => {
    const base = {
      gaId: 'maven2:org.test:artifact',
      sort: 'version' as const,
      direction: 'desc' as const,
      size: 20,
      versionFilter: '',
    };
    expect(cacheKeyOf(base)).toBe('maven2:org.test:artifact|version|desc|20|');
  });
});
