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

import { useState, useEffect, useMemo } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';
import { ExtJS } from '../../../../../../interface/ExtJS';

type GlobalDistributionStatus = 'UNSUPPORTED' | 'NOT_CONFIGURED' | 'CONFIGURED';

export interface BlobStoreCdnStatus {
  name: string;
  cdnEnabled: boolean;
  type?: string;
}

interface BlobStoreApiResponse {
  name: string;
  typeId: string;
  globalDistributionStatus?: GlobalDistributionStatus;
}

export interface BlobStoreCdnStatuses {
  blobStores: BlobStoreCdnStatus[];
  isLoading: boolean;
  error: string | null;
  /** False when the user may not read blob stores; the API is not called and blobStores is empty. */
  canReadBlobStores: boolean;
}

/**
 * Hook to fetch S3 blob stores and determine their CDN enablement status.
 *
 * Only returns S3 blob stores, which are the only type that supports CDN/global distribution.
 * A blob store is considered CDN-enabled if it has a globalDistribution configuration.
 * Without the nexus:blobstores:read permission the API is not called.
 */
export function useBlobStoreCdnStatuses(): BlobStoreCdnStatuses {
  const canReadBlobStores = ExtJS.checkPermission('nexus:blobstores:read');
  const [blobStores, setBlobStores] = useState<BlobStoreCdnStatus[]>([]);
  const [isLoading, setIsLoading] = useState(canReadBlobStores);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!canReadBlobStores) {
      setIsLoading(false);
      return undefined;
    }

    let cancelled = false;

    async function fetchBlobStores() {
      setIsLoading(true);
      setError(null);

      try {
        const response = await restClient.get<BlobStoreApiResponse[]>(
          '/service/rest/internal/ui/blobstores'
        );

        if (cancelled) return;

        // Filter to only S3 blob stores and extract CDN status
        // API returns an array directly, not wrapped in { data: [] }
        const blobStoreArray = Array.isArray(response) ? response : [];
        const s3BlobStores = blobStoreArray
          .filter((bs) => bs.globalDistributionStatus !== 'UNSUPPORTED')
          .map((bs) => ({
            name: bs.name,
            cdnEnabled: bs.globalDistributionStatus === 'CONFIGURED',
            type: bs.typeId,
          }))
          .sort((a, b) => a.name.localeCompare(b.name));

        setBlobStores(s3BlobStores);
      } catch (err) {
        if (!cancelled) {
          setError(parseApiError(err));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    fetchBlobStores();

    return () => {
      cancelled = true;
    };
  }, [canReadBlobStores]);

  return useMemo(
    () => ({
      blobStores,
      isLoading,
      error,
      canReadBlobStores,
    }),
    [blobStores, isLoading, error, canReadBlobStores]
  );
}

export default useBlobStoreCdnStatuses;
