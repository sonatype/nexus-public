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
  createUserAccountFormMachine,
  isPasswordFormReady,
  validateUserAccount,
} from '../userAccountFormMachine';

jest.mock('../../../../../../interface/api', () => ({
  ENDPOINTS: {
    USER_ACCOUNT: '/service/rest/internal/ui/user',
  },
  restClient: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

const { restClient } = jest.requireMock('../../../../../../interface/api');

const MOCK_USER = {
  userId: 'testuser',
  firstName: 'Test',
  lastName: 'User',
  email: 'test@example.com',
  external: false,
};

async function startAndLoad(overrides?: Partial<typeof MOCK_USER>) {
  restClient.get.mockResolvedValue({ ...MOCK_USER, ...overrides });
  const machine = createUserAccountFormMachine();
  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('userAccountFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading', () => {
    it('starts in loading state then transitions to editing', async () => {
      restClient.get.mockResolvedValue(MOCK_USER);
      const machine = createUserAccountFormMachine();
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('populates data from API response', async () => {
      const service = await startAndLoad();
      const { data } = service.getSnapshot().context;

      expect(data.userId).toBe('testuser');
      expect(data.firstName).toBe('Test');
      expect(data.lastName).toBe('User');
      expect(data.email).toBe('test@example.com');
      expect(data.external).toBe(false);
      expect(data.currentPassword).toBe('');
      expect(data.newPassword).toBe('');
      expect(data.confirmPassword).toBe('');

      service.stop();
    });

    it('transitions to loadError on API failure', async () => {
      restClient.get.mockRejectedValue(new Error('Network error'));
      const machine = createUserAccountFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().context.loadError).toBeTruthy();

      service.stop();
    });

    it('handles missing/empty fields in API response with defaults', async () => {
      const service = await startAndLoad({ firstName: '', lastName: '', email: '' });
      const { data } = service.getSnapshot().context;

      expect(data.firstName).toBe('');
      expect(data.lastName).toBe('');
      expect(data.email).toBe('');

      service.stop();
    });
  });

  describe('validation', () => {
    it('has no errors when no password fields are filled', async () => {
      const service = await startAndLoad();
      const { validationErrors } = service.getSnapshot().context;

      expect(validationErrors.newPassword).toBeFalsy();
      expect(validationErrors.confirmPassword).toBeFalsy();

      service.stop();
    });

    it('reports password-too-short error once newPassword has content', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'newPassword', value: 'short' } as any);

      const { validationErrors } = service.getSnapshot().context;
      expect(validationErrors.newPassword).toMatch(/at least.*8.*characters/i);

      service.stop();
    });

    it('reports mismatch error when confirmPassword differs from newPassword', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'newPassword', value: 'password123' } as any);
      service.send({ type: 'UPDATE', name: 'confirmPassword', value: 'different123' } as any);

      const { validationErrors } = service.getSnapshot().context;
      expect(validationErrors.confirmPassword).toMatch(/do not match/i);

      service.stop();
    });

    it('clears mismatch error when passwords match', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'newPassword', value: 'password123' } as any);
      service.send({ type: 'UPDATE', name: 'confirmPassword', value: 'password123' } as any);

      const { validationErrors } = service.getSnapshot().context;
      expect(validationErrors.confirmPassword).toBeFalsy();
      expect(validationErrors.newPassword).toBeFalsy();

      service.stop();
    });
  });

  describe('isPasswordFormReady helper', () => {
    const baseData = {
      userId: 'u',
      firstName: 'f',
      lastName: 'l',
      email: 'e@e.com',
      external: false,
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    };

    it('returns false when any password field is empty', () => {
      expect(isPasswordFormReady({ ...baseData, currentPassword: '', newPassword: 'newpassword', confirmPassword: 'newpassword' })).toBe(false);
      expect(isPasswordFormReady({ ...baseData, currentPassword: 'old', newPassword: '', confirmPassword: 'newpassword' })).toBe(false);
      expect(isPasswordFormReady({ ...baseData, currentPassword: 'old', newPassword: 'newpassword', confirmPassword: '' })).toBe(false);
    });

    it('returns false when validation fails (mismatch)', () => {
      expect(
        isPasswordFormReady({
          ...baseData,
          currentPassword: 'old',
          newPassword: 'newpassword',
          confirmPassword: 'different',
        })
      ).toBe(false);
    });

    it('returns false when newPassword is too short', () => {
      expect(
        isPasswordFormReady({
          ...baseData,
          currentPassword: 'old',
          newPassword: 'short',
          confirmPassword: 'short',
        })
      ).toBe(false);
    });

    it('returns true when all fields are filled, passwords match, and length is valid', () => {
      expect(
        isPasswordFormReady({
          ...baseData,
          currentPassword: 'old',
          newPassword: 'newpassword',
          confirmPassword: 'newpassword',
        })
      ).toBe(true);
    });
  });

  describe('validateUserAccount function', () => {
    const baseData = {
      userId: 'u',
      firstName: '',
      lastName: '',
      email: '',
      external: false,
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    };

    it('returns no errors when nothing is filled', () => {
      expect(validateUserAccount(baseData)).toEqual({});
    });

    it('validates only when a password field is filled', () => {
      expect(
        validateUserAccount({ ...baseData, currentPassword: 'old' })
      ).toEqual({});
      expect(
        validateUserAccount({ ...baseData, newPassword: 'short' })
      ).toEqual({
        newPassword: expect.stringMatching(/at least.*8/i),
        confirmPassword: expect.stringMatching(/do not match/i),
      });
    });
  });

  describe('save flow', () => {
    it('calls PUT change-password with new password as text/plain', async () => {
      restClient.put.mockResolvedValue(undefined);
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'currentPassword', value: 'oldpass1' } as any);
      service.send({ type: 'UPDATE', name: 'newPassword', value: 'newpassword' } as any);
      service.send({ type: 'UPDATE', name: 'confirmPassword', value: 'newpassword' } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);

      expect(restClient.put).toHaveBeenCalledWith(
        '/service/rest/v1/security/users/testuser/change-password',
        'newpassword',
        { headers: { 'Content-Type': 'text/plain' } }
      );

      service.stop();
    });

    it('surfaces save error when API fails', async () => {
      restClient.put.mockRejectedValue(new Error('Change failed'));
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'currentPassword', value: 'oldpass1' } as any);
      service.send({ type: 'UPDATE', name: 'newPassword', value: 'newpassword' } as any);
      service.send({ type: 'UPDATE', name: 'confirmPassword', value: 'newpassword' } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('editing') && state.context.saveError !== null);
      expect(service.getSnapshot().context.saveError).toBeTruthy();

      service.stop();
    });
  });

  describe('reset', () => {
    it('clears password fields after RESET', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'currentPassword', value: 'anything' } as any);
      service.send({ type: 'UPDATE', name: 'newPassword', value: 'newpassword' } as any);
      service.send({ type: 'RESET' } as any);

      const { data } = service.getSnapshot().context;
      expect(data.currentPassword).toBe('');
      expect(data.newPassword).toBe('');
      expect(data.confirmPassword).toBe('');

      service.stop();
    });
  });
});
