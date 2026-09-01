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
import {
  consumeSearchReturnUrl,
  SEARCH_RETURN_URL_KEY,
  useSearchNavigation,
} from '../useSearchNavigation';

// Mock @uirouter/react to avoid UIRouter context requirement
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockGo,
    },
  }),
}));

describe('useSearchNavigation', () => {
  let originalHash: string;

  beforeEach(() => {
    originalHash = window.location.hash;
    Object.defineProperty(window, 'location', {
      value: { hash: '' },
      writable: true,
    });
    mockGo.mockClear();
  });

  afterEach(() => {
    window.location.hash = originalHash;
  });

  describe('navigateToDetail', () => {
    it('navigates to component detail for maven2 format', () => {
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail({
          id: '1',
          name: 'commons-lang3',
          format: 'maven2',
          group: 'org.apache.commons',
          version: '3.12.0',
          repository: 'maven-central',
        });
      });

      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component',
        expect.objectContaining({
          keyword: 'commons-lang3',
          gaId: 'maven2:org.apache.commons:commons-lang3',
          version: '3.12.0',
        })
      );
    });

    it('navigates to component detail for npm format with group', () => {
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail({
          id: '3',
          name: 'react',
          format: 'npm',
          group: '@types',
          version: '18.0.0',
          repository: 'npm-proxy',
        });
      });

      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component',
        expect.objectContaining({
          keyword: 'react',
          gaId: 'npm:@types:react',
          version: '18.0.0',
        })
      );
    });

    it('navigates to component detail for nuget format', () => {
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail({
          id: '5',
          name: 'Newtonsoft.Json',
          format: 'nuget',
          version: '13.0.3',
          repository: 'nuget-proxy',
        });
      });

      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component',
        expect.objectContaining({
          keyword: 'Newtonsoft.Json',
          gaId: 'nuget:Newtonsoft.Json',
          version: '13.0.3',
        })
      );
    });

    it('includes version in the params', () => {
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail({
          id: '6',
          name: 'nginx',
          format: 'docker',
          version: 'latest',
          repository: 'docker-proxy',
        });
      });

      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component',
        expect.objectContaining({ version: 'latest' })
      );
    });

    it('handles component without group', () => {
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail({
          id: '10',
          name: 'package',
          format: 'raw',
          version: '1.0.0',
          repository: 'raw-hosted',
        });
      });

      expect(mockGo).toHaveBeenCalledWith(
        'preview.browse.search.component',
        expect.objectContaining({
          gaId: 'raw:package',
          keyword: 'package',
          version: '1.0.0',
        })
      );
    });
  });

  describe('getDetailRoute', () => {
    it('returns component detail route for maven', () => {
      const { result } = renderHook(() => useSearchNavigation());

      const route = result.current.getDetailRoute({
        id: '1',
        name: 'junit',
        format: 'maven2',
        group: 'junit',
        version: '4.13.2',
        repository: 'maven-central',
      });

      // URL format: preview/browse/search/maven/{keyword}/ga/{gaId}
      expect(route.url).toContain('preview/browse/search/maven/');
      expect(route.url).toContain('/ga/');
      expect(route.url).toContain('maven2');
      expect(route.isPreviewBrowse).toBe(false);
      // Should not have navigated
      expect(mockGo).not.toHaveBeenCalled();
    });

    it('builds gaId with group for scoped packages', () => {
      const { result } = renderHook(() => useSearchNavigation());

      const route = result.current.getDetailRoute({
        id: '2',
        name: 'eslint',
        format: 'npm',
        group: '@typescript-eslint',
        version: '6.0.0',
        repository: 'npm-proxy',
      });

      // gaId should include format:group:name
      expect(route.url).toContain('npm');
      expect(route.url).toContain('eslint');
      expect(route.isPreviewBrowse).toBe(false);
    });

    it('builds gaId without group when none provided', () => {
      const { result } = renderHook(() => useSearchNavigation());

      const route = result.current.getDetailRoute({
        id: '3',
        name: 'SomePackage',
        format: 'nuget',
        version: '1.0.0',
        repository: 'nuget-hosted',
      });

      expect(route.url).toContain('nuget');
      expect(route.url).toContain('SomePackage');
      expect(route.isPreviewBrowse).toBe(false);
    });

    it('marks all routes as component detail (not preview browse)', () => {
      const { result } = renderHook(() => useSearchNavigation());

      const formats = ['maven2', 'npm', 'nuget', 'docker', 'pypi', 'apt', 'yum', 'raw'];

      formats.forEach((format) => {
        const route = result.current.getDetailRoute({
          id: '1',
          name: 'test-package',
          format,
          version: '1.0.0',
          repository: `${format}-proxy`,
        });

        expect(route.isPreviewBrowse).toBe(false);
      });
    });
  });

  describe('search return URL', () => {
    const RESULT = {
      id: '1',
      name: 'commons-lang3',
      format: 'maven2',
      group: 'org.apache.commons',
      version: '3.12.0',
      repository: 'maven-central',
    };

    beforeEach(() => {
      sessionStorage.clear();
    });

    it('stores the current search hash before navigating (AT-015)', () => {
      window.location.hash = '#preview/browse/search?q=spring&format=maven';
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail(RESULT);
      });

      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBe(
        '#preview/browse/search?q=spring&format=maven',
      );
    });

    it('stores nothing when navigating from a non-search page (AT-015)', () => {
      window.location.hash = '#preview/browse/welcome?tab=overview';
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail(RESULT);
      });

      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
    });

    it('consumeSearchReturnUrl returns and clears a search hash (AT-015)', () => {
      sessionStorage.setItem(SEARCH_RETURN_URL_KEY, '#preview/browse/search?q=a');

      expect(consumeSearchReturnUrl()).toBe('#preview/browse/search?q=a');
      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
    });

    it('consumeSearchReturnUrl rejects and clears a foreign value (AT-015)', () => {
      // A tampered value must not be able to send the breadcrumb elsewhere.
      sessionStorage.setItem(SEARCH_RETURN_URL_KEY, '#preview/browse/welcome');

      expect(consumeSearchReturnUrl()).toBeUndefined();
      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
    });

    it('consumeSearchReturnUrl returns undefined when nothing is stored (AT-015)', () => {
      expect(consumeSearchReturnUrl()).toBeUndefined();
    });

    it('stores nothing when navigating from a component-detail page (AT-015)', () => {
      // '#preview/browse/search' also prefixes every detail hash. Accepting the
      // bare prefix let a detail -> detail hop overwrite the stored search URL
      // with a detail URL, after which the breadcrumb assigned the hash it was
      // already on and did nothing.
      window.location.hash =
        '#preview/browse/search/maven/commons-lang3/ga/maven2%3Aorg.apache.commons%3Acommons-lang3';
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail(RESULT);
      });

      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
    });

    it('consumeSearchReturnUrl rejects and clears a component-detail hash (AT-015)', () => {
      sessionStorage.setItem(
        SEARCH_RETURN_URL_KEY,
        '#preview/browse/search/maven/lodash/ga/npm%3Alodash',
      );

      expect(consumeSearchReturnUrl()).toBeUndefined();
      expect(sessionStorage.getItem(SEARCH_RETURN_URL_KEY)).toBeNull();
    });

    it('accepts the bare search hash with no query string (AT-015)', () => {
      sessionStorage.setItem(SEARCH_RETURN_URL_KEY, '#preview/browse/search');

      expect(consumeSearchReturnUrl()).toBe('#preview/browse/search');
    });
  });

  describe('unavailable sessionStorage', () => {
    // Safari private mode and storage-blocked embeds throw on every access. An
    // unguarded call aborted the click before stateService.go ever ran.
    const RESULT = {
      id: '1',
      name: 'lodash',
      format: 'npm',
      group: undefined,
      version: '4.17.21',
      repository: 'npm-proxy',
    };

    function breakStorage(): jest.SpyInstance[] {
      const boom = () => {
        throw new DOMException('denied', 'SecurityError');
      };
      return [
        jest.spyOn(Storage.prototype, 'getItem').mockImplementation(boom),
        jest.spyOn(Storage.prototype, 'setItem').mockImplementation(boom),
        jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(boom),
      ];
    }

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('still navigates when setItem throws (AT-015)', () => {
      breakStorage();
      window.location.hash = '#preview/browse/search?q=spring';
      const { result } = renderHook(() => useSearchNavigation());

      act(() => {
        result.current.navigateToDetail(RESULT);
      });

      expect(mockGo).toHaveBeenCalledWith('preview.browse.search.component', {
        keyword: 'lodash',
        gaId: 'npm:lodash',
        version: '4.17.21',
      });
    });

    it('consumeSearchReturnUrl returns undefined when storage throws (AT-015)', () => {
      breakStorage();

      expect(() => consumeSearchReturnUrl()).not.toThrow();
      expect(consumeSearchReturnUrl()).toBeUndefined();
    });
  });
});
