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
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { LicenseData, LICENSE_API, readFileAsArrayBuffer } from './types';

/**
 * Custom hook for Licensing API operations
 */
export function useLicensingApi() {
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
   * Fetch license data
   */
  const fetchLicense = useCallback(async (): Promise<LicenseData> => {
    try {
      const data = await restClient.get<LicenseData>(LICENSE_API.BASE_URL);
      return data || {};
    } catch (err) {
      console.error('Failed to fetch license:', err);
      throw new Error('Failed to load license information');
    }
  }, []);

  /**
   * Upload license file
   */
  const uploadLicense = useCallback(async (file: File): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const fileData = await readFileAsArrayBuffer(file);
      await restClient.post(LICENSE_API.BASE_URL, fileData, {
        headers: {
          'Content-Type': 'application/octet-stream',
        },
      });
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Get license agreement URL
   */
  const getLicenseAgreementUrl = useCallback((): string => {
    try {
      return ExtJS.proLicenseUrl() || '';
    } catch {
      return '';
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchLicense,
    uploadLicense,
    getLicenseAgreementUrl,
  };
}

export default useLicensingApi;


