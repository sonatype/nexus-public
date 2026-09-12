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
import { act, renderHook, waitFor } from '@testing-library/react';

import { WizardProvider } from '../WizardContext';
import { useWizard } from '../useWizard';
import type { OnboardingStep } from '../types';

describe('useWizard', () => {
  it('throws when used outside a WizardProvider', () => {
    // Using an error boundary pattern to catch the thrown error
    let error: Error | undefined;
    const { result } = renderHook(() => {
      try {
        return useWizard();
      } catch (e) {
        error = e as Error;
        return null;
      }
    });
    expect(error).toBeInstanceOf(Error);
    expect(error?.message).toBe('useWizard must be used within a WizardProvider');
  });

  describe('with provider and default state', () => {
    const steps: OnboardingStep[] = [
      { type: 'admin-password' },
      { type: 'anonymous-access' },
      { type: 'hosted-repo' },
    ];

    it('state === stepReady after fetchSteps resolves', async () => {
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });
    });

    it('isCurrentStepValid === true on Welcome (currentIndex === -1)', async () => {
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(-1);
        expect(result.current.isCurrentStepValid).toBe(true);
      });
    });
  });

  describe('registerStep', () => {
    it('registering {valid: false} sets isCurrentStepValid to false', async () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      // Wait for stepReady
      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      // Click "Get Started" to move to first step
      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      // Now we're on a configuration step, register as invalid
      act(() => {
        result.current.registerStep({
          valid: false,
          onSubmit: jest.fn(),
        });
      });

      await waitFor(() => {
        expect(result.current.isCurrentStepValid).toBe(false);
      });
    });

    it('re-registering with valid: true toggles isCurrentStepValid back', async () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      // Wait for stepReady
      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      // Click "Get Started" to move to first step
      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      // Register as invalid
      act(() => {
        result.current.registerStep({
          valid: false,
          onSubmit: jest.fn(),
        });
      });

      await waitFor(() => {
        expect(result.current.isCurrentStepValid).toBe(false);
      });

      // Re-register as valid
      act(() => {
        result.current.registerStep({
          valid: true,
          onSubmit: jest.fn(),
        });
      });

      await waitFor(() => {
        expect(result.current.isCurrentStepValid).toBe(true);
      });
    });
  });

  describe('getStarted', () => {
    it('after getStarted() with 3 steps, currentIndex === 0 and currentStep === steps[0]', async () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
        { type: 'hosted-repo' },
      ];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
        expect(result.current.currentStep).toEqual(steps[0]);
      });
    });
  });

  describe('submit', () => {
    it('submit() on a configuration step calls onSubmit and advances on success', async () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
      ];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);
      const mockOnSubmit = jest.fn().mockResolvedValue(undefined);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      act(() => {
        result.current.registerStep({
          valid: true,
          onSubmit: mockOnSubmit,
        });
      });

      await act(async () => {
        result.current.submit();
      });

      expect(mockOnSubmit).toHaveBeenCalledTimes(1);

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(1);
      });
    });

    it('if onSubmit rejects and no onError is registered, state === issueOccurred and errorMessage is set', async () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);
      const mockOnSubmit = jest.fn().mockRejectedValue(new Error('boom'));

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      act(() => {
        result.current.registerStep({
          valid: true,
          onSubmit: mockOnSubmit,
        });
      });

      await act(async () => {
        result.current.submit();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('issueOccurred');
        expect(result.current.errorMessage).toBe('boom');
      });
    });

    it('if onSubmit rejects and onError is registered, chrome delegates to onError and stays in stepReady (NEXUS-53556 recoverable-error contract)', async () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);
      const rejectionCause = new Error('policy: password too weak');
      const mockOnSubmit = jest.fn().mockRejectedValue(rejectionCause);
      const mockOnError = jest.fn();

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      act(() => {
        result.current.registerStep({
          valid: true,
          onSubmit: mockOnSubmit,
          onError: mockOnError,
        });
      });

      await act(async () => {
        result.current.submit();
      });

      await waitFor(() => {
        expect(mockOnError).toHaveBeenCalledTimes(1);
      });

      expect(mockOnError).toHaveBeenCalledWith(rejectionCause);
      expect(result.current.state).toBe('stepReady');
      expect(result.current.currentIndex).toBe(0);
      expect(result.current.errorMessage).toBeNull();
    });

    it('if onSubmit resolves and onError is registered, chrome still advances the step and does not call onError', async () => {
      const steps: OnboardingStep[] = [
        { type: 'admin-password' },
        { type: 'anonymous-access' },
      ];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);
      const mockOnSubmit = jest.fn().mockResolvedValue(undefined);
      const mockOnError = jest.fn();

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      act(() => {
        result.current.registerStep({
          valid: true,
          onSubmit: mockOnSubmit,
          onError: mockOnError,
        });
      });

      await act(async () => {
        result.current.submit();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(1);
      });

      expect(mockOnError).not.toHaveBeenCalled();
    });

    it('if submit() is called with no registration, state becomes issueOccurred with No step handler registered', async () => {
      const steps: OnboardingStep[] = [{ type: 'admin-password' }];
      const mockFetchSteps = jest.fn().mockResolvedValue(steps);

      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.currentIndex).toBe(0);
      });

      // Do NOT register a step - this is the error case

      await act(async () => {
        result.current.submit();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('issueOccurred');
        expect(result.current.errorMessage).toBe('No step handler registered');
      });

      expect(consoleErrorSpy).toHaveBeenCalledWith('No step handler registered');

      consoleErrorSpy.mockRestore();
    });
  });

  describe('onDone', () => {
    it('does NOT fire onDone automatically when the machine reaches done — finish() is the sole trigger', async () => {
      // Regression guard for the auto-fire race: firing onDone on machine
      // entry to done clears onboarding.required, unmounting the wizard
      // before the user can see or click Finish. finish() must be the only
      // path that invokes onDone.
      const mockFetchSteps = jest.fn().mockResolvedValue([]);
      const onDoneSpy = jest.fn();

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps} onDone={onDoneSpy}>
          {children}
        </WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      // With empty steps, getStarted goes to setupComplete
      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('setupComplete');
      });

      // Submit goes to verifying then done
      await act(async () => {
        result.current.submit();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('done');
      });

      // onDone must NOT have fired just from reaching done
      expect(onDoneSpy).not.toHaveBeenCalled();

      // Now the user clicks Finish — that's what fires onDone
      act(() => {
        result.current.finish();
      });

      expect(onDoneSpy).toHaveBeenCalledTimes(1);

      // Calling finish() again is idempotent
      act(() => {
        result.current.finish();
      });

      expect(onDoneSpy).toHaveBeenCalledTimes(1);
    });

    it('finish() calls onDone immediately and is idempotent', async () => {
      const mockFetchSteps = jest.fn().mockResolvedValue([]);
      const onDoneSpy = jest.fn();

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps} onDone={onDoneSpy}>
          {children}
        </WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      // Call finish before machine is done
      act(() => {
        result.current.finish();
      });

      // onDone is called by finish()
      expect(onDoneSpy).toHaveBeenCalledTimes(1);

      // Call finish again - should not fire again
      act(() => {
        result.current.finish();
      });

      expect(onDoneSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('isCurrentStepValid on special states', () => {
    it('isCurrentStepValid is true on setupComplete', async () => {
      const mockFetchSteps = jest.fn().mockResolvedValue([]);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('setupComplete');
        expect(result.current.isCurrentStepValid).toBe(true);
      });
    });

    it('isCurrentStepValid is true on verifying', async () => {
      const mockFetchSteps = jest.fn().mockResolvedValue([]);
      const verifyPromise = new Promise<void>((resolve) => setTimeout(resolve, 100));
      const mockVerify = jest.fn().mockReturnValue(verifyPromise);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps} verify={mockVerify}>
          {children}
        </WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.getStarted();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('setupComplete');
      });

      act(() => {
        result.current.submit();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('verifying');
        expect(result.current.isCurrentStepValid).toBe(true);
      });
    });

    it('isCurrentStepValid is true on issueOccurred', async () => {
      const mockFetchSteps = jest.fn().mockRejectedValue(new Error('fetch failed'));

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>
          {children}
        </WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('issueOccurred');
        expect(result.current.errorMessage).toBe('Error: fetch failed');
        expect(result.current.isCurrentStepValid).toBe(true);
      });
    });
  });

  describe('skip', () => {
    it('skip() from issueOccurred reaches skipped so users can escape a locked error state', async () => {
      const mockFetchSteps = jest.fn().mockRejectedValue(new Error('network is down'));
      const onDoneSpy = jest.fn();

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps} onDone={onDoneSpy}>
          {children}
        </WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      // fetchSteps rejection lands the machine in issueOccurred
      await waitFor(() => {
        expect(result.current.state).toBe('issueOccurred');
        expect(result.current.errorMessage).toBe('Error: network is down');
      });

      // User dismisses via the Skip button
      act(() => {
        result.current.skip();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('skipped');
      });

      // Reaching skipped alone must not fire onDone — Finish click is the trigger
      expect(onDoneSpy).not.toHaveBeenCalled();

      act(() => {
        result.current.finish();
      });

      expect(onDoneSpy).toHaveBeenCalledTimes(1);
    });

    it('sets onboarding.dismissed flag in sessionStorage on skip from issueOccurred', async () => {
      sessionStorage.clear();

      const mockFetchSteps = jest.fn().mockRejectedValue(new Error('fetch failed'));

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('issueOccurred');
      });

      act(() => {
        result.current.skip();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('skipped');
      });

      expect(sessionStorage.getItem('onboarding.dismissed')).toBe('true');

      sessionStorage.clear();
    });

    it('does NOT set onboarding.dismissed flag when skipping from stepReady', async () => {
      sessionStorage.clear();

      const mockFetchSteps = jest.fn().mockResolvedValue([{ type: 'admin-password' }]);

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('stepReady');
      });

      act(() => {
        result.current.skip();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('skipped');
      });

      expect(sessionStorage.getItem('onboarding.dismissed')).toBeNull();

      sessionStorage.clear();
    });

    it('still dispatches SKIP when sessionStorage.setItem throws', async () => {
      sessionStorage.clear();
      const setItemSpy = jest
        .spyOn(Storage.prototype, 'setItem')
        .mockImplementation(() => {
          throw new Error('QuotaExceededError');
        });

      const mockFetchSteps = jest.fn().mockRejectedValue(new Error('fetch failed'));

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <WizardProvider fetchSteps={mockFetchSteps}>{children}</WizardProvider>
      );

      const { result } = renderHook(() => useWizard(), { wrapper });

      await waitFor(() => {
        expect(result.current.state).toBe('issueOccurred');
      });

      act(() => {
        result.current.skip();
      });

      await waitFor(() => {
        expect(result.current.state).toBe('skipped');
      });

      setItemSpy.mockRestore();
      sessionStorage.clear();
    });
  });
});
