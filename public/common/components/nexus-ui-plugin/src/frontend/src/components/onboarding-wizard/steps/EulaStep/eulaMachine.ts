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

import UIStrings from '../../UIStrings';

/**
 * Kind of the last error captured in context. Consumers use this to decide
 * whether "retry" means re-fetching the disclaimer (fetch) or resubmitting
 * the acceptance (submit). Null when there is no error to display.
 */
export type EulaErrorKind = 'fetch' | 'submit' | null;

export interface EulaContext {
  accepted: boolean;
  disclaimer: string;
  loading: boolean;
  errorMessage: string | null;
  errorKind: EulaErrorKind;
}

export type EulaEvent =
  | { type: 'ACCEPT'; checked: boolean }
  | { type: 'FETCH_SUCCESS'; disclaimer: string }
  | { type: 'FETCH_ERROR'; error: string }
  | { type: 'SUBMIT' }
  | { type: 'RETRY' };

export type EulaStateValue = 'idle' | 'loading' | 'submitting' | 'success' | 'error';

const initialContext: EulaContext = {
  accepted: false,
  disclaimer: '',
  loading: true,
  errorMessage: null,
  errorKind: null,
};

/**
 * isValid - pure selector for client-side validation gating.
 *
 * The user must have checked the acceptance box AND we must have a
 * disclaimer to POST back. The disclaimer requirement blocks the
 * FETCH_ERROR -> ACCEPT -> SUBMIT path where an empty disclaimer would
 * otherwise be sent to the acceptance endpoint.
 */
export function isValid(context: Pick<EulaContext, 'accepted' | 'disclaimer'>): boolean {
  return context.accepted && context.disclaimer !== '';
}

const setAcceptance = assign<EulaContext, EulaEvent>({
  accepted: (_, event) => (event as { type: 'ACCEPT'; checked: boolean }).checked,
});

const setDisclaimer = assign<EulaContext, EulaEvent>({
  disclaimer: (_, event) => (event as { type: 'FETCH_SUCCESS'; disclaimer: string }).disclaimer,
  loading: false,
  errorMessage: null,
  errorKind: null,
});

const setLoadingError = assign<EulaContext, EulaEvent>({
  errorMessage: (_, event) => (event as { type: 'FETCH_ERROR'; error: string }).error,
  loading: false,
  errorKind: 'fetch',
});

const clearErrorMessage = assign<EulaContext, EulaEvent>({
  errorMessage: (_) => null,
  errorKind: (_) => null,
});

interface DoneInvokeErrorEvent {
  type: string;
  data?: unknown;
}

const captureSubmitError = assign<EulaContext, EulaEvent>({
  errorMessage: (_, event) => {
    const err = (event as unknown as DoneInvokeErrorEvent).data;
    if (err instanceof Error && err.message) {
      return err.message;
    }
    return UIStrings.ONBOARDING_WIZARD.COMMUNITY_EULA.SUBMISSION_ERROR;
  },
  errorKind: (_) => 'submit',
});

/**
 * Create the EULA acceptance step machine.
 *
 * States:
 *   idle       - initial state, waiting for user to check the box and submit
 *   loading    - fetching the disclaimer from the backend
 *   submitting - sending acceptance to the backend
 *   success    - final state after successful submission
 *   error      - recoverable error state (inline error with retry)
 *
 * The machine coordinates:
 * - Initial fetch of disclaimer from GET endpoint
 * - Checkbox state (accepted boolean)
 * - Submit action that POSTs to backend
 * - Error handling with inline display and retry
 *
 * @param submitEula - Service function that performs the actual submission
 */
export function createEulaMachine(
  submitEula?: (context: EulaContext) => Promise<void>
): StateMachine<EulaContext, any, EulaEvent> {
  return createMachine<EulaContext, EulaEvent>(
    {
      id: 'eula',
      initial: 'loading',
      context: initialContext,
      states: {
        loading: {
          on: {
            FETCH_SUCCESS: {
              target: 'idle',
              actions: setDisclaimer,
            },
            FETCH_ERROR: {
              target: 'error',
              actions: setLoadingError,
            },
          },
        },
        idle: {
          on: {
            ACCEPT: { actions: setAcceptance },
            SUBMIT: {
              target: 'submitting',
              cond: 'isValid',
              actions: clearErrorMessage,
            },
          },
        },
        submitting: {
          invoke: {
            id: 'submitEula',
            src: 'submitEulaService',
            onDone: {
              target: 'success',
            },
            onError: {
              target: 'error',
              actions: captureSubmitError,
            },
          },
        },
        success: {
          type: 'final',
        },
        error: {
          on: {
            ACCEPT: { actions: setAcceptance },
            SUBMIT: {
              target: 'submitting',
              cond: 'isValid',
              actions: clearErrorMessage,
            },
            RETRY: {
              target: 'loading',
              actions: assign({
                loading: true,
                errorMessage: null,
                errorKind: null,
              }),
            },
          },
        },
      },
    },
    {
      guards: {
        isValid: (context) => isValid(context),
      },
      services: {
        submitEulaService: submitEula || (() => Promise.resolve()),
      },
    }
  );
}
