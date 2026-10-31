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

/**
 * Tasks API Hook
 *
 * MIGRATION STATUS: 100% REST
 * - fetchTasks: ✅ REST (GET /v1/tasks)
 * - fetchTask: ✅ REST (GET /v1/tasks/{id})
 * - runTask: ✅ REST (POST /v1/tasks/{id}/run)
 * - stopTask: ✅ REST (POST /v1/tasks/{id}/stop)
 * - fetchTaskTypes: ✅ REST (GET /v1/tasks/templates)
 * - createTask: ✅ REST (POST /v1/tasks)
 * - updateTask: ✅ REST (PUT /v1/tasks/{id})
 * - deleteTask: ✅ REST (DELETE /v1/tasks/{id})
 */

import { useState, useCallback, useRef, useEffect } from 'react';
import { restClient, parseApiError, urlBuilder } from '../../../../../../interface/api';
import { resolveTaskFieldMeta, ALL_BLOB_STORES } from './taskFieldMetadata';
import {
  humanizePropertyKey,
  mapRestStateToStatus,
  restTemplateToTaskType,
  type RestTaskTemplate,
} from './taskTransformers';
import {
  Task,
  TaskType,
  TaskFormData,
  getTimezoneOffset,
  combineDateAndTime,
} from './types';

// Re-export from the shared transformer module so existing imports still resolve.
export { humanizePropertyKey };

// =============================================================================
// REST API RESPONSE TYPES
// =============================================================================

/**
 * REST API Task shape (from TaskXO.java)
 * All schedule and configuration fields are returned flat (not nested under "frequency").
 */
interface RestTask {
  id: string;
  enabled: boolean;
  name: string;
  type: string;
  typeName?: string;
  message?: string;
  // Raw `TaskState` enum value from the backend — one of WAITING,
  // RUNNING_STARTING / RUNNING / RUNNING_BLOCKED / RUNNING_CANCELED, or the
  // DONE-group OK / FAILED / CANCELED / INTERRUPTED. When a task reports
  // progress it arrives suffixed, e.g. "RUNNING: 42 of 100" (see TaskXO.java),
  // so this is intentionally a free-form string normalized by the transform.
  currentState: string;
  lastRunResult?: 'OK' | 'FAILED' | 'CANCELED' | 'INTERRUPTED' | null;
  nextRun?: string | null;
  lastRun?: string | null;
  schedule?: string | null;
  // Configuration fields present on GET /v1/tasks/{id}
  properties?: Record<string, string> | null;
  alertEmail?: string | null;
  notificationCondition?: string | null;
  startDate?: string | null;
  recurringDays?: number[] | null;
  cronExpression?: string | null;
  timeZoneOffset?: string | null;
}

/**
 * REST API paginated response
 */
interface RestTasksResponse {
  items: RestTask[];
  continuationToken?: string | null;
}

/**
 * REST API Frequency shape (from FrequencyXO.java)
 */
interface RestFrequency {
  schedule: 'manual' | 'once' | 'hourly' | 'daily' | 'weekly' | 'monthly' | 'cron';
  startDate?: number | null; // Unix timestamp
  timeZoneOffset?: string | null;
  recurringDays?: number[] | null;
  cronExpression?: string | null;
}

/**
 * Strict shape for POST/PUT bodies — narrows RestTaskTemplate so
 * frequency/notificationCondition are well-typed when building requests.
 */
type RestTaskCreatePayload = Omit<RestTaskTemplate, 'frequency' | 'notificationCondition' | 'formFields'> & {
  frequency: RestFrequency;
  notificationCondition: 'FAILURE' | 'SUCCESS_FAILURE';
};

// =============================================================================
// TRANSFORMERS
// =============================================================================

/**
 * `mapRestStateToStatus` now lives in `taskTransformers.ts` (imported above) so
 * the initial-load/polling path and the form machine's own fetch share ONE
 * normalization — they can never disagree about a task's status (NEXUS-53525).
 */

/**
 * Transform REST Task to UI Task shape
 */
function restToTask(rest: RestTask): Task {
  const status = mapRestStateToStatus(rest.currentState);
  // A task is stoppable while it is in the running group and runnable otherwise.
  // Derive both from the normalized status so a progress suffix or a running
  // sub-state (e.g. "RUNNING: 42%", RUNNING_STARTING) is handled correctly —
  // a raw `currentState === 'RUNNING'` check missed those.
  const isRunning = status === 'RUNNING';
  return {
    id: rest.id,
    enabled: rest.enabled !== false,
    name: rest.name,
    typeId: rest.type,
    typeName: rest.typeName || rest.type,
    status,
    statusDescription: rest.message || '',
    nextRun: rest.nextRun ? new Date(rest.nextRun) : null,
    lastRun: rest.lastRun ? new Date(rest.lastRun) : null,
    lastRunResult: rest.lastRunResult || null,
    runnable: !isRunning,
    stoppable: isRunning,
    properties: rest.properties || {},
    alertEmail: rest.alertEmail || '',
    notificationCondition: (rest.notificationCondition as Task['notificationCondition']) || 'FAILURE',
    schedule: rest.schedule === 'cron' ? 'advanced' : ((rest.schedule as Task['schedule']) || 'manual'),
    startDate: rest.startDate ? new Date(rest.startDate) : null,
    recurringDays: rest.recurringDays || [],
    cronExpression: rest.cronExpression || '',
    timeZoneOffset: rest.timeZoneOffset || '',
  };
}

