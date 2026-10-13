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

import React from 'react';
import { render, screen, fireEvent, waitFor, cleanup, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

// UX-16: mock useIsCloud so tests can flip cloud/self-hosted edition.
jest.mock('../../../../../shared/hooks/useIsCloud', () => ({
  useIsCloud: jest.fn(() => false),
}));

// UIRouter navigation — captured via mockGo for assertion.
// useCurrentStateAndParams defaults to iqConnected (dialog closed).
const mockGo = jest.fn();
const mockUseCurrentStateAndParams = jest.fn(() => ({
  state: { name: 'preview.admin.iqConnected' },
  params: {},
}));
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: mockGo } }),
  useCurrentStateAndParams: (...args: any[]) => mockUseCurrentStateAndParams(...args),
}));

// NEXUS-53610 replaced the useIqServerApi hook with standalone async helpers.
// The dialog now imports fetchIqSettings/saveIqSettings from iqServerFormMachine
// and verifyConnection from iqConnectionMachine. Mock those directly so the
// Save-pre-verify tests can override verify/save without touching the machines.
// Keep toFormData/toUpdatePayload as their real implementations — they're pure
// converters the dialog uses on the boundary and re-implementing them here would
// duplicate logic that could drift.
jest.mock('../iqServerFormMachine', () => {
  const actual = jest.requireActual('../iqServerFormMachine');
  return {
    ...actual,
    fetchIqSettings: jest.fn(),
    putIqSettings: jest.fn(),
    disableIqServer: jest.fn(),
    saveIqSettings: jest.fn(),
  };
});
jest.mock('../iqConnectionMachine', () => {
  const actual = jest.requireActual('../iqConnectionMachine');
  return {
    ...actual,
    verifyConnection: jest.fn(),
    fetchCapabilities: jest.fn(),
  };
});

// The on-page enable/disable control is gated on nexus:settings:update.
// coreui never mounts a <PermissionsProvider>, so spy on the ExtJS singleton (same
// approach as IqServerOverviewPage.test.tsx) and drive the check per test.
import { ExtJS } from '../../../../../../../interface/ExtJS';
import Permissions from '../../../../../../../constants/Permissions';
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

import { IqServerConnectedPage } from '../IqServerConnectedPage';
import {
  clearFreshIqConfigCache,
  clearPendingDisconnect,
  setFreshIqConfigCache,
} from '../iqServerStateCache';
import * as useIqConnectedApiModule from '../useIqConnectedApi';
import * as iqServerFormMachineModule from '../iqServerFormMachine';
import * as iqConnectionMachineModule from '../iqConnectionMachine';

afterEach(() => {
  // Explicit React cleanup first so Radix effect-cleanups run inside act().
  // Then purge any Radix portals/focus-guards that survive (they are appended
  // directly to document.body via DOM APIs, outside any React fiber, so RTL's
  // own cleanup never removes them).
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
  // Reset module-level caches so tests don't pollute each other.
  clearFreshIqConfigCache();
  clearPendingDisconnect();
});

// Helper to render with Radix Theme wrapper
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

// Default mock returns — adjust per-test by overriding mockReturnValue
function mockApi(overrides: Partial<ReturnType<typeof useIqConnectedApiModule.useIqConnectedApi>> = {}) {
  const defaults = {
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchIq: jest.fn().mockResolvedValue({
      enabled: true,
      showLink: true,
      url: 'http://localhost:8070',
      authenticationType: 'USER' as const,
      username: 'admin',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [
        { id: 'lifecycle', url: 'http://localhost:8070/ui/links/lifecycle/dashboard' },
        { id: 'firewall', url: 'http://localhost:8070/ui/links/firewall/dashboard' },
      ],
      hasFirewall: true,
    }),
    verifyConnection: jest.fn().mockResolvedValue({ success: true }),
    fetchDashboardSummary: jest.fn().mockResolvedValue({
      numberOfMonitoredRepositories: 0,
      totalRepositories: 54,
      globalConfigAvailable: false,
      hasSelections: false,
    }),
    fetchEvaluationSettings: jest.fn().mockResolvedValue(null),
  };
  jest.spyOn(useIqConnectedApiModule, 'useIqConnectedApi').mockReturnValue({
    ...defaults,
    ...overrides,
  });
}

// The dialog uses standalone helpers (post NEXUS-53610). Wire the module-level
// mocks with defaults; per-test overrides call `mockResolvedValueOnce(...)` on
// the specific helper.
const fetchIqSettingsMock = iqServerFormMachineModule.fetchIqSettings as jest.Mock;
const putIqSettingsMock = iqServerFormMachineModule.putIqSettings as jest.Mock;
const disableIqServerMock = iqServerFormMachineModule.disableIqServer as jest.Mock;
const verifyIqConnectionMock = iqConnectionMachineModule.verifyConnection as jest.Mock;
const fetchCapabilitiesMock = iqConnectionMachineModule.fetchCapabilities as jest.Mock;

/**
 * Reset the dialog-layer mocks to a happy-path baseline. fetchIqSettings
 * returns IqServerFormData (properties array) — dialog immediately converts to
 * wire shape via toUpdatePayload. `overrides` accepts partial return values for
 * any of the helpers so individual tests can flip one without redeclaring all.
 */
function mockDialogHelpers(overrides: {
  fetchIqSettings?: any;
  verifyConnection?: any;
} = {}) {
  fetchIqSettingsMock.mockResolvedValue(overrides.fetchIqSettings ?? {
    enabled: false,
    showLink: true,
    url: '',
    authenticationType: 'USER' as const,
    username: '',
    password: '',
    useTrustStoreForUrl: false,
    timeoutSeconds: null,
    properties: [],
    propertiesDroppedLineCount: 0,
    failOpenModeEnabled: false,
  });
  putIqSettingsMock.mockResolvedValue(undefined);
  disableIqServerMock.mockResolvedValue(undefined);
  verifyIqConnectionMock.mockResolvedValue(overrides.verifyConnection ?? { success: true });
  fetchCapabilitiesMock.mockResolvedValue({});
}

