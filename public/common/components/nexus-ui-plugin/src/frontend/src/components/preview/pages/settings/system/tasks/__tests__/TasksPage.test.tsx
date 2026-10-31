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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { TasksPage } from '../TasksPage';
import { useTasksApi } from '../useTasksApi';

// Mock the API hook
jest.mock('../useTasksApi');
const mockUseTasksApi = useTasksApi as jest.MockedFunction<typeof useTasksApi>;

// Mock the internal API module used by tasksFormMachine so it does not fire real
// network requests for /tasks/templates (see taskApiMock for details). (NEXUS-52612)
jest.mock('../../../../../../../interface/api', () =>
  require('./taskApiMock').createTaskApiMock()
);

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

// Get reference to the mocked ExtJS for permission control in tests
import { ExtJS } from '../../../../../../../interface/ExtJS';
const mockCheckPermission = ExtJS.checkPermission as jest.MockedFunction<typeof ExtJS.checkPermission>;

// Mock TaskDetail and TaskForm to avoid their deep shared/form dependency cascades.
// The stub surfaces the live status it receives (data-testid="detail-live-status")
// and exposes onRun/onStop so polling-orchestration tests can drive Run/Stop without
// the real component's form machinery.
jest.mock('../TaskDetail', () => ({
  TaskDetail: ({ task, liveTask, onBack, onRun, onStop }: any) => {
    const React = require('react');
    const [activeTab, setActiveTab] = React.useState('details');
    if (!task) return React.createElement('div', { 'data-testid': 'task-detail-loading' }, 'Loading...');
    return React.createElement('div', { 'data-testid': 'task-detail' },
      React.createElement('h1', null, task.name),
      React.createElement('span', { 'data-testid': 'detail-seed-status' }, task.status),
      React.createElement('span', { 'data-testid': 'detail-live-status' }, liveTask ? liveTask.status : ''),
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
      React.createElement('button', { 'data-testid': 'mock-run', onClick: onRun }, 'Run'),
      React.createElement('button', { 'data-testid': 'mock-stop', onClick: onStop }, 'Stop'),
      React.createElement('button', { onClick: onBack }, 'Back')
    );
  },
}));

jest.mock('../TaskForm', () => ({
  TaskForm: ({ onSave, onCancel }: any) => {
    const React = require('react');
    return React.createElement('div', { 'data-testid': 'task-form' },
      React.createElement('input', { placeholder: 'Filter task types...' }),
      React.createElement('button', { onClick: onCancel }, 'Cancel'),
      React.createElement('button', { onClick: () => onSave({}) }, 'Save')
    );
  },
}));

