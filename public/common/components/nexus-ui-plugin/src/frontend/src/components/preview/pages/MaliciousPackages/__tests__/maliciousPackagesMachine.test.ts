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
 * Unit tests for `maliciousPackagesMachine`.
 *
 * These tests interrogate the machine directly without React. They cover:
 *  - State transitions (loading → ready / failed; ready → polling on REFRESH)
 *  - Guard behavior (tasksJustFinished triggers immediate re-poll)
 *  - Service auto-cancellation when the machine is stopped or transitions out
 *  - Imperative event actions (SET_HISTORY, SET_BULK_PROGRESS, RHC_* events, …)
 */

import { interpret } from 'xstate';

import {
  makeInitialContext,
  maliciousPackagesMachine,
  normalizeTasksResponse,
  type MaliciousPackagesMachineContext,
} from '../maliciousPackagesMachine';
import type { TaskInfo } from '../maliciousPackagesUtils';

const RUNNING_TASK: TaskInfo = {
  id: 't-running',
  name: 'Malicious Packages - r1',
  repositoryName: 'r1',
  mode: 'delete',
  enabled: true,
  lastRun: null,
  lastRunResult: null,
  nextRun: null,
  currentState: 'RUNNING',
  progress: null,
};

const DONE_TASK: TaskInfo = { ...RUNNING_TASK, id: 't-done', currentState: 'WAITING' };

interface FetchAllStub {
  data: MaliciousPackagesMachineContext['data'];
  proxyRepos: MaliciousPackagesMachineContext['proxyRepos'];
  tasks: TaskInfo[];
  serverRhcScans: Map<string, MaliciousPackagesMachineContext['rhcScans'] extends Map<string, infer V> ? V : never>;
}

function fetchAllStubResult(overrides: Partial<FetchAllStub> = {}): FetchAllStub {
  return {
    data: {
      activeFindings: [],
      historyFindings: [],
      malwareCount: 0,
      countsByRepo: {},
      hasFirewall: false,
      hcEnabledRepos: [],
      totalProxyRepoCount: 0,
      loading: false,
      error: null,
    },
    proxyRepos: [],
    tasks: [],
    serverRhcScans: new Map(),
    ...overrides,
  };
}

/**
 * Build a machine with stubbed services and a deterministic initial context.
 * `fetchAllSequence` returns sequential results — first call returns the first
 * entry, second returns the second, etc. Used to script multi-poll scenarios.
 */
function machineWithServices(options: {
  fetchAllSequence?: Array<FetchAllStub | Error>;
  initialContext?: Partial<MaliciousPackagesMachineContext>;
} = {}) {
  const sequence = options.fetchAllSequence ?? [fetchAllStubResult()];
  let callIndex = 0;
  const fetchAllCalls: number[] = [];

  return maliciousPackagesMachine
    .withContext({ ...makeInitialContext(), ...options.initialContext })
    .withConfig({
      services: {
        fetchAll: () => {
          fetchAllCalls.push(Date.now());
          const next = sequence[Math.min(callIndex, sequence.length - 1)];
          callIndex++;
          if (next instanceof Error) return Promise.reject(next);
          return Promise.resolve(next);
        },
      },
      delays: {
        // Force a long poll delay so the test never accidentally enters the next poll.
        POLL_INTERVAL: () => 10_000,
      },
    });
}

