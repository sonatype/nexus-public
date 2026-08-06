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

import React, { useEffect } from 'react';
import { AlertDialog, Button, Callout, Text, VisuallyHidden } from '@radix-ui/themes';

import Welcome from './Welcome';
import { useWizard } from './useWizard';
import { stepRegistry } from './stepRegistry';
import UIStrings from './UIStrings';

import './Wizard.scss';

const WIZARD_CONTENT_HEIGHT = 560; // px — fixed content height per acceptance criteria

// Escape/outside-click are suppressed to keep the wizard blocking-modal — defined
// at module scope so identity is stable across renders (Radix subscribes them to
// native event listeners).
const preventEvent = (event: Event) => event.preventDefault();

/**
 * UnknownStepBody - Fallback component for unregistered step types.
 * Registers an invalid step to keep the action button disabled.
 */
function UnknownStepBody(): JSX.Element {
  const { registerStep } = useWizard();
  const { UNKNOWN_STEP } = UIStrings.ONBOARDING_WIZARD;

  useEffect(() => {
    registerStep({
      valid: false,
      onSubmit: async () => {
        throw new Error(UNKNOWN_STEP.MESSAGE);
      },
    });
  }, [registerStep, UNKNOWN_STEP.MESSAGE]);

  return (
    <div data-testid="onboarding-wizard__unknown-step">
      {UNKNOWN_STEP.MESSAGE}
    </div>
  );
}

/**
 * ProgressStepper - Visual indicator for wizard step progress.
 * Non-clickable step indicators showing completed, active, and future steps.
 */
function ProgressStepper({
  steps,
  currentIndex,
}: {
  steps: Array<{ type: string }>;
  currentIndex: number;
}): JSX.Element {
  return (
    <div className="onboarding-wizard__stepper" data-testid="onboarding-wizard__stepper">
      {steps.map((_, i) => {
        const isCompleted = i < currentIndex;
        const isActive = i === currentIndex;
        return (
          <div
            key={i}
            className={
              'onboarding-wizard__stepper-item' +
              (isActive ? ' onboarding-wizard__stepper-item--active' : '') +
              (isCompleted ? ' onboarding-wizard__stepper-item--completed' : '')
            }
            data-testid="onboarding-wizard__stepper-item"
            data-index={i}
            data-active={isActive ? 'true' : undefined}
            data-completed={isCompleted ? 'true' : undefined}
          >
            {isCompleted ? '✓' : i + 1}
          </div>
        );
      })}
    </div>
  );
}

/**
 * SetupCompleteBody - Renders the setup complete message.
 * Used for setupComplete, done, and skipped states.
 */
function SetupCompleteBody(): JSX.Element {
  const { SETUP_COMPLETE } = UIStrings.ONBOARDING_WIZARD;

  return (
    <div className="onboarding-wizard__setup-complete">
      <h2 className="onboarding-wizard__setup-complete-title">
        {SETUP_COMPLETE.TITLE}
      </h2>
      <p className="onboarding-wizard__setup-complete-description">
        {SETUP_COMPLETE.DESCRIPTION}
      </p>
    </div>
  );
}

/**
 * computeAction - Determines the action button label and click handler
 * based on the current wizard state.
 */
function computeAction(
  state: string,
  currentIndex: number,
  steps: Array<{ type: string }>,
  wizard: {
    getStarted: () => void;
    submit: () => void;
    finish: () => void;
  },
): { label: string; onClick: () => void } {
  const { ACTIONS } = UIStrings.ONBOARDING_WIZARD;

  if (currentIndex === -1 && state === 'stepReady') {
    return { label: ACTIONS.GET_STARTED, onClick: wizard.getStarted };
  }
  if (state === 'stepReady' && currentIndex >= 0) {
    const isLast = currentIndex === steps.length - 1;
    return { label: isLast ? ACTIONS.FINISH : ACTIONS.NEXT, onClick: wizard.submit };
  }
  if (state === 'setupComplete') {
    return { label: ACTIONS.FINISH, onClick: wizard.submit };
  }
  if (state === 'done' || state === 'skipped') {
    return { label: ACTIONS.FINISH, onClick: wizard.finish };
  }
  // Retry contract: submit() is currently a no-op on issueOccurred per Task 2
  // (the wizard machine has no re-fetch path from issueOccurred). The button
  // is rendered with the RETRY label but kept disabled by the caller so a
  // follow-up story can plug in the retry event without touching the chrome.
  if (state === 'issueOccurred') {
    return { label: ACTIONS.RETRY, onClick: wizard.submit };
  }
  // loading or verifying — the caller keeps the button disabled, but a
  // non-empty label ensures a screen-reader label remains if the guard ever
  // changes so the button becomes visible in these states.
  return { label: ACTIONS.NEXT, onClick: () => {} };
}

