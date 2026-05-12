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
import { createRolesFormMachine } from '../rolesFormMachine';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    ENDPOINTS: {
      PRIVILEGES: '/service/rest/v1/security/privileges',
      ROLES: '/service/rest/v1/security/roles',
      ROLE_SOURCES: '/service/rest/v1/security/roles/sources',
    },
    restClient: {
      get: jest.fn().mockResolvedValue([]),
    },
  };
});

const { restClient } = jest.requireMock('@sonatype/nexus-ui-plugin');

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createRolesFormMachine>,
  loadData?: Record<string, unknown>
) {
  // Mock the API responses for the load service
  restClient.get.mockImplementation((url: string) => {
    if (url?.endsWith('/privileges')) {
      return Promise.resolve([
        { name: 'nx-all' },
        { name: 'nx-repository-view-*-*-browse' },
        { name: 'nx-repository-view-*-*-read' },
      ]);
    }
    if (url?.endsWith('/sources')) {
      return Promise.resolve([
        { id: 'default', name: 'Default' },
        { id: 'LDAP', name: 'LDAP' },
      ]);
    }
    if (url?.endsWith('/roles')) {
      return Promise.resolve([
        { id: 'nx-admin', name: 'nx-admin', source: 'default', description: 'Admin role', readOnly: false, privileges: [], roles: [] },
        { id: 'nx-anonymous', name: 'nx-anonymous', source: 'default', description: 'Anonymous role', readOnly: false, privileges: [], roles: [] },
      ]);
    }
    return Promise.resolve(loadData || []);
  });

  const service = interpret(machine).start();

  // Wait for loading to complete
  await waitFor(service, (state) => state.matches('editing'));

  return service;
}

describe('rolesFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('initializes with empty form data in create mode', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.id).toBe('');
      expect(state.context.data.name).toBe('');
      expect(state.context.data.description).toBe('');
      expect(state.context.data.privileges).toEqual([]);
      expect(state.context.data.roles).toEqual([]);

      service.stop();
    });

    it('loads privilege and role references on start', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.allPrivileges).toHaveLength(3);
      expect(state.context.allPrivileges[0]).toEqual({ id: 'nx-all', name: 'nx-all' });
      expect(state.context.allRoles).toHaveLength(2);

      service.stop();
    });

    it('has null role reference in create mode', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.role).toBeNull();

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads role data from preloaded role', async () => {
      const preloadedRole = {
        id: 'nx-admin',
        version: '1',
        source: 'Default',
        name: 'nx-admin',
        description: 'Administrator role',
        readOnly: false,
        privileges: ['nx-all'],
        roles: [],
      };

      const machine = createRolesFormMachine('nx-admin', preloadedRole);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.id).toBe('nx-admin');
      expect(state.context.data.name).toBe('nx-admin');
      expect(state.context.data.description).toBe('Administrator role');
      expect(state.context.data.privileges).toEqual(['nx-all']);
      expect(state.context.role).toEqual(preloadedRole);

      service.stop();
    });

    it('filters out current role from available roles', async () => {
      const preloadedRole = {
        id: 'nx-admin',
        version: '1',
        source: 'Default',
        name: 'nx-admin',
        description: 'Admin role',
        readOnly: false,
        privileges: ['nx-all'],
        roles: [],
      };

      const machine = createRolesFormMachine('nx-admin', preloadedRole);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      // nx-admin should be filtered out from available roles
      const roleIds = state.context.allRoles.map((r: { id: string }) => r.id);
      expect(roleIds).not.toContain('nx-admin');
      expect(roleIds).toContain('nx-anonymous');

      service.stop();
    });
  });

  describe('validation', () => {
    it('validates role ID is required', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Try to submit with empty fields
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.id).toBeTruthy();

      service.stop();
    });

    it('validates role ID format (alphanumeric, hyphens, underscores)', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'invalid id!' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.id).toContain('letters, numbers');

      service.stop();
    });

    it('validates role name is required', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'test-role' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates at least one privilege or role must be assigned (P0 - blocks submit)', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'test-role' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test Role' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.privileges).toBe('Select at least one privilege or contained role');

      service.stop();
    });

    it('shows error when role name duplicates an existing role', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'new-role' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'nx-admin' } as any);
      service.send({ type: 'UPDATE', name: 'privileges', value: ['nx-all'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toContain('already exists');

      service.stop();
    });

    it('allows same name when editing the role itself', async () => {
      const preloadedRole = {
        id: 'nx-admin',
        version: '1',
        source: 'Default',
        name: 'nx-admin',
        description: 'Admin role',
        readOnly: false,
        privileges: ['nx-all'],
        roles: [],
      };

      const machine = createRolesFormMachine('nx-admin', preloadedRole);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toBeFalsy();

      service.stop();
    });

    it('passes validation with privileges assigned', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'test-role' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test Role' } as any);
      service.send({ type: 'UPDATE', name: 'privileges', value: ['nx-all'] } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.id).toBeFalsy();
      expect(state.context.validationErrors.name).toBeFalsy();
      expect(state.context.validationErrors.privileges).toBeFalsy();

      service.stop();
    });

    it('passes validation with contained roles assigned (no privileges)', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'test-role' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test Role' } as any);
      service.send({ type: 'UPDATE', name: 'roles', value: ['nx-admin'] } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.privileges).toBeFalsy();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates fields via UPDATE event', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'new-role' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'New Role' } as any);
      service.send({ type: 'UPDATE', name: 'description', value: 'A new role' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.id).toBe('new-role');
      expect(state.context.data.name).toBe('New Role');
      expect(state.context.data.description).toBe('A new role');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'id', value: 'new-role' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'id', value: 'new-role' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.id).toBe('');

      service.stop();
    });

    it('updates array fields (privileges and roles)', async () => {
      const machine = createRolesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'privileges', value: ['nx-all', 'nx-repository-view-*-*-browse'] } as any);
      service.send({ type: 'UPDATE', name: 'roles', value: ['nx-anonymous'] } as any);

      const state = service.getSnapshot();
      expect(state.context.data.privileges).toEqual(['nx-all', 'nx-repository-view-*-*-browse']);
      expect(state.context.data.roles).toEqual(['nx-anonymous']);

      service.stop();
    });
  });

  describe('error handling', () => {
    it('handles load errors gracefully for reference data', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url?.includes('privileges')) {
          return Promise.reject(new Error('Network error'));
        }
        if (url?.includes('roles')) {
          return Promise.resolve([]);
        }
        return Promise.resolve([]);
      });

      const machine = createRolesFormMachine(undefined);
      const service = interpret(machine).start();

      // Should still reach editing state (reference data failures are non-fatal)
      await waitFor(service, (state) => state.matches('editing'));

      const state = service.getSnapshot();
      expect(state.context.allPrivileges).toEqual([]);

      service.stop();
    });

    it('handles gracefully when role fetch fails', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url?.includes('roles/missing-role')) {
          return Promise.reject(new Error('Not found'));
        }
        return Promise.resolve([]);
      });

      const machine = createRolesFormMachine('missing-role');
      const service = interpret(machine).start();

      // Machine transitions to editing with role: null when fetch fails
      await waitFor(service, (state) => state.matches('editing'));

      // Verify role is null due to fetch failure
      expect(service.getSnapshot().context.role).toBeNull();

      service.stop();
    });
  });
});
