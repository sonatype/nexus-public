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

import React, { useEffect, useState } from 'react';
import { Box, Button, Card, Flex, Text, Heading, Badge } from '@radix-ui/themes';
import { ArrowRight, Key, Package, User, Loader2, Shield, Mail } from 'lucide-react';
import { ExtJS } from '../../../../../interface/ExtJS';

import {
  SettingsFormSection,
  SettingsPasswordInput,
  SettingsAlert,
} from '../../../shared/form';
import { useUserAccountForm } from './useUserAccountForm';

import './UserAccountPage.scss';

interface UserAccountPageProps {
  className?: string;
}

const LOAD_ERROR_MESSAGE = 'Failed to load account information.';

export function UserAccountPage({ className }: UserAccountPageProps) {
  const user = ExtJS.useUser();
  const form = useUserAccountForm();
  const [dismissedError, setDismissedError] = useState(false);

  useEffect(() => {
    if (form.hasLoadError) {
      setDismissedError(false);
    }
  }, [form.hasLoadError]);

  if (!user) {
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

  if (form.isLoading) {
    return (
      <Box className={`user-account-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="user-account-page__loading">
          <Loader2 size={24} className="user-account-page__spinner" />
          <Text size="2">Loading account information...</Text>
        </Flex>
      </Box>
    );
  }

  const { data, validationErrors, touched } = form;
  const profileLoaded = !form.hasLoadError && Boolean(data.userId);

  const displayName = data.firstName || data.lastName
    ? `${data.firstName} ${data.lastName}`.trim()
    : 'Not set';
  const displayEmail = data.email || 'Not set';
  const isExternal = data.external;
  const showLoadError = form.hasLoadError && !dismissedError;

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

      {showLoadError && (
        <Box className="user-account-page__alerts">
          <SettingsAlert type="error" onClose={() => setDismissedError(true)}>
            {LOAD_ERROR_MESSAGE}
          </SettingsAlert>
        </Box>
      )}

      <SettingsFormSection title="Account Information">
        <Box className="user-account-page__info">
          <Flex className="user-account-page__info-row">
            <User size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Username</Text>
              <Text size="2" weight="medium">{data.userId || user.id}</Text>
            </Box>
          </Flex>

          <Flex className="user-account-page__info-row">
            <Mail size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Email</Text>
              <Text size="2" weight="medium">{displayEmail}</Text>
            </Box>
          </Flex>

          <Flex className="user-account-page__info-row">
            <User size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Name</Text>
              <Text size="2" weight="medium">{displayName}</Text>
            </Box>
          </Flex>

          <Flex className="user-account-page__info-row">
            <Shield size={16} className="user-account-page__info-icon" />
            <Box>
              <Text size="1" className="user-account-page__info-label">Source</Text>
              <Badge variant="soft" color={isExternal ? 'gray' : 'blue'}>
                {isExternal ? 'external' : 'default'}
              </Badge>
            </Box>
          </Flex>

        </Box>
      </SettingsFormSection>

      {profileLoaded && !isExternal && (
        <SettingsFormSection title="Change Password">
          <Text size="2" className="user-account-page__section-description">
            Update your account password. Password must be at least 8 characters.
          </Text>

          <SettingsPasswordInput
            name="currentPassword"
            label="Current Password"
            value={data.currentPassword}
            onChange={(value) => form.send({ type: 'UPDATE', name: 'currentPassword', value })}
            required
          />

          <SettingsPasswordInput
            name="newPassword"
            label="New Password"
            value={data.newPassword}
            onChange={(value) => form.send({ type: 'UPDATE', name: 'newPassword', value })}
            error={touched.newPassword ? validationErrors.newPassword ?? undefined : undefined}
            helpText="Minimum 8 characters"
            required
          />

          <SettingsPasswordInput
            name="confirmPassword"
            label="Confirm New Password"
            value={data.confirmPassword}
            onChange={(value) => form.send({ type: 'UPDATE', name: 'confirmPassword', value })}
            error={touched.confirmPassword ? validationErrors.confirmPassword ?? undefined : undefined}
            required
          />

          <Flex gap="2" mt="4">
            <Button
              type="button"
              variant="solid"
              size="2"
              disabled={!form.canSubmitPassword || form.isSaving}
              onClick={() => form.submit()}
              data-testid="change-password-submit"
            >
              Change Password
            </Button>
            <Button
              type="button"
              variant="outline"
              size="2"
              onClick={() => form.reset()}
              data-testid="change-password-cancel"
            >
              Discard
            </Button>
          </Flex>
        </SettingsFormSection>
      )}

      {profileLoaded && isExternal && (
        <SettingsFormSection title="Password">
          <SettingsAlert type="info">
            Your account is managed externally. Password changes must be made through your
            authentication provider.
          </SettingsAlert>
        </SettingsFormSection>
      )}

      <SettingsFormSection title="Security">
        <Flex direction="column" gap="3" data-testid="security-section">
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
