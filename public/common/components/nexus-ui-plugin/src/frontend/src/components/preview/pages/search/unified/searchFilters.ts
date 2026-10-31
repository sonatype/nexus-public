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

import type { SearchFormat, FormatInfo, FilterDefinition, FormatFilterConfig, GenericAliasLabels } from './unified.types';
import { FILTER_FIELD_OPTIONS } from '../custom/custom.types';

/**
 * Read the placeholder text for a shared filter field from the custom-query
 * builder's canonical option list.  Kept in a helper so the group/name/version
 * placeholders below stay in sync with `FILTER_FIELD_OPTIONS` and cannot drift
 * from the vocabulary the custom-query builder uses.
 */
function genericPlaceholder(field: 'group' | 'name' | 'version'): string {
  const option = FILTER_FIELD_OPTIONS.find((o) => o.value === field);
  // The custom-query builder always defines these three entries; fall back only
  // as a type-narrowing safeguard.
  return option?.placeholder ?? '';
}

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
// GENERIC GNV FILTERS
//
// These three filters are prepended to every format's filter list (after
// REPOSITORY_FILTER).  Formats that use format-specific vocabulary for the
// group / name / version concepts set `aliasLabels` on their entry so the
// sidebar can relabel these inputs without adding duplicate rows.
//
// Placeholders are read from `FILTER_FIELD_OPTIONS` (the custom-query
// builder's canonical option list) so the two search surfaces cannot drift.
// =============================================================================

const GROUP_FILTER: FilterDefinition = {
  id: 'group',
  label: 'Group',
  type: 'text',
  apiParam: 'group',
  placeholder: genericPlaceholder('group'),
};

const NAME_FILTER: FilterDefinition = {
  id: 'name',
  label: 'Name',
  type: 'text',
  apiParam: 'name',
  placeholder: genericPlaceholder('name'),
};

const VERSION_FILTER: FilterDefinition = {
  id: 'version',
  label: 'Version',
  type: 'text',
  apiParam: 'version',
  placeholder: genericPlaceholder('version'),
};

