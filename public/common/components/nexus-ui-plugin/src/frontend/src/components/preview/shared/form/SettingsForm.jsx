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

import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { AlertDialog, Box, Button, Flex, Heading, Text } from '@radix-ui/themes';
import { AlertCircle } from 'lucide-react';
import { SettingsAlert } from './SettingsAlert';
import { useUnsavedChangesWarning, clearDirtyState } from '../hooks/useUnsavedChangesWarning';

import './SettingsForm.scss';

/**
 * SettingsForm - Main form wrapper for settings pages
 * 
 * Provides consistent layout with:
 * - Page header (title + description + optional header actions)
 * - Scrollable content area
 * - Sticky footer with save/cancel actions + optional footer extra
 * - Loading and error states
 * 
 * @example
 * <SettingsForm
 *   title="Edit Blob Store"
 *   description="File Blob Store"
 *   onSave={handleSave}
 *   onCancel={handleCancel}
 *   saving={isSaving}
 *   dirty={hasChanges}
 *   headerActions={<SettingsButton icon={ArrowLeft} onClick={goBack}>Back</SettingsButton>}
 *   footerExtra={<SettingsButton variant="danger" icon={Trash2}>Delete</SettingsButton>}
 * >
 *   <SettingsFormSection title="Settings">
 *     <SettingsTextInput ... />
 *   </SettingsFormSection>
 * </SettingsForm>
 */
