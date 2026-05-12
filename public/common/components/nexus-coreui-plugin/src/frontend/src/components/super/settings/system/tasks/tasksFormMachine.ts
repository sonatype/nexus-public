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

import { assign } from 'xstate';
import { createFormMachine, ENDPOINTS, restClient } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';

import {
  Task,
  TaskType,
  TaskFormData,
  ScheduleType,
  ScheduleData,
  FormField,
  DEFAULT_TASK_FORM_DATA,
  isValidCronExpression,
  isValidEmail,
} from './types';

// =============================================================================
// TASK FORM DATA (extended with schedule metadata for machine context)
// =============================================================================

/**
 * Extended task form data used by the machine.
 * The machine tracks both the form data and the currently-selected task type
 * metadata so validation and field rendering can be driven from context.
 */
export interface TaskMachineFormData extends TaskFormData {
  /** Start time (HH:mm format) - tracked separately from startDate */
  startTime?: string;
}

// =============================================================================
// SCHEDULE TYPE GUARDS
// =============================================================================

const SCHEDULE_TYPES: ScheduleType[] = [
  'manual', 'once', 'hourly', 'daily', 'weekly', 'monthly', 'advanced',
];

/**
 * Guard factory: creates a guard that checks if SCHEDULE_CHANGE targets a specific schedule.
 */
const isScheduleGuard = (targetSchedule: ScheduleType) =>
  (_context: unknown, event: { type: string; value?: string }) => event.value === targetSchedule;

// =============================================================================
// VALIDATION
// =============================================================================

/**
 * Validate task form data.
 * Returns an object with field names as keys and error messages as values.
 */
function validateTask(data: TaskMachineFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  // Name is always required
  if (!data.name?.trim()) {
    errors.name = 'Task name is required';
  }

  // Task type is required
  if (!data.typeId) {
    errors.typeId = 'Task type is required';
  }

  // Email validation (optional field)
  if (data.alertEmail && !isValidEmail(data.alertEmail)) {
    errors.alertEmail = 'Invalid email address format';
  }

  // Schedule-specific validation
  if (!data.schedule) {
    errors.schedule = 'Schedule is required';
  }

  // Time-based schedules require start date and time
  if (['once', 'hourly', 'daily', 'weekly', 'monthly'].includes(data.schedule)) {
    if (!data.startDate) {
      errors.startDate = 'Start date is required';
    }
    if (!data.startTime) {
      errors.startTime = 'Start time is required';
    }
  }

  // Weekly/monthly require at least one recurring day
  if (['weekly', 'monthly'].includes(data.schedule)) {
    if (!data.recurringDays || data.recurringDays.length === 0) {
      errors.recurringDays = 'Select at least one day';
    }
  }

  // Advanced schedule requires a valid cron expression
  if (data.schedule === 'advanced') {
    if (!data.cronExpression?.trim()) {
      errors.cronExpression = 'Cron expression is required';
    } else if (!isValidCronExpression(data.cronExpression)) {
      errors.cronExpression = 'Invalid cron expression format (expected 5-7 parts)';
    }
  }

  return errors;
}

// =============================================================================
// REST HELPERS
// =============================================================================

const TASKS_TEMPLATES_URL = `${ENDPOINTS.TASKS}/templates`;

/**
 * Fetch task types from REST API
 */
async function fetchTaskTypes(): Promise<TaskType[]> {
  try {
    const data = await restClient.get(TASKS_TEMPLATES_URL);
    if (!Array.isArray(data)) return [];
    return data.map((template: any) => {
      const formFields = Object.entries(template.properties || {}).map(
        ([key, value]: [string, unknown]) => ({
          id: key,
          type: 'string' as const,
          label: key,
          required: false,
          initialValue: value as string,
        })
      );
      return {
        id: template.type,
        name: template.name,
        exposed: true,
        formFields: formFields.length > 0 ? formFields : undefined,
      } as TaskType;
    });
  } catch (err) {
    console.error('Failed to load task types:', err);
    return [];
  }
}

/**
 * Fetch a single task by ID
 */
