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

import { MetricHealthPage } from '../MetricHealthPage';
import * as useMetricHealthApiModule from '../useMetricHealthApi';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

// Mock the API hook
jest.mock('../useMetricHealthApi');

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

const mockedUseMetricHealthApi = useMetricHealthApiModule.useMetricHealthApi as jest.MockedFunction<
  typeof useMetricHealthApiModule.useMetricHealthApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('MetricHealthPage', () => {
  const mockHealthChecks = [
    { name: 'threadDeadlockHealthCheck', result: { healthy: true, message: 'No deadlocks detected' } },
    { name: 'databaseHealthCheck', result: { healthy: true, message: 'Database is running' } },
    { name: 'memoryHealthCheck', result: { healthy: false, message: 'Memory usage high' } },
  ];

  const mockNodes = [
    { nodeId: 'node-1', hostname: 'nexus-node-1', healthy: true },
    { nodeId: 'node-2', hostname: 'nexus-node-2', healthy: false, message: 'High memory usage' },
  ];

  const mockFetchMetricHealth = jest.fn();
  const mockFetchClusterNodes = jest.fn();
  const mockFetchNodeMetricHealth = jest.fn();
  const mockDownloadMetricHealth = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Mock checkPermission to return true by default
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(true);
    // Mock state().getValue to return false (non-clustered) by default
    jest.spyOn(ExtJS, 'state').mockReturnValue({
      getValue: jest.fn().mockReturnValue(false),
    } as any);

    mockedUseMetricHealthApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchMetricHealth: mockFetchMetricHealth.mockResolvedValue(mockHealthChecks),
      fetchClusterNodes: mockFetchClusterNodes.mockResolvedValue([]),
      fetchNodeMetricHealth: mockFetchNodeMetricHealth.mockResolvedValue(mockHealthChecks),
      downloadMetricHealth: mockDownloadMetricHealth,
    });
  });

  it('renders loading state initially', () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading health checks...')).toBeInTheDocument();
  });

  it('renders the page title', async () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Status')).toBeInTheDocument();
    });

    expect(screen.getByText('View system health checks and diagnostics')).toBeInTheDocument();
  });

  it('displays health checks in single-node mode', async () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Thread Deadlock')).toBeInTheDocument();
    });

    expect(screen.getByText('Database')).toBeInTheDocument();
    // Use getAllByText because Memory appears in both list and detail views
    expect(screen.getAllByText('Memory').length).toBeGreaterThan(0);
  });

  it('selects first unhealthy check by default', async () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use getAllByText because Memory appears in both list and detail views
      expect(screen.getAllByText('Memory').length).toBeGreaterThan(0);
    });

    // The Memory check should be selected (it's unhealthy)
    const memoryButton = screen.getByRole('button', { name: /memory/i });
    expect(memoryButton).toHaveAttribute('aria-selected', 'true');
  });

  it('handles refresh button click', async () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Status')).toBeInTheDocument();
    });

    const refreshButton = screen.getByTestId('refresh-button');
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(mockFetchMetricHealth).toHaveBeenCalledTimes(2);
    });

    expect(mockToastSuccess).toHaveBeenCalledWith('Health checks refreshed');
  });

  it('handles download button click', async () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Status')).toBeInTheDocument();
    });

    const downloadButton = screen.getByTestId('download-button');
    fireEvent.click(downloadButton);

    expect(mockDownloadMetricHealth).toHaveBeenCalledWith(mockHealthChecks, 'metric-health.json');
  });

  it('displays error state', async () => {
    mockedUseMetricHealthApi.mockReturnValue({
      loading: false,
      error: 'Failed to load health checks',
      setError: mockSetError,
      fetchMetricHealth: mockFetchMetricHealth.mockResolvedValue([]),
      fetchClusterNodes: mockFetchClusterNodes.mockResolvedValue([]),
      fetchNodeMetricHealth: mockFetchNodeMetricHealth,
      downloadMetricHealth: mockDownloadMetricHealth,
    });

    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load health checks')).toBeInTheDocument();
    });
  });

  it('shows no permission message when user lacks permission', async () => {
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(false);

    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('You do not have permission to view metric health.')).toBeInTheDocument();
    });
  });

  it('displays help section with documentation link', async () => {
    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About Status')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nxrm3/docs/metrics'
    );
  });

  it('shows empty state when no health checks', async () => {
    mockFetchMetricHealth.mockResolvedValue([]);

    render(<MetricHealthPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('No health checks available')).toBeInTheDocument();
    });
  });

  describe('Clustered Mode', () => {
    beforeEach(() => {
      // Mock state().getValue to return true (clustered mode)
      jest.spyOn(ExtJS, 'state').mockReturnValue({
        getValue: jest.fn().mockReturnValue(true),
      } as any);

      mockedUseMetricHealthApi.mockReturnValue({
        loading: false,
        error: null,
        setError: mockSetError,
        fetchMetricHealth: mockFetchMetricHealth.mockResolvedValue(mockHealthChecks),
        fetchClusterNodes: mockFetchClusterNodes.mockResolvedValue(mockNodes),
        fetchNodeMetricHealth: mockFetchNodeMetricHealth.mockResolvedValue(mockHealthChecks),
        downloadMetricHealth: mockDownloadMetricHealth,
      });
    });

    it('shows node list in clustered mode', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('Select a node to view health checks')).toBeInTheDocument();
      });

      expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      expect(screen.getByText('nexus-node-2')).toBeInTheDocument();
    });

    it('displays node health status indicators', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      // Node 2 should show its message
      expect(screen.getByText('High memory usage')).toBeInTheDocument();
    });

    it('navigates to node health details on click', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      const nodeButton = screen.getByTestId('node-item-node-1');
      fireEvent.click(nodeButton);

      await waitFor(() => {
        expect(mockFetchNodeMetricHealth).toHaveBeenCalledWith('node-1');
      });

      // Should now show health checks for the node
      expect(screen.getByText('Thread Deadlock')).toBeInTheDocument();
    });

    it('shows back button when viewing node details', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      const nodeButton = screen.getByTestId('node-item-node-1');
      fireEvent.click(nodeButton);

      await waitFor(() => {
        expect(screen.getByTestId('back-to-nodes-button')).toBeInTheDocument();
      });
    });

    it('navigates back to node list when clicking back button', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      // Click a node
      const nodeButton = screen.getByTestId('node-item-node-1');
      fireEvent.click(nodeButton);

      await waitFor(() => {
        expect(screen.getByTestId('back-to-nodes-button')).toBeInTheDocument();
      });

      // Click back
      const backButton = screen.getByTestId('back-to-nodes-button');
      fireEvent.click(backButton);

      await waitFor(() => {
        expect(screen.getByText('Select a node to view health checks')).toBeInTheDocument();
      });
    });

    it('refreshes node list when on nodes view', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      const refreshButton = screen.getByTestId('refresh-button');
      fireEvent.click(refreshButton);

      await waitFor(() => {
        expect(mockFetchClusterNodes).toHaveBeenCalledTimes(2);
      });
    });

    it('shows empty state when no cluster nodes', async () => {
      mockFetchClusterNodes.mockResolvedValue([]);

      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('No cluster nodes available')).toBeInTheDocument();
      });
    });

    it('hides download button when viewing nodes list', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      expect(screen.queryByTestId('download-button')).not.toBeInTheDocument();
    });

    it('shows download button when viewing node health details', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('nexus-node-1')).toBeInTheDocument();
      });

      const nodeButton = screen.getByTestId('node-item-node-1');
      fireEvent.click(nodeButton);

      await waitFor(() => {
        expect(screen.getByTestId('download-button')).toBeInTheDocument();
      });
    });
  });

  describe('Form State Attributes', () => {
    it('has data-testid on page container', async () => {
      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('metric-health-page')).toBeInTheDocument();
      });
    });

    it('has data-loading attribute during loading', async () => {
      mockedUseMetricHealthApi.mockReturnValue({
        loading: true,
        error: null,
        setError: mockSetError,
        fetchMetricHealth: mockFetchMetricHealth.mockResolvedValue(mockHealthChecks),
        fetchClusterNodes: mockFetchClusterNodes.mockResolvedValue([]),
        fetchNodeMetricHealth: mockFetchNodeMetricHealth,
        downloadMetricHealth: mockDownloadMetricHealth,
      });

      render(<MetricHealthPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        const page = screen.getByTestId('metric-health-page');
        expect(page).toHaveAttribute('data-loading', 'true');
      });
    });
  });
});
