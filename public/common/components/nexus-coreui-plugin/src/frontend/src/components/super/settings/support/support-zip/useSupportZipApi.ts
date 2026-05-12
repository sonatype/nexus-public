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
import { SupportZipParams, SupportZipResponse, SUPPORT_ZIP_API } from './types';

/**
 * Custom hook for Support ZIP API operations
 */
export function useSupportZipApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Create a support ZIP for single node
   */
  const createSupportZip = useCallback(async (
    params: SupportZipParams
  ): Promise<SupportZipResponse> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.post<SupportZipResponse>(SUPPORT_ZIP_API.CREATE, params);
      return data;
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to create support ZIP';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Create support ZIPs for all HA nodes
   */
  const createHaSupportZips = useCallback(async (
    params: SupportZipParams
  ): Promise<SupportZipResponse[]> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.post<SupportZipResponse[]>(SUPPORT_ZIP_API.CREATE_HA, params);
      return data;
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to create support ZIPs';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Get download URL for a support ZIP file
   */
  const getDownloadUrl = useCallback((filename: string): string => {
    return SUPPORT_ZIP_API.DOWNLOAD(filename);
  }, []);

  return {
    loading,
    error,
    setError,
    createSupportZip,
    createHaSupportZips,
    getDownloadUrl,
  };
}

export default useSupportZipApi;


