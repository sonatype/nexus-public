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
import { restClient, ENDPOINTS } from '../../../../../interface/api';
import { isMockMode } from '../../../config/featureFlags';

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

/**
 * Response of `GET /v1/iq/capabilities` (`IqCapabilitiesXo`). Shape is identical across
 * self-hosted pro, community, and cloud editions.
 */
interface IqCapabilitiesResponse {
  connected?: boolean;
  hasLifecycle?: boolean;
  hasFirewall?: boolean;
  url?: string;
  deploymentId?: string;
}

/**
 * Mutually exclusive outcomes of resolving a component's security posture. Every value maps
 * to exactly one rendered state, so the tab can never fall through to a blank render.
 *
 * - `idle`: no version selected yet; nothing has been requested.
 * - `checking`: the IQ capabilities request is in flight.
 * - `not-connected`: IQ is not configured, not reachable, or reports `connected: false`.
 * - `not-entitled`: IQ is connected but this instance has neither Lifecycle nor Firewall.
 * - `unsupported`: the capabilities endpoint is absent (404). `GET /v1/iq/**` is declared only
 *   in `private/` modules — `@ConditionalOnEdition(pro, community)` self-hosted and cloud — so
 *   Nexus Repository Core (OSS) ships this UI without ever shipping the endpoint. Permanent for
 *   the deployment, so not retryable.
 * - `forbidden`: the caller lacks `nexus:settings:read` (403), which every method on the IQ
 *   resource requires. Browsing and searching do not imply that privilege, so most users who can
 *   open this tab cannot read IQ settings. Permanent for the user, so not retryable.
 * - `unavailable`: the capabilities request failed in a way we cannot classify (recoverable).
 * - `no-evaluation-data`: IQ is connected and entitled, but no per-component evaluation data
 *   can be retrieved — see {@link useComponentSecurity} for why.
 * - `evaluated`: evaluation data is present in {@link UseComponentSecurityResult.data}.
 */
export type ComponentSecurityStatus =
  | 'idle'
  | 'checking'
  | 'not-connected'
  | 'not-entitled'
  | 'unsupported'
  | 'forbidden'
  | 'unavailable'
  | 'no-evaluation-data'
  | 'evaluated';

interface UseComponentSecurityOptions {
  gaId: string;
  version: string | null;
  /**
   * When false the hook performs no requests and stays `idle`. Lets a caller that already
   * receives this state from a parent keep calling the hook unconditionally, so React's
   * rules of hooks are not violated by an early return.
   */
  enabled?: boolean;
}

export interface UseComponentSecurityResult {
  data: ComponentSecurityData | null;
  loading: boolean;
  /**
   * Operator-facing message for a recoverable failure. Always one of this module's own
   * fixed strings — raw API error bodies and exception messages are never propagated here,
   * because this value is rendered directly in the UI.
   */
  error: string | null;
  /** `null` until the capabilities check resolves. */
  iqConnected: boolean | null;
  status: ComponentSecurityStatus;
  refetch: () => void;
}

/**
 * Shown when the IQ capabilities request fails. Deliberately generic: the underlying error
 * may carry an IQ Server URL, a stack trace, or an HTML error page, none of which belong in
 * the UI.
 */
const CAPABILITIES_ERROR_MESSAGE =
  'Unable to determine the IQ Server connection status. Check your connection and try again.';

/**
 * HTTP status of a failed request, or undefined when the request never got a response
 * (offline, DNS failure, timeout). Mirrors the shape read by `useArtifactList`, which
 * classifies 404/403 from its own commercial-only endpoint the same way.
 */
