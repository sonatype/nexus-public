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

import { restClient, parseApiError } from '../../../../../../interface/api';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { OAuth2Config, DEFAULT_OAUTH2_CONFIG } from './types';

const OAUTH2_API_URL = APIConstants.REST.INTERNAL.OAUTH2;

/**
 * Convert an object-valued field to a pretty-printed JSON string for display in
 * a textarea. Non-object values are coerced to their string form.
 */
function stringifyObject(val: unknown): string {
  if (val && typeof val === 'object') {
    try {
      return JSON.stringify(val, null, 2);
    } catch {
      return '';
    }
  }
  return (val as string) || '';
}

/**
 * Parse a JSON string field back to an object for the API payload. Blank input
 * becomes an empty object.
 *
 * Throws on malformed or non-object input rather than coercing to `{}`
 * (NEXUS-54266): the previous silent coercion reported a successful save while
 * discarding whatever the field held before. `validateOAuth2` rejects such input
 * first, so this is a backstop for any caller that bypasses validation — and it
 * matches Classic, which aborts the save by letting JSON.parse throw.
 */
function parseString(fieldLabel: string, val: string): Record<string, string> {
  if (!val?.trim()) {
    return {};
  }

  const invalid = () => new Error(`${fieldLabel} must be a valid JSON object with string values`);

  let parsed: unknown;
  try {
    parsed = JSON.parse(val);
  } catch {
    throw invalid();
  }

  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw invalid();
  }
  // The API declares these as Map<String, String>; anything else is either
  // rejected with a 400 (arrays, nested objects) or silently coerced (scalars).
  if (!Object.values(parsed).every((entry) => typeof entry === 'string')) {
    throw invalid();
  }
  return parsed as Record<string, string>;
}

/**
 * Fetch the OAuth2 configuration and normalize object-valued fields into JSON
 * strings suitable for form textareas.
 *
 * Pure (no React) so it can be invoked from the form machine's `load` service.
 */
export async function fetchOAuth2Config(): Promise<OAuth2Config> {
  try {
    const data = await restClient.get<OAuth2Config>(OAUTH2_API_URL);

    return {
      ...DEFAULT_OAUTH2_CONFIG,
      ...data,
      // Coerce explicitly: the backend XO sends a primitive boolean, but a
      // missing/null value must land on `false` rather than `undefined` so the
      // checkbox stays controlled and pristine-tracking works.
      useTrustStore: data.useTrustStore ?? false,
      exactMatchClaims: stringifyObject(data.exactMatchClaims),
      authorizationCustomParams: stringifyObject(data.authorizationCustomParams),
      tokenRequestCustomParams: stringifyObject(data.tokenRequestCustomParams),
    };
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

/**
 * Save the OAuth2 configuration. Trims string fields and parses the JSON string
 * fields back into objects before sending the PUT request.
 *
 * Pure (no React) so it can be invoked from the form machine's `save` service.
 */
export async function saveOAuth2Config(config: OAuth2Config): Promise<void> {
  try {
    const payload = {
      ...config,
      clientId: config.clientId?.trim(),
      clientSecret: config.clientSecret,
      idpAuthorizationUrl: config.idpAuthorizationUrl?.trim(),
      idpLogoutUrl: config.idpLogoutUrl?.trim(),
      idpTokenUrl: config.idpTokenUrl?.trim(),
      idpJwksUrl: config.idpJwksUrl?.trim(),
      idpJwsAlgorithm: config.idpJwsAlgorithm?.trim(),
      usernameClaim: config.usernameClaim?.trim(),
      firstNameClaim: config.firstNameClaim?.trim(),
      lastNameClaim: config.lastNameClaim?.trim(),
      emailClaim: config.emailClaim?.trim(),
      groupsClaim: config.groupsClaim?.trim(),
      // Sent explicitly: the backend XO field is a primitive boolean defaulting
      // to false, so omitting it here would reset the admin's truststore choice
      // on every save (NEXUS-54266).
      useTrustStore: config.useTrustStore ?? false,
      exactMatchClaims: parseString('Exact Match Claims', config.exactMatchClaims || ''),
      authorizationCustomParams: parseString(
        'Authorization Custom Parameters',
        config.authorizationCustomParams || ''),
      tokenRequestCustomParams: parseString(
        'Token Request Custom Parameters',
        config.tokenRequestCustomParams || ''),
    };

    await restClient.put(OAUTH2_API_URL, payload);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}
