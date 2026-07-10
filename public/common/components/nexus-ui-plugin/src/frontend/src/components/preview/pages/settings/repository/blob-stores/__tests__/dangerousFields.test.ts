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

import { hasDangerousFieldChanges, getDangerousFieldsChanged } from '../dangerousFields';
import type { BlobStoreFormData } from '../blobStoreFormMachine';

function makeFormData(overrides: Partial<BlobStoreFormData> = {}): BlobStoreFormData {
  return {
    name: 'test-store',
    type: 'file',
    softQuota: { enabled: false },
    path: '/data/blobs/test-store',
    bucketConfiguration: {},
    members: [],
    fillPolicy: '',
    ...overrides,
  };
}

describe('dangerousFields', () => {
  describe('hasDangerousFieldChanges', () => {
    describe('file blob store', () => {
      it('should return false when path is unchanged', () => {
        const pristine = makeFormData({ path: '/data/blobs/store1' });
        const current = makeFormData({ path: '/data/blobs/store1' });
        expect(hasDangerousFieldChanges(pristine, current, 'file')).toBe(false);
      });

      it('should return true when path is changed', () => {
        const pristine = makeFormData({ path: '/data/blobs/store1' });
        const current = makeFormData({ path: '/data/blobs/store2' });
        expect(hasDangerousFieldChanges(pristine, current, 'file')).toBe(true);
      });

      it('should return false when only non-dangerous fields change', () => {
        const pristine = makeFormData({ path: '/data/blobs/store1', softQuota: { enabled: false } });
        const current = makeFormData({ path: '/data/blobs/store1', softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 500 } });
        expect(hasDangerousFieldChanges(pristine, current, 'file')).toBe(false);
      });
    });

    describe('s3 blob store', () => {
      it('should return true when bucket name changes', () => {
        const pristine = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'us-east-1' }, encryption: null },
        });
        const current = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'other-bucket', region: 'us-east-1' }, encryption: null },
        });
        expect(hasDangerousFieldChanges(pristine, current, 's3')).toBe(true);
      });

      it('should return true when region changes', () => {
        const pristine = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'us-east-1' }, encryption: null },
        });
        const current = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'eu-west-1' }, encryption: null },
        });
        expect(hasDangerousFieldChanges(pristine, current, 's3')).toBe(true);
      });

      it('should return true when encryption changes', () => {
        const pristine = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'us-east-1' }, encryption: null },
        });
        const current = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'us-east-1' }, encryption: { encryptionType: 's3ManagedEncryption' } },
        });
        expect(hasDangerousFieldChanges(pristine, current, 's3')).toBe(true);
      });

      it('should return false when only non-dangerous s3 fields change', () => {
        const pristine = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'us-east-1', prefix: '' }, encryption: null },
        });
        const current = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'my-bucket', region: 'us-east-1', prefix: 'new-prefix' }, encryption: null },
        });
        expect(hasDangerousFieldChanges(pristine, current, 's3')).toBe(false);
      });
    });

    describe('azure blob store', () => {
      it('should return true when account name changes', () => {
        const pristine = makeFormData({
          type: 'azure',
          bucketConfiguration: { accountName: 'acct1', containerName: 'container1' },
        });
        const current = makeFormData({
          type: 'azure',
          bucketConfiguration: { accountName: 'acct2', containerName: 'container1' },
        });
        expect(hasDangerousFieldChanges(pristine, current, 'azure')).toBe(true);
      });

      it('should return true when container name changes', () => {
        const pristine = makeFormData({
          type: 'azure',
          bucketConfiguration: { accountName: 'acct1', containerName: 'container1' },
        });
        const current = makeFormData({
          type: 'azure',
          bucketConfiguration: { accountName: 'acct1', containerName: 'container2' },
        });
        expect(hasDangerousFieldChanges(pristine, current, 'azure')).toBe(true);
      });

      it('should return false when azure config is unchanged', () => {
        const pristine = makeFormData({
          type: 'azure',
          bucketConfiguration: { accountName: 'acct1', containerName: 'container1' },
        });
        const current = makeFormData({
          type: 'azure',
          bucketConfiguration: { accountName: 'acct1', containerName: 'container1' },
        });
        expect(hasDangerousFieldChanges(pristine, current, 'azure')).toBe(false);
      });
    });

    describe('google blob store', () => {
      it('should return true when bucket name changes', () => {
        const pristine = makeFormData({
          type: 'google',
          bucketConfiguration: { bucket: { name: 'gcs-bucket', projectId: 'proj-1', region: 'us' }, encryption: { encryptionType: 'default' } },
        });
        const current = makeFormData({
          type: 'google',
          bucketConfiguration: { bucket: { name: 'gcs-bucket-2', projectId: 'proj-1', region: 'us' }, encryption: { encryptionType: 'default' } },
        });
        expect(hasDangerousFieldChanges(pristine, current, 'google')).toBe(true);
      });

      it('should return true when project ID changes', () => {
        const pristine = makeFormData({
          type: 'google',
          bucketConfiguration: { bucket: { name: 'gcs-bucket', projectId: 'proj-1', region: 'us' }, encryption: { encryptionType: 'default' } },
        });
        const current = makeFormData({
          type: 'google',
          bucketConfiguration: { bucket: { name: 'gcs-bucket', projectId: 'proj-2', region: 'us' }, encryption: { encryptionType: 'default' } },
        });
        expect(hasDangerousFieldChanges(pristine, current, 'google')).toBe(true);
      });

      it('should return false when google config is unchanged', () => {
        const pristine = makeFormData({
          type: 'google',
          bucketConfiguration: { bucket: { name: 'gcs-bucket', projectId: 'proj-1', region: 'us' }, encryption: { encryptionType: 'default' } },
        });
        const current = makeFormData({
          type: 'google',
          bucketConfiguration: { bucket: { name: 'gcs-bucket', projectId: 'proj-1', region: 'us' }, encryption: { encryptionType: 'default' } },
        });
        expect(hasDangerousFieldChanges(pristine, current, 'google')).toBe(false);
      });
    });

    describe('group blob store', () => {
      it('should return true when members change', () => {
        const pristine = makeFormData({ type: 'group', members: ['store-a', 'store-b'] });
        const current = makeFormData({ type: 'group', members: ['store-a', 'store-c'] });
        expect(hasDangerousFieldChanges(pristine, current, 'group')).toBe(true);
      });

      it('should return true when members are added', () => {
        const pristine = makeFormData({ type: 'group', members: ['store-a'] });
        const current = makeFormData({ type: 'group', members: ['store-a', 'store-b'] });
        expect(hasDangerousFieldChanges(pristine, current, 'group')).toBe(true);
      });

      it('should return true when members are removed', () => {
        const pristine = makeFormData({ type: 'group', members: ['store-a', 'store-b'] });
        const current = makeFormData({ type: 'group', members: ['store-a'] });
        expect(hasDangerousFieldChanges(pristine, current, 'group')).toBe(true);
      });

      it('should return false when members are unchanged', () => {
        const pristine = makeFormData({ type: 'group', members: ['store-a', 'store-b'] });
        const current = makeFormData({ type: 'group', members: ['store-a', 'store-b'] });
        expect(hasDangerousFieldChanges(pristine, current, 'group')).toBe(false);
      });
    });

    describe('edge cases', () => {
      it('should return false for unknown type', () => {
        const pristine = makeFormData({ type: 'unknown' });
        const current = makeFormData({ type: 'unknown', path: '/changed' });
        expect(hasDangerousFieldChanges(pristine, current, 'unknown')).toBe(false);
      });

      it('should handle null/undefined type gracefully', () => {
        const pristine = makeFormData();
        const current = makeFormData({ path: '/changed' });
        expect(hasDangerousFieldChanges(pristine, current, null as any)).toBe(false);
        expect(hasDangerousFieldChanges(pristine, current, undefined as any)).toBe(false);
      });

      it('should treat null and undefined as equal', () => {
        const pristine = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'b', region: 'r' }, encryption: null },
        });
        const current = makeFormData({
          type: 's3',
          bucketConfiguration: { bucket: { name: 'b', region: 'r' } },
        });
        expect(hasDangerousFieldChanges(pristine, current, 's3')).toBe(false);
      });

      it('should be case-insensitive on type', () => {
        const pristine = makeFormData({ path: '/old' });
        const current = makeFormData({ path: '/new' });
        expect(hasDangerousFieldChanges(pristine, current, 'File')).toBe(true);
        expect(hasDangerousFieldChanges(pristine, current, 'FILE')).toBe(true);
      });
    });
  });

  describe('getDangerousFieldsChanged', () => {
    it('should return empty array when no dangerous fields changed', () => {
      const pristine = makeFormData({ path: '/same' });
      const current = makeFormData({ path: '/same' });
      expect(getDangerousFieldsChanged(pristine, current, 'file')).toEqual([]);
    });

    it('should return changed field info for file type', () => {
      const pristine = makeFormData({ path: '/old' });
      const current = makeFormData({ path: '/new' });
      const result = getDangerousFieldsChanged(pristine, current, 'file');
      expect(result).toEqual([{ field: 'path', label: 'Path' }]);
    });

    it('should return multiple changed fields for s3', () => {
      const pristine = makeFormData({
        type: 's3',
        bucketConfiguration: { bucket: { name: 'old-bucket', region: 'us-east-1' }, encryption: null },
      });
      const current = makeFormData({
        type: 's3',
        bucketConfiguration: { bucket: { name: 'new-bucket', region: 'eu-west-1' }, encryption: { encryptionType: 'kms' } },
      });
      const result = getDangerousFieldsChanged(pristine, current, 's3');
      expect(result).toHaveLength(3);
      expect(result.map(r => r.label)).toContain('Bucket Name');
      expect(result.map(r => r.label)).toContain('Region');
      expect(result.map(r => r.label)).toContain('Encryption');
    });

    it('should return only the changed fields, not all dangerous fields', () => {
      const pristine = makeFormData({
        type: 'azure',
        bucketConfiguration: { accountName: 'acct1', containerName: 'container1' },
      });
      const current = makeFormData({
        type: 'azure',
        bucketConfiguration: { accountName: 'acct1', containerName: 'container2' },
      });
      const result = getDangerousFieldsChanged(pristine, current, 'azure');
      expect(result).toEqual([{ field: 'bucketConfiguration.containerName', label: 'Container Name' }]);
    });
  });
});
