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
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

jest.mock('@uirouter/react', () => {
  const router = { stateService: { go: jest.fn() } };
  return {
    useRouter: () => router,
    useCurrentStateAndParams: () => ({ state: { name: 'preview.admin.iqHostedReposEval' }, params: {} }),
  };
});

import { HostedRepoEvaluationSetupPage } from '../HostedRepoEvaluationSetupPage';
import * as useHostedRepoEvaluationModule from '../useHostedRepoEvaluation';

// Helper to render with Radix Theme wrapper
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme accentColor="blue" hasBackground={false}>{ui}</Theme>);
}

// Default mock returns — adjust per-test by overriding mockReturnValue
function mockApi(overrides: Partial<ReturnType<typeof useHostedRepoEvaluationModule.useHostedRepoEvaluation>> = {}) {
  const defaults = {
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchSettingsWithRepos: jest.fn().mockResolvedValue({
      settings: useHostedRepoEvaluationModule.DEFAULT_SETTINGS,
      monitoredRepoIds: [],
      totalRepoCount: 0,
    }),
    fetchRepositories: jest.fn().mockResolvedValue({
      rows: [],
      totalCount: 0,
      monitoredCount: 0,
      page: 1,
      pageSize: 25,
    }),
    fetchFormats: jest.fn().mockResolvedValue([]),
    fetchGlobalConfigStatus: jest.fn().mockResolvedValue({ globalConfigAvailable: true, monitoredCount: 0 }),
    saveSettings: jest.fn().mockResolvedValue({ ok: true, message: 'Saved' }),
    applySelectionDelta: jest.fn().mockResolvedValue({ ok: true, message: 'Updated' }),
    putSettingsWithRepos: jest.fn().mockResolvedValue({ ok: true }),
    fetchAllRepoIds: jest.fn().mockResolvedValue([]),
  };
  jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue({
    ...defaults,
    ...overrides,
  } as ReturnType<typeof useHostedRepoEvaluationModule.useHostedRepoEvaluation>);
}

describe('HostedRepoEvaluationSetupPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset hash
    window.location.hash = '';
  });

  it('renders loading state initially', async () => {
    mockApi({
      fetchSettingsWithRepos: jest.fn().mockImplementation(() => new Promise(() => {})), // never resolves
    });

    renderWithTheme(<HostedRepoEvaluationSetupPage />);

    // Loading state has data-testid="loading-state"
    expect(screen.getByTestId('loading-state')).toBeInTheDocument();
  });

  it('renders error state when fetchSettingsWithRepos throws network error', async () => {
    mockApi({
      fetchSettingsWithRepos: jest.fn().mockRejectedValue(new Error('Network down')),
    });

    renderWithTheme(<HostedRepoEvaluationSetupPage />);

    await waitFor(() => {
      expect(screen.getByText(/Failed to load/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Network down/i)).toBeInTheDocument();
  });

  it('renders both tabs after load', async () => {
    mockApi();

    renderWithTheme(<HostedRepoEvaluationSetupPage />);

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /repositories/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
    });
  });

  it('default tab is Repositories', async () => {
    mockApi();

    renderWithTheme(<HostedRepoEvaluationSetupPage />);

    await waitFor(() => {
      const reposTab = screen.getByRole('tab', { name: /repositories/i });
      expect(reposTab).toHaveAttribute('data-state', 'active');
    });
  });

  it('tab clicks switch content', async () => {
    mockApi();

    renderWithTheme(<HostedRepoEvaluationSetupPage />);

    // Wait for initial render (Repositories tab with Hosted Repositories heading)
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Hosted Repositories/i })).toBeInTheDocument();
    });

    // Click Settings tab using userEvent for more realistic interaction
    const settingsTab = screen.getByRole('tab', { name: /settings/i });
    await act(async () => {
      await userEvent.click(settingsTab);
    });

    // Verify Settings tab content appears
    await waitFor(() => {
      // Use getAllByText since "Activity Time Frame" appears in multiple places
      expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
    });

    // Click back to Repositories tab
    const reposTab = screen.getByRole('tab', { name: /repositories/i });
    await act(async () => {
      await userEvent.click(reposTab);
    });

    // Verify Repositories tab content appears
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Hosted Repositories/i })).toBeInTheDocument();
    });
  });

  it('page header shows breadcrumbs (Settings > IQ Server > Hosted Repository Evaluation)', async () => {
    mockApi();

    renderWithTheme(<HostedRepoEvaluationSetupPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Hosted Repository Evaluation/i })).toBeInTheDocument();
    });

    const breadcrumb = screen.getByLabelText('Breadcrumb');
    expect(breadcrumb).toBeInTheDocument();
    expect(breadcrumb).toHaveTextContent('Settings');
    expect(breadcrumb).toHaveTextContent('IQ Server');
    expect(breadcrumb).toHaveTextContent('Hosted Repository Evaluation');
  });
});
