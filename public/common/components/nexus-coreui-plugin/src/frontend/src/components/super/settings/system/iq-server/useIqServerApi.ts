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
import Axios from 'axios';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import {
  IqServerConfiguration,
  IqVerificationResult,
  DEFAULT_IQ_CONFIGURATION,
  IqCapabilities,
  DEFAULT_IQ_CAPABILITIES
} from './types';

const IQ_API = 'service/rest/v1/iq';
const IQ_CAPABILITIES_API = 'service/rest/v1/iq/capabilities';
const IQ_CAPABILITIES_TEST_API = 'service/rest/v1/iq/capabilities/test';
const IQ_VERIFY_SELFHOSTED = 'service/rest/internal/ui/iq/verify-connection';
const IQ_VERIFY_CLOUD = 'service/rest/v1/iq/test-new-connection';

function getVerifyApi(): string {
  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;
  return isCloud ? IQ_VERIFY_CLOUD : IQ_VERIFY_SELFHOSTED;
}

/**
 * Custom hook for IQ Server API operations using REST
 */
export function useIqServerApi() {
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch IQ Server configuration
   */
  const fetchSettings = useCallback(async (): Promise<IqServerConfiguration> => {
    try {
      const response = await Axios.get(IQ_API);
      const data = {
        ...DEFAULT_IQ_CONFIGURATION,
        ...response.data,
      };
      if (!data.authenticationType && data.username) {
        data.authenticationType = 'USER';
      }
      return data;
    } catch (err: any) {
      console.error('Failed to fetch IQ Server settings:', err);
      throw new Error(err?.response?.data?.message || err?.message || 'Failed to load IQ Server settings');
    }
  }, []);

  /**
   * Fetch IQ Server capabilities
   */
  const fetchCapabilities = useCallback(async (): Promise<IqCapabilities> => {
    try {
      const response = await Axios.get(IQ_CAPABILITIES_API);
      return {
        ...DEFAULT_IQ_CAPABILITIES,
        ...response.data,
      };
    } catch (err: any) {
      console.error('Failed to fetch IQ Server capabilities:', err);
      return DEFAULT_IQ_CAPABILITIES;
    }
  }, []);

  /**
   * Fetch IQ Server capabilities for a specific configuration (unsaved)
   */
  const fetchCapabilitiesWithConfig = useCallback(async (settings: IqServerConfiguration): Promise<IqCapabilities> => {
    try {
      const response = await Axios.post(IQ_CAPABILITIES_TEST_API, settings);
      return {
        ...DEFAULT_IQ_CAPABILITIES,
        ...response.data,
      };
    } catch (err: any) {
      console.error('Failed to fetch IQ Server capabilities with config:', err);
      return DEFAULT_IQ_CAPABILITIES;
    }
  }, []);

  /**
   * Save IQ Server configuration
   */
  const saveSettings = useCallback(async (settings: IqServerConfiguration): Promise<IqServerConfiguration> => {
    setLoading(true);
    setError(null);
    try {
      await Axios.put(IQ_API, settings);
      const response = await Axios.get(IQ_API);
      const serverData = response.data || {};
      // Merge: server-returned fields win, but preserve local fields the API doesn't return
      // (authenticationType, useTrustStoreForUrl, timeoutSeconds, properties)
      const data: IqServerConfiguration = {
        ...DEFAULT_IQ_CONFIGURATION,
        authenticationType: settings.authenticationType,
        useTrustStoreForUrl: settings.useTrustStoreForUrl,
        timeoutSeconds: settings.timeoutSeconds,
        properties: settings.properties,
        ...serverData,
      };
      if (!data.authenticationType && data.username) {
        data.authenticationType = 'USER';
      }
      return data;
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Failed to save IQ Server settings';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Verify connection to IQ Server
   */
  const verifyConnection = useCallback(async (settings: IqServerConfiguration): Promise<IqVerificationResult> => {
    setVerifying(true);
    setError(null);
    try {
      const response = await Axios.post(getVerifyApi(), settings);
      return {
        success: true,
        reason: response.data?.reason,
      };
    } catch (err: any) {
      const reason = err?.response?.data || err?.message || 'Connection verification failed';
      return {
        success: false,
        reason: typeof reason === 'string' ? reason : JSON.stringify(reason),
      };
    } finally {
      setVerifying(false);
    }
  }, []);

  return {
    loading,
    verifying,
    error,
    setError,
    fetchSettings,
    fetchCapabilities,
    fetchCapabilitiesWithConfig,
    saveSettings,
    verifyConnection,
  };
}

export default useIqServerApi;


