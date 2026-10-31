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
  FORMATS,
  FORMAT_FILTERS,
  GENERIC_FILTERS,
  getFormatOptions,
  getFiltersForFormat,
  getAliasLabelsForFormat,
  getAliasPlaceholdersForFormat,
  getPlaceholderForFormat,
  getApiFormat,
  buildQueryParams,
} from '../searchFilters';

// =============================================================================
// TEST DATA: All expected formats with their configurations
//
// Architecture: Format definitions include both:
// 1. Global filters (name, version, repository) that work across all formats
//    per DefaultSearchMappings.java
// 2. Format-specific filters with custom API parameters per backend
//    SearchMappings (e.g., MavenSearchMappings, DockerSearchMappings)
// =============================================================================

const ALL_FORMATS = [
  'all', 'alpine','ansiblegalaxy', 'apt', 'cargo', 'cocoapods', 'composer', 'conan', 'conda',
  'docker', 'gitlfs', 'go', 'helm', 'huggingface', 'maven', 'npm',
  'nuget', 'oci', 'p2', 'pub', 'pypi', 'r', 'raw', 'rubygems', 'swift', 'terraform', 'yum'
] as const;

// Expected API format mappings
const API_FORMAT_MAPPINGS: Record<string, string> = {
  all: '',
  alpine: 'alpine',
  ansiblegalaxy: 'ansiblegalaxy',
  apt: 'apt',
  cargo: 'cargo',
  cocoapods: 'cocoapods',
  composer: 'composer',
  conan: 'conan',
  conda: 'conda',
  docker: 'docker',
  gitlfs: 'gitlfs',
  go: 'go',
  helm: 'helm',
  huggingface: 'huggingface',
  maven: 'maven2', // Special case: maven -> maven2
  npm: 'npm',
  nuget: 'nuget',
  oci: 'oci',
  p2: 'p2',
  pub: 'pub',
  pypi: 'pypi',
  r: 'r',
  raw: 'raw',
  rubygems: 'rubygems',
  swift: 'swift',
  terraform: 'terraform',
  yum: 'yum',
};

// Expected placeholder text patterns for each format
const PLACEHOLDER_PATTERNS: Record<string, RegExp> = {
  all: /component/i,
  alpine: /package/i,
  ansiblegalaxy: /namespace|collection/i,
  apt: /package/i,
  cargo: /crate/i,
  cocoapods: /pod/i,
  composer: /vendor|package/i,
  conan: /package/i,
  conda: /package/i,
  docker: /image/i,
  gitlfs: /object|sha|hash/i,
  go: /module/i,
  helm: /chart/i,
  huggingface: /model/i,
  maven: /group.*id|artifact/i,
  npm: /name|scope/i,
  nuget: /id|tags|package/i,
  oci: /image/i,
  p2: /plugin/i,
  pub: /package/i,
  pypi: /package/i,
  r: /package/i,
  raw: /path|file/i,
  rubygems: /gem/i,
  swift: /package/i,
  terraform: /module/i,
  yum: /package/i,
};

