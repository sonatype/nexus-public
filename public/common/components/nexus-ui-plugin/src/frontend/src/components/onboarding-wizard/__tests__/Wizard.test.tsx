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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import ExtJS from '../../../interface/ExtJS';
import { restClient } from '../../../interface/api/rest-client';

// Setup mocking for ExtJS and restClient at module level
jest.mock('../../../interface/ExtJS', () => ({
  useState: jest.fn(),
  useUser: jest.fn(),
  state: jest.fn(),
}));

jest.mock('../../../interface/api/rest-client', () => ({
  restClient: {
    get: jest.fn(),
  },
}));

// Mock useWizard for unit tests
jest.mock('../useWizard');

// Import the mocked useWizard for unit tests
import { useWizard } from '../useWizard';
import Wizard from '../Wizard';

const mockedUseWizard = jest.mocked(useWizard);

const defaultMocks = {
  state: 'stepReady' as const,
  steps: [] as Array<{ type: string }>,
  currentIndex: -1,
  currentStep: null,
  errorMessage: null,
  errorKind: null,
  isCurrentStepValid: true,
  registerStep: jest.fn(),
  getStarted: jest.fn(),
  submit: jest.fn(),
  skip: jest.fn(),
  finish: jest.fn(),
  actionButtonRef: { current: null } as React.RefObject<HTMLButtonElement>,
};

