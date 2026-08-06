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
import { createUserTokensMachine, validateExpirationDays } from '../userTokensMachine';
import type { UserTokenSettings } from '../types';

const LOADED: UserTokenSettings = {
  enabled: true,
  protectContent: true,
  expirationEnabled: false,
  expirationDays: 30,
};

const withServices = (over: Partial<Record<'load' | 'save' | 'resetAllTokens', any>> = {}) =>
  createUserTokensMachine().withConfig({
    services: {
      load: async () => LOADED,
      save: async () => ({}),
      resetAllTokens: async () => undefined,
      ...over,
    },
  });

const startLoaded = async (over: Partial<Record<'load' | 'save' | 'resetAllTokens', any>> = {}) => {
  const s = interpret(withServices(over)).start();
  await waitFor(s, (st) => st.matches('editing'));
  return s;
};

describe('validateExpirationDays', () => {
  it('is null when expiration disabled', () => {
    expect(validateExpirationDays({ ...LOADED, expirationEnabled: false })).toBeNull();
  });

  it('requires a value when enabled', () => {
    expect(
      validateExpirationDays({ ...LOADED, expirationEnabled: true, expirationDays: 0 })
    ).toBe('Expiration days is required');
  });

  it('rejects out-of-range values', () => {
    expect(
      validateExpirationDays({ ...LOADED, expirationEnabled: true, expirationDays: 1000 })
    ).toContain('between 1 and 999');
  });

  it('accepts an in-range value', () => {
    expect(
      validateExpirationDays({ ...LOADED, expirationEnabled: true, expirationDays: 30 })
    ).toBeNull();
  });
});

describe('userTokensMachine', () => {
  it('loads settings into data + pristineData', async () => {
    const s = await startLoaded();
    expect(s.getSnapshot().context.data).toEqual(LOADED);
    expect(s.getSnapshot().context.isPristine).toBe(true);
    s.stop();
  });

  it('keeps editing and surfaces loadError when load fails', async () => {
    const s = interpret(withServices({ load: async () => { throw new Error('nope'); } })).start();
    await waitFor(s, (st) => st.matches('editing'));
    expect(s.getSnapshot().context.loadError).toBe('nope');
    s.stop();
  });

  it('disabling enabled cascades protectContent/expiration off and days to 30', async () => {
    const s = await startLoaded();
    s.send({ type: 'UPDATE', field: 'expirationEnabled', value: true });
    s.send({ type: 'UPDATE', field: 'enabled', value: false });
    const d = s.getSnapshot().context.data;
    expect(d).toEqual({ enabled: false, protectContent: false, expirationEnabled: false, expirationDays: 30 });
    s.stop();
  });

  it('validates expirationDays only when expiration enabled', async () => {
    const s = await startLoaded();
    s.send({ type: 'UPDATE', field: 'expirationEnabled', value: true });
    s.send({ type: 'UPDATE', field: 'expirationDays', value: 0 });
    expect(s.getSnapshot().context.validationErrors.expirationDays).toBeTruthy();
    s.send({ type: 'UPDATE', field: 'expirationDays', value: 1000 });
    expect(s.getSnapshot().context.validationErrors.expirationDays).toContain('between 1 and 999');
    s.send({ type: 'UPDATE', field: 'expirationDays', value: 30 });
    expect(s.getSnapshot().context.validationErrors.expirationDays).toBeFalsy();
    s.stop();
  });

  it('SUBMIT with an unchanged expiration goes straight to saving then back to editing pristine', async () => {
    const s = await startLoaded();
    s.send({ type: 'UPDATE', field: 'protectContent', value: false });
    s.send({ type: 'SUBMIT' });
    await waitFor(s, (st) => st.matches('editing') && st.context.isPristine);
    expect(s.getSnapshot().context.data.protectContent).toBe(false);
    s.stop();
  });

  it('SUBMIT after toggling expiration routes through the warning state', async () => {
    const s = await startLoaded();
    s.send({ type: 'UPDATE', field: 'expirationEnabled', value: true });
    s.send({ type: 'UPDATE', field: 'expirationDays', value: 45 });
    s.send({ type: 'SUBMIT' });
    expect(s.getSnapshot().matches('confirmingSaveWithExpirationWarning')).toBe(true);
    s.send({ type: 'CANCEL_SAVE' });
    expect(s.getSnapshot().matches('editing')).toBe(true);
    s.send({ type: 'SUBMIT' });
    s.send({ type: 'CONFIRM_SAVE' });
    await waitFor(s, (st) => st.matches('editing') && st.context.isPristine);
    s.stop();
  });

  it('SUBMIT with validation errors stays in editing without saving', async () => {
    const save = jest.fn().mockResolvedValue({});
    const s = await startLoaded({ save });
    s.send({ type: 'UPDATE', field: 'expirationEnabled', value: true });
    s.send({ type: 'UPDATE', field: 'expirationDays', value: 0 });
    s.send({ type: 'SUBMIT' });
    expect(s.getSnapshot().matches('editing')).toBe(true);
    expect(save).not.toHaveBeenCalled();
    s.stop();
  });

  it('surfaces saveError and returns to editing on save failure', async () => {
    const s = await startLoaded({ save: async () => { throw new Error('save boom'); } });
    s.send({ type: 'UPDATE', field: 'protectContent', value: false });
    s.send({ type: 'SUBMIT' });
    await waitFor(s, (st) => st.matches('editing') && st.context.saveError !== null);
    expect(s.getSnapshot().context.saveError).toBe('save boom');
    expect(s.getSnapshot().context.isPristine).toBe(false);
    s.stop();
  });

  it('DISCARD restores pristine data', async () => {
    const s = await startLoaded();
    s.send({ type: 'UPDATE', field: 'protectContent', value: false });
    expect(s.getSnapshot().context.isPristine).toBe(false);
    s.send({ type: 'DISCARD' });
    expect(s.getSnapshot().context.data).toEqual(LOADED);
    expect(s.getSnapshot().context.isPristine).toBe(true);
    s.stop();
  });

  it('CLEAR_ERROR clears the surfaced error', async () => {
    const s = interpret(withServices({ load: async () => { throw new Error('nope'); } })).start();
    await waitFor(s, (st) => st.matches('editing'));
    expect(s.getSnapshot().context.loadError).toBe('nope');
    s.send({ type: 'CLEAR_ERROR' });
    expect(s.getSnapshot().context.loadError).toBeNull();
    s.stop();
  });
});

