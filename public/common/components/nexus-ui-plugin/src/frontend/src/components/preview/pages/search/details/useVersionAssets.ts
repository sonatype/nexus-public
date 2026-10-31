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
 * useVersionAssets — fetches the assets for a single (version, repository) via SEARCH_ASSETS so
 * the header Download menu can offer API-provided downloadUrls without the expensive aggregate
 * walk performed by useGADetail. When repository is null, it searches all repositories serving
 * the version.
 */

import { useEffect, useRef, useState } from 'react';

import { ENDPOINTS, restClient } from '../../../../../interface/api';
import { deriveFileType, type GAAsset } from '../core';
import { parseGaId } from './gaIdUtils';

interface SearchAssetItem {
  readonly id: string;
  readonly repository: string;
  readonly format: string;
  readonly path: string;
  readonly downloadUrl: string;
  readonly contentType?: string;
  readonly lastModified?: string;
  readonly fileSize?: number;
  readonly checksum?: {
    readonly sha1?: string;
    readonly sha256?: string;
    readonly sha512?: string;
    readonly md5?: string;
  };
}

interface SearchAssetsResponse {
  readonly items: SearchAssetItem[];
  readonly continuationToken: string | null;
}

export interface UseVersionAssetsResult {
  readonly assets: readonly GAAsset[];
  readonly loading: boolean;
  readonly error: string | null;
}

/** Upper bound on continuation-token pages, so a malformed API response cannot loop forever. */
const MAX_ASSET_PAGES = 100;

function toGAAsset(item: SearchAssetItem): GAAsset {
  return {
    id: item.id,
    repository: item.repository,
    path: item.path,
    downloadUrl: item.downloadUrl,
    format: item.contentType || item.format || 'application/octet-stream',
    classifier: undefined,
    // Shares componentVersionDetailApi's derivation so the Download menu and the Files tab
    // can never disagree about a file's type. See fileType.ts.
    extension: deriveFileType({ path: item.path, format: item.format }),
    size: item.fileSize ?? 0,
    contentType: item.contentType || 'application/octet-stream',
    lastModified: item.lastModified || '',
    checksums: item.checksum || {},
  };
}

export function useVersionAssets({
  gaId,
  version,
  repository,
}: {
  gaId: string;
  version: string | null;
  repository: string | null;
}): UseVersionAssetsResult {
  const [assets, setAssets] = useState<readonly GAAsset[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const reqIdRef = useRef(0);

  useEffect(() => {
    const { format, group, name } = parseGaId(gaId);
    if (version === null || !format || !name) {
      setAssets([]);
      setLoading(false);
      setError(null);
      return;
    }

    const reqId = ++reqIdRef.current;
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.set('format', format);
    // Without the group, a Maven search matches every artifact that shares this name across
    // groups, so the Download menu can resolve to an asset from the wrong coordinate. Send it
    // when present (npm/raw have none), mirroring fetchComponentVersionDetail.
    if (group) {
      params.set('group', group);
    }
    params.set('name', name);
    params.set('version', version);
    if (repository) {
      params.set('repository', repository);
    }

    let cancelled = false;
    (async () => {
      try {
        let items: SearchAssetItem[] = [];
        let token: string | null = null;
        // Safety cap: a well-behaved API returns a null token to terminate the walk, but a
        // malformed one that always echoes a token must not spin forever. 100 pages is far beyond
        // any real component's asset count.
        let page = 0;
        do {
          // Clone and use .set so the token is percent-encoded. A continuationToken can contain
          // '+', which interpolated raw into the query string the server reads back as a space —
          // a corrupted token that breaks pagination. URLSearchParams encodes it correctly.
          const pageParams = new URLSearchParams(params);
          if (token) {
            pageParams.set('continuationToken', token);
          }
          const url = `${ENDPOINTS.SEARCH_ASSETS}?${pageParams.toString()}`;
          const res = await restClient.get<SearchAssetsResponse>(url);
          items = items.concat(res.items || []);
          token = res.continuationToken || null;
        } while (token && ++page < MAX_ASSET_PAGES);

        if (!cancelled && reqIdRef.current === reqId) {
          setAssets(items.map(toGAAsset));
          setLoading(false);
        }
      } catch (err) {
        if (!cancelled && reqIdRef.current === reqId) {
          setAssets([]);
          setError(err instanceof Error ? err.message : 'Failed to load assets');
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [gaId, version, repository]);

  return { assets, loading, error };
}

export default useVersionAssets;
