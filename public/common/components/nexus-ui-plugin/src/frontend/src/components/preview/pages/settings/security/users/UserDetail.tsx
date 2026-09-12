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
import { Box, Flex, Text, Separator } from '@radix-ui/themes';
import { Loader2, Key, RefreshCw, AlertTriangle } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { MetadataGrid, StatusBadge, type StatusType } from '../../../../shared';

import {
  SettingsFormSection,
  SettingsButton,
  SettingsAlert,
  SettingsPasswordInput,
  ConfirmDialog,
} from '../../../../shared/form';
import { useToast } from '../../../../shared';
import { useUsersApi } from './useUsersApi';
import {
  User,
  isExternalUser,
  getFullName,
} from './types';

import './UserDetail.scss';

interface UserDetailProps {
  user: User | null;
  loading: boolean;
  canEdit: boolean;
  onCancel: () => void;
}

/**
 * UserDetail - Detailed view and edit form for a single user
 */
export function UserDetail({
  user,
  loading,
  canEdit,
  onCancel,
}: UserDetailProps) {
  const { changePassword, resetUserToken, loading: apiLoading, error: apiError, setError } = useUsersApi();
  const toast = useToast();
  
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [resetTokenDialogOpen, setResetTokenDialogOpen] = useState(false);

  const isPro = ExtJS.isProEdition();
  const isAdmin = ExtJS.checkPermission('nexus:*');
  const state = ExtJS.state();
  const isAnonymous = user?.userId === state?.getValue?.('anonymousUsername');
  const isExternal = user ? isExternalUser(user.source) : false;
  const activeCapabilities = state?.getValue?.('capabilityActiveTypes') || [];
  const isUserTokenCapabilityActive = activeCapabilities.includes('usertoken');
  const canResetUserToken = ExtJS.checkPermission('nexus:usertoken-user:delete');

  const showChangePasswordButton = canEdit && !isExternal && !isAnonymous && isAdmin;
  const showResetTokenButton = isPro && !isExternal && isUserTokenCapabilityActive && canResetUserToken;

  // Loading state
  if (loading) {
    return (
      <Flex
        align="center"
        justify="center"
        className="user-detail__loading"
        role="status"
        aria-live="polite"
        aria-busy="true"
      >
        <Loader2 size={24} className="user-detail__spinner" aria-hidden="true" />
        <Text size="2">Loading user details...</Text>
      </Flex>
    );
  }

  // No user found
  if (!user) {
    return (
      <Box
        className="user-detail__not-found"
        role="alert"
        aria-live="assertive"
      >
        <AlertTriangle size={24} aria-hidden="true" />
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
    } catch (_err) {
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
    } catch (_err) {
      // Error is set by the API hook
    }
  };

  // Read-only view for users without edit permission
  if (!canEdit) {
    const userStatusMap: Record<string, StatusType> = {
      active: 'success',
      disabled: 'offline',
    };
    const statusType: StatusType = userStatusMap[user.status] ?? 'unknown';
    const statusLabel = user.status.charAt(0).toUpperCase() + user.status.slice(1);

    return (
      <Box className="user-detail">
        <SettingsFormSection title="User Information" defaultOpen>
          <MetadataGrid items={[
            { label: 'ID', value: user.userId },
            { label: 'First Name', value: user.firstName },
            { label: 'Last Name', value: user.lastName },
            { label: 'Email', value: user.emailAddress || user.email },
            { label: 'Status', value: <StatusBadge status={statusType} label={statusLabel} size="small" /> },
            { label: 'Source', value: isExternal ? user.source : 'Local' },
          ]} />
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

