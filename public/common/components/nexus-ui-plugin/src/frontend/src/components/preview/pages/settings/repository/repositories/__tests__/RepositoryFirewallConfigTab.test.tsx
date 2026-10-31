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
import userEvent from '@testing-library/user-event';
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

const mockEnableHealthCheck = jest.fn().mockResolvedValue(undefined);
const mockDisableHealthCheck = jest.fn().mockResolvedValue(undefined);
jest.mock('../useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({
    enableHealthCheck: mockEnableHealthCheck,
    disableHealthCheck: mockDisableHealthCheck,
  }),
}));

// RepositoryFirewallConfigTab gates the Health Check enable/disable actions via the
// provider-independent ExtJS.usePermission (reads window.NX.Permissions.check directly),
// not the context-based usePermission — coreui never mounts a <PermissionsProvider>, so the
// context hook returns false for everyone (NEXUS-54212). Spy on the real ExtJS singleton and
// drive the healthcheck:update check per test; ExtJS.usePermission evaluates its getter
// synchronously at render.
import { ExtJS } from '../../../../../../../interface/ExtJS';
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

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
    // Default: user can update health check so pre-existing behavior is exercised.
    mockCheckPermission.mockReturnValue(true);
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

  it('shows Enabled (green) immediately after clicking Enable, not Analyzing', async () => {
    // Render with HC disabled initially
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    await waitFor(() => expect(screen.getByText('Enable Health Check')).toBeInTheDocument());

    await userEvent.click(screen.getByRole('button', { name: 'Enable Health Check' }));

    // Optimistic update must show "Enabled" (green), never "Analyzing…"
    await waitFor(() => expect(screen.getByText('Enabled')).toBeInTheDocument());
    expect(screen.queryByText('Analyzing…')).not.toBeInTheDocument();
  });

  // -------- Health-check write gating (NEXUS-54212) --------

  describe('health-check gating (NEXUS-54212)', () => {
    it('shows Enable Health Check with healthcheck:update', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      expect(await screen.findByRole('button', { name: /enable health check/i })).toBeInTheDocument();
    });

    it('hides Enable Health Check without healthcheck:update', async () => {
      mockCheckPermission.mockImplementation((p) => p !== 'nexus:healthcheck:update');
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      // Wait for the Health Check card to settle (Disabled status renders regardless of permission).
      await waitFor(() => expect(screen.getByText('Disabled')).toBeInTheDocument());
      expect(screen.queryByRole('button', { name: /enable health check/i })).not.toBeInTheDocument();
    });

    it('hides Disable Health Check without healthcheck:update when enabled', async () => {
      mockCheckPermission.mockImplementation((p) => p !== 'nexus:healthcheck:update');
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
      expect(screen.queryByRole('button', { name: /disable health check/i })).not.toBeInTheDocument();
    });
  });

  // -------- Protection-level write gating (NEXUS-54212) --------

  describe('protection-level gating (NEXUS-54212)', () => {
    it('shows the Protection level selector with repository-admin:edit', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      // The protection selector renders its mode buttons (Audit/Quarantine) once status settles.
      expect(await screen.findByRole('button', { name: 'Audit' })).toBeInTheDocument();
      expect(screen.getByText('Protection level')).toBeInTheDocument();
    });

    it('hides the Protection level selector without repository-admin:edit', async () => {
      mockCheckPermission.mockImplementation((p) => p !== 'nexus:repository-admin:*:*:edit');
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      // Wait for the Health Check card to settle so the Firewall card has finished rendering too.
      await waitFor(() => expect(screen.getByText('Disabled')).toBeInTheDocument());
      expect(screen.queryByText('Protection level')).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Audit' })).not.toBeInTheDocument();
    });
  });
});
