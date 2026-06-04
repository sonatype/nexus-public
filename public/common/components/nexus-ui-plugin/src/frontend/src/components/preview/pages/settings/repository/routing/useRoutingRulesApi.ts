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
import { 
  RoutingRule, 
  RoutingRuleFormData, 
  RoutingRuleTestRequest, 
  RoutingRulesPreview,
  PreviewFilter,
} from './types';

// API base URL for routing rules
const ROUTING_RULES_URL = '/service/rest/internal/ui/routing-rules';

/**
 * Custom hook for Routing Rules API operations
 */
export function useRoutingRulesApi() {
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
   * Fetch all routing rules
   */
  const fetchRoutingRules = useCallback(async (includeRepositoryNames = true): Promise<RoutingRule[]> => {
    try {
      const url = `${ROUTING_RULES_URL}?includeRepositoryNames=${includeRepositoryNames}`;
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<RoutingRule[]>(url);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch routing rules:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch a single routing rule by name
   */
  const fetchRoutingRule = useCallback(async (name: string): Promise<RoutingRule | null> => {
    try {
      const url = `${ROUTING_RULES_URL}/${encodeURIComponent(name)}`;
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<RoutingRule>(url);
      return data || null;
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      if (apiError.status === 404) {
        return null;
      }
      console.error('Failed to fetch routing rule:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new routing rule
   */
  const createRoutingRule = useCallback(async (data: RoutingRuleFormData): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        name: data.name,
        description: data.description || '',
        mode: data.mode,
        matchers: data.matchers.filter(m => m.trim()),
      };
      await restClient.post(ROUTING_RULES_URL, payload);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing routing rule
   */
  const updateRoutingRule = useCallback(async (name: string, data: RoutingRuleFormData): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = `${ROUTING_RULES_URL}/${encodeURIComponent(name)}`;
      const payload = {
        name: data.name,
        description: data.description || '',
        mode: data.mode,
        matchers: data.matchers.filter(m => m.trim()),
      };
      await restClient.put(url, payload);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a routing rule
   */
  const deleteRoutingRule = useCallback(async (name: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = `${ROUTING_RULES_URL}/${encodeURIComponent(name)}`;
      await restClient.delete(url);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Test if a path is allowed based on mode and matchers
   */
  const testRoutingRule = useCallback(async (data: RoutingRuleTestRequest): Promise<boolean> => {
    try {
      const url = `${ROUTING_RULES_URL}/test`;
      // restClient.post() returns data directly, not Axios response
      const result = await restClient.post<boolean>(url, data);
      return result === true;
    } catch (err: unknown) {
      console.error('Failed to test routing rule:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Get routing rules preview for path testing
   */
  const fetchRoutingRulesPreview = useCallback(async (
    path: string,
    filter: PreviewFilter = 'all'
  ): Promise<RoutingRulesPreview> => {
    try {
      const params = new URLSearchParams();
      params.set('path', path);
      if (filter !== 'all') {
        params.set('filter', filter);
      }
      const url = `${ROUTING_RULES_URL}/preview?${params.toString()}`;
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<RoutingRulesPreview>(url);
      return data || { children: [], expanded: false, expandable: false };
    } catch (err: unknown) {
      console.error('Failed to fetch routing rules preview:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchRoutingRules,
    fetchRoutingRule,
    createRoutingRule,
    updateRoutingRule,
    deleteRoutingRule,
    testRoutingRule,
    fetchRoutingRulesPreview,
  };
}

export default useRoutingRulesApi;