/**
 * Convert UI task form data to REST FrequencyXO format
 */
function toRestFrequency(
  schedule: Task['schedule'],
  startDate: Date | null,
  recurringDays?: number[],
  cronExpression?: string,
  timeZoneOffset?: string
): RestFrequency {
  const apiSchedule = schedule === 'advanced' ? 'cron' : (schedule || 'manual');
  const frequency: RestFrequency = {
    schedule: apiSchedule,
  };

  if (apiSchedule !== 'manual' && startDate) {
    frequency.startDate = startDate.getTime();
    frequency.timeZoneOffset = timeZoneOffset || getTimezoneOffset();
  }

  if ((apiSchedule === 'weekly' || apiSchedule === 'monthly') && recurringDays?.length) {
    frequency.recurringDays = recurringDays;
  }

  if (apiSchedule === 'cron' && cronExpression) {
    frequency.cronExpression = cronExpression;
  }

  return frequency;
}

/**
 * Build the REST `properties` map from the form's dynamic property values.
 *
 * Field metadata is resolved per task type (resolveTaskFieldMeta) so the same key can carry
 * different semantics across tasks. Fields are dropped when they are:
 *  - hidden (server-managed internals like moveInitialBlobstore, or the Data Repair Plan `name`
 *    template / cloud staticInfo) — the backend rejects unknown/internal keys on PUT/POST;
 *  - alert banners (display-only, carry no value);
 *  - the inactive side of a `taskScope` toggle (only the active timespan persists — Classic
 *    parity; the backend would otherwise see both duration and date inputs).
 * Checkbox values are coerced to 'true'/'false'. Blank values are preserved (e.g. all-blank
 * duration fields stay empty so the backend completeConfiguration default for sinceMinutes runs).
 * Fields without a TASK_FIELD_UI entry pass through unchanged, allowing custom/undocumented
 * fields to round-trip (add a metadata entry to control visibility/serialization behavior).
 */
function serializeProperties(
  data: TaskFormData,
  options?: { isUpdate?: boolean }
): Record<string, string> {
  const typeId = data.typeId;
  const activeScope = data.properties?.taskScope;
  const isUpdate = options?.isUpdate ?? false;
  const isClearedSelector = (meta: ReturnType<typeof resolveTaskFieldMeta>, value: string) =>
    !!meta?.omitWhenEmpty && (value === '' || value === ALL_BLOB_STORES);
  return Object.fromEntries(
    Object.entries(data.properties || {})
      .filter(([key, value]) => {
        const meta = resolveTaskFieldMeta(typeId, key);
        if (meta?.hidden) return false;
        if (meta?.neverSerialize) return false;
        if (meta?.type === 'alertBanner' || meta?.type === 'staticInfo' || meta?.type === 'planInformation') return false;
        if (meta?.scope && activeScope && meta.scope !== activeScope) return false;
        // "Empty = all" selectors (Data Repair Plan blob store/repository) with no explicit value:
        //  - CREATE: omit entirely — Classic does not persist these for the implicit all-state.
        //  - UPDATE: keep, sent as '' below. PUT is a merge (the backend overlays the payload onto
        //    the existing config), so omitting a *cleared* field would silently retain its old
        //    value; an explicit '' overwrites it so the cleared selection actually persists.
        if (isClearedSelector(meta, value)) return isUpdate;
        return true;
      })
      .map(([key, value]) => {
        const meta = resolveTaskFieldMeta(typeId, key);
        if (meta?.type === 'checkbox') {
          return [key, value === 'true' ? 'true' : 'false'];
        }
        // On update, a cleared "empty = all" selector is sent as '' to overwrite the old value.
        if (isUpdate && isClearedSelector(meta, value)) {
          return [key, ''];
        }
        // Classic serializes an empty duration field as the literal string "null" — match it.
        if (meta?.serializeEmptyAs !== undefined && (value === '' || value === null || value === undefined)) {
          return [key, meta.serializeEmptyAs];
        }
        return [key, value];
      })
  );
}

/**
 * Convert UI task form data to REST TaskTemplateXO format for create
 */
function toRestTaskCreate(
  data: TaskFormData,
  startTime?: string
): RestTaskCreatePayload {
  const properties = serializeProperties(data);

  // Combine date and time
  let startDate = data.startDate;
  if (startDate && startTime) {
    startDate = combineDateAndTime(startDate, startTime);
  }

  return {
    type: data.typeId,
    name: data.name,
    enabled: data.enabled,
    alertEmail: data.alertEmail || null,
    notificationCondition: (data.notificationCondition || 'FAILURE') as RestTaskCreatePayload['notificationCondition'],
    frequency: toRestFrequency(
      data.schedule,
      startDate,
      data.recurringDays,
      data.cronExpression,
      data.timeZoneOffset
    ),
    properties,
  };
}

