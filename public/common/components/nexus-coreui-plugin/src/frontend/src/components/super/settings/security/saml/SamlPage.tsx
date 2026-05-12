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


import React, { useState, useEffect, useCallback } from 'react';
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
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsCheckbox,
  SettingsButton,
  SettingsAlert,
} from '../../../shared/form';
import { ConfirmDialog } from '../shared/ConfirmDialog';
import { useToast, PageHeader } from '../../../../shared';
import { useUnsavedChangesWarning } from '../../../../shared';
import { SamlConfiguration, SamlPageProps } from './types';
import { useSamlApi } from './useSamlApi';
import './SamlPage.scss';

const DEFAULT_CONFIG: SamlConfiguration = {
  entityId: '',
  idpMetadata: '',
  usernameAttribute: '',
  firstNameAttribute: '',
  lastNameAttribute: '',
  emailAttribute: '',
  groupsAttribute: '',
  validateResponseSignature: true,
  validateAssertionSignature: true,
};

export function SamlPage({ className }: SamlPageProps) {
  const {
    loading,
    error,
    setError,
    fetchConfiguration,
    saveConfiguration,
    deleteConfiguration,
    getMetadataUrl,
  } = useSamlApi();

  const [config, setConfig] = useState<SamlConfiguration>(DEFAULT_CONFIG);
  const [isConfigured, setIsConfigured] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [hasChanges, setHasChanges] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Toast notifications (app-level provider)
  const toast = useToast();

  // Permission checks
  const canUpdate = ExtJS.checkPermission('nexus:saml:update');

  // Load initial configuration
  useEffect(() => {
    loadConfiguration();
  }, []);

  const loadConfiguration = async () => {
    setIsLoading(true);
    try {
      const loadedConfig = await fetchConfiguration();
      if (loadedConfig) {
        setConfig(loadedConfig);
        setIsConfigured(true);
      } else {
        setConfig(DEFAULT_CONFIG);
        setIsConfigured(false);
      }
    } catch {
      // Error already set by hook
    } finally {
      setIsLoading(false);
      setHasChanges(false);
    }
  };

  const handleFieldChange = useCallback((field: keyof SamlConfiguration, value: string | boolean) => {
    setConfig((prev) => ({ ...prev, [field]: value }));
    setHasChanges(true);

    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors((prev) => {
        const updated = { ...prev };
        delete updated[field];
        return updated;
      });
    }
  }, [validationErrors]);

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!config.idpMetadata || config.idpMetadata.trim() === '') {
      errors.idpMetadata = 'Identity Provider Metadata is required';
    }

    if (!config.usernameAttribute || config.usernameAttribute.trim() === '') {
      errors.usernameAttribute = 'Username Attribute is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      await saveConfiguration(config);
      setIsConfigured(true);
      setHasChanges(false);
      toast.success('SAML configuration saved successfully');
    } catch (err) {
      throw err;
    }
  };

  const handleDelete = async () => {
    try {
      await deleteConfiguration();
      setConfig(DEFAULT_CONFIG);
      setIsConfigured(false);
      setHasChanges(false);
      setShowDeleteConfirm(false);
      toast.success('SAML configuration deleted successfully');
    } catch {
      // Error already set by hook
    }
  };

  const handleCancel = () => {
    loadConfiguration();
    setShowDeleteConfirm(false);
  };

  const copyMetadataUrl = () => {
    const url = `${window.location.origin}${getMetadataUrl()}`;
    navigator.clipboard.writeText(url);
    toast.success('Metadata URL copied to clipboard');
  };

  if (isLoading) {
    return (
      <Box 
        className={`saml-page ${className || ''}`} 
        p="5"
        data-testid="saml-page"
        data-loading="true"
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
      data-loading={loading ? 'true' : 'false'}
      data-configured={isConfigured ? 'true' : 'false'}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {/* Header */}
      <Box mb="4">
        <PageHeader
          title="SAML"
          description="Configure SAML authentication with your Identity Provider"
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
        <SettingsAlert type="error" className="mb-4" onClose={() => setError(null)}>
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
        loading={loading}
        onSave={canUpdate ? handleSave : undefined}
        onCancel={canUpdate ? handleCancel : undefined}
        dirty={hasChanges}
        submitLabel="Save"
        data-testid="saml-form"
        data-dirty={hasChanges ? 'true' : 'false'}
        data-valid={Object.keys(validationErrors).length === 0 ? 'true' : 'false'}
        footerExtra={
          isConfigured && canUpdate ? (
            <SettingsButton
              variant="danger"
              onClick={() => setShowDeleteConfirm(true)}
              disabled={loading}
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
                {`${window.location.origin}${getMetadataUrl()}`}
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
                onClick={() => window.open(getMetadataUrl(), '_blank')}
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
          <SettingsTextInput
            name="entityId"
            label="Entity ID"
            value={config.entityId || ''}
            onChange={(value) => handleFieldChange('entityId', value)}
            placeholder="https://your-idp.example.com"
            helpText="SAML Service Provider's unique identifying URI (optional - will be auto-generated if not specified)"
            disabled={!canUpdate}
            data-testid="saml-input-entityId"
          />

          <SettingsTextArea
            name="idpMetadata"
            label="Identity Provider Metadata XML"
            value={config.idpMetadata || ''}
            onChange={(value) => handleFieldChange('idpMetadata', value)}
            placeholder="Paste your Identity Provider metadata XML here..."
            rows={10}
            required
            error={validationErrors.idpMetadata}
            helpText="The SAML metadata XML provided by your Identity Provider"
            disabled={!canUpdate}
            data-testid="saml-input-idpMetadata"
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
            error={validationErrors.usernameAttribute}
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
            label="Groups Attribute"
            value={config.groupsAttribute || ''}
            onChange={(value) => handleFieldChange('groupsAttribute', value)}
            placeholder="e.g., groups, memberOf"
            helpText="SAML attribute name containing the user's group memberships for role mapping (optional)"
            disabled={!canUpdate}
            data-testid="saml-input-groupsAttribute"
          />
        </SettingsFormSection>

        {/* Signature Validation */}
        <SettingsFormSection
          title="Signature Validation"
          description="Configure signature validation settings"
        >
          <SettingsCheckbox
            name="validateResponseSignature"
            label="Validate Response Signature"
            checked={config.validateResponseSignature ?? true}
            onChange={(checked) => handleFieldChange('validateResponseSignature', checked)}
            helpText="Require a valid signature on SAML responses from the Identity Provider"
            disabled={!canUpdate}
            data-testid="saml-checkbox-validateResponseSignature"
          />

          <SettingsCheckbox
            name="validateAssertionSignature"
            label="Validate Assertion Signature"
            checked={config.validateAssertionSignature ?? true}
            onChange={(checked) => handleFieldChange('validateAssertionSignature', checked)}
            helpText="Require a valid signature on SAML assertions from the Identity Provider"
            disabled={!canUpdate}
            data-testid="saml-checkbox-validateAssertionSignature"
          />
        </SettingsFormSection>

      </SettingsForm>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={showDeleteConfirm}
        testId="delete-saml-config-dialog"
        onOpenChange={setShowDeleteConfirm}
        title="Delete SAML Configuration"
        message="Are you sure you want to delete the SAML configuration? This will disable SAML authentication and users will no longer be able to log in via SAML."
        confirmLabel="Delete Configuration"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDelete}
      />
    </Box>
  );
}

export default SamlPage;
