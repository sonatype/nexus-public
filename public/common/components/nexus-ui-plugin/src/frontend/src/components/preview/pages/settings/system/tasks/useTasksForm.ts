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

import { useMemo } from 'react';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createTaskFormMachine, TaskMachineFormData } from './tasksFormMachine';
import { Task, TaskType, TaskFormData } from './types';

export interface UseTasksFormOptions {
  taskId?: string;
  task?: Task;
  onCancel: () => void;
  createTask: (data: TaskFormData, startTime?: string) => Promise<Task>;
  updateTask: (taskId: string, data: TaskFormData, startTime?: string) => Promise<Task>;
  deleteTask?: (taskId: string) => Promise<void>;
}

export interface UseTasksFormReturn {
  form: ReturnType<typeof useForm>;
  task: Task | null;
  taskTypes: TaskType[];
  selectedTaskType: TaskType | null;
  isCreate: boolean;
}

/**
 * Custom hook for managing TaskForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads both the task being edited
 * (if taskId provided) and reference data (task types with form fields).
 *
 * Task type changes trigger TASK_TYPE_CHANGE events (resets dynamic properties).
 * Schedule changes trigger SCHEDULE_CHANGE events (transitions schedule sub-states).
 */
export function useTasksForm({
  taskId,
  task,
  onCancel,
  createTask,
  updateTask,
  deleteTask,
}: UseTasksFormOptions): UseTasksFormReturn {
  const toast = useToast();
  const isCreate = !(taskId || task);

  // Create the form machine - memoized based on taskId and task
  const machine = useMemo(
    () => createTaskFormMachine(taskId, task),
    [taskId, task]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: TaskMachineFormData; task: Task | null }) => {
        try {
          const taskToUpdate = task || ctx.task;
          const { startTime, ...formData } = ctx.data;

          if (isCreate) {
            await createTask(formData, startTime);
            toast.success(`Task "${ctx.data.name}" created successfully`);
          } else if (taskToUpdate) {
            await updateTask(taskToUpdate.id, formData, startTime);
            toast.success(`Task "${ctx.data.name}" updated successfully`);
          }
          onCancel();
        } catch (err) {
          const rawMessage = err instanceof Error ? err.message : 'Operation failed';
          // Translate the backend's "Property 'X' not found" into a clearer
          // user-facing message. canAdvance should prevent this for normal
          // flows (NEXUS-53357), but this still helps when a new field is
          // added on the backend that the frontend hasn't surfaced yet.
          const friendly = rawMessage.replace(
            /Property '([^']+)' not found/g,
            "Required field '$1' is missing — fill it in and try again",
          );
          toast.error(friendly);
          throw err;
        }
      },
      ...(deleteTask && {
        delete: async (ctx: { data: TaskMachineFormData; task: Task | null }) => {
          const taskToDelete = task || ctx.task;
          if (taskToDelete) {
            await deleteTask(taskToDelete.id);
            toast.success(`Task "${taskToDelete.name}" deleted successfully`);
            onCancel();
          }
        },
      }),
    },
  });

  // Access the raw state to get the extended context
  const context = (form.state as {
    context: {
      task: Task | null;
      taskTypes: TaskType[];
      selectedTaskType: TaskType | null;
    };
  }).context;

  return {
    form,
    task: context.task,
    taskTypes: context.taskTypes,
    selectedTaskType: context.selectedTaskType,
    isCreate,
  };
}
