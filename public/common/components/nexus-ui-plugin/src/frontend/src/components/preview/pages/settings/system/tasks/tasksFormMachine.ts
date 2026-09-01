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
import { ENDPOINTS, restClient } from '../../../../../../interface/api';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';

import { mapRestStateToStatus, restTemplateToTaskType, deriveExecutePlanProperties, type RestTaskTemplate } from './taskTransformers';
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
import {
  TASK_TYPE_FIELD_OVERRIDES,
  resolveTaskFieldMeta,
  resolveDefaultScope,
  mdyToIso,
  EXECUTE_RECONCILE_PLAN_TYPE_ID,
  PLAN_RECONCILE_TYPE_ID,
} from './taskFieldMetadata';

// =============================================================================
// PER-TASK-TYPE QUIRKS
// =============================================================================

/**
 * Cloud-only task that the backend auto-creates when a repository is deleted; it
 * runs once, three days later, to actually remove the blob store. The form needs
 * two special-cases for this task type — confined here so the rest of the form
 * code stays generic:
 *
 *  1. `properties.blobstoreName` is set to the (now-deleted) blob store name,
 *     which doesn't exist in `/v1/blobstores` anymore — the picker would render
 *     an orphan value. Clear the displayed value and treat the field as not
 *     required so Run/Save aren't blocked by the empty display. The persisted
 *     value remains in the backend so the manual Run still operates on the
 *     correct blob store.
 *  2. The auto-scheduled "once" trigger sometimes lands back without a
 *     `startDate` in the REST response even though `nextRun` is set. Fall back
 *     to `nextRun` so the Schedule tab shows the same date/time as the History
 *     tab's "Next Scheduled Run".
 */
export const CLOUD_BLOBSTORE_REMOVAL_TYPE_ID = 'nexus.cloud.blobstore.removal';

// =============================================================================
// CHECKBOX NORMALIZATION
// =============================================================================

/**
 * Fill in 'false' for any checkbox field that is recognized in TASK_FIELD_UI but
 * absent from the current properties map. Mirrors Classic/ExtJS behavior: unchecked
 * checkboxes always serialize as 'false', never as absent.
 *
 * Only iterates formFields supplied by the API template — never synthesizes a checkbox
 * for a task type that does not declare it.
 */
function normalizeCheckboxProperties(
  properties: Record<string, string>,
  formFields: FormField[] | null | undefined,
  taskTypeId: string | undefined
): Record<string, string> {
  if (!formFields) return properties;
  const normalized = { ...properties };
  for (const field of formFields) {
    if (resolveTaskFieldMeta(taskTypeId, field.id)?.type === 'checkbox' && !(field.id in normalized)) {
      normalized[field.id] = 'false';
    }
  }
  return normalized;
}

/**
 * Inverse of the serializeEmptyAs serialization. A loaded task carries a field's serializeEmptyAs
 * sentinel for empty values (e.g. the literal "null" Classic writes for an empty duration field);
 * map it back to '' so the form treats it as genuinely empty (empty input, no number-validation
 * error). serializeProperties converts it back to the sentinel on save, so the wire value
 * round-trips unchanged. Scoped via metadata — only fields that declare serializeEmptyAs.
 */
function normalizeSerializedEmpties(
  properties: Record<string, string>,
  formFields: FormField[] | null | undefined,
  taskTypeId: string | undefined
): Record<string, string> {
  if (!formFields) return properties;
  const normalized = { ...properties };
  for (const field of formFields) {
    const sentinel = resolveTaskFieldMeta(taskTypeId, field.id)?.serializeEmptyAs;
    if (sentinel !== undefined && normalized[field.id] === sentinel) {
      normalized[field.id] = '';
    }
  }
  return normalized;
}

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

/**
 * Full machine context — the standard FormContext fields plus the task-specific
 * reference data populated by the load service. Use this on assign() callbacks so
 * accesses to `selectedTaskType`, `taskTypes`, etc. retain type safety instead of
 * being widened to `any`.
 */
