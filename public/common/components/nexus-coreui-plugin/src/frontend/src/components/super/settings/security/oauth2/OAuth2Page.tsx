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

import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import {
  SettingsForm,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsTextArea,
  SettingsFormSection,
} from '../../../shared/form';
import { useOAuth2Api } from './useOAuth2Api';
import { OAuth2Config, DEFAULT_OAUTH2_CONFIG, OAuth2PageProps } from './types';

import './OAuth2Page.scss';

export function OAuth2Page({ className }: OAuth2PageProps) {
  const { loading, error, setError, fetchConfig, saveConfig } = useOAuth2Api();
  const [config, setConfig] = useState<OAuth2Config>(DEFAULT_OAUTH2_CONFIG);
  const [pristineConfig, setPristineConfig] = useState<OAuth2Config>(DEFAULT_OAUTH2_CONFIG);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');

  useEffect(() => {
    const loadConfig = async () => {
      setLoadingInitial(true);
      try {
        const data = await fetchConfig();
        setConfig(data);
        setPristineConfig(data);
      } catch (err) {
        // Error handled by hook
      } finally {
        setLoadingInitial(false);
      }
    };

    loadConfig();
  }, [fetchConfig]);

  const isPristine = JSON.stringify(config) === JSON.stringify(pristineConfig);

  const handleChange = useCallback((field: keyof OAuth2Config, value: string) => {
    setConfig((prev) => ({ ...prev, [field]: value }));
    setValidationErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  const validateForm = useCallback((): boolean => {
    const errors: Record<string, string> = {};

    if (!config.clientId?.trim()) errors.clientId = 'Client ID is required';
    if (!config.clientSecret?.trim()) errors.clientSecret = 'Client Secret is required';
    if (!config.idpAuthorizationUrl?.trim()) errors.idpAuthorizationUrl = 'Authorization URL is required';
    if (!config.idpLogoutUrl?.trim()) errors.idpLogoutUrl = 'Logout URL is required';
    if (!config.idpTokenUrl?.trim()) errors.idpTokenUrl = 'Token URL is required';
    if (!config.idpJwksUrl?.trim()) errors.idpJwksUrl = 'JWKS URL is required';
    if (!config.usernameClaim?.trim()) errors.usernameClaim = 'Username claim is required';
    if (!config.firstNameClaim?.trim()) errors.firstNameClaim = 'First name claim is required';
    if (!config.lastNameClaim?.trim()) errors.lastNameClaim = 'Last name claim is required';
    if (!config.emailClaim?.trim()) errors.emailClaim = 'Email claim is required';
    if (!config.groupsClaim?.trim()) errors.groupsClaim = 'Groups claim is required';
    if (!config.idpJwsAlgorithm?.trim()) errors.idpJwsAlgorithm = 'JWS Algorithm is required';

    const urlPattern = /^https?:\/\/.+/;
    if (config.idpAuthorizationUrl && !urlPattern.test(config.idpAuthorizationUrl)) {
      errors.idpAuthorizationUrl = 'Must be a valid URL';
    }
    if (config.idpLogoutUrl && !urlPattern.test(config.idpLogoutUrl)) {
      errors.idpLogoutUrl = 'Must be a valid URL';
    }
    if (config.idpTokenUrl && !urlPattern.test(config.idpTokenUrl)) {
      errors.idpTokenUrl = 'Must be a valid URL';
    }
    if (config.idpJwksUrl && !urlPattern.test(config.idpJwksUrl)) {
      errors.idpJwksUrl = 'Must be a valid URL';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }, [config]);

  const handleSave = useCallback(async () => {
    if (!validateForm()) {
      throw new Error('Validation failed');
    }

    try {
      await saveConfig(config);
      setPristineConfig(config);
    } catch (err) {
      throw err;
    }
  }, [validateForm, saveConfig, config]);

  const handleDiscard = useCallback(() => {
    setConfig(pristineConfig);
    setValidationErrors({});
    setError(null);
  }, [pristineConfig, setError]);

  if (loadingInitial) {
    return (
      <Box className={`oauth2-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="oauth2-page__loading">
          <Loader2 size={24} className="oauth2-page__spinner" />
          <Text size="2">Loading OAuth2 configuration...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <SettingsForm
      title="OAuth2"
      description="Configure OpenID Connect (OIDC) authentication settings"
      onSave={handleSave}
      onCancel={handleDiscard}
      dirty={!isPristine}
      saving={loading}
      error={error || undefined}
      showActions={canUpdate}
      testId="oauth2-settings-form"
      className={className || ''}
    >
      <SettingsFormSection title="OIDC Settings">
        <SettingsPasswordInput
          name="clientId"
          label="Client ID"
          value={config.clientId}
          onChange={(val) => handleChange('clientId', val)}
          helpText="The client ID registered with your identity provider"
          error={validationErrors.clientId}
          required
          disabled={!canUpdate}
        />
        <SettingsPasswordInput
          name="clientSecret"
          label="Client Secret"
          value={config.clientSecret}
          onChange={(val) => handleChange('clientSecret', val)}
          helpText="The client secret for authentication"
          error={validationErrors.clientSecret}
          required
          disabled={!canUpdate}
          autoComplete="new-password"
        />
        <SettingsTextInput
          name="idpAuthorizationUrl"
          label="IDP Authorization URL"
          value={config.idpAuthorizationUrl}
          onChange={(val) => handleChange('idpAuthorizationUrl', val)}
          helpText="The authorization endpoint of your identity provider"
          error={validationErrors.idpAuthorizationUrl}
          required
          disabled={!canUpdate}
          type="url"
        />
        <SettingsTextInput
          name="idpLogoutUrl"
          label="IDP Logout URL"
          value={config.idpLogoutUrl}
          onChange={(val) => handleChange('idpLogoutUrl', val)}
          helpText="The logout endpoint of your identity provider"
          error={validationErrors.idpLogoutUrl}
          required
          disabled={!canUpdate}
          type="url"
        />
        <SettingsTextInput
          name="idpTokenUrl"
          label="IDP Token URL"
          value={config.idpTokenUrl}
          onChange={(val) => handleChange('idpTokenUrl', val)}
          helpText="The token endpoint of your identity provider"
          error={validationErrors.idpTokenUrl}
          required
          disabled={!canUpdate}
          type="url"
        />
        <SettingsTextInput
          name="idpJwksUrl"
          label="IDP JWKS URL"
          value={config.idpJwksUrl}
          onChange={(val) => handleChange('idpJwksUrl', val)}
          helpText="The JWKS endpoint for token verification"
          error={validationErrors.idpJwksUrl}
          required
          disabled={!canUpdate}
          type="url"
        />
      </SettingsFormSection>

      <SettingsFormSection title="Claim Mappings">
        <SettingsTextInput
          name="usernameClaim"
          label="Username Claim"
          value={config.usernameClaim}
          onChange={(val) => handleChange('usernameClaim', val)}
          helpText="The claim that contains the username"
          error={validationErrors.usernameClaim}
          required
          disabled={!canUpdate}
        />
        <SettingsTextInput
          name="firstNameClaim"
          label="First Name Claim"
          value={config.firstNameClaim}
          onChange={(val) => handleChange('firstNameClaim', val)}
          helpText="The claim that contains the first name"
          error={validationErrors.firstNameClaim}
          required
          disabled={!canUpdate}
        />
        <SettingsTextInput
          name="lastNameClaim"
          label="Last Name Claim"
          value={config.lastNameClaim}
          onChange={(val) => handleChange('lastNameClaim', val)}
          helpText="The claim that contains the last name"
          error={validationErrors.lastNameClaim}
          required
          disabled={!canUpdate}
        />
        <SettingsTextInput
          name="emailClaim"
          label="Email Claim"
          value={config.emailClaim}
          onChange={(val) => handleChange('emailClaim', val)}
          helpText="The claim that contains the email address"
          error={validationErrors.emailClaim}
          required
          disabled={!canUpdate}
        />
        <SettingsTextInput
          name="groupsClaim"
          label="Groups Claim"
          value={config.groupsClaim}
          onChange={(val) => handleChange('groupsClaim', val)}
          helpText="The claim that contains the group memberships"
          error={validationErrors.groupsClaim}
          required
          disabled={!canUpdate}
        />
      </SettingsFormSection>

      <SettingsFormSection title="JWT Settings">
        <SettingsTextInput
          name="idpJwsAlgorithm"
          label="JWS Algorithm"
          value={config.idpJwsAlgorithm}
          onChange={(val) => handleChange('idpJwsAlgorithm', val)}
          helpText="The signing algorithm used by the IDP (e.g., RS256)"
          error={validationErrors.idpJwsAlgorithm}
          required
          disabled={!canUpdate}
        />
        <SettingsTextArea
          name="idpJwks"
          label="JWKS (Optional)"
          value={config.idpJwks || ''}
          onChange={(val) => handleChange('idpJwks', val)}
          helpText="Optional JSON Web Key Set for offline verification"
          disabled={!canUpdate}
          rows={4}
        />
      </SettingsFormSection>

      <SettingsFormSection title="Advanced Settings">
        <SettingsTextArea
          name="authorizationCustomParams"
          label="Authorization Custom Parameters"
          value={config.authorizationCustomParams || ''}
          onChange={(val) => handleChange('authorizationCustomParams', val)}
          helpText="Custom parameters for authorization requests (JSON format)"
          disabled={!canUpdate}
          rows={3}
        />
        <SettingsTextArea
          name="tokenRequestCustomParams"
          label="Token Request Custom Parameters"
          value={config.tokenRequestCustomParams || ''}
          onChange={(val) => handleChange('tokenRequestCustomParams', val)}
          helpText="Custom parameters for token requests (JSON format)"
          disabled={!canUpdate}
          rows={3}
        />
        <SettingsTextArea
          name="exactMatchClaims"
          label="Exact Match Claims"
          value={config.exactMatchClaims || ''}
          onChange={(val) => handleChange('exactMatchClaims', val)}
          helpText="Claims that must match exactly (JSON format)"
          disabled={!canUpdate}
          rows={3}
        />
      </SettingsFormSection>
    </SettingsForm>
  );
}

export default OAuth2Page;
