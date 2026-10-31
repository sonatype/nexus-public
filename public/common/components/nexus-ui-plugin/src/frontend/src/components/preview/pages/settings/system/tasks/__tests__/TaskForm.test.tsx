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
import { TaskForm } from '../TaskForm';
import { useTasksApi } from '../useTasksApi';
import { useTasksForm } from '../useTasksForm';
import { Task, TaskType } from '../types';

// Mock hooks
jest.mock('../useTasksApi');
jest.mock('../useTasksForm');

// Mock the internal API module used by tasksFormMachine so it does not fire real
// network requests for /tasks/templates (see taskApiMock for details). (NEXUS-52612)
jest.mock('../../../../../../../interface/api', () =>
  require('./taskApiMock').createTaskApiMock()
);

// Mock TaskTypeSelector and TaskScheduler
jest.mock('../TaskTypeSelector', () => ({
  TaskTypeSelector: ({ taskTypes, onSelect, loading, error }) => (
    <div data-testid="task-type-selector">
      <input placeholder="Filter task types..." />
      {taskTypes.map((type) => (
        <div 
          key={type.id} 
          data-testid={`type-card-${type.id}`}
          onClick={() => onSelect(type)}
        >
          {type.name}
        </div>
      ))}
      {error && <div className="error">{error}</div>}
    </div>
  ),
  DynamicFormFields: ({ taskType, values, onChange, errors, disabled }) => (
    <div data-testid="dynamic-form-fields">
      {taskType.formFields.map((field) => (
        <div key={field.id}>
          <label>{field.label}</label>
          <input
            value={values[field.id] || ''}
            onChange={(e) => onChange(field.id, e.target.value)}
            disabled={disabled}
          />
          {errors?.[field.id] && <span className="error">{errors[field.id]}</span>}
        </div>
      ))}
    </div>
  ),
}));

jest.mock('../TaskScheduler', () => ({
  TaskScheduler: ({ value, onChange, errors, disabled, allowedSchedules }) => (
    <div
      data-testid="task-scheduler"
      data-allowed-schedules={allowedSchedules ? JSON.stringify(allowedSchedules) : undefined}
    >
      <select
        value={value.schedule}
        onChange={(e) => onChange({ ...value, schedule: e.target.value })}
        disabled={disabled}
      >
        <option value="manual">Manual</option>
        <option value="daily">Daily</option>
        <option value="weekly">Weekly</option>
      </select>
      {errors?.schedule && <span className="error">{errors.schedule}</span>}
    </div>
  ),
}));

// Mock shared form components
jest.mock('../../../../../shared/form', () => ({
  WizardForm: ({ children, steps, currentStep, onStepChange, onComplete, onCancel, completeLabel, canAdvance, loading, error, testId, footerExtra, submitAnalyticsId }) => {
    const isLastStep = currentStep === steps.length - 1;
    return (
      <div data-testid={testId || 'wizard-form'}>
        <div data-testid={`${testId || 'wizard-form'}-steps`}>
          {steps.map((step) => (
            <div key={step.id} data-testid={`wizard-step-${step.id}`}>
              {step.label}
            </div>
          ))}
        </div>
        <div data-step-index={currentStep}>
          {children}
        </div>
        <button onClick={onCancel} disabled={loading}>Cancel</button>
        {isLastStep ? (
          <button onClick={onComplete} disabled={!canAdvance || loading} {...(submitAnalyticsId ? {'data-analytics-id': submitAnalyticsId} : {})}>{completeLabel || 'Complete'}</button>
        ) : (
          <button
            onClick={() => onStepChange?.(currentStep + 1)}
            disabled={!canAdvance || loading}
            data-testid="wizard-next"
          >
            Next
          </button>
        )}
        {footerExtra}
      </div>
    );
  },
  SettingsFormSection: ({ children, title }) => (
    <div data-testid="settings-form-section">
      <h2>{title}</h2>
      {children}
    </div>
  ),
  SettingsTextInput: ({ label, value, onChange, error }) => (
    <div>
      <label>{label}</label>
      <input 
        value={value || ''} 
        onChange={(e) => onChange(e.target.value)} 
        data-testid={`input-${label}`}
      />
      {error && <span className="error">{error}</span>}
    </div>
  ),
  SettingsCheckbox: ({ label, checked, onChange }) => (
    <div>
      <label>
        <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
        {label}
      </label>
    </div>
  ),
  SettingsSelect: ({ label, value, onChange, options }) => (
    <div>
      <label>{label}</label>
      <select value={value} onChange={(e) => onChange(e.target.value)} data-testid={`select-${label}`}>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  ),
  SettingsAlert: ({ children }) => <div data-testid="alert">{children}</div>,
  SettingsButton: ({ children, onClick, disabled, variant, icon: Icon, testId, ...rest }) => (
    <button onClick={onClick} disabled={disabled} data-variant={variant} data-testid={testId} {...rest}>
      {Icon && <Icon />}
      {children}
    </button>
  ),
}));

