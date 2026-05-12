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

import React, { useMemo, useEffect, useState } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Trash2, Loader2, AlertCircle } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsCombobox,
  SettingsTransferList,
  SettingsButton,
  SettingsSelect,
} from '../../../shared/form';
import { useRolesApi } from './useRolesApi';
import { useRolesForm } from './useRolesForm';
import {
  Role,
  RoleReference,
  PrivilegeReference,
  RoleFormData,
  RoleFormProps,
  RoleSource,
  NEXUS_SOURCE,
  isReadOnlyRole,
} from './types';

import './RoleForm.scss';

/**
 * RoleForm - Form for creating and editing roles
 * Uses XState for state management via useRolesForm hook
 */
export function RoleForm({
  role,
  isCreate,
  onCancel,
  onDelete,
  onComplete,
  loading = false,
  error,
  hideActions = false,
  onSubmitRef,
  onValidationChange,
  wizardStep = 0,
}: RoleFormProps & {
  hideActions?: boolean;
  onSubmitRef?: React.MutableRefObject<(() => void) | null>;
  onValidationChange?: (isValid: boolean) => void;
  wizardStep?: number;
}) {
  const { createRole, updateRole } = useRolesApi();

  const { form, role: loadedRole } = useRolesForm({
    roleId: isCreate ? undefined : role?.id,
    role: role || undefined,
    onCancel,
    onComplete,
    createRole,
    updateRole,
  });

  // P0: Require at least one privilege or contained role on final step
  const formData = form.data as RoleFormData;
  const hasPrivilegeOrRole = (formData.privileges?.length ?? 0) > 0 || (formData.roles?.length ?? 0) > 0;

  // Notify parent of validation status (drives WizardForm canAdvance / submitDisabled)
  useEffect(() => {
    if (onValidationChange) {
      const isIdValid = !!form.data.id?.trim() && !form.validationErrors?.id;
      const isNameValid = !!form.data.name?.trim() && !form.validationErrors?.name;
      if (wizardStep === 0) {
        onValidationChange(isIdValid && isNameValid);
      } else if (wizardStep === 2) {
        // Final step: require at least one privilege or contained role
        onValidationChange(hasPrivilegeOrRole);
      } else {
        onValidationChange(true);
      }
    }
  }, [form.data.id, form.data.name, form.data.privileges, form.data.roles, form.validationErrors?.id, form.validationErrors?.name, onValidationChange, wizardStep, hasPrivilegeOrRole]);

  const submitDisabled = !hasPrivilegeOrRole;
  const [hasAttemptedAdvance, setHasAttemptedAdvance] = useState(false);

  // Expose submit function to parent via ref (for WizardForm integration)
  useEffect(() => {
    if (onSubmitRef) {
      onSubmitRef.current = () => {
        if (wizardStep === 2) setHasAttemptedAdvance(true);
        if (submitDisabled && wizardStep === 2) return;
        if (!form.isLoading && !form.isSaving) {
          form.submit();
        }
      };
      return () => { onSubmitRef.current = null; };
    }
  }, [onSubmitRef, form, wizardStep, submitDisabled]);

  // Reset attempt flag only when returning to Setup (step 0) - keeps warning visible if user goes Back from step 2 to step 1
  useEffect(() => {
    if (wizardStep === 0) setHasAttemptedAdvance(false);
  }, [wizardStep]);

  // Use the role from props if provided, otherwise from form state
  const currentRole = role || loadedRole;
  const isExternal = currentRole ? currentRole.source !== NEXUS_SOURCE && currentRole.source !== 'default' && currentRole.source !== '' : false;
  const isReadOnly = currentRole ? isReadOnlyRole(currentRole) : false;

  // Access extended context for reference data
  const context = form.state.context as any;
  const allPrivileges: PrivilegeReference[] = context.allPrivileges || [];
  const allRoles: RoleReference[] = context.allRoles || [];
  const allSources: RoleSource[] = context.allSources || [];

  const sourceOptions = useMemo(() => {
    const options = allSources.map(s => ({ value: s.id, label: s.name }));
    if (!options.some(o => o.value === NEXUS_SOURCE)) {
      options.unshift({ value: NEXUS_SOURCE, label: NEXUS_SOURCE });
    }
    return options;
  }, [allSources]);

  // Convert privileges to combobox option format
  const privilegeOptions = useMemo(() => {
    return allPrivileges.map((priv) => ({
      value: priv.id,
      label: priv.name,
      description: priv.description,
    }));
  }, [allPrivileges]);

  const PRIVILEGE_GROUP_LABELS: Record<string, string> = {
    'nx-repository-view': 'Repository View',
    'nx-repository-admin': 'Repository Admin',
    'nx-repository-content-selector': 'Content Selector',
    'nx-application': 'Application',
    'nx-script': 'Script',
    'nx-healthcheck': 'Health Check',
    'nx-blobstore': 'Blob Store',
  };

  const groupPrivilege = useMemo(() => {
    const prefixes = Object.keys(PRIVILEGE_GROUP_LABELS);
    return (option: { value: string; label: string }) => {
      for (const prefix of prefixes) {
        if (option.value.startsWith(prefix)) return PRIVILEGE_GROUP_LABELS[prefix];
      }
      return 'Other';
    };
  }, []);

  const showPrivilegeWarning = submitDisabled && hasAttemptedAdvance && wizardStep === 2;

  const handlePrivilegesChange = (values: string[]) => {
    form.send({ type: 'UPDATE', name: 'privileges', value: values });
  };

  // Convert roles to transfer list format
  const availableRoles = useMemo(() => {
    return allRoles
      .filter((r) => r.id !== role?.id && r.id !== formData.id)
      .map((r) => ({
        id: r.id,
        name: r.name,
      }));
  }, [allRoles, role?.id, formData.id]);

  const selectedRoles = useMemo(() => {
    return allRoles
      .filter((r) => formData.roles?.includes(r.id))
      .map((r) => ({
        id: r.id,
        name: r.name,
      }));
  }, [allRoles, formData.roles]);

  const handleRolesChange = (newSelected: Array<{ id: string; name: string }>) => {
    form.send({ type: 'UPDATE', name: 'roles', value: newSelected.map((r) => r.id) });
  };

  // Show loading state while data loads
  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" className="role-form__loading">
        <Loader2 size={24} className="role-form__spinner" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  const formContent = (
    <>
      {/* Step 1: Role Setup Section */}
      {wizardStep === 0 && (
        <SettingsFormSection title="Role Setup" defaultOpen>
          <SettingsTextInput
            {...form.field('id')}
            label="ID"
            placeholder="nx-custom-role"
            helpText="Unique identifier for this role"
            required
            disabled={!isCreate || isExternal}
          />

          <SettingsTextInput
            {...form.field('name')}
            label="Name"
            placeholder="My Custom Role"
            helpText="Human-readable name for this role"
            required
            disabled={isReadOnly}
          />

          <SettingsTextArea
            {...form.field('description')}
            label="Description"
            placeholder="Grants read access to all Maven repositories"
            helpText="Optional description of this role's purpose"
            rows={3}
            disabled={isReadOnly}
          />

          <SettingsSelect
            {...form.field('source')}
            label="Source"
            options={sourceOptions}
            helpText="The security source where this role is defined"
            disabled={!isCreate || isReadOnly}
          />
        </SettingsFormSection>
      )}

      {/* Step 2: Privileges Section */}
      {wizardStep === 1 && (
        <SettingsFormSection title="Privileges" defaultOpen>
          <Text size="2" className="role-form__section-help">
            Search and select privileges to grant. Results are grouped by type.
          </Text>
          <SettingsCombobox
            name="privileges"
            multiple
            selectedValues={formData.privileges || []}
            onMultiChange={handlePrivilegesChange}
            options={privilegeOptions}
            groupBy={groupPrivilege}
            placeholder="Search privileges by name..."
            helpText={`${formData.privileges?.length || 0} privilege${(formData.privileges?.length || 0) === 1 ? '' : 's'} granted`}
            disabled={isReadOnly}
          />
          {hasAttemptedAdvance && form.validationErrors?.privileges && (
            <Text size="1" className="role-form__error">{form.validationErrors.privileges}</Text>
          )}
        </SettingsFormSection>
      )}

      {/* Step 3: Contained Roles Section */}
      {wizardStep === 2 && (
        <>
          <SettingsFormSection title="Contained Roles" defaultOpen>
            <Text size="2" className="role-form__section-help">
              Select other roles to include. Their privileges will be inherited by this role.
            </Text>
            <SettingsTransferList
              name="roles"
              testId="role-form-roles"
              availableItems={availableRoles}
              selectedItems={selectedRoles}
              onChange={handleRolesChange}
              availableLabel="Available"
              selectedLabel="Contained"
              getItemId={(item) => item.id}
              getItemLabel={(item) => item.name}
              disabled={isReadOnly}
            />
          </SettingsFormSection>

          {/* Validation error: no privileges or roles selected */}
          {showPrivilegeWarning && (
            <Flex align="center" gap="2" className="role-form__privilege-warning" data-testid="privilege-warning" role="alert">
              <AlertCircle size={14} />
              <Text size="2" weight="medium">
                {form.validationErrors?.privileges ?? 'Select at least one privilege or contained role'}
              </Text>
            </Flex>
          )}
        </>
      )}
    </>
  );

  if (hideActions) {
    return <Box className="role-form">{formContent}</Box>;
  }

  return (
    <Box className="role-form">
      <SettingsForm
        testId="role-form"
        onSubmit={() => {
          if (wizardStep === 2) setHasAttemptedAdvance(true);
          if (submitDisabled && wizardStep === 2) return;
          form.submit();
        }}
        onCancel={onCancel}
        cancelLabel="Cancel"
        loading={form.isSaving || loading}
        pristine={form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create' : 'Save'}
        submitDisabled={submitDisabled}
        footerExtra={
          !isCreate && onDelete && !isReadOnly ? (
            <SettingsButton
              testId="form-delete"
              variant="danger"
              onClick={onDelete}
              disabled={form.isSaving || loading}
              icon={Trash2}
            >
              Delete Role
            </SettingsButton>
          ) : undefined
        }
      >
        {formContent}
      </SettingsForm>
    </Box>
  );
}


export default RoleForm;
