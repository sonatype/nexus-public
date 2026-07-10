/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark
 * of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

const mockRestGet = jest.fn();
const mockRestPut = jest.fn();
jest.mock('../../../../../../../interface/api', () => {
  const actual = jest.requireActual<typeof import('../../../../../../../interface/api')>('../../../../../../../interface/api');
  return {
    ...actual,
    restClient: {
      ...actual.restClient,
      get: (...args: unknown[]) => mockRestGet(...args),
      put: (...args: unknown[]) => mockRestPut(...args),
    },
  };
});

jest.mock('../useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({
    enableHealthCheck: jest.fn(),
    disableHealthCheck: jest.fn(),
  }),
}));

import { RepositoryFirewallConfigTab } from '../RepositoryFirewallConfigTab';
import { __resetPccsFormatsCacheForTests } from '../../../../../shared/security/useFirewallEnable';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

// Default mock chain. Covers every endpoint any test in this suite needs:
//   - GET /service/rest/internal/ui/healthcheck    -> array health-check status (used by RepositoryFirewallConfigTab to render the Health Check card)
//   - GET /v1/repositories/<name>/health-check     -> per-repo health-check status (legacy endpoint still referenced in some flows)
//   - GET .../iq/audit                             -> IQ audit status
//   - GET .../firewall/format-capabilities         -> PCCS-capable formats
//   - GET /v1/repositories/<format>/<type>/<name>  -> typed body (used by fetchIqAuditStatus + setFirewallMode)
//   - GET /v1/repositories/<name>                  -> basic lookup ({ format, type })
// Individual tests can call `mockRestGet.mockImplementationOnce` (or replace the implementation) before
// rendering when they need a non-default response (e.g. healthcheck rejecting, healthcheck enabled, etc).
function mockTypedRepoLookup(format: string, mode: 'DISABLED' | 'AUDIT' | 'QUARANTINE' | 'PCCS' = 'DISABLED') {
  mockRestGet.mockImplementation((url: string) => {
    if (typeof url !== 'string') return Promise.resolve(null);
    if (url.includes('/healthcheck')) {
      return Promise.resolve([{ repositoryName: 'maven-proxy', enabled: false }]);
    }
    if (url.includes('health-check')) return Promise.resolve({ enabled: false });
    if (url.includes('iq/audit')) {
      return Promise.resolve({ repositoryName: 'maven-proxy', enabled: false, enabledQuarantine: false });
    }
    if (url.endsWith('firewall/format-capabilities')) {
      return Promise.resolve([
        { format: 'npm', pccsModeSupported: true },
        { format: 'pypi', pccsModeSupported: true },
      ]);
    }
    if (url.match(/\/v1\/repositories\/[^/]+\/[^/]+\/[^/]+$/)) {
      // typed lookup
      return Promise.resolve({ name: 'r', online: true, firewall: { mode } });
    }
    if (url.match(/\/v1\/repositories\/[^/]+$/)) {
      // basic lookup
      return Promise.resolve({ format, type: 'proxy' });
    }
    return Promise.resolve(null);
  });
}

describe('RepositoryFirewallConfigTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    __resetPccsFormatsCacheForTests();
    mockTypedRepoLookup('maven2');
  });

  it('renders audit trail placeholder in Firewall card', async () => {
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    const auditTexts = await waitFor(() =>
      screen.getAllByText(/Audit trail: Configuration change history will be available in a future release/)
    );
    expect(auditTexts).toHaveLength(1);
  });

  it('shows Disabled status and Enable button when health check is off', async () => {
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    await waitFor(() => expect(screen.getByText('Disabled')).toBeInTheDocument());
    expect(screen.getByText('Enable Health Check')).toBeInTheDocument();
  });

  it('shows Enabled status and Disable button when health check is on', async () => {
    mockRestGet.mockImplementation((url: string) => {
      if (typeof url === 'string' && url.includes('/healthcheck')) {
        return Promise.resolve([{ repositoryName: 'maven-proxy', enabled: true, analyzing: false }]);
      }
      if (typeof url === 'string' && url.includes('iq/audit')) {
        return Promise.resolve({ repositoryName: 'maven-proxy', enabled: false, enabledQuarantine: false });
      }
      return Promise.resolve(null);
    });

    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    await waitFor(() => expect(screen.getByText('Enabled')).toBeInTheDocument());
    expect(screen.getByText('Disable Health Check')).toBeInTheDocument();
  });

  it('queries the internal-UI healthcheck list endpoint, not the per-repo POST/DELETE path', async () => {
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    await waitFor(() => expect(mockRestGet).toHaveBeenCalled());
    const calledUrls = mockRestGet.mock.calls.map((c) => c[0]);
    expect(calledUrls).toEqual(
      expect.arrayContaining([expect.stringMatching(/\/service\/rest\/internal\/ui\/healthcheck$/)])
    );
    expect(calledUrls).not.toEqual(
      expect.arrayContaining([expect.stringMatching(/\/repositories\/[^/]+\/health-check$/)])
    );
  });

  it('filters the healthcheck array by repositoryName and ignores other repos', async () => {
    mockRestGet.mockImplementation((url: string) => {
      if (typeof url === 'string' && url.includes('/healthcheck')) {
        return Promise.resolve([
          { repositoryName: 'other-proxy', enabled: true, analyzing: false },
          { repositoryName: 'maven-proxy', enabled: true, analyzing: true },
        ]);
      }
      if (typeof url === 'string' && url.includes('iq/audit')) {
        return Promise.resolve({ repositoryName: 'maven-proxy', enabled: false, enabledQuarantine: false });
      }
      return Promise.resolve(null);
    });

    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    await waitFor(() => expect(screen.getByText('Analyzing…')).toBeInTheDocument());
  });

  it('renders Health Check title and description as block elements (no inline run-on)', async () => {
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    const title = await waitFor(() => screen.getByText('Repository Health Check'));
    const description = screen.getByText(
      /Repository Health Check analyzes components for security vulnerabilities and license issues\./
    );
    expect(title.tagName.toLowerCase()).toBe('div');
    expect(description.tagName.toLowerCase()).toBe('div');
  });

  it('does not issue a healthcheck GET when showHealthCheck is false', async () => {
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="maven-proxy"
        hasFirewallLicense={true}
        showHealthCheck={false}
      />
    );

    await waitFor(() => expect(mockRestGet).toHaveBeenCalled());
    const calledUrls = mockRestGet.mock.calls.map((c) => c[0]);
    expect(calledUrls.every((url: unknown) => typeof url !== 'string' || !url.includes('/healthcheck'))).toBe(true);
  });

  it('renders Disabled state without crashing when the healthcheck GET rejects', async () => {
    mockRestGet.mockImplementation((url: string) => {
      if (typeof url === 'string' && url.includes('/healthcheck')) {
        return Promise.reject(new Error('Network error'));
      }
      if (typeof url === 'string' && url.includes('iq/audit')) {
        return Promise.resolve({ repositoryName: 'maven-proxy', enabled: false, enabledQuarantine: false });
      }
      return Promise.resolve(null);
    });

    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    await waitFor(() => expect(screen.getByText('Disabled')).toBeInTheDocument());
    expect(screen.getByText('Enable Health Check')).toBeInTheDocument();
  });

  // -------- PCCS protection-selector tests (STL-381) --------

  it('does NOT render the PCCS button for non-PCCS formats (maven2)', async () => {
    mockTypedRepoLookup('maven2');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="maven-proxy"
        hasFirewallLicense={true}
        format="maven2"
      />
    );
    // Wait for status fetch to settle so the protection selector has rendered.
    await waitFor(() => expect(screen.getByRole('button', { name: 'Audit' })).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'PCCS' })).not.toBeInTheDocument();
  });

  it('renders the PCCS button for npm proxies', async () => {
    mockTypedRepoLookup('npm');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="npm-proxy"
        hasFirewallLicense={true}
        format="npm"
      />
    );
    await waitFor(() => expect(screen.getByRole('button', { name: 'PCCS' })).toBeInTheDocument());
  });

  it('renders the PCCS button for pypi proxies', async () => {
    mockTypedRepoLookup('pypi');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="pypi-proxy"
        hasFirewallLicense={true}
        format="pypi"
      />
    );
    await waitFor(() => expect(screen.getByRole('button', { name: 'PCCS' })).toBeInTheDocument());
  });

  it('does NOT render PCCS when format prop is omitted (back-compat with pre-PCCS callers)', async () => {
    mockTypedRepoLookup('npm');
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="npm-proxy" hasFirewallLicense={true} />
    );
    await waitFor(() => expect(screen.getByRole('button', { name: 'Audit' })).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'PCCS' })).not.toBeInTheDocument();
  });

  it('reflects an existing PCCS mode by selecting the PCCS button (aria-pressed)', async () => {
    mockTypedRepoLookup('npm', 'PCCS');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="npm-proxy"
        hasFirewallLicense={true}
        format="npm"
      />
    );
    const pccsBtn = await waitFor(() => screen.getByRole('button', { name: 'PCCS' }));
    expect(pccsBtn).toHaveAttribute('aria-pressed', 'true');
  });
});
