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
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { RepositoryList } from '../RepositoryList';
import { RepositoryStatusBadge } from '../RepositoryStatusBadge';
import { HealthCheckCell } from '../../../../shared/security/HealthCheckCell';
import { FirewallCell as IqPolicyViolationsCell } from '../../../../shared/security/FirewallCell';
import * as useRepositoryListModule from '../useRepositoryList';

/**
 * Wrapper component that provides Radix UI Theme context.
 */
function ThemeWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

/**
 * Custom render function that wraps components in Theme.
 */
function renderWithTheme(ui: React.ReactElement) {
  return render(ui, { wrapper: ThemeWrapper });
}
import {
  mockRepositories,
  mockHealthCheckData,
  mockFirewallStatusData,
  mockFirewallStatusDataWithMessages,
  healthCheckDataToMap,
  firewallStatusDataToMap,
} from '../mockData';
import { RepositoryListState, SortDirection } from '../repository-list.types';

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

// Mock the useRepositoryList hook
jest.mock('../useRepositoryList', () => ({
  ...jest.requireActual('../useRepositoryList'),
  useRepositoryList: jest.fn(),
  isIqServerEnabled: jest.fn().mockReturnValue(true),
  canUpdateHealthCheck: jest.fn().mockReturnValue(true),
  canReadFirewallStatus: jest.fn().mockReturnValue(true),
}));

// Mock ExtJS
const mockToastSuccess = jest.fn();
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    showSuccessMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue({ enabled: true }),
    }),
    checkPermission: jest.fn().mockReturnValue(true),
  },
  APIConstants: {
    EXT: {
      HEALTH_CHECK: { ACTION: 'healthcheck_Status', METHODS: { READ: 'read', UPDATE: 'update' } },
      FIREWALL_REPOSITORY_STATUS: { ACTION: 'firewall_RepositoryStatus', METHODS: { READ: 'read' } },
    },
    REST: { INTERNAL: { REPOSITORIES_DETAILS: '/service/rest/internal/ui/repositories' } },
  },
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForError: jest.fn(),
    extractResult: jest.fn(),
  },
}));

// Mock useToast
jest.mock('../../../../shared', () => ({
  ...jest.requireActual('../../../../shared'),
  useToast: () => ({
    success: mockToastSuccess,
    error: jest.fn(),
    info: jest.fn(),
    warning: jest.fn(),
  }),
}));

/**
 * Create a mock state for useRepositoryList.
 */
function createMockState(overrides: Partial<RepositoryListState> = {}): RepositoryListState {
  return {
    repositories: mockRepositories,
    filteredRepositories: mockRepositories,
    filterText: '',
    sort: { field: 'name', direction: 'asc' },
    loading: false,
    error: undefined,
    healthCheck: healthCheckDataToMap(mockHealthCheckData),
    firewallStatus: firewallStatusDataToMap(mockFirewallStatusData),
    healthCheckError: undefined,
    firewallStatusError: undefined,
    ...overrides,
  };
}

/**
 * Create a mock return value for useRepositoryList.
 */
function createMockHookReturn(stateOverrides: Partial<RepositoryListState> = {}) {
  const mockSetFilter = jest.fn();
  const mockClearFilter = jest.fn();
  const mockToggleSort = jest.fn();
  const mockGetSortDirection = jest.fn((field: string): SortDirection => {
    if (field === 'name') return 'asc';
    return null;
  });
  const mockRefresh = jest.fn();
  const mockEnableHealthCheck = jest.fn();

  return {
    state: createMockState(stateOverrides),
    setFilter: mockSetFilter,
    clearFilter: mockClearFilter,
    toggleSort: mockToggleSort,
    getSortDirection: mockGetSortDirection,
    refresh: mockRefresh,
    enableHealthCheck: mockEnableHealthCheck,
    showHealthCheckColumn: true,
    showIqPolicyViolationsColumn: true,
  };
}

