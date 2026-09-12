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

import { useCallback } from 'react';
import { restClient } from '../../../../../../interface/api';

// No leading slash — restClient has no baseURL so both forms resolve identically.
// useHostedRepoEvaluation uses leading slashes; either convention is valid here.
const IQ_API = 'service/rest/v1/iq';
const IQ_VERIFY_API = 'service/rest/v1/iq/verify-connection';
const REPOSITORY_DASHBOARD_API = 'service/rest/v1/repository-dashboard';
const EVALUATION_SETTINGS_API = 'service/rest/v1/evaluation/settings';

import type {
  DashboardSummary,
  EvaluationSettingsSummary,
  IqConfigResponse,
  IqVerifyResult,
} from './types';

export type {
  DashboardSummary,
  EvaluationSettingsSummary,
  IqConfigResponse,
  IqVerifyResult,
  LicensedSolution,
} from './types';

/**
 * Custom hook for IQ Server "Connected" page API operations.
 *
 * Returns operations (not data) — the component owns its data state.
 */
export function useIqConnectedApi() {
  const fetchIq = useCallback(async (): Promise<IqConfigResponse> => {
    try {
      return await restClient.get<IqConfigResponse>(IQ_API);
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Failed to load IQ Server configuration';
      throw new Error(message);
    }
  }, []);

  /**
   * Verify connection to the configured IQ Server.
   * Returns a result object even on failure (does not throw).
   */
  const verifyConnection = useCallback(async (): Promise<IqVerifyResult> => {
    try {
      const data = await restClient.post<IqVerifyResult | null>(IQ_VERIFY_API);
      // Require explicit success===true; null/empty on HTTP 200 must not default to success.
      return {
        success: data?.success === true,
        reason: data?.reason,
        applicationCount: data?.applicationCount,
      };
    } catch (err: any) {
      const reason = err?.response?.data?.reason || err?.response?.data?.message || err?.message || 'IQ Server is not connected';
      return {
        success: false,
        reason: typeof reason === 'string' ? reason : 'IQ Server is not connected',
      };
    }
  }, []);

  /** Fetch repository-dashboard summary counts. */
  const fetchDashboardSummary = useCallback(async (): Promise<DashboardSummary> => {
    try {
      const data = await restClient.get<Partial<DashboardSummary> | null>(REPOSITORY_DASHBOARD_API, {
        params: { page: 1, pageSize: 1 },
      });
      return {
        numberOfMonitoredRepositories: data?.numberOfMonitoredRepositories ?? 0,
        totalRepositories: data?.totalRepositories ?? 0,
        globalConfigAvailable: data?.globalConfigAvailable ?? false,
        hasSelections: data?.hasSelections ?? false,
      };
    } catch (err: any) {
      // Non-fatal — if the dashboard endpoint is unavailable (e.g., feature flag
      // is off mid-session), fall back to "empty" state.
      return {
        numberOfMonitoredRepositories: 0,
        totalRepositories: 0,
        globalConfigAvailable: false,
        hasSelections: false,
      };
    }
  }, []);

  /** Fetch global evaluation settings for summary tile. Returns null if not configured. */
  const fetchEvaluationSettings = useCallback(async (): Promise<EvaluationSettingsSummary | null> => {
    try {
      // restClient surfaces 204 No Content as an empty body — falsy here.
      const data = await restClient.get<Partial<EvaluationSettingsSummary> | null>(EVALUATION_SETTINGS_API);
      if (!data) {
        return null;
      }
      return {
        activityTimeFrame: data.activityTimeFrame ?? 30,
        artifactLatestVersions: data.artifactLatestVersions ?? 1,
        policyEvaluationStage: data.policyEvaluationStage ?? 'RELEASE',
        monitoredRepoCount: data.monitoredRepoCount ?? 0,
        totalRepoCount: data.totalRepoCount ?? 0,
      };
    } catch (err: any) {
      // Treat any error as "no settings yet" — caller decides what to render.
      return null;
    }
  }, []);

  return {
    fetchIq,
    verifyConnection,
    fetchDashboardSummary,
    fetchEvaluationSettings,
  };
}

export default useIqConnectedApi;
