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

import {
  createSamlFormMachine,
  validateSaml,
  parseSignatureValidation,
  toSamlPayload,
  DEFAULT_CONFIG,
} from '../samlFormMachine';
import { SamlConfiguration } from '../types';

const CONFIGURED: SamlConfiguration = {
  entityId: 'https://nexus.example.com',
  idpMetadata: '<EntityDescriptor>...</EntityDescriptor>',
  usernameAttribute: 'email',
  firstNameAttribute: 'firstName',
  lastNameAttribute: 'lastName',
  emailAttribute: 'email',
  groupsAttribute: 'groups',
  validateResponseSignature: true,
  validateAssertionSignature: null,
};

const RESET_DEFAULT: SamlConfiguration = { ...DEFAULT_CONFIG, entityId: 'http://localhost/metadata' };

const withServices = (over: Record<string, unknown> = {}) =>
  createSamlFormMachine().withConfig({
    services: {
      load: async () => ({ data: CONFIGURED, isConfigured: true }),
      save: async () => undefined,
      delete: async () => RESET_DEFAULT,
      ...over,
    },
  });

const startLoaded = async (over: Record<string, unknown> = {}) => {
  const service = interpret(withServices(over)).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
};

describe('validateSaml', () => {
  it('requires idpMetadata and usernameAttribute', () => {
    const errors = validateSaml({ ...DEFAULT_CONFIG, idpMetadata: '', usernameAttribute: '' });
    expect(errors.idpMetadata).toBe('Identity Provider Metadata is required');
    expect(errors.usernameAttribute).toBe('Username Attribute is required');
  });

  it('rejects a non-URI entityId', () => {
    const errors = validateSaml({ ...CONFIGURED, entityId: 'not-a-uri' });
    expect(errors.entityId).toBe('Entity ID must be a URI');
  });

  it('accepts a valid config', () => {
    expect(validateSaml(CONFIGURED)).toEqual({});
  });
});

describe('parseSignatureValidation', () => {
  it.each([
    ['default', null],
    ['true', true],
    ['false', false],
    [null, null],
    [undefined, null],
    [true, true],
    [false, false],
  ])('maps %s to %s', (input, expected) => {
    expect(parseSignatureValidation(input as never)).toBe(expected);
  });
});

describe('toSamlPayload', () => {
  it('trims attributes and converts tri-state signature fields', () => {
    const payload = toSamlPayload({
      ...CONFIGURED,
      usernameAttribute: '  email  ',
      validateResponseSignature: null,
      validateAssertionSignature: null,
    });
    expect(payload.usernameAttribute).toBe('email');
    expect(payload.validateResponseSignature).toBeNull();
    expect(payload.validateAssertionSignature).toBeNull();
  });
});

