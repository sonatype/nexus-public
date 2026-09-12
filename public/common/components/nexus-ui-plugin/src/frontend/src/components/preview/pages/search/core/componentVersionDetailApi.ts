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

import Axios from 'axios';

import type {
  ComponentVersionDetail,
  ComponentVersionDetailRequest,
  GAAsset,
} from './search.types';

const SEARCH_ENDPOINT = '/service/rest/v1/search';

/**
 * Hard ceiling on pages followed for one version's detail.
 *
 * /v1/search has a fixed page size of 50 (SearchResource.java), and rows are
 * component/repository pairs, so one page covers a version present in up to 50
 * repositories. The cap makes the request count a compile-time constant rather than a
 * function of the data — which is the whole point of this ticket. A version spanning
 * more than 250 repositories is truncated; that is an accepted, documented limit.
 */
export const MAX_DETAIL_PAGES = 5;

interface RawAsset {
  id: string;
  repository?: string | null;
  path: string;
  downloadUrl: string;
  format?: string | null;
  contentType?: string | null;
  lastModified?: string | null;
  fileSize?: number | null;
  checksum?: Record<string, string> | null;
}

interface RawSearchItem {
  repository: string;
  assets?: RawAsset[] | null;
}

interface RawSearchResponse {
  items?: RawSearchItem[] | null;
  continuationToken?: string | null;
}

function toAsset(raw: RawAsset, itemRepository: string): GAAsset {
  const extension = (raw.path.split('.').pop() || 'jar').toLowerCase();
  return {
    id: raw.id,
    repository: raw.repository || itemRepository,
    path: raw.path,
    downloadUrl: raw.downloadUrl,
    format: raw.contentType || raw.format || 'application/octet-stream',
    extension,
    classifier: undefined,
    size: raw.fileSize ?? 0,
    contentType: raw.contentType || 'application/octet-stream',
    // No "now" fallback: a substituted timestamp sorts to the top and changes every reload.
    // null rather than '' — `new Date('')` is an Invalid Date that throws nothing, so an
    // empty-string sentinel renders as the literal "Invalid Date" and sorts as NaN.
    lastModified: raw.lastModified ?? null,
    checksums: raw.checksum || {},
  };
}

/**
 * Fetches one version's assets and the repositories holding it.
 *
 * `version` is an exact-match filter (DefaultSearchMappings), so the response contains
 * exactly this version's rows — one per repository holding it — never neighboring
 * versions. Bounded by MAX_DETAIL_PAGES regardless of how many versions the component
 * has; that independence is the requirement, not merely an optimisation.
 */
export async function fetchComponentVersionDetail(
  request: ComponentVersionDetailRequest,
): Promise<ComponentVersionDetail> {
  const params = new URLSearchParams();
  // No `q`: DefaultSearchMappings maps it to `keyword` with exactMatch=false, i.e. a tsvector
  // `@@ to_tsquery` predicate, and the comment above those mappings records that such a predicate
  // is exactly what makes idx_search_components_format_ns_name_version unusable. format + group +
  // name + version already identify the row set exactly, so `q` would only cost the index.
  if (request.name) params.set('name', request.name);
  if (request.format) params.set('format', request.format);
  if (request.group) params.set('group', request.group);
  // Sent unconditionally, including the '' that versionless formats (raw) carry. A blank filter
  // is not dropped: tokenize() turns it into ExactTerm(''), an exact `version = ''` predicate
  // matching precisely the versionless rows. Verified against a populated instance — blank
  // returns the raw component's one row and 0 maven rows, where a dropped filter would have
  // returned every maven row. Omitting it for '' would de-scope the query to the whole
  // format+group+name group and drop the fourth column of
  // idx_search_components_format_ns_name_version, for exactly the components the comment above
  // argues to keep it for.
  params.set('version', request.version);

  const assets: GAAsset[] = [];
  const repositories: string[] = [];
  const seenRepositories = new Set<string>();
  let lastUpdated: string | null = null;

  let continuationToken: string | null = null;
  let pages = 0;

  do {
    // Token goes through URLSearchParams rather than string interpolation: a '+' in a token
    // decodes server-side as a space, silently breaking pagination.
    const pageParams = new URLSearchParams(params);
    if (continuationToken) pageParams.set('continuationToken', continuationToken);
    const url = `${SEARCH_ENDPOINT}?${pageParams.toString()}`;

    const response = await Axios.get<RawSearchResponse>(url);
    const data = response.data ?? {};

    for (const item of data.items ?? []) {
      if (!seenRepositories.has(item.repository)) {
        seenRepositories.add(item.repository);
        repositories.push(item.repository);
      }
      for (const raw of item.assets ?? []) {
        assets.push(toAsset(raw, item.repository));
        // String comparison, not Date: /v1/search returns zero-padded ISO 8601, which sorts
        // lexicographically. A non-ISO format appearing here would need a parse instead.
        if (raw.lastModified && (lastUpdated === null || raw.lastModified > lastUpdated)) {
          lastUpdated = raw.lastModified;
        }
      }
    }

    continuationToken = data.continuationToken || null;
    pages += 1;
  } while (continuationToken && pages < MAX_DETAIL_PAGES);

  return { assets, repositories, lastUpdated };
}
