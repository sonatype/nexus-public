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
import { restClient, parseApiError } from '@/utils/api';
import { DataStoreConfig } from './types';

// REST API URL for DataStore configuration
const DATASTORE_URL = '/service/rest/internal/ui/datastore';

/**
 * Custom hook for DataStore Configuration API operations
 */
export function useDataStoreApi() {
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
   * Fetch current DataStore configuration
   */
  const fetchConfig = useCallback(async (): Promise<DataStoreConfig> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<DataStoreConfig>(DATASTORE_URL);
      return data || {
        jdbcUrl: '',
        username: '',
        schema: '',
        maximumConnectionPool: 10,
        advanced: '',
      };
    } catch (err: unknown) {
      console.error('Failed to fetch datastore configuration:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Update DataStore configuration
   */
  const updateConfig = useCallback(async (config: DataStoreConfig): Promise<DataStoreConfig> => {
    setLoading(true);
    setError(null);
    try {
      console.log('Updating datastore config:', JSON.stringify(config, null, 2));
      // restClient.put() returns data directly, not Axios response
      const result = await restClient.put<DataStoreConfig>(DATASTORE_URL, config);
      return result || config;
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Datastore update failed:', apiError.message);
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
    updateConfig,
  };
}

export default useDataStoreApi;

