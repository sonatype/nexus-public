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

/**
 * Comprehensive tests for Unified Search - All Formats and Filters
 *
 * Tests that every format has correct filter definitions and that
 * all filters work correctly for their respective formats.
 *
 * ARCHITECTURE:
 * - Global filters (name, version) are handled by the UI above search results
 * - Format-specific filters in the sidebar contain ONLY custom attributes unique to each format
 * - This matches the Default UI (ExtJS) architecture and backend SearchMappings
 */

import { describe, it, expect } from '@jest/globals';
import { FORMATS, FORMAT_FILTERS, getFiltersForFormat, buildQueryParams } from '../searchFilters';
import type { SearchFormat } from '../unified.types';

/**
 * All formats that should be tested.
 */
const ALL_FORMATS: SearchFormat[] = [
  'all',
  'apt',
  'cargo',
  'cocoapods',
  'composer',
  'conan',
  'conda',
  'docker',
  'gitlfs',
  'go',
  'helm',
  'huggingface',
  'maven',
  'npm',
  'nuget',
  'p2',
  'pypi',
  'r',
  'raw',
  'rubygems',
  'swift',
  'terraform',
  'yum',
];

describe('Unified Search - Format Definitions', () => {
  describe('All formats are defined', () => {
    it.each(ALL_FORMATS)('should have format definition for %s', (format) => {
      expect(FORMATS[format]).toBeDefined();
      expect(FORMATS[format].id).toBe(format);
      expect(FORMATS[format].label).toBeTruthy();
      expect(FORMATS[format].placeholder).toBeTruthy();
    });
  });

  describe('All formats have filter configurations', () => {
    it.each(ALL_FORMATS)('should have filter config for %s', (format) => {
      expect(FORMAT_FILTERS[format]).toBeDefined();
      expect(FORMAT_FILTERS[format].format).toBeDefined();
      expect(FORMAT_FILTERS[format].filters).toBeDefined();
      expect(Array.isArray(FORMAT_FILTERS[format].filters)).toBe(true);
      expect(FORMAT_FILTERS[format].filters.length).toBeGreaterThan(0);
    });
  });

  describe('Repository filter is present for all formats', () => {
    it.each(ALL_FORMATS)('should have repository filter for %s', (format) => {
      const filters = getFiltersForFormat(format);
      const repoFilter = filters.find((f) => f.id === 'repository');
      expect(repoFilter).toBeDefined();
      expect(repoFilter?.apiParam).toBe('repository');
      expect(repoFilter?.type).toBe('select');
    });
  });
});

