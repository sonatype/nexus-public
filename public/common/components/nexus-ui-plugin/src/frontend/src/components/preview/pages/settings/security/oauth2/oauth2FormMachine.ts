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
import ValidationUtils from '../../../../../../interface/ValidationUtils';
import UIStrings from '../../../../../../constants/UIStrings';
import { OAuth2Config, DEFAULT_OAUTH2_CONFIG } from './types';
import { fetchOAuth2Config, saveOAuth2Config } from './oauth2Api';

/** Fields that are required but carry no format constraint. */
const REQUIRED_FIELDS = [
  'clientId',
  'clientSecret',
  'usernameClaim',
  'firstNameClaim',
  'lastNameClaim',
  'emailClaim',
  'groupsClaim',
  'idpJwsAlgorithm',
] as const;

/** Fields that are required AND must parse as a URL. */
const REQUIRED_URL_FIELDS = ['idpAuthorizationUrl', 'idpLogoutUrl', 'idpTokenUrl', 'idpJwksUrl'] as const;

/**
 * Optional fields sent to the API as JSON objects. Blank is allowed (becomes
 * `{}`), but non-blank input must parse to a JSON object.
 */
const JSON_OBJECT_FIELDS = ['exactMatchClaims', 'authorizationCustomParams', 'tokenRequestCustomParams'] as const;

/**
 * True when `value` parses as a flat JSON object whose values are all strings.
 *
 * The API models these fields as `Map<String, String>`
 * (`OAuth2OidcConfigurationXO`), so arrays and nested objects are rejected — the
 * backend answers 400 for those, which surfaces only as a generic save error.
 * Non-string scalars are rejected too: Jackson silently coerces them (`5` is
 * stored as `"5"`), so rejecting keeps what is saved equal to what was typed.
 */
function isStringMap(value: string): boolean {
  let parsed: unknown;
  try {
    parsed = JSON.parse(value);
  } catch {
    return false;
  }

  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    return false;
  }
  return Object.values(parsed).every((entry) => typeof entry === 'string');
}

/**
 * Validate OAuth2 form data.
 *
 * Delegates to the shared `ValidationUtils` so both the rules and the message
 * text stay identical to the Classic page (NEXUS-54266) — previously this
 * reimplemented blank/URL checks with a local regex and bespoke per-field
 * strings, which drifted from Classic's shared `UIStrings.ERROR` messages.
 */
export function validateOAuth2(data: OAuth2Config): ValidationErrors {
  const errors: ValidationErrors = {};

  for (const name of REQUIRED_FIELDS) {
    const error = ValidationUtils.validateNotBlank(data[name]);
    if (error) errors[name] = error;
  }

  for (const name of REQUIRED_URL_FIELDS) {
    // Blank takes precedence over format, matching Classic's ternary ordering.
    const error = ValidationUtils.validateNotBlank(data[name]) ?? ValidationUtils.validateIsUrl(data[name]);
    if (error) errors[name] = error;
  }

  // NEXUS-54266: reject malformed JSON rather than letting the API layer coerce
  // it to `{}`, which reported a successful save while destroying the previous
  // value. Classic aborts the save by throwing from JSON.parse; validating here
  // rejects earlier and points at the offending field.
  for (const name of JSON_OBJECT_FIELDS) {
    const value = data[name];
    if (value?.trim() && !isStringMap(value)) {
      errors[name] = UIStrings.ERROR.INVALID_JSON_OBJECT;
    }
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
