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

import { LogsList } from '../LogsList';
import * as useLogsApiModule from '../useLogsApi';

// Mock the API hook
jest.mock('../useLogsApi');

// Mock luxon for date formatting
jest.mock('luxon', () => ({
  DateTime: {
    fromMillis: jest.fn().mockReturnValue({
      toLocaleString: jest.fn().mockReturnValue('1/1/2024, 12:00:00 PM'),
    }),
  },
}));

// Mock nexus-ui-plugin
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  HumanReadableUtils: {
    bytesToString: jest.fn((bytes) => `${Math.round(bytes / 1024)} KB`),
  },
}));

const mockedUseLogsApi = useLogsApiModule.useLogsApi as jest.MockedFunction<typeof useLogsApiModule.useLogsApi>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockLogs = [
  { fileName: 'nexus.log', size: 1048576, lastModified: 1704110400000 },
  { fileName: 'request.log', size: 524288, lastModified: 1704024000000 },
  { fileName: 'audit.log', size: 262144, lastModified: 1703937600000 },
];

describe('LogsList', () => {
  const mockOnSelect = jest.fn();
  const mockSetError = jest.fn();
  const mockFetchLogs = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchLogs.mockResolvedValue(mockLogs);
    mockedUseLogsApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLogs: mockFetchLogs,
      fetchLogContent: jest.fn(),
      insertMark: jest.fn(),
      getDownloadUrl: jest.fn(),
    });
  });

  it('renders loading state initially', () => {
    mockFetchLogs.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading log files...')).toBeInTheDocument();
  });

  it('renders log files list after loading', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
      expect(screen.getByText('request.log')).toBeInTheDocument();
      expect(screen.getByText('audit.log')).toBeInTheDocument();
    });
  });

  it('displays file size in human readable format', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getAllByText(/KB/).length).toBeGreaterThan(0);
    });
  });

  it('calls onSelect when a log file row is clicked', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('nexus.log'));

    expect(mockOnSelect).toHaveBeenCalledWith('nexus.log');
  });

  it('filters logs by filename', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by file name');
    fireEvent.change(filterInput, { target: { value: 'nexus' } });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
      expect(screen.queryByText('request.log')).not.toBeInTheDocument();
      expect(screen.queryByText('audit.log')).not.toBeInTheDocument();
    });
  });

  it('shows empty message when no logs match filter', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by file name');
    fireEvent.change(filterInput, { target: { value: 'nonexistent' } });

    await waitFor(() => {
      expect(screen.getByText('No log files match your filter')).toBeInTheDocument();
    });
  });

  it('sorts by filename when header is clicked', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    // Click on File Name header to toggle sort
    fireEvent.click(screen.getByText('File Name'));

    // Should now be descending (z-a)
    const rows = screen.getAllByRole('row');
    // First row is header, so data starts at index 1
    expect(rows[1]).toHaveTextContent('request.log');
  });

  it('sorts by size when header is clicked', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    // Click on Size header
    fireEvent.click(screen.getByText('Size'));

    // Size should be sorted ascending (smallest first)
    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('audit.log');
  });

  it('displays error alert when fetch fails', async () => {
    mockedUseLogsApi.mockReturnValue({
      loading: false,
      error: 'Failed to load logs',
      setError: mockSetError,
      fetchLogs: mockFetchLogs.mockRejectedValue(new Error('Failed to load logs')),
      fetchLogContent: jest.fn(),
      insertMark: jest.fn(),
      getDownloadUrl: jest.fn(),
    });

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load logs')).toBeInTheDocument();
    });
  });

  it('shows empty message when no logs exist', async () => {
    mockFetchLogs.mockResolvedValue([]);

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('No log files found')).toBeInTheDocument();
    });
  });

  it('has sortable column headers', async () => {
    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    expect(screen.getByText('File Name')).toBeInTheDocument();
    expect(screen.getByText('Size')).toBeInTheDocument();
    expect(screen.getByText('Last Modified')).toBeInTheDocument();
  });
});