describe('RepositoryList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(
      createMockHookReturn()
    );
  });

  describe('Rendering', () => {
    it('renders the page title and description', () => {
      renderWithTheme(<RepositoryList />);

      expect(screen.getByRole('heading', { name: /browse/i })).toBeInTheDocument();
      expect(screen.getByText(/browse assets and components/i)).toBeInTheDocument();
    });

    it('renders the filter input', () => {
      renderWithTheme(<RepositoryList />);

      expect(screen.getByPlaceholderText(/filter by name/i)).toBeInTheDocument();
    });

    it('renders all column headers', () => {
      renderWithTheme(<RepositoryList />);

      expect(screen.getByText('Name')).toBeInTheDocument();
      expect(screen.getByText('Type')).toBeInTheDocument();
      expect(screen.getByText('Format')).toBeInTheDocument();
      expect(screen.getByText('Status')).toBeInTheDocument();
      expect(screen.getByText('URL')).toBeInTheDocument();
      expect(screen.getByText('Health Check')).toBeInTheDocument();
      expect(screen.getByText('Firewall Report')).toBeInTheDocument();
    });

    it('renders all repository rows', () => {
      renderWithTheme(<RepositoryList />);

      mockRepositories.forEach((repo) => {
        expect(screen.getByText(repo.name)).toBeInTheDocument();
      });
    });

    it('renders copy URL buttons for each row', () => {
      renderWithTheme(<RepositoryList />);

      const copyButtons = screen.getAllByRole('button', { name: /copy url to clipboard/i });
      expect(copyButtons).toHaveLength(mockRepositories.length);
    });
  });

  describe('Loading State', () => {
    it('renders loading spinner when loading', () => {
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(
        createMockHookReturn({ loading: true, filteredRepositories: [] })
      );

      renderWithTheme(<RepositoryList />);

      expect(screen.getByText(/loading repositories/i)).toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('renders error message when there is an error', () => {
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(
        createMockHookReturn({ error: 'Failed to load repositories' })
      );

      renderWithTheme(<RepositoryList />);

      expect(screen.getByText(/failed to load repositories/i)).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('renders empty message when no repositories', () => {
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(
        createMockHookReturn({ repositories: [], filteredRepositories: [] })
      );

      renderWithTheme(<RepositoryList />);

      expect(screen.getByText(/there are no repositories available/i)).toBeInTheDocument();
    });
  });

  describe('Filtering', () => {
    it('calls setFilter when typing in filter input', async () => {
      const mockReturn = createMockHookReturn();
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(mockReturn);

      renderWithTheme(<RepositoryList />);

      const filterInput = screen.getByPlaceholderText(/filter by name/i);
      await userEvent.type(filterInput, 'maven');

      expect(mockReturn.setFilter).toHaveBeenCalled();
    });

    it('calls clearFilter when clear button is clicked', async () => {
      const mockReturn = createMockHookReturn({ filterText: 'maven' });
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(mockReturn);

      renderWithTheme(<RepositoryList />);

      const clearButton = screen.getByRole('button', { name: /clear filter/i });
      await userEvent.click(clearButton);

      expect(mockReturn.clearFilter).toHaveBeenCalled();
    });

    it('calls clearFilter when Escape is pressed in filter input', async () => {
      const mockReturn = createMockHookReturn({ filterText: 'maven' });
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(mockReturn);

      renderWithTheme(<RepositoryList />);

      const filterInput = screen.getByPlaceholderText(/filter by name/i);
      fireEvent.keyDown(filterInput, { key: 'Escape' });

      expect(mockReturn.clearFilter).toHaveBeenCalled();
    });
  });

  describe('Sorting', () => {
    it('calls toggleSort when clicking on Name header', async () => {
      const mockReturn = createMockHookReturn();
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(mockReturn);

      renderWithTheme(<RepositoryList />);

      const nameHeader = screen.getByRole('button', { name: /name ascending/i });
      await userEvent.click(nameHeader);

      expect(mockReturn.toggleSort).toHaveBeenCalledWith('name');
    });

    it('calls toggleSort when clicking on Type header', async () => {
      const mockReturn = createMockHookReturn();
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(mockReturn);

      renderWithTheme(<RepositoryList />);

      const typeHeader = screen.getByRole('button', { name: /type unsorted/i });
      await userEvent.click(typeHeader);

      expect(mockReturn.toggleSort).toHaveBeenCalledWith('type');
    });

    it('calls toggleSort when pressing Enter on sortable header', async () => {
      const mockReturn = createMockHookReturn();
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue(mockReturn);

      renderWithTheme(<RepositoryList />);

      const formatHeader = screen.getByRole('button', { name: /format unsorted/i });
      formatHeader.focus();
      fireEvent.keyDown(formatHeader, { key: 'Enter' });

      expect(mockReturn.toggleSort).toHaveBeenCalledWith('format');
    });
  });

  describe('Row Selection', () => {
    it('calls onSelect when clicking a row', async () => {
      const onSelect = jest.fn();
      renderWithTheme(<RepositoryList onSelect={onSelect} />);

      const row = screen.getByRole('button', { name: /view maven-central/i });
      await userEvent.click(row);

      expect(onSelect).toHaveBeenCalledWith('maven-central');
    });

    it('calls onSelect when pressing Enter on a row', async () => {
      const onSelect = jest.fn();
      renderWithTheme(<RepositoryList onSelect={onSelect} />);

      const row = screen.getByRole('button', { name: /view maven-central/i });
      row.focus();
      fireEvent.keyDown(row, { key: 'Enter' });

      expect(onSelect).toHaveBeenCalledWith('maven-central');
    });
  });

  describe('Copy URL', () => {
    it('copies URL to clipboard and shows success toast', async () => {
      const mockClipboard = { writeText: jest.fn().mockResolvedValue(undefined) };
      Object.assign(navigator, { clipboard: mockClipboard });

      renderWithTheme(<RepositoryList />);

      const copyButtons = screen.getAllByRole('button', { name: /copy url to clipboard/i });
      await userEvent.click(copyButtons[0]);

      expect(mockClipboard.writeText).toHaveBeenCalledWith(mockRepositories[0].url);
      expect(mockToastSuccess).toHaveBeenCalledWith('URL Copied to Clipboard');
    });

    it('calls custom onCopyUrl when provided', async () => {
      const onCopyUrl = jest.fn((e: React.MouseEvent) => e.stopPropagation());
      renderWithTheme(<RepositoryList onCopyUrl={onCopyUrl} />);

      const copyButtons = screen.getAllByRole('button', { name: /copy url to clipboard/i });
      await userEvent.click(copyButtons[0]);

      expect(onCopyUrl).toHaveBeenCalled();
    });
  });

  describe('IQ Server Columns', () => {
    it('hides health check column when showHealthCheckColumn is false', () => {
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue({
        ...createMockHookReturn(),
        showHealthCheckColumn: false,
      });

      renderWithTheme(<RepositoryList />);

      expect(screen.queryByText('Health Check')).not.toBeInTheDocument();
    });

    it('hides firewall report column when showIqPolicyViolationsColumn is false', () => {
      (useRepositoryListModule.useRepositoryList as jest.Mock).mockReturnValue({
        ...createMockHookReturn(),
        showIqPolicyViolationsColumn: false,
      });

      renderWithTheme(<RepositoryList />);

      expect(screen.queryByText('Firewall Report')).not.toBeInTheDocument();
    });
  });
});

describe('RepositoryStatusBadge', () => {
  it('renders Online status with green indicator', () => {
    renderWithTheme(
      <RepositoryStatusBadge
        status={{ repositoryName: 'test', online: true }}
      />
    );

    expect(screen.getByText('Online')).toBeInTheDocument();
  });

  it('renders Offline status with red indicator', () => {
    renderWithTheme(
      <RepositoryStatusBadge
        status={{ repositoryName: 'test', online: false }}
      />
    );

    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    renderWithTheme(
      <RepositoryStatusBadge
        status={{ repositoryName: 'test', online: true, description: 'Ready to Connect' }}
      />
    );

    expect(screen.getByText(/ready to connect/i)).toBeInTheDocument();
  });

  it('renders reason when provided', () => {
    renderWithTheme(
      <RepositoryStatusBadge
        status={{ repositoryName: 'test', online: false, reason: 'Connection timeout' }}
      />
    );

    expect(screen.getByText(/connection timeout/i)).toBeInTheDocument();
  });
});

describe('HealthCheckCell', () => {
  const proxyRepo = { name: 'test', type: 'proxy', format: 'maven2' };
  const hostedRepo = { name: 'test', type: 'hosted', format: 'maven2' };

  it('renders Analyze button when health check is not enabled', () => {
    renderWithTheme(
      <HealthCheckCell
        repository={proxyRepo}
        healthStatus={{ enabled: false, analyzing: false }}
        onAnalyze={jest.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /analyze/i })).toBeInTheDocument();
  });

  it('renders analyzing state with spinner', () => {
    renderWithTheme(
      <HealthCheckCell
        repository={proxyRepo}
        healthStatus={{ enabled: false, analyzing: true }}
      />
    );

    expect(screen.getByText(/analyzing/i)).toBeInTheDocument();
  });

  it('renders security and license badges when enabled with data', () => {
    renderWithTheme(
      <HealthCheckCell
        repository={proxyRepo}
        healthStatus={{
          enabled: true,
          securityIssueCount: 2,
          licenseIssueCount: 0,
        }}
      />
    );

    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('renders Not supported for non-proxy repositories', () => {
    renderWithTheme(
      <HealthCheckCell repository={hostedRepo} />
    );

    expect(screen.getByText('Not supported')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /analyze/i })).not.toBeInTheDocument();
  });

  it('renders N/A when no onAnalyze and no health status for proxy', () => {
    renderWithTheme(
      <HealthCheckCell repository={proxyRepo} />
    );

    expect(screen.getByText('N/A')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /analyze/i })).not.toBeInTheDocument();
  });

  it('calls onAnalyze when Analyze button is clicked', async () => {
    const onAnalyze = jest.fn();
    renderWithTheme(
      <HealthCheckCell
        repository={proxyRepo}
        healthStatus={{ enabled: false, analyzing: false }}
        onAnalyze={onAnalyze}
      />
    );

    const analyzeButton = screen.getByRole('button', { name: /analyze/i });
    await userEvent.click(analyzeButton);

    expect(onAnalyze).toHaveBeenCalledWith('test');
  });
});

