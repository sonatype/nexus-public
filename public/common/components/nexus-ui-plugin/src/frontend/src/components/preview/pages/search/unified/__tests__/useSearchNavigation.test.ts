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
import { useSearchNavigation } from '../useSearchNavigation';

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
          gaId: encodeURIComponent('maven2:org.apache.commons:commons-lang3'),
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
          gaId: encodeURIComponent('npm:@types:react'),
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
          gaId: encodeURIComponent('nuget:Newtonsoft.Json'),
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
          gaId: encodeURIComponent('raw:package'),
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
});
