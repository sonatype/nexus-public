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

import { useCallback, useEffect, useState } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';

const DETAILS_URL = '/service/rest/internal/ui/repositories/details';
const BULK_URL = '/service/rest/internal/ui/repositories/global-distribution';

export type RepositoryCdnType = 'hosted' | 'proxy' | 'group';

export interface RepositoryCdnRow {
  id: string;
  name: string;
  type: RepositoryCdnType;
  format: string;
  /** Empty string for group repositories (they have no blob store). */
  blobStoreName: string;
  globalDistribution: boolean;
}

export interface ApplyResult {
  updated: string[];
  failed: Array<{ id: string; message: string }>;
}

export interface UseRepositoryCdnListResult {
  rows: RepositoryCdnRow[];
  isLoading: boolean;
  error: string | null;
  isUpdating: boolean;
  applyCdnState: (ids: string[], value: boolean) => Promise<ApplyResult>;
  refetch: () => Promise<void>;
}

interface RepositoryFromApi {
  name: string;
  type: string;
  format: string;
  blobStoreName?: string;
  globalDistribution?: boolean;
}

interface BulkResponse {
  updated: string[];
  failed: Array<{ name: string; message: string }>;
}

export function useRepositoryCdnList(): UseRepositoryCdnListResult {
  const [rows, setRows] = useState<RepositoryCdnRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpdating, setIsUpdating] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await restClient.get<RepositoryFromApi[]>(DETAILS_URL);
      const list = Array.isArray(data) ? data : [];
      const mapped: RepositoryCdnRow[] = list
        .filter((r) => r.type === 'hosted' || r.type === 'proxy' || r.type === 'group')
        .map((r) => ({
          id: r.name,
          name: r.name,
          type: r.type as RepositoryCdnType,
          format: r.format,
          // Missing blob store maps to '' so the `blob-store-off` derivation
          // (statuses[''] === false) never fires.
          blobStoreName: r.blobStoreName ?? '',
          globalDistribution: Boolean(r.globalDistribution),
        }));
      setRows(mapped);
    } catch (err) {
      const apiError = parseApiError(err);
      setError(apiError.message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const applyCdnState = useCallback(
    async (ids: string[], value: boolean): Promise<ApplyResult> => {
      if (ids.length === 0) {
        return { updated: [], failed: [] };
      }
      const selection = new Set(ids);
      const targetRows = rows.filter((r) => selection.has(r.id));
      if (targetRows.length === 0) {
        return { updated: [], failed: [] };
      }

      setIsUpdating(true);
      try {
        // Bulk toggle runs server-side against the persisted Configuration to preserve
        // encrypted httpclient secrets. The prior client-side GET+PUT loop replayed the
        // stripped GET body (password/bearerToken are @JsonProperty WRITE_ONLY) as a PUT and
        // caused HttpAuthenticationSecretEncoder to delete the stored secret — see
        // RepositoryGlobalDistributionResource for the full mechanism.
        let response: BulkResponse;
        try {
          response = await restClient.post<BulkResponse>(BULK_URL, {
            repositoryNames: targetRows.map((r) => r.id),
            globalDistribution: value,
          });
        } catch (err) {
          const apiError = parseApiError(err);
          const message = apiError.message;
          return {
            updated: [],
            failed: targetRows.map((r) => ({ id: r.id, message })),
          };
        }

        const updated = Array.isArray(response?.updated) ? response.updated : [];
        const failed = Array.isArray(response?.failed)
          ? response.failed.map((f) => ({ id: f.name, message: f.message }))
          : [];

        if (updated.length > 0) {
          const updatedSet = new Set(updated);
          setRows((prev) =>
            prev.map((r) =>
              updatedSet.has(r.id) ? { ...r, globalDistribution: value } : r,
            ),
          );
        }

        return { updated, failed };
      } finally {
        setIsUpdating(false);
      }
    },
    [rows],
  );

  return {
    rows,
    isLoading,
    error,
    isUpdating,
    applyCdnState,
    refetch: load,
  };
}

export default useRepositoryCdnList;
