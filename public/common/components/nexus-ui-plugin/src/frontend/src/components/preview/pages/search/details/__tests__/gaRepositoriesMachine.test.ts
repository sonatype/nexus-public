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
import { createGaRepositoriesMachine, type VersionResult } from '../gaRepositoriesMachine';

const RESULT_V1: VersionResult = {
  items: [{ repositoryName: 'r-a', type: 'hosted', versionCount: 3 }],
  totalCount: 1,
};

const RESULT_V2: VersionResult = {
  items: [{ repositoryName: 'r-b', type: 'proxy', versionCount: 5 }],
  totalCount: 1,
};

/**
 * Deferred-promise helper: the service resolves only when we call `resolve`,
 * so tests can observe the loading state deterministically.
 */
function deferred<T>() {
  let resolve!: (v: T) => void;
  let reject!: (e: Error) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('gaRepositoriesMachine', () => {
  it('starts idle with empty cache and no currentResult', () => {
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: { fetchForVersion: () => Promise.resolve(RESULT_V1) },
    })).start();

    expect(service.getSnapshot().value).toBe('idle');
    expect(service.getSnapshot().context.currentResult).toBeNull();
    expect(service.getSnapshot().context.cache.size).toBe(0);
    service.stop();
  });

  it('SELECT_VERSION(null) stays idle and clears currentResult without fetching', () => {
    const fetchSpy = jest.fn().mockResolvedValue(RESULT_V1);
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: { fetchForVersion: fetchSpy },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: null });

    expect(service.getSnapshot().value).toBe('idle');
    expect(service.getSnapshot().context.currentResult).toBeNull();
    expect(fetchSpy).not.toHaveBeenCalled();
    service.stop();
  });

  it('SELECT_VERSION(uncached) transitions to loading, then idle with cached result on success', async () => {
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: { fetchForVersion: () => Promise.resolve(RESULT_V1) },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    await new Promise((r) => setTimeout(r, 0));    // let promise resolve

    const snap = service.getSnapshot();
    expect(snap.value).toBe('idle');
    expect(snap.context.currentResult).toEqual(RESULT_V1);
    expect(snap.context.cache.get('1.0')).toEqual(RESULT_V1);
    service.stop();
  });

  it('SELECT_VERSION(cached) hydrates synchronously without a fetch', async () => {
    const fetchSpy = jest.fn().mockResolvedValue(RESULT_V1);
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: { fetchForVersion: fetchSpy },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    await new Promise((r) => setTimeout(r, 0));    // first fetch resolves

    fetchSpy.mockClear();
    service.send({ type: 'SELECT_VERSION', version: '1.0' });

    expect(service.getSnapshot().value).toBe('idle');
    expect(service.getSnapshot().context.currentResult).toEqual(RESULT_V1);
    expect(fetchSpy).not.toHaveBeenCalled();
    service.stop();
  });

  it('fetch error transitions to error; SELECT_VERSION retries', async () => {
    let attempts = 0;
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: {
        fetchForVersion: () => {
          attempts++;
          return attempts === 1
            ? Promise.reject(new Error('boom'))
            : Promise.resolve(RESULT_V1);
        },
      },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    await new Promise((r) => setTimeout(r, 0));
    expect(service.getSnapshot().value).toBe('error');
    expect(service.getSnapshot().context.error).toContain('boom');

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    await new Promise((r) => setTimeout(r, 0));
    expect(service.getSnapshot().value).toBe('idle');
    expect(service.getSnapshot().context.currentResult).toEqual(RESULT_V1);
    service.stop();
  });

  it('GA_CHANGED clears cache but keeps selectedVersion', async () => {
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: { fetchForVersion: () => Promise.resolve(RESULT_V1) },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    await new Promise((r) => setTimeout(r, 0));
    expect(service.getSnapshot().context.cache.size).toBe(1);

    service.send({ type: 'GA_CHANGED', gaId: 'ga-2' });

    expect(service.getSnapshot().context.cache.size).toBe(0);
    expect(service.getSnapshot().context.currentResult).toBeNull();
    expect(service.getSnapshot().context.selectedVersion).toBe('1.0');
    expect(service.getSnapshot().context.gaId).toBe('ga-2');
    service.stop();
  });

  it('REFRESH invalidates only the current version cache entry', async () => {
    let call = 0;
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: {
        fetchForVersion: (ctx) => {
          call++;
          return Promise.resolve(ctx.selectedVersion === '1.0'
            ? (call === 1 ? RESULT_V1 : RESULT_V2)
            : RESULT_V2);
        },
      },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    await new Promise((r) => setTimeout(r, 0));
    service.send({ type: 'SELECT_VERSION', version: '2.0' });
    await new Promise((r) => setTimeout(r, 0));
    // Two versions cached
    expect(service.getSnapshot().context.cache.size).toBe(2);

    // REFRESH while '2.0' is current — clears only '2.0'
    service.send({ type: 'REFRESH' });
    await new Promise((r) => setTimeout(r, 0));
    expect(service.getSnapshot().context.cache.has('1.0')).toBe(true);
    service.stop();
  });

  it('a second SELECT_VERSION while loading supersedes the pending fetch', async () => {
    const first = deferred<VersionResult>();
    const second = deferred<VersionResult>();
    let call = 0;
    const service = interpret(createGaRepositoriesMachine('ga-1').withConfig({
      services: {
        fetchForVersion: () => (++call === 1 ? first.promise : second.promise),
      },
    })).start();

    service.send({ type: 'SELECT_VERSION', version: '1.0' });
    service.send({ type: 'SELECT_VERSION', version: '2.0' });

    second.resolve(RESULT_V2);
    await new Promise((r) => setTimeout(r, 0));

    // Result for the second version is what ends up in currentResult.
    expect(service.getSnapshot().context.currentResult).toEqual(RESULT_V2);
    // First promise resolving later must not overwrite.
    first.resolve(RESULT_V1);
    await new Promise((r) => setTimeout(r, 0));
    expect(service.getSnapshot().context.currentResult).toEqual(RESULT_V2);
    service.stop();
  });
});
