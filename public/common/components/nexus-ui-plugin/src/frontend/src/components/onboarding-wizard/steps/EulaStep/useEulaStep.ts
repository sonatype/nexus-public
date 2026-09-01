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

import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';

import UIStrings from '../../UIStrings';
import { useWizard } from '../../useWizard';
import { useEulaApi, type EulaStatus } from '../../../preview/pages/settings/security/eula/useEulaApi';
import { createEulaMachine, isValid, type EulaContext, type EulaErrorKind } from './eulaMachine';

/**
 * AC #16: hold advancement for this many ms after the POST succeeds so the
 * user sees a brief success indicator before the wizard advances. Matches
 * SUCCESS_INDICATOR_DELAY_MS in changePasswordMachine.ts so the two wizard
 * steps feel consistent.
 */
export const SUCCESS_INDICATOR_MS = 500;

interface SubmitResolvers {
  resolve: () => void;
  reject: (error: unknown) => void;
}

export interface UseEulaStepResult {
  accepted: boolean;
  loading: boolean;
  success: boolean;
  error: string | null;
  /**
   * When `error` is non-null, indicates whether the failure came from the
   * initial disclaimer fetch or from a submission attempt. Consumers use
   * this to decide whether to surface an explicit "Retry" button (fetch
   * errors) or let the wizard's Next button drive re-submission.
   */
  errorKind: EulaErrorKind;
  onAcceptChange: (checked: boolean) => void;
  onRetry: () => void;
}

/**
 * useEulaStep - custom hook for the EULA acceptance step.
 *
 * Manages the EULA lifecycle:
 * 1. Fetches the disclaimer from the backend on mount
 * 2. Tracks checkbox state for form validity
 * 3. Submits acceptance via POST when the wizard advances
 * 4. Handles errors inline with retry capability
 *
 * Uses the onError hook from the wizard to prevent transition
 * to issueOccurred state, allowing inline error recovery.
 */
export function useEulaStep(): UseEulaStepResult {
  const { registerStep, submit: wizardSubmit } = useWizard();
  const { fetchEulaStatus, acceptEula } = useEulaApi();

  const submitResolversRef = useRef<SubmitResolvers | null>(null);

  // Create the submit service that will be passed to the machine
  const submitEulaService = useCallback(async (context: EulaContext) => {
    await acceptEula(context.disclaimer);
  }, [acceptEula]);

  // Create the machine with the submit service
  const machine = useMemo(
    () => createEulaMachine(submitEulaService),
    [submitEulaService]
  );
  const [state, send] = useMachine(machine);

  const { accepted, errorMessage, errorKind } = state.context;
  const isLoading = state.matches('loading');
  const isSubmitting = state.matches('submitting');
  const isSuccess = state.matches('success');
  const isErrorState = state.matches('error');
  const isValidState = isValid(state.context);

  // Tracks whether the currently mounted component is still interested in
  // the outcome of an in-flight fetch. Guards against a stale response
  // updating the machine after unmount (or superseded by a newer retry).
  const activeFetchRef = useRef<{ cancelled: boolean } | null>(null);

  const fetchDisclaimer = useCallback(async () => {
    if (activeFetchRef.current) {
      activeFetchRef.current.cancelled = true;
    }
    const token = { cancelled: false };
    activeFetchRef.current = token;

    try {
      const status: EulaStatus = await fetchEulaStatus();
      if (!token.cancelled) {
        send({ type: 'FETCH_SUCCESS', disclaimer: status.disclaimer });
      }
    } catch (error) {
      if (!token.cancelled) {
        const message =
          error instanceof Error
            ? error.message
            : UIStrings.ONBOARDING_WIZARD.COMMUNITY_EULA.LOADING_ERROR;
        send({ type: 'FETCH_ERROR', error: message });
      }
    }
  }, [fetchEulaStatus, send]);

  useEffect(() => {
    fetchDisclaimer();
    return () => {
      if (activeFetchRef.current) {
        activeFetchRef.current.cancelled = true;
      }
    };
  }, [fetchDisclaimer]);

  // Bridge the machine success/error states to the wizard chrome's Promise contract.
  // AC #16: on success we render the success callout for SUCCESS_INDICATOR_MS
  // before resolving so the wizard advances after the user sees confirmation.
  useEffect(() => {
    const resolvers = submitResolversRef.current;
    if (!resolvers) return;

    if (isSuccess) {
      submitResolversRef.current = null;
      const timeoutId = setTimeout(() => {
        resolvers.resolve();
      }, SUCCESS_INDICATOR_MS);
      return () => clearTimeout(timeoutId);
    }
    if (isErrorState) {
      submitResolversRef.current = null;
      resolvers.reject(new Error(state.context.errorMessage ?? 'submit failed'));
    }
    return undefined;
  }, [isSuccess, isErrorState, state.context.errorMessage]);

  const onWizardSubmit = useCallback(
    () =>
      new Promise<void>((resolve, reject) => {
        submitResolversRef.current = { resolve, reject };
        send({ type: 'SUBMIT' });
      }),
    [send]
  );

  const onError = useCallback(() => {
    // Empty on purpose: recoverable-error contract.
    // The machine has already captured the display message in context
    // and the view renders the banner from state.context.errorMessage.
    // This registration exists so the wizard chrome stays in stepReady
    // instead of terminating via STEP_FAILED / issueOccurred.
  }, []);

  // Register the step with the wizard
  // Include isSubmitting in validity to prevent double-submission
  useEffect(() => {
    registerStep({
      valid: isValidState && !isSubmitting,
      onSubmit: onWizardSubmit,
      onError,
    });
  }, [registerStep, isValidState, isSubmitting, onWizardSubmit, onError]);

  const onAcceptChange = useCallback((checked: boolean) => {
    send({ type: 'ACCEPT', checked });
  }, [send]);

  const onRetry = useCallback(() => {
    send({ type: 'RETRY' });
    fetchDisclaimer();
  }, [send, fetchDisclaimer]);

  return {
    accepted,
    loading: isLoading,
    success: isSuccess,
    error: isErrorState ? errorMessage : null,
    errorKind: isErrorState ? errorKind : null,
    onAcceptChange,
    onRetry,
  };
}
