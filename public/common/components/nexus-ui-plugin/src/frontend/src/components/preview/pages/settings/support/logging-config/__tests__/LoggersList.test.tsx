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
import * as useLoggersListModule from '../useLoggersList';

// Mock the integration hook
jest.mock('../useLoggersList');

const mockedUseLoggersList = useLoggersListModule.useLoggersList as jest.MockedFunction<
  typeof useLoggersListModule.useLoggersList
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

function makeHook(overrides: Partial<ReturnType<typeof useLoggersListModule.useLoggersList>> = {}) {
  return {
    filteredLoggers: mockLoggers,
    loggers: mockLoggers,
    filter: '',
    levelFilter: [] as string[],
    sortField: 'name' as const,
    sortDirection: 'asc' as const,
    error: null,
    isLoading: false,
    filterSections: [
      {
        id: 'level',
        label: 'Log Level',
        type: 'checkbox' as const,
        options: ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'].map((l) => ({
          value: l,
          label: l,
          count: mockLoggers.filter((lg) => lg.level === l).length,
        })),
        value: [] as string[],
        defaultExpanded: true,
      },
    ],
    setFilter: jest.fn(),
    setLevelFilter: jest.fn(),
    handleSort: jest.fn(),
    handleFilterChange: jest.fn(),
    handleClearFilters: jest.fn(),
    ...overrides,
  };
}

describe('LoggersList', () => {
  const mockOnSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseLoggersList.mockReturnValue(makeHook());
  });

  it('renders loading state initially', () => {
    mockedUseLoggersList.mockReturnValue(makeHook({ isLoading: true, filteredLoggers: [], loggers: [] }));

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

  it('calls setFilter when filter input changes', async () => {
    const mockSetFilter = jest.fn();
    mockedUseLoggersList.mockReturnValue(makeHook({ setFilter: mockSetFilter }));

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    const filterInput = screen.getByPlaceholderText('Filter by logger name');
    fireEvent.change(filterInput, { target: { value: 'sonatype' } });

    expect(mockSetFilter).toHaveBeenCalledWith('sonatype', expect.anything());
  });

  it('shows empty message when no loggers match filter', async () => {
    mockedUseLoggersList.mockReturnValue(
      makeHook({ filteredLoggers: [], loggers: mockLoggers, filter: 'nonexistent' })
    );

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('No loggers match your filters')).toBeInTheDocument();
    });
  });

  it('has sortable column headers', async () => {
    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /logger name/i });
    expect(nameHeader).toBeInTheDocument();

    fireEvent.click(nameHeader);

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });
  });

  it('calls handleSort when Logger Level header is clicked', async () => {
    const mockHandleSort = jest.fn();
    mockedUseLoggersList.mockReturnValue(makeHook({ handleSort: mockHandleSort }));

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('ROOT')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Logger Level'));

    expect(mockHandleSort).toHaveBeenCalledWith('level');
  });

  it('displays error alert when fetch fails', async () => {
    mockedUseLoggersList.mockReturnValue(makeHook({ error: 'Failed to load loggers', filteredLoggers: [], loggers: [] }));

    render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load loggers')).toBeInTheDocument();
    });
  });

  it('shows empty message when no loggers exist', async () => {
    mockedUseLoggersList.mockReturnValue(makeHook({ filteredLoggers: [], loggers: [] }));

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

  describe('level filter sidebar', () => {
    it('renders the FilterSidebar with Log Level section', async () => {
      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('ROOT')).toBeInTheDocument();
      });

      expect(screen.getByText('Log Level')).toBeInTheDocument();
    });

    it('shows counter with total loggers', async () => {
      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('loggers-counter')).toHaveTextContent('Showing 3 of 3 loggers');
      });
    });

    it('calls handleFilterChange when a level checkbox is clicked', async () => {
      const mockHandleFilterChange = jest.fn();
      mockedUseLoggersList.mockReturnValue(makeHook({ handleFilterChange: mockHandleFilterChange }));

      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('ROOT')).toBeInTheDocument();
      });

      const debugCheckbox = screen.getByRole('checkbox', { name: /DEBUG/i });
      fireEvent.click(debugCheckbox);

      expect(mockHandleFilterChange).toHaveBeenCalled();
    });

    it('renders filtered loggers when filteredLoggers is a subset', async () => {
      const debugOnly = [{ name: 'debug-logger-1', level: 'DEBUG' as const, override: true }];
      mockedUseLoggersList.mockReturnValue(
        makeHook({
          filteredLoggers: debugOnly,
          loggers: mockLoggers,
          filter: '',
        })
      );

      render(<LoggersList onSelect={mockOnSelect} />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText('debug-logger-1')).toBeInTheDocument();
        expect(screen.queryByText('ROOT')).not.toBeInTheDocument();
      });
    });
  });
});