export interface TaskMachineContext extends FormContext<TaskMachineFormData> {
  task: Task | null;
  taskTypes: TaskType[];
  selectedTaskType: TaskType | null;
  prefetchedPlanTaskProps: Record<string, string> | null;
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
 *
 * When `selectedTaskType` is provided, every required form field on that task
 * type is also validated; missing values are surfaced under `errors.properties`
 * keyed by field id (consumed by `DynamicFormFields` in TaskTypeSelector).
 */
export function validateTask(
  data: TaskMachineFormData,
  selectedTaskType?: TaskType | null,
): ValidationErrors {
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

  // Per-property errors are written in two passes: (1) backend-supplied formFields metadata
  // for required-field enforcement, then (2) TASK_FIELD_UI metadata for type/range/cross-field
  // checks. Share one map so neither pass discards the other's errors.
  const properties = data.properties || {};
  const propertyErrors: Record<string, string> = {};

  // Per-type required-field validation (NEXUS-53357). A TASK_FIELD_UI override of
  // `required: false` vetoes the backend `required: true` flag — Block 2 below would
  // not be able to clear an error written here (it writes but never deletes), so guard
  // the write up-front to mirror Block 2's `meta.required !== false` semantic.
  //
  // Note: the generic 'Required' message below is overwritten by Block 2's richer
  // `${meta.label} is required` for any field that also has a TASK_FIELD_UI entry and
  // a key in data.properties (initialized by changeTaskType). This fallback covers
  // fields that have no TASK_FIELD_UI entry or are absent from properties.
  const requiredFields = selectedTaskType?.formFields?.filter((f) => f.required) ?? [];
  requiredFields.forEach((field) => {
    if (resolveTaskFieldMeta(data.typeId, field.id)?.required === false) return;
    const value = properties[field.id];
    if (value === undefined || value === null || String(value).trim() === '') {
      propertyErrors[field.id] = 'Required';
    }
  });

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

  // Per-property validation driven by TASK_FIELD_UI metadata.
  // Collected into the same propertyErrors map so TaskForm can read validationErrors.properties[key]
  // and the formFields-required errors above are not overwritten.
  for (const [key, value] of Object.entries(properties)) {
    const meta = resolveTaskFieldMeta(data.typeId, key);
    if (!meta) continue;

    const isRequired = meta.required !== false && meta.type !== 'checkbox' && !meta.hidden;
    // A field whose empty value serializes to a sentinel (e.g. the Data Repair Plan duration
    // fields, which serialize empty as the literal "null" for Classic parity) is "empty" when it
    // holds that sentinel — so a loaded "null" must not be flagged as a non-numeric value.
    const isEmpty = value === '' || value === null || value === undefined
      || (meta.serializeEmptyAs !== undefined && value === meta.serializeEmptyAs);
    // Prevent saving with empty blobstoreName for cloud blob-store removal task
    // to avoid corrupting backend state (the original blobstore name must be preserved).
    if (isRequired && isEmpty) {
      propertyErrors[key] = `${meta.label} is required`;
      continue;
    }

    if (meta.type === 'number' && !isEmpty) {
      const n = Number(value);
      if (Number.isNaN(n)) {
        propertyErrors[key] = `${meta.label} must be a number`;
      } else if (meta.min !== undefined && n < meta.min) {
        propertyErrors[key] = `${meta.label} must be ${meta.min} or greater`;
      } else if (meta.max !== undefined && n > meta.max) {
        propertyErrors[key] = `${meta.label} must be ${meta.max} or less`;
      }
    }

    if (meta.validate && !propertyErrors[key]) {
      const v = String(value ?? '');
      const err = meta.validate(v);
      if (err) {
        propertyErrors[key] = err;
      }
    }
  }

  // Date-range cross-field check: scoped to the two reconcile task types to prevent any
  // coincidental taskScope === 'dates' on unrelated tasks from triggering reconcile-field
  // validation. Use resolveDefaultScope so a new Execute task whose taskScope is still ''
  // (unset) but whose descriptor defaults to 'dates' is also validated.
  const effectiveScope = properties.taskScope || resolveDefaultScope(data.typeId);
  const isReconcileTask = data.typeId === PLAN_RECONCILE_TYPE_ID || data.typeId === EXECUTE_RECONCILE_PLAN_TYPE_ID;
  if (isReconcileTask && effectiveScope === 'dates') {
    const start = mdyToIso(properties.reconcileStartDate);
    const end = mdyToIso(properties.reconcileEndDate);
    if (start && end && end < start) {
      propertyErrors.reconcileEndDate = 'End date must be on or after start date';
    }
  }

  if (Object.keys(propertyErrors).length > 0) {
    // hasValidationErrors() treats any non-null value as an error, so a nested object
    // here disables Save without widening the shared ValidationErrors type alias.
    (errors as Record<string, unknown>).properties = propertyErrors;
  }

  return errors;
}

// =============================================================================
// REST HELPERS
// =============================================================================

const TASKS_TEMPLATES_URL = `${ENDPOINTS.TASKS}/templates`;

/**
 * Fetch task types from REST API.
 *
 * Delegates the per-template mapping to the shared `restTemplateToTaskType`
 * (taskTransformers.ts), which preserves NEXUS-53044's TASK_FIELD_UI enrichment
 * for backends that don't yet publish the NEXUS-53357 `template.formFields`
 * metadata — so EDIT and CREATE see the same FormField shape for every task type.
 */
async function fetchTaskTypes(): Promise<TaskType[]> {
  try {
    const data = await restClient.get<RestTaskTemplate[]>(TASKS_TEMPLATES_URL);
    return Array.isArray(data) ? data.map(restTemplateToTaskType) : [];
  } catch (err) {
    console.error('Failed to load task types:', err);
    return [];
  }
}

/**
 * Fetch a single task by ID
 * The GET /v1/tasks/{id} response is flat — cronExpression, recurringDays, etc.
 * are top-level fields on the response, not nested under a "frequency" object.
 */
async function fetchTask(taskId: string): Promise<Task | null> {
  try {
    const data = await restClient.get(`${ENDPOINTS.TASKS}/${encodeURIComponent(taskId)}`);
    if (!data) return null;
    const rest = data as any;
    const schedule = (rest.schedule || 'manual') === 'cron' ? 'advanced' : (rest.schedule || 'manual');
    // Normalize status through the SAME shared mapper as useTasksApi.fetchTask so a
    // page load (this path, when the task is fetched here) and a poll never disagree
    // about whether the task is running. Derive runnable/stoppable from the running
    // group rather than a literal `=== 'RUNNING'`, which missed progress suffixes and
    // sub-states (e.g. "RUNNING: 7 of 9", RUNNING_STARTING). (NEXUS-53525)
    const status = mapRestStateToStatus(rest.currentState);
    const isRunning = status === 'RUNNING';
    return {
      id: rest.id,
      enabled: rest.enabled,
      name: rest.name,
      typeId: rest.type,
      typeName: rest.type,
      status,
      statusDescription: rest.message || '',
      nextRun: rest.nextRun ? new Date(rest.nextRun) : null,
      lastRun: rest.lastRun ? new Date(rest.lastRun) : null,
      lastRunResult: rest.lastRunResult || null,
      runnable: !isRunning,
      stoppable: isRunning,
      properties: rest.properties || {},
      alertEmail: rest.alertEmail || '',
      notificationCondition: rest.notificationCondition || 'FAILURE',
      schedule,
      startDate: rest.startDate ? new Date(rest.startDate) : null,
      recurringDays: rest.recurringDays || [],
      cronExpression: rest.cronExpression || '',
      timeZoneOffset: rest.timeZoneOffset || '',
    } as Task;
  } catch (err) {
    console.error('Failed to load task:', err);
    throw err;
  }
}

// =============================================================================
// PLAN TASK HELPER
// =============================================================================

/**
 * Paginate through `/v1/tasks` to find the sibling Data Repair Plan task and
 * return its derived display properties (blob store, repository, scope, date
 * range). Returns null if no Plan task exists or the fetch fails.
 *
 * MAX_TASK_PAGES bounds the loop — worst case 100 sequential GET /v1/tasks
 * calls, but a typical install with one Plan task resolves on page 1.
 */
async function fetchPlanTaskProps(): Promise<Record<string, string> | null> {
  try {
    type TaskPage = { items?: Array<{ type?: string; properties?: Record<string, string> }>; continuationToken?: string | null };
    let planTask: { type?: string; properties?: Record<string, string> } | undefined;
    let token: string | null | undefined;
    const MAX_TASK_PAGES = 100;
    let pages = 0;
    do {
      const url = token ? `${ENDPOINTS.TASKS}?continuationToken=${encodeURIComponent(token)}` : ENDPOINTS.TASKS;
      const page = await restClient.get<TaskPage>(url);
      planTask = page?.items?.find((t) => t.type === PLAN_RECONCILE_TYPE_ID);
      token = page?.continuationToken;
      pages += 1;
    } while (!planTask && token && pages < MAX_TASK_PAGES);
    return planTask?.properties ? deriveExecutePlanProperties(planTask.properties, new Date()) : null;
  }
  catch (err) {
    console.warn('Failed to pre-fetch Data Repair Plan task properties:', err);
    return null;
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
      // Plan task derived props pre-fetched during create-mode load (null in edit mode).
      // Stored in context (not a closure variable) so changeTaskType always reads a
      // consistent snapshot of machine state.
      prefetchedPlanTaskProps: null as Record<string, string> | null,
    },
    actions: {
      validate: assign((ctx: TaskMachineContext) => ({
        validationErrors: validateTask(ctx.data, ctx.selectedTaskType),
      })),
      // Custom action: update task type, reset dynamic properties, and select type metadata
      changeTaskType: assign((context: any, event: any) => {
        const typeId = event.value;
        const taskType = (context.taskTypes as TaskType[]).find((t) => t.id === typeId) ?? null;

        // Initialize properties with defaults from the selected task type's form fields.
        // Hidden fields (e.g. isNameTemplate `name` on Data Repair Plan) default the task NAME,
        // not a persisted property, and must be skipped here so they don't leak into properties.
        const properties: Record<string, string> = {};
        taskType?.formFields?.forEach((field: FormField) => {
          if (resolveTaskFieldMeta(typeId, field.id)?.hidden) return;
          if (field.initialValue !== null && field.initialValue !== undefined) {
            properties[field.id] = String(field.initialValue);
          }
        });
        // Absent checkbox fields default to 'false' — matches Classic/ExtJS parity
        const normalizedProperties = normalizeCheckboxProperties(properties, taskType?.formFields, typeId);

        // In create mode the load() service pre-fetches the Plan task so the read-only
        // Blob store / Repository / date fields are populated immediately on type selection,
        // mirroring the edit flow's deriveExecutePlanProperties call (mirrors Classic behaviour).
        const cachedPlanProps = (context as any).prefetchedPlanTaskProps as Record<string, string> | null;
        const effectiveProperties =
          typeId === EXECUTE_RECONCILE_PLAN_TYPE_ID && cachedPlanProps
            ? { ...normalizedProperties, ...cachedPlanProps }
            : normalizedProperties;

        // Some descriptors (e.g. Data Repair Plan) declare a hidden `name` TemplateFormField that
        // defaults the task NAME, not a property. The field is filtered out of formFields (hidden),
        // and the template's display name equals that default, so when such a task is selected and
        // the user hasn't named the task yet, prefill the name from the template name.
        const hasNameTemplate = !!(
          typeId &&
          TASK_TYPE_FIELD_OVERRIDES[typeId] &&
          Object.values(TASK_TYPE_FIELD_OVERRIDES[typeId]).some((m) => m.isNameTemplate)
        );
        const resolvedName =
          hasNameTemplate && !context.data.name?.trim() && taskType?.name
            ? taskType.name
            : context.data.name;

        const newData = { ...context.data, typeId, name: resolvedName, properties: effectiveProperties };
        return {
          data: newData,
          // Reset pristine baseline so type selection is NOT treated as a user edit
          pristineData: { ...newData },
          touched: { ...context.touched, typeId: true },
          selectedTaskType: taskType,
        };
      }),
      // Custom action: update schedule type and reset schedule-specific fields.
      changeSchedule: assign((context: any, event: any) => {
        const schedule = event.value as ScheduleType;
        const scheduleData = event.data as ScheduleData | undefined;
        const isTimeBased = ['once', 'hourly', 'daily', 'weekly', 'monthly'].includes(schedule);

        const carriedStartDate = scheduleData?.startDate !== undefined
          ? scheduleData.startDate
          : (context.data.startDate as Date | null | undefined);
        const resolvedStartDate: Date | null = isTimeBased
          ? (carriedStartDate ? new Date(carriedStartDate as any) : new Date())
          : null;

        return {
          data: {
            ...context.data,
            schedule,
            startDate: resolvedStartDate,
            startTime: scheduleData?.startTime ?? (
              isTimeBased
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
        let prefetchedPlanTaskProps: Record<string, string> | null = null;

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

        // The REST GET response carries `type` (the type ID) but not the human-readable
        // type label, so fetchTask sets typeName to the ID as a placeholder. Once the
        // matching task type is resolved, swap in its `name` so the header and Summary
        // tab show "Admin - Cleanup repositories" instead of "repository.cleanup",
        // matching the classic UI's behaviour.
        const enrichedTask = task && selectedTaskType
          ? { ...task, typeName: selectedTaskType.name }
          : task;

        // Per-task quirks for the cloud blob-store removal task (see header note).
        const isCloudBlobstoreRemoval =
          enrichedTask?.typeId === CLOUD_BLOBSTORE_REMOVAL_TYPE_ID;

        // Quirk 1: the persisted blobstoreName points at a now-deleted blob store,
        // so clear it for display. The backend keeps the original value, which is
        // what the manual Run consumes.
        let baseProperties: Record<string, string> = enrichedTask
          ? (isCloudBlobstoreRemoval
              ? { ...enrichedTask.properties, blobstoreName: '' }
              : { ...enrichedTask.properties })
          : {};

        // Quirk 2: the once-trigger that the backend created at repo-delete time
        // sometimes lands back without `startDate` while `nextRun` is correct, so
        // surface nextRun in the Schedule tab so it matches the History tab.
        const effectiveStartDate = enrichedTask
          ? (enrichedTask.startDate
              ?? (isCloudBlobstoreRemoval ? enrichedTask.nextRun : null))
          : null;

        // The Execute Data Repair Plan task stores only `planIds`; its displayed blob store /
        // repository / scope / dates are sourced from the sibling Data Repair Plan task (mirrors the
        // backend TaskComponent.replaceDataRepairProperties, which the REST `/v1/tasks` contract does
        // not perform — so without this the read-only fields would render empty).
        if (enrichedTask?.typeId === EXECUTE_RECONCILE_PLAN_TYPE_ID) {
          // Edit mode: apply derived Plan task properties to the loaded baseProperties.
          const derived = await fetchPlanTaskProps();
          if (derived) {
            baseProperties = { ...baseProperties, ...derived };
          }
        }
        else if (!enrichedTask) {
          // Create mode: pre-fetch Plan task properties now (before the user selects a type)
          // so changeTaskType can apply them synchronously when EXECUTE_RECONCILE_PLAN_TYPE_ID
          // is selected. The editing state only activates after load() completes, so this data
          // is always available by the time the user can make a type selection.
          // Note: this fires for ALL new-task loads, not just Execute task creation. On most
          // installs the Plan task is on page 1 — cost is 1 extra GET /v1/tasks call at
          // create-form open time. If the Execute type is not selected, the result is discarded.
          prefetchedPlanTaskProps = await fetchPlanTaskProps();
        }

        // Build initial form data
        const initialData: TaskMachineFormData = enrichedTask
          ? {
              id: enrichedTask.id,
              enabled: enrichedTask.enabled,
              name: enrichedTask.name,
              typeId: enrichedTask.typeId,
              alertEmail: enrichedTask.alertEmail || '',
              notificationCondition: enrichedTask.notificationCondition || 'FAILURE',
              properties: normalizeSerializedEmpties(
                normalizeCheckboxProperties(
                  baseProperties,
                  selectedTaskType?.formFields,
                  selectedTaskType?.id
                ),
                selectedTaskType?.formFields,
                selectedTaskType?.id
              ),
              schedule: enrichedTask.schedule || 'manual',
              startDate: effectiveStartDate ?? null,
              startTime: effectiveStartDate
                ? (() => {
                    const d = new Date(typeof effectiveStartDate === 'string' ? effectiveStartDate : effectiveStartDate.getTime());
                    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                  })()
                : '00:00',
              recurringDays: enrichedTask.recurringDays || [],
              cronExpression: enrichedTask.cronExpression || '',
              timeZoneOffset: enrichedTask.timeZoneOffset || '',
            }
          : {
              ...DEFAULT_TASK_FORM_DATA,
              startTime: '00:00',
            };

        return {
          data: initialData,
          task: enrichedTask,
          taskTypes,
          selectedTaskType,
          prefetchedPlanTaskProps,
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
