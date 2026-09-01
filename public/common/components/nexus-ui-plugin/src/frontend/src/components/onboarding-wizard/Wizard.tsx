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
import { AlertDialog, Button, Callout, Text, Theme, VisuallyHidden } from '@radix-ui/themes';

import Welcome from './Welcome';
import { useWizard } from './useWizard';
import { stepRegistry } from './stepRegistry';
import UIStrings from './UIStrings';
import type { WizardErrorKind } from './types';

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
        const isLast = i === steps.length - 1;
        return (
          <React.Fragment key={i}>
            <div
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
            {!isLast && (
              <div
                aria-hidden="true"
                className={
                  'onboarding-wizard__stepper-connector' +
                  (isCompleted ? ' onboarding-wizard__stepper-connector--completed' : '')
                }
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

/**
 * SuccessIcon - Green checkmark icon for success state.
 */
function SuccessIcon(): JSX.Element {
  return (
    <div
      data-testid="onboarding-wizard__success-icon"
      className="onboarding-wizard__complete-icon"
    >
      <svg
        width="64"
        height="64"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <circle cx="12" cy="12" r="12" fill="var(--green-9, #10b981)" />
        <path
          d="M7 12l3 3 7-7"
          stroke="white"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  );
}

/**
 * WarningIcon - Warning icon for issue occurred state.
 */
function WarningIcon(): JSX.Element {
  return (
    <div
      data-testid="onboarding-wizard__warning-icon"
      className="onboarding-wizard__complete-icon"
    >
      <svg
        width="64"
        height="64"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <circle cx="12" cy="12" r="12" fill="var(--yellow-9, #f59e0b)" />
        <path
          d="M12 8v4m0 4h.01"
          stroke="white"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  );
}

/**
 * SetupCompleteBody - Renders the setup complete screen.
 * When errorKind is set (retry after a verify failure), it swaps to a warning
 * variant with a callout describing the failure so the user can act correctly.
 */
function SetupCompleteBody({
  errorKind,
}: {
  errorKind?: WizardErrorKind | null;
}): JSX.Element {
  const { SETUP_COMPLETE } = UIStrings.ONBOARDING_WIZARD;
  const hasError = !!errorKind;

  let calloutMessage: string | null = null;
  let calloutTestId: string | null = null;
  if (errorKind === 'stillRequired') {
    calloutMessage = SETUP_COMPLETE.STILL_REQUIRED_MESSAGE;
    calloutTestId = 'onboarding-wizard__still-required';
  } else if (errorKind === 'network') {
    calloutMessage = SETUP_COMPLETE.NETWORK_ERROR_MESSAGE;
    calloutTestId = 'onboarding-wizard__network-error';
  }

  return (
    <div className="onboarding-wizard__setup-complete">
      {hasError ? <WarningIcon /> : <SuccessIcon />}
      <h2 className="onboarding-wizard__setup-complete-title">
        {SETUP_COMPLETE.TITLE}
      </h2>
      <p className="onboarding-wizard__setup-complete-description">
        {SETUP_COMPLETE.DESCRIPTION}
      </p>
      {calloutMessage && (
        <Callout.Root
          color="red"
          data-testid={calloutTestId ?? undefined}
          style={{ marginTop: 'var(--space-4, 16px)' }}
        >
          <Callout.Text>{calloutMessage}</Callout.Text>
        </Callout.Root>
      )}
    </div>
  );
}

/**
 * IssueOccurredBody - Renders the issue occurred screen with warning icon.
 */
function IssueOccurredBody({ onDismiss }: { onDismiss: () => void }): JSX.Element {
  const { ISSUE_OCCURRED, ACTIONS } = UIStrings.ONBOARDING_WIZARD;

  return (
    <div className="onboarding-wizard__setup-complete">
      <WarningIcon />
      <h2 className="onboarding-wizard__setup-complete-title">
        {ISSUE_OCCURRED.TITLE}
      </h2>
      <p className="onboarding-wizard__setup-complete-description">
        {ISSUE_OCCURRED.MESSAGE}
      </p>
      <div className="onboarding-wizard__setup-complete-actions">
        <Button
          data-testid="onboarding-wizard__dismiss"
          variant="soft"
          color="gray"
          onClick={onDismiss}
          autoFocus
        >
          {ACTIONS.DISMISS}
        </Button>
      </div>
    </div>
  );
}

/**
 * computeAction - Determines the action button label and click handler
 * based on the current wizard state.
 *
 * Not called for states that hide the footer (loading, verifying, issueOccurred).
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
    return { label: ACTIONS.COMPLETE_SETUP, onClick: wizard.submit };
  }
  if (state === 'done' || state === 'skipped') {
    return { label: ACTIONS.FINISH, onClick: wizard.finish };
  }
  // Fallback — footer is hidden for the remaining states (loading, verifying,
  // issueOccurred). issueOccurred renders its own Dismiss button in the body.
  return { label: '', onClick: () => {} };
}

export default function Wizard(): JSX.Element {
  const wizard = useWizard();
  const { state, steps, currentIndex, errorKind, isCurrentStepValid, skip, actionButtonRef } = wizard;

  const { ARIA, LOADING, VERIFYING, SETUP_COMPLETE, ISSUE_OCCURRED } = UIStrings.ONBOARDING_WIZARD;

  // Determine aria-live content based on state. On setupComplete with an error
  // we announce the error title so screen readers don't say "Setup Complete"
  // while a warning callout is visible.
  let ariaLiveContent = '';
  if (state === 'setupComplete') {
    ariaLiveContent = errorKind ? ISSUE_OCCURRED.TITLE : SETUP_COMPLETE.TITLE;
  } else if (state === 'done' || state === 'skipped') {
    ariaLiveContent = SETUP_COMPLETE.TITLE;
  } else if (state === 'issueOccurred') {
    ariaLiveContent = ISSUE_OCCURRED.TITLE;
  }

  // Stepper is visible only when in stepReady with currentIndex >= 0
  const showStepper = state === 'stepReady' && currentIndex >= 0;

  // Action button visibility
  // issueOccurred has its own Dismiss button in the body
  const showFooter = state !== 'loading' && state !== 'verifying' && state !== 'issueOccurred';
  const isActionEnabled = isCurrentStepValid;

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
    body = <SetupCompleteBody errorKind={errorKind} />;
  } else if (state === 'verifying') {
    body = (
      <div className="onboarding-wizard__verifying">
        <Text>{VERIFYING.MESSAGE}</Text>
      </div>
    );
  } else if (state === 'done' || state === 'skipped') {
    body = <SetupCompleteBody />;
  } else if (state === 'issueOccurred') {
    body = <IssueOccurredBody onDismiss={skip} />;
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
        {/*
          Scope accent color inside the dialog portal. AlertDialog.Content
          portals into document.body, outside the app's outer green Theme
          and the blue Theme that wraps .nxrm-main-content. Without this
          wrap, wizard controls resolve --accent-* to green.
        */}
        <Theme accentColor="blue" hasBackground={false} asChild>
          <div className="onboarding-wizard__theme-scope">
            <VisuallyHidden>
              <AlertDialog.Title>{ARIA.TITLE}</AlertDialog.Title>
            </VisuallyHidden>
            <VisuallyHidden>
              <AlertDialog.Description>{ARIA.DESCRIPTION}</AlertDialog.Description>
            </VisuallyHidden>

            {/* aria-live region for screen reader announcements */}
            <div
              data-testid="onboarding-wizard__aria-live"
              role="status"
              aria-live="polite"
              className="sr-only"
            >
              {ariaLiveContent}
            </div>

            <div className="onboarding-wizard__stepper-area">
              {showStepper && <ProgressStepper steps={steps} currentIndex={currentIndex} />}
            </div>

            <div data-testid="onboarding-wizard__body" className="onboarding-wizard__body">
              {body}
            </div>

            {showFooter && (
              <div className="onboarding-wizard__footer">
                <Button
                  ref={actionButtonRef}
                  data-testid="onboarding-wizard__action"
                  disabled={!isActionEnabled}
                  onClick={onActionClick}
                  autoFocus={state === 'setupComplete' && !errorKind}
                >
                  {actionLabel}
                </Button>
              </div>
            )}
          </div>
        </Theme>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}
