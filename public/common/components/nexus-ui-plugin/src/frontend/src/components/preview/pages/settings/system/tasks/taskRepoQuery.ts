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

import { TaskRepoFilter } from './taskFieldMetadata';

/**
 * Map a descriptor's `storeApi` (RepositoryCombobox.getStoreApi) to the internal endpoint's
 * synthetic-entry flags. `readReferencesAddingEntryForAll` => a "(All Repositories)" entry;
 * `readReferencesAddingEntriesForAllFormats` => that plus per-format "(All <format>)" entries.
 */
export function deriveAllEntries(storeApi: string | undefined): { withAll: boolean; withFormats: boolean } {
  if (!storeApi) return { withAll: false, withFormats: false };
  if (storeApi.endsWith('readReferencesAddingEntriesForAllFormats')) return { withAll: true, withFormats: true };
  if (storeApi.endsWith('readReferencesAddingEntryForAll')) return { withAll: true, withFormats: false };
  return { withAll: false, withFormats: false };
}

/**
 * Build the querystring for GET /service/rest/internal/ui/repositories from a repository field's
 * backend-shipped storeApi + storeFilters. storeFilters values are passed through verbatim (they
 * are already comma-separated with `!` excludes, which the endpoint's include/exclude parser
 * understands). Returns the querystring WITHOUT a leading `?`.
 */
export function buildRepoQuery(
  storeApi: string | undefined,
  storeFilters: Record<string, string> | undefined,
): string {
  const params = new URLSearchParams();
  const sf = storeFilters ?? {};
  // Only the four filter keys the internal /repositories endpoint understands are forwarded.
  // `regardlessViewPermissions` (emitted by some Classic descriptors) is deliberately omitted: it
  // has no equivalent at this endpoint, which always applies the caller's browse permissions.
  if (sf.facets) params.set('facets', sf.facets);
  if (sf.versionPolicies) params.set('versionPolicies', sf.versionPolicies);
  if (sf.format) params.set('format', sf.format);
  if (sf.type) params.set('type', sf.type);
  const { withAll, withFormats } = deriveAllEntries(storeApi);
  if (withAll) params.set('withAll', 'true');
  if (withFormats) params.set('withFormats', 'true');
  return params.toString();
}

/**
 * Fallback for OSS/older builds whose task template omits storeApi/storeFilters: derive the same
 * querystring from a static TASK_TYPE_REPO_FILTERS entry. Preview consumes the descriptor's
 * storeApi/storeFilters whenever present; this only runs when they are absent.
 */
export function queryFromStaticFilter(filter: TaskRepoFilter | undefined): string {
  const params = new URLSearchParams();
  if (!filter) return params.toString();
  if (filter.facets?.length) params.set('facets', filter.facets.join(','));
  if (filter.versionPolicies?.length) params.set('versionPolicies', filter.versionPolicies.join(','));
  if (filter.formats?.length) params.set('format', filter.formats.join(','));
  if (filter.types?.length) params.set('type', filter.types.join(','));
  if (filter.includeAll) params.set('withAll', 'true');
  return params.toString();
}
