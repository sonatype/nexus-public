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

import { createCrowdSettingsMachine, validateCrowd } from '../crowdSettingsMachine';
import { CrowdConfig, DEFAULT_CROWD_CONFIG } from '../types';

const VALID_CONFIG: CrowdConfig = {
  enabled: true,
  realmActive: false,
  url: 'http://crowd.example.com',
  useTrustStoreForUrl: false,
  applicationName: 'nexus',
  applicationPassword: 'secret',
  timeout: 30,
};

const withServices = (over: Record<string, unknown> = {}) =>
  createCrowdSettingsMachine().withConfig({
    services: {
      load: async () => VALID_CONFIG,
      save: async () => undefined,
      verifyConnection: async () => undefined,
      clearCache: async () => undefined,
      ...over,
    },
  });

const startLoaded = async (over: Record<string, unknown> = {}) => {
  const service = interpret(withServices(over)).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
};

describe('validateCrowd', () => {
  it('requires a URL', () => {
    expect(validateCrowd(DEFAULT_CROWD_CONFIG).url).toBe('Crowd server URL is required');
  });

  it('rejects a malformed URL', () => {
    expect(validateCrowd({ ...VALID_CONFIG, url: 'not-a-url' }).url).toBe('URL is not valid');
  });

  it('requires application name and password', () => {
    const errors = validateCrowd({ ...VALID_CONFIG, applicationName: '', applicationPassword: '' });
    expect(errors.applicationName).toBe('Application name is required');
    expect(errors.applicationPassword).toBe('Application password is required');
  });

  it('reports a distinct non-numeric timeout message', () => {
    const errors = validateCrowd({ ...VALID_CONFIG, timeout: 'abc' as unknown as number });
    expect(errors.timeout).toBe('Timeout must be a number');
  });

  it('reports out-of-range timeout', () => {
    expect(validateCrowd({ ...VALID_CONFIG, timeout: 5000 }).timeout).toBe(
      'Timeout must be between 1 and 3600 seconds'
    );
  });

  it('returns no errors for a valid config', () => {
    expect(validateCrowd(VALID_CONFIG)).toEqual({});
  });
});

describe('crowdSettingsMachine', () => {
  it('loads config then enters editing with validated, pristine state', async () => {
    const service = await startLoaded();
    const snap = service.getSnapshot();
    expect(snap.matches('editing')).toBe(true);
    expect(snap.context.data.applicationName).toBe('nexus');
    expect(snap.context.isPristine).toBe(true);
    expect(snap.context.validationErrors).toEqual({});
    service.stop();
  });

  it('swallows load failure and shows defaults (URL required)', async () => {
    const service = interpret(
      withServices({ load: async () => Promise.reject(new Error('load boom')) })
    ).start();
    await waitFor(service, (state) => state.matches('editing'));
    const snap = service.getSnapshot();
    expect(snap.context.error).toBeNull();
    expect(snap.context.validationErrors.url).toBe('Crowd server URL is required');
    service.stop();
  });

  it('marks dirty and validates on UPDATE', async () => {
    const service = await startLoaded();
    service.send({ type: 'UPDATE', field: 'applicationName', value: '' });
    const snap = service.getSnapshot();
    expect(snap.context.isPristine).toBe(false);
    expect(snap.context.validationErrors.applicationName).toBe('Application name is required');
    service.stop();
  });

  it('does not save when validation fails (canSave guard)', async () => {
    const save = jest.fn(async () => undefined);
    const service = await startLoaded({ save });
    service.send({ type: 'UPDATE', field: 'url', value: 'bad' });
    service.send({ type: 'SUBMIT' });
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(save).not.toHaveBeenCalled();
    service.stop();
  });

  it('saves valid changes and returns to pristine editing', async () => {
    const save = jest.fn(async () => undefined);
    const service = await startLoaded({ save });
    service.send({ type: 'UPDATE', field: 'applicationName', value: 'updated' });
    service.send({ type: 'SUBMIT' });
    await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);
    expect(save).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('surfaces save errors', async () => {
    const service = await startLoaded({ save: async () => Promise.reject(new Error('save boom')) });
    service.send({ type: 'UPDATE', field: 'applicationName', value: 'updated' });
    service.send({ type: 'SUBMIT' });
    await waitFor(service, (state) => state.context.error !== null);
    expect(service.getSnapshot().context.error).toBe('save boom');
    service.stop();
  });

  it('verifies connection and returns to editing', async () => {
    const verifyConnection = jest.fn(async () => undefined);
    const service = await startLoaded({ verifyConnection });
    service.send({ type: 'VERIFY_CONNECTION' });
    await waitFor(service, (state) => state.matches('editing') && verifyConnection.mock.calls.length > 0);
    expect(verifyConnection).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('does not verify when validation fails (canVerify guard)', async () => {
    const verifyConnection = jest.fn(async () => undefined);
    const service = await startLoaded({ verifyConnection });
    service.send({ type: 'UPDATE', field: 'url', value: '' });
    service.send({ type: 'VERIFY_CONNECTION' });
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(verifyConnection).not.toHaveBeenCalled();
    service.stop();
  });

  it('surfaces verify errors and returns to editing', async () => {
    const service = await startLoaded({
      verifyConnection: async () => Promise.reject(new Error('verify boom')),
    });
    service.send({ type: 'VERIFY_CONNECTION' });
    await waitFor(service, (state) => state.context.error !== null);
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(service.getSnapshot().context.error).toBe('verify boom');
    service.stop();
  });

  it('surfaces clear-cache errors and returns to editing', async () => {
    const service = await startLoaded({
      clearCache: async () => Promise.reject(new Error('clear boom')),
    });
    service.send({ type: 'CLEAR_CACHE' });
    await waitFor(service, (state) => state.context.error !== null);
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(service.getSnapshot().context.error).toBe('clear boom');
    service.stop();
  });

  it('clears cache and returns to editing', async () => {
    const clearCache = jest.fn(async () => undefined);
    const service = await startLoaded({ clearCache });
    service.send({ type: 'CLEAR_CACHE' });
    await waitFor(service, (state) => state.matches('editing') && clearCache.mock.calls.length > 0);
    expect(clearCache).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('DISCARD restores pristine data', async () => {
    const service = await startLoaded();
    service.send({ type: 'UPDATE', field: 'applicationName', value: 'changed' });
    expect(service.getSnapshot().context.isPristine).toBe(false);
    service.send({ type: 'DISCARD' });
    expect(service.getSnapshot().context.data.applicationName).toBe('nexus');
    expect(service.getSnapshot().context.isPristine).toBe(true);
    service.stop();
  });

  it('CLEAR_ERROR clears the error banner', async () => {
    const service = await startLoaded({ clearCache: async () => Promise.reject(new Error('boom')) });
    service.send({ type: 'CLEAR_CACHE' });
    await waitFor(service, (state) => state.context.error !== null);
    service.send({ type: 'CLEAR_ERROR' });
    expect(service.getSnapshot().context.error).toBeNull();
    service.stop();
  });
});
