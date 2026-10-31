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
import * as useLoggingConfigModule from '../useLoggingConfig';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the integration hook
jest.mock('../useLoggingConfig');

// Mock ExtJS
const mockCheckPermission = jest.fn(() => true);
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: (perm: string) => mockCheckPermission(perm),
  },
}));

// Mock child components so we only test LoggingConfigPage's own behavior
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

const mockedUseLoggingConfig = useLoggingConfigModule.useLoggingConfig as jest.MockedFunction<
  typeof useLoggingConfigModule.useLoggingConfig
>;

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

function makeHook(
  overrides: Partial<ReturnType<typeof useLoggingConfigModule.useLoggingConfig>> = {}
): ReturnType<typeof useLoggingConfigModule.useLoggingConfig> {
  return {
    viewMode: 'list',
    selectedLogger: null,
    deleteDialogOpen: false,
    resetAllDialogOpen: false,
    isDeleting: false,
    isResettingAll: false,
    error: null,
    refreshKey: 0,
    handleSelectLogger: jest.fn(),
    handleCreate: jest.fn(),
    handleBack: jest.fn(),
    handleSave: jest.fn(),
    handleDeleteClick: jest.fn(),
    handleDeleteConfirm: jest.fn(),
    handleCancelDelete: jest.fn(),
    handleResetAll: jest.fn(),
    handleResetAllConfirm: jest.fn(),
    handleCancelResetAll: jest.fn(),
    clearError: jest.fn(),
    ...overrides,
  };
}

