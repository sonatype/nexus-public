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

import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { Callout } from '@radix-ui/themes';
import { CheckCircle2 } from 'lucide-react';
import { useMachine } from '@xstate/react';

import UIStrings from './UIStrings';
import { useWizard } from './useWizard';
import {
  createChangePasswordMachine,
  isFormValid,
  shouldShowMismatchError,
} from './changePasswordMachine';

import './ChangePasswordStep.scss';

const PASSWORD_INPUT_ID = 'onboarding-wizard__change-password-input';
const CONFIRM_INPUT_ID = 'onboarding-wizard__change-password-confirm-input';

interface SubmitResolvers {
  resolve: () => void;
  reject: (error: unknown) => void;
}

export default function ChangePasswordStep(): JSX.Element {
  const machine = useMemo(() => createChangePasswordMachine(), []);
  const [state, send] = useMachine(machine);

  const { registerStep, submit: wizardSubmit } = useWizard();

  const { password, confirm, errorMessage } = state.context;
  const { CHANGE_ADMIN_PASSWORD } = UIStrings.ONBOARDING_WIZARD;

  const passwordInputRef = useRef<HTMLInputElement | null>(null);
  const submitResolversRef = useRef<SubmitResolvers | null>(null);

  const valid = isFormValid(state.context);
  const showMismatch = shouldShowMismatchError(state.context);
  const isSubmitting = state.matches('submitting');
  const isShowingSuccess = state.matches('showingSuccess');
  const isSuccess = state.matches('success');
  const isErrorState = state.matches('error');

  // AC #11: focus the password field when the step loads. Uses an imperative
  // ref-based focus instead of the autoFocus attribute (biome noAutofocus)
  // because it lets us reason about *when* the focus happens (post-mount, once)
  // and keeps the DOM attribute clean.
  useEffect(() => {
    passwordInputRef.current?.focus();
  }, []);

  // Bridge the invoked-service machine to the wizard chrome's Promise-based
  // onSubmit contract. When state reaches the terminal `success` (after the
  // showingSuccess indicator window) we resolve, so chrome dispatches
  // STEP_ADVANCED. When state lands in `error` we reject; chrome's `.catch`
  // then invokes the registered `onError`, keeping the wizard in stepReady
  // and rendering the banner from context.errorMessage. Resolvers are
  // one-shot per SUBMIT invocation; retries install a fresh pair.
  useEffect(() => {
    const resolvers = submitResolversRef.current;
    if (!resolvers) return;
    if (isSuccess) {
      submitResolversRef.current = null;
      resolvers.resolve();
    } else if (isErrorState) {
      submitResolversRef.current = null;
      resolvers.reject(new Error(state.context.errorMessage ?? 'submit failed'));
    }
  }, [isSuccess, isErrorState]);

  const onSubmit = useCallback(
    () =>
      new Promise<void>((resolve, reject) => {
        submitResolversRef.current = { resolve, reject };
        send({ type: 'SUBMIT' });
      }),
    [send]
  );

  const onError = useCallback(() => {
    // Empty on purpose: recoverable-error contract. The machine has already
    // captured the display message in context and the view renders the banner
    // from state.context.errorMessage. This registration exists so the wizard
    // chrome stays in stepReady instead of terminating via STEP_FAILED /
    // issueOccurred (see NEXUS-53556 in WizardContext.tsx).
  }, []);

  // Include isSubmitting in the registered validity so the wizard chrome's
  // Next button is disabled while a PUT is in flight. Chrome's default
  // enable-gate is `stepRegistration.valid`; without this, a rapid
  // double-click on Next would dispatch two `onSubmit()` invocations and race
  // two PUTs.
  useEffect(() => {
    registerStep({ valid: valid && !isSubmitting, onSubmit, onError });
  }, [registerStep, valid, isSubmitting, onSubmit, onError]);

  // Native <form> submission (AC #12): browsers fire `submit` on Enter inside
  // any form input (implicit submission), so wrapping the fields in a <form>
  // gives us Enter-to-submit for free. `preventDefault` stops the default
  // full-page navigation. Guarded by isFormValid + !isSubmitting so a stray
  // Enter while invalid or mid-flight is a no-op.
  const handleFormSubmit = useCallback(
    (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (isSubmitting || !isFormValid(state.context)) return;
      wizardSubmit();
    },
    [wizardSubmit, state.context, isSubmitting]
  );

  const inputsDisabled = isSubmitting || isShowingSuccess || isSuccess;

  return (
    <div className="onboarding-wizard__change-password">
      <h2 className="onboarding-wizard__change-password-title">
        {CHANGE_ADMIN_PASSWORD.TITLE}
      </h2>
      <p className="onboarding-wizard__change-password-description">
        {CHANGE_ADMIN_PASSWORD.DESCRIPTION}
      </p>

      {isErrorState && errorMessage ? (
        <Callout.Root
          color="red"
          data-testid="onboarding-wizard__change-password-error"
        >
          <Callout.Text>{errorMessage}</Callout.Text>
        </Callout.Root>
      ) : null}

      {isShowingSuccess || isSuccess ? (
        <Callout.Root
          color="green"
          role="status"
          data-testid="onboarding-wizard__change-password-success"
        >
          <Callout.Icon>
            <CheckCircle2 size={16} />
          </Callout.Icon>
          <Callout.Text>{CHANGE_ADMIN_PASSWORD.SUCCESS_MESSAGE}</Callout.Text>
        </Callout.Root>
      ) : null}

      <form
        onSubmit={handleFormSubmit}
        noValidate
        className="onboarding-wizard__change-password-form"
        data-testid="onboarding-wizard__change-password-form"
      >
        <div className="onboarding-wizard__change-password-field">
          <label htmlFor={PASSWORD_INPUT_ID}>{CHANGE_ADMIN_PASSWORD.PASSWORD_LABEL}</label>
          <input
            id={PASSWORD_INPUT_ID}
            ref={passwordInputRef}
            type="password"
            autoComplete="new-password"
            disabled={inputsDisabled}
            value={password}
            onChange={(event) => send({ type: 'UPDATE_PASSWORD', value: event.target.value })}
            data-testid="onboarding-wizard__change-password-password-input"
          />
        </div>

        <div className="onboarding-wizard__change-password-field">
          <label htmlFor={CONFIRM_INPUT_ID}>{CHANGE_ADMIN_PASSWORD.CONFIRM_LABEL}</label>
          <input
            id={CONFIRM_INPUT_ID}
            type="password"
            autoComplete="new-password"
            disabled={inputsDisabled}
            value={confirm}
            onChange={(event) => send({ type: 'UPDATE_CONFIRM', value: event.target.value })}
            data-testid="onboarding-wizard__change-password-confirm-input"
          />
          {showMismatch ? (
            <div
              role="alert"
              className="onboarding-wizard__change-password-mismatch"
              data-testid="onboarding-wizard__change-password-mismatch"
            >
              {CHANGE_ADMIN_PASSWORD.MISMATCH_ERROR}
            </div>
          ) : null}
        </div>

        <p className="onboarding-wizard__change-password-helper">
          {CHANGE_ADMIN_PASSWORD.HELPER_TEXT}
        </p>

        {/* Backstop for implicit form submission: browsers require a submit
            control for Enter-in-a-single-input to fire the submit event
            reliably across engines. Kept out of the tab order and off-screen
            since the primary submit control is the wizard chrome's Next
            button. */}
        <button
          type="submit"
          className="onboarding-wizard__change-password-visually-hidden"
          tabIndex={-1}
          aria-hidden="true"
          data-testid="onboarding-wizard__change-password-hidden-submit"
        >
          {CHANGE_ADMIN_PASSWORD.HIDDEN_SUBMIT_LABEL}
        </button>
      </form>
    </div>
  );
}
