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
import { createAnonymousFormMachine } from '../anonymousFormMachine';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    APIConstants: {
      REST: {
        INTERNAL: {
          ANONYMOUS_SETTINGS: '/service/rest/internal/ui/anonymous-settings',
          REALMS_TYPES: '/service/rest/internal/ui/realms/types',
        },
      },
    },
    restClient: {
      get: jest.fn().mockResolvedValue([]),
      put: jest.fn().mockResolvedValue({}),
    },
  };
});

const { restClient } = jest.requireMock('@sonatype/nexus-ui-plugin');

const MOCK_SETTINGS = {
  enabled: true,
  userId: 'anonymous',
  realmName: 'NexusAuthorizingRealm',
};

const MOCK_REALM_TYPES = [
  { id: 'NexusAuthorizingRealm', name: 'Local Authorizing Realm' },
  { id: 'LdapRealm', name: 'LDAP Realm' },
];

/**
 * Helper: start machine and wait for it to reach editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createAnonymousFormMachine>,
  settings = MOCK_SETTINGS,
  realmTypes = MOCK_REALM_TYPES
) {
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('anonymous-settings')) {
      return Promise.resolve(settings);
    }
    if (url.includes('realms/types')) {
      return Promise.resolve(realmTypes);
    }
    return Promise.resolve([]);
  });

  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('anonymousFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createAnonymousFormMachine();
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      // Mock API responses
      restClient.get.mockResolvedValue(MOCK_SETTINGS);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('loads settings and realm types into context', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.enabled).toBe(true);
      expect(state.context.data.userId).toBe('anonymous');
      expect(state.context.data.realmName).toBe('NexusAuthorizingRealm');
      expect(state.context.realmTypes).toEqual(MOCK_REALM_TYPES);

      service.stop();
    });

    it('transitions to loadError on API failure', async () => {
      restClient.get.mockRejectedValue(new Error('Network error'));

      const machine = createAnonymousFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().matches('loadError')).toBe(true);

      service.stop();
    });

    it('retries loading on RETRY event', async () => {
      restClient.get.mockRejectedValueOnce(new Error('Network error'));

      const machine = createAnonymousFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));

      // Now mock successful response and retry
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('anonymous-settings')) return Promise.resolve(MOCK_SETTINGS);
        if (url.includes('realms/types')) return Promise.resolve(MOCK_REALM_TYPES);
        return Promise.resolve([]);
      });

      service.send({ type: 'RETRY' } as any);
      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('still loads settings even if realm types fail', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('anonymous-settings')) return Promise.resolve(MOCK_SETTINGS);
        if (url.includes('realms/types')) return Promise.reject(new Error('Realm types failed'));
        return Promise.resolve([]);
      });

      const machine = createAnonymousFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('editing'));

      const state = service.getSnapshot();
      expect(state.context.data.enabled).toBe(true);
      expect(state.context.realmTypes).toEqual([]);

      service.stop();
    });
  });

  describe('validation', () => {
    it('validates userId is required', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.userId).toBe('Username is required');

      service.stop();
    });

    it('validates userId whitespace-only is invalid', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: '   ' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.userId).toBeTruthy();

      service.stop();
    });

    it('validates realmName is required', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'realmName', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.realmName).toBe('Realm is required');

      service.stop();
    });

    it('passes validation with valid data', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      // Data is already valid from load, just make a change to dirty the form
      service.send({ type: 'UPDATE', name: 'enabled', value: false } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userId).toBeFalsy();
      expect(state.context.validationErrors.realmName).toBeFalsy();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates enabled field', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'enabled', value: false } as any);

      expect(service.getSnapshot().context.data.enabled).toBe(false);

      service.stop();
    });

    it('updates userId field', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'anon-user' } as any);

      expect(service.getSnapshot().context.data.userId).toBe('anon-user');

      service.stop();
    });

    it('updates realmName field', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'realmName', value: 'LdapRealm' } as any);

      expect(service.getSnapshot().context.data.realmName).toBe('LdapRealm');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'userId', value: 'new-user' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'new-user' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.userId).toBe('anonymous');

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions to saving on valid SUBMIT', async () => {
      const machine = createAnonymousFormMachine();
      const service = await startAndLoad(machine);

      // Dirty the form
      service.send({ type: 'UPDATE', name: 'enabled', value: false } as any);

      // SUBMIT → validating → saving (since no errors)
      service.send({ type: 'SUBMIT' } as any);

      // Should be in saving or saved (depending on mock timing)
      const state = service.getSnapshot();
      expect(
        state.matches('saving') || state.matches('saved') || state.matches('validating')
      ).toBe(true);

      service.stop();
    });
  });
});
