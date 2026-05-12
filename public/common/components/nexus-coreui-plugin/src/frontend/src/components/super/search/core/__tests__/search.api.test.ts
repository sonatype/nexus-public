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
  GA_SEARCH_API,
  buildSearchUrl,
  buildSuggestUrl,
  buildDetailUrl,
  buildVersionAssetsUrl,
  gaSearchApi,
  GASearchError,
} from '../search.api';

describe('search.api', () => {
  describe('GA_SEARCH_API constants', () => {
    it('defines correct base path', () => {
      expect(GA_SEARCH_API.BASE_PATH).toBe('/service/rest/v1/search/ga');
    });

    it('defines correct search endpoint', () => {
      expect(GA_SEARCH_API.SEARCH).toBe('/service/rest/v1/search/ga');
    });

    it('defines correct suggest endpoint', () => {
      expect(GA_SEARCH_API.SUGGEST).toBe('/service/rest/v1/search/ga/suggest');
    });

    it('defines correct detail endpoint template', () => {
      expect(GA_SEARCH_API.DETAIL).toBe('/service/rest/v1/search/ga/:gaId');
    });

    it('defines correct version assets endpoint template', () => {
      expect(GA_SEARCH_API.VERSION_ASSETS).toBe(
        '/service/rest/v1/search/ga/:gaId/versions/:version/assets'
      );
    });
  });

  describe('buildSearchUrl', () => {
    it('builds URL with minimal params', () => {
      const url = buildSearchUrl({ format: 'maven' });

      expect(url).toContain(GA_SEARCH_API.SEARCH);
      expect(url).toContain('format=maven');
    });

    it('includes query parameter when provided', () => {
      const url = buildSearchUrl({ format: 'maven', query: 'commons-lang' });

      expect(url).toContain('q=commons-lang');
    });

    it('includes groupId parameter when provided', () => {
      const url = buildSearchUrl({ format: 'maven', groupId: 'org.apache.commons' });

      expect(url).toContain('maven.groupId=org.apache.commons');
    });

    it('includes artifactId parameter when provided', () => {
      const url = buildSearchUrl({ format: 'maven', artifactId: 'commons-lang3' });

      expect(url).toContain('maven.artifactId=commons-lang3');
    });

    it('includes repository parameter when provided', () => {
      const url = buildSearchUrl({ format: 'maven', repository: 'maven-central' });

      expect(url).toContain('repository=maven-central');
    });

    it('includes sort parameter when provided', () => {
      const url = buildSearchUrl({ format: 'maven', sort: 'lastUpdated' });

      expect(url).toContain('sort=lastUpdated');
    });

    it('includes sort direction when provided', () => {
      const url = buildSearchUrl({ format: 'maven', sortDirection: 'desc' });

      expect(url).toContain('direction=desc');
    });

    it('includes continuation token when provided', () => {
      const url = buildSearchUrl({ format: 'maven', continuationToken: 'abc123' });

      expect(url).toContain('continuationToken=abc123');
    });

    it('includes limit parameter when provided', () => {
      const url = buildSearchUrl({ format: 'maven', limit: 25 });

      expect(url).toContain('limit=25');
    });

    it('builds complete URL with all parameters', () => {
      const url = buildSearchUrl({
        format: 'maven',
        query: 'test',
        groupId: 'com.example',
        artifactId: 'my-artifact',
        repository: 'releases',
        sort: 'name',
        sortDirection: 'asc',
        continuationToken: 'token123',
        limit: 50,
      });

      expect(url).toContain('format=maven');
      expect(url).toContain('q=test');
      expect(url).toContain('maven.groupId=com.example');
      expect(url).toContain('maven.artifactId=my-artifact');
      expect(url).toContain('repository=releases');
      expect(url).toContain('sort=name');
      expect(url).toContain('direction=asc');
      expect(url).toContain('continuationToken=token123');
      expect(url).toContain('limit=50');
    });
  });

  describe('buildSuggestUrl', () => {
    it('builds URL with required params', () => {
      const url = buildSuggestUrl({ query: 'comm', format: 'maven' });

      expect(url).toContain(GA_SEARCH_API.SUGGEST);
      expect(url).toContain('q=comm');
      expect(url).toContain('format=maven');
    });

    it('includes limit when provided', () => {
      const url = buildSuggestUrl({ query: 'test', format: 'maven', limit: 5 });

      expect(url).toContain('limit=5');
    });
  });

  describe('buildDetailUrl', () => {
    it('builds URL with gaId', () => {
      const url = buildDetailUrl('maven:org.apache.commons:commons-lang3');

      expect(url).toBe(
        '/service/rest/v1/search/ga/maven%3Aorg.apache.commons%3Acommons-lang3'
      );
    });

    it('properly encodes special characters', () => {
      const url = buildDetailUrl('maven:com.example:my-artifact');

      expect(url).toContain(encodeURIComponent('maven:com.example:my-artifact'));
    });
  });

  describe('buildVersionAssetsUrl', () => {
    it('builds URL with gaId and version', () => {
      const url = buildVersionAssetsUrl(
        'maven:org.apache.commons:commons-lang3',
        '3.12.0'
      );

      expect(url).toContain('/service/rest/v1/search/ga/');
      expect(url).toContain('/versions/');
      expect(url).toContain('/assets');
      expect(url).toContain(encodeURIComponent('maven:org.apache.commons:commons-lang3'));
      expect(url).toContain('3.12.0');
    });

    it('includes repository query param when provided', () => {
      const url = buildVersionAssetsUrl(
        'maven:com.example:artifact',
        '1.0.0',
        'maven-central'
      );

      expect(url).toContain('repository=maven-central');
    });

    it('does not include repository param when not provided', () => {
      const url = buildVersionAssetsUrl('maven:com.example:artifact', '1.0.0');

      expect(url).not.toContain('repository=');
      expect(url).not.toContain('?');
    });

    it('properly encodes version with special characters', () => {
      const url = buildVersionAssetsUrl(
        'maven:com.example:artifact',
        '1.0.0-SNAPSHOT'
      );

      expect(url).toContain(encodeURIComponent('1.0.0-SNAPSHOT'));
    });
  });

  describe('GASearchError', () => {
    it('creates error with message and status code', () => {
      const error = new GASearchError('Not found', 404);

      expect(error.message).toBe('Not found');
      expect(error.statusCode).toBe(404);
      expect(error.name).toBe('GASearchError');
    });

    it('is an instance of Error', () => {
      const error = new GASearchError('Error', 500);

      expect(error).toBeInstanceOf(Error);
    });
  });

  describe('gaSearchApi', () => {
    const mockFetch = jest.fn();
    const originalFetch = global.fetch;

    beforeEach(() => {
      global.fetch = mockFetch;
      mockFetch.mockClear();
    });

    afterEach(() => {
      global.fetch = originalFetch;
    });

    describe('search', () => {
      it('calls fetch with correct URL and headers', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ items: [], totalCount: 0 }),
        });

        await gaSearchApi.search({ format: 'maven', query: 'test' });

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/service/rest/v1/search/ga'),
          expect.objectContaining({
            method: 'GET',
            headers: { Accept: 'application/json' },
          })
        );
      });

      it('throws GASearchError on non-ok response', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 500,
        });

        await expect(gaSearchApi.search({ format: 'maven' })).rejects.toThrow(
          GASearchError
        );
      });
    });

    describe('suggest', () => {
      it('calls fetch with correct URL', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ suggestions: [] }),
        });

        await gaSearchApi.suggest({ query: 'test', format: 'maven' });

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/service/rest/v1/search/ga/suggest'),
          expect.any(Object)
        );
      });

      it('throws GASearchError on failure', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 400,
        });

        await expect(
          gaSearchApi.suggest({ query: 't', format: 'maven' })
        ).rejects.toThrow(GASearchError);
      });
    });

    describe('getDetail', () => {
      it('returns detail data on success', async () => {
        const mockDetail = {
          gaId: 'maven:org.example:artifact',
          versions: [],
        };
        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve(mockDetail),
        });

        const result = await gaSearchApi.getDetail('maven:org.example:artifact');

        expect(result).toEqual(mockDetail);
      });

      it('throws with 404 message when not found', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 404,
        });

        await expect(gaSearchApi.getDetail('nonexistent')).rejects.toThrow(
          'GA not found'
        );
      });

      it('throws with generic message on other errors', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 500,
        });

        await expect(gaSearchApi.getDetail('test')).rejects.toThrow(
          'Failed to load GA detail'
        );
      });
    });

    describe('getVersionAssets', () => {
      it('returns assets on success', async () => {
        const mockAssets = { assets: [{ name: 'artifact.jar' }] };
        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve(mockAssets),
        });

        const result = await gaSearchApi.getVersionAssets(
          'maven:org.example:artifact',
          '1.0.0'
        );

        expect(result).toEqual(mockAssets);
      });

      it('throws with 404 message when version not found', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 404,
        });

        await expect(
          gaSearchApi.getVersionAssets('test', 'nonexistent')
        ).rejects.toThrow('Version not found');
      });

      it('includes repository param when provided', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve({ assets: [] }),
        });

        await gaSearchApi.getVersionAssets('test', '1.0.0', 'my-repo');

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('repository=my-repo'),
          expect.any(Object)
        );
      });
    });
  });
});
