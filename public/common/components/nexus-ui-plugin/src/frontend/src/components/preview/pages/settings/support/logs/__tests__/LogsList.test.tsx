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
import * as useLogsListModule from '../useLogsList';

// Mock the integration hook
jest.mock('../useLogsList');

// Mock luxon for date formatting
jest.mock('luxon', () => ({
  DateTime: {
    fromMillis: jest.fn().mockReturnValue({
      toLocaleString: jest.fn().mockReturnValue('1/1/2024, 12:00:00 PM'),
    }),
  },
}));

// Mock HumanReadableUtils
jest.mock('../../../../../../../interface/HumanReadableUtils', () => ({
  HumanReadableUtils: {
    bytesToString: jest.fn((bytes) => `${Math.round(bytes / 1024)} KB`),
  },
}));

const mockedUseLogsList = useLogsListModule.useLogsList as jest.MockedFunction<typeof useLogsListModule.useLogsList>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const allLogs = [
  { fileName: 'nexus.log', size: 1048576, lastModified: 1704110400000 },
  { fileName: 'request.log', size: 524288, lastModified: 1704024000000 },
  { fileName: 'audit.log', size: 262144, lastModified: 1703937600000 },
];

function makeHook(overrides: Partial<ReturnType<typeof useLogsListModule.useLogsList>> = {}) {
  return {
    filteredLogs: allLogs,
    filter: '',
    sortField: 'fileName' as const,
    sortDirection: 'asc' as const,
    error: null,
    isLoading: false,
    setFilter: jest.fn(),
    handleSort: jest.fn(),
    ...overrides,
  };
}

describe('LogsList', () => {
  const mockOnSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseLogsList.mockReturnValue(makeHook());
  });

  it('renders loading state initially', () => {
    mockedUseLogsList.mockReturnValue(makeHook({ isLoading: true, filteredLogs: [] }));

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

  it('calls setFilter when filter input changes', async () => {
    const mockSetFilter = jest.fn();
    mockedUseLogsList.mockReturnValue(makeHook({ setFilter: mockSetFilter }));

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    const filterInput = screen.getByPlaceholderText('Filter by file name');
    fireEvent.change(filterInput, { target: { value: 'nexus' } });

    expect(mockSetFilter).toHaveBeenCalledWith('nexus', expect.anything());
  });

  it('shows empty message when no logs match filter', async () => {
    mockedUseLogsList.mockReturnValue(makeHook({ filteredLogs: [], filter: 'nonexistent' }));

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('No log files match your filter')).toBeInTheDocument();
    });
  });

  it('calls handleSort when File Name header is clicked', async () => {
    const mockHandleSort = jest.fn();
    mockedUseLogsList.mockReturnValue(makeHook({ handleSort: mockHandleSort }));

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('File Name'));

    expect(mockHandleSort).toHaveBeenCalledWith('fileName');
  });

  it('calls handleSort when Size header is clicked', async () => {
    const mockHandleSort = jest.fn();
    mockedUseLogsList.mockReturnValue(makeHook({ handleSort: mockHandleSort }));

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('nexus.log')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Size'));

    expect(mockHandleSort).toHaveBeenCalledWith('size');
  });

  it('displays error alert when error is present', async () => {
    mockedUseLogsList.mockReturnValue(makeHook({ error: 'Failed to load logs', filteredLogs: [] }));

    render(<LogsList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load logs')).toBeInTheDocument();
    });
  });

  it('shows empty message when no logs exist', async () => {
    mockedUseLogsList.mockReturnValue(makeHook({ filteredLogs: [] }));

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
