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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';

import { createOAuth2FormMachine, validateOAuth2 } from '../oauth2FormMachine';
import { OAuth2Config, DEFAULT_OAUTH2_CONFIG } from '../types';
import UIStrings from '../../../../../../../constants/UIStrings';

// Validation delegates to the shared ValidationUtils, so assert against the
// shared message constants rather than literals (NEXUS-54266).
const { FIELD_REQUIRED, URL_ERROR, INVALID_JSON_OBJECT } = UIStrings.ERROR;

const VALID_CONFIG: OAuth2Config = {
  clientId: 'client-id',
  clientSecret: 'client-secret',
  idpAuthorizationUrl: 'https://example.com/auth',
  idpLogoutUrl: 'https://example.com/logout',
  idpTokenUrl: 'https://example.com/token',
  idpJwksUrl: 'https://example.com/jwks',
  idpJwsAlgorithm: 'RS256',
  idpJwks: '',
  usernameClaim: 'sub',
  firstNameClaim: 'given_name',
  lastNameClaim: 'family_name',
  emailClaim: 'email',
  groupsClaim: 'groups',
  exactMatchClaims: '',
  authorizationCustomParams: '',
  tokenRequestCustomParams: '',
  useTrustStore: false,
};

const withServices = (over: Record<string, unknown> = {}) =>
  createOAuth2FormMachine().withConfig({
    services: {
      load: async () => ({ data: VALID_CONFIG }),
      save: async () => ({}),
      ...over,
    },
  });

const startLoaded = async (over: Record<string, unknown> = {}) => {
  const service = interpret(withServices(over)).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
};

describe('validateOAuth2', () => {
  it('reports required fields on empty data', () => {
    const errors = validateOAuth2(DEFAULT_OAUTH2_CONFIG);
    expect(errors.clientId).toBe(FIELD_REQUIRED);
    expect(errors.clientSecret).toBe(FIELD_REQUIRED);
    expect(errors.groupsClaim).toBe(FIELD_REQUIRED);
    expect(errors.idpJwsAlgorithm).toBe(FIELD_REQUIRED);
  });

  it.each([
    'idpAuthorizationUrl',
    'idpLogoutUrl',
    'idpTokenUrl',
    'idpJwksUrl',
  ] as const)('reports invalid URL format for %s', (field) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, [field]: 'not-a-url' });
    expect(errors[field]).toBe(URL_ERROR);
  });

  it('returns no errors for a valid config', () => {
    expect(validateOAuth2(VALID_CONFIG)).toEqual({});
  });
});

describe('oauth2FormMachine', () => {
  it('starts in loading and transitions to editing after load', async () => {
    const service = await startLoaded();
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(service.getSnapshot().context.data.clientId).toBe('client-id');
    expect(service.getSnapshot().context.isPristine).toBe(true);
    service.stop();
  });

  it('transitions to loadError on failure, then RETRY reloads successfully into editing', async () => {
    let attempt = 0;
    const load = async () => {
      attempt += 1;
      if (attempt === 1) throw new Error('load boom');
      return { data: VALID_CONFIG };
    };
    const service = interpret(withServices({ load })).start();

    await waitFor(service, (state) => state.matches('loadError'));
    expect(service.getSnapshot().context.loadError).toBe('load boom');

    // RETRY is an internal createFormMachine event (fired from the loadError
    // state); it is not part of the public FormEvent union, hence the cast.
    service.send({ type: 'RETRY' } as any);
    await waitFor(service, (state) => state.matches('editing'));
    expect(service.getSnapshot().context.loadError).toBeNull();
    expect(service.getSnapshot().context.data.clientId).toBe('client-id');
    service.stop();
  });

  it('marks the form dirty and validates on UPDATE', async () => {
    const service = await startLoaded();
    service.send({ type: 'UPDATE', name: 'clientId', value: '' } as any);
    const snap = service.getSnapshot();
    expect(snap.context.isPristine).toBe(false);
    expect(snap.context.validationErrors.clientId).toBe(FIELD_REQUIRED);
    service.stop();
  });

  it('SUBMIT with validation errors stays in editing (no save)', async () => {
    const save = jest.fn(async () => ({}));
    const service = await startLoaded({ save });
    service.send({ type: 'UPDATE', name: 'clientId', value: '' } as any);
    service.send({ type: 'SUBMIT' } as any);
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(save).not.toHaveBeenCalled();
    service.stop();
  });

  it('SUBMIT with valid data saves and returns to a pristine editing state', async () => {
    const save = jest.fn(async () => ({}));
    const service = await startLoaded({ save });
    service.send({ type: 'UPDATE', name: 'clientId', value: 'updated' } as any);
    service.send({ type: 'SUBMIT' } as any);
    await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);
    expect(save).toHaveBeenCalledTimes(1);
    expect(service.getSnapshot().context.data.clientId).toBe('updated');
    service.stop();
  });

  it('surfaces saveError and returns to editing on save failure', async () => {
    const service = await startLoaded({
      save: async () => Promise.reject(new Error('save boom')),
    });
    service.send({ type: 'UPDATE', name: 'clientId', value: 'updated' } as any);
    service.send({ type: 'SUBMIT' } as any);
    await waitFor(service, (state) => state.matches('editing') && state.context.saveError !== null);
    expect(service.getSnapshot().context.saveError).toBe('save boom');
    service.stop();
  });

  it('RESET restores the pristine data', async () => {
    const service = await startLoaded();
    service.send({ type: 'UPDATE', name: 'clientId', value: 'changed' } as any);
    expect(service.getSnapshot().context.isPristine).toBe(false);
    service.send({ type: 'RESET' } as any);
    expect(service.getSnapshot().context.data.clientId).toBe('client-id');
    expect(service.getSnapshot().context.isPristine).toBe(true);
    service.stop();
  });
});

