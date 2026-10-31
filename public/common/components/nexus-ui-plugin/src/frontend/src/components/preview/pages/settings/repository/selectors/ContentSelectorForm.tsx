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

import React, { useState, useCallback, useEffect } from 'react';
import { Box, Flex, Text, Code, Table, Badge } from '@radix-ui/themes';
import { Trash2, ChevronDown, ChevronRight, Plus, Loader2, Link } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsButton,
  SettingsAlert,
  ConfirmDialog,
} from '../../../../shared/form';
import { clearDirtyState } from '../../../../shared/hooks/useUnsavedChangesWarning';
import { useContentSelectorsApi, PrivilegeReference } from './useContentSelectorsApi';
import { useContentSelectorForm } from './useContentSelectorForm';
import { ContentSelectorPreview } from './ContentSelectorPreview';
import { CSELEditor } from './CSELEditor';
import { ExpressionPreview } from './ExpressionPreview';
import { CSEL_CONFIG } from './cselConfig';
import { ValidationResult } from './cselValidator';
import {
  ContentSelector,
  CONTENT_SELECTOR_TYPE,
} from './types';

import './ContentSelectorForm.scss';

interface ContentSelectorFormProps {
  selector?: ContentSelector;
  isCreate: boolean;
  canDelete?: boolean;
  /**
   * When false, hides the Save button so a user without nexus:selectors:update
   * cannot submit edits (NEXUS-54212). The route only requires nexus:selectors:read,
   * so a read-only user can open the detail. Defaults to true; create mode is gated
   * on nexus:selectors:create at navigation.
   */
  canEdit?: boolean;
  onCancel: () => void;
  onComplete?: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * ContentSelectorForm - Create/Edit form for content selectors with enhanced CSEL editor
 *
 * Uses XState for state management via createFormMachine pattern.
 *
 * Features:
 * - CSEL Editor with autocomplete and inline validation
 * - Click-to-insert examples
 * - Collapsible reference documentation
 * - Human-readable expression preview
 * - Delete confirmation via machine state (no window.confirm)
 */
export function ContentSelectorForm({
  selector,
  isCreate,
  canDelete,
  canEdit = true,
  onCancel,
  onComplete,
  loading = false,
  error,
}: ContentSelectorFormProps) {
  const {
    createContentSelector,
    updateContentSelector,
    deleteContentSelector,
    fetchPrivilegesForSelector,
  } = useContentSelectorsApi();

  // State for attached privileges (only for edit mode)
  const [attachedPrivileges, setAttachedPrivileges] = useState<PrivilegeReference[]>([]);
  const [_loadingPrivileges, setLoadingPrivileges] = useState(false);

  // Form ID must match contentSelectorFormMachine's id exactly:
  // - Edit mode: `content-selector-form-${selectorName}`
  // - Create mode: `content-selector-form-new`
  // This formId is used to clear dirty state on discard.
  const formId = `content-selector-form-${selector?.name ?? 'new'}`;

  // Handle discard confirmation - clear machine's dirty state before navigating
  const handleDiscardConfirm = useCallback(() => {
    clearDirtyState(formId);
    onCancel();
  }, [formId, onCancel]);

  // Fetch attached privileges when editing an existing selector
  useEffect(() => {
    if (!isCreate && selector?.name) {
      setLoadingPrivileges(true);
      fetchPrivilegesForSelector(selector.name)
        .then((privileges) => {
          setAttachedPrivileges(privileges);
        })
        .finally(() => {
          setLoadingPrivileges(false);
        });
    }
  }, [isCreate, selector?.name, fetchPrivilegesForSelector]);

  // Use XState form hook
  const { form } = useContentSelectorForm({
    selectorName: isCreate ? undefined : selector?.name,
    selector: selector || undefined,
    onCancel,
    onComplete,
    createContentSelector,
    updateContentSelector,
    deleteContentSelector,
  });

  // UI-only state: collapsible reference docs and example insertion highlight
  const [showReference, setShowReference] = useState(false);
  const [insertedHighlight, setInsertedHighlight] = useState<string | null>(null);

  // Access form state and context
  const isLoading = form.isLoading;
  const isSaving = form.isSaving;
  const isDeleting = form.isDeleting;
  const isInDeleteFlow = form.isConfirmingDelete || isDeleting;
  const context = form.state.context as any;
  const formData = context.data;
  const expressionHasBlockingErrors = context.expressionHasBlockingErrors || false;

  // Handle CSEL editor validation changes - update machine context
  // Depend on form.send (stable from useMachine) to avoid infinite re-render loops
  const handleValidationChange = useCallback(
    (result: ValidationResult) => {
      form.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: result.hasBlockingErrors,
      } as any);
    },
    [form.send]
  );

  // Insert example expression - replaces current content for safety
  const insertExample = useCallback(
    (expression: string) => {
      form.send({ type: 'UPDATE', name: 'expression', value: expression } as any);

      // Briefly highlight the inserted expression
      setInsertedHighlight(expression);
      setTimeout(() => setInsertedHighlight(null), 2000);
    },
    [form.send]
  );

  // Show loading state while form loads
  if (isLoading) {
    return (
      <Flex align="center" justify="center" className="content-selector-form__loading">
        <Loader2 size={24} className="content-selector-form__spinner" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  return (
    <Box className="content-selector-form">
        <SettingsForm
          onSubmit={canEdit ? () => form.submit() : undefined}
          onCancel={onCancel}
          onDiscardConfirm={handleDiscardConfirm}
          loading={isSaving || isDeleting || loading}
          pristine={form.isPristine || isInDeleteFlow}
          externalDirtyTracking={true}
          error={error || form.saveError || undefined}
          submitLabel={isCreate ? 'Create' : 'Save'}
          noDirtyTracking={isInDeleteFlow}
          confirmDiscard={true}
          testId="content-selector-form"
          submitAnalyticsId={isCreate ? 'nxrm-content-selector-create' : 'nxrm-content-selector-save'}
          cancelAnalyticsId="nxrm-content-selector-cancel"
          data-mode={isCreate ? 'create' : 'edit'}
          data-loading={loading ? 'true' : 'false'}
          data-dirty={!form.isPristine ? 'true' : 'false'}
          data-valid={!form.hasValidationErrors ? 'true' : 'false'}
          aria-busy={loading}
          footerExtra={
            !isCreate ? (
              <SettingsButton
                variant="danger"
                onClick={() => form.requestDelete()}
                disabled={!canDelete || isSaving || isDeleting || loading || attachedPrivileges.length > 0}
                icon={Trash2}
                testId="form-delete"
                title={attachedPrivileges.length > 0 ? `Cannot delete: used by ${attachedPrivileges.length} privilege${attachedPrivileges.length === 1 ? '' : 's'}` : undefined}
              >
                Delete
              </SettingsButton>
            ) : undefined
          }
        >
          {/* Delete Confirmation Dialog */}
          <ConfirmDialog
            open={form.isConfirmingDelete}
            testId="delete-content-selector-dialog"
            analyticsId="nxrm-content-selector-delete"
            onOpenChange={(open) => { if (!open) form.cancelDelete(); }}
            title="Delete Content Selector"
            message={`Are you sure you want to delete the content selector "${formData?.name}"? This action cannot be undone.`}
            confirmLabel="Delete"
            cancelLabel="Cancel"
            variant="danger"
            onConfirm={() => form.confirmDelete()}
          />

          {/* Delete Error */}
          {form.deleteError && (
            <Box mb="3">
              <SettingsAlert type="error">
                Delete failed: {form.deleteError}
              </SettingsAlert>
            </Box>
          )}

          {/* Attached Privileges Warning (edit mode only) */}
          {!isCreate && attachedPrivileges.length > 0 && (
            <Box mb="3">
              <SettingsAlert type="info">
                <Flex align="center" gap="2">
                  <Link size={14} />
                  <Text size="2">
                    This selector is used by {attachedPrivileges.length} privilege{attachedPrivileges.length === 1 ? '' : 's'}:{' '}
                    <Text weight="medium">
                      {attachedPrivileges.map((p) => p.name).join(', ')}
                    </Text>
                  </Text>
                </Flex>
              </SettingsAlert>
            </Box>
          )}

          {/* Basic Info Section */}
          <SettingsFormSection title="Selector Settings">
            {isCreate ? (
              <SettingsTextInput
                {...form.field('name')}
                label="Name"
                helpText="Unique name for this content selector. Cannot be changed after creation."
                required
              />
            ) : (
              <Box className="content-selector-form__readonly">
                <Text as="label" size="2" weight="medium" className="content-selector-form__label">
                  Name
                </Text>
                <Text size="2">{formData?.name}</Text>
              </Box>
            )}

            <Box className="content-selector-form__readonly">
              <Text as="label" size="2" weight="medium" className="content-selector-form__label">
                Type
              </Text>
              <Text size="2">{CONTENT_SELECTOR_TYPE.toUpperCase()}</Text>
            </Box>

            <SettingsTextInput
              {...form.field('description')}
              label="Description"
            />
          </SettingsFormSection>

          {/* CSEL Expression Editor Section */}
          <SettingsFormSection title="Search Expression">
            <Text as="p" size="2" color="gray" className="content-selector-form__sublabel">
              Define the expression to identify repository content. Start typing for autocomplete suggestions.
            </Text>

            <CSELEditor
              value={formData?.expression || ''}
              onChange={(value: string) =>
                form.send({ type: 'UPDATE', name: 'expression', value } as any)
              }
              onValidationChange={handleValidationChange}
              placeholder='Enter CSEL expression (e.g., format == "maven2")'
              rows={4}
            />

            {/* Show expression validation error from machine */}
            {form.validationErrors.expression && form.touched.expression && (
              <Text size="1" color="red" mt="1">
                {form.validationErrors.expression}
              </Text>
            )}

            {/* Human-readable preview */}
            <ExpressionPreview expression={formData?.expression || ''} />
          </SettingsFormSection>

          {/* Examples Section - Click to use as starting point */}
          <Box className="content-selector-form__examples-section">
            <Text as="h4" size="2" weight="medium" className="content-selector-form__examples-title">
              Example Expressions
              <Text size="1" color="gray" style={{ fontWeight: 'normal', marginLeft: '8px' }}>
                Click to use as starting point
              </Text>
            </Text>
            <Flex wrap="wrap" gap="2" className="content-selector-form__examples-list">
              {CSEL_CONFIG.examples.map((example, index) => (
                <Box
                  key={index}
                  className={`content-selector-form__example-chip ${
                    insertedHighlight === example.expression
                      ? 'content-selector-form__example-chip--inserted'
                      : ''
                  }`}
                  onClick={() => insertExample(example.expression)}
                  title={example.description}
                >
                  <Plus size={12} className="content-selector-form__example-icon" />
                  <Code size="1">{example.expression}</Code>
                </Box>
              ))}
            </Flex>
          </Box>

          {/* Collapsible Reference Documentation */}
          <Box className="content-selector-form__reference">
            <Flex
              align="center"
              gap="2"
              className="content-selector-form__reference-toggle"
              onClick={() => setShowReference(!showReference)}
            >
              {showReference ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
              <Text size="2" weight="medium">
                CSEL Reference Documentation
              </Text>
            </Flex>

            {showReference && (
              <Box className="content-selector-form__reference-content">
                {/* Attributes */}
                <Box className="content-selector-form__reference-section">
                  <Text as="h4" size="2" weight="medium">Available Attributes</Text>
                  <Table.Root size="1">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Attribute</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Operators</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {CSEL_CONFIG.attributes.map((attr, index) => (
                        <Table.Row key={index}>
                          <Table.Cell>
                            <Code size="1">{attr.name}</Code>
                            {attr.formatSpecific && (
                              <Badge size="1" color="gray" style={{ marginLeft: '4px' }}>
                                {attr.formatSpecific}
                              </Badge>
                            )}
                          </Table.Cell>
                          <Table.Cell>{attr.description}</Table.Cell>
                          <Table.Cell>
                            <Code size="1">{attr.operators.join(', ')}</Code>
                          </Table.Cell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table.Root>
                </Box>

                {/* Operators */}
                <Box className="content-selector-form__reference-section">
                  <Text as="h4" size="2" weight="medium">Comparison Operators</Text>
                  <Table.Root size="1">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Operator</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {CSEL_CONFIG.operators.map((op, index) => (
                        <Table.Row key={index}>
                          <Table.Cell>
                            <Code size="1">{op.symbol}</Code>
                          </Table.Cell>
                          <Table.Cell>{op.description}</Table.Cell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table.Root>
                </Box>

                {/* Logical Operators */}
                <Box className="content-selector-form__reference-section">
                  <Text as="h4" size="2" weight="medium">Logical Operators</Text>
                  <Table.Root size="1">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Operator</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {CSEL_CONFIG.logicalOperators.map((op, index) => (
                        <Table.Row key={index}>
                          <Table.Cell>
                            <Code size="1">{op.symbol}</Code>
                          </Table.Cell>
                          <Table.Cell>{op.description}</Table.Cell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table.Root>
                </Box>
              </Box>
            )}
          </Box>

          {/* Preview Content Section */}
          {formData?.expression && !expressionHasBlockingErrors && (
            <SettingsFormSection title="Preview Content Selector Results">
              <ContentSelectorPreview
                type={CONTENT_SELECTOR_TYPE}
                expression={formData.expression}
              />
            </SettingsFormSection>
          )}
        </SettingsForm>
    </Box>
  );
}

export default ContentSelectorForm;
