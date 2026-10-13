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

import { useState, useCallback, useEffect, useRef } from 'react';

import ExtJS from '../../../../../../interface/ExtJS';
import { restClient, API_BASE } from '../../../../../../interface/api';

import { DEFAULT_RETENTION_DAYS, DeletedItem, DeletedItemType, FacetsResponse, PageResponse } from './types';

const SOFT_DELETE_API = `${API_BASE}/v1/soft-delete`;

// =============================================================================
// Backend response types
// =============================================================================

// Exported for unit testing.
export interface SoftDeletedEntityXO {
  entityType: string;
  identifier: string;
  displayName: string;
  deletedOn: string;
  deletedBy: string;
  /** Friendly display name resolved from the principal server-side; null when unresolvable. */
  deletedByName?: string | null;
  metadata?: Record<string, unknown> | null;
  /** Assets only: whether the asset's parent component is also in Trash (null for non-assets). */
  parentComponentInTrash?: boolean | null;
}

// Exported for unit testing and for callers that need per-item outcomes.
export interface BulkActionResultXO {
  entityType: string;
  identifier: string;
  error?: string | null;
}

// Exported for unit testing and for callers that need per-item outcomes.
export interface BulkActionResponseXO {
  successful: BulkActionResultXO[];
  failed: BulkActionResultXO[];
}

// =============================================================================
// Mapping helpers
// =============================================================================

// The backend soft-delete services only support these three entity types (repository, component, asset);
// blob-store/tag/user were descoped, so any other value from the server falls back to 'component' below.
const VALID_ENTITY_TYPES = new Set<DeletedItemType>([
  'repository', 'component', 'asset',
]);

function extractFormat(metadata?: Record<string, unknown> | null): string | undefined {
  if (!metadata) return undefined;
  if (typeof metadata.format === 'string') return metadata.format;
  if (typeof metadata.recipeName === 'string') {
    const lastDash = metadata.recipeName.lastIndexOf('-');
    return lastDash > 0 ? metadata.recipeName.substring(0, lastDash) : metadata.recipeName;
  }
  return undefined;
}

function buildCoordinates(metadata?: Record<string, unknown> | null): string | undefined {
  if (!metadata) return undefined;
  const { namespace, name, version } = metadata;
  if (!name) return undefined;
  const parts = [namespace, name, version].filter((p) => typeof p === 'string' && p.length > 0);
  return parts.join(':') || undefined;
}

function addDays(isoDate: string, days: number): string {
  // Use UTC getters/setters so a DST transition within the window can't shift the computed retention
  // expiry by an hour (the value is then rendered from the ISO string) (NEXUS-51733).
  const d = new Date(isoDate);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString();
}

// Exported so callers can correlate BulkActionResponseXO entries (entityType +
// identifier) back to DeletedItem.id, which is built with this same scheme.
export function makeUniqueId(entityType: string, identifier: string): string {
  return `${entityType}::${identifier}`;
}

function parseUniqueId(uniqueId: string): { entityType: string; identifier: string } {
  const separatorIndex = uniqueId.indexOf('::');
  if (separatorIndex < 0) {
    // No '::' separator: return the whole string as the identifier with an empty entityType rather than
    // silently corrupting it (substring(0,-1) => '' and substring(1) => uniqueId.slice(1)) (NEXUS-51733).
    return { entityType: '', identifier: uniqueId };
  }
  return {
    entityType: uniqueId.substring(0, separatorIndex),
    identifier: uniqueId.substring(separatorIndex + 2),
  };
}

// Exported for unit testing.
export function mapToDeletedItem(xo: SoftDeletedEntityXO, retentionDays: number): DeletedItem {
  let entityType: DeletedItemType;
  if (VALID_ENTITY_TYPES.has(xo.entityType as DeletedItemType)) {
    entityType = xo.entityType as DeletedItemType;
  } else {
    console.warn(`Unknown entity type "${xo.entityType}" received from server, treating as component`);
    entityType = 'component';
  }

  // For repository entities the soft-deleted item IS the repository, so the
  // metadata does not carry repositoryName. Surface the entity's own name as
  // the repository field so the Repository column renders the repo name in
  // line with assets and components, instead of falling back to an em-dash.
  const repository: string | undefined =
    entityType === 'repository'
      ? xo.displayName
      : ((xo.metadata?.repositoryName as string) ?? undefined);

  return {
    id: makeUniqueId(entityType, xo.identifier),
    name: xo.displayName,
    entityType,
    repository,
    format: extractFormat(xo.metadata),
    deletedOn: xo.deletedOn,
    deletedBy: xo.deletedBy,
    deletedByName: xo.deletedByName ?? undefined,
    path: typeof xo.metadata?.path === 'string' ? xo.metadata.path : undefined,
    componentCoordinates: buildCoordinates(xo.metadata),
    retentionExpiresOn: addDays(xo.deletedOn, retentionDays),
    containingGroups: Array.isArray(xo.metadata?.containingGroups)
      ? (xo.metadata.containingGroups as string[])
      : undefined,
    parentComponentInTrash:
      typeof xo.parentComponentInTrash === 'boolean' ? xo.parentComponentInTrash : undefined,
  };
}

