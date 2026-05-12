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

import { useState, useCallback, useEffect, useMemo } from 'react';
import { ExtJS, APIConstants, ExtAPIUtils } from '@sonatype/nexus-ui-plugin';
import { restClient, ENDPOINTS, parseApiError } from '@/utils/api';
import type {
  Repository,
  RepositoryListState,
  SortableField,
  SortDirection,
  SortConfig,
  HealthCheckStatus,
  FirewallStatus,
} from './repository-list.types';

const { EXT } = APIConstants;

/**
 * Check if IQ Server is enabled.
 */
export function isIqServerEnabled(): boolean {
  try {
    return ExtJS.state().getValue('clm')?.enabled ?? false;
  } catch {
    return false;
  }
}

/**
 * Check if user can update health check.
 */
export function canUpdateHealthCheck(): boolean {
  try {
    return ExtJS.checkPermission('nexus:healthcheck:update');
  } catch {
    return false;
  }
}

/**
 * Check if user can read firewall status.
 */
export function canReadFirewallStatus(): boolean {
  try {
    return ExtJS.checkPermission('nexus:iq-violation-summary:read');
  } catch {
    return false;
  }
}

/**
 * Initial sort configuration.
 */
const initialSort: SortConfig = {
  field: 'name',
  direction: 'asc',
};

/**
 * Options for the useRepositoryList hook.
 */
export interface UseRepositoryListOptions {
  /** Filter repositories by format (e.g., 'maven2', 'npm') */
  formatFilter?: string;
}

/**
 * Initial state for the repository list.
 */
const initialState: RepositoryListState = {
  repositories: [],
  filteredRepositories: [],
  filterText: '',
  sort: initialSort,
  loading: true,
  error: undefined,
  healthCheck: {},
  firewallStatus: {},
  firewallLoaded: false,
  healthCheckError: undefined,
  firewallStatusError: undefined,
};

/**
 * Compare function for sorting repositories.
 */
function compareRepositories(
  a: Repository,
  b: Repository,
  field: SortableField,
  direction: SortDirection
): number {
  if (!direction) return 0;

  let aValue: string;
  let bValue: string;

  if (field === 'status') {
    // Sort by online status, then description
    aValue = `${a.status.online ? '0' : '1'}${a.status.description ?? ''}`;
    bValue = `${b.status.online ? '0' : '1'}${b.status.description ?? ''}`;
  } else {
    aValue = String(a[field]).toLowerCase();
    bValue = String(b[field]).toLowerCase();
  }

  const comparison = aValue.localeCompare(bValue);
  return direction === 'asc' ? comparison : -comparison;
}

/**
 * Filter repositories by name and optionally by format.
 */
function filterRepositories(
  repositories: readonly Repository[],
  filterText: string,
  formatFilter?: string
): Repository[] {
  let filtered = [...repositories];

  // Filter by format first
  if (formatFilter?.trim()) {
    const format = formatFilter.toLowerCase();
    filtered = filtered.filter(repo =>
      repo.format.toLowerCase() === format
    );
  }

  // Then filter by name
  if (filterText.trim()) {
    const searchTerm = filterText.toLowerCase();
    filtered = filtered.filter(repo =>
      repo.name.toLowerCase().includes(searchTerm)
    );
  }

  return filtered;
}

/**
 * Sort repositories.
 */
function sortRepositories(
  repositories: Repository[],
  sort: SortConfig
): Repository[] {
  if (!sort.direction) {
    return repositories;
  }
  return [...repositories].sort((a, b) =>
    compareRepositories(a, b, sort.field, sort.direction)
  );
}

/**
 * Custom hook for managing repository list state.
 *
 * Handles:
 * - Fetching repositories from API
 * - Filtering by name and format
 * - Sorting by columns
 * - Loading health check and firewall status
 */
