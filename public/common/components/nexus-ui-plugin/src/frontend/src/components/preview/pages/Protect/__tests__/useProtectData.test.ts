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

let mockIqEnabled = true;
let mockCanReadFw = true;
let mockCanHc = true;

jest.mock('../../browse/repository-list/useRepositoryList', () => ({
  isIqServerEnabled: () => mockIqEnabled,
  canReadFirewallStatus: () => mockCanReadFw,
  canUpdateHealthCheck: () => mockCanHc,
}));

const mockQuickActionsData = {
  repos: [
    {
      name: 'maven-proxy',
      format: 'maven2',
      type: 'proxy',
      protection: 'quarantine' as const,
      malwareCount: 1,
      malware: [],
      taskEnabled: true,
      rhcEnabled: true,
      rhcAnalyzing: false,
      rhcSecurityIssues: 3,
      rhcLicenseIssues: 1,
    },
    {
      name: 'npm-proxy',
      format: 'npm',
      type: 'proxy',
      protection: 'none' as const,
      malwareCount: 0,
      malware: [],
      taskEnabled: false,
      rhcEnabled: false,
      rhcAnalyzing: false,
      rhcSecurityIssues: null,
      rhcLicenseIssues: null,
    },
  ],
  loading: false,
  error: null,
  refetch: jest.fn(),
};

jest.mock('../../MalwareRisk/useQuickActionsData', () => ({
  useQuickActionsData: () => mockQuickActionsData,
}));

const mockHcSummary = {
  loading: false,
  error: null,
  enabledCount: 1,
  totalProxyCount: 2,
  unsupportedFormatProxyCount: 0,
  totalSecurityIssues: 3,
  totalLicenseIssues: 1,
  repos: [
    { repositoryName: 'maven-proxy', enabled: true, lastAnalyzedDate: 1700000000000 },
    { repositoryName: 'npm-proxy', enabled: false, lastAnalyzedDate: null },
  ],
  refetch: jest.fn(),
};

jest.mock('../../Welcome/useHealthCheckSummary', () => ({
  useHealthCheckSummary: () => mockHcSummary,
}));

jest.mock('../../MalwareRisk/useIqAudit', () => ({
  useIqAudit: (enabled: boolean) => ({
    counts: enabled ? { reposProtected: 1, reposInAudit: 0, reposUnprotected: 1 } : null,
    loading: false,
    error: null,
  }),
}));

jest.mock('../../../../../interface/api', () => {
  const iqCapabilitiesUrl = '/service/rest/v1/iq/capabilities';
  const fallback = {
    get: jest.fn((url: string) => {
      if (url === iqCapabilitiesUrl) {
        return Promise.resolve({
          connected: true,
          hasFirewall: true,
          hasLifecycle: false,
          url: 'https://iq.example.com',
        });
      }
      return Promise.resolve([]);
    }),
    post: jest.fn().mockResolvedValue(undefined),
    delete: jest.fn().mockResolvedValue(undefined),
  };
  return {
    __esModule: true,
    restClient: fallback,
    default: fallback,
    ENDPOINTS: {
      CAPABILITIES: '/service/rest/v1/capabilities',
      IQ_CAPABILITIES: iqCapabilitiesUrl,
    },
  };
});

jest.mock('../../../../../utils/firewallFormats', () => ({
  isFirewallSupportedFormat: (f: string) =>
    ['maven2', 'npm', 'docker', 'pypi', 'nuget'].includes(f),
}));

import { useProtectData } from '../useProtectData';

describe('useProtectData', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIqEnabled = true;
    mockCanReadFw = true;
    mockCanHc = true;
  });

  it('returns repos from useQuickActionsData', async () => {
    const { result } = renderHook(() => useProtectData());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.repos).toHaveLength(2);
    expect(result.current.repos[0].name).toBe('maven-proxy');
    expect(result.current.repos[1].name).toBe('npm-proxy');
  });

  it('computes hasFirewall from IQ + permission flags', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.hasFirewall).toBe(true);
    expect(result.current.hasIqConnection).toBe(true);
    expect(result.current.canUpdateHealthCheck).toBe(true);
  });

  it('sets hasFirewall false when IQ is disabled', async () => {
    mockIqEnabled = false;
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.hasFirewall).toBe(false);
    expect(result.current.hasIqConnection).toBe(false);
  });

  it('sets hasFirewall false when user cannot read firewall status', async () => {
    mockCanReadFw = false;
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.hasFirewall).toBe(false);
  });

  it('builds filterCounts from repos', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    const { filterCounts } = result.current;
    expect(filterCounts.formats.get('maven2')).toBe(1);
    expect(filterCounts.formats.get('npm')).toBe(1);
    expect(filterCounts.healthCheck.enabled).toBe(1);
    expect(filterCounts.healthCheck.disabled).toBe(1);
    expect(filterCounts.healthCheck.unsupported).toBe(0);
    expect(filterCounts.cleanup.audit).toBe(1);
    expect(filterCounts.cleanup.off).toBe(1);
  });

  it('builds lastAnalyzedByRepo map from hcSummary repos', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.lastAnalyzedByRepo.get('maven-proxy')).toBe(1700000000000);
    expect(result.current.lastAnalyzedByRepo.get('npm-proxy')).toBeNull();
  });

  it('returns iqAudit counts when IQ is connected', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.iqAudit.counts).toEqual({
      reposProtected: 1,
      reposInAudit: 0,
      reposUnprotected: 1,
    });
  });

  it('returns hcSummary data', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.hcSummary.enabledCount).toBe(1);
    expect(result.current.hcSummary.totalProxyCount).toBe(2);
  });

  it('defaults hcInstanceEnabled to true', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.hcInstanceEnabled).toBe(true);
  });

  it('returns a refetch function', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(typeof result.current.refetch).toBe('function');
  });

  it('fetches IQ capabilities for Protect overview', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.iqCapabilities).toEqual({
      connected: true,
      hasFirewall: true,
      hasLifecycle: false,
      url: 'https://iq.example.com',
    });
  });

  it('computes protection filter counts including unsupported formats', async () => {
    const { result } = renderHook(() => useProtectData());
    await waitFor(() => expect(result.current.loading).toBe(false));

    const protMap = result.current.filterCounts.protection;
    expect(protMap.get('quarantine')).toBe(1);
    expect(protMap.get('none')).toBe(1);
  });
});
