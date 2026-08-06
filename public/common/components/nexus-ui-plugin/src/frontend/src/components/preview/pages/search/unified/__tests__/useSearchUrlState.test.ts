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

import { renderHook } from '@testing-library/react';
import { useSearchUrlState, type UrlSearchState } from '../useSearchUrlState';

/**
 * Set the browser hash to a given search query string (hash-based routing).
 */
function setHash(queryString: string): void {
  const path = '#preview/browse/search';
  window.location.hash = queryString ? `${path}?${queryString}` : path;
}

describe('useSearchUrlState', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', window.location.pathname);
    setHash('');
  });

  describe('readFromUrl', () => {
    it('parses query, format, and format-specific filters', () => {
      setHash('q=spring&format=maven&maven.groupId=org.apache');
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.query).toBe('spring');
      expect(state.format).toBe('maven');
      expect(state.filters.groupId).toBe('org.apache');
    });

    it('parses npm scope filter (group -> scope mapping)', () => {
      setHash('q=react&format=npm&group=@types');
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.format).toBe('npm');
      expect(state.filters.scope).toBe('@types');
    });

    it('defaults invalid format to "all" (TC-009)', () => {
      setHash('q=test&format=nonexistent');
      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.readFromUrl().format).toBe('all');
    });

    it('handles an empty URL gracefully', () => {
      setHash('');
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.format).toBe('all');
      expect(state.query).toBe('');
      expect(state.filters).toEqual({});
    });

    it('parses sort and direction when present', () => {
      setHash('q=test&sort=name&direction=asc');
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.sortField).toBe('name');
      expect(state.sortDirection).toBe('asc');
    });

    it('ignores invalid sort/direction values', () => {
      setHash('q=test&sort=bogus&direction=sideways');
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.sortField).toBeUndefined();
      expect(state.sortDirection).toBeUndefined();
    });

    it('decodes special characters (TC-014)', () => {
      setHash(`q=${encodeURIComponent('node.js')}`);
      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.readFromUrl().query).toBe('node.js');
    });
  });

  describe('syncToUrl', () => {
    it('writes query, format, and filters to the URL', () => {
      const { result } = renderHook(() => useSearchUrlState());
      const state: UrlSearchState = {
        format: 'maven',
        query: 'spring',
        filters: { groupId: 'org.apache' },
      };

      result.current.syncToUrl(state);

      expect(window.location.hash).toContain('q=spring');
      expect(window.location.hash).toContain('format=maven');
      expect(window.location.hash).toContain('maven.groupId=org.apache');
    });

    it('omits format when "all" and clears filters', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'all', query: 'react', filters: {} });

      expect(window.location.hash).toContain('q=react');
      expect(window.location.hash).not.toContain('format=');
    });

    it('omits default sort (lastUpdated/desc) but writes non-defaults', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({
        format: 'all',
        query: 'x',
        filters: {},
        sortField: 'lastUpdated',
        sortDirection: 'desc',
      });
      expect(window.location.hash).not.toContain('sort=');
      expect(window.location.hash).not.toContain('direction=');

      result.current.syncToUrl({
        format: 'all',
        query: 'x',
        filters: {},
        sortField: 'name',
        sortDirection: 'asc',
      });
      expect(window.location.hash).toContain('sort=name');
      expect(window.location.hash).toContain('direction=asc');
    });

    it('writes a non-default direction on the default sort field', () => {
      const { result } = renderHook(() => useSearchUrlState());

      // lastUpdated is the default field, but asc is a non-default direction —
      // direction must still be written independently of the field.
      result.current.syncToUrl({
        format: 'all',
        query: 'x',
        filters: {},
        sortField: 'lastUpdated',
        sortDirection: 'asc',
      });
      expect(window.location.hash).not.toContain('sort=');
      expect(window.location.hash).toContain('direction=asc');
    });

    it('uses pushState for commits and replaceState when replace=true', () => {
      const pushSpy = jest.spyOn(window.history, 'pushState');
      const replaceSpy = jest.spyOn(window.history, 'replaceState');
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'all', query: 'a', filters: {} });
      expect(pushSpy).toHaveBeenCalled();

      result.current.syncToUrl({ format: 'all', query: 'ab', filters: {} }, true);
      expect(replaceSpy).toHaveBeenCalled();

      pushSpy.mockRestore();
      replaceSpy.mockRestore();
    });
  });

  describe('round-trip', () => {
    it('read(write(state)) preserves query/format/filters/sort', () => {
      const { result } = renderHook(() => useSearchUrlState());
      const original: UrlSearchState = {
        format: 'npm',
        query: 'express',
        filters: { author: 'tj' },
        sortField: 'name',
        sortDirection: 'asc',
      };

      result.current.syncToUrl(original);
      const restored = result.current.readFromUrl();

      expect(restored.format).toBe('npm');
      expect(restored.query).toBe('express');
      expect(restored.filters.author).toBe('tj');
      expect(restored.sortField).toBe('name');
      expect(restored.sortDirection).toBe('asc');
    });

    it('preserves a non-default direction on the default sort field', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({
        format: 'all',
        query: 'x',
        filters: {},
        sortField: 'lastUpdated',
        sortDirection: 'asc',
      });
      const restored = result.current.readFromUrl();

      // The default field is not written to the URL (parses back as undefined),
      // but the non-default direction survives the round-trip.
      expect(restored.sortField).toBeUndefined();
      expect(restored.sortDirection).toBe('asc');
    });
  });

  describe('getShareableUrl', () => {
    beforeEach(() => setHash(''));

    it('produces an absolute URL carrying the state', () => {
      const { result } = renderHook(() => useSearchUrlState());

      const url = result.current.getShareableUrl({
        format: 'maven',
        query: 'spring',
        filters: { groupId: 'org.apache' },
      });

      expect(url).toContain('#preview/browse/search');
      expect(url).toContain('q=spring');
      expect(url).toContain('format=maven');
      expect(url).toContain('maven.groupId=org.apache');
    });

    it('omits format when it is "all"', () => {
      const { result } = renderHook(() => useSearchUrlState());

      const url = result.current.getShareableUrl({
        format: 'all',
        query: 'test',
        filters: {},
      });

      expect(url).not.toContain('format=');
      expect(url).toContain('q=test');
    });
  });
});
