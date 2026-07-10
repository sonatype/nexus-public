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
  getFormatOptions,
  getFiltersForFormat,
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
  'nuget', 'p2', 'pub', 'pypi', 'r', 'raw', 'rubygems', 'swift', 'terraform', 'terraformbackend', 'yum'
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
  p2: 'p2',
  pub: 'pub',
  pypi: 'pypi',
  r: 'r',
  raw: 'raw',
  rubygems: 'rubygems',
  swift: 'swift',
  terraform: 'terraform',
  terraformbackend: 'terraformbackend',
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
  p2: /plugin/i,
  pub: /package/i,
  pypi: /package/i,
  r: /package/i,
  raw: /path|file/i,
  rubygems: /gem/i,
  swift: /package/i,
  terraform: /module/i,
  terraformbackend: /state|path/i,
  yum: /package/i,
};

// Expected filters per format (filter IDs)
// NOTE: Global filters (name, version) are handled by the UI above search results
// Format-specific filters in the sidebar contain ONLY custom attributes unique to each format
const EXPECTED_FILTERS: Record<string, string[]> = {
  // Formats with NO custom filters - only repository
  all: ['repository'],
  alpine: ['repository'],
  ansiblegalaxy: ['repository', 'namespace', 'name'],
  apt: ['repository'],
  cargo: ['repository'],
  cocoapods: ['repository'],
  conda: ['repository'],
  go: ['repository'],
  helm: ['repository'],
  huggingface: ['repository'],
  pub: ['repository'],
  r: ['repository'],
  raw: ['repository'],

  // Formats with custom filters
  composer: ['repository', 'vendor', 'package'],
  conan: ['repository', 'baseVersion', 'channel', 'revision', 'packageId', 'packageRevision'],
  docker: ['repository', 'imageName', 'imageTag', 'layerId', 'contentDigest'],
  gitlfs: ['repository', 'sha256'],
  maven: ['repository', 'groupId', 'artifactId', 'baseVersion', 'classifier', 'extension'],
  npm: ['repository', 'scope', 'author', 'description', 'keywords', 'license'],
  nuget: ['repository', 'nugetId', 'tags', 'title', 'authors', 'description', 'summary'],
  p2: ['repository', 'pluginName'],
  pypi: ['repository', 'classifiers', 'description', 'keywords', 'summary'],
  rubygems: ['repository', 'description', 'platform', 'summary'],
  swift: ['repository', 'scope'],
  terraform: ['repository', 'provider', 'namespace'],
  terraformbackend: ['repository'],
  yum: ['repository', 'yumName', 'architecture'],
};

