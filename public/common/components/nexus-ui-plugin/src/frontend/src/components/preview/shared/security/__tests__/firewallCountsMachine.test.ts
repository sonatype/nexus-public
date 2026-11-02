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
import { FIREWALL_COUNTS_CHUNK_SIZE, firewallCountsMachine } from '../firewallCountsMachine';
import type { SecurityStatusData } from '../security.types';

const mockGet = jest.fn();
jest.mock('../../../../../interface/api', () => ({
  restClient: { get: (...args: unknown[]) => mockGet(...args) },
  ENDPOINTS: { FIREWALL_STATUS_REPO: (name: string) => `/repo-status/${name}` },
}));

const countsFor = (name: string): SecurityStatusData => ({
  repositoryName: name,
  affectedComponentCount: 1,
  criticalComponentCount: 1,
  severeComponentCount: 0,
  moderateComponentCount: 0,
  quarantinedComponentCount: 0,
});

const nameOf = (url: string) => url.split('/').pop() as string;
const flush = () => new Promise((resolve) => setTimeout(resolve, 0));
const names = (count: number) => Array.from({ length: count }, (_, i) => `repo-${i}`);
const BAD_REPO = 'repo-bad';

function start() {
  return interpret(firewallCountsMachine).start();
}

describe('firewallCountsMachine failure matrix', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockGet.mockImplementation((url: string) => Promise.resolve(countsFor(nameOf(url))));
  });

  it('shouldFetchOnlyTargetedRepos', async () => {
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a', 'b'] });
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(2);
    expect(Object.keys(service.state.context.counts).sort()).toEqual(['a', 'b']);
    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('shouldRequestAtMostChunkSizeRepoCallsAtOnce', async () => {
    let inFlight = 0;
    let maxInFlight = 0;
    mockGet.mockImplementation((url: string) => {
      inFlight++;
      maxInFlight = Math.max(maxInFlight, inFlight);
      return new Promise((resolve) => setTimeout(() => {
        inFlight--;
        resolve(countsFor(nameOf(url)));
      }, 5));
    });
    const service = start();
    service.send({ type: 'SET_TARGETS', names: names(10) });
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(maxInFlight).toBe(FIREWALL_COUNTS_CHUNK_SIZE);
    expect(Object.keys(service.state.context.counts)).toHaveLength(10);
    service.stop();
  });

  it('shouldKeepCountsForOtherReposWhenOneRepoFails', async () => {
    mockGet.mockImplementation((url: string) =>
      nameOf(url) === BAD_REPO ? Promise.reject(new Error('boom')) : Promise.resolve(countsFor(nameOf(url))));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a', BAD_REPO, 'b', 'c', 'd', 'e'] });
    await flush();
    await flush();
    expect(service.state.context.failures).toEqual([BAD_REPO]);
    expect(Object.keys(service.state.context.counts).sort()).toEqual(['a', 'b', 'c', 'd', 'e']);
    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('shouldTreatResponseWithErrorMessageAsThatReposFailure', async () => {
    mockGet.mockImplementation((url: string) =>
      Promise.resolve(nameOf(url) === BAD_REPO ? { ...countsFor(BAD_REPO), errorMessage: 'IQ unreachable' } : countsFor(nameOf(url))));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a', BAD_REPO] });
    await flush();
    expect(service.state.context.failures).toEqual([BAD_REPO]);
    expect(service.state.context.counts.a).toBeDefined();
    service.stop();
  });

  it('shouldHaltWhenAnEntireChunkFails', async () => {
    mockGet.mockRejectedValue(new Error('IQ down'));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: names(10) });
    await flush();
    await flush();
    expect(service.state.matches('halted')).toBe(true);
    expect(mockGet).toHaveBeenCalledTimes(FIREWALL_COUNTS_CHUNK_SIZE);
    service.stop();
  });

  it('shouldNotHaltWhenOnlyASingleRepoIsRequestedAndFails', async () => {
    mockGet.mockRejectedValue(new Error('boom'));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a'] });
    await flush();
    await flush();
    expect(service.state.matches('idle')).toBe(true);
    expect(service.state.context.failures).toEqual(['a']);
    service.stop();
  });

  it('shouldFetchNewPageAfterOneRepoFailedOnPreviousPage', async () => {
    mockGet.mockImplementation((url: string) =>
      nameOf(url) === BAD_REPO ? Promise.reject(new Error('boom')) : Promise.resolve(countsFor(nameOf(url))));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a', BAD_REPO] });
    await flush();
    await flush();
    mockGet.mockClear();
    service.send({ type: 'SET_TARGETS', names: ['x', 'y'] });
    await flush();
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(2);
    expect(service.state.context.counts.x).toBeDefined();
    expect(service.state.context.counts.y).toBeDefined();
    service.stop();
  });

  it('shouldNotRefetchLoadedOrFailedReposWhenTargetsRepeat', async () => {
    mockGet.mockImplementation((url: string) =>
      nameOf(url) === BAD_REPO ? Promise.reject(new Error('boom')) : Promise.resolve(countsFor(nameOf(url))));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a', BAD_REPO] });
    await flush();
    await flush();
    mockGet.mockClear();
    service.send({ type: 'SET_TARGETS', names: ['a', BAD_REPO] });
    service.send({ type: 'SET_TARGETS', names: ['a', BAD_REPO, 'c'] });
    await flush();
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(mockGet).toHaveBeenCalledWith('/repo-status/c');
    service.stop();
  });

  it('shouldResumeAfterHaltOnRefresh', async () => {
    mockGet.mockRejectedValue(new Error('IQ down'));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a', 'b'] });
    await flush();
    await flush();
    expect(service.state.matches('halted')).toBe(true);
    mockGet.mockImplementation((url: string) => Promise.resolve(countsFor(nameOf(url))));
    service.send({ type: 'REFRESH' });
    await flush();
    await flush();
    expect(service.state.matches('idle')).toBe(true);
    expect(service.state.context.failures).toEqual([]);
    expect(Object.keys(service.state.context.counts).sort()).toEqual(['a', 'b']);
    service.stop();
  });

  it('shouldRetryFailedRepoOnRefresh', async () => {
    mockGet.mockRejectedValueOnce(new Error('boom'));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a'] });
    await flush();
    await flush();
    expect(service.state.context.failures).toEqual(['a']);
    service.send({ type: 'REFRESH' });
    await flush();
    await flush();
    expect(service.state.context.counts.a).toBeDefined();
    expect(service.state.context.failures).toEqual([]);
    service.stop();
  });

  it('shouldIgnoreResultsAfterTargetsChangeAwayMidFetch', async () => {
    const resolvers: Array<() => void> = [];
    mockGet.mockImplementation((url: string) => new Promise((resolve) => {
      resolvers.push(() => resolve(countsFor(nameOf(url))));
    }));
    const service = start();
    service.send({ type: 'SET_TARGETS', names: ['a'] });
    service.send({ type: 'SET_TARGETS', names: [] });
    resolvers.forEach((resolve) => resolve());
    await flush();
    await flush();
    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });
});