const mockUseTasksApi = useTasksApi as jest.MockedFunction<typeof useTasksApi>;
const mockUseTasksForm = useTasksForm as jest.MockedFunction<typeof useTasksForm>;

function createMockTaskForm(data: any = {}, taskTypes: any[] = []) {
  return {
    field: jest.fn((name: string) => {
      const value = data[name];
      return { name, value: value != null ? String(value) : '', onChange: jest.fn(), onBlur: jest.fn(), error: undefined };
    }),
    data,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors: {},
    touched: {},
    state: { matches: jest.fn(() => false), context: { data, taskTypes, task: null, selectedTaskType: null } },
    send: jest.fn(),
  } as any;
}

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('TaskForm', () => {
  const mockTask: Task = {
    id: 'task-1',
    enabled: true,
    name: 'Test Task',
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
  };

  const mockTaskTypes: TaskType[] = [
    {
      id: 'repository.cleanup',
      name: 'Cleanup repositories',
      exposed: true,
      formFields: [
        {
          id: 'repositoryName',
          type: 'string',
          label: 'Repository',
          required: true,
          helpText: 'Select repository to clean',
        },
      ],
    },
    {
      id: 'db.backup',
      name: 'Database backup',
      exposed: true,
      formFields: [],
    },
  ];

  const defaultProps = {
    taskTypes: mockTaskTypes,
    isCreate: true,
    onSave: jest.fn().mockResolvedValue(undefined),
    onCancel: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseTasksApi.mockReturnValue({
      loading: false, error: null, setError: jest.fn(),
      fetchTasks: jest.fn(), fetchTask: jest.fn(), fetchTaskTypes: jest.fn().mockResolvedValue(mockTaskTypes),
      createTask: jest.fn(), updateTask: jest.fn(), deleteTask: jest.fn(), runTask: jest.fn(), stopTask: jest.fn(),
    } as any);
    mockUseTasksForm.mockImplementation(({ task }: any) => {
      const formData = task ? {
        id: task.id, enabled: task.enabled, name: task.name, typeId: task.typeId,
        alertEmail: task.alertEmail || '', notificationCondition: task.notificationCondition || 'FAILURE',
        properties: task.properties || {}, schedule: task.schedule || 'manual',
        startDate: task.startDate, recurringDays: task.recurringDays || [],
        cronExpression: task.cronExpression || '', timeZoneOffset: task.timeZoneOffset || '',
      } : { id: '', enabled: true, name: '', typeId: '', alertEmail: '', notificationCondition: 'FAILURE',
        properties: {}, schedule: 'manual', startDate: null, recurringDays: [], cronExpression: '', timeZoneOffset: '' };
      return {
        form: createMockTaskForm(formData, mockTaskTypes),
        task: task || null,
        taskTypes: mockTaskTypes,
        selectedTaskType: task ? mockTaskTypes.find(t => t.id === task.typeId) || null : null,
        isCreate: !task,
      } as any;
    });
  });

  describe('create mode', () => {
    it('renders the form', () => {
      const { container } = renderWithTheme(<TaskForm {...defaultProps} />);

      expect(container).toBeInTheDocument();
    });

    it('renders wizard step indicator', () => {
      renderWithTheme(<TaskForm {...defaultProps} />);

      // Wizard step indicator should be visible
      const stepIndicator = screen.getByTestId('task-wizard-steps');
      expect(stepIndicator).toBeInTheDocument();

      // Should show 3 steps for create mode
      expect(screen.getByTestId('wizard-step-type')).toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-config')).toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-schedule')).toBeInTheDocument();
    });

    it('shows task type selector on step 0', () => {
      renderWithTheme(<TaskForm {...defaultProps} />);

      // Task type selector is displayed as card grid on step 0
      const searchInput = screen.getByPlaceholderText('Filter task types...');
      expect(searchInput).toBeInTheDocument();

      // Task type cards are displayed
      const cleanupCard = screen.getByTestId('type-card-repository.cleanup');
      expect(cleanupCard).toBeInTheDocument();
      expect(cleanupCard).toHaveTextContent('Cleanup repositories');
    });

    it('calls onCancel when cancel button is clicked', () => {
      renderWithTheme(<TaskForm {...defaultProps} />);

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      fireEvent.click(cancelButton);

      expect(defaultProps.onCancel).toHaveBeenCalled();
    });

    /**
     * NEXUS-52435 — regression: classic UI kept the create button disabled until
     * every required descriptor field was filled. canAdvance on the configure
     * step must respect formField.required so e.g. an empty repositoryName
     * blocks the wizard from moving forward.
     */
    describe('required dynamic fields gate the configure step', () => {
      // Leave formData.typeId empty so TaskForm's initialTypeId effect kicks
      // in: it sets the local selectedTaskTypeObj, advances internalStep to 1
      // (the configure step) — exactly the state we want to assert on.
      function withCreateAtConfigStep(properties: Record<string, string>) {
        mockUseTasksForm.mockImplementation(() => ({
          form: createMockTaskForm({
            id: '', enabled: true, name: 'Filled name', typeId: '',
            alertEmail: '', notificationCondition: 'FAILURE',
            properties, schedule: 'manual',
            startDate: null, recurringDays: [], cronExpression: '', timeZoneOffset: '',
          }, mockTaskTypes),
          task: null,
          taskTypes: mockTaskTypes,
          selectedTaskType: null,
          isCreate: true,
        } as any));
      }

      it('disables Next when a required dynamic field is empty', () => {
        withCreateAtConfigStep({});
        renderWithTheme(<TaskForm {...defaultProps} initialTypeId="repository.cleanup" />);

        const nextButton = screen.getByTestId('wizard-next');
        expect(nextButton).toBeDisabled();
      });

      it('enables Next once the required dynamic field has a value', () => {
        withCreateAtConfigStep({ repositoryName: 'maven-central' });
        renderWithTheme(<TaskForm {...defaultProps} initialTypeId="repository.cleanup" />);

        const nextButton = screen.getByTestId('wizard-next');
        expect(nextButton).not.toBeDisabled();
      });

      it('disables Next when a required dynamic field is whitespace only', () => {
        withCreateAtConfigStep({ repositoryName: '   ' });
        renderWithTheme(<TaskForm {...defaultProps} initialTypeId="repository.cleanup" />);

        const nextButton = screen.getByTestId('wizard-next');
        expect(nextButton).toBeDisabled();
      });
    });
  });

  describe('edit mode', () => {
    it('renders form in edit mode', () => {
      const { container } = renderWithTheme(<TaskForm {...defaultProps} isCreate={false} task={mockTask} />);

      expect(container).toBeInTheDocument();
    });

    it('renders wizard step indicator with 2 steps', () => {
      renderWithTheme(<TaskForm {...defaultProps} isCreate={false} task={mockTask} />);

      // Wizard step indicator should be visible
      const stepIndicator = screen.getByTestId('task-wizard-steps');
      expect(stepIndicator).toBeInTheDocument();

      // Should show 2 steps for edit mode (config and schedule)
      expect(screen.getByTestId('wizard-step-config')).toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-schedule')).toBeInTheDocument();
      // Type step should not be present in edit mode
      expect(screen.queryByTestId('wizard-step-type')).not.toBeInTheDocument();
    });

    it('starts on configure step in edit mode', () => {
      renderWithTheme(<TaskForm {...defaultProps} isCreate={false} task={mockTask} />);

      // Config and schedule steps are present
      expect(screen.getByTestId('wizard-step-config')).toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-schedule')).toBeInTheDocument();

      // Type selector should not be visible in edit mode
      expect(screen.queryByPlaceholderText('Filter task types...')).not.toBeInTheDocument();
      expect(screen.queryByPlaceholderText('Search task category...')).not.toBeInTheDocument();
    });
  });

  describe('schedule restrictions', () => {
    const moveTaskType: TaskType = {
      id: 'repository.move',
      name: 'Admin - Change repository blob store',
      exposed: true,
      concurrentRun: false,
      formFields: [],
    };

    const moveTask: Task = {
      id: 'task-move',
      enabled: true,
      name: 'Move repos',
      typeId: 'repository.move',
      typeName: 'Admin - Change repository blob store',
      status: 'WAITING',
      statusDescription: 'Waiting',
      nextRun: null,
      lastRun: null,
      lastRunResult: null,
      runnable: true,
      stoppable: false,
      properties: {},
      schedule: 'manual',
      recurringDays: [],
      cronExpression: '',
      timeZoneOffset: '+00:00',
    };

    it('passes allowedSchedules=[manual,once] when selected task type has concurrentRun:false', () => {
      const allTypes = [...mockTaskTypes, moveTaskType];
      mockUseTasksForm.mockImplementation(() => ({
        form: createMockTaskForm(
          { schedule: 'manual', typeId: 'repository.move', enabled: true, name: 'Move repos',
            alertEmail: '', notificationCondition: 'FAILURE', properties: {},
            startDate: null, recurringDays: [], cronExpression: '', timeZoneOffset: '' },
          allTypes,
        ),
        task: moveTask,
        taskTypes: allTypes,
        selectedTaskType: moveTaskType,
        isCreate: false,
      } as any));

      renderWithTheme(
        <TaskForm
          taskTypes={allTypes}
          isCreate={false}
          task={moveTask}
          onSave={jest.fn()}
          onCancel={jest.fn()}
        />,
      );

      // Advance from config step (0) to schedule step (1)
      fireEvent.click(screen.getByTestId('wizard-next'));

      const scheduler = screen.getByTestId('task-scheduler');
      expect(scheduler).toHaveAttribute(
        'data-allowed-schedules',
        JSON.stringify(['manual', 'once']),
      );
    });

    it('resets schedule to manual when the selected task type restricts schedules and the current schedule is not allowed', () => {
      const mockSend = jest.fn();
      const allTypes = [...mockTaskTypes, moveTaskType];

      mockUseTasksForm.mockImplementation(() => ({
        form: {
          ...createMockTaskForm(
            { schedule: 'daily', typeId: 'repository.move', enabled: true, name: 'Move repos',
              alertEmail: '', notificationCondition: 'FAILURE', properties: {},
              startDate: null, recurringDays: [], cronExpression: '', timeZoneOffset: '' },
            allTypes,
          ),
          send: mockSend,
        },
        task: { ...moveTask, schedule: 'daily' },
        taskTypes: allTypes,
        selectedTaskType: moveTaskType,
        isCreate: false,
      } as any));

      renderWithTheme(
        <TaskForm
          taskTypes={allTypes}
          isCreate={false}
          task={{ ...moveTask, schedule: 'daily' }}
          onSave={jest.fn()}
          onCancel={jest.fn()}
        />,
      );

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({ type: 'SCHEDULE_CHANGE', value: 'manual' }),
      );
    });
  });

  describe('analytics ids', () => {
    const EDIT_TASK = {
      id: 'task-1', enabled: true, name: 'Test', typeId: 'repository.cleanup',
      typeName: 'Cleanup', status: 'WAITING', statusDescription: '',
      nextRun: null, lastRun: null, lastRunResult: '', runnable: true, stoppable: false,
      alertEmail: '', notificationCondition: 'FAILURE', properties: {}, schedule: 'manual',
      startDate: null, recurringDays: [], cronExpression: '', timeZoneOffset: '',
    };

    function renderEditTask(extra: Record<string, unknown> = {}) {
      mockUseTasksForm.mockImplementation(({task}: any) => ({
        form: createMockTaskForm(
          // repositoryName must be filled so canAdvance lets the wizard reach the Schedule step
          // (the wizard's Save button only appears on the last step).
          {id: 'task-1', enabled: true, name: 'Test', typeId: 'repository.cleanup',
            alertEmail: '', notificationCondition: 'FAILURE',
            properties: {repositoryName: 'maven-central'}, schedule: 'manual',
            startDate: null, recurringDays: [], cronExpression: '', timeZoneOffset: ''},
          defaultProps.taskTypes,
        ),
        task: task || null,
        taskTypes: defaultProps.taskTypes,
        selectedTaskType: defaultProps.taskTypes[0],
        isCreate: false,
      } as any));

      return renderWithTheme(
        <TaskForm
          taskTypes={defaultProps.taskTypes}
          isCreate={false}
          task={EDIT_TASK as any}
          onSave={jest.fn()}
          onCancel={jest.fn()}
          {...extra}
        />,
      );
    }

    it('Save button carries data-analytics-id="nxrm-task-save" in edit mode', () => {
      renderEditTask();

      // In edit mode: 2 steps, wizard shows Complete (Save Task) button on step 1.
      // Advance to schedule step to make the Save button appear.
      fireEvent.click(screen.getByTestId('wizard-next'));
      const saveBtn = screen.getByRole('button', {name: /save task/i});
      expect(saveBtn).toHaveAttribute('data-analytics-id', 'nxrm-task-save');
    });

    it('Delete button in footerExtra carries data-analytics-id="nxrm-task-delete"', () => {
      renderEditTask();

      const deleteBtn = screen.getByRole('button', {name: /delete/i});
      expect(deleteBtn).toHaveAttribute('data-analytics-id', 'nxrm-task-delete');
    });

    it('Run Now button in footerExtra carries data-analytics-id="nxrm-task-run"', () => {
      const onRun = jest.fn();
      renderEditTask({onRun});

      const runBtn = screen.getByRole('button', {name: /run now/i});
      expect(runBtn).toHaveAttribute('data-analytics-id', 'nxrm-task-run');
    });
  });

  /**
   * NEXUS-53358 — Schedule-only task types declare zero per-task form fields.
   * The Configure step must omit the "Task Configuration" section entirely
   * so the user sees only the common Settings (Enabled, Name, Email,
   * Notification) before moving to the Schedule step. AT-035 / BDD-023.
   */
  describe('Schedule-only task types omit Task Configuration section (NEXUS-53358)', () => {
    const SCHEDULE_ONLY: Array<[string, string]> = [
      ['repository.cleanup',                  'Admin - Cleanup repositories using their associated policies'],
      ['security.purge-api-keys',             'Admin - Delete orphaned API keys'],
      ['usertoken.cleanup',                   'Admin - Cleanup expired user tokens'],
      ['repository.docker.ecr.token.refresh', 'Docker - ECR Token Refresh'],
    ];

    it.each(SCHEDULE_ONLY)(
      'omits "Task Configuration" on the Configure step for %s',
      (typeId, name) => {
        const scheduleOnlyType: TaskType = {
          id: typeId,
          name,
          exposed: true,
          formFields: [],
        };

        const task: Task = {
          ...mockTask,
          id: `task-${typeId}`,
          name: 'Test Task',
          typeId,
          typeName: name,
          properties: {},
        };

        mockUseTasksForm.mockImplementation(() => ({
          form: createMockTaskForm(
            {
              id: task.id,
              enabled: true,
              name: task.name,
              typeId: task.typeId,
              alertEmail: '',
              notificationCondition: 'FAILURE',
              properties: {},
              schedule: 'manual',
              startDate: null,
              recurringDays: [],
              cronExpression: '',
              timeZoneOffset: '',
            },
            [scheduleOnlyType],
          ),
          task,
          taskTypes: [scheduleOnlyType],
          selectedTaskType: scheduleOnlyType,
          isCreate: false,
        } as any));

        renderWithTheme(
          <TaskForm
            {...defaultProps}
            taskTypes={[scheduleOnlyType]} // schedule-only override — no formFields
            isCreate={false}
            task={task}
          />,
        );

        // Settings section is always present on the Configure step.
        expect(screen.getByRole('heading', {name: 'Settings'})).toBeInTheDocument();

        // Task Configuration heading must NOT appear when formFields is empty.
        expect(
          screen.queryByRole('heading', {name: 'Task Configuration'}),
        ).not.toBeInTheDocument();

        // DynamicFormFields must not render at all.
        expect(screen.queryByTestId('dynamic-form-fields')).not.toBeInTheDocument();

        // No required fields means Next is immediately enabled — the user can
        // proceed to Schedule without filling anything in.
        expect(screen.getByTestId('wizard-next')).not.toBeDisabled();
      },
    );
  });

  // NEXUS-53485: Data Repair Plan is singleton + manual-only (parity with Classic).
  describe('singleton & manual-only parity (blobstore.planReconciliation)', () => {
    const PLAN = 'blobstore.planReconciliation';
    const planType: TaskType = {
      id: PLAN,
      name: 'Repair - Data Repair Plan',
      exposed: true,
      formFields: [],
    };
    const typesWithPlan = [...mockTaskTypes, planType];

    const apiWithTasks = (existing: Array<{ typeId: string }>) =>
      ({
        loading: false, error: null, setError: jest.fn(),
        fetchTasks: jest.fn().mockResolvedValue(existing),
        fetchTask: jest.fn(), fetchTaskTypes: jest.fn().mockResolvedValue(typesWithPlan),
        createTask: jest.fn(), updateTask: jest.fn(), deleteTask: jest.fn(), runTask: jest.fn(), stopTask: jest.fn(),
      } as any);

    it('hides the Data Repair Plan type in the create selector when one already exists', async () => {
      mockUseTasksApi.mockReturnValue(apiWithTasks([{ typeId: PLAN }]));

      renderWithTheme(<TaskForm {...defaultProps} taskTypes={typesWithPlan} />);

      await waitFor(() =>
        expect(screen.queryByTestId(`type-card-${PLAN}`)).not.toBeInTheDocument()
      );
      // Other types remain creatable.
      expect(screen.getByTestId('type-card-repository.cleanup')).toBeInTheDocument();
      expect(screen.getByTestId('type-card-db.backup')).toBeInTheDocument();
    });

    it('still offers the Data Repair Plan type when none exists yet', async () => {
      mockUseTasksApi.mockReturnValue(apiWithTasks([]));

      renderWithTheme(<TaskForm {...defaultProps} taskTypes={typesWithPlan} />);

      expect(await screen.findByTestId(`type-card-${PLAN}`)).toBeInTheDocument();
    });

    it('does not hide a non-singleton type even when an instance of it already exists', async () => {
      // db.backup is not a singleton — an existing instance must not remove it from the selector.
      mockUseTasksApi.mockReturnValue(apiWithTasks([{ typeId: 'db.backup' }]));

      renderWithTheme(<TaskForm {...defaultProps} taskTypes={typesWithPlan} />);

      expect(await screen.findByTestId('type-card-db.backup')).toBeInTheDocument();
      expect(screen.getByTestId(`type-card-${PLAN}`)).toBeInTheDocument();
    });

    it('omits the Schedule step for the manual-only Data Repair Plan task', async () => {
      mockUseTasksApi.mockReturnValue(apiWithTasks([]));

      renderWithTheme(<TaskForm {...defaultProps} taskTypes={typesWithPlan} />);

      fireEvent.click(await screen.findByTestId(`type-card-${PLAN}`));

      expect(screen.queryByTestId('wizard-step-schedule')).not.toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-config')).toBeInTheDocument();
    });

    it('keeps the Schedule step for non-manual-only task types', async () => {
      mockUseTasksApi.mockReturnValue(apiWithTasks([]));

      renderWithTheme(<TaskForm {...defaultProps} taskTypes={typesWithPlan} />);

      fireEvent.click(await screen.findByTestId('type-card-repository.cleanup'));

      expect(screen.getByTestId('wizard-step-schedule')).toBeInTheDocument();
    });

    it('omits the Schedule step when editing a manual-only task, even before selectedTaskType resolves', () => {
      mockUseTasksApi.mockReturnValue(apiWithTasks([]));
      const planTask = {
        ...mockTask, id: 'plan-1', name: 'Repair - Data Repair Plan',
        typeId: PLAN, typeName: 'Repair - Data Repair Plan', schedule: 'manual' as const,
      };
      // taskTypes intentionally omits the plan type so selectedTaskType is unresolved; the manual-only
      // decision must come from task.typeId (no one-render Schedule-step flash on edit).
      renderWithTheme(
        <TaskForm {...defaultProps} isCreate={false} task={planTask} taskTypes={mockTaskTypes} />
      );

      expect(screen.queryByTestId('wizard-step-schedule')).not.toBeInTheDocument();
      expect(screen.getByTestId('wizard-step-config')).toBeInTheDocument();
    });
  });

});
