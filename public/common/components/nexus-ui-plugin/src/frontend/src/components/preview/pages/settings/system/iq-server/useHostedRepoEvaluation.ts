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

import { useCallback, useState } from 'react';
import { restClient } from '../../../../../../interface/api';
import { formatErrorMessage } from './iqServerUtils';

const REPOSITORY_DASHBOARD_API = '/service/rest/v1/repository-dashboard';
const REPOSITORY_DASHBOARD_FORMATS_API = '/service/rest/v1/repository-dashboard/formats';
export const EVALUATION_SETTINGS_API = '/service/rest/v1/evaluation/settings';
const EVALUATION_SETTINGS_WITH_REPOS_API = '/service/rest/v1/evaluation/settings-with-repos';
const EVALUATION_REPOSITORIES_API = '/service/rest/v1/evaluation/repositories';

import type {
  ActivityTimeFrame,
  ArtifactLatestVersions,
  PolicyEvaluationStage,
  DashboardPage,
  DashboardQuery,
  DashboardRepository,
  GlobalEvaluationSettings,
  SaveResult,
  SelectionDelta,
  SettingsWithRepos,
} from './types';
import { DEFAULT_SETTINGS } from './types';

export type {
  ActivityTimeFrame,
  ArtifactLatestVersions,
  PolicyEvaluationStage,
  MonitoringFilter,
  DashboardPage,
  DashboardQuery,
  DashboardRepository,
  GlobalEvaluationSettings,
  SaveResult,
  SelectionDelta,
  SettingsWithRepos,
} from './types';
export { DEFAULT_SETTINGS } from './types';

/** Raw backend response shape returned by /service/rest/v1/evaluation/settings */
interface BackendGlobalSettings {
  activityTimeFrame?: number;
  artifactLatestVersions?: number;
  policyEvaluationStage?: string;
  autoEnrollNewRepos?: boolean;
  monitoredRepoCount?: number;
  totalRepoCount?: number;
}

/** Raw item shape from /service/rest/v1/repository-dashboard */
interface DashboardItem {
  repositoryId?: string;
  id?: string;
  repositoryName?: string;
  name?: string;
  format?: string;
  size?: number;
  numberOfComponents?: number;
  componentCount?: number;
  isSelected?: boolean;
  isMonitored?: boolean;
  monitored?: boolean;
  hasCustomConfig?: boolean;
}

interface DashboardPagination {
  page?: number;
  pageSize?: number;
  totalItems?: number;
  totalPages?: number;
  hasNext?: boolean;
  hasPrevious?: boolean;
  selectedCount?: number;
  unselectedCount?: number;
}

interface DashboardResponse {
  items?: DashboardItem[];
  pagination?: DashboardPagination;
  totalRepositories?: number;
  numberOfMonitoredRepositories?: number;
  totalCount?: number;
  globalConfigAvailable?: boolean;
}

interface UnifiedSaveResponse {
  success?: boolean;
  message?: string;
  errorCode?: string;
}

function unifySaveResponse(data: UnifiedSaveResponse | undefined, fallback = 'OK'): SaveResult {
  // HTTP 200 with success:false is possible.
  if (data && data.success === false) {
    return { ok: false, message: data.message || data.errorCode || fallback };
  }
  return { ok: true, message: data?.message || fallback };
}

/**
 * Custom hook for the Hosted Repository Evaluation Setup page.
 *
 * Returns operations only (not data) — caller owns state. Mirrors the pattern
 * used by useIqConnectedApi. All HTTP calls go through the project's
 * restClient (CSRF, 401-redirect, no-cache on GETs).
 */
