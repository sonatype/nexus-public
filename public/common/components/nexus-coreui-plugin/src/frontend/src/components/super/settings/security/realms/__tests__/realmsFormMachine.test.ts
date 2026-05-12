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
import { createRealmsFormMachine, validateRealms } from '../realmsFormMachine';

const MOCK_AVAILABLE_REALMS = [
  { id: 'NexusAuthenticatingRealm', name: 'Local Authenticating Realm' },
  { id: 'NexusAuthorizingRealm', name: 'Local Authorizing Realm' },
  { id: 'LdapRealm', name: 'LDAP Realm' },
  { id: 'DockerToken', name: 'Docker Bearer Token Realm' },
  { id: 'NpmToken', name: 'npm Bearer Token Realm' },
  { id: 'NuGetApiKey', name: 'NuGet API-Key Realm' },
];

const MOCK_ACTIVE_REALM_IDS = [
  'NexusAuthenticatingRealm',
  'NexusAuthorizingRealm',
  'DockerToken',
];

/**
 * Helper: start a machine with mock services and wait for editing
 */
async function startAndLoad(
  availableRealms = MOCK_AVAILABLE_REALMS,
  activeRealmIds = MOCK_ACTIVE_REALM_IDS
) {
  const machine = createRealmsFormMachine();
  const service = interpret(
    machine.withConfig({
      services: {
        load: async () => ({ availableRealms, activeRealmIds }),
        save: async () => {},
      },
    })
  ).start();

  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('realmsFormMachine', () => {
  describe('validateRealms', () => {
    it('returns error when no active realms', () => {
      expect(validateRealms([])).toBe('At least one active realm is required');
    });

    it('returns null when at least one realm is active', () => {
      expect(validateRealms([{ id: 'test', name: 'Test' }])).toBeNull();
    });
  });

  describe('loading', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createRealmsFormMachine();
      const service = interpret(
        machine.withConfig({
          services: {
            load: async () => ({
              availableRealms: MOCK_AVAILABLE_REALMS,
              activeRealmIds: MOCK_ACTIVE_REALM_IDS,
            }),
            save: async () => {},
          },
        })
      ).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('loads available and active realms', async () => {
      const service = await startAndLoad();
      const ctx = service.getSnapshot().context;

      expect(ctx.availableRealms).toHaveLength(6);
      expect(ctx.activeRealms).toHaveLength(3);
      expect(ctx.activeRealms[0].id).toBe('NexusAuthenticatingRealm');
      expect(ctx.activeRealms[1].id).toBe('NexusAuthorizingRealm');
      expect(ctx.activeRealms[2].id).toBe('DockerToken');

      service.stop();
    });

    it('starts pristine after loading', async () => {
      const service = await startAndLoad();

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.stop();
    });

    it('transitions to loadError on failure', async () => {
      const machine = createRealmsFormMachine();
      const service = interpret(
        machine.withConfig({
          services: {
            load: async () => { throw new Error('Network error'); },
            save: async () => {},
          },
        })
      ).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().context.loadError).toBeTruthy();

      service.stop();
    });

    it('retries loading on RETRY event', async () => {
      let attempt = 0;
      const machine = createRealmsFormMachine();
      const service = interpret(
        machine.withConfig({
          services: {
            load: async () => {
              attempt++;
              if (attempt === 1) throw new Error('Network error');
              return {
                availableRealms: MOCK_AVAILABLE_REALMS,
                activeRealmIds: MOCK_ACTIVE_REALM_IDS,
              };
            },
            save: async () => {},
          },
        })
      ).start();

      await waitFor(service, (state) => state.matches('loadError'));
      service.send({ type: 'RETRY' });

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().context.activeRealms).toHaveLength(3);

      service.stop();
    });
  });

  describe('add realm', () => {
    it('adds a realm to active list', async () => {
      const service = await startAndLoad();

      service.send({
        type: 'ADD_REALM',
        realm: { id: 'LdapRealm', name: 'LDAP Realm' },
      });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms).toHaveLength(4);
      expect(ctx.activeRealms[3].id).toBe('LdapRealm');
      expect(ctx.isPristine).toBe(false);

      service.stop();
    });

    it('does not add duplicate realm', async () => {
      const service = await startAndLoad();

      service.send({
        type: 'ADD_REALM',
        realm: { id: 'NexusAuthenticatingRealm', name: 'Local Authenticating Realm' },
      });

      expect(service.getSnapshot().context.activeRealms).toHaveLength(3);

      service.stop();
    });
  });

  describe('remove realm', () => {
    it('removes a realm from active list', async () => {
      const service = await startAndLoad();

      service.send({ type: 'REMOVE_REALM', realmId: 'DockerToken' });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms).toHaveLength(2);
      expect(ctx.activeRealms.find((r) => r.id === 'DockerToken')).toBeUndefined();
      expect(ctx.isPristine).toBe(false);

      service.stop();
    });
  });

  describe('reorder', () => {
    it('reorders active realms via REORDER event', async () => {
      const service = await startAndLoad();

      const reversed = [...service.getSnapshot().context.activeRealms].reverse();
      service.send({ type: 'REORDER', activeRealms: reversed });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms[0].id).toBe('DockerToken');
      expect(ctx.activeRealms[2].id).toBe('NexusAuthenticatingRealm');
      expect(ctx.isPristine).toBe(false);

      service.stop();
    });

    it('moves a realm up', async () => {
      const service = await startAndLoad();

      service.send({ type: 'MOVE_UP', realmId: 'DockerToken' });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms[1].id).toBe('DockerToken');
      expect(ctx.activeRealms[2].id).toBe('NexusAuthorizingRealm');

      service.stop();
    });

    it('does not move the first realm up', async () => {
      const service = await startAndLoad();

      service.send({ type: 'MOVE_UP', realmId: 'NexusAuthenticatingRealm' });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms[0].id).toBe('NexusAuthenticatingRealm');

      service.stop();
    });

    it('moves a realm down', async () => {
      const service = await startAndLoad();

      service.send({ type: 'MOVE_DOWN', realmId: 'NexusAuthenticatingRealm' });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms[0].id).toBe('NexusAuthorizingRealm');
      expect(ctx.activeRealms[1].id).toBe('NexusAuthenticatingRealm');

      service.stop();
    });

    it('does not move the last realm down', async () => {
      const service = await startAndLoad();

      service.send({ type: 'MOVE_DOWN', realmId: 'DockerToken' });

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms[2].id).toBe('DockerToken');

      service.stop();
    });
  });

  describe('validation', () => {
    it('blocks submit when no active realms', async () => {
      const service = await startAndLoad();

      // Remove all active realms
      service.send({ type: 'REMOVE_REALM', realmId: 'NexusAuthenticatingRealm' });
      service.send({ type: 'REMOVE_REALM', realmId: 'NexusAuthorizingRealm' });
      service.send({ type: 'REMOVE_REALM', realmId: 'DockerToken' });

      service.send({ type: 'SUBMIT' });

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationError).toBe('At least one active realm is required');

      service.stop();
    });

    it('allows submit with at least one active realm', async () => {
      const service = await startAndLoad();

      service.send({ type: 'SUBMIT' });

      await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);

      service.stop();
    });
  });

  describe('discard', () => {
    it('reverts to pristine state on DISCARD', async () => {
      const service = await startAndLoad();

      // Make changes
      service.send({
        type: 'ADD_REALM',
        realm: { id: 'LdapRealm', name: 'LDAP Realm' },
      });
      expect(service.getSnapshot().context.isPristine).toBe(false);
      expect(service.getSnapshot().context.activeRealms).toHaveLength(4);

      // Discard
      service.send({ type: 'DISCARD' });

      const ctx = service.getSnapshot().context;
      expect(ctx.isPristine).toBe(true);
      expect(ctx.activeRealms).toHaveLength(3);

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions through saving and back to editing', async () => {
      const service = await startAndLoad();

      // Make a change to make form dirty
      service.send({
        type: 'ADD_REALM',
        realm: { id: 'LdapRealm', name: 'LDAP Realm' },
      });

      service.send({ type: 'SUBMIT' });

      // After save, should be back in editing and pristine
      await waitFor(service, (state) =>
        state.matches('editing') && state.context.isPristine
      );

      const ctx = service.getSnapshot().context;
      expect(ctx.activeRealms).toHaveLength(4);
      expect(ctx.pristineActiveRealms).toHaveLength(4);

      service.stop();
    });

    it('returns to editing with error on save failure', async () => {
      const machine = createRealmsFormMachine();
      const service = interpret(
        machine.withConfig({
          services: {
            load: async () => ({
              availableRealms: MOCK_AVAILABLE_REALMS,
              activeRealmIds: MOCK_ACTIVE_REALM_IDS,
            }),
            save: async () => { throw new Error('Save failed'); },
          },
        })
      ).start();

      await waitFor(service, (state) => state.matches('editing'));

      service.send({
        type: 'ADD_REALM',
        realm: { id: 'LdapRealm', name: 'LDAP Realm' },
      });

      service.send({ type: 'SUBMIT' });

      await waitFor(service, (state) =>
        state.matches('editing') && state.context.saveError !== null
      );

      expect(service.getSnapshot().context.saveError).toBe('Save failed');

      service.stop();
    });
  });
});
