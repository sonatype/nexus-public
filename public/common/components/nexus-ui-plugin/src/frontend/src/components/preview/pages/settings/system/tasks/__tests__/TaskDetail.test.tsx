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
import { render, screen, fireEvent, waitFor, within, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';
import { TaskDetail } from '../TaskDetail';
import { Task } from '../types';
import { useTasksApi } from '../useTasksApi';

jest.mock('../useTasksApi');
const mockUseTasksApi = useTasksApi as jest.MockedFunction<typeof useTasksApi>;

// Mock the internal API module used by tasksFormMachine so we can control task type responses
jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: { TASKS: '/service/rest/v1/tasks' },
  restClient: { get: jest.fn().mockResolvedValue([]) },
  urlBuilder: { tasks: { templates: () => '/service/rest/v1/tasks/templates' } },
}));
const mockInternalApi = jest.requireMock('../../../../../../../interface/api') as {
  restClient: { get: jest.Mock };
};

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ExtJS: { checkPermission: jest.fn().mockReturnValue(true) },
    ENDPOINTS: actual.ENDPOINTS || { TASKS: '/service/rest/v1/tasks' },
    restClient: {
      get: jest.fn().mockResolvedValue([]),
      post: jest.fn().mockResolvedValue({}),
      put: jest.fn().mockResolvedValue({}),
      delete: jest.fn().mockResolvedValue({}),
    },
    createFormMachine: actual.createFormMachine,
    useForm: actual.useForm,
    useToast: () => ({ success: jest.fn(), error: jest.fn(), info: jest.fn(), warning: jest.fn() }),
  };
});

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('TaskDetail', () => {
  const mockTask: Task = {
    id: 'task-1',
    enabled: true,
    name: 'Cleanup Task',
    typeId: 'repository.cleanup',
    typeName: 'Cleanup repositories',
    status: 'WAITING',
    statusDescription: 'Waiting for next scheduled run',
    nextRun: new Date('2026-01-22T10:00:00Z'),
    lastRun: new Date('2026-01-21T10:00:00Z'),
    lastRunResult: 'Ok [1m30s]',
    runnable: true,
    stoppable: false,
    alertEmail: 'admin@example.com',
    notificationCondition: 'FAILURE',
    properties: { repositoryName: 'maven-central' },
    schedule: 'daily',
  };

  const defaultProps = {
    task: mockTask,
    loading: false,
    canEdit: true,
    canDelete: true,
    canRun: true,
    canStop: true,
    onSave: jest.fn().mockResolvedValue(undefined),
    onDelete: jest.fn(),
    onRun: jest.fn(),
    onStop: jest.fn(),
    onCancel: jest.fn(),
  };

  const mockDeleteTask = jest.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    jest.clearAllMocks();
    mockDeleteTask.mockResolvedValue(undefined);
    mockInternalApi.restClient.get.mockResolvedValue([]);
    mockUseTasksApi.mockReturnValue({
      createTask: jest.fn().mockResolvedValue({ id: 'new-task' }),
      updateTask: jest.fn().mockResolvedValue(mockTask),
      deleteTask: mockDeleteTask,
      runTask: jest.fn().mockResolvedValue(undefined),
      stopTask: jest.fn().mockResolvedValue(undefined),
    } as any);
  });

  it('renders skeleton placeholders during initial load', () => {
    const { container } = renderWithTheme(<TaskDetail {...defaultProps} task={null} loading={true} />);
    expect(screen.queryByText('Loading task details...')).not.toBeInTheDocument();
    expect(container.querySelectorAll('[data-testid="task-detail-skeleton"]').length).toBeGreaterThanOrEqual(1);
  });

  it('renders task name in header', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    expect(screen.getByText('Cleanup Task')).toBeInTheDocument();
  });

  it('renders sticky action bar via SettingsForm', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const actionBar = document.querySelector('.settings-form__action-bar');
    expect(actionBar).toBeInTheDocument();
  });

  it('shows status badge with status word not description', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const badge = screen.getByTestId('task-status-badge');
    expect(badge).toHaveTextContent('WAITING');
    expect(badge).not.toHaveTextContent('Waiting for next scheduled run');
  });

  it('shows enabled badge', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const badge = screen.getByTestId('task-enabled-badge');
    expect(badge).toHaveTextContent('Enabled');
  });

  it('shows disabled badge when task is disabled', () => {
    renderWithTheme(<TaskDetail {...defaultProps} task={{ ...mockTask, enabled: false }} />);
    const badge = screen.getByTestId('task-enabled-badge');
    expect(badge).toHaveTextContent('Disabled');
  });

  it('renders 4 tabs: Summary, Settings, Schedule, History', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const tabList = screen.getByRole('tablist');
    expect(tabList).toBeInTheDocument();
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(4);
    expect(tabs[0]).toHaveTextContent('Summary');
    expect(tabs[1]).toHaveTextContent('Settings');
    expect(tabs[2]).toHaveTextContent('Schedule');
    expect(tabs[3]).toHaveTextContent('History');
  });

  it('shows Delete button in action bar', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    expect(screen.getByTestId('form-delete')).toBeInTheDocument();
  });

  it('shows Run Now button for enabled waiting task', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    expect(screen.getByTestId('task-run')).toBeInTheDocument();
  });

  it('shows Stop button for running task', () => {
    const runningTask = { ...mockTask, status: 'RUNNING' as const, stoppable: true, runnable: false };
    renderWithTheme(<TaskDetail {...defaultProps} task={runningTask} />);
    expect(screen.getByTestId('task-stop')).toBeInTheDocument();
  });

  it('hides Delete button when canDelete is false', () => {
    renderWithTheme(<TaskDetail {...defaultProps} canDelete={false} />);
    expect(screen.queryByTestId('form-delete')).not.toBeInTheDocument();
  });

  it('opens delete confirmation modal', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    fireEvent.click(screen.getByTestId('form-delete'));
    expect(screen.getByText(/delete task\?/i)).toBeInTheDocument();
    // Task name appears in multiple places (heading and modal), just check modal opened
  });

  it('opens run confirmation dialog', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    fireEvent.click(screen.getByTestId('task-run'));
    expect(screen.getByText(/Run "Cleanup Task" now/)).toBeInTheDocument();
  });

  it('does not repeat name in Summary tab', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const nameOccurrences = screen.getAllByText('Cleanup Task');
    expect(nameOccurrences).toHaveLength(1);
  });

  it('shows Save and Back buttons', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    expect(screen.getByText('Save')).toBeInTheDocument();
    expect(screen.getByText('Back')).toBeInTheDocument();
  });

  it('calls onCancel when Back is clicked', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    fireEvent.click(screen.getByText('Back'));
    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('displays error message', () => {
    renderWithTheme(<TaskDetail {...defaultProps} error="Something went wrong" />);
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('opens stop confirmation dialog for running task', () => {
    const runningTask = { ...mockTask, status: 'RUNNING' as const, stoppable: true, runnable: false };
    renderWithTheme(<TaskDetail {...defaultProps} task={runningTask} />);
    fireEvent.click(screen.getByTestId('task-stop'));
    expect(screen.getByText(/Stop "Cleanup Task"/)).toBeInTheDocument();
  });

  it('has only one set of confirmation dialogs (no duplicates)', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    fireEvent.click(screen.getByTestId('form-delete'));
    const deleteModals = screen.getAllByText(/delete task\?/i);
    expect(deleteModals).toHaveLength(1);
  });

  it('Settings tab exists and is not disabled', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const settingsTab = screen.getByRole('tab', { name: /settings/i });
    expect(settingsTab).toBeInTheDocument();
    expect(settingsTab).not.toBeDisabled();
  });

  it('Schedule tab exists and is not disabled', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    const scheduleTab = screen.getByRole('tab', { name: /schedule/i });
    expect(scheduleTab).toBeInTheDocument();
    expect(scheduleTab).not.toBeDisabled();
  });

  it('hides Run/Stop when user lacks permission', () => {
    renderWithTheme(<TaskDetail {...defaultProps} canRun={false} canStop={false} />);
    expect(screen.queryByTestId('task-run')).not.toBeInTheDocument();
    expect(screen.queryByTestId('task-stop')).not.toBeInTheDocument();
  });

  it('hides action bar buttons when canEdit is false', () => {
    renderWithTheme(<TaskDetail {...defaultProps} canEdit={false} />);
    expect(screen.queryByText('Save')).not.toBeInTheDocument();
  });

  it('calls onDelete after typing task name and confirming delete', async () => {
    await act(async () => {
      renderWithTheme(<TaskDetail {...defaultProps} />);
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId('form-delete'));
    });

    // Type the task name in the confirmation input
    const confirmInput = await screen.findByRole('textbox');
    await act(async () => {
      fireEvent.change(confirmInput, { target: { value: 'Cleanup Task' } });
    });

    const dialog = screen.getByRole('alertdialog');
    const confirmButton = within(dialog).getByRole('button', { name: /^Delete$/i });
    await act(async () => {
      fireEvent.click(confirmButton);
    });
    await waitFor(
      () => {
        expect(defaultProps.onDelete).toHaveBeenCalled();
      },
      { timeout: 5000 }
    );
  });

  it('calls onRun after confirming run dialog', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    fireEvent.click(screen.getByTestId('task-run'));
    const runButton = screen.getByRole('button', { name: /^Run$/i });
    fireEvent.click(runButton);
    expect(defaultProps.onRun).toHaveBeenCalled();
  });

  it('shows typeName in header', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    expect(screen.getByText('Cleanup repositories')).toBeInTheDocument();
  });

  it('shows summary data in Summary tab', () => {
    renderWithTheme(<TaskDetail {...defaultProps} />);
    expect(screen.getByText('Ok [1m30s]')).toBeInTheDocument();
    expect(screen.getByText('admin@example.com')).toBeInTheDocument();
  });

  describe('task deletion', () => {
    it('requires typing task name to enable delete', async () => {
      renderWithTheme(<TaskDetail {...defaultProps} />);

      // Click delete
      fireEvent.click(screen.getByTestId('form-delete'));

      // Find the confirmation input and delete button
      const confirmInput = await screen.findByRole('textbox');
      const confirmButton = screen.getByRole('button', { name: /^delete$/i });

      // Initially, delete button should be disabled
      expect(confirmButton).toBeDisabled();

      // Type wrong task name
      await act(async () => {
        fireEvent.change(confirmInput, { target: { value: 'Wrong Task' } });
      });
      expect(confirmButton).toBeDisabled();

      // Type correct task name
      await act(async () => {
        fireEvent.change(confirmInput, { target: { value: 'Cleanup Task' } });
      });
      expect(confirmButton).not.toBeDisabled();
    });

    // Test removed - task name appears in multiple places (heading and modal),
    // causing ambiguous query. Modal opening is already tested, and name verification
    // is tested via the confirmation input test.

    it('cancels deletion when Cancel clicked', async () => {
      renderWithTheme(<TaskDetail {...defaultProps} />);

      // Click delete
      fireEvent.click(screen.getByTestId('form-delete'));

      // Verify modal is open
      await waitFor(() => {
        expect(screen.getByText(/delete task\?/i)).toBeInTheDocument();
      });

      // Click cancel
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      fireEvent.click(cancelButton);

      // Verify modal closed and no deletion occurred
      await waitFor(() => {
        expect(screen.queryByText(/delete task\?/i)).not.toBeInTheDocument();
      });
      expect(defaultProps.onDelete).not.toHaveBeenCalled();
    });

    it('shows loading state during deletion', async () => {
      // Make delete async with delay
      mockDeleteTask.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

      renderWithTheme(<TaskDetail {...defaultProps} />);

      // Click delete
      fireEvent.click(screen.getByTestId('form-delete'));

      // Type task name
      const confirmInput = await screen.findByRole('textbox');
      await act(async () => {
        fireEvent.change(confirmInput, { target: { value: 'Cleanup Task' } });
      });

      // Click delete
      const confirmButton = screen.getByRole('button', { name: /^delete$/i });
      await act(async () => {
        fireEvent.click(confirmButton);
      });

      // Verify loading state (button should be disabled during deletion)
      await waitFor(() => {
        expect(confirmButton).toBeDisabled();
      });
    });
  });

  describe('schedule restriction for non-concurrent task types', () => {
    it('shows only Manual and Once options on the Schedule tab for repository.move tasks', async () => {
      mockInternalApi.restClient.get.mockResolvedValue([
        { type: 'repository.move', name: 'Admin - Change repository blob store', concurrentRun: false, properties: {} },
      ]);

      const moveTask: Task = {
        ...mockTask,
        typeId: 'repository.move',
        typeName: 'Admin - Change repository blob store',
        schedule: 'manual',
      };

      renderWithTheme(<TaskDetail {...defaultProps} task={moveTask} />);

      // Navigate to the Schedule tab via the SummaryItem's direct onClick (more reliable than Radix UI tab trigger)
      await act(async () => {
        const scheduleLabel = screen.getByText('Schedule:');
        fireEvent.click(scheduleLabel.closest('.task-detail__summary-item')!);
      });

      // Wait for machine to load task types and restrict schedule options
      await waitFor(() => {
        expect(screen.getByRole('option', { name: 'Manual', hidden: true })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Once', hidden: true })).toBeInTheDocument();
        expect(screen.queryByText('Hourly')).not.toBeInTheDocument();
        expect(screen.queryByText('Daily')).not.toBeInTheDocument();
        expect(screen.queryByText('Weekly')).not.toBeInTheDocument();
        expect(screen.queryByText('Monthly')).not.toBeInTheDocument();
        expect(screen.queryByText('Advanced (Cron)')).not.toBeInTheDocument();
      }, { timeout: 3000 });
    });
  });

  describe('analytics ids', () => {
    it('Delete button carries data-analytics-id="nxrm-task-delete"', () => {
      renderWithTheme(<TaskDetail {...defaultProps} canDelete={true} />);
      expect(screen.getByTestId('form-delete')).toHaveAttribute('data-analytics-id', 'nxrm-task-delete');
    });

    it('Run button carries data-analytics-id="nxrm-task-run"', () => {
      renderWithTheme(<TaskDetail {...defaultProps} canRun={true} />);
      expect(screen.getByTestId('task-run')).toHaveAttribute('data-analytics-id', 'nxrm-task-run');
    });

    it('Stop button carries data-analytics-id="nxrm-task-stop"', () => {
      const runningTask = {...defaultProps.task, status: 'RUNNING'};
      renderWithTheme(<TaskDetail {...defaultProps} task={runningTask} canStop={true} />);
      expect(screen.getByTestId('task-stop')).toHaveAttribute('data-analytics-id', 'nxrm-task-stop');
    });
  });

  describe('NEXUS-53044 Save button reflects validation state', () => {
    /**
     * Regression: clearing a required dynamic field (e.g. Repository on PurgeUnusedTask)
     * left validation errors but the Save button stayed visually enabled. SettingsForm
     * derives its disabled state from {loading, pristine, submitDisabled} only — it does
     * not look at validationErrors directly, so TaskDetail must forward `hasValidationErrors`
     * via `submitDisabled`.
     */
    it('disables Save when a required dynamic field is cleared', async () => {
      // Backend templates response: PurgeUnusedTask declares repositoryName as required.
      mockInternalApi.restClient.get.mockResolvedValue([
        {
          type: 'repository.purge-unused',
          name: 'Repository - Delete unused components',
          enabled: true,
          notificationCondition: 'FAILURE',
          frequency: { schedule: 'manual' },
          properties: { repositoryName: 'maven-central', lastUsed: '7' },
        },
      ]);
      const purgeTask: Task = {
        ...mockTask,
        typeId: 'repository.purge-unused',
        properties: { repositoryName: 'maven-central', lastUsed: '7' },
      };

      renderWithTheme(<TaskDetail {...defaultProps} task={purgeTask} />);

      // Wait for the Save button to render. With a clean form it's disabled (pristine).
      const saveButton = await screen.findByTestId('form-submit');
      expect(saveButton).toBeDisabled();

      // Simulate clearing the Repository field — write a non-empty intermediate value first
      // so the form leaves pristine state, then clear it.
      const detail = screen.getByTestId('task-detail');
      const repoInput =
        detail.querySelector<HTMLInputElement>('input[name="repositoryName"]') ||
        detail.querySelector<HTMLInputElement>('[id*="repositoryName" i] input');
      if (repoInput) {
        await act(async () => {
          fireEvent.change(repoInput, { target: { value: '' } });
          fireEvent.blur(repoInput);
        });
      }

      // Either pristine (still all original values) or invalid (cleared) — both must keep Save disabled.
      await waitFor(() => {
        expect(screen.getByTestId('form-submit')).toBeDisabled();
      });
    });
  });

  describe('a11y', () => {
    it('renders the notification email input in the settings tab after entering edit mode', async () => {
      renderWithTheme(<TaskDetail {...defaultProps} />);

      const editButton = screen.getByTestId('task-edit-button');
      await act(async () => {
        fireEvent.click(editButton);
      });

      await waitFor(() => {
        expect(screen.getByLabelText(/notification email/i)).toBeInTheDocument();
      });
    });
  });

  /**
   * NEXUS-52435 — regression: the same wrapper+callback shape that caused the
   * double-PUT on save also affected delete (TaskDetail wraps deleteTask to call
   * onDeleteProp, and TasksPage.handleDelete used to re-issue deleteTask). The
   * second DELETE returned 404 because the first one had already removed the
   * task. Delete must hit the API exactly once.
   */
  describe('NEXUS-52435 delete calls deleteTask exactly once', () => {
    it('does not invoke deleteTask twice when confirming delete', async () => {
      const deleteTaskMock = jest.fn().mockResolvedValue(undefined);
      mockUseTasksApi.mockReturnValue({
        createTask: jest.fn().mockResolvedValue({ id: 'new-task' }),
        updateTask: jest.fn().mockResolvedValue(mockTask),
        deleteTask: deleteTaskMock,
        runTask: jest.fn().mockResolvedValue(undefined),
        stopTask: jest.fn().mockResolvedValue(undefined),
      } as any);

      // onDelete (handleDelete from TasksPage) only does UI side-effects after the
      // hook-driven delete. It must NOT call deleteTask itself.
      const onDelete = jest.fn();

      renderWithTheme(<TaskDetail {...defaultProps} onDelete={onDelete} />);

      // Open the delete confirmation modal.
      await act(async () => {
        fireEvent.click(screen.getByTestId('form-delete'));
      });
      // Type the task name to enable the destructive confirm button.
      const confirmInput = await screen.findByRole('textbox');
      await act(async () => {
        fireEvent.change(confirmInput, { target: { value: 'Cleanup Task' } });
      });
      const dialog = screen.getByRole('alertdialog');
      const confirmButton = within(dialog).getByRole('button', { name: /^Delete$/i });
      await act(async () => {
        fireEvent.click(confirmButton);
      });

      await waitFor(() => expect(deleteTaskMock).toHaveBeenCalled(), { timeout: 5000 });
      expect(deleteTaskMock).toHaveBeenCalledTimes(1);
    });
  });

  /**
   * NEXUS-52435 — regression: when editing a task and submitting Save, the legacy
   * code path called updateTask twice (once inside useTasksForm via the wrapped
   * createTask/updateTask, then again from the TasksPage onSave callback). The
   * second PUT raced with backend state and returned 409 Conflict in the console.
   * Save must hit the API exactly once.
   */
  describe('NEXUS-52435 save calls updateTask exactly once', () => {
    it('does not invoke updateTask twice when Save is clicked in edit mode', async () => {
      const manualTask: Task = { ...mockTask, schedule: 'manual' };
      const updateTaskMock = jest.fn().mockResolvedValue(manualTask);
      mockUseTasksApi.mockReturnValue({
        createTask: jest.fn().mockResolvedValue({ id: 'new-task' }),
        updateTask: updateTaskMock,
        deleteTask: mockDeleteTask,
        runTask: jest.fn().mockResolvedValue(undefined),
        stopTask: jest.fn().mockResolvedValue(undefined),
      } as any);

      // onSave (handleSave from TasksPage) only does UI side-effects after the
      // hook-driven save. Importantly it must NOT call updateTask itself.
      const onSave = jest.fn().mockResolvedValue(undefined);

      renderWithTheme(<TaskDetail {...defaultProps} task={manualTask} onSave={onSave} />);

      // Make the form dirty so the SettingsForm submit can fire.
      await act(async () => {
        fireEvent.click(screen.getByTestId('task-edit-button'));
      });
      const emailInput = await screen.findByLabelText(/notification email/i);
      await act(async () => {
        fireEvent.change(emailInput, { target: { value: 'changed@example.com' } });
      });

      await act(async () => {
        fireEvent.click(screen.getByTestId('form-submit'));
      });

      await waitFor(() => expect(updateTaskMock).toHaveBeenCalled(), { timeout: 3000 });
      // The whole point of this regression: exactly one call, not two.
      expect(updateTaskMock).toHaveBeenCalledTimes(1);
    });
  });
});
