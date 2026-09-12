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
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
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

import { IqServerConnectedPage } from '../IqServerConnectedPage';
import { clearFreshIqConfigCache, clearPendingDisconnect } from '../iqServerStateCache';
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
  verifyIqConnectionMock.mockResolvedValue(overrides.verifyConnection ?? { success: true });
  fetchCapabilitiesMock.mockResolvedValue({});
}

describe('IqServerConnectedPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGo.mockReset();
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

      await waitFor(() => expect(putIqSettingsMock).toHaveBeenCalledTimes(1));
      // Dialog closes after navigation back to connected route.
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
      // verifyConnection only on initial load — remount on navigation handles refresh.
      expect(verifyConnection).toHaveBeenCalledTimes(1);
    });
  });
});
