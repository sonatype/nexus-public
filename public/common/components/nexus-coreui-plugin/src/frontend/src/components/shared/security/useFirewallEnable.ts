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

import { useState, useCallback } from 'react';
import { restClient, ENDPOINTS } from '@/utils/api';
import { parseApiError } from '@/utils/api';

export interface IqAuditStatus {
  repositoryName: string;
  enabled: boolean;
  enabledQuarantine: boolean;
}

/**
 * Enable Firewall in Audit-only mode (monitors, no blocking).
 * PUT /service/rest/v1/iq/audit { repositoryName, enabled: true, enabledQuarantine: false }
 */
export async function enableFirewallAudit(
  repositoryName: string
): Promise<void> {
  await restClient.put(ENDPOINTS.IQ_AUDIT, {
    repositoryName,
    enabled: true,
    enabledQuarantine: false,
  });
}

/**
 * Enable Firewall in Quarantine mode (monitors and blocks policy violations).
 * PUT /service/rest/v1/iq/audit { repositoryName, enabled: true, enabledQuarantine: true }
 */
export async function enableFirewallQuarantine(
  repositoryName: string
): Promise<void> {
  await restClient.put(ENDPOINTS.IQ_AUDIT, {
    repositoryName,
    enabled: true,
    enabledQuarantine: true,
  });
}

/**
 * Disable Firewall (None / no protection).
 * PUT /service/rest/v1/iq/audit { repositoryName, enabled: false, enabledQuarantine: false }
 */
export async function disableFirewall(repositoryName: string): Promise<void> {
  await restClient.put(ENDPOINTS.IQ_AUDIT, {
    repositoryName,
    enabled: false,
    enabledQuarantine: false,
  });
}

/**
 * Fetch audit status for a repository.
 * GET /service/rest/v1/iq/audit/{repositoryName}
 */
export async function fetchIqAuditStatus(
  repositoryName: string
): Promise<IqAuditStatus | null> {
  try {
    const data = await restClient.get<IqAuditStatus>(
      ENDPOINTS.IQ_AUDIT_REPO(repositoryName)
    );
    return data ?? null;
  } catch {
    return null;
  }
}

/**
 * Hook for enabling Firewall (Audit or Quarantine) on a repository.
 * Provides enable functions and loading/error state.
 */
export function useFirewallEnable(repositoryName: string) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const enableAudit = useCallback(
    async (onSuccess?: () => void) => {
      setLoading(true);
      setError(null);
      try {
        await enableFirewallAudit(repositoryName);
        onSuccess?.();
      } catch (err) {
        const apiError = parseApiError(err);
        setError(apiError.message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [repositoryName]
  );

  const enableQuarantine = useCallback(
    async (onSuccess?: () => void) => {
      setLoading(true);
      setError(null);
      try {
        await enableFirewallQuarantine(repositoryName);
        onSuccess?.();
      } catch (err) {
        const apiError = parseApiError(err);
        setError(apiError.message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [repositoryName]
  );

  const disable = useCallback(
    async (onSuccess?: () => void) => {
      setLoading(true);
      setError(null);
      try {
        await disableFirewall(repositoryName);
        onSuccess?.();
      } catch (err) {
        const apiError = parseApiError(err);
        setError(apiError.message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [repositoryName]
  );

  return { enableAudit, enableQuarantine, disable, loading, error };
}
