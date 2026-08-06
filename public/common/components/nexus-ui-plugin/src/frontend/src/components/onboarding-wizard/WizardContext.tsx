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

import React, { createContext, useCallback, useMemo, useRef, useState } from 'react';
import { useMachine } from '@xstate/react';

import { createWizardMachine } from './wizardMachine';
import type { OnboardingStep, WizardStateValue } from './types';

export interface StepRegistration {
  valid: boolean;
  onSubmit: () => void | Promise<void>;
  /**
   * Optional recoverable-error hook (NEXUS-53556). When provided, the wizard
   * chrome delegates onSubmit rejections to this callback and stays in the
   * current step instead of transitioning the wizard to the terminal
   * issueOccurred state. Steps that own an inline error surface with a Next-to-
   * retry contract (per acceptance criteria of change-admin-password et al.)
   * supply this. Steps that omit it keep the default STEP_FAILED behavior.
   */
  onError?: (error: unknown) => void;
}

export interface UseWizardResult {
  state: WizardStateValue;
  steps: OnboardingStep[];
  currentIndex: number;
  currentStep: OnboardingStep | null;
  errorMessage: string | null;
  isCurrentStepValid: boolean;
  registerStep: (registration: StepRegistration) => void;
  getStarted: () => void;
  submit: () => void;
  skip: () => void;
  finish: () => void;
}

export interface WizardProviderProps {
  fetchSteps: () => Promise<OnboardingStep[]>;
  verify?: () => Promise<void>;
  onDone?: () => void;
  children: React.ReactNode;
}

export const WizardContext = createContext<UseWizardResult | null>(null);

export function WizardProvider({
  fetchSteps,
  verify,
  onDone,
  children,
}: WizardProviderProps): JSX.Element {
  const machine = useMemo(
    () => createWizardMachine({ fetchSteps, verify }),
    [fetchSteps, verify]
  );

  const [snapshot, send] = useMachine(machine);

  const [stepRegistration, setStepRegistration] = useState<StepRegistration | null>(null);
  const onDoneFiredRef = useRef(false);

  const state = snapshot.value as WizardStateValue;
  const { steps, currentIndex, errorMessage } = snapshot.context;

  const currentStep: OnboardingStep | null =
    currentIndex >= 0 && currentIndex < steps.length ? steps[currentIndex] : null;

  const isCurrentStepValid = useMemo(() => {
    if (currentIndex === -1) {
      return true;
    }
    if (
      state === 'setupComplete' ||
      state === 'done' ||
      state === 'skipped' ||
      state === 'issueOccurred' ||
      state === 'verifying'
    ) {
      return true;
    }
    return stepRegistration?.valid ?? false;
  }, [currentIndex, state, stepRegistration?.valid]);

  const registerStep = useCallback((registration: StepRegistration) => {
    setStepRegistration(registration);
  }, []);

  const getStarted = useCallback(() => {
    send({ type: 'GET_STARTED' });
  }, [send]);

  const submit = useCallback(() => {
    if (state === 'setupComplete') {
      send({ type: 'SUBMIT' });
      return;
    }

    if (state === 'issueOccurred') {
      // No retry path from issueOccurred in the current machine design
      return;
    }

    // Configuration step (stepReady state, currentIndex >= 0)
    if (state === 'stepReady' && currentIndex >= 0) {
      if (!stepRegistration) {
        console.error('No step handler registered');
        send({ type: 'STEP_FAILED', error: 'No step handler registered' });
        return;
      }

      const registration = stepRegistration;
      Promise.resolve()
        .then(() => registration.onSubmit())
        .then(() => {
          send({ type: 'STEP_ADVANCED' });
        })
        .catch((error) => {
          if (registration.onError) {
            registration.onError(error);
            return;
          }
          const message = error instanceof Error ? error.message : String(error);
          send({ type: 'STEP_FAILED', error: message });
        });
    }
  }, [state, currentIndex, stepRegistration, send]);

  const skip = useCallback(() => {
    send({ type: 'SKIP' });
  }, [send]);

  // onDone is invoked exclusively by finish() (idempotent via onDoneFiredRef).
  // We intentionally do NOT fire onDone on machine entry into done/skipped —
  // doing so races with the Finish button: onDone clears onboarding.required,
  // OnboardingWizardMount's gate flips false, and the Setup Complete screen
  // unmounts before the user can click Finish. Keeping the click as the sole
  // trigger gives a single deterministic close path from every terminal state.
  const finish = useCallback(() => {
    if (!onDoneFiredRef.current && onDone) {
      onDoneFiredRef.current = true;
      onDone();
    }
  }, [onDone]);

  const value: UseWizardResult = useMemo(
    () => ({
      state,
      steps,
      currentIndex,
      currentStep,
      errorMessage,
      isCurrentStepValid,
      registerStep,
      getStarted,
      submit,
      skip,
      finish,
    }),
    [
      state,
      steps,
      currentIndex,
      currentStep,
      errorMessage,
      isCurrentStepValid,
      registerStep,
      getStarted,
      submit,
      skip,
      finish,
    ]
  );

  return <WizardContext.Provider value={value}>{children}</WizardContext.Provider>;
}
