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
 * Unified Search Filter Definitions
 *
 * This file defines format-specific search filters that appear in the left sidebar.
 *
 * ARCHITECTURE:
 * - Global filters (name, version, repository) are handled by the search UI above results
 * - This file only defines FORMAT-SPECIFIC filters that are unique to each format
 * - API parameters match backend SearchMapping definitions (e.g., conan.channel, maven.groupId)
 *
 * See backend SearchMappings for authoritative list:
 * - DefaultSearchMappings.java (global)
 * - MavenSearchMappings.java, NpmSearchMappings.java, etc. (format-specific)
 */

import type { SearchFormat, FormatInfo, FilterDefinition, FormatFilterConfig } from './unified.types';

// =============================================================================
// GLOBAL FILTER (Repository - shown in sidebar for all formats)
// =============================================================================

const REPOSITORY_FILTER: FilterDefinition = {
  id: 'repository',
  label: 'Repository',
  type: 'select',
  apiParam: 'repository',
  global: true,
  placeholder: 'All repositories',
};

// =============================================================================
// FORMAT DEFINITIONS
// =============================================================================

export const FORMATS: Record<SearchFormat, FormatInfo> = {
  all: {
    id: 'all',
    label: 'All Formats',
    apiFormat: '',
    placeholder: 'Search by component name or ID',
  },
  alpine: {
    id: 'alpine',
    label: 'Alpine',
    apiFormat: 'alpine',
    placeholder: 'Search by package name',
  },
  ansiblegalaxy: {
    id: 'ansiblegalaxy',
    label: 'Ansible Galaxy',
    apiFormat: 'ansiblegalaxy',
    placeholder: 'Search by namespace or collection name',
  },
  apt: {
    id: 'apt',
    label: 'Apt',
    apiFormat: 'apt',
    placeholder: 'Search by package name',
  },
  cargo: {
    id: 'cargo',
    label: 'Cargo',
    apiFormat: 'cargo',
    placeholder: 'Search by crate name',
  },
  cocoapods: {
    id: 'cocoapods',
    label: 'CocoaPods',
    apiFormat: 'cocoapods',
    placeholder: 'Search by pod name',
  },
  composer: {
    id: 'composer',
    label: 'Composer',
    apiFormat: 'composer',
    placeholder: 'Search by vendor or package',
  },
  conan: {
    id: 'conan',
    label: 'Conan',
    apiFormat: 'conan',
    placeholder: 'Search by package name',
  },
  conda: {
    id: 'conda',
    label: 'Conda',
    apiFormat: 'conda',
    placeholder: 'Search by package name',
  },
  docker: {
    id: 'docker',
    label: 'Docker',
    apiFormat: 'docker',
    placeholder: 'Search by image name or tag',
  },
  gitlfs: {
    id: 'gitlfs',
    label: 'Git LFS',
    apiFormat: 'gitlfs',
    placeholder: 'Search by hash or object ID',
  },
  go: {
    id: 'go',
    label: 'Go',
    apiFormat: 'go',
    placeholder: 'Search by module name',
  },
  helm: {
    id: 'helm',
    label: 'Helm',
    apiFormat: 'helm',
    placeholder: 'Search by chart name',
  },
  huggingface: {
    id: 'huggingface',
    label: 'Hugging Face',
    apiFormat: 'huggingface',
    placeholder: 'Search by model name',
  },
  maven: {
    id: 'maven',
    label: 'Maven',
    apiFormat: 'maven2',
    placeholder: 'Search by group ID or artifact ID',
  },
  npm: {
    id: 'npm',
    label: 'npm',
    apiFormat: 'npm',
    placeholder: 'Search by name or scope',
  },
  nuget: {
    id: 'nuget',
    label: 'NuGet',
    apiFormat: 'nuget',
    placeholder: 'Search by ID or tags',
  },
  p2: {
    id: 'p2',
    label: 'P2',
    apiFormat: 'p2',
    placeholder: 'Search by plugin name',
  },
  pypi: {
    id: 'pypi',
    label: 'PyPI',
    apiFormat: 'pypi',
    placeholder: 'Search by package name',
  },
  pub: {
    id: 'pub',
    label: 'Pub',
    apiFormat: 'pub',
    placeholder: 'Search by package name',
  },
  r: {
    id: 'r',
    label: 'R',
    apiFormat: 'r',
    placeholder: 'Search by package name',
  },
  raw: {
    id: 'raw',
    label: 'Raw',
    apiFormat: 'raw',
    placeholder: 'Search by file name',
  },
  rubygems: {
    id: 'rubygems',
    label: 'RubyGems',
    apiFormat: 'rubygems',
    placeholder: 'Search by gem name',
  },
  swift: {
    id: 'swift',
    label: 'Swift',
    apiFormat: 'swift',
    placeholder: 'Search by package name',
  },
  terraform: {
    id: 'terraform',
    label: 'Terraform',
    apiFormat: 'terraform',
    placeholder: 'Search by module name',
  },
  yum: {
    id: 'yum',
    label: 'Yum (RPM)',
    apiFormat: 'yum',
    placeholder: 'Search by package name',
  },
};

