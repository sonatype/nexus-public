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
import { createPrivilegeFormMachine } from '../privilegeFormMachine';
import { PRIVILEGE_TYPES } from '../types';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    ENDPOINTS: {
      PRIVILEGES: '/service/rest/v1/security/privileges',
      REPOSITORIES: '/service/rest/v1/repositories',
      CONTENT_SELECTORS: '/service/rest/v1/security/content-selectors',
      SCRIPTS: '/service/rest/v1/script',
    },
    API_INTERNAL_UI: '/service/rest/internal/ui',
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
  machine: ReturnType<typeof createPrivilegeFormMachine>,
  loadData?: Record<string, unknown>
) {
  // Mock the API responses for the load service
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('privileges/types')) {
      return Promise.resolve([
        { id: 'application', name: 'Application', formFields: null },
        { id: 'wildcard', name: 'Wildcard', formFields: null },
        { id: 'repository-view', name: 'Repository View', formFields: null },
        { id: 'repository-admin', name: 'Repository Admin', formFields: null },
        { id: 'repository-content-selector', name: 'Repository Content Selector', formFields: null },
        { id: 'script', name: 'Script', formFields: null },
      ]);
    }
    if (url.includes('repositories')) return Promise.resolve([]);
    if (url.includes('content-selectors')) return Promise.resolve([]);
    if (url.includes('script')) return Promise.resolve([]);
    return Promise.resolve(loadData || []);
  });

  const service = interpret(machine).start();

  // Wait for loading to complete
  await waitFor(service, (state) => state.matches('editing'));

  return service;
}

describe('privilegeFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to application type in create mode', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.type).toBe(PRIVILEGE_TYPES.APPLICATION);
      expect(state.matches({ editing: PRIVILEGE_TYPES.APPLICATION })).toBe(true);

      service.stop();
    });
  });

  describe('type variant sub-states', () => {
    const allTypes = [
      PRIVILEGE_TYPES.APPLICATION,
      PRIVILEGE_TYPES.WILDCARD,
      PRIVILEGE_TYPES.REPOSITORY_VIEW,
      PRIVILEGE_TYPES.REPOSITORY_ADMIN,
      PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR,
      PRIVILEGE_TYPES.SCRIPT,
    ];

    it.each(allTypes)('transitions to %s sub-state on TYPE_CHANGE', async (type) => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: type } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: type })).toBe(true);
      expect(state.context.data.type).toBe(type);
      // Properties should be cleared on type change
      expect(state.context.data.properties).toEqual({});

      service.stop();
    });

    it('transitions between all type variants', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Start at application (default)
      expect(service.getSnapshot().matches({ editing: 'application' })).toBe(true);

      // Transition through all types
      for (const type of allTypes) {
        service.send({ type: 'TYPE_CHANGE', value: type } as any);
        expect(service.getSnapshot().matches({ editing: type })).toBe(true);
      }

      // Transition back to application
      service.send({ type: 'TYPE_CHANGE', value: 'application' } as any);
      expect(service.getSnapshot().matches({ editing: 'application' })).toBe(true);

      service.stop();
    });
  });

  describe('sub-state metadata', () => {
    it('application sub-state has correct field metadata', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'application' } as any);
      const state = service.getSnapshot();

      // Find the meta for the active sub-state
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Application');
      expect(typeMeta.fields).toContain('properties.domain');
      expect(typeMeta.fields).toContain('properties.actions');
      expect(typeMeta.requiredFields).toContain('properties.domain');
      expect(typeMeta.requiredFields).toContain('properties.actions');

      service.stop();
    });

    it('wildcard sub-state has correct field metadata', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'wildcard' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Wildcard');
      expect(typeMeta.fields).toEqual(['properties.pattern']);
      expect(typeMeta.requiredFields).toEqual(['properties.pattern']);

      service.stop();
    });

    it('repository-content-selector sub-state has contentSelector field', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'repository-content-selector' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta.fields).toContain('properties.contentSelector');
      expect(typeMeta.requiredFields).toContain('properties.contentSelector');

      service.stop();
    });

    it('every type variant has metadata with fields and requiredFields', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      const allTypes = Object.values(PRIVILEGE_TYPES);

      for (const type of allTypes) {
        service.send({ type: 'TYPE_CHANGE', value: type } as any);
        const state = service.getSnapshot();

        const metaValues = Object.values(state.meta);
        const typeMeta = metaValues.find((m: any) => m?.fields) as any;

        expect(typeMeta).toBeDefined();
        expect(typeMeta.typeLabel).toBeTruthy();
        expect(Array.isArray(typeMeta.fields)).toBe(true);
        expect(typeMeta.fields.length).toBeGreaterThan(0);
        expect(Array.isArray(typeMeta.requiredFields)).toBe(true);
      }

      service.stop();
    });
  });

  describe('validation per type', () => {
    it('validates application type requires domain and actions', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set name and type to application (default)
      service.send({ type: 'UPDATE', name: 'name', value: 'test-priv' } as any);

      // Try to submit without domain and actions
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      // Should stay in editing (validation failed)
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['properties.domain']).toBeTruthy();
      expect(state.context.validationErrors['properties.actions']).toBeTruthy();

      service.stop();
    });

    it('validates wildcard type requires pattern', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'wildcard' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'test-priv' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['properties.pattern']).toBeTruthy();

      service.stop();
    });

    it('validates repository-view type requires format, repository, and actions', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'repository-view' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'test-priv' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['properties.format']).toBeTruthy();
      expect(state.context.validationErrors['properties.repository']).toBeTruthy();
      expect(state.context.validationErrors['properties.actions']).toBeTruthy();

      service.stop();
    });

    it('validates name is required for all types', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Try to submit with no name
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates name format (alphanumeric, hyphens, underscores)', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'invalid name!' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toContain('letters, numbers');

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates nested properties via dot notation', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'properties.domain', value: 'users' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.properties.domain).toBe('users');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-priv' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-priv' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });
  });

  describe('TYPE_CHANGE clears properties', () => {
    it('resets properties when switching between types', async () => {
      const machine = createPrivilegeFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set some application properties
      service.send({ type: 'UPDATE', name: 'properties.domain', value: 'users' } as any);
      service.send({ type: 'UPDATE', name: 'properties.actions', value: 'read,create' } as any);

      expect(service.getSnapshot().context.data.properties.domain).toBe('users');

      // Switch to wildcard
      service.send({ type: 'TYPE_CHANGE', value: 'wildcard' } as any);

      // Properties should be cleared
      expect(service.getSnapshot().context.data.properties).toEqual({});

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads privilege data and enters correct type sub-state', async () => {
      const preloadedPrivilege = {
        id: 'nx-repo-view-all',
        version: '1',
        name: 'nx-repo-view-all',
        description: 'All repository view',
        type: 'repository-view',
        readOnly: false,
        properties: { format: '*', repository: '*', actions: 'browse,read' },
        permission: 'nexus:repository-view:*:*:browse,read',
      };

      const machine = createPrivilegeFormMachine('nx-repo-view-all', preloadedPrivilege);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'repository-view' })).toBe(true);
      expect(state.context.data.name).toBe('nx-repo-view-all');
      expect(state.context.data.type).toBe('repository-view');
      expect(state.context.data.properties.format).toBe('*');

      service.stop();
    });
  });
});
