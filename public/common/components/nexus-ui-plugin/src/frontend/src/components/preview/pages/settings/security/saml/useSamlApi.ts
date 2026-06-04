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
import { APIConstants } from '../../../../../../constants/APIConstants';
import { restClient, parseApiError } from '../../../../../../interface/api';
import { SamlConfiguration } from './types';

const SAML_URL = APIConstants.REST.PUBLIC.SAML;

export function useSamlApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchConfiguration = useCallback(async (): Promise<SamlConfiguration | null> => {
    setLoading(true);
    setError(null);

    try {
      const data = await restClient.get<SamlConfiguration>(SAML_URL);
      return data;
    } catch (err: any) {
      // 404 means no configuration exists yet - not an error
      if (err.response?.status === 404) {
        return null;
      }
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to load SAML configuration';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const saveConfiguration = useCallback(async (config: SamlConfiguration): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      await restClient.put(SAML_URL, config);
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to save SAML configuration';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const deleteConfiguration = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      await restClient.delete(SAML_URL);
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to delete SAML configuration';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const getMetadataUrl = useCallback((): string => {
    return `${SAML_URL}/metadata`;
  }, []);

  return {
    loading,
    error,
    setError,
    fetchConfiguration,
    saveConfiguration,
    deleteConfiguration,
    getMetadataUrl,
  };
}