export function useHostedRepoEvaluation() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSettingsWithRepos = useCallback(async (): Promise<SettingsWithRepos> => {
    try {
      // /settings-with-repos is write-only; GET returns 405.
      const data = await restClient.get<BackendGlobalSettings | null>(EVALUATION_SETTINGS_API);
      if (!data) {
        // 204 No Content — restClient surfaces this as an empty body.
        return {
          settings: DEFAULT_SETTINGS,
          monitoredRepoIds: [],
          totalRepoCount: 0,
        };
      }
      const settings: GlobalEvaluationSettings = {
        activityTimeFrame: (data.activityTimeFrame as ActivityTimeFrame) ?? DEFAULT_SETTINGS.activityTimeFrame,
        artifactLatestVersions: (data.artifactLatestVersions as ArtifactLatestVersions) ?? DEFAULT_SETTINGS.artifactLatestVersions,
        policyEvaluationStage: (data.policyEvaluationStage as PolicyEvaluationStage) ?? DEFAULT_SETTINGS.policyEvaluationStage,
        autoEnrollNewRepos: data.autoEnrollNewRepos ?? false,
      };
      // /settings only returns the monitored repo count; callers needing IDs must page /repository-dashboard.
      return {
        settings,
        monitoredRepoIds: [],
        totalRepoCount: data.monitoredRepoCount ?? 0,
      };
    } catch {
      // Non-fatal: fall back to defaults for first-time users.
      return {
        settings: DEFAULT_SETTINGS,
        monitoredRepoIds: [],
        totalRepoCount: 0,
      };
    }
  }, []);

  const fetchGlobalConfigStatus = useCallback(async (): Promise<{ globalConfigAvailable: boolean; monitoredCount: number }> => {
    try {
      const data = await restClient.get<DashboardResponse>(REPOSITORY_DASHBOARD_API, {
        params: { page: 1, pageSize: 1 },
      });
      return {
        globalConfigAvailable: Boolean(data?.globalConfigAvailable),
        monitoredCount: data?.numberOfMonitoredRepositories ?? 0,
      };
    } catch {
      return { globalConfigAvailable: false, monitoredCount: 0 };
    }
  }, []);

  const fetchFormats = useCallback(async (): Promise<string[]> => {
    try {
      const data = await restClient.get<string[]>(REPOSITORY_DASHBOARD_FORMATS_API);
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  }, []);

  const fetchRepositories = useCallback(async (query: DashboardQuery, signal?: AbortSignal): Promise<DashboardPage> => {
    setLoading(true);
    setError(null);
    try {
      const params: Record<string, string | number> = {
        page: query.page,
        pageSize: query.pageSize,
      };
      if (query.sortBy) {
        const backendSortMap: Record<string, string> = {
          name: 'name',
          format: 'format',
          size: 'size',
          componentCount: 'numberOfComponents',
        };
        const backendSort = backendSortMap[query.sortBy];
        if (backendSort) {
          params.sortBy = backendSort;
          params.sortOrder = query.sortDir ?? 'asc';
        }
      }
      if (query.search) params.search = query.search;
      if (query.formatFilter && query.formatFilter !== 'all') params.format = query.formatFilter;
      if (query.monitoringFilter && query.monitoringFilter !== 'all') params.monitoring = query.monitoringFilter;
      const data = await restClient.get<DashboardResponse>(REPOSITORY_DASHBOARD_API, { params, signal });
      // Backend: { items: [...], totalRepositories, numberOfMonitoredRepositories }
      // Each item: { repositoryId, repositoryName, format, size, numberOfComponents, isSelected }
      const rawRows = Array.isArray(data?.items) ? data.items : [];
      const rows: DashboardRepository[] = rawRows.map(r => ({
        id: String(r.repositoryId ?? r.id ?? r.repositoryName ?? r.name),
        name: r.repositoryName ?? r.name ?? '',
        format: r.format ?? '',
        size: typeof r.size === 'number' ? r.size : null,
        componentCount: typeof r.numberOfComponents === 'number'
          ? r.numberOfComponents
          : (typeof r.componentCount === 'number' ? r.componentCount : null),
        isMonitored: Boolean(r.isSelected ?? r.isMonitored ?? r.monitored ?? false),
        hasCustomConfig: Boolean(r.hasCustomConfig ?? false),
      }));
      // pagination.totalItems reflects the filtered result set; fall back to unfiltered totals.
      const totalCount = data?.pagination?.totalItems
        ?? data?.totalRepositories
        ?? data?.totalCount
        ?? rows.length;
      return {
        rows,
        totalCount,
        monitoredCount: data?.numberOfMonitoredRepositories ?? 0,
        page: query.page,
        pageSize: query.pageSize,
        globalConfigAvailable: Boolean(data?.globalConfigAvailable),
      };
    } catch (err) {
      // Aborted requests are expected (user typed another key); leave state untouched.
      if (signal?.aborted || (err as { name?: string })?.name === 'CanceledError' || (err as { name?: string })?.name === 'AbortError') {
        return { rows: [], totalCount: 0, monitoredCount: 0, page: query.page, pageSize: query.pageSize, globalConfigAvailable: false };
      }
      setError(formatErrorMessage(err, 'Failed to load repositories'));
      return { rows: [], totalCount: 0, monitoredCount: 0, page: query.page, pageSize: query.pageSize, globalConfigAvailable: false };
    } finally {
      setLoading(false);
    }
  }, []);

  const saveSettings = useCallback(async (settings: GlobalEvaluationSettings): Promise<SaveResult> => {
    setLoading(true);
    setError(null);
    try {
      // PATCH /settings is field-only — no repositoryIds enumeration required.
      // Mirrors Classic UI's patchSettings save path and is O(1) regardless of repo count.
      const payload = {
        activityTimeFrame: settings.activityTimeFrame,
        artifactLatestVersions: settings.artifactLatestVersions,
        policyEvaluationStage: settings.policyEvaluationStage,
        autoEnrollNewRepos: settings.autoEnrollNewRepos,
      };
      const data = await restClient.patch<UnifiedSaveResponse>(EVALUATION_SETTINGS_API, payload);
      return unifySaveResponse(data, 'Settings saved');
    } catch (err) {
      const message = formatErrorMessage(err, 'Failed to save settings');
      setError(message);
      return { ok: false, message };
    } finally {
      setLoading(false);
    }
  }, []);

  const applySelectionDelta = useCallback(async (delta: SelectionDelta): Promise<SaveResult> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.patch<UnifiedSaveResponse>(EVALUATION_REPOSITORIES_API, delta);
      return unifySaveResponse(data, 'Selections updated');
    } catch (err) {
      const message = formatErrorMessage(err, 'Failed to update selections');
      setError(message);
      return { ok: false, message };
    } finally {
      setLoading(false);
    }
  }, []);

  // Atomically creates global settings + enables repos in one PUT.
  // Called on the first repo-enable when no global config row exists yet,
  // mirroring the Classic UI's first-time PUT that the wizard enforced.
  const putSettingsWithRepos = useCallback(async (
    settings: GlobalEvaluationSettings,
    repositoryIds: string[]
  ): Promise<SaveResult> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        activityTimeFrame: settings.activityTimeFrame,
        artifactLatestVersions: settings.artifactLatestVersions,
        policyEvaluationStage: settings.policyEvaluationStage,
        autoEnrollNewRepos: settings.autoEnrollNewRepos,
        repositoryIds,
      };
      const data = await restClient.put<UnifiedSaveResponse>(EVALUATION_SETTINGS_WITH_REPOS_API, payload);
      return unifySaveResponse(data, 'Settings saved');
    } catch (err) {
      const message = formatErrorMessage(err, 'Failed to save settings');
      setError(message);
      return { ok: false, message };
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchSettingsWithRepos,
    fetchGlobalConfigStatus,
    fetchFormats,
    fetchRepositories,
    saveSettings,
    applySelectionDelta,
    putSettingsWithRepos,
  };
}

export default useHostedRepoEvaluation;
