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

import { renderHook, act } from '@testing-library/react';
import { useTasksApi } from '../useTasksApi';
import { restClient, parseApiError, urlBuilder } from '../../../../../../../interface/api';

// Mock the REST API - path relative to this test file
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: (err: unknown) => ({
    message: (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
      || (err as { message?: string })?.message
      || 'An error occurred',
    status: (err as { response?: { status?: number } })?.response?.status || 0,
  }),
  urlBuilder: {
    build: jest.fn(),
    tasks: {
      list: () => '/service/rest/v1/tasks',
      get: (id: string) => `/service/rest/v1/tasks/${id}`,
      create: () => '/service/rest/v1/tasks',
      update: (id: string) => `/service/rest/v1/tasks/${id}`,
      delete: (id: string) => `/service/rest/v1/tasks/${id}`,
      run: (id: string) => `/service/rest/v1/tasks/${id}/run`,
      stop: (id: string) => `/service/rest/v1/tasks/${id}/stop`,
      templates: () => '/service/rest/v1/tasks/templates',
      template: (typeId: string) => `/service/rest/v1/tasks/templates/${typeId}`,
    },
  },
}));

// Get typed mock references after module is mocked
const mockRestClientGet = restClient.get as jest.Mock;
const mockRestClientPost = restClient.post as jest.Mock;
const mockRestClientPut = restClient.put as jest.Mock;
const mockRestClientDelete = restClient.delete as jest.Mock;

