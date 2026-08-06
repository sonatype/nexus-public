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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import Wizard from '../Wizard';
import { useWizard } from '../useWizard';

jest.mock('../useWizard');
const mockedUseWizard = jest.mocked(useWizard);

describe('Wizard', () => {
  const defaultMocks = {
    state: 'stepReady' as const,
    steps: [] as Array<{ type: string }>,
    currentIndex: -1,
    currentStep: null,
    errorMessage: null,
    isCurrentStepValid: true,
    registerStep: jest.fn(),
    getStarted: jest.fn(),
    submit: jest.fn(),
    skip: jest.fn(),
    finish: jest.fn(),
  };

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
    it('shows error banner with message and Retry button', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: 'boom',
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByTestId('onboarding-wizard__error')).toBeInTheDocument();
      expect(screen.getByText('boom')).toBeInTheDocument();
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Retry');
    });

    it('Retry button is disabled until the retry transition ships', () => {
      const submit = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: 'boom',
        isCurrentStepValid: true,
        submit,
      });

      renderWithTheme(<Wizard />);

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toBeDisabled();
      fireEvent.click(button);
      expect(submit).not.toHaveBeenCalled();
    });

    it('shows fallback message when errorMessage is null', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'issueOccurred',
        currentIndex: 0,
        errorMessage: null,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(
        screen.getByText('An unexpected error occurred while setting up your instance.'),
      ).toBeInTheDocument();
    });

    it('renders a Skip button that dispatches skip so users can escape the locked error state', () => {
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

      const dismissButton = screen.getByTestId('onboarding-wizard__dismiss');
      expect(dismissButton).toHaveTextContent('Skip for Now');
      expect(dismissButton).not.toBeDisabled();
      fireEvent.click(dismissButton);
      expect(skipMock).toHaveBeenCalledTimes(1);
    });
  });

  describe('9. setupComplete state', () => {
    it('renders Setup Complete content and Finish button that calls submit', () => {
      const submit = jest.fn();
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'setupComplete',
        currentIndex: 1,
        isCurrentStepValid: true,
        submit,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByText('Setup Complete')).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Finish');

      fireEvent.click(button);
      expect(submit).toHaveBeenCalledTimes(1);
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

      expect(screen.getByText('Setup Complete')).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toHaveTextContent('Finish');

      fireEvent.click(button);
      expect(finish).toHaveBeenCalledTimes(1);
    });
  });

  describe('11. verifying state', () => {
    it('renders verifying message and disabled button', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'verifying',
        currentIndex: 1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByText('Verifying setup...')).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toBeDisabled();
    });
  });

  describe('12. loading state', () => {
    it('renders Loading... and disabled button', () => {
      mockedUseWizard.mockReturnValue({
        ...defaultMocks,
        state: 'loading',
        currentIndex: -1,
        isCurrentStepValid: true,
      });

      renderWithTheme(<Wizard />);

      expect(screen.getByText('Loading...')).toBeInTheDocument();

      const button = screen.getByTestId('onboarding-wizard__action');
      expect(button).toBeDisabled();
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

      expect(screen.getByText('Setup Complete')).toBeInTheDocument();

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
});
