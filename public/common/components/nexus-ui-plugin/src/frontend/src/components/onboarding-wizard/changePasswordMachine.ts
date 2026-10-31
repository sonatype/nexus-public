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

import { assign, createMachine, type StateMachine } from 'xstate';

import { parseApiError } from '../../interface/api/error-handler';
import { restClient } from '../../interface/api/rest-client';
import UIStrings from './UIStrings';

export const CHANGE_ADMIN_PASSWORD_ENDPOINT =
  '/service/rest/internal/ui/onboarding/change-admin-password';

export const SUCCESS_INDICATOR_DELAY_MS = 500;

const PLAIN_TEXT_HEADER = { headers: { 'Content-Type': 'text/plain' } };

export interface ChangePasswordContext {
  password: string;
  confirm: string;
  errorMessage: string | null;
}

export type ChangePasswordEvent =
  | { type: 'UPDATE_PASSWORD'; value: string }
  | { type: 'UPDATE_CONFIRM'; value: string }
  | { type: 'SUBMIT' };

export type ChangePasswordStateValue =
  | 'idle'
  | 'submitting'
  | 'showingSuccess'
  | 'success'
  | 'error';

const initialContext: ChangePasswordContext = {
  password: '',
  confirm: '',
  errorMessage: null,
};

/**
 * isFormValid — pure selector for client-side validation gating (AC #4, #5).
 * Both fields must be non-empty and equal. Real policy enforcement stays on the backend.
 */
export function isFormValid(
  context: Pick<ChangePasswordContext, 'password' | 'confirm'>
): boolean {
  return (
    context.password.length > 0 &&
    context.confirm.length > 0 &&
    context.password === context.confirm
  );
}

/**
 * shouldShowMismatchError — pure selector for the inline confirm-field error (AC #6).
 * Only shown once the user has typed something into confirm; hiding until then avoids
 * a red flash on the first character typed into the password field.
 */
export function shouldShowMismatchError(
  context: Pick<ChangePasswordContext, 'password' | 'confirm'>
): boolean {
  return context.confirm.length > 0 && context.password !== context.confirm;
}

/**
 * submitPasswordService — the invoked service that performs the PUT for the
 * change-admin-password endpoint. Extracted at module scope so tests can mock
 * `restClient.put` via jest and override the service via `.withConfig` when
 * driving the machine through `interpret`.
 *
 * On failure, wraps the underlying axios error in a plain `Error` whose message
 * is the display string. The wizard chrome only needs a rejection to route into
 * the step's `onError` delegate; the banner itself is rendered from the
 * machine's `context.errorMessage`.
 */
export async function submitPasswordService(context: ChangePasswordContext): Promise<void> {
  try {
    await restClient.put(
      CHANGE_ADMIN_PASSWORD_ENDPOINT,
      context.password,
      PLAIN_TEXT_HEADER
    );
  } catch (error) {
    const parsed = parseApiError(error);
    // AC #10: distinguish network from policy errors. parseApiError returns
    // status 0 for both timeouts and "no response" cases; map both to the
    // generic fallback so the banner does not leak backend implementation
    // details when the request never reached the server.
    const displayMessage =
      parsed.status === 0
        ? UIStrings.ONBOARDING_WIZARD.CHANGE_ADMIN_PASSWORD.NETWORK_ERROR_FALLBACK
        : parsed.message;
    const wrapped = new Error(displayMessage);
    wrapped.name = 'ChangePasswordSubmitError';
    throw wrapped;
  }
}

type UpdatePasswordEvent = Extract<ChangePasswordEvent, { type: 'UPDATE_PASSWORD' }>;
type UpdateConfirmEvent = Extract<ChangePasswordEvent, { type: 'UPDATE_CONFIRM' }>;

const setPassword = assign<ChangePasswordContext, ChangePasswordEvent>({
  password: (_, event) => (event as UpdatePasswordEvent).value,
});

const setConfirm = assign<ChangePasswordContext, ChangePasswordEvent>({
  confirm: (_, event) => (event as UpdateConfirmEvent).value,
});

const clearErrorMessage = assign<ChangePasswordContext, ChangePasswordEvent>({
  errorMessage: (_) => null,
});

// XState v4 emits `done.invoke.<id>` / `error.platform.<id>` events for invoked
// promises; the rejected value is on `event.data`. `assign` here is typed with
// `any` for the event to avoid coupling to xstate's internal invoke-event names.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const captureError = assign<ChangePasswordContext, any>({
  errorMessage: (_, event) => {
    const err = event.data;
    if (err instanceof Error && err.message) return err.message;
    return UIStrings.ONBOARDING_WIZARD.CHANGE_ADMIN_PASSWORD.NETWORK_ERROR_FALLBACK;
  },
});

// Defense-in-depth: wipe the password/confirm fields from machine context on
// entry to `showingSuccess`. The React component unmounts moments later when
// the wizard chrome advances, but clearing on the success transition ensures
// the password string is not retained in-memory any longer than strictly
// necessary — including during the 500ms indicator window.
const clearPasswordFields = assign<ChangePasswordContext, ChangePasswordEvent>({
  password: (_) => '',
  confirm: (_) => '',
});

/**
 * Create the change-admin-password step machine.
 *
 * States:
 *   idle           — initial. Accepts field updates and SUBMIT (if valid).
 *   submitting     — invokes `submitPassword` (PUT). Transitions on invoke outcome:
 *                    onDone → showingSuccess (also clears password fields);
 *                    onError → error (captures the display message).
 *   showingSuccess — 500ms window that renders a green success indicator before
 *                    handing control back to the wizard chrome. AC #8.
 *   success        — final; the wizard chrome advances the step after this.
 *   error          — retryable; same accepted events as idle. SUBMIT clears
 *                    the previous errorMessage before re-invoking so the top
 *                    banner disappears while the retry is in flight.
 *
 * The HTTP call is owned by the machine (invoked service), not the view. The
 * view bridges the invocation lifecycle to the wizard chrome's Promise contract
 * via a resolver stored in a ref (see ChangePasswordStep.tsx).
 */
export function createChangePasswordMachine(): StateMachine<
  ChangePasswordContext,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  any,
  ChangePasswordEvent
> {
  return createMachine<ChangePasswordContext, ChangePasswordEvent>(
    {
      id: 'changePassword',
      initial: 'idle',
      context: initialContext,
      states: {
        idle: {
          on: {
            UPDATE_PASSWORD: { actions: setPassword },
            UPDATE_CONFIRM: { actions: setConfirm },
            SUBMIT: {
              target: 'submitting',
              cond: 'isFormValid',
              actions: clearErrorMessage,
            },
          },
        },
        submitting: {
          invoke: {
            id: 'submitPassword',
            src: 'submitPassword',
            onDone: {
              target: 'showingSuccess',
              actions: clearPasswordFields,
            },
            onError: {
              target: 'error',
              actions: captureError,
            },
          },
        },
        showingSuccess: {
          after: {
            SUCCESS_INDICATOR_DELAY: 'success',
          },
        },
        success: {
          type: 'final',
        },
        error: {
          on: {
            UPDATE_PASSWORD: { actions: setPassword },
            UPDATE_CONFIRM: { actions: setConfirm },
            SUBMIT: {
              target: 'submitting',
              cond: 'isFormValid',
              actions: clearErrorMessage,
            },
          },
        },
      },
    },
    {
      services: {
        submitPassword: submitPasswordService,
      },
      guards: {
        isFormValid: (context) => isFormValid(context),
      },
      delays: {
        SUCCESS_INDICATOR_DELAY: SUCCESS_INDICATOR_DELAY_MS,
      },
    }
  );
}