describe('Wizard', () => {
  let warnSpy: jest.SpyInstance;
  let errorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseWizard.mockReturnValue(defaultMocks);
    warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    warnSpy.mockRestore();
    errorSpy.mockRestore();
  });

  const renderWithTheme = (ui: React.ReactElement) => {
    return render(<Theme>{ui}</Theme>);
  };

  describe('1. Welcome path', () => {
    it('renders Welcome content when currentIndex === -1 and state is stepReady', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByText(/Let's Get You Set Up/i)).toBeInTheDocument();
    });

    it('does not render stepper on Welcome', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
      });

      renderWithTheme(<Wizard />);

      expect(screen.queryByTestId('onboarding-wizard__stepper')).not.toBeInTheDocument();
    });

    it('shows "Get Started" button label on Welcome', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByTestId('onboarding-wizard__action')).toHaveTextContent('Get Started');
    });
  });

  describe('2. Get Started click', () => {
    it('calls getStarted when action button is clicked on Welcome', () => {
      const getStarted = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
        getStarted,
      });

      renderWithTheme(<Wizard />);

      const button = screen.getByTestId('onboarding-wizard__action');
      fireEvent.click(button);

      expect(getStarted).toHaveBeenCalledTimes(1);
      // Assert no console warnings/errors (catches Radix a11y warnings)
      expect(warnSpy).not.toHaveBeenCalled();
      expect(errorSpy).not.toHaveBeenCalled();
    });
  });

  describe('3. Stepper with 3 steps, currentIndex=1', () => {
    it('renders stepper with correct completion/active states', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        steps: [{ type: 'A' }, { type: 'B' }, { type: 'C' }],
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const stepperItems = screen.getAllByTestId('onboarding-wizard__stepper-item');
      expect(stepperItems).toHaveLength(3);

      // Item 0: completed
      expect(stepperItems[0]).toHaveAttribute('data-completed', 'true');
      expect(stepperItems[0]).not.toHaveAttribute('data-active');

      // Item 1: active
      expect(stepperItems[1]).toHaveAttribute('data-active', 'true');
      expect(stepperItems[1]).not.toHaveAttribute('data-completed');

      // Item 2: neither completed nor active
      expect(stepperItems[2]).not.toHaveAttribute('data-completed');
      expect(stepperItems[2]).not.toHaveAttribute('data-active');
    });
  });

  describe('4. Fallback for unknown step type', () => {
    it('renders unknown-step fallback when registry is empty', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        steps: [{ type: 'A' }, { type: 'B' }, { type: 'C' }],
        currentIndex: 1,
        isCurrentStepValid: false,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByTestId('onboarding-wizard__unknown-step')).toBeInTheDocument();
      expect(
        screen.getByText('This step type is not yet supported.'),
      ).toBeInTheDocument();
    });
  });

  describe('5. Action button disabled when isCurrentStepValid=false', () => {
    it('disables action button when step is invalid', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        steps: [{ type: 'A' }],
        currentIndex: 0,
        isCurrentStepValid: false,
      });

      renderWithTheme(<Wizard />);

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toBeDisabled();
    });
  });

  describe('6. Action button enabled when valid', () => {
    it('enables action button when step is valid', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        steps: [{ type: 'A' }],
        currentIndex: 0,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).not.toBeDisabled();
    });
  });

  describe('7. Submit click on configuration step', () => {
    it('calls submit when action button is clicked on a config step', () => {
      const submit = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        steps: [{ type: 'A' }, { type: 'B' }],
        currentIndex: 0,
        isCurrentStepValid: true,
        submit,
      });

      renderWithTheme(<Wizard />);

      const button = screen.getByTestId('onboarding-wizard__action');
      fireEvent.click(button);

      expect(submit).toHaveBeenCalledTimes(1);
    });
  });

  describe('8. issueOccurred state', () => {
    it('shows warning icon, title, message, and Dismiss button in body', () => {
      const skipMock = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: 'boom',
        isCurrentStepValid: true,
        skip: skipMock,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByTestId('onboarding-wizard__warning-icon')).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Issue Occurred' })).toBeInTheDocument();
      expect(screen.getByText('An issue occurred during setup. You may need to re-finalize your choices or selections.')).toBeInTheDocument();

      const dismissButton = screen.getByTestId('onboarding-wizard__dismiss');
      expect(dismissButton).toHaveTextContent('Dismiss for now');
      expect(dismissButton).not.toBeDisabled();
      fireEvent.click(dismissButton);
      expect(skipMock).toHaveBeenCalledTimes(1);
    });

    it('hides footer on issueOccurred state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: 'boom',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      // Footer action button should not be present
      expect(screen.queryByTestId('onboarding-wizard__action')).not.toBeInTheDocument();
    });
  });

  describe('9. setupComplete state', () => {
    it('renders Setup Complete content with success icon and Complete Setup button', () => {
      const submit = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        isCurrentStepValid: true,
        submit,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();
      expect(screen.getByText('Your Nexus Repository is ready to use.')).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__success-icon')).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Complete Setup');

      fireEvent.click(button);
      expect(submit).toHaveBeenCalledTimes(1);
    });

    it('displays network error message and warning icon when errorKind=network', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        errorMessage: 'Network failed',
        errorKind: 'network',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByTestId('onboarding-wizard__network-error')).toBeInTheDocument();
      expect(screen.getByText('A network error occurred. Please try again.')).toBeInTheDocument();
      // Warning icon replaces success icon while an error is active
      expect(screen.getByTestId('onboarding-wizard__warning-icon')).toBeInTheDocument();
      expect(screen.queryByTestId('onboarding-wizard__success-icon')).not.toBeInTheDocument();
    });

    it('displays still-required message and warning icon when errorKind=stillRequired', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        errorMessage: 'Onboarding still required',
        errorKind: 'stillRequired',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByTestId('onboarding-wizard__still-required')).toBeInTheDocument();
      expect(
        screen.getByText(
          'Some onboarding steps still need to be completed. Please re-finalize your choices.',
        ),
      ).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__warning-icon')).toBeInTheDocument();
      expect(screen.queryByTestId('onboarding-wizard__success-icon')).not.toBeInTheDocument();
    });

    it('does not display stepper on Setup Complete screen', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.queryByTestId('onboarding-wizard__stepper')).not.toBeInTheDocument();
    });
  });

  describe('10. done state', () => {
    it('renders Setup Complete content and Finish button that calls finish', () => {
      const finish = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'done',
        currentIndex: 1,
        isCurrentStepValid: true,
        finish,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Finish');

      fireEvent.click(button);
      expect(finish).toHaveBeenCalledTimes(1);
    });
  });

  describe('11. verifying state', () => {
    it('renders verifying message and hides footer', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verifying',
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByText('Verifying setup...')).toBeInTheDocument();

      // Footer is hidden during verification
      expect(screen.queryByTestId('onboarding-wizard__action')).not.toBeInTheDocument();
    });
  });

  describe('12. loading state', () => {
    it('renders Loading... and hides footer', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'loading',
        currentIndex: -1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByText('Loading...')).toBeInTheDocument();

      // Footer is hidden during loading
      expect(screen.queryByTestId('onboarding-wizard__action')).not.toBeInTheDocument();
    });
  });

  describe('13. Blocking modal', () => {
    it('has no close button', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      // Check that only the action button exists (no cancel/close buttons)
      const buttons = screen.queryAllByRole('button');
      expect(buttons).toHaveLength(1);
      expect(buttons[0]).toHaveAttribute('data-testid', 'onboarding-wizard__action');
    });
  });

  describe('14. Fixed-height container', () => {
    it('has minHeight and maxHeight set to 560px', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const content = screen.getByTestId('onboarding-wizard__root');
      expect(content.style.minHeight).toBe('560px');
      expect(content.style.maxHeight).toBe('560px');
    });
  });

  describe('15. No jest warnings (a11y)', () => {
    it('does not log console warnings or errors', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        currentIndex: -1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      // Assert no console warnings/errors (catches Radix a11y warnings)
      expect(warnSpy).not.toHaveBeenCalled();
      expect(errorSpy).not.toHaveBeenCalled();
    });
  });

  describe('skipped state', () => {
    it('renders Setup Complete content and Finish button that calls finish', () => {
      const finish = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'skipped',
        currentIndex: -1,
        isCurrentStepValid: true,
        finish,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Finish');

      fireEvent.click(button);
      expect(finish).toHaveBeenCalledTimes(1);
    });
  });

  describe('Last step shows Finish label', () => {
    it('shows Finish on the last configuration step', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'stepReady',
        steps: [{ type: 'A' }, { type: 'B' }],
        currentIndex: 1, // Last step
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Finish');
    });
  });

  describe('accessibility', () => {
    it('focuses Complete Setup button when Setup Complete screen appears', async () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const completeBtn = screen.getByRole('button', { name: 'Complete Setup' });
      await waitFor(() => {
        expect(completeBtn).toHaveFocus();
      });
    });

    it('screen reader announces title on Setup Complete', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const ariaLive = screen.getByTestId('onboarding-wizard__aria-live');
      expect(ariaLive).toHaveTextContent('Setup Complete');
    });

    it('screen reader announces title on Issue Occurred', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: 'boom',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const ariaLive = screen.getByTestId('onboarding-wizard__aria-live');
      expect(ariaLive).toHaveTextContent('Issue Occurred');
    });

    it('screen reader announces error title on Setup Complete with errorKind', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        errorMessage: 'Network failed',
        errorKind: 'network',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const ariaLive = screen.getByTestId('onboarding-wizard__aria-live');
      // Should NOT announce "Setup Complete" when there's an error, to avoid
      // a contradictory announcement alongside the warning callout.
      expect(ariaLive).toHaveTextContent('Issue Occurred');
    });

    it('focuses Dismiss button when Issue Occurred screen appears', async () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: 'boom',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const dismissBtn = screen.getByRole('button', { name: 'Dismiss for now' });
      await waitFor(() => {
        expect(dismissBtn).toHaveFocus();
      });
    });

    it('does not autoFocus Complete Setup when re-rendered with an errorKind', async () => {
      // On a verify failure the machine returns to setupComplete with errorKind
      // set; re-focusing the action button here would pre-empt the aria-live
      // error announcement on many screen readers. Leave focus alone so assistive
      // tech can finish speaking the error before the user tabs to the button.
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        errorMessage: 'Network failed',
        errorKind: 'network',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const completeBtn = screen.getByRole('button', { name: 'Complete Setup' });
      // Give React one paint tick to run any autoFocus effects.
      await new Promise((resolve) => setTimeout(resolve, 0));
      expect(completeBtn).not.toHaveFocus();
    });
  });
});

