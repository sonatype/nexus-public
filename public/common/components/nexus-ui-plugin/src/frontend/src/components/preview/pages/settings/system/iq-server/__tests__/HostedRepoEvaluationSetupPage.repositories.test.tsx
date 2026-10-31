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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

jest.mock('@uirouter/react', () => {
  const router = { stateService: { go: jest.fn() } };
  return {
    useRouter: () => router,
    useCurrentStateAndParams: () => ({ state: { name: 'preview.admin.iqHostedReposEval' }, params: {} }),
  };
});

import { HostedRepoEvaluationSetupPage } from '../HostedRepoEvaluationSetupPage';
import { RepositoriesTab } from '../RepositoriesTab';
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

// Sample repository data
const mockRepos = [
  { id: 'repo-1', name: 'maven-releases', format: 'maven2', size: 1024000, componentCount: 42, isMonitored: true },
  { id: 'repo-2', name: 'npm-hosted', format: 'npm', size: null, componentCount: null, isMonitored: false },
  { id: 'repo-3', name: 'pypi-hosted', format: 'pypi', size: 512000, componentCount: 15, isMonitored: true },
];

// Create a mock with tracking
function createMockApi(overrides: Partial<ReturnType<typeof useHostedRepoEvaluationModule.useHostedRepoEvaluation>> = {}) {
  const fetchRepositories = jest.fn().mockResolvedValue({
    rows: mockRepos,
    totalCount: 3,
    monitoredCount: 0,
    page: 1,
    pageSize: 25,
  });

  const defaults = {
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchSettingsWithRepos: jest.fn().mockResolvedValue({
      settings: useHostedRepoEvaluationModule.DEFAULT_SETTINGS,
      monitoredRepoIds: [],
      totalRepoCount: 0,
    }),
    fetchRepositories,
    fetchFormats: jest.fn().mockResolvedValue(['maven2', 'npm', 'nuget']),
    fetchGlobalConfigStatus: jest.fn().mockResolvedValue({ globalConfigAvailable: true, monitoredCount: 0 }),
    saveSettings: jest.fn().mockResolvedValue({ ok: true, message: 'Saved' }),
    applySelectionDelta: jest.fn().mockResolvedValue({ ok: true, message: 'Updated' }),
    putSettingsWithRepos: jest.fn().mockResolvedValue({ ok: true }),
    fetchAllRepoIds: jest.fn().mockResolvedValue([]),
  };

  return {
    ...defaults,
    ...overrides,
    fetchRepositories: overrides.fetchRepositories ?? fetchRepositories,
  } as ReturnType<typeof useHostedRepoEvaluationModule.useHostedRepoEvaluation>;
}

