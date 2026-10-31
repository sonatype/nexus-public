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

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Box } from '@radix-ui/themes';
import { Plus } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton, SettingsAlert } from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { TasksList } from './TasksList';
import { TaskDetail } from './TaskDetail';
import { TaskForm } from './TaskForm';
import { useTasksApi } from './useTasksApi';
import { usePolling, POLL_INTERVAL_MS, POST_ACTION_POLL_COUNT } from './usePolling';
import { Task, TaskType, TaskFormData, TasksPageProps, isActiveStatus, isTerminalStatus } from './types';

import './TasksPage.scss';

type ViewMode = 'list' | 'create' | 'detail';

const BASE_PATH = 'preview/admin/system/tasks';

// Permissions
const Permissions = {
  READ: 'nexus:tasks:read',
  CREATE: 'nexus:tasks:create',
  UPDATE: 'nexus:tasks:update',
  DELETE: 'nexus:tasks:delete',
  RUN: 'nexus:tasks:start',
  STOP: 'nexus:tasks:stop',
};

/**
 * Parse the URL hash to determine view mode and parameters.
 *
 * URL patterns:
 * - #preview/admin/system/tasks              → list
 * - #preview/admin/system/tasks/create       → create (type selector)
 * - #preview/admin/system/tasks/create/{typeId} → create with pre-selected type
 * - #preview/admin/system/tasks/{taskId}     → detail/edit
 */
function parseTasksRoute(hash: string): { viewMode: ViewMode; taskId: string | null; typeId: string | null } {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');
  const tasksIndex = parts.indexOf('tasks');
  if (tasksIndex === -1) return { viewMode: 'list', taskId: null, typeId: null };

  const after = parts.slice(tasksIndex + 1).filter(Boolean);
  if (after.length === 0) return { viewMode: 'list', taskId: null, typeId: null };
  if (after[0] === 'create') {
    const typeId = after[1] ? decodeURIComponent(after[1]) : null;
    return { viewMode: 'create', taskId: null, typeId };
  }
  return { viewMode: 'detail', taskId: decodeURIComponent(after[0]), typeId: null };
}

function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * TasksPage - Main Tasks management page for Preview UI
 *
 * URL-based routing:
 * - /tasks                     → Task list
 * - /tasks/create              → Type selector
 * - /tasks/create/{typeId}     → Create form with pre-selected type
 * - /tasks/{taskId}            → View/edit existing task
 */
