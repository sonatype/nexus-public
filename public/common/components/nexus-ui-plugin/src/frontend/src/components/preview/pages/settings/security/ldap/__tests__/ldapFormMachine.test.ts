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
import { createLdapFormMachine, validateLdap } from '../ldapFormMachine';
import { DEFAULT_LDAP_SERVER, type LdapFormData, type LdapServer } from '../types';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
  };
});

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(machine: ReturnType<typeof createLdapFormMachine>) {
  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

/**
 * Build a complete valid LDAP form data object for testing
 */
function validLdapFormData(overrides: Partial<LdapFormData> = {}): LdapFormData {
  return {
    ...DEFAULT_LDAP_SERVER,
    name: 'test-ldap',
    host: 'ldap.example.com',
    port: 389,
    searchBase: 'dc=example,dc=com',
    authScheme: 'simple',
    authUsername: 'cn=admin,dc=example,dc=com',
    authPassword: 'secret',
    userObjectClass: 'inetOrgPerson',
    userIdAttribute: 'uid',
    userRealNameAttribute: 'cn',
    userEmailAddressAttribute: 'mail',
    ...overrides,
  };
}

/**
 * Build a preloaded LdapServer for edit mode tests
 */
function buildPreloadedServer(overrides: Partial<LdapServer> = {}): LdapServer {
  return {
    id: 'test-server-id',
    order: 1,
    name: 'production-ldap',
    protocol: 'ldaps',
    useTrustStore: true,
    host: 'ldap.prod.example.com',
    port: 636,
    searchBase: 'dc=prod,dc=example,dc=com',
    authScheme: 'simple',
    authUsername: 'cn=svc,dc=prod,dc=example,dc=com',
    authPassword: '',
    connectionTimeout: 30,
    connectionRetryDelay: 300,
    maxIncidentsCount: 3,
    userBaseDn: 'ou=users',
    userSubtree: true,
    userObjectClass: 'person',
    userIdAttribute: 'sAMAccountName',
    userRealNameAttribute: 'displayName',
    userEmailAddressAttribute: 'mail',
    ldapGroupsAsRoles: true,
    groupType: 'static',
    groupBaseDn: 'ou=groups',
    groupSubtree: false,
    groupObjectClass: 'group',
    groupIdAttribute: 'cn',
    groupMemberAttribute: 'member',
    groupMemberFormat: '${dn}',
    ...overrides,
  };
}

describe('ldapFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('initializes with default LDAP server values', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('');
      expect(state.context.data.protocol).toBe('ldap');
      expect(state.context.data.port).toBe(389);
      expect(state.context.data.authScheme).toBe('simple');
      expect(state.context.data.userObjectClass).toBe('inetOrgPerson');

      service.stop();
    });

    it('sets server to null in create mode', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.server).toBeNull();

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads preloaded server data into form', async () => {
      const server = buildPreloadedServer();
      const machine = createLdapFormMachine('test-server-id', server);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('production-ldap');
      expect(state.context.data.protocol).toBe('ldaps');
      expect(state.context.data.port).toBe(636);
      expect(state.context.data.host).toBe('ldap.prod.example.com');
      expect(state.context.data.searchBase).toBe('dc=prod,dc=example,dc=com');
      expect(state.context.data.userObjectClass).toBe('person');
      expect(state.context.data.ldapGroupsAsRoles).toBe(true);
      expect(state.context.data.groupType).toBe('static');

      service.stop();
    });

    it('preserves server reference in context', async () => {
      const server = buildPreloadedServer();
      const machine = createLdapFormMachine('test-server-id', server);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.server).not.toBeNull();
      expect(state.context.server?.name).toBe('production-ldap');

      service.stop();
    });
  });

  describe('connection validation', () => {
    it('validates name is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates host is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.host).toBeTruthy();

      service.stop();
    });

    it('validates port range (1-65535)', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'port', value: 0 } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.port).toBeTruthy();

      service.stop();
    });

    it('validates search base DN is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test' } as any);
      service.send({ type: 'UPDATE', name: 'host', value: 'ldap.example.com' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.searchBase).toBeTruthy();

      service.stop();
    });

    it('validates auth credentials when scheme is not none', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      // authScheme defaults to 'simple', so credentials are required
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.authUsername).toBeTruthy();
      expect(state.context.validationErrors.authPassword).toBeTruthy();

      service.stop();
    });

    it('does not require auth credentials when scheme is none', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'authScheme', value: 'none' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.authUsername).toBeFalsy();
      expect(state.context.validationErrors.authPassword).toBeFalsy();

      service.stop();
    });
  });

  describe('user mapping validation', () => {
    it('validates user object class is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userObjectClass', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userObjectClass).toBeTruthy();

      service.stop();
    });

    it('validates user ID attribute is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userIdAttribute', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userIdAttribute).toBeTruthy();

      service.stop();
    });

    it('validates real name attribute is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userRealNameAttribute', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userRealNameAttribute).toBeTruthy();

      service.stop();
    });

    it('validates email attribute is required', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userEmailAddressAttribute', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userEmailAddressAttribute).toBeTruthy();

      service.stop();
    });
  });

  describe('group mapping validation', () => {
    it('validates static group fields when ldapGroupsAsRoles is true', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'ldapGroupsAsRoles', value: true } as any);
      service.send({ type: 'UPDATE', name: 'groupType', value: 'static' } as any);
      service.send({ type: 'UPDATE', name: 'groupObjectClass', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'groupIdAttribute', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'groupMemberAttribute', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'groupMemberFormat', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.groupObjectClass).toBeTruthy();
      expect(state.context.validationErrors.groupIdAttribute).toBeTruthy();
      expect(state.context.validationErrors.groupMemberAttribute).toBeTruthy();
      expect(state.context.validationErrors.groupMemberFormat).toBeTruthy();

      service.stop();
    });

    it('validates dynamic group fields when ldapGroupsAsRoles is true', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'ldapGroupsAsRoles', value: true } as any);
      service.send({ type: 'UPDATE', name: 'groupType', value: 'dynamic' } as any);
      service.send({ type: 'UPDATE', name: 'userMemberOfAttribute', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userMemberOfAttribute).toBeTruthy();

      service.stop();
    });

    it('does not require group fields when ldapGroupsAsRoles is false', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      // ldapGroupsAsRoles defaults to false
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.groupObjectClass).toBeFalsy();
      expect(state.context.validationErrors.groupIdAttribute).toBeFalsy();
      expect(state.context.validationErrors.groupMemberAttribute).toBeFalsy();
      expect(state.context.validationErrors.userMemberOfAttribute).toBeFalsy();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates fields via UPDATE event', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-ldap' } as any);

      expect(service.getSnapshot().context.data.name).toBe('my-ldap');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-server' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'changed' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });
  });

  describe('PROTOCOL_CHANGE event', () => {
    it('updates port to 636 when switching to ldaps from default port', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Default port is 389
      expect(service.getSnapshot().context.data.port).toBe(389);

      service.send({ type: 'PROTOCOL_CHANGE', value: 'ldaps' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.protocol).toBe('ldaps');
      expect(state.context.data.port).toBe(636);

      service.stop();
    });

    it('updates port to 389 when switching to ldap from SSL port', async () => {
      const server = buildPreloadedServer({ port: 636, protocol: 'ldaps' });
      const machine = createLdapFormMachine('test-id', server);
      const service = await startAndLoad(machine);

      service.send({ type: 'PROTOCOL_CHANGE', value: 'ldap' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.protocol).toBe('ldap');
      expect(state.context.data.port).toBe(389);

      service.stop();
    });

    it('preserves custom port when switching protocols', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set a custom port
      service.send({ type: 'UPDATE', name: 'port', value: 10389 } as any);
      expect(service.getSnapshot().context.data.port).toBe(10389);

      // Switch to ldaps - custom port should not be changed
      service.send({ type: 'PROTOCOL_CHANGE', value: 'ldaps' } as any);
      expect(service.getSnapshot().context.data.port).toBe(10389);

      service.stop();
    });
  });

  describe('APPLY_TEMPLATE event', () => {
    it('applies template values to user/group mapping fields', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      const template = {
        name: 'Active Directory',
        userObjectClass: 'user',
        userIdAttribute: 'sAMAccountName',
        userRealNameAttribute: 'displayName',
        userEmailAddressAttribute: 'mail',
        ldapGroupsAsRoles: true,
        groupType: 'static' as const,
        groupObjectClass: 'group',
        groupIdAttribute: 'cn',
        groupMemberAttribute: 'member',
        groupMemberFormat: '${dn}',
      };

      service.send({ type: 'APPLY_TEMPLATE', template } as any);

      const state = service.getSnapshot();
      expect(state.context.data.userObjectClass).toBe('user');
      expect(state.context.data.userIdAttribute).toBe('sAMAccountName');
      expect(state.context.data.ldapGroupsAsRoles).toBe(true);
      expect(state.context.data.groupObjectClass).toBe('group');

      service.stop();
    });

    it('preserves connection fields when applying template', async () => {
      const machine = createLdapFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set connection fields
      service.send({ type: 'UPDATE', name: 'name', value: 'my-server' } as any);
      service.send({ type: 'UPDATE', name: 'host', value: 'ldap.example.com' } as any);

      const template = {
        name: 'OpenLDAP',
        userObjectClass: 'posixAccount',
        userIdAttribute: 'uid',
      };

      service.send({ type: 'APPLY_TEMPLATE', template } as any);

      const state = service.getSnapshot();
      // Connection fields should be preserved
      expect(state.context.data.name).toBe('my-server');
      expect(state.context.data.host).toBe('ldap.example.com');
      // Template fields should be applied
      expect(state.context.data.userObjectClass).toBe('posixAccount');

      service.stop();
    });
  });

  describe('validateLdap function', () => {
    it('returns no errors for valid complete data', () => {
      const data = validLdapFormData();
      const errors = validateLdap(data);
      expect(Object.keys(errors).filter((k) => errors[k])).toHaveLength(0);
    });

    it('returns errors for empty form', () => {
      const errors = validateLdap(DEFAULT_LDAP_SERVER);
      expect(errors.name).toBeTruthy();
      expect(errors.host).toBeTruthy();
    });

    it('validates static group fields when groups as roles enabled', () => {
      const data = validLdapFormData({
        ldapGroupsAsRoles: true,
        groupType: 'static',
        groupObjectClass: '',
        groupIdAttribute: '',
        groupMemberAttribute: '',
        groupMemberFormat: '',
      });
      const errors = validateLdap(data);
      expect(errors.groupObjectClass).toBeTruthy();
      expect(errors.groupIdAttribute).toBeTruthy();
      expect(errors.groupMemberAttribute).toBeTruthy();
      expect(errors.groupMemberFormat).toBeTruthy();
    });

    it('validates dynamic group fields when groups as roles enabled', () => {
      const data = validLdapFormData({
        ldapGroupsAsRoles: true,
        groupType: 'dynamic',
        userMemberOfAttribute: '',
      });
      const errors = validateLdap(data);
      expect(errors.userMemberOfAttribute).toBeTruthy();
    });
  });
});
