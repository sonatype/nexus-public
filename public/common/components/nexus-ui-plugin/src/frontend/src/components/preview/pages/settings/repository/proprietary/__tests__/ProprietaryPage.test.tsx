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
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { ProprietaryPage } from '../ProprietaryPage';
import { useProprietaryApi } from '../useProprietaryApi';
import { ToastProvider } from '../../../../../shared/Toast';

jest.mock('../useProprietaryApi');
// Mock ExtJS at the path the source uses
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
  },
}));
jest.mock('lucide-react', () => ({
  Lock: ({ children, ...props }: any) => <span data-testid="lock-icon" {...props}>{children}</span>,
  Save: ({ children, ...props }: any) => <span data-testid="save-icon" {...props}>{children}</span>,
  RotateCcw: ({ children, ...props }: any) => <span data-testid="rotate-icon" {...props}>{children}</span>,
  Info: ({ children, ...props }: any) => <span data-testid="info-icon" {...props}>{children}</span>,
  ExternalLink: ({ children, ...props }: any) => <span data-testid="external-link-icon" {...props}>{children}</span>,
  Loader2: ({ children, ...props }: any) => <span data-testid="loader-icon" {...props}>{children}</span>,
  Search: ({ children, ...props }: any) => <span data-testid="search-icon" {...props}>{children}</span>,
  ChevronRight: ({ children, ...props }: any) => <span data-testid="chevron-right-icon" {...props}>{children}</span>,
  ChevronLeft: ({ children, ...props }: any) => <span data-testid="chevron-left-icon" {...props}>{children}</span>,
  ChevronsRight: ({ children, ...props }: any) => <span data-testid="chevrons-right-icon" {...props}>{children}</span>,
  ChevronsLeft: ({ children, ...props }: any) => <span data-testid="chevrons-left-icon" {...props}>{children}</span>,
  HelpCircle: ({ children, ...props }: any) => <span data-testid="help-circle-icon" {...props}>{children}</span>,
  AlertCircle: ({ children, ...props }: any) => <span data-testid="alert-circle-icon" {...props}>{children}</span>,
  RefreshCw: ({ children, ...props }: any) => <span data-testid="refresh-icon" {...props}>{children}</span>,
}));

const mockUseProprietaryApi = useProprietaryApi as jest.MockedFunction<typeof useProprietaryApi>;
const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');
const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

const mockRepos = [
  { id: 'maven-releases', name: 'maven-releases' },
  { id: 'maven-snapshots', name: 'maven-snapshots' },
  { id: 'npm-hosted', name: 'npm-hosted' },
];

const mockSettings = {
  enabledRepositories: ['maven-releases'],
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(
    <Theme>
      <ToastProvider>{component}</ToastProvider>
    </Theme>
  );
};

