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

import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { Box, Flex, Text, ScrollArea } from '@radix-ui/themes';
import { Trash2, Loader2 } from 'lucide-react';
import DOMPurify from 'dompurify';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsTextArea,
  SettingsButton,
  SettingsAlert,
  SettingsSelect,
  SettingsCombobox,
} from '../../../shared/form';
import { useCapabilitiesApi } from './useCapabilitiesApi';
import { useCapabilitiesForm } from './useCapabilitiesForm';
import { restClient } from '@/utils/api';
import { Capability, CapabilityType, CapabilityFormData, FormField } from './types';

import './DynamicCapabilityForm.scss';

const WEBHOOK_EVENT_TYPES: Record<string, Array<{value: string; label: string}>> = {
  'webhook.global': [
    {value: 'repository', label: 'Repository'},
  ],
  'webhook.repository': [
    {value: 'asset', label: 'Asset'},
    {value: 'component', label: 'Component'},
  ],
};

interface DynamicCapabilityFormProps {
  capability?: Capability | null;
  capabilityType: CapabilityType;
  isCreate: boolean;
  onSave: (data: CapabilityFormData) => Promise<void>;
  onCancel: () => void;
  /** Called after successful save - navigates to list. Defaults to onCancel if not provided. */
  onSaveComplete?: () => void;
  onDelete?: () => void;
  loading?: boolean;
  error?: string;
  /** When true, skip the SettingsForm wrapper (for use in WizardForm) */
  embedded?: boolean;
  /** Ref callback to expose submit function (for embedded mode) */
  onSubmitRef?: (submitFn: () => void) => void;
}

/**
 * DynamicCapabilityForm - Generates form fields dynamically based on capability type
 * Now uses XState for state management via useCapabilitiesForm hook
 */
