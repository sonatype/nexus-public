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
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { PageHeader } from '../../../../shared';
import {
  SettingsForm,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsTextArea,
  SettingsFormSection,
} from '../../../../shared/form';
import { useOAuth2Form } from './useOAuth2Form';
import { OAuth2PageProps } from './types';

import './OAuth2Page.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

export function OAuth2Page({ className }: OAuth2PageProps) {
  const { isLoading, isSaving, isPristine, loadError, saveError, field, submit, reset } = useOAuth2Form();

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');

  if (isLoading) {
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
    <>
      <PageHeader
        title="OAuth2"
        description="Configure OpenID Connect (OIDC) authentication settings"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'OAuth2' }
        ]}
      />
      <SettingsForm
        onSave={submit}
        onCancel={reset}
        dirty={!isPristine}
        saving={isSaving}
        error={saveError || loadError || undefined}
        showActions={canUpdate}
        externalDirtyTracking
        testId="oauth2-settings-form"
        className={className || ''}
      >
        <SettingsFormSection title="OIDC Settings">
          <SettingsPasswordInput
            {...field('clientId')}
            label="Client ID"
            helpText="The client ID registered with your identity provider"
            required
            disabled={!canUpdate}
          />
          <SettingsPasswordInput
            {...field('clientSecret')}
            label="Client Secret"
            helpText="The client secret for authentication"
            required
            disabled={!canUpdate}
            autoComplete="new-password"
          />
          <SettingsTextInput
            {...field('idpAuthorizationUrl')}
            label="IDP Authorization URL"
            helpText="The authorization endpoint of your identity provider"
            required
            disabled={!canUpdate}
            type="url"
          />
          <SettingsTextInput
            {...field('idpLogoutUrl')}
            label="IDP Logout URL"
            helpText="The logout endpoint of your identity provider"
            required
            disabled={!canUpdate}
            type="url"
          />
          <SettingsTextInput
            {...field('idpTokenUrl')}
            label="IDP Token URL"
            helpText="The token endpoint of your identity provider"
            required
            disabled={!canUpdate}
            type="url"
          />
          <SettingsTextInput
            {...field('idpJwksUrl')}
            label="IDP JWKS URL"
            helpText="The JWKS endpoint for token verification"
            required
            disabled={!canUpdate}
            type="url"
          />
        </SettingsFormSection>

        <SettingsFormSection title="Claim Mappings">
          <SettingsTextInput
            {...field('usernameClaim')}
            label="Username Claim"
            helpText="The claim that contains the username"
            required
            disabled={!canUpdate}
          />
          <SettingsTextInput
            {...field('firstNameClaim')}
            label="First Name Claim"
            helpText="The claim that contains the first name"
            required
            disabled={!canUpdate}
          />
          <SettingsTextInput
            {...field('lastNameClaim')}
            label="Last Name Claim"
            helpText="The claim that contains the last name"
            required
            disabled={!canUpdate}
          />
          <SettingsTextInput
            {...field('emailClaim')}
            label="Email Claim"
            helpText="The claim that contains the email address"
            required
            disabled={!canUpdate}
          />
          <SettingsTextInput
            {...field('groupsClaim')}
            label="Groups Claim"
            helpText="The claim that contains the group memberships"
            required
            disabled={!canUpdate}
          />
        </SettingsFormSection>

        <SettingsFormSection title="JWT Settings">
          <SettingsTextInput
            {...field('idpJwsAlgorithm')}
            label="JWS Algorithm"
            helpText="The signing algorithm used by the IDP (e.g., RS256)"
            required
            disabled={!canUpdate}
          />
          <SettingsTextArea
            {...field('idpJwks')}
            label="JWKS (Optional)"
            helpText="Optional JSON Web Key Set for offline verification"
            disabled={!canUpdate}
            rows={4}
          />
        </SettingsFormSection>

        <SettingsFormSection title="Advanced Settings">
          <SettingsTextArea
            {...field('authorizationCustomParams')}
            label="Authorization Custom Parameters"
            helpText="Custom parameters for authorization requests (JSON format)"
            disabled={!canUpdate}
            rows={3}
          />
          <SettingsTextArea
            {...field('tokenRequestCustomParams')}
            label="Token Request Custom Parameters"
            helpText="Custom parameters for token requests (JSON format)"
            disabled={!canUpdate}
            rows={3}
          />
          <SettingsTextArea
            {...field('exactMatchClaims')}
            label="Exact Match Claims"
            helpText="Claims that must match exactly (JSON format)"
            disabled={!canUpdate}
            rows={3}
          />
        </SettingsFormSection>
      </SettingsForm>
    </>
  );
}

export default OAuth2Page;
