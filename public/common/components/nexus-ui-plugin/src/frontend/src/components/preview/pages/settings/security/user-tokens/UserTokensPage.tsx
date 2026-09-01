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
import React from 'react';
import { Box, Flex, Text, Heading, AlertDialog, Button, TextField } from '@radix-ui/themes';
import * as Dialog from '@radix-ui/react-dialog';
import { Loader2, RefreshCw, AlertTriangle } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { Permissions } from '../../../../../../constants/Permissions';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { useUserTokensForm } from './useUserTokensForm';
import { RESET_CONFIRMATION_STRING } from './types';

import './UserTokensPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * UserTokensPage - User Token settings configuration page for Preview UI
 */
export function UserTokensPage() {
  const {
    data,
    pristineData,
    expirationDaysError,
    isPristine,
    isLoading,
    isSaving,
    isResetting,
    error,
    showExpirationWarning,
    showResetModal,
    resetConfirmationInput,
    resetConfirmationError,
    handleChange,
    handleSubmit,
    handleDiscard,
    confirmSave,
    cancelSave,
    requestReset,
    setResetConfirmation,
    confirmReset,
    cancelReset,
    clearError,
  } = useUserTokensForm();

  const isBusy = isSaving || isResetting;

  const canUpdate = ExtJS.checkPermission(Permissions.USER_TOKENS_SETTINGS.UPDATE);
  const canDelete = ExtJS.checkPermission(Permissions.USER_TOKENS_USERS.DELETE);

  // Loading state
  if (isLoading) {
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
              <Text size="2">{data.enabled ? 'Enabled' : 'Disabled'}</Text>
            </Flex>
            <Flex className="user-tokens-page__row">
              <Text size="2" weight="medium" className="user-tokens-page__label">Repository Authentication</Text>
              <Text size="2">{data.protectContent ? 'Required' : 'Not Required'}</Text>
            </Flex>
            <Flex className="user-tokens-page__row">
              <Text size="2" weight="medium" className="user-tokens-page__label">Token Expiration</Text>
              <Text size="2">
                {data.expirationEnabled
                  ? `${data.expirationDays} days`
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
      data-loading={isBusy ? 'true' : 'false'}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {/* Header */}
      <Box mb="4">
        <PageHeader
          title="User Tokens"
          description="Configure user token settings for repository authentication"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'User Tokens' }
          ]}
        />
      </Box>

      {/* Alerts */}
      {error && (
        <Box className="user-tokens-page__alerts">
          <SettingsAlert type="error" onClose={clearError}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <SettingsForm
        title=""
        onSubmit={handleSubmit}
        onCancel={handleDiscard}
        loading={isSaving}
        pristine={isPristine}
        submitDisabled={!!expirationDaysError}
        showActions={true}
        testId="user-tokens-form"
        data-submitting={isSaving ? 'true' : 'false'}
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
            checked={data.enabled}
            onChange={(checked) => handleChange('enabled', checked)}
            description="Allow users to generate and use authentication tokens"
          />
        </SettingsFormSection>

        {/* Repository Authentication */}
        <SettingsFormSection title="Repository Authentication" defaultOpen={data.enabled}>
          <SettingsCheckbox
            name="protectContent"
            label="Require user tokens for repository authentication"
            checked={data.protectContent}
            onChange={(checked) => handleChange('protectContent', checked)}
            description="When enabled, users must use tokens instead of passwords to access repository content"
            disabled={!data.enabled}
          />
        </SettingsFormSection>

        {/* Token Expiration */}
        <SettingsFormSection title="Token Expiration" defaultOpen={data.enabled}>
          <SettingsCheckbox
            name="expirationEnabled"
            label="Enable token expiration"
            checked={data.expirationEnabled}
            onChange={(checked) => handleChange('expirationEnabled', checked)}
            description="Automatically expire tokens after a specified number of days"
            disabled={!data.enabled}
          />

          {data.expirationEnabled && (
            <SettingsTextInput
              name="expirationDays"
              label="Token expiration (days)"
              type="number"
              value={data.expirationDays}
              onChange={(value) => handleChange('expirationDays', parseInt(value, 10) || 0)}
              error={expirationDaysError}
              helpText="Number of days before tokens expire (1-999)"
              min={1}
              max={999}
              disabled={!data.enabled}
              required
            />
          )}
        </SettingsFormSection>

        {/* Reset All Tokens */}
        {canDelete && data.enabled && pristineData.enabled && (
          <SettingsFormSection title="Reset All Tokens">
            <Text size="2" className="user-tokens-page__warning-text">
              <AlertTriangle size={16} className="user-tokens-page__warning-icon" />
              Resetting all tokens will invalidate every user's current token. Users will need to generate new tokens.
            </Text>
            <SettingsButton
              variant="danger"
              onClick={requestReset}
              disabled={isBusy}
              icon={RefreshCw}
            >
              Reset All User Tokens
            </SettingsButton>
          </SettingsFormSection>
        )}
      </SettingsForm>

      {/* Reset Confirmation Modal — aligned with the Delete Repository destructive
          pattern (Radix Themes AlertDialog). Confirmation stays machine-driven:
          confirm-on-click validates the typed phrase and, if wrong, keeps the modal
          open with an inline error rather than disabling the button. */}
      <AlertDialog.Root open={showResetModal} onOpenChange={(open) => !open && cancelReset()}>
        <AlertDialog.Content maxWidth="450px" data-testid="user-tokens-reset-modal">
          <AlertDialog.Title size="5">Reset all user tokens?</AlertDialog.Title>

          <AlertDialog.Description size="2" mb="4">
            This action will invalidate all existing user tokens. Users will need to generate new tokens to authenticate.
          </AlertDialog.Description>

          {/* Highlighted warning box showing the exact phrase the user must type.
              Sourced from RESET_CONFIRMATION_STRING so the displayed phrase, the
              placeholder, and the machine guard stay in sync. */}
          <Box
            p="3"
            mb="4"
            style={{
              backgroundColor: 'var(--red-2)',
              border: '1px solid var(--red-6)',
              borderRadius: '6px',
            }}
          >
            <Text size="2" weight="medium" style={{ display: 'block' }}>
              {RESET_CONFIRMATION_STRING}
            </Text>
            <Text size="1" color="gray">
              Type this to confirm reset
            </Text>
          </Box>

          {/* Acknowledgement input */}
          <Box mb="4">
            <Flex align="center" gap="1" mb="2">
              <Text size="2" weight="bold">
                Acknowledgement
              </Text>
              <Text size="2" style={{ color: 'var(--red-9)' }}>
                *
              </Text>
            </Flex>
            <TextField.Root
              size="2"
              value={resetConfirmationInput}
              onChange={(e) => setResetConfirmation(e.target.value)}
              onKeyDown={(e) => {
                // Enter mirrors the confirm click exactly: call confirmReset() and let the
                // machine guard decide. Correct phrase resets; wrong phrase shows the same
                // inline error a click would. No phrase check here.
                if (e.key === 'Enter' && !isResetting) {
                  confirmReset();
                }
              }}
              placeholder={`Type "${RESET_CONFIRMATION_STRING}" to confirm`}
              disabled={isResetting}
              color={resetConfirmationError ? 'red' : undefined}
              aria-label="Acknowledgement"
              data-testid="user-tokens-reset-confirmation-input"
              autoFocus
            />
            {resetConfirmationError && (
              <Text size="1" color="red" mt="1" style={{ display: 'block' }} role="alert">
                {resetConfirmationError}
              </Text>
            )}
          </Box>

          {/* Action Buttons — confirm is a plain Button (not AlertDialog.Action) so a
              wrong phrase does not auto-close the dialog; the machine controls closure. */}
          <Flex gap="3" mt="4" justify="end">
            <AlertDialog.Cancel>
              <Button
                variant="surface"
                color="gray"
                size="2"
                data-testid="user-tokens-reset-cancel"
              >
                Cancel
              </Button>
            </AlertDialog.Cancel>
            <Button
              variant="solid"
              color="red"
              size="2"
              onClick={confirmReset}
              disabled={isResetting}
              data-testid="user-tokens-reset-confirm"
            >
              {isResetting ? 'Resetting…' : 'Reset All Tokens'}
            </Button>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Root>

      {/* Expiration Warning Modal */}
      <Dialog.Root open={showExpirationWarning} onOpenChange={(open) => !open && cancelSave()}>
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
                  {data.expirationEnabled
                    ? 'Enabling token expiration will cause existing tokens to expire based on their creation date. Some users may need to generate new tokens. Do you want to continue?'
                    : 'Disabling token expiration will allow all existing tokens to remain valid indefinitely. Do you want to continue?'}
                </Text>
              </Box>
            </Dialog.Description>
            <Flex gap="4" justify="end" className="user-tokens-page__modal-actions">
              <Dialog.Close asChild>
                <SettingsButton
                  variant="secondary"
                  disabled={isSaving}
                  data-testid="user-tokens-expiration-cancel"
                >
                  Cancel
                </SettingsButton>
              </Dialog.Close>
              <SettingsButton
                variant="primary"
                onClick={confirmSave}
                loading={isSaving}
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
