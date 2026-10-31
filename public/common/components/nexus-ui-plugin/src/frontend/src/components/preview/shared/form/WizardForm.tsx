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

import React, { useCallback, useMemo } from 'react';
import { Box, Flex, Progress, Heading, Button } from '@radix-ui/themes';
import { ArrowLeft } from 'lucide-react';
import { SettingsForm } from './SettingsForm';

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
  /**
   * Opt out of the wizard's own dirty registration when the embedded form
   * already tracks dirty state via a form machine (`useForm`). Prevents the
   * wizard from adding a second `window.dirty` entry that would trigger a
   * duplicate unsaved-changes dialog. Pair with `onDiscardConfirm`.
   */
  externalDirtyTracking?: boolean;
  /**
   * Called after the user confirms "Leave" in the discard dialog. Callers
   * using `externalDirtyTracking` must clear their form machine's dirty entry
   * here (via `clearDirtyState(machineId)`) and then navigate. Replaces the
   * default `onCancel()` invocation.
   */
  onDiscardConfirm?: () => void;
  /** Override submit handler for specific step (e.g. to run nested form validation before advancing) */
  onStepSubmitOverride?: (step: number) => (() => void) | undefined;
  /** Hide the Next/Complete button (for auto-advance scenarios) */
  hideSubmitButton?: boolean;
  /** Hide the step title (for custom title rendering in children) */
  hideStepTitle?: boolean;
  /** Analytics ID for the submit button */
  submitAnalyticsId?: string;
  /** Analytics ID for the cancel button */
  cancelAnalyticsId?: string;
  /** Analytics ID for the back (previous step) button */
  backAnalyticsId?: string;
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
  const progressPercentage = ((currentIndex + 1) / steps.length) * 100;

  if (steps.length === 0) {
    return null;
  }

  return (
    <Box
      className="wizard-form__progress-indicator"
      data-testid={testId ? `${testId}-steps` : 'wizard-steps'}
    >
      {/* Progress Bar Only */}
      <Box position="relative">
        <Box width="100%">
          <Progress value={progressPercentage} color="blue" size="3" />
        </Box>

        {/* Vertical dividers between steps */}
        <Flex
          position="absolute"
          inset="0"
          className="wizard-form__dividers"
        >
          {steps.map((step, i) => {
            if (i === steps.length - 1) return null;
            const position = ((i + 1) / steps.length) * 100;
            // dynamic positioning — left% is computed from step count, cannot be a static CSS class
            return (
              <Box
                key={step.id}
                position="absolute"
                inset="0"
                className="wizard-form__divider"
                style={{ left: `${position}%` }}
              />
            );
          })}
        </Flex>
      </Box>
    </Box>
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
  externalDirtyTracking = false,
  onDiscardConfirm,
  onStepSubmitOverride,
  hideSubmitButton = false,
  hideStepTitle = false,
  submitAnalyticsId,
  cancelAnalyticsId,
  backAnalyticsId,
}: WizardFormProps) {
  const isFirstStep = currentStep === 0;
  const isLastStep = currentStep === steps.length - 1;

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
    return 'Continue';
  }, [isLastStep, completeLabel]);

  return (
    <SettingsForm
      onSubmit={hideSubmitButton ? undefined : handleSubmit}
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
      externalDirtyTracking={externalDirtyTracking}
      onDiscardConfirm={onDiscardConfirm}
      showActions
      error={error}
      testId={testId || 'wizard-form'}
      submitAnalyticsId={submitAnalyticsId}
      cancelAnalyticsId={cancelAnalyticsId}
      className={`wizard-form ${className}`.trim()}
      cancelOnLeft={true}
      actionButtons={
        !isFirstStep && (
          <Button
            type="button"
            variant="soft"
            size="2"
            onClick={handleBack}
            disabled={loading}
            data-testid="wizard-back"
            data-analytics-id={backAnalyticsId}
          >
            <ArrowLeft size={14} />
            Back
          </Button>
        )
      }
      footerExtra={footerExtra}
      progressBar={
        <StepIndicator steps={steps} currentIndex={currentStep} testId={testId} />
      }
    >
      <Box
        className="wizard-form__content"
        data-step={steps[currentStep]?.id}
        data-step-index={currentStep}
      >
        {/* Current Step Title */}
        {!hideStepTitle && (
          <Heading as="h2" size="6" mb="4">
            {steps[currentStep]?.label}
          </Heading>
        )}
        {children}
      </Box>
    </SettingsForm>
  );
}

export default WizardForm;
