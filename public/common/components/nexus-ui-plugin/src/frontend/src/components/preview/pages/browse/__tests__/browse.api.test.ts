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

// Mock the REST API from the relative path that the source imports from
// Note: jest.mock is hoisted, so we use jest.fn() inside the factory
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
  })),
  ENDPOINTS: {
    REPOSITORIES: '/service/rest/v1/repositories',
    COMPONENTS: '/service/rest/v1/components',
    ASSETS: '/service/rest/v1/assets',
    SEARCH: '/service/rest/v1/search',
    REPOSITORY_BROWSE: (repositoryName: string) => `/service/rest/v1/repositories/${repositoryName}/browse`,
  },
  urlBuilder: {
    components: {
      get: (id: string) => `/service/rest/v1/components/${id}`,
      delete: (id: string) => `/service/rest/v1/components/${id}`,
    },
    assets: {
      get: (id: string) => `/service/rest/v1/assets/${id}`,
      delete: (id: string) => `/service/rest/v1/assets/${id}`,
    },
    query: (baseUrl: string, params: Record<string, string | number | boolean | undefined>) => {
      const searchParams = new URLSearchParams();
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined) {
          searchParams.append(key, String(value));
        }
      }
      const queryString = searchParams.toString();
      return queryString ? `${baseUrl}?${queryString}` : baseUrl;
    },
  },
  encodeRepositoryItemId: jest.fn((repositoryName: string, rawId: string) => rawId),
}));

// fetchAsset uses ExtDirect (coreui_Component.readAsset) — mock the helper here.
jest.mock('../../../../../interface/ExtAPIUtils', () => ({
  __esModule: true,
  default: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn(),
  },
}));

import {
  fetchRepositories,
  fetchBrowseNodes,
  fetchComponent,
  fetchAsset,
  deleteComponent,
  deleteAsset,
  deleteFolder,
  searchInRepository,
  showSuccessMessage,
  showErrorMessage,
} from '../browse.api';
import { restClient } from '../../../../../interface/api';
import ExtAPIUtils from '../../../../../interface/ExtAPIUtils';
import type { ComponentXO } from '../browse.types';

// Get mock references
const mockRestClient = restClient as jest.Mocked<typeof restClient>;
const mockExtAPI = ExtAPIUtils as unknown as {
  extAPIRequest: jest.Mock;
  checkForErrorAndExtract: jest.Mock;
};

