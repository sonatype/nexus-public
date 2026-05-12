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


const navigateTo = (path: string) => {
  window.location.hash = path;
}


import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import * as Dialog from '@radix-ui/react-dialog';
import { Loader2, RefreshCw, AlertTriangle } from 'lucide-react';
import { ExtJS, Permissions } from '@sonatype/nexus-ui-plugin';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsButton,
  SettingsAlert,
} from '../../../shared/form';
import { useToast, PageHeader } from '../../../../shared';
import { useUnsavedChangesWarning } from '../../../../shared';
import { useUserTokensApi } from './useUserTokensApi';
import { 
  UserTokenSettings, 
  DEFAULT_USER_TOKEN_SETTINGS, 
  RESET_CONFIRMATION_STRING 
} from './types';

import './UserTokensPage.scss';

/**
 * UserTokensPage - User Token settings configuration page for Preview UI
 */
export function UserTokensPage() {
  const { loading, error, setError, fetchSettings, saveSettings, resetAllTokens } = useUserTokensApi();
  
  const [settings, setSettings] = useState<UserTokenSettings>(DEFAULT_USER_TOKEN_SETTINGS);
  const [pristineSettings, setPristineSettings] = useState<UserTokenSettings>(DEFAULT_USER_TOKEN_SETTINGS);
  const [loadingInitial, setLoadingInitial] = useState(true);

  // Toast notifications (app-level provider)
  const toast = useToast();
  
  // Reset confirmation modal state
  const [showResetModal, setShowResetModal] = useState(false);
  const [confirmationString, setConfirmationString] = useState('');
  const [confirmationError, setConfirmationError] = useState<string | null>(null);
  
  // Expiration change warning modal
  const [showExpirationWarning, setShowExpirationWarning] = useState(false);

  const canUpdate = ExtJS.checkPermission(Permissions.USER_TOKENS_SETTINGS.UPDATE);
  const canDelete = ExtJS.checkPermission(Permissions.USER_TOKENS_USERS.DELETE);

  // Load settings on mount
  useEffect(() => {
    setLoadingInitial(true);
    fetchSettings()
      .then((data) => {
        setSettings(data);
        setPristineSettings(data);
      })
      .catch(() => {
        // Error is set by the hook
      })
      .finally(() => {
        setLoadingInitial(false);
      });
  }, [fetchSettings]);

  // Check if form is pristine
  const isPristine = useMemo(() => {
    return (
      settings.enabled === pristineSettings.enabled &&
      settings.protectContent === pristineSettings.protectContent &&
      settings.expirationEnabled === pristineSettings.expirationEnabled &&
      settings.expirationDays === pristineSettings.expirationDays
    );
  }, [settings, pristineSettings]);

  // Check if expiration settings changed
  const expirationChanged = useMemo(() => {
    return settings.expirationEnabled !== pristineSettings.expirationEnabled;
  }, [settings.expirationEnabled, pristineSettings.expirationEnabled]);

  // Validate expiration days
  const expirationDaysError = useMemo(() => {
    if (!settings.expirationEnabled) return undefined;
    if (!settings.expirationDays) return 'Expiration days is required';
    if (settings.expirationDays < 1 || settings.expirationDays > 999) {
      return 'Must be between 1 and 999 days';
    }
    if (!Number.isInteger(Number(settings.expirationDays))) {
      return 'Must be a whole number';
    }
    return undefined;
  }, [settings.expirationEnabled, settings.expirationDays]);

  const handleChange = useCallback((field: keyof UserTokenSettings, value: boolean | number) => {
    setSettings((prev) => {
      // If disabling user tokens, reset all related settings
      if (field === 'enabled' && value === false) {
        return {
          ...prev,
          enabled: false,
          protectContent: false,
          expirationEnabled: false,
          expirationDays: 30,
        };
      }
      return { ...prev, [field]: value };
    });
  }, []);

  // performSave must be defined BEFORE handleSubmit since handleSubmit references it
  const performSave = useCallback(async () => {
    setShowExpirationWarning(false);
    try {
      await saveSettings(settings);
      setPristineSettings(settings);
      toast.success('User token settings saved successfully');
    } catch (err) {
      throw err;
    }
  }, [settings, saveSettings, toast]);

  const handleSubmit = useCallback(async () => {
    // Check if expiration settings changed - show warning
    if (expirationChanged) {
      setShowExpirationWarning(true);
      return;
    }
    await performSave();
  }, [expirationChanged, performSave]);

  const handleDiscard = useCallback(() => {
    setSettings(pristineSettings);
    setError(null);
  }, [pristineSettings, setError]);

  const handleResetAllTokens = useCallback(() => {
    setShowResetModal(true);
    setConfirmationString('');
    setConfirmationError(null);
  }, []);

  const confirmResetAllTokens = useCallback(async () => {
    if (confirmationString !== RESET_CONFIRMATION_STRING) {
      setConfirmationError(`Please type "${RESET_CONFIRMATION_STRING}" to confirm`);
      return;
    }

    try {
      await resetAllTokens();
      setShowResetModal(false);
      setConfirmationString('');
      toast.success('All user tokens have been reset');
    } catch (err) {
      // Error is set by the hook
    }
  }, [confirmationString, resetAllTokens, toast]);

  const cancelResetAllTokens = useCallback(() => {
    setShowResetModal(false);
    setConfirmationString('');
    setConfirmationError(null);
  }, []);

  // Loading state
  if (loadingInitial) {
    return (
      <Box className="user-tokens-page" data-testid="user-tokens-page" data-loading="true">
        <Flex align="center" justify="center" className="user-tokens-page__loading">
          <Loader2 size={24} className="user-tokens-page__spinner" />
          <Text size="2">Loading user token settings...</Text>
        </Flex>
      </Box>
    );
  }

  // Read-only view for users without update permission
  if (!canUpdate) {
    return (
      <Box
        className="user-tokens-page"
        data-testid="user-tokens-page"
        data-view="readonly"
        px={{ initial: '4', md: '6', lg: '6' }}
        py={{ initial: '4', md: '5', lg: '6' }}
      >
        <Box mb="4">
          <PageHeader
            title="User Tokens"
            description="Configure user token settings"
          
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'User Tokens' }
          ]}