// Expected filters per format (filter IDs)
// All formats now have the generic group/name/version filters prepended after repository.
// Formats with alias-only rows (Maven groupId/artifactId, npm scope, Docker/OCI
// imageName/imageTag, Composer vendor/package, Raw name) have those rows deleted;
// the generic inputs relabeled via aliasLabels replace them.
const EXPECTED_FILTERS: Record<string, string[]> = {
  // Formats with only generic filters (repository + group + name + version)
  all: ['repository', 'group', 'name', 'version'],
  alpine: ['repository', 'group', 'name', 'version'],
  apt: ['repository', 'group', 'name', 'version'],
  cargo: ['repository', 'group', 'name', 'version'],
  cocoapods: ['repository', 'group', 'name', 'version'],
  conda: ['repository', 'group', 'name', 'version'],
  go: ['repository', 'group', 'name', 'version'],
  helm: ['repository', 'group', 'name', 'version'],
  huggingface: ['repository', 'group', 'name', 'version'],
  pub: ['repository', 'group', 'name', 'version'],
  r: ['repository', 'group', 'name', 'version'],
  raw: ['repository', 'group', 'name', 'version'],

  // Ansible Galaxy: distinct namespace/collectionName alongside generics
  // ('name' id renamed to 'collectionName' to avoid collision with generic NAME_FILTER)
  ansiblegalaxy: ['repository', 'group', 'name', 'version', 'namespace', 'collectionName'],

  // Formats with alias-only rows deleted + distinct fields kept
  // Composer: vendor/package/version alias group/name/version (deleted); description/keywords kept
  composer: ['repository', 'group', 'name', 'version', 'description', 'keywords'],
  // Docker: imageName/imageTag alias name/version (deleted); layerId/contentDigest kept
  docker: ['repository', 'group', 'name', 'version', 'layerId', 'contentDigest'],
  // Maven: groupId/artifactId alias group/name (deleted); baseVersion/classifier/extension kept
  maven: ['repository', 'group', 'name', 'version', 'baseVersion', 'classifier', 'extension'],
  // npm: scope aliases group (deleted); author/description/keywords/license kept
  npm: ['repository', 'group', 'name', 'version', 'author', 'description', 'keywords', 'license'],
  // OCI: imageName/imageTag alias name/version (deleted); contentDigest kept
  oci: ['repository', 'group', 'name', 'version', 'contentDigest'],

  // Formats with generics + distinct fields (no alias deletions needed)
  conan: ['repository', 'group', 'name', 'version', 'baseVersion', 'channel', 'revision', 'packageId', 'packageRevision'],
  gitlfs: ['repository', 'group', 'name', 'version', 'sha256'],
  nuget: ['repository', 'group', 'name', 'version', 'nugetId', 'tags', 'title', 'authors', 'description', 'summary'],
  p2: ['repository', 'group', 'name', 'version', 'pluginName'],
  pypi: ['repository', 'group', 'name', 'version', 'classifiers', 'description', 'keywords', 'summary'],
  rubygems: ['repository', 'group', 'name', 'version', 'description', 'platform', 'summary'],
  swift: ['repository', 'group', 'name', 'version', 'scope'],
  terraform: ['repository', 'group', 'name', 'version', 'provider', 'namespace'],
  yum: ['repository', 'group', 'name', 'version', 'yumName', 'architecture'],
};

// Expected API params for format-specific (non-generic) filters.
// These MUST match backend SearchMappings (e.g., MavenSearchMappings.java).
// NOTE: generic group/name/version filters (apiParams 'group'/'name'/'version') are
// verified separately in the GENERIC_FILTERS tests below.
const FORMAT_SPECIFIC_API_PARAMS: Record<string, Record<string, string>> = {
  // Ansible Galaxy: distinct FORMAT_FIELD_* filters.
  // 'name' id renamed to 'collectionName' to avoid collision with generic NAME_FILTER.
  ansiblegalaxy: {
    namespace: 'ansible-galaxy.namespace',
    collectionName: 'ansible-galaxy.name',
  },
  // Composer: vendor/package alias rows deleted; only distinct fields remain
  composer: {
    description: 'composer.description',
    keywords: 'composer.keywords',
  },
  conan: {
    baseVersion: 'conan.baseVersion',
    channel: 'conan.channel',
    revision: 'conan.revision',
    packageId: 'conan.packageId',
    packageRevision: 'conan.packageRevision',
  },
  // Docker: imageName/imageTag alias rows deleted; only distinct fields remain
  docker: {
    layerId: 'docker.layerId',
    contentDigest: 'docker.contentDigest',
  },
  gitlfs: {
    sha256: 'sha256',
  },
  // Maven: groupId/artifactId alias rows deleted; only distinct fields remain
  maven: {
    baseVersion: 'maven.baseVersion',
    classifier: 'maven.classifier',
    extension: 'maven.extension',
  },
  // npm: scope alias row deleted; only distinct fields remain
  npm: {
    author: 'npm.author',
    description: 'npm.description',
    keywords: 'npm.keywords',
    license: 'npm.license',
  },
  nuget: {
    nugetId: 'nuget.id',
    tags: 'nuget.tags',
    title: 'nuget.title',
    authors: 'nuget.authors',
    description: 'nuget.description',
    summary: 'nuget.summary',
  },
  // OCI: imageName/imageTag alias rows deleted; only distinct fields remain
  oci: {
    contentDigest: 'oci.contentDigest',
  },
  p2: {
    pluginName: 'p2.pluginName',
  },
  pypi: {
    classifiers: 'pypi.classifiers',
    description: 'pypi.description',
    keywords: 'pypi.keywords',
    summary: 'pypi.summary',
  },
  rubygems: {
    description: 'rubygems.description',
    platform: 'rubygems.platform',
    summary: 'rubygems.summary',
  },
  swift: {
    scope: 'swift.scope',
  },
  terraform: {
    provider: 'terraform.provider',
    namespace: 'terraform.namespace',
  },
  yum: {
    yumName: 'yum.name',
    architecture: 'yum.architecture',
  },
};

