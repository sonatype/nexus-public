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

import { assign, createMachine, type StateMachine, type StateSchema } from 'xstate';

import type {
  OnboardingStep,
  WizardErrorKind,
  WizardMachineContext,
  WizardEvent,
} from './types';

interface WizardStateSchema extends StateSchema<WizardMachineContext> {
  states: {
    loading: Record<string, never>;
    stepReady: Record<string, never>;
    setupComplete: Record<string, never>;
    verifying: Record<string, never>;
    done: Record<string, never>;
    issueOccurred: Record<string, never>;
    skipped: Record<string, never>;
  };
}

export interface WizardMachineServices {
  fetchSteps: () => Promise<OnboardingStep[]>;
  verify?: () => Promise<void>;
}

const initialContext: WizardMachineContext = {
  steps: [],
  currentIndex: -1,
  errorMessage: null,
  errorKind: null,
};

// Identifies "the API says onboarding isn't finished" so the UI can prompt the
// user to re-finalize choices instead of showing a transport-failure message.
export const ONBOARDING_STILL_REQUIRED_NAME = 'OnboardingStillRequired';

// Ceiling for the verify request before the machine falls back to setupComplete.
// Prevents the user getting stranded on the verifying screen if the request never
// resolves or rejects (silently-dropped TCP, hung proxy, etc.).
export const VERIFY_TIMEOUT_MS = 30_000;

function classifyVerifyError(err: unknown): WizardErrorKind {
  if (err && typeof err === 'object' && (err as { name?: string }).name === ONBOARDING_STILL_REQUIRED_NAME) {
    return 'stillRequired';
  }
  return 'network';
}

export function createWizardMachine(
  services: WizardMachineServices,
): StateMachine<WizardMachineContext, WizardStateSchema, WizardEvent> {
  const verifyService = services.verify ?? (() => Promise.resolve());

  return createMachine<WizardMachineContext, WizardEvent>(
    {
      id: 'onboardingWizard',
      initial: 'loading',
      context: initialContext,
      states: {
        loading: {
          invoke: {
            src: 'fetchSteps',
            onDone: {
              target: 'stepReady',
              actions: assign((_, event) => ({
                steps: event.data,
                currentIndex: -1,
              })),
            },
            onError: {
              target: 'issueOccurred',
              actions: assign((_, event) => ({
                errorMessage: String(event.data),
              })),
            },
          },
        },
        stepReady: {
          on: {
            GET_STARTED: [
              {
                cond: 'noSteps',
                target: 'setupComplete',
                actions: assign({ currentIndex: 0 }),
              },
              {
                target: 'stepReady',
                actions: assign({ currentIndex: 0 }),
              },
            ],
            STEP_ADVANCED: [
              {
                cond: 'isLastStep',
                target: 'setupComplete',
                actions: assign((context) => ({
                  currentIndex: context.currentIndex + 1,
                })),
              },
              {
                target: 'stepReady',
                actions: assign((context) => ({
                  currentIndex: context.currentIndex + 1,
                })),
              },
            ],
            STEP_FAILED: {
              target: 'issueOccurred',
              actions: assign((_, event) => ({
                errorMessage: event.error,
              })),
            },
            SKIP: 'skipped',
          },
        },
        setupComplete: {
          on: {
            // Clear any prior verify failure so the retry starts from a clean slate.
            SUBMIT: {
              target: 'verifying',
              actions: assign({ errorMessage: null, errorKind: null }),
            },
          },
        },
        verifying: {
          invoke: {
            src: 'verify',
            onDone: 'done',
            onError: {
              target: 'setupComplete',
              actions: assign((_, event) => ({
                errorMessage: String(event.data),
                errorKind: classifyVerifyError(event.data),
              })),
            },
          },
          after: {
            [VERIFY_TIMEOUT_MS]: {
              target: 'setupComplete',
              actions: assign({
                errorMessage: 'Verification timed out',
                errorKind: 'network',
              }),
            },
          },
        },
        done: {
          type: 'final',
        },
        issueOccurred: {
          on: {
            SKIP: 'skipped',
          },
        },
        skipped: {
          type: 'final',
        },
      },
    },
    {
      services: {
        fetchSteps: services.fetchSteps,
        verify: verifyService,
      },
      guards: {
        noSteps: (context) => context.steps.length === 0,
        isLastStep: (context) => {
          const nextIndex = context.currentIndex + 1;
          return nextIndex >= context.steps.length;
        },
      },
    },
  );
}
