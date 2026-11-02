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
import { render, screen, waitFor, within, fireEvent, } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoriesList } from '../RepositoriesList';
import { useRepositoriesApi } from '../useRepositoriesApi';
import { ToastProvider } from '../../../../../shared';
import { restClient, ENDPOINTS } from '../../../../../../../interface/api';

// Mock Radix UI DropdownMenu to avoid portal issues in tests
// (Portals don't render reliably in jsdom - see SettingsSelect.test.tsx for same pattern)
// We only mock DropdownMenu while preserving all other Radix UI components
jest.mock('@radix-ui/themes', () => {
  const actual = jest.requireActual('@radix-ui/themes');
  return {
    ...actual,
    DropdownMenu: {
      Root: ({ children, ...props }: any) => <div data-testid="dropdown-root" {...props}>{children}</div>,
      Trigger: ({ children, ...props }: any) => <div {...props}>{children}</div>,
      Content: ({ children, align, ...props }: any) => (
        <div data-testid="dropdown-content" data-align={align} {...props}>
          {children}
        </div>
      ),
      Item: ({ children, onClick, color, ...props }: any) => (
        <button
          {...props}
          onClick={onClick}
          data-color={color}
          style={{ display: 'block', width: '100%', textAlign: 'left' }}
        >
          {children}
        </button>
      ),
    },
  };
});

// Mock @uirouter/react to avoid UIRouter context requirement in RepositoryListTable
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: jest.fn() },
  }),
}));

// Mock the API hook
jest.mock('../useRepositoriesApi');
const mockUseRepositoriesApi = useRepositoriesApi as jest.MockedFunction<typeof useRepositoriesApi>;

// Mock ExtJS state at module scope, read by a STABLE getValue implementation, reset in
// beforeEach. This avoids the mock-leak that `ExtJS.state.mockReturnValue(...)` caused: with
// jest.config.js's `clearMocks: true`, mockClear() resets call history but NOT configured
// return values, so a per-test mockReturnValue override used to leak into every later test in
// this file (NEXUS-55079 A3). Pattern mirrors RepositorySettingsUsageTab.test.tsx.
const mockExtState: Record<string, unknown> = { clm: { enabled: true } };

// Mock ExtJS - include state for isIqServerEnabled (Firewall column visibility) and useState
// for useIsRepositorySizeSupported (Size column visibility).
// The source imports from relative path, so mock needs to target that path
// ExtJS has both default and named export
jest.mock('../../../../../../../interface/ExtJS', () => {
  const mockExtJS = {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockImplementation((key: string) => mockExtState[key]),
    }),
    useState: jest.fn((getValue: () => unknown) => getValue()),
    usePermission: jest.fn((getValue: () => unknown) => getValue()),
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  };
  return {
    __esModule: true,
    default: mockExtJS,
    ExtJS: mockExtJS,
  };
});

const mockRepositories = [
  {
    name: 'maven-central',
    type: 'proxy',
    format: 'maven2',
    url: 'http://localhost:8081/repository/maven-central/',
    online: true,
    status: { online: true },
  },
  {
    name: 'maven-releases',
    type: 'hosted',
    format: 'maven2',
    url: 'http://localhost:8081/repository/maven-releases/',
    online: true,
    status: { online: true },
  },
  {
    name: 'npm-proxy',
    type: 'proxy',
    format: 'npm',
    url: 'http://localhost:8081/repository/npm-proxy/',
    online: false,
    status: { online: false, description: 'Remote unavailable' },
  },
];

const mockApiHook = {
  loading: false,
  error: null,
  setError: jest.fn(),
  fetchRepositories: jest.fn().mockResolvedValue(mockRepositories),
  fetchRepository: jest.fn().mockImplementation((name: string) => {
    const repo = mockRepositories.find((r) => r.name === name);
    return Promise.resolve(repo ? { ...repo, status: { online: false } } : null);
  }),
  fetchHealthCheckStatus: jest.fn().mockResolvedValue({}),
  enableHealthCheck: jest.fn().mockResolvedValue(undefined),
  deleteRepository: jest.fn().mockResolvedValue(undefined),
  invalidateCache: jest.fn().mockResolvedValue(undefined),
  rebuildIndex: jest.fn().mockResolvedValue(undefined),
  setRepositoryOnline: jest.fn().mockResolvedValue(undefined),
};

const renderWithTheme = (component: React.ReactElement) => {
  const container = document.createElement('div');
  document.body.appendChild(container);

  return render(
    <Theme>
      <ToastProvider>
        {component}
      </ToastProvider>
    </Theme>,
    { container, baseElement: document.body }
  );
};

