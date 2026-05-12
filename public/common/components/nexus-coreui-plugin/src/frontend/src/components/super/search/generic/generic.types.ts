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
 * Generic search result - works for any format.
 */
export interface GenericResult {
  /** Unique identifier */
  id: string;
  /** Format type (maven2, npm, nuget, docker, pypi, etc.) */
  format: string;
  /** Repository name */
  repository: string;
  /** Group/namespace (null for formats without groups) */
  group: string | null;
  /** Component name */
  name: string;
  /** Version */
  version: string;
  /** Display name for UI */
  displayName: string;
  /** Assets in this component */
  assets: GenericAsset[];
}

/**
 * Asset within a component.
 */
export interface GenericAsset {
  id: string;
  path: string;
  downloadUrl: string;
  checksum?: {
    sha1?: string;
    sha256?: string;
    md5?: string;
  };
  contentType?: string;
  lastModified?: string;
}

/**
 * Search filters for generic search.
 */
export interface GenericSearchFilters {
  /** Free text query */
  q?: string;
  /** Repository name filter */
  repository?: string;
  /** Format filter (optional - omit for all formats) */
  format?: string;
  /** Group/namespace filter */
  group?: string;
  /** Component name filter */
  name?: string;
  /** Version filter */
  version?: string;
}

/**
 * Search response from API.
 */
export interface GenericSearchResponse {
  items: GenericResult[];
  totalCount: number;
  continuationToken?: string;
}

/**
 * Search state for hook.
 */
export interface GenericSearchState {
  filters: GenericSearchFilters;
  sort: 'relevance' | 'lastUpdated' | 'name';
  sortDirection: 'asc' | 'desc';
  loading: boolean;
  error?: string;
  results: GenericResult[];
  totalCount: number;
  continuationToken?: string;
}

/**
 * Supported format types with display metadata.
 */
export const FORMAT_CONFIG: Record<string, { label: string; color: string; icon?: string }> = {
  maven2: { label: 'Maven', color: 'orange' },
  npm: { label: 'npm', color: 'red' },
  nuget: { label: 'NuGet', color: 'blue' },
  docker: { label: 'Docker', color: 'cyan' },
  pypi: { label: 'PyPI', color: 'yellow' },
  raw: { label: 'Raw', color: 'gray' },
  helm: { label: 'Helm', color: 'indigo' },
  go: { label: 'Go', color: 'teal' },
  rubygems: { label: 'RubyGems', color: 'ruby' },
  apt: { label: 'APT', color: 'grass' },
  yum: { label: 'Yum', color: 'bronze' },
  conda: { label: 'Conda', color: 'green' },
  conan: { label: 'Conan', color: 'sky' },
  r: { label: 'R', color: 'plum' },
  gitlfs: { label: 'Git LFS', color: 'crimson' },
  cocoapods: { label: 'CocoaPods', color: 'tomato' },
};