describe('browse.api', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchRepositories', () => {
    it('returns repository list on success', async () => {
      const mockRestRepositories = [
        { name: 'maven-central', type: 'proxy', format: 'maven2' },
        { name: 'npm-hosted', type: 'hosted', format: 'npm' },
      ];

      mockRestClient.get.mockResolvedValue(mockRestRepositories);

      const result = await fetchRepositories();

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/repositories');
      expect(result).toEqual([
        { id: 'maven-central', name: 'maven-central', format: 'maven2', type: 'proxy' },
        { id: 'npm-hosted', name: 'npm-hosted', format: 'npm', type: 'hosted' },
      ]);
    });

    it('returns empty array when no data', async () => {
      mockRestClient.get.mockResolvedValue([]);

      const result = await fetchRepositories();

      expect(result).toEqual([]);
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      await expect(fetchRepositories()).rejects.toThrow('Network error');
      consoleSpy.mockRestore();
    });
  });

  describe('fetchBrowseNodes', () => {
    it('returns nodes for path on success', async () => {
      const mockRestNodes = [
        { id: 'org', text: 'org', type: 'folder', leaf: false },
        { id: 'com', text: 'com', type: 'folder', leaf: false },
      ];

      mockRestClient.get.mockResolvedValue(mockRestNodes);

      const result = await fetchBrowseNodes({ repositoryName: 'maven-central', node: '/' });

      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/maven-central/browse?path=%2F'
      );
      expect(result).toEqual([
        { id: 'org', text: 'org', type: 'folder', leaf: false },
        { id: 'com', text: 'com', type: 'folder', leaf: false },
      ]);
    });

    it('handles nested path', async () => {
      const mockRestNodes = [
        { id: 'org/apache/maven', text: 'maven', type: 'component', leaf: false, componentId: '123' },
      ];

      mockRestClient.get.mockResolvedValue(mockRestNodes);

      const result = await fetchBrowseNodes({
        repositoryName: 'maven-central',
        node: '/org/apache',
      });

      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/maven-central/browse?path=%2Forg%2Fapache'
      );
      expect(result).toEqual([
        { id: 'org/apache/maven', text: 'maven', type: 'component', leaf: false, componentId: '123' },
      ]);
    });

    it('returns empty array on error', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      await expect(
        fetchBrowseNodes({ repositoryName: 'maven-central', node: '/' })
      ).rejects.toThrow('Network error');
      consoleSpy.mockRestore();
    });
  });

  describe('fetchComponent', () => {
    it('returns component data on success', async () => {
      const mockRestComponent = {
        id: 'comp-123',
        repository: 'maven-central',
        format: 'maven2',
        group: 'org.apache',
        name: 'commons-lang3',
        version: '3.12.0',
        assets: [],
      };

      mockRestClient.get.mockResolvedValue(mockRestComponent);

      const result = await fetchComponent('comp-123', 'maven-central');

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/components/comp-123');
      expect(result).toEqual({
        id: 'comp-123',
        repositoryName: 'maven-central',
        format: 'maven2',
        group: 'org.apache',
        name: 'commons-lang3',
        version: '3.12.0',
      });
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue({ message: 'Component not found' });

      await expect(fetchComponent('invalid-id', 'maven-central')).rejects.toThrow(
        'Component not found'
      );
      consoleSpy.mockRestore();
    });
  });

  describe('fetchAsset', () => {
    it('calls ExtDirect coreui_Component.readAsset with raw assetId and repository', async () => {
      mockExtAPI.checkForErrorAndExtract.mockReturnValue({
        id: 'asset-456',
        name: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        format: 'maven2',
        repositoryName: 'maven-central',
        contentType: 'application/java-archive',
        size: 587402,
        attributes: {
          maven2: { groupId: 'org.apache.commons', artifactId: 'commons-lang3', version: '3.12.0' },
          checksum: { sha1: 'aaa', sha256: 'bbb' },
        },
      });

      const result = await fetchAsset('asset-456', 'maven-central');

      expect(mockExtAPI.extAPIRequest).toHaveBeenCalledWith('coreui_Component', 'readAsset', {
        data: ['asset-456', 'maven-central'],
      });
      expect(result).toMatchObject({
        id: 'asset-456',
        repositoryName: 'maven-central',
        format: 'maven2',
        name: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        path: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        downloadUrl: '/repository/maven-central/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        contentType: 'application/java-archive',
        size: 587402,
        // checksum lifted from attributes.checksum to top level for the Checksums section
        checksum: { sha1: 'aaa', sha256: 'bbb' },
      });
    });

    it('encodes special characters in download URL path segments', async () => {
      mockExtAPI.checkForErrorAndExtract.mockReturnValue({
        id: 'asset-special',
        name: '/path with spaces/file+name.tgz',
        format: 'npm',
        repositoryName: 'my repo',
        attributes: {},
      });

      const result = await fetchAsset('asset-special', 'my repo');

      // Repository name and path segments should be URL-encoded
      expect(result.downloadUrl).toBe('/repository/my%20repo/path%20with%20spaces/file%2Bname.tgz');
    });

    it('preserves the full attributes bag including firewall facet (NEXUS-52920)', async () => {
      mockExtAPI.checkForErrorAndExtract.mockReturnValue({
        id: 'asset-fw',
        name: '/debug/-/debug-4.3.7.tgz',
        format: 'npm',
        repositoryName: 'example2',
        attributes: {
          npm: { name: 'debug', version: '4.3.7' },
          firewall: {
            firewall_audited: 'true',
            quarantine_status: 'released',
            policy_violations_count: '0',
          },
          content: { last_modified: 1234567890 },
        },
      });

      const result = await fetchAsset('asset-fw', 'example2');

      expect(result.attributes).toEqual({
        npm: { name: 'debug', version: '4.3.7' },
        firewall: {
          firewall_audited: 'true',
          quarantine_status: 'released',
          policy_violations_count: '0',
        },
        content: { last_modified: 1234567890 },
      });
    });

    it('maps date fields and exposes blobUpdated/createdBy/registryUrl', async () => {
      mockExtAPI.checkForErrorAndExtract.mockReturnValue({
        id: 'asset-789',
        name: '/com/example/lib/1.0/lib-1.0.jar',
        format: 'maven2',
        repositoryName: 'maven-hosted',
        size: 12345,
        contentType: 'application/java-archive',
        blobCreated: '2024-01-01T00:00:00Z',
        blobUpdated: '2024-01-02T00:00:00Z',
        lastDownloaded: '2024-01-03T00:00:00Z',
        blobRef: 'default@abc123',
        componentId: 'comp-1',
        createdBy: 'admin',
        createdByIp: '127.0.0.1',
        registryUrl: 'docker.example.com:443',
        attributes: { maven2: {} },
      });

      const result = await fetchAsset('asset-789', 'maven-hosted');

      expect(result).toMatchObject({
        blobCreated: '2024-01-01T00:00:00Z',
        blobUpdated: '2024-01-02T00:00:00Z',
        lastDownloaded: '2024-01-03T00:00:00Z',
        blobRef: 'default@abc123',
        componentId: 'comp-1',
        createdBy: 'admin',
        createdByIp: '127.0.0.1',
        registryUrl: 'docker.example.com:443',
      });
    });

    it('converts numeric timestamps (milliseconds) to ISO strings', async () => {
      // Unix timestamp in milliseconds (June 11, 2026)
      const timestampMs = 1749649800000;
      mockExtAPI.checkForErrorAndExtract.mockReturnValue({
        id: 'asset-ts',
        name: '/test/file.jar',
        format: 'maven2',
        repositoryName: 'maven-central',
        blobCreated: timestampMs,
        blobUpdated: timestampMs,
        lastDownloaded: timestampMs,
        attributes: {},
      });

      const result = await fetchAsset('asset-ts', 'maven-central');

      // Verify numeric timestamps are converted to ISO strings
      expect(result.blobCreated).toBe(new Date(timestampMs).toISOString());
      expect(result.blobUpdated).toBe(new Date(timestampMs).toISOString());
      expect(result.lastDownloaded).toBe(new Date(timestampMs).toISOString());
    });

    it('throws when ExtDirect returns no data', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockExtAPI.checkForErrorAndExtract.mockReturnValue(undefined);

      await expect(fetchAsset('missing', 'maven-central')).rejects.toThrow('Asset not found');
      consoleSpy.mockRestore();
    });

    it('throws when ExtDirect helper throws (e.g. exception or success=false)', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockExtAPI.checkForErrorAndExtract.mockImplementation(() => {
        throw new Error('Asset not found');
      });

      await expect(fetchAsset('invalid-id', 'maven-central')).rejects.toThrow('Asset not found');
      consoleSpy.mockRestore();
    });
  });

  describe('deleteComponent', () => {
    it('handles success and returns deleted IDs', async () => {
      const mockComponent: ComponentXO = {
        id: 'comp-123',
        repositoryName: 'maven-hosted',
        format: 'maven2',
        group: 'com.example',
        name: 'my-lib',
        version: '1.0.0',
      };

      mockRestClient.delete.mockResolvedValue(undefined);

      const result = await deleteComponent(mockComponent);

      expect(mockRestClient.delete).toHaveBeenCalledWith('/service/rest/v1/components/comp-123');
      expect(result).toEqual(['comp-123']);
    });

    it('throws error on failure', async () => {
      const mockComponent: ComponentXO = {
        id: 'comp-123',
        repositoryName: 'maven-hosted',
        format: 'maven2',
        group: 'com.example',
        name: 'my-lib',
        version: '1.0.0',
      };

      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.delete.mockRejectedValue({ message: 'Permission denied' });

      await expect(deleteComponent(mockComponent)).rejects.toThrow('Permission denied');
      consoleSpy.mockRestore();
    });
  });

  describe('deleteAsset', () => {
    it('handles success', async () => {
      mockRestClient.delete.mockResolvedValue(undefined);

      await expect(deleteAsset('asset-123', 'maven-hosted')).resolves.toBeUndefined();

      expect(mockRestClient.delete).toHaveBeenCalledWith('/service/rest/v1/assets/asset-123');
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.delete.mockRejectedValue({ message: 'Asset locked' });

      await expect(deleteAsset('asset-123', 'maven-hosted')).rejects.toThrow('Asset locked');
      consoleSpy.mockRestore();
    });

    it('handles Go asset IDs with special characters using real base64 encoding', async () => {
      // deleteAsset now expects the ID to be already encoded (the encoded RepositoryItemIDXO from fetchAsset)
      mockRestClient.delete.mockResolvedValue(undefined);
      const goRawId = 'v2.2.1+incompatible';
      // Use URL-safe base64 without padding (matching encodeRepositoryItemId implementation)
      const expectedEncoded = btoa(`go-proxy:${goRawId}`)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');

      // Pass the already-encoded ID (as fetchAsset would return)
      await deleteAsset(expectedEncoded, 'go-proxy');

      expect(mockRestClient.delete).toHaveBeenCalledWith(
        `/service/rest/v1/assets/${expectedEncoded}`
      );
    });
  });

  describe('deleteFolder', () => {
    it('calls REST API with correct URL', async () => {
      mockRestClient.delete.mockResolvedValue(undefined);

      await deleteFolder('/org/apache/maven', 'maven-hosted');

      expect(mockRestClient.delete).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/maven-hosted/browse?path=%2Forg%2Fapache%2Fmaven'
      );
    });

    it('handles success', async () => {
      mockRestClient.delete.mockResolvedValue(undefined);

      await expect(deleteFolder('/org/apache', 'maven-hosted')).resolves.toBeUndefined();
    });

    it('handles error', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.delete.mockRejectedValue({ message: 'Permission denied' });

      await expect(deleteFolder('/org/apache', 'maven-hosted')).rejects.toThrow(
        'Permission denied'
      );
      consoleSpy.mockRestore();
    });

    it('handles 404 error', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.delete.mockRejectedValue({
        response: { status: 404, data: { message: 'Folder not found' } },
      });

      await expect(deleteFolder('/nonexistent', 'maven-hosted')).rejects.toThrow(
        'Folder not found'
      );
      consoleSpy.mockRestore();
    });
  });

  describe('searchInRepository', () => {
    it('returns search results on success', async () => {
      const mockSearchResponse = {
        items: [
          {
            id: 'comp-1',
            repository: 'maven-central',
            format: 'maven2',
            group: 'org.apache.commons',
            name: 'commons-lang3',
            version: '3.12.0',
          },
          {
            id: 'comp-2',
            repository: 'maven-central',
            format: 'maven2',
            group: 'org.apache.commons',
            name: 'commons-io',
            version: '2.11.0',
          },
        ],
        continuationToken: null,
      };

      mockRestClient.get.mockResolvedValue(mockSearchResponse);

      const result = await searchInRepository('maven-central', 'commons', 20);

      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/search?repository=maven-central&q=commons&limit=20'
      );
      expect(result).toEqual(mockSearchResponse.items);
    });

    it('returns empty array when no results', async () => {
      mockRestClient.get.mockResolvedValue({ items: [] });

      const result = await searchInRepository('maven-central', 'nonexistent', 20);

      expect(result).toEqual([]);
    });

    it('uses default limit of 20', async () => {
      mockRestClient.get.mockResolvedValue({ items: [] });

      await searchInRepository('maven-central', 'test');

      expect(mockRestClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/search?repository=maven-central&q=test&limit=20'
      );
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue(new Error('Search failed'));

      await expect(searchInRepository('maven-central', 'test')).rejects.toThrow('Search failed');
      consoleSpy.mockRestore();
    });

    it('handles missing items in response', async () => {
      mockRestClient.get.mockResolvedValue({});

      const result = await searchInRepository('maven-central', 'test');

      expect(result).toEqual([]);
    });
  });

  describe('utility functions', () => {
    const originalNX = (window as any).NX;
    const mockToast = {
      success: jest.fn(),
      error: jest.fn(),
    };

    beforeEach(() => {
      (window as any).NX = {
        Messages: {
          success: jest.fn(),
          error: jest.fn(),
        },
        Permissions: {
          check: jest.fn().mockReturnValue(true),
        },
      };
      (window as any).__nexusToast = mockToast;
      window.location.hash = '';
    });

    afterEach(() => {
      (window as any).NX = originalNX;
      delete (window as any).__nexusToast;
    });

    it('showSuccessMessage routes to Radix toast in Preview UI mode', () => {
      window.location.hash = '#preview/browse/welcome';
      showSuccessMessage('test message');
      expect(mockToast.success).toHaveBeenCalledWith('test message');
      expect((window as any).NX.Messages.success).not.toHaveBeenCalled();
    });

    it('showSuccessMessage routes to ExtJS in Default UI mode', () => {
      window.location.hash = '#browse/welcome';
      showSuccessMessage('test message');
      expect((window as any).NX.Messages.success).toHaveBeenCalledWith('test message');
      expect(mockToast.success).not.toHaveBeenCalled();
    });

    it('showErrorMessage routes to Radix toast in Preview UI mode', () => {
      window.location.hash = '#preview/browse/welcome';
      showErrorMessage('error message');
      expect(mockToast.error).toHaveBeenCalledWith('error message');
      expect((window as any).NX.Messages.error).not.toHaveBeenCalled();
    });

    it('showErrorMessage routes to ExtJS in Default UI mode', () => {
      window.location.hash = '#browse/welcome';
      showErrorMessage('error message');
      expect((window as any).NX.Messages.error).toHaveBeenCalledWith('error message');
      expect(mockToast.error).not.toHaveBeenCalled();
    });
  });
});
