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

import { assign } from 'xstate';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';
import { OAuth2Config, DEFAULT_OAUTH2_CONFIG } from './types';
import { fetchOAuth2Config, saveOAuth2Config } from './oauth2Api';

const URL_PATTERN = /^https?:\/\/.+/;

/**
 * Validate OAuth2 form data. Mirrors the legacy synchronous validation so the
 * same error messages are produced.
 */
export function validateOAuth2(data: OAuth2Config): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.clientId?.trim()) errors.clientId = 'Client ID is required';
  if (!data.clientSecret?.trim()) errors.clientSecret = 'Client Secret is required';
  if (!data.idpAuthorizationUrl?.trim()) errors.idpAuthorizationUrl = 'Authorization URL is required';
  if (!data.idpLogoutUrl?.trim()) errors.idpLogoutUrl = 'Logout URL is required';
  if (!data.idpTokenUrl?.trim()) errors.idpTokenUrl = 'Token URL is required';
  if (!data.idpJwksUrl?.trim()) errors.idpJwksUrl = 'JWKS URL is required';
  if (!data.usernameClaim?.trim()) errors.usernameClaim = 'Username claim is required';
  if (!data.firstNameClaim?.trim()) errors.firstNameClaim = 'First name claim is required';
  if (!data.lastNameClaim?.trim()) errors.lastNameClaim = 'Last name claim is required';
  if (!data.emailClaim?.trim()) errors.emailClaim = 'Email claim is required';
  if (!data.groupsClaim?.trim()) errors.groupsClaim = 'Groups claim is required';
  if (!data.idpJwsAlgorithm?.trim()) errors.idpJwsAlgorithm = 'JWS Algorithm is required';

  if (data.idpAuthorizationUrl && !URL_PATTERN.test(data.idpAuthorizationUrl)) {
    errors.idpAuthorizationUrl = 'Must be a valid URL';
  }
  if (data.idpLogoutUrl && !URL_PATTERN.test(data.idpLogoutUrl)) {
    errors.idpLogoutUrl = 'Must be a valid URL';
  }
  if (data.idpTokenUrl && !URL_PATTERN.test(data.idpTokenUrl)) {
    errors.idpTokenUrl = 'Must be a valid URL';
  }
  if (data.idpJwksUrl && !URL_PATTERN.test(data.idpJwksUrl)) {
    errors.idpJwksUrl = 'Must be a valid URL';
  }

  return errors;
}

/**
 * OAuth2 settings form machine.
 *
 * Standard settings form: loads the current config, validates on the client,
 * and saves. `stayEditableAfterSave` keeps the form editable after a successful
 * save (no post-save navigation), matching the legacy page.
 *
 * Load/save are `invoke`d services (automatic cancellation on state exit).
 */
export function createOAuth2FormMachine() {
  return createFormMachine<OAuth2Config>({
    id: 'oauth2-form',
    context: {
      data: { ...DEFAULT_OAUTH2_CONFIG },
    } as FormContext<OAuth2Config>,
    stayEditableAfterSave: true,
    actions: {
      validate: assign((ctx: FormContext<OAuth2Config>) => ({
        validationErrors: validateOAuth2(ctx.data),
      })),
    },
    services: {
      load: async () => ({ data: await fetchOAuth2Config() }),
      save: async (ctx) => saveOAuth2Config(ctx.data),
    },
  });
}
