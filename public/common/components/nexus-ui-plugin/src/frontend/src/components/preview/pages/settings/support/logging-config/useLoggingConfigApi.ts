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
import { Logger, LogLevel, LOGGING_CONFIG_API } from './types';

/**
 * Custom hook for Logging Configuration API operations
 */
export function useLoggingConfigApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch all loggers
   */
  const fetchLoggers = useCallback(async (): Promise<Logger[]> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.get<Logger[]>(LOGGING_CONFIG_API.LIST);
      return Array.isArray(data) ? data : [];
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to load loggers';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Fetch a single logger by name
   */
  const fetchLogger = useCallback(async (name: string): Promise<Logger | null> => {
    try {
      const data = await restClient.get<Logger>(LOGGING_CONFIG_API.GET(name));
      return data || null;
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to load logger';
      throw new Error(message);
    }
  }, []);

  /**
   * Update or create a logger
   */
  const updateLogger = useCallback(async (name: string, level: LogLevel): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.put(LOGGING_CONFIG_API.UPDATE(name), { level });
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to update logger';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Reset a single logger to default level
   */
  const resetLogger = useCallback(async (name: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(LOGGING_CONFIG_API.RESET(name));
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to reset logger';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Reset all loggers to default levels
   */
  const resetAllLoggers = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(LOGGING_CONFIG_API.RESET_ALL);
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to reset all loggers';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchLoggers,
    fetchLogger,
    updateLogger,
    resetLogger,
    resetAllLoggers,
  };
}

export default useLoggingConfigApi;


