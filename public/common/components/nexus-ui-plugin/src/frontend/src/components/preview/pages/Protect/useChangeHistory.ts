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

import { useState, useEffect, useCallback } from 'react';
import { restClient, ENDPOINTS } from '../../../../interface/api';

export interface ProtectAuditEvent {
  type: string;
  context: string;
  timestamp: string | null;
  initiator: string | null;
  attributes: Record<string, unknown> | null;
}

export interface ProtectAuditEventsResponse {
  events: ProtectAuditEvent[];
  totalCount: number;
}

export function useChangeHistory(
  domain: string | null,
  context: string | null,
  enabled: boolean,
  limit = 3
) {
  const [data, setData] = useState<ProtectAuditEventsResponse>({ events: [], totalCount: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchEvents = useCallback(async () => {
    if (!enabled || !domain || !context) {
      setData({ events: [], totalCount: 0 });
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        domain,
        context,
        limit: String(Math.min(Math.max(limit, 1), 10)),
      });
      const res = await restClient.get<ProtectAuditEventsResponse>(
        `${ENDPOINTS.PROTECT_AUDIT_EVENTS}?${params.toString()}`
      );
      setData({
        events: Array.isArray(res?.events) ? res.events : [],
        totalCount: typeof res?.totalCount === 'number' ? res.totalCount : 0,
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setData({ events: [], totalCount: 0 });
    } finally {
      setLoading(false);
    }
  }, [domain, context, enabled, limit]);

  useEffect(() => {
    void fetchEvents();
  }, [fetchEvents]);

  return { ...data, loading, error, refetch: fetchEvents };
}
