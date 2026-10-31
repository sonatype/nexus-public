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
import { Box, Text, Flex, Button, Callout, Spinner, Badge } from '@radix-ui/themes';
import {
  ExternalLink,
  Copy,
  Trash2,
  AlertCircle,
  CheckCircle2,
  Info,
  Circle,
} from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';
import { ConfirmDialog } from '../shared/ConfirmDialog';
import { PageHeader } from '../../../../shared';
import { SamlPageProps } from './types';
import { useSamlForm } from './useSamlForm';
import './SamlPage.scss';

// Shared options for tri-state signature validation selects
const SIGNATURE_VALIDATION_OPTIONS = [
  { value: 'default', label: 'Default' },
  { value: 'true', label: 'True' },
  { value: 'false', label: 'False' },
];

export function SamlPage({ className }: SamlPageProps) {
  const {
    config,
    isConfigured,
    isInitialLoading,
    isBusy,
    isPristine,
    isConfirmingDelete,
    error,
    deleteError,
    fieldError,
    handleFieldChange,
    handleSave,
    handleDiscard,
    requestDelete,
    confirmDelete,
    cancelDelete,
    clearError,
    getAbsoluteMetadataUrl,
    copyMetadataUrl,
  } = useSamlForm();

  // Permission checks
  const canUpdate = ExtJS.checkPermission('nexus:saml:update');

  if (isInitialLoading) {
    return (
      <Box
        className={`saml-page ${className || ''}`}
        p="5"
        data-testid="saml-page"
        data-loading="true"
        aria-busy="true"
        aria-live="polite"
      >
        <Flex align="center" justify="center" py="9" gap="3">
          <Spinner size="3" />
          <Text size="3" color="gray">Loading SAML configuration...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box
      className={`saml-page ${className || ''}`}
      data-testid="saml-page"
      data-loading={isBusy ? 'true' : 'false'}
      data-configured={isConfigured ? 'true' : 'false'}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {/* Header */}
      <Box mb="4">
        <PageHeader
          title="SAML"
          description="Configure SAML authentication with your Identity Provider"
          breadcrumbs={[
            { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
            { label: 'SAML' }
          ]}
          actions={
            isConfigured ? (
              <Badge color="green" variant="soft" data-testid="saml-badge-configured">
                <CheckCircle2 size={12} />
                Configured
              </Badge>
            ) : (
              <Badge color="gray" variant="soft" data-testid="saml-badge-not-configured">
                <Circle size={12} />
                Not Configured
              </Badge>
            )
          }
        />
      </Box>

      {/* Alerts */}
      {error && (
        <SettingsAlert type="error" className="mb-4" onClose={clearError}>
          {error}
        </SettingsAlert>
      )}


      {/* Info callout for unconfigured state */}
      {!isConfigured && canUpdate && (
        <Callout.Root color="blue" variant="soft" mb="4">
          <Callout.Icon>
            <Info size={16} />
          </Callout.Icon>
          <Callout.Text>
            SAML is not yet configured. Fill in the form below to enable SAML authentication.
            You'll need the metadata XML from your Identity Provider.
          </Callout.Text>
        </Callout.Root>
      )}

      {/* Read-only notice for users without update permission */}
      {!canUpdate && (
        <Callout.Root color="amber" variant="soft" mb="4">
          <Callout.Icon>
            <AlertCircle size={16} />
          </Callout.Icon>
          <Callout.Text>
            You do not have permission to modify SAML settings. Contact an administrator to make changes.
          </Callout.Text>
        </Callout.Root>
      )}

      <SettingsForm
        title=""
        description={undefined}
        loading={isBusy}
        onSave={canUpdate ? handleSave : undefined}
        onSubmit={undefined}
        onCancel={canUpdate ? handleDiscard : undefined}
        dirty={!isPristine}
        pristine={isPristine}
        externalDirtyTracking
        submitLabel="Save"
        submitAnalyticsId="nxrm-saml-save"
        cancelAnalyticsId={undefined}
        data-testid="saml-form"
        data-dirty={!isPristine ? 'true' : 'false'}
        data-valid={!fieldError('idpMetadata') && !fieldError('entityId') && !fieldError('usernameAttribute') ? 'true' : 'false'}
        footerExtra={
          isConfigured && canUpdate ? (
            <SettingsButton
              variant="danger"
              onClick={requestDelete}
              disabled={isBusy}
              icon={Trash2}
              testId="saml-delete-button"
            >
              Delete Configuration
            </SettingsButton>
          ) : undefined
        }
      >
        {/* Service Provider Metadata Section */}
        {isConfigured && (
          <SettingsFormSection
            title="Service Provider Metadata"
            description="Share this metadata URL with your Identity Provider"
          >
            <Flex align="center" gap="3" className="metadata-url-container">
              <Text size="2" className="metadata-url" data-testid="saml-metadata-url">
                {getAbsoluteMetadataUrl()}
              </Text>
              <Button
                variant="soft"
                size="1"
                onClick={copyMetadataUrl}
                data-testid="saml-metadata-copy"
              >
                <Copy size={14} />
                Copy
              </Button>
              <Button
                variant="soft"
                size="1"
                onClick={() => window.open(getAbsoluteMetadataUrl(), '_blank')}
                data-testid="saml-metadata-open"
              >
                <ExternalLink size={14} />
                Open
              </Button>
            </Flex>
          </SettingsFormSection>
        )}

        {/* Identity Provider Configuration */}
        <SettingsFormSection
          title="Identity Provider Configuration"
          description="Configure your SAML Identity Provider settings"
        >
          <SettingsTextArea
            name="idpMetadata"
            label="Identity Provider Metadata XML"
            value={config.idpMetadata || ''}
            onChange={(value) => handleFieldChange('idpMetadata', value)}
            placeholder="Paste your Identity Provider metadata XML here..."
            rows={10}
            required
            error={fieldError('idpMetadata')}
            helpText="The SAML metadata XML provided by your Identity Provider"
            disabled={!canUpdate}
            className="saml-idp-metadata-textarea"
            data-testid="saml-input-idpMetadata"
          />

          <SettingsTextInput
            name="entityId"
            label="Entity ID"
            value={config.entityId || ''}
            onChange={(value) => handleFieldChange('entityId', value)}
            placeholder="https://your-idp.example.com"
            error={fieldError('entityId')}
            helpText="SAML Service Provider's unique identifying URI (optional - will be auto-generated if not specified)"
            disabled={!canUpdate}
            data-testid="saml-input-entityId"
          />
        </SettingsFormSection>

        {/* Signature Validation */}
        <SettingsFormSection
          title="Signature Validation"
          description="Configure signature validation settings"
        >
          <SettingsSelect
            name="validateResponseSignature"
            label="Validate Response Signature"
            value={config.validateResponseSignature === null || config.validateResponseSignature === undefined ? 'default' : String(config.validateResponseSignature)}
            onChange={(value) => handleFieldChange('validateResponseSignature', value === 'default' ? null : value === 'true')}
            options={SIGNATURE_VALIDATION_OPTIONS}
            helpText="Require a valid signature on SAML responses. 'Default' uses the IdP's signing key presence."
            disabled={!canUpdate}
            data-testid="saml-select-validateResponseSignature"
          />

          <SettingsSelect
            name="validateAssertionSignature"
            label="Validate Assertion Signature"
            value={config.validateAssertionSignature === null || config.validateAssertionSignature === undefined ? 'default' : String(config.validateAssertionSignature)}
            onChange={(value) => handleFieldChange('validateAssertionSignature', value === 'default' ? null : value === 'true')}
            options={SIGNATURE_VALIDATION_OPTIONS}
            helpText="Require a valid signature on SAML assertions. 'Default' uses the IdP's signing key presence."
            disabled={!canUpdate}
            data-testid="saml-select-validateAssertionSignature"
          />
        </SettingsFormSection>

        {/* Attribute Mapping */}
        <SettingsFormSection
          title="Attribute Mapping"
          description="Map SAML attributes to user properties"
        >
          <SettingsTextInput
            name="usernameAttribute"
            label="Username Attribute"
            value={config.usernameAttribute || ''}
            onChange={(value) => handleFieldChange('usernameAttribute', value)}
            placeholder="e.g., NameID, uid, email"
            required
            error={fieldError('usernameAttribute')}
            helpText="SAML attribute name containing the username"
            disabled={!canUpdate}
            data-testid="saml-input-usernameAttribute"
          />

          <SettingsTextInput
            name="firstNameAttribute"
            label="First Name Attribute"
            value={config.firstNameAttribute || ''}
            onChange={(value) => handleFieldChange('firstNameAttribute', value)}
            placeholder="e.g., firstName, givenName"
            helpText="SAML attribute name containing the user's first name (optional)"
            disabled={!canUpdate}
            data-testid="saml-input-firstNameAttribute"
          />

          <SettingsTextInput
            name="lastNameAttribute"
            label="Last Name Attribute"
            value={config.lastNameAttribute || ''}
            onChange={(value) => handleFieldChange('lastNameAttribute', value)}
            placeholder="e.g., lastName, sn, surname"
            helpText="SAML attribute name containing the user's last name (optional)"
            disabled={!canUpdate}
            data-testid="saml-input-lastNameAttribute"
          />

          <SettingsTextInput
            name="emailAttribute"
            label="Email Attribute"
            value={config.emailAttribute || ''}
            onChange={(value) => handleFieldChange('emailAttribute', value)}
            placeholder="e.g., email, mail"
            helpText="SAML attribute name containing the user's email address (optional)"
            disabled={!canUpdate}
            data-testid="saml-input-emailAttribute"
          />

          <SettingsTextInput
            name="groupsAttribute"
            label="Roles/Groups"
            value={config.groupsAttribute || ''}
            onChange={(value) => handleFieldChange('groupsAttribute', value)}
            placeholder="e.g., groups, memberOf"
            helpText="Map Identity Provider roles or groups to Nexus Repository Manager roles"
            disabled={!canUpdate}
            data-testid="saml-input-groupsAttribute"
          />
        </SettingsFormSection>


      </SettingsForm>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={isConfirmingDelete}
        testId="delete-saml-config-dialog"
        onOpenChange={(open) => { if (!open) cancelDelete(); }}
        title="Delete SAML Configuration"
        message="Are you sure you want to delete the SAML configuration? This will disable SAML authentication and users will no longer be able to log in via SAML."
        confirmLabel="Delete Configuration"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={confirmDelete}
      >
        {/* A failed delete keeps the dialog open with the error shown here. */}
        {deleteError && (
          <Box mt="3">
            <SettingsAlert type="error">{deleteError}</SettingsAlert>
          </Box>
        )}
      </ConfirmDialog>
    </Box>
  );
}

export default SamlPage;
