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

import { restClient } from '../../../interface/api/rest-client';
import UIStrings from '../UIStrings';
import {
  CHANGE_ADMIN_PASSWORD_ENDPOINT,
  SUCCESS_INDICATOR_DELAY_MS,
  createChangePasswordMachine,
  isFormValid,
  shouldShowMismatchError,
} from '../changePasswordMachine';

jest.mock('../../../interface/api/rest-client', () => ({
  restClient: {
    put: jest.fn(),
  },
}));

const mockedPut = restClient.put as jest.MockedFunction<typeof restClient.put>;

const VALID_PASSWORD = 'Sonatype-Nexus-123';
const OTHER_PASSWORD = 'DifferentPassword-456';
const SERVER_ERROR_MESSAGE = 'Password does not meet policy requirements';
const PLAIN_TEXT_HEADER = { headers: { 'Content-Type': 'text/plain' } };
const NETWORK_FALLBACK =
  UIStrings.ONBOARDING_WIZARD.CHANGE_ADMIN_PASSWORD.NETWORK_ERROR_FALLBACK;

/**
 * Flush queued microtasks. XState v4 executes invoke onDone/onError transitions
 * on the microtask queue after the invoked promise settles; two flushes are
 * enough for the settle → transition → follow-up assign chain.
 */
async function flushMicrotasks(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}

