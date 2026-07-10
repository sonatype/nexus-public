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
import { TASK_FIELD_UI } from './taskFieldMetadata';
import {
  humanizePropertyKey,
  restTemplateToTaskType,
  RestTaskTemplate,
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
  message?: string;
  currentState: 'WAITING' | 'RUNNING' | 'BLOCKED' | 'DONE' | 'CANCELED' | 'FAILED';
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
 * Strict shape for POST/PUT bodies — narrows the canonical `RestTaskTemplate` so
 * `frequency`/`notificationCondition` are well-typed when we build the request.
 * GET responses keep using `RestTaskTemplate` from taskTransformers (loose since
 * the transformer doesn't read frequency).
 */
type RestTaskCreatePayload = Omit<RestTaskTemplate, 'frequency' | 'notificationCondition'> & {
  frequency: RestFrequency;
  notificationCondition: 'FAILURE' | 'SUCCESS_FAILURE';
};

// =============================================================================
// TRANSFORMERS
// =============================================================================

/**
 * Map REST currentState to UI status
 */
function mapRestStateToStatus(state: string): Task['status'] {
  const stateUpper = (state || '').toUpperCase() as RestTask['currentState'];
  const stateMap: Record<RestTask['currentState'], Task['status']> = {
    WAITING: 'WAITING',
    RUNNING: 'RUNNING',
    BLOCKED: 'BLOCKED',
    DONE: 'OK',
    CANCELED: 'CANCELED',
    FAILED: 'FAILED',
  };
  return stateMap[stateUpper] || 'WAITING';
}

/**
 * Transform REST Task to UI Task shape
 */
function restToTask(rest: RestTask): Task {
  return {
    id: rest.id,
    enabled: rest.enabled !== false,
    name: rest.name,
    typeId: rest.type,
    typeName: rest.type,
    status: mapRestStateToStatus(rest.currentState),
    statusDescription: rest.message || '',
    nextRun: rest.nextRun ? new Date(rest.nextRun) : null,
    lastRun: rest.lastRun ? new Date(rest.lastRun) : null,
    lastRunResult: rest.lastRunResult || null,
    runnable: rest.currentState !== 'RUNNING',
    stoppable: rest.currentState === 'RUNNING',
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
 * Both `humanizePropertyKey` and `restTemplateToTaskType` now live in
 * `taskTransformers.ts` and are imported above. They were extracted so the
 * XState form machine can reuse them without `jest.mock('../useTasksApi')`
 * stubbing them out in tests (which broke the EDIT flow's formFields
 * enrichment).
 */

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
 * Convert UI task form data to REST TaskTemplateXO format for create
 */
function toRestTaskCreate(
  data: TaskFormData,
  startTime?: string
): RestTaskCreatePayload {
  // Pass properties through, but strip any known-hidden fields (server-managed internals
  // like moveInitialBlobstore that the backend rejects when sent via PUT/POST).
  const properties: Record<string, string> = Object.fromEntries(
    Object.entries(data.properties || {})
      .filter(([key]) => !TASK_FIELD_UI[key]?.hidden)
      .map(([key, value]) => {
        if (TASK_FIELD_UI[key]?.type === 'checkbox') {
          return [key, value === 'true' ? 'true' : 'false'];
        }
        return [key, value];
      })
  );

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
  // Pass properties through, but strip any known-hidden fields (server-managed internals
  // like moveInitialBlobstore that the backend rejects when sent via PUT/POST).
  const properties: Record<string, string> = Object.fromEntries(
    Object.entries(data.properties || {})
      .filter(([key]) => !TASK_FIELD_UI[key]?.hidden)
      .map(([key, value]) => {
        if (TASK_FIELD_UI[key]?.type === 'checkbox') {
          return [key, value === 'true' ? 'true' : 'false'];
        }
        return [key, value];
      })
  );

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
  }, []);

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
  }, []);

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
  }, []);

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
  }, []);

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
  }, []);

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
