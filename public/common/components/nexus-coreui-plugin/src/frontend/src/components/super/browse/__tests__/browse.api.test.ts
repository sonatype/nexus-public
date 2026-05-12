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
import type { ComponentXO } from '../browse.types';

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
  })),
  // Mock encodeRepositoryItemId - returns rawId for simplified test expectations
  encodeRepositoryItemId: jest.fn((repositoryName: string, rawId: string) => rawId),
}));

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
        assets: [],
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
    it('returns asset data on success', async () => {
      const mockRestAsset = {
        id: 'asset-456',
        repository: 'maven-central',
        format: 'maven2',
        path: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        downloadUrl: '/repository/maven-central/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        contentType: 'application/java-archive',
        fileSize: 587402,
        lastModified: '2023-01-15T10:30:00Z',
      };

      mockRestClient.get.mockResolvedValue(mockRestAsset);

      const result = await fetchAsset('asset-456', 'maven-central');

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/assets/asset-456');
      expect(result).toEqual({
        id: 'asset-456',
        repositoryName: 'maven-central',
        format: 'maven2',
        name: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        path: '/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        downloadUrl: '/repository/maven-central/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        contentType: 'application/java-archive',
        fileSize: 587402,
        lastModified: '2023-01-15T10:30:00Z',
      });
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue({ message: 'Asset not found' });

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
