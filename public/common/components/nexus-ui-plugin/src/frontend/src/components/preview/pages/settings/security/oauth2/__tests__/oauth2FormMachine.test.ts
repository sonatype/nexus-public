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
    expect(errors.clientId).toBe('Client ID is required');
    expect(errors.clientSecret).toBe('Client Secret is required');
    expect(errors.groupsClaim).toBe('Groups claim is required');
    expect(errors.idpJwsAlgorithm).toBe('JWS Algorithm is required');
  });

  it.each([
    'idpAuthorizationUrl',
    'idpLogoutUrl',
    'idpTokenUrl',
    'idpJwksUrl',
  ] as const)('reports invalid URL format for %s', (field) => {
    const errors = validateOAuth2({ ...VALID_CONFIG, [field]: 'not-a-url' });
    expect(errors[field]).toBe('Must be a valid URL');
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
    expect(snap.context.validationErrors.clientId).toBe('Client ID is required');
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
