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
  GA_SEARCH_ROUTE_NAMES,
  DEFAULT_SEARCH_ROUTE_NAMES,
  GA_SEARCH_URLS,
  PREVIEW_BASE_URL,
  GA_SEARCH_PARAMS,
  buildSearchRoute,
  buildDetailRoute,
  buildDefaultSearchRoute,
  parseGaId,
  parseSearchParams,
  TAB_ROUTE_MAP,
  getTabFromRoute,
} from '../search.routes';

describe('search.routes', () => {
  describe('constants', () => {
    describe('GA_SEARCH_ROUTE_NAMES', () => {
      it('has correct root route', () => {
        expect(GA_SEARCH_ROUTE_NAMES.ROOT).toBe('preview.browse.search');
      });

      it('has correct maven route', () => {
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN).toBe('preview.browse.search.maven');
      });

      it('has correct detail route', () => {
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL).toBe('preview.browse.search.component');
      });

      it('has all detail tab routes', () => {
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW).toBeDefined();
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_VERSIONS).toBeDefined();
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_REPOS).toBeDefined();
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_FILES).toBeDefined();
        expect(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_SECURITY).toBeDefined();
      });
    });

    describe('DEFAULT_SEARCH_ROUTE_NAMES', () => {
      it('has maven route without preview prefix', () => {
        expect(DEFAULT_SEARCH_ROUTE_NAMES.MAVEN).toBe('browse.search.maven');
      });
    });

    describe('GA_SEARCH_URLS', () => {
      it('has maven URL with keyword segment', () => {
        expect(GA_SEARCH_URLS.MAVEN).toBe('/maven:keyword');
      });

      it('has detail URL with gaId param', () => {
        expect(GA_SEARCH_URLS.MAVEN_DETAIL).toBe('/ga/:gaId');
      });

      it('has all tab URLs', () => {
        expect(GA_SEARCH_URLS.DETAIL_OVERVIEW).toBe('/overview');
        expect(GA_SEARCH_URLS.DETAIL_VERSIONS).toBe('/versions');
        expect(GA_SEARCH_URLS.DETAIL_REPOS).toBe('/repos');
        expect(GA_SEARCH_URLS.DETAIL_FILES).toBe('/files');
        expect(GA_SEARCH_URLS.DETAIL_SECURITY).toBe('/security');
      });
    });

    describe('PREVIEW_BASE_URL', () => {
      it('has correct value', () => {
        expect(PREVIEW_BASE_URL).toBe('/preview/browse/search');
      });
    });

    describe('GA_SEARCH_PARAMS', () => {
      it('has all search parameters', () => {
        expect(GA_SEARCH_PARAMS.QUERY).toBe('q');
        expect(GA_SEARCH_PARAMS.FORMAT).toBe('format');
        expect(GA_SEARCH_PARAMS.GROUP_ID).toBe('groupId');
        expect(GA_SEARCH_PARAMS.ARTIFACT_ID).toBe('artifactId');
        expect(GA_SEARCH_PARAMS.REPOSITORY).toBe('repository');
        expect(GA_SEARCH_PARAMS.SORT).toBe('sort');
        expect(GA_SEARCH_PARAMS.DIRECTION).toBe('direction');
        expect(GA_SEARCH_PARAMS.VERSION).toBe('version');
      });
    });
  });

  describe('buildSearchRoute', () => {
    it('returns base URL when no params provided', () => {
      expect(buildSearchRoute()).toBe('/preview/browse/search/maven');
    });

    it('returns base URL for empty params', () => {
      expect(buildSearchRoute({})).toBe('/preview/browse/search/maven');
    });

    it('includes query parameter', () => {
      const url = buildSearchRoute({ query: 'commons-lang' });
      expect(url).toBe('/preview/browse/search/maven?q=commons-lang');
    });

    it('includes groupId parameter', () => {
      const url = buildSearchRoute({ groupId: 'org.apache.commons' });
      expect(url).toBe('/preview/browse/search/maven?groupId=org.apache.commons');
    });

    it('includes artifactId parameter', () => {
      const url = buildSearchRoute({ artifactId: 'commons-lang3' });
      expect(url).toBe('/preview/browse/search/maven?artifactId=commons-lang3');
    });

    it('includes repository parameter', () => {
      const url = buildSearchRoute({ repository: 'maven-central' });
      expect(url).toBe('/preview/browse/search/maven?repository=maven-central');
    });

    it('includes sort parameter', () => {
      const url = buildSearchRoute({ sort: 'name' });
      expect(url).toBe('/preview/browse/search/maven?sort=name');
    });

    it('includes direction parameter', () => {
      const url = buildSearchRoute({ direction: 'desc' });
      expect(url).toBe('/preview/browse/search/maven?direction=desc');
    });

    it('combines multiple parameters', () => {
      const url = buildSearchRoute({
        query: 'guava',
        groupId: 'com.google',
        repository: 'maven-central',
        sort: 'lastUpdated',
        direction: 'desc',
      });

      expect(url).toContain('q=guava');
      expect(url).toContain('groupId=com.google');
      expect(url).toContain('repository=maven-central');
      expect(url).toContain('sort=lastUpdated');
      expect(url).toContain('direction=desc');
    });

    it('excludes undefined parameters', () => {
      const url = buildSearchRoute({ query: 'test', groupId: undefined });
      expect(url).toBe('/preview/browse/search/maven?q=test');
      expect(url).not.toContain('groupId');
    });
  });

  describe('buildDetailRoute', () => {
    it('builds URL with just gaId', () => {
      const url = buildDetailRoute('maven:org.apache:commons-lang3');
      expect(url).toBe(
        '/preview/browse/search/maven/ga/maven%3Aorg.apache%3Acommons-lang3'
      );
    });

    it('encodes special characters in gaId', () => {
      const url = buildDetailRoute('maven:com.example:my-artifact');
      expect(url).toContain(encodeURIComponent('maven:com.example:my-artifact'));
    });

    it('includes tab when not overview', () => {
      const url = buildDetailRoute('maven:org:artifact', 'versions');
      expect(url).toBe(
        '/preview/browse/search/maven/ga/maven%3Aorg%3Aartifact/versions'
      );
    });

    it('excludes tab segment for overview', () => {
      const url = buildDetailRoute('maven:org:artifact', 'overview');
      expect(url).not.toContain('/overview');
      expect(url).toBe('/preview/browse/search/maven/ga/maven%3Aorg%3Aartifact');
    });

    it('includes version for files tab', () => {
      const url = buildDetailRoute('maven:org:artifact', 'files', '1.0.0');
      expect(url).toContain('/files');
      expect(url).toContain('version=1.0.0');
    });

    it('includes version for security tab', () => {
      const url = buildDetailRoute('maven:org:artifact', 'security', '2.0.0');
      expect(url).toContain('/security');
      expect(url).toContain('version=2.0.0');
    });

    it('excludes version for versions tab', () => {
      const url = buildDetailRoute('maven:org:artifact', 'versions', '1.0.0');
      expect(url).not.toContain('version=');
    });

    it('excludes version for repositories tab', () => {
      const url = buildDetailRoute('maven:org:artifact', 'repositories', '1.0.0');
      expect(url).not.toContain('version=');
    });

    it('encodes version with special characters', () => {
      const url = buildDetailRoute('maven:org:artifact', 'files', '1.0.0-SNAPSHOT');
      expect(url).toContain(encodeURIComponent('1.0.0-SNAPSHOT'));
    });
  });

  describe('buildDefaultSearchRoute', () => {
    it('returns correct default search URL', () => {
      expect(buildDefaultSearchRoute()).toBe('#browse/search/maven');
    });
  });

  describe('parseGaId', () => {
    it('decodes URL-encoded gaId', () => {
      const encoded = encodeURIComponent('maven:org.apache.commons:commons-lang3');
      expect(parseGaId(encoded)).toBe('maven:org.apache.commons:commons-lang3');
    });

    it('handles already-decoded gaId', () => {
      expect(parseGaId('maven:com.example:artifact')).toBe('maven:com.example:artifact');
    });

    it('handles special characters', () => {
      const encoded = encodeURIComponent('maven:com.example:my-artifact');
      expect(parseGaId(encoded)).toBe('maven:com.example:my-artifact');
    });
  });

  describe('parseSearchParams', () => {
    it('extracts all parameters when present', () => {
      const params = new URLSearchParams(
        'q=test&format=maven&groupId=org.apache&artifactId=commons&repository=central&sort=name&direction=asc'
      );

      const result = parseSearchParams(params);

      expect(result.query).toBe('test');
      expect(result.format).toBe('maven');
      expect(result.groupId).toBe('org.apache');
      expect(result.artifactId).toBe('commons');
      expect(result.repository).toBe('central');
      expect(result.sort).toBe('name');
      expect(result.direction).toBe('asc');
    });

    it('returns undefined for missing parameters', () => {
      const params = new URLSearchParams('q=test');

      const result = parseSearchParams(params);

      expect(result.query).toBe('test');
      expect(result.format).toBeUndefined();
      expect(result.groupId).toBeUndefined();
      expect(result.artifactId).toBeUndefined();
      expect(result.repository).toBeUndefined();
      expect(result.sort).toBeUndefined();
      expect(result.direction).toBeUndefined();
    });

    it('handles empty URLSearchParams', () => {
      const params = new URLSearchParams();

      const result = parseSearchParams(params);

      expect(result.query).toBeUndefined();
      expect(result.format).toBeUndefined();
    });
  });

  describe('TAB_ROUTE_MAP', () => {
    it('maps all tabs to correct routes', () => {
      expect(TAB_ROUTE_MAP.overview).toBe(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW);
      expect(TAB_ROUTE_MAP.versions).toBe(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_VERSIONS);
      expect(TAB_ROUTE_MAP.repositories).toBe(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_REPOS);
      expect(TAB_ROUTE_MAP.files).toBe(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_FILES);
      expect(TAB_ROUTE_MAP.security).toBe(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_SECURITY);
    });
  });

  describe('getTabFromRoute', () => {
    it('returns overview for overview route', () => {
      expect(getTabFromRoute(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW)).toBe('overview');
    });

    it('returns versions for versions route', () => {
      expect(getTabFromRoute(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_VERSIONS)).toBe('versions');
    });

    it('returns repositories for repos route', () => {
      expect(getTabFromRoute(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_REPOS)).toBe('repositories');
    });

    it('returns files for files route', () => {
      expect(getTabFromRoute(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_FILES)).toBe('files');
    });

    it('returns security for security route', () => {
      expect(getTabFromRoute(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_SECURITY)).toBe('security');
    });

    it('defaults to overview for unknown routes', () => {
      expect(getTabFromRoute('unknown.route')).toBe('overview');
      expect(getTabFromRoute('')).toBe('overview');
    });

    it('defaults to overview for parent detail route', () => {
      expect(getTabFromRoute(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL)).toBe('overview');
    });
  });
});