describe('Unified Search - Format-Specific Filters', () => {
  describe('Maven filters', () => {
    it('should have all Maven-specific filters', () => {
      const filters = getFiltersForFormat('maven');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('groupId');
      expect(filterIds).toContain('artifactId');
      expect(filterIds).toContain('baseVersion');
      expect(filterIds).toContain('classifier');
      expect(filterIds).toContain('extension');
      // Note: name and version are global filters (above search results)
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for Maven filters', () => {
      const filters = getFiltersForFormat('maven');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('groupId')?.apiParam).toBe('maven.groupId');
      expect(filterMap.get('artifactId')?.apiParam).toBe('maven.artifactId');
      expect(filterMap.get('baseVersion')?.apiParam).toBe('maven.baseVersion');
      expect(filterMap.get('classifier')?.apiParam).toBe('maven.classifier');
      expect(filterMap.get('extension')?.apiParam).toBe('maven.extension');
    });
  });

  describe('npm filters', () => {
    it('should have all npm-specific filters', () => {
      const filters = getFiltersForFormat('npm');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('scope');
      expect(filterIds).toContain('author');
      expect(filterIds).toContain('description');
      expect(filterIds).toContain('keywords');
      expect(filterIds).toContain('license');
      // Note: name and version are global filters (above search results)
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for npm filters', () => {
      const filters = getFiltersForFormat('npm');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('scope')?.apiParam).toBe('group');
      expect(filterMap.get('author')?.apiParam).toBe('npm.author');
      expect(filterMap.get('description')?.apiParam).toBe('npm.description');
      expect(filterMap.get('keywords')?.apiParam).toBe('npm.keywords');
      expect(filterMap.get('license')?.apiParam).toBe('npm.license');
    });
  });

  describe('Docker filters', () => {
    it('should have all Docker-specific filters', () => {
      const filters = getFiltersForFormat('docker');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('imageName');
      expect(filterIds).toContain('imageTag');
      expect(filterIds).toContain('layerId');
      expect(filterIds).toContain('contentDigest');
    });

    it('should have correct API parameters for Docker filters', () => {
      const filters = getFiltersForFormat('docker');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('imageName')?.apiParam).toBe('docker.imageName');
      expect(filterMap.get('imageTag')?.apiParam).toBe('docker.imageTag');
      expect(filterMap.get('layerId')?.apiParam).toBe('docker.layerId');
      expect(filterMap.get('contentDigest')?.apiParam).toBe('docker.contentDigest');
    });
  });

  describe('Composer filters', () => {
    it('should have all Composer-specific filters', () => {
      const filters = getFiltersForFormat('composer');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('vendor');
      expect(filterIds).toContain('package');
      // Note: version is a global filter (above search results)
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for Composer filters', () => {
      const filters = getFiltersForFormat('composer');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('vendor')?.apiParam).toBe('composer.vendor');
      expect(filterMap.get('package')?.apiParam).toBe('composer.package');
    });
  });

  describe('Conan filters', () => {
    it('should have all Conan-specific filters', () => {
      const filters = getFiltersForFormat('conan');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('baseVersion');
      expect(filterIds).toContain('channel');
      expect(filterIds).toContain('revision');
      expect(filterIds).toContain('packageId');
      expect(filterIds).toContain('packageRevision');
      // Note: name and version are global filters (above search results)
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for Conan filters', () => {
      const filters = getFiltersForFormat('conan');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('baseVersion')?.apiParam).toBe('conan.baseVersion');
      expect(filterMap.get('channel')?.apiParam).toBe('conan.channel');
      expect(filterMap.get('revision')?.apiParam).toBe('conan.revision');
      expect(filterMap.get('packageId')?.apiParam).toBe('conan.packageId');
      expect(filterMap.get('packageRevision')?.apiParam).toBe('conan.packageRevision');
    });
  });

  describe('PyPI filters', () => {
    it('should have all PyPI-specific filters', () => {
      const filters = getFiltersForFormat('pypi');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('classifiers');
      expect(filterIds).toContain('description');
      expect(filterIds).toContain('summary');
      expect(filterIds).toContain('keywords');
      // Note: name and version are global filters (above search results)
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for PyPI filters', () => {
      const filters = getFiltersForFormat('pypi');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('classifiers')?.apiParam).toBe('pypi.classifiers');
      expect(filterMap.get('description')?.apiParam).toBe('pypi.description');
      expect(filterMap.get('summary')?.apiParam).toBe('pypi.summary');
      expect(filterMap.get('keywords')?.apiParam).toBe('pypi.keywords');
    });
  });

  describe('NuGet filters', () => {
    it('should have all NuGet-specific filters', () => {
      const filters = getFiltersForFormat('nuget');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('nugetId');
      expect(filterIds).toContain('tags');
      expect(filterIds).toContain('title');
      expect(filterIds).toContain('authors');
      expect(filterIds).toContain('description');
      expect(filterIds).toContain('summary');
    });

    it('should have correct API parameters for NuGet filters', () => {
      const filters = getFiltersForFormat('nuget');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('nugetId')?.apiParam).toBe('nuget.id');
      expect(filterMap.get('tags')?.apiParam).toBe('nuget.tags');
      expect(filterMap.get('title')?.apiParam).toBe('nuget.title');
      expect(filterMap.get('authors')?.apiParam).toBe('nuget.authors');
      expect(filterMap.get('description')?.apiParam).toBe('nuget.description');
      expect(filterMap.get('summary')?.apiParam).toBe('nuget.summary');
    });
  });

  describe('RubyGems filters', () => {
    it('should have all RubyGems-specific filters', () => {
      const filters = getFiltersForFormat('rubygems');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('description');
      expect(filterIds).toContain('platform');
      expect(filterIds).toContain('summary');
      // Note: name and version are global filters (above search results)
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for RubyGems filters', () => {
      const filters = getFiltersForFormat('rubygems');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('description')?.apiParam).toBe('rubygems.description');
      expect(filterMap.get('platform')?.apiParam).toBe('rubygems.platform');
      expect(filterMap.get('summary')?.apiParam).toBe('rubygems.summary');
    });
  });

  describe('Yum filters', () => {
    it('should have all Yum-specific filters', () => {
      const filters = getFiltersForFormat('yum');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('yumName');
      expect(filterIds).toContain('architecture');
      // Note: version is a global filter (above search results)
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for Yum filters', () => {
      const filters = getFiltersForFormat('yum');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('yumName')?.apiParam).toBe('yum.name');
      expect(filterMap.get('architecture')?.apiParam).toBe('yum.architecture');
    });
  });

  describe('P2 filters', () => {
    it('should have all P2-specific filters', () => {
      const filters = getFiltersForFormat('p2');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('pluginName');
      // Note: name is a global filter (above search results)
      expect(filterIds).not.toContain('name');
    });

    it('should have correct API parameters for P2 filters', () => {
      const filters = getFiltersForFormat('p2');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('pluginName')?.apiParam).toBe('p2.pluginName');
    });
  });

  describe('Git LFS filters', () => {
    it('should have all Git LFS-specific filters', () => {
      const filters = getFiltersForFormat('gitlfs');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('sha256');
    });

    it('should have correct API parameters for Git LFS filters', () => {
      const filters = getFiltersForFormat('gitlfs');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('sha256')?.apiParam).toBe('sha256');
    });
  });

  describe('Swift filters', () => {
    it('should have all Swift-specific filters', () => {
      const filters = getFiltersForFormat('swift');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('scope');
      // Note: name and version are global filters
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for Swift filters', () => {
      const filters = getFiltersForFormat('swift');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('scope')?.apiParam).toBe('swift.scope');
    });
  });

  describe('Terraform filters', () => {
    it('should have all Terraform-specific filters', () => {
      const filters = getFiltersForFormat('terraform');
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toContain('provider');
      expect(filterIds).toContain('namespace');
      // Note: name and version are global filters
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });

    it('should have correct API parameters for Terraform filters', () => {
      const filters = getFiltersForFormat('terraform');
      const filterMap = new Map(filters.map((f) => [f.id, f]));

      expect(filterMap.get('provider')?.apiParam).toBe('terraform.provider');
      expect(filterMap.get('namespace')?.apiParam).toBe('terraform.namespace');
    });
  });

  describe('Simple formats (only repository filter)', () => {
    // These formats use global name/version filters and have no custom filters
    const simpleFormats: SearchFormat[] = [
      'all',
      'apt',
      'cargo',
      'cocoapods',
      'conda',
      'go',
      'helm',
      'huggingface',
      'r',
      'raw',
    ];

    it.each(simpleFormats)('should only have repository filter for %s (no redundant name/version)', (format) => {
      const filters = getFiltersForFormat(format);
      const filterIds = filters.map((f) => f.id);

      expect(filterIds).toContain('repository');
      expect(filterIds).toEqual(['repository']);
      // Verify no redundant name/version filters
      expect(filterIds).not.toContain('name');
      expect(filterIds).not.toContain('version');
    });
  });
});

