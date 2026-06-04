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
import { restClient, parseApiError, ENDPOINTS } from '../../../../../../interface/api';
import {
  ContentSelector,
  ContentSelectorFormData,
  RepositoryOption,
  CONTENT_SELECTOR_API,
  getContentSelectorUrl,
} from './types';

/**
 * Reference type for privileges that use a content selector
 */
export interface PrivilegeReference {
  name: string;
  description: string;
}

/**
 * Custom hook for Content Selectors API operations
 */
export function useContentSelectorsApi() {
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
   * Fetch all content selectors
   */
  const fetchContentSelectors = useCallback(async (): Promise<ContentSelector[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<ContentSelector[]>(CONTENT_SELECTOR_API.BASE_URL);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch content selectors:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch a single content selector by name
   */
  const fetchContentSelector = useCallback(async (name: string): Promise<ContentSelector | null> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<ContentSelector>(getContentSelectorUrl(name));
      return data || null;
    } catch (err: unknown) {
      console.error('Failed to fetch content selector:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch all repositories for preview
   */
  const fetchRepositories = useCallback(async (): Promise<RepositoryOption[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<RepositoryOption[]>(CONTENT_SELECTOR_API.REPOSITORIES_URL);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch repositories:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new content selector
   */
  const createContentSelector = useCallback(async (data: ContentSelectorFormData): Promise<ContentSelector> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        name: data.name,
        description: data.description,
        expression: data.expression,
      };
      // restClient.post() returns data directly, not Axios response
      const result = await restClient.post<ContentSelector>(CONTENT_SELECTOR_API.BASE_URL, payload);
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
   * Update an existing content selector
   */
  const updateContentSelector = useCallback(async (
    name: string,
    data: ContentSelectorFormData
  ): Promise<ContentSelector> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        description: data.description,
        expression: data.expression,
      };
      // restClient.put() returns data directly, not Axios response
      const result = await restClient.put<ContentSelector>(getContentSelectorUrl(name), payload);
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
   * Delete a content selector
   */
  const deleteContentSelector = useCallback(async (name: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(getContentSelectorUrl(name));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Preview content selector results
   * Uses POST to match Default UI implementation
   */
  const previewContentSelector = useCallback(async (
    repository: string,
    type: string,
    expression: string
  ): Promise<string[]> => {
    try {
      const payload = {
        repository,
        type,
        expression,
      };

      // restClient.post() returns data directly, not Axios response
      const data = await restClient.post<{ results: Array<{ name: string }> }>(CONTENT_SELECTOR_API.PREVIEW_URL, payload);
      // Default UI returns { results: [{ name: '...' }, ...] }
      const results = data?.results;
      return Array.isArray(results) ? results.map((r: { name: string }) => r.name) : [];
    } catch (err: unknown) {
      console.error('Failed to preview content selector:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch privileges that reference a specific content selector
   * Filters privileges of type 'repository-content-selector' by contentSelector property
   */
  const fetchPrivilegesForSelector = useCallback(async (selectorName: string): Promise<PrivilegeReference[]> => {
    try {
      // Fetch all privileges
      const privileges = await restClient.get<Array<{
        type: string;
        name: string;
        description: string;
        contentSelector?: string;
      }>>(ENDPOINTS.PRIVILEGES);

      // Filter for privileges that use this content selector
      const matchingPrivileges = privileges.filter(
        (p) => p.type === 'repository-content-selector' && p.contentSelector === selectorName
      );

      return matchingPrivileges.map((p) => ({
        name: p.name,
        description: p.description || '',
      }));
    } catch (err: unknown) {
      console.error('Failed to fetch privileges for selector:', err);
      // Don't throw - this is informational, not critical
      return [];
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchContentSelectors,
    fetchContentSelector,
    fetchRepositories,
    createContentSelector,
    updateContentSelector,
    deleteContentSelector,
    previewContentSelector,
    fetchPrivilegesForSelector,
  };
}

export default useContentSelectorsApi;

