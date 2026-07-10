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

import { useState, useCallback, useRef, useEffect } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';
import { ExtJS } from '../../../../../../interface/ExtJS';
import {
  CleanupPolicy,
  CleanupPolicyFormData,
  FormatCriteria,
  RepositoryOption,
  PreviewComponent,
  CLEANUP_POLICY_API,
  getCleanupPolicyUrl,
  getRepositoriesUrl,
  isRepositoriesFieldSupportedFormat,
} from './types';

/**
 * Build the create/update request payload. The {@code repositories} field is
 * intentionally omitted when the CLEANUP_RETAIN_ALL_FORMATS feature flag is
 * disabled, when the format does not expose the attachment field, or when the
 * list is empty: the server treats these cases identically as "no attachment
 * change", and sending a non-null list against an unsupported format /
 * disabled feature flag would trip the server-side validator and reject the
 * request.
 */
function buildPolicyPayload(
  formData: CleanupPolicyFormData,
  retainAllFormatsEnabled: boolean
): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    name: formData.name,
    notes: formData.notes,
    format: formData.format,
    criteriaLastBlobUpdated: formData.criteriaLastBlobUpdated,
    criteriaLastDownloaded: formData.criteriaLastDownloaded,
    criteriaReleaseType: formData.criteriaReleaseType || null,
    criteriaAssetRegex: formData.criteriaAssetRegex,
    retain: formData.retain,
    sortBy: formData.sortBy,
  };
  if (
    retainAllFormatsEnabled &&
    Array.isArray(formData.repositories) &&
    formData.repositories.length > 0 &&
    isRepositoriesFieldSupportedFormat(formData.format)
  ) {
    payload.repositories = formData.repositories;
  }
  return payload;
}

/**
 * Custom hook for Cleanup Policies API operations
 */
