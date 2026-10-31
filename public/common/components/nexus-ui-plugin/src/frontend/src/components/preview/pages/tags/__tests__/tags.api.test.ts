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
  parseApiError: jest.fn((err: any) => ({
    message: err?.response?.data?.message || err?.message || 'Error',
    status: err?.response?.status,
  })),
  urlBuilder: {
    tags: {
      list: () => '/service/rest/v1/tags',
      get: (name: string) => `/service/rest/v1/tags/${encodeURIComponent(name)}`,
      create: () => '/service/rest/v1/tags',
      delete: (name: string) => `/service/rest/v1/tags/${encodeURIComponent(name)}`,
      filtered: () => '/service/rest/internal/ui/tags/filtered',
    },
  },
}));

import { fetchTagDetail, createTag } from '../tags.api';
import { restClient } from '../../../../../interface/api';
import { mockTagDetail } from './mockData';

// Get mock references
const mockRestClient = restClient as jest.Mocked<typeof restClient>;

describe('tags.api', () => {
  beforeEach(() => {
    jest.clearAllMocks();
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

});
