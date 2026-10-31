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

import { renderHook, waitFor, act } from '@testing-library/react';

import { useComponentSecurity } from '../useComponentSecurity';

/**
 * Only `restClient` is mocked. ENDPOINTS is deliberately the real module: the defect this
 * suite guards was a missing ENDPOINTS key, so a mocked registry would hide it again.
 */
jest.mock('../../../../../../interface/api', () => {
  const actual = jest.requireActual('../../../../../../interface/api');
  return {
    ...actual,
    restClient: { get: jest.fn() },
  };
});

jest.mock('../../../../config/featureFlags', () => ({
  isMockMode: () => false,
}));

import { restClient } from '../../../../../../interface/api';

const mockGet = restClient.get as jest.Mock;

const GA_ID = 'maven:org.example:lib';
const VERSION = '1.0.0';

const CAPABILITIES_URL = '/service/rest/v1/iq/capabilities';

const CONNECTED_ENTITLED = {
  connected: true,
  hasLifecycle: true,
  hasFirewall: false,
  url: 'https://iq.example.com',
  deploymentId: 'deployment-1',
};

describe('useComponentSecurity', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  // -------------------------------------------------------------------------
  // Regression: the original rendering failure
  // -------------------------------------------------------------------------
  describe('IQ capabilities request URL (NEXUS-54431 regression)', () => {
    it('requests the real, fully-defined capabilities path — never undefined', async () => {
      mockGet.mockResolvedValue(CONNECTED_ENTITLED);

      renderHook(() => useComponentSecurity({ gaId: GA_ID, version: VERSION }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const requestedUrl = mockGet.mock.calls[0][0];
      expect(requestedUrl).toBe(CAPABILITIES_URL);
      // The defect: an absent ENDPOINTS key resolved to `undefined`, so axios requested the
      // current document URL and every response parsed as "IQ not connected".
      expect(requestedUrl).toBeDefined();
      expect(String(requestedUrl)).not.toContain('undefined');
    });

    it('reports IQ as connected when capabilities say so (was always false before the fix)', async () => {
      mockGet.mockResolvedValue(CONNECTED_ENTITLED);

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.iqConnected).toBe(true));
      expect(result.current.status).toBe('no-evaluation-data');
    });
  });

  // -------------------------------------------------------------------------
  // State: idle
  // -------------------------------------------------------------------------
  describe('no version selected', () => {
    it('stays idle and issues no request', () => {
      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: null })
      );

      expect(result.current.status).toBe('idle');
      expect(result.current.loading).toBe(false);
      expect(result.current.data).toBeNull();
      expect(result.current.error).toBeNull();
      expect(mockGet).not.toHaveBeenCalled();
    });
  });

  // -------------------------------------------------------------------------
  // State: checking / loading
  // -------------------------------------------------------------------------
  describe('loading', () => {
    it('reports loading while the capabilities request is in flight', async () => {
      let resolveGet: (value: unknown) => void = () => {};
      mockGet.mockReturnValue(
        new Promise((resolve) => {
          resolveGet = resolve;
        })
      );

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.loading).toBe(true));
      expect(result.current.status).toBe('checking');
      // Connectivity is genuinely unknown mid-flight, so it must not read as "not connected".
      expect(result.current.iqConnected).toBeNull();

      await act(async () => {
        resolveGet(CONNECTED_ENTITLED);
      });

      await waitFor(() => expect(result.current.loading).toBe(false));
    });
  });

  // -------------------------------------------------------------------------
  // State: not connected
  // -------------------------------------------------------------------------
  describe('IQ not connected', () => {
    it('resolves to not-connected when capabilities report connected: false', async () => {
      mockGet.mockResolvedValue({ connected: false, hasLifecycle: false, hasFirewall: false });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('not-connected'));
      expect(result.current.iqConnected).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('resolves to not-connected on an empty response body', async () => {
      mockGet.mockResolvedValue(null);

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('not-connected'));
      expect(result.current.data).toBeNull();
    });

    it('resolves to not-connected on an unexpected response shape', async () => {
      mockGet.mockResolvedValue({ unexpected: 'payload' });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('not-connected'));
      expect(result.current.error).toBeNull();
    });
  });

  // -------------------------------------------------------------------------
  // State: not entitled
  // -------------------------------------------------------------------------
  describe('IQ connected without entitlement', () => {
    it('resolves to not-entitled when neither Lifecycle nor Firewall is present', async () => {
      mockGet.mockResolvedValue({ connected: true, hasLifecycle: false, hasFirewall: false });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('not-entitled'));
      expect(result.current.iqConnected).toBe(true);
      expect(result.current.data).toBeNull();
      expect(result.current.error).toBeNull();
    });

    it('accepts Firewall alone as an entitlement', async () => {
      mockGet.mockResolvedValue({ connected: true, hasLifecycle: false, hasFirewall: true });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
    });
  });

  // -------------------------------------------------------------------------
  // State: no evaluation data
  // -------------------------------------------------------------------------
  describe('connected and entitled', () => {
    it('never fabricates zero counts when no evaluation data can be retrieved', async () => {
      mockGet.mockResolvedValue(CONNECTED_ENTITLED);

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
      // A zero-count object here would render as "passed all active policies" for a
      // component that was never evaluated.
      expect(result.current.data).toBeNull();
    });

    it('issues exactly one request — the removed component-evaluation call is not attempted', async () => {
      mockGet.mockResolvedValue(CONNECTED_ENTITLED);

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(mockGet.mock.calls.every(([url]) => !String(url).includes('component-evaluation'))).toBe(
        true
      );
    });
  });

  // -------------------------------------------------------------------------
  // State: unavailable (recoverable failure)
  // -------------------------------------------------------------------------
  describe('capabilities request failure', () => {
    it('resolves to unavailable with a fixed message, not the raw error', async () => {
      mockGet.mockRejectedValue(
        new Error('Request failed with status code 500: <html>Internal Server Error</html>')
      );

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('unavailable'));
      expect(result.current.error).toBe(
        'Unable to determine the IQ Server connection status. Check your connection and try again.'
      );
      expect(result.current.error).not.toContain('500');
      expect(result.current.error).not.toContain('html');
    });

    it('treats a response-less failure (offline, timeout) as recoverable', async () => {
      // No `response` property at all — axios shape for a request that never reached a server.
      mockGet.mockRejectedValue(new Error('Network Error'));

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('unavailable'));
      expect(result.current.error).not.toBeNull();
      expect(result.current.iqConnected).toBeNull();
    });

    it('treats a 500 as recoverable', async () => {
      mockGet.mockRejectedValue({ response: { status: 500, data: '<html>oops</html>' } });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('unavailable'));
      expect(result.current.error).not.toBeNull();
    });

    it('does not leak a raw string rejection value', async () => {
      mockGet.mockRejectedValue('connect ECONNREFUSED 10.0.0.5:8070');

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.error).not.toBeNull());
      expect(result.current.error).not.toContain('ECONNREFUSED');
      expect(result.current.error).not.toContain('10.0.0.5');
    });
  });

  // -------------------------------------------------------------------------
  // Permanent conditions: endpoint absent, or caller not permitted
  // -------------------------------------------------------------------------
  describe('permanent conditions are not reported as recoverable failures', () => {
    it('resolves a 404 to unsupported with no retryable error', async () => {
      // Nexus Repository Core ships this UI but no `/v1/iq` resource — every one is declared in
      // a `private/` module gated by @ConditionalOnEdition(pro, community).
      mockGet.mockRejectedValue({ response: { status: 404, data: 'Not Found' } });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('unsupported'));
      // No error string, so the UI offers no "Try again" that could never succeed.
      expect(result.current.error).toBeNull();
      // Not `false`: an absent endpoint says nothing about whether IQ is configured.
      expect(result.current.iqConnected).toBeNull();
      expect(result.current.loading).toBe(false);
      expect(result.current.data).toBeNull();
    });

    it('resolves a 403 to forbidden with no retryable error', async () => {
      // Every IQ resource method requires `nexus:settings:read`; browsing and searching do not
      // grant it, so most users who can open this tab land here.
      mockGet.mockRejectedValue({ response: { status: 403, data: 'Forbidden' } });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('forbidden'));
      expect(result.current.error).toBeNull();
      // 403 means the user cannot read IQ settings, not that IQ is absent.
      expect(result.current.iqConnected).toBeNull();
      expect(result.current.loading).toBe(false);
    });

    it.each([404, 403])('never leaks the raw %i response body', async (httpStatus) => {
      mockGet.mockRejectedValue({
        response: { status: httpStatus, data: '<html>https://iq.internal.example.com</html>' },
      });

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).not.toBe('checking'));
      expect(result.current.error).toBeNull();
    });
  });

  // -------------------------------------------------------------------------
  // Pending work must never read as resolved (initial idle state)
  // -------------------------------------------------------------------------
  describe('idle with pending work', () => {
    it('reports loading before the capabilities request has been issued', () => {
      mockGet.mockReturnValue(new Promise(() => {}));

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      // Whether the very first frame is `idle` or already `checking` is an implementation
      // detail; what matters is that nothing reads as resolved.
      expect(result.current.loading).toBe(true);
      expect(result.current.iqConnected).toBeNull();
      expect(result.current.data).toBeNull();
    });

    it('does not report loading when idle because there is nothing to do', () => {
      const noVersion = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: null })
      );
      expect(noVersion.result.current.status).toBe('idle');
      expect(noVersion.result.current.loading).toBe(false);

      const disabled = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION, enabled: false })
      );
      expect(disabled.result.current.status).toBe('idle');
      expect(disabled.result.current.loading).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // refetch
  // -------------------------------------------------------------------------
  describe('refetch', () => {
    it('re-runs the capabilities request and can recover from a failure', async () => {
      mockGet.mockRejectedValueOnce(new Error('boom'));

      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION })
      );

      await waitFor(() => expect(result.current.status).toBe('unavailable'));

      mockGet.mockResolvedValue(CONNECTED_ENTITLED);
      act(() => {
        result.current.refetch();
      });

      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
      expect(result.current.error).toBeNull();
      expect(mockGet).toHaveBeenCalledTimes(2);
    });
  });

  // -------------------------------------------------------------------------
  // Component navigation
  // -------------------------------------------------------------------------
  describe('component navigation', () => {
    /**
     * `gaId` is a dependency of the effect even though nothing in the effect body reads it yet.
     * Today re-running only repeats an identical instance-wide capabilities call; the reason it
     * matters is the per-component evaluation request the hook is built for, which is keyed by
     * `gaId`. Two components sharing a version must not share a resolved state.
     */
    it('re-runs when gaId changes while version stays the same', async () => {
      mockGet.mockResolvedValue(CONNECTED_ENTITLED);

      const { result, rerender } = renderHook(
        ({ gaId }: { gaId: string }) => useComponentSecurity({ gaId, version: VERSION }),
        { initialProps: { gaId: GA_ID } }
      );

      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
      expect(mockGet).toHaveBeenCalledTimes(1);

      rerender({ gaId: 'maven:org.example:other-lib' });

      await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));
      // The re-run passes back through `checking`, so wait for it to settle again.
      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
    });

    it('does not re-run when neither gaId nor version changes', async () => {
      mockGet.mockResolvedValue(CONNECTED_ENTITLED);

      const { result, rerender } = renderHook(
        ({ gaId }: { gaId: string }) => useComponentSecurity({ gaId, version: VERSION }),
        { initialProps: { gaId: GA_ID } }
      );

      await waitFor(() => expect(result.current.status).toBe('no-evaluation-data'));
      rerender({ gaId: GA_ID });

      expect(mockGet).toHaveBeenCalledTimes(1);
    });
  });

  // -------------------------------------------------------------------------
  // enabled: false
  // -------------------------------------------------------------------------
  describe('enabled: false', () => {
    it('issues no request when the caller already has this state', () => {
      const { result } = renderHook(() =>
        useComponentSecurity({ gaId: GA_ID, version: VERSION, enabled: false })
      );

      expect(mockGet).not.toHaveBeenCalled();
      expect(result.current.status).toBe('idle');
    });
  });
});
