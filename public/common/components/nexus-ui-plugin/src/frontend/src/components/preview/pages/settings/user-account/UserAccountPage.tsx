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

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Box, Button, Card, Flex, Text, Heading, Badge } from '@radix-ui/themes';
import { ArrowRight, Key, Package, User, Loader2, Shield, Mail, Calendar } from 'lucide-react';
import { ExtJS } from '../../../../../interface/ExtJS';
import { restClient } from '../../../../../interface/api';
import { useToast } from '../../../shared';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsAlert,
} from '../../../shared/form';

import './UserAccountPage.scss';

interface UserAccountPageProps {
  className?: string;
}

interface UserData {
  userId: string;
  firstName: string;
  lastName: string;
  emailAddress: string;
  source: string;
  status: string;
  roles: string[];
  readOnly?: boolean;
}

interface PasswordChangeData {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * UserAccountPage - User Account settings page for Preview UI
 *
 * Allows users to view and update their account information.
 */
export function UserAccountPage({ className }: UserAccountPageProps) {
  const user = ExtJS.useUser();
  const toast = useToast();
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [userData, setUserData] = useState<UserData | null>(null);
  const [passwordData, setPasswordData] = useState<PasswordChangeData>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  // Load user data
  useEffect(() => {
    if (user) {
      setUserData({
        userId: user.id || '',
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        emailAddress: user.email || '',
        source: user.source || 'default',
        status: user.status || 'active',
        roles: user.roles || [],
        readOnly: user.external || false,
      });
    }
    // Always finish loading, whether user exists or not
    setLoadingInitial(false);
  }, [user]);

  // Success/error handled via Toast (Sprint 15)

  // Password validation
  const passwordErrors = useMemo(() => {
    const errors: Record<string, string> = {};
    
    if (touched.newPassword && passwordData.newPassword && passwordData.newPassword.length < 8) {
      errors.newPassword = 'Password must be at least 8 characters';
    }
    
    if (touched.confirmPassword && passwordData.newPassword !== passwordData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    return errors;
  }, [passwordData, touched]);

  const hasPasswordErrors = useMemo(() => {
    return Object.keys(passwordErrors).length > 0 ||
      !passwordData.currentPassword ||
      !passwordData.newPassword ||
      !passwordData.confirmPassword;
  }, [passwordErrors, passwordData]);

  // Handle password field change
  const handlePasswordChange = useCallback((field: keyof PasswordChangeData, value: string) => {
    setPasswordData((prev) => ({ ...prev, [field]: value }));
    setTouched((prev) => ({ ...prev, [field]: true }));
  }, []);

  // Handle password change submit
  const handlePasswordSubmit = useCallback(async () => {
    setTouched({
      currentPassword: true,
      newPassword: true,
      confirmPassword: true,
    });

    if (hasPasswordErrors) {
      return;
    }

    try {
      const userId = userData?.userId;
      if (!userId) throw new Error('User not found');
      await restClient.put(
        `/service/rest/v1/security/users/${encodeURIComponent(userId)}/change-password`,
        passwordData.newPassword,
        { headers: { 'Content-Type': 'text/plain' } }
      );
      toast.success('Password changed successfully');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setTouched({});
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to change password';
      toast.error(message);
    }
  }, [hasPasswordErrors, userData, passwordData.newPassword]);

  // Reset password form
  const handlePasswordReset = useCallback(() => {
    setPasswordData({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    });
    setTouched({});
    setError(null);
  }, []);

  // Loading state
  if (loadingInitial) {
    return (
      <Box className={`user-account-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="user-account-page__loading">
          <Loader2 size={24} className="user-account-page__spinner" />
          <Text size="2">Loading account information...</Text>
        </Flex>
      </Box>
    );
  }

  // Not logged in
  if (!user || !userData) {
    return (
      <Box className={`user-account-page ${className || ''}`.trim()}>
        <Flex align="center" gap="3" className="user-account-page__header">
          <User size={24} className="user-account-page__icon" />
          <Box>
            <Heading as="h1" size="6" weight="medium">User Account</Heading>
            <Text size="2" className="user-account-page__description">
              Manage your account settings
            </Text>
          </Box>
        </Flex>
        
        <SettingsAlert type="warning">
          You must be logged in to view your account settings.
        </SettingsAlert>
      </Box>
    );
  }

  return (
    <Box className={`user-account-page ${className || ''}`.trim()}>
      {/* Header */}
      <Flex align="center" gap="3" className="user-account-page__header">
        <User size={24} className="user-account-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">User Account</Heading>
          <Text size="2" className="user-account-page__description">
            Manage your account settings
          </Text>
        </Box>
      </Flex>

      {/* Alerts */}
      {error && (
        <Box className="user-account-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}
      {/* Account Information (Read-only) */}
      <SettingsFormSection title="Account Information">
        <Box className="user-account-page__info">
          <Flex className="user-account-page__info-row">
            <User size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Username</Text>
              <Text size="2" weight="medium">{userData.userId}</Text>
            </Box>
          </Flex>

          <Flex className="user-account-page__info-row">
            <Mail size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Email</Text>
              <Text size="2" weight="medium">{userData.emailAddress || 'Not set'}</Text>
            </Box>
          </Flex>

          <Flex className="user-account-page__info-row">
            <Text size="1" className="user-account-page__info-label" style={{ marginLeft: '24px' }}>Name</Text>
            <Text size="2" weight="medium">
              {userData.firstName || userData.lastName
                ? `${userData.firstName} ${userData.lastName}`.trim()
                : 'Not set'}
            </Text>
          </Flex>

          <Flex className="user-account-page__info-row">
            <Shield size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Source</Text>
              <Badge variant="soft" color={userData.source === 'default' ? 'blue' : 'gray'}>
                {userData.source}
              </Badge>
            </Box>
          </Flex>

          {userData.roles.length > 0 && (
            <Flex className="user-account-page__info-row" align="start">
              <Text size="1" className="user-account-page__info-label" style={{ marginLeft: '24px' }}>Roles</Text>
              <Flex gap="1" wrap="wrap">
                {userData.roles.slice(0, 5).map((role) => (
                  <Badge key={role} variant="outline" size="1">
                    {role}
                  </Badge>
                ))}
                {userData.roles.length > 5 && (
                  <Badge variant="outline" size="1">
                    +{userData.roles.length - 5} more
                  </Badge>
                )}
              </Flex>
            </Flex>
          )}
        </Box>
      </SettingsFormSection>

      {/* Change Password (only for local users) */}
      {!userData.readOnly && (
        <SettingsForm
          title=""
          onSubmit={handlePasswordSubmit}
          onCancel={handlePasswordReset}
          submitDisabled={hasPasswordErrors}
          submitLabel="Change Password"
          showActions={true}
        >
          <SettingsFormSection title="Change Password">
            <Text size="2" className="user-account-page__section-description">
              Update your account password. Password must be at least 8 characters.
            </Text>

            <SettingsPasswordInput
              name="currentPassword"
              label="Current Password"
              value={passwordData.currentPassword}
              onChange={(value) => handlePasswordChange('currentPassword', value)}
              required
            />

            <SettingsPasswordInput
              name="newPassword"
              label="New Password"
              value={passwordData.newPassword}
              onChange={(value) => handlePasswordChange('newPassword', value)}
              error={passwordErrors.newPassword}
              helpText="Minimum 8 characters"
              required
            />

            <SettingsPasswordInput
              name="confirmPassword"
              label="Confirm New Password"
              value={passwordData.confirmPassword}
              onChange={(value) => handlePasswordChange('confirmPassword', value)}
              error={passwordErrors.confirmPassword}
              required
            />
          </SettingsFormSection>
        </SettingsForm>
      )}

      {userData.readOnly && (
        <SettingsFormSection title="Password">
          <SettingsAlert type="info">
            Your account is managed externally. Password changes must be made through your
            authentication provider ({userData.source}).
          </SettingsAlert>
        </SettingsFormSection>
      )}

      {/* Security — links to User Token and NuGet API Key pages */}
      <SettingsFormSection title="Security">
        <Flex direction="column" gap="3" data-testid="security-section">
          {/* User Token */}
          <Card size="1" data-testid="user-token-link-card">
            <Flex align="center" justify="between">
              <Flex align="center" gap="3">
                <Key size={20} color="var(--gray-10)" />
                <Box>
                  <Text size="2" weight="medium">User Token</Text>
                  <Text size="1" color="gray" style={{ display: 'block' }}>
                    Personal access token for API authentication
                  </Text>
                </Box>
              </Flex>
              <Button
                variant="ghost"
                size="2"
                onClick={() => { window.location.hash = '#preview/user/usertoken'; }}
                aria-label="Manage User Token"
                data-testid="manage-user-token-btn"
              >
                Manage User Token
                <ArrowRight size={14} />
              </Button>
            </Flex>
          </Card>

          {/* NuGet API Key */}
          <Card size="1" data-testid="nuget-key-link-card">
            <Flex align="center" justify="between">
              <Flex align="center" gap="3">
                <Package size={20} color="var(--gray-10)" />
                <Box>
                  <Flex align="center" gap="2">
                    <Text size="2" weight="medium">NuGet API Key</Text>
                    <Badge variant="soft" color="blue" size="1">Pro</Badge>
                  </Flex>
                  <Text size="1" color="gray" style={{ display: 'block' }}>
                    API key for NuGet package feed authentication
                  </Text>
                </Box>
              </Flex>
              <Button
                variant="ghost"
                size="2"
                onClick={() => { window.location.hash = '#preview/user/nugetapitoken'; }}
                aria-label="Manage NuGet API Key"
                data-testid="manage-nuget-key-btn"
              >
                Manage NuGet API Key
                <ArrowRight size={14} />
              </Button>
            </Flex>
          </Card>
        </Flex>
      </SettingsFormSection>
    </Box>
  );
}

export default UserAccountPage;


