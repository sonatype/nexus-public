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

import React, { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { AlertDialog, Box, Button, Flex, Heading, Text } from '@radix-ui/themes';
import { AlertCircle } from 'lucide-react';
import { SettingsButton } from './SettingsButton';
import { SettingsAlert } from './SettingsAlert';
import { useUnsavedChangesWarning, clearDirtyState } from '../../../shared';

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
  showActions = true,
  // Whether to show the header section
  showHeader = true,
  // New props for header actions and footer extras
  headerActions,
  footerExtra,
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
    // Clear this specific form from dirty tracking
    clearDirtyState(formIdRef.current);
    if (onCancel) onCancel();
  };

  const formIdRef = useRef(`settings-form-${Math.random().toString(36).slice(2, 8)}`);

  // Automatically track unsaved changes and warn on navigation
  // Hook clears automatically when form becomes pristine (isDirty = false)
  // Skip tracking if noDirtyTracking is enabled (e.g., one-shot upload forms)
  useUnsavedChangesWarning(!isPristine && !noDirtyTracking, formIdRef.current);

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
          <Flex gap="3" align="center">
            {!isPristine && !error && !noDirtyTracking && (
              <Text size="1" className="settings-form__unsaved">
                Unsaved changes
              </Text>
            )}
            {onCancel && (
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={handleCancelClick}
                disabled={isLoading || cancelDisabled}
                testId="form-cancel"
                data-analytics-id={cancelAnalyticsId}
              >
                {cancelLabel}
              </SettingsButton>
            )}
            {saveHandler && (
              <SettingsButton
                type="submit"
                variant="primary"
                disabled={isSubmitDisabled}
                loading={isLoading}
                testId="form-submit"
                data-analytics-id={submitAnalyticsId}
              >
                {submitLabel}
              </SettingsButton>
            )}
          </Flex>
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
  /** Additional CSS class */
  className: PropTypes.string,
  /** Custom test ID for the form (overrides default 'settings-form') */
  testId: PropTypes.string,
  /** Analytics ID for the submit button */
  submitAnalyticsId: PropTypes.string,
  /** Analytics ID for the cancel button */
  cancelAnalyticsId: PropTypes.string,
};

export default SettingsForm;
