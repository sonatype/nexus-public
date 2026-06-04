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

import React, { useState } from 'react';
import { Box, Flex, Text, Heading, Separator } from '@radix-ui/themes';
import { Loader2, Key, RefreshCw, AlertTriangle } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsFormSection,
  SettingsButton,
  SettingsAlert,
  SettingsPasswordInput,
  ConfirmDialog,
} from '../../../../shared/form';
import { useToast } from '../../../../shared';
import { UserForm } from './UserForm';
import { useUsersApi } from './useUsersApi';
import {
  User,
  UserFormData,
  DEFAULT_SOURCE,
  isExternalUser,
  getFullName,
} from './types';

import './UserDetail.scss';

interface UserDetailProps {
  user: User | null;
  loading: boolean;
  canEdit: boolean;
  canDelete: boolean;
  onSave: (data: UserFormData) => Promise<void>;
  onDelete: () => void;
  onCancel: () => void;
  error?: string;
}

/**
 * UserDetail - Detailed view and edit form for a single user
 */
export function UserDetail({
  user,
  loading,
  canEdit,
  canDelete,
  onSave,
  onDelete,
  onCancel,
  error,
}: UserDetailProps) {
  const { changePassword, resetUserToken, loading: apiLoading, error: apiError, setError } = useUsersApi();
  const toast = useToast();
  
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [showResetToken, setShowResetToken] = useState(false);
  const [resetTokenDialogOpen, setResetTokenDialogOpen] = useState(false);

  const isPro = ExtJS.isProEdition();
  const isAdmin = ExtJS.checkPermission('nexus:*');
  const state = ExtJS.state();
  const isAnonymous = user?.userId === state?.getValue?.('anonymousUsername');
  const isCurrentUser = user?.userId === state?.getUser?.()?.id;
  const isAdminUser = user?.userId === 'admin';
  const isExternal = user ? isExternalUser(user.source) : false;

  const showDeleteButton = canDelete && !isExternal && !isAnonymous && !isCurrentUser && !isAdminUser;
  const showChangePasswordButton = canEdit && !isExternal && !isAnonymous && isAdmin;
  const showResetTokenButton = isPro && !isExternal;

  // Loading state
  if (loading) {
    return (
      <Flex align="center" justify="center" className="user-detail__loading">
        <Loader2 size={24} className="user-detail__spinner" />
        <Text size="2">Loading user details...</Text>
      </Flex>
    );
  }

  // No user found
  if (!user) {
    return (
      <Box className="user-detail__not-found">
        <AlertTriangle size={24} />
        <Text size="2">User not found</Text>
      </Box>
    );
  }

  const handleChangePassword = async () => {
    setPasswordError(null);

    if (!newPassword) {
      setPasswordError('New password is required');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Passwords do not match');
      return;
    }

    try {
      await changePassword(user.userId, newPassword);
      toast.success(`Password changed successfully for ${getFullName(user)}`);
      setShowChangePassword(false);
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      // Error is set by the API hook
    }
  };

  const handleResetTokenClick = () => {
    setResetTokenDialogOpen(true);
  };

  const handleResetTokenConfirm = async () => {
    setResetTokenDialogOpen(false);
    try {
      await resetUserToken(user.userId, user.realm || user.source);
      toast.success(`User token has been reset for ${getFullName(user)}`);
      setShowResetToken(false);
    } catch (err) {
      // Error is set by the API hook
    }
  };

  // Read-only view for users without edit permission
  if (!canEdit) {
    return (
      <Box className="user-detail">
        <SettingsFormSection title="User Information" defaultOpen>
          <Box className="user-detail__info">
            <Flex className="user-detail__row">
              <Text size="2" weight="medium" className="user-detail__label">ID</Text>
              <Text size="2">{user.userId}</Text>
            </Flex>
            <Flex className="user-detail__row">
              <Text size="2" weight="medium" className="user-detail__label">First Name</Text>
              <Text size="2">{user.firstName}</Text>
            </Flex>
            <Flex className="user-detail__row">
              <Text size="2" weight="medium" className="user-detail__label">Last Name</Text>
              <Text size="2">{user.lastName}</Text>
            </Flex>
            <Flex className="user-detail__row">
              <Text size="2" weight="medium" className="user-detail__label">Email</Text>
              <Text size="2">{user.emailAddress || user.email}</Text>
            </Flex>
            <Flex className="user-detail__row">
              <Text size="2" weight="medium" className="user-detail__label">Status</Text>
              <Text size="2" className={`user-detail__status user-detail__status--${user.status}`}>
                {user.status}
              </Text>
            </Flex>
            <Flex className="user-detail__row">
              <Text size="2" weight="medium" className="user-detail__label">Source</Text>
              <Text size="2">{isExternal ? user.source : 'Local'}</Text>
            </Flex>
          </Box>
        </SettingsFormSection>

        <SettingsFormSection title="Assigned Roles">
          <Box className="user-detail__roles">
            {user.roles && user.roles.length > 0 ? (
              user.roles.map((role) => (
                <Text key={role} size="2" className="user-detail__role">
                  {role}
                </Text>
              ))
            ) : (
              <Text size="2" className="user-detail__no-roles">
                No roles assigned
              </Text>
            )}
          </Box>
        </SettingsFormSection>

        <Flex gap="3" className="user-detail__actions">
          <SettingsButton variant="secondary" onClick={onCancel}>
            Back to List
          </SettingsButton>
        </Flex>
      </Box>
    );
  }

  // Edit view
  return (
    <Box className="user-detail">
      {/* Error Messages */}
      {apiError && (
        <SettingsAlert type="error" onClose={() => setError(null)}>
          {apiError}
        </SettingsAlert>
      )}

      {/* User Form */}
      <UserForm
        user={user}
        isCreate={false}
        onSave={onSave}
        onCancel={onCancel}
        onDelete={showDeleteButton ? onDelete : undefined}
        loading={loading || apiLoading}
        error={error}
      />

      {/* Account Actions -- grouped in one section */}
      {(showChangePasswordButton || showResetTokenButton) && (
        <SettingsFormSection title="Account Actions">
          <Flex direction="column" gap="3">
            {showChangePasswordButton && (
              <>
                <Flex align="center" justify="between" className="user-detail__action-row">
                  <Box>
                    <Text size="2" weight="medium">Change Password</Text>
                    <Text as="p" size="1" color="gray">Set a new password for this user</Text>
                  </Box>
                  {!showChangePassword && (
                    <SettingsButton variant="secondary" icon={Key} onClick={() => setShowChangePassword(true)}>
                      Change Password
                    </SettingsButton>
                  )}
                </Flex>

                {showChangePassword && (
                  <Box className="user-detail__password-form">
                    {passwordError && (
                      <SettingsAlert type="error" onClose={() => setPasswordError(null)}>
                        {passwordError}
                      </SettingsAlert>
                    )}
                    <SettingsPasswordInput
                      name="newPassword"
                      label="New Password"
                      value={newPassword}
                      onChange={setNewPassword}
                      required
                      autoComplete="new-password"
                    />
                    <SettingsPasswordInput
                      name="confirmPassword"
                      label="Confirm New Password"
                      value={confirmPassword}
                      onChange={setConfirmPassword}
                      required
                      autoComplete="new-password"
                    />
                    <Flex gap="2">
                      <SettingsButton
                        variant="primary"
                        onClick={handleChangePassword}
                        loading={apiLoading}
                      >
                        Change Password
                      </SettingsButton>
                      <SettingsButton
                        variant="secondary"
                        onClick={() => {
                          setShowChangePassword(false);
                          setNewPassword('');
                          setConfirmPassword('');
                          setPasswordError(null);
                        }}
                      >
                        Cancel
                      </SettingsButton>
                    </Flex>
                  </Box>
                )}
              </>
            )}

            {showChangePasswordButton && showResetTokenButton && <Separator size="4" />}

            {showResetTokenButton && (
              <Flex align="center" justify="between" className="user-detail__action-row">
                <Box>
                  <Text size="2" weight="medium">Reset User Token</Text>
                  <Text as="p" size="1" color="gray">Invalidate current token and force regeneration</Text>
                </Box>
                <SettingsButton
                  variant="secondary"
                  icon={RefreshCw}
                  onClick={handleResetTokenClick}
                  loading={apiLoading}
                >
                  Reset Token
                </SettingsButton>
              </Flex>
            )}
          </Flex>
        </SettingsFormSection>
      )}

      {/* Reset Token Confirmation Dialog */}
      <ConfirmDialog
        open={resetTokenDialogOpen}
        testId="reset-user-token-dialog"
        onOpenChange={setResetTokenDialogOpen}
        title="Reset User Token"
        message={`Are you sure you want to reset the token for "${user ? getFullName(user) : ''}"? This will invalidate their current token.`}
        confirmLabel="Reset"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleResetTokenConfirm}
      />
    </Box>
  );
}

export default UserDetail;


