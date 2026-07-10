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

import { renderHook, waitFor, act } from '@testing-library/react';
import {
  useBlobStoresList,
  useBlobStoreTypes,
  useBlobStore,
  useAzureConnectionTest,
  useS3DropdownValues,
  useBlobStorePromote
} from '../useBlobStores';

// Mock the REST API at the path the source uses
// Variables used in mock must have 'mock' prefix for hoisting
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

describe('useBlobStores hooks', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('useBlobStoresList', () => {
    it('calls correct REST endpoint - GET service/rest/internal/ui/blobstores', async () => {
      const mockData = [
        { name: 'default', type: 'File', unavailable: false },
        { name: 's3-bucket', type: 'S3', unavailable: false, unlimited: true }
      ];

      mockRestClient.get.mockResolvedValue(mockData);

      const { result } = renderHook(() => useBlobStoresList());

      expect(result.current.loading).toBe(true);

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // CRITICAL: Verify correct REST endpoint is called
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/blobstores');
      expect(result.current.blobStores.length).toBe(2);
      expect(result.current.error).toBeNull();
    });

    it('handles error', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Network Error'));

      const { result } = renderHook(() => useBlobStoresList());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBe('Network Error');
      expect(result.current.blobStores).toEqual([]);
    });

    it('transforms data correctly - sets available flag', async () => {
      const mockData = [
        { name: 'default', type: 'File', unavailable: true, blobCount: 100, totalSizeInBytes: 500 },
        { name: 's3-bucket', type: 'S3', unavailable: false, unlimited: true, availableSpaceInBytes: 1000 }
      ];

      mockRestClient.get.mockResolvedValue(mockData);

      const { result } = renderHook(() => useBlobStoresList());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Unavailable blob store should have available: false, and -1 for counts
      expect(result.current.blobStores[0].available).toBe(false);
      expect(result.current.blobStores[0].blobCount).toBe(-1);
      expect(result.current.blobStores[0].totalSizeInBytes).toBe(-1);

      // Available blob store with unlimited should have Infinity for available space
      expect(result.current.blobStores[1].available).toBe(true);
      expect(result.current.blobStores[1].availableSpaceInBytes).toBe(Infinity);
    });

    it('refetch function works', async () => {
      const mockData = [{ name: 'default', type: 'File', unavailable: false }];
      mockRestClient.get.mockResolvedValue(mockData);

      const { result } = renderHook(() => useBlobStoresList());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        result.current.refetch();
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(mockRestClient.get).toHaveBeenCalledTimes(2);
    });
  });

  describe('useBlobStoreTypes', () => {
    it('calls correct REST endpoints - GET /service/rest/internal/ui/blobstores/types and /quotaTypes', async () => {
      const mockTypes = [{ id: 'File', name: 'File' }, { id: 's3', name: 'S3' }];
      const mockQuotaTypes = [{ id: 'spaceUsed', name: 'Space Used' }];

      mockRestClient.get
        .mockResolvedValueOnce(mockTypes)
        .mockResolvedValueOnce(mockQuotaTypes);

      const { result } = renderHook(() => useBlobStoreTypes());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // CRITICAL: Verify correct REST endpoints are called
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/blobstores/types');
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/blobstores/quotaTypes');
      expect(result.current.types).toEqual(mockTypes);
      expect(result.current.quotaTypes).toEqual(mockQuotaTypes);
      expect(result.current.error).toBeNull();
    });

    it('handles error when fetching types', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Failed to load blob store types'));

      const { result } = renderHook(() => useBlobStoreTypes());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBe('Failed to load blob store types');
      expect(result.current.types).toEqual([]);
      expect(result.current.quotaTypes).toEqual([]);
    });

    it('handles non-array response data', async () => {
      mockRestClient.get
        .mockResolvedValueOnce({ id: 'File', name: 'File' }) // Not an array
        .mockResolvedValueOnce([]);

      const { result } = renderHook(() => useBlobStoreTypes());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Should handle non-array gracefully
      expect(result.current.types).toEqual([]);
      expect(result.current.quotaTypes).toEqual([]);
    });
  });

  describe('useBlobStore', () => {
    it('calls correct REST endpoints - GET service/rest/v1/blobstores/{type}/{name} and usage endpoint', async () => {
      const mockBlobStore = { name: 'test', type: 'File', path: '/data' };
      const mockUsage = { blobStoreUsage: 0, repositoryUsage: 2 };

      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce(mockUsage);

      const { result } = renderHook(() => useBlobStore('test', 'file'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // CRITICAL: Verify correct REST endpoints are called
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/blobstores/file/test');
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/blobstores/usage/test');
      expect(result.current.repositoryUsage).toBe(2);
    });

    // -----------------------------------------------------------------------
    // REGRESSION: soft quota transformation on load
    // Bug: API response has no "enabled" field and stores limit in bytes.
    // BlobStoresPage binds checkbox to softQuota.enabled — was always false.
    // -----------------------------------------------------------------------

    it('sets softQuota.enabled=true when API response includes a softQuota object', async () => {
      const mockBlobStore = {
        name: 'test',
        type: 'file',
        path: '/data',
        softQuota: { type: 'spaceUsedQuota', limit: 104857600 }, // 100 MB in bytes
      };
      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });

      const { result } = renderHook(() => useBlobStore('test', 'file'));

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.blobStore?.softQuota?.enabled).toBe(true);
    });

    it('converts softQuota.limit from bytes to MB when loading', async () => {
      // 100 MB = 104857600 bytes; the form field must show 100, not 104857600
      const mockBlobStore = {
        name: 'test',
        type: 'file',
        path: '/data',
        softQuota: { type: 'spaceUsedQuota', limit: 104857600 },
      };
      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });

      const { result } = renderHook(() => useBlobStore('test', 'file'));

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.blobStore?.softQuota?.limit).toBe(100);
    });

    it('converts a 1-GiB softQuota.limit from bytes to MB (1073741824 → 1024)', async () => {
      const mockBlobStore = {
        name: 'test',
        type: 'file',
        path: '/data',
        softQuota: { type: 'spaceRemainingQuota', limit: 1073741824 },
      };
      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });

      const { result } = renderHook(() => useBlobStore('test', 'file'));

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.blobStore?.softQuota?.enabled).toBe(true);
      expect(result.current.blobStore?.softQuota?.limit).toBe(1024);
    });

    it('leaves softQuota undefined when the API response has no softQuota', async () => {
      const mockBlobStore = { name: 'no-quota', type: 'file', path: '/data' };
      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });

      const { result } = renderHook(() => useBlobStore('no-quota', 'file'));

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.blobStore?.softQuota).toBeUndefined();
    });

    // -----------------------------------------------------------------------
    // REGRESSION: type casing in GET URL
    // Bug: list API returns typeId="File" (capital); hook built URL with that
    // casing → /blobstores/File/name → 404 → blobStore stayed null.
    // -----------------------------------------------------------------------

    it('lowercases type in GET URL even when caller passes mixed-case type', async () => {
      const mockBlobStore = { name: 'my-store', type: 'file', path: '/data' };
      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });

      // Simulate what BlobStoresList does: passes store.typeId which may be "File"
      const { result } = renderHook(() => useBlobStore('my-store', 'File'));

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file/my-store' // must be lowercase
      );
    });

    it.each(['S3', 'Azure', 'Google'])(
      'lowercases mixed-case type "%s" in GET URL',
      async (mixedType) => {
        const mockBlobStore = { name: 'cloud-store', type: mixedType.toLowerCase() };
        mockRestClient.get
          .mockResolvedValueOnce(mockBlobStore)
          .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });

        const { result } = renderHook(() => useBlobStore('cloud-store', mixedType));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(mockRestClient.get).toHaveBeenCalledWith(
          `/service/rest/v1/blobstores/${mixedType.toLowerCase()}/cloud-store`
        );
      }
    );

    // -----------------------------------------------------------------------
    // REGRESSION: type casing in PUT/POST URL
    // Bug: save() used data.type as-is in the URL, causing 404 on update.
    // -----------------------------------------------------------------------

    it('lowercases type in PUT URL when saving an existing blob store', async () => {
      const mockBlobStore = { name: 'existing', type: 'file', path: '/data' };
      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });
      mockRestClient.put.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore('existing', 'file'));
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        // Caller may pass mixed-case type on save
        await result.current.save({ name: 'existing', type: 'File', path: '/new-path' });
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file/existing', // must be lowercase
        expect.any(Object)
      );
    });

    it.each(['S3', 'Azure', 'Google'])(
      'lowercases mixed-case type "%s" in PUT URL when saving',
      async (mixedType) => {
        const mockBlobStore = { name: 'cloud', type: mixedType.toLowerCase() };
        mockRestClient.get
          .mockResolvedValueOnce(mockBlobStore)
          .mockResolvedValueOnce({ blobStoreUsage: 0, repositoryUsage: 0 });
        mockRestClient.put.mockResolvedValue({ success: true });

        const { result } = renderHook(() => useBlobStore('cloud', mixedType));
        await waitFor(() => expect(result.current.loading).toBe(false));

        await act(async () => {
          await result.current.save({ name: 'cloud', type: mixedType });
        });

        expect(mockRestClient.put).toHaveBeenCalledWith(
          `/service/rest/v1/blobstores/${mixedType.toLowerCase()}/cloud`,
          expect.any(Object)
        );
      }
    );

    it('lowercases type in POST URL when creating a new blob store', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore(undefined));

      await act(async () => {
        await result.current.save({ name: 'new-store', type: 'File' });
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file', // must be lowercase
        expect.any(Object)
      );
    });

    // -----------------------------------------------------------------------
    // REGRESSION: formatBlobStoreData — softQuota MB→bytes on save
    // The form stores limit in MB; the API expects bytes.
    // -----------------------------------------------------------------------

    it('converts softQuota.limit from MB to bytes when saving', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore(undefined));

      await act(async () => {
        await result.current.save({
          name: 'quota-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 }, // 100 MB in form
        });
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file',
        expect.objectContaining({
          softQuota: { type: 'spaceUsedQuota', limit: 104857600 }, // 100 MB → bytes
        })
      );
    });

    it('strips softQuota from save payload when enabled is false', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore(undefined));

      await act(async () => {
        await result.current.save({
          name: 'no-quota-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: false },
        });
      });

      const [, body] = mockRestClient.post.mock.calls[0];
      expect(body).not.toHaveProperty('softQuota');
    });

    it('strips the UI-only enabled flag from the softQuota save payload', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore(undefined));

      await act(async () => {
        await result.current.save({
          name: 'quota-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 50 },
        });
      });

      const [, body] = mockRestClient.post.mock.calls[0];
      // The REST API does not accept an "enabled" field — it must be stripped
      expect(body.softQuota).not.toHaveProperty('enabled');
      expect(body.softQuota).toEqual({ type: 'spaceUsedQuota', limit: 52428800 });
    });

    it('does not fetch when name is not provided', () => {
      const { result } = renderHook(() => useBlobStore(undefined));

      expect(result.current.loading).toBe(false);
      expect(result.current.blobStore).toBeNull();
      expect(mockRestClient.get).not.toHaveBeenCalled();
    });

    it('does not fetch when type is not provided', () => {
      const { result } = renderHook(() => useBlobStore('test', undefined));

      expect(result.current.loading).toBe(false);
      expect(result.current.blobStore).toBeNull();
      expect(mockRestClient.get).not.toHaveBeenCalled();
    });

    it('save function calls correct REST endpoint for CREATE - POST service/rest/v1/blobstores/{type}', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore(undefined));

      await act(async () => {
        await result.current.save({ name: 'new-store', type: 'file' });
      });

      // CRITICAL: Verify correct REST endpoint is called for CREATE
      // Note: 'type' is removed from body (it's in the URL)
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file',
        { name: 'new-store' }
      );
    });

    it('save function calls correct REST endpoint for UPDATE - PUT service/rest/v1/blobstores/{type}/{name}', async () => {
      const mockBlobStore = { name: 'existing-store', type: 'file', path: '/data' };
      const mockUsage = { blobStoreUsage: 0, repositoryUsage: 2 };

      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce(mockUsage);
      mockRestClient.put.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore('existing-store', 'file'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.save({ name: 'existing-store', type: 'file', path: '/new-path' });
      });

      // CRITICAL: Verify correct REST endpoint is called for UPDATE
      // Note: 'type' is removed from body (it's in the URL)
      expect(mockRestClient.put).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file/existing-store',
        { name: 'existing-store', path: '/new-path' }
      );
    });

    it('remove function calls correct REST endpoint - DELETE service/rest/v1/blobstores/{name}', async () => {
      const mockBlobStore = { name: 'test', type: 'file' };
      const mockUsage = { blobStoreUsage: 0, repositoryUsage: 0 };

      mockRestClient.get
        .mockResolvedValueOnce(mockBlobStore)
        .mockResolvedValueOnce(mockUsage);
      mockRestClient.delete.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStore('test', 'file'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.remove();
      });

      // CRITICAL: Verify correct REST endpoint is called for DELETE
      expect(mockRestClient.delete).toHaveBeenCalledWith('/service/rest/v1/blobstores/test');
    });

    it('handles error when fetching blob store', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Blob store not found'));

      const { result } = renderHook(() => useBlobStore('nonexistent', 'file'));

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBe('Blob store not found');
      expect(result.current.blobStore).toBeNull();
    });
  });

  describe('useAzureConnectionTest', () => {
    it('calls correct REST endpoint for test - POST service/rest/internal/ui/azureblobstore/test-connection', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useAzureConnectionTest());

      expect(result.current.testing).toBe(false);
      expect(result.current.result).toBeNull();

      await act(async () => {
        await result.current.testConnection({
          accountName: 'test-account',
          containerName: 'test-container',
          authenticationMethod: 'ENVIRONMENTVARIABLE'
        });
      });

      // CRITICAL: Verify correct REST endpoint is called
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/internal/ui/azureblobstore/test-connection',
        {
          accountName: 'test-account',
          containerName: 'test-container',
          authenticationMethod: 'ENVIRONMENTVARIABLE'
        }
      );
      expect(result.current.result).toBe('success');
    });

    it('appends blob store name to URL for existing blob stores', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useAzureConnectionTest());

      await act(async () => {
        await result.current.testConnection({
          blobStoreName: 'existing-azure',
          accountName: 'test-account',
          containerName: 'test-container',
          authenticationMethod: 'ACCOUNTKEY',
          accountKey: 'secret-key'
        });
      });

      // CRITICAL: Verify correct REST endpoint is called with blob store name
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/internal/ui/azureblobstore/test-connection/existing-azure',
        expect.objectContaining({
          blobStoreName: 'existing-azure',
          accountName: 'test-account'
        })
      );
    });

    it('handles connection test failure', async () => {
      mockRestClient.post.mockRejectedValue(new Error('Connection failed'));

      const { result } = renderHook(() => useAzureConnectionTest());

      await act(async () => {
        await result.current.testConnection({
          accountName: 'test',
          containerName: 'container',
          authenticationMethod: 'ENVIRONMENTVARIABLE'
        });
      });

      expect(result.current.result).toBe('error');
    });

    it('reset clears the result', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useAzureConnectionTest());

      await act(async () => {
        await result.current.testConnection({
          accountName: 'test',
          containerName: 'container',
          authenticationMethod: 'ENVIRONMENTVARIABLE'
        });
      });

      expect(result.current.result).toBe('success');

      act(() => {
        result.current.reset();
      });

      expect(result.current.result).toBeNull();
    });
  });

  describe('useS3DropdownValues', () => {
    it('extracts S3 dropdown values from types endpoint', async () => {
      const mockS3DropdownValues = {
        regions: [{ id: 'us-east-1', name: 'US East' }],
        encryptionTypes: [{ id: 's3', name: 'S3 Managed' }]
      };

      const mockTypes = [
        { id: 'file', name: 'File' },
        { id: 's3', name: 'S3', dropDownValues: mockS3DropdownValues }
      ];

      mockRestClient.get.mockResolvedValue(mockTypes);

      const { result } = renderHook(() => useS3DropdownValues());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // CRITICAL: Verify it calls the types endpoint and extracts S3 values
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/blobstores/types');
      expect(result.current.values).toEqual(mockS3DropdownValues);
    });

    it('returns null if S3 type not found', async () => {
      const mockTypes = [{ id: 'file', name: 'File' }];

      mockRestClient.get.mockResolvedValue(mockTypes);

      const { result } = renderHook(() => useS3DropdownValues());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.values).toBeNull();
    });

    it('handles error gracefully', async () => {
      mockRestClient.get.mockRejectedValue(new Error('Network Error'));

      const { result } = renderHook(() => useS3DropdownValues());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.values).toBeNull();
    });
  });

  describe('useBlobStorePromote', () => {
    it('calls correct REST endpoint - POST service/rest/v1/blobstores/group/convert/{name}/{newName}', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useBlobStorePromote());

      expect(result.current.promoting).toBe(false);

      await act(async () => {
        await result.current.promote('original-blob-store', 'new-group-name');
      });

      // CRITICAL: Verify correct REST endpoint is called
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/group/convert/original-blob-store/new-group-name'
      );
    });

    it('handles promotion error', async () => {
      mockRestClient.post.mockRejectedValue(new Error('Promotion failed'));

      const { result } = renderHook(() => useBlobStorePromote());

      await expect(act(async () => {
        await result.current.promote('original', 'new-group');
      })).rejects.toThrow('Promotion failed');
    });
  });
});
