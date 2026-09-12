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
    // Reset to the root path, not window.location.pathname: the shareable-URL
    // tests move the browser under a context path and must not leak it.
    window.history.replaceState({}, '', '/');
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

    it('preserves the nameOrVersion virtual filter across formats', () => {
      // `nameOrVersion` is the results-page combined filter — it isn't declared
      // in any format's filter defs, so it must round-trip via its own explicit
      // URL param. Without this the filter would silently vanish on refresh,
      // back-nav, or component-detail-return (see NEXUS-54333).
      const { result } = renderHook(() => useSearchUrlState());
      result.current.syncToUrl({
        format: 'all',
        query: '',
        filters: { nameOrVersion: 'commons' },
      });
      expect(result.current.readFromUrl().filters.nameOrVersion).toBe('commons');
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

    it('keeps the context path so sub-path installs are shareable (AT-012)', () => {
      // Nexus is routinely served under a context path. Composing only
      // origin + hash produced a link that 404s for the recipient.
      window.history.replaceState({}, '', '/nexus/#preview/browse/search');
      const { result } = renderHook(() => useSearchUrlState());

      const url = result.current.getShareableUrl({
        format: 'all',
        query: 'spring',
        filters: {},
      });

      expect(url).toBe(`${window.location.origin}/nexus/#preview/browse/search?q=spring`);
    });

    it('keeps the context path when there is no state to carry (AT-012)', () => {
      window.history.replaceState({}, '', '/nexus/#preview/browse/search');
      const { result } = renderHook(() => useSearchUrlState());

      const url = result.current.getShareableUrl({ format: 'all', query: '', filters: {} });

      expect(url).toBe(`${window.location.origin}/nexus/#preview/browse/search`);
    });
  });

  describe('write-side validation', () => {
    // The length/control-character rules used to be enforced on read only, so a
    // value could be written to the URL and then silently dropped on refresh,
    // back-nav or breadcrumb return — widening the search without notice.
    it('omits an over-long query rather than writing it (AT-009)', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'maven', query: 'a'.repeat(257), filters: {} });

      expect(window.location.hash).not.toContain('q=');
      expect(window.location.hash).toContain('format=maven');
    });

    it('writes a query exactly at the length limit (AT-009)', () => {
      const atLimit = 'a'.repeat(256);
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'all', query: atLimit, filters: {} });

      expect(result.current.readFromUrl().query).toBe(atLimit);
    });

    it('omits an over-long format filter but keeps the query (AT-009)', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({
        format: 'maven',
        query: 'spring',
        filters: { groupId: 'g'.repeat(257) },
      });

      expect(window.location.hash).not.toContain('maven.groupId=');
      expect(window.location.hash).toContain('q=spring');
    });

    it('omits an over-long nameOrVersion but keeps the query (AT-009)', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({
        format: 'all',
        query: 'spring',
        filters: { nameOrVersion: 'v'.repeat(300) },
      });

      expect(window.location.hash).not.toContain('nameOrVersion=');
      expect(window.location.hash).toContain('q=spring');
    });

    it('omits values containing control characters (AT-010)', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({
        format: 'all',
        query: 'spring\x00boot',
        filters: { nameOrVersion: 'commons\x1b' },
      });

      expect(window.location.hash).not.toContain('q=');
      expect(window.location.hash).not.toContain('nameOrVersion=');
    });

    it('never writes a value the read path would drop (AT-009)', () => {
      const { result } = renderHook(() => useSearchUrlState());
      const state = {
        format: 'maven' as const,
        query: 'q'.repeat(300),
        filters: { groupId: 'org.apache', nameOrVersion: 'n'.repeat(300) },
      };

      result.current.syncToUrl(state);

      // Round-trip: whatever reached the URL must read back unchanged.
      const readBack = result.current.readFromUrl();
      expect(readBack.query).toBe('');
      expect(readBack.filters.nameOrVersion).toBeUndefined();
      expect(readBack.filters.groupId).toBe('org.apache');
      expect(readBack.format).toBe('maven');
    });
  });

  describe('read-back validation', () => {
    it('drops an over-long value but keeps valid siblings (AT-009)', () => {
      setHash(`q=${'a'.repeat(257)}&format=maven`);
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.query).toBe('');
      expect(state.format).toBe('maven');
    });

    it('accepts a value exactly at the length limit (AT-009)', () => {
      const atLimit = 'a'.repeat(256);
      setHash(`q=${atLimit}`);
      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.readFromUrl().query).toBe(atLimit);
    });

    it('drops an over-long format filter but keeps the query (AT-009)', () => {
      setHash(`format=maven&q=spring&maven.groupId=${'g'.repeat(257)}`);
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.filters.groupId).toBeUndefined();
      expect(state.query).toBe('spring');
    });

    it('drops values containing control characters (AT-010)', () => {
      const q = encodeURIComponent('spring\x00boot');
      const nameOrVersion = encodeURIComponent('commons\x1b');
      setHash(`q=${q}&nameOrVersion=${nameOrVersion}`);
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.query).toBe('');
      expect(state.filters.nameOrVersion).toBeUndefined();
    });

    it('drops an over-long nameOrVersion but keeps the format (AT-009)', () => {
      setHash(`format=npm&nameOrVersion=${'v'.repeat(300)}`);
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.filters.nameOrVersion).toBeUndefined();
      expect(state.format).toBe('npm');
    });
  });

  describe('hostile-URL characterization pins', () => {
    it('ignores unknown params and never pollutes Object.prototype (AT-006)', () => {
      // parseUrlState assigns under filterDef.id (a searchFilters.ts identifier),
      // never under a key taken from the URL, so `__proto__` is simply not read.
      // This pin fails loudly if parsing is ever made key-driven.
      setHash('q=spring&bogus=1&__proto__=polluted&constructor=x');
      const { result } = renderHook(() => useSearchUrlState());

      const state = result.current.readFromUrl();
      expect(state.query).toBe('spring');
      expect(Object.keys(state.filters)).toEqual([]);
      expect(Object.prototype).not.toHaveProperty('polluted');
      expect({}).not.toHaveProperty('polluted');
    });

    it('resolves a repeated param to its first occurrence (AT-019)', () => {
      setHash('q=first&q=second');
      const { result } = renderHook(() => useSearchUrlState());

      expect(result.current.readFromUrl().query).toBe('first');
    });

    it('never writes a duplicate key (AT-019)', () => {
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({
        format: 'maven',
        query: 'spring',
        filters: { groupId: 'org.apache', nameOrVersion: 'commons' },
      });

      const written = [...new URLSearchParams(window.location.hash.split('?')[1] ?? '').keys()];
      expect(new Set(written).size).toBe(written.length);
    });
  });

  describe('pre-hash query string', () => {
    /** Put the browser at `<pathname>?debug#preview/browse/search[?<search state>]`. */
    function setDebugUrl(searchState: string): void {
      const hashPath = '#preview/browse/search';
      const hash = searchState ? `${hashPath}?${searchState}` : hashPath;
      window.history.replaceState({}, '', `${window.location.pathname}?debug${hash}`);
    }

    it('keeps ?debug when writing search state (AT-011)', () => {
      setDebugUrl('');
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'maven', query: 'spring', filters: {} });

      expect(window.location.search).toBe('?debug');
      expect(window.location.hash).toContain('q=spring');
    });

    it('keeps ?debug when the written state is empty (AT-011)', () => {
      setDebugUrl('q=spring');
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'all', query: '', filters: {} });

      expect(window.location.search).toBe('?debug');
      expect(window.location.hash).not.toContain('q=');
    });

    it('keeps ?debug on a replaceState write (AT-011)', () => {
      setDebugUrl('');
      const { result } = renderHook(() => useSearchUrlState());

      result.current.syncToUrl({ format: 'all', query: 'react', filters: {} }, true);

      expect(window.location.search).toBe('?debug');
      expect(window.location.hash).toContain('q=react');
    });

    it('omits ?debug from a shareable URL (AT-012)', () => {
      // Deliberate asymmetry: a shared link must not carry a local dev flag.
      setDebugUrl('');
      const { result } = renderHook(() => useSearchUrlState());

      const url = result.current.getShareableUrl({
        format: 'all',
        query: 'spring',
        filters: {},
      });

      expect(url).not.toContain('debug');
      expect(url).toContain('q=spring');
    });
  });
});