/**
 * Convert UI task form data to REST TaskTemplateXO format for update
 * Note: type field is not allowed on update
 */
function toRestTaskUpdate(
  data: TaskFormData,
  startTime?: string
): Omit<RestTaskCreatePayload, 'type'> {
  const properties = serializeProperties(data, { isUpdate: true });

  // Combine date and time
  let startDate = data.startDate;
  if (startDate && startTime) {
    startDate = combineDateAndTime(startDate, startTime);
  }

  return {
    name: data.name,
    enabled: data.enabled,
    alertEmail: data.alertEmail || null,
    notificationCondition: (data.notificationCondition || 'FAILURE') as RestTaskCreatePayload['notificationCondition'],
    frequency: toRestFrequency(
      data.schedule,
      startDate,
      data.recurringDays,
      data.cronExpression,
      data.timeZoneOffset
    ),
    properties,
  };
}

// =============================================================================
// HOOK
// =============================================================================

/**
 * Custom hook for Tasks API operations
 */
export function useTasksApi() {
  const [loading, setLoadingRaw] = useState(false);
  const [error, setErrorRaw] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => { isMountedRef.current = false; };
  }, []);

  const setLoading = useCallback((val: boolean) => {
    if (isMountedRef.current) setLoadingRaw(val);
  }, []);

  const setError = useCallback((val: string | null) => {
    if (isMountedRef.current) setErrorRaw(val);
  }, []);

  /**
   * Fetch all tasks using REST API
   */
  const fetchTasks = useCallback(async (): Promise<Task[]> => {
    try {
      const url = urlBuilder.tasks.list();
      const response = await restClient.get<RestTasksResponse>(url);
      return Array.isArray(response.items) ? response.items.map(restToTask) : [];
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch tasks:', err);
      throw new Error(apiError.message || 'Failed to load tasks');
    }
  }, []);

  /**
   * Fetch a single task by ID using REST API
   */
  const fetchTask = useCallback(async (taskId: string): Promise<Task | null> => {
    try {
      const url = urlBuilder.tasks.get(taskId);
      const response = await restClient.get<RestTask>(url);
      return restToTask(response);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // Return null for 404 (not found)
      if (apiError.status === 404) {
        return null;
      }
      console.error('Failed to fetch task:', err);
      throw new Error(apiError.message || 'Failed to load task details');
    }
  }, []);

  /**
   * Fetch available task types using REST API
   */
  const fetchTaskTypes = useCallback(async (): Promise<TaskType[]> => {
    try {
      const url = urlBuilder.tasks.templates();
      const templates = await restClient.get<RestTaskTemplate[]>(url);
      return Array.isArray(templates) ? templates.map(restTemplateToTaskType) : [];
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch task types:', err);
      throw new Error(apiError.message || 'Failed to load task types');
    }
  }, []);

  /**
   * Create a new task using REST API
   */
  const createTask = useCallback(async (
    data: TaskFormData,
    startTime?: string
  ): Promise<Task> => {
    setLoading(true);
    setError(null);

    try {
      const payload = toRestTaskCreate(data, startTime);
      const url = urlBuilder.tasks.create();
      const response = await restClient.post<RestTask>(url, payload);
      return restToTask(response);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to create task');
    } finally {
      setLoading(false);
    }
  }, [setError, setLoading]);

  /**
   * Update an existing task using REST API
   */
  const updateTask = useCallback(async (
    taskId: string,
    data: TaskFormData,
    startTime?: string
  ): Promise<Task> => {
    setLoading(true);
    setError(null);

    try {
      const payload = toRestTaskUpdate(data, startTime);
      const url = urlBuilder.tasks.update(taskId);
      await restClient.put(url, payload);
      // Fetch the updated task to return
      const updated = await restClient.get<RestTask>(urlBuilder.tasks.get(taskId));
      return restToTask(updated);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to update task');
    } finally {
      setLoading(false);
    }
  }, [setError, setLoading]);

  /**
   * Delete a task using REST API
   */
  const deleteTask = useCallback(async (taskId: string): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      const url = urlBuilder.tasks.delete(taskId);
      await restClient.delete(url);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to delete task');
    } finally {
      setLoading(false);
    }
  }, [setError, setLoading]);

  /**
   * Run a task immediately using REST API
   */
  const runTask = useCallback(async (taskId: string): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      const url = urlBuilder.tasks.run(taskId);
      await restClient.post(url);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to run task');
    } finally {
      setLoading(false);
    }
  }, [setError, setLoading]);

  /**
   * Stop a running task using REST API
   */
  const stopTask = useCallback(async (taskId: string): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      const url = urlBuilder.tasks.stop(taskId);
      await restClient.post(url);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to stop task');
    } finally {
      setLoading(false);
    }
  }, [setError, setLoading]);

  return {
    loading,
    error,
    setError,
    fetchTasks,
    fetchTask,
    fetchTaskTypes,
    createTask,
    updateTask,
    deleteTask,
    runTask,
    stopTask,
  };
}

export default useTasksApi;
