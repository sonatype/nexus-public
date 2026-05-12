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

import { useState, useEffect, useCallback, useMemo } from 'react';
import { restClient, parseApiError, isNotFoundError } from '@/utils/api';

import type {
  UploadableRepository,
  RepositoryReference,
  UploadDefinition,
  SortColumn,
  SortDirection,
} from '../upload.types';

// REST API endpoints
const UPLOAD_SPECS_ENDPOINT = '/service/rest/v1/formats/upload-specs';

/**
 * Check if a repository is online.
 * Handles both REST API format (flat `online` property) and
 * ExtDirect format (nested `status.online` property).
 */
function isRepositoryOnline(repo: RepositoryReference): boolean {
  // REST API format: online is at top level
  if (typeof repo.online === 'boolean') {
    return repo.online;
  }
  // ExtDirect format: online is nested in status
  if (repo.status && typeof repo.status.online === 'boolean') {
    return repo.status.online;
  }
  // Default to true if neither is present (assume online)
  return true;
}

/**
 * Filters repositories to only include those that support UI upload.
 *
 * Criteria:
 * - Must be a hosted repository
 * - Must be online
 * - Must not be a Maven SNAPSHOT repo
 * - Must have a matching upload definition
 *
 * Note: REST API /v1/formats/upload-specs does not include `uiUpload` field,
 * so we assume all formats returned by upload-specs support UI upload.
 */
function filterUploadableRepositories(
  repositories: RepositoryReference[],
  uploadDefinitions: UploadDefinition[]
): UploadableRepository[] {
  // REST API doesn't return uiUpload field, so we assume all formats in upload-specs
  // support upload (they wouldn't be returned if they didn't).
  // For backwards compatibility, still check uiUpload if present.
  const uiUploadableFormats = new Set(
    uploadDefinitions
      .filter((def) => def.uiUpload !== false) // Accept undefined as true
      .map((def) => def.format)
  );

  return repositories
    .filter((repo) => {
      // Must be hosted
      if (repo.type !== 'hosted') return false;
      // Must be online (handles both REST and ExtDirect formats)
      if (!isRepositoryOnline(repo)) return false;
      // Must not be Maven SNAPSHOT repo
      if (repo.versionPolicy === 'SNAPSHOT') return false;
      // Must have a UI-uploadable format
      if (!uiUploadableFormats.has(repo.format)) return false;
      return true;
    })
    .map((repo) => ({
      name: repo.name,
      format: repo.format,
      url: repo.url,
    }));
}

/**
 * Sorts repositories by the specified column and direction.
 */
function sortRepositories(
  repositories: UploadableRepository[],
  column: SortColumn | null,
  direction: SortDirection
): UploadableRepository[] {
  if (!column || !direction) {
    return repositories;
  }

  return [...repositories].sort((a, b) => {
    const aVal = a[column].toLowerCase();
    const bVal = b[column].toLowerCase();
    const comparison = aVal.localeCompare(bVal);
    return direction === 'asc' ? comparison : -comparison;
  });
}

/**
 * Filters repositories by name or format matching the filter text.
 */
function filterByText(
  repositories: UploadableRepository[],
  filterText: string
): UploadableRepository[] {
  if (!filterText.trim()) {
    return repositories;
  }

  const search = filterText.toLowerCase();
  return repositories.filter(
    (repo) =>
      repo.name.toLowerCase().includes(search) ||
      repo.format.toLowerCase().includes(search)
  );
}

/**
 * Hook to fetch and manage the list of repositories that support file uploads.
 *
 * Features:
 * - Fetches repositories and upload definitions from the API
 * - Filters to only show repositories that support UI upload
 * - Provides filtering by name
 * - Provides filtering by format (checkbox multi-select)
 * - Provides sorting by name or format columns
 *
 * @returns State and actions for managing the uploadable repositories list
 */