describe('RepositoriesList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRepositoriesApi.mockReturnValue(mockApiHook as any);
    Object.keys(mockExtState).forEach((key) => delete mockExtState[key]);
    mockExtState.clm = { enabled: true };
  });

  it('renders loading state initially', () => {
    mockUseRepositoriesApi.mockReturnValue({
      ...mockApiHook,
      fetchRepositories: jest.fn().mockImplementation(() => new Promise(() => {})),
    } as any);

    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    // RepositoriesList shows loading message
    expect(screen.getByText('Loading repositories...')).toBeInTheDocument();
  });

  it('renders repositories in a table', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.getByText('maven-releases')).toBeInTheDocument();
      expect(screen.getByText('npm-proxy')).toBeInTheDocument();
    });
  });

  it('displays repository types correctly', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      // Type badges in table cells
      expect(screen.getAllByText('Proxy').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Hosted').length).toBeGreaterThan(0);
    });
  });

  it('displays repository formats correctly', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      // Format labels in the table
      // RepositoryListTable uses FORMAT_LABELS to display formats
      expect(screen.getAllByText('Maven').length).toBeGreaterThan(0);
      expect(screen.getAllByText('npm').length).toBeGreaterThan(0);
    });
  });

  it('filters repositories by search term', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    // New search input placeholder
    const searchInput = screen.getByPlaceholderText(/search repositories by name/i);
    await userEvent.type(searchInput, 'npm');

    await waitFor(() => {
      expect(screen.queryByText('maven-central')).not.toBeInTheDocument();
      expect(screen.getByText('npm-proxy')).toBeInTheDocument();
    });
  });

  it('filters repositories by type using checkbox filter', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    // FilterSidebar uses checkboxes, find and click the hosted checkbox
    const filterSidebar = screen.getByTestId('filter-sidebar');
    expect(filterSidebar).toBeInTheDocument();
    
    // The checkboxes are labeled by their filter option labels
    // Click the "Hosted" checkbox to filter
    const hostedCheckbox = screen.getByRole('checkbox', { name: /hosted/i });
    await userEvent.click(hostedCheckbox);

    await waitFor(() => {
      expect(screen.queryByText('maven-central')).not.toBeInTheDocument();
      expect(screen.getByText('maven-releases')).toBeInTheDocument();
    });
  });

  it('filters repositories by format using checkbox filter', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    // Click the "npm" format checkbox to filter
    const npmCheckbox = screen.getByRole('checkbox', { name: /npm/i });
    await userEvent.click(npmCheckbox);

    await waitFor(() => {
      expect(screen.queryByText('maven-central')).not.toBeInTheDocument();
      expect(screen.getByText('npm-proxy')).toBeInTheDocument();
    });
  });

  it('sorts repositories by name when clicking header', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    // EntityTable renders sortable column headers
    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    await userEvent.click(nameHeader);
    
    // Table should still render after sorting
    expect(screen.getByText('maven-central')).toBeInTheDocument();
  });

  it('calls onSelect when clicking a repository row', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    // EntityTable renders clickable rows - click the repository name text
    // RepositoryListTable renders clickable rows
    const repoName = screen.getByText('maven-central');
    await userEvent.click(repoName);
    expect(mockOnSelect).toHaveBeenCalledWith('maven-central');
  });

  describe('URL copy functionality', () => {
    it('adds trailing slash to URL when copying from URL column button', async () => {
      // Mock repository without trailing slash
      const mockRepoWithoutSlash = [
        {
          name: 'test-repo',
          type: 'proxy',
          format: 'maven2',
          url: 'http://localhost:8081/repository/test-repo',
          online: true,
          status: { online: true },
        },
      ];

      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepositories: jest.fn().mockResolvedValue(mockRepoWithoutSlash),
      } as any);

      // Mock clipboard API
      const mockWriteText = jest.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('test-repo')).toBeInTheDocument();
      });

      // Find the copy button by aria-label (there's only one repo)
      const copyButtons = screen.getAllByLabelText('Copy URL to Clipboard');
      await userEvent.click(copyButtons[0]);

      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalledWith('http://localhost:8081/repository/test-repo/');
      });
    });

    it('preserves existing trailing slash when copying URL', async () => {
      // Mock clipboard API
      const mockWriteText = jest.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      // Find the copy button for maven-central (first row)
      const copyButtons = screen.getAllByLabelText('Copy URL to Clipboard');
      await userEvent.click(copyButtons[0]);

      await waitFor(() => {
        // URL already has trailing slash in mock data
        expect(mockWriteText).toHaveBeenCalledWith('http://localhost:8081/repository/maven-central/');
      });
    });

    it('adds trailing slash when copying URL from dropdown menu', async () => {
      // Mock repository without trailing slash
      const mockRepoWithoutSlash = [
        {
          name: 'test-repo-2',
          type: 'proxy',
          format: 'maven2',
          url: 'http://localhost:8081/repository/test-repo-2',
          online: true,
          status: { online: true },
        },
      ];

      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepositories: jest.fn().mockResolvedValue(mockRepoWithoutSlash),
      } as any);

      // Mock clipboard API
      const mockWriteText = jest.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('test-repo-2')).toBeInTheDocument();
      });

      // Find and click the Copy URL menu item (mocked dropdown renders inline)
      const copyUrlMenuItem = screen.getByTestId('repo-action-copy-url-test-repo-2');
      await userEvent.click(copyUrlMenuItem);

      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalledWith('http://localhost:8081/repository/test-repo-2/');
      });
    });

    it('handles clipboard write failure gracefully', async () => {
      // Mock repository without trailing slash
      const mockRepoWithoutSlash = [
        {
          name: 'test-repo-3',
          type: 'proxy',
          format: 'maven2',
          url: 'http://localhost:8081/repository/test-repo-3',
          online: true,
          status: { online: true },
        },
      ];

      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepositories: jest.fn().mockResolvedValue(mockRepoWithoutSlash),
      } as any);

      // Mock clipboard API to reject
      const mockWriteText = jest.fn().mockRejectedValue(new Error('Clipboard not available'));
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('test-repo-3')).toBeInTheDocument();
      });

      // Find the copy button
      const copyButtons = screen.getAllByLabelText('Copy URL to Clipboard');
      await userEvent.click(copyButtons[0]);

      await waitFor(() => {
        // Should show error toast
        expect(screen.getByText('Failed to copy URL to clipboard')).toBeInTheDocument();
      });
    });
  });

  it('displays online/offline status correctly', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      // Each online repo has 1 "Online" text element
      // 2 online repos × 1 = 2 "Online" text elements
      expect(screen.getAllByText('Online').length).toBe(2);
      // Offline repo shows "Offline"
      expect(screen.getByText('Offline')).toBeInTheDocument();
    });
  });

  it('displays empty state when no repositories', async () => {
    mockUseRepositoriesApi.mockReturnValue({
      ...mockApiHook,
      fetchRepositories: jest.fn().mockResolvedValue([]),
    } as any);

    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      // EmptyState component shows this
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
      expect(screen.getByText('No Repositories')).toBeInTheDocument();
    });
  });

  it('displays error state', async () => {
    mockUseRepositoriesApi.mockReturnValue({
      ...mockApiHook,
      error: 'Failed to load repositories',
      fetchRepositories: jest.fn().mockRejectedValue(new Error('Failed')),
    } as any);

    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      // ErrorState component
      expect(screen.getByTestId('error-state')).toBeInTheDocument();
    });
  });

  it('displays summary of filtered results', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByText(/showing 3 of 3 repositories/i)).toBeInTheDocument();
    });
  });

  it('renders filter sidebar', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByTestId('filter-sidebar')).toBeInTheDocument();
    });
  });

  it('renders help section', async () => {
    renderWithTheme(
      <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
    );

    await waitFor(() => {
      expect(screen.getByTestId('help-section')).toBeInTheDocument();
      expect(screen.getByText('About Repositories')).toBeInTheDocument();
    });
  });

  describe('Health Check and Firewall columns', () => {
    it('shows Health Check column when user has permission', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const healthCheckHeader = screen.queryByRole('columnheader', { name: /health check/i });
      expect(healthCheckHeader).toBeInTheDocument();
    });

    it('shows Firewall Report column when IQ Server is enabled', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const firewallHeader = screen.queryByRole('columnheader', { name: /firewall report/i });
      expect(firewallHeader).toBeInTheDocument();
    });

    it('hides Health Check column when user lacks permission', async () => {
      const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:healthcheck:update') return false;
        return true;
      });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const healthCheckHeader = screen.queryByRole('columnheader', { name: /health check/i });
      expect(healthCheckHeader).not.toBeInTheDocument();
    });

    it('hides Firewall Report column when IQ Server is disabled', async () => {
      mockExtState.clm = { enabled: false };

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const firewallHeader = screen.queryByRole('columnheader', { name: /firewall report/i });
      expect(firewallHeader).not.toBeInTheDocument();
    });

    // NEXUS-53278: when IQ Server is configured AND Firewall is licensed, the Health Check
    // column is superseded by Firewall Report — the classic UI has always hidden it in this state
    // (see NX.Conditions.hasNoFirewall in ExtJS), and the preview UI must match.
    it('hides Health Check column when IQ Server is enabled and Firewall is licensed', async () => {
      mockExtState.clm = { enabled: true, hasFirewall: true };

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const healthCheckHeader = screen.queryByRole('columnheader', { name: /health check/i });
      expect(healthCheckHeader).not.toBeInTheDocument();
    });

    // NEXUS-53278 boundary: IQ Server on but no Firewall license (e.g. Sonatype Lifecycle only) —
    // Health Check must still be visible; it is only redundant when Firewall is what the customer paid for.
    // checkPermission is reset explicitly because a sibling test above installs a
    // mockImplementation, and jest.config.js's `clearMocks` clears call history but NOT
    // implementations, so it would otherwise leak into this test.
    it('shows Health Check column when IQ Server is enabled but Firewall is not licensed', async () => {
      const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');
      ExtJS.checkPermission.mockReturnValue(true);
      mockExtState.clm = { enabled: true, hasFirewall: false };

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const healthCheckHeader = screen.queryByRole('columnheader', { name: /health check/i });
      expect(healthCheckHeader).toBeInTheDocument();
    });

    it('fetches firewall status from the lightweight summary endpoint, not the full IQ endpoint', async () => {
      const FIREWALL_STATUS_FULL_URL = '/service/rest/internal/ui/firewall/status';
      const FIREWALL_STATUS_SUMMARY_URL = '/service/rest/internal/ui/firewall/status/summary';
      const getSpy = jest.spyOn(restClient, 'get').mockResolvedValue([]);

      try {
        renderWithTheme(
          <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
        );

        await waitFor(() => {
          expect(screen.getByText('maven-central')).toBeInTheDocument();
        });

        // Sanity-check the constants are wired up the way this assertion assumes.
        expect(ENDPOINTS.FIREWALL_STATUS).toBe(FIREWALL_STATUS_FULL_URL);
        expect(ENDPOINTS.FIREWALL_STATUS_SUMMARY).toBe(FIREWALL_STATUS_SUMMARY_URL);

        await waitFor(() => {
          expect(getSpy).toHaveBeenCalledWith(ENDPOINTS.FIREWALL_STATUS_SUMMARY);
        });
        expect(getSpy).not.toHaveBeenCalledWith(ENDPOINTS.FIREWALL_STATUS);
      } finally {
        getSpy.mockRestore();
      }
    });
  });

  describe('Firewall violation counts in the settings list (NEXUS-54499 review)', () => {
    const PROXY_REPO = 'maven-central';
    const VIOLATION_COUNT = 7;
    const SUMMARY_URL = '/service/rest/internal/ui/firewall/status/summary';
    const REPO_STATUS_FRAGMENT = `firewall/status/repo/${PROXY_REPO}`;
    const enabledSummary = [{
      repositoryName: PROXY_REPO,
      affectedComponentCount: 0,
      criticalComponentCount: 0,
      severeComponentCount: 0,
      moderateComponentCount: 0,
      quarantinedComponentCount: 0,
      message: 'Quarantine enabled',
    }];

    function stubFirewall(repoStatus: () => Promise<unknown>) {
      return jest.spyOn(restClient, 'get').mockImplementation((url: string) => {
        if (url === SUMMARY_URL) return Promise.resolve(enabledSummary);
        if (String(url).includes(REPO_STATUS_FRAGMENT)) return repoStatus();
        return Promise.resolve([]);
      });
    }

    async function openFirewallDialog() {
      renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
      const cell = await screen.findByRole('button', { name: /Firewall: Quarantine/i });
      fireEvent.click(cell);
    }

    it('shouldShowViolationCountsForEnabledRepoInSettingsList', async () => {
      const getSpy = stubFirewall(() => Promise.resolve({
        ...enabledSummary[0],
        affectedComponentCount: VIOLATION_COUNT,
        criticalComponentCount: VIOLATION_COUNT,
      }));
      try {
        await openFirewallDialog();
        expect(await screen.findByText(`Firewall Report \u2013 ${PROXY_REPO}`)).toBeInTheDocument();
        expect(screen.getAllByText(String(VIOLATION_COUNT)).length).toBeGreaterThan(0);
        expect(screen.queryByText('No issues found')).not.toBeInTheDocument();
      } finally {
        getSpy.mockRestore();
      }
    });

    it('shouldRefreshFirewallStatusInRowAfterEnablingFromSettingsList', async () => {
      let enabled = false;
      const getSpy = jest.spyOn(restClient, 'get').mockImplementation((url: string) => {
        if (url === SUMMARY_URL) return Promise.resolve(enabled ? enabledSummary : []);
        if (url === `/service/rest/v1/repositories/${PROXY_REPO}`) return Promise.resolve({ format: 'maven2', type: 'proxy' });
        if (url === `/service/rest/v1/repositories/maven/proxy/${PROXY_REPO}`) {
          return Promise.resolve({ name: PROXY_REPO, firewall: { mode: 'DISABLED' } });
        }
        // setFirewallMode reads the repo from the internal UI endpoint (FIRE-1149) before the PUT.
        if (url === `/service/rest/internal/ui/repositories/repository/${PROXY_REPO}`) {
          return Promise.resolve({ name: PROXY_REPO, format: 'maven2', type: 'proxy', firewall: { mode: 'DISABLED' } });
        }
        if (String(url).includes(REPO_STATUS_FRAGMENT)) return Promise.resolve(enabledSummary[0]);
        return Promise.resolve([]);
      });
      const putSpy = jest.spyOn(restClient, 'put').mockImplementation(async () => { enabled = true; });
      try {
        renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
        const row = (await screen.findByText(PROXY_REPO)).closest('tr') as HTMLElement;
        fireEvent.click(await within(row).findByRole('button', { name: /Firewall: Unprotected/i }));
        fireEvent.click(await screen.findByText('Enable Firewall Protection'));
        fireEvent.click(await screen.findByRole('radio', { name: /Quarantine/ }));
        fireEvent.click(screen.getByRole('button', { name: 'Enable' }));

        expect(await within(row).findByRole('button', { name: /Firewall: Quarantine/i })).toBeInTheDocument();
        expect(within(row).queryByRole('button', { name: /Firewall: Unprotected/i })).not.toBeInTheDocument();
      } finally {
        getSpy.mockRestore();
        putSpy.mockRestore();
      }
    });

    it('shouldNotShowCleanReportWhenCountsFailForEnabledRepoInSettingsList', async () => {
      const getSpy = stubFirewall(() => Promise.reject(new Error('IQ down')));
      try {
        await openFirewallDialog();
        expect(await screen.findByText(/Violation counts could not be loaded/)).toBeInTheDocument();
        expect(screen.queryByText('No issues found')).not.toBeInTheDocument();
      } finally {
        getSpy.mockRestore();
      }
    });
  });

  // NEXUS-55079: repository size is a Pro-only feature (RepositoryMetricsFeatureCheck requires a
  // license AND Postgres AND a finished CopyBlobSizeTask). The backend publishes the resolved answer
  // as the `nexus.repository.size` state key; when it is not true the column must be hidden outright
  // rather than rendering an unpopulated size, matching the classic UI (RepositoryList.js:40).
  describe('Size column (NEXUS-55079)', () => {
    const mockStateValues = (values: Record<string, unknown>) => {
      Object.keys(mockExtState).forEach((key) => delete mockExtState[key]);
      Object.assign(mockExtState, values);
    };

    it('shouldHideSizeColumnWhenRepositorySizeStateIsAbsent', async () => {
      mockStateValues({ clm: { enabled: true } });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      expect(screen.queryByRole('columnheader', { name: /size/i })).not.toBeInTheDocument();
    });

    it('shouldHideSizeColumnWhenRepositorySizeIsFalse', async () => {
      mockStateValues({ clm: { enabled: true }, 'nexus.repository.size': false });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      expect(screen.queryByRole('columnheader', { name: /size/i })).not.toBeInTheDocument();
    });

    it('shouldShowSizeColumnWhenRepositorySizeIsTrue', async () => {
      mockStateValues({ clm: { enabled: true }, 'nexus.repository.size': true });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      expect(screen.getByRole('columnheader', { name: /size/i })).toBeInTheDocument();
    });

    // A repository whose size has not been computed yet (CopyBlobSizeTask still running for that
    // repository) comes back with no `size` field. That is "unknown", not zero, so it must not be
    // rendered as "0 B" even while the column is legitimately visible.
    // The Size column is gated in two places that must stay in step: the header cell and the body
    // cell. Gating only one of them yields a table whose rows are offset from their headers - every
    // value lands under the wrong label - which renders without error and is easy to miss by eye.
    // Assert header count against body cell count rather than against a literal, so the test cannot
    // drift by making the same off-by-one the code would.
    const expectHeaderAndBodyCellCountsMatch = async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const headerCount = screen.getAllByRole('columnheader').length;
      const firstRow = screen.getByText('maven-central').closest('tr') as HTMLElement;
      expect(firstRow).not.toBeNull();
      expect(firstRow.querySelectorAll('td, th').length).toBe(headerCount);
      return headerCount;
    };

    it('shouldKeepHeaderAndBodyCellCountsAlignedWhenSizeColumnIsHidden', async () => {
      mockStateValues({ clm: { enabled: true } });

      await expectHeaderAndBodyCellCountsMatch();

      expect(screen.queryByRole('columnheader', { name: /size/i })).not.toBeInTheDocument();
    });

    it('shouldKeepHeaderAndBodyCellCountsAlignedWhenSizeColumnIsVisible', async () => {
      mockStateValues({ clm: { enabled: true }, 'nexus.repository.size': true });

      await expectHeaderAndBodyCellCountsMatch();

      expect(screen.getByRole('columnheader', { name: /size/i })).toBeInTheDocument();
    });

    // Asserting the counts match in each state still permits the gate adding or removing the wrong
    // NUMBER of columns, as long as header and body agree. Pin the delta to exactly one so that a
    // gate accidentally wrapping a neighbouring column is caught too. Absolute counts are left
    // unasserted deliberately: the health-check and firewall columns are independently gated, so a
    // literal here would break whenever an unrelated column is added.
    it('shouldAddExactlyOneColumnWhenRepositorySizeBecomesSupported', async () => {
      mockStateValues({ clm: { enabled: true } });
      const { unmount } = renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );
      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });
      const withoutSize = screen.getAllByRole('columnheader').length;
      unmount();

      mockStateValues({ clm: { enabled: true }, 'nexus.repository.size': true });
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );
      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });
      const withSize = screen.getAllByRole('columnheader').length;

      expect(withSize).toBe(withoutSize + 1);
    });

    it('shouldRenderPlaceholderRatherThanZeroForUnknownSize', async () => {
      mockStateValues({ clm: { enabled: true }, 'nexus.repository.size': true });
      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepositories: jest.fn().mockResolvedValue([
          { ...mockRepositories[0], size: undefined },
          { ...mockRepositories[1], size: 0 },
        ]),
      } as any);

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const unknownSizeRow = screen.getByText('maven-central').closest('tr') as HTMLElement;
      expect(within(unknownSizeRow).getByText('-')).toBeInTheDocument();
      expect(within(unknownSizeRow).queryByText('0 B')).not.toBeInTheDocument();

      // A genuinely empty repository still reads as "0 B" — only unknown becomes a placeholder.
      const emptyRepoRow = screen.getByText('maven-releases').closest('tr') as HTMLElement;
      expect(within(emptyRepoRow).getByText('0 B')).toBeInTheDocument();
    });
  });

  // Helper function to open delete dialog via dropdown menu
  const openDeleteDialog = async (repositoryName: string) => {
    // Find and click the Delete menu item (mocked dropdown renders inline)
    const deleteMenuItem = screen.getByTestId(`repo-action-delete-${repositoryName}`);
    fireEvent.click(deleteMenuItem);
  };

  describe('delete repository', () => {
    it('opens delete confirmation dialog when delete button is clicked', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      await openDeleteDialog('maven-central');

      await waitFor(() => {
        // DeleteConfirmationModal renders with role="alertdialog" via Radix AlertDialog
        const dialog = screen.getByRole('alertdialog');
        expect(dialog).toBeInTheDocument();
        // Type-to-confirm modal shows entity name in the warning box
        expect(within(dialog).getByText('maven-central')).toBeInTheDocument();
        expect(within(dialog).getByText(/Delete repository\?/i)).toBeInTheDocument();
      });
    });

    it('calls deleteRepository and removes repo from list on confirm', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      await openDeleteDialog('maven-central');

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      // Acknowledgement is the literal word "Delete" (case-insensitive) for every
      // entity now (NEXUS-53356 — DeleteConfirmationModal no longer demands the name).
      const confirmInput = screen.getByPlaceholderText(/type "delete" to confirm/i);
      await userEvent.type(confirmInput, 'Delete');

      // Click the Delete button
      const confirmBtn = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmBtn);

      await waitFor(() => {
        expect(mockApiHook.deleteRepository).toHaveBeenCalledWith('maven-central');
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('calls onDelete prop when provided', async () => {
      const mockOnDelete = jest.fn().mockResolvedValue(undefined);

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} onDelete={mockOnDelete} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      await openDeleteDialog('maven-central');

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      // Acknowledgement is the literal word "Delete" (case-insensitive) for every
      // entity now (NEXUS-53356 — DeleteConfirmationModal no longer demands the name).
      const confirmInput = screen.getByPlaceholderText(/type "delete" to confirm/i);
      await userEvent.type(confirmInput, 'Delete');

      // Click the Delete button
      const confirmBtn = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmBtn);

      await waitFor(() => {
        expect(mockOnDelete).toHaveBeenCalledWith('maven-central');
        expect(mockApiHook.deleteRepository).not.toHaveBeenCalled();
      });
    });

    it('cancels delete when Cancel button is clicked', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      await openDeleteDialog('maven-central');

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      const cancelBtn = screen.getByRole('button', { name: /Cancel/i });
      await userEvent.click(cancelBtn);

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(mockApiHook.deleteRepository).not.toHaveBeenCalled();
      });
    });
  });

  // NEXUS-53946: post-Repository-Profile migration the row menu drops
  // "View Profile" and adds the operational actions (rebuild index /
  // invalidate cache / toggle online). Visibility rules match the Settings
  // form: rebuild = hosted|proxy, invalidate = proxy|group, toggle = all.
  describe('row action menu (NEXUS-53946)', () => {
    it('does not render View Profile menu item', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      expect(screen.queryByTestId('repo-action-view-maven-central')).not.toBeInTheDocument();
      expect(screen.queryByText('View Profile')).not.toBeInTheDocument();
    });

    it('shows Rebuild Index for hosted and proxy rows but not group', async () => {
      const reposWithGroup = [
        ...mockRepositories,
        {
          name: 'maven-group',
          type: 'group',
          format: 'maven2',
          url: 'http://localhost:8081/repository/maven-group/',
          online: true,
          status: { online: true },
        },
      ];
      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepositories: jest.fn().mockResolvedValue(reposWithGroup),
      } as any);

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-group')).toBeInTheDocument();
      });

      expect(screen.getByTestId('repo-action-rebuild-index-maven-central')).toBeInTheDocument();
      expect(screen.getByTestId('repo-action-rebuild-index-maven-releases')).toBeInTheDocument();
      expect(screen.queryByTestId('repo-action-rebuild-index-maven-group')).not.toBeInTheDocument();
    });

    it('shows Invalidate Cache for proxy and group rows but not hosted', async () => {
      const reposWithGroup = [
        ...mockRepositories,
        {
          name: 'maven-group',
          type: 'group',
          format: 'maven2',
          url: 'http://localhost:8081/repository/maven-group/',
          online: true,
          status: { online: true },
        },
      ];
      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepositories: jest.fn().mockResolvedValue(reposWithGroup),
      } as any);

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-group')).toBeInTheDocument();
      });

      expect(screen.getByTestId('repo-action-invalidate-cache-maven-central')).toBeInTheDocument();
      expect(screen.getByTestId('repo-action-invalidate-cache-maven-group')).toBeInTheDocument();
      expect(screen.queryByTestId('repo-action-invalidate-cache-maven-releases')).not.toBeInTheDocument();
    });

    it('shows the toggle-online item for every row', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      // online repos show "Take Offline"; offline npm-proxy shows "Bring Online"
      const central = screen.getByTestId('repo-action-toggle-online-maven-central');
      expect(central).toHaveTextContent('Take Offline');
      const releases = screen.getByTestId('repo-action-toggle-online-maven-releases');
      expect(releases).toHaveTextContent('Take Offline');
      const npm = screen.getByTestId('repo-action-toggle-online-npm-proxy');
      expect(npm).toHaveTextContent('Bring Online');
    });

    it('opens confirm dialog and calls rebuildIndex on confirm', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('repo-action-rebuild-index-maven-central'));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      const confirmBtn = screen.getByRole('button', { name: /rebuild index/i });
      await userEvent.click(confirmBtn);

      await waitFor(() => {
        expect(mockApiHook.rebuildIndex).toHaveBeenCalledWith('maven-central');
      });
    });

    it('opens confirm dialog and calls invalidateCache on confirm', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('repo-action-invalidate-cache-maven-central'));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      const confirmBtn = screen.getByRole('button', { name: /invalidate cache/i });
      await userEvent.click(confirmBtn);

      await waitFor(() => {
        expect(mockApiHook.invalidateCache).toHaveBeenCalledWith('maven-central');
      });
    });

    it('opens confirm dialog and calls setRepositoryOnline on confirm', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('repo-action-toggle-online-maven-central'));

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      const confirmBtn = screen.getByRole('button', { name: /take offline/i });
      await userEvent.click(confirmBtn);

      await waitFor(() => {
        // maven-central is online → next state is false
        expect(mockApiHook.setRepositoryOnline).toHaveBeenCalledWith(
          'maven-central',
          'maven2',
          'proxy',
          false,
        );
      });
    });
  });

  describe('row toggle status freshness (NEXUS-55318)', () => {
    const ONLINE_REPO = 'maven-central';
    const TAKE_OFFLINE = /take offline/i;
    const BRING_ONLINE = /bring online/i;

    // The real fetchRepository returns a fresh top-level `online` but NO `status`.
    // The default suite mock returns `status`, which would mask the stale-merge bug,
    // so this block installs a realistic refetch payload.
    const mockRealisticRefetch = (online: boolean) =>
      jest.fn().mockImplementation((name: string) =>
        Promise.resolve({
          name,
          type: 'proxy',
          format: 'maven2',
          url: `http://localhost:8081/repository/${name}/`,
          online,
        })
      );

    const openRowMenuAndConfirm = async (label: RegExp) => {
      fireEvent.click(screen.getByTestId(`repo-action-toggle-online-${ONLINE_REPO}`));
      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });
      await userEvent.click(screen.getByRole('button', { name: label }));
    };

    it('shouldUpdateStatusCellAfterTakeOfflineWithoutReload', async () => {
      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepository: mockRealisticRefetch(false),
      } as any);

      renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
      await waitFor(() => {
        expect(screen.getByText(ONLINE_REPO)).toBeInTheDocument();
      });

      // Baseline: 2 online repos in the fixture.
      expect(screen.getAllByText('Online').length).toBe(2);

      await openRowMenuAndConfirm(TAKE_OFFLINE);

      // After the toggle the just-toggled row must flip to Offline without a reload.
      await waitFor(() => {
        expect(screen.getAllByText('Online').length).toBe(1);
        // npm-proxy was already offline; maven-central now joins it.
        expect(screen.getAllByText('Offline').length).toBe(2);
      });
    });

    it('shouldFlipRowMenuLabelAfterTakeOfflineWithoutReload', async () => {
      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepository: mockRealisticRefetch(false),
      } as any);

      renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
      await waitFor(() => {
        expect(screen.getByTestId(`repo-action-toggle-online-${ONLINE_REPO}`)).toHaveTextContent('Take Offline');
      });

      await openRowMenuAndConfirm(TAKE_OFFLINE);

      await waitFor(() => {
        expect(screen.getByTestId(`repo-action-toggle-online-${ONLINE_REPO}`)).toHaveTextContent('Bring Online');
      });
    });

    it('shouldBringRowBackOnlineWithoutReload', async () => {
      // Fixture starts npm-proxy offline; bring it online and confirm the row updates.
      mockUseRepositoriesApi.mockReturnValue({
        ...mockApiHook,
        fetchRepository: mockRealisticRefetch(true),
      } as any);

      renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
      await waitFor(() => {
        expect(screen.getByTestId('repo-action-toggle-online-npm-proxy')).toHaveTextContent('Bring Online');
      });

      fireEvent.click(screen.getByTestId('repo-action-toggle-online-npm-proxy'));
      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });
      await userEvent.click(screen.getByRole('button', { name: BRING_ONLINE }));

      await waitFor(() => {
        expect(screen.getByTestId('repo-action-toggle-online-npm-proxy')).toHaveTextContent('Take Offline');
        // all 3 repos now online
        expect(screen.getAllByText('Online').length).toBe(3);
      });
    });
  });

  describe('row toggle confirm button color (NEXUS-55035)', () => {
    const findConfirmButton = (label: RegExp) => screen.getByRole('button', { name: label });

    it('shouldRenderTakeOfflineConfirmButtonRed', async () => {
      renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('repo-action-toggle-online-maven-central'));
      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      // Take Offline is disruptive: Radix renders the danger variant with color="red".
      expect(findConfirmButton(/take offline/i)).toHaveAttribute('data-accent-color', 'red');
    });

    it('shouldRenderBringOnlineConfirmButtonGreen', async () => {
      renderWithTheme(<RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
      await waitFor(() => {
        expect(screen.getByTestId('repo-action-toggle-online-npm-proxy')).toHaveTextContent('Bring Online');
      });

      fireEvent.click(screen.getByTestId('repo-action-toggle-online-npm-proxy'));
      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      // Bring Online is positive: Radix renders the success variant with color="green".
      expect(findConfirmButton(/bring online/i)).toHaveAttribute('data-accent-color', 'green');
    });
  });

  describe('Actions column (NEXUS-53948)', () => {
    it('shouldRenderActionsColumnHeaderWithRowActionsLabel', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const actionsHeader = screen.getByRole('columnheader', {
        name: /row actions/i,
      });
      expect(actionsHeader).toBeInTheDocument();
    });

    it('shouldRenderPerRowActionsTriggerForEveryRepository', async () => {
      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      mockRepositories.forEach((repo) => {
        expect(
          screen.getByRole('button', { name: `Actions for ${repo.name}` })
        ).toBeInTheDocument();
      });
    });
  });
});
