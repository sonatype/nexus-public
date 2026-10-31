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

import React, { useCallback } from 'react';
import { Box, Button, Flex, Switch, Text } from '@radix-ui/themes';
import { RefreshCw, Trash2 } from 'lucide-react';

import { PageHeader } from '../../../../shared';
import {
  ConfirmDialog,
  SettingsButton,
  SettingsForm,
} from '../../../../shared/form';
import { UserForm } from './UserForm';
import { useUsersForm } from './useUsersForm';
import { User, DEFAULT_SOURCE, getFullName } from './types';

import './EditUserView.scss';

export interface EditUserViewProps {
  /** True for Create route, false for Edit route. */
  isCreate: boolean;
  /** User ID from the route (Edit route only). */
  userId?: string | null;
  /** User source/realm from the route (Edit route only). */
  userSource?: string | null;
  /** Preloaded user (may arrive after the machine's own fetch). */
  user?: User | null;
  /** True when the current admin has nexus:users:delete. */
  canDelete: boolean;
  /** Human-readable reason the current user cannot be deleted, or null. */
  protectionReason: string | null;
  /** Parent handler that opens the delete confirmation modal. */
  onDeleteRequest: () => void;
  /** Parent handler for post-save side effects (list refresh). */
  onSuccess: () => void;
  /** Parent handler for navigation back to the list. */
  onCancel: () => void;
  /** True while a delete request is in flight (disables the toolbar Delete). */
  isDeleting?: boolean;
}

const navigateToSettings = () => {
  window.location.hash = '#preview/admin/settings';
};

export function EditUserView({
  isCreate,
  userId,
  userSource,
  user,
  canDelete,
  protectionReason,
  onDeleteRequest,
  onSuccess,
  onCancel,
  isDeleting = false,
}: EditUserViewProps) {
  const vm = useUsersForm({
    userId: isCreate ? undefined : userId ?? undefined,
    userSource: userSource ?? DEFAULT_SOURCE,
    user: user ?? undefined,
    onSave: async () => {
      onSuccess();
    },
    onCancel,
  });

  const { formData, currentUser, isExternal, showsUserTokenReset, form } = vm;
  const statusToggleDisabled =
    !currentUser || isExternal || vm.isSaving || vm.isTogglingStatus;

  const title = isCreate
    ? 'Create User'
    : currentUser
      ? `Edit ${getFullName(currentUser) || currentUser.userId}`
      : 'Edit User';

  const description = isCreate
    ? 'Add a new local user account.'
    : 'Update the user profile, roles, and effective permissions.';

  const statusLabel = formData.status ? 'Active' : 'Inactive';
  const isProtectedForDelete = protectionReason !== null;

  const headerActions = !isCreate ? (
    <Flex align="center" gap="3" data-testid="edit-user-toolbar">
      <Flex align="center" gap="2">
        <Switch
          checked={!!formData.status}
          onCheckedChange={vm.toggleStatus}
          disabled={statusToggleDisabled}
          aria-label={`Toggle user status (currently ${statusLabel})`}
          data-testid="user-status-toggle"
          data-analytics-id="nxrm-user-toggle-status"
        />
        <Text size="2" data-testid="user-status-label">
          {statusLabel}
        </Text>
      </Flex>

      {showsUserTokenReset && (
        <SettingsButton
          variant="secondary"
          icon={RefreshCw}
          onClick={vm.openResetTokenDialog}
          loading={vm.isResettingToken}
          testId="reset-user-token-button"
          data-analytics-id="nxrm-user-reset-token"
        >
          Reset User Token
        </SettingsButton>
      )}
    </Flex>
  ) : undefined;

  const deleteFooter =
    !isCreate && canDelete && currentUser ? (
      <Button
        type="button"
        data-testid="page-delete-user"
        variant="soft"
        color="red"
        onClick={onDeleteRequest}
        disabled={isProtectedForDelete || isDeleting}
        title={protectionReason ?? undefined}
        aria-label={
          isProtectedForDelete
            ? `Delete User (${protectionReason})`
            : 'Delete User'
        }
        data-analytics-id="nxrm-user-delete"
      >
        <Trash2 size={16} />
        Delete User
      </Button>
    ) : undefined;

  const handleDiscardConfirm = useCallback(() => {
    form.reset();
    onCancel();
  }, [form, onCancel]);

  return (
    <Box className="edit-user-view">
      <PageHeader
        breadcrumbs={[
          { label: 'Settings', onClick: navigateToSettings },
          { label: 'Users', onClick: onCancel },
          {
            label: isCreate
              ? 'Create'
              : currentUser?.userId || userId || 'Loading...',
          },
        ]}
      />

      <SettingsForm
        testId="user-form"
        title={title}
        description={description}
        onSubmit={vm.submit}
        onCancel={vm.cancel}
        pristine={form.isPristine}
        loading={vm.isSaving}
        error={vm.saveError || undefined}
        submitLabel={isCreate ? 'Create' : 'Save'}
        externalDirtyTracking={true}
        onDiscardConfirm={handleDiscardConfirm}
        headerActions={headerActions}
        footerExtra={deleteFooter}
      >
        <UserForm vm={vm} />
      </SettingsForm>

      <ConfirmDialog
        open={vm.resetTokenDialogOpen}
        testId="reset-user-token-dialog"
        onOpenChange={(open) => {
          if (!open) vm.closeResetTokenDialog();
        }}
        title="Reset User Token"
        message={`Are you sure you want to reset the token for "${
          currentUser ? getFullName(currentUser) || currentUser.userId : ''
        }"? This will invalidate their current token.`}
        confirmLabel="Reset"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={vm.confirmResetToken}
      />
    </Box>
  );
}

export default EditUserView;
