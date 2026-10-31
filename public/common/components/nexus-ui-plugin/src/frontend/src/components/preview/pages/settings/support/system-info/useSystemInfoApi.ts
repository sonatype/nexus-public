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

import { SystemInformation, HASystemInformation, HANode } from './types';

const { REST } = APIConstants;

// Backend shape: { [sectionName]: { [nodeId]: sectionData } }
// Frontend shape: { [nodeId]: { [sectionName]: sectionData } }
function transposeHASystemInfo(
  raw: Record<string, Record<string, unknown>>
): HASystemInformation {
  const result: HASystemInformation = {};
  for (const [section, nodeMap] of Object.entries(raw)) {
    if (nodeMap && typeof nodeMap === 'object') {
      for (const [nodeId, sectionData] of Object.entries(nodeMap)) {
        if (!result[nodeId]) result[nodeId] = {};
        (result[nodeId] as Record<string, unknown>)[section] = sectionData;
      }
    }
  }
  return result;
}

// API endpoints
const SYSTEM_INFO_URL = REST.SYSTEM_INFORMATION;
const SYSTEM_INFO_HA_URL = REST.SYSTEM_INFORMATION_HA;
const ACTIVE_NODES_URL = REST.INTERNAL.GET_SUPPORT_ZIP_ACTIVE_NODES;

/**
 * Custom hook for System Information API operations
 */
export function useSystemInfoApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch system information (non-HA mode)
   */
  const fetchSystemInfo = useCallback(async (): Promise<SystemInformation> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.get<SystemInformation>(SYSTEM_INFO_URL);
      return data || {};
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to load system information';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Fetch system information for HA cluster (all nodes)
   */
  const fetchSystemInfoHA = useCallback(async (): Promise<HASystemInformation> => {
    setLoading(true);
    setError(null);
    try {
      const data = await restClient.get<Record<string, Record<string, unknown>>>(SYSTEM_INFO_HA_URL);
      return transposeHASystemInfo(data || {});
    } catch (err: any) {
      const apiError = parseApiError(err);
      const message = apiError.message || 'Failed to load HA system information';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Fetch active nodes for HA mode
   */
  const fetchActiveNodes = useCallback(async (): Promise<HANode[]> => {
    try {
      const data = await restClient.get<HANode[]>(ACTIVE_NODES_URL);
      if (Array.isArray(data)) {
        return data;
      }
      return [];
    } catch (_err: any) {
      return [];
    }
  }, []);

  /**
   * Download system information as JSON file
   */
  const downloadSystemInfo = useCallback((data: SystemInformation | HASystemInformation, filename: string = 'system-information.json') => {
    const jsonStr = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, []);

  /**
   * Copy system information to clipboard
   */
  const copyToClipboard = useCallback(async (data: SystemInformation | HASystemInformation): Promise<boolean> => {
    try {
      const jsonStr = JSON.stringify(data, null, 2);
      await navigator.clipboard.writeText(jsonStr);
      return true;
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
      return false;
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchSystemInfo,
    fetchSystemInfoHA,
    fetchActiveNodes,
    downloadSystemInfo,
    copyToClipboard,
  };
}

export default useSystemInfoApi;
