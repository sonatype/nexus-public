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

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
  },
  parseApiError: jest.fn((err: any) => ({
    message: err?.response?.data?.message || err?.message || 'Error',
    status: err?.response?.status,
  })),
}));

import { fetchTags, fetchTagDetail, fetchFilteredTags, createTag, deleteTag } from '../tags.api';
import { restClient } from '@/utils/api';
import { mockTags, mockTagDetail } from './mockData';

// Get mock references
const mockRestClient = restClient as jest.Mocked<typeof restClient>;

describe('tags.api', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchTags', () => {
    it('should fetch tags using REST API', async () => {
      mockRestClient.get.mockResolvedValue(mockTags);

      const result = await fetchTags();

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/tags');
      expect(result).toEqual(mockTags);
    });

    it('should return empty array when response is not an array', async () => {
      mockRestClient.get.mockResolvedValue(null);

      const result = await fetchTags();

      expect(result).toEqual([]);
    });

    it('should throw error when API fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const error = new Error('Network error');
      mockRestClient.get.mockRejectedValue(error);

      await expect(fetchTags()).rejects.toThrow('Network error');
      errorSpy.mockRestore();
    });
  });

  describe('fetchTagDetail', () => {
    it('should fetch tag detail using REST API', async () => {
      mockRestClient.get.mockResolvedValue(mockTagDetail);

      const result = await fetchTagDetail('release-1.0');

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/tags/release-1.0');
      expect(result).toEqual(mockTagDetail);
    });

    it('should encode tag name in URL', async () => {
      mockRestClient.get.mockResolvedValue(mockTagDetail);

      await fetchTagDetail('tag/with/slashes');

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/tags/tag%2Fwith%2Fslashes');
    });

    it('should throw error when API fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const error = new Error('Network error');
      mockRestClient.get.mockRejectedValue(error);

      await expect(fetchTagDetail('release-1.0')).rejects.toThrow('Network error');
      errorSpy.mockRestore();
    });
  });

  describe('fetchFilteredTags', () => {
    const mockFilters = {
      nameFilter: 'release',
      componentCounts: ['1-10', '10-100'],
      activityDays: ['7', '30'],
    };

    const mockResponse = {
      items: mockTags,
      totalCount: 10,
      page: 0,
      pageSize: 25,
    };

    it('should fetch filtered tags with all parameters', async () => {
      mockRestClient.get.mockResolvedValue(mockResponse);

      const result = await fetchFilteredTags(
        mockFilters,
        'name',
        'asc',
        0,
        25
      );

      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/service/rest/internal/ui/tags/filtered?')
      );
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('nameFilter=release')
      );
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('componentCounts=1-10%2C10-100')
      );
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('activityDays=7%2C30')
      );
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('sortField=name')
      );
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('sortDirection=asc')
      );
      expect(result).toEqual(mockResponse);
    });

    it('should fetch filtered tags with minimal parameters', async () => {
      mockRestClient.get.mockResolvedValue(mockResponse);

      const emptyFilters = {
        nameFilter: '',
        componentCounts: [],
        activityDays: [],
      };

      await fetchFilteredTags(emptyFilters, 'name', 'desc', 1, 50);

      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('page=1')
      );
      expect(mockRestClient.get).toHaveBeenCalledWith(
        expect.stringContaining('pageSize=50')
      );
    });

    it('should throw error when API fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const error = new Error('Network error');
      mockRestClient.get.mockRejectedValue(error);

      await expect(
        fetchFilteredTags(mockFilters, 'name', 'asc', 0, 25)
      ).rejects.toThrow('Network error');
      errorSpy.mockRestore();
    });
  });

  describe('createTag', () => {
    it('should create a tag with name and attributes', async () => {
      const newTag = { name: 'release-2.0', firstCreated: '', lastUpdated: '', attributes: { env: 'prod' } };
      mockRestClient.post.mockResolvedValue(newTag);

      const result = await createTag('release-2.0', { env: 'prod' });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/tags',
        { name: 'release-2.0', attributes: { env: 'prod' } }
      );
      expect(result).toEqual(newTag);
    });

    it('should create a tag with empty attributes when none provided', async () => {
      const newTag = { name: 'simple-tag', firstCreated: '', lastUpdated: '', attributes: {} };
      mockRestClient.post.mockResolvedValue(newTag);

      await createTag('simple-tag');

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/tags',
        { name: 'simple-tag', attributes: {} }
      );
    });

    it('should throw error when create fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      mockRestClient.post.mockRejectedValue(new Error('Conflict'));

      await expect(createTag('duplicate')).rejects.toThrow('Conflict');
      errorSpy.mockRestore();
    });
  });

  describe('deleteTag', () => {
    it('should delete a tag by name', async () => {
      mockRestClient.delete.mockResolvedValue(undefined);

      await deleteTag('old-tag');

      expect(mockRestClient.delete).toHaveBeenCalledWith('/service/rest/v1/tags/old-tag');
    });

    it('should throw error when delete fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      mockRestClient.delete.mockRejectedValue(new Error('Not found'));

      await expect(deleteTag('missing')).rejects.toThrow('Not found');
      errorSpy.mockRestore();
    });
  });
});
