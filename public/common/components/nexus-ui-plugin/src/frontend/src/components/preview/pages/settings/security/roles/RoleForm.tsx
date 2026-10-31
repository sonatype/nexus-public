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

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Button, Flex, Text } from '@radix-ui/themes';
import { Trash2, Loader2, AlertCircle, Pencil } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsCombobox,
  SettingsButton,
  SettingsSelect,
  SettingsSelectionModal,
  useSettingsSelectionModal,
} from '../../../../shared/form';
import type { SettingsSelectionItem } from '../../../../shared/form/settingsSelectionModalMachine';
import { useRolesApi } from './useRolesApi';
import { useRolesForm } from './useRolesForm';
import {
  RoleFormData,
  RoleFormProps,
  RolesFormContext,
  LDAP_EMPTY_SEARCH_MESSAGE,
  isExternalRole,
} from './types';
import { ROLE_FORM } from './RoleFormStrings';

import './RoleForm.scss';

// Single flat progressive-disclosure form for Create and Edit. Type selector at the
// top drives visibility of external-source fields; all other sections render inline.
// Privileges and Contained Roles use the shared RolePicker (search + pagination +
// sortable columns) instead of the old unpaginated combobox / transfer list.
export function RoleForm({
  role,
  isCreate,
  onCancel,
  onDelete,
  onComplete,
  loading = false,
  error,
}: RoleFormProps) {
  const { createRole, updateRole, searchRoles } = useRolesApi();
  const [ldapRoleOptions, setLdapRoleOptions] = useState<Array<{ value: string; label: string }>>([]);
  const [ldapSearchLoading, setLdapSearchLoading] = useState(false);
  const [ldapEmptyMessage, setLdapEmptyMessage] = useState<string | null>(null);
  const ldapSearchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const { form, role: loadedRole } = useRolesForm({
    roleId: isCreate ? undefined : role?.id,
    role: role || undefined,
    onCancel,
    onComplete,
    createRole,
    updateRole,
  });

  const formData = form.data as RoleFormData;

  // Cleanup LDAP search timer on unmount
  useEffect(() => {
    return () => {
      if (ldapSearchTimerRef.current) {
        clearTimeout(ldapSearchTimerRef.current);
      }
    };
  }, []);

  const currentRole = role || loadedRole;
  const isExternal = currentRole ? isExternalRole(currentRole.source) : false;
  // Defense in depth: even though callers gate on `canEdit`, the form disables inputs when the
  // backend flags the role read-only. Source (LDAP/SAML/Crowd/OAuth2) is not part of this check.
  const isReadOnly = currentRole?.readOnly === true;

  // Access extended context for reference data
  const context = form.state.context as RolesFormContext;
  const allPrivileges = context.allPrivileges || [];
  const allRoles = context.allRoles || [];

  // Adapt reference data to the SettingsSelectionModal's item shape.
  const privilegeItems: SettingsSelectionItem[] = useMemo(
    () =>
      allPrivileges.map((priv) => ({
        id: priv.id,
        name: priv.name,
        description: priv.description,
      })),
    [allPrivileges],
  );

  const roleItems: SettingsSelectionItem[] = useMemo(
    () =>
      allRoles
        .filter((r) => r.id !== role?.id && r.id !== formData.id)
        .map((r) => ({ id: r.id, name: r.name })),
    [allRoles, role?.id, formData.id],
  );

  const handleApplyPrivileges = useCallback(
    (ids: string[]) => form.send({ type: 'UPDATE', name: 'privileges', value: ids }),
    [form],
  );
  const handleApplyRoles = useCallback(
    (ids: string[]) => form.send({ type: 'UPDATE', name: 'roles', value: ids }),
    [form],
  );

  const privilegesPicker = useSettingsSelectionModal({
    availableItems: privilegeItems,
    onConfirm: handleApplyPrivileges,
  });
  const containedRolesPicker = useSettingsSelectionModal({
    availableItems: roleItems,
    onConfirm: handleApplyRoles,
  });

  const openPrivilegesPicker = useCallback(
    () => privilegesPicker.open(formData.privileges ?? []),
    [privilegesPicker, formData.privileges],
  );
  const openContainedRolesPicker = useCallback(
    () => containedRolesPicker.open(formData.roles ?? []),
    [containedRolesPicker, formData.roles],
  );

  const hasPrivilegeOrRole =
    (formData.privileges?.length ?? 0) > 0 || (formData.roles?.length ?? 0) > 0;
  const submitDisabled = !hasPrivilegeOrRole;

  // Handle LDAP role search with debounce
  const handleLdapSearch = useCallback(
    async (query: string) => {
      if (formData.externalSource !== 'LDAP') {
        setLdapRoleOptions([]);
        setLdapEmptyMessage(null);
        return;
      }

      if (query.length < 3) {
        setLdapRoleOptions([]);
        setLdapEmptyMessage(ROLE_FORM.SECTION_EXTERNAL.LDAP_MIN_CHARS);
        return;
      }

      if (ldapSearchTimerRef.current) {
        clearTimeout(ldapSearchTimerRef.current);
      }

      ldapSearchTimerRef.current = setTimeout(async () => {
        setLdapSearchLoading(true);
        setLdapEmptyMessage(null);

        try {
          const results = await searchRoles(formData.externalSource!, query);
          const options = results.map((r) => ({ value: r.id, label: r.name }));
          setLdapRoleOptions(options);
          if (options.length === 0) {
            setLdapEmptyMessage(LDAP_EMPTY_SEARCH_MESSAGE);
          }
        } catch (err) {
          console.error('Failed to search LDAP roles:', err);
          setLdapRoleOptions([]);
          setLdapEmptyMessage(ROLE_FORM.SECTION_EXTERNAL.LDAP_SEARCH_FAILED);
        } finally {
          setLdapSearchLoading(false);
        }
      }, 300);
    },
    [formData.externalSource, searchRoles],
  );

  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" className="role-form__loading">
        <Loader2 size={24} className="role-form__spinner" />
        <Text size="2">{ROLE_FORM.LOADING}</Text>
      </Flex>
    );
  }

  const roleTypeOptions = [
    {
      value: 'nexus',
      label: ROLE_FORM.SECTION_TYPE.OPTION_NEXUS_LABEL,
      description: ROLE_FORM.SECTION_TYPE.OPTION_NEXUS_DESCRIPTION,
    },
    {
      value: 'external',
      label: ROLE_FORM.SECTION_TYPE.OPTION_EXTERNAL_LABEL,
      description: ROLE_FORM.SECTION_TYPE.OPTION_EXTERNAL_DESCRIPTION,
    },
  ];

  const selectedRoleType = formData.roleType ?? 'nexus';
  const isExternalType = selectedRoleType === 'external';

  return (
    <Box className="role-form">
      <SettingsForm
        testId="role-form"
        onSubmit={() => {
          if (submitDisabled) return;
          form.submit();
        }}
        onCancel={onCancel}
        onDiscardConfirm={() => {
          // useForm already registers this form's machine.id in window.dirty.
          // SettingsForm's default onDiscardConfirm only clears its own
          // duplicate registration; without clearing ours the router prompts
          // again after Leave. Clear ours synchronously before navigating.
          form.clearDirty();
          onCancel();
        }}
        externalDirtyTracking
        cancelLabel={ROLE_FORM.BUTTON_CANCEL}
        loading={form.isSaving || loading}
        pristine={form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? ROLE_FORM.BUTTON_CREATE : ROLE_FORM.BUTTON_SAVE}
        submitDisabled={submitDisabled}
        footerExtra={
          !isCreate && onDelete && !isReadOnly ? (
            <Button
              type="button"
              variant="soft"
              color="red"
              onClick={onDelete}
              disabled={form.isSaving || loading}
              data-testid="form-delete"
              data-analytics-id="nxrm-role-delete"
            >
              <Trash2 size={16} />
              {ROLE_FORM.BUTTON_DELETE}
            </Button>
          ) : undefined
        }
      >
        {/* Role Type - Create only. Edit mode never shows this because role type
            is immutable once persisted; the external source shows read-only under Setup. */}
        {isCreate && (
          <SettingsFormSection title={ROLE_FORM.SECTION_TYPE.TITLE} defaultOpen>
            <Text size="2" className="role-form__section-help">
              {ROLE_FORM.SECTION_TYPE.HELP}
            </Text>
            <SettingsSelect
              name="roleType"
              onChange={(value) => form.send({ type: 'UPDATE', name: 'roleType', value })}
              label={ROLE_FORM.SECTION_TYPE.FIELD_LABEL}
              options={roleTypeOptions}
              helpText={ROLE_FORM.SECTION_TYPE.FIELD_HELP}
              value={selectedRoleType}
            />
          </SettingsFormSection>
        )}

        {/* External Source + Mapped Role - shown only when Type=External (Create) or role is external (Edit) */}
        {(isExternalType || (!isCreate && isExternal)) && (
          <SettingsFormSection title={ROLE_FORM.SECTION_EXTERNAL.TITLE} defaultOpen>
            <SettingsSelect
              {...form.field('externalSource')}
              label={ROLE_FORM.SECTION_EXTERNAL.FIELD_LABEL}
              options={[
                { value: 'LDAP', label: 'LDAP' },
                { value: 'SAML', label: 'SAML' },
                { value: 'Crowd', label: 'Crowd' },
              ]}
              helpText={ROLE_FORM.SECTION_EXTERNAL.FIELD_HELP}
              disabled={!isCreate || isReadOnly}
              required
            />

            {formData.externalSource && (
              <SettingsCombobox
                name="mappedRole"
                value={formData.mappedRole || ''}
                onChange={(value) => {
                  form.send({ type: 'UPDATE', name: 'mappedRole', value });
                }}
                onInputChange={handleLdapSearch}
                options={ldapRoleOptions}
                placeholder={
                  formData.externalSource === 'LDAP'
                    ? ROLE_FORM.SECTION_EXTERNAL.LDAP_PLACEHOLDER
                    : ROLE_FORM.SECTION_EXTERNAL.OTHER_PLACEHOLDER(formData.externalSource)
                }
                label={ROLE_FORM.SECTION_EXTERNAL.MAPPED_ROLE_LABEL}
                helpText={
                  formData.externalSource === 'LDAP'
                    ? ROLE_FORM.SECTION_EXTERNAL.LDAP_HELP
                    : ROLE_FORM.SECTION_EXTERNAL.OTHER_HELP(formData.externalSource)
                }
                allowCustomValue
                loading={ldapSearchLoading}
                disabled={isReadOnly}
                hideEmptyMessage={formData.externalSource !== 'LDAP'}
                emptyMessage={ldapEmptyMessage}
              />
            )}
          </SettingsFormSection>
        )}

        {/* Setup - always visible */}
        <SettingsFormSection title={ROLE_FORM.SECTION_SETUP.TITLE} defaultOpen>
          <SettingsTextInput
            {...form.field('id')}
            label={ROLE_FORM.SECTION_SETUP.ID_LABEL}
            placeholder={ROLE_FORM.SECTION_SETUP.ID_PLACEHOLDER}
            helpText={ROLE_FORM.SECTION_SETUP.ID_HELP}
            required
            disabled={!isCreate || isExternal}
          />

          <SettingsTextInput
            {...form.field('name')}
            label={ROLE_FORM.SECTION_SETUP.NAME_LABEL}
            placeholder={ROLE_FORM.SECTION_SETUP.NAME_PLACEHOLDER}
            helpText={ROLE_FORM.SECTION_SETUP.NAME_HELP}
            required
            disabled={isReadOnly}
          />

          <SettingsTextArea
            {...form.field('description')}
            label={ROLE_FORM.SECTION_SETUP.DESCRIPTION_LABEL}
            placeholder={ROLE_FORM.SECTION_SETUP.DESCRIPTION_PLACEHOLDER}
            helpText={ROLE_FORM.SECTION_SETUP.DESCRIPTION_HELP}
            rows={3}
            disabled={isReadOnly}
          />
        </SettingsFormSection>

        {/* Privileges - RolePicker trigger + summary */}
        <SettingsFormSection title={ROLE_FORM.SECTION_PRIVILEGES.TITLE} defaultOpen>
          <Text size="2" className="role-form__section-help">
            {ROLE_FORM.SECTION_PRIVILEGES.HELP}
          </Text>
          <Flex align="center" gap="3" className="role-form__picker-row">
            <Text size="2" data-testid="role-form-privileges-count">
              {ROLE_FORM.SECTION_PRIVILEGES.COUNT(formData.privileges?.length ?? 0)}
            </Text>
            <SettingsButton
              variant="secondary"
              onClick={openPrivilegesPicker}
              disabled={isReadOnly}
              testId="role-form-open-privileges"
              icon={Pencil}
            >
              {ROLE_FORM.BUTTON_EDIT_PRIVILEGES}
            </SettingsButton>
          </Flex>
        </SettingsFormSection>

        {/* Contained Roles - RolePicker trigger + summary */}
        <SettingsFormSection title={ROLE_FORM.SECTION_CONTAINED_ROLES.TITLE} defaultOpen>
          <Text size="2" className="role-form__section-help">
            {ROLE_FORM.SECTION_CONTAINED_ROLES.HELP}
          </Text>
          <Flex align="center" gap="3" className="role-form__picker-row">
            <Text size="2" data-testid="role-form-roles-count">
              {ROLE_FORM.SECTION_CONTAINED_ROLES.COUNT(formData.roles?.length ?? 0)}
            </Text>
            <SettingsButton
              variant="secondary"
              onClick={openContainedRolesPicker}
              disabled={isReadOnly}
              testId="role-form-open-contained-roles"
              icon={Pencil}
            >
              {ROLE_FORM.BUTTON_EDIT_CONTAINED_ROLES}
            </SettingsButton>
          </Flex>

          {submitDisabled && (
            <Flex
              align="center"
              gap="2"
              className="role-form__privilege-warning"
              data-testid="privilege-warning"
              role="alert"
            >
              <AlertCircle size={14} />
              <Text size="2" weight="medium">
                {ROLE_FORM.PRIVILEGE_WARNING}
              </Text>
            </Flex>
          )}
        </SettingsFormSection>
      </SettingsForm>

      <SettingsSelectionModal
        picker={privilegesPicker}
        title={ROLE_FORM.PICKERS.PRIVILEGES_TITLE}
        emptyMessage={ROLE_FORM.PICKERS.PRIVILEGES_EMPTY}
        testId="privileges-picker"
      />
      <SettingsSelectionModal
        picker={containedRolesPicker}
        title={ROLE_FORM.PICKERS.CONTAINED_ROLES_TITLE}
        showDescriptionColumn={false}
        emptyMessage={ROLE_FORM.PICKERS.CONTAINED_ROLES_EMPTY}
        testId="contained-roles-picker"
      />
    </Box>
  );
}

export default RoleForm;