export function useUploadableRepositories() {
  // Data state
  const [allRepositories, setAllRepositories] = useState<UploadableRepository[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter/sort state
  const [filterText, setFilterText] = useState('');
  const [selectedFormats, setSelectedFormats] = useState<string[]>([]);
  const [sortColumn, setSortColumn] = useState<SortColumn | null>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>(null);

  /**
   * Fetch repositories and upload definitions from the API.
   * Uses REST endpoints instead of ExtDirect.
   */
  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const PAGE_SIZE = 100;

      const fetchAllHosted = async (): Promise<RepositoryReference[]> => {
        try {
          let repos: RepositoryReference[] = [];
          let page = 1;
          let hasMore = true;
          while (hasMore) {
            const pageData = await restClient.get<{ data: RepositoryReference[] }>(
              `/service/rest/internal/ui/repositories/details/filtered?type=hosted&pageSize=${PAGE_SIZE}&page=${page}`
            );
            const pageRepos = (pageData as any)?.data || (Array.isArray(pageData) ? pageData : []);
            repos = repos.concat(pageRepos);
            hasMore = pageRepos.length === PAGE_SIZE;
            page++;
          }
          return repos;
        } catch (err) {
          const apiError = parseApiError(err);
          if (isNotFoundError(apiError)) {
            const repos = await restClient.get<RepositoryReference[]>('/service/rest/v1/repositories');
            return (repos || []).filter((r: RepositoryReference) => r.type === 'hosted');
          }
          throw err;
        }
      };

      const fetchUploadDefinitions = async (): Promise<{
        definitions: UploadDefinition[];
        is404: boolean;
      }> => {
        try {
          const definitions = await restClient.get<UploadDefinition[]>(UPLOAD_SPECS_ENDPOINT);
          return { definitions: definitions || [], is404: false };
        } catch (err) {
          const apiError = parseApiError(err);
          if (isNotFoundError(apiError)) {
            return { definitions: [], is404: true };
          }
          throw err;
        }
      };

      const [repositories, { definitions: uploadDefinitions, is404: uploadSpecsNotFound }] =
        await Promise.all([fetchAllHosted(), fetchUploadDefinitions()]);

      let uploadableRepos: UploadableRepository[];

      if (uploadSpecsNotFound) {
        uploadableRepos = (repositories || [])
          .filter(
            (repo) =>
              repo.type === 'hosted' &&
              isRepositoryOnline(repo) &&
              repo.versionPolicy !== 'SNAPSHOT',
          )
          .map((repo) => ({ name: repo.name, format: repo.format, url: repo.url }));
      } else {
        uploadableRepos = filterUploadableRepositories(
          repositories || [],
          uploadDefinitions,
        );
      }

      setAllRepositories(uploadableRepos);
    } catch (err) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      setAllRepositories([]);
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch data on mount
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /**
   * Handle sort column click - cycles through: asc -> desc -> none
   */
  const handleSort = useCallback((column: SortColumn) => {
    if (sortColumn !== column) {
      // New column - start with ascending
      setSortColumn(column);
      setSortDirection('asc');
    } else if (sortDirection === 'asc') {
      // Same column, currently asc - switch to desc
      setSortDirection('desc');
    } else if (sortDirection === 'desc') {
      // Same column, currently desc - clear sort
      setSortColumn(null);
      setSortDirection(null);
    } else {
      // Same column, no direction - start with asc
      setSortColumn(column);
      setSortDirection('asc');
    }
  }, [sortColumn, sortDirection]);

  /**
   * Set sort directly (for dropdown controls).
   */
  const handleSortChange = useCallback((column: SortColumn | null, direction: 'asc' | 'desc' | null) => {
    setSortColumn(column);
    setSortDirection(direction);
  }, []);

  /**
   * Handle filter text change.
   */
  const handleFilterChange = useCallback((value: string) => {
    setFilterText(value);
  }, []);

  /**
   * Clear the filter text.
   */
  const clearFilter = useCallback(() => {
    setFilterText('');
  }, []);

  /**
   * Toggle a format filter.
   */
  const toggleFormat = useCallback((format: string) => {
    setSelectedFormats((prev) =>
      prev.includes(format)
        ? prev.filter((f) => f !== format)
        : [...prev, format]
    );
  }, []);

  /**
   * Set selected formats directly (for multi-select filter controls).
   */
  const setFormats = useCallback((values: string[]) => {
    setSelectedFormats(values);
  }, []);

  /**
   * Clear all filters.
   */
  const clearAllFilters = useCallback(() => {
    setFilterText('');
    setSelectedFormats([]);
  }, []);

  /**
   * Get unique formats from all repositories.
   */
  const availableFormats = useMemo(() => {
    const formats = new Set(allRepositories.map((repo) => repo.format));
    return Array.from(formats).sort();
  }, [allRepositories]);

  /**
   * Format options with counts for filter sidebar.
   */
  const formatOptions = useMemo(() => {
    const countByFormat = new Map<string, number>();
    allRepositories.forEach((repo) => {
      countByFormat.set(repo.format, (countByFormat.get(repo.format) || 0) + 1);
    });
    return availableFormats.map((f) => ({
      value: f,
      label: f,
      count: countByFormat.get(f) || 0,
    }));
  }, [allRepositories, availableFormats]);

  /**
   * Computed filtered and sorted repositories.
   */
  const repositories = useMemo(() => {
    let result = allRepositories;

    // Filter by text
    result = filterByText(result, filterText);

    // Filter by selected formats
    if (selectedFormats.length > 0) {
      result = result.filter((repo) => selectedFormats.includes(repo.format));
    }

    // Sort
    result = sortRepositories(result, sortColumn, sortDirection);
    return result;
  }, [allRepositories, filterText, selectedFormats, sortColumn, sortDirection]);

  const hasActiveFilters = selectedFormats.length > 0;

  return {
    // State
    repositories,
    loading,
    error,
    filterText,
    selectedFormats,
    availableFormats,
    formatOptions,
    hasActiveFilters,
    sortColumn,
    sortDirection,

    // Actions
    handleSort,
    handleSortChange,
    handleFilterChange,
    clearFilter,
    toggleFormat,
    setFormats,
    clearAllFilters,
    refetch: fetchData,
  };
}

export default useUploadableRepositories;

