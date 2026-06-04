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
import { restClient, parseApiError } from '../../../../../../interface/api';
import { UserTokenSettings, DEFAULT_USER_TOKEN_SETTINGS } from './types';

// REST API V1 endpoints (UserTokensApiResourceV1.java)
const USER_TOKENS_URL = '/service/rest/v1/security/user-tokens';

/**
 * Custom hook for User Tokens API operations
 * Uses restClient for CSRF token handling
 */
export function useUserTokensApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch current user token settings.
   * Returns DEFAULT_USER_TOKEN_SETTINGS on 404 (feature not licensed / not available).
   */
  const fetchSettings = useCallback(async (): Promise<UserTokenSettings> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.get<UserTokenSettings>(USER_TOKENS_URL);
      return data || DEFAULT_USER_TOKEN_SETTINGS;
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 404) {
        // Feature not licensed or endpoint not available — return defaults silently
        return DEFAULT_USER_TOKEN_SETTINGS;
      }
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Save user token settings
   */
  const saveSettings = useCallback(async (settings: UserTokenSettings): Promise<UserTokenSettings> => {
    setLoading(true);
    setError(null);
    try {
      // PUT usually returns 204 No Content, so data might be undefined/empty
      const data = await restClient.put<UserTokenSettings>(USER_TOKENS_URL, settings);
      return data || settings;
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Reset all user tokens
   * Uses DELETE on the same base URL (per UserTokensApiResource.resetAllUserTokens)
   */
  const resetAllTokens = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(USER_TOKENS_URL);
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
    fetchSettings,
    saveSettings,
    resetAllTokens,
  };
}

export default useUserTokensApi;