export default function Wizard(): JSX.Element {
  const wizard = useWizard();
  const { state, steps, currentIndex, errorMessage, isCurrentStepValid, skip } = wizard;

  const { ACTIONS, ARIA, ISSUE_OCCURRED, LOADING, VERIFYING } = UIStrings.ONBOARDING_WIZARD;

  // Stepper is visible only when in stepReady with currentIndex >= 0
  const showStepper = state === 'stepReady' && currentIndex >= 0;

  // Action button visibility
  const hideButtonStates = state === 'loading' || state === 'verifying';
  // Retry is disabled until the machine has a re-fetch/retry transition (see
  // computeAction's issueOccurred branch). Keeping it disabled prevents users
  // from clicking a placebo button in a blocking modal with no other exit.
  const isRetryDisabled = state === 'issueOccurred';
  const isActionEnabled = !hideButtonStates && !isRetryDisabled && isCurrentStepValid;

  const { label: actionLabel, onClick: onActionClick } = computeAction(
    state,
    currentIndex,
    steps,
    wizard,
  );

  // Render body based on state
  let body: React.ReactNode;

  if (state === 'loading') {
    body = (
      <Callout.Root color="gray">
        <Callout.Text>{LOADING.MESSAGE}</Callout.Text>
      </Callout.Root>
    );
  } else if (state === 'stepReady') {
    if (currentIndex === -1) {
      body = <Welcome />;
    } else {
      // Look up step component from registry
      const currentStep = steps[currentIndex];
      const StepComponent = currentStep ? stepRegistry[currentStep.type] : null;

      if (StepComponent) {
        body = <StepComponent />;
      } else {
        body = <UnknownStepBody />;
      }
    }
  } else if (state === 'setupComplete') {
    body = <SetupCompleteBody />;
  } else if (state === 'verifying') {
    body = (
      <div className="onboarding-wizard__verifying">
        <Text>{VERIFYING.MESSAGE}</Text>
      </div>
    );
  } else if (state === 'done' || state === 'skipped') {
    body = <SetupCompleteBody />;
  } else if (state === 'issueOccurred') {
    body = (
      <div className="onboarding-wizard__error-container">
        <h2 className="onboarding-wizard__error-title">{ISSUE_OCCURRED.TITLE}</h2>
        <Callout.Root color="red" data-testid="onboarding-wizard__error">
          <Callout.Text>
            {errorMessage ?? ISSUE_OCCURRED.FALLBACK_MESSAGE}
          </Callout.Text>
        </Callout.Root>
        <p className="onboarding-wizard__error-dismiss-help">
          {ISSUE_OCCURRED.DISMISS_HELP}
        </p>
        <div className="onboarding-wizard__error-actions">
          <Button
            data-testid="onboarding-wizard__dismiss"
            variant="soft"
            color="gray"
            onClick={skip}
          >
            {ACTIONS.DISMISS}
          </Button>
        </div>
      </div>
    );
  } else {
    // Fallback for unknown states
    body = null;
  }

  return (
    <AlertDialog.Root open>
      <AlertDialog.Content
        data-testid="onboarding-wizard__root"
        onEscapeKeyDown={preventEvent}
        onInteractOutside={preventEvent}
        className="onboarding-wizard__content"
        style={{
          minHeight: `${WIZARD_CONTENT_HEIGHT}px`,
          maxHeight: `${WIZARD_CONTENT_HEIGHT}px`,
        }}
      >
        <VisuallyHidden>
          <AlertDialog.Title>{ARIA.TITLE}</AlertDialog.Title>
        </VisuallyHidden>
        <VisuallyHidden>
          <AlertDialog.Description>{ARIA.DESCRIPTION}</AlertDialog.Description>
        </VisuallyHidden>

        <div className="onboarding-wizard__stepper-area">
          {showStepper && <ProgressStepper steps={steps} currentIndex={currentIndex} />}
        </div>

        <div data-testid="onboarding-wizard__body" className="onboarding-wizard__body">
          {body}
        </div>

        <div className="onboarding-wizard__footer">
          <Button
            data-testid="onboarding-wizard__action"
            disabled={!isActionEnabled}
            onClick={onActionClick}
          >
            {actionLabel}
          </Button>
        </div>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}