describe('Unified Search - Query Parameter Building', () => {
  describe('buildQueryParams', () => {
    it('should build correct params for Maven search', () => {
      const params = buildQueryParams(
        'maven',
        'spring',
        {
          repository: 'maven-central',
          groupId: 'org.springframework',
          artifactId: 'spring-core',
        }
      );

      expect(params.get('format')).toBe('maven2');
      expect(params.get('q')).toBe('spring');
      expect(params.get('repository')).toBe('maven-central');
      expect(params.get('maven.groupId')).toBe('org.springframework');
      expect(params.get('maven.artifactId')).toBe('spring-core');
    });

    it('should build correct params for Docker search', () => {
      const params = buildQueryParams(
        'docker',
        'nginx',
        {
          repository: 'docker-proxy',
          imageName: 'nginx',
          imageTag: 'latest',
        }
      );

      expect(params.get('format')).toBe('docker');
      expect(params.get('q')).toBe('nginx');
      expect(params.get('repository')).toBe('docker-proxy');
      expect(params.get('docker.imageName')).toBe('nginx');
      expect(params.get('docker.imageTag')).toBe('latest');
    });

    it('should build correct params for npm search', () => {
      const params = buildQueryParams(
        'npm',
        'react',
        {
          repository: 'npm-proxy',
          scope: '@angular',
          author: 'angular',
        }
      );

      expect(params.get('format')).toBe('npm');
      expect(params.get('q')).toBe('react');
      expect(params.get('repository')).toBe('npm-proxy');
      expect(params.get('group')).toBe('@angular');
      expect(params.get('npm.author')).toBe('angular');
    });

    it('should build correct params for Composer search', () => {
      const params = buildQueryParams(
        'composer',
        'symfony',
        {
          repository: 'composer-proxy',
          vendor: 'symfony',
          package: 'console',
        }
      );

      expect(params.get('format')).toBe('composer');
      expect(params.get('q')).toBe('symfony');
      expect(params.get('repository')).toBe('composer-proxy');
      expect(params.get('composer.vendor')).toBe('symfony');
      expect(params.get('composer.package')).toBe('console');
    });

    it('should build correct params for Conan search', () => {
      const params = buildQueryParams(
        'conan',
        'boost',
        {
          repository: 'conan-proxy',
          channel: 'stable',
          baseVersion: '1.82.0',
        }
      );

      expect(params.get('format')).toBe('conan');
      expect(params.get('q')).toBe('boost');
      expect(params.get('repository')).toBe('conan-proxy');
      expect(params.get('conan.channel')).toBe('stable');
      expect(params.get('conan.baseVersion')).toBe('1.82.0');
    });

    it('should build correct params for PyPI search', () => {
      const params = buildQueryParams(
        'pypi',
        'flask',
        {
          repository: 'pypi-proxy',
          keywords: 'web framework',
        }
      );

      expect(params.get('format')).toBe('pypi');
      expect(params.get('q')).toBe('flask');
      expect(params.get('repository')).toBe('pypi-proxy');
      expect(params.get('pypi.keywords')).toBe('web framework');
    });

    it('should build correct params for NuGet search', () => {
      const params = buildQueryParams(
        'nuget',
        'json',
        {
          repository: 'nuget-proxy',
          nugetId: 'Newtonsoft.Json',
          tags: 'json serialization',
        }
      );

      expect(params.get('format')).toBe('nuget');
      expect(params.get('q')).toBe('json');
      expect(params.get('repository')).toBe('nuget-proxy');
      expect(params.get('nuget.id')).toBe('Newtonsoft.Json');
      expect(params.get('nuget.tags')).toBe('json serialization');
    });

    it('should build correct params for RubyGems search', () => {
      const params = buildQueryParams(
        'rubygems',
        'rails',
        {
          repository: 'rubygems-proxy',
          platform: 'ruby',
        }
      );

      expect(params.get('format')).toBe('rubygems');
      expect(params.get('q')).toBe('rails');
      expect(params.get('repository')).toBe('rubygems-proxy');
      expect(params.get('rubygems.platform')).toBe('ruby');
    });

    it('should build correct params for Yum search', () => {
      const params = buildQueryParams(
        'yum',
        'httpd',
        {
          repository: 'yum-proxy',
          yumName: 'httpd',
          architecture: 'x86_64',
        }
      );

      expect(params.get('format')).toBe('yum');
      expect(params.get('q')).toBe('httpd');
      expect(params.get('repository')).toBe('yum-proxy');
      expect(params.get('yum.name')).toBe('httpd');
      expect(params.get('yum.architecture')).toBe('x86_64');
    });

    it('should build correct params for P2 search', () => {
      const params = buildQueryParams(
        'p2',
        'eclipse',
        {
          repository: 'p2-proxy',
          pluginName: 'org.eclipse.core',
        }
      );

      expect(params.get('format')).toBe('p2');
      expect(params.get('q')).toBe('eclipse');
      expect(params.get('repository')).toBe('p2-proxy');
      expect(params.get('p2.pluginName')).toBe('org.eclipse.core');
    });

    it('should build correct params for Git LFS search', () => {
      const params = buildQueryParams(
        'gitlfs',
        '',
        {
          repository: 'gitlfs-proxy',
          sha256: 'abc123def456',
        }
      );

      expect(params.get('format')).toBe('gitlfs');
      expect(params.get('repository')).toBe('gitlfs-proxy');
      expect(params.get('sha256')).toBe('abc123def456');
    });

    it('should handle "all" format correctly', () => {
      const params = buildQueryParams(
        'all',
        'test',
        {
          repository: 'test-repo',
        }
      );

      // 'all' format should not include format parameter
      expect(params.get('format')).toBeNull();
      expect(params.get('q')).toBe('test');
      expect(params.get('repository')).toBe('test-repo');
    });

    it('should handle version-like values in nameOrVersion filter', () => {
      const params = buildQueryParams(
        'maven',
        '',
        {
          nameOrVersion: '1.2.3',
        }
      );

      expect(params.get('version')).toBe('1.2.3');
    });

    it('should handle name-like values in nameOrVersion filter', () => {
      const params = buildQueryParams(
        'maven',
        'spring',
        {
          nameOrVersion: 'boot',
        }
      );

      expect(params.get('q')).toBe('spring boot');
    });
  });
});

