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
import { createBlobStoreFormMachine, BLOB_STORE_TYPE_IDS } from '../blobStoreFormMachine';

// Mock the API interface at the path the source uses
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  API_INTERNAL_UI: '/service/rest/internal/ui',
  ENDPOINTS: {
    BLOBSTORES: '/service/rest/v1/blobstores',
  },
  restClient: {
    get: jest.fn().mockResolvedValue([]),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

const MOCK_BLOB_STORE_TYPES = [
  { id: 'file', name: 'File' },
  { id: 's3', name: 'S3' },
  { id: 'azure', name: 'Azure Cloud Storage' },
  { id: 'google', name: 'Google Cloud Storage' },
  { id: 'group', name: 'Group' },
];

const MOCK_QUOTA_TYPES = [
  { id: 'spaceRemainingQuota', name: 'Space Remaining' },
  { id: 'spaceUsedQuota', name: 'Space Used' },
];

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createBlobStoreFormMachine>,
  loadData?: Record<string, unknown>
) {
  // Mock the API responses for the load service
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('blobstores/types')) {
      return Promise.resolve(MOCK_BLOB_STORE_TYPES);
    }
    if (url.includes('blobstores/quotaTypes')) {
      return Promise.resolve(MOCK_QUOTA_TYPES);
    }
    if (url.includes('blobstores/usage')) {
      return Promise.resolve({ blobStoreUsage: 0, repositoryUsage: 0 });
    }
    return Promise.resolve(loadData || []);
  });

  const service = interpret(machine).start();

  // Wait for loading to complete
  await waitFor(service, (state) => state.matches('editing'));

  return service;
}

