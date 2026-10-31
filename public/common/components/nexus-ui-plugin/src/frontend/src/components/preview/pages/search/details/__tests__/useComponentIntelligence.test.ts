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

import { renderHook, waitFor } from '@testing-library/react';

import ExtAPIUtils from '../../../../../../interface/ExtAPIUtils';
import {
  formatComponentAge,
  formatComponentPopularity,
  licenseThreatSeverity,
  useComponentIntelligence,
} from '../useComponentIntelligence';

const mockExtAPIRequest = jest.spyOn(ExtAPIUtils, 'extAPIRequest');

/** Ext.Direct success envelope carrying one HealthCheckXO. */
function envelope(healthCheck: Record<string, unknown> | null) {
  return { data: { result: { success: true, data: [{ healthCheck }] } } };
}

const MAVEN = {
  gaId: 'maven2:org.apache.commons:commons-lang3',
  format: 'maven2',
  version: '3.19.0',
};

describe('formatComponentAge', () => {
  // The cutoff is Classic's `dayAge > 364`, so 365 days is the first value shown in years.
  it('renders a day count up to and including the 364-day cutoff', () => {
    expect(formatComponentAge(364 * 86400000)).toBe('364 d');
  });

  it('renders years past the cutoff, to one decimal place', () => {
    expect(formatComponentAge(365 * 86400000)).toBe('1.0 yrs');
    expect(formatComponentAge(730 * 86400000)).toBe('2.0 yrs');
  });

  it('renders a zero age as zero days rather than as no data', () => {
    expect(formatComponentAge(0)).toBe('0 d');
  });

  it('has no rendering for a missing or nonsensical age', () => {
    expect(formatComponentAge(null)).toBeNull();
    expect(formatComponentAge(-1)).toBeNull();
  });
});

describe('formatComponentPopularity', () => {
  it('renders a percentage, preserving a real zero', () => {
    expect(formatComponentPopularity(0)).toBe('0 %');
    expect(formatComponentPopularity(6)).toBe('6 %');
  });

  it('clamps above 100, as Classic does', () => {
    expect(formatComponentPopularity(140)).toBe('100 %');
  });

  it('has no rendering for a missing popularity', () => {
    expect(formatComponentPopularity(null)).toBeNull();
  });

  // Classic's guard is `popularity === 0 || popularity > 0`, so a negative value or sentinel is
  // unavailable rather than rendered as `-1 %`.
  it('has no rendering for a negative popularity', () => {
    expect(formatComponentPopularity(-1)).toBeNull();
  });
});

describe('licenseThreatSeverity', () => {
  // These are an upstream enum, not a scale: anything that is not 4, 2 or 1 means severe.
  it('maps the upstream threat-level enum', () => {
    expect(licenseThreatSeverity(4)).toBe('critical');
    expect(licenseThreatSeverity(2)).toBe('moderate');
    expect(licenseThreatSeverity(1)).toBe('none');
    expect(licenseThreatSeverity(3)).toBe('severe');
    expect(licenseThreatSeverity(0)).toBe('severe');
  });
});