describe('changePasswordMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('initial state', () => {
    it('starts in idle with empty fields and no error message', () => {
      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('idle');
      expect(snapshot.context.password).toBe('');
      expect(snapshot.context.confirm).toBe('');
      expect(snapshot.context.errorMessage).toBeNull();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('UPDATE_PASSWORD writes the value to context.password', () => {
      const machine = createChangePasswordMachine();
      const state = machine.transition('idle', {
        type: 'UPDATE_PASSWORD',
        value: VALID_PASSWORD,
      });

      expect(state.value).toBe('idle');
      expect(state.context.password).toBe(VALID_PASSWORD);
    });

    it('UPDATE_CONFIRM writes the value to context.confirm', () => {
      const machine = createChangePasswordMachine();
      const state = machine.transition('idle', {
        type: 'UPDATE_CONFIRM',
        value: VALID_PASSWORD,
      });

      expect(state.value).toBe('idle');
      expect(state.context.confirm).toBe(VALID_PASSWORD);
    });

    it('field updates in error state are still captured (retry contract)', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: OTHER_PASSWORD,
          confirm: OTHER_PASSWORD,
          errorMessage: SERVER_ERROR_MESSAGE,
        })
        .transition('error', {
          type: 'UPDATE_PASSWORD',
          value: VALID_PASSWORD,
        });

      expect(state.value).toBe('error');
      expect(state.context.password).toBe(VALID_PASSWORD);
      expect(state.context.errorMessage).toBe(SERVER_ERROR_MESSAGE);
    });
  });

  describe('SUBMIT gating', () => {
    it('SUBMIT with empty password stays in idle', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: '',
          confirm: VALID_PASSWORD,
          errorMessage: null,
        })
        .transition('idle', { type: 'SUBMIT' });

      expect(state.value).toBe('idle');
    });

    it('SUBMIT with empty confirm stays in idle', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: '',
          errorMessage: null,
        })
        .transition('idle', { type: 'SUBMIT' });

      expect(state.value).toBe('idle');
    });

    it('SUBMIT with mismatched fields stays in idle', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: OTHER_PASSWORD,
          errorMessage: null,
        })
        .transition('idle', { type: 'SUBMIT' });

      expect(state.value).toBe('idle');
    });

    it('SUBMIT with valid matching non-empty fields transitions to submitting', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: VALID_PASSWORD,
          errorMessage: null,
        })
        .transition('idle', { type: 'SUBMIT' });

      expect(state.value).toBe('submitting');
    });

    it('SUBMIT from error state with valid form transitions to submitting and clears previous errorMessage', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: VALID_PASSWORD,
          errorMessage: SERVER_ERROR_MESSAGE,
        })
        .transition('error', { type: 'SUBMIT' });

      expect(state.value).toBe('submitting');
      expect(state.context.errorMessage).toBeNull();
    });

    it('SUBMIT from error state with invalid form stays in error and keeps errorMessage', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: OTHER_PASSWORD,
          errorMessage: SERVER_ERROR_MESSAGE,
        })
        .transition('error', { type: 'SUBMIT' });

      expect(state.value).toBe('error');
      expect(state.context.errorMessage).toBe(SERVER_ERROR_MESSAGE);
    });
  });

  describe('invoked submit service', () => {
    it('invokes restClient.put with the plain-text password on entering submitting', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();
      service.send({ type: 'UPDATE_PASSWORD', value: VALID_PASSWORD });
      service.send({ type: 'UPDATE_CONFIRM', value: VALID_PASSWORD });
      service.send({ type: 'SUBMIT' });

      expect(mockedPut).toHaveBeenCalledWith(
        CHANGE_ADMIN_PASSWORD_ENDPOINT,
        VALID_PASSWORD,
        PLAIN_TEXT_HEADER
      );

      service.stop();
    });

    it('service resolve routes submitting -> showingSuccess and clears password fields', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();
      service.send({ type: 'UPDATE_PASSWORD', value: VALID_PASSWORD });
      service.send({ type: 'UPDATE_CONFIRM', value: VALID_PASSWORD });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('showingSuccess');
      // Defense-in-depth: password material must not linger in memory after
      // submission — clear on transition into the success indicator window.
      expect(snapshot.context.password).toBe('');
      expect(snapshot.context.confirm).toBe('');

      service.stop();
    });

    it('showingSuccess advances to success after SUCCESS_INDICATOR_DELAY_MS', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();
      service.send({ type: 'UPDATE_PASSWORD', value: VALID_PASSWORD });
      service.send({ type: 'UPDATE_CONFIRM', value: VALID_PASSWORD });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();
      expect(service.getSnapshot().value).toBe('showingSuccess');

      jest.advanceTimersByTime(SUCCESS_INDICATOR_DELAY_MS + 1);
      expect(service.getSnapshot().value).toBe('success');

      service.stop();
    });

    it('service reject routes submitting -> error and captures the display message', async () => {
      mockedPut.mockRejectedValueOnce({
        isAxiosError: true,
        response: {
          status: 400,
          data: { message: SERVER_ERROR_MESSAGE },
          headers: {},
        },
      });

      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();
      service.send({ type: 'UPDATE_PASSWORD', value: VALID_PASSWORD });
      service.send({ type: 'UPDATE_CONFIRM', value: VALID_PASSWORD });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('error');
      expect(snapshot.context.errorMessage).toBe(SERVER_ERROR_MESSAGE);

      service.stop();
    });

    it('service reject with status 0 (no response) captures the generic network fallback message', async () => {
      mockedPut.mockRejectedValueOnce({
        isAxiosError: true,
        response: undefined,
        code: 'NETWORK_ERROR',
      });

      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();
      service.send({ type: 'UPDATE_PASSWORD', value: VALID_PASSWORD });
      service.send({ type: 'UPDATE_CONFIRM', value: VALID_PASSWORD });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();

      const snapshot = service.getSnapshot();
      expect(snapshot.value).toBe('error');
      expect(snapshot.context.errorMessage).toBe(NETWORK_FALLBACK);

      service.stop();
    });

    it('retry from error re-invokes the service and clears the previous errorMessage', async () => {
      mockedPut
        .mockRejectedValueOnce({
          isAxiosError: true,
          response: {
            status: 400,
            data: { message: SERVER_ERROR_MESSAGE },
            headers: {},
          },
        })
        .mockResolvedValueOnce(undefined);

      const machine = createChangePasswordMachine();
      const service = interpret(machine).start();
      service.send({ type: 'UPDATE_PASSWORD', value: VALID_PASSWORD });
      service.send({ type: 'UPDATE_CONFIRM', value: VALID_PASSWORD });
      service.send({ type: 'SUBMIT' });

      await flushMicrotasks();
      expect(service.getSnapshot().value).toBe('error');
      expect(service.getSnapshot().context.errorMessage).toBe(SERVER_ERROR_MESSAGE);

      service.send({ type: 'SUBMIT' });
      // clearErrorMessage action runs on the SUBMIT transition, before the
      // invoked service resolves.
      expect(service.getSnapshot().value).toBe('submitting');
      expect(service.getSnapshot().context.errorMessage).toBeNull();

      await flushMicrotasks();
      expect(service.getSnapshot().value).toBe('showingSuccess');
      expect(mockedPut).toHaveBeenCalledTimes(2);

      service.stop();
    });
  });

  describe('success is terminal', () => {
    it('once in success, further SUBMIT events are ignored (state is final)', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: VALID_PASSWORD,
          errorMessage: null,
        })
        .transition('success', { type: 'SUBMIT' });

      expect(state.value).toBe('success');
    });

    it('once in success, UPDATE_PASSWORD events are ignored (state is final)', () => {
      const machine = createChangePasswordMachine();
      const state = machine
        .withContext({
          password: VALID_PASSWORD,
          confirm: VALID_PASSWORD,
          errorMessage: null,
        })
        .transition('success', { type: 'UPDATE_PASSWORD', value: OTHER_PASSWORD });

      expect(state.value).toBe('success');
      expect(state.context.password).toBe(VALID_PASSWORD);
    });
  });

  describe('isFormValid selector', () => {
    it('returns false when both fields are empty', () => {
      expect(isFormValid({ password: '', confirm: '' })).toBe(false);
    });

    it('returns false when password is empty', () => {
      expect(isFormValid({ password: '', confirm: VALID_PASSWORD })).toBe(false);
    });

    it('returns false when confirm is empty', () => {
      expect(isFormValid({ password: VALID_PASSWORD, confirm: '' })).toBe(false);
    });

    it('returns false when fields differ', () => {
      expect(isFormValid({ password: VALID_PASSWORD, confirm: OTHER_PASSWORD })).toBe(false);
    });

    it('returns true when both are non-empty and match', () => {
      expect(isFormValid({ password: VALID_PASSWORD, confirm: VALID_PASSWORD })).toBe(true);
    });
  });

  describe('shouldShowMismatchError selector', () => {
    it('returns false when confirm is empty (user has not entered anything yet)', () => {
      expect(shouldShowMismatchError({ password: VALID_PASSWORD, confirm: '' })).toBe(false);
    });

    it('returns false when both fields match', () => {
      expect(
        shouldShowMismatchError({ password: VALID_PASSWORD, confirm: VALID_PASSWORD })
      ).toBe(false);
    });

    it('returns true when confirm is non-empty and fields differ', () => {
      expect(shouldShowMismatchError({ password: VALID_PASSWORD, confirm: OTHER_PASSWORD })).toBe(
        true
      );
    });
  });
});