export const GENERIC_FILTERS = [GROUP_FILTER, NAME_FILTER, VERSION_FILTER] as const;

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
  oci: {
    id: 'oci',
    label: 'OCI',
    apiFormat: 'oci',
    placeholder: 'Search by image name or tag',
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
  // All Formats - repository + generic group/name/version
  all: {
    format: FORMATS.all,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Alpine - generic group/name/version only (no format-specific aliases)
  alpine: {
    format: FORMATS.alpine,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Ansible Galaxy - namespace and collection name are distinct FORMAT_FIELD_*
  // (not aliases of NAMESPACE/NAME/VERSION), so both generic and distinct rows render.
  // The distinct "Collection Name" filter uses id 'collectionName' to avoid collision
  // with the generic NAME_FILTER (id 'name').
  ansiblegalaxy: {
    format: FORMATS.ansiblegalaxy,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
      {
        id: 'namespace',
        label: 'Namespace',
        type: 'text',
        apiParam: 'ansible-galaxy.namespace',
        placeholder: 'e.g., community',
      },
      {
        id: 'collectionName',
        label: 'Collection Name',
        type: 'text',
        apiParam: 'ansible-galaxy.name',
        placeholder: 'Collection name',
      },
    ],
  },

  // Apt - generic group/name/version only
  apt: {
    format: FORMATS.apt,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Cargo - generic group/name/version only
  cargo: {
    format: FORMATS.cargo,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // CocoaPods - generic group/name/version only
  cocoapods: {
    format: FORMATS.cocoapods,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Composer (PHP) - vendor/package/version alias generic group/name/version.
  // The old vendor/package/version rows are deleted; aliasLabels relabels the
  // generic inputs in the sidebar.  Distinct fields (description, keywords) remain.
  composer: {
    format: FORMATS.composer,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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
    aliasLabels: { group: 'Vendor', name: 'Package' },
    aliasPlaceholders: { group: 'e.g., symfony', name: 'e.g., console' },
  },

  // Conan (C/C++) - generic group/name/version + distinct Conan fields
  conan: {
    format: FORMATS.conan,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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

  // Conda - generic group/name/version only
  conda: {
    format: FORMATS.conda,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Docker - imageName/imageTag alias generic name/version.
  // The old imageName/imageTag rows are deleted; aliasLabels relabels the
  // generic inputs.  Distinct fields (layerId, contentDigest) remain.
  docker: {
    format: FORMATS.docker,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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
    aliasLabels: { name: 'Image Name', version: 'Image Tag' },
    aliasPlaceholders: { name: 'e.g., nginx', version: 'e.g., latest' },
  },

  // Git LFS - generic group/name/version + sha256 distinct field
  gitlfs: {
    format: FORMATS.gitlfs,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
      {
        id: 'sha256',
        label: 'SHA-256',
        type: 'text',
        apiParam: 'sha256',
        placeholder: 'Object hash',
      },
    ],
  },

  // Go - generic group/name/version only
  go: {
    format: FORMATS.go,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Helm - generic group/name/version only
  helm: {
    format: FORMATS.helm,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Hugging Face - generic group/name/version only
  huggingface: {
    format: FORMATS.huggingface,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Maven - groupId/artifactId alias generic group/name.
  // The old groupId/artifactId rows are deleted; aliasLabels relabels the
  // generic inputs.  Distinct fields (baseVersion, classifier, extension) remain.
  maven: {
    format: FORMATS.maven,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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
    aliasLabels: { group: 'Group ID', name: 'Artifact ID' },
  },

  // npm - scope aliases generic group (NpmSearchMappings maps npm.scope → NAMESPACE,
  // same as DefaultSearchMappings group → NAMESPACE).
  // The old scope row is deleted; aliasLabels relabels the generic Group input.
  // Distinct fields (author, description, keywords, license) remain.
  npm: {
    format: FORMATS.npm,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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
    aliasLabels: { group: 'Scope' },
    aliasPlaceholders: { group: 'e.g., @angular' },
  },

  // NuGet - nuget.id is FORMAT_FIELD_1, NOT a NAME alias (ticket-confirmed).
  // Generic name/version render alongside the distinct nugetId row.
  nuget: {
    format: FORMATS.nuget,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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

  // OCI - imageName/imageTag alias generic name/version (oci.* namespace).
  // The old imageName/imageTag rows are deleted; aliasLabels relabels the
  // generic inputs.  Distinct fields (contentDigest) remain.
  oci: {
    format: FORMATS.oci,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
      {
        id: 'contentDigest',
        label: 'Content Digest',
        type: 'text',
        apiParam: 'oci.contentDigest',
        placeholder: 'sha256:...',
      },
    ],
    aliasLabels: { name: 'Image Name', version: 'Image Tag' },
    aliasPlaceholders: { name: 'e.g., nginx', version: 'e.g., latest' },
  },

  // P2 (Eclipse) - generic group/name/version + distinct pluginName
  p2: {
    format: FORMATS.p2,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
      {
        id: 'pluginName',
        label: 'Plugin Name',
        type: 'text',
        apiParam: 'p2.pluginName',
        placeholder: 'Eclipse plugin name',
      },
    ],
  },

  // PyPI - generic group/name/version + distinct PyPI fields
  pypi: {
    format: FORMATS.pypi,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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

  // Pub (Dart) - generic group/name/version only
  pub: {
    format: FORMATS.pub,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // R - generic group/name/version only
  r: {
    format: FORMATS.r,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
  },

  // Raw - raw.name aliases generic name (→NAME, lenient/fulltext via
  // RawSearchMappings).  aliasLabels relabels the generic Name input.
  raw: {
    format: FORMATS.raw,
    filters: [REPOSITORY_FILTER, ...GENERIC_FILTERS],
    aliasLabels: { name: 'Filename' },
    aliasPlaceholders: { name: 'e.g., README.md' },
  },

  // RubyGems - generic group/name/version + distinct gem fields
  rubygems: {
    format: FORMATS.rubygems,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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

  // Swift - swift.scope is FORMAT_FIELD_1 (not a NAMESPACE alias), so it renders
  // as a distinct row alongside the generic group input.
  swift: {
    format: FORMATS.swift,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
      {
        id: 'scope',
        label: 'Scope',
        type: 'text',
        apiParam: 'swift.scope',
        placeholder: 'Package scope',
      },
    ],
  },

  // Terraform - generic group/name/version + distinct provider/namespace
  terraform: {
    format: FORMATS.terraform,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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

  // Yum (RPM) - yum.name is FORMAT_FIELD_2 (not a NAME alias), so it renders as
  // a distinct row alongside the generic name input.
  yum: {
    format: FORMATS.yum,
    filters: [
      REPOSITORY_FILTER,
      ...GENERIC_FILTERS,
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
  'go', 'helm', 'huggingface', 'maven', 'npm', 'nuget', 'oci', 'p2', 'pub', 'pypi', 'r',
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
 *
 * Returns the module-level readonly array directly.  Callers only read; the
 * one caller that needs a mutable list (the sidebar's `filtersToRender`)
 * derives its own array via `.filter(...)`, which already yields a fresh one.
 * Returning the shared reference avoids per-render allocations on every
 * sidebar render / buildQueryParams / URL round-trip.
 */
export function getFiltersForFormat(format: SearchFormat): readonly FilterDefinition[] {
  return FORMAT_FILTERS[format].filters;
}

/**
 * Get alias labels for a specific format.
 * Returns the format's aliasLabels map, or undefined if the format uses
 * default generic labels (Group / Name / Version).
 */
export function getAliasLabelsForFormat(format: SearchFormat): GenericAliasLabels | undefined {
  return FORMAT_FILTERS[format].aliasLabels;
}

/**
 * Get alias placeholder overrides for a specific format.
 * Returns the format's aliasPlaceholders map, or undefined when the generic
 * placeholder text already matches the aliased label (e.g. Maven's
 * "e.g., org.apache.commons" reads correctly under "Group ID").
 */
export function getAliasPlaceholdersForFormat(
  format: SearchFormat,
): GenericAliasLabels | undefined {
  return FORMAT_FILTERS[format].aliasPlaceholders;
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

  // Add filter values from the format's filter list.
  // Group, Name, and Version are registered as GENERIC_FILTERS on every format
  // (including 'all'), so they are emitted here via the per-format loop rather
  // than through a separate special-case block.
  //
  // If the header nameOrVersion input parsed as a version, it has already set
  // `version` above.  Preserve that value against a stale sidebar `version`
  // that was set for a previous session: the header input is the more recent,
  // visible signal, and the pre-refactor code applied the same precedence via
  // a `!params.has('version')` gate on the version handling.
  const filterDefs = getFiltersForFormat(format);
  for (const filter of filterDefs) {
    // Skip nameOrVersion and repository as they're handled above
    if (filter.id === 'nameOrVersion' || filter.id === 'repository') {
      continue;
    }
    // Do not overwrite version if nameOrVersion already set it above.
    if (filter.apiParam === 'version' && params.has('version')) {
      continue;
    }
    const value = filters[filter.id];
    if (value?.trim()) {
      params.set(filter.apiParam, value.trim());
    }
  }

  return params;
}
