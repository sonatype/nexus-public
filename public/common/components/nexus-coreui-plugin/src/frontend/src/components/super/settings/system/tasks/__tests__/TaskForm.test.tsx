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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { TaskForm } from '../TaskForm';
import { useTasksApi } from '../useTasksApi';
import { useTasksForm } from '../useTasksForm';
import { Task, TaskType } from '../types';

// Mock hooks
jest.mock('../useTasksApi');
jest.mock('../useTasksForm');

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
  TaskScheduler: ({ value, onChange, errors, disabled }) => (
    <div data-testid="task-scheduler">
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
jest.mock('../../../../shared/form', () => ({
  WizardForm: ({ children, steps, currentStep, onStepChange, onComplete, onCancel, completeLabel, canAdvance, loading, error, testId, footerExtra }) => (
    <div data-testid={testId || 'wizard-form'}>
      <div data-testid={`${testId || 'wizard-form'}-steps`}>
        {steps.map((step, i) => (
          <div key={step.id} data-testid={`wizard-step-${step.id}`}>
            {step.label}
          </div>
        ))}
      </div>
      <div data-step-index={currentStep}>
        {children}
      </div>
      <button onClick={onCancel} disabled={loading}>Cancel</button>
      <button onClick={onComplete} disabled={!canAdvance || loading}>{completeLabel || 'Complete'}</button>
      {footerExtra}
    </div>
  ),
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
  SettingsButton: ({ children, onClick, disabled, variant, icon: Icon, testId }) => (
    <button onClick={onClick} disabled={disabled} data-variant={variant} data-testid={testId}>
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

  beforeEach(() => {
    jest.clearAllMocks();
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
});
