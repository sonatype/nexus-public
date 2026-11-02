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

/*
 * Radix Select (the protection-level dropdown, FIRE-958) relies on pointer-capture and scroll
 * APIs that jsdom does not implement. Matches the polyfill in BrowseFilterSidebar.test.tsx.
 */
beforeAll(() => {
  Element.prototype.hasPointerCapture = jest.fn(() => false);
  Element.prototype.setPointerCapture = jest.fn();
  Element.prototype.releasePointerCapture = jest.fn();
  Element.prototype.scrollIntoView = jest.fn();
});

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
    if (url.match(/\/internal\/ui\/repositories\/repository\/[^/]+$/)) {
      // internal lookup (setFirewallMode read-modify-write source)
      return Promise.resolve({ name: 'r', online: true, format, type: 'proxy', firewall: { mode } });
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

// Opens the protection-level dropdown (there is exactly one combobox on this component,
// the Health Check section has no Select) and returns the listbox. Waits for the initial
// status fetch to settle first — the component shows "Loading Firewall status…" until then.
async function openProtectionLevelSelect() {
  const trigger = await screen.findByTestId('protection-level-select');
  await userEvent.click(trigger);
  return screen.getByRole('listbox');
}

describe('RepositoryFirewallConfigTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    __resetPccsFormatsCacheForTests();
    mockTypedRepoLookup('maven2');
    // Default: user can update health check so pre-existing behavior is exercised.
    mockCheckPermission.mockReturnValue(true);
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

  it('does NOT render the PCCS option for non-PCCS formats (maven2)', async () => {
    mockTypedRepoLookup('maven2');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="maven-proxy"
        hasFirewallLicense={true}
        format="maven2"
      />
    );
    // Wait for status fetch to settle so the protection selector has rendered.
    await waitFor(() => expect(screen.getByTestId('protection-level-select')).toBeInTheDocument());
    await openProtectionLevelSelect();
    expect(screen.getByRole('option', { name: /^Audit Only/ })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /^PCCS/ })).not.toBeInTheDocument();
  });

  it('renders the PCCS option for npm proxies', async () => {
    mockTypedRepoLookup('npm');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="npm-proxy"
        hasFirewallLicense={true}
        format="npm"
      />
    );
    await waitFor(() => expect(screen.getByTestId('protection-level-select')).toBeInTheDocument());
    await openProtectionLevelSelect();
    expect(screen.getByRole('option', { name: /^PCCS/ })).toBeInTheDocument();
  });

  it('renders the PCCS option for pypi proxies', async () => {
    mockTypedRepoLookup('pypi');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="pypi-proxy"
        hasFirewallLicense={true}
        format="pypi"
      />
    );
    await waitFor(() => expect(screen.getByTestId('protection-level-select')).toBeInTheDocument());
    await openProtectionLevelSelect();
    expect(screen.getByRole('option', { name: /^PCCS/ })).toBeInTheDocument();
  });

  it('shows the Classic UI description on every protection level option', async () => {
    mockTypedRepoLookup('npm');
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="npm-proxy" hasFirewallLicense={true} format="npm" />
    );
    await waitFor(() => expect(screen.getByTestId('protection-level-select')).toBeInTheDocument());
    await openProtectionLevelSelect();
    expect(screen.getByRole('option', { name: 'None – do not use Firewall' })).toBeInTheDocument();
    expect(
      screen.getByRole('option', { name: 'Audit Only - allow components that violate policy' })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('option', { name: 'Quarantine - block components that violate policy' })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('option', {
        name: 'PCCS - Quarantine, plus metadata filtering to help clients select a policy compliance version',
      })
    ).toBeInTheDocument();
  });

  it('does NOT render PCCS when format prop is omitted (back-compat with pre-PCCS callers)', async () => {
    mockTypedRepoLookup('npm');
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="npm-proxy" hasFirewallLicense={true} />
    );
    await waitFor(() => expect(screen.getByTestId('protection-level-select')).toBeInTheDocument());
    await openProtectionLevelSelect();
    expect(screen.getByRole('option', { name: /^Audit Only/ })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /^PCCS/ })).not.toBeInTheDocument();
  });

  it('reflects an existing PCCS mode as the selected trigger value', async () => {
    mockTypedRepoLookup('npm', 'PCCS');
    renderWithTheme(
      <RepositoryFirewallConfigTab
        repositoryName="npm-proxy"
        hasFirewallLicense={true}
        format="npm"
      />
    );
    const trigger = await waitFor(() => screen.getByTestId('protection-level-select'));
    expect(trigger).toHaveTextContent('PCCS');
  });

  it('shows an error and disables the selector when the firewall mode fails to load, instead of silently presenting None as the real setting', async () => {
    // fetchIqAuditStatus resolves null (not a thrown rejection) when the basic repository
    // lookup is missing format/type — simulate that by having every GET resolve null.
    mockRestGet.mockImplementation(() => Promise.resolve(null));

    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    const trigger = await screen.findByTestId('protection-level-select');
    expect(screen.getByText(/Could not load the current Firewall mode/)).toBeInTheDocument();
    expect(trigger).toHaveAttribute('data-disabled');
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

  // -------- XET / Firewall interaction --------

  describe('XET disabled firewall', () => {
    it('shows XET warning when xetEnabled is true', async () => {
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="hf-proxy"
          hasFirewallLicense={true}
          xetEnabled={true}
        />
      );
      expect(
        await screen.findByText(/Firewall is disabled while XET is enabled/i),
      ).toBeInTheDocument();
    });

    it('disables the protection-level selector when xetEnabled is true', async () => {
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="hf-proxy"
          hasFirewallLicense={true}
          xetEnabled={true}
        />
      );
      const select = await screen.findByTestId('protection-level-select');
      expect(select).toBeDisabled();
    });

    it('shows None instead of the saved mode when xetEnabled is true', async () => {
      mockTypedRepoLookup('huggingface', 'AUDIT');
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="hf-proxy"
          hasFirewallLicense={true}
          xetEnabled={true}
        />
      );
      const select = await screen.findByTestId('protection-level-select');
      expect(select).toHaveTextContent(/none/i);
      expect(select).not.toHaveTextContent(/audit/i);
    });

    it('does not show XET warning when xetEnabled is false', async () => {
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="hf-proxy"
          hasFirewallLicense={true}
          xetEnabled={false}
        />
      );
      await screen.findByTestId('protection-level-select');
      expect(
        screen.queryByText(/Firewall is disabled while XET is enabled/i),
      ).not.toBeInTheDocument();
    });

    it('shows XET warning even when user lacks edit permission', async () => {
      mockCheckPermission.mockImplementation((p) => p !== 'nexus:repository-admin:*:*:edit');
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="hf-proxy"
          hasFirewallLicense={true}
          xetEnabled={true}
        />
      );
      expect(
        await screen.findByText(/Firewall is disabled while XET is enabled/i),
      ).toBeInTheDocument();
      expect(screen.queryByText('Protection level')).not.toBeInTheDocument();
    });
  });

  // -------- Protection-level write gating (NEXUS-54212) --------

  describe('protection-level gating (NEXUS-54212)', () => {
    it('shows the Protection level selector with repository-admin:edit', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      // The protection selector renders as a dropdown once status settles.
      expect(await screen.findByTestId('protection-level-select')).toBeInTheDocument();
      expect(screen.getByText('Protection level')).toBeInTheDocument();
    });

    it('exposes an accessible name on the dropdown via aria-labelledby, not just visible text', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      // A combobox's accessible name can't come from its own content (it shows the selected
      // value, not a label) — this only passes if aria-labelledby wires it to the "Protection
      // level" text.
      expect(await screen.findByRole('combobox', { name: 'Protection level' })).toBeInTheDocument();
    });

    it('hides the Protection level selector without repository-admin:edit', async () => {
      mockCheckPermission.mockImplementation((p) => p !== 'nexus:repository-admin:*:*:edit');
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      // Wait for the Health Check card to settle so the Firewall card has finished rendering too.
      await waitFor(() => expect(screen.getByText('Disabled')).toBeInTheDocument());
      expect(screen.queryByText('Protection level')).not.toBeInTheDocument();
      expect(screen.queryByTestId('protection-level-select')).not.toBeInTheDocument();
    });

    // FIRE-958: a repository-view-only (read-only) user has no repository-admin:read, so the
    // typed GET fetchIqAuditStatus relies on (`/v1/repositories/<format>/<type>/<name>`) 403s and
    // statusError is always true for them. The selector is already correctly hidden for this user
    // via canEditFirewall above — the bug was that the "Could not load the current Firewall mode"
    // banner rendered unconditionally regardless of canEditFirewall, so a read-only user saw a
    // scary red error about a control they were never shown and can never use.
    it('does not show the "could not load Firewall mode" error without repository-admin:edit, even if the status fetch fails', async () => {
      mockCheckPermission.mockImplementation((p) => p !== 'nexus:repository-admin:*:*:edit');
      mockRestGet.mockImplementation(() => Promise.resolve(null));
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      await waitFor(() => expect(screen.getByText('Disabled')).toBeInTheDocument());
      expect(screen.queryByText(/Could not load the current Firewall mode/)).not.toBeInTheDocument();
      expect(screen.queryByTestId('protection-level-select')).not.toBeInTheDocument();
    });
  });

  // -------- Save/Cancel wiring (FIRE-958) --------
  //
  // Selecting a protection level must only stage a pending change; it must not call the
  // server. The server call only happens when the exposed save handler (onSaveRef) is invoked
  // — mirroring the parent form's Save Changes button — and can be discarded via onCancelRef
  // (mirroring Cancel). None/Quarantine additionally require confirming a dialog before the
  // save handler actually commits.

  describe('Save/Cancel wiring (FIRE-958)', () => {
    it('does not call the server when a protection level is selected — only Save commits it', async () => {
      renderWithTheme(
        <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: /^Audit Only/ }));

      expect(mockRestPut).not.toHaveBeenCalled();
      expect(screen.getByTestId('protection-level-select')).toHaveTextContent('Audit');
      expect(screen.getByText(/Unsaved change/)).toBeInTheDocument();
    });

    it('reports dirty via onDirtyChange after selecting a different level, and clean again after a successful save', async () => {
      const onDirtyChange = jest.fn();
      const saveRef = { current: null as (() => Promise<void>) | null };
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="maven-proxy"
          hasFirewallLicense={true}
          onDirtyChange={onDirtyChange}
          onSaveRef={saveRef}
        />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: /^Audit Only/ }));

      expect(onDirtyChange).toHaveBeenCalledWith(true);

      await waitFor(() => expect(saveRef.current).not.toBeNull());
      await saveRef.current!();

      await waitFor(() => expect(mockRestPut).toHaveBeenCalled());
      await waitFor(() => expect(onDirtyChange).toHaveBeenLastCalledWith(false));
    });

    it('commits Audit immediately on save with no confirmation dialog', async () => {
      const saveRef = { current: null as (() => Promise<void>) | null };
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="maven-proxy"
          hasFirewallLicense={true}
          onSaveRef={saveRef}
        />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: /^Audit Only/ }));

      await waitFor(() => expect(saveRef.current).not.toBeNull());
      await saveRef.current!();

      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
      await waitFor(() =>
        expect(mockRestPut).toHaveBeenCalledWith(
          expect.stringMatching(/\/v1\/repositories\/[^/]+\/[^/]+\/[^/]+$/),
          expect.objectContaining({ firewall: { mode: 'AUDIT' } })
        )
      );
    });

    it('prompts for confirmation before committing Quarantine, and does not PUT until confirmed', async () => {
      const saveRef = { current: null as (() => Promise<void>) | null };
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="maven-proxy"
          hasFirewallLicense={true}
          onSaveRef={saveRef}
        />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: /^Quarantine/ }));

      await waitFor(() => expect(saveRef.current).not.toBeNull());
      await saveRef.current!();

      expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
      expect(mockRestPut).not.toHaveBeenCalled();

      await userEvent.click(screen.getByTestId('firewall-quarantine-confirm-confirm'));

      await waitFor(() =>
        expect(mockRestPut).toHaveBeenCalledWith(
          expect.stringMatching(/\/v1\/repositories\/[^/]+\/[^/]+\/[^/]+$/),
          expect.objectContaining({ firewall: { mode: 'QUARANTINE' } })
        )
      );
    });

    it('prompts for confirmation before committing None, and leaves the pending selection intact if cancelled', async () => {
      mockTypedRepoLookup('maven2', 'AUDIT');
      const saveRef = { current: null as (() => Promise<void>) | null };
      const onDirtyChange = jest.fn();
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="maven-proxy"
          hasFirewallLicense={true}
          onSaveRef={saveRef}
          onDirtyChange={onDirtyChange}
        />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: 'None – do not use Firewall' }));

      await waitFor(() => expect(saveRef.current).not.toBeNull());
      await saveRef.current!();

      expect(await screen.findByRole('alertdialog')).toBeInTheDocument();

      await userEvent.click(screen.getByTestId('firewall-disable-confirm-cancel'));

      expect(mockRestPut).not.toHaveBeenCalled();
      // The pending "None" selection survives a cancelled confirm dialog — the user still has
      // to explicitly discard via the parent form's Cancel (onCancelRef), not just close the
      // dialog.
      expect(screen.getByTestId('protection-level-select')).toHaveTextContent('None');
    });

    it('discards the pending selection via the exposed cancel handler without calling the server', async () => {
      mockTypedRepoLookup('maven2', 'AUDIT');
      const cancelRef = { current: null as (() => void) | null };
      const onDirtyChange = jest.fn();
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="maven-proxy"
          hasFirewallLicense={true}
          onCancelRef={cancelRef}
          onDirtyChange={onDirtyChange}
        />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: /^Quarantine/ }));

      expect(screen.getByTestId('protection-level-select')).toHaveTextContent('Quarantine');
      expect(onDirtyChange).toHaveBeenCalledWith(true);

      await waitFor(() => expect(cancelRef.current).not.toBeNull());
      cancelRef.current!();

      expect(mockRestPut).not.toHaveBeenCalled();
      await waitFor(() => expect(screen.getByTestId('protection-level-select')).toHaveTextContent('Audit'));
      expect(onDirtyChange).toHaveBeenLastCalledWith(false);
    });

    it('is a no-op on save when the selected level matches the already-committed level', async () => {
      mockTypedRepoLookup('maven2', 'AUDIT');
      const saveRef = { current: null as (() => Promise<void>) | null };
      const onDirtyChange = jest.fn();
      renderWithTheme(
        <RepositoryFirewallConfigTab
          repositoryName="maven-proxy"
          hasFirewallLicense={true}
          onSaveRef={saveRef}
          onDirtyChange={onDirtyChange}
        />
      );
      await openProtectionLevelSelect();
      await userEvent.click(screen.getByRole('option', { name: /^Audit Only/ }));

      expect(onDirtyChange).toHaveBeenCalledWith(false);

      await waitFor(() => expect(saveRef.current).not.toBeNull());
      await saveRef.current!();

      expect(mockRestPut).not.toHaveBeenCalled();
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
  });
});