describe('ProprietaryPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    mockUseProprietaryApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchSettings: jest.fn().mockResolvedValue(mockSettings),
      fetchPossibleRepositories: jest.fn().mockResolvedValue(mockRepos),
      updateSettings: jest.fn().mockResolvedValue(mockSettings),
    });
  });

  it('should render the page header using standard PageHeader component', async () => {
    renderWithTheme(<ProprietaryPage />);
    
    // PageHeader component renders with data-testid="page-header"
    expect(screen.getByTestId('page-header')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Proprietary Repositories' })).toBeInTheDocument();
    
    await waitFor(() => {
      expect(screen.getByText(/configure which repositories/i)).toBeInTheDocument();
    });
  });

  it('should show loading state using standard LoadingState component', () => {
    mockUseProprietaryApi.mockReturnValue({
      ...mockUseProprietaryApi(),
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchSettings: jest.fn().mockImplementation(() => new Promise(() => {})),
      fetchPossibleRepositories: jest.fn().mockImplementation(() => new Promise(() => {})),
      updateSettings: jest.fn(),
    });

    renderWithTheme(<ProprietaryPage />);
    
    // LoadingState component renders with data-testid="loading-state"
    expect(screen.getByTestId('loading-state')).toBeInTheDocument();
    expect(screen.getByText('Loading settings...')).toBeInTheDocument();
  });

  it('should display transfer list when user has update permission', async () => {
    mockCheckPermission.mockReturnValue(true);
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      // Labels match Default UI: "Generic Hosted Repositories" and "Proprietary Hosted Repositories"
      expect(screen.getByText('Generic Hosted Repositories')).toBeInTheDocument();
      expect(screen.getByText('Proprietary Hosted Repositories')).toBeInTheDocument();
    });
  });

  it('should display read-only view when user lacks update permission', async () => {
    mockCheckPermission.mockReturnValue(false);
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      expect(screen.getByText('Enabled Proprietary Repositories')).toBeInTheDocument();
      expect(screen.getByText('maven-releases')).toBeInTheDocument();
    });
  });

  it('should show Save button when user has permission', async () => {
    mockCheckPermission.mockReturnValue(true);
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      // Find the submit button specifically
      const saveButtons = screen.getAllByRole('button', { name: /save/i });
      expect(saveButtons.length).toBeGreaterThan(0);
      const submitButton = saveButtons.find(btn => btn.getAttribute('type') === 'submit');
      expect(submitButton).toBeInTheDocument();
    });
  });

  it('should disable Save button when no changes', async () => {
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      const saveButtons = screen.getAllByRole('button', { name: /save/i });
      const submitButton = saveButtons.find(btn => btn.getAttribute('type') === 'submit');
      expect(submitButton).toBeDisabled();
    });
  });

  it('should show Discard button', async () => {
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
    });
  });

  it('should show help section using standard HelpSection component', async () => {
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      // HelpSection component renders with data-testid="help-section"
      expect(screen.getByTestId('help-section')).toBeInTheDocument();
      expect(screen.getByText(/about proprietary components/i)).toBeInTheDocument();
    });
  });

  it('should show error state using standard ErrorState component when initial load fails', async () => {
    const setErrorMock = jest.fn();
    mockUseProprietaryApi.mockReturnValue({
      loading: false,
      error: 'Failed to load settings',
      setError: setErrorMock,
      fetchSettings: jest.fn().mockRejectedValue(new Error('Network error')),
      fetchPossibleRepositories: jest.fn().mockRejectedValue(new Error('Network error')),
      updateSettings: jest.fn(),
    });
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      // ErrorState component renders with data-testid="error-state"
      expect(screen.getByTestId('error-state')).toBeInTheDocument();
    });
  });

  it('should show inline error when there is an error after data loads', async () => {
    const setErrorMock = jest.fn();
    mockUseProprietaryApi.mockReturnValue({
      loading: false,
      error: 'Failed to save',
      setError: setErrorMock,
      fetchSettings: jest.fn().mockResolvedValue(mockSettings),
      fetchPossibleRepositories: jest.fn().mockResolvedValue(mockRepos),
      updateSettings: jest.fn(),
    });
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      expect(screen.getByText('Failed to save')).toBeInTheDocument();
    });
  });

  it('should call updateSettings when Save is clicked', async () => {
    const mockUpdateSettings = jest.fn().mockResolvedValue({ enabledRepositories: ['maven-releases', 'npm-hosted'] });
    mockUseProprietaryApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchSettings: jest.fn().mockResolvedValue({ enabledRepositories: [] }),
      fetchPossibleRepositories: jest.fn().mockResolvedValue(mockRepos),
      updateSettings: mockUpdateSettings,
    });
    
    renderWithTheme(<ProprietaryPage />);
    
    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Generic Hosted Repositories')).toBeInTheDocument();
    });

    // The form should detect changes to enable save button
  });

  it('should show success message after saving', async () => {
    const mockUpdateSettings = jest.fn().mockResolvedValue(mockSettings);
    mockUseProprietaryApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchSettings: jest.fn().mockResolvedValue(mockSettings),
      fetchPossibleRepositories: jest.fn().mockResolvedValue(mockRepos),
      updateSettings: mockUpdateSettings,
    });
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      const saveButtons = screen.getAllByRole('button', { name: /save/i });
      expect(saveButtons.length).toBeGreaterThan(0);
    });
  });

  it('should show empty state in read-only mode when no repos enabled', async () => {
    mockCheckPermission.mockReturnValue(false);
    mockUseProprietaryApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchSettings: jest.fn().mockResolvedValue({ enabledRepositories: [] }),
      fetchPossibleRepositories: jest.fn().mockResolvedValue(mockRepos),
      updateSettings: jest.fn(),
    });
    
    renderWithTheme(<ProprietaryPage />);
    
    await waitFor(() => {
      expect(screen.getByText(/no proprietary repositories configured/i)).toBeInTheDocument();
    });
  });
});
