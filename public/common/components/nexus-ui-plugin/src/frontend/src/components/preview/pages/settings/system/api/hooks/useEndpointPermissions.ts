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

import { useCallback, useEffect, useRef, useState } from 'react';

import { ENDPOINTS, restClient } from '../../../../../../../interface/api';

import type { ApiPermissionsResponseDto } from '../types';

export interface UseEndpointPermissionsResult {
  data: ApiPermissionsResponseDto | null;
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

let sessionCache: ApiPermissionsResponseDto | null = null;

/**
 * Loads {@code GET /service/rest/internal/ui/api/permissions} once per browser session (shared cache).
 */
export function useEndpointPermissions(): UseEndpointPermissionsResult {
  const [data, setData] = useState<ApiPermissionsResponseDto | null>(sessionCache);
  const [loading, setLoading] = useState(!sessionCache);
  const [error, setError] = useState<string | null>(null);
  const mounted = useRef(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await restClient.get<ApiPermissionsResponseDto>(ENDPOINTS.API_PERMISSIONS);
      sessionCache = response;
      if (mounted.current) {
        setData(response);
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Failed to load API permissions';
      if (mounted.current) {
        setError(message);
        setData(null);
      }
    } finally {
      if (mounted.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    mounted.current = true;
    if (sessionCache) {
      setData(sessionCache);
      setLoading(false);
      return () => {
        mounted.current = false;
      };
    }
    void load();
    return () => {
      mounted.current = false;
    };
  }, [load]);

  const refetch = useCallback(async () => {
    sessionCache = null;
    await load();
  }, [load]);

  return { data, loading, error, refetch };
}
