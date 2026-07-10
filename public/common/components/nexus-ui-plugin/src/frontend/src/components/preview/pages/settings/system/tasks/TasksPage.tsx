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

import React, { useState, useEffect, useCallback } from 'react';
import { Box } from '@radix-ui/themes';
import { Plus } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton, SettingsAlert } from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { TasksList } from './TasksList';
import { TaskDetail } from './TaskDetail';
import { TaskForm } from './TaskForm';
import { useTasksApi } from './useTasksApi';
import { Task, TaskType, TaskFormData, TasksPageProps } from './types';

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

  // Handle run
  const handleRun = useCallback(async () => {
    if (!task) return;
    
    try {
      await runTask(task.id);
      setSuccessMessage(`Task started: ${task.name}`);
      // Refresh to update status
      const updatedTask = await fetchTask(task.id);
      if (updatedTask) setTask(updatedTask);
    } catch (err) {
      // Error is set by the API hook
    }
  }, [task, runTask, fetchTask]);

  // Handle stop
  const handleStop = useCallback(async () => {
    if (!task) return;
    
    try {
      await stopTask(task.id);
      setSuccessMessage(`Task stopped: ${task.name}`);
      // Refresh to update status
      const updatedTask = await fetchTask(task.id);
      if (updatedTask) setTask(updatedTask);
    } catch (err) {
      // Error is set by the API hook
    }
  }, [task, stopTask, fetchTask]);

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

        {viewMode === 'detail' && (
          <TaskDetail
            task={task}
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

