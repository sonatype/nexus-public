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
import { createUsersFormMachine } from '../usersFormMachine';

// Mock the local API module that usersFormMachine imports from
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn().mockResolvedValue([]),
  },
  ENDPOINTS: {
    USERS: '/service/rest/v1/security/users',
    ROLES: '/service/rest/v1/security/roles',
    USER_SOURCES: '/service/rest/v1/security/user-sources',
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createUsersFormMachine>,
  loadData?: Record<string, unknown>
) {
  // Mock the API responses for the load service
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('user-sources')) {
      return Promise.resolve(['LDAP', 'Crowd']);
    }
    if (url.includes('roles')) {
      return Promise.resolve([
        { id: 'nx-admin', name: 'nx-admin' },
        { id: 'nx-anonymous', name: 'nx-anonymous' },
      ]);
    }
    if (url.includes('users')) {
      return Promise.resolve(loadData?.users || []);
    }
    return Promise.resolve(loadData || []);
  });

  const service = interpret(machine).start();

  // Wait for loading to complete
  await waitFor(service, (state) => state.matches('editing'));

  return service;
}

describe('usersFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to local sub-state in create mode', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.source).toBe('default');
      expect(state.matches({ editing: 'local' })).toBe(true);

      service.stop();
    });

    it('initializes with empty form data in create mode', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.userId).toBe('');
      expect(state.context.data.firstName).toBe('');
      expect(state.context.data.lastName).toBe('');
      expect(state.context.data.emailAddress).toBe('');
      expect(state.context.data.password).toBe('');
      expect(state.context.data.status).toBe(true);
      expect(state.context.data.roles).toEqual([]);

      service.stop();
    });

    it('loads roles and user sources on start', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.allRoles).toHaveLength(2);
      // userSources includes the auto-added 'Local' entry plus fetched sources
      expect(state.context.userSources).toHaveLength(3);
      expect(state.context.userSources[0]).toEqual({ id: 'default', name: 'Local' });

      service.stop();
    });

    it('has null user reference in create mode', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.user).toBeNull();

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads local user data from preloaded user', async () => {
      const preloadedUser = {
        userId: 'admin',
        realm: 'default',
        source: 'default',
        firstName: 'Admin',
        lastName: 'User',
        emailAddress: 'admin@example.com',
        status: 'active' as const,
        roles: ['nx-admin'],
      };

      const machine = createUsersFormMachine('admin', 'default', preloadedUser);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'local' })).toBe(true);
      expect(state.context.data.userId).toBe('admin');
      expect(state.context.data.firstName).toBe('Admin');
      expect(state.context.data.lastName).toBe('User');
      expect(state.context.data.emailAddress).toBe('admin@example.com');
      expect(state.context.data.status).toBe(true);
      expect(state.context.data.roles).toEqual(['nx-admin']);
      expect(state.context.user).toEqual(preloadedUser);

      service.stop();
    });

    it('loads external user and enters external sub-state', async () => {
      const preloadedUser = {
        userId: 'ldap-user',
        realm: 'LDAP',
        source: 'LDAP',
        firstName: 'LDAP',
        lastName: 'User',
        emailAddress: 'ldap@example.com',
        status: 'active' as const,
        roles: ['nx-anonymous'],
        externalRoles: ['ldap-admin'],
      };

      const machine = createUsersFormMachine('ldap-user', 'LDAP', preloadedUser);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'external' })).toBe(true);
      expect(state.context.data.userId).toBe('ldap-user');
      expect(state.context.data.source).toBe('LDAP');

      service.stop();
    });
  });

  describe('source variant sub-states', () => {
    it('transitions from local to external on SOURCE_CHANGE', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Start in local sub-state
      expect(service.getSnapshot().matches({ editing: 'local' })).toBe(true);

      // Switch to LDAP (external)
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'external' })).toBe(true);
      expect(state.context.data.source).toBe('LDAP');

      service.stop();
    });

    it('transitions from external back to local on SOURCE_CHANGE', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to external
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);
      expect(service.getSnapshot().matches({ editing: 'external' })).toBe(true);

      // Switch back to local
      service.send({ type: 'SOURCE_CHANGE', value: 'default' } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'local' })).toBe(true);
      expect(state.context.data.source).toBe('default');

      service.stop();
    });

    it('clears local-only fields when switching to external source', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Fill in local fields
      service.send({ type: 'UPDATE', name: 'firstName', value: 'John' } as any);
      service.send({ type: 'UPDATE', name: 'lastName', value: 'Doe' } as any);
      service.send({ type: 'UPDATE', name: 'emailAddress', value: 'john@example.com' } as any);
      service.send({ type: 'UPDATE', name: 'password', value: 'secret123' } as any);

      expect(service.getSnapshot().context.data.firstName).toBe('John');

      // Switch to external source
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);

      const state = service.getSnapshot();
      // Local-only fields should be cleared
      expect(state.context.data.firstName).toBe('');
      expect(state.context.data.lastName).toBe('');
      expect(state.context.data.emailAddress).toBe('');
      expect(state.context.data.password).toBe('');
      expect(state.context.data.passwordConfirm).toBe('');

      service.stop();
    });

    it('transitions between multiple external sources', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to LDAP
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);
      expect(service.getSnapshot().matches({ editing: 'external' })).toBe(true);
      expect(service.getSnapshot().context.data.source).toBe('LDAP');

      // Switch to Crowd (another external source)
      service.send({ type: 'SOURCE_CHANGE', value: 'Crowd' } as any);
      expect(service.getSnapshot().matches({ editing: 'external' })).toBe(true);
      expect(service.getSnapshot().context.data.source).toBe('Crowd');

      service.stop();
    });
  });

  describe('sub-state metadata', () => {
    it('local sub-state has correct field metadata', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Local');
      expect(typeMeta.fields).toContain('userId');
      expect(typeMeta.fields).toContain('firstName');
      expect(typeMeta.fields).toContain('lastName');
      expect(typeMeta.fields).toContain('emailAddress');
      expect(typeMeta.fields).toContain('password');
      expect(typeMeta.fields).toContain('passwordConfirm');
      expect(typeMeta.fields).toContain('status');
      expect(typeMeta.fields).toContain('roles');
      expect(typeMeta.requiredFields).toContain('userId');
      expect(typeMeta.requiredFields).toContain('firstName');
      expect(typeMeta.requiredFields).toContain('lastName');
      expect(typeMeta.requiredFields).toContain('emailAddress');

      service.stop();
    });

    it('external sub-state has reduced field metadata', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to external
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('External');
      expect(typeMeta.fields).toEqual(['userId', 'status', 'roles']);
      expect(typeMeta.requiredFields).toEqual(['userId']);
      // Should NOT have local-only fields
      expect(typeMeta.fields).not.toContain('firstName');
      expect(typeMeta.fields).not.toContain('lastName');
      expect(typeMeta.fields).not.toContain('emailAddress');
      expect(typeMeta.fields).not.toContain('password');

      service.stop();
    });

    it('both sub-states have fields and requiredFields', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Check local
      let state = service.getSnapshot();
      let metaValues = Object.values(state.meta);
      let typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBeTruthy();
      expect(Array.isArray(typeMeta.fields)).toBe(true);
      expect(typeMeta.fields.length).toBeGreaterThan(0);
      expect(Array.isArray(typeMeta.requiredFields)).toBe(true);

      // Check external
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);

      state = service.getSnapshot();
      metaValues = Object.values(state.meta);
      typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBeTruthy();
      expect(Array.isArray(typeMeta.fields)).toBe(true);
      expect(typeMeta.fields.length).toBeGreaterThan(0);
      expect(Array.isArray(typeMeta.requiredFields)).toBe(true);

      service.stop();
    });
  });

  describe('validation - local users', () => {
    it('validates userId is required', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.userId).toBeTruthy();

      service.stop();
    });

    it('validates firstName is required for local users', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'test-user' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.firstName).toBeTruthy();

      service.stop();
    });

    it('validates lastName is required for local users', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'test-user' } as any);
      service.send({ type: 'UPDATE', name: 'firstName', value: 'Test' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.lastName).toBeTruthy();

      service.stop();
    });

    it('validates email is required and properly formatted for local users', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'test-user' } as any);
      service.send({ type: 'UPDATE', name: 'firstName', value: 'Test' } as any);
      service.send({ type: 'UPDATE', name: 'lastName', value: 'User' } as any);
      service.send({ type: 'UPDATE', name: 'emailAddress', value: 'invalid-email' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.emailAddress).toBeTruthy();

      service.stop();
    });

    it('validates password is required for local user creation', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'test-user' } as any);
      service.send({ type: 'UPDATE', name: 'firstName', value: 'Test' } as any);
      service.send({ type: 'UPDATE', name: 'lastName', value: 'User' } as any);
      service.send({ type: 'UPDATE', name: 'emailAddress', value: 'test@example.com' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.password).toBeTruthy();

      service.stop();
    });

    it('validates password confirmation must match', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'test-user' } as any);
      service.send({ type: 'UPDATE', name: 'firstName', value: 'Test' } as any);
      service.send({ type: 'UPDATE', name: 'lastName', value: 'User' } as any);
      service.send({ type: 'UPDATE', name: 'emailAddress', value: 'test@example.com' } as any);
      service.send({ type: 'UPDATE', name: 'password', value: 'secret123' } as any);
      service.send({ type: 'UPDATE', name: 'passwordConfirm', value: 'different' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.passwordConfirm).toContain('do not match');

      service.stop();
    });

    it('validates at least one role must be assigned', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'test-user' } as any);
      service.send({ type: 'UPDATE', name: 'firstName', value: 'Test' } as any);
      service.send({ type: 'UPDATE', name: 'lastName', value: 'User' } as any);
      service.send({ type: 'UPDATE', name: 'emailAddress', value: 'test@example.com' } as any);
      service.send({ type: 'UPDATE', name: 'password', value: 'secret123' } as any);
      service.send({ type: 'UPDATE', name: 'passwordConfirm', value: 'secret123' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.roles).toBeTruthy();

      service.stop();
    });
  });

  describe('validation - external users', () => {
    it('does NOT require firstName for external users', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to external
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);

      service.send({ type: 'UPDATE', name: 'userId', value: 'ldap-user' } as any);
      service.send({ type: 'UPDATE', name: 'roles', value: ['nx-admin'] } as any);

      const state = service.getSnapshot();
      // External users don't require firstName, lastName, email, password
      expect(state.context.validationErrors.firstName).toBeFalsy();
      expect(state.context.validationErrors.lastName).toBeFalsy();
      expect(state.context.validationErrors.emailAddress).toBeFalsy();
      expect(state.context.validationErrors.password).toBeFalsy();

      service.stop();
    });

    it('still requires userId for external users', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to external
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.userId).toBeTruthy();

      service.stop();
    });

    it('still requires at least one role for external users', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to external
      service.send({ type: 'SOURCE_CHANGE', value: 'LDAP' } as any);
      service.send({ type: 'UPDATE', name: 'userId', value: 'ldap-user' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.roles).toBeTruthy();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates fields via UPDATE event', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'new-user' } as any);
      service.send({ type: 'UPDATE', name: 'firstName', value: 'New' } as any);
      service.send({ type: 'UPDATE', name: 'lastName', value: 'User' } as any);
      service.send({ type: 'UPDATE', name: 'emailAddress', value: 'new@example.com' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.userId).toBe('new-user');
      expect(state.context.data.firstName).toBe('New');
      expect(state.context.data.lastName).toBe('User');
      expect(state.context.data.emailAddress).toBe('new@example.com');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'userId', value: 'new-user' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'userId', value: 'new-user' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.userId).toBe('');

      service.stop();
    });

    it('updates boolean fields (status)', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'status', value: false } as any);

      expect(service.getSnapshot().context.data.status).toBe(false);

      service.stop();
    });

    it('updates array fields (roles)', async () => {
      const machine = createUsersFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'roles', value: ['nx-admin', 'nx-anonymous'] } as any);

      expect(service.getSnapshot().context.data.roles).toEqual(['nx-admin', 'nx-anonymous']);

      service.stop();
    });
  });

  describe('error handling', () => {
    it('handles load errors gracefully for reference data', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('roles')) {
          return Promise.reject(new Error('Network error'));
        }
        if (url.includes('user-sources')) {
          return Promise.resolve([]);
        }
        return Promise.resolve([]);
      });

      const machine = createUsersFormMachine(undefined);
      const service = interpret(machine).start();

      // Should still reach editing state (reference data failures are non-fatal)
      await waitFor(service, (state) => state.matches('editing'));

      const state = service.getSnapshot();
      expect(state.context.allRoles).toEqual([]);

      service.stop();
    });

    it('transitions to loadError when user fetch fails', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('users')) {
          return Promise.reject(new Error('Not found'));
        }
        if (url.includes('roles')) {
          return Promise.resolve([]);
        }
        if (url.includes('user-sources')) {
          return Promise.resolve([]);
        }
        return Promise.resolve([]);
      });

      const machine = createUsersFormMachine('missing-user', 'default');
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));

      expect(service.getSnapshot().matches('loadError')).toBe(true);

      service.stop();
    });
  });
});
