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

import React, { useMemo } from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import * as Dialog from '@radix-ui/react-dialog';
import { Loader2, AlertTriangle, X, UserX } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsSelect,
  SettingsAlert,
  SettingsButton,
} from '../../../../shared/form';
import { HelpSection, PageHeader } from '../../../../shared';
import { useAnonymousForm } from './useAnonymousForm';
import { AnonymousPageProps } from './types';

import './AnonymousPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * AnonymousPage - Anonymous Access settings configuration page for Preview UI
 *
 * Allows enabling/disabling anonymous access and configuring the anonymous user.
 * Uses XState form machine for state management.
 */
export function AnonymousPage({ className }: AnonymousPageProps) {
  // XState form hook handles load, save, dirty tracking, toast, validation, disable confirmation
  const anonForm = useAnonymousForm();

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');

  // Convert realm types to options format for SettingsSelect
  const realmOptions = useMemo(() => {
    return anonForm.realmTypes.map((realm) => ({
      value: realm.id,
      label: realm.name,
    }));
  }, [anonForm.realmTypes]);

  // Loading state
  if (anonForm.isLoading) {
    return (
      <Box className={`anonymous-page ${className || ''}`.trim()} data-testid="anonymous-page">
        <Flex align="center" justify="center" className="anonymous-page__loading">
          <Loader2 size={24} className="anonymous-page__spinner" />
          <Text size="2">Loading anonymous access settings...</Text>
        </Flex>
      </Box>
    );
  }

  // Read-only view for users without update permission
  if (!canUpdate) {
    return (
      <Box
        className={`anonymous-page ${className || ''}`.trim()}
        data-testid="anonymous-page"
        data-mode="view"
        px={{ initial: '4', md: '6', lg: '6' }}
        py={{ initial: '4', md: '5', lg: '6' }}
      >
        <Box mb="4">
          <PageHeader
            title="Anonymous Access"
            description="Configure anonymous user access settings"
          
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Anonymous' }
          ]}
/>
        </Box>

        <SettingsFormSection title="Current Settings" defaultOpen>
          <Box className="anonymous-page__readonly">
            <Flex className="anonymous-page__row">
              <Text size="2" weight="medium" className="anonymous-page__label">Anonymous Access</Text>
              <Text size="2">{anonForm.formData.enabled ? 'Enabled' : 'Disabled'}</Text>
            </Flex>
            <Flex className="anonymous-page__row">
              <Text size="2" weight="medium" className="anonymous-page__label">Username</Text>
              <Text size="2">{anonForm.formData.userId}</Text>
            </Flex>
            <Flex className="anonymous-page__row">
              <Text size="2" weight="medium" className="anonymous-page__label">Realm</Text>
              <Text size="2">
                {anonForm.realmTypes.find((r) => r.id === anonForm.formData.realmName)?.name || anonForm.formData.realmName}
              </Text>
            </Flex>
          </Box>
        </SettingsFormSection>
      </Box>
    );
  }

  return (
    <Box
      className={`anonymous-page ${className || ''}`.trim()}
      data-testid="anonymous-page"
      data-mode="edit"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      <Box mb="4">
        <PageHeader
          title="Anonymous Access"
          description="Configure anonymous user access settings"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Anonymous Access' }
          ]}
        />
      </Box>

      {/* Alerts */}
      {anonForm.loadError && (
        <Box className="anonymous-page__alerts">
          <SettingsAlert type="error">
            {anonForm.loadError}
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <SettingsForm
        onSubmit={anonForm.handleSubmit}
        onCancel={anonForm.handleDiscard}
        loading={anonForm.isSaving}
        pristine={anonForm.isPristine}
        testId="anonymous-form"
        submitAnalyticsId="nxrm-anonymous-save"
        data-loading={anonForm.isSaving ? 'true' : 'false'}
        data-dirty={!anonForm.isPristine ? 'true' : 'false'}
        data-submitting={anonForm.isSaving ? 'true' : 'false'}
        data-valid={!anonForm.hasValidationErrors ? 'true' : 'false'}
      >
        <SettingsFormSection title="Anonymous Access Configuration" defaultOpen>
          <Text size="2" className="anonymous-page__section-description">
            When enabled, anonymous users can access content without authentication based on the
            permissions granted to the anonymous user account.
          </Text>

          <SettingsCheckbox
            {...anonForm.checkbox('enabled')}
            label="Allow anonymous users to access the server"
            description="Enable anonymous access to repository content"
            analyticsId="nxrm-anonymous-toggle"
          />

          <SettingsTextInput
            {...anonForm.field('userId')}
            label="Username"
            helpText="The username that will be used for anonymous access"
            required
            placeholder="anonymous"
          />

          <SettingsSelect
            name="realmName"
            label="Realm"
            value={anonForm.formData.realmName}
            onChange={(value: string) => anonForm.handleChange('realmName', value)}
            options={realmOptions}
            helpText="The security realm to use for anonymous authentication"
            required
          />
        </SettingsFormSection>

        {/* Help Section */}
        <HelpSection
          title="About Anonymous Access"
          content="Anonymous access allows users to browse and access repository content without authenticating. The permissions granted to the anonymous user determine what content is accessible."
          docLink={{
            label: 'View Documentation',
            href: 'https://help.sonatype.com/en/anonymous-access.html',
          }}
        />
      </SettingsForm>

      {/* Confirmation Modal for Disabling Anonymous Access */}
      <Dialog.Root open={anonForm.showDisableConfirm} onOpenChange={() => anonForm.handleCancelDisable()}>
        <Dialog.Portal>
          <Dialog.Overlay className="anonymous-page__modal-overlay" />
          <Dialog.Content className="anonymous-page__modal" data-testid="anonymous-disable-modal">
            <Dialog.Title className="anonymous-page__modal-title">
              <Flex align="center" gap="2">
                <AlertTriangle size={20} className="anonymous-page__modal-icon" />
                Disable Anonymous Access
              </Flex>
            </Dialog.Title>
            <Dialog.Description className="anonymous-page__modal-description">
              Disabling anonymous access will require authentication for all requests.
              Users will need to log in to access the repository.
              Are you sure you want to continue?
            </Dialog.Description>
            <Flex gap="3" justify="end" className="anonymous-page__modal-actions">
              <SettingsButton
                variant="secondary"
                onClick={anonForm.handleCancelDisable}
                testId="anonymous-disable-cancel"
              >
                Cancel
              </SettingsButton>
              <SettingsButton
                variant="danger"
                onClick={anonForm.handleConfirmDisable}
                testId="anonymous-disable-confirm"
              >
                Disable Anonymous Access
              </SettingsButton>
            </Flex>
            <Dialog.Close asChild>
              <button
                className="anonymous-page__modal-close"
                aria-label="Close"
                onClick={anonForm.handleCancelDisable}
              >
                <X size={16} />
              </button>
            </Dialog.Close>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </Box>
  );
}

export default AnonymousPage;
