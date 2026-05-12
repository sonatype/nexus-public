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
import { WizardForm } from '../WizardForm';

const STEPS = [
  { id: 'step1', label: 'First Step' },
  { id: 'step2', label: 'Second Step' },
  { id: 'step3', label: 'Third Step' },
];

function renderWizard(props: Partial<React.ComponentProps<typeof WizardForm>> = {}) {
  const defaultProps = {
    steps: STEPS,
    currentStep: 0,
    onStepChange: jest.fn(),
    onComplete: jest.fn(),
    onCancel: jest.fn(),
    children: <div data-testid="step-content">Step Content</div>,
  };

  return render(
    <Theme>
      <WizardForm {...defaultProps} {...props} />
    </Theme>
  );
}

describe('WizardForm', () => {
  describe('rendering', () => {
    it('renders step indicator with all steps', () => {
      renderWizard();
      expect(screen.getByTestId('wizard-step-step1')).toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-step2')).toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-step3')).toBeInTheDocument();
    });

    it('renders children content', () => {
      renderWizard();
      expect(screen.getByTestId('step-content')).toBeInTheDocument();
    });

    it('shows "Cancel" on first step', () => {
      renderWizard({ currentStep: 0 });
      expect(screen.getByTestId('form-cancel')).toHaveTextContent('Cancel');
    });

    it('shows "Cancel" and separate "Back" on subsequent steps', () => {
      renderWizard({ currentStep: 1 });
      expect(screen.getByTestId('form-cancel')).toHaveTextContent('Cancel');
      expect(screen.getByTestId('wizard-back')).toHaveTextContent('Back');
    });

    it('shows "Next: {label}" on non-final steps', () => {
      renderWizard({ currentStep: 0 });
      expect(screen.getByTestId('form-submit')).toHaveTextContent('Next: Second Step');
    });

    it('shows complete label on final step', () => {
      renderWizard({ currentStep: 2, completeLabel: 'Create Task' });
      expect(screen.getByTestId('form-submit')).toHaveTextContent('Create Task');
    });

    it('defaults complete label to "Create"', () => {
      renderWizard({ currentStep: 2 });
      expect(screen.getByTestId('form-submit')).toHaveTextContent('Create');
    });

    it('renders footer extra content', () => {
      renderWizard({
        footerExtra: <button data-testid="delete-btn">Delete</button>,
      });
      expect(screen.getByTestId('delete-btn')).toBeInTheDocument();
    });
  });

  describe('navigation', () => {
    it('calls onStepChange with next step when Next is clicked', () => {
      const onStepChange = jest.fn();
      renderWizard({ currentStep: 0, onStepChange });

      fireEvent.click(screen.getByTestId('form-submit'));
      expect(onStepChange).toHaveBeenCalledWith(1);
    });

    it('calls onStepChange with previous step when Back is clicked', () => {
      const onStepChange = jest.fn();
      renderWizard({ currentStep: 1, onStepChange });

      fireEvent.click(screen.getByTestId('wizard-back'));
      expect(onStepChange).toHaveBeenCalledWith(0);
    });

    it('calls onCancel when Cancel is clicked on first step', () => {
      const onCancel = jest.fn();
      renderWizard({ currentStep: 0, onCancel });

      fireEvent.click(screen.getByTestId('form-cancel'));
      expect(onCancel).toHaveBeenCalled();
    });

    it('calls onCancel when Cancel is clicked on non-first step', () => {
      const onCancel = jest.fn();
      renderWizard({ currentStep: 1, onCancel });

      fireEvent.click(screen.getByTestId('form-cancel'));
      expect(onCancel).toHaveBeenCalled();
    });

    it('calls onComplete when submit is clicked on final step', () => {
      const onComplete = jest.fn();
      renderWizard({ currentStep: 2, onComplete });

      fireEvent.click(screen.getByTestId('form-submit'));
      expect(onComplete).toHaveBeenCalled();
    });
  });

  describe('validation gating', () => {
    it('disables Next button when canAdvance is false', () => {
      renderWizard({ currentStep: 0, canAdvance: false });
      expect(screen.getByTestId('form-submit')).toBeDisabled();
    });

    it('enables Next button when canAdvance is true', () => {
      renderWizard({ currentStep: 0, canAdvance: true });
      expect(screen.getByTestId('form-submit')).not.toBeDisabled();
    });

    it('does not advance step when canAdvance is false', () => {
      const onStepChange = jest.fn();
      renderWizard({ currentStep: 0, onStepChange, canAdvance: false });

      fireEvent.click(screen.getByTestId('form-submit'));
      expect(onStepChange).not.toHaveBeenCalled();
    });
  });

  describe('step indicator states', () => {
    it('marks current step', () => {
      renderWizard({ currentStep: 1 });
      const step = screen.getByTestId('wizard-step-step2');
      expect(step.className).toContain('wizard-form__step--current');
    });

    it('marks completed steps', () => {
      renderWizard({ currentStep: 2 });
      const step1 = screen.getByTestId('wizard-step-step1');
      const step2 = screen.getByTestId('wizard-step-step2');
      expect(step1.className).toContain('wizard-form__step--complete');
      expect(step2.className).toContain('wizard-form__step--complete');
    });
  });

  describe('loading state', () => {
    it('disables submit when loading', () => {
      renderWizard({ loading: true });
      expect(screen.getByTestId('form-submit')).toBeDisabled();
    });
  });

  describe('custom testId', () => {
    it('uses custom testId for form and steps', () => {
      renderWizard({ testId: 'task-wizard' });
      expect(screen.getByTestId('task-wizard')).toBeInTheDocument();
      expect(screen.getByTestId('task-wizard-steps')).toBeInTheDocument();
    });
  });

  describe('two-step wizard', () => {
    const twoSteps = [
      { id: 'connect', label: 'Connection' },
      { id: 'mapping', label: 'User & Group' },
    ];

    it('works with two steps', () => {
      const onComplete = jest.fn();
      renderWizard({ steps: twoSteps, currentStep: 1, onComplete });

      fireEvent.click(screen.getByTestId('form-submit'));
      expect(onComplete).toHaveBeenCalled();
    });

    it('shows correct submit label on step 1 of 2', () => {
      renderWizard({ steps: twoSteps, currentStep: 0 });
      expect(screen.getByTestId('form-submit')).toHaveTextContent('Next: User & Group');
    });
  });
});
