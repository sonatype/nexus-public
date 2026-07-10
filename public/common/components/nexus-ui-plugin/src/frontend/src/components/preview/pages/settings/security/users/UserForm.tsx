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

import React, { useMemo, useCallback, useEffect, useState } from 'react';
import { Box, Flex, Text, ScrollArea, Button as RadixButton } from '@radix-ui/themes';
import { Trash2, Loader2, Key, RefreshCw } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsTransferList,
  SettingsButton,
  ConfirmDialog,
} from '../../../../shared/form';
import { useToast } from '../../../../shared';
import { useUsersApi } from './useUsersApi';
import { useUsersForm } from './useUsersForm';
import {
  UserFormData,
  UserFormProps,
  DEFAULT_SOURCE,
  isExternalUser,
  getFullName,
} from './types';
import { useCombinedRoleTree } from '../roles/useRoleTree';
import { RoleExplorerTree } from '../roles/RoleExplorerTree';

import './UserForm.scss';

const UNIFIED_INSPECTOR_EMPTY =
  'Select roles from Available or Granted to see combined permissions.';

/**
 * UserForm - Form for creating and editing users
 * Uses XState form machine for state management
 */
export function UserForm({
  user,
  userId: userIdProp,
  userSource: userSourceProp,
  isCreate: propIsCreate,
  onSave,
  onCancel,
  onDelete,
  loading = false,
  error,
  wizardStep = 0,
  hideActions = false,
  onValidationChange,
  onDirtyChange,
  onSubmitRef,
}: UserFormProps) {
  const { createUser, updateUser, changePassword, resetUserToken } = useUsersApi();
  const [showPasswordChange, setShowChangePassword] = useState(false);
  const [resetTokenDialogOpen, setResetTokenDialogOpen] = useState(false);
  const [isResettingToken, setIsResettingToken] = useState(false);
  const [hasAttemptedRolesSubmit, setHasAttemptedRolesSubmit] = useState(false);

  const toast = useToast();
  const isPro = ExtJS.isProEdition();
  const state = ExtJS.state();
  const isAnonymous = user?.userId === state?.getValue?.('anonymousUsername');
  const isCurrentUser = user?.userId === state?.getUser?.()?.id;
  const isAdminUser = user?.userId === 'admin';
  const activeCapabilities = state?.getValue?.('capabilityActiveTypes') || [];
  const isUserTokenCapabilityActive = activeCapabilities.includes('usertoken');
  const canResetUserToken = ExtJS.checkPermission('nexus:usertoken-user:delete');

  // Use XState form hook - use route params when user not yet loaded so machine can fetch
  const { form, user: formUser, isCreate } = useUsersForm({
    userId: propIsCreate ? undefined : (user?.userId ?? userIdProp ?? undefined),
    userSource: user?.source ?? userSourceProp ?? DEFAULT_SOURCE,
    user: user || undefined,
    onSave: onSave ? async (data: UserFormData) => { await onSave(data); } : undefined,
    onCancel,
    createUser,
    updateUser,
    changePassword,
  });

  // Expose submit to parent via ref; track attempt on roles step for validation display
  if (onSubmitRef) {
    onSubmitRef.current = () => {
      if (wizardStep === 1) setHasAttemptedRolesSubmit(true);
      form.submit();
    };
  }

  const handleCancelPasswordChange = () => {
    form.send({ type: 'UPDATE', name: 'password', value: '' } as any);
    form.send({ type: 'UPDATE', name: 'passwordConfirm', value: '' } as any);
    setShowChangePassword(false);
  };

  // Reset attempt flag when leaving roles step
  useEffect(() => {
    if (wizardStep === 0) setHasAttemptedRolesSubmit(false);
  }, [wizardStep]);

  // Notify parent of dirty state changes
  useEffect(() => {
    if (onDirtyChange) {
      onDirtyChange(!form.isPristine);
    }
  }, [form.isPristine, onDirtyChange]);

  // Use the user from props if provided, otherwise from form state
  const currentUser = user || formUser;

  // Access form state and reference data from machine context
  const isLoading = form.isLoading;
  const isSaving = form.isSaving;
  const context = form.state.context as any;
  const formData = form.data as UserFormData;
  const allRoles = context.allRoles || [];
  const isExternal = currentUser ? isExternalUser(currentUser.source) : isExternalUser(formData.source);

  // Notify parent of validation status
  useEffect(() => {
    if (onValidationChange) {
      if (wizardStep === 0) {
        // Step 1: Setup validation
        const hasBasicInfo = !!formData.userId?.trim() && 
                            (isExternal || (!!formData.firstName?.trim() && !!formData.lastName?.trim() && !!formData.emailAddress?.trim()));
        
        const isEmailValid = isExternal || !formData.emailAddress || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.emailAddress);
        
        const isPasswordValid = isExternal || !isCreate ||
                                (!!formData.password && formData.password.length >= 8 &&
                                 !!formData.passwordConfirm && formData.password === formData.passwordConfirm);

        // If editing and change password is open, validate it
        const isEditPasswordValid = !isCreate && (!showPasswordChange ||
                                    (!!formData.password && formData.password.length >= 8 &&
                                     !!formData.passwordConfirm && formData.password === formData.passwordConfirm));

        const isValid = hasBasicInfo && isEmailValid && (isCreate ? isPasswordValid : isEditPasswordValid);
        onValidationChange(isValid);
      } else {
        // Step 2: Roles validation (at least one role required)
        onValidationChange(formData.roles.length > 0);
      }
    }
  }, [formData, isExternal, isCreate, onValidationChange, wizardStep, showPasswordChange]);

  // Convert roles to transfer list format
  const availableRoles = useMemo(() => {
    return allRoles.map((role: { id: string; name: string }) => ({
      id: role.id,
      name: role.name,
    }));
  }, [allRoles]);

  const selectedRoles = useMemo(() => {
    return allRoles
      .filter((role: { id: string; name: string }) => formData.roles.includes(role.id))
      .map((role: { id: string; name: string }) => ({
        id: role.id,
        name: role.name,
      }));
  }, [allRoles, formData.roles]);

  const handleRolesChange = useCallback((newSelectedRoles: Array<{ id: string; name: string }>) => {
    form.send({ type: 'UPDATE', name: 'roles', value: newSelectedRoles.map((r) => r.id) } as any);
  }, [form]);

  // Role Inspector: show all granted roles (or single preview from Available when none granted)
  const [inspectedRoleId, setInspectedRoleId] = useState<string | null>(null);
  const [inspectorSearchTerm, setInspectorSearchTerm] = useState('');
  const handleRoleSelect = useCallback((item: { id: string; name: string }, _isSelected: boolean) => {
    setInspectedRoleId(item.id);
  }, []);

  // Roles to display in inspector: all granted roles, or single preview (from Available) when no roles granted
  const rolesToInspect = useMemo(() => {
    if (selectedRoles.length > 0) {
      return selectedRoles.map((r) => ({ id: r.id, name: r.name }));
    }
    if (inspectedRoleId) {
      const role = allRoles.find((r: { id: string; name: string }) => r.id === inspectedRoleId);
      return role ? [{ id: role.id, name: role.name }] : [];
    }
    return [];
  }, [selectedRoles, inspectedRoleId, allRoles]);

  const roleIdsToInspect = useMemo(() => rolesToInspect.map((r) => r.id), [rolesToInspect]);

  const {
    tree: combinedTree,
    loading: inspectorLoading,
    toggleExpand: inspectorToggleExpand,
    expandAll: inspectorExpandAll,
    collapseAll: inspectorCollapseAll,
  } = useCombinedRoleTree(roleIdsToInspect, { searchTerm: inspectorSearchTerm });

  // Show loading state while data loads
  if (isLoading) {
    return (
      <Flex align="center" justify="center" className="user-form__loading">
        <Loader2 size={24} className="user-form__spinner" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  const handleResetTokenConfirm = async () => {
    if (!currentUser) return;
    setResetTokenDialogOpen(false);
    setIsResettingToken(true);
    try {
      await resetUserToken(currentUser.userId, currentUser.realm || currentUser.source);
      toast.success(`User token has been reset for ${getFullName(currentUser)}`);
    } catch (err) {
      // Error is set by the API hook
    } finally {
      setIsResettingToken(false);
    }
  };

  const setupContent = (
    <SettingsFormSection title="User Setup" defaultOpen>
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
              onClick={() => setShowChangePassword(true)}
              testId="change-password-btn"
              data-analytics-id="nxrm-user-change-password"
            >
              Change Password
            </SettingsButton>
          ) : (
            <Box className="user-form__password-change">
              <Flex align="center" justify="between" mb="2">
                <Text size="2" weight="medium">Change Password</Text>
                <RadixButton variant="ghost" size="1" onClick={handleCancelPasswordChange}>
                  Cancel
                </RadixButton>
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

      {!isCreate && !isExternal && isPro && isUserTokenCapabilityActive && canResetUserToken && (
        <Box mt="4" mb="4">
          <Flex align="center" justify="between">
            <Box>
              <Text size="2" weight="medium">User Token</Text>
              <Text as="p" size="1" color="gray">Invalidate current token and force regeneration</Text>
            </Box>
            <SettingsButton
              variant="secondary"
              icon={RefreshCw}
              onClick={() => setResetTokenDialogOpen(true)}
              loading={isResettingToken}
              testId="reset-token-btn"
            >
              Reset Token
            </SettingsButton>
          </Flex>
        </Box>
      )}

      <SettingsCheckbox
        {...form.checkbox('status')}
        label="Active"
        description="User account is enabled"
        disabled={isExternal}
        data-analytics-id="nxrm-user-toggle-status"
      />
    </SettingsFormSection>
  );

  const showRoleInspector = wizardStep === 1;

  const rolesContent = (
    <SettingsFormSection title="Roles" defaultOpen>
      <Flex
        className="user-form__roles-step"
        gap="4"
        direction={showRoleInspector ? 'row' : 'column'}
        style={showRoleInspector ? { minHeight: 320 } : undefined}
      >
        {/* 45% (or full width): Transfer List */}
        <Box
          className="user-form__roles-transfer"
          style={showRoleInspector ? { flex: '0 0 45%' } : undefined}
        >
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
            helpText={formData.roles.length > 0 ? `${formData.roles.length} role${formData.roles.length === 1 ? '' : 's'} granted` : undefined}
            onItemSelect={showRoleInspector ? handleRoleSelect : undefined}
          />
          {hasAttemptedRolesSubmit && form.validationErrors?.roles && (
            <Text size="1" className="user-form__error">{form.validationErrors.roles}</Text>
          )}

          {/* External Roles (read-only, clickable for Inspector) */}
          {isExternal && currentUser?.externalRoles && currentUser.externalRoles.length > 0 && (
            <Box mt="4">
              <Text size="2" weight="medium" mb="2" style={{ display: 'block' }}>External Roles</Text>
              <Box className="user-form__external-roles">
                <Flex wrap="wrap" gap="1">
                  {currentUser.externalRoles.map((roleId) => {
                    const role = allRoles.find((r: { id: string }) => r.id === roleId);
                    const label = role?.name ?? roleId;
                    return (
                      <Box
                        key={roleId}
                        as="button"
                        type="button"
                        className="user-form__external-role-chip"
                        onClick={() => showRoleInspector && setInspectedRoleId(roleId)}
                        data-testid={`external-role-${roleId}`}
                        title="Click to view role contents in Inspector"
                      >
                        {label}
                      </Box>
                    );
                  })}
                </Flex>
              </Box>
            </Box>
          )}
        </Box>

        {/* 55%: Unified Role Inspector (wizard step 2 only) - formatted like Available/Granted panels */}
        {showRoleInspector && (
          <Box className="user-form__role-inspector" style={{ flex: '1 1 55%', minWidth: 320 }}>
            <Flex className="user-form__role-inspector-header">
              <Text size="2" weight="medium">Role Inspector</Text>
              {rolesToInspect.length > 0 && (
                <Text size="1" color="gray">
                  {rolesToInspect.length} role{rolesToInspect.length !== 1 ? 's' : ''} granted
                </Text>
              )}
            </Flex>
            {rolesToInspect.length > 0 ? (
              <Box className="user-form__role-inspector-pane">
                <RoleExplorerTree
                  tree={combinedTree}
                  loading={inspectorLoading}
                  onToggleExpand={inspectorToggleExpand}
                  onExpandAll={inspectorExpandAll}
                  onCollapseAll={inspectorCollapseAll}
                  onSearchChange={setInspectorSearchTerm}
                />
              </Box>
            ) : (
              <Box className="user-form__role-inspector-placeholder">
                <Text size="2" color="gray">
                  {UNIFIED_INSPECTOR_EMPTY}
                </Text>
              </Box>
            )}
          </Box>
        )}
      </Flex>
    </SettingsFormSection>
  );

  if (hideActions) {
    return (
      <Box className="user-form">
        {wizardStep === 0 && setupContent}
        {wizardStep === 1 && rolesContent}

        {/* Reset Token Confirmation Dialog */}
        <ConfirmDialog
          open={resetTokenDialogOpen}
          testId="reset-user-token-dialog"
          onOpenChange={setResetTokenDialogOpen}
          title="Reset User Token"
          message={`Are you sure you want to reset the token for "${currentUser ? getFullName(currentUser) : ''}"? This will invalidate their current token.`}
          confirmLabel="Reset"
          cancelLabel="Cancel"
          variant="danger"
          onConfirm={handleResetTokenConfirm}
        />
      </Box>
    );
  }

  return (
    <Box className="user-form">
      <SettingsForm
        testId="user-form"
        onSubmit={() => form.submit()}
        onCancel={onCancel}
        loading={isSaving || loading}
        pristine={form.isPristine}
        noDirtyTracking={hideActions}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create' : 'Save'}
        footerExtra={
          !isCreate && onDelete && !isAdminUser ? (
            <SettingsButton
              testId="form-delete"
              variant="danger"
              onClick={onDelete}
              disabled={isSaving || loading}
              icon={Trash2}
            >
              Delete User
            </SettingsButton>
          ) : undefined
        }
      >
        {setupContent}
        {rolesContent}

        {/* Reset Token Confirmation Dialog */}
        <ConfirmDialog
          open={resetTokenDialogOpen}
          testId="reset-user-token-dialog"
          onOpenChange={setResetTokenDialogOpen}
          title="Reset User Token"
          message={`Are you sure you want to reset the token for "${currentUser ? getFullName(currentUser) : ''}"? This will invalidate their current token.`}
          confirmLabel="Reset"
          cancelLabel="Cancel"
          variant="danger"
          onConfirm={handleResetTokenConfirm}
        />
      </SettingsForm>
    </Box>
  );
}

export default UserForm;
