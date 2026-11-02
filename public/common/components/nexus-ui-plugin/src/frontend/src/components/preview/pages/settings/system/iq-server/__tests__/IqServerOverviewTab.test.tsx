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
import { render, screen, waitFor, cleanup, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: mockGo } }),
}));

// Feature flag reader — default to enabled so the summary tile path is exercised.
jest.mock('../../../repository/repositories/useRepoEvaluationOverride', () => ({
  isEvaluationFeatureEnabled: jest.fn(() => true),
}));

import { IqServerOverviewTab } from '../IqServerOverviewTab';
import * as useIqConnectedApiModule from '../useIqConnectedApi';

function renderTab(saveRevision = 0, isActive = true) {
  return render(
    <Theme>
      <IqServerOverviewTab isActive={isActive} saveRevision={saveRevision} />
    </Theme>
  );
}

function mockApi(overrides: Partial<ReturnType<typeof useIqConnectedApiModule.useIqConnectedApi>> = {}) {
  const defaults = {
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchIq: jest.fn().mockResolvedValue({
      enabled: true,
      showLink: true,
      url: 'https://iq.example.com',
      authenticationType: 'USER' as const,
      username: 'admin',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [
        { id: 'lifecycle', url: 'https://iq.example.com/lifecycle' },
        { id: 'firewall', url: 'https://iq.example.com/firewall' },
      ],
      hasFirewall: true,
    }),
    verifyConnection: jest.fn().mockResolvedValue({ success: true }),
    fetchDashboardSummary: jest.fn().mockResolvedValue({
      numberOfMonitoredRepositories: 3,
      totalRepositories: 10,
      globalConfigAvailable: true,
      hasSelections: true,
    }),
    fetchEvaluationSettings: jest.fn().mockResolvedValue({
      activityTimeFrame: 60,
      artifactLatestVersions: 4,
      policyEvaluationStage: 'RELEASE',
      monitoredRepoCount: 3,
      totalRepoCount: 10,
    }),
  };
  jest.spyOn(useIqConnectedApiModule, 'useIqConnectedApi').mockReturnValue({
    ...defaults,
    ...overrides,
  });
  return { ...defaults, ...overrides };
}

beforeEach(() => {
  jest.clearAllMocks();
});

afterEach(() => {
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
});

describe('IqServerOverviewTab', () => {
  it('renders the summary tile when Lifecycle is licensed and globally configured', async () => {
    mockApi();
    renderTab();

    expect(await screen.findByTestId('lifecycle-summary-tile')).toBeInTheDocument();
    expect(screen.getByTestId('lifecycle-metric-monitored')).toHaveTextContent('3');
    expect(screen.getByTestId('lifecycle-metric-timeframe')).toHaveTextContent('60');
    expect(screen.getByTestId('lifecycle-metric-latest-versions')).toHaveTextContent('4');
    expect(screen.getByTestId('lifecycle-stage-pill')).toHaveTextContent('Release');
  });

  it('renders the setup card when Lifecycle is licensed but not yet globally configured', async () => {
    mockApi({
      fetchDashboardSummary: jest.fn().mockResolvedValue({
        numberOfMonitoredRepositories: 0,
        totalRepositories: 10,
        globalConfigAvailable: false,
        hasSelections: false,
      }),
    });
    renderTab();

    expect(await screen.findByTestId('lifecycle-setup-card')).toBeInTheDocument();
  });

  it('shows the connection error banner when verifyConnection reports failure', async () => {
    mockApi({
      verifyConnection: jest.fn().mockResolvedValue({ success: false, reason: 'boom' }),
    });
    renderTab();

    expect(await screen.findByTestId('iq-connection-error-banner')).toBeInTheDocument();
    // "Not connected" pills on both cards.
    expect(screen.getByTestId('lifecycle-status-disconnected')).toBeInTheDocument();
    expect(screen.getByTestId('firewall-status-disconnected')).toBeInTheDocument();
  });

  it('renders "Not connected" without an error banner when IQ Server was never configured', async () => {
    // GET /v1/iq returns an empty/unconfigured payload (enabled=false, no url) and verify reports
    // failure by design. That is the normal "Not connected" state, not a broken connection, so the
    // tab must render its summary without the red error banner.
    mockApi({
      fetchIq: jest.fn().mockResolvedValue({
        enabled: false,
        showLink: false,
        url: '',
        authenticationType: 'USER' as const,
        username: '',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [],
        hasFirewall: false,
      }),
      verifyConnection: jest.fn().mockResolvedValue({ success: false, reason: 'IQ Server is not configured' }),
    });
    renderTab();

    expect(await screen.findByTestId('lifecycle-status-disconnected')).toBeInTheDocument();
    expect(screen.getByTestId('firewall-status-disconnected')).toBeInTheDocument();
    // No red banner: nothing is broken, IQ Server simply hasn't been set up yet.
    expect(screen.queryByTestId('iq-connection-error-banner')).not.toBeInTheDocument();
  });

  it('shows the unavailable card when Lifecycle is not in licensedSolutions', async () => {
    mockApi({
      fetchIq: jest.fn().mockResolvedValue({
        enabled: true,
        showLink: true,
        url: 'https://iq.example.com',
        authenticationType: 'USER',
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [{ id: 'firewall', url: 'x' }],
        hasFirewall: true,
      }),
    });
    renderTab();

    expect(await screen.findByTestId('lifecycle-unavailable-card')).toBeInTheDocument();
  });

  it('reloads its data when saveRevision changes', async () => {
    const api = mockApi();
    const { rerender } = renderTab(0);
    await waitFor(() => expect(api.fetchIq).toHaveBeenCalledTimes(1));

    rerender(
      <Theme>
        <IqServerOverviewTab isActive={true} saveRevision={1} />
      </Theme>
    );
    await waitFor(() => expect(api.fetchIq).toHaveBeenCalledTimes(2));
  });

  it('shows an error state when the initial fetch fails and lets the user retry', async () => {
    const fetchIq = jest.fn().mockRejectedValueOnce(new Error('network down')).mockResolvedValue({
      enabled: true,
      showLink: true,
      url: '',
      authenticationType: 'USER',
      username: '',
      password: '',
      useTrustStoreForUrl: false,
      timeoutSeconds: null,
      properties: '',
      failOpenModeEnabled: false,
      licensedSolutions: [],
      hasFirewall: false,
    });
    mockApi({ fetchIq });
    renderTab();

    expect(await screen.findByTestId('iq-details-error')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /try again/i }));
    await waitFor(() => expect(fetchIq).toHaveBeenCalledTimes(2));
  });

  it('navigates to hosted-repos-eval after successfully re-verifying from the setup card', async () => {
    const verifyConnection = jest.fn().mockResolvedValue({ success: true });
    mockApi({
      verifyConnection,
      fetchDashboardSummary: jest.fn().mockResolvedValue({
        numberOfMonitoredRepositories: 0,
        totalRepositories: 10,
        globalConfigAvailable: false,
        hasSelections: false,
      }),
    });
    renderTab();

    fireEvent.click(await screen.findByTestId('lifecycle-setup-button'));
    await waitFor(() =>
      expect(mockGo).toHaveBeenCalledWith('preview.admin.iqHostedReposEval', { tab: 'settings' })
    );
  });
});