// Integration tests for the complete verification flow
// These tests simulate state transitions by controlling the mock values
describe('complete verification flow', () => {
  const mockRestClient = restClient as jest.Mocked<typeof restClient>;
  const mockExtJS = ExtJS as jest.Mocked<typeof ExtJS>;

  let warnSpy: jest.SpyInstance;
  let errorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockRestClient.get.mockResolvedValue([]);
    mockExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(true),
      setValue: jest.fn(),
    });
    mockExtJS.useUser.mockReturnValue({ administrator: true });
    mockExtJS.useState.mockReturnValue(true);

    warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    warnSpy.mockRestore();
    errorSpy.mockRestore();
  });

  it('clicking Complete Setup verifies and closes on success', async () => {
    const handleDone = jest.fn();
    const mockGetStarted = jest.fn();
    const mockSubmit = jest.fn();
    const mockFinish = jest.fn().mockImplementation(() => handleDone());

    // Start at welcome screen
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'stepReady',
      currentIndex: -1,
      isCurrentStepValid: true,
      getStarted: mockGetStarted,
    });

    const { rerender } = render(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Verify we're on Welcome screen
    expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();

    // Click "Get Started" - simulates transition to setupComplete (no steps)
    fireEvent.click(screen.getByRole('button', { name: 'Get Started' }));
    expect(mockGetStarted).toHaveBeenCalled();

    // Simulate state transition to setupComplete
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'setupComplete',
      currentIndex: 0,
      isCurrentStepValid: true,
      submit: mockSubmit,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Verify Setup Complete screen with "Complete Setup" button
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();
    });

    // Click "Complete Setup" - simulates transition through verifying to done
    fireEvent.click(screen.getByRole('button', { name: 'Complete Setup' }));
    expect(mockSubmit).toHaveBeenCalled();

    // Simulate state transition to done
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'done',
      currentIndex: 0,
      isCurrentStepValid: true,
      finish: mockFinish,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Verify Finish button appears
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Finish' })).toBeInTheDocument();
    });

    // Click "Finish" - calls onDone
    fireEvent.click(screen.getByRole('button', { name: 'Finish' }));
    expect(mockFinish).toHaveBeenCalled();
    expect(handleDone).toHaveBeenCalled();
  });

  it('shows still-required warning on Setup Complete when verification reports steps remain', async () => {
    const mockGetStarted = jest.fn();
    const mockSubmit = jest.fn();

    // Start at welcome screen
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'stepReady',
      currentIndex: -1,
      isCurrentStepValid: true,
      getStarted: mockGetStarted,
    });

    const { rerender } = render(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Click "Get Started"
    fireEvent.click(screen.getByRole('button', { name: 'Get Started' }));

    // Simulate transition to setupComplete
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'setupComplete',
      currentIndex: 0,
      isCurrentStepValid: true,
      submit: mockSubmit,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();
    });

    // Click "Complete Setup" - verify reports remaining steps
    fireEvent.click(screen.getByRole('button', { name: 'Complete Setup' }));

    // Machine catches OnboardingStillRequired and stays on setupComplete with
    // errorKind='stillRequired' so the user can retry (or fix their choices).
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'setupComplete',
      currentIndex: 0,
      errorMessage: 'Error: Onboarding still required',
      errorKind: 'stillRequired',
      isCurrentStepValid: true,
      submit: mockSubmit,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByTestId('onboarding-wizard__still-required')).toBeInTheDocument();
    });
    // The primary CTA remains "Complete Setup" so the user can retry.
    expect(screen.getByRole('button', { name: 'Complete Setup' })).toBeEnabled();
  });

  it('shows Issue Occurred when initial fetchSteps fails', async () => {
    const mockSkip = jest.fn();

    // Machine goes loading -> issueOccurred when fetchSteps rejects.
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'issueOccurred',
      currentIndex: -1,
      errorMessage: 'Error: fetch failed',
      isCurrentStepValid: true,
      skip: mockSkip,
    });

    render(
      <Theme>
        <Wizard />
      </Theme>
    );

    expect(screen.getByRole('heading', { name: 'Issue Occurred' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dismiss for now' })).toBeInTheDocument();

    // Click "Dismiss for now" - skips the wizard
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss for now' }));
    expect(mockSkip).toHaveBeenCalled();
  });

  it('shows network error banner on verify transport failure and allows retry', async () => {
    const mockGetStarted = jest.fn();
    const mockSubmit = jest.fn();

    // Start at welcome screen
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'stepReady',
      currentIndex: -1,
      isCurrentStepValid: true,
      getStarted: mockGetStarted,
    });

    const { rerender } = render(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Click "Get Started"
    fireEvent.click(screen.getByRole('button', { name: 'Get Started' }));

    // Simulate transition to setupComplete
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'setupComplete',
      currentIndex: 0,
      isCurrentStepValid: true,
      submit: mockSubmit,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();
    });

    // Click "Complete Setup" - first attempt fails with network error
    fireEvent.click(screen.getByRole('button', { name: 'Complete Setup' }));

    // Simulate setupComplete with network error (verify failed but returned to setupComplete)
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'setupComplete',
      currentIndex: 0,
      errorMessage: 'Error: Request failed',
      errorKind: 'network',
      isCurrentStepValid: true,
      submit: mockSubmit,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Verify network error banner is shown
    await waitFor(() => {
      expect(screen.getByTestId('onboarding-wizard__network-error')).toBeInTheDocument();
    });

    // Complete Setup button should still be enabled for retry
    const completeBtn = screen.getByRole('button', { name: 'Complete Setup' });
    expect(completeBtn).toBeEnabled();

    // Retry - click Complete Setup again
    fireEvent.click(completeBtn);

    // Simulate successful verification (done state)
    const mockFinish = jest.fn();
    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'done',
      currentIndex: 0,
      isCurrentStepValid: true,
      finish: mockFinish,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    // Verify "Finish" button appears (setup complete after retry)
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Finish' })).toBeInTheDocument();
    });
  });
});