export function DynamicCapabilityForm({
  capability,
  capabilityType,
  isCreate,
  onSave,
  onCancel,
  onSaveComplete,
  onDelete,
  loading = false,
  error,
  embedded = false,
  onSubmitRef,
}: DynamicCapabilityFormProps) {
  const { createCapability, updateCapability, deleteCapability } = useCapabilitiesApi();

  // Use XState form hook
  const {
    form,
    capability: loadedCapability,
    isCreate: hookIsCreate,
  } = useCapabilitiesForm({
    capabilityId: isCreate ? undefined : capability?.id,
    capability: capability || undefined,
    initialTypeId: capabilityType?.id,
    onCancel,
    onSaveComplete,
    createCapability,
    updateCapability,
    deleteCapability: onDelete ? (id) => {
      if (onDelete) onDelete();
      return Promise.resolve();
    } : undefined,
  });

  const formData = form.data as CapabilityFormData;

  // Sync the parent's capabilityType with the machine context if needed.
  // We've improved createCapabilityFormMachine to accept initialTypeId,
  // so this is mostly for safety or when capabilityType changes later.
  useEffect(() => {
    if (capabilityType?.id && !form.isLoading) {
      const currentTypeId = formData.typeId;
      if (!currentTypeId) {
        form.send({ type: 'CAPABILITY_TYPE_CHANGE', value: capabilityType.id });
      }
    }
  }, [capabilityType?.id, form.isLoading, formData.typeId, form]);

  // Load repository options for repo-target fields
  const [repoOptions, setRepoOptions] = useState<{value: string; label: string}[]>([]);
  useEffect(() => {
    restClient.get<{name: string}[]>('/service/rest/v1/repositories')
      .then((repos) => {
        if (Array.isArray(repos)) {
          setRepoOptions(repos.map((r) => ({ value: r.name, label: r.name })));
        }
      })
      .catch((err) => console.warn('Failed to load reference data:', err));
  }, []);

  // Handle field value change via machine
  const handleFieldChange = useCallback((fieldId: string, value: string) => {
    const currentProps = formData.properties || {};
    form.send({ type: 'UPDATE', name: 'properties', value: { ...currentProps, [fieldId]: value } });
  }, [form, formData.properties]);

  // Render a form field based on its type
  const renderField = useCallback((field: FormField) => {
    const value = formData.properties?.[field.id] ?? '';
    const fieldTouched = form.touched?.[`properties.${field.id}`] || form.touched?.properties;
    const fieldError = fieldTouched ? form.validationErrors?.[`properties.${field.id}`] : undefined;

    // Fix for nexus-internal-dpv8: use combobox for repository field
    const isRepoField = field.id === 'repository' || field.type === 'repo-target' || field.type === 'repo-or-group-target';

    switch (field.type) {
      case 'password':
        return (
          <SettingsPasswordInput
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
            autoComplete="new-password"
          />
        );

      case 'boolean':
        return (
          <SettingsCheckbox
            key={field.id}
            name={field.id}
            label={field.label}
            checked={value === 'true'}
            onChange={(checked) => handleFieldChange(field.id, String(checked))}
            helpText={field.helpText}
            disabled={field.disabled || field.readOnly}
          />
        );

      case 'text':
        return (
          <SettingsTextArea
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
            rows={4}
          />
        );

      case 'number':
        return (
          <SettingsTextInput
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
            type="number"
            min={field.minValue}
            max={field.maxValue}
          />
        );

      case 'url':
        return (
          <SettingsTextInput
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
            type="url"
            placeholder="https://..."
          />
        );

      case 'itemselect': {
        const eventOptions = WEBHOOK_EVENT_TYPES[capabilityType.id];
        if (eventOptions) {
          const selectedValues = new Set(
            value ? value.split(',').map((v: string) => v.trim()).filter(Boolean) : []
          );
          return (
            <Box key={field.id} mb="3">
              <Text as="label" size="2" weight="medium" mb="1" style={{display: 'block'}}>
                {field.label} {field.required && <Text color="red">*</Text>}
              </Text>
              {field.helpText && <Text size="1" color="gray" mb="2" style={{display: 'block'}}>{field.helpText}</Text>}
              <Flex direction="column" gap="2" data-testid={`event-types-${field.id}`}>
                {eventOptions.map((opt) => (
                  <SettingsCheckbox
                    key={opt.value}
                    name={`${field.id}-${opt.value}`}
                    label={opt.label}
                    checked={selectedValues.has(opt.value)}
                    onChange={(checked) => {
                      const next = new Set(selectedValues);
                      if (checked) { next.add(opt.value); } else { next.delete(opt.value); }
                      handleFieldChange(field.id, Array.from(next).join(','));
                    }}
                    disabled={field.disabled || field.readOnly}
                  />
                ))}
              </Flex>
              {fieldError && <Text size="1" color="red">{fieldError}</Text>}
            </Box>
          );
        }
        return (
          <SettingsTextInput
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
          />
        );
      }

      case 'combobox':
        if (isRepoField) {
          return (
            <SettingsCombobox
              key={field.id}
              name={field.id}
              label={field.label}
              value={value}
              onChange={(val) => handleFieldChange(field.id, val)}
              options={repoOptions}
              helpText={field.helpText}
              error={fieldError}
              required={field.required}
              disabled={field.disabled || field.readOnly}
              allowCustom
              placeholder="Select repository..."
            />
          );
        }
        return (
          <SettingsTextInput
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
          />
        );

      case 'repo-or-group-target':
      case 'repo-target':
        return (
          <SettingsCombobox
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            options={repoOptions}
            helpText={field.helpText || 'Select a repository'}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
            allowCustom
            placeholder="Select repository..."
          />
        );

      case 'string':
      default:
        return (
          <SettingsTextInput
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            helpText={field.helpText}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
          />
        );
    }
  }, [formData.properties, form.validationErrors, handleFieldChange, repoOptions]);

  const formFields = capabilityType.formFields || [];

  // Expose submit handler for embedded mode
  const handleSubmit = useCallback(() => {
    form.send('SUBMIT');
  }, [form]);

  // Expose submit function to parent via ref callback (for embedded mode)
  useEffect(() => {
    if (embedded && onSubmitRef) {
      onSubmitRef(handleSubmit);
    }
  }, [embedded, onSubmitRef, handleSubmit]);

  // Show loading state
  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" gap="2" p="4">
        <Loader2 size={24} className="dynamic-capability-form__spinner" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  const formContent = (
    <>
      {/* Capability Type Info */}
      <SettingsFormSection title="Capability Type">
        <Box className="dynamic-capability-form__type-info">
          <Text weight="medium">{capabilityType.name}</Text>
          {capabilityType.about && (
            <ScrollArea scrollbars="vertical" style={{ maxHeight: '150px' }}>
              <Text
                size="2"
                className="dynamic-capability-form__about"
                dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(capabilityType.about) }}
              />
            </ScrollArea>
          )}
        </Box>
      </SettingsFormSection>

      {/* Settings Section */}
      <SettingsFormSection title="Settings">
        <SettingsCheckbox
          name="enabled"
          label="Enable this capability"
          checked={formData.enabled ?? true}
          onChange={(checked) => form.send({ type: 'UPDATE', name: 'enabled', value: checked })}
          helpText="When enabled, this capability will be active"
        />

        {/* Dynamic Form Fields */}
        {formFields.map((field) => renderField(field))}
      </SettingsFormSection>

      {/* Notes Section */}
      <SettingsFormSection title="Notes">
        <SettingsTextArea
          {...form.field('notes')}
          label="Notes"
          helpText="Optional notes about this capability"
          rows={3}
        />
      </SettingsFormSection>
    </>
  );

  // If embedded, render content without SettingsForm wrapper
  if (embedded) {
    return (
      <Box className="dynamic-capability-form">
        {formContent}
      </Box>
    );
  }

  // Otherwise, render with SettingsForm wrapper
  return (
    <Box className="dynamic-capability-form">
      <SettingsForm
        testId="capability-form"
        onSubmit={() => form.send('SUBMIT')}
        onCancel={onCancel}
        loading={form.isSaving || loading}
        pristine={form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create' : 'Save'}
        cancelLabel="Cancel"
        footerExtra={
          !isCreate && onDelete ? (
            <SettingsButton
              variant="danger"
              onClick={onDelete}
              disabled={form.isSaving || loading}
              icon={Trash2}
            >
              Delete
            </SettingsButton>
          ) : undefined
        }
      >
        {formContent}
      </SettingsForm>
    </Box>
  );
}

export default DynamicCapabilityForm;