export function TasksPage({ className }: TasksPageProps) {
  const [routeState, setRouteState] = useState(() => parseTasksRoute(window.location.hash));
  const viewMode = routeState.viewMode;
  const selectedTaskId = routeState.taskId;
  const preSelectedTypeId = routeState.typeId;

  const [task, setTask] = useState<Task | null>(null);
  const [taskTypes, setTaskTypes] = useState<TaskType[]>([]);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  // Live status polling state (NEXUS-53525).
  //   liveTask        – latest polled snapshot, kept SEPARATE from `task` (the
  //                     form seed) so a poll never recreates the detail form
  //                     machine or clobbers an in-flight edit.
  //   taskNotFound    – set when a poll 404s (task deleted server-side); stops
  //                     the loop and degrades to a not-found state.
  //   detailPollsLeft – remaining polls in the post-action window (see
  //                     POST_ACTION_POLL_COUNT) that bridges WAITING→RUNNING.
  const [liveTask, setLiveTask] = useState<Task | null>(null);
  const [taskNotFound, setTaskNotFound] = useState(false);
  const [detailPollsLeft, setDetailPollsLeft] = useState(0);

  // Mirror selectedTaskId into a ref so an in-flight poll can detect that the
  // route moved on before its response landed (stale-response protection).
  const selectedTaskIdRef = useRef<string | null>(selectedTaskId);
  selectedTaskIdRef.current = selectedTaskId;

  // Listen for hash changes (browser back/forward)
  useEffect(() => {
    const handleHashChange = () => setRouteState(parseTasksRoute(window.location.hash));
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  const {
    loading,
    error,
    setError,
    fetchTask,
    fetchTaskTypes,
    runTask,
    stopTask,
  } = useTasksApi();

  // Check permissions
  const canCreate = ExtJS.checkPermission(Permissions.CREATE);
  const canUpdate = ExtJS.checkPermission(Permissions.UPDATE);
  const canDelete = ExtJS.checkPermission(Permissions.DELETE);
  const canRun = ExtJS.checkPermission(Permissions.RUN);
  const canStop = ExtJS.checkPermission(Permissions.STOP);

  // Load task types on mount
  useEffect(() => {
    const loadTaskTypes = async () => {
      try {
        const types = await fetchTaskTypes();
        setTaskTypes(types);
      } catch (err) {
        console.error('Failed to load task types:', err);
      }
    };
    loadTaskTypes();
  }, [fetchTaskTypes]);

  // Reset the live-status slices whenever the selected task changes so a new
  // detail page never shows the previous task's polled status while loading.
  useEffect(() => {
    setLiveTask(null);
    setTaskNotFound(false);
    setDetailPollsLeft(0);
  }, [selectedTaskId]);

  // Load task details when selected
  useEffect(() => {
    if (selectedTaskId && viewMode === 'detail') {
      const loadTask = async () => {
        try {
          const taskData = await fetchTask(selectedTaskId);
          if (taskData) {
            setTask(taskData);
          } else {
            setError('Task not found');
            navigateTo(BASE_PATH);
          }
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Failed to load task');
          navigateTo(BASE_PATH);
        }
      };
      loadTask();
    }
  }, [selectedTaskId, viewMode, fetchTask, setError]);

  // --- Live status polling for the detail route (NEXUS-53525) -----------------
  // The status that drives the gate is the freshest one we have: a polled
  // snapshot if present, otherwise the loaded seed.
  const detailStatus = (liveTask && liveTask.id === selectedTaskId)
    ? liveTask.status
    : task?.status;

  // Poll while on the detail route for a task that is either actively working
  // (RUNNING/BLOCKED) or inside the post-action window. A terminal status, a
  // 404, or leaving the detail route all stop the loop — so an idle WAITING
  // task that the user merely views is not polled.
  const detailPollingEnabled =
    viewMode === 'detail' &&
    !!selectedTaskId &&
    !taskNotFound &&
    !isTerminalStatus(detailStatus) &&
    (isActiveStatus(detailStatus) || detailPollsLeft > 0);

  const pollDetailStatus = useCallback(async () => {
    const id = selectedTaskIdRef.current;
    if (!id) {
      return;
    }
    const polled = await fetchTask(id); // non-404 errors propagate and are swallowed by usePolling
    // Ignore a response that resolved after we navigated to a different task.
    if (selectedTaskIdRef.current !== id) {
      return;
    }
    if (polled === null) {
      // 404: the task was deleted server-side. Degrade to a not-found state
      // (this also flips detailPollingEnabled to false, ending the loop).
      setTaskNotFound(true);
      return;
    }
    setLiveTask(polled);
    // Burn down the post-action window only while the task is NOT actively running.
    // Once it is RUNNING/BLOCKED we reset the window to 0 on purpose: from that point
    // `isActiveStatus` alone keeps the interval alive (see `detailPollingEnabled`), so
    // the window's only job is to bridge the brief gap before the server flips to
    // RUNNING. (A second Run on an already-active task re-arms the window in handleRun,
    // but the next active poll zeroes it again — harmless, since active status is what
    // drives polling then.)
    setDetailPollsLeft((remaining) => (isActiveStatus(polled.status) ? 0 : Math.max(0, remaining - 1)));
  }, [fetchTask]);

  // pollOnEnable is false: the seed loaded on mount is already fresh, and Run/Stop
  // trigger their own immediate pollNow(), so there is never a duplicate fetch.
  const { pollNow: pollDetailNow } = usePolling(pollDetailStatus, {
    intervalMs: POLL_INTERVAL_MS,
    enabled: detailPollingEnabled,
    pollOnEnable: false,
  });

  // Clear success message after delay
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  // Handle task selection from list
  const handleSelectTask = useCallback((taskId: string) => {
    setTask(null);
    setError(null);
    navigateTo(`${BASE_PATH}/${encodeURIComponent(taskId)}`);
  }, [setError]);

  // Handle create button click
  const handleCreate = useCallback(() => {
    setTask(null);
    setError(null);
    navigateTo(`${BASE_PATH}/create`);
  }, [setError]);

  // Handle create with specific type (for direct URL navigation)
  const handleCreateWithType = useCallback((typeId: string) => {
    navigateTo(`${BASE_PATH}/create/${encodeURIComponent(typeId)}`);
  }, []);

  // Handle back to list
  const handleBack = useCallback(() => {
    setTask(null);
    setError(null);
    navigateTo(BASE_PATH);
  }, [setError]);

  // Handle save (create or update) — UI side-effects only.
  // The actual REST call runs inside useTasksForm via the createTask/updateTask
  // props passed by TaskDetail/TaskForm; this callback is invoked AFTER that
  // save has already succeeded, so it must not re-issue the request.
  const handleSave = useCallback(async (_data: TaskFormData, _startTime?: string) => {
    setSuccessMessage(viewMode === 'create' ? 'Task created successfully' : 'Task updated successfully');
    setRefreshKey((k) => k + 1);
    navigateTo(BASE_PATH);
  }, [viewMode]);

  // Handle delete — UI side-effects only.
  // Mirrors handleSave: the REST DELETE runs once inside useTasksForm via the deleteTask
  // prop wired in TaskDetail. Re-issuing it here would produce a second 404'd call.
  const handleDelete = useCallback(async () => {
    if (!task) return;
    setSuccessMessage(`Task "${task.name}" deleted successfully`);
    setRefreshKey((k) => k + 1);
    navigateTo(BASE_PATH);
  }, [task]);

  // Handle run / stop — reconciled with polling (NEXUS-53525).
  // Instead of the old one-shot refetch (which raced the server and left the
  // badge stuck at WAITING), we open the post-action window and trigger a single
  // immediate poll. The window + interval then carry the badge through
  // WAITING → RUNNING → terminal. We deliberately do NOT setTask here: status
  // flows via the separate liveTask slice so the form seed stays stable.
  const handleRun = useCallback(async () => {
    if (!task) return;

    try {
      await runTask(task.id);
      setSuccessMessage(`Task started: ${task.name}`);
      setDetailPollsLeft(POST_ACTION_POLL_COUNT);
      pollDetailNow();
    } catch (err) {
      // Swallowed intentionally: useTasksApi already surfaces the error to the user.
      // We deliberately leave the post-action poll window unopened on failure — if the
      // run/stop didn't start, there is nothing new to poll for. (setDetailPollsLeft +
      // pollDetailNow run only on the success path above.)
    }
  }, [task, runTask, pollDetailNow]);

  const handleStop = useCallback(async () => {
    if (!task) return;

    try {
      await stopTask(task.id);
      setSuccessMessage(`Task stopped: ${task.name}`);
      setDetailPollsLeft(POST_ACTION_POLL_COUNT);
      pollDetailNow();
    } catch (err) {
      // Swallowed intentionally: useTasksApi already surfaces the error to the user.
      // We deliberately leave the post-action poll window unopened on failure — if the
      // run/stop didn't start, there is nothing new to poll for. (setDetailPollsLeft +
      // pollDetailNow run only on the success path above.)
    }
  }, [task, stopTask, pollDetailNow]);

  // Navigation helper for Settings breadcrumb
  const navigateToSettings = () => {
    window.location.hash = '#preview/admin/settings';
  };

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      const breadcrumbs = [
        { label: 'Settings', onClick: navigateToSettings },
        { label: 'Tasks' },
      ];
      const actions = canCreate ? (
        <SettingsButton variant="primary" onClick={handleCreate} icon={Plus} testId="tasks-create-button" data-analytics-id="nxrm-task-create">
          Create Task
        </SettingsButton>
      ) : undefined;
      return (
        <PageHeader
          title="Tasks"
          description="Manage scheduled tasks and background jobs"
          breadcrumbs={breadcrumbs}
          actions={actions}
          className="tasks-page__header"
        />
      );
    }

    const title = viewMode === 'create'
      ? 'Create Task'
      : task
        ? task.name
        : 'Task Details';

    const breadcrumbs = [
      { label: 'Settings', onClick: navigateToSettings },
      { label: 'Tasks', onClick: handleBack },
      { label: viewMode === 'create' ? 'Create' : (task?.name || 'Task') },
    ];

    return (
      <PageHeader
        title={title}
        description={task && viewMode === 'detail' ? task.typeName : undefined}
        breadcrumbs={breadcrumbs}
        className="tasks-page__header"
      />
    );
  };

  return (
    <Box 
      className={`tasks-page ${className || ''}`}
      data-testid="tasks-page"
      data-view={viewMode}
      data-loading={loading ? 'true' : 'false'}
    >
      {renderHeader()}

      {/* Alerts - announced to assistive tech */}
      <Box
        role="status"
        aria-live="polite"
        data-testid="tasks-page-alerts"
      >
        {error && viewMode === 'list' && (
          <Box className="tasks-page__alerts">
            <SettingsAlert type="error" onClose={() => setError(null)}>
              {error}
            </SettingsAlert>
          </Box>
        )}
        {successMessage && (
          <Box className="tasks-page__alerts">
            <SettingsAlert type="success" onClose={() => setSuccessMessage(null)}>
              {successMessage}
            </SettingsAlert>
          </Box>
        )}
      </Box>

      {/* Content */}
      <Box className="tasks-page__content">
        {viewMode === 'list' && (
          <TasksList
            key={refreshKey}
            onSelect={handleSelectTask}
            onCreate={handleCreate}
          />
        )}

        {viewMode === 'create' && (
          <TaskForm
            taskTypes={taskTypes}
            isCreate={true}
            initialTypeId={preSelectedTypeId || undefined}
            onSave={handleSave}
            onCancel={handleBack}
            onTypeChange={handleCreateWithType}
            loading={loading}
            error={error || undefined}
          />
        )}

        {viewMode === 'detail' && taskNotFound && (
          <Box className="tasks-page__alerts">
            <SettingsAlert type="error">
              This task no longer exists. It may have been deleted.
            </SettingsAlert>
            <SettingsButton variant="secondary" onClick={handleBack} testId="task-not-found-back">
              Back to Tasks
            </SettingsButton>
          </Box>
        )}

        {viewMode === 'detail' && !taskNotFound && (
          <TaskDetail
            task={task}
            liveTask={liveTask}
            taskId={selectedTaskId}
            loading={loading && !task}
            canEdit={canUpdate}
            canDelete={canDelete}
            canRun={canRun}
            canStop={canStop}
            onSave={handleSave}
            onDelete={handleDelete}
            onRun={handleRun}
            onStop={handleStop}
            onCancel={handleBack}
            error={error || undefined}
          />
        )}
      </Box>
    </Box>
  );
}

export default TasksPage;