// Expected API params for format-specific filters
// These MUST match backend SearchMappings (e.g., MavenSearchMappings.java)
const FORMAT_SPECIFIC_API_PARAMS: Record<string, Record<string, string>> = {
  ansiblegalaxy: {
    namespace: 'ansible-galaxy.namespace',
    name: 'ansible-galaxy.name',
  },
  composer: {
    vendor: 'composer.vendor',
    package: 'composer.package',
  },
  conan: {
    baseVersion: 'conan.baseVersion',
    channel: 'conan.channel',
    revision: 'conan.revision',
    packageId: 'conan.packageId',
    packageRevision: 'conan.packageRevision',
  },
  docker: {
    imageName: 'docker.imageName',
    imageTag: 'docker.imageTag',
    layerId: 'docker.layerId',
    contentDigest: 'docker.contentDigest',
  },
  gitlfs: {
    sha256: 'sha256',
  },
  maven: {
    groupId: 'maven.groupId',
    artifactId: 'maven.artifactId',
    baseVersion: 'maven.baseVersion',
    classifier: 'maven.classifier',
    extension: 'maven.extension',
  },
  npm: {
    scope: 'group', // npm scope maps to 'group' API param
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

        it('has at least one filter (repository)', () => {
          expect(FORMAT_FILTERS[format].filters.length).toBeGreaterThan(0);
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

    // Test that formats with no custom filters only have repository
    describe('Formats with no custom filters', () => {
      const formatsWithOnlyRepository = ['all', 'alpine', 'apt', 'cargo', 'cocoapods', 'conda', 'go', 'helm', 'huggingface', 'pub', 'r', 'raw', 'terraformbackend'];

      formatsWithOnlyRepository.forEach(format => {
        it(`${format} only has repository filter (no redundant name/version)`, () => {
          const filterIds = FORMAT_FILTERS[format as keyof typeof FORMAT_FILTERS].filters.map(f => f.id);
          expect(filterIds).toEqual(['repository']);
          expect(filterIds).not.toContain('name');
          expect(filterIds).not.toContain('version');
        });
      });
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

    it('returns a copy, not the original array', () => {
      const filters1 = getFiltersForFormat('maven');
      const filters2 = getFiltersForFormat('maven');

      expect(filters1).not.toBe(filters2);
      expect(filters1).toEqual(filters2);
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
      it('includes non-empty filter values', () => {
        const params = buildQueryParams('maven', '', {
          groupId: 'org.apache',
          artifactId: 'commons-lang3',
        });

        expect(params.get('maven.groupId')).toBe('org.apache');
        expect(params.get('maven.artifactId')).toBe('commons-lang3');
      });

      it('excludes empty filter values', () => {
        const params = buildQueryParams('maven', '', {
          groupId: 'org.apache',
          artifactId: '',
        });

        expect(params.get('maven.groupId')).toBe('org.apache');
        expect(params.has('maven.artifactId')).toBe(false);
      });

      it('excludes whitespace-only filter values', () => {
        const params = buildQueryParams('maven', '', {
          groupId: '   ',
        });

        expect(params.has('maven.groupId')).toBe(false);
      });

      it('trims filter value whitespace', () => {
        const params = buildQueryParams('maven', '', {
          groupId: '  org.apache  ',
        });

        expect(params.get('maven.groupId')).toBe('org.apache');
      });
    });

    // Test buildQueryParams for each format with their specific filters
    describe('Format-specific filter building', () => {
      it('builds correct params for maven', () => {
        const params = buildQueryParams('maven', 'spring', {
          groupId: 'org.springframework',
          artifactId: 'spring-core',
          baseVersion: '6.0.0',
          classifier: 'sources',
          extension: 'jar',
        });

        expect(params.get('q')).toBe('spring');
        expect(params.get('format')).toBe('maven2');
        expect(params.get('maven.groupId')).toBe('org.springframework');
        expect(params.get('maven.artifactId')).toBe('spring-core');
        expect(params.get('maven.baseVersion')).toBe('6.0.0');
        expect(params.get('maven.classifier')).toBe('sources');
        expect(params.get('maven.extension')).toBe('jar');
      });

      it('builds correct params for npm', () => {
        const params = buildQueryParams('npm', 'angular', {
          scope: '@angular',
          author: 'Google',
          description: 'Web framework',
          keywords: 'framework',
          license: 'MIT',
        });

        expect(params.get('q')).toBe('angular');
        expect(params.get('format')).toBe('npm');
        expect(params.get('group')).toBe('@angular'); // npm scope uses 'group'
        expect(params.get('npm.author')).toBe('Google');
        expect(params.get('npm.description')).toBe('Web framework');
        expect(params.get('npm.keywords')).toBe('framework');
        expect(params.get('npm.license')).toBe('MIT');
      });

      it('builds correct params for docker', () => {
        const params = buildQueryParams('docker', 'nginx', {
          imageName: 'nginx',
          imageTag: 'latest',
          layerId: 'sha256:abc123',
          contentDigest: 'sha256:def456',
        });

        expect(params.get('q')).toBe('nginx');
        expect(params.get('format')).toBe('docker');
        expect(params.get('docker.imageName')).toBe('nginx');
        expect(params.get('docker.imageTag')).toBe('latest');
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

      it('builds correct params for composer', () => {
        const params = buildQueryParams('composer', 'symfony', {
          vendor: 'symfony',
          package: 'console',
        });

        expect(params.get('format')).toBe('composer');
        expect(params.get('composer.vendor')).toBe('symfony');
        expect(params.get('composer.package')).toBe('console');
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
    });
  });
});
