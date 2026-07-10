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
import { APIConstants } from '../../../../../../constants/APIConstants';
import { SupportZipParams, SupportZipResponse, NodeInfo, SUPPORT_ZIP_API } from './types';

const { REST } = APIConstants;

interface SupportZipNodeRequest extends SupportZipParams {
  hostname: string;
}

/**
 * Custom hook for Support ZIP API operations.
 *
 * Single-node uses the legacy /v1/support/supportzippath endpoint.
 * HA uses the per-node internal endpoints under /service/rest/internal/ui/supportzip/.
 */
export function useSupportZipApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const wrap = useCallback(
    async <T,>(action: () => Promise<T>, fallbackMessage: string): Promise<T> => {
      setLoading(true);
      setError(null);
      try {
        return await action();
      } catch (err) {
        const message = parseApiError(err).message || fallbackMessage;
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  const createSupportZip = useCallback(
    (params: SupportZipParams) =>
      wrap(
        () => restClient.post<SupportZipResponse>(SUPPORT_ZIP_API.CREATE, params),
        'Failed to create support ZIP'
      ),
    [wrap]
  );

  const fetchActiveNodes = useCallback(
    () =>
      wrap(
        () => restClient.get<NodeInfo[]>(REST.INTERNAL.GET_SUPPORT_ZIP_ACTIVE_NODES),
        'Failed to fetch active nodes'
      ),
    [wrap]
  );

  const fetchNodeStatus = useCallback(
    (nodeId: string) =>
      restClient.get<NodeInfo>(`${REST.INTERNAL.GET_ZIP_STATUS}${nodeId}`),
    []
  );

  const clearNode = useCallback(
    (nodeId: string) =>
      restClient.delete<void>(`${REST.INTERNAL.CLEAR_SUPPORT_ZIP_HISTORY}${nodeId}`),
    []
  );

  const generateForNode = useCallback(
    (nodeId: string, params: SupportZipParams, hostname: string) => {
      const body: SupportZipNodeRequest = { ...params, hostname };
      return wrap(
        () => restClient.post<NodeInfo>(`${REST.INTERNAL.SUPPORT_ZIP}${nodeId}`, body),
        'Failed to generate support ZIP for node'
      );
    },
    [wrap]
  );

  const getDownloadUrl = useCallback((filename: string): string => {
    return SUPPORT_ZIP_API.DOWNLOAD(filename);
  }, []);

  return {
    loading,
    error,
    setError,
    createSupportZip,
    fetchActiveNodes,
    fetchNodeStatus,
    generateForNode,
    clearNode,
    getDownloadUrl,
  };
}

export default useSupportZipApi;
