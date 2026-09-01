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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

jest.mock('@uirouter/react', () => {
  const router = { stateService: { go: jest.fn() } };
  return {
    useRouter: () => router,
    useCurrentStateAndParams: () => ({ state: { name: 'preview.admin.iqHostedReposEval' }, params: { tab: 'settings' } }),
  };
});

import { HostedRepoEvaluationSetupPage } from '../HostedRepoEvaluationSetupPage';
import * as useHostedRepoEvaluationModule from '../useHostedRepoEvaluation';
import { ToastProvider } from '../../../../../shared';

// Helper to render with Radix Theme wrapper and ToastProvider
function renderWithTheme(ui: React.ReactElement) {
  return render(
    <Theme accentColor="blue" hasBackground={false}>
      <ToastProvider>
        {ui}
      </ToastProvider>
    </Theme>
  );
}

// Default mock returns
function createMockApi(overrides: Partial<ReturnType<typeof useHostedRepoEvaluationModule.useHostedRepoEvaluation>> = {}) {
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
    saveSettings: jest.fn().mockResolvedValue({ ok: true, message: 'Settings saved successfully' }),
    applySelectionDelta: jest.fn().mockResolvedValue({ ok: true }),
    putSettingsWithRepos: jest.fn().mockResolvedValue({ ok: true }),
    fetchAllRepoIds: jest.fn().mockResolvedValue([]),
  };

  return {
    ...defaults,
    ...overrides,
  } as ReturnType<typeof useHostedRepoEvaluationModule.useHostedRepoEvaluation>;
}

describe('HostedRepoEvaluationSetupPage - Settings Tab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Form rendering', () => {
    it('renders form with DEFAULT_SETTINGS values', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      // Wait for the form to load (Settings tab is active via URL hash)
      await waitFor(() => {
        // "Activity Time Frame" appears in the label - use getAllByText since it may appear multiple times
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Check form fields have expected structure
      // The labels for both settings should be present
      expect(document.getElementById('hre-latest-versions-label')).toBeInTheDocument();
      expect(document.getElementById('hre-time-frame-label')).toBeInTheDocument();
    });

    it('Activity Time Frame and Latest Deployed Versions selects are always visible', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      // Wait for the form to load
      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Activity Time Frame and Latest Deployed Versions fields should both be visible
      expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      expect(screen.getByText(/Set the time frame for evaluating components based on recent repository activity/i)).toBeInTheDocument();
      expect(screen.getByText(/Set the number of most recently deployed component versions to evaluate/i)).toBeInTheDocument();
    });
  });

  describe('Form dirty state', () => {
    it('each field change marks form dirty', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Save button should be disabled (form is clean)
      const saveButton = screen.getByRole('button', { name: /save/i });
      expect(saveButton).toBeDisabled();

      // Change Latest Deployed Versions from 5 to 3
      const comboboxes = screen.getAllByRole('combobox');
      const latestVersionsSelect = comboboxes.find(cb => cb.getAttribute('aria-labelledby')?.includes('latest-versions')) || comboboxes[comboboxes.length - 1];
      fireEvent.click(latestVersionsSelect);

      await waitFor(() => {
        const option3 = screen.getByRole('option', { name: '3' });
        fireEvent.click(option3);
      });

      // Save button should now be enabled (form is dirty)
      await waitFor(() => {
        expect(saveButton).toBeEnabled();
      });
    });

    it('Save button disabled when form clean', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      const saveButton = screen.getByRole('button', { name: /save/i });
      expect(saveButton).toBeDisabled();
    });

    it('Save button enabled when dirty', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      const saveButton = screen.getByRole('button', { name: /save/i });

      // Change autoEnrollNewRepos checkbox
      const checkbox = screen.getByRole('checkbox');
      fireEvent.click(checkbox);

      await waitFor(() => {
        expect(saveButton).toBeEnabled();
      });
    });
  });

  describe('Cancel button', () => {
    it('reverts to pristine settings and clears dirty', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      const saveButton = screen.getByRole('button', { name: /save/i });

      // Cancel should be disabled initially
      expect(cancelButton).toBeDisabled();

      // Make a change via checkbox (autoEnrollNewRepos)
      const checkbox = screen.getByRole('checkbox');
      fireEvent.click(checkbox);

      await waitFor(() => {
        expect(saveButton).toBeEnabled();
        expect(cancelButton).toBeEnabled();
      });

      // Click cancel
      fireEvent.click(cancelButton);

      await waitFor(() => {
        // Form should be clean again
        expect(saveButton).toBeDisabled();
        expect(cancelButton).toBeDisabled();
      });
    });
  });

  describe('Save action', () => {
    it('calls saveSettings with current form values', async () => {
      const saveSettings = jest.fn().mockResolvedValue({ ok: true, message: 'Saved' });
      const mockApi = createMockApi({
        saveSettings,
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [{ id: 'r1', name: 'test', format: 'maven2', size: 0, componentCount: 0, isMonitored: true }],
          totalCount: 1,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Make a change to enable save
      const checkbox = screen.getByRole('checkbox');
      fireEvent.click(checkbox);

      const saveButton = screen.getByRole('button', { name: /save/i });
      await waitFor(() => {
        expect(saveButton).toBeEnabled();
      });

      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(saveSettings).toHaveBeenCalled();
      });
    });

    it('shows success message after successful save', async () => {
      const mockApi = createMockApi({
        saveSettings: jest.fn().mockResolvedValue({ ok: true, message: 'Settings saved successfully' }),
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [{ id: 'r1', name: 'test', format: 'maven2', size: 0, componentCount: 0, isMonitored: true }],
          totalCount: 1,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Make a change and save
      const checkbox = screen.getByRole('checkbox');
      fireEvent.click(checkbox);

      const saveButton = screen.getByRole('button', { name: /save/i });
      await waitFor(() => {
        expect(saveButton).toBeEnabled();
      });

      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByRole('status')).toHaveTextContent(/saved successfully/i);
      });
    });

    it('shows error message on failed save', async () => {
      const mockApi = createMockApi({
        saveSettings: jest.fn().mockResolvedValue({ ok: false, message: 'Failed to save settings' }),
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [{ id: 'r1', name: 'test', format: 'maven2', size: 0, componentCount: 0, isMonitored: true }],
          totalCount: 1,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Make a change and save
      const checkbox = screen.getByRole('checkbox');
      fireEvent.click(checkbox);

      const saveButton = screen.getByRole('button', { name: /save/i });
      await waitFor(() => {
        expect(saveButton).toBeEnabled();
      });

      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByTestId('toast-error')).toHaveTextContent(/Failed to save settings/i);
      });
    });

    it('shows "Saving…" label during in-flight save', async () => {
      let resolveSave: (value: any) => void;
      const savePromise = new Promise(resolve => { resolveSave = resolve; });
      const mockApi = createMockApi({
        saveSettings: jest.fn().mockReturnValue(savePromise),
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [{ id: 'r1', name: 'test', format: 'maven2', size: 0, componentCount: 0, isMonitored: true }],
          totalCount: 1,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getAllByText('Activity Time Frame').length).toBeGreaterThan(0);
      });

      // Make a change and save
      const checkbox = screen.getByRole('checkbox');
      fireEvent.click(checkbox);

      const saveButton = screen.getByRole('button', { name: /save/i });
      await waitFor(() => {
        expect(saveButton).toBeEnabled();
      });

      fireEvent.click(saveButton);

      // Should show "Saving…"
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /saving/i })).toBeInTheDocument();
      });

      // Resolve the save
      resolveSave!({ ok: true, message: 'Saved' });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
      });
    });
  });
});
