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
  createRepositoryFormMachine,
  DOCKER_FOREIGN_LAYER_WHITELIST_ERROR_KEY,
  INVALID_URL_FORMAT_MESSAGE,
  REPOSITORY_TYPES,
} from '../repositoryFormMachine';
import { ExtJS } from '../../../../../../../interface/ExtJS';

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

    // Regression: pypi proxy create used to start with `data.pypi`
    // undefined. If the user saved without opening the PyPI section the
    // payload omitted the `pypi` block, the backend's converter
    // short-circuited, and the saved repository ended up with no PyPI
    // attributes (no indexPath). Seeding the defaults here makes the
    // create payload self-describing.
    // (Post-migration STL-381: `removeQuarantinedVersions` was removed
    // from PypiConfig — PCCS is now expressed via `firewall.mode = "PCCS"`.)
    it('seeds PyPI proxy defaults (indexPath=/simple)', async () => {
      const machine = createRepositoryFormMachine({ format: 'pypi', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.pypi?.indexPath).toBe('/simple');

      service.stop();
    });

    // NEXUS-54771: `oci.v1Enabled` and `oci.forceBasicAuth` are @NotNull on the
    // REST model. Before this, the Preview UI seeded no oci block at all and every
    // create POST failed with 400 `request.oci: must not be null`.
    it('seeds OCI defaults with non-null required booleans (proxy)', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.oci?.v1Enabled).toBe(false);
      expect(data.oci?.forceBasicAuth).toBe(true);
      expect(data.oci?.pathEnabled).toBe(true);
      expect(data.ociProxy?.indexType).toBe('REGISTRY');

      service.stop();
    });

    it('seeds OCI defaults for hosted but no ociProxy block', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.oci?.v1Enabled).toBe(false);
      expect(data.oci?.forceBasicAuth).toBe(true);
      expect(data.ociProxy).toBeUndefined();

      service.stop();
    });

    it('does not seed oci blocks for non-oci formats', async () => {
      const machine = createRepositoryFormMachine({ format: 'raw', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.oci).toBeUndefined();
      expect(data.ociProxy).toBeUndefined();

      service.stop();
    });

    // Post-migration STL-381: there are no npm-specific defaults to seed.
    // The legacy `removeQuarantinedVersions` flag was npm's only field and
    // has been removed; `NpmConfig` no longer exists in types.ts.
    it('does not seed an npm block for npm proxy (no NpmConfig post-migration)', async () => {
      const machine = createRepositoryFormMachine({ format: 'npm', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect((data as any).npm).toBeUndefined();

      service.stop();
    });

    // NEXUS-48685: new HuggingFace proxies default to XET disabled. Create
    // mode explicitly seeds `xet.enabled=false` so the checkbox renders
    // unchecked and the save payload always carries the xet block with an
    // explicit value. The facet's undefined-fallback is also *unchecked*,
    // preserving LFS behavior for pre-feature repositories (which have no
    // persisted `xet` block).
    it('seeds xet.enabled=false for huggingface proxy create mode', async () => {
      const machine = createRepositoryFormMachine({
        format: 'huggingface',
        repositoryType: 'proxy',
      });
      const service = await startAndLoad(machine);

      const data = service.getSnapshot().context.data;
      expect(data.xet?.enabled).toBe(false);

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
      // Unparseable input keeps its pre-existing message — only the scheme check is new.
      expect(state.context.validationErrors['proxy.remoteUrl']).toBe(INVALID_URL_FORMAT_MESSAGE);

      service.stop();
    });

    // NEXUS-54771: proxy remoteUrl must be http/https only. Previously this used a
    // bare `new URL()`, which parses any scheme — so oci:// and friends produced no
    // client-side error at all and were only rejected later by the server.
    describe('proxy remoteUrl scheme restriction (NEXUS-54771)', () => {
      const submitRemoteUrl = async (remoteUrl: string) => {
        const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: 'proxy-repo' } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: remoteUrl } as any);
        service.send({ type: 'SUBMIT' } as any);

        const error = service.getSnapshot().context.validationErrors['proxy.remoteUrl'];
        service.stop();
        return error;
      };

      it.each(['oci://ghcr.io', 'oci+http://localhost:5000', 'ftp://example.com', 'file:///etc/passwd'])(
        'rejects non-http(s) scheme %s',
        async (remoteUrl) => {
          expect(await submitRemoteUrl(remoteUrl)).toContain('http://www.example.com');
        },
      );

      it.each(['https://ghcr.io', 'http://localhost:5000'])('accepts %s', async (remoteUrl) => {
        expect(await submitRemoteUrl(remoteUrl)).toBeFalsy();
      });

      // The scheme check must not be implemented via ValidationUtils.validateIsUrl:
      // that helper re-extracts the host with a regex that rejects IPv6 literals and
      // userinfo, both of which the server's @Url constraint accepts (UrlValidatorTest
      // asserts 'http://[::1]' and 'http://username@without.slash.example.com' valid).
      // These cases pin the client to the server's contract.
      it.each([
        'http://[::1]',
        'http://[2001:db8::ff00:42:8329]',
        'http://username@without.slash.example.com',
        'http://username:password@example.com:8080',
      ])('accepts server-valid host form %s', async (remoteUrl) => {
        expect(await submitRemoteUrl(remoteUrl)).toBeFalsy();
      });

      it('reports unparseable input with the parse message, not the scheme message', async () => {
        expect(await submitRemoteUrl('notaurl')).toBe(INVALID_URL_FORMAT_MESSAGE);
      });
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

    it('validates terraform hosted type requires terraformSigning keypair', async () => {
      const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'terraform-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['terraformSigning.keypair']).toBe(
        'GPG signing key is required for Terraform hosted repositories'
      );

      service.stop();
    });

    it('accepts terraform hosted with keypair provided', async () => {
      const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'terraform-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'terraformSigning.keypair', value: '-----BEGIN PGP PRIVATE KEY BLOCK-----\ntest\n-----END PGP PRIVATE KEY BLOCK-----' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['terraformSigning.keypair']).toBeFalsy();

      service.stop();
    });

    // Cloud has no blob stores, so storage.blobStoreName validation is skipped there
    // (validateRepository options.isCloud). The terraform signing requirement is
    // distribution-agnostic and must still apply.
    it('requires terraformSigning keypair for terraform hosted on cloud', async () => {
      const stateSpy = jest
        .spyOn(ExtJS, 'state')
        .mockReturnValue({ getValue: (k: string) => k === 'isCloud' } as any);

      try {
        const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'hosted' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: 'terraform-hosted-cloud' } as any);
        service.send({ type: 'SUBMIT' } as any);

        const state = service.getSnapshot();
        expect(state.context.validationErrors['storage.blobStoreName']).toBeFalsy();
        expect(state.context.validationErrors['terraformSigning.keypair']).toBe(
          'GPG signing key is required for Terraform hosted repositories'
        );

        service.stop();
      } finally {
        stateSpy.mockRestore();
      }
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

    it('validates terraform hosted type requires terraformSigning keypair', async () => {
      const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'terraform-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['terraformSigning.keypair']).toBe(
        'GPG signing key is required for Terraform hosted repositories'
      );

      service.stop();
    });

    it('accepts terraform hosted with keypair provided', async () => {
      const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'terraform-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'terraformSigning.keypair', value: '-----BEGIN PGP PRIVATE KEY BLOCK-----\ntest\n-----END PGP PRIVATE KEY BLOCK-----' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['terraformSigning.keypair']).toBeFalsy();

      service.stop();
    });

    // Cloud has no blob stores, so storage.blobStoreName validation is skipped there
    // (validateRepository options.isCloud). The terraform signing requirement is
    // distribution-agnostic and must still apply.
    it('requires terraformSigning keypair for terraform hosted on cloud', async () => {
      const stateSpy = jest
        .spyOn(ExtJS, 'state')
        .mockReturnValue({ getValue: (k: string) => k === 'isCloud' } as any);

      try {
        const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'hosted' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: 'terraform-hosted-cloud' } as any);
        service.send({ type: 'SUBMIT' } as any);

        const state = service.getSnapshot();
        expect(state.context.validationErrors['storage.blobStoreName']).toBeFalsy();
        expect(state.context.validationErrors['terraformSigning.keypair']).toBe(
          'GPG signing key is required for Terraform hosted repositories'
        );

        service.stop();
      } finally {
        stateSpy.mockRestore();
      }
    });

    it('does not require terraformSigning keypair for terraform proxy', async () => {
      const machine = createRepositoryFormMachine({ format: 'terraform', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'terraform-proxy' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'https://registry.terraform.io' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['terraformSigning.keypair']).toBeFalsy();

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

    // OCI connector ports mirror the Docker branch above. Without this the only feedback on an
    // out-of-range port was a round-trip 400 from the server's @PortNumber constraint.
    it('validates OCI HTTP port is within valid range', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'oci', value: { httpPort: 70000, pathEnabled: false } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['oci.httpPort']).toContain('between 1 and 65535');

      service.stop();
    });

    it('validates OCI HTTPS port is within valid range', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'oci', value: { httpsPort: 0, pathEnabled: false } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['oci.httpsPort']).toContain('between 1 and 65535');

      service.stop();
    });

    // The port check is deliberately skipped under path-based routing, matching the Docker
    // branch. That is only sound because OciFacet hides the connector inputs and clears the
    // ports when path routing is switched on, so an out-of-range port is not user-reachable in
    // this state (see the facet tests for the hiding and clearing). This test pins the skip
    // itself; it must not be read as "an out-of-range port is acceptable".
    it('skips OCI port range checks when path-based routing is enabled and ports are cleared', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      // The state the facet actually produces when path routing is turned on.
      service.send({
        type: 'UPDATE',
        name: 'oci',
        value: { pathEnabled: true, httpPort: null, httpsPort: null, subdomain: null },
      } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['oci.httpPort']).toBeFalsy();
      expect(state.context.validationErrors['oci.httpsPort']).toBeFalsy();

      service.stop();
    });

    // Guards the other half of the contract: the range check must still fire once the operator
    // turns path routing off and the connector fields become reachable again.
    it('validates OCI ports once path-based routing is disabled', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({
        type: 'UPDATE',
        name: 'oci',
        value: { pathEnabled: false, httpPort: 70000, httpsPort: 0 },
      } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['oci.httpPort']).toContain('between 1 and 65535');
      expect(state.context.validationErrors['oci.httpsPort']).toContain('between 1 and 65535');

      service.stop();
    });

    it('accepts valid OCI port numbers', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-hosted' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'oci', value: { httpPort: 8082, httpsPort: 8083, pathEnabled: false } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['oci.httpPort']).toBeFalsy();
      expect(state.context.validationErrors['oci.httpsPort']).toBeFalsy();

      service.stop();
    });

    // The facet presents the custom index URL as required, but the message used to be produced
    // at render time only, so it displayed without blocking Save.
    it('requires ociProxy.indexUrl when the index type is CUSTOM', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-proxy' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'https://ghcr.io' } as any);
      service.send({ type: 'UPDATE', name: 'ociProxy', value: { indexType: 'CUSTOM', indexUrl: '' } } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['ociProxy.indexUrl'])
        .toBe('Index URL is required when Custom index is selected');

      service.stop();
    });

    it('rejects a non-http(s) ociProxy.indexUrl under CUSTOM', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-proxy' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'https://ghcr.io' } as any);
      service.send({
        type: 'UPDATE',
        name: 'ociProxy',
        value: { indexType: 'CUSTOM', indexUrl: 'oci://idx.example.com' },
      } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['ociProxy.indexUrl'])
        .toBe('Index URL must use http or https');

      service.stop();
    });

    it('accepts a valid ociProxy.indexUrl under CUSTOM and ignores it under REGISTRY', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'oci-proxy' } as any);
      service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
      service.send({ type: 'UPDATE', name: 'proxy.remoteUrl', value: 'https://ghcr.io' } as any);
      service.send({
        type: 'UPDATE',
        name: 'ociProxy',
        value: { indexType: 'CUSTOM', indexUrl: 'https://idx.example.com' },
      } as any);
      service.send({ type: 'SUBMIT' } as any);
      expect(service.getSnapshot().context.validationErrors['ociProxy.indexUrl']).toBeFalsy();

      // REGISTRY does not use a custom index, so a blank URL must not block Save.
      service.send({ type: 'UPDATE', name: 'ociProxy', value: { indexType: 'REGISTRY', indexUrl: '' } } as any);
      service.send({ type: 'SUBMIT' } as any);
      expect(service.getSnapshot().context.validationErrors['ociProxy.indexUrl']).toBeFalsy();

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

    describe('docker proxy foreign layer URL whitelist validation', () => {
      const FOREIGN_LAYER_ERROR_KEY = DOCKER_FOREIGN_LAYER_WHITELIST_ERROR_KEY;
      const VALID_URL_ENTRY = 'https://example.com';
      const ANOTHER_VALID_URL_ENTRY = 'https://registry.example.com/v2/';
      const INVALID_URL_ENTRY = 'not-a-url';

      async function setupDockerProxyMachine(dockerProxy: Record<string, unknown>) {
        const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);
        service.send({ type: 'UPDATE', name: 'name', value: 'docker-proxy' } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy', value: { remoteUrl: 'https://hub.docker.io' } } as any);
        service.send({ type: 'UPDATE', name: 'dockerProxy', value: dockerProxy } as any);
        service.send({ type: 'SUBMIT' } as any);
        return service;
      }

      it('emits error when an entry is not a valid URL', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [INVALID_URL_ENTRY],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY])
          .toBe(`Invalid URL format: "${INVALID_URL_ENTRY}"`);
        service.stop();
      });

      it('does not emit error when all entries are valid URLs', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [VALID_URL_ENTRY, ANOTHER_VALID_URL_ENTRY],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it('flags the first invalid entry in a mixed list', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [VALID_URL_ENTRY, INVALID_URL_ENTRY, ANOTHER_VALID_URL_ENTRY],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY])
          .toBe(`Invalid URL format: "${INVALID_URL_ENTRY}"`);
        service.stop();
      });

      it('skips validation when cacheForeignLayers is false', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: false,
          foreignLayerUrlWhitelist: [INVALID_URL_ENTRY],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it('does not emit error when the whitelist is empty', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it('does not emit error when the whitelist is undefined', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          // no foreignLayerUrlWhitelist
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it('does not validate the whitelist for hosted docker repositories', async () => {
        const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'hosted' });
        const service = await startAndLoad(machine);
        service.send({ type: 'UPDATE', name: 'name', value: 'docker-hosted' } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'dockerProxy', value: {
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [INVALID_URL_ENTRY],
        } } as any);
        service.send({ type: 'SUBMIT' } as any);

        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it('allows the Classic UI default seed value ".*"', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: ['.*'],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it.each([
        ['^https://.*$'],
        ['https?://example\\.com/.*'],
        ['^https://(eu|us)\\.example\\.com/.*'],
      ])('allows valid regex pattern %s', async (regexPattern) => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [regexPattern],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it.each([
        ['example.com'],
        ['example.com?path'],
        ['example.com+more'],
      ])('blocks typo entries without strong regex signals (%s)', async (typo) => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [typo],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY])
          .toBe(`Invalid URL format: "${typo}"`);
        service.stop();
      });

      it.each([
        ['*.example.com'],
        ['[unclosed'],
      ])('blocks entries with regex chars that do not compile as a regex (%s)', async (badRegex) => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [badRegex],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY])
          .toBe(`Invalid URL format: "${badRegex}"`);
        service.stop();
      });

      it('trims whitespace from each entry before validating', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [`  ${VALID_URL_ENTRY}  `, '   '],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });

      it('reports the trimmed value in the error message, not the padded value', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: [`  ${INVALID_URL_ENTRY}  `],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY])
          .toBe(`Invalid URL format: "${INVALID_URL_ENTRY}"`);
        service.stop();
      });

      it('mixes legacy regex with valid URLs without false positives', async () => {
        const service = await setupDockerProxyMachine({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: ['.*', VALID_URL_ENTRY, '^https://.+$'],
        });
        expect(service.getSnapshot().context.validationErrors[FOREIGN_LAYER_ERROR_KEY]).toBeFalsy();
        service.stop();
      });
    });

    describe('Docker custom index URL validation', () => {
      const baseDockerProxyFields = {
        proxy: { remoteUrl: 'https://registry.example.com', contentMaxAge: -1, metadataMaxAge: 1440 },
        negativeCache: { enabled: false, timeToLive: 1440 },
        httpClient: { blocked: false, autoBlock: true, connection: null, authentication: null },
      };

      // Hydrates the form with the minimum-valid base for a Docker proxy, then
      // applies the supplied dockerProxy override and SUBMITs. Returns the
      // validationErrors map for the caller to assert against.
      async function submitWithDockerProxy(dockerProxy: Record<string, unknown>) {
        const machine = createRepositoryFormMachine({ format: 'docker', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: 'docker-proxy' } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy', value: baseDockerProxyFields.proxy } as any);
        service.send({ type: 'UPDATE', name: 'negativeCache', value: baseDockerProxyFields.negativeCache } as any);
        service.send({ type: 'UPDATE', name: 'httpClient', value: baseDockerProxyFields.httpClient } as any);
        service.send({ type: 'UPDATE', name: 'dockerProxy', value: dockerProxy } as any);
        service.send({ type: 'SUBMIT' } as any);

        const errors = service.getSnapshot().context.validationErrors;
        service.stop();
        return errors;
      }

      it('reports required error for empty index URL when index type is CUSTOM', async () => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl: '' });
        expect(errors['dockerProxy.indexUrl']).toBe('Index URL is required');
      });

      it('reports required error for whitespace-only index URL', async () => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl: '   ' });
        expect(errors['dockerProxy.indexUrl']).toBe('Index URL is required');
      });

      it('reports invalid URL format for unparseable input', async () => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl: 'not-a-url' });
        expect(errors['dockerProxy.indexUrl']).toBe('Invalid URL format');
      });

      // NEXUS-54771: indexUrl and proxy.remoteUrl share one validateHttpUrl helper for the
      // RULE, but each keeps its own message — indexUrl's wording is unchanged by this ticket.
      it.each([
        ['javascript:', 'javascript:alert(1)'],
        ['file:', 'file:///etc/passwd'],
        ['ftp:', 'ftp://example.com'],
        ['oci:', 'oci://ghcr.io'],
        ['oci+http:', 'oci+http://localhost:5000'],
      ])('rejects %s scheme', async (_label, indexUrl) => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl });
        expect(errors['dockerProxy.indexUrl']).toBe('Index URL must use http or https');
      });

      it('accepts http URL', async () => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl: 'http://index.example.com' });
        expect(errors['dockerProxy.indexUrl']).toBeFalsy();
      });

      it('accepts https URL', async () => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl: 'https://index.example.com' });
        expect(errors['dockerProxy.indexUrl']).toBeFalsy();
      });

      // Shared helper must not import ValidationUtils' hostname regex, which rejects these.
      it.each([
        ['IPv6 literal', 'http://[::1]'],
        ['userinfo', 'http://username@without.slash.example.com'],
      ])('accepts %s index URL, matching the server', async (_label, indexUrl) => {
        const errors = await submitWithDockerProxy({ indexType: 'CUSTOM', indexUrl });
        expect(errors['dockerProxy.indexUrl']).toBeFalsy();
      });

      it('does not validate index URL when index type is not CUSTOM', async () => {
        const errors = await submitWithDockerProxy({ indexType: 'HUB', indexUrl: '' });
        expect(errors['dockerProxy.indexUrl']).toBeFalsy();
      });
    });

    describe('HTTP authentication validation', () => {
      const baseProxyData = {
        name: 'test-proxy',
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://example.com', contentMaxAge: -1, metadataMaxAge: 1440 },
        negativeCache: { enabled: true, timeToLive: 1440 },
        httpClient: { blocked: false, autoBlock: true, connection: null, authentication: null },
      };

      it('requires bearerToken when authType is bearer, not username/password', async () => {
        const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: baseProxyData.name } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy', value: baseProxyData.proxy } as any);
        service.send({ type: 'UPDATE', name: 'httpClient', value: {
          ...baseProxyData.httpClient,
          authentication: { type: 'bearer', bearerToken: '' },
        } } as any);
        service.send({ type: 'SUBMIT' } as any);

        const errors = service.getSnapshot().context.validationErrors;
        // bearer-specific error must fire
        expect(errors['httpClient.authentication.bearerToken']).toBeTruthy();
        // username/password errors must NOT fire (those fields aren't rendered for bearer)
        expect(errors['httpClient.authentication.username']).toBeFalsy();
        expect(errors['httpClient.authentication.password']).toBeFalsy();

        service.stop();
      });

      it('clears bearerToken error when a token is provided', async () => {
        const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: baseProxyData.name } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy', value: baseProxyData.proxy } as any);
        service.send({ type: 'UPDATE', name: 'httpClient', value: {
          ...baseProxyData.httpClient,
          authentication: { type: 'bearer', bearerToken: 'my-secret-token' },
        } } as any);
        service.send({ type: 'SUBMIT' } as any);

        const errors = service.getSnapshot().context.validationErrors;
        expect(errors['httpClient.authentication.bearerToken']).toBeFalsy();

        service.stop();
      });

      it('requires username and password for username auth type, not bearerToken', async () => {
        const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: baseProxyData.name } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy', value: baseProxyData.proxy } as any);
        service.send({ type: 'UPDATE', name: 'httpClient', value: {
          ...baseProxyData.httpClient,
          authentication: { type: 'username', username: '', password: '' },
        } } as any);
        service.send({ type: 'SUBMIT' } as any);

        const errors = service.getSnapshot().context.validationErrors;
        expect(errors['httpClient.authentication.username']).toBeTruthy();
        expect(errors['httpClient.authentication.password']).toBeTruthy();
        expect(errors['httpClient.authentication.bearerToken']).toBeFalsy();

        service.stop();
      });

      it('does not produce auth errors when authentication is null', async () => {
        const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'proxy' });
        const service = await startAndLoad(machine);

        service.send({ type: 'UPDATE', name: 'name', value: baseProxyData.name } as any);
        service.send({ type: 'UPDATE', name: 'storage.blobStoreName', value: 'default' } as any);
        service.send({ type: 'UPDATE', name: 'proxy', value: baseProxyData.proxy } as any);
        service.send({ type: 'UPDATE', name: 'httpClient', value: baseProxyData.httpClient } as any);
        service.send({ type: 'SUBMIT' } as any);

        const errors = service.getSnapshot().context.validationErrors;
        expect(errors['httpClient.authentication.bearerToken']).toBeFalsy();
        expect(errors['httpClient.authentication.username']).toBeFalsy();
        expect(errors['httpClient.authentication.password']).toBeFalsy();

        service.stop();
      });
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

    // formatDefaults inside changeType exists to mirror buildDefaultFormData. The OCI seeding was
    // originally added only to the latter, so a TYPE_CHANGE into an OCI recipe left data.oci
    // undefined — benign only because the ?? fallbacks happened to match. Pinned so they cannot
    // drift apart again: v1Enabled and forceBasicAuth are @NotNull on the REST model.
    it('seeds the OCI defaults on TYPE_CHANGE, matching a fresh create', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.oci).toEqual({
        v1Enabled: false,
        forceBasicAuth: true,
        pathEnabled: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
      expect(data.ociProxy).toEqual({
        indexType: 'REGISTRY',
        indexUrl: null,
        cacheForeignLayers: false,
        foreignLayerUrlWhitelist: [],
      });

      service.stop();
    });

    it('does not seed ociProxy when switching an OCI repository to hosted', async () => {
      const machine = createRepositoryFormMachine({ format: 'oci', repositoryType: 'proxy' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'hosted' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.oci?.forceBasicAuth).toBe(true);
      expect(data.ociProxy).toBeUndefined();

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

    // Regression: TYPE_CHANGE used to ignore format-specific defaults
    // (it only consulted DEFAULT_HOSTED/PROXY/GROUP_VALUES). Switching
    // to pypi-proxy or npm-proxy via TYPE_CHANGE left data.pypi /
    // data.npm undefined, so the firewall checkbox would render
    // unchecked even though buildRepositoryConfig now defaults it on
    // save. Format-aware seeding inside changeType keeps the create
    // wizard and TYPE_CHANGE in sync.
    it('seeds pypi defaults when TYPE_CHANGE switches to proxy on pypi format', async () => {
      const machine = createRepositoryFormMachine({ format: 'pypi', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.type).toBe('proxy');
      expect(data.pypi?.indexPath).toBe('/simple');

      service.stop();
    });

    // Post-migration STL-381: TYPE_CHANGE to npm-proxy does not seed any
    // npm-specific defaults (NpmConfig was removed; `removeQuarantinedVersions`
    // is dead code, replaced by `firewall.mode = "PCCS"`).
    it('does not seed an npm block when TYPE_CHANGE switches to proxy on npm format', async () => {
      const machine = createRepositoryFormMachine({ format: 'npm', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.type).toBe('proxy');
      expect((data as any).npm).toBeUndefined();

      service.stop();
    });

    it('seeds nugetProxy defaults when TYPE_CHANGE switches to proxy on nuget format', async () => {
      const machine = createRepositoryFormMachine({ format: 'nuget', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      const data = service.getSnapshot().context.data;
      expect(data.type).toBe('proxy');
      expect(data.nugetProxy?.queryCacheItemMaxAge).toBe(3600);
      expect(data.nugetProxy?.nugetVersion).toBe('V3');

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

    // Regression: editing a PyPI proxy used to render the form with
    // `pypi` undefined because buildFormDataFromRepository did not pluck
    // it. A Save without touching the section omitted `pypi` from the
    // PUT entirely.
    // (Post-migration STL-381: `removeQuarantinedVersions` was removed
    // from PypiConfig — only `indexPath` is preserved on edit.)
    // Regression: OCI stores three sibling blocks (`oci`, `ociProxy`, `cosign`). If
    // buildFormDataFromRepository does not pluck all three, saving without opening the OCI
    // section drops the persisted values from the PUT body. Verified manually against a live
    // server (edit an unrelated field, save, confirm all three come back byte-identical); these
    // tests lock that behaviour in.
    it('loads OCI proxy attributes (oci, ociProxy and cosign siblings)', async () => {
      const preloadedRepository = {
        name: 'oci-proxy',
        type: 'proxy' as const,
        format: 'oci',
        url: 'http://localhost:8081/repository/oci-proxy',
        online: true,
        status: { online: true },
        recipe: 'oci-proxy',
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: {
          remoteUrl: 'https://registry-1.docker.io',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
        oci: {
          v1Enabled: false,
          forceBasicAuth: true,
          pathEnabled: true,
          httpPort: null,
          httpsPort: null,
          subdomain: null,
        },
        ociProxy: {
          indexType: 'CUSTOM',
          indexUrl: 'https://index.docker.io/',
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: ['^https://production\\.cloudflare\\.docker\\.com/.*$'],
        },
        cosign: { enforcement: 'NONE', identityRegex: null, issuerRegex: null },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'oci-proxy',
        preloadedRepository: preloadedRepository as any,
        format: 'oci',
      });
      const service = await startAndLoad(machine);
      const data = service.getSnapshot().context.data as any;

      expect(data.oci?.forceBasicAuth).toBe(true);
      expect(data.oci?.pathEnabled).toBe(true);
      expect(data.ociProxy?.indexType).toBe('CUSTOM');
      expect(data.ociProxy?.indexUrl).toBe('https://index.docker.io/');
      expect(data.ociProxy?.cacheForeignLayers).toBe(true);
      expect(data.ociProxy?.foreignLayerUrlWhitelist)
        .toEqual(['^https://production\\.cloudflare\\.docker\\.com/.*$']);
      expect(data.cosign?.enforcement).toBe('NONE');

      service.stop();
    });

    // A repository whose stored policy predates the server-side KEYLESS rejection must still
    // load. The form surfaces it read-only; the payload builder downgrades it on save.
    it('loads a legacy KEYLESS cosign policy without dropping it from form state', async () => {
      const preloadedRepository = {
        name: 'oci-legacy-keyless',
        type: 'proxy' as const,
        format: 'oci',
        url: 'http://localhost:8081/repository/oci-legacy-keyless',
        online: true,
        status: { online: true },
        recipe: 'oci-proxy',
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://ghcr.io', contentMaxAge: 1440, metadataMaxAge: 1440 },
        oci: { v1Enabled: false, forceBasicAuth: true, pathEnabled: true },
        cosign: {
          enforcement: 'KEYLESS',
          identityRegex: '^https://github\\.com/acme/.*$',
          issuerRegex: '^https://token\\.example$',
        },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'oci-legacy-keyless',
        preloadedRepository: preloadedRepository as any,
        format: 'oci',
      });
      const service = await startAndLoad(machine);
      const data = service.getSnapshot().context.data as any;

      expect(data.cosign?.enforcement).toBe('KEYLESS');
      expect(data.cosign?.identityRegex).toBe('^https://github\\.com/acme/.*$');
      // Loading must not itself be a validation failure — the operator can still edit the repo.
      expect(service.getSnapshot().context.validationErrors['cosign.enforcement']).toBeFalsy();

      service.stop();
    });

    // The absent-block case: a repo with no OCI sub-blocks must not explode on load.
    it('loads an OCI repository with no oci/ociProxy/cosign blocks', async () => {
      const preloadedRepository = {
        name: 'oci-bare',
        type: 'proxy' as const,
        format: 'oci',
        url: 'http://localhost:8081/repository/oci-bare',
        online: true,
        status: { online: true },
        recipe: 'oci-proxy',
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://ghcr.io', contentMaxAge: 1440, metadataMaxAge: 1440 },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'oci-bare',
        preloadedRepository: preloadedRepository as any,
        format: 'oci',
      });
      const service = await startAndLoad(machine);
      const state = service.getSnapshot();

      expect(state.matches({ editing: 'proxy' })).toBe(true);
      expect((state.context.data as any).proxy?.remoteUrl).toBe('https://ghcr.io');

      service.stop();
    });

    it('loads PyPI proxy attributes (indexPath)', async () => {
      const preloadedRepository = {
        name: 'pypi-proxy',
        type: 'proxy' as const,
        format: 'pypi',
        url: 'http://localhost:8081/repository/pypi-proxy',
        online: true,
        status: { online: true },
        recipe: 'pypi-proxy',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        proxy: {
          remoteUrl: 'https://pypi.org',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
        // Server returns the saved PyPI configuration on the top-level
        // GET response (matches the shape used by the existing maven
        // proxy fixture above).
        pypi: {
          indexPath: '',
        },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'pypi-proxy',
        preloadedRepository: preloadedRepository as any,
        format: 'pypi',
      });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'proxy' })).toBe(true);
      expect(state.context.data.pypi?.indexPath).toBe('');

      service.stop();
    });

    // Regression NEXUS-48685: editing a HuggingFace proxy with XET disabled
    // rendered the XET checkbox as checked because buildFormDataFromRepository
    // did not pluck the top-level `xet` block from the API response, and the
    // component fell back to its `?? true` default. Preserve `xet.enabled=false`
    // through the form-data build so the checkbox reflects the persisted state.
    it('loads HuggingFace proxy XET attributes (enabled=false)', async () => {
      const preloadedRepository = {
        name: 'huggingface-proxy',
        type: 'proxy' as const,
        format: 'huggingface',
        url: 'http://localhost:8081/repository/huggingface-proxy',
        online: true,
        status: { online: true },
        recipe: 'huggingface-proxy',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        proxy: {
          remoteUrl: 'https://huggingface.co/',
        },
        xet: {
          enabled: false,
        },
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'huggingface-proxy',
        preloadedRepository: preloadedRepository as any,
        format: 'huggingface',
      });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'proxy' })).toBe(true);
      expect(state.context.data.xet?.enabled).toBe(false);

      service.stop();
    });

    // Regression NEXUS-48685: the generic REST endpoint
    // `/service/rest/v1/repositories/{name}` serves a minimal RepositoryXO shape
    // that does NOT include the `xet` block. Fetching a HuggingFace repository
    // for edit must go through the format-specific endpoint
    // `/service/rest/v1/repositories/huggingface/proxy/{name}` so that the `xet`
    // configuration is available to seed the form.
    it('loads HuggingFace proxy via the format-specific REST endpoint', async () => {
      const responseFromFormatSpecificEndpoint = {
        name: 'huggingface-proxy',
        type: 'proxy' as const,
        format: 'huggingface',
        url: 'http://localhost:8081/repository/huggingface-proxy',
        online: true,
        recipe: 'huggingface-proxy',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        proxy: {
          remoteUrl: 'https://huggingface.co/',
        },
        xet: {
          enabled: false,
        },
      };

      restClient.get.mockImplementation((url: string) => {
        if (url === '/service/rest/v1/repositories/huggingface/proxy/huggingface-proxy') {
          return Promise.resolve(responseFromFormatSpecificEndpoint);
        }
        // The generic endpoint returns a stripped-down shape without `xet`.
        // If the machine hits this URL, the test should fail.
        if (url === '/service/rest/v1/repositories/huggingface-proxy') {
          return Promise.resolve({
            name: 'huggingface-proxy',
            type: 'proxy',
            format: 'huggingface',
            url: 'http://localhost:8081/repository/huggingface-proxy',
            online: true,
            attributes: { proxy: { remoteUrl: 'https://huggingface.co/' } },
          });
        }
        if (url.includes('/blobstores')) {
          return Promise.resolve([{ name: 'default', type: 'File' }]);
        }
        if (url.includes('/routing-rules')) return Promise.resolve([]);
        if (url.includes('/cleanup-policies')) return Promise.resolve([]);
        if (url.includes('/repositories/recipes')) {
          return Promise.resolve([{ format: 'huggingface', type: 'proxy' }]);
        }
        if (url.includes('/internal/ui/repositories')) return Promise.resolve([]);
        return Promise.resolve([]);
      });

      const machine = createRepositoryFormMachine({
        repositoryName: 'huggingface-proxy',
        format: 'huggingface',
        repositoryType: 'proxy',
      });
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      // The machine must have called the format-specific endpoint, not the generic one.
      expect(restClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/huggingface/proxy/huggingface-proxy',
      );
      expect(restClient.get).not.toHaveBeenCalledWith(
        '/service/rest/v1/repositories/huggingface-proxy',
      );

      const state = service.getSnapshot();
      expect(state.context.data.xet?.enabled).toBe(false);

      service.stop();
    });

    // Regression NEXUS-48685: existing HuggingFace proxy repositories created
    // before the XET toggle feature have no persisted `xet` block. The GET
    // response DTO uses @JsonInclude(Include.NON_NULL) on the xet field, so
    // the response omits `xet` entirely. Loading such a repository must not
    // throw or produce a partial xet object — formData.xet must stay
    // undefined so the facet's undefined-fallback renders the checkbox
    // UNCHECKED (LFS-preserving behavior for pre-feature repos on upgrade).
    it('loads an existing HuggingFace proxy that has no xet field in the response', async () => {
      const preloadedRepositoryWithoutXet = {
        name: 'huggingface-proxy',
        type: 'proxy' as const,
        format: 'huggingface',
        url: 'http://localhost:8081/repository/huggingface-proxy',
        online: true,
        status: { online: true },
        recipe: 'huggingface-proxy',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        proxy: {
          remoteUrl: 'https://huggingface.co/',
        },
        // NOTE: no `xet` property — simulates a repository created before
        // NEXUS-48685 shipped, whose GET response has been stripped by
        // Jackson's NON_NULL inclusion policy.
      };

      const machine = createRepositoryFormMachine({
        repositoryName: 'huggingface-proxy',
        preloadedRepository: preloadedRepositoryWithoutXet as any,
        format: 'huggingface',
      });
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'proxy' })).toBe(true);
      expect(state.context.data.xet).toBeUndefined();

      service.stop();
    });

    // Regression NEXUS-48685: same backward-compat guarantee as above, but
    // exercising the findRepository code path (no preloadedRepository). The
    // format-specific REST endpoint's response omits `xet` (Jackson
    // NON_NULL) for repositories with no persisted xet block; the form
    // machine must accept that shape without error and leave formData.xet
    // undefined so the facet's unchecked-by-default fallback applies.
    it('loads an existing HuggingFace proxy via findRepository when the response has no xet field', async () => {
      const responseWithoutXet = {
        name: 'huggingface-proxy',
        type: 'proxy' as const,
        format: 'huggingface',
        url: 'http://localhost:8081/repository/huggingface-proxy',
        online: true,
        recipe: 'huggingface-proxy',
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
        proxy: {
          remoteUrl: 'https://huggingface.co/',
        },
        // NOTE: no `xet` — Jackson stripped it because it's null.
      };

      restClient.get.mockImplementation((url: string) => {
        if (
          url ===
          '/service/rest/v1/repositories/huggingface/proxy/huggingface-proxy'
        ) {
          return Promise.resolve(responseWithoutXet);
        }
        if (url.includes('/blobstores')) {
          return Promise.resolve([{ name: 'default', type: 'File' }]);
        }
        if (url.includes('/routing-rules')) return Promise.resolve([]);
        if (url.includes('/cleanup-policies')) return Promise.resolve([]);
        if (url.includes('/repositories/recipes')) {
          return Promise.resolve([{ format: 'huggingface', type: 'proxy' }]);
        }
        if (url.includes('/internal/ui/repositories')) {
          return Promise.resolve([]);
        }
        return Promise.resolve([]);
      });

      const machine = createRepositoryFormMachine({
        repositoryName: 'huggingface-proxy',
        format: 'huggingface',
        repositoryType: 'proxy',
      });
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'proxy' })).toBe(true);
      expect(state.context.data.xet).toBeUndefined();

      service.stop();
    });
  });

  describe('global distribution', () => {
    afterEach(() => {
      global.NX.State.getValue.mockReset();
    });

    function withEnterprise(hasEnterprise: boolean) {
      global.NX.State.getValue.mockImplementation((key: string, defaultValue: unknown) =>
        key === 'hasEnterprise' ? hasEnterprise : defaultValue,
      );
    }

    function buildRepositoryWithoutGlobalDistribution() {
      return {
        name: 'maven-hosted',
        type: 'hosted' as const,
        format: 'maven2',
        online: true,
        storage: {
          blobStoreName: 'default',
          strictContentTypeValidation: true,
        },
      } as any;
    }

    it.each(['hosted', 'proxy', 'group'] as const)(
      'seeds globalDistribution=false in create mode for a %s repository',
      async (repositoryType) => {
        withEnterprise(true);
        const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType });
        const service = await startAndLoad(machine);

        expect(service.getSnapshot().context.data.storage?.globalDistribution).toBe(false);

        service.stop();
      },
    );

    it('leaves globalDistribution undefined in create mode when the edition is not entitled', async () => {
      withEnterprise(false);
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.data.storage?.globalDistribution).toBeUndefined();

      service.stop();
    });

    it('preserves globalDistribution=true from the API in edit mode', async () => {
      const machine = createRepositoryFormMachine({
        repositoryName: 'maven-hosted',
        preloadedRepository: {
          name: 'maven-hosted',
          type: 'hosted' as const,
          format: 'maven2',
          online: true,
          storage: {
            blobStoreName: 'default',
            strictContentTypeValidation: true,
            globalDistribution: true,
          },
        } as any,
        format: 'maven2',
      });
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.data.storage?.globalDistribution).toBe(true);

      service.stop();
    });

    /**
     * Repositories created before the feature existed carry no {@code globalDistribution}
     * attribute, so the API omits the field; without a default the checkbox would never render on
     * an existing repository.
     */
    it('defaults globalDistribution to false in edit mode when the API omits the field', async () => {
      withEnterprise(true);
      const machine = createRepositoryFormMachine({
        repositoryName: 'maven-hosted',
        preloadedRepository: buildRepositoryWithoutGlobalDistribution(),
        format: 'maven2',
      });
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.data.storage?.globalDistribution).toBe(false);

      service.stop();
    });

    it('leaves globalDistribution undefined in edit mode when the edition is not entitled', async () => {
      withEnterprise(false);
      const machine = createRepositoryFormMachine({
        repositoryName: 'maven-hosted',
        preloadedRepository: buildRepositoryWithoutGlobalDistribution(),
        format: 'maven2',
      });
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.data.storage?.globalDistribution).toBeUndefined();

      service.stop();
    });

    it('keeps globalDistribution when TYPE_CHANGE switches the repository type', async () => {
      withEnterprise(true);
      const machine = createRepositoryFormMachine({ format: 'maven2', repositoryType: 'hosted' });
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'storage.globalDistribution', value: true } as any);
      service.send({ type: 'TYPE_CHANGE', value: 'proxy' } as any);

      expect(service.getSnapshot().context.data.storage?.globalDistribution).toBe(true);

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

    it('loads cleanup policies from the internal endpoint so CE returns 200, not 404', async () => {
      const machine = createRepositoryFormMachine({ format: 'maven2' });
      const service = await startAndLoad(machine);

      expect(restClient.get).toHaveBeenCalledWith('/service/rest/internal/cleanup-policies');

      service.stop();
    });
  });
});