// NEXUS-54266: the page previously omitted useTrustStore from OAuth2Config
// entirely, so the machine's context never carried it and every save sent a
// payload without it — silently resetting the backend's primitive boolean to
// false. This asserts the value survives load -> edit -> save untouched.
describe('useTrustStore preservation', () => {
  it('carries a loaded useTrustStore=true through save of an unrelated field', async () => {
    const save = jest.fn(async () => ({}));
    const service = interpret(
      createOAuth2FormMachine().withConfig({
        services: {
          load: async () => ({ data: { ...VALID_CONFIG, useTrustStore: true } }),
          save,
        },
      })
    ).start();
    await waitFor(service, (state) => state.matches('editing'));

    service.send({ type: 'UPDATE', name: 'emailClaim', value: 'mail' } as any);
    service.send({ type: 'SUBMIT' } as any);
    await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);

    expect(save).toHaveBeenCalledTimes(1);
    const savedContext = save.mock.calls[0][0] as unknown as { data: { useTrustStore: boolean } };
    expect(savedContext.data.useTrustStore).toBe(true);
  });

  it('exposes useTrustStore on the loaded context so the checkbox is controlled', async () => {
    const service = interpret(
      createOAuth2FormMachine().withConfig({
        services: {
          load: async () => ({ data: { ...VALID_CONFIG, useTrustStore: true } }),
          save: async () => ({}),
        },
      })
    ).start();
    await waitFor(service, (state) => state.matches('editing'));

    expect(service.getSnapshot().context.data.useTrustStore).toBe(true);
  });
});

// NEXUS-54266: malformed JSON in these fields used to be coerced to `{}` by the
// API layer, so the save "succeeded" while destroying the previous value.
describe('JSON object field validation', () => {
  const JSON_FIELDS = ['exactMatchClaims', 'authorizationCustomParams', 'tokenRequestCustomParams'] as const;

  it.each(JSON_FIELDS)('rejects malformed JSON in %s', (field) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, [field]: '{"dept": "engineering"' });
    expect(errors[field]).toBe(INVALID_JSON_OBJECT);
  });

  it.each(JSON_FIELDS)('allows a blank %s', (field) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, [field]: '' });
    expect(errors[field]).toBeUndefined();
  });

  it.each(JSON_FIELDS)('allows a valid JSON object in %s', (field) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, [field]: '{"dept": "engineering"}' });
    expect(errors[field]).toBeUndefined();
  });

  it('rejects a JSON array (the API models these as string maps)', () => {
    const errors = validateOAuth2({ ...VALID_CONFIG, exactMatchClaims: '["a", "b"]' });
    expect(errors.exactMatchClaims).toBe(INVALID_JSON_OBJECT);
  });

  it.each(['5', '"a string"', 'true', 'null'])('rejects the non-object JSON literal %s', (literal) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, exactMatchClaims: literal });
    expect(errors.exactMatchClaims).toBe(INVALID_JSON_OBJECT);
  });

  it('blocks SUBMIT so no save is attempted with malformed JSON', async () => {
    const save = jest.fn(async () => ({}));
    const service = await startLoaded({ save });

    service.send({ type: 'UPDATE', name: 'exactMatchClaims', value: '{"dept": "engineering"' } as any);
    service.send({ type: 'SUBMIT' } as any);

    expect(save).not.toHaveBeenCalled();
    expect(service.getSnapshot().value).toBe('editing');
    expect(service.getSnapshot().context.validationErrors.exactMatchClaims).toBe(INVALID_JSON_OBJECT);
  });
});

