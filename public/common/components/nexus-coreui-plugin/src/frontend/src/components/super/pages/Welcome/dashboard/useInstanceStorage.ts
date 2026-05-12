/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */

import { useState, useEffect, useCallback } from 'react';
import { restClient } from '@/utils/api';

const BLOB_STORES_URL = '/service/rest/internal/ui/blobstores';

interface BlobStoreRecord {
  unavailable?: boolean;
  totalSizeInBytes?: number;
}

/**
 * Returns current instance storage by summing totalSizeInBytes from all blob stores.
 * Use when monthly-metrics peakStorage is null (e.g. new instance, Cloud).
 */
export function useInstanceStorage(): {
  currentStorageBytes: number | null;
  loading: boolean;
  error: string | null;
} {
  const [state, setState] = useState<{
    currentStorageBytes: number | null;
    loading: boolean;
    error: string | null;
  }>({
    currentStorageBytes: null,
    loading: true,
    error: null,
  });

  const fetchStorage = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const data = await restClient.get<BlobStoreRecord[]>(BLOB_STORES_URL);
      const stores = Array.isArray(data) ? data : [];
      const total = stores.reduce((sum, s) => {
        if (s.unavailable) return sum;
        const size = s.totalSizeInBytes;
        return sum + (typeof size === 'number' && size > 0 ? size : 0);
      }, 0);
      setState({
        currentStorageBytes: total > 0 ? total : null,
        loading: false,
        error: null,
      });
    } catch {
      setState({
        currentStorageBytes: null,
        loading: false,
        error: null,
      });
    }
  }, []);

  useEffect(() => {
    fetchStorage();
  }, [fetchStorage]);

  return state;
}