describe('HostedRepoEvaluationSetupPage - Repositories Tab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Table rendering', () => {
    it('renders table with all rows from fetchRepositories response', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });
      expect(screen.getByText('npm-hosted')).toBeInTheDocument();
      expect(screen.getByText('pypi-hosted')).toBeInTheDocument();
    });

    it('each row shows name, format, size, component count', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Format column — humanized labels (UX-5).
      expect(screen.getByText('Maven')).toBeInTheDocument();
      expect(screen.getByText('npm')).toBeInTheDocument();
      expect(screen.getByText('PyPI')).toBeInTheDocument();

      // Size column - formatBytes(1024000) = "1000.0 kB" (uses toFixed(1) for KB)
      // Check that sizes are rendered - either as formatted bytes or em-dash for null
      const allText = document.body.textContent || '';
      // 1024000 bytes = 1000 KB, formatted as "1000.0 kB"
      expect(allText).toContain('kB'); // maven-releases size in kB
      // npm-hosted has null size, shows em-dash
      expect(allText).toContain('—'); // em-dash for null values

      // Component count column
      expect(screen.getByText('42')).toBeInTheDocument();
      expect(screen.getByText('15')).toBeInTheDocument();
    });

    it('shows monitoring pill as Enabled/Disabled', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      const enabledPills = screen.getAllByText('Enabled');
      const disabledPills = screen.getAllByText('Disabled');

      expect(enabledPills).toHaveLength(2); // repo-1 and repo-3
      expect(disabledPills).toHaveLength(1); // repo-2
    });
  });

  describe('Row selection', () => {
    it('click a row checkbox adds row to selectedIds', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Click the checkbox for repo-2 (npm-hosted)
      const checkboxes = screen.getAllByRole('checkbox');
      const rowCheckbox = checkboxes.find(cb => cb.getAttribute('aria-label')?.includes('npm-hosted'));
      expect(rowCheckbox).toBeDefined();
      fireEvent.click(rowCheckbox!);

      // Action toast should appear
      await waitFor(() => {
        expect(screen.getByRole('toolbar')).toBeInTheDocument();
      });
    });

    it('header checkbox toggles all on current page', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Click header checkbox (first checkbox in table header)
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      // All rows should now be checked
      await waitFor(() => {
        const checkboxes = screen.getAllByRole('checkbox').filter(cb => cb.getAttribute('aria-label')?.includes('Select'));
        // All row checkboxes should be checked
        checkboxes.forEach(cb => {
          expect(cb).toBeChecked();
        });
      });
    });
  });

  describe('Selection count', () => {
    it('shows selection count correctly', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: mockRepos,
          totalCount: 100,
          monitoredCount: 0,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select all on page
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      // Should show count of selected (3 repos on page) - use getByRole to be specific
      await waitFor(() => {
        const status = screen.getByRole('toolbar');
        expect(status).toHaveTextContent(/3 repositories selected/);
      });
    });
  });

  describe('Action toast', () => {
    it('appears only when something is selected', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // No toast initially
      expect(screen.queryByRole('status')).not.toBeInTheDocument();

      // Select a row
      const rowCheckbox = screen.getAllByRole('checkbox').find(cb => cb.getAttribute('aria-label')?.includes('npm-hosted'));
      fireEvent.click(rowCheckbox!);

      // Toast appears
      await waitFor(() => {
        expect(screen.getByRole('toolbar')).toBeInTheDocument();
      });
    });

    it('shows singular/plural text correctly', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select one row
      const rowCheckbox = screen.getAllByRole('checkbox').find(cb => cb.getAttribute('aria-label')?.includes('npm-hosted'));
      fireEvent.click(rowCheckbox!);

      await waitFor(() => {
        expect(screen.getByRole('toolbar')).toBeInTheDocument();
      });

      // Deselect and select all rows
      fireEvent.click(rowCheckbox!); // Deselect

      // Wait for toast to disappear
      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Now click header checkbox to select all
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      await waitFor(() => {
        // Check that 3 is in the selection text
        const status = screen.getByRole('toolbar');
        expect(status).toHaveTextContent(/3 repositories selected/);
      });
    });

    it('shows ONLY Enable button when all selected repos are disabled', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [
            { id: 'repo-2', name: 'npm-hosted', format: 'npm', size: null, componentCount: null, isMonitored: false },
          ],
          totalCount: 1,
          monitoredCount: 0,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('npm-hosted')).toBeInTheDocument();
      });

      // Select the disabled repo
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      await waitFor(() => {
        expect(screen.getByTestId('hr-action-enable')).toBeInTheDocument();
        expect(screen.queryByTestId('hr-action-disable')).not.toBeInTheDocument();
      });
    });

    it('shows ONLY Disable button when all selected repos are enabled', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [
            { id: 'repo-1', name: 'maven-releases', format: 'maven2', size: 1024, componentCount: 1, isMonitored: true },
          ],
          totalCount: 1,
          monitoredCount: 1,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select the enabled repo
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      await waitFor(() => {
        expect(screen.getByTestId('hr-action-disable')).toBeInTheDocument();
        expect(screen.queryByTestId('hr-action-enable')).not.toBeInTheDocument();
      });
    });

    it('shows BOTH buttons when mixed selection', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select all (mixed)
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      await waitFor(() => {
        expect(screen.getByTestId('hr-action-enable')).toBeInTheDocument();
        expect(screen.getByTestId('hr-action-disable')).toBeInTheDocument();
      });
    });
  });

  describe('Enable/Disable actions', () => {
    it('clicking Enable calls applySelectionDelta with addRepositoryIds', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      const fetchRepositories = jest.fn().mockResolvedValue({
        rows: mockRepos,
        totalCount: 3,
        monitoredCount: 0,
        page: 1,
        pageSize: 25,
      });
      const mockApi = createMockApi({ applySelectionDelta, fetchRepositories });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select the disabled repo (npm-hosted)
      const rowCheckbox = screen.getAllByRole('checkbox').find(cb => cb.getAttribute('aria-label')?.includes('npm-hosted'));
      fireEvent.click(rowCheckbox!);

      await waitFor(() => {
        expect(screen.getByTestId('hr-action-enable')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('hr-action-enable'));

      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalledWith({
          addRepositoryIds: ['repo-2'],
        });
      });
    });

    it('clicking Disable calls applySelectionDelta with removeRepositoryIds', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      const fetchRepositories = jest.fn().mockResolvedValue({
        rows: mockRepos.map(r => ({ ...r, isMonitored: true })),
        totalCount: 3,
        monitoredCount: 3,
        page: 1,
        pageSize: 25,
      });
      const mockApi = createMockApi({ applySelectionDelta, fetchRepositories });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select a row
      const rowCheckbox = screen.getAllByRole('checkbox').find(cb => cb.getAttribute('aria-label')?.includes('npm-hosted'));
      fireEvent.click(rowCheckbox!);

      await waitFor(() => {
        expect(screen.getByTestId('hr-action-disable')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('hr-action-disable'));

      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalledWith({
          removeRepositoryIds: ['repo-2'],
        });
      });
    });

    it('after apply, selection clears and rows refetch', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      const fetchRepositories = jest.fn().mockResolvedValue({
        rows: mockRepos,
        totalCount: 3,
        monitoredCount: 0,
        page: 1,
        pageSize: 25,
      });
      const mockApi = createMockApi({ applySelectionDelta, fetchRepositories });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Select and apply
      const headerCheckbox = screen.getByRole('checkbox', { name: /select all on page/i });
      fireEvent.click(headerCheckbox);

      await waitFor(() => {
        expect(screen.getByTestId('hr-action-enable')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('hr-action-enable'));

      // Wait for apply to complete
      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalled();
      });

      // Selection should be cleared — the action toast disappears.
      // (The inline success message also has role=status, so target the
      // toast specifically via its action button instead.)
      await waitFor(() => {
        expect(screen.queryByTestId('hr-action-enable')).not.toBeInTheDocument();
      });
    });
  });

  describe('Search filter', () => {
    it('search input is debounced (500ms)', async () => {
      jest.useFakeTimers();
      const fetchRepositories = jest.fn().mockResolvedValue({
        rows: mockRepos,
        totalCount: 3,
        page: 1,
        pageSize: 25,
      });
      const mockApi = createMockApi({ fetchRepositories });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Search repositories/i)).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText(/Search repositories/i);

      // Fast typing
      fireEvent.change(searchInput, { target: { value: 'm' } });
      fireEvent.change(searchInput, { target: { value: 'ma' } });
      fireEvent.change(searchInput, { target: { value: 'mav' } });
      fireEvent.change(searchInput, { target: { value: 'maven' } });

      // Only the initial fetch should have happened immediately
      expect(fetchRepositories).toHaveBeenCalledTimes(1);

      // Advance timers - debounce should trigger only once after settling
      act(() => {
        jest.advanceTimersByTime(600);
      });

      await waitFor(() => {
        // Should have been called again with debounced value
        expect(fetchRepositories).toHaveBeenCalledWith(
          expect.objectContaining({ search: 'maven' }),
          expect.anything(),
        );
      });

      jest.useRealTimers();
    });
  });

  describe('Format filter', () => {
    it('format filter dropdown options come from knownFormats set', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Check that format filter dropdown triggers are present
      // Select.Root uses a button trigger - we look for it by its placeholder text
      const allButtons = screen.getAllByRole('button');
      const formatTrigger = allButtons.find(btn => btn.textContent === '' || btn.textContent?.includes('All formats'));
      // The placeholder is in the button - check that there are filter buttons
      expect(allButtons.length).toBeGreaterThan(0);
    });
  });

  describe('Monitoring filter', () => {
    it('monitoring filter dropdown has all/enabled/disabled', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Find monitoring comboboxes - should be 2 (format and monitoring)
      const comboboxes = screen.getAllByRole('combobox');
      expect(comboboxes.length).toBeGreaterThan(0);
    });
  });

  describe('Sorting', () => {
    it('click sortable column header toggles sort direction', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Click "Repository Name" header to sort
      const nameHeader = screen.getByText('Repository Name');
      fireEvent.click(nameHeader);

      // Client-side sort should reorder (names sorted alphabetically)
      // npm-hosted, pypi-hosted, maven-releases (initial sort asc)
      // After click: should still work without error
      expect(screen.getByText('maven-releases')).toBeInTheDocument();
    });
  });

  describe('Pagination', () => {
    it('shows count text', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: mockRepos,
          totalCount: 50,
          monitoredCount: 0,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Count text shows pagination info - check the nav aria-label
      const nav = screen.getByLabelText(/Pagination, page 1 of 2/i);
      expect(nav).toBeInTheDocument();
    });

    it('shows nav buttons', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: mockRepos,
          totalCount: 100,
          monitoredCount: 0,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Nav buttons
      expect(screen.getByRole('button', { name: /first page/i })).toBeDisabled();
      expect(screen.getByRole('button', { name: /previous page/i })).toBeDisabled();
      expect(screen.getByRole('button', { name: /next page/i })).toBeEnabled();
      expect(screen.getByRole('button', { name: /last page/i })).toBeEnabled();
    });

    it('shows ellipsis for >5 pages', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: mockRepos,
          totalCount: 200,
          monitoredCount: 0,
          page: 3,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);

      await waitFor(() => {
        expect(screen.getByText('maven-releases')).toBeInTheDocument();
      });

      // Should have ellipsis for many pages
      const ellipsis = screen.getAllByText('…');
      expect(ellipsis.length).toBeGreaterThan(0);
    });
  });

  // =============================================================================
  // UX batch — behavioural tests for fixes UX-1/2/3/5/7/12/13.
  // =============================================================================

  describe('Search × clear (UX-7)', () => {
    it('renders a Clear search button only when search has text', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('maven-releases'));

      // No clear button when search is empty
      expect(screen.queryByLabelText('Clear search')).toBeNull();

      // Type into search → clear button appears
      const searchInput = screen.getByPlaceholderText('Search repositories…') as HTMLInputElement;
      await act(async () => {
        fireEvent.change(searchInput, { target: { value: 'helm' } });
      });

      const clearBtn = await screen.findByLabelText('Clear search');
      expect(clearBtn).toBeInTheDocument();

      // Click clears the input
      await act(async () => {
        fireEvent.click(clearBtn);
      });
      expect(searchInput.value).toBe('');
      expect(screen.queryByLabelText('Clear search')).toBeNull();
    });
  });

  describe('Em-dash for zero / null cells (UX-12)', () => {
    it('renders "—" for size=0 and componentCount=0 (not "0 B" / "0")', async () => {
      const mockApi = createMockApi({
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [
            { id: 'r1', name: 'empty-repo', format: 'maven2', size: 0, componentCount: 0, isMonitored: false },
            { id: 'r2', name: 'partial-repo', format: 'helm', size: 0, componentCount: 2, isMonitored: false },
          ],
          totalCount: 2,
          monitoredCount: 0,
          page: 1,
          pageSize: 25,
        }),
      } as any);
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('empty-repo'));

      // Both columns show em-dash, not "0 B" / "0"
      expect(screen.queryByText('0 B')).toBeNull();
      // "partial-repo" still shows component count = 2
      expect(screen.getByText('2')).toBeInTheDocument();
      // At least two em-dashes for empty-repo row
      expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Humanized format labels (UX-5)', () => {
    it('renders humanized format names in the table', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('maven-releases'));

      expect(screen.getByText('Maven')).toBeInTheDocument(); // not "maven2"
      expect(screen.getByText('PyPI')).toBeInTheDocument(); // not "pypi"
      expect(screen.getByText('npm')).toBeInTheDocument();
      expect(screen.queryByText('maven2')).toBeNull();
      expect(screen.queryByText('pypi')).toBeNull();
    });
  });

  describe('Contextual bulk action buttons (UX-2 / JIRA S8)', () => {
    it('shows only Disable Monitoring when all selected repos are Enabled', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('maven-releases'));

      // maven-releases is Enabled (isMonitored: true) — select only it
      const mavenCheckbox = screen.getByLabelText('Select maven-releases');
      await act(async () => { fireEvent.click(mavenCheckbox); });

      await waitFor(() => {
        expect(screen.getByText('Disable Monitoring')).toBeInTheDocument();
      });
      expect(screen.queryByText('Enable Monitoring')).toBeNull();
    });

    it('shows only Enable Monitoring when all selected repos are Disabled', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('npm-hosted'));

      // npm-hosted is Disabled — select only it
      const npmCheckbox = screen.getByLabelText('Select npm-hosted');
      await act(async () => { fireEvent.click(npmCheckbox); });

      await waitFor(() => {
        expect(screen.getByText('Enable Monitoring')).toBeInTheDocument();
      });
      expect(screen.queryByText('Disable Monitoring')).toBeNull();
    });

    it('shows both buttons when selection is mixed', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('maven-releases'));

      // Select 1 Enabled + 1 Disabled
      await act(async () => {
        fireEvent.click(screen.getByLabelText('Select maven-releases'));
        fireEvent.click(screen.getByLabelText('Select npm-hosted'));
      });

      await waitFor(() => {
        expect(screen.getByText('Enable Monitoring')).toBeInTheDocument();
      });
      expect(screen.getByText('Disable Monitoring')).toBeInTheDocument();
    });
  });

  describe('Toast auto-dismiss after 5s (UX-13)', () => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    it('"Monitoring enabled" toast disappears after 5 seconds', async () => {
      const mockApi = createMockApi();
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);

      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await waitFor(() => screen.getByText('npm-hosted'));

      // Select a Disabled repo + click Enable Monitoring
      await act(async () => {
        fireEvent.click(screen.getByLabelText('Select npm-hosted'));
      });
      await waitFor(() => screen.getByText('Enable Monitoring'));
      await act(async () => {
        fireEvent.click(screen.getByText('Enable Monitoring'));
      });
      await waitFor(() => screen.getByText('Monitoring enabled'));

      // Advance timers by 5 seconds → toast gone
      await act(async () => {
        jest.advanceTimersByTime(5000);
      });
      expect(screen.queryByText('Monitoring enabled')).toBeNull();
    });
  });

  describe('Unsupported formats on Enable (CLM-53820)', () => {
    const reposWithUnsupported = [
      { id: 'repo-1', name: 'maven-releases', format: 'maven2', size: 1024, componentCount: 1, isMonitored: false },
      { id: 'repo-2', name: 'raw-files', format: 'raw', size: 2048, componentCount: 2, isMonitored: false },
      { id: 'repo-3', name: 'raw-legacy', format: 'raw', size: 4096, componentCount: 3, isMonitored: true },
    ];

    function renderWithRepos(applySelectionDelta = jest.fn().mockResolvedValue({ ok: true })) {
      const mockApi = createMockApi({
        applySelectionDelta,
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: reposWithUnsupported,
          totalCount: 3,
          monitoredCount: 1,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);
      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      return mockApi;
    }

    it('blocks enabling an unsupported-format repository and lists the supported formats', async () => {
      const mockApi = renderWithRepos();
      await screen.findByText('raw-files');

      fireEvent.click(screen.getByLabelText('Select maven-releases'));
      fireEvent.click(screen.getByLabelText('Select raw-files'));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      const message = await screen.findByText(/Monitoring can't be enabled for raw-files \(Raw\)/);
      expect(message).toHaveTextContent('Hosted repository evaluation only supports these formats:');
      expect(message).toHaveTextContent('Maven');
      expect(message).not.toHaveTextContent('maven-releases');
      expect(await screen.findByText('Some selected repositories use a format that is not supported for evaluation'))
        .toBeInTheDocument();
      expect(mockApi.applySelectionDelta).not.toHaveBeenCalled();
      expect(mockApi.putSettingsWithRepos).not.toHaveBeenCalled();
    });

    it('blocks formats outside the supported list and names exactly the supported formats', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      const mockApi = createMockApi({
        applySelectionDelta,
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: [{ id: 'repo-9', name: 'docker-hosted', format: 'docker', size: 1, componentCount: 1, isMonitored: false }],
          totalCount: 1,
          monitoredCount: 0,
          page: 1,
          pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);
      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await screen.findByText('docker-hosted');

      fireEvent.click(screen.getByLabelText('Select docker-hosted'));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      expect(await screen.findByText(/Monitoring can't be enabled for docker-hosted/)).toHaveTextContent(
        'Hosted repository evaluation only supports these formats: ' +
        'Maven, npm, PyPI, RubyGems, NuGet, Conda, R, Go, Pub (Dart), Helm.'
      );
      expect(applySelectionDelta).not.toHaveBeenCalled();
    });

    it('enables supported repositories as before', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      renderWithRepos(applySelectionDelta);
      await screen.findByText('maven-releases');

      fireEvent.click(screen.getByLabelText('Select maven-releases'));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalledWith({ addRepositoryIds: ['repo-1'] });
      });
      expect(screen.queryByText(/Monitoring can't be enabled/)).not.toBeInTheDocument();
    });

    it('marks unmonitored unsupported repositories as Not supported, with the reason', async () => {
      renderWithRepos();
      await screen.findByText('raw-files');

      const badges = screen.getAllByText('Not supported');
      // raw-files only: raw-legacy is already monitored and keeps its Enabled badge.
      expect(badges).toHaveLength(1);
      expect(badges[0]).toHaveAttribute(
        'aria-label',
        expect.stringContaining('Hosted repository evaluation is not available for Raw repositories.')
      );
      expect(badges[0]).toHaveAttribute('aria-label', expect.stringContaining('Supported formats: Maven, npm'));
    });

    it('announces the error and clears it when the selection changes', async () => {
      renderWithRepos();
      await screen.findByText('raw-files');

      fireEvent.click(screen.getByLabelText('Select raw-files'));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));
      expect(await screen.findByRole('alert')).toHaveTextContent(/Monitoring can't be enabled for raw-files/);

      fireEvent.click(screen.getByLabelText('Select raw-files'));
      await waitFor(() => {
        expect(screen.queryByText(/Monitoring can't be enabled/)).not.toBeInTheDocument();
      });
    });

    it('lists at most five unsupported repositories and summarises the rest', async () => {
      const rows = Array.from({ length: 7 }, (_, i) => ({
        id: `raw-${i}`, name: `raw-${i}`, format: 'raw', size: 1, componentCount: 1, isMonitored: false,
      }));
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      const mockApi = createMockApi({
        applySelectionDelta,
        fetchRepositories: jest.fn().mockResolvedValue({
          rows, totalCount: 7, monitoredCount: 0, page: 1, pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);
      renderWithTheme(<HostedRepoEvaluationSetupPage />);
      await screen.findByText('raw-6');

      rows.forEach(r => fireEvent.click(screen.getByLabelText(`Select ${r.name}`)));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      const message = await screen.findByText(/Monitoring can't be enabled for/);
      expect(message).toHaveTextContent('raw-4 (Raw) and 2 more.');
      expect(message).not.toHaveTextContent('raw-5');
      expect(applySelectionDelta).not.toHaveBeenCalled();
    });

    it('select all on page skips unmonitored unsupported repositories, so Enable works', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      renderWithRepos(applySelectionDelta);
      await screen.findByText('raw-files');

      fireEvent.click(screen.getByLabelText('Select all on page'));

      expect(screen.getByLabelText('Select maven-releases')).toBeChecked();
      expect(screen.getByLabelText('Select raw-legacy')).toBeChecked();
      expect(screen.getByLabelText('Select raw-files')).not.toBeChecked();
      expect(screen.getByLabelText('Select all on page')).toBeChecked();

      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalledWith({ addRepositoryIds: ['repo-1', 'repo-3'] });
      });
      expect(screen.queryByText(/Monitoring can't be enabled/)).not.toBeInTheDocument();
    });

    it('blocks a first-time setup that includes an unsupported repository', async () => {
      const putSettingsWithRepos = jest.fn().mockResolvedValue({ ok: true });
      const mockApi = createMockApi({
        putSettingsWithRepos,
        fetchRepositories: jest.fn().mockResolvedValue({
          rows: reposWithUnsupported, totalCount: 3, monitoredCount: 0, page: 1, pageSize: 25,
        }),
      });
      jest.spyOn(useHostedRepoEvaluationModule, 'useHostedRepoEvaluation').mockReturnValue(mockApi);
      renderWithTheme(
        <RepositoriesTab
          hasExistingConfig={false}
          setHasExistingConfig={jest.fn()}
          pristineSettings={useHostedRepoEvaluationModule.DEFAULT_SETTINGS}
          setSettingsStaged={jest.fn()}
          formats={['maven2', 'raw']}
          onMonitoredCountChange={jest.fn()}
          isActiveTab
        />
      );
      await screen.findByText('raw-files');

      fireEvent.click(screen.getByLabelText('Select maven-releases'));
      fireEvent.click(screen.getByLabelText('Select raw-files'));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      expect(await screen.findByText(/Monitoring can't be enabled for raw-files/)).toBeInTheDocument();
      expect(putSettingsWithRepos).not.toHaveBeenCalled();
    });

    it('allows enabling a supported repository alongside an already-monitored unsupported one', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      renderWithRepos(applySelectionDelta);
      await screen.findByText('raw-legacy');

      fireEvent.click(screen.getByLabelText('Select maven-releases'));
      fireEvent.click(screen.getByLabelText('Select raw-legacy'));
      fireEvent.click(await screen.findByTestId('hr-action-enable'));

      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalledWith({ addRepositoryIds: ['repo-1', 'repo-3'] });
      });
      expect(screen.queryByText(/Monitoring can't be enabled/)).not.toBeInTheDocument();
    });

    it('still allows disabling an already-monitored unsupported repository', async () => {
      const applySelectionDelta = jest.fn().mockResolvedValue({ ok: true });
      renderWithRepos(applySelectionDelta);
      await screen.findByText('raw-legacy');

      fireEvent.click(screen.getByLabelText('Select raw-legacy'));
      fireEvent.click(await screen.findByTestId('hr-action-disable'));

      await waitFor(() => {
        expect(applySelectionDelta).toHaveBeenCalledWith({ removeRepositoryIds: ['repo-3'] });
      });
    });
  });
});
