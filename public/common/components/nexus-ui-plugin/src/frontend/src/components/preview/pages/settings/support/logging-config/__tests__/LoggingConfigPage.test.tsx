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

import { LoggingConfigPage } from '../LoggingConfigPage';
import * as useLoggingConfigApiModule from '../useLoggingConfigApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook
jest.mock('../useLoggingConfigApi');

// Mock ExtJS
const mockCheckPermission = jest.fn((perm) => true);
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: (perm: string) => mockCheckPermission(perm),
  },
}));

// Mock child components
jest.mock('../LoggersList', () => ({
  LoggersList: function MockLoggersList({ onSelect }: { onSelect: (name: string) => void }) {
    return (
      <div data-testid="loggers-list">
        <button onClick={() => onSelect('org.sonatype')}>Select Logger</button>
      </div>
    );
  },
}));

jest.mock('../LoggerForm', () => ({
  LoggerForm: function MockLoggerForm({
    loggerName,
    isCreate,
    onSave,
    onCancel,
    onDelete,
  }: {
    loggerName?: string | null;
    isCreate?: boolean;
    onSave: () => void;
    onCancel: () => void;
    onDelete?: () => void;
  }) {
    return (
      <div data-testid="logger-form">
        <span>{isCreate ? 'Create Logger' : `Edit ${loggerName}`}</span>
        <button onClick={onSave}>Save</button>
        <button onClick={onCancel}>Cancel</button>
        {onDelete && <button onClick={onDelete}>Delete</button>}
      </div>
    );
  },
}));

const mockedUseLoggingConfigApi = useLoggingConfigApiModule.useLoggingConfigApi as jest.MockedFunction<
  typeof useLoggingConfigApiModule.useLoggingConfigApi
>;

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('LoggingConfigPage', () => {
  const mockResetAllLoggers = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockResetAllLoggers.mockResolvedValue(undefined);
    mockedUseLoggingConfigApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLoggers: jest.fn().mockResolvedValue([]),
      fetchLogger: jest.fn().mockResolvedValue(null),
      updateLogger: jest.fn().mockResolvedValue(undefined),
      resetLogger: jest.fn().mockResolvedValue(undefined),
      resetAllLoggers: mockResetAllLoggers,
    });
  });

  it('renders the loggers list by default', () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('loggers-list')).toBeInTheDocument();
    expect(screen.getByText('Logging')).toBeInTheDocument();
    expect(screen.getByText('Control logging levels')).toBeInTheDocument();
  });

  it('displays page header with actions', () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Logging')).toBeInTheDocument();
    expect(screen.getByText('Create Logger')).toBeInTheDocument();
    expect(screen.getByText('Reset to Default Levels')).toBeInTheDocument();
  });

  it('shows create form when Create Logger button is clicked', async () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create Logger'));

    await waitFor(() => {
      expect(screen.getByTestId('logger-form')).toBeInTheDocument();
    });
  });

  it('navigates to logger detail when a logger is selected', async () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Logger'));

    await waitFor(() => {
      expect(screen.getByTestId('logger-form')).toBeInTheDocument();
      expect(screen.getByText('Edit org.sonatype')).toBeInTheDocument();
    });
  });

  it('returns to list view when cancel is clicked', async () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    // Go to create mode
    fireEvent.click(screen.getByText('Create Logger'));

    await waitFor(() => {
      expect(screen.getByTestId('logger-form')).toBeInTheDocument();
    });

    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));

    await waitFor(() => {
      expect(screen.getByTestId('loggers-list')).toBeInTheDocument();
    });
  });

  it('opens confirmation dialog when Reset to Default Levels is clicked', async () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Reset to Default Levels'));

    await waitFor(() => {
      expect(screen.getByText('Reset All Loggers')).toBeInTheDocument();
      expect(screen.getByText(/Are you sure you want to reset all loggers/)).toBeInTheDocument();
    });
  });

  it('calls resetAllLoggers when confirm button is clicked in dialog', async () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    // Open the dialog
    fireEvent.click(screen.getByText('Reset to Default Levels'));

    await waitFor(() => {
      expect(screen.getByText('Reset All Loggers')).toBeInTheDocument();
    });

    // Click the confirm button in the dialog
    fireEvent.click(screen.getByRole('button', { name: 'Reset All' }));

    await waitFor(() => {
      expect(mockResetAllLoggers).toHaveBeenCalled();
    });
  });

  it('does not reset if user clicks cancel in confirmation dialog', async () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    // Open the dialog
    fireEvent.click(screen.getByText('Reset to Default Levels'));

    await waitFor(() => {
      expect(screen.getByText('Reset All Loggers')).toBeInTheDocument();
    });

    // Click cancel
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    await waitFor(() => {
      expect(screen.queryByText('Reset All Loggers')).not.toBeInTheDocument();
    });
    expect(mockResetAllLoggers).not.toHaveBeenCalled();
  });

  it('hides action buttons when user lacks update permission', () => {
    mockCheckPermission.mockReturnValue(false);

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.queryByText('Create Logger')).not.toBeInTheDocument();
    expect(screen.queryByText('Reset to Default Levels')).not.toBeInTheDocument();
  });

  it('displays error alert when error occurs', () => {
    mockedUseLoggingConfigApi.mockReturnValue({
      loading: false,
      error: 'Failed to reset loggers',
      setError: mockSetError,
      fetchLoggers: jest.fn().mockResolvedValue([]),
      fetchLogger: jest.fn().mockResolvedValue(null),
      updateLogger: jest.fn().mockResolvedValue(undefined),
      resetLogger: jest.fn().mockResolvedValue(undefined),
      resetAllLoggers: mockResetAllLoggers,
    });

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Failed to reset loggers')).toBeInTheDocument();
  });

  it('opens confirmation dialog when individual logger Delete is clicked', async () => {
    mockCheckPermission.mockReturnValue(true);

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    // Select a logger to go to detail view
    fireEvent.click(screen.getByText('Select Logger'));

    await waitFor(() => {
      expect(screen.getByTestId('logger-form')).toBeInTheDocument();
    });

    // Click delete in the form
    const deleteButton = await screen.findByText('Delete');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(screen.getByText('Delete Logger Override')).toBeInTheDocument();
      expect(screen.getByText(/Remove the custom log level for "org.sonatype"/)).toBeInTheDocument();
    });
  });

  it('calls resetLogger when individual delete is confirmed', async () => {
    mockCheckPermission.mockReturnValue(true);

    const mockResetLogger = jest.fn().mockResolvedValue(undefined);
    mockedUseLoggingConfigApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLoggers: jest.fn().mockResolvedValue([]),
      fetchLogger: jest.fn().mockResolvedValue({ name: 'org.sonatype', level: 'DEBUG' }),
      updateLogger: jest.fn().mockResolvedValue(undefined),
      resetLogger: mockResetLogger,
      resetAllLoggers: mockResetAllLoggers,
    });

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Logger'));

    const deleteButton = await screen.findByText('Delete');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(screen.getByText('Delete Logger Override')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /^Delete$/ }));

    await waitFor(() => {
      expect(mockResetLogger).toHaveBeenCalledWith('org.sonatype');
    });
  });
});