describe('useTasksApi', () => {
  const mockRestTasks = {
    items: [
      {
        id: 'task-1',
        enabled: true,
        name: 'Cleanup Task',
        type: 'repository.cleanup',
        currentState: 'WAITING' as const,
        message: 'Waiting',
        nextRun: '2026-01-22T10:00:00.000Z',
        lastRun: '2026-01-21T10:00:00.000Z',
        lastRunResult: 'OK' as const,
      },
    ],
  };

  const mockRestTaskTemplates = [
    {
      type: 'repository.cleanup',
      name: 'Cleanup repositories',
      enabled: true,
      alertEmail: null,
      notificationCondition: 'FAILURE',
      frequency: { schedule: 'manual' },
      properties: { repositoryName: '' },
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchTasks', () => {
    it('fetches tasks successfully using REST API', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestTasks);

      const { result } = renderHook(() => useTasksApi());

      let tasks;
      await act(async () => {
        tasks = await result.current.fetchTasks();
      });

      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/tasks');
      expect(tasks).toHaveLength(1);
      expect(tasks![0].id).toBe('task-1');
      expect(tasks![0].name).toBe('Cleanup Task');
      expect(tasks![0].status).toBe('WAITING');
    });

    it('returns empty array when no tasks', async () => {
      mockRestClientGet.mockResolvedValueOnce({ items: [] });

      const { result } = renderHook(() => useTasksApi());

      let tasks;
      await act(async () => {
        tasks = await result.current.fetchTasks();
      });

      expect(tasks).toEqual([]);
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClientGet.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.fetchTasks()).rejects.toThrow('Network error');
      });

      consoleSpy.mockRestore();
    });
  });

  describe('fetchTask', () => {
    it('fetches single task successfully', async () => {
      const mockTask = mockRestTasks.items[0];
      mockRestClientGet.mockResolvedValueOnce(mockTask);

      const { result } = renderHook(() => useTasksApi());

      let task;
      await act(async () => {
        task = await result.current.fetchTask('task-1');
      });

      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/tasks/task-1');
      expect(task?.id).toBe('task-1');
    });

    it('returns null for 404', async () => {
      mockRestClientGet.mockRejectedValueOnce({ response: { status: 404 } });

      const { result } = renderHook(() => useTasksApi());

      let task;
      await act(async () => {
        task = await result.current.fetchTask('nonexistent');
      });

      expect(task).toBeNull();
    });
  });

  describe('fetchTaskTypes', () => {
    it('fetches task types using REST API', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestTaskTemplates);

      const { result } = renderHook(() => useTasksApi());

      let types;
      await act(async () => {
        types = await result.current.fetchTaskTypes();
      });

      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/tasks/templates');
      expect(types).toHaveLength(1);
      expect(types![0].id).toBe('repository.cleanup');
      expect(types![0].name).toBe('Cleanup repositories');
    });

    it('returns empty array when no task types', async () => {
      mockRestClientGet.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useTasksApi());

      let types;
      await act(async () => {
        types = await result.current.fetchTaskTypes();
      });

      expect(types).toEqual([]);
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClientGet.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.fetchTaskTypes()).rejects.toThrow('Network error');
      });

      consoleSpy.mockRestore();
    });
  });

  describe('createTask', () => {
    const mockTaskFormData = {
      enabled: true,
      name: 'New Task',
      typeId: 'repository.cleanup',
      properties: { repositoryName: 'maven-central' },
      schedule: 'daily' as const,
      startDate: new Date('2026-01-22T10:00:00.000Z'),
    };

    it('creates task using REST API', async () => {
      const createdTask = {
        id: 'new-task-id',
        enabled: true,
        name: 'New Task',
        type: 'repository.cleanup',
        currentState: 'WAITING',
        message: '',
      };

      mockRestClientPost.mockResolvedValueOnce(createdTask);

      const { result } = renderHook(() => useTasksApi());

      let task;
      await act(async () => {
        task = await result.current.createTask(mockTaskFormData);
      });

      expect(mockRestClientPost).toHaveBeenCalledWith(
        '/service/rest/v1/tasks',
        expect.objectContaining({
          type: 'repository.cleanup',
          name: 'New Task',
          enabled: true,
          frequency: expect.objectContaining({
            schedule: 'daily',
          }),
        })
      );
      expect(task?.name).toBe('New Task');
    });

    it('throws error on creation failure', async () => {
      mockRestClientPost.mockRejectedValueOnce({
        response: { data: { message: 'Validation failed' } },
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.createTask(mockTaskFormData)).rejects.toThrow('Validation failed');
      });

      expect(result.current.error).toBe('Validation failed');
    });

    it('sets loading state during creation', async () => {
      let resolvePost: (value: unknown) => void;
      mockRestClientPost.mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePost = resolve;
        })
      );

      const { result } = renderHook(() => useTasksApi());

      let createPromise: Promise<unknown>;
      act(() => {
        createPromise = result.current.createTask(mockTaskFormData);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePost!({ id: 'task-1', name: 'New Task', type: 'repository.cleanup', currentState: 'WAITING' });
        await createPromise;
      });

      expect(result.current.loading).toBe(false);
    });
  });

  describe('updateTask', () => {
    const mockTaskFormData = {
      enabled: true,
      name: 'Updated Task',
      typeId: 'repository.cleanup',
      properties: { repositoryName: 'maven-central' },
      schedule: 'daily' as const,
      startDate: new Date('2026-01-22T10:00:00.000Z'),
    };

    it('updates task using REST API', async () => {
      const updatedTask = {
        id: 'task-1',
        enabled: true,
        name: 'Updated Task',
        type: 'repository.cleanup',
        currentState: 'WAITING',
        message: '',
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(updatedTask);

      const { result } = renderHook(() => useTasksApi());

      let task;
      await act(async () => {
        task = await result.current.updateTask('task-1', mockTaskFormData);
      });

      expect(mockRestClientPut).toHaveBeenCalledWith(
        '/service/rest/v1/tasks/task-1',
        expect.objectContaining({
          name: 'Updated Task',
          enabled: true,
        })
      );
      // PUT doesn't include type field
      expect(mockRestClientPut.mock.calls[0][1]).not.toHaveProperty('type');
      expect(task?.name).toBe('Updated Task');
    });

    it('throws error on update failure', async () => {
      mockRestClientPut.mockRejectedValueOnce({
        response: { data: { message: 'Task not found' } },
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.updateTask('task-1', mockTaskFormData)).rejects.toThrow('Task not found');
      });

      expect(result.current.error).toBe('Task not found');
    });
  });

  describe('runTask', () => {
    it('runs task using REST API', async () => {
      mockRestClientPost.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.runTask('task-1');
      });

      expect(mockRestClientPost).toHaveBeenCalledWith('/service/rest/v1/tasks/task-1/run');
    });

    it('sets loading state during run', async () => {
      let resolvePost: () => void;
      mockRestClientPost.mockReturnValueOnce(
        new Promise<void>((resolve) => {
          resolvePost = resolve;
        })
      );

      const { result } = renderHook(() => useTasksApi());

      act(() => {
        result.current.runTask('task-1');
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePost!();
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error on failure', async () => {
      mockRestClientPost.mockRejectedValueOnce({ message: 'Task locked' });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.runTask('task-1')).rejects.toThrow('Task locked');
      });

      expect(result.current.error).toBe('Task locked');
    });
  });

  describe('stopTask', () => {
    it('stops task using REST API', async () => {
      mockRestClientPost.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.stopTask('task-1');
      });

      expect(mockRestClientPost).toHaveBeenCalledWith('/service/rest/v1/tasks/task-1/stop');
    });

    it('sets error on failure', async () => {
      mockRestClientPost.mockRejectedValueOnce({ message: 'Task not running' });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.stopTask('task-1')).rejects.toThrow('Task not running');
      });

      expect(result.current.error).toBe('Task not running');
    });
  });

  describe('deleteTask', () => {
    it('deletes task using REST API', async () => {
      mockRestClientDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.deleteTask('task-1');
      });

      expect(mockRestClientDelete).toHaveBeenCalledWith('/service/rest/v1/tasks/task-1');
    });

    it('throws error on deletion failure', async () => {
      mockRestClientDelete.mockRejectedValueOnce({
        response: { data: { message: 'Cannot delete running task' } },
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await expect(result.current.deleteTask('task-1')).rejects.toThrow('Cannot delete running task');
      });

      expect(result.current.error).toBe('Cannot delete running task');
    });

    it('sets loading state during deletion', async () => {
      let resolveDelete: () => void;
      mockRestClientDelete.mockReturnValueOnce(
        new Promise<void>((resolve) => {
          resolveDelete = resolve;
        })
      );

      const { result } = renderHook(() => useTasksApi());

      let deletePromise: Promise<void>;
      act(() => {
        deletePromise = result.current.deleteTask('task-1');
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolveDelete!();
        await deletePromise;
      });

      expect(result.current.loading).toBe(false);
    });
  });

  describe('setError', () => {
    it('allows clearing error state', async () => {
      mockRestClientPost.mockRejectedValueOnce({ message: 'Some error' });

      const { result } = renderHook(() => useTasksApi());

      // First, trigger an error
      await act(async () => {
        try {
          await result.current.runTask('task-1');
        } catch {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Some error');

      // Clear the error
      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});