describe('blobStoreFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to file type in create mode', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.type).toBe(BLOB_STORE_TYPE_IDS.FILE);
      expect(state.matches({ editing: BLOB_STORE_TYPE_IDS.FILE })).toBe(true);

      service.stop();
    });

    it('loads blob store types and quota types', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.blobStoreTypes).toEqual(MOCK_BLOB_STORE_TYPES);
      expect(state.context.quotaTypes).toEqual(MOCK_QUOTA_TYPES);

      service.stop();
    });
  });

  describe('type variant sub-states', () => {
    const allTypes = [
      BLOB_STORE_TYPE_IDS.FILE,
      BLOB_STORE_TYPE_IDS.S3,
      BLOB_STORE_TYPE_IDS.AZURE,
      BLOB_STORE_TYPE_IDS.GOOGLE,
      BLOB_STORE_TYPE_IDS.GROUP,
    ];

    it.each(allTypes)('transitions to %s sub-state on TYPE_CHANGE', async (type) => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: type } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: type })).toBe(true);
      expect(state.context.data.type).toBe(type);

      service.stop();
    });

    it('transitions between all type variants', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Start at file (default)
      expect(service.getSnapshot().matches({ editing: 'file' })).toBe(true);

      // Transition through all types
      for (const type of allTypes) {
        service.send({ type: 'TYPE_CHANGE', value: type } as any);
        expect(service.getSnapshot().matches({ editing: type })).toBe(true);
      }

      // Transition back to file
      service.send({ type: 'TYPE_CHANGE', value: 'file' } as any);
      expect(service.getSnapshot().matches({ editing: 'file' })).toBe(true);

      service.stop();
    });
  });

  describe('sub-state metadata', () => {
    it('file sub-state has correct field metadata', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'file' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('File');
      expect(typeMeta.fields).toEqual(['path']);
      expect(typeMeta.requiredFields).toEqual(['path']);

      service.stop();
    });

    it('s3 sub-state has bucket configuration fields', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('S3');
      expect(typeMeta.fields).toContain('bucketConfiguration.bucket.name');
      expect(typeMeta.fields).toContain('bucketConfiguration.bucket.region');
      expect(typeMeta.fields).toContain('bucketConfiguration.bucketSecurity.accessKeyId');
      expect(typeMeta.fields).toContain('bucketConfiguration.encryption.encryptionType');
      expect(typeMeta.fields).toContain('bucketConfiguration.advancedBucketConnection.endpoint');
      expect(typeMeta.requiredFields).toEqual(['bucketConfiguration.bucket.name']);

      service.stop();
    });

    it('azure sub-state has account and container fields', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'azure' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Azure');
      expect(typeMeta.fields).toContain('bucketConfiguration.accountName');
      expect(typeMeta.fields).toContain('bucketConfiguration.containerName');
      expect(typeMeta.fields).toContain('bucketConfiguration.authentication.authenticationMethod');
      expect(typeMeta.requiredFields).toContain('bucketConfiguration.accountName');
      expect(typeMeta.requiredFields).toContain('bucketConfiguration.containerName');

      service.stop();
    });

    it('google sub-state has bucket and authentication fields', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'google' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Google Cloud');
      expect(typeMeta.fields).toContain('bucketConfiguration.bucket.name');
      expect(typeMeta.fields).toContain('bucketConfiguration.bucket.projectId');
      expect(typeMeta.fields).toContain('bucketConfiguration.bucketSecurity.authenticationMethod');
      expect(typeMeta.requiredFields).toEqual(['bucketConfiguration.bucket.name']);

      service.stop();
    });

    it('group sub-state has members and fill policy fields', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Group');
      expect(typeMeta.fields).toEqual(['members', 'fillPolicy']);
      expect(typeMeta.requiredFields).toEqual(['members', 'fillPolicy']);

      service.stop();
    });

    it('every type variant has metadata with fields and requiredFields', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      const allTypes = Object.values(BLOB_STORE_TYPE_IDS);

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
  });

  describe('validation per type', () => {
    it('validates file type requires path', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set name, stay at file type (default)
      service.send({ type: 'UPDATE', name: 'name', value: 'test-store' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.path).toBeTruthy();

      service.stop();
    });

    it('validates s3 type requires bucket name', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'test-store' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['bucketConfiguration.bucket.name']).toBeTruthy();

      service.stop();
    });

    it('validates azure type requires account name and container name', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'azure' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'test-store' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['bucketConfiguration.accountName']).toBeTruthy();
      expect(state.context.validationErrors['bucketConfiguration.containerName']).toBeTruthy();

      service.stop();
    });

    it('validates google type requires bucket name', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'google' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'test-store' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['bucketConfiguration.bucket.name']).toBeTruthy();

      service.stop();
    });

    it('validates group type requires members and fill policy', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'test-store' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.members).toBeTruthy();
      expect(state.context.validationErrors.fillPolicy).toBeTruthy();

      service.stop();
    });

    it('validates name is required for all types', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Try to submit with no name
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates name format (alphanumeric, hyphens, underscores)', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'invalid name!' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toContain('letters, numbers');

      service.stop();
    });

    it('validates soft quota requires type and limit when enabled', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-store' } as any);
      service.send({ type: 'UPDATE', name: 'path', value: '/data/blobs' } as any);
      service.send({ type: 'UPDATE', name: 'softQuota.enabled', value: true } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['softQuota.type']).toBeTruthy();
      expect(state.context.validationErrors['softQuota.limit']).toBeTruthy();

      service.stop();
    });
  });

  describe('TYPE_CHANGE clears type-specific configuration', () => {
    it('resets path when switching from file to s3', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set file path
      service.send({ type: 'UPDATE', name: 'path', value: '/data/blobs/test' } as any);
      expect(service.getSnapshot().context.data.path).toBe('/data/blobs/test');

      // Switch to S3
      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);

      // Path should be cleared, bucketConfiguration should be initialized
      const state = service.getSnapshot();
      expect(state.context.data.path).toBe('');
      expect(state.context.data.bucketConfiguration).toBeDefined();
      expect((state.context.data.bucketConfiguration as any).bucket).toBeDefined();

      service.stop();
    });

    it('resets bucketConfiguration when switching from s3 to file', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to S3 first
      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);
      service.send({ type: 'UPDATE', name: 'bucketConfiguration.bucket.name', value: 'my-bucket' } as any);

      // Switch to file
      service.send({ type: 'TYPE_CHANGE', value: 'file' } as any);

      // S3 config should be cleared
      const state = service.getSnapshot();
      expect(state.context.data.bucketConfiguration).toEqual({});
      expect(state.context.data.path).toBe('');

      service.stop();
    });

    it('resets group fields when switching from group to file', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Switch to group
      service.send({ type: 'TYPE_CHANGE', value: 'group' } as any);
      service.send({ type: 'UPDATE', name: 'members', value: ['store-a', 'store-b'] } as any);
      service.send({ type: 'UPDATE', name: 'fillPolicy', value: 'writeToFirst' } as any);

      expect(service.getSnapshot().context.data.members).toEqual(['store-a', 'store-b']);

      // Switch to file
      service.send({ type: 'TYPE_CHANGE', value: 'file' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.members).toEqual([]);
      expect(state.context.data.fillPolicy).toBe('');

      service.stop();
    });

    it('preserves name and softQuota when switching types', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set name and soft quota
      service.send({ type: 'UPDATE', name: 'name', value: 'my-store' } as any);
      service.send({ type: 'UPDATE', name: 'softQuota.enabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'softQuota.type', value: 'spaceUsedQuota' } as any);

      // Switch to S3
      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('my-store');
      expect(state.context.data.softQuota.enabled).toBe(true);
      expect(state.context.data.softQuota.type).toBe('spaceUsedQuota');

      service.stop();
    });

    it('initializes S3 defaults with bucket structure on TYPE_CHANGE', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);

      const config = service.getSnapshot().context.data.bucketConfiguration as any;
      expect(config.bucket).toEqual({ region: 'DEFAULT', name: '', prefix: '' });
      expect(config.bucketSecurity).toEqual({});
      expect(config.encryption).toBeNull();
      expect(config.advancedBucketConnection).toEqual({});
      expect(config.failoverBuckets).toEqual([]);

      service.stop();
    });

    it('initializes Azure defaults with authentication on TYPE_CHANGE', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'azure' } as any);

      const config = service.getSnapshot().context.data.bucketConfiguration as any;
      expect(config.accountName).toBe('');
      expect(config.containerName).toBe('');
      expect(config.authentication.authenticationMethod).toBe('ENVIRONMENTVARIABLE');

      service.stop();
    });

    it('initializes Google defaults with authentication on TYPE_CHANGE', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 'google' } as any);

      const config = service.getSnapshot().context.data.bucketConfiguration as any;
      expect(config.bucket).toEqual({ name: '', prefix: '' });
      expect(config.bucketSecurity.authenticationMethod).toBe('applicationDefault');
      expect(config.encryption.encryptionType).toBe('default');

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates nested bucketConfiguration via dot notation', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TYPE_CHANGE', value: 's3' } as any);
      service.send({ type: 'UPDATE', name: 'bucketConfiguration.bucket.name', value: 'my-s3-bucket' } as any);

      const config = service.getSnapshot().context.data.bucketConfiguration as any;
      expect(config.bucket.name).toBe('my-s3-bucket');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-store' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createBlobStoreFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-store' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads file blob store data and enters file sub-state', async () => {
      const preloadedBlobStore = {
        name: 'default',
        type: 'file',
        path: '/data/blobs/default',
        softQuota: { type: 'spaceUsedQuota', limit: 1073741824 },
      };

      const machine = createBlobStoreFormMachine('default', 'file', preloadedBlobStore);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'file' })).toBe(true);
      expect(state.context.data.name).toBe('default');
      expect(state.context.data.path).toBe('/data/blobs/default');
      expect(state.context.data.softQuota.enabled).toBe(true);
      expect(state.context.data.softQuota.type).toBe('spaceUsedQuota');

      service.stop();
    });

    it('loads s3 blob store data and enters s3 sub-state', async () => {
      const preloadedBlobStore = {
        name: 's3-store',
        type: 's3',
        bucketConfiguration: {
          bucket: { region: 'us-east-1', name: 'my-nexus-bucket', prefix: 'nexus/' },
          bucketSecurity: { accessKeyId: 'AKIA123' },
        },
      };

      const machine = createBlobStoreFormMachine('s3-store', 's3', preloadedBlobStore);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 's3' })).toBe(true);
      expect(state.context.data.name).toBe('s3-store');
      expect((state.context.data.bucketConfiguration as any).bucket.name).toBe('my-nexus-bucket');
      expect((state.context.data.bucketConfiguration as any).bucket.region).toBe('us-east-1');

      service.stop();
    });

    it('loads group blob store data and enters group sub-state', async () => {
      const preloadedBlobStore = {
        name: 'group-store',
        type: 'group',
        members: ['store-a', 'store-b'],
        fillPolicy: 'writeToFirst',
      };

      const machine = createBlobStoreFormMachine('group-store', 'group', preloadedBlobStore);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'group' })).toBe(true);
      expect(state.context.data.name).toBe('group-store');
      expect(state.context.data.members).toEqual(['store-a', 'store-b']);
      expect(state.context.data.fillPolicy).toBe('writeToFirst');

      service.stop();
    });
  });
});