// =============================================================================
// FORMAT-SPECIFIC FILTERS
//
// Only include filters that are UNIQUE to each format.
// Global filters (name, version) are handled by the UI above search results.
// =============================================================================

export const FORMAT_FILTERS: Record<SearchFormat, FormatFilterConfig> = {
  // All Formats - repository only (global name/version handled above results)
  all: {
    format: FORMATS.all,
    filters: [REPOSITORY_FILTER],
  },
  // Alpine - no custom filters (uses global name/version)
  alpine: {
    format: FORMATS.alpine,
    filters: [REPOSITORY_FILTER],
  },
  // Ansible Galaxy - namespace and collection name filters
  ansiblegalaxy: {
    format: FORMATS.ansiblegalaxy,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'namespace',
        label: 'Namespace',
        type: 'text',
        apiParam: 'ansible-galaxy.namespace',
        placeholder: 'e.g., community',
      },
      {
        id: 'name',
        label: 'Collection Name',
        type: 'text',
        apiParam: 'ansible-galaxy.name',
        placeholder: 'Collection name',
      },
    ],
  },

  // Apt - no custom filters (uses global name/version)
  apt: {
    format: FORMATS.apt,
    filters: [REPOSITORY_FILTER],
  },

  // Cargo - no custom filters (uses global name/version)
  cargo: {
    format: FORMATS.cargo,
    filters: [REPOSITORY_FILTER],
  },

  // CocoaPods - no custom filters (uses global name/version)
  cocoapods: {
    format: FORMATS.cocoapods,
    filters: [REPOSITORY_FILTER],
  },

  // Composer (PHP) - has vendor/package instead of name
  composer: {
    format: FORMATS.composer,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'vendor',
        label: 'Vendor',
        type: 'text',
        apiParam: 'composer.vendor',
        placeholder: 'e.g., symfony',
      },
      {
        id: 'package',
        label: 'Package',
        type: 'text',
        apiParam: 'composer.package',
        placeholder: 'e.g., console',
      },
      {
        id: 'description',
        label: 'Description',
        type: 'text',
        apiParam: 'composer.description',
        placeholder: 'Package description',
      },
      {
        id: 'keywords',
        label: 'Keywords',
        type: 'text',
        apiParam: 'composer.keywords',
        placeholder: 'Package keywords',
      },
    ],
  },

  // Conan (C/C++) - full filter set from ConanSearchMappings.java
  conan: {
    format: FORMATS.conan,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'baseVersion',
        label: 'Base Version',
        type: 'text',
        apiParam: 'conan.baseVersion',
        placeholder: 'e.g., 1.0.0',
      },
      {
        id: 'channel',
        label: 'Channel',
        type: 'text',
        apiParam: 'conan.channel',
        placeholder: 'e.g., stable',
      },
      {
        id: 'revision',
        label: 'Recipe Revision',
        type: 'text',
        apiParam: 'conan.revision',
        placeholder: 'Recipe revision hash',
      },
      {
        id: 'packageId',
        label: 'Package ID',
        type: 'text',
        apiParam: 'conan.packageId',
        placeholder: 'Package ID hash',
      },
      {
        id: 'packageRevision',
        label: 'Package Revision',
        type: 'text',
        apiParam: 'conan.packageRevision',
        placeholder: 'Package revision hash',
      },
    ],
  },

  // Conda - no custom filters (uses global name/version)
  conda: {
    format: FORMATS.conda,
    filters: [REPOSITORY_FILTER],
  },

  // Docker - uses imageName/imageTag instead of name/version
  docker: {
    format: FORMATS.docker,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'imageName',
        label: 'Image Name',
        type: 'text',
        apiParam: 'docker.imageName',
        placeholder: 'e.g., nginx',
      },
      {
        id: 'imageTag',
        label: 'Image Tag',
        type: 'text',
        apiParam: 'docker.imageTag',
        placeholder: 'e.g., latest',
      },
      {
        id: 'layerId',
        label: 'Layer ID',
        type: 'text',
        apiParam: 'docker.layerId',
        placeholder: 'SHA256 hash',
      },
      {
        id: 'contentDigest',
        label: 'Content Digest',
        type: 'text',
        apiParam: 'docker.contentDigest',
        placeholder: 'sha256:...',
      },
    ],
  },

  // Git LFS - sha256 filter
  gitlfs: {
    format: FORMATS.gitlfs,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'sha256',
        label: 'SHA-256',
        type: 'text',
        apiParam: 'sha256',
        placeholder: 'Object hash',
      },
    ],
  },

  // Go - no custom filters (uses global name/version)
  go: {
    format: FORMATS.go,
    filters: [REPOSITORY_FILTER],
  },

  // Helm - no custom filters (uses global name/version)
  helm: {
    format: FORMATS.helm,
    filters: [REPOSITORY_FILTER],
  },

  // Hugging Face - no custom filters (uses global name/version)
  huggingface: {
    format: FORMATS.huggingface,
    filters: [REPOSITORY_FILTER],
  },

  // Maven - uses groupId/artifactId instead of group/name
  maven: {
    format: FORMATS.maven,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'groupId',
        label: 'Group ID',
        type: 'text',
        apiParam: 'maven.groupId',
        placeholder: 'e.g., org.apache.commons',
      },
      {
        id: 'artifactId',
        label: 'Artifact ID',
        type: 'text',
        apiParam: 'maven.artifactId',
        placeholder: 'e.g., commons-lang3',
      },
      {
        id: 'baseVersion',
        label: 'Base Version',
        type: 'text',
        apiParam: 'maven.baseVersion',
        placeholder: 'e.g., 3.12.0',
      },
      {
        id: 'classifier',
        label: 'Classifier',
        type: 'text',
        apiParam: 'maven.classifier',
        placeholder: 'e.g., sources',
      },
      {
        id: 'extension',
        label: 'Extension',
        type: 'text',
        apiParam: 'maven.extension',
        placeholder: 'e.g., jar',
      },
    ],
  },

  // npm - full filter set from NpmSearchMappings.java
  npm: {
    format: FORMATS.npm,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'scope',
        label: 'Scope',
        type: 'text',
        apiParam: 'group',
        placeholder: 'e.g., @angular',
      },
      {
        id: 'author',
        label: 'Author',
        type: 'text',
        apiParam: 'npm.author',
        placeholder: 'Package author',
      },
      {
        id: 'description',
        label: 'Description',
        type: 'text',
        apiParam: 'npm.description',
        placeholder: 'Package description',
      },
      {
        id: 'keywords',
        label: 'Keywords',
        type: 'text',
        apiParam: 'npm.keywords',
        placeholder: 'Package keywords',
      },
      {
        id: 'license',
        label: 'License',
        type: 'text',
        apiParam: 'npm.license',
        placeholder: 'e.g., MIT',
      },
    ],
  },

  // NuGet - full filter set from NugetSearchMappings.java
  nuget: {
    format: FORMATS.nuget,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'nugetId',
        label: 'Package ID',
        type: 'text',
        apiParam: 'nuget.id',
        placeholder: 'e.g., Newtonsoft.Json',
      },
      {
        id: 'tags',
        label: 'Tags',
        type: 'text',
        apiParam: 'nuget.tags',
        placeholder: 'Package tags',
      },
      {
        id: 'title',
        label: 'Title',
        type: 'text',
        apiParam: 'nuget.title',
        placeholder: 'Package title',
      },
      {
        id: 'authors',
        label: 'Authors',
        type: 'text',
        apiParam: 'nuget.authors',
        placeholder: 'Package authors',
      },
      {
        id: 'description',
        label: 'Description',
        type: 'text',
        apiParam: 'nuget.description',
        placeholder: 'Package description',
      },
      {
        id: 'summary',
        label: 'Summary',
        type: 'text',
        apiParam: 'nuget.summary',
        placeholder: 'Package summary',
      },
    ],
  },

  // P2 (Eclipse)
  p2: {
    format: FORMATS.p2,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'pluginName',
        label: 'Plugin Name',
        type: 'text',
        apiParam: 'p2.pluginName',
        placeholder: 'Eclipse plugin name',
      },
    ],
  },

  // PyPI - full filter set from PyPiSearchMappings.java
  pypi: {
    format: FORMATS.pypi,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'classifiers',
        label: 'Classifiers',
        type: 'text',
        apiParam: 'pypi.classifiers',
        placeholder: 'Package classifiers',
      },
      {
        id: 'description',
        label: 'Description',
        type: 'text',
        apiParam: 'pypi.description',
        placeholder: 'Package description',
      },
      {
        id: 'keywords',
        label: 'Keywords',
        type: 'text',
        apiParam: 'pypi.keywords',
        placeholder: 'Package keywords',
      },
      {
        id: 'summary',
        label: 'Summary',
        type: 'text',
        apiParam: 'pypi.summary',
        placeholder: 'Package summary',
      },
    ],
  },

  // Pub (Dart) - no custom filters (uses global name/version)
  pub: {
    format: FORMATS.pub,
    filters: [REPOSITORY_FILTER],
  },

  // R - no custom filters (uses global name/version)
  r: {
    format: FORMATS.r,
    filters: [REPOSITORY_FILTER],
  },

  // Raw - no custom filters (uses global name)
  raw: {
    format: FORMATS.raw,
    filters: [REPOSITORY_FILTER],
  },

  // RubyGems - full filter set from RubygemsSearchMappings.java
  rubygems: {
    format: FORMATS.rubygems,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'description',
        label: 'Description',
        type: 'text',
        apiParam: 'rubygems.description',
        placeholder: 'Gem description',
      },
      {
        id: 'platform',
        label: 'Platform',
        type: 'text',
        apiParam: 'rubygems.platform',
        placeholder: 'e.g., ruby',
      },
      {
        id: 'summary',
        label: 'Summary',
        type: 'text',
        apiParam: 'rubygems.summary',
        placeholder: 'Gem summary',
      },
    ],
  },

  // Swift - filters from SwiftSearchMappings.java
  swift: {
    format: FORMATS.swift,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'scope',
        label: 'Scope',
        type: 'text',
        apiParam: 'swift.scope',
        placeholder: 'Package scope',
      },
    ],
  },

  // Terraform - filters from TerraformSearchMappings.java
  terraform: {
    format: FORMATS.terraform,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'provider',
        label: 'Provider',
        type: 'text',
        apiParam: 'terraform.provider',
        placeholder: 'e.g., aws',
      },
      {
        id: 'namespace',
        label: 'Namespace',
        type: 'text',
        apiParam: 'terraform.namespace',
        placeholder: 'Module namespace',
      },
    ],
  },

  // Yum (RPM) - uses yum.name instead of global name
  yum: {
    format: FORMATS.yum,
    filters: [
      REPOSITORY_FILTER,
      {
        id: 'yumName',
        label: 'Package Name',
        type: 'text',
        apiParam: 'yum.name',
        placeholder: 'RPM package name',
      },
      {
        id: 'architecture',
        label: 'Architecture',
        type: 'text',
        apiParam: 'yum.architecture',
        placeholder: 'e.g., x86_64',
      },
    ],
  },
};