// Mock shared/form to avoid SCSS loading
jest.mock('../../../../../shared/form', () => ({
  SettingsButton: ({ children, testId, disabled, type, onClick, ...rest }: { children: React.ReactNode; testId?: string; disabled?: boolean; type?: string; onClick?: () => void; [key: string]: any }) => (
    <button data-testid={testId} disabled={disabled} type={(type as any) || 'button'} onClick={onClick} {...rest}>
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
  PageHeader: jest.fn(({ title, breadcrumbs, actions }: { title: string; breadcrumbs?: Array<{ label: string; onClick?: () => void }>; actions?: React.ReactNode }) => {
    const React = require('react');
    return React.createElement('div', null,
      breadcrumbs?.map((b: { label: string; onClick?: () => void }, i: number) =>
        b.onClick
          ? React.createElement('button', { key: i, onClick: b.onClick }, b.label)
          : React.createElement('span', { key: i, 'aria-current': 'page' }, b.label)
      ),
      React.createElement('h1', null, title),
      actions
    );
  }),
  LoadingState: jest.fn(({ message }: { message?: string }) => require('react').createElement('div', null, message || 'Loading...')),
  EntityTable: jest.fn(function EntityTable({ data, columns, getRowKey, onRowClick, emptyState, loading, loadingMessage }: any) {
    const React = require('react');
    if (loading) return React.createElement('div', null, loadingMessage || 'Loading...');
    if (!data || data.length === 0) return emptyState || null;
    return React.createElement('div', { 'data-testid': 'entity-table' },
      data.map((item: any) => React.createElement('div', {
          key: getRowKey(item),
          onClick: () => { if (onRowClick) onRowClick(item); },
          style: { cursor: 'pointer' }
        },
          columns.map((col: any) => React.createElement('span', { key: col.id },
              typeof col.accessor === 'function' ? col.accessor(item) : String((item)[col.accessor] || '')
            ))
        ))
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

    it('should show skeleton placeholders while fetching tasks', () => {
      // Use a never-resolving promise to keep loading state
      mockUseTasksApi.mockReturnValue({
        ...defaultApiMock,
        loading: true,
        fetchTasks: jest.fn(() => new Promise(() => {})),
      });
      const { container } = renderWithProviders(<TasksPage />);

      expect(screen.queryByText('Loading tasks...')).not.toBeInTheDocument();
      expect(container.querySelectorAll('[data-testid="tasks-list-skeleton-row"]').length).toBeGreaterThanOrEqual(1);
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

    it('renders the page-level alert region as a polite live region', () => {
      renderWithProviders(<TasksPage />);

      const liveRegion = screen.getByTestId('tasks-page-alerts');
      expect(liveRegion.getAttribute('role')).toBe('status');
      expect(liveRegion.getAttribute('aria-live')).toBe('polite');
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

    it('should show task type selector as flat table in create form', async () => {
      renderWithProviders(<TasksPage />);

      const createButton = await screen.findByRole('button', { name: /create task/i });
      fireEvent.click(createButton);

      // Wait for form to render with task type filter
      await waitFor(() => {
        expect(screen.getByPlaceholderText('Filter task types...')).toBeInTheDocument();
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

  describe('breadcrumb navigation', () => {
    it('renders breadcrumbs for list view', async () => {
      renderWithProviders(<TasksPage />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
      });

      // Breadcrumbs: Settings (clickable) > Tasks (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(document.querySelector('[aria-current="page"]')?.textContent).toBe('Tasks');
    });

    it('clicking Settings breadcrumb navigates to settings page', async () => {
      renderWithProviders(<TasksPage />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
      });

      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders breadcrumbs for create view', async () => {
      renderWithProviders(<TasksPage />);

      const createButton = await screen.findByRole('button', { name: /create task/i });
      fireEvent.click(createButton);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /create task/i })).toBeInTheDocument();
      });

      // Breadcrumbs: Settings (clickable) > Tasks (clickable) > Create (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Tasks' })).toBeInTheDocument();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('navigates back to list when Tasks breadcrumb is clicked in create view', async () => {
      renderWithProviders(<TasksPage />);

      const createButton = await screen.findByRole('button', { name: /create task/i });
      fireEvent.click(createButton);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /create task/i })).toBeInTheDocument();
      });

      screen.getByRole('button', { name: 'Tasks' }).click();

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
      });
    });

    it('renders breadcrumbs for detail view with task name', async () => {
      renderWithProviders(<TasksPage />);

      await selectTaskRow('Cleanup Task');

      await waitFor(() => {
        const headings = screen.getAllByRole('heading', { name: /cleanup task/i });
        expect(headings.length).toBeGreaterThanOrEqual(1);
      });

      // Breadcrumbs: Settings (clickable) > Tasks (clickable) > {taskName} (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Tasks' })).toBeInTheDocument();
      expect(document.querySelector('[aria-current="page"]')?.textContent).toBe('Cleanup Task');
    });

    it('navigates back to list when Tasks breadcrumb is clicked in detail view', async () => {
      renderWithProviders(<TasksPage />);

      await selectTaskRow('Cleanup Task');

      await waitFor(() => {
        const headings = screen.getAllByRole('heading', { name: /cleanup task/i });
        expect(headings.length).toBeGreaterThanOrEqual(1);
      });

      screen.getByRole('button', { name: 'Tasks' }).click();

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Tasks' })).toBeInTheDocument();
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

  describe('analytics ids', () => {
    it('Create Task button carries data-analytics-id="nxrm-task-create"', async () => {
      mockCheckPermission.mockReturnValue(true);
      renderWithProviders(<TasksPage />);
      const createBtn = await screen.findByRole('button', {name: /create task/i});
      expect(createBtn).toHaveAttribute('data-analytics-id', 'nxrm-task-create');
    });
  });

  // NEXUS-53525: live status polling on the detail route. These tests exercise the
  // orchestration (fetch cadence, visibility gating, route-change/unmount cleanup,
  // Run/Stop reconciliation, 404 degradation). The badge/Stop rendering and dirty-
  // form protection are covered by TaskDetail.test.tsx.
  describe('detail status polling', () => {
    const runningTask = { ...mockTasks[0], status: 'RUNNING', stoppable: true, runnable: false };
    const waitingTask = { ...mockTasks[0], status: 'WAITING', stoppable: false, runnable: true };

    // Drain several microtask rounds so the chained mount fetches
    // (fetchTaskTypes → fetchTask → setState) all settle inside act().
    const flush = () => act(async () => {
      for (let i = 0; i < 5; i += 1) {
        // eslint-disable-next-line no-await-in-loop
        await Promise.resolve();
      }
    });
    // Advance fake timers across a tick, flushing the microtasks on either side so
    // the in-flight guard from the previous poll clears before the next one fires.
    const advance = async (ms: number) => {
      await act(async () => {
        await Promise.resolve();
        jest.advanceTimersByTime(ms);
        await Promise.resolve();
      });
    };
    const setVisibility = (state: 'visible' | 'hidden') => {
      Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => state });
      document.dispatchEvent(new Event('visibilitychange'));
    };

    beforeEach(() => {
      jest.useFakeTimers();
      Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => 'visible' });
      // Start directly on the detail route so we don't depend on row-click navigation
      // (which itself uses async waitFor) under fake timers.
      window.location.hash = '#preview/admin/system/tasks/task-1';
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('polls task status on an interval while the task is running', async () => {
      const fetchTask = jest.fn().mockResolvedValue(runningTask);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();
      const afterMount = fetchTask.mock.calls.length;

      await advance(5000);
      expect(fetchTask.mock.calls.length).toBeGreaterThan(afterMount);
      const afterFirstTick = fetchTask.mock.calls.length;

      await advance(5000);
      expect(fetchTask.mock.calls.length).toBeGreaterThan(afterFirstTick);
    });

    it('propagates the polled live status down to TaskDetail', async () => {
      const fetchTask = jest.fn().mockResolvedValue(runningTask);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();
      await advance(5000);

      expect(screen.getByTestId('detail-live-status')).toHaveTextContent('RUNNING');
    });

    it('stops polling once the task reaches a terminal status', async () => {
      const fetchTask = jest.fn()
        .mockResolvedValueOnce(runningTask) // initial load → RUNNING enables polling
        .mockResolvedValue({ ...mockTasks[0], status: 'OK', lastRunResult: 'OK [2s]' });
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();
      await advance(5000);
      const settled = fetchTask.mock.calls.length;

      await advance(5000 * 4);
      expect(fetchTask.mock.calls.length).toBe(settled); // no further polls after terminal
    });

    it('pauses polling while the tab is hidden and resumes with one catch-up poll', async () => {
      const fetchTask = jest.fn().mockResolvedValue(runningTask);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();

      setVisibility('hidden');
      const beforeHidden = fetchTask.mock.calls.length;
      await advance(5000 * 3);
      expect(fetchTask.mock.calls.length).toBe(beforeHidden); // no polls while hidden

      await act(async () => { setVisibility('visible'); await Promise.resolve(); });
      expect(fetchTask.mock.calls.length).toBe(beforeHidden + 1); // single catch-up, not a burst
    });

    it('stops polling when navigating back to the list', async () => {
      const fetchTask = jest.fn().mockResolvedValue(runningTask);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();
      await advance(5000);

      act(() => {
        window.location.hash = '#preview/admin/system/tasks';
        window.dispatchEvent(new Event('hashchange'));
      });
      const afterNav = fetchTask.mock.calls.length;

      await advance(5000 * 4);
      expect(fetchTask.mock.calls.length).toBe(afterNav); // route change tore down the interval
    });

    it('stops polling after unmount', async () => {
      const fetchTask = jest.fn().mockResolvedValue(runningTask);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      const { unmount } = renderWithProviders(<TasksPage />);
      await flush();
      await advance(5000);

      unmount();
      const afterUnmount = fetchTask.mock.calls.length;
      await advance(5000 * 4);
      expect(fetchTask.mock.calls.length).toBe(afterUnmount);
    });

    it('issues exactly one immediate poll after Run and keeps polling through WAITING', async () => {
      const fetchTask = jest.fn().mockResolvedValue(waitingTask); // server lags at WAITING
      const runTask = jest.fn().mockResolvedValue(undefined);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask, runTask });

      renderWithProviders(<TasksPage />);
      await flush();
      // Idle WAITING task is not polled until the user acts.
      const beforeRun = fetchTask.mock.calls.length;

      await act(async () => {
        fireEvent.click(screen.getByTestId('mock-run'));
        await Promise.resolve();
      });

      expect(runTask).toHaveBeenCalledTimes(1);
      // Exactly one immediate poll (no duplicate one-shot + poll flicker).
      expect(fetchTask.mock.calls.length).toBe(beforeRun + 1);

      // The post-action window keeps polling even though the badge still reads WAITING.
      const afterRun = fetchTask.mock.calls.length;
      await advance(5000);
      expect(fetchTask.mock.calls.length).toBeGreaterThan(afterRun);
    });

    it('renders the running seed immediately on initial load and enables polling (refresh while running)', async () => {
      const fetchTask = jest.fn().mockResolvedValue(runningTask);
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();

      // The seed loaded by the detail mount already reflects RUNNING — no poll
      // needed for the badge to be correct after a refresh.
      expect(screen.getByTestId('detail-seed-status')).toHaveTextContent('RUNNING');
      const afterLoad = fetchTask.mock.calls.length;

      // ...and because the seed is active, polling is enabled to keep it fresh.
      await advance(5000);
      expect(fetchTask.mock.calls.length).toBeGreaterThan(afterLoad);
    });

    it('degrades to a task-not-found state instead of looping on 404s', async () => {
      const fetchTask = jest.fn()
        .mockResolvedValueOnce(runningTask) // load succeeds, enables polling
        .mockResolvedValue(null);           // subsequent polls 404 (task deleted)
      mockUseTasksApi.mockReturnValue({ ...defaultApiMock, fetchTask });

      renderWithProviders(<TasksPage />);
      await flush();
      await advance(5000);

      expect(await screen.findByText(/no longer exists/i)).toBeInTheDocument();
      const afterNotFound = fetchTask.mock.calls.length;
      await advance(5000 * 4);
      expect(fetchTask.mock.calls.length).toBe(afterNotFound); // no endless 404 loop
    });
  });
});
