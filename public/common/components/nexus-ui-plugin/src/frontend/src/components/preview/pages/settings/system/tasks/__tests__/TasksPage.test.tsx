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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { TasksPage } from '../TasksPage';
import { useTasksApi } from '../useTasksApi';

// Mock the API hook
jest.mock('../useTasksApi');
const mockUseTasksApi = useTasksApi as jest.MockedFunction<typeof useTasksApi>;

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

// Get reference to the mocked ExtJS for permission control in tests
import { ExtJS } from '../../../../../../../interface/ExtJS';
const mockCheckPermission = ExtJS.checkPermission as jest.MockedFunction<typeof ExtJS.checkPermission>;

// Mock TaskDetail and TaskForm to avoid their deep shared/form dependency cascades
jest.mock('../TaskDetail', () => ({
  TaskDetail: ({ task, onBack }: any) => {
    const React = require('react');
    const [activeTab, setActiveTab] = React.useState('details');
    if (!task) return React.createElement('div', { 'data-testid': 'task-detail-loading' }, 'Loading...');
    return React.createElement('div', { 'data-testid': 'task-detail' },
      React.createElement('h1', null, task.name),
      React.createElement('div', { role: 'tablist' },
        React.createElement('button', {
          role: 'tab',
          'data-state': activeTab === 'details' ? 'active' : 'inactive',
          onClick: () => setActiveTab('details')
        }, 'Details'),
        React.createElement('button', {
          role: 'tab',
          'data-state': activeTab === 'history' ? 'active' : 'inactive',
          onClick: () => setActiveTab('history')
        }, 'History')
      ),
      React.createElement('button', { onClick: onBack }, 'Back')
    );
  },
}));

jest.mock('../TaskForm', () => ({
  TaskForm: ({ onSave, onCancel }: any) => {
    const React = require('react');
    return React.createElement('div', { 'data-testid': 'task-form' },
      React.createElement('input', { placeholder: 'Search task category...' }),
      React.createElement('button', { onClick: onCancel }, 'Cancel'),
      React.createElement('button', { onClick: () => onSave({}) }, 'Save')
    );
  },
}));

// Mock shared/form to avoid SCSS loading
jest.mock('../../../../../shared/form', () => ({
  SettingsButton: ({ children, testId, disabled, type, onClick }: { children: React.ReactNode; testId?: string; disabled?: boolean; type?: string; onClick?: () => void }) => (
    <button data-testid={testId} disabled={disabled} type={(type as any) || 'button'} onClick={onClick}>
      {children}
    </button>
  ),
  SettingsAlert: ({ children }: { children: React.ReactNode }) => (
    <div className="settings-alert">{children}</div>
  ),
}));

// Mock shared utilities to avoid ToastProvider requirement
jest.mock('../../../../../shared', () => ({
  useToast: () => ({
    success: jest.fn(),
    error: jest.fn(),
    info: jest.fn(),
    warning: jest.fn(),
  }),
  PageHeader: jest.fn(({ title }: { title: string }) => require('react').createElement('div', null, require('react').createElement('h1', null, title))),
  LoadingState: jest.fn(({ message }: { message?: string }) => require('react').createElement('div', null, message || 'Loading...')),
  EntityTable: jest.fn(function EntityTable({ data, columns, getRowKey, onRowClick, emptyState, loading, loadingMessage }: any) {
    const React = require('react');
    if (loading) return React.createElement('div', null, loadingMessage || 'Loading...');
    if (!data || data.length === 0) return emptyState || null;
    return React.createElement('div', { 'data-testid': 'entity-table' },
      data.map(function(item: any) {
        return React.createElement('div', {
          key: getRowKey(item),
          onClick: function() { if (onRowClick) onRowClick(item); },
          style: { cursor: 'pointer' }
        },
          columns.map(function(col: any) {
            return React.createElement('span', { key: col.id },
              typeof col.accessor === 'function' ? col.accessor(item) : String((item)[col.accessor] || '')
            );
          })
        );
      })
    );
  }),
  FilterSidebar: jest.fn(function FilterSidebar({ children }: any) {
    return require('react').createElement('div', { 'data-testid': 'filter-sidebar' }, children);
  }),
  EmptyState: jest.fn(function EmptyState({ title, description }: any) {
    const React = require('react');
    return React.createElement('div', { 'data-testid': 'empty-state' },
      React.createElement('span', null, title),
      description && React.createElement('span', null, description)
    );
  }),
  HelpSection: jest.fn(function HelpSection({ children }: any) {
    return require('react').createElement('div', { 'data-testid': 'help-section' }, children);
  }),
  ConfirmDialog: jest.fn(function ConfirmDialog({ open, title, children, onConfirm, onCancel }: any) {
    const React = require('react');
    if (!open) return null;
    return React.createElement('div', { role: 'dialog' },
      React.createElement('span', null, title),
      children,
      React.createElement('button', { onClick: onConfirm }, 'Confirm'),
      React.createElement('button', { onClick: onCancel }, 'Cancel')
    );
  }),
}));