async function fetchTask(taskId: string): Promise<Task | null> {
  try {
    const data = await restClient.get(`${ENDPOINTS.TASKS}/${encodeURIComponent(taskId)}`);
    if (!data) return null;
    const rest = data as any;
    return {
      id: rest.id,
      enabled: rest.enabled,
      name: rest.name,
      typeId: rest.type,
      typeName: rest.type,
      status: rest.currentState || 'WAITING',
      statusDescription: rest.message || '',
      nextRun: rest.nextRun ? new Date(rest.nextRun) : null,
      lastRun: rest.lastRun ? new Date(rest.lastRun) : null,
      lastRunResult: rest.lastRunResult || null,
      runnable: rest.currentState !== 'RUNNING',
      stoppable: rest.currentState === 'RUNNING',
      properties: rest.properties || {},
      schedule: (rest.frequency?.schedule || rest.schedule || 'manual') === 'cron' ? 'advanced' : (rest.frequency?.schedule || rest.schedule || 'manual'),
      startDate: rest.frequency?.startDate ? new Date(rest.frequency.startDate) : null,
      recurringDays: rest.frequency?.recurringDays || [],
      cronExpression: rest.frequency?.cronExpression || '',
      timeZoneOffset: rest.frequency?.timeZoneOffset || '',
    } as Task;
  } catch (err) {
    console.error('Failed to load task:', err);
    throw err;
  }
}

// =============================================================================
// MACHINE FACTORY
// =============================================================================

/**
 * Create a task form machine with XState.
 *
 * Task types are DYNAMIC (come from the API at runtime), so rather than using
 * editingConfig with compile-time sub-states, the machine keeps the selected
 * task type and its field metadata in context. The component reads
 * `context.selectedTaskType.formFields` to render dynamic fields.
 *
 * Schedule types ARE known at build time, so the machine uses editingConfig
 * to model schedule variant sub-states with field metadata per schedule type.
 */