describe('Unified Search - Filter Validation', () => {
  describe('All filters have required properties', () => {
    it.each(ALL_FORMATS)('should validate filters for %s', (format) => {
      const filters = getFiltersForFormat(format);

      filters.forEach((filter) => {
        expect(filter.id).toBeTruthy();
        expect(filter.label).toBeTruthy();
        expect(filter.type).toBeTruthy();
        expect(['text', 'select']).toContain(filter.type);
        expect(filter.apiParam).toBeTruthy();

        if (filter.type === 'text' && !filter.placeholder) {
          // Text filters should have placeholders
          console.warn(`Format ${format}, filter ${filter.id} missing placeholder`);
        }
      });
    });
  });

  describe('No duplicate filter IDs per format', () => {
    it.each(ALL_FORMATS)('should have unique filter IDs for %s', (format) => {
      const filters = getFiltersForFormat(format);
      const ids = filters.map((f) => f.id);
      const uniqueIds = new Set(ids);

      expect(ids.length).toBe(uniqueIds.size);
    });
  });

  describe('All API parameters are valid', () => {
    it.each(ALL_FORMATS)('should have valid API params for %s', (format) => {
      const filters = getFiltersForFormat(format);

      filters.forEach((filter) => {
        // API params should not be empty
        expect(filter.apiParam.length).toBeGreaterThan(0);
        // API params should not contain spaces
        expect(filter.apiParam).not.toContain(' ');
      });
    });
  });
});
