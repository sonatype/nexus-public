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
import { AnonymousSettings, RealmType } from './types';

const { REST } = APIConstants;

/**
 * Custom hook for Anonymous settings API operations
 */
export function useAnonymousApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch available realm types for the anonymous user dropdown
   */
  const fetchRealmTypes = useCallback(async (): Promise<RealmType[]> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<RealmType[]>(REST.INTERNAL.REALMS_TYPES);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch realm types:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch anonymous settings
   */
  const fetchSettings = useCallback(async (): Promise<AnonymousSettings> => {
    try {
      // restClient.get() returns data directly, not Axios response
      const data = await restClient.get<AnonymousSettings>(REST.INTERNAL.ANONYMOUS_SETTINGS);
      return data;
    } catch (err: unknown) {
      console.error('Failed to fetch anonymous settings:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Update anonymous settings
   */
  const saveSettings = useCallback(async (settings: AnonymousSettings): Promise<AnonymousSettings> => {
    setLoading(true);
    setError(null);
    try {
      // restClient.put() returns data directly, not Axios response
      const data = await restClient.put<AnonymousSettings>(REST.INTERNAL.ANONYMOUS_SETTINGS, {
        ...settings,
        userId: settings.userId.trim(),
      });
      return data;
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
    fetchRealmTypes,
    fetchSettings,
    saveSettings,
  };
}

export default useAnonymousApi;