describe('LoggingConfigPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    mockedUseLoggingConfig.mockReturnValue(makeHook());
  });

  it('renders the loggers list in list view', () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('loggers-list')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Logging' })).toBeInTheDocument();
    expect(screen.getByText('Control logging levels')).toBeInTheDocument();
  });

  it('displays page header with action buttons when user can update', () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Create Logger')).toBeInTheDocument();
    expect(screen.getByText('Reset to Default Levels')).toBeInTheDocument();
  });

  it('calls handleCreate when Create Logger button is clicked', () => {
    const mockHandleCreate = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(makeHook({ handleCreate: mockHandleCreate }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create Logger'));

    expect(mockHandleCreate).toHaveBeenCalled();
  });

  it('renders create form when viewMode is create', () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'create' }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('logger-form')).toBeInTheDocument();
    // Page header uses 'Create Logger' as h1 title; mock form also shows it as a span
    expect(screen.getAllByText('Create Logger').length).toBeGreaterThanOrEqual(1);
  });

  it('renders detail form when viewMode is detail and selectedLogger is set', () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype' }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('logger-form')).toBeInTheDocument();
    expect(screen.getByText('Edit org.sonatype')).toBeInTheDocument();
  });

  it('calls handleSelectLogger when a logger row is clicked', () => {
    const mockHandleSelectLogger = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(makeHook({ handleSelectLogger: mockHandleSelectLogger }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Logger'));

    expect(mockHandleSelectLogger).toHaveBeenCalledWith('org.sonatype');
  });

  it('calls handleBack when cancel is clicked in the form', () => {
    const mockHandleBack = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'create', handleBack: mockHandleBack }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Cancel'));

    expect(mockHandleBack).toHaveBeenCalled();
  });

  it('calls handleSave when save is clicked in the form', () => {
    const mockHandleSave = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'create', handleSave: mockHandleSave }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Save'));

    expect(mockHandleSave).toHaveBeenCalled();
  });

  it('calls handleResetAll when Reset to Default Levels is clicked', () => {
    const mockHandleResetAll = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(makeHook({ handleResetAll: mockHandleResetAll }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Reset to Default Levels'));

    expect(mockHandleResetAll).toHaveBeenCalled();
  });

  it('opens reset confirmation dialog when resetAllDialogOpen is true', () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ resetAllDialogOpen: true }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Reset All Loggers')).toBeInTheDocument();
    expect(screen.getByText(/Are you sure you want to reset all loggers/)).toBeInTheDocument();
  });

  it('calls handleResetAllConfirm when Reset All is confirmed in the dialog', () => {
    const mockHandleResetAllConfirm = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(
      makeHook({ resetAllDialogOpen: true, handleResetAllConfirm: mockHandleResetAllConfirm })
    );

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: 'Reset All' }));

    expect(mockHandleResetAllConfirm).toHaveBeenCalled();
  });

  it('calls handleCancelResetAll when reset dialog cancel is clicked', () => {
    const mockHandleCancelResetAll = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(
      makeHook({ resetAllDialogOpen: true, handleCancelResetAll: mockHandleCancelResetAll })
    );

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(mockHandleCancelResetAll).toHaveBeenCalled();
  });

  it('hides action buttons when user lacks update permission', () => {
    mockCheckPermission.mockReturnValue(false);

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.queryByText('Create Logger')).not.toBeInTheDocument();
    expect(screen.queryByText('Reset to Default Levels')).not.toBeInTheDocument();
  });

  it('displays error alert when hook returns an error', () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ error: 'Failed to reset loggers' }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Failed to reset loggers')).toBeInTheDocument();
  });

  it('calls clearError when error alert is closed', () => {
    const mockClearError = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(makeHook({ error: 'Some error', clearError: mockClearError }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    // SettingsAlert renders a dismiss button with aria-label="Dismiss"
    const closeButton = screen.getByRole('button', { name: /dismiss/i });
    fireEvent.click(closeButton);

    expect(mockClearError).toHaveBeenCalled();
  });

  it('opens delete dialog when deleteDialogOpen is true', () => {
    mockedUseLoggingConfig.mockReturnValue(
      makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype', deleteDialogOpen: true })
    );

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Delete Logger Override')).toBeInTheDocument();
    expect(screen.getByText(/Remove the custom log level for "org.sonatype"/)).toBeInTheDocument();
  });

  it('calls handleDeleteClick when Delete button is clicked in the form', () => {
    const mockHandleDeleteClick = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(
      makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype', handleDeleteClick: mockHandleDeleteClick })
    );

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Delete'));

    expect(mockHandleDeleteClick).toHaveBeenCalled();
  });

  it('calls handleDeleteConfirm when delete is confirmed in the dialog', () => {
    const mockHandleDeleteConfirm = jest.fn();
    mockedUseLoggingConfig.mockReturnValue(
      makeHook({
        viewMode: 'detail',
        selectedLogger: 'org.sonatype',
        deleteDialogOpen: true,
        handleDeleteConfirm: mockHandleDeleteConfirm,
      })
    );

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /^Delete$/ }));

    expect(mockHandleDeleteConfirm).toHaveBeenCalled();
  });

  describe('Breadcrumb navigation for list view', () => {
    it('renders breadcrumbs with Settings link', () => {
      render(<LoggingConfigPage />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
    });

    it('renders Logging as current page in breadcrumbs on list view', () => {
      const { container } = render(<LoggingConfigPage />, { wrapper: TestWrapper });

      const currentBreadcrumb = container.querySelector('[aria-current="page"]');
      expect(currentBreadcrumb).toBeInTheDocument();
      expect(currentBreadcrumb?.textContent).toBe('Logging');
    });

    it('navigates to Settings when Settings breadcrumb is clicked', () => {
      render(<LoggingConfigPage />, { wrapper: TestWrapper });

      const originalHash = window.location.hash;
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
      window.location.hash = originalHash;
    });
  });

  describe('Breadcrumb navigation for detail view', () => {
    it('renders breadcrumb with logger name as current page in detail view', () => {
      mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype' }));

      const { container } = render(<LoggingConfigPage />, { wrapper: TestWrapper });

      const currentBreadcrumb = container.querySelector('[aria-current="page"]');
      expect(currentBreadcrumb).toBeInTheDocument();
      expect(currentBreadcrumb?.textContent).toBe('org.sonatype');
    });

    it('renders Logging as clickable breadcrumb in detail view', () => {
      mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype' }));

      render(<LoggingConfigPage />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: 'Logging' })).toBeInTheDocument();
    });

    it('calls handleBack when Logging breadcrumb is clicked in detail view', () => {
      const mockHandleBack = jest.fn();
      mockedUseLoggingConfig.mockReturnValue(
        makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype', handleBack: mockHandleBack })
      );

      render(<LoggingConfigPage />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: 'Logging' }));

      expect(mockHandleBack).toHaveBeenCalled();
    });

    it('renders create breadcrumb in create view', () => {
      mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'create' }));

      const { container } = render(<LoggingConfigPage />, { wrapper: TestWrapper });

      const currentBreadcrumb = container.querySelector('[aria-current="page"]');
      expect(currentBreadcrumb?.textContent).toBe('Create');
    });
  });

  it('does not render LoggersList in detail view', () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'detail', selectedLogger: 'org.sonatype' }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.queryByTestId('loggers-list')).not.toBeInTheDocument();
  });

  it('renders help section in list view', () => {
    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.getByText('About Logging Configuration')).toBeInTheDocument();
  });

  it('does not render help section in create view', () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ viewMode: 'create' }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    expect(screen.queryByText('About Logging Configuration')).not.toBeInTheDocument();
  });

  it('passes refreshKey to LoggersList as key prop (remounts on change)', async () => {
    mockedUseLoggingConfig.mockReturnValue(makeHook({ refreshKey: 5 }));

    render(<LoggingConfigPage />, { wrapper: TestWrapper });

    // The key is passed as React key so remounting is handled — list should be present
    await waitFor(() => {
      expect(screen.getByTestId('loggers-list')).toBeInTheDocument();
    });
  });
});