// =============================================================================
// TESTS: Format Definitions
// =============================================================================

describe('searchFilters', () => {
  describe('FORMATS - All 27 Formats', () => {
    it('defines exactly 27 formats', () => {
      expect(Object.keys(FORMATS).length).toBe(27);
    });

    ALL_FORMATS.forEach(format => {
      describe(`Format: ${format}`, () => {
        it('is defined in FORMATS', () => {
          expect(FORMATS[format]).toBeDefined();
        });

        it('has correct id', () => {
          expect(FORMATS[format].id).toBe(format);
        });

        it('has a non-empty label', () => {
          expect(FORMATS[format].label).toBeTruthy();
          expect(typeof FORMATS[format].label).toBe('string');
        });

        it('has a non-empty placeholder', () => {
          expect(FORMATS[format].placeholder).toBeTruthy();
          expect(typeof FORMATS[format].placeholder).toBe('string');
        });

        it('has correct API format mapping', () => {
          expect(FORMATS[format].apiFormat).toBe(API_FORMAT_MAPPINGS[format]);
        });

        it('has placeholder matching expected pattern', () => {
          const placeholder = FORMATS[format].placeholder.toLowerCase();
          expect(placeholder).toMatch(PLACEHOLDER_PATTERNS[format]);
        });
      });
    });
  });

  // =============================================================================
  // TESTS: Format Filters
  // =============================================================================

  describe('FORMAT_FILTERS - All 27 Formats', () => {
    it('defines filters for exactly 27 formats', () => {
      expect(Object.keys(FORMAT_FILTERS).length).toBe(27);
    });

    ALL_FORMATS.forEach(format => {
      describe(`Filters for: ${format}`, () => {
        it('is defined in FORMAT_FILTERS', () => {
          expect(FORMAT_FILTERS[format]).toBeDefined();
        });

        it('has a filters array', () => {
          expect(FORMAT_FILTERS[format].filters).toBeInstanceOf(Array);
        });

        it('has at least four filters (repository + group + name + version)', () => {
          expect(FORMAT_FILTERS[format].filters.length).toBeGreaterThanOrEqual(4);
        });

        it('includes repository filter', () => {
          const filterIds = FORMAT_FILTERS[format].filters.map(f => f.id);
          expect(filterIds).toContain('repository');
        });

        it('has all expected filters', () => {
          const filterIds = FORMAT_FILTERS[format].filters.map(f => f.id);
          const expectedFilterIds = EXPECTED_FILTERS[format];

          expectedFilterIds.forEach(expectedId => {
            expect(filterIds).toContain(expectedId);
          });
        });

        it('has correct number of filters', () => {
          expect(FORMAT_FILTERS[format].filters.length).toBe(EXPECTED_FILTERS[format].length);
        });

        // Test each filter in the format
        FORMAT_FILTERS[format].filters.forEach(filter => {
          describe(`Filter: ${filter.id}`, () => {
            it('has required properties', () => {
              expect(filter.id).toBeTruthy();
              expect(filter.label).toBeTruthy();
              expect(filter.type).toBeTruthy();
              expect(filter.apiParam).toBeTruthy();
            });

            it('has valid type', () => {
              expect(['text', 'select']).toContain(filter.type);
            });

            it('has placeholder', () => {
              expect(filter.placeholder).toBeTruthy();
            });
          });
        });
      });
    });

    // Test format-specific filter API params
    describe('Format-specific API params', () => {
      Object.entries(FORMAT_SPECIFIC_API_PARAMS).forEach(([format, apiParams]) => {
        describe(`${format} format-specific filters`, () => {
          Object.entries(apiParams).forEach(([filterId, expectedApiParam]) => {
            it(`${filterId} has apiParam "${expectedApiParam}"`, () => {
              const filter = FORMAT_FILTERS[format as keyof typeof FORMAT_FILTERS].filters.find(
                f => f.id === filterId
              );
              expect(filter).toBeDefined();
              expect(filter?.apiParam).toBe(expectedApiParam);
            });
          });
        });
      });
    });

    // Test that formats with only generic filters have exactly repository + group + name + version
    describe('Formats with only generic filters (no format-specific rows)', () => {
      const genericOnlyFormats = ['all', 'alpine', 'apt', 'cargo', 'cocoapods', 'conda', 'go', 'helm', 'huggingface', 'pub', 'r', 'raw'];

      genericOnlyFormats.forEach(format => {
        it(`${format} has exactly repository + group + name + version`, () => {
          const filterIds = FORMAT_FILTERS[format as keyof typeof FORMAT_FILTERS].filters.map(f => f.id);
          expect(filterIds).toEqual(['repository', 'group', 'name', 'version']);
        });
      });
    });
  });

  // =============================================================================
  // TESTS: GENERIC_FILTERS (Task 2 — plan §Task 2 step 7)
  // =============================================================================

  describe('GENERIC_FILTERS', () => {
    it('contains exactly group, name, version in that order', () => {
      const ids = GENERIC_FILTERS.map(f => f.id);
      expect(ids).toEqual(['group', 'name', 'version']);
    });

    it('GENERIC_FILTERS is prepended after repository on every format including "all"', () => {
      ALL_FORMATS.forEach(format => {
        const filterIds = FORMAT_FILTERS[format].filters.map(f => f.id);
        expect(filterIds[0]).toBe('repository');
        expect(filterIds[1]).toBe('group');
        expect(filterIds[2]).toBe('name');
        expect(filterIds[3]).toBe('version');
      });
    });

    it('aliasLabels populated only for the six alias formats', () => {
      const aliasFormats = ['maven', 'npm', 'docker', 'oci', 'composer', 'raw'] as const;
      aliasFormats.forEach(format => {
        expect(FORMAT_FILTERS[format].aliasLabels).toBeDefined();
      });

      // All other formats must NOT have aliasLabels
      ALL_FORMATS.filter(f => !(aliasFormats as readonly string[]).includes(f)).forEach(format => {
        expect(FORMAT_FILTERS[format].aliasLabels).toBeUndefined();
      });
    });

    it('NuGet Package ID stays as its own distinct filter with id nugetId (regression guard)', () => {
      const nugetFilters = FORMAT_FILTERS.nuget.filters;
      const nugetIdFilter = nugetFilters.find(f => f.id === 'nugetId');
      expect(nugetIdFilter).toBeDefined();
      expect(nugetIdFilter?.apiParam).toBe('nuget.id');
      expect(nugetIdFilter?.label).toBe('Package ID');
      // generic name is also present and separate
      const nameFilter = nugetFilters.find(f => f.id === 'name');
      expect(nameFilter).toBeDefined();
      expect(nameFilter?.apiParam).toBe('name');
    });

    // Prepending GENERIC_FILTERS to every format means any format-specific row
    // reusing id 'group', 'name', or 'version' would collide silently (duplicate
    // sidebar rows, one wins in filter state and buildQueryParams). Distinct rows
    // that would have collided are renamed — collectionName (ansiblegalaxy),
    // nugetId (nuget), yumName (yum) — so this guard catches a future format
    // declaring a colliding id.
    it.each(ALL_FORMATS)('has unique filter IDs for %s', (format) => {
      const ids = getFiltersForFormat(format).map(f => f.id);
      expect(ids.length).toBe(new Set(ids).size);
    });
  });

  // =============================================================================
  // TESTS: getAliasLabelsForFormat
  // =============================================================================

  describe('getAliasLabelsForFormat', () => {
    it('returns Group ID / Artifact ID aliases for maven', () => {
      const labels = getAliasLabelsForFormat('maven');
      expect(labels).toEqual({ group: 'Group ID', name: 'Artifact ID' });
    });

    it('returns Scope alias for npm', () => {
      const labels = getAliasLabelsForFormat('npm');
      expect(labels).toEqual({ group: 'Scope' });
    });

    it('returns Image Name / Image Tag aliases for docker', () => {
      const labels = getAliasLabelsForFormat('docker');
      expect(labels).toEqual({ name: 'Image Name', version: 'Image Tag' });
    });

    it('returns Image Name / Image Tag aliases for oci', () => {
      const labels = getAliasLabelsForFormat('oci');
      expect(labels).toEqual({ name: 'Image Name', version: 'Image Tag' });
    });

    it('returns Vendor / Package aliases for composer (version stays generic)', () => {
      const labels = getAliasLabelsForFormat('composer');
      expect(labels).toEqual({ group: 'Vendor', name: 'Package' });
    });

    it('returns Filename alias for raw', () => {
      const labels = getAliasLabelsForFormat('raw');
      expect(labels).toEqual({ name: 'Filename' });
    });

    it('returns undefined for formats without aliases (all, alpine, nuget, pypi)', () => {
      expect(getAliasLabelsForFormat('all')).toBeUndefined();
      expect(getAliasLabelsForFormat('alpine')).toBeUndefined();
      expect(getAliasLabelsForFormat('nuget')).toBeUndefined();
      expect(getAliasLabelsForFormat('pypi')).toBeUndefined();
    });
  });

  // =============================================================================
  // TESTS: getAliasPlaceholdersForFormat
  //
  // Formats whose aliasLabels change the vocabulary (Scope, Image Name/Tag,
  // Vendor/Package, Filename) must ship matching placeholder overrides so the
  // input hint does not contradict the label.  Maven keeps the generic
  // placeholders because they are already Maven-flavored.
  // =============================================================================

  describe('getAliasPlaceholdersForFormat', () => {
    it('supplies scope-flavored placeholder for npm', () => {
      expect(getAliasPlaceholdersForFormat('npm')).toEqual({ group: 'e.g., @angular' });
    });

    it('supplies image name/tag placeholders for docker', () => {
      expect(getAliasPlaceholdersForFormat('docker')).toEqual({
        name: 'e.g., nginx',
        version: 'e.g., latest',
      });
    });

    it('supplies image name/tag placeholders for oci', () => {
      expect(getAliasPlaceholdersForFormat('oci')).toEqual({
        name: 'e.g., nginx',
        version: 'e.g., latest',
      });
    });

    it('supplies vendor/package placeholders for composer', () => {
      expect(getAliasPlaceholdersForFormat('composer')).toEqual({
        group: 'e.g., symfony',
        name: 'e.g., console',
      });
    });

    it('supplies filename placeholder for raw', () => {
      expect(getAliasPlaceholdersForFormat('raw')).toEqual({ name: 'e.g., README.md' });
    });

    it('returns undefined for maven (generic placeholders already Maven-flavored)', () => {
      expect(getAliasPlaceholdersForFormat('maven')).toBeUndefined();
    });

    it('returns undefined for formats without label aliases', () => {
      expect(getAliasPlaceholdersForFormat('all')).toBeUndefined();
      expect(getAliasPlaceholdersForFormat('alpine')).toBeUndefined();
      expect(getAliasPlaceholdersForFormat('nuget')).toBeUndefined();
      expect(getAliasPlaceholdersForFormat('pypi')).toBeUndefined();
    });
  });

  // =============================================================================
  // TESTS: Helper Functions
  // =============================================================================

  describe('getFormatOptions', () => {
    it('returns array of all formats', () => {
      const options = getFormatOptions();
      expect(Array.isArray(options)).toBe(true);
      expect(options.length).toBe(27);
    });

    it('includes all expected formats', () => {
      const options = getFormatOptions();
      const optionIds = options.map(o => o.id);

      ALL_FORMATS.forEach(format => {
        expect(optionIds).toContain(format);
      });
    });

    it('each option has id and label', () => {
      const options = getFormatOptions();

      options.forEach(option => {
        expect(option.id).toBeTruthy();
        expect(option.label).toBeTruthy();
      });
    });
  });

  describe('getFiltersForFormat', () => {
    ALL_FORMATS.forEach(format => {
      it(`returns correct filters for ${format}`, () => {
        const filters = getFiltersForFormat(format);
        const filterIds = filters.map(f => f.id);

        expect(filterIds).toEqual(EXPECTED_FILTERS[format]);
      });
    });

    it('returns the same readonly reference across calls', () => {
      // Contract: callers only read. Returning the module-level array avoids
      // per-render allocations on every SearchSidebar render / buildQueryParams /
      // URL round-trip. A caller that needs a mutable list must copy explicitly.
      const filters1 = getFiltersForFormat('maven');
      const filters2 = getFiltersForFormat('maven');

      expect(filters1).toBe(filters2);
    });
  });

  describe('getPlaceholderForFormat', () => {
    ALL_FORMATS.forEach(format => {
      it(`returns correct placeholder for ${format}`, () => {
        const placeholder = getPlaceholderForFormat(format);

        expect(placeholder).toBeTruthy();
        expect(typeof placeholder).toBe('string');
        expect(placeholder.toLowerCase()).toMatch(PLACEHOLDER_PATTERNS[format]);
      });
    });
  });

  describe('getApiFormat', () => {
    ALL_FORMATS.forEach(format => {
      it(`returns "${API_FORMAT_MAPPINGS[format]}" for ${format}`, () => {
        expect(getApiFormat(format)).toBe(API_FORMAT_MAPPINGS[format]);
      });
    });
  });

  // =============================================================================
  // TESTS: buildQueryParams
  // =============================================================================

  describe('buildQueryParams', () => {
    describe('Basic functionality', () => {
      it('builds params with query', () => {
        const params = buildQueryParams('all', 'lodash', {});
        expect(params.get('q')).toBe('lodash');
      });

      it('excludes empty query', () => {
        const params = buildQueryParams('all', '', {});
        expect(params.has('q')).toBe(false);
      });

      it('excludes whitespace-only query', () => {
        const params = buildQueryParams('all', '   ', {});
        expect(params.has('q')).toBe(false);
      });

      it('trims query whitespace', () => {
        const params = buildQueryParams('all', '  lodash  ', {});
        expect(params.get('q')).toBe('lodash');
      });
    });

    describe('Format parameter', () => {
      it('excludes format param for "all"', () => {
        const params = buildQueryParams('all', 'test', {});
        expect(params.has('format')).toBe(false);
      });

      ALL_FORMATS.filter(f => f !== 'all').forEach(format => {
        it(`includes correct format param for ${format}`, () => {
          const params = buildQueryParams(format, '', {});

          if (API_FORMAT_MAPPINGS[format]) {
            expect(params.get('format')).toBe(API_FORMAT_MAPPINGS[format]);
          }
        });
      });
    });

    describe('Filter values', () => {
      it('includes non-empty filter values (generic group/name keys)', () => {
        const params = buildQueryParams('maven', '', {
          group: 'org.apache',
          name: 'commons-lang3',
        });

        expect(params.get('group')).toBe('org.apache');
        expect(params.get('name')).toBe('commons-lang3');
      });

      it('excludes empty filter values', () => {
        const params = buildQueryParams('maven', '', {
          group: 'org.apache',
          name: '',
        });

        expect(params.get('group')).toBe('org.apache');
        expect(params.has('name')).toBe(false);
      });

      it('excludes whitespace-only filter values', () => {
        const params = buildQueryParams('maven', '', {
          group: '   ',
        });

        expect(params.has('group')).toBe(false);
      });

      it('trims filter value whitespace', () => {
        const params = buildQueryParams('maven', '', {
          group: '  org.apache  ',
        });

        expect(params.get('group')).toBe('org.apache');
      });
    });

    // Test buildQueryParams for each format with their specific filters
    describe('Format-specific filter building', () => {
      it('builds correct params for maven (generic group/name, not maven.groupId/artifactId)', () => {
        const params = buildQueryParams('maven', 'spring', {
          group: 'org.springframework',
          name: 'spring-core',
          baseVersion: '6.0.0',
          classifier: 'sources',
          extension: 'jar',
        });

        expect(params.get('q')).toBe('spring');
        expect(params.get('format')).toBe('maven2');
        // generic group/name params emitted through GENERIC_FILTERS in the loop
        expect(params.get('group')).toBe('org.springframework');
        expect(params.get('name')).toBe('spring-core');
        // old alias params must not appear
        expect(params.has('maven.groupId')).toBe(false);
        expect(params.has('maven.artifactId')).toBe(false);
        expect(params.get('maven.baseVersion')).toBe('6.0.0');
        expect(params.get('maven.classifier')).toBe('sources');
        expect(params.get('maven.extension')).toBe('jar');
      });

      it('builds correct params for npm (generic group for scope, not old scope filter)', () => {
        const params = buildQueryParams('npm', 'angular', {
          group: '@angular',
          author: 'Google',
          description: 'Web framework',
          keywords: 'framework',
          license: 'MIT',
        });

        expect(params.get('q')).toBe('angular');
        expect(params.get('format')).toBe('npm');
        // generic group emitted via GENERIC_FILTERS
        expect(params.get('group')).toBe('@angular');
        expect(params.get('npm.author')).toBe('Google');
        expect(params.get('npm.description')).toBe('Web framework');
        expect(params.get('npm.keywords')).toBe('framework');
        expect(params.get('npm.license')).toBe('MIT');
      });

      it('builds correct params for docker (generic name/version, not docker.imageName/imageTag)', () => {
        const params = buildQueryParams('docker', 'nginx', {
          name: 'nginx',
          version: 'latest',
          layerId: 'sha256:abc123',
          contentDigest: 'sha256:def456',
        });

        expect(params.get('q')).toBe('nginx');
        expect(params.get('format')).toBe('docker');
        // generic name/version emitted via GENERIC_FILTERS
        expect(params.get('name')).toBe('nginx');
        expect(params.get('version')).toBe('latest');
        // old alias params must not appear
        expect(params.has('docker.imageName')).toBe(false);
        expect(params.has('docker.imageTag')).toBe(false);
        expect(params.get('docker.layerId')).toBe('sha256:abc123');
        expect(params.get('docker.contentDigest')).toBe('sha256:def456');
      });

      it('builds correct params for pypi', () => {
        const params = buildQueryParams('pypi', 'django', {
          classifiers: 'Framework :: Django',
          description: 'A web framework',
          summary: 'Web framework',
          keywords: 'web framework',
        });

        expect(params.get('format')).toBe('pypi');
        expect(params.get('pypi.classifiers')).toBe('Framework :: Django');
        expect(params.get('pypi.description')).toBe('A web framework');
        expect(params.get('pypi.summary')).toBe('Web framework');
        expect(params.get('pypi.keywords')).toBe('web framework');
      });

      it('builds correct params for nuget', () => {
        const params = buildQueryParams('nuget', 'newtonsoft', {
          nugetId: 'Newtonsoft.Json',
          tags: 'json serialization',
          title: 'Json.NET',
          authors: 'James Newton-King',
          description: 'JSON framework',
          summary: 'Popular JSON library',
        });

        expect(params.get('format')).toBe('nuget');
        expect(params.get('nuget.id')).toBe('Newtonsoft.Json');
        expect(params.get('nuget.tags')).toBe('json serialization');
        expect(params.get('nuget.title')).toBe('Json.NET');
        expect(params.get('nuget.authors')).toBe('James Newton-King');
        expect(params.get('nuget.description')).toBe('JSON framework');
        expect(params.get('nuget.summary')).toBe('Popular JSON library');
      });

      it('builds correct params for composer (generic group/name, not composer.vendor/package)', () => {
        const params = buildQueryParams('composer', 'symfony', {
          group: 'symfony',
          name: 'console',
        });

        expect(params.get('format')).toBe('composer');
        // generic group/name emitted via GENERIC_FILTERS
        expect(params.get('group')).toBe('symfony');
        expect(params.get('name')).toBe('console');
        // old alias params must not appear
        expect(params.has('composer.vendor')).toBe(false);
        expect(params.has('composer.package')).toBe(false);
      });

      it('builds correct params for conan with all filters', () => {
        const params = buildQueryParams('conan', 'boost', {
          baseVersion: '1.80.0',
          channel: 'stable',
          revision: 'abc123',
          packageId: 'def456',
          packageRevision: 'ghi789',
        });

        expect(params.get('format')).toBe('conan');
        expect(params.get('conan.baseVersion')).toBe('1.80.0');
        expect(params.get('conan.channel')).toBe('stable');
        expect(params.get('conan.revision')).toBe('abc123');
        expect(params.get('conan.packageId')).toBe('def456');
        expect(params.get('conan.packageRevision')).toBe('ghi789');
      });

      it('builds correct params for rubygems', () => {
        const params = buildQueryParams('rubygems', 'rails', {
          description: 'Full-stack web framework',
          platform: 'ruby',
          summary: 'Web framework',
        });

        expect(params.get('format')).toBe('rubygems');
        expect(params.get('rubygems.description')).toBe('Full-stack web framework');
        expect(params.get('rubygems.platform')).toBe('ruby');
        expect(params.get('rubygems.summary')).toBe('Web framework');
      });

      it('builds correct params for yum', () => {
        const params = buildQueryParams('yum', 'httpd', {
          yumName: 'httpd',
          architecture: 'x86_64',
        });

        expect(params.get('format')).toBe('yum');
        expect(params.get('yum.name')).toBe('httpd');
        expect(params.get('yum.architecture')).toBe('x86_64');
      });

      it('builds correct params for gitlfs', () => {
        const params = buildQueryParams('gitlfs', '', {
          sha256: 'abc123def456',
        });

        expect(params.get('format')).toBe('gitlfs');
        expect(params.get('sha256')).toBe('abc123def456');
      });

      it('builds correct params for p2', () => {
        const params = buildQueryParams('p2', 'eclipse', {
          pluginName: 'org.eclipse.jdt',
        });

        expect(params.get('format')).toBe('p2');
        expect(params.get('p2.pluginName')).toBe('org.eclipse.jdt');
      });

      it('builds correct params for swift', () => {
        const params = buildQueryParams('swift', 'alamofire', {
          scope: 'Alamofire',
        });

        expect(params.get('format')).toBe('swift');
        expect(params.get('swift.scope')).toBe('Alamofire');
      });

      it('builds correct params for terraform', () => {
        const params = buildQueryParams('terraform', 'aws', {
          provider: 'hashicorp',
          namespace: 'aws',
        });

        expect(params.get('format')).toBe('terraform');
        expect(params.get('terraform.provider')).toBe('hashicorp');
        expect(params.get('terraform.namespace')).toBe('aws');
      });
    });

    describe('Repository filter (common to all formats)', () => {
      ALL_FORMATS.forEach(format => {
        it(`includes repository filter for ${format}`, () => {
          const params = buildQueryParams(format, '', {
            repository: 'my-repo',
          });

          expect(params.get('repository')).toBe('my-repo');
        });
      });
    });

    describe('Generic group / name / version params (Task 2 assertions)', () => {
      it('emits group / name / version params for format=all', () => {
        const params = buildQueryParams('all', '', { group: 'g1', name: 'n1', version: 'v1' });
        expect(params.get('group')).toBe('g1');
        expect(params.get('name')).toBe('n1');
        expect(params.get('version')).toBe('v1');
      });

      it('emits group / name / version params for maven via generic path (not maven.groupId/artifactId)', () => {
        const params = buildQueryParams('maven', '', { group: 'com.example', name: 'my-lib', version: '1.0.0' });
        expect(params.get('group')).toBe('com.example');
        expect(params.get('name')).toBe('my-lib');
        expect(params.get('version')).toBe('1.0.0');
        expect(params.has('maven.groupId')).toBe(false);
        expect(params.has('maven.artifactId')).toBe(false);
      });
    });

    describe('nameOrVersion combined filter (from UI above results)', () => {
      it('sends version-like values to version param', () => {
        const params = buildQueryParams('all', '', {
          nameOrVersion: '1.0.0',
        });
        expect(params.get('version')).toBe('1.0.0');
        expect(params.has('q')).toBe(false);
      });

      it('sends v-prefixed version values to version param', () => {
        const params = buildQueryParams('all', '', {
          nameOrVersion: 'v2.0.0',
        });
        expect(params.get('version')).toBe('v2.0.0');
      });

      it('sends non-version values to q param', () => {
        const params = buildQueryParams('all', '', {
          nameOrVersion: 'lodash',
        });
        expect(params.get('q')).toBe('lodash');
        expect(params.has('version')).toBe(false);
      });

      it('appends to existing q param for non-version values', () => {
        const params = buildQueryParams('all', 'search-term', {
          nameOrVersion: 'lodash',
        });
        expect(params.get('q')).toBe('search-term lodash');
      });

      it('preserves version set by nameOrVersion over a stale sidebar version filter', () => {
        // Regression: with the pre-refactor `!params.has('version')` gate the
        // header nameOrVersion (visible near the results) wins over any sidebar
        // version that has been left set from a previous search.  Without the
        // gate the generic-filters loop would silently overwrite it.
        const params = buildQueryParams('maven', '', {
          nameOrVersion: '1.2.3',
          version: '2.0.0',
        });
        expect(params.get('version')).toBe('1.2.3');
      });

      it('emits sidebar version when nameOrVersion is not version-like', () => {
        const params = buildQueryParams('maven', '', {
          nameOrVersion: 'lodash',
          version: '2.0.0',
        });
        expect(params.get('version')).toBe('2.0.0');
        expect(params.get('q')).toBe('lodash');
      });
    });
  });
});
