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

import { useEffect, useState } from 'react';

import { ENDPOINTS, restClient } from '@/utils/api';

import type { ApiAccessCheckResponseDto } from '../types';
import type { EndpointAccessDot } from '../utils/endpointAccess';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';
import { endpointRowId } from '../EndpointList';

const BATCH = 14;

export interface UseViewAsUserAccessResult {
  accessById: Record<string, EndpointAccessDot> | null;
  loading: boolean;
  error: string | null;
}

/**
 * When {@code viewAsUserId} differs from the signed-in user, loads per-endpoint access via
 * POST /internal/ui/security/access-check (admin-only for other users).
 */
export function useViewAsUserAccess(
  endpoints: MergedApiEndpoint[],
  viewAsUserId: string | null,
  currentUserId: string | null,
  enabled: boolean
): UseViewAsUserAccessResult {
  const [accessById, setAccessById] = useState<Record<string, EndpointAccessDot> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const needsServer =
    enabled &&
    !!viewAsUserId &&
    !!currentUserId &&
    viewAsUserId !== currentUserId;

  useEffect(() => {
    if (!needsServer) {
      setAccessById(null);
      setLoading(false);
      setError(null);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setAccessById(null);

    (async () => {
      const map: Record<string, EndpointAccessDot> = {};
      if (endpoints.length === 0) {
        if (!cancelled) {
          setAccessById({});
        }
        return;
      }
      for (let i = 0; i < endpoints.length; i += BATCH) {
        if (cancelled) {
          return;
        }
        const slice = endpoints.slice(i, i + BATCH);
        const batch = await Promise.all(
          slice.map(async (row) => {
            const id = endpointRowId(row);
            try {
              const res = await restClient.post<ApiAccessCheckResponseDto>(ENDPOINTS.SECURITY_ACCESS_CHECK, {
                userId: viewAsUserId,
                endpoint: row.fullPath,
                method: row.httpMethod,
              });
              return { id, dot: (res.hasAccess ? 'granted' : 'denied') as EndpointAccessDot };
            } catch {
              return { id, dot: 'unknown' as EndpointAccessDot };
            }
          })
        );
        for (const { id, dot } of batch) {
          map[id] = dot;
        }
      }
      if (!cancelled) {
        setAccessById(map);
      }
    })()
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Access check failed');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [needsServer, viewAsUserId, currentUserId, endpoints]);

  return { accessById: needsServer ? accessById : null, loading: needsServer && loading, error };
}
