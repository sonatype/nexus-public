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
import { APIConstants } from '@sonatype/nexus-ui-plugin';
import { Realm } from './types';

const { REST } = APIConstants;

/**
 * Custom hook for Realms API operations
 */
export function useRealmsApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch all available realms
   */
  const fetchAvailableRealms = useCallback(async (): Promise<Realm[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<Realm[]>(REST.PUBLIC.AVAILABLE_REALMS);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch available realms:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch active (configured) realm IDs
   */
  const fetchActiveRealmIds = useCallback(async (): Promise<string[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<string[]>(REST.PUBLIC.ACTIVE_REALMS);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch active realms:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Update active realm IDs (order matters)
   */
  const updateActiveRealms = useCallback(async (realmIds: string[]): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.put(REST.PUBLIC.ACTIVE_REALMS, realmIds);
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
    fetchAvailableRealms,
    fetchActiveRealmIds,
    updateActiveRealms,
  };
}

export default useRealmsApi;



