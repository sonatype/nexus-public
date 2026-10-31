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
import { formatErrorMessage } from '../../system/iq-server/iqServerUtils';

/**
 * Per-repository evaluation override endpoint.
 * GET/PUT /service/rest/v1/repositories/{name}/evaluation-settings
 * Endpoint uses lowercase stage values ("build", "release") which we normalize to UPPERCASE.
 */

export type EvaluationMode = 'INHERIT' | 'OVERRIDE' | 'DISABLE';

export type PolicyEvaluationStage =
  | 'BUILD'
  | 'STAGE_RELEASE'
  | 'RELEASE'
  | 'OPERATE';

export interface RepoEvaluationOverride {
  mode: EvaluationMode;
  activityTimeFrame?: number;
  artifactLatestVersions?: number;
  policyEvaluationStage?: PolicyEvaluationStage;
}

export interface SaveResult {
  ok: boolean;
  message?: string;
}

interface BackendOverrideResponse {
  mode?: string;
  activityTimeFrame?: number;
  artifactLatestVersions?: number;
  policyEvaluationStage?: string;
}

interface UnifiedSaveResponse {
  success?: boolean;
  message?: string;
  errorCode?: string;
}

const evaluationSettingsUrl = (repoName: string): string =>
  `/service/rest/v1/repositories/${encodeURIComponent(repoName)}/evaluation-settings`;

/**
 * Lowercase form used by the per-repo endpoint.
 *
 * Backend accepts "build" / "stage-release" / "release" / "operate".
 * We convert to/from these whenever we talk to this endpoint.
 */
function toLowerCaseStage(s: PolicyEvaluationStage | undefined): string | undefined {
  if (!s) return undefined;
  return s.toLowerCase().replace(/_/g, '-');
}

function toUpperCaseStage(s: string | undefined): PolicyEvaluationStage | undefined {
  if (!s) return undefined;
  const upper = s.toUpperCase().replace(/-/g, '_');
  if (upper === 'BUILD' || upper === 'STAGE_RELEASE' || upper === 'RELEASE' || upper === 'OPERATE') {
    return upper;
  }
  return undefined;
}

/**
 * Per-repo evaluation hook. API operations only; caller owns UI state
 * (mode, dirty-tracking) for the "preserve OVERRIDE values across toggles" UX.
 */
export function useRepoEvaluationOverride() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch the current per-repo evaluation override. Returns null when nothing
   * is set on the server (204) — caller should treat as default INHERIT.
   */
  const fetchOverride = useCallback(async (repoName: string): Promise<RepoEvaluationOverride | null> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.get<BackendOverrideResponse | null>(evaluationSettingsUrl(repoName));
      if (!data) return null;
      const mode: EvaluationMode = (data.mode === 'OVERRIDE' || data.mode === 'DISABLE') ? data.mode : 'INHERIT';
      return {
        mode,
        activityTimeFrame: typeof data.activityTimeFrame === 'number' ? data.activityTimeFrame : undefined,
        artifactLatestVersions: typeof data.artifactLatestVersions === 'number' ? data.artifactLatestVersions : undefined,
        policyEvaluationStage: toUpperCaseStage(data.policyEvaluationStage),
      };
    } catch (err: any) {
      // 404 = no override (INHERIT); other errors bubble.
      if (err?.response?.status === 404) return null;
      const message = formatErrorMessage(err, 'Failed to load evaluation override');
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * PUT the per-repo override. For INHERIT/DISABLE, override fields are cleared server-side.
   */
  const saveOverride = useCallback(
    async (repoName: string, payload: RepoEvaluationOverride): Promise<SaveResult> => {
      setLoading(true);
      setError(null);
      try {
        const body: Record<string, unknown> = { mode: payload.mode };
        if (payload.mode === 'OVERRIDE') {
          body.activityTimeFrame = payload.activityTimeFrame;
          body.artifactLatestVersions = payload.artifactLatestVersions;
          body.policyEvaluationStage = toLowerCaseStage(payload.policyEvaluationStage);
        }
        const data = await restClient.put<UnifiedSaveResponse>(evaluationSettingsUrl(repoName), body);
        // Backend may return 200 with success:false.
        if (data && data.success === false) {
          const message = data.message || data.errorCode || 'Failed to save override';
          setError(message);
          return { ok: false, message };
        }
        return { ok: true, message: data?.message || 'Override saved' };
      } catch (err) {
        const message = formatErrorMessage(err, 'Failed to save evaluation override');
        setError(message);
        return { ok: false, message };
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  return {
    loading,
    error,
    setError,
    fetchOverride,
    saveOverride,
  };
}

/**
 * Tab eligibility for the Evaluation tab.
 * Checks ExtJS State flag `hostedRepositoryEvaluationEnabled`.
 */
interface NxGlobal {
  State?: { getValue?: (key: string) => unknown };
}

export function isEvaluationFeatureEnabled(): boolean {
  if (typeof window === 'undefined') return false;
  const NX = (window as unknown as { NX?: NxGlobal }).NX;
  const value = NX?.State?.getValue?.('hostedRepositoryEvaluationEnabled');
  return value === true;
}

export default useRepoEvaluationOverride;