describe('userTokensMachine reset flow', () => {
  it('rejects a wrong confirmation string and stays in the confirm state', async () => {
    const s = await startLoaded();
    s.send({ type: 'REQUEST_RESET' });
    s.send({ type: 'UPDATE_RESET_CONFIRMATION', value: 'nope' });
    s.send({ type: 'CONFIRM_RESET' });
    expect(s.getSnapshot().matches('confirmingResetAllTokens')).toBe(true);
    expect(s.getSnapshot().context.resetConfirmationError).toContain('Reset all tokens');
    s.stop();
  });

  it('accepts the exact confirmation string and resets successfully', async () => {
    const reset = jest.fn().mockResolvedValue(undefined);
    const s = await startLoaded({ resetAllTokens: reset });
    s.send({ type: 'REQUEST_RESET' });
    s.send({ type: 'UPDATE_RESET_CONFIRMATION', value: 'Reset all tokens' });
    s.send({ type: 'CONFIRM_RESET' });
    await waitFor(s, (st) => st.matches('editing'));
    expect(reset).toHaveBeenCalled();
    expect(s.getSnapshot().context.resetConfirmationInput).toBe('');
    s.stop();
  });

  it('on reset failure returns to the confirm state with an error (modal stays open)', async () => {
    const s = await startLoaded({ resetAllTokens: async () => { throw new Error('boom'); } });
    s.send({ type: 'REQUEST_RESET' });
    s.send({ type: 'UPDATE_RESET_CONFIRMATION', value: 'Reset all tokens' });
    s.send({ type: 'CONFIRM_RESET' });
    await waitFor(s, (st) => st.matches('confirmingResetAllTokens') && st.context.resetError !== null);
    expect(s.getSnapshot().context.resetError).toBe('boom');
    s.stop();
  });

  it('CANCEL_RESET returns to editing and clears input/error', async () => {
    const s = await startLoaded();
    s.send({ type: 'REQUEST_RESET' });
    s.send({ type: 'UPDATE_RESET_CONFIRMATION', value: 'x' });
    s.send({ type: 'CANCEL_RESET' });
    expect(s.getSnapshot().matches('editing')).toBe(true);
    expect(s.getSnapshot().context.resetConfirmationInput).toBe('');
    s.stop();
  });

  it('entering the confirm state clears any leftover input from a prior attempt', async () => {
    const s = await startLoaded();
    s.send({ type: 'REQUEST_RESET' });
    s.send({ type: 'UPDATE_RESET_CONFIRMATION', value: 'wrong' });
    s.send({ type: 'CONFIRM_RESET' });
    expect(s.getSnapshot().context.resetConfirmationError).toBeTruthy();
    s.send({ type: 'CANCEL_RESET' });
    s.send({ type: 'REQUEST_RESET' });
    expect(s.getSnapshot().context.resetConfirmationInput).toBe('');
    expect(s.getSnapshot().context.resetConfirmationError).toBeNull();
    s.stop();
  });
});
