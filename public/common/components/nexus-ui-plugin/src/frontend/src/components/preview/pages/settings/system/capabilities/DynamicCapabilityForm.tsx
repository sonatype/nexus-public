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

import React, { useCallback, useEffect, useState, } from 'react';
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
  SettingsCombobox,
  SettingsTransferList,
} from '../../../../shared/form';
import { useCapabilitiesApi } from './useCapabilitiesApi';
import { useCapabilitiesForm } from './useCapabilitiesForm';
import { restClient } from '../../../../../../interface/api';
import { Capability, CapabilityType, CapabilityFormData, FormField } from './types';

import './DynamicCapabilityForm.scss';

/**
 * Mapping from ExtDirect storeApi names to REST API endpoints.
 * The storeApi values come from Java capability descriptors (e.g., "coreui_Role.read").
 */
const STORE_API_TO_REST: Record<string, string> = {
  'coreui_Role.read': '/service/rest/internal/ui/roles',
  'coreui_Repository.readReferences': '/service/rest/internal/ui/repositories',
  'coreui_Repository.readReferencesAddingEntryForAll': '/service/rest/internal/ui/repositories?withAll=true',
  'coreui_Repository.readReferencesAddingEntriesForAllFormats': '/service/rest/internal/ui/repositories?withFormats=true',
  'coreui_Webhook.listWithTypeGlobal': '/service/rest/internal/ui/webhooks?type=global',
  'coreui_Webhook.listWithTypeRepository': '/service/rest/internal/ui/webhooks?type=repository',
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
  /**
   * When false, hides the Save button so a user without nexus:capabilities:update
   * cannot submit edits (NEXUS-54212). Matches Classic CapabilitySettingsForm, whose
   * editableCondition is NX.Conditions.isPermitted('nexus:capabilities:update').
   * Defaults to true; create mode is gated on nexus:capabilities:create at navigation.
   */
  canEdit?: boolean;
  /** When true, skip the SettingsForm wrapper (for use in WizardForm) */
  embedded?: boolean;
  /** Ref callback to expose submit function (for embedded mode) */
  onSubmitRef?: (submitFn: () => void) => void;
  /** Current enabled state (controlled from parent for sync with header buttons) */
  isEnabled?: boolean;
  /** Callback when enabled state changes from checkbox */
  onEnabledChange?: (enabled: boolean) => void;
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
  canEdit = true,
  embedded = false,
  onSubmitRef,
  isEnabled: isEnabledProp,
  onEnabledChange,
}: DynamicCapabilityFormProps) {
  const { createCapability, updateCapability } = useCapabilitiesApi();

  // Use XState form hook
  const {
    form,
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

  const formData = (form.data || {}) as CapabilityFormData;

  // Sync the parent's capabilityType with the machine context if needed.
  // We've improved createCapabilityFormMachine to accept initialTypeId,
  // so this is mostly for safety or when capabilityType changes later.
  useEffect(() => {
    if (capabilityType?.id && !form.isLoading && formData?.typeId !== undefined) {
      const currentTypeId = formData.typeId;
      if (!currentTypeId) {
        form.send({ type: 'CAPABILITY_TYPE_CHANGE', value: capabilityType.id });
      }
    }
  }, [capabilityType?.id, form.isLoading, formData?.typeId, form]);

  // Load repository options for repo-target fields (only if needed)
  const [repoOptions, setRepoOptions] = useState<{value: string; label: string}[]>([]);
  useEffect(() => {
    const formFields = capabilityType.formFields || [];
    const hasRepoField = formFields.some(
      (f) => f.type === 'repo-target' || f.type === 'repo-or-group-target' || f.id === 'repository'
    );
    if (!hasRepoField) return;

    restClient.get<{name: string}[]>('/service/rest/v1/repositories')
      .then((repos) => {
        if (Array.isArray(repos)) {
          setRepoOptions(repos.map((r) => ({ value: r.name, label: r.name })));
        }
      })
      .catch((err) => console.warn('Failed to load reference data:', err));
  }, [capabilityType.formFields]);

  // Load dynamic options for fields with storeApi (e.g., roles dropdown, repository selector)
  const [storeOptions, setStoreOptions] = useState<Record<string, {value: string; label: string}[]>>({});
  useEffect(() => {
    setStoreOptions({});
    const formFields = capabilityType.formFields || [];
    // Include fields with storeApi AND repo-target fields that need filtering
    const fieldsWithStoreApi = formFields.filter(
      (f) => f.storeApi && STORE_API_TO_REST[f.storeApi]
    );
    // Also include repo-target fields that have storeFilters
    const repoFieldsWithFilters = formFields.filter(
      (f) => (f.type === 'repo-target' || f.type === 'repo-or-group-target') && f.storeFilters
    );

    const seen = new Set<string>();
    const allFieldsToLoad = [...fieldsWithStoreApi, ...repoFieldsWithFilters].filter((f) => {
      if (seen.has(f.id)) return false;
      seen.add(f.id);
      return true;
    });
    if (allFieldsToLoad.length === 0) return;

    // Load options for each field
    allFieldsToLoad.forEach((field) => {
      const storeApi = field.storeApi || 'coreui_Repository.readReferences';
      const restEndpoint = STORE_API_TO_REST[storeApi];
      if (!restEndpoint) return;

      // Determine id/name mappings (defaults to id/name)
      const idKey = field.idMapping || 'id';
      const nameKey = field.nameMapping || 'name';

      // Build query params from storeFilters
      const params = new URLSearchParams();
      if (field.storeFilters) {
        Object.entries(field.storeFilters).forEach(([key, value]) => {
          if (value !== null && value !== undefined) {
            params.set(key, String(value));
          }
        });
      }

      const separator = restEndpoint.includes('?') ? '&' : '?';
      const url = params.toString() ? `${restEndpoint}${separator}${params.toString()}` : restEndpoint;

      restClient.get<Record<string, unknown>[]>(url)
        .then((items) => {
          if (Array.isArray(items)) {
            setStoreOptions((prev) => ({
              ...prev,
              [field.id]: items.map((item) => ({
                value: String(item[idKey] || ''),
                label: String(item[nameKey] || item[idKey] || ''),
              })),
            }));
          }
        })
        .catch((err) => console.warn(`Failed to load options for ${field.id}:`, err));
    });
  }, [capabilityType.formFields]);

  // Handle field value change via machine
  const handleFieldChange = useCallback((fieldId: string, value: string) => {
    const currentProps = formData?.properties || {};
    form.send({ type: 'UPDATE', name: 'properties', value: { ...currentProps, [fieldId]: value } });
  }, [form, formData?.properties]);

  // Render a form field based on its type
  const renderField = useCallback((field: FormField) => {
    const value = formData?.properties?.[field.id] ?? '';
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
      case 'checkbox':
        return (
          <SettingsCheckbox
            key={field.id}
            name={field.id}
            label={field.label}
            checked={value === 'true' || value === true}
            onChange={(checked) => handleFieldChange(field.id, String(checked))}
            helpText={field.helpText}
            disabled={field.disabled || field.readOnly}
          />
        );

      case 'text-area':
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
        const itemOptions = storeOptions[field.id];
        if (itemOptions === undefined && field.storeApi && STORE_API_TO_REST[field.storeApi]) {
          return (
            <Flex key={field.id} align="center" gap="2" p="2">
              <Loader2 size={16} className="dynamic-capability-form__spinner" />
              <Text size="2">Loading {field.label.toLowerCase()}...</Text>
            </Flex>
          );
        }
        if (itemOptions && itemOptions.length > 0) {
          const selectedValues = value ? value.split(',').map((v: string) => v.trim()).filter(Boolean) : [];
          const selectedItemObjects = selectedValues
            .map((v: string) => itemOptions.find((opt) => opt.value === v))
            .filter(Boolean);
          return (
            <SettingsTransferList
              key={field.id}
              name={field.id}
              label={field.label}
              availableItems={itemOptions}
              selectedItems={selectedItemObjects}
              onChange={(newSelected) => {
                const newValue = newSelected.map((item: {value: string; label: string}) => item.value).join(',');
                handleFieldChange(field.id, newValue);
              }}
              getItemId={(item) => item.value}
              getItemLabel={(item) => item.label}
              availableLabel="Available"
              selectedLabel="Selected"
              helpText={field.helpText}
              error={fieldError}
              required={field.required}
              disabled={field.disabled || field.readOnly}
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
      }

      case 'combobox':
        // Loading state for fields with storeApi
        if (field.storeApi && STORE_API_TO_REST[field.storeApi] && storeOptions[field.id] === undefined) {
          return (
            <SettingsCombobox
              key={field.id}
              name={field.id}
              label={field.label}
              value=""
              onChange={() => {}}
              options={[]}
              helpText={field.helpText}
              disabled
              placeholder="Loading..."
            />
          );
        }
        // Field with storeApi (dynamic options from REST API) — prefer filtered options
        if (field.storeApi && storeOptions[field.id]) {
          return (
            <SettingsCombobox
              key={field.id}
              name={field.id}
              label={field.label}
              value={value}
              onChange={(val) => handleFieldChange(field.id, val)}
              options={storeOptions[field.id]}
              helpText={field.helpText}
              error={fieldError}
              required={field.required}
              disabled={field.disabled || field.readOnly}
              allowCustom={field.allowAutocomplete}
              placeholder={`Select ${field.label.toLowerCase()}...`}
            />
          );
        }
        // Repository field without storeApi — use unfiltered repo list
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
        // Regular combobox without dynamic options (fallback to text input)
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
        // Use filtered options if available (with storeFilters), otherwise use all repos
        return (
          <SettingsCombobox
            key={field.id}
            name={field.id}
            label={field.label}
            value={value}
            onChange={(val) => handleFieldChange(field.id, val)}
            options={storeOptions[field.id] || repoOptions}
            helpText={field.helpText || 'Select a repository'}
            error={fieldError}
            required={field.required}
            disabled={field.disabled || field.readOnly}
            allowCustom
            placeholder="Select repository..."
          />
        );
      default:
        // Loading state for fields with storeApi
        if (field.storeApi && STORE_API_TO_REST[field.storeApi] && storeOptions[field.id] === undefined) {
          return (
            <SettingsCombobox
              key={field.id}
              name={field.id}
              label={field.label}
              value=""
              onChange={() => {}}
              options={[]}
              helpText={field.helpText}
              disabled
              placeholder="Loading..."
            />
          );
        }
        // Field with storeApi should be rendered as combobox
        if (field.storeApi && storeOptions[field.id]) {
          return (
            <SettingsCombobox
              key={field.id}
              name={field.id}
              label={field.label}
              value={value}
              onChange={(val) => handleFieldChange(field.id, val)}
              options={storeOptions[field.id]}
              helpText={field.helpText}
              error={fieldError}
              required={field.required}
              disabled={field.disabled || field.readOnly}
              allowCustom={field.allowAutocomplete}
              placeholder={`Select ${field.label.toLowerCase()}...`}
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
    }
  }, [formData?.properties, form.validationErrors, handleFieldChange, repoOptions, storeOptions, form.touched]);

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
          checked={isEnabledProp !== undefined ? isEnabledProp : (formData?.enabled ?? true)}
          onChange={(checked) => {
            form.send({ type: 'UPDATE', name: 'enabled', value: checked });
            onEnabledChange?.(checked);
          }}
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
  // Note: noDirtyTracking={true} because XState form machine already handles
  // dirty state tracking via useUnsavedChangesWarning with its own form ID.
  // Using SettingsForm's tracking would add a second random ID to window.dirty
  // that never gets cleared when the XState machine saves.
  return (
    <Box className="dynamic-capability-form">
      <SettingsForm
        testId="capability-form"
        onSubmit={canEdit ? handleSubmit : undefined}
        onCancel={onCancel}
        loading={form.isSaving || loading}
        pristine={isCreate ? false : form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create' : 'Save'}
        submitAnalyticsId={isCreate ? 'nxrm-capability-create' : 'nxrm-capability-save'}
        cancelLabel="Cancel"
        showActions={isCreate || !form.isPristine}
        noDirtyTracking={true}
        confirmDiscard={false}
        footerExtra={
          !isCreate && onDelete ? (
            <SettingsButton
              variant="danger"
              onClick={onDelete}
              disabled={form.isSaving || loading}
              icon={Trash2}
              data-analytics-id="nxrm-capability-delete"
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
