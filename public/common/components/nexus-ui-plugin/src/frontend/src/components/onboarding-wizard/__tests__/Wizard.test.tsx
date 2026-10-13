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

  describe('9. verificationFailed state (verify-error retry landing)', () => {
    it('renders Verification failed heading and Retry button on network error', () => {
      const submit = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorMessage: 'Network failed',
        errorKind: 'network',
        isCurrentStepValid: true,
        submit,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByRole('heading', { name: 'Verification failed' })).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__network-error')).toBeInTheDocument();
      expect(screen.getByText('A network error occurred. Please try again.')).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__warning-icon')).toBeInTheDocument();
      expect(screen.queryByTestId('onboarding-wizard__success-icon')).not.toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Retry');

      fireEvent.click(button);
      expect(submit).toHaveBeenCalledTimes(1);
    });

    it('renders Verification failed heading and still-required callout on stillRequired error', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorMessage: 'Onboarding still required',
        errorKind: 'stillRequired',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByRole('heading', { name: 'Verification failed' })).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__still-required')).toBeInTheDocument();
      expect(
        screen.getByText(
          'Some onboarding steps still need to be completed. Please re-finalize your choices.',
        ),
      ).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__warning-icon')).toBeInTheDocument();
      expect(screen.queryByTestId('onboarding-wizard__success-icon')).not.toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__action')).toHaveTextContent('Retry');
    });

    it('renders generic callout when errorKind is null on verificationFailed state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorMessage: null,
        errorKind: null,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByRole('heading', { name: 'Verification failed' })).toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__generic-error')).toBeInTheDocument();
      expect(screen.getByText('Setup could not be verified. Please try again.')).toBeInTheDocument();
      expect(screen.queryByTestId('onboarding-wizard__network-error')).not.toBeInTheDocument();
      expect(screen.queryByTestId('onboarding-wizard__still-required')).not.toBeInTheDocument();
      expect(screen.getByTestId('onboarding-wizard__action')).toHaveTextContent('Retry');
    });

    it('does not render Setup Complete success body on verificationFailed state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorKind: 'network',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.queryByRole('heading', { name: 'Setup Complete' })).not.toBeInTheDocument();
      expect(screen.queryByText('Your Nexus Repository is ready to use.')).not.toBeInTheDocument();
    });

    it('does not display stepper on verificationFailed state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorKind: 'network',
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
    it('focuses Finish button when done state appears', async () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'done',
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const finishBtn = screen.getByRole('button', { name: 'Finish' });
      await waitFor(() => {
        expect(finishBtn).toHaveFocus();
      });
    });

    it('screen reader announces Setup Complete on done state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'done',
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

    it('screen reader announces Verification failed with network detail on verificationFailed state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorMessage: 'Network failed',
        errorKind: 'network',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const ariaLive = screen.getByTestId('onboarding-wizard__aria-live');
      expect(ariaLive).toHaveTextContent(
        'Verification failed. A network error occurred. Please try again.',
      );
    });

    it('screen reader announces Verification failed with still-required detail on verificationFailed state', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorMessage: 'Still required',
        errorKind: 'stillRequired',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const ariaLive = screen.getByTestId('onboarding-wizard__aria-live');
      expect(ariaLive).toHaveTextContent(
        'Verification failed. Some onboarding steps still need to be completed. Please re-finalize your choices.',
      );
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

    it('does not autoFocus Retry button on Verification failed state', async () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verificationFailed',
        currentIndex: 1,
        errorMessage: 'Network failed',
        errorKind: 'network',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      const retryBtn = screen.getByRole('button', { name: 'Retry' });
      await new Promise((resolve) => setTimeout(resolve, 0));
      expect(retryBtn).not.toHaveFocus();
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

  it('happy path shows Setup Complete only once: Welcome -> verifying -> done -> Finish', async () => {
    const handleDone = jest.fn();
    const mockGetStarted = jest.fn();
    const mockFinish = jest.fn().mockImplementation(() => handleDone());

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

    expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Get Started' }));
    expect(mockGetStarted).toHaveBeenCalled();

    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'verifying',
      currentIndex: 0,
      isCurrentStepValid: true,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByText('Verifying setup...')).toBeInTheDocument();
    });
    expect(screen.queryByRole('heading', { name: 'Setup Complete' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Complete Setup' })).not.toBeInTheDocument();

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

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Finish' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Finish' }));
    expect(mockFinish).toHaveBeenCalled();
    expect(handleDone).toHaveBeenCalled();
  });

  it('shows Verification failed with still-required callout when verify reports steps remain', async () => {
    const mockGetStarted = jest.fn();
    const mockSubmit = jest.fn();

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

    fireEvent.click(screen.getByRole('button', { name: 'Get Started' }));

    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'verifying',
      currentIndex: 0,
      isCurrentStepValid: true,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByText('Verifying setup...')).toBeInTheDocument();
    });

    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'verificationFailed',
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
      expect(screen.getByRole('heading', { name: 'Verification failed' })).toBeInTheDocument();
    });
    expect(screen.getByTestId('onboarding-wizard__still-required')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled();
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

  it('shows Verification failed with network callout on transport failure and allows retry', async () => {
    const mockGetStarted = jest.fn();
    const mockSubmit = jest.fn();

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

    fireEvent.click(screen.getByRole('button', { name: 'Get Started' }));

    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'verifying',
      currentIndex: 0,
      isCurrentStepValid: true,
    });
    rerender(
      <Theme>
        <Wizard />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByText('Verifying setup...')).toBeInTheDocument();
    });

    mockedUseWizard.mockReturnValue({
      ...defaultMocks,
      state: 'verificationFailed',
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

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Verification failed' })).toBeInTheDocument();
    });
    expect(screen.getByTestId('onboarding-wizard__network-error')).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: 'Retry' });
    expect(retryBtn).toBeEnabled();
    fireEvent.click(retryBtn);
    expect(mockSubmit).toHaveBeenCalled();

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

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Finish' })).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: 'Setup Complete' })).toBeInTheDocument();
  });
});