export function SettingsForm({
  title,
  description,
  children,
  // Support both onSave and onSubmit for flexibility
  onSave,
  onSubmit,
  onCancel,
  // Called when user confirms discard (after dialog, before navigation)
  // If provided, replaces default behavior - caller must clear dirty state and navigate
  onDiscardConfirm,
  // Support both saving and loading
  saving = false,
  loading = false,
  // Support both dirty and pristine (inverted logic)
  dirty = false,
  pristine,
  submitLabel = 'Save',
  cancelLabel = 'Discard',
  error = null,
  success = null,
  submitDisabled = false,
  cancelDisabled = false,
  noDirtyTracking = false,
  confirmDiscard = true,
  // Explicit opt-in to skip internal dirty tracking.
  // Use this when parent manages dirty state via form machine (e.g., useForm)
  // that also calls useUnsavedChangesWarning. Prevents duplicate window.dirty entries.
  externalDirtyTracking = false,
  showActions = true,
  // Whether to show the header section
  showHeader = true,
  // New props for header actions and footer extras
  headerActions,
  footerExtra,
  // Additional action buttons to render before Cancel (e.g., Back button)
  actionButtons,
  // Progress bar or stepper to render below action bar (for wizards)
  progressBar,
  // Wizard mode: Cancel on left, Submit on right (default: Cancel on right)
  cancelOnLeft = false,
  className = '',
  // Testability props
  testId,
  // Analytics props
  submitAnalyticsId,
  cancelAnalyticsId,
  // Spread additional data-* attributes
  ...restProps
}) {
  // Normalize props - support both naming conventions
  const isLoading = saving || loading;
  // If pristine is explicitly passed, use it; otherwise use !dirty
  const isPristine = pristine !== undefined ? pristine : !dirty;
  const saveHandler = onSave || onSubmit;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isLoading && !isPristine && !submitDisabled && saveHandler) {
      try {
        await saveHandler(e);
        clearDirtyState(formIdRef.current);
      } catch {
        // Keep dirty state on failure so navigation warning still fires
      }
    }
  };

  const isSubmitDisabled = isLoading || isPristine || submitDisabled;
  const [discardDialogOpen, setDiscardDialogOpen] = useState(false);
  const discardedRef = useRef(false);

  // Reset discardedRef when form becomes pristine (e.g., component reused with fresh data)
  useEffect(() => {
    if (isPristine) {
      discardedRef.current = false;
    }
  }, [isPristine]);

  const handleCancelClick = () => {
    if (!onCancel) return;
    if (!confirmDiscard || isPristine || noDirtyTracking) {
      onCancel();
    } else {
      setDiscardDialogOpen(true);
    }
  };

  const handleDiscardConfirm = () => {
    setDiscardDialogOpen(false);
    // Mark as discarded so useUnsavedChangesWarning won't re-register this form
    // during the React re-render triggered by the dialog closing
    discardedRef.current = true;
    // Clear this specific form from dirty tracking
    clearDirtyState(formIdRef.current);

    // If caller provides onDiscardConfirm, they clear their own dirty state (e.g., form machine)
    // and navigate. This form's dirty state was already cleared above.
    if (onDiscardConfirm) {
      onDiscardConfirm();
    } else if (onCancel) {
      onCancel();
    }
  };

  const formIdRef = useRef(`settings-form-${Math.random().toString(36).slice(2, 8)}`);

  // Track unsaved changes; skip when noDirtyTracking/externalDirtyTracking set, or after discard to prevent double modal.
  useUnsavedChangesWarning(
    !isPristine && !noDirtyTracking && !externalDirtyTracking && !discardedRef.current,
    formIdRef.current
  );

  // Filter restProps to only include data-* attributes for safety
  const dataProps = Object.keys(restProps).reduce((acc, key) => {
    if (key.startsWith('data-') || key.startsWith('aria-')) {
      acc[key] = restProps[key];
    }
    return acc;
  }, {});

  return (
    <form 
      className={`settings-form ${className}`.trim()} 
      onSubmit={handleSubmit}
      noValidate
      autoComplete="off"
      data-testid={testId || 'settings-form'}
      data-loading={isLoading ? 'true' : 'false'}
      data-dirty={dirty ? 'true' : 'false'}
      data-pristine={isPristine ? 'true' : 'false'}
      data-submit-disabled={isSubmitDisabled ? 'true' : 'false'}
      aria-busy={isLoading}
      {...dataProps}
    >
      {/* Header - only render if title is provided */}
      {showHeader && title && (
        <Box className="settings-form__header">
          <Flex justify="between" align="start" className="settings-form__header-row">
            <Box className="settings-form__header-content">
              <Heading as="h1" size="6" weight="medium" className="settings-form__title">
                {title}
              </Heading>
              {description && (
                <Text as="p" size="2" className="settings-form__description">
                  {description}
                </Text>
              )}
            </Box>
            {headerActions && (
              <Flex gap="2" className="settings-form__header-actions">
                {headerActions}
              </Flex>
            )}
          </Flex>
        </Box>
      )}

      {/* Sticky Action Bar - always visible at top of form */}
      {/* UX Best Practice: Primary actions (Save) on RIGHT, destructive (Delete) on LEFT */}
      {showActions && (
        <Box className="settings-form__action-bar">
          {cancelOnLeft ? (
            // Wizard layout: Cancel on LEFT, Back + Continue on RIGHT
            <>
              {/* Left side: Cancel + footerExtra (e.g. Delete button) */}
              <Flex gap="2" align="center">
                {onCancel && (
                  <Button
                    type="button"
                    variant="outline"
                    size="2"
                    onClick={handleCancelClick}
                    disabled={isLoading || cancelDisabled}
                    data-testid="form-cancel"
                    data-analytics-id={cancelAnalyticsId}
                  >
                    {cancelLabel}
                  </Button>
                )}
                {footerExtra}
              </Flex>

              {/* Right side: Back + Continue */}
              <Flex gap="3" align="center">
                {actionButtons}
                {saveHandler && (
                  <Button
                    type="submit"
                    variant="solid"
                    size="2"
                    disabled={isSubmitDisabled}
                    loading={isLoading}
                    aria-busy={isLoading ? 'true' : undefined}
                    data-testid="form-submit"
                    data-analytics-id={submitAnalyticsId}
                  >
                    {submitLabel}
                  </Button>
                )}
              </Flex>
            </>
          ) : (
            // Standard layout: Extra on LEFT, Cancel + Submit on RIGHT
            <>
              {/* Left side: Destructive actions (Delete) + error message */}
              <Flex gap="2" align="center" className="settings-form__action-bar-extra">
                {footerExtra}
                {error && (
                  <Flex gap="1" align="center" className="settings-form__action-bar-error">
                    <AlertCircle size={14} />
                    <Text size="1" weight="medium">{error}</Text>
                  </Flex>
                )}
              </Flex>

              {/* Right side: Primary actions + status */}
              {/* "Unsaved changes" is always rendered so the action bar reserves the same horizontal space in pristine and dirty states — toggling visibility (not the DOM) prevents the layout shift that would otherwise wrap nearby buttons (e.g. Delete) when the indicator first appears. Plain <span> rather than Radix <Text> because Radix Text strips data-* attributes. */}
              <Flex gap="3" align="center">
                <span
                  className="settings-form__unsaved"
                  data-pristine={isPristine || !!error || noDirtyTracking ? 'true' : 'false'}
                  aria-hidden={isPristine || !!error || noDirtyTracking ? 'true' : undefined}
                >
                  Unsaved changes
                </span>
                {actionButtons}
                {onCancel && (
                  <Button
                    type="button"
                    variant="outline"
                    size="2"
                    onClick={handleCancelClick}
                    disabled={isLoading || cancelDisabled}
                    data-testid="form-cancel"
                    data-analytics-id={cancelAnalyticsId}
                  >
                    {cancelLabel}
                  </Button>
                )}
                {saveHandler && (
                  <Button
                    type="submit"
                    variant="solid"
                    size="2"
                    disabled={isSubmitDisabled}
                    loading={isLoading}
                    aria-busy={isLoading ? 'true' : undefined}
                    data-testid="form-submit"
                    data-analytics-id={submitAnalyticsId}
                  >
                    {submitLabel}
                  </Button>
                )}
              </Flex>
            </>
          )}
        </Box>
      )}

      {/* Progress Bar / Stepper - for wizards */}
      {progressBar && (
        <Box className="settings-form__progress-bar">
          {progressBar}
        </Box>
      )}

      {/* Alerts - errors now shown in sticky action bar above */}
      {success && (
        <Box className="settings-form__alerts">
          <SettingsAlert type="success">{success}</SettingsAlert>
        </Box>
      )}

      {/* Content - scrollable, action bar sticks above */}
      <Box className="settings-form__content">
        <Box className="settings-form__inner">
          {children}
        </Box>
      </Box>

      {/* Discard Changes Confirmation - only rendered when confirmDiscard is enabled */}
      {confirmDiscard && (
        <AlertDialog.Root open={discardDialogOpen} onOpenChange={setDiscardDialogOpen}>
          <AlertDialog.Content maxWidth="450px">
            <AlertDialog.Title>Unsaved Changes</AlertDialog.Title>
            <AlertDialog.Description size="2">
              You have unsaved changes. Are you sure you want to leave? Your changes will be lost.
            </AlertDialog.Description>
            <Flex gap="3" mt="4" justify="end">
              <AlertDialog.Cancel>
                <Button variant="soft" color="gray">Stay</Button>
              </AlertDialog.Cancel>
              <AlertDialog.Action>
                <Button variant="solid" color="red" onClick={handleDiscardConfirm}>Leave</Button>
              </AlertDialog.Action>
            </Flex>
          </AlertDialog.Content>
        </AlertDialog.Root>
      )}
    </form>
  );
}

