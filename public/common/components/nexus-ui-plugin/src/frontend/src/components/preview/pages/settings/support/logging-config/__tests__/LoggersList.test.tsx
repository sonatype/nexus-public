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
import { Theme } from '@radix-ui/themes';

import { LoggersList } from '../LoggersList';
import * as useLoggingConfigApiModule from '../useLoggingConfigApi';

// Mock the API hook
jest.mock('../useLoggingConfigApi');

const mockedUseLoggingConfigApi = useLoggingConfigApiModule.useLoggingConfigApi as jest.MockedFunction<
  typeof useLoggingConfigApiModule.useLoggingConfigApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockLoggers = [
  { name: 'ROOT', level: 'INFO' as const, override: false },
  { name: 'org.sonatype.nexus', level: 'DEBUG' as const, override: true },
  { name: 'org.apache', level: 'WARN' as const, override: true },
];

describe('LoggersList', () => {
  const mockOnSelect = jest.fn();
  const mockSetError = jest.fn();
  const mockFetchLoggers = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchLoggers.mockResolvedValue(mockLoggers);
    mockedUseLoggingConfigApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLoggers: mockFetchLoggers,
      fetchLogger: jest.fn(),
      updateLogger: jest.fn(),
      resetLogger: jest.fn(),
      resetAllLoggers: jest.fn(),
    });
  });

  it('renders loading state initially', () => {
    mockFetchLoggers.mockImplementation(() => new Promise(() => {}));

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading loggers...')).toBeInTheDocument();
  });

  it('renders loggers list after loading', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
      expect(screen.getByText('org.sonatype.nexus')).toBeInTheDocument();
      expect(screen.getByText('org.apache')).toBeInTheDocument();
    });
  });

  it('displays log levels as badges in the table', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    const table = screen.getByRole('table');
    expect(within(table).getByText('INFO')).toBeInTheDocument();
    expect(within(table).getByText('DEBUG')).toBeInTheDocument();
    expect(within(table).getByText('WARN')).toBeInTheDocument();
  });

  it('calls onSelect when a logger row is clicked', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('ROOT'));

    expect(mockOnSelect).toHaveBeenCalledWith('ROOT');
  });

  it('filters loggers by name', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by logger name');
    fireEvent.change(filterInput, { target: { value: 'sonatype' } });

    await waitFor(() => {
      expect(screen.getByText('org.sonatype.nexus')).toBeInTheDocument();
      expect(screen.queryByText('ROOT')).not.toBeInTheDocument();
      expect(screen.queryByText('org.apache')).not.toBeInTheDocument();
    });
  });

  it('shows empty message when no loggers match filter', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    const filterInput = screen.getByPlaceholderText('Filter by logger name');
    fireEvent.change(filterInput, { target: { value: 'nonexistent' } });

    await waitFor(() => {
      expect(screen.getByText('No loggers match your filters')).toBeInTheDocument();
    });
  });

  it('has sortable column headers', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    // Logger Name header should be clickable for sorting
    const nameHeader = screen.getByRole('columnheader', { name: /logger name/i });
    expect(nameHeader).toBeInTheDocument();

    // Clicking the header shouldn't cause an error
    fireEvent.click(nameHeader);

    // Data should still be visible after clicking
    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });
  });

  it('sorts by level when header is clicked', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    // Click on Logger Level header
    fireEvent.click(screen.getByText('Logger Level'));

    // Levels should be sorted alphabetically
    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      expect(rows[1]).toHaveTextContent('DEBUG');
    });
  });

  it('displays error alert when fetch fails', async () => {
    mockedUseLoggingConfigApi.mockReturnValue({
      loading: false,
      error: 'Failed to load loggers',
      setError: mockSetError,
      fetchLoggers: mockFetchLoggers.mockRejectedValue(new Error('Failed to load loggers')),
      fetchLogger: jest.fn(),
      updateLogger: jest.fn(),
      resetLogger: jest.fn(),
      resetAllLoggers: jest.fn(),
    });

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load loggers')).toBeInTheDocument();
    });
  });

  it('shows empty message when no loggers exist', async () => {
    mockFetchLoggers.mockResolvedValue([]);

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('No loggers found')).toBeInTheDocument();
    });
  });

  it('has sortable column headers', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    expect(screen.getByText('Logger Name')).toBeInTheDocument();
    expect(screen.getByText('Logger Level')).toBeInTheDocument();
  });

  describe('level filter (bug 4rq9)', () => {
    const loggersWithMixedLevels = [
      { name: 'root-logger', level: 'INFO' as const, override: false },
      { name: 'debug-logger-1', level: 'DEBUG' as const, override: true },
      { name: 'debug-logger-2', level: 'DEBUG' as const, override: true },
      { name: 'warn-logger', level: 'WARN' as const, override: true },
      { name: 'trace-logger', level: 'TRACE' as const, override: true },
    ];

    beforeEach(() => {
      mockFetchLoggers.mockResolvedValue(loggersWithMixedLevels);
    });

    it('renders the FilterSidebar with Log Level section', async () => {
      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('root-logger')).toBeInTheDocument();
      });

      expect(screen.getByText('Log Level')).toBeInTheDocument();
    });

    it('shows counter with total loggers', async () => {
      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('loggers-counter')).toHaveTextContent('Showing 5 of 5 loggers');
      });
    });

    it('filters loggers by level when checkbox is clicked', async () => {
      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('root-logger')).toBeInTheDocument();
      });

      const debugCheckbox = screen.getByRole('checkbox', { name: /DEBUG/i });
      fireEvent.click(debugCheckbox);

      await waitFor(() => {
        expect(screen.getByText('debug-logger-1')).toBeInTheDocument();
        expect(screen.getByText('debug-logger-2')).toBeInTheDocument();
        expect(screen.queryByText('root-logger')).not.toBeInTheDocument();
        expect(screen.queryByText('warn-logger')).not.toBeInTheDocument();
      });
    });

    it('name filter and level filter work together (AND logic)', async () => {
      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('root-logger')).toBeInTheDocument();
      });

      const debugCheckbox = screen.getByRole('checkbox', { name: /DEBUG/i });
      fireEvent.click(debugCheckbox);

      const filterInput = screen.getByPlaceholderText('Filter by logger name');
      fireEvent.change(filterInput, { target: { value: 'logger-1' } });

      await waitFor(() => {
        expect(screen.getByText('debug-logger-1')).toBeInTheDocument();
        expect(screen.queryByText('debug-logger-2')).not.toBeInTheDocument();
      });
    });
  });
});

