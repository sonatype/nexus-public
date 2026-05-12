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
import { restClient, ENDPOINTS } from '@/utils/api';
import { isMockMode } from '@/config/previewFeatureFlags';

export interface ConstraintViolation {
  constraintName: string;
  reasons: string[];
}

export interface PolicyViolation {
  policyName: string;
  threatLevel: number;
  constraintViolations: ConstraintViolation[];
}

export interface ComponentSecurityData {
  criticalCount: number;
  severeCount: number;
  moderateCount: number;
  lowCount: number;
  violations: PolicyViolation[];
  reportUrl?: string;
  evaluationDate?: string;
  policyEvaluationId?: string;
}

interface IqConnectionResponse {
  enabled: boolean;
  url?: string;
}

interface UseComponentSecurityOptions {
  gaId: string;
  version: string | null;
}

export interface UseComponentSecurityResult {
  data: ComponentSecurityData | null;
  loading: boolean;
  error: string | null;
  iqConnected: boolean | null;
  refetch: () => void;
}

/** Mock security data for development (maven-core and others when isMockMode) */
const MOCK_SECURITY_DATA: ComponentSecurityData = {
  criticalCount: 1,
  severeCount: 2,
  moderateCount: 3,
  lowCount: 4,
  reportUrl: 'https://example.com/iq-report',
  evaluationDate: '2024-01-15T10:00:00Z',
  policyEvaluationId: 'mock-eval-123',
  violations: [
    {
      policyName: 'Security-Critical',
      threatLevel: 10,
      constraintViolations: [
        {
          constraintName: 'Security-Vulnerability',
          reasons: ['CVE-2023-12345: Remote code execution vulnerability in dependency'],
        },
      ],
    },
    {
      policyName: 'Security-High',
      threatLevel: 8,
      constraintViolations: [
        {
          constraintName: 'Security-Vulnerability',
          reasons: ['CVE-2023-67890: SQL injection in transitive dependency'],
        },
      ],
    },
    {
      policyName: 'License-GPL',
      threatLevel: 5,
      constraintViolations: [
        {
          constraintName: 'License',
          reasons: ['Component uses GPL-3.0 license which may have copyleft implications'],
        },
      ],
    },
  ],
};

/**
 * Parse the gaId format "format:group:name" into its parts.
 * e.g. "maven:org.apache.logging.log4j:log4j-core"
 */
function parseGaId(gaId: string): { format: string; group: string; name: string } {
  const parts = gaId.split(':');
  return {
    format: parts[0] ?? 'maven',
    group: parts[1] ?? '',
    name: parts[2] ?? '',
  };
}

/**
 * Hook for fetching IQ Server security/policy violation data for a specific
 * component version. Checks IQ connection first; only fetches component
 * evaluation when IQ is enabled.
 *
 * States:
 * - version === null: returns immediately, no API calls
 * - iqConnected === false: IQ not configured, do not fetch
 * - loading === true: fetching in progress
 * - data !== null: violations data available (may be zero counts = clean)
 * - error !== null: network or server failure
 */
export function useComponentSecurity({ gaId, version }: UseComponentSecurityOptions): UseComponentSecurityResult {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<ComponentSecurityData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [iqConnected, setIqConnected] = useState<boolean | null>(null);
  const [fetchCount, setFetchCount] = useState(0);

  const refetch = useCallback(() => {
    setFetchCount((c) => c + 1);
  }, []);

  useEffect(() => {
    if (!version) {
      setData(null);
      setError(null);
      setIqConnected(null);
      setLoading(false);
      return;
    }

    if (isMockMode()) {
      setLoading(false);
      setError(null);
      setIqConnected(true);
      setData(MOCK_SECURITY_DATA);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setError(null);
      setData(null);

      // Step 1: Check IQ connection
      try {
        const iqStatus = await restClient.get<IqConnectionResponse>(ENDPOINTS.IQ_CONNECTION);
        if (cancelled) return;

        if (!iqStatus?.enabled) {
          setIqConnected(false);
          setLoading(false);
          return;
        }

        setIqConnected(true);
      } catch {
        if (cancelled) return;
        // Cannot reach IQ check endpoint — treat as not connected
        setIqConnected(false);
        setLoading(false);
        return;
      }

      // Step 2: Fetch component evaluation
      const { format, group, name } = parseGaId(gaId);
      const params = new URLSearchParams({ format, group, name, version });
      const url = `${ENDPOINTS.IQ_COMPONENT_EVALUATION}?${params.toString()}`;

      try {
        const result = await restClient.get<ComponentSecurityData>(url);
        if (cancelled) return;

        // Null / empty response: connected but no evaluation data — treat as clean (zero violations)
        if (!result) {
          setData({
            criticalCount: 0,
            severeCount: 0,
            moderateCount: 0,
            lowCount: 0,
            violations: [],
          });
        } else {
          setData(result);
        }
      } catch (err: unknown) {
        if (cancelled) return;

        // 404 = component not yet evaluated — treat as clean with no data
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 404) {
          setData({
            criticalCount: 0,
            severeCount: 0,
            moderateCount: 0,
            lowCount: 0,
            violations: [],
          });
        } else {
          const message =
            err instanceof Error
              ? err.message
              : 'Unable to contact IQ Server. Check your connection and try again.';
          setError(message);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    run();

    return () => {
      cancelled = true;
    };
  }, [gaId, version, fetchCount]); // eslint-disable-line react-hooks/exhaustive-deps

  return { data, loading, error, iqConnected, refetch };
}