/>
        </Box>

        <SettingsFormSection title="Current Settings" defaultOpen>
          <Box className="user-tokens-page__readonly">
            <Flex className="user-tokens-page__row">
              <Text size="2" weight="medium" className="user-tokens-page__label">User Tokens</Text>
              <Text size="2">{settings.enabled ? 'Enabled' : 'Disabled'}</Text>
            </Flex>
            <Flex className="user-tokens-page__row">
              <Text size="2" weight="medium" className="user-tokens-page__label">Repository Authentication</Text>
              <Text size="2">{settings.protectContent ? 'Required' : 'Not Required'}</Text>
            </Flex>
            <Flex className="user-tokens-page__row">
              <Text size="2" weight="medium" className="user-tokens-page__label">Token Expiration</Text>
              <Text size="2">
                {settings.expirationEnabled 
                  ? `${settings.expirationDays} days` 
                  : 'Never expires'}
              </Text>
            </Flex>
          </Box>
        </SettingsFormSection>
      </Box>
    );
  }

  return (
    <Box
      className="user-tokens-page"
      data-testid="user-tokens-page"
      data-view="edit"
      data-loading={loading ? 'true' : 'false'}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {/* Header */}
      <Box mb="4">
        <PageHeader
          title="User Tokens"
          description="Configure user token settings for repository authentication"
        />
      </Box>

      {/* Alerts */}
      {error && (
        <Box className="user-tokens-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <SettingsForm
        title=""
        onSubmit={handleSubmit}
        onCancel={handleDiscard}
        loading={loading}
        pristine={isPristine}
        submitDisabled={!!expirationDaysError}
        showActions={true}
        testId="user-tokens-form"
        data-submitting={loading ? 'true' : 'false'}
        data-valid={!expirationDaysError ? 'true' : 'false'}
      >
        {/* User Tokens Enable/Disable */}
        <SettingsFormSection title="User Token Configuration" defaultOpen>
          <Text size="2" className="user-tokens-page__help-text">
            User tokens allow users to authenticate securely without typical user credentials such as those used by LDAP or Crowd.
          </Text>

          <SettingsCheckbox
            name="enabled"
            label="Enable user tokens"
            checked={settings.enabled}
            onChange={(checked) => handleChange('enabled', checked)}
            description="Allow users to generate and use authentication tokens"
          />
        </SettingsFormSection>

        {/* Repository Authentication */}
        <SettingsFormSection title="Repository Authentication" defaultOpen={settings.enabled}>
          <SettingsCheckbox
            name="protectContent"
            label="Require user tokens for repository authentication"
            checked={settings.protectContent}
            onChange={(checked) => handleChange('protectContent', checked)}
            description="When enabled, users must use tokens instead of passwords to access repository content"
            disabled={!settings.enabled}
          />
        </SettingsFormSection>

        {/* Token Expiration */}
        <SettingsFormSection title="Token Expiration" defaultOpen={settings.enabled}>
          <SettingsCheckbox
            name="expirationEnabled"
            label="Enable token expiration"
            checked={settings.expirationEnabled}
            onChange={(checked) => handleChange('expirationEnabled', checked)}
            description="Automatically expire tokens after a specified number of days"
            disabled={!settings.enabled}
          />

          {settings.expirationEnabled && (
            <SettingsTextInput
              name="expirationDays"
              label="Token expiration (days)"
              type="number"
              value={settings.expirationDays}
              onChange={(value) => handleChange('expirationDays', parseInt(value, 10) || 0)}
              error={expirationDaysError}
              helpText="Number of days before tokens expire (1-999)"
              min={1}
              max={999}
              disabled={!settings.enabled}
              required
            />
          )}
        </SettingsFormSection>

        {/* Reset All Tokens */}
        {canDelete && settings.enabled && pristineSettings.enabled && (
          <SettingsFormSection title="Reset All Tokens">
            <Text size="2" className="user-tokens-page__warning-text">
              <AlertTriangle size={16} className="user-tokens-page__warning-icon" />
              Resetting all tokens will invalidate every user's current token. Users will need to generate new tokens.
            </Text>
            <SettingsButton
              variant="danger"
              onClick={handleResetAllTokens}
              disabled={loading}
              icon={RefreshCw}
            >
              Reset All User Tokens
            </SettingsButton>
          </SettingsFormSection>
        )}
      </SettingsForm>

      {/* Reset Confirmation Modal */}
      <Dialog.Root open={showResetModal} onOpenChange={(open) => !open && cancelResetAllTokens()}>
        <Dialog.Portal>
          <Dialog.Overlay className="user-tokens-page__modal-overlay" />
          <Dialog.Content className="user-tokens-page__modal" data-testid="user-tokens-reset-modal">
            <Flex align="center" gap="3" className="user-tokens-page__modal-header">
              <AlertTriangle size={24} className="user-tokens-page__modal-icon" />
              <Dialog.Title asChild>
                <Heading as="h2" size="4" weight="medium" className="user-tokens-page__modal-title">
                  Reset All User Tokens
                </Heading>
              </Dialog.Title>
            </Flex>
            <Dialog.Description asChild>
              <Box className="user-tokens-page__modal-description">
                <Text size="2" className="user-tokens-page__modal-text">
                  This action will invalidate all existing user tokens. Users will need to generate new tokens to authenticate.
                </Text>
                <Text size="2" className="user-tokens-page__modal-text">
                  Type <strong>{RESET_CONFIRMATION_STRING}</strong> to confirm:
                </Text>
              </Box>
            </Dialog.Description>
            <SettingsTextInput
              name="confirmationString"
              value={confirmationString}
              onChange={setConfirmationString}
              error={confirmationError || undefined}
              placeholder={RESET_CONFIRMATION_STRING}
              data-testid="user-tokens-reset-confirmation-input"
            />
            <Flex gap="3" justify="end" className="user-tokens-page__modal-actions">
              <Dialog.Close asChild>
                <SettingsButton
                  variant="secondary"
                  disabled={loading}
                  data-testid="user-tokens-reset-cancel"
                >
                  Cancel
                </SettingsButton>
              </Dialog.Close>
              <SettingsButton
                variant="danger"
                onClick={confirmResetAllTokens}
                loading={loading}
                data-testid="user-tokens-reset-confirm"
              >
                Reset All Tokens
              </SettingsButton>
            </Flex>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      {/* Expiration Warning Modal */}
      <Dialog.Root open={showExpirationWarning} onOpenChange={setShowExpirationWarning}>
        <Dialog.Portal>
          <Dialog.Overlay className="user-tokens-page__modal-overlay" />
          <Dialog.Content className="user-tokens-page__modal" data-testid="user-tokens-expiration-modal">
            <Flex align="center" gap="3" className="user-tokens-page__modal-header">
              <AlertTriangle size={24} className="user-tokens-page__modal-icon" />
              <Dialog.Title asChild>
                <Heading as="h2" size="4" weight="medium" className="user-tokens-page__modal-title">
                  Token Expiration Change
                </Heading>
              </Dialog.Title>
            </Flex>
            <Dialog.Description asChild>
              <Box className="user-tokens-page__modal-description">
                <Text size="2" className="user-tokens-page__modal-text">
                  {settings.expirationEnabled
                    ? 'Enabling token expiration will cause existing tokens to expire based on their creation date. Some users may need to generate new tokens.'
                    : 'Disabling token expiration will allow all existing tokens to remain valid indefinitely.'}
                </Text>
                <Text size="2" className="user-tokens-page__modal-text">
                  Do you want to continue?
                </Text>
              </Box>
            </Dialog.Description>
            <Flex gap="3" justify="end" className="user-tokens-page__modal-actions">
              <Dialog.Close asChild>
                <SettingsButton
                  variant="secondary"
                  disabled={loading}
                  data-testid="user-tokens-expiration-cancel"
                >
                  Cancel
                </SettingsButton>
              </Dialog.Close>
              <SettingsButton
                variant="primary"
                onClick={performSave}
                loading={loading}
                data-testid="user-tokens-expiration-confirm"
              >
                Continue
              </SettingsButton>
            </Flex>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </Box>
  );
}

export default UserTokensPage;


