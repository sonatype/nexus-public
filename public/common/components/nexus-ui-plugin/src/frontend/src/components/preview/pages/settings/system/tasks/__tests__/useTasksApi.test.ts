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
import { restClient, } from '../../../../../../../interface/api';

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
        typeName: 'Admin - Cleanup repositories using their associated policies',
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
      expect(tasks![0].typeId).toBe('repository.cleanup');
      expect(tasks![0].typeName).toBe('Admin - Cleanup repositories using their associated policies');
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

    it('falls back to type ID when typeName is not provided', async () => {
      mockRestClientGet.mockResolvedValueOnce({
        items: [
          {
            id: 'task-2',
            enabled: true,
            name: 'Legacy Task',
            type: 'repository.cleanup',
            // typeName is not provided
            currentState: 'WAITING' as const,
            message: '',
          },
        ],
      });

      const { result } = renderHook(() => useTasksApi());

      let tasks;
      await act(async () => {
        tasks = await result.current.fetchTasks();
      });

      expect(tasks).toHaveLength(1);
      expect(tasks![0].typeId).toBe('repository.cleanup');
      expect(tasks![0].typeName).toBe('repository.cleanup'); // falls back to type
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

    it('maps cron schedule, properties, and alert config from task response', async () => {
      const mockCronTask = {
        id: 'cron-task',
        enabled: true,
        name: 'my-repair',
        type: 'create.browse.nodes',
        currentState: 'WAITING' as const,
        schedule: 'advanced',
        cronExpression: '0 0 12 * * ?',
        timeZoneOffset: '+00:00',
        startDate: '2026-01-22T12:00:00.000Z',
        recurringDays: null,
        properties: { repositoryName: 'maven-public' },
        alertEmail: 'admin@example.com',
        notificationCondition: 'FAILURE',
      };
      mockRestClientGet.mockResolvedValueOnce(mockCronTask);

      const { result } = renderHook(() => useTasksApi());

      let task;
      await act(async () => {
        task = await result.current.fetchTask('cron-task');
      });

      expect(task?.schedule).toBe('advanced');
      expect(task?.cronExpression).toBe('0 0 12 * * ?');
      expect(task?.timeZoneOffset).toBe('+00:00');
      expect(task?.startDate).toEqual(new Date('2026-01-22T12:00:00.000Z'));
      expect(task?.recurringDays).toEqual([]);
      expect(task?.properties).toEqual({ repositoryName: 'maven-public' });
      expect(task?.alertEmail).toBe('admin@example.com');
      expect(task?.notificationCondition).toBe('FAILURE');
    });

    it('maps weekly recurringDays from task response', async () => {
      const mockWeeklyTask = {
        id: 'weekly-task',
        enabled: true,
        name: 'weekly-backup',
        type: 'db.backup',
        currentState: 'WAITING' as const,
        schedule: 'weekly',
        recurringDays: [2, 4, 6],
        startDate: '2026-01-22T09:00:00.000Z',
        properties: {},
      };
      mockRestClientGet.mockResolvedValueOnce(mockWeeklyTask);

      const { result } = renderHook(() => useTasksApi());

      let task;
      await act(async () => {
        task = await result.current.fetchTask('weekly-task');
      });

      expect(task?.schedule).toBe('weekly');
      expect(task?.recurringDays).toEqual([2, 4, 6]);
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

    it('consumes template.formFields preserving per-field required flag and storeFilters', async () => {
      mockRestClientGet.mockResolvedValueOnce([
        {
          type: 'repository.export',
          name: 'Repository - Export assets',
          enabled: true,
          notificationCondition: 'FAILURE',
          frequency: { schedule: 'manual' },
          properties: { repositoryName: '', targetDir: '', exportThreshold: '' },
          formFields: [
            {
              id: 'repositoryName',
              type: 'combobox',
              label: 'Source repository',
              helpText: 'Select the repository to export from.',
              required: true,
              storeApi: 'coreui_Repository.readReferences',
              storeFilters: { type: 'hosted,proxy' },
            },
            {
              id: 'targetDir',
              type: 'string',
              label: 'Target directory',
              required: true,
            },
            {
              id: 'exportThreshold',
              type: 'number',
              label: 'Threshold (in days) of unused assets to include',
              required: false,
              minValue: '1',
            },
          ],
        },
      ]);

      const { result } = renderHook(() => useTasksApi());

      let types;
      await act(async () => {
        types = await result.current.fetchTaskTypes();
      });

      expect(types).toHaveLength(1);
      const fields = types![0].formFields!;
      const exportThreshold = fields.find((f) => f.id === 'exportThreshold')!;
      expect(exportThreshold.required).toBe(false);
      expect(exportThreshold.minValue).toBe('1');

      const repositoryName = fields.find((f) => f.id === 'repositoryName')!;
      expect(repositoryName.required).toBe(true);
      expect(repositoryName.storeFilters).toEqual({ type: 'hosted,proxy' });
      expect(repositoryName.storeApi).toBe('coreui_Repository.readReferences');
    });

    it('falls back to properties when template.formFields is absent', async () => {
      mockRestClientGet.mockResolvedValueOnce([
        {
          type: 'repository.cleanup',
          name: 'Cleanup repositories',
          enabled: true,
          notificationCondition: 'FAILURE',
          frequency: { schedule: 'manual' },
          properties: { repositoryName: '' },
        },
      ]);

      const { result } = renderHook(() => useTasksApi());

      let types;
      await act(async () => {
        types = await result.current.fetchTaskTypes();
      });

      expect(types).toHaveLength(1);
      expect(types![0].formFields).toBeDefined();
      expect(types![0].formFields!.some((f) => f.id === 'repositoryName')).toBe(true);
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

    describe('checkbox property normalization', () => {
      it('AT-001: serializes empty-string checkbox as "false" on create', async () => {
        mockRestClientPost.mockResolvedValueOnce({
          id: 'new-task', name: 'Tags Cleanup', type: 'tags.cleanup',
          currentState: 'WAITING', message: '',
        });

        const { result } = renderHook(() => useTasksApi());

        await act(async () => {
          await result.current.createTask({
            typeId: 'tags.cleanup',
            name: 'Tags Cleanup',
            enabled: true,
            schedule: 'manual' as const,
            properties: { deleteAssociatedComponents: '' },
          } as any);
        });

        expect(mockRestClientPost).toHaveBeenCalledWith(
          '/service/rest/v1/tasks',
          expect.objectContaining({
            properties: expect.objectContaining({ deleteAssociatedComponents: 'false' }),
          }),
        );
      });

      it('AT-002: preserves "true" checkbox value on create', async () => {
        mockRestClientPost.mockResolvedValueOnce({
          id: 'new-task', name: 'Tags Cleanup', type: 'tags.cleanup',
          currentState: 'WAITING', message: '',
        });

        const { result } = renderHook(() => useTasksApi());

        await act(async () => {
          await result.current.createTask({
            typeId: 'tags.cleanup',
            name: 'Tags Cleanup',
            enabled: true,
            schedule: 'manual' as const,
            properties: { deleteAssociatedComponents: 'true' },
          } as any);
        });

        expect(mockRestClientPost).toHaveBeenCalledWith(
          '/service/rest/v1/tasks',
          expect.objectContaining({
            properties: expect.objectContaining({ deleteAssociatedComponents: 'true' }),
          }),
        );
      });

      it('AT-003: preserves "false" checkbox value on create', async () => {
        mockRestClientPost.mockResolvedValueOnce({
          id: 'new-task', name: 'Tags Cleanup', type: 'tags.cleanup',
          currentState: 'WAITING', message: '',
        });

        const { result } = renderHook(() => useTasksApi());

        await act(async () => {
          await result.current.createTask({
            typeId: 'tags.cleanup',
            name: 'Tags Cleanup',
            enabled: true,
            schedule: 'manual' as const,
            properties: { deleteAssociatedComponents: 'false' },
          } as any);
        });

        expect(mockRestClientPost).toHaveBeenCalledWith(
          '/service/rest/v1/tasks',
          expect.objectContaining({
            properties: expect.objectContaining({ deleteAssociatedComponents: 'false' }),
          }),
        );
      });

      it('AT-005: does NOT coerce non-checkbox empty-string field on create', async () => {
        mockRestClientPost.mockResolvedValueOnce({
          id: 'new-task', name: 'Tags Cleanup', type: 'tags.cleanup',
          currentState: 'WAITING', message: '',
        });

        const { result } = renderHook(() => useTasksApi());

        await act(async () => {
          await result.current.createTask({
            typeId: 'tags.cleanup',
            name: 'Tags Cleanup',
            enabled: true,
            schedule: 'manual' as const,
            properties: { nameRegex: '' },
          } as any);
        });

        expect(mockRestClientPost).toHaveBeenCalledWith(
          '/service/rest/v1/tasks',
          expect.objectContaining({
            properties: expect.objectContaining({ nameRegex: '' }),
          }),
        );
      });
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

    it('AT-004: serializes empty-string checkbox as "false" on update', async () => {
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce({
        id: 'task-1', name: 'Tags Cleanup', type: 'tags.cleanup',
        currentState: 'WAITING', message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.updateTask('task-1', {
          typeId: 'tags.cleanup',
          name: 'Tags Cleanup',
          enabled: true,
          schedule: 'manual' as const,
          properties: { deleteAssociatedComponents: '' },
        } as any);
      });

      expect(mockRestClientPut).toHaveBeenCalledWith(
        '/service/rest/v1/tasks/task-1',
        expect.objectContaining({
          properties: expect.objectContaining({ deleteAssociatedComponents: 'false' }),
        }),
      );
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

  describe('APT checkbox persistence parity (NEXUS-53043)', () => {
    // These tests verify the full serialization path for repository.apt.rebuild.metadata.
    // The machine normalizes absent checkbox fields to '' (create) or 'false' (edit/load)
    // before calling createTask/updateTask; the serializer must map both to 'false'.

    it('APT create: unchecked checkboxes (empty string from template) submit as false', async () => {
      mockRestClientPost.mockResolvedValueOnce({
        id: 'apt-new', name: 'Rebuild APT', type: 'repository.apt.rebuild.metadata',
        currentState: 'WAITING', message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.createTask({
          typeId: 'repository.apt.rebuild.metadata',
          name: 'Rebuild APT',
          enabled: true,
          schedule: 'manual' as const,
          properties: {
            repositoryName: '*',
            rebuildAptMetadataFullRebuild: '',  // machine init from template
            resetProxyMetadata: '',              // machine init from template
          },
        } as any);
      });

      const body = mockRestClientPost.mock.calls[0][1] as any;
      expect(body.properties.rebuildAptMetadataFullRebuild).toBe('false');
      expect(body.properties.resetProxyMetadata).toBe('false');
    });

    it('APT create: visibleForRepoTypes-hidden checkbox value is preserved in serialized payload', async () => {
      // Mirror of the update-side test: rebuildAptMetadataFullRebuild is hidden in the UI when
      // a proxy repo is selected (visibleForRepoTypes: ['hosted']), but a value set before the
      // proxy selection (or via the API) must NOT be dropped from the POST body.
      // Only fields with hidden:true in TASK_FIELD_UI are filtered; visibleForRepoTypes does not filter.
      mockRestClientPost.mockResolvedValueOnce({
        id: 'apt-new', name: 'Rebuild APT', type: 'repository.apt.rebuild.metadata',
        currentState: 'WAITING', message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.createTask({
          typeId: 'repository.apt.rebuild.metadata',
          name: 'Rebuild APT',
          enabled: true,
          schedule: 'manual' as const,
          properties: {
            repositoryName: 'my-apt-proxy',
            rebuildAptMetadataFullRebuild: 'true',  // UI-hidden for proxy — must still be in body
            resetProxyMetadata: 'false',
          },
        } as any);
      });

      const body = mockRestClientPost.mock.calls[0][1] as any;
      expect(body.properties.rebuildAptMetadataFullRebuild).toBe('true');
      expect(body.properties.resetProxyMetadata).toBe('false');
    });

    it('APT save after edit: machine-normalized false values are sent as false', async () => {
      // Old APT task was saved without checkbox fields; machine normalizes both to 'false'.
      // updateTask should pass those 'false' values through to the PUT body.
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce({
        id: 'apt-old', name: 'Rebuild APT', type: 'repository.apt.rebuild.metadata',
        currentState: 'WAITING', message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.updateTask('apt-old', {
          typeId: 'repository.apt.rebuild.metadata',
          name: 'Rebuild APT',
          enabled: true,
          schedule: 'manual' as const,
          properties: {
            repositoryName: '*',
            rebuildAptMetadataFullRebuild: 'false',  // machine-normalized
            resetProxyMetadata: 'false',              // machine-normalized
          },
        } as any);
      });

      const body = mockRestClientPut.mock.calls[0][1] as any;
      expect(body.properties.rebuildAptMetadataFullRebuild).toBe('false');
      expect(body.properties.resetProxyMetadata).toBe('false');
    });

    it('APT: visibleForRepoTypes-hidden checkbox value is preserved in serialized payload', async () => {
      // rebuildAptMetadataFullRebuild is hidden in the UI when a proxy repo is selected
      // (visibleForRepoTypes: ['hosted']), but it must NOT be dropped from the PUT payload.
      // Only fields with hidden:true in TASK_FIELD_UI are filtered; visibleForRepoTypes does not filter.
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce({
        id: 'apt-proxy', name: 'Rebuild APT', type: 'repository.apt.rebuild.metadata',
        currentState: 'WAITING', message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.updateTask('apt-proxy', {
          typeId: 'repository.apt.rebuild.metadata',
          name: 'Rebuild APT',
          enabled: true,
          schedule: 'manual' as const,
          properties: {
            repositoryName: 'my-apt-proxy',
            rebuildAptMetadataFullRebuild: 'true',  // UI-hidden for proxy — must still be in body
            resetProxyMetadata: 'false',
          },
        } as any);
      });

      const body = mockRestClientPut.mock.calls[0][1] as any;
      expect(body.properties.rebuildAptMetadataFullRebuild).toBe('true');
      expect(body.properties.resetProxyMetadata).toBe('false');
    });

    it('APT: existing true/false values are preserved on update', async () => {
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce({
        id: 'apt-task', name: 'Rebuild APT', type: 'repository.apt.rebuild.metadata',
        currentState: 'WAITING', message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.updateTask('apt-task', {
          typeId: 'repository.apt.rebuild.metadata',
          name: 'Rebuild APT',
          enabled: true,
          schedule: 'manual' as const,
          properties: {
            repositoryName: 'my-apt-hosted',
            rebuildAptMetadataFullRebuild: 'true',
            resetProxyMetadata: 'false',
          },
        } as any);
      });

      const body = mockRestClientPut.mock.calls[0][1] as any;
      expect(body.properties.rebuildAptMetadataFullRebuild).toBe('true');
      expect(body.properties.resetProxyMetadata).toBe('false');
    });
  });

  describe('Data Repair Plan serialization (blobstore.planReconciliation)', () => {
    const created = {
      id: 'plan-1', name: 'Repair - Data Repair Plan', type: 'blobstore.planReconciliation',
      currentState: 'WAITING', message: '',
    };

    const durationProps = {
      topAlertBanner: '',
      bottomAlertBanner: '',
      onlyNotify: 'true',
      blobstoreName: '(All Blob Stores)',
      repositoryName: '',
      taskScope: 'duration',
      name: 'Repair - Data Repair Plan',
      sinceDays: '',
      sinceHours: '',
      sinceMinutes: '',
      reconcileStartDate: '',
      reconcileEndDate: '',
    };

    const createPlan = async (properties: Record<string, string>) => {
      mockRestClientPost.mockResolvedValueOnce(created);
      const { result } = renderHook(() => useTasksApi());
      await act(async () => {
        await result.current.createTask({
          typeId: 'blobstore.planReconciliation',
          name: 'Repair - Data Repair Plan',
          enabled: true,
          schedule: 'manual' as const,
          properties,
        } as any);
      });
      const calls = mockRestClientPost.mock.calls;
      return (calls[calls.length - 1][1] as any).properties as Record<string, string>;
    };

    // EDIT/UPDATE serialization. PUT is a merge on the backend (TasksApiResourcePro#updateTask
    // applies the existing config, then overlays the payload via setString), and
    // TaskConfiguration#setString removes a key when given an empty string. So to CLEAR a
    // previously-saved selector the payload must carry an explicit '' for that key — omitting it
    // would leave the old value in place. This helper returns the PUT request's properties map.
    const updatePlan = async (properties: Record<string, string>) => {
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(created);
      const { result } = renderHook(() => useTasksApi());
      await act(async () => {
        await result.current.updateTask('plan-1', {
          id: 'plan-1',
          typeId: 'blobstore.planReconciliation',
          name: 'Repair - Data Repair Plan',
          enabled: true,
          schedule: 'manual' as const,
          properties,
        } as any);
      });
      const calls = mockRestClientPut.mock.calls;
      return (calls[calls.length - 1][1] as any).properties as Record<string, string>;
    };

    it('drops display-only banners and the name template from the payload', async () => {
      const props = await createPlan(durationProps);
      expect(props).not.toHaveProperty('topAlertBanner');
      expect(props).not.toHaveProperty('bottomAlertBanner');
      expect(props).not.toHaveProperty('name');
    });

    // Classic parity: empty ExtJS number fields export the literal string "null".
    it('serializes empty duration fields as the literal "null" (only Minutes set)', async () => {
      const props = await createPlan({ ...durationProps, sinceMinutes: '30' });
      expect(props.sinceDays).toBe('null');
      expect(props.sinceHours).toBe('null');
      expect(props.sinceMinutes).toBe('30');
      // Duration scope drops the date side.
      expect(props).not.toHaveProperty('reconcileStartDate');
      expect(props).not.toHaveProperty('reconcileEndDate');
      expect(props.taskScope).toBe('duration');
    });

    it('serializes all-blank duration fields as "null" (matches Classic byte-for-byte)', async () => {
      const props = await createPlan(durationProps); // sinceDays/Hours/Minutes all ''
      expect(props.sinceDays).toBe('null');
      expect(props.sinceHours).toBe('null');
      expect(props.sinceMinutes).toBe('null');
    });

    it('omits blobstoreName for the implicit all-blob-stores state (empty or sentinel)', async () => {
      const fromSentinel = await createPlan({ ...durationProps, blobstoreName: '(All Blob Stores)' });
      expect(fromSentinel).not.toHaveProperty('blobstoreName');
      const fromEmpty = await createPlan({ ...durationProps, blobstoreName: '' });
      expect(fromEmpty).not.toHaveProperty('blobstoreName');
    });

    it('serializes an explicit blob-store selection as a comma-separated list', async () => {
      const props = await createPlan({ ...durationProps, blobstoreName: 'default,other' });
      expect(props.blobstoreName).toBe('default,other');
    });

    it('omits an empty repository selection but serializes an explicit one', async () => {
      const empty = await createPlan({ ...durationProps, repositoryName: '' });
      expect(empty).not.toHaveProperty('repositoryName');
      const explicit = await createPlan({ ...durationProps, repositoryName: 'maven-central,npm-proxy' });
      expect(explicit.repositoryName).toBe('maven-central,npm-proxy');
    });

    it('keeps the date side and drops the duration side when scope=dates', async () => {
      const props = await createPlan({
        ...durationProps,
        taskScope: 'dates',
        reconcileStartDate: '06/24/2026',
        reconcileEndDate: '06/25/2026',
      });
      expect(props.reconcileStartDate).toBe('06/24/2026');
      expect(props.reconcileEndDate).toBe('06/25/2026');
      expect(props).not.toHaveProperty('sinceDays');
      expect(props).not.toHaveProperty('sinceHours');
      expect(props).not.toHaveProperty('sinceMinutes');
    });

    it('coerces onlyNotify to a boolean string', async () => {
      const props = await createPlan({ ...durationProps, onlyNotify: 'true' });
      expect(props.onlyNotify).toBe('true');
      const propsFalse = await createPlan({ ...durationProps, onlyNotify: '' });
      expect(propsFalse.onlyNotify).toBe('false');
    });

    it('does not apply these rules to unrelated task types (scope guard)', async () => {
      mockRestClientPost.mockResolvedValueOnce({
        id: 'c1', name: 'Cleanup', type: 'repository.cleanup', currentState: 'WAITING', message: '',
      });
      const { result } = renderHook(() => useTasksApi());
      await act(async () => {
        await result.current.createTask({
          typeId: 'repository.cleanup', name: 'Cleanup', enabled: true, schedule: 'manual' as const,
          properties: { repositoryName: '' },
        } as any);
      });
      const calls = mockRestClientPost.mock.calls;
      const props = (calls[calls.length - 1][1] as any).properties as Record<string, string>;
      // repository.cleanup has no omitWhenEmpty/serializeEmptyAs override → empty value passes through as ''.
      expect(props.repositoryName).toBe('');
    });

    // ---- EDIT mode: clearing a previously-saved selection must persist (NEXUS-53485) ----
    // On CREATE the all-state selector is omitted (Classic parity); on UPDATE it must be sent as
    // '' so the backend merge overwrites/removes the prior explicit value instead of retaining it.

    it('UPDATE: clearing repositoryName sends an explicit "" so the merge drops the old value', async () => {
      const props = await updatePlan({ ...durationProps, repositoryName: '' });
      // Present (not omitted) and empty — setString('') removes the key on the merged config.
      expect(props).toHaveProperty('repositoryName');
      expect(props.repositoryName).toBe('');
    });

    it('UPDATE: clearing blobstoreName (sentinel or empty) sends "" to clear the prior selection', async () => {
      const fromSentinel = await updatePlan({ ...durationProps, blobstoreName: '(All Blob Stores)' });
      expect(fromSentinel).toHaveProperty('blobstoreName');
      expect(fromSentinel.blobstoreName).toBe('');
      const fromEmpty = await updatePlan({ ...durationProps, blobstoreName: '' });
      expect(fromEmpty.blobstoreName).toBe('');
    });

    it('UPDATE: clearing both blobstoreName and repositoryName sends "" for each', async () => {
      const props = await updatePlan({
        ...durationProps,
        blobstoreName: '(All Blob Stores)',
        repositoryName: '',
      });
      expect(props.blobstoreName).toBe('');
      expect(props.repositoryName).toBe('');
    });

    it('UPDATE: explicit selections still serialize as comma-separated lists (not cleared)', async () => {
      const props = await updatePlan({
        ...durationProps,
        blobstoreName: 'default,other',
        repositoryName: 'maven-central,npm-proxy',
      });
      expect(props.blobstoreName).toBe('default,other');
      expect(props.repositoryName).toBe('maven-central,npm-proxy');
    });

    it('CREATE omits but UPDATE clears the all-blob-stores selector (merge-aware asymmetry)', async () => {
      const createProps = await createPlan({ ...durationProps, blobstoreName: '(All Blob Stores)' });
      expect(createProps).not.toHaveProperty('blobstoreName');
      const updateProps = await updatePlan({ ...durationProps, blobstoreName: '(All Blob Stores)' });
      expect(updateProps.blobstoreName).toBe('');
    });

    it('UPDATE: blank active duration fields still serialize as the literal "null"', async () => {
      const props = await updatePlan(durationProps); // sinceDays/Hours/Minutes all ''
      expect(props.sinceDays).toBe('null');
      expect(props.sinceHours).toBe('null');
      expect(props.sinceMinutes).toBe('null');
    });

    it('UPDATE scope guard: unrelated task types are unaffected by the clear-on-update rule', async () => {
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce({
        id: 'c1', name: 'Cleanup', type: 'repository.cleanup', currentState: 'WAITING', message: '',
      });
      const { result } = renderHook(() => useTasksApi());
      await act(async () => {
        await result.current.updateTask('c1', {
          id: 'c1', typeId: 'repository.cleanup', name: 'Cleanup', enabled: true, schedule: 'manual' as const,
          properties: { repositoryName: '' },
        } as any);
      });
      const calls = mockRestClientPut.mock.calls;
      const props = (calls[calls.length - 1][1] as any).properties as Record<string, string>;
      // repository.cleanup has no omitWhenEmpty override → the value passes through unchanged ('').
      expect(props.repositoryName).toBe('');
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

  describe('serialization — Execute display fields', () => {
    it('drops banners, static-info headers, plan-information widget, and read-only display fields from the payload', async () => {
      const { EXECUTE_RECONCILE_PLAN_TYPE_ID } = require('../taskFieldMetadata');
      mockRestClientPost.mockResolvedValueOnce({ id: 'x', name: 'x', type: EXECUTE_RECONCILE_PLAN_TYPE_ID });
      const { result } = renderHook(() => useTasksApi());
      await act(async () => {
        await result.current.createTask({
          enabled: true,
          name: 'Repair - Execute Data Repair Plan',
          typeId: EXECUTE_RECONCILE_PLAN_TYPE_ID,
          schedule: 'manual',
          properties: {
            topAlertBanner: '',
            planOptionsLabelId: '',
            planInformationLabelId: '',
            planInformation: '',
            blobstoreName: '(All Blob Stores)',
            repositoryName: 'repo-1',
            taskScope: 'dates',
            reconcileStartDate: '06/01/2026',
            reconcileEndDate: '06/02/2026',
          },
        } as any);
      });
      const sent = (mockRestClientPost.mock.calls[0][1] as any).properties;
      // Display-only types (banners, static-info, plan-information widget) — never persisted
      expect(sent.topAlertBanner).toBeUndefined();
      expect(sent.planOptionsLabelId).toBeUndefined();
      expect(sent.planInformationLabelId).toBeUndefined();
      expect(sent.planInformation).toBeUndefined();
      // Derived display-only fields (neverSerialize) — never sent to the backend
      expect(sent.blobstoreName).toBeUndefined();
      expect(sent.repositoryName).toBeUndefined();
      expect(sent.reconcileStartDate).toBeUndefined();
      expect(sent.reconcileEndDate).toBeUndefined();
      // taskScope is a display-only field (Execute task ignores it at runtime) — never serialized
      expect(sent.taskScope).toBeUndefined();
    });

    it('preserves planIds through serialization (not a read-only field)', async () => {
      const { EXECUTE_RECONCILE_PLAN_TYPE_ID } = require('../taskFieldMetadata');
      mockRestClientPost.mockResolvedValueOnce({ id: 'x', name: 'x', type: EXECUTE_RECONCILE_PLAN_TYPE_ID });
      const { result } = renderHook(() => useTasksApi());
      await act(async () => {
        await result.current.createTask({
          enabled: true,
          name: 'Repair - Execute Data Repair Plan',
          typeId: EXECUTE_RECONCILE_PLAN_TYPE_ID,
          schedule: 'manual',
          properties: { planIds: 'plan-abc-123' },
        } as any);
      });
      const sent = (mockRestClientPost.mock.calls[0][1] as any).properties;
      expect(sent.planIds).toBe('plan-abc-123');
    });
  });

  describe('property round-trip', () => {
    it('preserves dot-prefixed properties when creating a task', async () => {
      mockRestClientPost.mockResolvedValueOnce({});

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.createTask(
          {
            typeId: 'repository.cleanup',
            name: 'Cleanup',
            enabled: true,
            schedule: 'manual',
            properties: {
              repositoryName: 'maven-public',
              'repository.cleanup.policies': 'old,older',
              'some.dotted.key': 'value',
            },
          } as any,
          undefined,
        );
      });

      expect(mockRestClientPost).toHaveBeenCalledWith(
        '/service/rest/v1/tasks',
        expect.objectContaining({
          properties: {
            repositoryName: 'maven-public',
            'repository.cleanup.policies': 'old,older',
            'some.dotted.key': 'value',
          },
        }),
      );
    });

    it('preserves dot-prefixed properties when updating a task', async () => {
      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce({
        id: 'task-1',
        enabled: true,
        name: 'Cleanup',
        type: 'repository.cleanup',
        currentState: 'WAITING',
        message: '',
      });

      const { result } = renderHook(() => useTasksApi());

      await act(async () => {
        await result.current.updateTask(
          'task-1',
          {
            typeId: 'repository.cleanup',
            name: 'Cleanup',
            enabled: true,
            schedule: 'manual',
            properties: {
              'repository.cleanup.policies': 'old,older',
            },
          } as any,
          undefined,
        );
      });

      expect(mockRestClientPut).toHaveBeenCalledWith(
        '/service/rest/v1/tasks/task-1',
        expect.objectContaining({
          properties: { 'repository.cleanup.policies': 'old,older' },
        }),
      );
    });
  });

  // The REST `currentState` is the raw TaskState enum (TaskState.java): WAITING,
  // RUNNING_STARTING / RUNNING / RUNNING_BLOCKED / RUNNING_CANCELED, and the
  // DONE-group OK / FAILED / CANCELED / INTERRUPTED. When a task reports progress
  // it arrives as "RUNNING: <progress>" (TaskXO.java). The transform must classify
  // every running-group value (including the progress suffix) as RUNNING so the
  // badge and Stop button reflect a live run; otherwise it stays stuck at WAITING.
  describe('currentState → status transform', () => {
    const fetchSingle = async (currentState: string) => {
      mockRestClientGet.mockResolvedValueOnce({
        id: 'task-1',
        enabled: true,
        name: 'T',
        type: 'repository.cleanup',
        currentState,
        message: '',
      });
      const { result } = renderHook(() => useTasksApi());
      let task;
      await act(async () => {
        task = await result.current.fetchTask('task-1');
      });
      return task!;
    };

    it('maps WAITING to WAITING (runnable, not stoppable)', async () => {
      const task = await fetchSingle('WAITING');
      expect(task.status).toBe('WAITING');
      expect(task.runnable).toBe(true);
      expect(task.stoppable).toBe(false);
    });

    it('maps RUNNING to RUNNING (stoppable, not runnable)', async () => {
      const task = await fetchSingle('RUNNING');
      expect(task.status).toBe('RUNNING');
      expect(task.runnable).toBe(false);
      expect(task.stoppable).toBe(true);
    });

    it('maps RUNNING with a progress suffix to RUNNING', async () => {
      const task = await fetchSingle('RUNNING: 42 of 100 assets');
      expect(task.status).toBe('RUNNING');
      expect(task.stoppable).toBe(true);
    });

    it('maps RUNNING_STARTING to RUNNING', async () => {
      expect((await fetchSingle('RUNNING_STARTING')).status).toBe('RUNNING');
    });

    it('maps RUNNING_BLOCKED to RUNNING', async () => {
      const task = await fetchSingle('RUNNING_BLOCKED');
      expect(task.status).toBe('RUNNING');
      expect(task.stoppable).toBe(true);
    });

    it('maps RUNNING_CANCELED to RUNNING', async () => {
      expect((await fetchSingle('RUNNING_CANCELED')).status).toBe('RUNNING');
    });

    it('maps the DONE-group terminal states to themselves', async () => {
      expect((await fetchSingle('OK')).status).toBe('OK');
      expect((await fetchSingle('FAILED')).status).toBe('FAILED');
      expect((await fetchSingle('CANCELED')).status).toBe('CANCELED');
      expect((await fetchSingle('INTERRUPTED')).status).toBe('INTERRUPTED');
    });

    it('treats terminal states as not running (runnable, not stoppable)', async () => {
      const task = await fetchSingle('OK');
      expect(task.runnable).toBe(true);
      expect(task.stoppable).toBe(false);
    });
  });
});
