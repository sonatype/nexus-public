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
import { Box, Flex, Text, Checkbox } from '@radix-ui/themes';
import { Trash2, AlertCircle, Info, Loader2 } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
} from '../../../shared/form';
import { CleanupPolicyPreview } from './CleanupPolicyPreview';
import { CleanupPolicyDryRun } from './CleanupPolicyDryRun';
import { useCleanupPoliciesApi } from './useCleanupPoliciesApi';
import { useCleanupPolicyForm } from './useCleanupPolicyForm';
import {
  CleanupPolicy,
  CleanupPolicyFormData,
  FormatCriteria,
  RELEASE_TYPES,
  SORT_BY_OPTIONS,
  NOTES_MAX_LENGTH,
  isValidCriteriaNumber,
  isRetainSupportedFormat,
  isReleaseType,
  getDefaultSortBy,
  hasCriteriaSelected,
} from './types';

import './CleanupPolicyForm.scss';

interface CleanupPolicyFormProps {
  policy?: CleanupPolicy;
  isCreate: boolean;
  formatCriteria: FormatCriteria[];
  canDelete?: boolean;
  onSave: (data: CleanupPolicyFormData) => Promise<void>;
  onDelete?: () => void;
  onCancel: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * CleanupPolicyForm - Create/Edit form for cleanup policies
 * Now uses XState for state management via useCleanupPolicyForm hook
 */
export function CleanupPolicyForm({
  policy,
  isCreate,
  formatCriteria,
  canDelete,
  onSave,
  onDelete,
  onCancel,
  loading,
  error,
}: CleanupPolicyFormProps) {
  const { createCleanupPolicy, updateCleanupPolicy, isPreviewEnabled, isRetainEnabled: checkRetainEnabled } = useCleanupPoliciesApi();

  // Use XState form hook
  const {
    form,
    policy: loadedPolicy,
    criteriaEnabled,
    changeFormat,
    toggleCriteria,
    changeReleaseType,
  } = useCleanupPolicyForm({
    policyName: isCreate ? undefined : policy?.name,
    policy: policy || null,
    formatCriteria,
    onSave,
    onCancel,
    createPolicy: createCleanupPolicy,
    updatePolicy: updateCleanupPolicy,
  });

  const formData = form.data as CleanupPolicyFormData;

  // Get current format's available criteria
  const currentFormatCriteria = useMemo(() => {
    return formatCriteria.find((fc) => fc.id === formData.format);
  }, [formatCriteria, formData.format]);

  const isFieldApplicable = useCallback(
    (fieldId: string): boolean => {
      return currentFormatCriteria?.availableCriteria?.includes(fieldId) ?? false;
    },
    [currentFormatCriteria]
  );

  const showRetain = useMemo(() => {
    return (
      isRetainSupportedFormat(formData.format) &&
      checkRetainEnabled(formData.format) &&
      isReleaseType(formData.criteriaReleaseType)
    );
  }, [formData.format, formData.criteriaReleaseType, checkRetainEnabled]);

  const showPreview = useMemo(() => isPreviewEnabled(), [isPreviewEnabled]);

  // Check if criteria section should show
  const showCriteriaSection = useMemo(() => {
    return (
      formData.format &&
      (isFieldApplicable('lastBlobUpdated') ||
        isFieldApplicable('lastDownloaded') ||
        isFieldApplicable('regex'))
    );
  }, [formData.format, isFieldApplicable]);

  // Show loading state
  if (form.isLoading || loading) {
    return (
      <Box className="cleanup-policy-form">
        <Flex align="center" justify="center" gap="2" p="4">
          <Loader2 size={24} className="cleanup-policy-form__spinner" />
          <Text>Loading...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box className="cleanup-policy-form">
      <SettingsForm
          onSubmit={() => form.send('SUBMIT')}
          onCancel={onCancel}
          loading={form.isSaving}
          pristine={form.isPristine}
          error={error || form.saveError || undefined}
          submitLabel={isCreate ? 'Create' : 'Save'}
          cancelLabel="Cancel"
          testId="cleanup-policy-form"
          footerExtra={
            canDelete && onDelete ? (
              <SettingsButton
                variant="danger"
                onClick={onDelete}
                icon={Trash2}
                disabled={form.isSaving}
                testId="form-delete"
              >
                Delete
              </SettingsButton>
            ) : undefined
          }
        >
          <Box mb="4">
            <Text as="p" size="2">
              <strong>Cleanup policies</strong> allow you to automatically delete unused components and
              free up storage space. Define <strong>cleanup criteria</strong> for selecting components to delete.
            </Text>
          </Box>

          {/* Basic Info Section */}
          <SettingsFormSection title="Policy Settings">
            <SettingsTextInput
              {...form.field('name')}
              label="Name"
              sublabel="Use a unique name for the cleanup policy"
              helpText="Unique policy name (e.g., delete-old-releases)"
              disabled={!isCreate}
              required
              placeholder="delete-old-snapshots"
            />

            <SettingsSelect
              name="format"
              label="Format"
              helpText="The format that this cleanup policy can be applied to"
              value={formData.format || ''}
              onChange={changeFormat}
              required
              placeholder="Select a format..."
              options={formatCriteria.map((fc) => ({
                value: fc.id,
                label: fc.name,
              }))}
            />

            <SettingsTextArea
              {...form.field('notes')}
              label="Description"
              helpText="Optional notes about why this policy exists"
              maxLength={NOTES_MAX_LENGTH}
            />
          </SettingsFormSection>

          {/* Release Type (for applicable formats) */}
          {isFieldApplicable('isPrerelease') && (
            <SettingsFormSection title="Release Type">
              <SettingsSelect
                name="releaseType"
                label="Release Type"
                helpText="Remove components that are of the following release type"
                value={formData.criteriaReleaseType || ''}
                onChange={changeReleaseType}
                placeholder=""
                options={Object.values(RELEASE_TYPES).map((type) => ({
                  value: type.id,
                  label: type.label,
                }))}
              />
            </SettingsFormSection>
          )}

          {/* Cleanup Criteria Section */}
          {showCriteriaSection && (
            <SettingsFormSection
              title="Cleanup Criteria"
              description="Remove all components that match all selected criteria. At least one criterion must be selected."
            >
              {form.touched?.criteriaSelected && form.validationErrors?.criteriaSelected && (
                <Box className="cleanup-policy-form__criteria-error">
                  <SettingsAlert type="error">
                    <Flex align="center" gap="2">
                      <AlertCircle size={16} />
                      {form.validationErrors.criteriaSelected}
                    </Flex>
                  </SettingsAlert>
                </Box>
              )}

              {/* Component Age */}
              {isFieldApplicable('lastBlobUpdated') && (
                <Box className="cleanup-policy-form__criteria-item">
                  <Flex align="start" gap="3">
                    <Checkbox
                      data-testid="checkbox-criteria-lastBlobUpdated"
                      checked={criteriaEnabled?.lastBlobUpdated || false}
                      onCheckedChange={(checked) =>
                        toggleCriteria('lastBlobUpdated', checked === true)
                      }
                    />
                    <Box className="cleanup-policy-form__criteria-content">
                      <Text as="label" size="2" weight="medium">
                        Component Age
                      </Text>
                      <Text as="p" size="1" color="gray">
                        Components published over "x" days ago (e.g 1-9999)
                      </Text>
                      {criteriaEnabled?.lastBlobUpdated && (
                        <Box className="cleanup-policy-form__criteria-input">
                          <SettingsTextInput
                            {...form.field('criteriaLastBlobUpdated')}
                            label=""
                            placeholder="e.g 100 days"
                            type="number"
                          />
                        </Box>
                      )}
                    </Box>
                  </Flex>
                </Box>
              )}

              {/* Component Usage */}
              {isFieldApplicable('lastDownloaded') && (
                <Box className="cleanup-policy-form__criteria-item">
                  <Flex align="start" gap="3">
                    <Checkbox
                      data-testid="checkbox-criteria-lastDownloaded"
                      checked={criteriaEnabled?.lastDownloaded || false}
                      onCheckedChange={(checked) =>
                        toggleCriteria('lastDownloaded', checked === true)
                      }
                    />
                    <Box className="cleanup-policy-form__criteria-content">
                      <Text as="label" size="2" weight="medium">
                        Component Usage
                      </Text>
                      <Text as="p" size="1" color="gray">
                        Components downloaded in "x" amount of days (e.g 1-9999)
                      </Text>
                      {criteriaEnabled?.lastDownloaded && (
                        <Box className="cleanup-policy-form__criteria-input">
                          <SettingsTextInput
                            {...form.field('criteriaLastDownloaded')}
                            label=""
                            placeholder="e.g 100 days"
                            type="number"
                          />
                        </Box>
                      )}
                    </Box>
                  </Flex>
                </Box>
              )}

              {/* Asset Name Matcher */}
              {isFieldApplicable('regex') && (
                <Box className="cleanup-policy-form__criteria-item">
                  <Flex align="start" gap="3">
                    <Checkbox
                      data-testid="checkbox-criteria-assetRegex"
                      checked={criteriaEnabled?.assetRegex || false}
                      onCheckedChange={(checked) =>
                        toggleCriteria('assetRegex', checked === true)
                      }
                    />
                    <Box className="cleanup-policy-form__criteria-content">
                      <Text as="label" size="2" weight="medium">
                        Asset Name Matcher
                      </Text>
                      <Text as="p" size="1" color="gray">
                        Remove components that have at least one asset name matching the following
                        regular expression pattern
                      </Text>
                      {criteriaEnabled?.assetRegex && (
                        <Box className="cleanup-policy-form__criteria-input">
                          <SettingsTextInput
                            {...form.field('criteriaAssetRegex')}
                            label=""
                            placeholder="e.g. .*-SNAPSHOT.*"
                          />
                        </Box>
                      )}
                    </Box>
                  </Flex>
                </Box>
              )}

              {/* Retain Versions (Exclusion Criteria) */}
              {showRetain && (
                <Box className="cleanup-policy-form__criteria-item cleanup-policy-form__exclusion">
                  <Flex align="center" gap="2" className="cleanup-policy-form__exclusion-header">
                    <Info size={16} />
                    <Text size="2" weight="medium">
                      Except, do not remove any component that meets the following criterion:
                    </Text>
                  </Flex>
                  <Flex align="start" gap="3" mt="3">
                    <Checkbox
                      data-testid="checkbox-criteria-retain"
                      checked={criteriaEnabled?.retain || false}
                      onCheckedChange={(checked) =>
                        toggleCriteria('retain', checked === true)
                      }
                    />
                    <Box className="cleanup-policy-form__criteria-content">
                      <Text as="label" size="2" weight="medium">
                        Number of Versions
                      </Text>
                      <Text as="p" size="1" color="gray">
                        Keep the latest "x" number of versions by{' '}
                        {formData.format === 'docker'
                          ? SORT_BY_OPTIONS.DATE.label
                          : SORT_BY_OPTIONS.VERSION.label}
                        .
                      </Text>
                      {criteriaEnabled?.retain && (
                        <Box className="cleanup-policy-form__criteria-input">
                          <SettingsTextInput
                            {...form.field('retain')}
                            label=""
                            placeholder="e.g. 5"
                            type="number"
                          />
                        </Box>
                      )}
                    </Box>
                  </Flex>
                </Box>
              )}
            </SettingsFormSection>
          )}

          {/* Preview Section */}
          {showPreview && formData.format && hasCriteriaSelected(formData) && (
            <SettingsFormSection title="Preview Cleanup Policy Results">
              <CleanupPolicyDryRun policyData={formData} policyName={policy?.name} />
            </SettingsFormSection>
          )}
        </SettingsForm>

      {/* Legacy Preview (for non-PostgreSQL) */}
      {!showPreview && formData.format && hasCriteriaSelected(formData) && (
        <CleanupPolicyPreview policyData={formData} />
      )}
    </Box>
  );
}

export default CleanupPolicyForm;
