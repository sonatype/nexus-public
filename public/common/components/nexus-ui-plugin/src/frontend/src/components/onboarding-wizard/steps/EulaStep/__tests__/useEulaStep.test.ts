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

import { act, renderHook } from '@testing-library/react';

import { useWizard } from '../../../useWizard';
import { useEulaApi } from '../../../../preview/pages/settings/security/eula/useEulaApi';
import { useEulaStep, SUCCESS_INDICATOR_MS } from '../useEulaStep';

jest.mock('../../../useWizard');
jest.mock('../../../../preview/pages/settings/security/eula/useEulaApi');

const mockedUseWizard = jest.mocked(useWizard);
const mockedUseEulaApi = jest.mocked(useEulaApi);

/**
 * Flush queued microtasks. XState v4 executes invoke onDone/onError transitions
 * on the microtask queue after the invoked promise settles; two flushes are
 * enough for the settle -> transition -> follow-up assign chain.
 */
async function flushMicrotasks(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}

/**
 * Wrap a submit promise so its rejection is always considered "handled" the
 * moment it settles. Without this, the resolver useEffect can reject inside a
 * React commit before the test has attached its own `.catch`, and Jest flags
 * the unobserved rejection even though the test itself is about to await it.
 */
function trackOutcome(promise: Promise<void>): Promise<{
  status: 'resolved' | 'rejected';
  error?: unknown;
}> {
  return promise.then(
    () => ({ status: 'resolved' as const }),
    (error) => ({ status: 'rejected' as const, error })
  );
}

function lastRegistration(mock: jest.Mock) {
  const calls = mock.mock.calls;
  return calls[calls.length - 1][0] as {
    valid: boolean;
    onSubmit: () => Promise<void>;
    onError: () => void;
  };
}

