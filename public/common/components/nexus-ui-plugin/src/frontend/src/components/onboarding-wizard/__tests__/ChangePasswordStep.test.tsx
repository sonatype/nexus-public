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

import React from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';

import ChangePasswordStep from '../ChangePasswordStep';
import {
  CHANGE_ADMIN_PASSWORD_ENDPOINT,
  SUCCESS_INDICATOR_DELAY_MS,
} from '../changePasswordMachine';
import type { StepRegistration } from '../WizardContext';
import { useWizard } from '../useWizard';
import { restClient } from '../../../interface/api/rest-client';
import UIStrings from '../UIStrings';

jest.mock('../useWizard');
jest.mock('../../../interface/api/rest-client', () => ({
  restClient: {
    put: jest.fn(),
  },
}));

const mockedUseWizard = jest.mocked(useWizard);
const mockedPut = restClient.put as jest.MockedFunction<typeof restClient.put>;

const { CHANGE_ADMIN_PASSWORD } = UIStrings.ONBOARDING_WIZARD;
const VALID_PASSWORD = 'Sonatype-Nexus-123';
const OTHER_PASSWORD = 'DifferentPassword-456';
const PLAIN_TEXT_HEADER = { headers: { 'Content-Type': 'text/plain' } };
const PASSWORD_INPUT_TESTID = 'onboarding-wizard__change-password-password-input';
const CONFIRM_INPUT_TESTID = 'onboarding-wizard__change-password-confirm-input';
const ERROR_BANNER_TESTID = 'onboarding-wizard__change-password-error';
const SUCCESS_BANNER_TESTID = 'onboarding-wizard__change-password-success';
const MISMATCH_TESTID = 'onboarding-wizard__change-password-mismatch';
const FORM_TESTID = 'onboarding-wizard__change-password-form';

function lastRegistration(mock: jest.Mock): StepRegistration {
  const calls = mock.mock.calls;
  return calls[calls.length - 1][0] as StepRegistration;
}

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