describe('samlFormMachine', () => {
  it('loads an existing configuration and marks it configured', async () => {
    const service = await startLoaded();
    const snap = service.getSnapshot();
    expect(snap.context.data.entityId).toBe('https://nexus.example.com');
    expect((snap.context as any).isConfigured).toBe(true);
    service.stop();
  });

  it('treats a missing configuration as unconfigured', async () => {
    const service = await startLoaded({
      load: async () => ({ data: { ...DEFAULT_CONFIG }, isConfigured: false }),
    });
    expect((service.getSnapshot().context as any).isConfigured).toBe(false);
    service.stop();
  });

  it('surfaces a load error via the machine error path while staying editable', async () => {
    // A real load failure rejects; the machine's loading.onError sets loadError
    // and lands in editing (not a dead state), so the form stays usable.
    const service = interpret(
      withServices({ load: async () => Promise.reject(new Error('boom')) })
    ).start();
    await waitFor(service, (state) => state.matches('editing'));
    const snap = service.getSnapshot();
    expect(snap.matches('editing')).toBe(true);
    expect(snap.context.loadError).toBe('boom');
    service.stop();
  });

  it('does not save when validation fails', async () => {
    const save = jest.fn(async () => undefined);
    const service = await startLoaded({ save });
    service.send({ type: 'UPDATE', name: 'idpMetadata', value: '' } as any);
    service.send({ type: 'SUBMIT' } as any);
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(save).not.toHaveBeenCalled();
    service.stop();
  });

  it('saves and flips isConfigured true', async () => {
    const service = await startLoaded({
      load: async () => ({ data: { ...DEFAULT_CONFIG }, isConfigured: false, loadError: null }),
    });
    service.send({ type: 'UPDATE', name: 'idpMetadata', value: '<xml/>' } as any);
    service.send({ type: 'SUBMIT' } as any);
    await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);
    const snap = service.getSnapshot();
    expect((snap.context as any).isConfigured).toBe(true);
    expect(snap.context.saveError).toBeNull();
    service.stop();
  });

  it('surfaces save errors', async () => {
    const service = await startLoaded({ save: async () => Promise.reject(new Error('save boom')) });
    service.send({ type: 'UPDATE', name: 'usernameAttribute', value: 'changed' } as any);
    service.send({ type: 'SUBMIT' } as any);
    await waitFor(service, (state) => state.context.saveError !== null);
    expect(service.getSnapshot().context.saveError).toBe('save boom');
    service.stop();
  });

  it('opens and cancels the delete confirmation without deleting', async () => {
    const del = jest.fn(async () => RESET_DEFAULT);
    const service = await startLoaded({ delete: del });
    service.send({ type: 'DELETE' } as any);
    expect(service.getSnapshot().matches('confirmingDelete')).toBe(true);
    service.send({ type: 'CANCEL_DELETE' } as any);
    expect(service.getSnapshot().matches('editing')).toBe(true);
    expect(del).not.toHaveBeenCalled();
    service.stop();
  });

  it('deletes, then stays in editing and resets to unconfigured (delete-with-stay override)', async () => {
    const service = await startLoaded({ delete: async () => RESET_DEFAULT });
    service.send({ type: 'DELETE' } as any);
    service.send({ type: 'CONFIRM_DELETE' } as any);
    await waitFor(service, (state) => state.matches('editing') && !(state.context as any).isConfigured);
    const snap = service.getSnapshot();
    expect(snap.matches('editing')).toBe(true);
    expect((snap.context as any).isConfigured).toBe(false);
    expect(snap.context.data.entityId).toBe('http://localhost/metadata');
    expect(snap.context.isPristine).toBe(true);
    service.stop();
  });

  it('keeps the confirmation open and surfaces the error on delete failure (retriable)', async () => {
    const service = await startLoaded({ delete: async () => Promise.reject(new Error('del boom')) });
    service.send({ type: 'DELETE' } as any);
    service.send({ type: 'CONFIRM_DELETE' } as any);
    await waitFor(service, (state) => state.context.deleteError !== null);
    const snap = service.getSnapshot();
    // Parity with legacy: dialog stays open (confirmingDelete) with the error shown.
    expect(snap.matches('confirmingDelete')).toBe(true);
    expect(snap.context.deleteError).toBe('del boom');
    expect((snap.context as any).isConfigured).toBe(true);
    service.stop();
  });

  it('clears a prior delete error when the confirmation is re-opened', async () => {
    const service = await startLoaded({ delete: async () => Promise.reject(new Error('del boom')) });
    service.send({ type: 'DELETE' } as any);
    service.send({ type: 'CONFIRM_DELETE' } as any);
    await waitFor(service, (state) => state.context.deleteError !== null);
    // Cancel, then re-open — the stale error should be cleared by the DELETE transition.
    service.send({ type: 'CANCEL_DELETE' } as any);
    service.send({ type: 'DELETE' } as any);
    expect(service.getSnapshot().matches('confirmingDelete')).toBe(true);
    expect(service.getSnapshot().context.deleteError).toBeNull();
    service.stop();
  });

  it('CLEAR_ERROR dismisses the error banner', async () => {
    const service = await startLoaded({ save: async () => Promise.reject(new Error('boom')) });
    service.send({ type: 'UPDATE', name: 'usernameAttribute', value: 'x' } as any);
    service.send({ type: 'SUBMIT' } as any);
    await waitFor(service, (state) => state.context.saveError !== null);
    service.send({ type: 'CLEAR_ERROR' } as any);
    expect(service.getSnapshot().context.saveError).toBeNull();
    service.stop();
  });
});
