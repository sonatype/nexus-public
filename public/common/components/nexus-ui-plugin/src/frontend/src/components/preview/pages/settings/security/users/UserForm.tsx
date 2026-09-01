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
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2, Key, Info } from 'lucide-react';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsButton,
  SettingsTransferList,
} from '../../../../shared/form';
import { RoleExplorerTree } from '../roles/RoleExplorerTree';
import type { UseUsersFormResult } from './useUsersForm';
import { useUserTreePreview } from './useUserTreePreview';
import { getSourceLabel } from './types';

import './UserForm.scss';

export interface UserFormProps {
  vm: UseUsersFormResult;
}

/**
 * Body of the Edit/Create User form. Renders Details, Roles, and the live
 * Privileges/Security Tree preview. Meant to be embedded inside a parent
 * SettingsForm; the parent owns title, actions, and the sticky action bar.
 */
export function UserForm({ vm }: UserFormProps) {
  const {
    form,
    formData,
    isCreate,
    isExternal,
    isLoading,
    showPasswordChange,
    currentUser,
    externalRoles,
    allRoles,
    rolesDirty,
    setRoles,
    showPasswordChangeSection,
    resetPasswordFields,
  } = vm;

  const {
    tree: previewTree,
    loading: previewLoading,
    toggleExpand: previewToggleExpand,
    expandAll: previewExpandAll,
    collapseAll: previewCollapseAll,
    setSearchTerm: setPreviewSearchTerm,
  } = useUserTreePreview(formData.roles ?? [], currentUser);

  const availableRoles = useMemo(
    () => allRoles.map((r) => ({ id: r.id, name: r.name })),
    [allRoles],
  );

  const selectedRoles = useMemo(
    () =>
      allRoles
        .filter((r) => (formData.roles ?? []).includes(r.id))
        .map((r) => ({ id: r.id, name: r.name })),
    [allRoles, formData.roles],
  );

  const handleRolesChange = useCallback(
    (next: Array<{ id: string; name: string }>) => {
      setRoles(next.map((r) => r.id));
    },
    [setRoles],
  );

  if (isLoading) {
    return (
      <Flex
        align="center"
        justify="center"
        className="user-form__loading"
        role="status"
        aria-live="polite"
        aria-busy="true"
      >
        <Loader2 size={24} className="user-form__spinner" aria-hidden="true" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  const externalLabel = currentUser
    ? getSourceLabel(currentUser.source)
    : undefined;

  return (
    <Box className="user-form">
      {isExternal && (
        <Box
          className="user-form__external-indicator"
          role="note"
          data-testid="user-form-external-indicator"
        >
          <Info size={16} aria-hidden="true" />
          <Text size="2">
            {externalLabel
              ? `This user is externally managed via ${externalLabel}. Profile fields are read-only.`
              : 'This user is externally managed. Profile fields are read-only.'}
          </Text>
        </Box>
      )}

      <SettingsFormSection title="Details" defaultOpen>
        <SettingsTextInput
          {...form.field('userId')}
          label="ID"
          placeholder="jsmith"
          helpText="This will be used as the username"
          required
          disabled={!isCreate || isExternal}
        />

        <SettingsTextInput
          {...form.field('firstName')}
          label="First Name"
          placeholder="John"
          helpText="User's first name for display purposes"
          required={!isExternal}
          disabled={isExternal}
        />

        <SettingsTextInput
          {...form.field('lastName')}
          label="Last Name"
          placeholder="Smith"
          helpText="User's last name for display purposes"
          required={!isExternal}
          disabled={isExternal}
        />

        <SettingsTextInput
          {...form.field('emailAddress')}
          label="Email"
          placeholder="jsmith@example.com"
          type="email"
          helpText="Used for notifications"
          required={!isExternal}
          disabled={isExternal}
        />

        {isCreate && !isExternal && (
          <>
            <SettingsPasswordInput
              {...form.field('password')}
              label="Password"
              helpText="Set the initial password. User can change it later."
              required
              autoComplete="new-password"
            />
            <SettingsPasswordInput
              {...form.field('passwordConfirm')}
              label="Confirm Password"
              helpText="Re-enter the password to confirm"
              required
              autoComplete="new-password"
            />
          </>
        )}

        {!isCreate && !isExternal && (
          <Box mt="4" mb="4">
            {!showPasswordChange ? (
              <SettingsButton
                variant="secondary"
                icon={Key}
                onClick={showPasswordChangeSection}
                testId="change-password-btn"
                data-analytics-id="nxrm-user-change-password"
              >
                Change Password
              </SettingsButton>
            ) : (
              <Box className="user-form__password-change">
                <Flex align="center" justify="between" mb="2">
                  <Text size="2" weight="medium">
                    Change Password
                  </Text>
                  <SettingsButton
                    variant="ghost"
                    size="small"
                    onClick={resetPasswordFields}
                  >
                    Cancel
                  </SettingsButton>
                </Flex>
                <SettingsPasswordInput
                  {...form.field('password')}
                  label="New Password"
                  required
                  autoComplete="new-password"
                />
                <SettingsPasswordInput
                  {...form.field('passwordConfirm')}
                  label="Confirm New Password"
                  required
                  autoComplete="new-password"
                />
              </Box>
            )}
          </Box>
        )}
      </SettingsFormSection>

      <SettingsFormSection title="Roles" defaultOpen>
        <SettingsTransferList
          name="roles"
          testId="user-form-roles"
          availableItems={availableRoles}
          selectedItems={selectedRoles}
          onChange={handleRolesChange}
          availableLabel="Available"
          selectedLabel="Granted"
          getItemId={(item) => item.id}
          getItemLabel={(item) => item.name}
          helpText={
            (formData.roles ?? []).length > 0
              ? `${(formData.roles ?? []).length} role${(formData.roles ?? []).length === 1 ? '' : 's'} granted`
              : undefined
          }
        />
        {form.validationErrors?.roles && (
          <Text size="1" className="user-form__error">
            {form.validationErrors.roles}
          </Text>
        )}

        {isExternal && externalRoles.length > 0 && (
          <Box mt="4">
            <Text
              size="2"
              weight="medium"
              mb="2"
              style={{ display: 'block' }}
            >
              Externally Assigned Roles
            </Text>
            <Box className="user-form__external-roles">
              <Flex wrap="wrap" gap="1">
                {externalRoles.map((roleId) => {
                  const role = allRoles.find((r) => r.id === roleId);
                  const label = role?.name ?? roleId;
                  return (
                    <Box
                      key={roleId}
                      className="user-form__external-role-chip"
                      data-testid={`external-role-${roleId}`}
                    >
                      {label}
                    </Box>
                  );
                })}
              </Flex>
            </Box>
          </Box>
        )}
      </SettingsFormSection>

      <SettingsFormSection
        title="Privileges / Security Tree"
        defaultOpen
      >
        {rolesDirty && (
          <Box
            className="user-form__preview-hint"
            data-testid="tree-preview-unsaved"
          >
            <Text size="1" color="gray">
              Preview reflects unsaved role changes. Save to persist.
            </Text>
          </Box>
        )}
        {(formData.roles ?? []).length > 0 ? (
          <Box className="user-form__preview-pane">
            <RoleExplorerTree
              tree={previewTree}
              loading={previewLoading}
              onToggleExpand={previewToggleExpand}
              onExpandAll={previewExpandAll}
              onCollapseAll={previewCollapseAll}
              onSearchChange={setPreviewSearchTerm}
            />
          </Box>
        ) : (
          <Box className="user-form__preview-placeholder">
            <Text size="2" color="gray">
              Grant at least one role to preview effective permissions.
            </Text>
          </Box>
        )}
      </SettingsFormSection>
    </Box>
  );
}

export default UserForm;
