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

import UIStrings from '../../../UIStrings';
import { createEulaMachine, isValid } from '../eulaMachine';

const { COMMUNITY_EULA } = UIStrings.ONBOARDING_WIZARD;

/**
 * Flush queued microtasks. XState v4 executes invoke onDone/onError transitions
 * on the microtask queue after the invoked promise settles; two flushes are
 * enough for the settle -> transition -> follow-up assign chain.
 */
async function flushMicrotasks(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}

describe('eulaMachine', () => {
  describe('initial state', () => {
    it('starts in loading state with initial context', () => {
      const machine = createEulaMachine();
      const service = interpret(machine).start();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('loading');
      expect(snapshot.context.accepted).toBe(false);
      expect(snapshot.context.disclaimer).toBe('');
      expect(snapshot.context.loading).toBe(true);
      expect(snapshot.context.errorMessage).toBeNull();
      expect(snapshot.context.errorKind).toBeNull();

      service.stop();
    });
  });

  describe('FETCH_SUCCESS', () => {
    it('transitions from loading to idle and sets disclaimer', () => {
      const machine = createEulaMachine();
      const state = machine.transition('loading', {
        type: 'FETCH_SUCCESS',
        disclaimer: 'Test EULA text',
      });

      expect(state.value).toBe('idle');
      expect(state.context.disclaimer).toBe('Test EULA text');
      expect(state.context.loading).toBe(false);
      expect(state.context.errorMessage).toBeNull();
      expect(state.context.errorKind).toBeNull();
    });
  });

  describe('FETCH_ERROR', () => {
    it('transitions from loading to error and tags the error as fetch', () => {
      const machine = createEulaMachine();
      const state = machine.transition('loading', {
        type: 'FETCH_ERROR',
        error: 'Failed to load',
      });

      expect(state.value).toBe('error');
      expect(state.context.errorMessage).toBe('Failed to load');
      expect(state.context.loading).toBe(false);
      expect(state.context.errorKind).toBe('fetch');
    });
  });

  describe('ACCEPT event', () => {
    it('sets accepted to true in idle state', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: false,
          disclaimer: 'Test',
          loading: false,
          errorMessage: null,
          errorKind: null,
        })
        .transition('idle', { type: 'ACCEPT', checked: true });

      expect(state.value).toBe('idle');
      expect(state.context.accepted).toBe(true);
    });

    it('sets accepted to false in idle state', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: null,
          errorKind: null,
        })
        .transition('idle', { type: 'ACCEPT', checked: false });

      expect(state.value).toBe('idle');
      expect(state.context.accepted).toBe(false);
    });

    it('sets accepted in error state (for retry scenario)', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: false,
          disclaimer: 'Test',
          loading: false,
          errorMessage: 'Previous error',
          errorKind: 'submit',
        })
        .transition('error', { type: 'ACCEPT', checked: true });

      expect(state.value).toBe('error');
      expect(state.context.accepted).toBe(true);
      expect(state.context.errorMessage).toBe('Previous error');
    });
  });

  describe('SUBMIT gating', () => {
    it('SUBMIT with accepted=false stays in idle', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: false,
          disclaimer: 'Test',
          loading: false,
          errorMessage: null,
          errorKind: null,
        })
        .transition('idle', { type: 'SUBMIT' });

      expect(state.value).toBe('idle');
    });

    it('SUBMIT with accepted=true transitions to submitting', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: null,
          errorKind: null,
        })
        .transition('idle', { type: 'SUBMIT' });

      expect(state.value).toBe('submitting');
    });

    it('SUBMIT clears errorMessage and errorKind on transition to submitting', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: 'Previous error',
          errorKind: 'submit',
        })
        .transition('error', { type: 'SUBMIT' });

      expect(state.value).toBe('submitting');
      expect(state.context.errorMessage).toBeNull();
      expect(state.context.errorKind).toBeNull();
    });

    it('SUBMIT from error state with accepted=true transitions to submitting', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: 'Some error',
          errorKind: 'submit',
        })
        .transition('error', { type: 'SUBMIT' });

      expect(state.value).toBe('submitting');
    });

    it('SUBMIT from error state with accepted=false stays in error', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: false,
          disclaimer: 'Test',
          loading: false,
          errorMessage: 'Some error',
          errorKind: 'submit',
        })
        .transition('error', { type: 'SUBMIT' });

      expect(state.value).toBe('error');
    });

    it('FETCH_ERROR -> ACCEPT -> SUBMIT stays in error when disclaimer is empty', () => {
      // Guards against POSTing { accepted: true, disclaimer: '' } to the
      // acceptance endpoint when the initial fetch failed and the user
      // checked the box before retrying.
      const machine = createEulaMachine();
      const service = interpret(machine).start();

      service.send({ type: 'FETCH_ERROR', error: 'boom' });
      expect(service.getSnapshot().value).toBe('error');
      expect(service.getSnapshot().context.disclaimer).toBe('');

      service.send({ type: 'ACCEPT', checked: true });
      expect(service.getSnapshot().context.accepted).toBe(true);

      service.send({ type: 'SUBMIT' });
      expect(service.getSnapshot().value).toBe('error');
    });
  });

  describe('invoked submit service', () => {
    it('service resolve routes submitting -> success', async () => {
      const mockSubmit = jest.fn().mockResolvedValue(undefined);
      const machine = createEulaMachine(mockSubmit);
      const service = interpret(machine).start();

      // First, transition to idle via FETCH_SUCCESS
      service.send({ type: 'FETCH_SUCCESS', disclaimer: 'Test EULA' });
      // Accept the EULA
      service.send({ type: 'ACCEPT', checked: true });
      // Submit
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('success');
      expect(mockSubmit).toHaveBeenCalledTimes(1);
      // Submit service receives context as first arg (XState v4 passes context, event, meta)
      const callArgs = mockSubmit.mock.calls[0];
      expect(callArgs[0]).toEqual(
        expect.objectContaining({
          accepted: true,
          disclaimer: 'Test EULA',
          loading: false,
          errorMessage: null,
        })
      );

      service.stop();
    });

    it('service reject routes submitting -> error and captures message', async () => {
      const mockSubmit = jest.fn().mockRejectedValue(new Error('Network error'));
      const machine = createEulaMachine(mockSubmit);
      const service = interpret(machine).start();

      service.send({ type: 'FETCH_SUCCESS', disclaimer: 'Test EULA' });
      service.send({ type: 'ACCEPT', checked: true });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('error');
      expect(snapshot.context.errorMessage).toBe('Network error');
      expect(snapshot.context.errorKind).toBe('submit');

      service.stop();
    });

    it('service reject with non-Error captures fallback message', async () => {
      const mockSubmit = jest.fn().mockRejectedValue({ some: 'object' });
      const machine = createEulaMachine(mockSubmit);
      const service = interpret(machine).start();

      service.send({ type: 'FETCH_SUCCESS', disclaimer: 'Test EULA' });
      service.send({ type: 'ACCEPT', checked: true });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('error');
      expect(snapshot.context.errorMessage).toBe(COMMUNITY_EULA.SUBMISSION_ERROR);
      expect(snapshot.context.errorKind).toBe('submit');

      service.stop();
    });

    it('submit-error → SUBMIT → success recovers via a second submit', async () => {
      const mockSubmit = jest
        .fn()
        .mockRejectedValueOnce(new Error('Transient error'))
        .mockResolvedValueOnce(undefined);
      const machine = createEulaMachine(mockSubmit);
      const service = interpret(machine).start();

      service.send({ type: 'FETCH_SUCCESS', disclaimer: 'Test EULA' });
      service.send({ type: 'ACCEPT', checked: true });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();
      expect(service.getSnapshot().value).toBe('error');
      expect(service.getSnapshot().context.errorKind).toBe('submit');

      // Retrying via SUBMIT (Next click again) — no RETRY event, no re-fetch.
      service.send({ type: 'SUBMIT' });
      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('success');
      expect(mockSubmit).toHaveBeenCalledTimes(2);

      service.stop();
    });
  });

  describe('RETRY event', () => {
    it('RETRY from error transitions to loading and clears error', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: 'Some error',
          errorKind: 'fetch',
        })
        .transition('error', { type: 'RETRY' });

      expect(state.value).toBe('loading');
      expect(state.context.loading).toBe(true);
      expect(state.context.errorMessage).toBeNull();
      expect(state.context.errorKind).toBeNull();
    });
  });

  describe('success is terminal', () => {
    it('once in success, further SUBMIT events are ignored (state is final)', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: null,
          errorKind: null,
        })
        .transition('success', { type: 'SUBMIT' });

      expect(state.value).toBe('success');
    });

    it('once in success, ACCEPT events are ignored', () => {
      const machine = createEulaMachine();
      const state = machine
        .withContext({
          accepted: true,
          disclaimer: 'Test',
          loading: false,
          errorMessage: null,
          errorKind: null,
        })
        .transition('success', { type: 'ACCEPT', checked: false });

      expect(state.value).toBe('success');
      expect(state.context.accepted).toBe(true);
    });
  });

  describe('isValid selector', () => {
    it('returns false when accepted is false', () => {
      expect(isValid({ accepted: false, disclaimer: 'Test EULA' })).toBe(false);
    });

    it('returns true when accepted is true and disclaimer is non-empty', () => {
      expect(isValid({ accepted: true, disclaimer: 'Test EULA' })).toBe(true);
    });

    it('returns false when disclaimer is empty even if accepted is true', () => {
      expect(isValid({ accepted: true, disclaimer: '' })).toBe(false);
    });
  });
});
