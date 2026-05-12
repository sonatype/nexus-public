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
import PropTypes from 'prop-types';
import { Box, Flex, ScrollArea, Heading, Text } from '@radix-ui/themes';
import { SettingsButton } from './SettingsButton';
import { SettingsAlert } from './SettingsAlert';

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
  showActions = true,
  // Whether to show the header section
  showHeader = true,
  // New props for header actions and footer extras
  headerActions,
  footerExtra,
  className = '',
  // Testability props
  testId,
  // Spread additional data-* attributes
  ...restProps
}) {
  // Normalize props - support both naming conventions
  const isLoading = saving || loading;
  // If pristine is explicitly passed, use it; otherwise use !dirty
  const isPristine = pristine !== undefined ? pristine : !dirty;
  const saveHandler = onSave || onSubmit;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!isLoading && !isPristine && !submitDisabled && saveHandler) {
      saveHandler(e);
    }
  };

  const isSubmitDisabled = isLoading || isPristine || submitDisabled;

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
      data-testid={testId || 'settings-form'}
      data-loading={isLoading ? 'true' : 'false'}
      data-dirty={dirty ? 'true' : 'false'}
      data-pristine={isPristine ? 'true' : 'false'}
      data-submit-disabled={isSubmitDisabled ? 'true' : 'false'}
      aria-busy={isLoading}
      {...dataProps}
    >
      {/* Header */}
      {showHeader && (
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

      {/* Alerts */}
      {error && (
        <Box className="settings-form__alerts">
          <SettingsAlert type="error">{error}</SettingsAlert>
        </Box>
      )}
      {success && (
        <Box className="settings-form__alerts">
          <SettingsAlert type="success">{success}</SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <ScrollArea className="settings-form__content" scrollbars="vertical">
        <Box className="settings-form__inner">
          {children}
        </Box>
      </ScrollArea>

      {/* Footer Actions */}
      {showActions && (
        <Box className="settings-form__footer">
          <Flex gap="3" justify="between" align="center">
            <Flex gap="3" align="center">
              {saveHandler && (
                <SettingsButton
                  type="submit"
                  variant="primary"
                  disabled={isSubmitDisabled}
                  loading={isLoading}
                  testId="form-submit"
                >
                  {submitLabel}
                </SettingsButton>
              )}
              {onCancel && (
                <SettingsButton
                  type="button"
                  variant="secondary"
                  onClick={onCancel}
                  disabled={isLoading}
                  testId="form-cancel"
                >
                  {cancelLabel}
                </SettingsButton>
              )}
              {!isPristine && (
                <Text size="1" className="settings-form__unsaved">
                  You have unsaved changes
                </Text>
              )}
            </Flex>
            {footerExtra && (
              <Flex gap="2" className="settings-form__footer-extra">
                {footerExtra}
              </Flex>
            )}
          </Flex>
        </Box>
      )}
    </form>
  );
}

SettingsForm.propTypes = {
  /** Page title displayed in header */
  title: PropTypes.string.isRequired,
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
};

export default SettingsForm;
