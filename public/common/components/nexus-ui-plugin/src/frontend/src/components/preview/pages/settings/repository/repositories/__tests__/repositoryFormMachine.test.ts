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
import { createRepositoryFormMachine, REPOSITORY_TYPES } from '../repositoryFormMachine';

// Mock the API interface at the path the source uses
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  API_INTERNAL_UI: '/service/rest/internal/ui',
  ENDPOINTS: {
    REPOSITORIES: '/service/rest/v1/repositories',
    BLOBSTORES: '/service/rest/v1/blobstores',
    ROUTING_RULES: '/service/rest/v1/routing-rules',
  },
  restClient: {
    get: jest.fn().mockResolvedValue([]),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createRepositoryFormMachine>,
) {
  // Mock the API responses for the load service
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('/blobstores')) {
      return Promise.resolve([{ name: 'default', type: 'File' }]);
    }
    if (url.includes('/routing-rules')) {
      return Promise.resolve([]);
    }
    if (url.includes('/cleanup-policies')) {
      return Promise.resolve([]);
    }
    if (url.includes('/repositories/recipes')) {
      return Promise.resolve([
        { format: 'maven2', type: 'hosted' },
        { format: 'maven2', type: 'proxy' },
        { format: 'maven2', type: 'group' },
        { format: 'npm', type: 'hosted' },
        { format: 'npm', type: 'proxy' },
        { format: 'npm', type: 'group' },
        { format: 'docker', type: 'hosted' },
        { format: 'docker', type: 'proxy' },
        { format: 'docker', type: 'group' },
      ]);
    }
    if (url.includes('/internal/ui/repositories')) {
      return Promise.resolve([]);
    }
    return Promise.resolve([]);
  });

  const service = interpret(machine).start();

  // Wait for loading to complete
  await waitFor(service, (state) => state.matches('editing'));

  return service;
}