export function useCleanupPoliciesApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  /**
   * Fetch all cleanup policies
   */
  const fetchCleanupPolicies = useCallback(async (): Promise<CleanupPolicy[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<CleanupPolicy[]>(CLEANUP_POLICY_API.BASE_URL);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch cleanup policies:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch a single cleanup policy by name
   */
  const fetchCleanupPolicy = useCallback(async (name: string): Promise<CleanupPolicy | null> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<CleanupPolicy>(getCleanupPolicyUrl(name));
      return data || null;
    } catch (err: unknown) {
      console.error('Failed to fetch cleanup policy:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch available format criteria
   */
  const fetchFormatCriteria = useCallback(async (): Promise<FormatCriteria[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<FormatCriteria[]>(CLEANUP_POLICY_API.CRITERIA_FORMATS_URL);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch format criteria:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch repositories for a format
   */
  const fetchRepositories = useCallback(async (format: string): Promise<RepositoryOption[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<RepositoryOption[]>(getRepositoriesUrl(format));
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch repositories:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new cleanup policy
   */
  const createCleanupPolicy = useCallback(async (formData: CleanupPolicyFormData): Promise<CleanupPolicy> => {
    setLoading(true);
    setError(null);
    try {
      const payload = buildPolicyPayload(formData, isRetainAllFormatsEnabled());
      // restClient.post() returns data directly, not Axios response
      const result = await restClient.post<CleanupPolicy>(CLEANUP_POLICY_API.BASE_URL, payload);
      return result;
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing cleanup policy
   */
  const updateCleanupPolicy = useCallback(async (
    name: string,
    formData: CleanupPolicyFormData
  ): Promise<CleanupPolicy> => {
    setLoading(true);
    setError(null);
    try {
      const payload = buildPolicyPayload(formData, isRetainAllFormatsEnabled());
      // restClient.put() returns data directly, not Axios response
      const result = await restClient.put<CleanupPolicy>(getCleanupPolicyUrl(name), payload);
      return result;
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a cleanup policy
   */
  const deleteCleanupPolicy = useCallback(async (name: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(getCleanupPolicyUrl(name));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Preview cleanup policy results
   */
  const previewCleanupPolicy = useCallback(async (
    repository: string,
    policyData: CleanupPolicyFormData,
    policyName?: string
  ): Promise<{ components: PreviewComponent[]; total: number }> => {
    try {
      // Build request body for POST endpoint
      const requestBody: Record<string, unknown> = {
        repository,
      };

      if (policyName) {
        requestBody.name = policyName;
      }
      if (policyData.criteriaLastBlobUpdated) {
        requestBody.criteriaLastBlobUpdated = policyData.criteriaLastBlobUpdated;
      }
      if (policyData.criteriaLastDownloaded) {
        requestBody.criteriaLastDownloaded = policyData.criteriaLastDownloaded;
      }
      if (policyData.criteriaReleaseType) {
        requestBody.criteriaReleaseType = policyData.criteriaReleaseType;
      }
      if (policyData.criteriaAssetRegex) {
        requestBody.criteriaAssetRegex = policyData.criteriaAssetRegex;
      }
      if (policyData.retain) {
        requestBody.criteriaRetain = policyData.retain;
      }
      if (policyData.sortBy) {
        requestBody.criteriaSortBy = policyData.sortBy;
      }

      // restClient.post() returns data directly, not Axios response
      const data = await restClient.post<{ results: PreviewComponent[]; total: number }>(CLEANUP_POLICY_API.PREVIEW_URL, requestBody);
      return {
        components: Array.isArray(data?.results) ? data.results : [],
        total: data?.total || 0,
      };
    } catch (err: unknown) {
      console.error('Failed to preview cleanup policy:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Generate CSV download URL for dry run
   */
  const getDryRunCsvUrl = useCallback((
    repository: string,
    policyData: CleanupPolicyFormData,
    name?: string
  ): string => {
    const params = new URLSearchParams();
    params.append('repository', repository);
    
    if (name) {
      params.append('name', name);
    }
    if (policyData.criteriaLastBlobUpdated) {
      params.append('criteriaLastBlobUpdated', String(policyData.criteriaLastBlobUpdated));
    }
    if (policyData.criteriaLastDownloaded) {
      params.append('criteriaLastDownloaded', String(policyData.criteriaLastDownloaded));
    }
    if (policyData.criteriaReleaseType) {
      params.append('criteriaReleaseType', policyData.criteriaReleaseType);
    }
    if (policyData.criteriaAssetRegex) {
      params.append('criteriaAssetRegex', policyData.criteriaAssetRegex);
    }
    if (policyData.retain) {
      params.append('criteriaRetain', String(policyData.retain));
    }
    if (policyData.sortBy) {
      params.append('criteriaSortBy', policyData.sortBy);
    }

    return ExtJS.urlOf(`/${CLEANUP_POLICY_API.PREVIEW_CSV_URL}?${params.toString()}`);
  }, []);

  /**
   * Check if preview is available based on database type
   */
  const isPreviewEnabled = useCallback((): boolean => {
    try {
      return (
        ExtJS.state().getValue('datastore.isPostgresql') &&
        ExtJS.state().getValue('nexus.cleanup.preview.enabled')
      );
    } catch {
      return false;
    }
  }, []);

  /**
   * Check if retain is enabled for a format.
   * CleanupRetainStateContributor sets nexus.cleanup.{format}Retain for each format
   * based on ProCleanupFeatureCheck.isRetainSupported().
   */
  const isRetainEnabled = useCallback((format: string): boolean => {
    try {
      return ExtJS.state().getValue(`nexus.cleanup.${format}Retain`) ?? false;
    } catch {
      return false;
    }
  }, []);

  /**
   * Check if the global retainAllFormats feature flag is enabled.
   * Gates multi-repo selection, preview table, and API execution features.
   */
  const isRetainAllFormatsEnabled = useCallback((): boolean => {
    try {
      return ExtJS.state().getValue('nexus.cleanup.retainAllFormats.enabled') ?? false;
    } catch {
      return false;
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchCleanupPolicies,
    fetchCleanupPolicy,
    fetchFormatCriteria,
    fetchRepositories,
    createCleanupPolicy,
    updateCleanupPolicy,
    deleteCleanupPolicy,
    previewCleanupPolicy,
    getDryRunCsvUrl,
    isPreviewEnabled,
    isRetainEnabled,
    isRetainAllFormatsEnabled,
  };
}

export default useCleanupPoliciesApi;

