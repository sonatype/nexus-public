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
 * Cleanup policy data model
 */
export interface CleanupPolicy {
  name: string;
  format: string;
  notes: string;
  criteriaLastBlobUpdated: number | null;
  criteriaLastDownloaded: number | null;
  criteriaReleaseType: string | null;
  criteriaAssetRegex: string | null;
  retain: number | null;
  sortBy: string | null;
  inUseCount: number;
}

/**
 * Format criteria configuration
 */
export interface FormatCriteria {
  id: string;
  name: string;
  availableCriteria: string[];
}

/**
 * Repository option for preview/dry run
 */
export interface RepositoryOption {
  id: string;
  name: string;
}

/**
 * Cleanup policy form data for create/edit
 */
export interface CleanupPolicyFormData {
  name: string;
  format: string;
  notes: string;
  criteriaLastBlobUpdated: number | null;
  criteriaLastDownloaded: number | null;
  criteriaReleaseType: string | null;
  criteriaAssetRegex: string | null;
  retain: number | null;
  sortBy: string | null;
}

/**
 * Preview result item
 */
export interface PreviewComponent {
  name: string;
  group: string;
  version: string;
  repository: string;
}

/**
 * Preview result
 */
export interface PreviewResult {
  components: PreviewComponent[];
  total: number;
}

/**
 * Form validation errors
 */
export interface CleanupPolicyFormErrors {
  name?: string;
  format?: string;
  notes?: string;
  criteriaLastBlobUpdated?: string;
  criteriaLastDownloaded?: string;
  criteriaAssetRegex?: string;
  retain?: string;
  criteriaSelected?: string;
}

/**
 * Release type options
 */
export const RELEASE_TYPES = {
  RELEASES_AND_SNAPSHOT: {
    id: '',
    label: 'Releases & Pre-Releases/Snapshots',
  },
  RELEASES: {
    id: 'RELEASES',
    label: 'Releases',
  },
  PRERELEASES: {
    id: 'PRERELEASES',
    label: 'Pre-Releases/Snapshots',
  },
} as const;

/**
 * Sort by options for exclusion criteria
 */
export const SORT_BY_OPTIONS = {
  VERSION: {
    id: 'version',
    label: 'version number',
    format: 'maven2',
  },
  DATE: {
    id: 'date',
    label: 'component age',
    format: 'docker',
  },
} as const;

/**
 * Formats that support retain functionality
 */
export const RETAIN_SUPPORTED_FORMATS = ['maven2', 'docker'];

/**
 * API URLs - all paths must start with leading slash for restClient
 */
export const CLEANUP_POLICY_API = {
  BASE_URL: '/service/rest/internal/cleanup-policies',
  CRITERIA_FORMATS_URL: '/service/rest/internal/cleanup-policies/criteria/formats',
  PREVIEW_URL: '/service/rest/internal/cleanup-policies/preview/components',
  PREVIEW_CSV_URL: 'service/rest/internal/cleanup-policies/preview/components/csv', // CSV URL is used differently via ExtJS.urlOf
};

/**
 * Get URL for a single cleanup policy
 */
export const getCleanupPolicyUrl = (name: string): string =>
  `${CLEANUP_POLICY_API.BASE_URL}/${encodeURIComponent(name)}`;

/**
 * Get repositories URL for a format
 */
export const getRepositoriesUrl = (format: string): string =>
  `/service/rest/internal/ui/repositories?format=${encodeURIComponent(format)}`;

/**
 * Check if release type supports exclusion criteria
 */
export const isReleaseType = (criteriaReleaseType: string | null): boolean =>
  criteriaReleaseType === RELEASE_TYPES.RELEASES.id;

/**
 * Check if format supports retain functionality
 */
export const isRetainSupportedFormat = (format: string): boolean =>
  RETAIN_SUPPORTED_FORMATS.includes(format);

/**
 * Get default sort by for a format
 */
export const getDefaultSortBy = (format: string): string | null => {
  if (format === 'maven2') return SORT_BY_OPTIONS.VERSION.id;
  if (format === 'docker') return SORT_BY_OPTIONS.DATE.id;
  return null;
};

/**
 * Empty cleanup policy for create
 */
export const EMPTY_CLEANUP_POLICY: CleanupPolicyFormData = {
  name: '',
  format: '',
  notes: '',
  criteriaLastBlobUpdated: null,
  criteriaLastDownloaded: null,
  criteriaReleaseType: null,
  criteriaAssetRegex: null,
  retain: null,
  sortBy: null,
};

/**
 * Maximum length for notes field
 */
export const NOTES_MAX_LENGTH = 400;

/**
 * Validate number field is in valid range
 */
export const isValidCriteriaNumber = (value: number | string | null): boolean => {
  if (value === null || value === '') return false;
  const num = typeof value === 'string' ? Number(value) : value;
  return !isNaN(num) && num >= 1 && num <= 24855 && Number.isInteger(num);
};

/**
 * Check if at least one criteria is selected
 */
export const hasCriteriaSelected = (data: CleanupPolicyFormData): boolean => {
  return !!(
    data.criteriaLastBlobUpdated ||
    data.criteriaLastDownloaded ||
    data.criteriaAssetRegex ||
    data.retain
  );
};


