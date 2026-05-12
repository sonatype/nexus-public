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

import { renderHook, act } from '@testing-library/react';

import { useContentSelectorsApi } from '../useContentSelectorsApi';
import { CONTENT_SELECTOR_API } from '../types';

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
    message: err?.response?.data?.message || err?.response?.data?.[0]?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

describe('useContentSelectorsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchContentSelectors', () => {
    it('fetches content selectors successfully', async () => {
      const mockSelectors = [
        { name: 'selector1', type: 'csel', description: 'Test', expression: 'format == "maven2"' },
        { name: 'selector2', type: 'csel', description: '', expression: 'format == "npm"' },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockSelectors);

      const { result } = renderHook(() => useContentSelectorsApi());

      const selectors = await act(async () => result.current.fetchContentSelectors());

      expect(mockRestClient.get).toHaveBeenCalledWith(CONTENT_SELECTOR_API.BASE_URL);
      expect(selectors).toEqual(mockSelectors);
    });

    it('handles fetch error', async () => {
      mockRestClient.get.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useContentSelectorsApi());

      await expect(result.current.fetchContentSelectors()).rejects.toThrow('Network error');
    });
  });

  describe('fetchContentSelector', () => {
    it('fetches a single content selector successfully', async () => {
      const mockSelector = { name: 'test-selector', type: 'csel', description: 'Test', expression: 'format == "raw"' };
      mockRestClient.get.mockResolvedValueOnce(mockSelector);

      const { result } = renderHook(() => useContentSelectorsApi());

      const selector = await act(async () => result.current.fetchContentSelector('test-selector'));

      expect(mockRestClient.get).toHaveBeenCalledWith(`${CONTENT_SELECTOR_API.BASE_URL}/test-selector`);
      expect(selector).toEqual(mockSelector);
    });
  });

  describe('fetchRepositories', () => {
    it('fetches repositories successfully', async () => {
      const mockRepos = [
        { id: '*', name: 'All Repositories' },
        { id: 'repo1', name: 'Repository 1' },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockRepos);

      const { result } = renderHook(() => useContentSelectorsApi());

      const repos = await act(async () => result.current.fetchRepositories());

      expect(mockRestClient.get).toHaveBeenCalledWith(CONTENT_SELECTOR_API.REPOSITORIES_URL);
      expect(repos).toEqual(mockRepos);
    });
  });

  describe('createContentSelector', () => {
    it('creates a content selector successfully', async () => {
      const mockSelector = {
        name: 'new-selector',
        type: 'csel',
        description: 'New selector',
        expression: 'format == "docker"',
      };
      mockRestClient.post.mockResolvedValueOnce(mockSelector);

      const { result } = renderHook(() => useContentSelectorsApi());

      let createdSelector;
      await act(async () => {
        createdSelector = await result.current.createContentSelector(mockSelector);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(CONTENT_SELECTOR_API.BASE_URL, expect.any(Object));
      expect(createdSelector).toEqual(mockSelector);
    });

    it('sets error on create failure', async () => {
      mockRestClient.post.mockRejectedValueOnce({
        response: { data: { message: 'Selector already exists' } },
      });

      const { result } = renderHook(() => useContentSelectorsApi());

      await act(async () => {
        try {
          await result.current.createContentSelector({
            name: 'existing-selector',
            type: 'csel',
            description: '',
            expression: 'format == "raw"',
          });
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Selector already exists');
    });
  });

  describe('updateContentSelector', () => {
    it('updates a content selector successfully', async () => {
      const mockSelector = {
        name: 'test-selector',
        type: 'csel',
        description: 'Updated description',
        expression: 'format == "maven2" and path =^ "/org"',
      };
      mockRestClient.put.mockResolvedValueOnce(mockSelector);

      const { result } = renderHook(() => useContentSelectorsApi());

      await act(async () => {
        await result.current.updateContentSelector('test-selector', mockSelector);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        `${CONTENT_SELECTOR_API.BASE_URL}/test-selector`,
        expect.any(Object)
      );
    });
  });

  describe('deleteContentSelector', () => {
    it('deletes a content selector successfully', async () => {
      mockRestClient.delete.mockResolvedValueOnce({});

      const { result } = renderHook(() => useContentSelectorsApi());

      await act(async () => {
        await result.current.deleteContentSelector('test-selector');
      });

      expect(mockRestClient.delete).toHaveBeenCalledWith(`${CONTENT_SELECTOR_API.BASE_URL}/test-selector`);
    });

    it('sets error on delete failure', async () => {
      mockRestClient.delete.mockRejectedValueOnce({
        response: { data: { message: 'Selector is in use' } },
      });

      const { result } = renderHook(() => useContentSelectorsApi());

      await act(async () => {
        try {
          await result.current.deleteContentSelector('test-selector');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Selector is in use');
    });
  });

  describe('previewContentSelector', () => {
    it('previews content selector results using POST', async () => {
      // Default UI returns { results: [{ name: '...' }, ...] }
      const mockResponse = {
        results: [
          { name: '/org/example/artifact-1.0.jar' },
          { name: '/org/example/artifact-2.0.jar' },
        ],
      };
      mockRestClient.post.mockResolvedValueOnce(mockResponse);

      const { result } = renderHook(() => useContentSelectorsApi());

      const preview = await act(async () =>
        result.current.previewContentSelector('*', 'csel', 'format == "maven2"')
      );

      expect(preview).toEqual(['/org/example/artifact-1.0.jar', '/org/example/artifact-2.0.jar']);
      expect(mockRestClient.post).toHaveBeenCalledWith(
        CONTENT_SELECTOR_API.PREVIEW_URL,
        {
          repository: '*',
          type: 'csel',
          expression: 'format == "maven2"',
        }
      );
    });

    it('handles preview error', async () => {
      mockRestClient.post.mockRejectedValueOnce({
        response: { data: [{ message: 'Invalid expression' }] },
      });

      const { result } = renderHook(() => useContentSelectorsApi());

      await expect(
        result.current.previewContentSelector('*', 'csel', 'invalid expression')
      ).rejects.toThrow('Invalid expression');
    });
  });
});