// NEXUS-54266: ValidationUtils.isUrl called decodeURIComponent on the pathname
// outside its try/catch, so a malformed percent escape threw URIError out of
// validation — in the machine it escaped the change handler and dropped the edit.
describe('malformed percent escapes in URL fields', () => {
  const URL_FIELDS = ['idpAuthorizationUrl', 'idpLogoutUrl', 'idpTokenUrl', 'idpJwksUrl'] as const;
  const MALFORMED = ['https://example.com/%', 'https://example.com/%zz', 'https://example.com/%E0%A4%A'];

  it.each(URL_FIELDS)('does not throw and reports a URL error for %s', (field) => {
    for (const value of MALFORMED) {
      expect(() => validateOAuth2({ ...VALID_CONFIG, [field]: value })).not.toThrow();
      expect(validateOAuth2({ ...VALID_CONFIG, [field]: value })[field]).toBe(URL_ERROR);
    }
  });

  it('still accepts a well-formed percent-encoded path', () => {
    // %41 decodes to 'A' — decodable and whitespace-free, so it stays valid.
    const errors = validateOAuth2({ ...VALID_CONFIG, idpTokenUrl: 'https://example.com/a%41b/token' });
    expect(errors.idpTokenUrl).toBeUndefined();
  });

  it('keeps rejecting an escape that decodes to whitespace', () => {
    // Pre-existing URL_PATHNAME_REGEX behaviour (/^([\S]*\S)?$/), unchanged here:
    // %20 decodes to a space, which the shared validator treats as invalid.
    const errors = validateOAuth2({ ...VALID_CONFIG, idpTokenUrl: 'https://example.com/a%20b/token' });
    expect(errors.idpTokenUrl).toBe(URL_ERROR);
  });

  it('UPDATE with a malformed escape does not throw out of the machine', async () => {
    const service = await startLoaded();

    expect(() =>
      service.send({ type: 'UPDATE', name: 'idpTokenUrl', value: 'https://example.com/%' } as any)
    ).not.toThrow();

    expect(service.getSnapshot().context.data.idpTokenUrl).toBe('https://example.com/%');
    expect(service.getSnapshot().context.validationErrors.idpTokenUrl).toBe(URL_ERROR);
  });
});

// NEXUS-54266: the API models these as Map<String, String>. Arrays and nested
// objects get a 400 (generic save error); scalars are silently coerced to strings.
describe('JSON map values must be strings', () => {
  it.each([
    ['array value', '{"role": ["admin"]}'],
    ['nested object value', '{"claims": {"role": "admin"}}'],
    ['number value', '{"max_age": 300}'],
    ['boolean value', '{"prompt": true}'],
    ['null value', '{"scope": null}'],
    ['mixed valid and invalid', '{"good": "yes", "bad": 1}'],
  ])('rejects %s', (_label, value) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, exactMatchClaims: value });
    expect(errors.exactMatchClaims).toBe(INVALID_JSON_OBJECT);
  });

  it.each([
    ['flat string map', '{"role": "admin", "dept": "eng"}'],
    ['empty object', '{}'],
    ['numeric-looking string', '{"max_age": "300"}'],
  ])('accepts %s', (_label, value) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, exactMatchClaims: value });
    expect(errors.exactMatchClaims).toBeUndefined();
  });
});
