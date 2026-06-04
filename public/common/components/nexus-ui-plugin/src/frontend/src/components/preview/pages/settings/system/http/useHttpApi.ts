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

/**
 * HTTP Settings API Hook
 *
 * MIGRATION STATUS: 100% REST
 * - fetchSettings: ✅ REST (GET /v1/http)
 * - saveSettings: ✅ REST (PUT /v1/http)
 */

import { useState, useCallback } from 'react';
import { restClient, ENDPOINTS, parseApiError } from '../../../../../../interface/api';
import { HttpConfiguration, DEFAULT_HTTP_CONFIGURATION } from './types';

/**
 * REST API HTTP settings shape
 */
interface RestHttpSettings {
  userAgentSuffix?: string | null;
  timeout?: number | null;
  retries?: number | null;
  httpEnabled?: boolean;
  httpHost?: string | null;
  httpPort?: number | null;
  httpAuthEnabled?: boolean;
  httpAuthUsername?: string | null;
  httpAuthPassword?: string | null;
  httpAuthNtlmHost?: string | null;
  httpAuthNtlmDomain?: string | null;
  httpsEnabled?: boolean;
  httpsHost?: string | null;
  httpsPort?: number | null;
  httpsAuthEnabled?: boolean;
  httpsAuthUsername?: string | null;
  httpsAuthPassword?: string | null;
  httpsAuthNtlmHost?: string | null;
  httpsAuthNtlmDomain?: string | null;
  nonProxyHosts?: string[];
}

/**
 * Convert REST response to HttpConfiguration
 */
function restToHttpConfig(data: RestHttpSettings): HttpConfiguration {
  return {
    ...DEFAULT_HTTP_CONFIGURATION,
    ...data,
    httpEnabled: data.httpEnabled || false,
    httpsEnabled: data.httpsEnabled || false,
    httpAuthEnabled: data.httpAuthEnabled || false,
    httpsAuthEnabled: data.httpsAuthEnabled || false,
    nonProxyHosts: data.nonProxyHosts || [],
  };
}

/**
 * Prepare HTTP configuration data for saving
 */
function prepareForSave(config: HttpConfiguration): RestHttpSettings {
  const saveData: RestHttpSettings = {
    userAgentSuffix: config.userAgentSuffix || null,
    timeout: config.timeout || null,
    retries: config.retries || null,
    httpEnabled: config.httpEnabled,
    httpsEnabled: config.httpsEnabled,
    nonProxyHosts: config.nonProxyHosts || [],
  };

  // Include HTTP proxy fields if enabled
  if (config.httpEnabled) {
    saveData.httpHost = config.httpHost || null;
    saveData.httpPort = config.httpPort || null;
    saveData.httpAuthEnabled = config.httpAuthEnabled || false;

    if (config.httpAuthEnabled) {
      saveData.httpAuthUsername = config.httpAuthUsername || null;
      saveData.httpAuthPassword = config.httpAuthPassword || null;
      saveData.httpAuthNtlmHost = config.httpAuthNtlmHost || null;
      saveData.httpAuthNtlmDomain = config.httpAuthNtlmDomain || null;
    }
  }

  // Include HTTPS proxy fields if enabled
  if (config.httpsEnabled) {
    saveData.httpsHost = config.httpsHost || null;
    saveData.httpsPort = config.httpsPort || null;
    saveData.httpsAuthEnabled = config.httpsAuthEnabled || false;

    if (config.httpsAuthEnabled) {
      saveData.httpsAuthUsername = config.httpsAuthUsername || null;
      saveData.httpsAuthPassword = config.httpsAuthPassword || null;
      saveData.httpsAuthNtlmHost = config.httpsAuthNtlmHost || null;
      saveData.httpsAuthNtlmDomain = config.httpsAuthNtlmDomain || null;
    }
  }

  // Reset non-proxy hosts if no proxy is enabled
  if (!config.httpEnabled && !config.httpsEnabled) {
    saveData.nonProxyHosts = [];
  }

  return saveData;
}

const HTTP_ERROR_MESSAGES: Record<number, string> = {
  400: 'Invalid HTTP settings. Please check your proxy host and port values.',
  403: 'You do not have permission to modify HTTP settings. Contact your administrator.',
  405: 'Unable to save HTTP settings. The server rejected this request.',
  500: 'Server error while saving HTTP settings. Please try again.',
};

function humanizeHttpError(err: unknown): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const status = (err as any).response?.status;
    if (status && HTTP_ERROR_MESSAGES[status]) {
      return HTTP_ERROR_MESSAGES[status];
    }
  }
  const apiError = parseApiError(err);
  if (apiError.message.includes('status code')) {
    return 'Unable to save HTTP settings. Please try again or contact your administrator.';
  }
  return apiError.message;
}

/**
 * Custom hook for HTTP settings API operations using REST
 */
export function useHttpApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch HTTP settings configuration using REST API
   */
  const fetchSettings = useCallback(async (): Promise<HttpConfiguration> => {
    try {
      const data = await restClient.get<RestHttpSettings>(ENDPOINTS.HTTP_SETTINGS);
      return restToHttpConfig(data);
    } catch (err: unknown) {
      console.error('Failed to fetch HTTP settings:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Save HTTP settings configuration using REST API
   */
  const saveSettings = useCallback(async (settings: HttpConfiguration): Promise<HttpConfiguration> => {
    setLoading(true);
    setError(null);
    try {
      const saveData = prepareForSave(settings);
      await restClient.put<void>(ENDPOINTS.HTTP_SETTINGS, saveData);
      // Fetch updated settings after save
      const updated = await restClient.get<RestHttpSettings>(ENDPOINTS.HTTP_SETTINGS);
      return restToHttpConfig(updated);
    } catch (err: unknown) {
      const message = humanizeHttpError(err);
      setError(message);
      throw new Error(message);
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
  };
}

export default useHttpApi;
