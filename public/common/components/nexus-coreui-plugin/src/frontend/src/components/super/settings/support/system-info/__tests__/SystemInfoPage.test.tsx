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

import { SystemInfoPage } from '../SystemInfoPage';
import * as useSystemInfoApiModule from '../useSystemInfoApi';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

// Mock the API hook
jest.mock('../useSystemInfoApi');

// Mock useToast with trackable mock functions
const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();

jest.mock('../../../../../shared', () => ({
  ...jest.requireActual('../../../../../shared'),
  useToast: () => ({
    success: mockToastSuccess,
    error: mockToastError,
    warning: jest.fn(),
    info: jest.fn(),
  }),
}));

const mockedUseSystemInfoApi = useSystemInfoApiModule.useSystemInfoApi as jest.MockedFunction<
  typeof useSystemInfoApiModule.useSystemInfoApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SystemInfoPage', () => {
  const mockSystemInfo = {
    'nexus-status': {
      version: '3.88.0-01',
      edition: 'PRO',
      status: 'Running',
    },
    'nexus-node': {
      nodeId: 'node-1',
      clustered: false,
    },
    'nexus-license': {
      licenseType: 'Professional',
      validTo: '2025-12-31',
    },
    'system-runtime': {
      javaVersion: '17.0.1',
      availableProcessors: 8,
    },
  };

  const mockFetchSystemInfo = jest.fn();
  const mockFetchSystemInfoHA = jest.fn();
  const mockFetchActiveNodes = jest.fn();
  const mockDownloadSystemInfo = jest.fn();
  const mockCopyToClipboard = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Spy on checkPermission and mock to return true by default
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(true);
    
    mockedUseSystemInfoApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchSystemInfo: mockFetchSystemInfo.mockResolvedValue(mockSystemInfo),
      fetchSystemInfoHA: mockFetchSystemInfoHA.mockResolvedValue({}),
      fetchActiveNodes: mockFetchActiveNodes.mockResolvedValue([]),
      downloadSystemInfo: mockDownloadSystemInfo,
      copyToClipboard: mockCopyToClipboard.mockResolvedValue(true),
    });
  });

  it('renders loading state initially', () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading system information...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('System Information')).toBeInTheDocument();
    });

    expect(screen.getByText('View detailed system and server information')).toBeInTheDocument();
  });

  it('displays system info sections', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Nexus Status')).toBeInTheDocument();
    });

    expect(screen.getByText('Nexus Node')).toBeInTheDocument();
    expect(screen.getByText('Nexus License')).toBeInTheDocument();
    expect(screen.getByText('System Runtime')).toBeInTheDocument();
  });

  it('expands first 3 sections by default', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Nexus Status')).toBeInTheDocument();
    });

    // First 3 sections should show their content
    expect(screen.getByText('version')).toBeInTheDocument();
    expect(screen.getByText('3.88.0-01')).toBeInTheDocument();
  });

  it('handles refresh button click', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('System Information')).toBeInTheDocument();
    });

    const refreshButton = screen.getByRole('button', { name: /refresh/i });
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(mockFetchSystemInfo).toHaveBeenCalledTimes(2);
    });
  });

  it('handles download button click', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('System Information')).toBeInTheDocument();
    });

    const downloadButton = screen.getByRole('button', { name: /download/i });
    fireEvent.click(downloadButton);

    expect(mockDownloadSystemInfo).toHaveBeenCalledWith(mockSystemInfo, 'system-information.json');
  });

  it('handles copy button click', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('System Information')).toBeInTheDocument();
    });

    const copyButton = screen.getByRole('button', { name: /copy/i });
    fireEvent.click(copyButton);

    await waitFor(() => {
      expect(mockCopyToClipboard).toHaveBeenCalledWith(mockSystemInfo);
    });
  });

  it('shows success message after copy', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('System Information')).toBeInTheDocument();
    });

    const copyButton = screen.getByRole('button', { name: /copy/i });
    fireEvent.click(copyButton);

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith('Copied to clipboard');
    });
  });

  it('shows error message when copy fails', async () => {
    mockCopyToClipboard.mockResolvedValue(false);

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('System Information')).toBeInTheDocument();
    });

    const copyButton = screen.getByRole('button', { name: /copy/i });
    fireEvent.click(copyButton);

    await waitFor(() => {
      expect(mockSetError).toHaveBeenCalledWith('Failed to copy to clipboard');
    });
  });

  it('displays error state', async () => {
    mockedUseSystemInfoApi.mockReturnValue({
      loading: false,
      error: 'Failed to load system information',
      setError: mockSetError,
      fetchSystemInfo: mockFetchSystemInfo.mockResolvedValue({}),
      fetchSystemInfoHA: mockFetchSystemInfoHA,
      fetchActiveNodes: mockFetchActiveNodes.mockResolvedValue([]),
      downloadSystemInfo: mockDownloadSystemInfo,
      copyToClipboard: mockCopyToClipboard,
    });

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load system information')).toBeInTheDocument();
    });
  });

  it('shows no permission message when user lacks permission', async () => {
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(false);

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('You do not have permission to view system information.')).toBeInTheDocument();
    });
  });

  it('displays help section with documentation link', async () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About System Information')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'https://help.sonatype.com/en/system-information.html'
    );
  });

  describe('HA mode', () => {
    const mockHANodes = [
      { nodeId: 'node-1', friendlyName: 'Node 1', local: true },
      { nodeId: 'node-2', friendlyName: 'Node 2', local: false },
    ];

    const mockHASystemInfo = {
      'node-1': mockSystemInfo,
      'node-2': {
        'nexus-status': {
          version: '3.88.0-01',
          edition: 'PRO',
          status: 'Running',
        },
      },
    };

    beforeEach(() => {
      // Override the entire mock to enable HA mode (multiple nodes)
      mockedUseSystemInfoApi.mockReturnValue({
        loading: false,
        error: null,
        setError: mockSetError,
        fetchSystemInfo: mockFetchSystemInfo.mockResolvedValue(mockSystemInfo),
        fetchSystemInfoHA: mockFetchSystemInfoHA.mockResolvedValue(mockHASystemInfo),
        fetchActiveNodes: mockFetchActiveNodes.mockResolvedValue(mockHANodes),
        downloadSystemInfo: mockDownloadSystemInfo,
        copyToClipboard: mockCopyToClipboard.mockResolvedValue(true),
      });
    });

    it('shows node selector in HA mode', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Select Node')).toBeInTheDocument();
      });

      expect(screen.getByText('Node 1')).toBeInTheDocument();
      expect(screen.getByText('Node 2')).toBeInTheDocument();
    });

    it('selects local node by default', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Select Node')).toBeInTheDocument();
      });

      const node1Button = screen.getByRole('button', { name: /node 1/i });
      expect(node1Button).toHaveAttribute('aria-pressed', 'true');
    });

    it('switches nodes when clicked', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Select Node')).toBeInTheDocument();
      });

      const node2Button = screen.getByRole('button', { name: /node 2/i });
      fireEvent.click(node2Button);

      expect(node2Button).toHaveAttribute('aria-pressed', 'true');
    });
  });

  describe('Navigation features', () => {
    it('renders navigation bar with Jump to Section dropdown', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Jump to:')).toBeInTheDocument();
      });

      expect(screen.getByTestId('system-info-jump-to')).toBeInTheDocument();
    });

    it('renders Expand All button', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('system-info-expand-all')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /expand all/i })).toBeInTheDocument();
    });

    it('renders Collapse All button', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('system-info-collapse-all')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /collapse all/i })).toBeInTheDocument();
    });

    it('expands all sections when Expand All is clicked', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Nexus Status')).toBeInTheDocument();
      });

      // Click Expand All
      const expandAllButton = screen.getByTestId('system-info-expand-all');
      fireEvent.click(expandAllButton);

      // All sections should be expanded - check for content from System Runtime (4th section)
      await waitFor(() => {
        expect(screen.getByText('javaVersion')).toBeInTheDocument();
      });
    });

    it('collapses all sections when Collapse All is clicked', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Nexus Status')).toBeInTheDocument();
      });

      // First 3 sections are expanded by default, verify content is visible
      expect(screen.getByText('version')).toBeInTheDocument();

      // Click Collapse All
      const collapseAllButton = screen.getByTestId('system-info-collapse-all');
      fireEvent.click(collapseAllButton);

      // Content should no longer be visible (sections collapsed)
      await waitFor(() => {
        expect(screen.queryByText('version')).not.toBeInTheDocument();
      });
    });

    it('toggles individual section when header is clicked', async () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Nexus Status')).toBeInTheDocument();
      });

      // First section is expanded, verify content
      expect(screen.getByText('version')).toBeInTheDocument();

      // Click the section header to collapse
      const sectionHeader = screen.getByText('Nexus Status').closest('[role="button"]');
      if (sectionHeader) {
        fireEvent.click(sectionHeader);
      }

      // Content should be hidden
      await waitFor(() => {
        expect(screen.queryByText('version')).not.toBeInTheDocument();
      });

      // Click again to expand
      if (sectionHeader) {
        fireEvent.click(sectionHeader);
      }

      // Content should be visible again
      await waitFor(() => {
        expect(screen.getByText('version')).toBeInTheDocument();
      });
    });
  });
});