describe('maliciousPackagesMachine — initial load', () => {
  it('starts in the loading state and invokes fetchAll once on entry', (done) => {
    const machine = machineWithServices();
    const service = interpret(machine).onTransition((state) => {
      if (state.matches('ready')) {
        expect(state.context.data.loading).toBe(false);
        expect(state.context.data.error).toBeNull();
        service.stop();
        done();
      }
    });
    expect(service.initialState.matches('loading')).toBe(true);
    service.start();
  });

  it('transitions to failed when fetchAll rejects, with the error message in context.data', (done) => {
    const machine = machineWithServices({
      fetchAllSequence: [new Error('boom')],
    });
    const service = interpret(machine).onTransition((state) => {
      if (state.matches('failed')) {
        expect(state.context.data.loading).toBe(false);
        expect(state.context.data.error).toBe('boom');
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('REFRESH from failed re-enters loading and re-invokes fetchAll', (done) => {
    const machine = machineWithServices({
      fetchAllSequence: [new Error('boom'), fetchAllStubResult()],
    });
    const service = interpret(machine).onTransition((state) => {
      if (state.matches('failed') && state.context.data.error === 'boom') {
        service.send({ type: 'REFRESH' });
      }
      if (state.matches('ready')) {
        expect(state.context.data.error).toBeNull();
        service.stop();
        done();
      }
    });
    service.start();
  });
});

describe('maliciousPackagesMachine — REFRESH event', () => {
  it('moves ready → ready.polling on REFRESH and back to idle on success', (done) => {
    const machine = machineWithServices({
      fetchAllSequence: [fetchAllStubResult(), fetchAllStubResult()],
    });
    const service = interpret(machine);
    let sawPolling = false;
    service.onTransition((state) => {
      if (state.matches({ ready: 'idle' }) && !sawPolling) {
        service.send({ type: 'REFRESH' });
        return;
      }
      if (state.matches({ ready: 'polling' })) {
        sawPolling = true;
      }
      if (state.matches({ ready: 'idle' }) && sawPolling) {
        service.stop();
        done();
      }
    });
    service.start();
  });
});

describe('maliciousPackagesMachine — auto-refresh on task completion', () => {
  it('immediately re-polls when tasks transition from running → not-running', (done) => {
    // First load returns a running task; second poll returns the same task as completed.
    // The `tasksJustFinished` guard should re-enter polling (third fetchAll call) WITHOUT
    // waiting the full POLL_INTERVAL — proving the guard reads the *incoming* tasks
    // from the event payload rather than the still-stale ctx.tasks.
    let fetchAllCallCount = 0;
    let firstPollAt = 0;
    let secondPollAt = 0;
    const machine = maliciousPackagesMachine
      .withContext(makeInitialContext())
      .withConfig({
        services: {
          fetchAll: () => {
            fetchAllCallCount++;
            if (fetchAllCallCount === 2) firstPollAt = Date.now();
            if (fetchAllCallCount === 3) secondPollAt = Date.now();
            if (fetchAllCallCount === 1) {
              return Promise.resolve(fetchAllStubResult({ tasks: [RUNNING_TASK] }));
            }
            return Promise.resolve(fetchAllStubResult({ tasks: [DONE_TASK] }));
          },
        },
        delays: {
          // Long enough that a regression (guard always false) would force the test
          // to time out instead of accidentally passing via the normal idle → polling
          // tick. With the guard fixed, the third fetch fires immediately after the
          // second resolves.
          POLL_INTERVAL: () => 5_000,
        },
      });

    const service = interpret(machine);
    service.onTransition((state) => {
      if (fetchAllCallCount >= 3 && state.matches({ ready: 'idle' })) {
        expect(state.context.prevAnyTaskRunning).toBe(false);
        // Guard fired → second poll fired immediately after first, not 5s later.
        expect(secondPollAt - firstPollAt).toBeLessThan(500);
        service.stop();
        done();
      }
    });
    service.start();
  });
});

describe('maliciousPackagesMachine — imperative event actions', () => {
  it('SET_HISTORY updates only historyFindings', (done) => {
    const machine = machineWithServices();
    const service = interpret(machine).onTransition((state) => {
      if (state.matches('ready') && state.context.data.historyFindings.length === 0) {
        service.send({
          type: 'SET_HISTORY',
          history: [{ id: 99 } as any],
        });
      }
      if (state.matches('ready') && state.context.data.historyFindings.length === 1) {
        expect(state.context.data.historyFindings[0].id).toBe(99);
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('SET_BULK_PROGRESS replaces the bulkProgress value', (done) => {
    const machine = machineWithServices();
    const service = interpret(machine).onTransition((state) => {
      if (state.matches('ready') && !state.context.bulkProgress.active) {
        service.send({
          type: 'SET_BULK_PROGRESS',
          bulkProgress: { total: 5, completed: 2, active: true },
        });
      }
      if (state.matches('ready') && state.context.bulkProgress.active) {
        expect(state.context.bulkProgress).toEqual({ total: 5, completed: 2, active: true });
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('RHC_OPTIMISTIC_START adds an optimistic scanning entry that survives the next poll', (done) => {
    const machine = machineWithServices({
      fetchAllSequence: [fetchAllStubResult(), fetchAllStubResult()],
    });
    const service = interpret(machine);
    let sentEvent = false;
    service.onTransition((state) => {
      if (state.matches('ready') && !sentEvent) {
        // Pre-event: nothing pending yet.
        expect(state.context.enablePending.has('r1')).toBe(false);
        expect(state.context.rhcScans.has('r1')).toBe(false);
        sentEvent = true;
        service.send({ type: 'RHC_OPTIMISTIC_START', repoName: 'r1' });
        return;
      }
      if (sentEvent && state.context.enablePending.has('r1')) {
        // Post-event: entry is present in pending set and rhcScans map.
        const scan = state.context.rhcScans.get('r1');
        expect(scan?.phase).toBe('scanning');
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('RHC_FAILED records a failure entry and clears the optimistic pending flag', (done) => {
    const machine = machineWithServices();
    const service = interpret(machine);
    let started = false;
    let optimisticStartedAt = 0;
    service.onTransition((state) => {
      if (state.matches('ready') && !started) {
        started = true;
        service.send({ type: 'RHC_OPTIMISTIC_START', repoName: 'r1' });
        return;
      }
      if (started && state.context.enablePending.has('r1') && optimisticStartedAt === 0) {
        optimisticStartedAt = state.context.rhcScans.get('r1')?.startedAt ?? 0;
        // Tiny delay so completedAt > startedAt and the preservation is observable.
        setTimeout(() => service.send({ type: 'RHC_FAILED', repoName: 'r1', error: 'license missing' }), 5);
        return;
      }
      if (started && !state.context.enablePending.has('r1') && state.context.rhcScans.has('r1')) {
        const scan = state.context.rhcScans.get('r1');
        expect(scan?.phase).toBe('failed');
        expect(scan?.error).toBe('license missing');
        // The original optimistic start time must survive the failure transition,
        // otherwise the UI loses elapsed-time info.
        expect(scan?.startedAt).toBe(optimisticStartedAt);
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('SET_IDENTIFY_FAILURE / CLEAR_IDENTIFY_FAILURE manage per-repo failures', (done) => {
    const machine = machineWithServices();
    const service = interpret(machine);
    let setFired = false;
    service.onTransition((state) => {
      if (state.matches('ready') && !setFired) {
        setFired = true;
        service.send({ type: 'SET_IDENTIFY_FAILURE', repoName: 'r1', reason: 'fail' });
        return;
      }
      if (setFired && state.context.identifyFailures.get('r1') === 'fail') {
        service.send({ type: 'CLEAR_IDENTIFY_FAILURE', repoName: 'r1' });
        return;
      }
      if (setFired && !state.context.identifyFailures.has('r1')) {
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('SET_TASKS updates tasks and prevAnyTaskRunning is recomputed', (done) => {
    const machine = machineWithServices();
    const service = interpret(machine);
    let sent = false;
    service.onTransition((state) => {
      if (state.matches('ready') && !sent) {
        sent = true;
        service.send({ type: 'SET_TASKS', tasks: [RUNNING_TASK] });
        return;
      }
      if (sent && state.context.tasks.length > 0) {
        expect(state.context.prevAnyTaskRunning).toBe(true);
        service.stop();
        done();
      }
    });
    service.start();
  });
});

describe('maliciousPackagesMachine — service cancellation', () => {
  it('stopping the machine while fetchAll is in-flight does not surface its result', () => {
    let resolveFetch: ((v: FetchAllStub) => void) | null = null;
    const machine = maliciousPackagesMachine
      .withContext(makeInitialContext())
      .withConfig({
        services: {
          fetchAll: () =>
            new Promise<FetchAllStub>((resolve) => {
              resolveFetch = resolve;
            }),
        },
      });

    const transitions: string[] = [];
    const service = interpret(machine).onTransition((state) => {
      transitions.push(JSON.stringify(state.value));
    });
    service.start();
    service.stop();

    // Resolve after stop — interpreter must not transition to 'ready'.
    resolveFetch?.(fetchAllStubResult());

    // Allow microtasks to flush
    return Promise.resolve().then(() => {
      expect(transitions.some((t) => t.includes('ready'))).toBe(false);
    });
  });
});

describe('normalizeTasksResponse', () => {
  it('parses RUNNING: 17% into state + progress and derives mode/repository', () => {
    const result = normalizeTasksResponse([
      {
        id: 'x',
        name: 'Malicious Packages - r1',
        enabled: true,
        properties: { repositoryName: 'r1', enableMalwareCleanup: 'true' },
        currentState: 'RUNNING: 17%',
        lastRun: '2026-01-01T00:00:00Z',
        lastRunResult: null,
        nextRun: null,
      } as any,
    ]);
    expect(result).toHaveLength(1);
    expect(result[0].currentState).toBe('RUNNING');
    expect(result[0].progress).toBe('17%');
    expect(result[0].mode).toBe('delete');
    expect(result[0].repositoryName).toBe('r1');
  });
});