/**
 * Publish the `clm` rapture bootstrap state, mirroring what the server
 * ships with the page (ClmStateContributor -> clmConnector.isActive()). This is the
 * local, non-remote source of the persisted enabled flag. Without it getValue('clm')
 * is undefined and the flag reads as unknown. beforeEach reinstalls the baseline
 * getValue for every test, so no explicit teardown is needed.
 */
function mockClmBootstrapState(enabled: boolean) {
  (global as any).NX.State.getValue = jest.fn((key: string) => {
    if (key === 'clm') return { enabled };
    if (key === 'hostedRepositoryEvaluationEnabled') return true;
    return undefined;
  });
}

describe('IqServerConnectedPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGo.mockReset();
    // Default: administrator with settings:update so the write affordances render.
    mockCheckPermission.mockReturnValue(true);
    mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnected' }, params: {} });
    mockDialogHelpers();
    // Hosted Repository Evaluation feature flag: default ON for tests. Individual
    // tests may override to false to exercise the flag-off card.
    (global as any).NX.State.getValue = jest.fn((key: string) =>
      key === 'hostedRepositoryEvaluationEnabled' ? true : undefined
    );
  });

  it('renders the page header after data loads', async () => {
    mockApi();
    renderWithTheme(<IqServerConnectedPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'IQ Server', level: 1 })).toBeInTheDocument();
    });
    expect(
      screen.getByText('Manage Sonatype Repository Firewall and Lifecycle configuration')
    ).toBeInTheDocument();
  });

  it('opens the connection-settings dialog when the header button is clicked', async () => {
    mockApi();
    renderWithTheme(<IqServerConnectedPage />);

    const button = await screen.findByRole('button', { name: /Connection Settings/i });
    fireEvent.click(button);
    // Clicking calls handleConnectionSettings → setDialogOpen(true) → dialog opens.
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeInTheDocument();
  });

  it('shows the empty-state setup card when no repositories are monitored', async () => {
    mockApi();
    renderWithTheme(<IqServerConnectedPage />);

    expect(await screen.findByTestId('lifecycle-setup-card')).toBeInTheDocument();
    expect(screen.getByTestId('lifecycle-setup-button')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Configure global evaluation settings to provide policy coverage for your hosted repositories.'
      )
    ).toBeInTheDocument();
    expect(screen.queryByTestId('lifecycle-summary-tile')).not.toBeInTheDocument();
  });

  it('shows the configured summary tile when repos are monitored', async () => {
    mockApi({
      fetchDashboardSummary: jest.fn().mockResolvedValue({
        numberOfMonitoredRepositories: 54,
        totalRepositories: 54,
        globalConfigAvailable: true,
        hasSelections: true,
      }),
      fetchEvaluationSettings: jest.fn().mockResolvedValue({
        activityTimeFrame: 30,
        artifactLatestVersions: 5,
        policyEvaluationStage: 'RELEASE',
        monitoredRepoCount: 54,
        totalRepoCount: 54,
      }),
    });
    renderWithTheme(<IqServerConnectedPage />);

    expect(await screen.findByTestId('lifecycle-summary-tile')).toBeInTheDocument();
    expect(screen.queryByTestId('lifecycle-setup-card')).not.toBeInTheDocument();
    expect(screen.getByTestId('lifecycle-stage-pill')).toHaveTextContent('Stage: Release');
    expect(screen.getByTestId('lifecycle-metric-monitored')).toHaveTextContent('54');
    expect(screen.getByTestId('lifecycle-metric-timeframe')).toHaveTextContent('30');
    expect(screen.getByTestId('lifecycle-metric-timeframe')).toHaveTextContent('days');
    expect(screen.getByTestId('lifecycle-metric-latest-versions')).toHaveTextContent('5');
  });

  it('clicking the configured summary tile navigates to the hosted-repos-eval workflow', async () => {
    mockApi({
      fetchDashboardSummary: jest.fn().mockResolvedValue({
        numberOfMonitoredRepositories: 54,
        totalRepositories: 54,
        globalConfigAvailable: true,
        hasSelections: true,
      }),
      fetchEvaluationSettings: jest.fn().mockResolvedValue({
        activityTimeFrame: 30,
        artifactLatestVersions: 5,
        policyEvaluationStage: 'RELEASE',
        monitoredRepoCount: 54,
        totalRepoCount: 54,
      }),
    });
    renderWithTheme(<IqServerConnectedPage />);

    // Card asChild renders the tile as a <button> itself — click directly.
    const tile = await screen.findByTestId('lifecycle-summary-tile');
    fireEvent.click(tile);
    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith('preview.admin.iqHostedReposEval', { tab: 'settings' });
    });
  });

  it('clicking the Set up button navigates to the hosted-repos-eval workflow', async () => {
    mockApi();
    renderWithTheme(<IqServerConnectedPage />);

    const button = await screen.findByTestId('lifecycle-setup-button');
    fireEvent.click(button);
    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith('preview.admin.iqHostedReposEval', { tab: 'settings' });
    });
  });

  it('shows connection error banner (no redirect) when verify-connection fails on a configured tenant', async () => {
    mockApi({
      verifyConnection: jest.fn().mockResolvedValue({ success: false, reason: 'Unsupported SSL message' }),
    });
    renderWithTheme(<IqServerConnectedPage />);

    // verify failure on a configured tenant → stay on page, show error banner.
    // Redirecting to iqOverview would cause a navigation loop (overview re-checks
    // iq.enabled which is still true → bounces back to connected → repeat).
    await screen.findByTestId('iq-connection-error-banner');
    expect(mockGo).not.toHaveBeenCalledWith('preview.admin.iq');
  });

  it('shows "Not Available" card when lifecycle is not licensed', async () => {
    mockApi({
      fetchIq: jest.fn().mockResolvedValue({
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [{ id: 'firewall', url: 'http://localhost:8070/ui/links/firewall/dashboard' }],
        hasFirewall: true,
      }),
    });
    renderWithTheme(<IqServerConnectedPage />);

    expect(await screen.findByTestId('lifecycle-unavailable-card')).toBeInTheDocument();
    // Nexus One UI parity: red "Purchase license" text + blue "Explore" link.
    expect(
      screen.getByText('Purchase license or contact Administrator')
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /Explore Sonatype Lifecycle/i })
    ).toBeInTheDocument();
    expect(screen.queryByTestId('lifecycle-setup-card')).not.toBeInTheDocument();
  });

  it('renders Firewall section with "Connected" badge and purchase-license card when hasFirewall is false', async () => {
    mockApi({
      fetchIq: jest.fn().mockResolvedValue({
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [{ id: 'lifecycle', url: 'http://localhost:8070/ui/links/lifecycle/dashboard' }],
        hasFirewall: false,
      }),
    });
    renderWithTheme(<IqServerConnectedPage />);

    expect(await screen.findByTestId('firewall-status-disconnected')).toBeInTheDocument();
    expect(screen.queryByTestId('firewall-status-connected')).not.toBeInTheDocument();
    expect(screen.getByTestId('firewall-card')).toHaveTextContent('Purchase license or contact Administrator');
  });

  it('renders ErrorState and supports retry on initial fetch failure', async () => {
    const fetchIq = jest.fn()
      .mockRejectedValueOnce(new Error('Failed to load IQ Server configuration'))
      .mockResolvedValueOnce({
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [
          { id: 'lifecycle', url: 'http://localhost:8070/ui/links/lifecycle/dashboard' },
          { id: 'firewall', url: 'http://localhost:8070/ui/links/firewall/dashboard' },
        ],
        hasFirewall: true,
      });
    mockApi({ fetchIq });

    renderWithTheme(<IqServerConnectedPage />);

    expect(await screen.findByText('Failed to load IQ Server configuration')).toBeInTheDocument();
    const retryBtn = await screen.findByRole('button', { name: /retry|try again/i });
    fireEvent.click(retryBtn);
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'IQ Server', level: 1 })).toBeInTheDocument();
    });
    // fetchIq called twice (initial failure + retry success)
    expect(fetchIq).toHaveBeenCalledTimes(2);
  });

  it('redirects to iqOverview when iq.enabled is false', async () => {
    mockApi({
      fetchIq: jest.fn().mockResolvedValue({
        enabled: false,
        showLink: true,
        url: '',
        authenticationType: '' as const,
        username: '',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [],
        hasFirewall: false,
      }),
    });
    renderWithTheme(<IqServerConnectedPage />);

    // Disconnected state — component redirects; the overview page shows the connect card.
    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith('preview.admin.iq');
    });
  });

  it('renders the Firewall learn-more link', async () => {
    mockApi();
    renderWithTheme(<IqServerConnectedPage />);

    const link = await screen.findByRole('link', { name: /Learn more about Repository Firewall/i });
    // Matches Nexus One UI's links.sonatype.com short link for consistency.
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/nexus-repository-firewall');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
  });

  // UX-16: On cloud, the IQ Server URL is environment-managed by
  // IqServerUrlAutomatedConfiguration. Disable the URL field in the
  // ConfigurationDialog so users don't think they can change it.
  describe('Connection Settings dialog URL field (UX-16)', () => {
    const baseIq = {
      enabled: false,
      showLink: true,
      url: '',
      authenticationType: '' as const,
      username: '',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [],
      hasFirewall: false,
    };

    // The hook lives in shared/hooks; jest auto-mocks it by setting the
    // module's default return value before each test.
    const useIsCloudMock = jest.requireMock('../../../../../shared/hooks/useIsCloud') as {
      useIsCloud: jest.Mock<boolean>;
    };

    it('URL field is enabled on self-hosted (useIsCloud=false)', async () => {
      useIsCloudMock.useIsCloud.mockReturnValue(false);
      // Simulate arriving via iqConnection route (dialog pre-opened).
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      renderWithTheme(<IqServerConnectedPage />);
      const url = await screen.findByLabelText(/IQ Server URL/i);
      expect(url).not.toBeDisabled();
    });

    it('URL field is disabled when running on cloud (useIsCloud=true)', async () => {
      useIsCloudMock.useIsCloud.mockReturnValue(true);
      // Simulate arriving via iqConnection route (dialog pre-opened).
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      renderWithTheme(<IqServerConnectedPage />);
      const url = await screen.findByLabelText(/IQ Server URL/i);
      expect(url).toBeDisabled();
      expect(
        screen.getByText(/Managed by tenant configuration/i)
      ).toBeInTheDocument();
    });
  });

  // Pre-verify on Save: a failing verify-connection must surface inline next
  // to the existing alert indicator and keep the dialog open — instead of
  // saving bad creds and letting the parent page bubble the 401 in its
  // disconnected card after the dialog has already closed.
  describe('Connection Settings dialog Save pre-verify', () => {
    const baseIq = {
      enabled: false,
      showLink: true,
      url: '',
      authenticationType: '' as const,
      username: '',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [],
      hasFirewall: false,
    };

    /**
     * The Authentication Method dropdown renders `settings.authenticationType || 'USER'`, so it
     * displays "User Authentication" even when the form state holds no value at all - which is
     * what the defaults contain after a failed load. The IQ REST API rejects a null/blank
     * authenticationType with a @NotNull violation, so the payload must carry the value the
     * dropdown is showing rather than the empty state value.
     */
    it('sends the displayed USER auth type even when the loaded configuration has none', async () => {
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      // Load failure -> dialog falls back to DEFAULT_IQ_CONFIGURATION, whose authenticationType is ''.
      fetchIqSettingsMock.mockRejectedValue(new Error('Failed to load IQ Server settings'));
      verifyIqConnectionMock.mockResolvedValue({ success: true });

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() => expect(document.getElementById('iq-dlg-pw')).not.toBeNull());
      fireEvent.change(document.getElementById('iq-dlg-url') as HTMLInputElement, {
        target: { value: 'http://localhost:8070' },
      });
      fireEvent.change(document.getElementById('iq-dlg-user') as HTMLInputElement, {
        target: { value: 'admin' },
      });
      fireEvent.change(document.getElementById('iq-dlg-pw') as HTMLInputElement, {
        target: { value: 'admin123' },
      });

      fireEvent.click(screen.getByTestId('iq-dlg-test'));

      await waitFor(() => expect(verifyIqConnectionMock).toHaveBeenCalledTimes(1));
      expect(verifyIqConnectionMock).toHaveBeenCalledWith(
        expect.objectContaining({ authenticationType: 'USER' })
      );
    });

    it('shows 401 inline and does NOT call saveIqSettings when verifyConnection fails', async () => {
      // Simulate arriving via iqConnection route (dialog pre-opened).
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      // Post NEXUS-53610: helpers are standalone. Override the module-level
      // jest.fn()s that mockDialogHelpers() set up in beforeEach.
      verifyIqConnectionMock.mockResolvedValue({
        success: false,
        reason: 'Error code 401: Invalid credentials. Please try again.',
      });
      fetchIqSettingsMock.mockResolvedValue({
        enabled: false,
        showLink: true,
        url: 'http://iq.example.com',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        // fetchIqSettings returns IqServerFormData shape (properties: array).
        properties: [],
        propertiesDroppedLineCount: 0,
        failOpenModeEnabled: false,
      });

      renderWithTheme(<IqServerConnectedPage />);

      // Provide a password so the placeholder-guard doesn't short-circuit before
      // verifyConnection runs.
      // Wait for the dialog to render then grab the password input by id —
      // /Password/i would also match the "Show password" toggle button.
      await waitFor(() => expect(document.getElementById('iq-dlg-pw')).not.toBeNull());
      const pw = document.getElementById('iq-dlg-pw') as HTMLInputElement;
      fireEvent.change(pw, { target: { value: 'bad-password' } });

      fireEvent.click(screen.getByTestId('iq-dlg-save'));

      await waitFor(() => {
        expect(verifyIqConnectionMock).toHaveBeenCalledTimes(1);
      });
      expect(putIqSettingsMock).not.toHaveBeenCalled();
      expect(
        await screen.findByText(/Authentication failed\. Check the username and password/i)
      ).toBeInTheDocument();
      // Dialog still open
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('calls putIqSettings and closes the dialog when verifyConnection succeeds', async () => {
      // Simulate arriving via iqConnection route (dialog pre-opened).
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      verifyIqConnectionMock.mockResolvedValue({ success: true });
      fetchIqSettingsMock.mockResolvedValue({
        enabled: false,
        showLink: true,
        url: 'http://iq.example.com',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        propertiesDroppedLineCount: 0,
        failOpenModeEnabled: false,
      });

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() => expect(document.getElementById('iq-dlg-pw')).not.toBeNull());
      const pw = document.getElementById('iq-dlg-pw') as HTMLInputElement;
      fireEvent.change(pw, { target: { value: 'good-password' } });

      fireEvent.click(screen.getByTestId('iq-dlg-save'));

      await waitFor(() => expect(verifyIqConnectionMock).toHaveBeenCalledTimes(1));
      await waitFor(() => expect(putIqSettingsMock).toHaveBeenCalledTimes(1));
    });

    /**
     * Save must not clear failOpenModeEnabled.
     *
     * The field has no control in this dialog, but PUT /v1/iq re-derives the stored flag
     * from the request payload (IqApiResource.updateConfiguration ->
     * withFailOpenModeEnabled(xo.isFailOpenModeEnabled()), a primitive defaulting to
     * false). buildPayload() enumerates fields explicitly, so any field it omits is
     * dropped and an unrelated Save resets a stored `true` to `false`. The value read
     * from GET /v1/iq must therefore be echoed back untouched.
     */
    it('echoes failOpenModeEnabled back in the Save payload when the server returned it', async () => {
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      verifyIqConnectionMock.mockResolvedValue({ success: true });
      fetchIqSettingsMock.mockResolvedValue({
        enabled: true,
        showLink: true,
        url: 'http://iq.example.com',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        propertiesDroppedLineCount: 0,
        // Self-hosted GET /v1/iq exposes the persisted fail-open flag.
        failOpenModeEnabled: true,
      });

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() => expect(document.getElementById('iq-dlg-pw')).not.toBeNull());
      fireEvent.change(document.getElementById('iq-dlg-pw') as HTMLInputElement, {
        target: { value: 'good-password' },
      });

      fireEvent.click(screen.getByTestId('iq-dlg-save'));

      await waitFor(() => expect(putIqSettingsMock).toHaveBeenCalledTimes(1));
      expect(putIqSettingsMock).toHaveBeenCalledWith(
        expect.objectContaining({ failOpenModeEnabled: true })
      );
      // The pre-Save verification uses the same payload, so it must carry it too.
      expect(verifyIqConnectionMock).toHaveBeenCalledWith(
        expect.objectContaining({ failOpenModeEnabled: true })
      );
    });

    /**
     * Cloud semantics for the same shared dialog.
     *
     * Cloud's GET (MTIQTenantConnectionOutput) never returns failOpenModeEnabled — cloud
     * derives the stored value from the Firewall tenant instead — and its input DTO has no
     * such property. So the round-tripped field must stay absent rather than being sent as
     * `false`, which would be inventing a value the UI never read.
     */
    it('omits failOpenModeEnabled from the Save payload when the server never returned it', async () => {
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(baseIq) });
      verifyIqConnectionMock.mockResolvedValue({ success: true });
      fetchIqSettingsMock.mockResolvedValue({
        enabled: true,
        showLink: true,
        url: 'http://iq.example.com',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        propertiesDroppedLineCount: 0,
        // No failOpenModeEnabled key — cloud shape.
      });

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() => expect(document.getElementById('iq-dlg-pw')).not.toBeNull());
      fireEvent.change(document.getElementById('iq-dlg-pw') as HTMLInputElement, {
        target: { value: 'good-password' },
      });

      fireEvent.click(screen.getByTestId('iq-dlg-save'));

      await waitFor(() => expect(putIqSettingsMock).toHaveBeenCalledTimes(1));
      expect(putIqSettingsMock.mock.calls[0][0]).not.toHaveProperty('failOpenModeEnabled');
    });
  });

  // Route-based connect/disconnect flow.
  // After Save/Disconnect the dialog navigates to #preview/admin/iq/connected —
  // UIRouter remounts the component so loadData() sees fresh server state.
  // Tests verify the navigation contract; production remount is UIRouter's job.
  describe('Connect/Disconnect route-based flow', () => {
    const disconnectedIq = {
      enabled: false,
      showLink: true,
      url: 'https://iq.example.com/',
      authenticationType: 'USER' as const,
      username: 'admin',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [],
      hasFirewall: false,
    };

    it('navigates to /iq/connected and closes dialog after Save', async () => {
      // Simulate arriving via iqConnection route (dialog pre-opened).
      mockUseCurrentStateAndParams.mockReturnValue({ state: { name: 'preview.admin.iqConnection' }, params: {} });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(disconnectedIq) });
      mockDialogHelpers({
        fetchIqSettings: {
          enabled: false, showLink: true, url: 'https://iq.example.com/',
          authenticationType: 'USER' as const, username: 'admin', password: '',
          useTrustStoreForUrl: false, timeoutSeconds: null,
          properties: [], propertiesDroppedLineCount: 0, failOpenModeEnabled: false,
        },
        verifyConnection: { success: true },
      });

      renderWithTheme(<IqServerConnectedPage />);
      await waitFor(() => expect(document.getElementById('iq-dlg-pw')).not.toBeNull());
      fireEvent.change(document.getElementById('iq-dlg-pw') as HTMLInputElement, {
        target: { value: 'good-password' },
      });
      fireEvent.click(screen.getByTestId('iq-dlg-save'));

      await waitFor(() => expect(putIqSettingsMock).toHaveBeenCalledTimes(1));
      // Dialog closes after navigation back to connected route.
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('closes dialog and navigates to /iq/connected after Disconnect', async () => {
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ verifyConnection });
      mockDialogHelpers({
        fetchIqSettings: {
          enabled: true, showLink: false, url: 'https://iq.example.com/',
          authenticationType: 'USER' as const, username: 'admin',
          password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
          useTrustStoreForUrl: false, timeoutSeconds: null,
          properties: [], propertiesDroppedLineCount: 0, failOpenModeEnabled: false,
        },
      });

      renderWithTheme(<IqServerConnectedPage />);
      await screen.findByTestId('lifecycle-status-connected');

      fireEvent.click(screen.getByRole('button', { name: /Connection Settings/i }));
      const disconnectBtn = await screen.findByRole('button', { name: /Disconnect/i });
      fireEvent.click(disconnectBtn);

      // Disconnect goes through the dedicated disable endpoint, so it
      // never rebuilds (and never truncates) the stored configuration.
      await waitFor(() => expect(disableIqServerMock).toHaveBeenCalledTimes(1));
      expect(putIqSettingsMock).not.toHaveBeenCalled();
      // Dialog closes after navigation back to connected route.
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
      // verifyConnection only on initial load — remount on navigation handles refresh.
      expect(verifyConnection).toHaveBeenCalledTimes(1);
    });
  });

  // A failing IQ connection must not remove the user's ability to disable the
  // integration. The enable/disable control is driven by the persisted `enabled`
  // value from GET /v1/iq — never by remote health — and turning it off issues
  // POST /v1/iq/disable with no verify step.
  describe('IQ enable/disable control', () => {
    const enabledIq = {
      enabled: true,
      showLink: true,
      url: 'http://localhost:8070',
      authenticationType: 'USER' as const,
      username: 'admin',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [{ id: 'lifecycle', url: 'http://localhost:8070/ui/links/lifecycle/dashboard' }],
      hasFirewall: true,
    };

    // Representative remote failure: expired IQ license / unreachable server.
    // verify-connection resolves {success:false} (it never throws) while the
    // local config GET succeeds — the exact state from the ticket.
    function mockConnectionError(reason = 'License expired') {
      mockApi({
        fetchIq: jest.fn().mockResolvedValue(enabledIq),
        verifyConnection: jest.fn().mockResolvedValue({ success: false, reason }),
      });
    }

    it('renders the enable control together with the connection-error banner', async () => {
      mockConnectionError();
      renderWithTheme(<IqServerConnectedPage />);

      // Error is shown...
      expect(await screen.findByTestId('iq-connection-error-banner')).toBeInTheDocument();
      // ...and the recovery control is still there, reflecting persisted enabled=true.
      const toggle = screen.getByTestId('iq-enable-toggle');
      expect(toggle).toBeInTheDocument();
      expect(toggle).toBeEnabled();
      expect(toggle).toBeChecked();
      // Accessible name describes the IQ Server integration (a11y).
      expect(screen.getByRole('switch', { name: /Enable IQ Server/i })).toBe(toggle);
    });

    it('disables IQ while the connection is failing, without a successful connection', async () => {
      const verifyConnection = jest.fn().mockResolvedValue({ success: false, reason: 'License expired' });
      mockApi({ fetchIq: jest.fn().mockResolvedValue(enabledIq), verifyConnection });
      renderWithTheme(<IqServerConnectedPage />);

      fireEvent.click(await screen.findByTestId('iq-enable-toggle'));

      // The dedicated endpoint takes no body — the browser never
      // reconstructs the stored configuration, so nothing it cannot see (the
      // credentials, failOpenModeEnabled) can be reset as a side effect.
      await waitFor(() => expect(disableIqServerMock).toHaveBeenCalledTimes(1));
      expect(disableIqServerMock).toHaveBeenCalledWith();
      expect(putIqSettingsMock).not.toHaveBeenCalled();
      // No connection test gates the disable — verify ran once, on initial load only.
      expect(verifyConnection).toHaveBeenCalledTimes(1);
      expect(verifyIqConnectionMock).not.toHaveBeenCalled();
      // Disabled state is committed and the user leaves the connected route.
      await waitFor(() => expect(mockGo).toHaveBeenCalledWith('preview.admin.iq'));
    });

    it('keeps the control and surfaces an error when the disable request fails', async () => {
      mockConnectionError();
      disableIqServerMock.mockRejectedValue(new Error('Failed to disable IQ Server'));
      renderWithTheme(<IqServerConnectedPage />);

      fireEvent.click(await screen.findByTestId('iq-enable-toggle'));

      expect(await screen.findByTestId('iq-enable-error')).toHaveTextContent(/Failed to disable IQ Server/i);
      // Never reported as disabled, and the control stays available for a retry.
      const toggle = screen.getByTestId('iq-enable-toggle');
      expect(toggle).toBeChecked();
      expect(toggle).toBeEnabled();
      expect(mockGo).not.toHaveBeenCalledWith('preview.admin.iq');
    });

    it('renders the control read-only without nexus:settings:update', async () => {
      mockCheckPermission.mockImplementation(p => p !== Permissions.SETTINGS.UPDATE);
      mockConnectionError();
      renderWithTheme(<IqServerConnectedPage />);

      const toggle = await screen.findByTestId('iq-enable-toggle');
      // State is visible but not modifiable by a read-only user.
      expect(toggle).toBeChecked();
      expect(toggle).toBeDisabled();
      fireEvent.click(toggle);
      expect(disableIqServerMock).not.toHaveBeenCalled();
      expect(putIqSettingsMock).not.toHaveBeenCalled();
    });

    it('renders the control when the connection succeeds (enabled + connected)', async () => {
      mockApi();
      renderWithTheme(<IqServerConnectedPage />);

      expect(await screen.findByTestId('iq-enable-toggle')).toBeChecked();
      expect(screen.queryByTestId('iq-connection-error-banner')).not.toBeInTheDocument();
    });

    it('keeps enabling on the validated dialog path', async () => {
      // Enabling is never a bare PUT from the toggle: the dialog collects the
      // required fields and pre-verifies the connection first. On this page the
      // disabled state lives at /iq, so the enable affordance is the dialog.
      mockApi({
        fetchIq: jest.fn().mockResolvedValue({ ...enabledIq, enabled: false }),
        verifyConnection: jest.fn().mockResolvedValue({ success: false, reason: 'License expired' }),
      });
      mockUseCurrentStateAndParams.mockReturnValue({
        state: { name: 'preview.admin.iqConnection' },
        params: {},
      });
      renderWithTheme(<IqServerConnectedPage />);

      expect(await screen.findByText('IQ Server Connection Settings')).toBeInTheDocument();
      expect(disableIqServerMock).not.toHaveBeenCalled();
      expect(putIqSettingsMock).not.toHaveBeenCalled();
    });

    it('does not render a toggle when the configuration request fails and no persisted state is known', async () => {
      // No bootstrap state stubbed: the persisted flag is unknown, so showing a
      // toggle would be a guess. The page falls back to the full error surface.
      mockApi({ fetchIq: jest.fn().mockRejectedValue(new Error('Failed to load IQ Server configuration')) });
      renderWithTheme(<IqServerConnectedPage />);

      expect(await screen.findByText('Failed to load IQ Server configuration')).toBeInTheDocument();
      expect(screen.queryByTestId('iq-enable-toggle')).not.toBeInTheDocument();
    });

    // GET /v1/iq asks IQ Server for licensed solutions while IQ is enabled, so it can
    // hang until timeout or fail outright. The control must not wait on it.
    describe('remote-dependent configuration request is unavailable', () => {
      it('keeps the control usable while GET /v1/iq is still pending', async () => {
        mockClmBootstrapState(true);
        const fetchIq = jest.fn(() => new Promise(() => {})); // never settles
        mockApi({ fetchIq: fetchIq as any });
        renderWithTheme(<IqServerConnectedPage />);

        // Detail body is still loading...
        expect(await screen.findByTestId('iq-details-loading')).toBeInTheDocument();
        // ...while the control is already there, reflecting persisted enabled=true.
        const toggle = await screen.findByTestId('iq-enable-toggle');
        expect(toggle).toBeChecked();
        expect(toggle).toBeEnabled();
      });

      it('disables IQ successfully while GET /v1/iq is still pending', async () => {
        mockClmBootstrapState(true);
        mockApi({ fetchIq: jest.fn(() => new Promise(() => {})) as any });
        renderWithTheme(<IqServerConnectedPage />);

        fireEvent.click(await screen.findByTestId('iq-enable-toggle'));

        await waitFor(() => expect(disableIqServerMock).toHaveBeenCalledTimes(1));
        // Disable needs no configuration payload, so a null iqData cannot block it.
        expect(putIqSettingsMock).not.toHaveBeenCalled();
        await waitFor(() => expect(mockGo).toHaveBeenCalledWith('preview.admin.iq'));
      });

      it('keeps the control usable and disables IQ when GET /v1/iq rejects', async () => {
        mockClmBootstrapState(true);
        const verifyConnection = jest.fn().mockResolvedValue({ success: false, reason: 'unreachable' });
        mockApi({
          fetchIq: jest.fn().mockRejectedValue(new Error('Failed to load IQ Server settings')),
          verifyConnection,
        });
        renderWithTheme(<IqServerConnectedPage />);

        // The failure is reported in the detail body, not in place of the page.
        expect(await screen.findByTestId('iq-details-error')).toBeInTheDocument();
        const toggle = await screen.findByTestId('iq-enable-toggle');
        expect(toggle).toBeChecked();

        fireEvent.click(toggle);

        await waitFor(() => expect(disableIqServerMock).toHaveBeenCalledTimes(1));
        expect(putIqSettingsMock).not.toHaveBeenCalled();
        // The disable itself triggers no IQ Server round-trip: verify ran once on
        // load, and the dialog's verify never ran at all.
        expect(verifyConnection).toHaveBeenCalledTimes(1);
        expect(verifyIqConnectionMock).not.toHaveBeenCalled();
        await waitFor(() => expect(mockGo).toHaveBeenCalledWith('preview.admin.iq'));
      });

      /**
       * The local `clm.enabled` fallback must not keep a disabled page alive. With the
       * GET still pending the component is in its initial-loading state, so the redirect
       * effect has not run yet; this test therefore asserts only what that state can
       * show — a persisted-disabled fallback renders no enable control. The redirect
       * itself is proven by the next test.
       */
      it('renders no enable control while the persisted state says IQ is disabled and the GET is pending', async () => {
        mockClmBootstrapState(false);
        mockApi({ fetchIq: jest.fn(() => new Promise(() => {})) as any });
        renderWithTheme(<IqServerConnectedPage />);

        expect(await screen.findByText('Loading IQ Server configuration...')).toBeInTheDocument();
        expect(screen.queryByTestId('iq-enable-toggle')).not.toBeInTheDocument();
      });

      it('redirects to the overview once a resolved configuration confirms IQ is disabled', async () => {
        // The local fallback must not suppress the redirect: bootstrap says disabled and
        // so does the server, so the page hands off to the overview.
        mockClmBootstrapState(false);
        mockApi({
          fetchIq: jest.fn().mockResolvedValue({
            enabled: false,
            showLink: true,
            url: 'http://localhost:8070',
            authenticationType: 'USER' as const,
            username: 'admin',
            password: '',
            useTrustStoreForUrl: false,
            timeoutSeconds: null,
            properties: '',
            failOpenModeEnabled: false,
            licensedSolutions: [],
            hasFirewall: false,
          }),
        });
        renderWithTheme(<IqServerConnectedPage />);

        await waitFor(() => expect(mockGo).toHaveBeenCalledWith('preview.admin.iq'));
        expect(screen.queryByTestId('iq-enable-toggle')).not.toBeInTheDocument();
      });
    });
  });
  describe('just-saved handoff', () => {
    const disabledIqResponse = {
      enabled: false,
      showLink: true,
      url: 'http://localhost:8070',
      authenticationType: 'USER' as const,
      username: 'admin',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [],
      hasFirewall: false,
    };

    function markJustSaved(overrides: Record<string, unknown> = {}) {
      setFreshIqConfigCache({
        enabled: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER',
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        failOpenModeEnabled: false,
        licensedSolutions: [{ id: 'lifecycle', url: '/ui/links/lifecycle' }],
        hasFirewall: false,
        ...overrides,
      } as any);
    }

    it('skips fetchIq (cache is authoritative) but STILL runs verify-connection on just-saved', async () => {
      const fetchIq = jest.fn().mockResolvedValue(disabledIqResponse);
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSaved();

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() =>
        expect(screen.getByRole('heading', { name: 'IQ Server', level: 1 })).toBeInTheDocument()
      );
      expect(fetchIq).not.toHaveBeenCalled();
      // Verify must still fire: SAVE doesn't imply the connection was validated.
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
    });

    it('renders the URL and licensedSolutions from the cached PUT response without a GET', async () => {
      const fetchIq = jest.fn().mockResolvedValue(disabledIqResponse);
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSaved({
        url: 'http://newly.saved.iq:8070',
        licensedSolutions: [{ id: 'lifecycle', url: '/ui/links/lifecycle' }],
      });

      renderWithTheme(<IqServerConnectedPage />);

      // URL chip picks up the cached URL, not the mocked GET default.
      await waitFor(() =>
        expect(screen.getByTestId('iq-server-url-chip')).toHaveTextContent(
          'http://newly.saved.iq:8070'
        )
      );
      expect(fetchIq).not.toHaveBeenCalled();
    });

    it('falls back to fetchIq when the cached PUT response has empty licensedSolutions', async () => {
      // Backend settle-wait covers URL+username but not isActive/licensedSolutions, so an empty
      // licensedSolutions on the cache means the tiles must fetch the real state.
      const fetchIq = jest.fn().mockResolvedValue({ ...disabledIqResponse, enabled: true, licensedSolutions: [{id:'lifecycle', url:''}] });
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSaved({ licensedSolutions: [] });

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(1));
    });

    it('falls back to a normal fetch + verify when the cache is empty (no just-save)', async () => {
      const fetchIq = jest.fn().mockResolvedValue({ ...disabledIqResponse, enabled: true });
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      // No markJustSaved() — normal page visit.

      renderWithTheme(<IqServerConnectedPage />);

      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(1));
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
    });
  });

  describe('post-save retry', () => {
    // Mounts the page with the cache already populated as empty-licensedSolutions, which forces
    // the just-saved path to fall through to fetchIq — the shape the retry loop exercises.
    function markJustSavedForceFetch() {
      setFreshIqConfigCache({
        enabled: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER',
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        failOpenModeEnabled: false,
        licensedSolutions: [], // empty → cache is not authoritative, GET runs
        hasFirewall: false,
      } as any);
    }

    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.runOnlyPendingTimers();
      jest.useRealTimers();
    });

    it('retries when GET returns enabled:false, stops as soon as GET returns enabled:true', async () => {
      const enabledResponse = {
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [{ id: 'lifecycle', url: '/x' }],
        hasFirewall: false,
      };
      const disabled = { ...enabledResponse, enabled: false, licensedSolutions: [] };
      const fetchIq = jest.fn()
        .mockResolvedValueOnce(disabled) // first load: stale
        .mockResolvedValueOnce(enabledResponse); // first retry: settled
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSavedForceFetch();

      renderWithTheme(<IqServerConnectedPage />);

      // First load fires; still awaiting the scheduled retry.
      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(1));

      // Advance the retry delay so the pending timer fires.
      await act(async () => {
        jest.advanceTimersByTime(2000);
      });
      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(2));

      // Second load returned enabled:true → loop stops. Advancing further must NOT schedule
      // additional retries.
      await act(async () => {
        jest.advanceTimersByTime(10000);
      });
      expect(fetchIq).toHaveBeenCalledTimes(2);
    });

    it('gives up after SAVE_RETRY_LIMIT attempts when the backend never settles', async () => {
      const stale = {
        enabled: false,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [],
        hasFirewall: false,
      };
      const fetchIq = jest.fn().mockResolvedValue(stale);
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSavedForceFetch();

      renderWithTheme(<IqServerConnectedPage />);
      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(1));

      // Three additional attempts (one per retry, up to SAVE_RETRY_LIMIT=3), then the loop stops
      // and the page renders whatever the last load reported.
      for (let i = 0; i < 4; i++) {
        await act(async () => {
          jest.advanceTimersByTime(2000);
        });
      }
      // Initial + 3 retries = 4. A 5th tick must not schedule any further fetches.
      expect(fetchIq).toHaveBeenCalledTimes(4);
      await act(async () => {
        jest.advanceTimersByTime(10000);
      });
      expect(fetchIq).toHaveBeenCalledTimes(4);
    });

    it('caps empty-licensedSolutions-only staleness at ONE retry (persistent for IQ < 200)', async () => {
      // enabled:true + verify.success + licensedSolutions:[] is the persistent shape for IQ < 200
      // or a swallowed-IOException server-side. Retrying 3× would waste 6 seconds of spinner on
      // every save. One retry is enough to distinguish "still settling" from "genuinely empty".
      const enabledButEmpty = {
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [],
        hasFirewall: false,
      };
      const fetchIq = jest.fn().mockResolvedValue(enabledButEmpty);
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSavedForceFetch();

      renderWithTheme(<IqServerConnectedPage />);
      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(1));

      // One retry only, then the loop stops.
      await act(async () => {
        jest.advanceTimersByTime(2000);
      });
      expect(fetchIq).toHaveBeenCalledTimes(2);
      await act(async () => {
        jest.advanceTimersByTime(10000);
      });
      expect(fetchIq).toHaveBeenCalledTimes(2);
    });

    it('retries when verify-connection returns {success:false} on the just-saved path', async () => {
      // useIqConnectedApi does not throw on transient post-save 400s — it returns {success:false}.
      // The retry gate must include !verify.success or the page renders "not connected" instantly.
      const enabledResponse = {
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [{ id: 'lifecycle', url: '/x' }],
        hasFirewall: false,
      };
      const fetchIq = jest.fn().mockResolvedValue(enabledResponse);
      const verifyConnection = jest.fn()
        .mockResolvedValueOnce({ success: false, reason: 'transient' })
        .mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSavedForceFetch();

      renderWithTheme(<IqServerConnectedPage />);
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));

      await act(async () => {
        jest.advanceTimersByTime(2000);
      });
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(2));
    });

    it('does not retry after unmount — no state update after the page is gone', async () => {
      const stale = {
        enabled: false,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER' as const,
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [],
        hasFirewall: false,
      };
      const fetchIq = jest.fn().mockResolvedValue(stale);
      const verifyConnection = jest.fn().mockResolvedValue({ success: true });
      mockApi({ fetchIq, verifyConnection });
      markJustSavedForceFetch();

      const { unmount } = renderWithTheme(<IqServerConnectedPage />);
      await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(1));

      // Unmount before the retry timer would fire — the cleanup effect clears it.
      unmount();
      await act(async () => {
        jest.advanceTimersByTime(10000);
      });
      expect(fetchIq).toHaveBeenCalledTimes(1);
    });
  });
});