describe('useEulaStep', () => {
  const registerStep = jest.fn();
  const wizardSubmit = jest.fn();
  const fetchEulaStatus = jest.fn();
  const acceptEula = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    mockedUseWizard.mockReturnValue({
      state: 'stepReady',
      steps: [],
      currentIndex: 0,
      currentStep: null,
      errorMessage: null,
      isCurrentStepValid: false,
      registerStep,
      getStarted: jest.fn(),
      submit: wizardSubmit,
      skip: jest.fn(),
      finish: jest.fn(),
      actionButtonRef: { current: null },
    });

    mockedUseEulaApi.mockReturnValue({
      fetchEulaStatus,
      acceptEula,
    });
  });

  describe('initialization', () => {
    it('returns loading: true initially while fetching EULA status', () => {
      // Never resolving to keep in loading state
      fetchEulaStatus.mockImplementation(() => new Promise(() => {}));

      const { result } = renderHook(() => useEulaStep());

      expect(result.current.loading).toBe(true);
      expect(result.current.accepted).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('fetches EULA status on mount', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });

      renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      expect(fetchEulaStatus).toHaveBeenCalledTimes(1);
    });

    it('transitions to not loading after successful fetch', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error when fetch fails and tags it as a fetch error', async () => {
      fetchEulaStatus.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBe('Network error');
      expect(result.current.errorKind).toBe('fetch');
    });
  });

  describe('registration', () => {
    it('registers step on mount with valid: false while loading', () => {
      fetchEulaStatus.mockImplementation(() => new Promise(() => {}));

      renderHook(() => useEulaStep());

      const registration = lastRegistration(registerStep);
      expect(registration.valid).toBe(false);
      expect(typeof registration.onSubmit).toBe('function');
      expect(typeof registration.onError).toBe('function');
    });

    it('re-registers with valid: false after fetch when not accepted', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });

      renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      const registration = lastRegistration(registerStep);
      expect(registration.valid).toBe(false);
    });
  });

  describe('acceptance checkbox', () => {
    it('onAcceptChange updates accepted state', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      expect(result.current.accepted).toBe(false);

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      expect(result.current.accepted).toBe(true);
    });

    it('re-registers with valid: true when accepted', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      const registration = lastRegistration(registerStep);
      expect(registration.valid).toBe(true);
    });
  });

  describe('submission', () => {
    it('onSubmit calls acceptEula with disclaimer', async () => {
      jest.useFakeTimers();
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });
      acceptEula.mockResolvedValue(undefined);

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      // Accept the EULA via the hook's callback
      await act(async () => {
        result.current.onAcceptChange(true);
      });

      const registration = lastRegistration(registerStep);
      let outcome;
      await act(async () => {
        outcome = trackOutcome(registration.onSubmit());
        await flushMicrotasks();
      });
      // AC #16: resolver is held for SUCCESS_INDICATOR_MS after success.
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
        await flushMicrotasks();
      });
      await outcome;

      expect(acceptEula).toHaveBeenCalledWith('Test EULA text');
      jest.useRealTimers();
    });

    it('onSubmit resolves when acceptEula succeeds', async () => {
      jest.useFakeTimers();
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });
      acceptEula.mockResolvedValue(undefined);

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      const registration = lastRegistration(registerStep);
      let outcome;
      await act(async () => {
        outcome = trackOutcome(registration.onSubmit());
        await flushMicrotasks();
      });
      // AC #16: exposes success=true before the resolver fires; resolver
      // is held for SUCCESS_INDICATOR_MS so the wizard advances after the
      // user has seen the indicator.
      expect(result.current.success).toBe(true);
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
        await flushMicrotasks();
      });
      const outcomeResult = await outcome!;

      expect(outcomeResult.status).toBe('resolved');
      jest.useRealTimers();
    });

    it('holds resolution for SUCCESS_INDICATOR_MS after success', async () => {
      jest.useFakeTimers();
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });
      acceptEula.mockResolvedValue(undefined);

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      const registration = lastRegistration(registerStep);
      let settled = false;
      await act(async () => {
        registration.onSubmit().then(() => {
          settled = true;
        });
        await flushMicrotasks();
      });

      // Just before the delay elapses, the promise must not have resolved yet.
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_MS - 1);
        await flushMicrotasks();
      });
      expect(settled).toBe(false);

      // Crossing the threshold resolves the promise so the wizard advances.
      await act(async () => {
        jest.advanceTimersByTime(1);
        await flushMicrotasks();
      });
      expect(settled).toBe(true);
      jest.useRealTimers();
    });

    it('onSubmit rejects when acceptEula fails', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });
      acceptEula.mockRejectedValue(new Error('Submit failed'));

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      const registration = lastRegistration(registerStep);
      let outcome;
      await act(async () => {
        outcome = trackOutcome(registration.onSubmit());
        await flushMicrotasks();
      });
      const outcomeResult = await outcome!;

      expect(outcomeResult.status).toBe('rejected');
      expect((outcomeResult.error as Error).message).toBe('Submit failed');
      expect(result.current.error).toBe('Submit failed');
      expect(result.current.errorKind).toBe('submit');
    });

    it('recovers from a submit failure when the user clicks Next again', async () => {
      jest.useFakeTimers();
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });
      acceptEula
        .mockRejectedValueOnce(new Error('Transient submit failure'))
        .mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      // First Next click — POST fails.
      let firstOutcome;
      await act(async () => {
        firstOutcome = trackOutcome(lastRegistration(registerStep).onSubmit());
        await flushMicrotasks();
      });
      const firstResult = await firstOutcome!;
      expect(firstResult.status).toBe('rejected');
      expect(result.current.errorKind).toBe('submit');

      // The registration must stay valid so the wizard chrome keeps Next
      // enabled — that is what makes the second click possible.
      expect(lastRegistration(registerStep).valid).toBe(true);

      // Second Next click — POST succeeds; no explicit retry event fires,
      // and fetchEulaStatus is NOT called again (submit retry doesn't refetch).
      let secondOutcome;
      await act(async () => {
        secondOutcome = trackOutcome(lastRegistration(registerStep).onSubmit());
        await flushMicrotasks();
      });
      expect(result.current.success).toBe(true);
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
        await flushMicrotasks();
      });
      const secondResult = await secondOutcome!;

      expect(secondResult.status).toBe('resolved');
      expect(acceptEula).toHaveBeenCalledTimes(2);
      expect(fetchEulaStatus).toHaveBeenCalledTimes(1);
      jest.useRealTimers();
    });

    it('re-registers with valid: false while submitting', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });
      let resolveAccept!: () => void;
      acceptEula.mockImplementation(
        () =>
          new Promise<void>((resolve) => {
            resolveAccept = resolve;
          })
      );

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      await act(async () => {
        result.current.onAcceptChange(true);
      });

      expect(lastRegistration(registerStep).valid).toBe(true);

      const registration = lastRegistration(registerStep);

      await act(async () => {
        registration.onSubmit();
        await flushMicrotasks();
      });

      // Should be false while submitting
      expect(lastRegistration(registerStep).valid).toBe(false);

      // Complete the submission
      await act(async () => {
        resolveAccept();
        await flushMicrotasks();
      });
    });
  });

  describe('error handling', () => {
    it('onError callback is a no-op safe to call', async () => {
      fetchEulaStatus.mockResolvedValue({
        accepted: false,
        disclaimer: 'Test EULA text',
      });

      renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      const registration = lastRegistration(registerStep);
      expect(() => registration.onError()).not.toThrow();
    });

    it('onRetry sends RETRY event and re-triggers the fetch', async () => {
      // First fetch fails; second (pending) is never resolved so we can
      // observe the intermediate "loading" state the machine transitions to.
      let resolveSecondFetch: (value: { accepted: boolean; disclaimer: string }) => void = () => {};
      const secondFetch = new Promise<{ accepted: boolean; disclaimer: string }>((resolve) => {
        resolveSecondFetch = resolve;
      });
      fetchEulaStatus
        .mockRejectedValueOnce(new Error('Fetch error'))
        .mockReturnValueOnce(secondFetch);

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      // Should be in error state after the first failure
      expect(result.current.error).toBe('Fetch error');
      expect(result.current.errorKind).toBe('fetch');
      expect(result.current.loading).toBe(false);

      // Trigger retry - RETRY drops the machine into loading and a fresh
      // fetch is issued (still pending, so loading remains true).
      act(() => {
        result.current.onRetry();
      });

      expect(fetchEulaStatus).toHaveBeenCalledTimes(2);
      expect(result.current.loading).toBe(true);
      expect(result.current.error).toBeNull();
      expect(result.current.errorKind).toBeNull();

      // Drain the pending fetch so React/xstate don't complain at teardown.
      await act(async () => {
        resolveSecondFetch({ accepted: false, disclaimer: 'ok' });
        await flushMicrotasks();
      });
    });

    it('clears error on retry', async () => {
      let resolveSecondFetch: (value: { accepted: boolean; disclaimer: string }) => void = () => {};
      const secondFetch = new Promise<{ accepted: boolean; disclaimer: string }>((resolve) => {
        resolveSecondFetch = resolve;
      });
      fetchEulaStatus
        .mockRejectedValueOnce(new Error('First error'))
        .mockReturnValueOnce(secondFetch);

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      expect(result.current.error).toBe('First error');

      // Trigger retry - RETRY event transitions to loading and clears the error.
      act(() => {
        result.current.onRetry();
      });

      expect(result.current.loading).toBe(true);
      expect(result.current.error).toBeNull();

      await act(async () => {
        resolveSecondFetch({ accepted: false, disclaimer: 'ok' });
        await flushMicrotasks();
      });
    });

    it('onRetry re-fetches the disclaimer and recovers when the second call succeeds', async () => {
      fetchEulaStatus
        .mockRejectedValueOnce(new Error('First error'))
        .mockResolvedValueOnce({ accepted: false, disclaimer: 'Retrieved EULA' });

      const { result } = renderHook(() => useEulaStep());

      await act(async () => {
        await flushMicrotasks();
      });

      expect(result.current.error).toBe('First error');
      expect(fetchEulaStatus).toHaveBeenCalledTimes(1);

      await act(async () => {
        result.current.onRetry();
        await flushMicrotasks();
      });

      // Retry must actually re-invoke the API, not just clear the banner.
      expect(fetchEulaStatus).toHaveBeenCalledTimes(2);
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });
});
