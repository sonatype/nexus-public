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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';
import {
  createSearchMachine,
  getActiveFormatMeta,
  ALL_FORMATS,
  FORMATS_WITH_CUSTOM_FILTERS,
  SIMPLE_FORMATS,
} from '../searchMachine';
import { FORMAT_FILTERS } from '../searchFilters';
import type { SearchFormat } from '../unified.types';

/**
 * Helper: start the machine with mock search/loadMore services.
 */
function startMachine(overrides?: {
  search?: (ctx: any) => Promise<any>;
  loadMore?: (ctx: any) => Promise<any>;
}) {
  const machine = createSearchMachine();
  const service = interpret(
    machine.withConfig({
      services: {
        search: overrides?.search ?? (() =>
          Promise.resolve({
            results: [
              { id: '1', name: 'test-pkg', format: 'maven2', repository: 'central', version: '1.0' },
            ],
            continuationToken: 'token-1',
          })
        ),
        loadMore: overrides?.loadMore ?? (() =>
          Promise.resolve({
            results: [
              { id: '2', name: 'test-pkg-2', format: 'maven2', repository: 'central', version: '2.0' },
            ],
            continuationToken: undefined,
          })
        ),
      },
    })
  ).start();

  return service;
}

// =============================================================================
// FORMAT SUB-STATE TESTS (the core value - tests every format's filter config)
// =============================================================================