// Wrapper component
const renderWithProviders = (component: React.ReactElement) => {
  return render(
    <Theme>
      {component}
    </Theme>
  );
};

const selectTaskRow = async (taskName: string) => {
  const taskText = await screen.findByText(taskName);
  fireEvent.click(taskText);
  await waitFor(() => {
    const headings = screen.getAllByRole('heading', { name: new RegExp(taskName, 'i') });
    expect(headings.length).toBeGreaterThanOrEqual(1);
  });
};

const getUser = () => {
  return typeof (userEvent as any).setup === 'function' ? (userEvent as any).setup() : userEvent;
};

describe('TasksPage', () => {
  const mockTasks = [
    {
      id: 'task-1',
      enabled: true,
      name: 'Cleanup Task',
      typeId: 'repository.cleanup',
      typeName: 'Cleanup repositories',
      status: 'WAITING',
      statusDescription: 'Waiting',
      nextRun: new Date('2026-01-22T10:00:00Z'),
      lastRun: new Date('2026-01-21T10:00:00Z'),
      lastRunResult: 'Ok [1m30s]',
      runnable: true,
      stoppable: false,
      alertEmail: 'admin@example.com',
      notificationCondition: 'FAILURE',
      properties: { repositoryName: 'maven-central' },
      schedule: 'daily',
      startDate: new Date('2026-01-21T10:00:00Z'),
      recurringDays: [],
      cronExpression: '',
      timeZoneOffset: '+00:00',
    },
    {
      id: 'task-2',
      enabled: false,
      name: 'Backup Task',
      typeId: 'db.backup',
      typeName: 'Database backup',
      status: 'DISABLED',
      statusDescription: 'Disabled',
      nextRun: null,
      lastRun: null,
      lastRunResult: '',
      runnable: false,
      stoppable: false,
      alertEmail: '',
      notificationCondition: 'FAILURE',
      properties: {},
      schedule: 'manual',
      startDate: null,
      recurringDays: [],
      cronExpression: '',
      timeZoneOffset: '+00:00',
    },
  ];

  const mockTaskTypes = [
    {
      id: 'repository.cleanup',
      name: 'Cleanup repositories',
      exposed: true,
      formFields: [],
    },
    {
      id: 'db.backup',
      name: 'Database backup',
      exposed: true,
      formFields: [],
    },
  ];

  const defaultApiMock = {
    tasks: mockTasks,
    taskTypes: mockTaskTypes,
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchTasks: jest.fn().mockResolvedValue(mockTasks),
    fetchTaskTypes: jest.fn().mockResolvedValue(mockTaskTypes),
    fetchTask: jest.fn().mockResolvedValue(mockTasks[0]),
    createTask: jest.fn().mockResolvedValue({ id: 'new-task' }),
    updateTask: jest.fn().mockResolvedValue(mockTasks[0]),
    deleteTask: jest.fn().mockResolvedValue(undefined),
    runTask: jest.fn().mockResolvedValue(undefined),
    stopTask: jest.fn().mockResolvedValue(undefined),
    clearError: jest.fn(),
    refresh: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset URL hash so each test starts in list view (stale hash from prior test causes wrong initial viewMode)
    window.location.hash = '';
    // Reset permission mock to allow all actions
    mockCheckPermission.mockReturnValue(true);
    mockUseTasksApi.mockReturnValue(defaultApiMock);
  });

  describe('list view', () => {
    it('should render page title', async () => {
      renderWithProviders(<TasksPage />);

      // Wait for tasks to load
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
      });
    });

    it('should fetch tasks on mount', async () => {
      renderWithProviders(<TasksPage />);

      // Wait for async operations
      await waitFor(() => {
        expect(defaultApiMock.fetchTasks).toHaveBeenCalled();
      });
      expect(defaultApiMock.fetchTaskTypes).toHaveBeenCalled();
    });

    it('should display task list', async () => {
      renderWithProviders(<TasksPage />);

      // Wait for tasks to load and display
      expect(await screen.findByText('Cleanup Task')).toBeInTheDocument();
      expect(await screen.findByText('Backup Task')).toBeInTheDocument();
    });

    it('should show create button when user has CREATE permission', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderWithProviders(<TasksPage />);

      // Wait for page to render, then check for button
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /create task/i })).toBeInTheDocument();
      });
    });

    it('should hide create button when user lacks CREATE permission', async () => {
      mockCheckPermission.mockImplementation((perm: string) => 
        !perm.includes('create')
      );
      renderWithProviders(<TasksPage />);

      // Wait for page to render
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
      });

      // Create button should not be present
      expect(screen.queryByRole('button', { name: /create task/i })).not.toBeInTheDocument();
    });

    it('should show loading state while fetching tasks', () => {
      // Use a never-resolving promise to keep loading state
      mockUseTasksApi.mockReturnValue({
        ...defaultApiMock,
        loading: true,
        fetchTasks: jest.fn(() => new Promise(() => {})),
      });
      renderWithProviders(<TasksPage />);

      expect(screen.getByText('Loading tasks...')).toBeInTheDocument();
    });

    it('should show error message when API fails', async () => {
      mockUseTasksApi.mockReturnValue({ 
        ...defaultApiMock, 
        error: 'Failed to load tasks' 
      });
      renderWithProviders(<TasksPage />);

      await waitFor(() => {
        expect(screen.getByText('Failed to load tasks')).toBeInTheDocument();
      });
    });

    it('should show empty state when no tasks exist', async () => {
      mockUseTasksApi.mockReturnValue({
        ...defaultApiMock,
        fetchTasks: jest.fn().mockResolvedValue([]),
      });
      renderWithProviders(<TasksPage />);

      await waitFor(() => {
        expect(screen.getByText('No Tasks')).toBeInTheDocument();
      });
    });

    it('should filter tasks by search query', async () => {
      renderWithProviders(<TasksPage />);

      const searchInput = await screen.findByPlaceholderText(/Search tasks/i);
      fireEvent.change(searchInput, { target: { value: 'Cleanup' } });

      await waitFor(() => {
        expect(screen.getByText('Cleanup Task')).toBeInTheDocument();
        expect(screen.queryByText('Backup Task')).not.toBeInTheDocument();
      });
    });
  });

  describe('create mode', () => {
    it('should navigate to create form when create button is clicked', async () => {
      renderWithProviders(<TasksPage />);

      // Wait for page to load, then find and click create button
      const createButton = await screen.findByRole('button', { name: /create task/i });
      fireEvent.click(createButton);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /create task/i })).toBeInTheDocument();
      });
    });

    it('should show task type selector as card grid in create form', async () => {
      renderWithProviders(<TasksPage />);

      const createButton = await screen.findByRole('button', { name: /create task/i });
      fireEvent.click(createButton);

      // Wait for form to render with task category selector card grid
      await waitFor(() => {
        expect(screen.getByPlaceholderText('Search task category...')).toBeInTheDocument();
      });
    });

    it('should return to list when cancel button is clicked', async () => {
      renderWithProviders(<TasksPage />);

      // Navigate to create form
      const createButton = await screen.findByRole('button', { name: /create task/i });
      fireEvent.click(createButton);

      // Wait for create view to fully render before navigating back
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /create task/i })).toBeInTheDocument();
      });

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      fireEvent.click(cancelButton);

      // Ensure hashchange is processed (jsdom may not dispatch it reliably for sequential changes)
      window.dispatchEvent(new Event('hashchange'));

      // Should return to list view
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
      });
    });

    // Note: Form submission with createTask is covered by TaskForm.test.tsx
    // which tests form validation and submission in isolation.
  });

  describe('detail view', () => {
    it('should show task details when task is selected', async () => {
      renderWithProviders(<TasksPage />);

      await selectTaskRow('Cleanup Task');

      await waitFor(() => {
        const headings = screen.getAllByRole('heading', { name: /cleanup task/i });
        expect(headings.length).toBeGreaterThanOrEqual(1);
      });
    });

    it('should show task execution history', async () => {
      renderWithProviders(<TasksPage />);

      await selectTaskRow('Cleanup Task');

      const historyTab = await screen.findByRole('tab', { name: /history/i });
      const user = getUser();
      await user.click(historyTab);

      await waitFor(() => {
        expect(historyTab).toHaveAttribute('data-state', 'active');
      });
    });
  });

  // Note: Action integration tests (runTask, deleteTask) are covered by TaskDetail.test.tsx
  // which test these components in isolation.
  // The TasksPage is primarily responsible for:
  // 1. Layout and navigation between views
  // 2. Permission-based rendering
  // 3. Data fetching coordination
  // These aspects are tested in the describe blocks above.

});