// =============================================================================
// FORMAT ORDER (alphabetical, excluding 'all')
// =============================================================================

/** Format order for sidebar checkboxes - alphabetically sorted */
export const FORMAT_ORDER: SearchFormat[] = [
  'alpine', 'ansiblegalaxy', 'apt', 'cargo', 'cocoapods', 'composer', 'conan', 'conda', 'docker', 'gitlfs',
  'go', 'helm', 'huggingface', 'maven', 'npm', 'nuget', 'p2', 'pub', 'pypi', 'r',
  'raw', 'rubygems', 'swift', 'terraform', 'yum',
];

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Get all formats as an array for dropdowns.
 */
export function getFormatOptions(): FormatInfo[] {
  return Object.values(FORMATS);
}

/**
 * Get filters for a specific format.
 */
export function getFiltersForFormat(format: SearchFormat): FilterDefinition[] {
  return [...FORMAT_FILTERS[format].filters];
}

/**
 * Get placeholder text for a format.
 */
export function getPlaceholderForFormat(format: SearchFormat): string {
  return FORMATS[format].placeholder;
}

/**
 * Get API format value for a format.
 */
export function getApiFormat(format: SearchFormat): string {
  return FORMATS[format].apiFormat;
}

/**
 * Build API query parameters from filter values.
 *
 * @param format - Current format (single) from machine state
 * @param query - Search query string
 * @param filters - Filter values
 */