describe('ChangePasswordStep', () => {
  const registerStep = jest.fn();
  const wizardSubmit = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockedUseWizard.mockReturnValue({
      state: 'stepReady',
      steps: [{ type: 'ChangeAdminPassword' }],
      currentIndex: 0,
      currentStep: { type: 'ChangeAdminPassword' },
      errorMessage: null,
      isCurrentStepValid: false,
      registerStep,
      getStarted: jest.fn(),
      submit: wizardSubmit,
      finish: jest.fn(),
      actionButtonRef: { current: null },
    });
    // Default resolve so tests that don't care about HTTP still complete cleanly
    mockedPut.mockResolvedValue(undefined);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('rendering', () => {
    it('renders the title, description, both password fields, and helper text', () => {
      render(<ChangePasswordStep />);
      expect(screen.getByText(CHANGE_ADMIN_PASSWORD.TITLE)).toBeInTheDocument();
      expect(screen.getByText(CHANGE_ADMIN_PASSWORD.DESCRIPTION)).toBeInTheDocument();
      expect(screen.getByLabelText(CHANGE_ADMIN_PASSWORD.PASSWORD_LABEL)).toBeInTheDocument();
      expect(screen.getByLabelText(CHANGE_ADMIN_PASSWORD.CONFIRM_LABEL)).toBeInTheDocument();
      expect(screen.getByText(CHANGE_ADMIN_PASSWORD.HELPER_TEXT)).toBeInTheDocument();
    });

    it('focuses the password field on mount', () => {
      render(<ChangePasswordStep />);
      expect(screen.getByTestId(PASSWORD_INPUT_TESTID)).toHaveFocus();
    });

    it('does not render the error banner in idle state', () => {
      render(<ChangePasswordStep />);
      expect(screen.queryByTestId(ERROR_BANNER_TESTID)).not.toBeInTheDocument();
    });

    it('does not render the success banner in idle state', () => {
      render(<ChangePasswordStep />);
      expect(screen.queryByTestId(SUCCESS_BANNER_TESTID)).not.toBeInTheDocument();
    });

    it('does not render the mismatch error when confirm field is empty', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      expect(screen.queryByTestId(MISMATCH_TESTID)).not.toBeInTheDocument();
    });

    it('wraps the password fields in a <form> element', () => {
      render(<ChangePasswordStep />);
      const form = screen.getByTestId(FORM_TESTID);
      expect(form.tagName).toBe('FORM');
      expect(form).toContainElement(screen.getByTestId(PASSWORD_INPUT_TESTID));
      expect(form).toContainElement(screen.getByTestId(CONFIRM_INPUT_TESTID));
    });
  });

  describe('validity and registration', () => {
    it('registers with valid: false, and both onSubmit and onError callbacks, on mount', () => {
      render(<ChangePasswordStep />);
      const registration = lastRegistration(registerStep);
      expect(registration.valid).toBe(false);
      expect(typeof registration.onSubmit).toBe('function');
      expect(typeof registration.onError).toBe('function');
    });

    it('re-registers with valid: true when both fields are non-empty and match', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      expect(lastRegistration(registerStep).valid).toBe(true);
    });

    it('re-registers with valid: false when fields do not match', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: OTHER_PASSWORD },
      });
      expect(lastRegistration(registerStep).valid).toBe(false);
    });
  });

  describe('mismatch inline error', () => {
    it('shows the mismatch error when confirm is non-empty and fields differ', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: OTHER_PASSWORD },
      });
      const mismatch = screen.getByTestId(MISMATCH_TESTID);
      expect(mismatch).toBeInTheDocument();
      expect(mismatch).toHaveAttribute('role', 'alert');
      expect(mismatch).toHaveTextContent(CHANGE_ADMIN_PASSWORD.MISMATCH_ERROR);
    });

    it('hides the mismatch error when both fields match', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      expect(screen.queryByTestId(MISMATCH_TESTID)).not.toBeInTheDocument();
    });
  });

  describe('form submission', () => {
    it('form submit calls wizardSubmit when form is valid', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      fireEvent.submit(screen.getByTestId(FORM_TESTID));

      expect(wizardSubmit).toHaveBeenCalledTimes(1);
    });

    it('form submit does not call wizardSubmit when form is invalid (mismatched)', () => {
      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: OTHER_PASSWORD },
      });

      fireEvent.submit(screen.getByTestId(FORM_TESTID));

      expect(wizardSubmit).not.toHaveBeenCalled();
    });

    it('form submit does not call wizardSubmit when both fields are empty', () => {
      render(<ChangePasswordStep />);
      fireEvent.submit(screen.getByTestId(FORM_TESTID));
      expect(wizardSubmit).not.toHaveBeenCalled();
    });
  });

  describe('onSubmit invocation (chrome integration)', () => {
    it('PUTs the plain-text password to the onboarding endpoint with the text/plain header', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      let submitPromise: Promise<void> | undefined;
      act(() => {
        submitPromise = lastRegistration(registerStep).onSubmit() as Promise<void>;
      });

      await act(async () => {
        await flushMicrotasks();
      });
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_DELAY_MS + 1);
      });
      await act(async () => {
        await submitPromise;
      });

      expect(mockedPut).toHaveBeenCalledWith(
        CHANGE_ADMIN_PASSWORD_ENDPOINT,
        VALID_PASSWORD,
        PLAIN_TEXT_HEADER
      );
    });

    it('onSubmit Promise resolves without throwing once the success indicator window closes', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      let submitError: unknown = 'not-set';
      let submitPromise: Promise<void> | undefined;
      act(() => {
        submitPromise = lastRegistration(registerStep).onSubmit() as Promise<void>;
      });

      await act(async () => {
        await flushMicrotasks();
      });
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_DELAY_MS + 1);
      });
      await act(async () => {
        try {
          await submitPromise;
          submitError = null;
        } catch (error) {
          submitError = error;
        }
      });

      expect(submitError).toBeNull();
    });
  });

  describe('success indicator (AC #8)', () => {
    it('renders the green success indicator during the showingSuccess window', async () => {
      // Never-resolving delay after put resolves keeps us in showingSuccess
      mockedPut.mockResolvedValueOnce(undefined);

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      act(() => {
        void lastRegistration(registerStep).onSubmit();
      });
      await act(async () => {
        await flushMicrotasks();
      });

      // We're now in showingSuccess (put resolved, delay timer has not fired)
      const banner = screen.getByTestId(SUCCESS_BANNER_TESTID);
      expect(banner).toBeInTheDocument();
      expect(banner).toHaveTextContent(CHANGE_ADMIN_PASSWORD.SUCCESS_MESSAGE);
    });

    it('does not render the error banner while the success indicator is showing', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      act(() => {
        void lastRegistration(registerStep).onSubmit();
      });
      await act(async () => {
        await flushMicrotasks();
      });

      expect(screen.queryByTestId(ERROR_BANNER_TESTID)).not.toBeInTheDocument();
    });

    it('keeps password inputs disabled during the showingSuccess window', async () => {
      mockedPut.mockResolvedValueOnce(undefined);

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      act(() => {
        void lastRegistration(registerStep).onSubmit();
      });
      await act(async () => {
        await flushMicrotasks();
      });

      expect(screen.getByTestId(PASSWORD_INPUT_TESTID)).toBeDisabled();
      expect(screen.getByTestId(CONFIRM_INPUT_TESTID)).toBeDisabled();
    });
  });

  describe('error handling', () => {
    it('renders the server error message in the top banner on HTTP 400', async () => {
      const backendMessage = 'Password does not meet policy requirements';
      mockedPut.mockRejectedValueOnce({
        isAxiosError: true,
        response: {
          status: 400,
          data: { message: backendMessage },
          headers: {},
        },
      });

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      let outcome: ReturnType<typeof trackOutcome> | undefined;
      act(() => {
        const p = lastRegistration(registerStep).onSubmit() as Promise<void>;
        outcome = trackOutcome(p);
      });

      await act(async () => {
        await flushMicrotasks();
      });
      await outcome;

      const banner = screen.getByTestId(ERROR_BANNER_TESTID);
      expect(banner).toHaveTextContent(backendMessage);
    });

    it('renders the generic network-error fallback when the request never reaches the server', async () => {
      mockedPut.mockRejectedValueOnce({
        isAxiosError: true,
        response: undefined,
        code: 'NETWORK_ERROR',
      });

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      let outcome: ReturnType<typeof trackOutcome> | undefined;
      act(() => {
        const p = lastRegistration(registerStep).onSubmit() as Promise<void>;
        outcome = trackOutcome(p);
      });

      await act(async () => {
        await flushMicrotasks();
      });
      await outcome;

      const banner = screen.getByTestId(ERROR_BANNER_TESTID);
      expect(banner).toHaveTextContent(CHANGE_ADMIN_PASSWORD.NETWORK_ERROR_FALLBACK);
    });

    it('rejects the onSubmit Promise with an Error carrying the display message so chrome routes to the onError delegate', async () => {
      const backendMessage = 'Password does not meet policy requirements';
      mockedPut.mockRejectedValueOnce({
        isAxiosError: true,
        response: {
          status: 400,
          data: { message: backendMessage },
          headers: {},
        },
      });

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      let outcome: ReturnType<typeof trackOutcome> | undefined;
      act(() => {
        const p = lastRegistration(registerStep).onSubmit() as Promise<void>;
        outcome = trackOutcome(p);
      });

      await act(async () => {
        await flushMicrotasks();
      });
      const settlement = await outcome!;

      expect(settlement.status).toBe('rejected');
      expect(settlement.error).toBeInstanceOf(Error);
      expect((settlement.error as Error).message).toBe(backendMessage);
    });

    it('retrying after error: submitting again fires a fresh PUT and clears the previous banner while in flight', async () => {
      mockedPut.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 400, data: { message: 'first try failed' }, headers: {} },
      });

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      let firstOutcome: ReturnType<typeof trackOutcome> | undefined;
      act(() => {
        const p = lastRegistration(registerStep).onSubmit() as Promise<void>;
        firstOutcome = trackOutcome(p);
      });
      await act(async () => {
        await flushMicrotasks();
      });
      await firstOutcome;

      expect(screen.getByTestId(ERROR_BANNER_TESTID)).toBeInTheDocument();

      // Retry with a pending promise to observe the "in flight" moment
      let resolveSecond!: () => void;
      mockedPut.mockImplementationOnce(
        () =>
          new Promise<void>((resolve) => {
            resolveSecond = resolve;
          })
      );

      let secondOutcome: ReturnType<typeof trackOutcome> | undefined;
      act(() => {
        const p = lastRegistration(registerStep).onSubmit() as Promise<void>;
        secondOutcome = trackOutcome(p);
      });

      // Machine has synchronously transitioned to submitting, which clears
      // errorMessage as a transition action; the banner should be gone.
      expect(screen.queryByTestId(ERROR_BANNER_TESTID)).not.toBeInTheDocument();

      await act(async () => {
        resolveSecond();
        await flushMicrotasks();
      });
      await act(async () => {
        jest.advanceTimersByTime(SUCCESS_INDICATOR_DELAY_MS + 1);
      });
      await secondOutcome;

      expect(mockedPut).toHaveBeenCalledTimes(2);
    });

    it('onError callback is a no-op safe to call (chrome delegates recoverable errors here without terminating the wizard)', () => {
      render(<ChangePasswordStep />);
      const registration = lastRegistration(registerStep);
      expect(() => registration.onError?.(new Error('anything'))).not.toThrow();
    });

    it('re-registers with valid: false while a submission is in flight so the wizard chrome disables Next and blocks double-submit', async () => {
      let resolveInFlight!: () => void;
      mockedPut.mockImplementationOnce(
        () =>
          new Promise<void>((resolve) => {
            resolveInFlight = resolve;
          })
      );

      render(<ChangePasswordStep />);
      fireEvent.change(screen.getByTestId(PASSWORD_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });
      fireEvent.change(screen.getByTestId(CONFIRM_INPUT_TESTID), {
        target: { value: VALID_PASSWORD },
      });

      expect(lastRegistration(registerStep).valid).toBe(true);

      act(() => {
        void lastRegistration(registerStep).onSubmit();
      });

      // Machine transitioned to submitting synchronously; re-registration ran.
      expect(lastRegistration(registerStep).valid).toBe(false);

      await act(async () => {
        resolveInFlight();
        await flushMicrotasks();
      });
    });
  });
});