SettingsForm.propTypes = {
  /** Page title displayed in header (header hidden if not provided) */
  title: PropTypes.string,
  /** Optional description below title */
  description: PropTypes.string,
  /** Form content (typically SettingsFormSection components) */
  children: PropTypes.node.isRequired,
  /** Called when form is submitted (alias for onSubmit) */
  onSave: PropTypes.func,
  /** Called when form is submitted */
  onSubmit: PropTypes.func,
  /** Called when cancel button is clicked */
  onCancel: PropTypes.func,
  /** Shows loading spinner on submit button (alias for loading) */
  saving: PropTypes.bool,
  /** Shows loading spinner on submit button */
  loading: PropTypes.bool,
  /** Form has unsaved changes (inverted from pristine) */
  dirty: PropTypes.bool,
  /** Disables submit when true (no changes made) */
  pristine: PropTypes.bool,
  /** Submit button label */
  submitLabel: PropTypes.string,
  /** Cancel button label */
  cancelLabel: PropTypes.string,
  /** Error message to display */
  error: PropTypes.string,
  /** Success message to display */
  success: PropTypes.string,
  /** Additional disable condition for submit */
  submitDisabled: PropTypes.bool,
  /** Disables the cancel button */
  cancelDisabled: PropTypes.bool,
  /** Skip dirty state tracking (for one-shot forms like upload) */
  noDirtyTracking: PropTypes.bool,
  /** Show confirmation dialog when canceling with unsaved changes (default: true) */
  confirmDiscard: PropTypes.bool,
  /** Whether to show action buttons */
  showActions: PropTypes.bool,
  /** Whether to show the header section */
  showHeader: PropTypes.bool,
  /** Optional actions to show in header (e.g., Back button) */
  headerActions: PropTypes.node,
  /** Optional extra content in footer (e.g., Delete button) */
  footerExtra: PropTypes.node,
  /** Additional action buttons before Cancel (e.g., Back button in wizards) */
  actionButtons: PropTypes.node,
  /** Progress bar or stepper to render below action bar (for wizards) */
  progressBar: PropTypes.node,
  /** Wizard mode: Cancel on left, Submit on right (default: Cancel on right) */
  cancelOnLeft: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Custom test ID for the form (overrides default 'settings-form') */
  testId: PropTypes.string,
  /** Analytics ID for the submit button */
  submitAnalyticsId: PropTypes.string,
  /** Analytics ID for the cancel button */
  cancelAnalyticsId: PropTypes.string,
  /** Called when user confirms discard in the dialog (after form clears its own dirty state) */
  onDiscardConfirm: PropTypes.func,
  /** Explicit opt-in to skip internal dirty tracking when parent manages state via form machine */
  externalDirtyTracking: PropTypes.bool,
};

export default SettingsForm;