export function useRepositoryList(options: UseRepositoryListOptions = {}) {
  const { formatFilter } = options;
  const [state, setState] = useState<RepositoryListState>(initialState);

  /**
   * Fetch repositories from the API.
   * Uses REST API via restClient.
   */
  const fetchRepositories = useCallback(async () => {
    setState(prev => ({ ...prev, loading: true, error: undefined }));

    try {
      const raw = await restClient.get<(Repository & { attributes?: Record<string, Record<string, unknown>> })[]>(ENDPOINTS.REPOSITORIES_DETAILS);
      const repositories: Repository[] = (raw || []).map((r) => ({
        ...r,
        versionPolicy: r.format === 'maven2'
          ? (r.attributes?.maven?.versionPolicy as Repository['versionPolicy']) ?? undefined
          : undefined,
      }));

      setState(prev => {
        const filtered = filterRepositories(repositories, prev.filterText, formatFilter);
        const sorted = sortRepositories(filtered, prev.sort);
        return {
          ...prev,
          repositories,
          filteredRepositories: sorted,
          loading: false,
          error: undefined,
        };
      });
    } catch (err) {
      const apiError = parseApiError(err);
      setState(prev => ({
        ...prev,
        loading: false,
        error: apiError.message,
      }));
    }
  }, [formatFilter]);

  /**
   * Fetch health check status from IQ Server.
   */
  const fetchHealthCheck = useCallback(async () => {
    if (!canUpdateHealthCheck()) {
      return;
    }

    try {
      const data = await restClient.get<Array<HealthCheckStatus & { repositoryName: string; detailUrl?: string; summaryUrl?: string }>>(ENDPOINTS.HEALTH_CHECK);

      const healthCheckMap = (data || []).reduce<Record<string, HealthCheckStatus>>(
        (acc, item) => {
          acc[item.repositoryName] = {
            ...item,
            detailedReport: item.detailedReport || item.detailUrl || item.summaryUrl || undefined,
          };
          return acc;
        },
        {}
      );

      setState(prev => ({
        ...prev,
        healthCheck: healthCheckMap,
        healthCheckError: undefined,
      }));
    } catch (err) {
      const apiError = parseApiError(err);
      setState(prev => ({
        ...prev,
        healthCheckError: apiError.message,
      }));
    }
  }, []);

  /**
   * Phase 1: Fetch lightweight firewall status (labels only, no IQ Server calls).
   * Returns Audit/Quarantine/inactive status from local capability data -- essentially instant.
   */
  const fetchFirewallSummary = useCallback(async () => {
    if (!isIqServerEnabled() || !canReadFirewallStatus()) {
      setState(prev => ({ ...prev, firewallLoaded: true }));
      return;
    }

    try {
      const data = await restClient.get<FirewallStatus[]>(ENDPOINTS.FIREWALL_STATUS_SUMMARY);

      const firewallStatusMap = (data || []).reduce<Record<string, FirewallStatus>>(
        (acc, item) => {
          acc[item.repositoryName] = item;
          return acc;
        },
        {}
      );

      setState(prev => ({
        ...prev,
        firewallStatus: firewallStatusMap,
        firewallLoaded: true,
        firewallStatusError: undefined,
      }));
    } catch (err) {
      const apiError = parseApiError(err);
      setState(prev => ({
        ...prev,
        firewallLoaded: true,
        firewallStatusError: apiError.message,
      }));
    }
  }, []);

  /**
   * Load all data on mount.
   * Fetches repos + health check + firewall summary (all fast, in parallel).
   * Firewall summary uses the lightweight /status/summary endpoint (no IQ Server calls).
   * Full violation counts are fetched on-demand when the user clicks a cell to open the modal.
   */
  useEffect(() => {
    Promise.all([
      fetchRepositories(),
      fetchHealthCheck(),
      fetchFirewallSummary(),
    ]);
  }, [fetchRepositories, fetchHealthCheck, fetchFirewallSummary]);

  /**
   * Set filter text.
   */
  const setFilter = useCallback((filterText: string) => {
    setState(prev => {
      const filtered = filterRepositories(prev.repositories, filterText, formatFilter);
      const sorted = sortRepositories(filtered, prev.sort);
      return {
        ...prev,
        filterText,
        filteredRepositories: sorted,
      };
    });
  }, [formatFilter]);

  /**
   * Clear filter.
   */
  const clearFilter = useCallback(() => {
    setFilter('');
  }, [setFilter]);

  /**
   * Toggle sort direction for a field.
   */
  const toggleSort = useCallback((field: SortableField) => {
    setState(prev => {
      let newDirection: SortDirection;

      if (prev.sort.field === field) {
        // Cycle through: asc -> desc -> null
        if (prev.sort.direction === 'asc') {
          newDirection = 'desc';
        } else if (prev.sort.direction === 'desc') {
          newDirection = null;
        } else {
          newDirection = 'asc';
        }
      } else {
        // New field, start with asc
        newDirection = 'asc';
      }

      const newSort: SortConfig = {
        field,
        direction: newDirection,
      };

      const filtered = filterRepositories(prev.repositories, prev.filterText, formatFilter);
      const sorted = sortRepositories(filtered, newSort);

      return {
        ...prev,
        sort: newSort,
        filteredRepositories: sorted,
      };
    });
  }, [formatFilter]);

  /**
   * Get sort direction for a specific field.
   */
  const getSortDirection = useCallback(
    (field: SortableField): SortDirection => {
      return state.sort.field === field ? state.sort.direction : null;
    },
    [state.sort]
  );

  /**
   * Refresh data.
   */
  const refresh = useCallback(async () => {
    await Promise.all([fetchRepositories(), fetchHealthCheck(), fetchFirewallSummary()]);
  }, [fetchRepositories, fetchHealthCheck, fetchFirewallSummary]);

  /**
   * Enable health check for a single repository.
   */
  const enableHealthCheck = useCallback(async (repositoryName: string) => {
    try {
      await restClient.post(ENDPOINTS.HEALTH_CHECK_ANALYZE(repositoryName));
      // Refresh health check status
      await fetchHealthCheck();
    } catch (err) {
      const apiError = parseApiError(err);
      setState(prev => ({
        ...prev,
        healthCheckError: apiError.message,
      }));
    }
  }, [fetchHealthCheck]);

  // Computed values
  const showHealthCheckColumn = useMemo(() => canUpdateHealthCheck(), []);
  const showIqPolicyViolationsColumn = useMemo(
    () => isIqServerEnabled(),
    []
  );

  return {
    // State
    state,
    // Actions
    setFilter,
    clearFilter,
    toggleSort,
    getSortDirection,
    refresh,
    enableHealthCheck,
    // Computed
    showHealthCheckColumn,
    showIqPolicyViolationsColumn,
  };
}

export default useRepositoryList;

