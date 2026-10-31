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
import { logViewerMachine } from '../LogViewerMachine';

const mockLogContent = '2024-01-01 12:00:00 INFO [main] - Starting application';

function makeMachine(overrides: Record<string, any> = {}) {
  return logViewerMachine
    .withContext({ ...logViewerMachine.context, filename: 'nexus.log', ...overrides })
    .withConfig({
      services: {
        fetchContent: async () => mockLogContent,
        insertMark: async () => undefined,
      },
    });
}

describe('LogViewerMachine', () => {
  describe('initial state', () => {
    it('starts in the loading state', () => {
      const service = interpret(makeMachine());
      service.start();
      expect(service.state.matches('loading')).toBe(true);
      service.stop();
    });

    it('has correct initial context values', () => {
      const service = interpret(makeMachine());
      service.start();
      const { filename, logContent, refreshPeriod, logSize, mark, error } = service.state.context;
      expect(filename).toBe('nexus.log');
      expect(logContent).toBe('');
      expect(refreshPeriod).toBe(0);
      expect(logSize).toBe(25);
      expect(mark).toBe('');
      expect(error).toBeNull();
      service.stop();
    });
  });

  describe('data loading', () => {
    it('transitions to loaded state after successful fetch', (done) => {
      const service = interpret(makeMachine());

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          expect(state.context.logContent).toBe(mockLogContent);
          expect(state.context.error).toBeNull();
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('transitions to error state on failed fetch', (done) => {
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log' })
        .withConfig({
          services: {
            fetchContent: async () => {
              throw new Error('Network error');
            },
            insertMark: async () => undefined,
          },
        });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('error')) {
          expect(state.context.error).toBeTruthy();
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('RETRY event', () => {
    it('transitions from error back to loading on RETRY', (done) => {
      let attempt = 0;
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log' })
        .withConfig({
          services: {
            fetchContent: async () => {
              attempt++;
              if (attempt === 1) throw new Error('First attempt failed');
              return mockLogContent;
            },
            insertMark: async () => undefined,
          },
        });
      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('error') && attempt === 1) {
          service.send({ type: 'RETRY' });
        } else if (state.matches('loaded') && attempt === 2) {
          expect(state.context.logContent).toBe(mockLogContent);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('SET_MARK event', () => {
    it('updates mark in context', (done) => {
      const service = interpret(makeMachine());

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.mark === 'TEST_MARK') {
          expect(state.context.mark).toBe('TEST_MARK');
          service.stop();
          done();
        } else if (state.matches('loaded') && state.context.mark === '') {
          service.send({ type: 'SET_MARK', value: 'TEST_MARK' });
        }
      });

      service.start();
    });
  });

  describe('SET_REFRESH_PERIOD event', () => {
    it('updates refreshPeriod in context', (done) => {
      const service = interpret(makeMachine());

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.refreshPeriod === 30) {
          expect(state.context.refreshPeriod).toBe(30);
          service.stop();
          done();
        } else if (state.matches('loaded') && state.context.refreshPeriod === 0) {
          service.send({ type: 'SET_REFRESH_PERIOD', value: 30 });
        }
      });

      service.start();
    });
  });

  describe('SET_LOG_SIZE event', () => {
    it('transitions to loading when log size changes', (done) => {
      const service = interpret(makeMachine());
      let loadedOnce = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !loadedOnce) {
          loadedOnce = true;
          service.send({ type: 'SET_LOG_SIZE', value: 50 });
        } else if (state.matches('loading') && loadedOnce) {
          expect(state.context.logSize).toBe(50);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('INSERT_MARK flow', () => {
    it('transitions to insertingMark state and back to loading on success', (done) => {
      let insertMarkCalled = false;
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log', mark: 'TEST_MARK' })
        .withConfig({
          services: {
            fetchContent: async () => mockLogContent,
            insertMark: async () => {
              insertMarkCalled = true;
            },
          },
        });
      const service = interpret(machine);
      let enteredInsertingMark = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !enteredInsertingMark) {
          service.send({ type: 'INSERT_MARK' });
        } else if (state.matches('insertingMark')) {
          enteredInsertingMark = true;
        } else if (state.matches('loading') && enteredInsertingMark) {
          expect(insertMarkCalled).toBe(true);
          expect(state.context.mark).toBe('');
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('transitions back to loaded on insertMark failure', (done) => {
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log', mark: 'TEST_MARK' })
        .withConfig({
          services: {
            fetchContent: async () => mockLogContent,
            insertMark: async () => {
              throw new Error('Insert failed');
            },
          },
        });
      const service = interpret(machine);
      let enteredInsertingMark = false;

      service.onTransition((state) => {
        if (state.matches('loaded') && !enteredInsertingMark) {
          service.send({ type: 'INSERT_MARK' });
        } else if (state.matches('insertingMark')) {
          enteredInsertingMark = true;
        } else if (state.matches('loaded') && enteredInsertingMark) {
          expect(state.context.error).toBeTruthy();
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('auto-refresh in loaded', () => {
    it('does not auto-refresh when refreshPeriod is 0 (stays in loaded.idle)', async () => {
      jest.useFakeTimers();
      const fetchCount = { count: 0 };
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log', refreshPeriod: 0 })
        .withConfig({
          services: {
            fetchContent: async () => {
              fetchCount.count++;
              return mockLogContent;
            },
            insertMark: async () => undefined,
          },
        });
      const service = interpret(machine);
      service.start();

      // Let microtasks resolve so the invoke's fetchContent onDone fires.
      await Promise.resolve();
      await Promise.resolve();

      // Advance a long way — with refreshPeriod=0 we're in loaded.idle (no scheduled timer).
      jest.advanceTimersByTime(60_000);

      const snapshot = service.getSnapshot();
      expect(snapshot.matches({ loaded: 'idle' })).toBe(true);
      expect(fetchCount.count).toBe(1);

      service.stop();
      jest.useRealTimers();
    });

    it('auto-refreshes after the configured period when refreshPeriod > 0', async () => {
      jest.useFakeTimers();
      const fetchCount = { count: 0 };
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log', refreshPeriod: 30 })
        .withConfig({
          services: {
            fetchContent: async () => {
              fetchCount.count++;
              return mockLogContent;
            },
            insertMark: async () => undefined,
          },
        });
      const service = interpret(machine);
      service.start();

      // Initial fetch resolves.
      await Promise.resolve();
      await Promise.resolve();
      expect(service.getSnapshot().matches({ loaded: 'polling' })).toBe(true);
      expect(fetchCount.count).toBe(1);

      // Advance past the refresh delay; the after transition should fire a refresh.
      jest.advanceTimersByTime(30_000);
      // Let the invoked refresh promise resolve.
      await Promise.resolve();
      await Promise.resolve();

      expect(fetchCount.count).toBe(2);
      expect(service.getSnapshot().matches({ loaded: 'polling' })).toBe(true);

      service.stop();
      jest.useRealTimers();
    });

    it('SET_REFRESH_PERIOD from non-zero to zero cancels the pending after timer', async () => {
      jest.useFakeTimers();
      const fetchCount = { count: 0 };
      const machine = logViewerMachine
        .withContext({ ...logViewerMachine.context, filename: 'nexus.log', refreshPeriod: 30 })
        .withConfig({
          services: {
            fetchContent: async () => {
              fetchCount.count++;
              return mockLogContent;
            },
            insertMark: async () => undefined,
          },
        });
      const service = interpret(machine);
      service.start();

      await Promise.resolve();
      await Promise.resolve();
      expect(service.getSnapshot().matches({ loaded: 'polling' })).toBe(true);

      // Change refreshPeriod to 0 mid-flight — should cancel the pending polling timer.
      service.send({ type: 'SET_REFRESH_PERIOD', value: 0 });
      expect(service.getSnapshot().matches({ loaded: 'idle' })).toBe(true);

      // Advance past the original delay — no refresh should fire.
      jest.advanceTimersByTime(60_000);
      expect(fetchCount.count).toBe(1);

      service.stop();
      jest.useRealTimers();
    });
  });
});
