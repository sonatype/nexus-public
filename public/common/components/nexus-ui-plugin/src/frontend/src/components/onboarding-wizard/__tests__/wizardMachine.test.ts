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

import {
  createWizardMachine,
  ONBOARDING_STILL_REQUIRED_NAME,
  VERIFY_TIMEOUT_MS,
} from '../wizardMachine';
import type { OnboardingStep } from '../types';

describe('wizardMachine', () => {
  describe('fetchSteps', () => {
    it('reaches stepReady after fetchSteps resolves; context.steps matches the mocked list; currentIndex is -1 (Welcome)', async () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
        { type: 'hosted-repo' },
      ];
      const machine = createWizardMachine({
        fetchSteps: async () => steps,
      });

      const service = interpret(machine).start();

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('stepReady')) {
            resolve();
          }
        });
      });

      const snapshot = service.getSnapshot();
      expect(snapshot.context.steps).toEqual(steps);
      expect(snapshot.context.currentIndex).toBe(-1);

      service.stop();
    });

    it('fetchSteps rejecting from loading transitions to issueOccurred and captures errorMessage', async () => {
      const machine = createWizardMachine({
        fetchSteps: async () => {
          throw new Error('Network failure');
        },
      });

      const service = interpret(machine).start();

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('issueOccurred')) {
            resolve();
          }
        });
      });

      const snapshot = service.getSnapshot();
      expect(snapshot.context.errorMessage).toBe('Error: Network failure');

      service.stop();
    });
  });

  describe('GET_STARTED', () => {
    it('with empty steps transitions to setupComplete (not stepReady)', () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [],
      });

      // Start at stepReady with empty steps
      let state = machine
        .withContext({
          steps: [],
          currentIndex: -1,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'GET_STARTED' });

      expect(state.value).toBe('setupComplete');
      expect(state.context.currentIndex).toBe(0);
    });

    it('with 3 steps sets currentIndex to 0 and stays in stepReady', () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
        { type: 'hosted-repo' },
      ];
      const machine = createWizardMachine({
        fetchSteps: async () => steps,
      });

      let state = machine
        .withContext({
          steps,
          currentIndex: -1,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'GET_STARTED' });

      expect(state.value).toBe('stepReady');
      expect(state.context.currentIndex).toBe(0);
    });
  });

  describe('STEP_ADVANCED', () => {
    it('at index N-1 (last step) transitions to setupComplete', () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
        { type: 'hosted-repo' },
      ];
      const machine = createWizardMachine({
        fetchSteps: async () => steps,
      });

      // currentIndex is 2 (last of 3 steps: 0, 1, 2)
      let state = machine
        .withContext({
          steps,
          currentIndex: 2,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'STEP_ADVANCED' });

      expect(state.value).toBe('setupComplete');
      expect(state.context.currentIndex).toBe(3);
    });

    it('at index i < N-1 increments the index and stays in stepReady', () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
        { type: 'hosted-repo' },
      ];
      const machine = createWizardMachine({
        fetchSteps: async () => steps,
      });

      // currentIndex is 0 (first of 3 steps)
      let state = machine
        .withContext({
          steps,
          currentIndex: 0,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'STEP_ADVANCED' });

      expect(state.value).toBe('stepReady');
      expect(state.context.currentIndex).toBe(1);

      // currentIndex is 1 (second of 3 steps)
      state = machine
        .withContext({
          steps,
          currentIndex: 1,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'STEP_ADVANCED' });

      expect(state.value).toBe('stepReady');
      expect(state.context.currentIndex).toBe(2);
    });
  });

  describe('STEP_FAILED', () => {
    it('sets errorMessage and transitions to issueOccurred', () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const machine = createWizardMachine({
        fetchSteps: async () => steps,
      });

      let state = machine
        .withContext({
          steps,
          currentIndex: 0,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'STEP_FAILED', error: 'Validation failed' });

      expect(state.value).toBe('issueOccurred');
      expect(state.context.errorMessage).toBe('Validation failed');
    });
  });

  describe('SUBMIT and verify', () => {
    it('SUBMIT from setupComplete transitions to verifying; verify resolving transitions to done', async () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [{ type: 'admin-password' }],
        verify: async () => {},
      });

      const service = interpret(machine).start();

      // Wait for the machine to reach stepReady (from initial loading -> fetchSteps)
      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('stepReady')) {
            resolve();
          }
        });
      });

      // Now at stepReady with 1 step, currentIndex = -1
      // Send STEP_ADVANCED to advance from Welcome (index -1) to first step (index 0)
      // Then again from index 0 to setupComplete
      service.send({ type: 'GET_STARTED' }); // moves from -1 to 0
      service.send({ type: 'STEP_ADVANCED' }); // moves from 0 to 1, since 1 >= steps.length, goes to setupComplete

      // Wait for setupComplete
      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('setupComplete')) {
            resolve();
          }
        });
      });

      // Now send SUBMIT to trigger verify
      service.send({ type: 'SUBMIT' });

      // Wait for done
      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('done')) {
            resolve();
          }
        });
      });

      expect(service.getSnapshot().matches('done')).toBe(true);
      service.stop();
    });

    it('verify rejecting with generic error classifies as network and returns to setupComplete', async () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [{ type: 'admin-password' }],
        verify: async () => {
          throw new Error('Verification failed');
        },
      });

      const service = interpret(machine).start();

      // Wait for stepReady
      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('stepReady')) {
            resolve();
          }
        });
      });

      // Navigate to setupComplete
      service.send({ type: 'GET_STARTED' });
      service.send({ type: 'STEP_ADVANCED' });

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('setupComplete')) {
            resolve();
          }
        });
      });

      service.send({ type: 'SUBMIT' });

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('setupComplete') && state.context.errorMessage) {
            resolve();
          }
        });
      });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('setupComplete')).toBe(true);
      expect(snapshot.context.errorMessage).toBe('Error: Verification failed');
      expect(snapshot.context.errorKind).toBe('network');

      service.stop();
    });

    it('verify rejecting with OnboardingStillRequired classifies as stillRequired', async () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [{ type: 'admin-password' }],
        verify: async () => {
          const err = new Error('Onboarding still required');
          err.name = ONBOARDING_STILL_REQUIRED_NAME;
          throw err;
        },
      });

      const service = interpret(machine).start();

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('stepReady')) {
            resolve();
          }
        });
      });

      service.send({ type: 'GET_STARTED' });
      service.send({ type: 'STEP_ADVANCED' });

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('setupComplete')) {
            resolve();
          }
        });
      });

      service.send({ type: 'SUBMIT' });

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('setupComplete') && state.context.errorMessage) {
            resolve();
          }
        });
      });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('setupComplete')).toBe(true);
      expect(snapshot.context.errorKind).toBe('stillRequired');

      service.stop();
    });

    it('SUBMIT from setupComplete clears prior error before verifying', () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [],
        verify: async () => {},
      });

      // Simulate a previously-failed verify: sitting on setupComplete with an error.
      const state = machine
        .withContext({
          steps: [],
          currentIndex: 0,
          errorMessage: 'Error: Previous failure',
          errorKind: 'network',
        })
        .transition('setupComplete', { type: 'SUBMIT' });

      expect(state.value).toBe('verifying');
      expect(state.context.errorMessage).toBeNull();
      expect(state.context.errorKind).toBeNull();
    });

    it('verifying falls back to setupComplete when verify hangs past the timeout', async () => {
      // A verify that never resolves or rejects would otherwise strand the user on
      // the verifying screen (footer hidden, Escape suppressed). The `after` guard
      // in the machine bails out to setupComplete so the user can retry or dismiss.
      jest.useFakeTimers();
      try {
        const machine = createWizardMachine({
          fetchSteps: async () => [],
          verify: () => new Promise<void>(() => {}),
        });

        const service = interpret(machine).start();

        // Drain the fetchSteps microtask so the machine reaches setupComplete
        // (empty steps means we land there directly from loading via stepReady).
        await Promise.resolve();
        await Promise.resolve();
        service.send({ type: 'GET_STARTED' });
        service.send({ type: 'SUBMIT' });

        expect(service.getSnapshot().matches('verifying')).toBe(true);

        jest.advanceTimersByTime(VERIFY_TIMEOUT_MS);

        const snapshot = service.getSnapshot();
        expect(snapshot.matches('setupComplete')).toBe(true);
        expect(snapshot.context.errorKind).toBe('network');
        expect(snapshot.context.errorMessage).toBe('Verification timed out');

        service.stop();
      } finally {
        jest.useRealTimers();
      }
    });

    it('exports a VERIFY_TIMEOUT_MS constant matching the machine timeout guard', () => {
      // Keeps the constant honest — if someone edits the machine's `after` block
      // without updating the constant, the guard becomes silently ineffective.
      expect(VERIFY_TIMEOUT_MS).toBe(30_000);
    });
  });

  describe('verification flow', () => {
    it('verifying state transitions to done when verify resolves (empty array)', async () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [],
        verify: async () => {},
      });

      const service = interpret(machine).start();

      // Wait for stepReady first
      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('stepReady')) {
            resolve();
          }
        });
      });

      // Transition to setupComplete
      service.send({ type: 'GET_STARTED' });

      // Transition to verifying
      service.send({ type: 'SUBMIT' });

      await new Promise<void>((resolve) => {
        service.onTransition((state) => {
          if (state.matches('done')) {
            resolve();
          }
        });
      });

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('done');
      expect(snapshot.context.errorMessage).toBeNull();

      service.stop();
    });
  });

  describe('SKIP', () => {
    it('transitions from stepReady to skipped', () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const machine = createWizardMachine({
        fetchSteps: async () => steps,
      });

      let state = machine
        .withContext({
          steps,
          currentIndex: 0,
          errorMessage: null,
          errorKind: null,
        })
        .transition('stepReady', { type: 'SKIP' });

      expect(state.value).toBe('skipped');
    });

    it('transitions from issueOccurred to skipped so users can escape a locked error state', () => {
      const machine = createWizardMachine({
        fetchSteps: async () => [],
      });

      const state = machine
        .withContext({
          steps: [],
          currentIndex: -1,
          errorMessage: 'boom',
        })
        .transition('issueOccurred', { type: 'SKIP' });

      expect(state.value).toBe('skipped');
    });
  });
});
