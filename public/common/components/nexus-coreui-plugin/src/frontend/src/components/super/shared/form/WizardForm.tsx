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

import React, { useCallback, useMemo, useRef, useEffect } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { CheckCircle2 } from 'lucide-react';
import { SettingsForm } from './SettingsForm';
import { SettingsButton } from './SettingsButton';

import './WizardForm.scss';

export interface WizardStep {
  id: string;
  label: string;
}

export interface WizardFormProps {
  steps: WizardStep[];
  currentStep: number;
  onStepChange: (step: number) => void;
  onComplete: () => void;
  onCancel: () => void;
  /** Label for the final step's submit button */
  completeLabel?: string;
  /** Page title for the form header */
  title?: string;
  /** Page description for the form header */
  description?: string;
  /** Whether advancing from the current step is allowed (validation gate) */
  canAdvance?: boolean;
  /** Whether the form has unsaved changes (enables discard confirmation on Cancel) */
  dirty?: boolean;
  loading?: boolean;
  error?: string;
  testId?: string;
  /** Extra content for the left side of the action bar (e.g., Delete button) */
  footerExtra?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  /** Disable dirty tracking (for create-only wizards) */
  noDirtyTracking?: boolean;
  /** Override submit handler for specific step (e.g. to run nested form validation before advancing) */
  onStepSubmitOverride?: (step: number) => (() => void) | undefined;
  /** Analytics ID for the submit button */
  submitAnalyticsId?: string;
}

function StepIndicator({
  steps,
  currentIndex,
  testId,
}: {
  steps: WizardStep[];
  currentIndex: number;
  testId?: string;
}) {
  return (
    <Flex
      align="center"
      gap="2"
      className="wizard-form__step-indicator"
      data-testid={testId ? `${testId}-steps` : 'wizard-steps'}
    >
      {steps.map((step, i) => {
        const isComplete = i < currentIndex;
        const isCurrent = i === currentIndex;
        return (
          <React.Fragment key={step.id}>
            {i > 0 && (
              <Box
                className={`wizard-form__step-line ${
                  isComplete ? 'wizard-form__step-line--complete' : ''
                }`}
              />
            )}
            <Flex
              align="center"
              gap="1"
              className={`wizard-form__step ${
                isCurrent ? 'wizard-form__step--current' : ''
              } ${isComplete ? 'wizard-form__step--complete' : ''}`}
              data-testid={`wizard-step-${step.id}`}
            >
              <span className="wizard-form__step-dot">
                {isComplete ? <CheckCircle2 size={16} /> : i + 1}
              </span>
              <Text size="1" weight={isCurrent ? 'medium' : 'regular'}>
                {step.label}
              </Text>
            </Flex>
          </React.Fragment>
        );
      })}
    </Flex>
  );
}

/**
 * WizardForm - Multi-step form wrapper using SettingsForm layout.
 *
 * Provides step indicator, navigation buttons (Back/Next/Complete),
 * and validation gating between steps. Parent controls step content
 * and form state.
 *
 * @example
 * <WizardForm
 *   steps={[
 *     { id: 'type', label: 'Select Type' },
 *     { id: 'config', label: 'Configure' },
 *     { id: 'schedule', label: 'Schedule' },
 *   ]}
 *   currentStep={step}
 *   onStepChange={setStep}
 *   onComplete={handleSave}
 *   onCancel={handleCancel}
 *   completeLabel="Create"
 *   canAdvance={isStepValid}
 * >
 *   {step === 0 && <TypeSelector />}
 *   {step === 1 && <ConfigForm />}
 *   {step === 2 && <ScheduleForm />}
 * </WizardForm>
 */
export function WizardForm({
  steps,
  currentStep,
  onStepChange,
  onComplete,
  onCancel,
  completeLabel = 'Create',
  title,
  description,
  canAdvance = true,
  dirty = false,
  loading = false,
  error,
  testId,
  footerExtra,
  children,
  className = '',
  noDirtyTracking = false,
  onStepSubmitOverride,
  submitAnalyticsId,
}: WizardFormProps) {
  const isFirstStep = currentStep === 0;
  const isLastStep = currentStep === steps.length - 1;
  const nextStepLabel = steps[currentStep + 1]?.label;

  const handleNext = useCallback(() => {
    if (!isLastStep && canAdvance) {
      onStepChange(currentStep + 1);
    }
  }, [isLastStep, canAdvance, currentStep, onStepChange]);

  const handleBack = useCallback(() => {
    if (!isFirstStep) {
      onStepChange(currentStep - 1);
    }
  }, [isFirstStep, currentStep, onStepChange]);

  const stepSubmitOverride = onStepSubmitOverride?.(currentStep);
  const handleSubmit = useCallback(() => {
    if (stepSubmitOverride) {
      return stepSubmitOverride();
    } else if (isLastStep) {
      return onComplete();
    } else {
      handleNext();
    }
  }, [stepSubmitOverride, isLastStep, onComplete, handleNext]);

  const handleCancel = useCallback(() => {
    onCancel();
  }, [onCancel]);

  const submitLabel = useMemo(() => {
    if (isLastStep) return completeLabel;
    return `Next: ${nextStepLabel} \u2192`;
  }, [isLastStep, completeLabel, nextStepLabel]);

  return (
    <SettingsForm
      onSubmit={handleSubmit}
      onCancel={handleCancel}
      title={title}
      description={description}
      submitLabel={submitLabel}
      cancelLabel="Cancel"
      loading={loading}
      dirty={dirty}
      pristine={false}
      submitDisabled={!canAdvance || loading}
      confirmDiscard={dirty}
      noDirtyTracking={noDirtyTracking || !dirty}
      showActions
      error={error}
      testId={testId || 'wizard-form'}
      submitAnalyticsId={submitAnalyticsId}
      className={`wizard-form ${className}`.trim()}
      footerExtra={
        <Flex align="center" gap="3">
          {!isFirstStep && (
            <SettingsButton
              variant="secondary"
              onClick={handleBack}
              disabled={loading}
              testId="wizard-back"
            >
              {'\u2190 Back'}
            </SettingsButton>
          )}
          {footerExtra}
          <StepIndicator steps={steps} currentIndex={currentStep} testId={testId} />
        </Flex>
      }
    >
      <Box
        className="wizard-form__content"
        data-step={steps[currentStep]?.id}
        data-step-index={currentStep}
      >
        {children}
      </Box>
    </SettingsForm>
  );
}

export default WizardForm;
