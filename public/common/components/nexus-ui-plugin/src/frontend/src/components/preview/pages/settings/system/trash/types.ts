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

/**
 * Types for the Trash / soft-delete feature (NEXUS-51733).
 *
 * Only 'repository', 'component' and 'asset' are active in this release; 'blob-store', 'tag' and 'user'
 * were descoped. The union deliberately still lists them: useTrashApi's VALID_ENTITY_TYPES relabels any
 * unsupported value as 'component' at runtime, and a test exercises that fallback by passing 'tag'/'user'.
 * They are kept so that path stays type-checked until/unless the descoped types are reintroduced.
 */

export type DeletedItemType =
  | 'repository'
  | 'blob-store'
  | 'component'
  | 'asset'
  | 'tag'
  | 'user';

export type BulkAction = 'restore' | 'purge';

export type SortField = 'name' | 'entityType' | 'repository' | 'format' | 'deletedOn' | 'deletedBy';

export type SortDirection = 'asc' | 'desc';

export interface PageResponse<T> {
  items: T[];
  total: number;
  offset: number;
  limit: number;
}

export interface DeletedItem {
  /** Unique identifier */
  id: string;
  /** Display name */
  name: string;
  /** Entity type */
  entityType: DeletedItemType;
  /** Parent repository name (N/A for users, blob stores) */
  repository?: string;
  /** Repository format (N/A for users, blob stores, tags) */
  format?: string;
  /** ISO timestamp of soft-deletion */
  deletedOn: string;
  /** User ID or system:<task-name> that performed deletion (raw login principal; authoritative identity) */
  deletedBy: string;
  /**
   * Friendly display name (first + last) resolved from the principal server-side, when available.
   * undefined when the principal can't be resolved to a named user (e.g. external SAML/LDAP or system);
   * the UI then falls back to showing deletedBy.
   */
  deletedByName?: string;
  /** Storage path (assets only) */
  path?: string;
  /** group:artifact:version coordinates (components and assets) */
  componentCoordinates?: string;
  /** When the retention window expires and auto-purge kicks in */
  retentionExpiresOn: string;
  /** Group repositories this repository was a member of before soft-deletion (repositories only) */
  containingGroups?: string[];
  /**
   * Assets only: whether the asset's parent component is also currently in Trash.
   * true  → restoring this asset also restores its (soft-deleted) parent component;
   * false → parent component is live, so restore adds the asset back to an existing component
   *         (which may have changed since deletion). undefined for non-asset entries.
   */
  parentComponentInTrash?: boolean;
}

export interface DeletedItemsFilterState {
  entityTypes: DeletedItemType[];
  formats: string[];
  deletedBy: string[];
}

export const DELETED_ITEM_TYPE_LABELS: Record<DeletedItemType, string> = {
  'repository': 'Repository',
  'blob-store': 'Blob Store',
  'component': 'Component',
  'asset': 'Asset',
  'tag': 'Tag',
  'user': 'User',
};

export const DEFAULT_FILTER_STATE: DeletedItemsFilterState = {
  entityTypes: [],
  formats: [],
  deletedBy: [],
};

/**
 * Fallback value used only when the server-provided `softDeleteRetentionDays` state key is unavailable.
 * The actual retention period is configured server-side via `nexus.softDelete.retentionDays`.
 */
export const DEFAULT_RETENTION_DAYS = 30;

/**
 * One row of the deletedBy facet: a username and the count of trash entries deleted by that user.
 */
export interface DeletedByEntry {
  /** Raw login principal; used as the filter value so filtering matches the stored deleted_by. */
  name: string;
  count: number;
  /** Friendly display name resolved from the principal, when available; used only as the filter label. */
  displayName?: string;
}

/**
 * Aggregated facet counts for the Trash sidebar. Counts are computed across the entire
 * trash_index table and are not affected by currently-applied filters or search.
 */
export interface FacetsResponse {
  entityTypes: Record<string, number>;
  formats: Record<string, number>;
  deletedBy: DeletedByEntry[];
}