export function buildQueryParams(
  format: SearchFormat,
  query: string,
  filters: Record<string, string>,
): URLSearchParams {
  const params = new URLSearchParams();

  // Add format filter. When format is 'all', omit (getApiFormat returns '').
  const apiFormat = getApiFormat(format);
  if (apiFormat) {
    params.set('format', apiFormat);
  }

  // Add main search query (from top nav)
  if (query.trim()) {
    params.set('q', query.trim());
  }

  // Handle combined "nameOrVersion" filter (from UI above results)
  // Smart detection: if input looks like a version, use the 'version' API param
  // Otherwise use 'q' for general name search
  const nameOrVersion = filters.nameOrVersion;
  if (nameOrVersion?.trim()) {
    const value = nameOrVersion.trim();
    // Detect version-like patterns: starts with digit, or 'v' followed by digit
    const isVersionLike = /^v?\d/.test(value);

    if (isVersionLike) {
      // Send to version parameter for proper version filtering
      params.set('version', value);
    } else {
      // Use q parameter for general name search
      const existingQ = params.get('q');
      if (existingQ) {
        params.set('q', `${existingQ} ${value}`);
      } else {
        params.set('q', value);
      }
    }
  }

  // Handle repository filter
  const repository = filters.repository;
  if (repository?.trim()) {
    params.set('repository', repository.trim());
  }

  // Handle generic 'name' filter (universal API parameter across all formats)
  const name = filters.name;
  if (name?.trim()) {
    params.set('name', name.trim());
  }

  // Handle generic 'version' filter (universal API parameter across all formats)
  // Only add if not already set by nameOrVersion handling
  const version = filters.version;
  if (version?.trim() && !params.has('version')) {
    params.set('version', version.trim());
  }

  // Add other filter values from format-specific filters
  const filterDefs = getFiltersForFormat(format);
  for (const filter of filterDefs) {
    // Skip nameOrVersion and repository as they're handled above
    if (filter.id === 'nameOrVersion' || filter.id === 'repository') {
      continue;
    }
    const value = filters[filter.id];
    if (value?.trim()) {
      params.set(filter.apiParam, value.trim());
    }
  }

  return params;
}