// =============================================================================
// useDeletedItems
// =============================================================================

export interface UseDeletedItemsParams {
  offset: number;
  limit: number;
  sort: string;
  entityTypes?: string[];
  formats?: string[];
  deletedBy?: string[];
  repositoryId?: string;
  q?: string;
}

export interface UseDeletedItemsResult {
  items: DeletedItem[];
  total: number;
  offset: number;
  limit: number;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useDeletedItems(params: UseDeletedItemsParams): UseDeletedItemsResult {
  const [items, setItems] = useState<DeletedItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const fetchCountRef = useRef(0);

  const fetchData = useCallback(async () => {
    const fetchId = ++fetchCountRef.current;
    setLoading(true);
    setError(null);

    try {
      const searchParams = new URLSearchParams();
      searchParams.set('offset', String(params.offset));
      searchParams.set('limit', String(params.limit));
      searchParams.set('sort', params.sort);
      params.entityTypes?.forEach((v) => searchParams.append('entityType', v));
      params.formats?.forEach((v) => searchParams.append('format', v));
      params.deletedBy?.forEach((v) => searchParams.append('deletedBy', v));
      if (params.repositoryId) searchParams.set('repositoryId', params.repositoryId);
      if (params.q) searchParams.set('q', params.q);

      const response = await restClient.get<PageResponse<SoftDeletedEntityXO>>(
        `${SOFT_DELETE_API}/browse-all?${searchParams.toString()}`
      );

      if (fetchId !== fetchCountRef.current) return;

      const retentionDays: number =
          ExtJS.state()?.getValue?.('softDeleteRetentionDays') ?? DEFAULT_RETENTION_DAYS;
      setItems(response.items.map((xo) => mapToDeletedItem(xo, retentionDays)));
      setTotal(response.total);
    } catch (err: unknown) {
      if (fetchId !== fetchCountRef.current) return;
      const message = err instanceof Error ? err.message : 'Failed to load deleted items. Please try again.';
      setError(message);
    } finally {
      if (fetchId === fetchCountRef.current) {
        setLoading(false);
      }
    }
  }, [params.offset, params.limit, params.sort, params.entityTypes, params.formats, params.deletedBy, params.repositoryId, params.q]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { items, total, offset: params.offset, limit: params.limit, loading, error, refetch: fetchData };
}

// =============================================================================
// fetchDeletedItemChildren (expandable Trash hierarchy)
// =============================================================================

/**
 * Fetches the soft-deleted children of a soft-deleted parent (currently a component's soft-deleted
 * assets) for the expandable Trash row. The children endpoint returns a bare array (not a page); the
 * child set for a single component is small, so it is not paginated.
 *
 * @param parentId the parent DeletedItem.id in `entityType::identifier` form (see makeUniqueId)
 */
export async function fetchDeletedItemChildren(parentId: string): Promise<DeletedItem[]> {
  const { entityType, identifier } = parseUniqueId(parentId);
  const retentionDays: number =
      ExtJS.state()?.getValue?.('softDeleteRetentionDays') ?? DEFAULT_RETENTION_DAYS;
  const children = await restClient.get<SoftDeletedEntityXO[]>(
    `${SOFT_DELETE_API}/${encodeURIComponent(entityType)}/${encodeURIComponent(identifier)}/children`
  );
  return children.map((xo) => mapToDeletedItem(xo, retentionDays));
}

// =============================================================================
// useRestoreItems
// =============================================================================

export interface UseRestoreItemsResult {
  restore: (items: Array<Pick<DeletedItem, 'id' | 'entityType'>>) => Promise<BulkActionResponseXO>;
  loading: boolean;
}

const EMPTY_BULK_RESPONSE: BulkActionResponseXO = { successful: [], failed: [] };

export function useRestoreItems(): UseRestoreItemsResult {
  const [loading, setLoading] = useState(false);

  // Returns the per-item outcome ({ successful, failed }) rather than throwing on
  // partial failure. A partial success is NOT a total failure: the caller needs the
  // list of succeeded items to clear them from the selection while keeping the failed
  // ones for retry. Only a transport/HTTP error (restClient.post rejecting) propagates.
  const restore = useCallback(
    async (items: Array<Pick<DeletedItem, 'id' | 'entityType'>>): Promise<BulkActionResponseXO> => {
      if (items.length === 0) return EMPTY_BULK_RESPONSE;

      setLoading(true);

      try {
        return await restClient.post<BulkActionResponseXO>(
          `${SOFT_DELETE_API}/bulk-restore`,
          {
            items: items.map((item) => {
              const parsed = parseUniqueId(item.id);
              return { entityType: parsed.entityType, identifier: parsed.identifier };
            }),
          }
        );
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return { restore, loading };
}

// =============================================================================
// usePurgeItems
// =============================================================================

export interface UsePurgeItemsResult {
  purge: (items: Array<Pick<DeletedItem, 'id' | 'entityType'>>) => Promise<BulkActionResponseXO>;
  loading: boolean;
}

export function usePurgeItems(): UsePurgeItemsResult {
  const [loading, setLoading] = useState(false);

  // Mirrors useRestoreItems: returns the per-item outcome instead of throwing on
  // partial failure, so the caller can clear succeeded items and retry the rest.
  const purge = useCallback(
    async (items: Array<Pick<DeletedItem, 'id' | 'entityType'>>): Promise<BulkActionResponseXO> => {
      if (items.length === 0) return EMPTY_BULK_RESPONSE;

      setLoading(true);

      try {
        return await restClient.post<BulkActionResponseXO>(
          `${SOFT_DELETE_API}/bulk-purge`,
          {
            items: items.map((item) => {
              const parsed = parseUniqueId(item.id);
              return { entityType: parsed.entityType, identifier: parsed.identifier };
            }),
          }
        );
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return { purge, loading };
}

// =============================================================================
// useDeletedItemsFacets
// =============================================================================

export interface UseFacetsResult {
  facets: FacetsResponse | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Fetches facet counts from /v1/soft-delete/facets. Counts are computed across the entire
 * trash_index and are NOT filtered by current sidebar selections or search bar.
 *
 * @param refetchKey  controller-supplied integer that callers increment after restore/purge
 *                    to force a refetch. The hook does NOT auto-refetch on filter changes.
 * @param deletedByPrefix  optional case-insensitive prefix filter for the deletedBy facet. Reserved for a
 *                         future "show all / search principals" affordance; no caller passes it yet.
 * @param deletedByLimit   number of deletedBy facet buckets to fetch (server cap is 100; default 10).
 */
export function useDeletedItemsFacets(
  refetchKey: number,
  deletedByPrefix?: string,
  deletedByLimit: number = 10,
): UseFacetsResult {
  const [facets, setFacets] = useState<FacetsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const fetchCountRef = useRef(0);

  const fetchData = useCallback(async () => {
    const fetchId = ++fetchCountRef.current;
    setLoading(true);
    setError(null);
    try {
      const sp = new URLSearchParams();
      if (deletedByPrefix) sp.set('deletedByPrefix', deletedByPrefix);
      sp.set('deletedByLimit', String(deletedByLimit));
      const url = `${SOFT_DELETE_API}/facets?${sp.toString()}`;
      const data = await restClient.get<FacetsResponse>(url);
      if (fetchId !== fetchCountRef.current) return;
      setFacets(data);
    } catch (err: unknown) {
      if (fetchId !== fetchCountRef.current) return;
      const message = err instanceof Error ? err.message : 'Failed to load facet counts.';
      setError(message);
    } finally {
      if (fetchId === fetchCountRef.current) {
        setLoading(false);
      }
    }
  }, [deletedByPrefix, deletedByLimit]);

  useEffect(() => {
    fetchData();
  }, [fetchData, refetchKey]);

  return { facets, loading, error, refetch: fetchData };
}
