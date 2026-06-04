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
import { render, screen, waitFor, within, fireEvent, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoriesList } from '../RepositoriesList';
import { useRepositoriesApi } from '../useRepositoriesApi';
import { ToastProvider } from '../../../../../shared';

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

// Mock ExtJS - include state for isIqServerEnabled (Firewall column visibility)
// The source imports from relative path, so mock needs to target that path
// ExtJS has both default and named export
jest.mock('../../../../../../../interface/ExtJS', () => {
  const mockExtJS = {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockImplementation((key: string) => {
        if (key === 'clm') return { enabled: true };
        return undefined;
      }),
    }),
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
  fetchHealthCheckStatus: jest.fn().mockResolvedValue({}),
  enableHealthCheck: jest.fn().mockResolvedValue(undefined),
  deleteRepository: jest.fn().mockResolvedValue(undefined),
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
      const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockImplementation((key: string) => {
          if (key === 'clm') return { enabled: false };
          return undefined;
        }),
      });

      renderWithTheme(
        <RepositoriesList onSelect={mockOnSelect} onCreate={mockOnCreate} />
      );

      await waitFor(() => {
        expect(screen.getByText('maven-central')).toBeInTheDocument();
      });

      const firewallHeader = screen.queryByRole('columnheader', { name: /firewall report/i });
      expect(firewallHeader).not.toBeInTheDocument();
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

      // Type the entity name to enable the delete button
      const confirmInput = screen.getByPlaceholderText(/type "maven-central" to confirm/i);
      await userEvent.type(confirmInput, 'maven-central');

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

      // Type the entity name to enable the delete button
      const confirmInput = screen.getByPlaceholderText(/type "maven-central" to confirm/i);
      await userEvent.type(confirmInput, 'maven-central');

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
});