describe('IqPolicyViolationsCell', () => {
  const testRepo = { name: 'test', type: 'proxy', format: 'maven2' };

  it('renders severity badges when data is available', () => {
    renderWithTheme(
      <IqPolicyViolationsCell
        repository={testRepo}
        firewallStatus={mockFirewallStatusData[0]}
      />
    );

    expect(screen.getByText('Quarantine')).toBeInTheDocument();
  });

  it('renders empty state icon when no firewall status data', () => {
    renderWithTheme(
      <IqPolicyViolationsCell repository={testRepo} />
    );

    const svgs = document.querySelectorAll('.firewall-cell svg');
    expect(svgs.length).toBeGreaterThan(0);
  });

  it('renders error state when firewallStatus has errorMessage', () => {
    renderWithTheme(
      <IqPolicyViolationsCell
        repository={testRepo}
        firewallStatus={mockFirewallStatusDataWithMessages[1]}
      />
    );

    expect(screen.getByText('Unavailable')).toBeInTheDocument();
  });

  it('renders badges from IQ Server when message is present but no error', () => {
    renderWithTheme(
      <IqPolicyViolationsCell
        repository={testRepo}
        firewallStatus={mockFirewallStatusDataWithMessages[0]}
      />
    );

    const cell = document.querySelector('.firewall-cell');
    expect(cell).toBeInTheDocument();
  });

  it('renders error text for IQ Server error message', () => {
    renderWithTheme(
      <IqPolicyViolationsCell
        repository={testRepo}
        firewallStatus={mockFirewallStatusDataWithMessages[1]}
      />
    );

    expect(screen.getByText('Unavailable')).toBeInTheDocument();
  });

  it('renders protected state when no issues', () => {
    renderWithTheme(
      <IqPolicyViolationsCell
        repository={testRepo}
        firewallStatus={{
          ...mockFirewallStatusData[0],
          criticalComponentCount: 0,
          severeComponentCount: 0,
          moderateComponentCount: 0,
          quarantinedComponentCount: 0,
        }}
      />
    );

    expect(screen.getByText('Quarantine')).toBeInTheDocument();
  });
});