export function createTaskFormMachine(
  taskId: string | undefined,
  preloadedTask?: Task
) {
  return createFormMachine({
    id: `task-form-${taskId ?? 'new'}`,
    context: {
      data: {
        ...DEFAULT_TASK_FORM_DATA,
        startTime: '00:00',
      } as TaskMachineFormData,
      // Reference data populated by the load service
      task: preloadedTask ?? (null as Task | null),
      taskTypes: [] as TaskType[],
      selectedTaskType: null as TaskType | null,
    },
    actions: {
      validate: assign((ctx: FormContext<TaskMachineFormData>) => ({
        validationErrors: validateTask(ctx.data),
      })),
      // Custom action: update task type, reset dynamic properties, and select type metadata
      changeTaskType: assign((context: any, event: any) => {
        const typeId = event.value;
        const taskType = (context.taskTypes as TaskType[]).find((t) => t.id === typeId) ?? null;

        // Initialize properties with defaults from the selected task type's form fields
        const properties: Record<string, string> = {};
        taskType?.formFields?.forEach((field: FormField) => {
          if (field.initialValue !== null && field.initialValue !== undefined) {
            properties[field.id] = String(field.initialValue);
          }
        });

        const newData = { ...context.data, typeId, properties };
        return {
          data: newData,
          // Reset pristine baseline so type selection is NOT treated as a user edit
          pristineData: { ...newData },
          touched: { ...context.touched, typeId: true },
          selectedTaskType: taskType,
        };
      }),
      // Custom action: update schedule type and reset schedule-specific fields
      changeSchedule: assign((context: any, event: any) => {
        const schedule = event.value as ScheduleType;
        // Use data from event if provided (from TaskScheduler), otherwise compute defaults
        const scheduleData = event.data as ScheduleData | undefined;
        return {
          data: {
            ...context.data,
            schedule,
            // Use values from scheduleData if available, otherwise reset to defaults
            startDate: scheduleData?.startDate ?? (
              ['once', 'hourly', 'daily', 'weekly', 'monthly'].includes(schedule)
                ? context.data.startDate || new Date()
                : null
            ),
            startTime: scheduleData?.startTime ?? (
              ['once', 'hourly', 'daily', 'weekly', 'monthly'].includes(schedule)
                ? context.data.startTime || '00:00'
                : undefined
            ),
            recurringDays: scheduleData?.recurringDays ?? (
              ['weekly', 'monthly'].includes(schedule)
                ? context.data.recurringDays || []
                : []
            ),
            cronExpression: scheduleData?.cronExpression ?? (
              schedule === 'advanced' ? context.data.cronExpression || '' : ''
            ),
            timeZoneOffset: scheduleData?.timeZoneOffset ?? context.data.timeZoneOffset ?? '',
          },
          touched: { ...context.touched, schedule: true },
        };
      }),
    },
    guards: {
      // Schedule type guards
      ...SCHEDULE_TYPES.reduce((guards, scheduleType) => {
        guards[`isSchedule_${scheduleType}`] = isScheduleGuard(scheduleType) as any;
        return guards;
      }, {} as Record<string, unknown>),
    },
    services: {
      load: async () => {
        // Load task and task types in parallel
        const [task, taskTypes] = await Promise.all([
          preloadedTask
            ? Promise.resolve(preloadedTask)
            : taskId
            ? fetchTask(taskId).catch((err: unknown) => {
                console.error('Failed to load task:', err);
                throw err;
              })
            : Promise.resolve(null),
          fetchTaskTypes().catch((err: unknown) => {
            console.error('Failed to load task types:', err);
            return [] as TaskType[];
          }),
        ]);

        // Find the selected task type from the loaded types
        const selectedTaskType = task
          ? taskTypes.find((t) => t.id === task.typeId) ?? null
          : null;

        // Build initial form data
        const initialData: TaskMachineFormData = task
          ? {
              id: task.id,
              enabled: task.enabled,
              name: task.name,
              typeId: task.typeId,
              alertEmail: task.alertEmail || '',
              notificationCondition: task.notificationCondition || 'FAILURE',
              properties: { ...task.properties },
              schedule: task.schedule || 'manual',
              startDate: task.startDate ? new Date(task.startDate as string | number) : null,
              startTime: task.startDate
                ? (() => {
                    const d = new Date(task.startDate as string | number);
                    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                  })()
                : '00:00',
              recurringDays: task.recurringDays || [],
              cronExpression: task.cronExpression || '',
              timeZoneOffset: task.timeZoneOffset || '',
            }
          : {
              ...DEFAULT_TASK_FORM_DATA,
              startTime: '00:00',
            };

        return {
          data: initialData,
          task,
          taskTypes,
          selectedTaskType,
        };
      },
      // save and delete services are provided via useForm options
      delete: () => Promise.reject(new Error('Delete service not configured')),
    },
    // Custom events for task type and schedule changes
    on: {
      TASK_TYPE_CHANGE: {
        actions: ['changeTaskType', 'validate', 'computePristine'],
      },
      SCHEDULE_CHANGE: [
        { target: '.manual', cond: 'isSchedule_manual', actions: ['changeSchedule', 'validate', 'computePristine'] },
        { target: '.once', cond: 'isSchedule_once', actions: ['changeSchedule', 'validate', 'computePristine'] },
        { target: '.hourly', cond: 'isSchedule_hourly', actions: ['changeSchedule', 'validate', 'computePristine'] },
        { target: '.daily', cond: 'isSchedule_daily', actions: ['changeSchedule', 'validate', 'computePristine'] },
        { target: '.weekly', cond: 'isSchedule_weekly', actions: ['changeSchedule', 'validate', 'computePristine'] },
        { target: '.monthly', cond: 'isSchedule_monthly', actions: ['changeSchedule', 'validate', 'computePristine'] },
        { target: '.advanced', cond: 'isSchedule_advanced', actions: ['changeSchedule', 'validate', 'computePristine'] },
      ],
    },
    // Schedule variant sub-states (known at build time)
    editingConfig: {
      defaultState: 'manual',
      typeField: 'schedule',
      states: {
        manual: {
          meta: {
            scheduleLabel: 'Manual',
            scheduleFields: [],
            requiredScheduleFields: [],
          },
        },
        once: {
          meta: {
            scheduleLabel: 'Once',
            scheduleFields: ['startDate', 'startTime'],
            requiredScheduleFields: ['startDate', 'startTime'],
          },
        },
        hourly: {
          meta: {
            scheduleLabel: 'Hourly',
            scheduleFields: ['startDate', 'startTime'],
            requiredScheduleFields: ['startDate', 'startTime'],
          },
        },
        daily: {
          meta: {
            scheduleLabel: 'Daily',
            scheduleFields: ['startDate', 'startTime'],
            requiredScheduleFields: ['startDate', 'startTime'],
          },
        },
        weekly: {
          meta: {
            scheduleLabel: 'Weekly',
            scheduleFields: ['startDate', 'startTime', 'recurringDays'],
            requiredScheduleFields: ['startDate', 'startTime', 'recurringDays'],
          },
        },
        monthly: {
          meta: {
            scheduleLabel: 'Monthly',
            scheduleFields: ['startDate', 'startTime', 'recurringDays'],
            requiredScheduleFields: ['startDate', 'startTime', 'recurringDays'],
          },
        },
        advanced: {
          meta: {
            scheduleLabel: 'Advanced (Cron)',
            scheduleFields: ['cronExpression'],
            requiredScheduleFields: ['cronExpression'],
          },
        },
      },
    },
  });
}
