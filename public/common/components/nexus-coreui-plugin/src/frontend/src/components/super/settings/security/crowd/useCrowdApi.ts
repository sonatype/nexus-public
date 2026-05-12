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

import { useState, useCallback } from 'react';
import { restClient, parseApiError } from '@/utils/api';
import { CrowdConfig, DEFAULT_CROWD_CONFIG } from './types';

const CROWD_API_URL = '/service/rest/v1/security/atlassian-crowd';

/**
 * Custom hook for Crowd API operations
 * Uses restClient for consistent API handling
 */
export function useCrowdApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch Crowd configuration
   */
  const fetchConfig = useCallback(async (): Promise<CrowdConfig> => {
    try {
      const data = await restClient.get<CrowdConfig>(CROWD_API_URL);
      return {
        ...DEFAULT_CROWD_CONFIG,
        ...data,
      };
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch Crowd config:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Save Crowd configuration
   */
  const saveConfig = useCallback(async (config: CrowdConfig): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.put(CROWD_API_URL, config);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Verify connection to Crowd server
   */
  const verifyConnection = useCallback(async (config: CrowdConfig): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(`${CROWD_API_URL}/verify-connection`, config);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Clear Crowd cache
   */
  const clearCache = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(`${CROWD_API_URL}/clear-cache`);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchConfig,
    saveConfig,
    verifyConnection,
    clearCache,
  };
}

export default useCrowdApi;