describe('searchMachine', () => {
  describe('format sub-states', () => {
    it('starts in all format by default', () => {
      const service = startMachine();
      const state = service.getSnapshot();

      expect(state.matches({ format: 'all' })).toBe(true);
      expect(state.context.format).toBe('all');

      service.stop();
    });

    it.each(ALL_FORMATS)('transitions to %s format on SELECT_FORMAT', (format) => {
      const service = startMachine();

      service.send({ type: 'SELECT_FORMAT', format });

      const state = service.getSnapshot();
      expect(state.matches({ format })).toBe(true);
      expect(state.context.format).toBe(format);

      service.stop();
    });

    it.each(ALL_FORMATS)('format %s has correct filter metadata in state.meta', (format) => {
      const service = startMachine();

      service.send({ type: 'SELECT_FORMAT', format });

      const state = service.getSnapshot();
      const meta = getActiveFormatMeta(state);

      expect(meta).toBeDefined();
      expect(meta!.formatId).toBe(format);
      expect(meta!.label).toBeTruthy();
      expect(Array.isArray(meta!.filters)).toBe(true);
      expect(meta!.filters.length).toBeGreaterThan(0);

      // Verify metadata matches searchFilters.ts source of truth
      const expected = FORMAT_FILTERS[format];
      expect(meta!.label).toBe(expected.format.label);
      expect(meta!.apiFormat).toBe(expected.format.apiFormat);
      expect(meta!.placeholder).toBe(expected.format.placeholder);

      service.stop();
    });
  });

  // =============================================================================
  // API PARAMETER MAPPING TESTS (catches tricky mappings that break silently)
  // =============================================================================

  describe('API parameter mapping per format', () => {
    // These are the TRICKY mappings where the filter ID differs from the API param.
    // If anyone changes these, the test fails immediately.
    const TRICKY_MAPPINGS: Array<{ format: SearchFormat; filterId: string; expectedApiParam: string }> = [
      // npm scope maps to 'group', NOT 'npm.scope'
      { format: 'npm', filterId: 'scope', expectedApiParam: 'group' },
      // npm author maps to 'npm.author'
      { format: 'npm', filterId: 'author', expectedApiParam: 'npm.author' },
      // NuGet nugetId maps to 'nuget.id', NOT 'nuget.nugetId'
      { format: 'nuget', filterId: 'nugetId', expectedApiParam: 'nuget.id' },
      // Yum yumName maps to 'yum.name', NOT 'yum.yumName'
      { format: 'yum', filterId: 'yumName', expectedApiParam: 'yum.name' },
      // Docker imageName maps correctly
      { format: 'docker', filterId: 'imageName', expectedApiParam: 'docker.imageName' },
      // Maven groupId maps correctly
      { format: 'maven', filterId: 'groupId', expectedApiParam: 'maven.groupId' },
      // Git LFS sha256 maps to 'sha256' (no prefix)
      { format: 'gitlfs', filterId: 'sha256', expectedApiParam: 'sha256' },
      // P2 pluginName maps to 'p2.pluginName'
      { format: 'p2', filterId: 'pluginName', expectedApiParam: 'p2.pluginName' },
      // Swift scope maps to 'swift.scope'
      { format: 'swift', filterId: 'scope', expectedApiParam: 'swift.scope' },
      // Terraform provider maps to 'terraform.provider'
      { format: 'terraform', filterId: 'provider', expectedApiParam: 'terraform.provider' },
    ];

    it.each(TRICKY_MAPPINGS)(
      '$format: filter "$filterId" maps to API param "$expectedApiParam"',
      ({ format, filterId, expectedApiParam }) => {
        const service = startMachine();

        service.send({ type: 'SELECT_FORMAT', format });

        const state = service.getSnapshot();
        const meta = getActiveFormatMeta(state);

        expect(meta).toBeDefined();
        expect(meta!.apiParamMap[filterId]).toBe(expectedApiParam);

        service.stop();
      }
    );

    it.each(FORMATS_WITH_CUSTOM_FILTERS)(
      'format %s has apiParamMap for all its custom filters',
      (format) => {
        const service = startMachine();

        service.send({ type: 'SELECT_FORMAT', format });

        const state = service.getSnapshot();
        const meta = getActiveFormatMeta(state);

        expect(meta).toBeDefined();

        // Every filter should have an apiParam mapping
        for (const filter of meta!.filters) {
          expect(meta!.apiParamMap[filter.id]).toBe(filter.apiParam);
        }

        service.stop();
      }
    );
  });

  // =============================================================================
  // SIMPLE vs CUSTOM FORMAT CLASSIFICATION
  // =============================================================================

  describe('format classification', () => {
    const EXPECTED_SIMPLE = ['all', 'alpine', 'apt', 'cargo', 'cocoapods', 'conda', 'go', 'helm', 'huggingface', 'pub', 'r', 'raw'];
    const EXPECTED_CUSTOM = ['ansiblegalaxy', 'composer', 'conan', 'docker', 'gitlfs', 'maven', 'npm', 'nuget', 'p2', 'pypi', 'rubygems', 'swift', 'terraform', 'yum'];

    it.each(EXPECTED_SIMPLE)('format %s is classified as simple (repository only)', (format) => {
      expect(SIMPLE_FORMATS).toContain(format);
    });

    it.each(EXPECTED_CUSTOM)('format %s is classified as having custom filters', (format) => {
      expect(FORMATS_WITH_CUSTOM_FILTERS).toContain(format);
    });

    it('covers all 27 formats', () => {
      expect(ALL_FORMATS).toHaveLength(27); // 27 formats (26 specific + 'all')
    });
  });

  // =============================================================================
  // SPECIFIC FORMAT FILTER VERIFICATION
  // =============================================================================

  describe('format-specific filter declarations', () => {
    it('Maven declares groupId, artifactId, baseVersion, classifier, extension', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'maven' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'groupId', 'artifactId', 'baseVersion', 'classifier', 'extension',
      ]);

      service.stop();
    });

    it('npm declares scope, author, description, keywords, license', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'npm' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'scope', 'author', 'description', 'keywords', 'license',
      ]);

      service.stop();
    });

    it('Docker declares imageName, imageTag, layerId, contentDigest', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'docker' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'imageName', 'imageTag', 'layerId', 'contentDigest',
      ]);

      service.stop();
    });

    it('NuGet declares nugetId, tags, title, authors, description, summary', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'nuget' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'nugetId', 'tags', 'title', 'authors', 'description', 'summary',
      ]);

      service.stop();
    });

    it('PyPI declares classifiers, description, keywords, summary', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'pypi' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'classifiers', 'description', 'keywords', 'summary',
      ]);

      service.stop();
    });

    it('Conan declares baseVersion, channel, revision, packageId, packageRevision', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'conan' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'baseVersion', 'channel', 'revision', 'packageId', 'packageRevision',
      ]);

      service.stop();
    });

    it('Yum declares yumName, architecture', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'yum' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'yumName', 'architecture',
      ]);

      service.stop();
    });

    it('Composer declares vendor, package', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'composer' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'vendor', 'package',
      ]);

      service.stop();
    });

    it('RubyGems declares description, platform, summary', () => {
      const service = startMachine();
      service.send({ type: 'SELECT_FORMAT', format: 'rubygems' });
      const meta = getActiveFormatMeta(service.getSnapshot());

      expect(meta!.customFilters.map((f) => f.id)).toEqual([
        'description', 'platform', 'summary',
      ]);

      service.stop();
    });
  });

  // =============================================================================
  // FORMAT SWITCHING BEHAVIOR
  // =============================================================================

  describe('format switching', () => {
    it('clears filters when switching formats', () => {
      const service = startMachine();

      // Set some Maven filters
      service.send({ type: 'SELECT_FORMAT', format: 'maven' });
      service.send({ type: 'UPDATE_FILTER', name: 'groupId', value: 'org.apache' });
      service.send({ type: 'UPDATE_FILTER', name: 'artifactId', value: 'commons-lang3' });

      expect(service.getSnapshot().context.filters.groupId).toBe('org.apache');

      // Switch to Docker
      service.send({ type: 'SELECT_FORMAT', format: 'docker' });

      // Maven filters should be cleared
      expect(service.getSnapshot().context.filters).toEqual({});
      expect(service.getSnapshot().context.format).toBe('docker');

      service.stop();
    });

    it('clears results when switching formats', async () => {
      const service = startMachine();

      // Do a search to get results
      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));
      expect(service.getSnapshot().context.results.length).toBeGreaterThan(0);

      // Switch format
      service.send({ type: 'SELECT_FORMAT', format: 'npm' });

      // Results should be cleared, lifecycle back to idle
      expect(service.getSnapshot().context.results).toEqual([]);
      expect(service.getSnapshot().matches({ lifecycle: 'idle' })).toBe(true);

      service.stop();
    });

    it('transitions between all formats without errors', () => {
      const service = startMachine();

      for (const format of ALL_FORMATS) {
        service.send({ type: 'SELECT_FORMAT', format });
        expect(service.getSnapshot().matches({ format })).toBe(true);
      }

      // And back to all
      service.send({ type: 'SELECT_FORMAT', format: 'all' });
      expect(service.getSnapshot().matches({ format: 'all' })).toBe(true);

      service.stop();
    });
  });

  // =============================================================================
  // SEARCH LIFECYCLE TESTS
  // =============================================================================

  describe('search lifecycle', () => {
    it('starts in idle state', () => {
      const service = startMachine();
      expect(service.getSnapshot().matches({ lifecycle: 'idle' })).toBe(true);
      service.stop();
    });

    it('transitions to searching on SEARCH event', () => {
      const service = startMachine({
        search: () => new Promise(() => {}), // Never resolves
      });

      service.send({ type: 'SEARCH' });
      expect(service.getSnapshot().matches({ lifecycle: 'searching' })).toBe(true);

      service.stop();
    });

    it('transitions to results after successful search', async () => {
      const service = startMachine();

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      const state = service.getSnapshot();
      expect(state.context.results).toHaveLength(1);
      expect(state.context.results[0].name).toBe('test-pkg');
      expect(state.context.continuationToken).toBe('token-1');

      service.stop();
    });

    it('transitions to error after failed search', async () => {
      const service = startMachine({
        search: () => Promise.reject(new Error('Network error')),
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'error' }));

      expect(service.getSnapshot().context.error).toBe('Network error');

      service.stop();
    });

    it('setError extracts response.data.message from Axios HTTP 400 errors', async () => {
      const axiosError = {
        isAxiosError: true,
        message: 'Request failed with status code 400',
        response: {
          status: 400,
          data: { message: 'Invalid query param' },
        },
      };

      const service = startMachine({
        search: () => Promise.reject(axiosError),
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'error' }));

      expect(service.getSnapshot().context.error).toBe('Invalid query param');

      service.stop();
    });

    it('setError falls back to err.message for network errors with no response', async () => {
      const service = startMachine({
        search: () => Promise.reject(new Error('Network error')),
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'error' }));

      expect(service.getSnapshot().context.error).toBe('Network error');

      service.stop();
    });

    it('setError uses generic fallback when rejected with null', async () => {
      const service = startMachine({
        search: () => Promise.reject(null),
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'error' }));

      expect(service.getSnapshot().context.error).toBe('Search failed');

      service.stop();
    });

    it('can retry after error', async () => {
      let callCount = 0;
      const service = startMachine({
        search: () => {
          callCount++;
          if (callCount === 1) return Promise.reject(new Error('Fail'));
          return Promise.resolve({ results: [{ id: '1', name: 'ok', format: 'maven2', repository: 'r', version: '1' }], continuationToken: undefined });
        },
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'error' }));

      service.send({ type: 'RETRY' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      expect(service.getSnapshot().context.results).toHaveLength(1);

      service.stop();
    });

    it('clears previous results when starting a new search', async () => {
      const service = startMachine();

      // First search
      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));
      expect(service.getSnapshot().context.results).toHaveLength(1);

      // Second search - results should be cleared during searching
      service.send({ type: 'SEARCH' });
      expect(service.getSnapshot().context.results).toEqual([]);

      service.stop();
    });
  });

  // =============================================================================
  // PAGINATION TESTS
  // =============================================================================

  describe('pagination', () => {
    it('loads more results with LOAD_MORE', async () => {
      const service = startMachine();

      // Initial search
      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      expect(service.getSnapshot().context.continuationToken).toBe('token-1');

      // Load more
      service.send({ type: 'LOAD_MORE' });
      await waitFor(service, (s) =>
        s.matches({ lifecycle: 'results' }) && s.context.results.length === 2
      );

      const state = service.getSnapshot();
      expect(state.context.results).toHaveLength(2);
      expect(state.context.continuationToken).toBeUndefined();

      service.stop();
    });

    it('ignores LOAD_MORE when no continuation token', async () => {
      const service = startMachine({
        search: () => Promise.resolve({ results: [{ id: '1', name: 'a', format: 'f', repository: 'r', version: '1' }], continuationToken: undefined }),
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      // No continuation token - LOAD_MORE should be ignored
      service.send({ type: 'LOAD_MORE' });
      expect(service.getSnapshot().matches({ lifecycle: 'results' })).toBe(true);

      service.stop();
    });

    it('sets error context and returns to results state when LOAD_MORE fails', async () => {
      const service = startMachine({
        loadMore: () => Promise.reject(new Error('Pagination error')),
      });

      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      service.send({ type: 'LOAD_MORE' });
      await waitFor(service, (s) =>
        s.matches({ lifecycle: 'results' }) && s.context.error !== undefined
      );

      const state = service.getSnapshot();
      expect(state.matches({ lifecycle: 'results' })).toBe(true);
      expect(state.context.error).toBe('Pagination error');

      service.stop();
    });
  });

  // =============================================================================
  // FILTER & QUERY STATE MANAGEMENT
  // =============================================================================

  describe('filter and query state', () => {
    it('updates query with SET_QUERY', () => {
      const service = startMachine();

      service.send({ type: 'SET_QUERY', query: 'commons-lang3' });
      expect(service.getSnapshot().context.query).toBe('commons-lang3');

      service.stop();
    });

    it('updates individual filter with UPDATE_FILTER', () => {
      const service = startMachine();

      service.send({ type: 'SELECT_FORMAT', format: 'maven' });
      service.send({ type: 'UPDATE_FILTER', name: 'groupId', value: 'org.apache' });

      expect(service.getSnapshot().context.filters.groupId).toBe('org.apache');

      service.stop();
    });

    it('sets all filters at once with SET_FILTERS', () => {
      const service = startMachine();

      service.send({
        type: 'SET_FILTERS',
        filters: { groupId: 'org.apache', artifactId: 'commons-lang3' },
      });

      const filters = service.getSnapshot().context.filters;
      expect(filters.groupId).toBe('org.apache');
      expect(filters.artifactId).toBe('commons-lang3');

      service.stop();
    });

    it('updates sort with SET_SORT', () => {
      const service = startMachine();

      service.send({ type: 'SET_SORT', field: 'version', direction: 'desc' });

      const ctx = service.getSnapshot().context;
      expect(ctx.sortField).toBe('version');
      expect(ctx.sortDirection).toBe('desc');

      service.stop();
    });
  });

  // =============================================================================
  // RESET BEHAVIOR
  // =============================================================================

  describe('reset', () => {
    it('RESET clears everything and returns to idle + all format', async () => {
      const service = startMachine();

      // Set format, query, filters, do a search
      service.send({ type: 'SELECT_FORMAT', format: 'docker' });
      service.send({ type: 'SET_QUERY', query: 'nginx' });
      service.send({ type: 'UPDATE_FILTER', name: 'imageName', value: 'nginx' });
      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      // Reset
      service.send({ type: 'RESET' });

      const state = service.getSnapshot();
      expect(state.matches({ lifecycle: 'idle', format: 'all' })).toBe(true);
      expect(state.context.query).toBe('');
      expect(state.context.filters).toEqual({});
      expect(state.context.results).toEqual([]);
      expect(state.context.format).toBe('all');

      service.stop();
    });
  });

  // =============================================================================
  // SEARCH WITH FORMAT-SPECIFIC CONTEXT
  // =============================================================================

  describe('search with format context', () => {
    it('search service receives correct format in context', async () => {
      let capturedFormat: string | undefined;

      const service = startMachine({
        search: (ctx: any) => {
          capturedFormat = ctx.format;
          return Promise.resolve({ results: [], continuationToken: undefined });
        },
      });

      service.send({ type: 'SELECT_FORMAT', format: 'docker' });
      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      expect(capturedFormat).toBe('docker');

      service.stop();
    });

    it('search service receives current filters in context', async () => {
      let capturedFilters: Record<string, string> | undefined;

      const service = startMachine({
        search: (ctx: any) => {
          capturedFilters = ctx.filters;
          return Promise.resolve({ results: [], continuationToken: undefined });
        },
      });

      service.send({ type: 'SELECT_FORMAT', format: 'maven' });
      service.send({ type: 'UPDATE_FILTER', name: 'groupId', value: 'org.apache' });
      service.send({ type: 'SEARCH' });
      await waitFor(service, (s) => s.matches({ lifecycle: 'results' }));

      expect(capturedFilters).toEqual({ groupId: 'org.apache' });

      service.stop();
    });
  });
});