function httpStatusOf(err: unknown): number | undefined {
  return (err as { response?: { status?: number } } | null)?.response?.status;
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
 * Hook resolving IQ Server security posture for a specific component version.
 *
 * Connectivity and entitlements come from `GET /v1/iq/capabilities`, which returns the same
 * shape on self-hosted pro, community, and cloud. `GET /v1/iq` is deliberately not used: its
 * response type differs per edition (`IqConnectionXo` vs `MTIQTenantConnectionOutput`).
 *
 * No per-component evaluation request is made. No REST endpoint currently exposes component
 * evaluation data to Nexus One. The underlying evaluation capability already exists in-process
 * through `ComponentEvaluationService` and `ComponentPolicyEvaluationApi`; it is simply not
 * reachable from the browser. The Classic UI gets its data by proxying IQ's own widget requests
 * through `ClmHealthCheckResource`, which is Pro/Cloud-only and application-scoped, and
 * `/internal/ui/security-report/artifacts` is repository-scoped and carries counts only. Until
 * an endpoint exposes it, a connected and entitled instance resolves to `no-evaluation-data`
 * rather than to fabricated zero counts, which would misreport an unevaluated component as
 * having passed every policy.
 *
 * Mock mode ({@link isMockMode}) short-circuits to fixture data so the rendered
 * `evaluated` state remains developable.
 */
export function useComponentSecurity({
  gaId,
  version,
  enabled = true,
}: UseComponentSecurityOptions): UseComponentSecurityResult {
  const [data, setData] = useState<ComponentSecurityData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<ComponentSecurityStatus>('idle');
  const [fetchCount, setFetchCount] = useState(0);

  const refetch = useCallback(() => {
    setFetchCount((c) => c + 1);
  }, []);

  // fetchCount is an intentional refetch trigger, not read in the body — omitting it from the
  // dependency list is what previously made the "Try again" button a no-op. gaId is listed for
  // the same reason (see the note at the dependency array).
  // biome-ignore lint/correctness/useExhaustiveDependencies: intentional refetch trigger
  useEffect(() => {
    /*
     * Three distinct reasons to make no request, all landing on the same `idle` status:
     *  - enabled === false: the caller already holds this state and passes it in, so a second
     *    request would only duplicate the parent's.
     *  - version === null: no version resolved yet, so there is nothing to evaluate.
     *  - version === '': a versionless format (raw). IQ identifies a component by coordinates
     *    including a version, so an evaluation with version= has nothing to match. Sending it
     *    anyway would trade the tab's empty state for an error callout, which is strictly worse.
     *
     * Note the distinction between the last two: elsewhere in the detail page '' is a real
     * selected version and must not be treated as absent (see gaDetailMachine.shouldLoadAssets,
     * which deliberately tests `!== null`). It is only here, against IQ, that '' is unusable —
     * and GASecurityTab renders the two cases differently, so they are not interchangeable
     * upstream of this gate even though both resolve to `idle` here.
     */
    if (!(enabled && version)) {
      setData(null);
      setError(null);
      setStatus('idle');
      return;
    }

    if (isMockMode()) {
      setError(null);
      setData(MOCK_SECURITY_DATA);
      setStatus('evaluated');
      return;
    }

    let cancelled = false;

    const run = async () => {
      setStatus('checking');
      setError(null);
      setData(null);

      let capabilities: IqCapabilitiesResponse | null;
      try {
        capabilities = await restClient.get<IqCapabilitiesResponse>(ENDPOINTS.IQ_CAPABILITIES);
      } catch (err: unknown) {
        if (cancelled) return;
        const httpStatus = httpStatusOf(err);
        // 404 and 403 are permanent conditions, not transient ones: the endpoint is absent from
        // this deployment, or this user may never read IQ settings. Surfacing either as a
        // retryable error would offer a "Try again" button that can never succeed. Neither
        // justifies claiming IQ is unconfigured, so neither maps to `not-connected`.
        if (httpStatus === 404) {
          setStatus('unsupported');
          return;
        }
        if (httpStatus === 403) {
          setStatus('forbidden');
          return;
        }
        // Anything else — offline, timeout, 5xx — is genuinely unknown and worth retrying.
        setError(CAPABILITIES_ERROR_MESSAGE);
        setStatus('unavailable');
        return;
      }

      if (cancelled) return;

      if (!capabilities?.connected) {
        setStatus('not-connected');
        return;
      }

      if (!(capabilities.hasLifecycle || capabilities.hasFirewall)) {
        setStatus('not-entitled');
        return;
      }

      setStatus('no-evaluation-data');
    };

    run();

    return () => {
      cancelled = true;
    };
    // `gaId` is intentionally a dependency even though the effect body does not read it. The one
    // request made here is instance-wide (`/v1/iq/capabilities`), so re-running on component
    // navigation only repeats an identical call — deliberately accepted. It is listed because the
    // per-component evaluation request this hook is designed to make (see the module comment) is
    // keyed by `gaId`: without it here, navigating between two components that share a version
    // would leave the previous component's evaluation on screen. Do not remove it as a redundant
    // dependency.
  }, [gaId, version, enabled, fetchCount]);

  // `idle` while a version is selected and requests are enabled means the effect has not run
  // yet — work is pending, nothing has been resolved. Reporting that as "not loading" let the
  // tab render a resolved state (and the words "IQ Server is connected") for the first frame,
  // before any connectivity check had been made.
  const pending = status === 'idle' && enabled && Boolean(version);

  return {
    data,
    loading: status === 'checking' || pending,
    error,
    // `null` means "not determined". Only an affirmative capabilities response settles this, so
    // every unresolved and every permanently-blocked status stays null rather than false —
    // false renders "IQ Server Not Connected", which none of these justify.
    iqConnected:
      status === 'idle' ||
      status === 'checking' ||
      status === 'unavailable' ||
      status === 'unsupported' ||
      status === 'forbidden'
        ? null
        : status !== 'not-connected',
    status,
    refetch,
  };
}