describe('useComponentIntelligence', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockExtAPIRequest.mockReturnValue(new Promise(() => {}));
  });

  it('resolves every signal the Ext.Direct response carries', async () => {
    mockExtAPIRequest.mockResolvedValue(
      envelope({
        mostPopularVersion: '3.12.0',
        age: 730 * 86400000,
        popularity: 6,
        securityAlerts: 1,
        criticalSecurityAlerts: 0,
        severeSecurityAlerts: 1,
        moderateSecurityAlerts: 0,
        licenseThreat: 1,
        licenseThreatName: 'Apache-2.0',
        cappedAlerts: false,
      }),
    );

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    await waitFor(() => expect(result.current.status).toBe('resolved'));
    expect(result.current.data).toEqual({
      mostPopularVersion: '3.12.0',
      ageMillis: 730 * 86400000,
      popularity: 6,
      securityAlerts: 1,
      criticalSecurityAlerts: 0,
      severeSecurityAlerts: 1,
      moderateSecurityAlerts: 0,
      licenseThreat: 1,
      licenseThreatName: 'Apache-2.0',
      cappedAlerts: false,
    });
  });

  it('resolves absent fields to null so a caller can tell them from a real zero', async () => {
    mockExtAPIRequest.mockResolvedValue(envelope({ age: 0, popularity: 0 }));

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    await waitFor(() => expect(result.current.status).toBe('resolved'));
    expect(result.current.data).toMatchObject({
      // Present and zero.
      ageMillis: 0,
      popularity: 0,
      // Absent upstream — "not evaluated", not "no findings".
      securityAlerts: null,
      criticalSecurityAlerts: null,
      licenseThreat: null,
      licenseThreatName: null,
      mostPopularVersion: null,
    });
  });

  it.each([
    ['disabled', { disabled: true }],
    ['error', { error: true }],
    ['capped', { capped: true }],
  ] as const)('reports the %s precondition and carries no data', async (status, info) => {
    // Precedence matters: each flag is set alongside real values here, and Classic reports the
    // precondition regardless.
    mockExtAPIRequest.mockResolvedValue(envelope({ ...info, mostPopularVersion: '3.12.0', popularity: 6 }));

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    await waitFor(() => expect(result.current.status).toBe(status));
    expect(result.current.data).toBeNull();
  });

  it('reports an error when the Ext.Direct call fails', async () => {
    mockExtAPIRequest.mockRejectedValue(new Error('network down'));

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.data).toBeNull();
  });

  it('reports an error when the response carries no healthCheck at all', async () => {
    mockExtAPIRequest.mockResolvedValue(envelope(null));

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    await waitFor(() => expect(result.current.status).toBe('error'));
  });

  it('sends the component array as the single Ext.Direct argument', async () => {
    mockExtAPIRequest.mockResolvedValue(envelope({ popularity: 6 }));

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    await waitFor(() => expect(result.current.status).toBe('resolved'));
    expect(mockExtAPIRequest).toHaveBeenCalledWith('healthcheck_Info', 'read', {
      data: [[{ group: 'org.apache.commons', name: 'commons-lang3', version: '3.19.0', format: 'maven2' }]],
    });
  });

  it('requests nothing for a format the backend cannot build coordinates for', () => {
    const { result } = renderHook(() =>
      useComponentIntelligence({ gaId: 'raw:some/path', format: 'raw', version: '1.0.0' }),
    );

    expect(result.current.status).toBe('unsupported-format');
    expect(mockExtAPIRequest).not.toHaveBeenCalled();
  });

  it('requests nothing without nexus:healthcheck:read', () => {
    (global as unknown as { NX: { Permissions: { check: jest.Mock } } }).NX.Permissions.check.mockReturnValue(false);

    const { result } = renderHook(() => useComponentIntelligence(MAVEN));

    expect(result.current.status).toBe('forbidden');
    expect(mockExtAPIRequest).not.toHaveBeenCalled();
  });

  it('requests nothing while there is no version to identify the component by', () => {
    const { result } = renderHook(() => useComponentIntelligence({ ...MAVEN, version: '' }));

    expect(result.current.status).toBe('idle');
    expect(mockExtAPIRequest).not.toHaveBeenCalled();
  });

  // The option is typed `string | null`, so both falsy versions have to reach the same guard: a
  // component whose version has not resolved yet arrives as null, not ''.
  it('requests nothing while the version is null', () => {
    const { result } = renderHook(() => useComponentIntelligence({ ...MAVEN, version: null }));

    expect(result.current.status).toBe('idle');
    expect(mockExtAPIRequest).not.toHaveBeenCalled();
  });

  it('requests nothing while disabled by the caller', () => {
    const { result } = renderHook(() => useComponentIntelligence({ ...MAVEN, enabled: false }));

    expect(result.current.status).toBe('idle');
    expect(mockExtAPIRequest).not.toHaveBeenCalled();
  });

  /*
   * The alert fields are subject to Classic's proui precondition ladder — entitlement, then
   * nexus:healthcheckalerts:update, then the alert quota — and the backend fills them in for any
   * caller holding nexus:healthcheck:read, so the hook has to report that verdict separately.
   */
  describe('alertsStatus', () => {
    const nx = () =>
      (global as unknown as {
        NX: { Permissions: { check: jest.Mock }; State: { getValue: jest.Mock; getEdition: jest.Mock } };
      }).NX;

    /** Publishes a `license` state value, as ExtJS does once the state store has loaded. */
    function licenseState(license: Record<string, unknown> | undefined) {
      nx().State.getValue.mockImplementation((key: string) => (key === 'license' ? license : undefined));
    }

    // clearMocks clears recorded calls but not implementations, so these overrides must be undone.
    afterEach(() => {
      nx().State.getEdition.mockReset();
      nx().State.getValue.mockReset();
      nx().Permissions.check.mockReset();
      nx().Permissions.check.mockReturnValue(true);
    });

    it('is available for an entitled, permitted caller within quota', async () => {
      licenseState({ installed: true, valid: true });
      mockExtAPIRequest.mockResolvedValue(envelope({ licenseThreat: 1, licenseThreatName: 'Apache-2.0' }));

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      await waitFor(() => expect(result.current.status).toBe('resolved'));
      expect(result.current.alertsStatus).toBe('available');
    });

    it('is unlicensed on an EVAL edition, taking precedence over the permission', () => {
      licenseState({ installed: true, valid: true });
      nx().State.getEdition.mockReturnValue('EVAL');

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      expect(result.current.alertsStatus).toBe('unlicensed');
    });

    // The edition check runs ahead of the license read, so an EVAL instance is unlicensed even
    // before the state store has published anything — no license state could entitle it.
    it('is unlicensed on EVAL even while the license state is still unknown', () => {
      nx().State.getEdition.mockReturnValue('EVAL');

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      expect(result.current.alertsStatus).toBe('unlicensed');
    });

    it('is unlicensed when the license is installed but not valid', () => {
      licenseState({ installed: true, valid: false });

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      expect(result.current.alertsStatus).toBe('unlicensed');
    });

    /*
     * Fails closed, matching NX.State.isLicenseValid(), which is false unless installed and valid
     * are both explicitly true. Reported as `unknown` rather than `unlicensed` so a transient
     * pre-initialization state is not presented as a settled entitlement verdict — but either way
     * the caller shows nothing.
     */
    it('is unknown while the state store has published no license at all', () => {
      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      expect(result.current.alertsStatus).toBe('unknown');
    });

    it.each([
      ['an empty license state', {}],
      ['a license state carrying only valid', { valid: true }],
    ] as const)('is unknown for %s rather than treating it as entitlement', (_label, license) => {
      licenseState(license as Record<string, unknown>);

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      expect(result.current.alertsStatus).toBe('unknown');
    });

    it('is forbidden without nexus:healthcheckalerts:update, even with nexus:healthcheck:read', async () => {
      licenseState({ installed: true, valid: true });
      nx().Permissions.check.mockImplementation((p: string) => p !== 'nexus:healthcheckalerts:update');
      mockExtAPIRequest.mockResolvedValue(envelope({ popularity: 6 }));

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      // The base permission is unaffected, so the response still resolves.
      await waitFor(() => expect(result.current.status).toBe('resolved'));
      expect(result.current.alertsStatus).toBe('forbidden');
    });

    it('is capped when only the alert quota is exhausted, leaving the other signals resolved', async () => {
      licenseState({ installed: true, valid: true });
      mockExtAPIRequest.mockResolvedValue(envelope({ popularity: 6, age: 0, cappedAlerts: true }));

      const { result } = renderHook(() => useComponentIntelligence(MAVEN));

      await waitFor(() => expect(result.current.alertsStatus).toBe('capped'));
      // cappedAlerts is alert-specific: unlike `capped`, it does not invalidate the response.
      expect(result.current.status).toBe('resolved');
      expect(result.current.data).toMatchObject({ popularity: 6, ageMillis: 0 });
    });
  });

  it('does not let a slower earlier response overwrite a newer version', async () => {
    let resolveFirst: (value: unknown) => void = () => {};
    mockExtAPIRequest
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveFirst = resolve;
        }) as never,
      )
      .mockResolvedValueOnce(envelope({ mostPopularVersion: 'from-second-request' }));

    const { result, rerender } = renderHook(
      (props: { version: string }) => useComponentIntelligence({ ...MAVEN, version: props.version }),
      { initialProps: { version: '3.19.0' } },
    );

    rerender({ version: '3.20.0' });
    await waitFor(() => expect(result.current.status).toBe('resolved'));

    resolveFirst(envelope({ mostPopularVersion: 'from-first-request' }));
    await waitFor(() => expect(result.current.data?.mostPopularVersion).toBe('from-second-request'));
  });
});