describe('repositoryFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to hosted type in create mode', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.type).toBe('hosted');
      expect(state.matches({ editing: 'hosted' })).toBe(true);

      service.stop();
    });

    it('respects initial repositoryType option', async () => {
      const machine = createRepositoryFormMachine({ format: 'npm', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.type).toBe('proxy');
      expect(state.matches({ editing: 'proxy' })).toBe(true);

      service.stop();
    });

    it('sets correct default values for hosted', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.storage.writePolicy).toBe('ALLOW_ONCE');
      expect(data.component?.proprietaryComponents).toBe(false);
      expect(data.online).toBe(true);

      service.stop();
    });

    it('sets correct default values for proxy', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.proxy?.remoteUrl).toBe('');
      expect(data.proxy?.contentMaxAge).toBe(-1);
      expect(data.proxy?.metadataMaxAge).toBe(1440);
      expect(data.negativeCache?.enabled).toBe(true);
      expect(data.httpClient?.autoBlock).toBe(true);

      service.stop();
    });

    it('sets correct default values for group', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.group?.memberNames).toEqual([]);

      service.stop();
    });
  });

  describe('type variant sub-states', () => {
    const allTypes = [
      REPOSITORY_TYPES.HOSTED,
      REPOSITORY_TYPES.PROXY,
      REPOSITORY_TYPES.GROUP,
    ];

    it.each(allTypes)('transitions to %s sub-state on TYPE_CHANGE', async (type) => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: type } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: type })).toBe(true);
      expect(state.context.data.type).toBe(type);

      service.stop();
    });

    it('transitions between all type variants', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      // Start at hosted (default)
      expect(service.getSnapshot().matches({ editing: 'hosted' })).toBe(true);

      // Transition to proxy
      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);
      expect(service.getSnapshot().matches({ editing: 'proxy' })).toBe(true);

      // Transition to group
      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);
      expect(service.getSnapshot().matches({ editing: 'group' })).toBe(true);

      // Transition back to hosted
      service.send({ type: 'TYPE_CHANGE', value: 'hosted' } as any);
      expect(service.getSnapshot().matches({ editing: 'hosted' })).toBe(true);

      service.stop();
    });
  });

  describe('sub-state metadata', () => {
    it('hosted sub-state has correct field metadata', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Hosted');
      expect(typeMeta.fields).toContain('storage.blobStoreName');
      expect(typeMeta.fields).toContain('storage.writePolicy');
      expect(typeMeta.fields).toContain('component.proprietaryComponents');
      expect(typeMeta.requiredFields).toContain('storage.blobStoreName');

      service.stop();
    });

    it('proxy sub-state has correct field metadata', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Proxy');
      expect(typeMeta.fields).toContain('proxy.remoteUrl');
      expect(typeMeta.fields).toContain('proxy.contentMaxAge');
      expect(typeMeta.fields).toContain('proxy.metadataMaxAge');
      expect(typeMeta.fields).toContain('negativeCache.enabled');
      expect(typeMeta.fields).toContain('httpClient.blocked');
      expect(typeMeta.fields).toContain('httpClient.autoBlock');
      expect(typeMeta.fields).toContain('replication.preemptivePullEnabled');
      expect(typeMeta.requiredFields).toContain('proxy.remoteUrl');
      expect(typeMeta.requiredFields).toContain('storage.blobStoreName');

      service.stop();
    });

    it('group sub-state has correct field metadata', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Group');
      expect(typeMeta.fields).toContain('group.memberNames');
      expect(typeMeta.fields).toContain('group.writableMember');
      expect(typeMeta.fields).toContain('routingRuleId');
      expect(typeMeta.requiredFields).toContain('group.memberNames');
      expect(typeMeta.requiredFields).toContain('storage.blobStoreName');

      service.stop();
    });

    it('every type variant has metadata with fields and requiredFields', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const allTypes = Object.values(REPOSITORY_TYPES);

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
        expect(typeMeta.requiredFields.length).toBeGreaterThan(0);
      }

      service.stop();
    });

    it('each type has fieldConfig with label and type for every listed field', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const allTypes = Object.values(REPOSITORY_TYPES);

      for (const type of allTypes) {
        service.send({ type: 'TYPE_CHANGE', value: type } as any);
        const state = service.getSnapshot();

        const metaValues = Object.values(state.meta);
        const typeMeta = metaValues.find((m: any) => m?.fieldConfig) as any;

        expect(typeMeta.fieldConfig).toBeDefined();
        for (const field of typeMeta.fields) {
          expect(typeMeta.fieldConfig[field]).toBeDefined();
          expect(typeMeta.fieldConfig[field].label).toBeTruthy();
          expect(typeMeta.fieldConfig[field].type).toBeTruthy();
        }
      }

      service.stop();
    });
  });

  describe('validation per type', () => {
    it('validates name is required for all types', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates name format (must start with letter or number)', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: '!invalid' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toContain('must start with');

      service.stop();
    });

    it('validates blob store is required for all types', async () => {
      // Return no blob stores so auto-select doesn't pre-fill blobStoreName
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('/blobstores')) return Promise.resolve([]);
        if (url.includes('/routing-rules')) return Promise.resolve([]);
        if (url.includes('/cleanup-policies')) return Promise.resolve([]);
        if (url.includes('/repositories/recipes')) return Promise.resolve([
          { format: 'maven2', type: 'hosted' },
          { format: 'maven2', type: 'proxy' },
          { format: 'maven2', type: 'group' },
        ]);
        return Promise.resolve([]);
      });

      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      service.send({ type: 'UPDATE', name: 'name', value: 'test-repo' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['storage.blobStoreName']).toBeTruthy();

      service.stop();
    });

    it('validates proxy type requires remoteUrl', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'proxy-repo' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['proxy.remoteUrl']).toBeTruthy();

      service.stop();
    });

    it('validates proxy remoteUrl format', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'proxy-repo' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'not-a-url' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['proxy.remoteUrl']).toContain('Invalid URL');

      service.stop();
    });

    it('validates group type requires members', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'group-repo' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['group.memberNames']).toBeTruthy();

      service.stop();
    });

    it('validates alpine group type requires alpineSigning keypair', async () => {
      const machine = createRepositoryFormMachine({ format: 'alpine', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'alpine-group' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'group.memberNames', value: ['alpine-hosted'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['alpineSigning.keypair']).toBe(
        'RSA signing key is required for Alpine group repositories'
      );

      service.stop();
    });

    it('accepts alpine group with keypair provided', async () => {
      const machine = createRepositoryFormMachine({ format: 'alpine', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'alpine-group' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'group.memberNames', value: ['alpine-hosted'] } as any);
      service.send({ type: 'UPDATE', name: 'alpineSigning.keypair', value: '-----BEGIN PGP PRIVATE KEY-----\ntest\n-----END PGP PRIVATE KEY-----' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['alpineSigning.keypair']).toBeFalsy();

      service.stop();
    });

    it('does not require alpineSigning keypair for alpine hosted', async () => {
      const machine = createRepositoryFormMachine({ format: 'alpine', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'alpine-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['alpineSigning.keypair']).toBeFalsy();

      service.stop();
    });

    it('accepts valid proxy form data', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'maven-proxy' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'https://repo1.maven.org/maven2/' } as any);

      // Validation should pass (no errors for the required fields)
      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toBeFalsy();
      expect(state.context.validationErrors['storage.blobStoreName']).toBeFalsy();
      expect(state.context.validationErrors['proxy.remoteUrl']).toBeFalsy();

      service.stop();
    });

    it('validates docker HTTP port is within valid range', async () => {
      const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'docker-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'docker', value: { httpPort: 70000, pathEnabled: false } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['docker.httpPort']).toContain('between 1 and 65535');

      service.stop();
    });

    it('validates docker HTTPS port is within valid range', async () => {
      const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'docker-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'docker', value: { httpsPort: 0, pathEnabled: false } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['docker.httpsPort']).toContain('between 1 and 65535');

      service.stop();
    });

    it('does not validate docker ports when path-based routing is enabled', async () => {
      const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'docker-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'docker', value: { httpPort: 70000, pathEnabled: true } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['docker.httpPort']).toBeFalsy();

      service.stop();
    });

    it('accepts valid docker port numbers', async () => {
      const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'docker-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'docker', value: { httpPort: 8082, httpsPort: 8083, pathEnabled: false } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['docker.httpPort']).toBeFalsy();
      expect(state.context.validationErrors['docker.httpsPort']).toBeFalsy();

      service.stop();
    });
  });

  describe('TYPE_CHANGE resets type-specific fields', () => {
    it('applies proxy defaults when switching from hosted to proxy', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      // Start at hosted
      expect(service.getSnapshot().context.data.type).toBe('hosted');

      // Switch to proxy
      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.type).toBe('proxy');
      expect(data.proxy).toBeDefined();
      expect(data.proxy?.remoteUrl).toBe('');
      expect(data.negativeCache).toBeDefined();
      expect(data.negativeCache?.enabled).toBe(true);
      expect(data.httpClient).toBeDefined();
      expect(data.httpClient?.autoBlock).toBe(true);
      // Hosted-specific fields should be gone
      expect(data.storage?.writePolicy).toBeUndefined();
      expect(data.component).toBeUndefined();

      service.stop();
    });

    it('applies group defaults when switching from proxy to group', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.type).toBe('group');
      expect(data.group).toBeDefined();
      expect(data.group?.memberNames).toEqual([]);
      // Proxy-specific fields should be gone
      expect(data.proxy).toBeUndefined();
      expect(data.negativeCache).toBeUndefined();
      expect(data.httpClient).toBeUndefined();

      service.stop();
    });

    it('applies hosted defaults when switching from group to hosted', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'hosted' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.type).toBe('hosted');
      expect(data.storage?.writePolicy).toBe('ALLOW_ONCE');
      expect(data.component?.proprietaryComponents).toBe(false);
      // Group-specific fields should be gone
      expect(data.group).toBeUndefined();

      service.stop();
    });

    it('preserves name and blobStoreName across type changes', async () => {
      const machine = createRepositoryFormMachine({ format: 'npm' });
      const service = await startAndLoad(machine);

      // Set name and blob store
      service.send({ type: 'UPDATE', name: 'name', value: 'my-repo' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);

      // Switch to proxy
      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      let data = service.getSnapshot().context.data;
      expect(data.name).toBe('my-repo');
      expect(data.storage.blobStoreName).toBe('default');
      expect(data.format).toBe('npm');

      // Switch to group
      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);

      data = service.getSnapshot().context.data;
      expect(data.name).toBe('my-repo');
      expect(data.storage.blobStoreName).toBe('default');
      expect(data.format).toBe('npm');

      service.stop();
    });

    it('updates recipe string on type change', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.data.recipe).toBe('maven2-hosted');

      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);
      expect(service.getSnapshot().context.data.recipe).toBe('maven2-proxy');

      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);
      expect(service.getSnapshot().context.data.recipe).toBe('maven2-group');

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates nested storage fields via dot notation', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'custom-store' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.storage.blobStoreName).toBe('custom-store');

      service.stop();
    });

    it('updates proxy fields via dot notation', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'https://repo.example.com' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.proxy?.remoteUrl).toBe('https://repo.example.com');

      service.stop();
    });

    it('updates group member names', async () => {
      const machine = createRepositoryFormMachine({ format: 'npm', repositoryType: 'group' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'group.memberNames', value: ['npm-hosted', 'npm-proxy'] } as any);

      const state = service.getSnapshot();
      expect(state.context.data.group?.memberNames).toEqual(['npm-hosted', 'npm-proxy']);

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-repo' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-repo' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });

    it('updates format-specific fields', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'maven.versionPolicy', value: 'SNAPSHOT' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.maven?.versionPolicy).toBe('SNAPSHOT');

      service.stop();
    });

    it('updates docker-specific fields', async () => {
      const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'docker.httpPort', value: 8082 } as any);
      service.send({ type: 'UPDATE', name: 'docker.forceBasicAuth', value: true } as any);

      const state = service.getSnapshot();
      expect(state.context.data.docker?.httpPort).toBe(8082);
      expect(state.context.data.docker?.forceBasicAuth).toBe(true);

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads repository data and enters correct type sub-state', async () => {
      const preloadedRepository = {
        name: 'maven-proxy',
        type: 'proxy' as const,
        format: 'maven2',
        url: 'http://localhost:8081/repository/maven-proxy',
        online: true,
        status: { online: true },
        recipe: 'maven2-proxy',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        proxy: {
          remoteUrl: 'https://repo1.maven.org/maven2/',
          contentMaxAge: -1,
          metadataMaxAge: 1440,
        },
        negativeCache: {
          enabled: true,
          timeToLive: 1440,
        },
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: null,
        },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'maven-proxy',
        preloadedRepository: preloadedRepository as any,
        format: 'maven2',
      });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'proxy' })).toBe(true);
      expect(state.context.data.name).toBe('maven-proxy');
      expect(state.context.data.type).toBe('proxy');
      expect(state.context.data.proxy?.remoteUrl).toBe('https://repo1.maven.org/maven2/');
      expect(state.context.data.storage.blobStoreName).toBe('default');

      service.stop();
    });

    it('loads hosted repository with writePolicy', async () => {
      const preloadedRepository = {
        name: 'maven-hosted',
        type: 'hosted' as const,
        format: 'maven2',
        url: 'http://localhost:8081/repository/maven-hosted',
        online: true,
        status: { online: true },
        recipe: 'maven2-hosted',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
          writePolicy: 'ALLOW_ONCE',
        },
        component: {
          proprietaryComponents: false,
        },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'maven-hosted',
        preloadedRepository: preloadedRepository as any,
        format: 'maven2',
      });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'hosted' })).toBe(true);
      expect(state.context.data.storage.writePolicy).toBe('ALLOW_ONCE');

      service.stop();
    });

    it('loads group repository with member names', async () => {
      const preloadedRepository = {
        name: 'npm-group',
        type: 'group' as const,
        format: 'npm',
        url: 'http://localhost:8081/repository/npm-group',
        online: true,
        status: { online: true },
        recipe: 'npm-group',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        group: {
          memberNames: ['npm-hosted', 'npm-proxy'],
        },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'npm-group',
        preloadedRepository: preloadedRepository as any,
        format: 'npm',
      });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'group' })).toBe(true);
      expect(state.context.data.group?.memberNames).toEqual(['npm-hosted', 'npm-proxy']);

      service.stop();
    });
  });

  describe('reference data loading', () => {
    it('loads blob stores into context', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.blobStores).toEqual([{ name: 'default', type: 'File' }]);

      service.stop();
    });

    it('loads recipes into context', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.recipes.length).toBeGreaterThan(0);
      expect(state.context.recipes[0]).toHaveProperty('format');
      expect(state.context.recipes[0]).toHaveProperty('type');
      expect(state.context.recipes[0]).toHaveProperty('name');

      service.stop();
    });
  });

  describe('cleanup policy format filtering', () => {
    it('cleanup policies in context include all formats (filtering is done at component level)', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      // Machine loads all policies - CleanupFacet filters by format at render time
      expect(Array.isArray(state.context.cleanupPolicies)).toBe(true);

      service.stop();
    });
  });
});
