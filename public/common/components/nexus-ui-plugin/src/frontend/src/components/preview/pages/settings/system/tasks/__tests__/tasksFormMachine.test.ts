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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';
import { createTaskFormMachine, validateTask } from '../tasksFormMachine';
import { EXECUTE_RECONCILE_PLAN_TYPE_ID } from '../taskFieldMetadata';

// Mock the local API module used by tasksFormMachine
jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: {
    TASKS: '/service/rest/v1/tasks',
  },
  restClient: {
    get: jest.fn().mockResolvedValue([]),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

// Mock task types returned by the API
const MOCK_TASK_TYPES = [
  {
    type: 'repository.cleanup',
    name: 'Admin - Cleanup repositories using cleanup policies',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: {},
  },
  {
    type: 'blobstore.compact',
    name: 'Admin - Compact blob store',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: { blobstoreName: '' },
  },
  {
    type: 'db.backup',
    name: 'Admin - Export databases for backup',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: { location: '' },
  },
  {
    type: 'tags.cleanup',
    name: 'Admin - Cleanup tags',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: {
      firstCreatedDays: '0',
      lastUpdatedDays: '0',
      nameRegex: '',
      deleteAssociatedComponents: 'false',
      restrictComponentDelete: '',
    },
    concurrentRun: true,
  },
  {
    // NEXUS-53360: ScriptTask — language has a default, source is MANDATORY/empty.
    // multinode is included unconditionally here on purpose to verify the checkbox is
    // never required; the real template only includes it on clustered deployments.
    type: 'script',
    name: 'Admin - Execute script',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: { language: 'groovy', source: '', multinode: 'false' },
    concurrentRun: true,
  },
  {
    // NEXUS-53484: Execute Data Repair Plan. planIds is the only stored property;
    // blobstoreName / repositoryName / dates are display-only, derived from the Plan task.
    type: 'blobstore.executeReconciliationPlan',
    name: 'Repair - Execute Data Repair Plan',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: { planIds: '' },
    concurrentRun: false,
  },
  {
    // NEXUS-53485: Data Repair Plan (self-hosted). Mirrors TaskTemplateXO.toTaskTemplateXO —
    // every descriptor form field id is a property keyed to its initial value.
    type: 'blobstore.planReconciliation',
    name: 'Repair - Data Repair Plan',
    enabled: true,
    alertEmail: null,
    notificationCondition: 'FAILURE',
    frequency: { schedule: 'manual' },
    properties: {
      topAlertBanner: '',
      bottomAlertBanner: '',
      onlyNotify: 'true',
      blobstoreName: '(All Blob Stores)',
      repositoryName: '',
      taskScope: 'duration',
      name: 'Repair - Data Repair Plan',
      sinceDays: '',
      sinceHours: '',
      sinceMinutes: '30',
      reconcileStartDate: '',
      reconcileEndDate: '',
    },
    concurrentRun: false,
  },
];

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createTaskFormMachine>,
) {
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('templates')) {
      return Promise.resolve(MOCK_TASK_TYPES);
    }
    return Promise.resolve([]);
  });

  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('tasksFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      restClient.get.mockResolvedValue(MOCK_TASK_TYPES);
      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to manual schedule in create mode', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.schedule).toBe('manual');
      expect(state.matches({ editing: 'manual' })).toBe(true);

      service.stop();
    });

    it('loads task types into context', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      const context = service.getSnapshot().context as any;
      expect(context.taskTypes).toHaveLength(MOCK_TASK_TYPES.length);
      expect(context.taskTypes[0].id).toBe('repository.cleanup');
      expect(context.taskTypes[1].id).toBe('blobstore.compact');
      expect(context.taskTypes[2].id).toBe('db.backup');
      expect(context.taskTypes[3].id).toBe('tags.cleanup');
      expect(context.taskTypes[4].id).toBe('script');
      expect(context.taskTypes[5].id).toBe('blobstore.executeReconciliationPlan');
      expect(context.taskTypes[6].id).toBe('blobstore.planReconciliation');

      service.stop();
    });
  });

  describe('schedule variant sub-states', () => {
    const allSchedules = ['manual', 'once', 'hourly', 'daily', 'weekly', 'monthly', 'advanced'] as const;

    it.each(allSchedules)('transitions to %s sub-state on SCHEDULE_CHANGE', async (schedule) => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: schedule } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: schedule })).toBe(true);
      expect(state.context.data.schedule).toBe(schedule);

      service.stop();
    });

    it('transitions between all schedule variants', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Start at manual (default)
      expect(service.getSnapshot().matches({ editing: 'manual' })).toBe(true);

      // Transition through all schedules
      for (const schedule of allSchedules) {
        service.send({ type: 'SCHEDULE_CHANGE', value: schedule } as any);
        expect(service.getSnapshot().matches({ editing: schedule })).toBe(true);
      }

      // Transition back to manual
      service.send({ type: 'SCHEDULE_CHANGE', value: 'manual' } as any);
      expect(service.getSnapshot().matches({ editing: 'manual' })).toBe(true);

      service.stop();
    });

    it('resets schedule-specific fields on SCHEDULE_CHANGE to manual', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set up weekly schedule with recurring days
      service.send({ type: 'SCHEDULE_CHANGE', value: 'weekly' } as any);
      service.send({ type: 'UPDATE', name: 'recurringDays', value: [2, 4, 6] } as any);

      expect(service.getSnapshot().context.data.recurringDays).toEqual([2, 4, 6]);

      // Switch to manual
      service.send({ type: 'SCHEDULE_CHANGE', value: 'manual' } as any);

      const ctx = service.getSnapshot().context;
      expect(ctx.data.startDate).toBeNull();
      expect(ctx.data.recurringDays).toEqual([]);
      expect(ctx.data.cronExpression).toBe('');

      service.stop();
    });
  });

  describe('sub-state metadata', () => {
    it('manual sub-state has correct schedule metadata', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'manual' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const scheduleMeta = metaValues.find((m: any) => m?.scheduleLabel) as any;

      expect(scheduleMeta).toBeDefined();
      expect(scheduleMeta.scheduleLabel).toBe('Manual');
      expect(scheduleMeta.scheduleFields).toEqual([]);
      expect(scheduleMeta.requiredScheduleFields).toEqual([]);

      service.stop();
    });

    it('weekly sub-state has recurringDays in schedule fields', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'weekly' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const scheduleMeta = metaValues.find((m: any) => m?.scheduleLabel) as any;

      expect(scheduleMeta).toBeDefined();
      expect(scheduleMeta.scheduleLabel).toBe('Weekly');
      expect(scheduleMeta.scheduleFields).toContain('recurringDays');
      expect(scheduleMeta.requiredScheduleFields).toContain('recurringDays');

      service.stop();
    });

    it('advanced sub-state has cronExpression field', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'advanced' } as any);
      const state = service.getSnapshot();

      const metaValues = Object.values(state.meta);
      const scheduleMeta = metaValues.find((m: any) => m?.scheduleLabel) as any;

      expect(scheduleMeta.scheduleFields).toContain('cronExpression');
      expect(scheduleMeta.requiredScheduleFields).toContain('cronExpression');

      service.stop();
    });

    it('every schedule variant has metadata', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      const allSchedules = ['manual', 'once', 'hourly', 'daily', 'weekly', 'monthly', 'advanced'];

      for (const schedule of allSchedules) {
        service.send({ type: 'SCHEDULE_CHANGE', value: schedule } as any);
        const state = service.getSnapshot();

        const metaValues = Object.values(state.meta);
        const scheduleMeta = metaValues.find((m: any) => m?.scheduleLabel) as any;

        expect(scheduleMeta).toBeDefined();
        expect(scheduleMeta.scheduleLabel).toBeTruthy();
        expect(Array.isArray(scheduleMeta.scheduleFields)).toBe(true);
        expect(Array.isArray(scheduleMeta.requiredScheduleFields)).toBe(true);
      }

      service.stop();
    });
  });

  describe('task type changes (TASK_TYPE_CHANGE)', () => {
    it('updates typeId and selectedTaskType on TASK_TYPE_CHANGE', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.compact' } as any);

      const context = service.getSnapshot().context as any;
      expect(context.data.typeId).toBe('blobstore.compact');
      expect(context.selectedTaskType).toBeDefined();
      expect(context.selectedTaskType.id).toBe('blobstore.compact');

      service.stop();
    });

    it('resets properties on TASK_TYPE_CHANGE', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set type to blobstore.compact (has blobstoreName property)
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.compact' } as any);
      service.send({ type: 'UPDATE', name: 'properties.blobstoreName', value: 'default' } as any);

      expect((service.getSnapshot().context as any).data.properties.blobstoreName).toBe('default');

      // Switch to db.backup (has location property)
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'db.backup' } as any);

      const ctx = (service.getSnapshot().context as any);
      // Old property should be gone, new one should be initialized
      expect(ctx.data.properties.blobstoreName).toBeUndefined();
      expect(ctx.data.properties.location).toBeDefined();

      service.stop();
    });

    it('initializes properties with default values from task type form fields', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'db.backup' } as any);

      const context = service.getSnapshot().context as any;
      // db.backup has location property with empty string default
      expect(context.data.properties).toHaveProperty('location');

      service.stop();
    });
  });

  describe('Data Repair Plan (blobstore.planReconciliation)', () => {
    it('prefills the task name from the descriptor default and omits it from properties', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);

      const ctx = service.getSnapshot().context as any;
      expect(ctx.data.name).toBe('Repair - Data Repair Plan');
      // The hidden `name` template field must not become a persisted property.
      expect(ctx.data.properties).not.toHaveProperty('name');
      // taskScope default and the blob-store sentinel are seeded from the template.
      expect(ctx.data.properties.taskScope).toBe('duration');
      expect(ctx.data.properties.blobstoreName).toBe('(All Blob Stores)');

      service.stop();
    });

    it('does not overwrite a name the user already typed', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'My Custom Plan' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);

      expect((service.getSnapshot().context as any).data.name).toBe('My Custom Plan');

      service.stop();
    });

    it('rejects an end date before the start date when scope=dates', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      service.send({ type: 'UPDATE', name: 'properties.taskScope', value: 'dates' } as any);
      service.send({ type: 'UPDATE', name: 'properties.reconcileStartDate', value: '06/25/2026' } as any);
      service.send({ type: 'UPDATE', name: 'properties.reconcileEndDate', value: '06/24/2026' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.reconcileEndDate).toBe('End date must be on or after start date');

      service.stop();
    });

    it('accepts an end date on or after the start date', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      service.send({ type: 'UPDATE', name: 'properties.taskScope', value: 'dates' } as any);
      service.send({ type: 'UPDATE', name: 'properties.reconcileStartDate', value: '06/24/2026' } as any);
      service.send({ type: 'UPDATE', name: 'properties.reconcileEndDate', value: '06/25/2026' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.reconcileEndDate).toBeUndefined();

      service.stop();
    });

    it('does not apply date-range validation in duration scope', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      // Even with an inconsistent (stale) inactive-side date pair, duration scope ignores them.
      service.send({ type: 'UPDATE', name: 'properties.reconcileStartDate', value: '06/25/2026' } as any);
      service.send({ type: 'UPDATE', name: 'properties.reconcileEndDate', value: '06/24/2026' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.reconcileEndDate).toBeUndefined();

      service.stop();
    });

    it('does not flag blank duration fields as non-numeric', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceDays', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceHours', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceMinutes', value: '30' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.sinceDays).toBeUndefined();
      expect(errors.properties?.sinceHours).toBeUndefined();

      service.stop();
    });

    it('treats a "null" duration value (loaded sentinel) as empty, not as a bad number', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceDays', value: 'null' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.sinceDays).toBeUndefined();

      service.stop();
    });

    it('still rejects a non-numeric duration value', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceDays', value: 'abc' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.sinceDays).toBe('Days must be a number');

      service.stop();
    });

    it('allows save when all three duration fields are blank', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.planReconciliation' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Plan' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceDays', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceHours', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'properties.sinceMinutes', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.sinceDays).toBeUndefined();
      expect(errors.properties?.sinceHours).toBeUndefined();
      expect(errors.properties?.sinceMinutes).toBeUndefined();

      service.stop();
    });

    it('normalizes a loaded "null" duration value back to empty on edit', async () => {
      const planTask: any = {
        id: 'plan-1', enabled: true, name: 'Repair - Data Repair Plan',
        typeId: 'blobstore.planReconciliation', typeName: 'Repair - Data Repair Plan',
        status: 'WAITING', statusDescription: '', runnable: true, stoppable: false,
        alertEmail: '', notificationCondition: 'FAILURE', schedule: 'manual',
        properties: { taskScope: 'duration', sinceDays: 'null', sinceHours: 'null', sinceMinutes: '30', onlyNotify: 'true' },
      };
      const machine = createTaskFormMachine('plan-1', planTask);
      const service = await startAndLoad(machine);

      const props = (service.getSnapshot().context as any).data.properties;
      expect(props.sinceDays).toBe('');
      expect(props.sinceHours).toBe('');
      expect(props.sinceMinutes).toBe('30');

      service.stop();
    });

    it('does not relax numeric validation for unrelated task types', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      // tags.cleanup has the numeric firstCreatedDays field (no serializeEmptyAs override).
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'tags.cleanup' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Tags' } as any);
      service.send({ type: 'UPDATE', name: 'properties.firstCreatedDays', value: 'abc' } as any);
      service.send({ type: 'SUBMIT' } as any);
      let errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.firstCreatedDays).toBeTruthy();

      // ...but a blank value is still accepted (unchanged behavior).
      service.send({ type: 'UPDATE', name: 'properties.firstCreatedDays', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);
      errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.firstCreatedDays).toBeUndefined();

      service.stop();
    });
  });

  describe('validation', () => {
    it('validates name is required', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates task type is required', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'Test Task' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.typeId).toBeTruthy();

      service.stop();
    });

    it('validates email format', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'alertEmail', value: 'invalid-email' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.alertEmail).toBeTruthy();

      service.stop();
    });

    it('validates cron expression for advanced schedule', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'advanced' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test Task' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.cleanup' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.cronExpression).toBeTruthy();

      service.stop();
    });

    it('validates invalid cron expression format', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'advanced' } as any);
      service.send({ type: 'UPDATE', name: 'cronExpression', value: 'bad' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.cronExpression).toContain('Invalid');

      service.stop();
    });

    it('accepts valid cron expression', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'advanced' } as any);
      service.send({ type: 'UPDATE', name: 'cronExpression', value: '0 0 3 * * ?' } as any);
      // Trigger validation
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.cronExpression).toBeFalsy();

      service.stop();
    });

    it('validates weekly schedule requires recurring days', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'weekly' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.cleanup' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.recurringDays).toBeTruthy();

      service.stop();
    });

    it('validates start date is required for time-based schedules', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'once' } as any);
      // Clear the start date that was auto-set
      service.send({ type: 'UPDATE', name: 'startDate', value: null } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.cleanup' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.startDate).toBeTruthy();

      service.stop();
    });

    it('reports a regex-compile error on the nameRegex property', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'tags.cleanup' } as any);
      service.send({
        type: 'UPDATE',
        name: 'properties',
        value: { nameRegex: '[unclosed' },
      } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      const errors = state.context.validationErrors as any;
      expect(errors.properties?.nameRegex).toContain('valid regular expression');

      service.stop();
    });

    it('accepts a once schedule whose startDate is in the past (matches classic UI behavior)', async () => {
      // Classic ExtJS UI lets the user create a "once" task with a past startDate.
      // The Preview UI must not be stricter — the previous client- and server-side
      // future-only check was removed under NEXUS-53044.
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'once' } as any);
      service.send({ type: 'UPDATE', name: 'startDate', value: new Date(2000, 0, 1) } as any);
      service.send({ type: 'UPDATE', name: 'startTime', value: '00:00' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.cleanup' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.startDate).toBeFalsy();

      service.stop();
    });

    it('reports a required error for an empty required property (db.backup location)', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'db.backup' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      const errors = state.context.validationErrors as any;
      expect(errors.properties?.location).toContain('required');

      service.stop();
    });

    it('NEXUS-53360: requires script source, accepts the default language, and never requires multinode', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'My Script' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'script' } as any);
      // source starts empty (MANDATORY) -> error; language defaults to 'groovy' -> ok.
      service.send({ type: 'SUBMIT' } as any);

      const errors = service.getSnapshot().context.validationErrors as any;
      expect(errors.properties?.source).toContain('required');
      expect(errors.properties?.language).toBeUndefined();
      // multinode is a checkbox — never user-required even when blank/false.
      expect(errors.properties?.multinode).toBeUndefined();

      // Filling in the source clears the error.
      service.send({
        type: 'UPDATE',
        name: 'properties',
        value: { language: 'groovy', source: 'log.info("hi")', multinode: 'false' },
      } as any);
      service.send({ type: 'SUBMIT' } as any);

      const cleared = service.getSnapshot().context.validationErrors as any;
      expect(cleared.properties?.source).toBeUndefined();

      service.stop();
    });

    it('does not report a required error for tags.cleanup properties left blank', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'tags.cleanup' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      const errors = state.context.validationErrors as any;
      expect(errors.properties?.firstCreatedDays).toBeUndefined();
      expect(errors.properties?.lastUpdatedDays).toBeUndefined();
      expect(errors.properties?.nameRegex).toBeUndefined();
      expect(errors.properties?.restrictComponentDelete).toBeUndefined();

      service.stop();
    });

    /**
     * NEXUS-53044 — regression: clearing a required dynamic field (e.g. the Repository
     * combobox on most repo tasks) used to leave the EDIT screen's Save button enabled
     * because validateTask only inspected static fields. The canSave guard reads the
     * full validationErrors map, so writing a `properties` sub-object disables Save.
     */
    it('flags an empty required dynamic field and disables Save', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'Compact' } as any);
      // blobstore.compact declares blobstoreName as required (default for fields with
      // a TASK_FIELD_UI entry — no `required: false` override).
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.compact' } as any);
      // Initial blobstoreName from the descriptor is '' — validation should already fail.
      service.send({ type: 'UPDATE', name: 'name', value: 'Compact' } as any);

      const errors = service.getSnapshot().context.validationErrors as Record<string, unknown>;
      expect(errors.properties).toEqual({ blobstoreName: 'Blob Store is required' });

      service.stop();
    });

    it('clears the dynamic-field error when a value is filled in', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'Compact' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'blobstore.compact' } as any);
      service.send({
        type: 'UPDATE',
        name: 'properties',
        value: { blobstoreName: 'default' },
      } as any);

      const errors = service.getSnapshot().context.validationErrors as Record<string, unknown>;
      expect(errors.properties).toBeUndefined();

      service.stop();
    });

    it('does not block Save for fields the descriptor declares optional', async () => {
      // ExternalMetadataTask declares external.metadata.repository.format with
      // required: false; clearing it must not produce a validation error.
      const machine = createTaskFormMachine(undefined);
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) {
          return Promise.resolve([
            {
              type: 'external.blobstore.metadata',
              name: 'Retrieve external blobstore metadata',
              enabled: true,
              notificationCondition: 'FAILURE',
              frequency: { schedule: 'manual' },
              properties: {
                repositoryName: 'maven-central',
                'external.metadata.repository.format': '',
              },
            },
          ]);
        }
        return Promise.resolve([]);
      });

      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      service.send({ type: 'UPDATE', name: 'name', value: 'Test' } as any);
      service.send({ type: 'TASK_TYPE_CHANGE', value: 'external.blobstore.metadata' } as any);

      const errors = service.getSnapshot().context.validationErrors as Record<
        string,
        Record<string, string> | string
      >;
      // Only the repositoryName is required; format is optional and must not appear.
      const propErrors = (errors.properties || {}) as Record<string, string>;
      expect(propErrors['external.metadata.repository.format']).toBeUndefined();

      service.stop();
    });

    /**
     * NEXUS-53357: when the selected task type exposes formFields with
     * required=true, validateTask must surface per-property errors so the
     * Configure step can block advancement and the form can highlight the
     * empty fields. Without this, users would submit empty required fields
     * and the backend would reject with "Property 'X' not found".
     */
    it('surfaces per-type required-field errors under errors.properties', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) {
          return Promise.resolve([
            {
              type: 'repository.export',
              name: 'Repository - Export assets',
              properties: { repositoryName: '', targetDir: '', exportThreshold: '' },
              formFields: [
                {id: 'repositoryName', type: 'repository', label: 'Source repository', required: true},
                {id: 'targetDir', type: 'string', label: 'Target directory', required: true},
                {id: 'exportThreshold', type: 'number', label: 'Threshold (days)', required: false},
              ],
            },
          ]);
        }
        return Promise.resolve([]);
      });

      const machine = createTaskFormMachine(undefined);
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.export' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Export task' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      const props = (state.context.validationErrors as Record<string, unknown>).properties as
        | Record<string, string>
        | undefined;
      expect(props).toBeDefined();
      // Both fields are flagged required — the descriptive TASK_FIELD_UI-driven message wins
      // over the generic "Required" fallback when both validation passes apply.
      expect(props?.repositoryName).toBe('Repository is required');
      expect(props?.targetDir).toBe('Target Directory is required');
      // Optional field must not produce an error.
      expect(props?.exportThreshold).toBeUndefined();

      service.stop();
    });

    it('clears per-type required-field errors once the fields are filled', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) {
          return Promise.resolve([
            {
              type: 'repository.export',
              name: 'Repository - Export assets',
              properties: { repositoryName: '', targetDir: '' },
              formFields: [
                {id: 'repositoryName', type: 'repository', label: 'Source repository', required: true},
                {id: 'targetDir', type: 'string', label: 'Target directory', required: true},
              ],
            },
          ]);
        }
        return Promise.resolve([]);
      });

      const machine = createTaskFormMachine(undefined);
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.export' } as any);
      service.send({ type: 'UPDATE', name: 'name', value: 'Export task' } as any);
      service.send({
        type: 'UPDATE',
        name: 'properties',
        value: { repositoryName: 'maven-public', targetDir: '/tmp/export' },
      } as any);

      const state = service.getSnapshot();
      const props = (state.context.validationErrors as Record<string, unknown>).properties as
        | Record<string, string>
        | undefined;
      expect(props).toBeUndefined();

      service.stop();
    });
  });

  describe('once schedule carries inherited startDate verbatim', () => {
    // Past-date enforcement was removed (matches classic UI). Switching to "once"
    // simply forwards whatever startDate the previous schedule had.
    it('keeps a past startDate when switching to once', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      const pastDate = new Date(2000, 0, 1);
      service.send({ type: 'SCHEDULE_CHANGE', value: 'monthly' } as any);
      service.send({ type: 'UPDATE', name: 'startDate', value: pastDate } as any);
      service.send({ type: 'SCHEDULE_CHANGE', value: 'once' } as any);

      expect(service.getSnapshot().context.data.startDate).toEqual(pastDate);
      service.stop();
    });

    it('keeps an explicit future startDate when switching to once', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 7);

      service.send({ type: 'SCHEDULE_CHANGE', value: 'monthly' } as any);
      service.send({ type: 'UPDATE', name: 'startDate', value: futureDate } as any);
      service.send({ type: 'SCHEDULE_CHANGE', value: 'once' } as any);

      expect(service.getSnapshot().context.data.startDate).toEqual(futureDate);
      service.stop();
    });
  });

  /**
   * NEXUS-53357: fetchTaskTypes prefers the backend's per-field metadata
   * (template.formFields) over the legacy properties-only synthesis. This
   * test verifies the new path lights up when the metadata is present.
   */
  describe('fetchTaskTypes (per-field metadata)', () => {
    it('consumes template.formFields when the backend provides it', async () => {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) {
          return Promise.resolve([
            {
              type: 'repository.import',
              name: 'Repository - Import external files',
              properties: { repositoryName: '', sourceDir: '', batchSize: '', enableHardLinks: '' },
              formFields: [
                {
                  id: 'repositoryName', type: 'repository', label: 'Target repository', required: true,
                  storeFilters: { type: 'hosted' },
                },
                {
                  id: 'sourceDir', type: 'string', label: 'Source directory', required: true,
                  helpText: 'Absolute path on the host',
                },
                {
                  id: 'batchSize', type: 'number', label: 'Batch Size', required: false,
                  minValue: '1', maxValue: '2147483647', regexValidation: '^[0-9]+$',
                },
                {
                  id: 'enableHardLinks', type: 'checkbox', label: 'Enable Hard Links', required: false,
                },
              ],
            },
          ]);
        }
        return Promise.resolve([]);
      });

      const machine = createTaskFormMachine(undefined);
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const context = service.getSnapshot().context as any;
      const importType = (context.taskTypes as any[]).find((t) => t.id === 'repository.import');
      expect(importType).toBeDefined();
      expect(importType.formFields).toHaveLength(4);
      // Order is preserved.
      expect(importType.formFields.map((f: any) => f.id)).toEqual([
        'repositoryName', 'sourceDir', 'batchSize', 'enableHardLinks',
      ]);
      // Required flag honoured.
      expect(importType.formFields[0].required).toBe(true);
      expect(importType.formFields[2].required).toBe(false);
      // storeFilters threaded through.
      expect(importType.formFields[0].storeFilters).toEqual({ type: 'hosted' });
      // Number metadata threaded through.
      expect(importType.formFields[2].minValue).toBe('1');
      expect(importType.formFields[2].maxValue).toBe('2147483647');

      service.stop();
    });

    it('falls back to TASK_FIELD_UI-enriched synthesis when formFields is absent', async () => {
      // MOCK_TASK_TYPES (above) has no `formFields` arrays — the shared transformer
      // (taskTransformers.restTemplateToTaskType) must still enrich known field ids
      // via TASK_FIELD_UI so EDIT renders the same combobox/required markers as CREATE
      // on backends that don't yet publish formFields metadata.
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      const context = service.getSnapshot().context as any;
      const blobstoreType = (context.taskTypes as any[]).find((t) => t.id === 'blobstore.compact');
      expect(blobstoreType).toBeDefined();
      expect(blobstoreType.formFields).toHaveLength(1);
      expect(blobstoreType.formFields[0].id).toBe('blobstoreName');
      // TASK_FIELD_UI: blobstoreName → type 'blobstore', required by default.
      expect(blobstoreType.formFields[0].type).toBe('blobstore');
      expect(blobstoreType.formFields[0].required).toBe(true);

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates form fields via UPDATE event', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'My Task' } as any);

      expect(service.getSnapshot().context.data.name).toBe('My Task');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'New Task' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'New Task' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });
  });

  describe('edit mode (fetch by taskId)', () => {
    it('loads cron expression and properties from flat API response when fetching by taskId', async () => {
      // The GET /v1/tasks/{id} response is flat (not nested under frequency)
      const flatApiResponse = {
        id: 'cron-task',
        enabled: true,
        name: 'my-repair',
        type: 'create.browse.nodes',
        currentState: 'WAITING',
        schedule: 'advanced',
        cronExpression: '0 0 12 * * ?',
        timeZoneOffset: '+00:00',
        startDate: '2026-01-22T12:00:00.000Z',
        recurringDays: null,
        properties: { repositoryName: 'maven-public' },
        alertEmail: 'admin@example.com',
        notificationCondition: 'FAILURE',
        message: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
      };

      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(MOCK_TASK_TYPES);
        return Promise.resolve(flatApiResponse);
      });

      // No preloadedTask — machine must fetch by taskId
      const machine = createTaskFormMachine('cron-task');
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const ctx = (service.getSnapshot().context as any);
      expect(ctx.data.schedule).toBe('advanced');
      expect(ctx.data.cronExpression).toBe('0 0 12 * * ?');
      expect(ctx.data.timeZoneOffset).toBe('+00:00');
      expect(ctx.data.properties).toEqual({ repositoryName: 'maven-public' });
      expect(ctx.data.alertEmail).toBe('admin@example.com');
      expect(ctx.data.notificationCondition).toBe('FAILURE');

      service.stop();
    });

    /**
     * NEXUS-52435 — regression: GET /v1/tasks/{id} returns the type ID (e.g.
     * "repository.cleanup") under `type`, not the human-readable label. fetchTask
     * sets typeName to that ID as a placeholder; the load service must enrich it
     * with selectedTaskType.name once both task and task types resolve so the
     * header reads "Admin - Cleanup repositories ..." like the classic UI did.
     */
    it('enriches typeName with the resolved task type name after load', async () => {
      const flatApiResponse = {
        id: 'cleanup-task',
        enabled: true,
        name: 'Nightly cleanup',
        type: 'repository.cleanup',
        currentState: 'WAITING',
        schedule: 'manual',
        properties: { repositoryName: 'maven-releases' },
        alertEmail: null,
        notificationCondition: 'FAILURE',
        message: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
      };

      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(MOCK_TASK_TYPES);
        return Promise.resolve(flatApiResponse);
      });

      const machine = createTaskFormMachine('cleanup-task');
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const ctx = service.getSnapshot().context as any;
      expect(ctx.task).toBeTruthy();
      expect(ctx.task.typeId).toBe('repository.cleanup');
      // Mocked task type for repository.cleanup carries the human-readable name.
      expect(ctx.task.typeName).toBe('Admin - Cleanup repositories using cleanup policies');

      service.stop();
    });
  });

  describe('checkbox normalization (APT parity)', () => {
    // APT task type template — template includes both checkbox fields with empty-string defaults
    const APT_TASK_TYPES = [
      ...MOCK_TASK_TYPES,
      {
        type: 'repository.apt.rebuild.metadata',
        name: 'Repository - Rebuild APT repository metadata',
        enabled: true,
        alertEmail: null,
        notificationCondition: 'FAILURE',
        frequency: { schedule: 'manual' },
        properties: {
          repositoryName: '',
          rebuildAptMetadataFullRebuild: '',
          resetProxyMetadata: '',
        },
      },
    ];

    /** Start machine with APT task types loaded */
    async function startWithAptTypes(
      machine: ReturnType<typeof createTaskFormMachine>,
    ) {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(APT_TASK_TYPES);
        return Promise.resolve([]);
      });
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));
      return service;
    }

    it('TASK_TYPE_CHANGE to APT sets both checkbox fields in properties (create flow)', async () => {
      const machine = createTaskFormMachine(undefined);
      const service = await startWithAptTypes(machine);

      service.send({ type: 'TASK_TYPE_CHANGE', value: 'repository.apt.rebuild.metadata' } as any);

      const properties = (service.getSnapshot().context as any).data.properties;
      // Template sends '' for both; machine preserves template value (serializer maps '' → 'false')
      expect('rebuildAptMetadataFullRebuild' in properties).toBe(true);
      expect('resetProxyMetadata' in properties).toBe(true);

      service.stop();
    });

    it('load service normalizes absent APT checkbox fields to false (edit flow)', async () => {
      // Task was saved before these checkbox fields were added — neither is in properties
      const aptTask = {
        id: 'apt-task-old',
        enabled: true,
        name: 'Rebuild APT Metadata',
        typeId: 'repository.apt.rebuild.metadata',
        typeName: 'Repository - Rebuild APT repository metadata',
        status: 'WAITING' as const,
        statusDescription: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
        runnable: true,
        stoppable: false,
        properties: { repositoryName: '*' }, // checkboxes absent
        schedule: 'manual' as const,
        startDate: null,
        recurringDays: [],
        cronExpression: '',
        timeZoneOffset: '',
      };

      const machine = createTaskFormMachine('apt-task-old', aptTask);
      const service = await startWithAptTypes(machine);

      const properties = (service.getSnapshot().context as any).data.properties;
      expect(properties.rebuildAptMetadataFullRebuild).toBe('false');
      expect(properties.resetProxyMetadata).toBe('false');
      // Existing field should be untouched
      expect(properties.repositoryName).toBe('*');

      service.stop();
    });

    it('load service preserves existing APT checkbox values — does not overwrite', async () => {
      const aptTask = {
        id: 'apt-task-set',
        enabled: true,
        name: 'Rebuild APT Metadata',
        typeId: 'repository.apt.rebuild.metadata',
        typeName: 'Repository - Rebuild APT repository metadata',
        status: 'WAITING' as const,
        statusDescription: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
        runnable: true,
        stoppable: false,
        properties: {
          repositoryName: 'my-apt-hosted',
          rebuildAptMetadataFullRebuild: 'true',
          // resetProxyMetadata absent — should be synthesized as 'false'
        },
        schedule: 'manual' as const,
        startDate: null,
        recurringDays: [],
        cronExpression: '',
        timeZoneOffset: '',
      };

      const machine = createTaskFormMachine('apt-task-set', aptTask);
      const service = await startWithAptTypes(machine);

      const properties = (service.getSnapshot().context as any).data.properties;
      expect(properties.rebuildAptMetadataFullRebuild).toBe('true'); // preserved
      expect(properties.resetProxyMetadata).toBe('false');           // synthesized

      service.stop();
    });

    it('load service does not synthesize checkbox fields for non-checkbox task types', async () => {
      const dbBackupTask = {
        id: 'db-task-1',
        enabled: true,
        name: 'DB Backup',
        typeId: 'db.backup',
        typeName: 'Admin - Export databases for backup',
        status: 'WAITING' as const,
        statusDescription: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
        runnable: true,
        stoppable: false,
        properties: { location: '/backup' },
        schedule: 'manual' as const,
        startDate: null,
        recurringDays: [],
        cronExpression: '',
        timeZoneOffset: '',
      };

      const machine = createTaskFormMachine('db-task-1', dbBackupTask);
      const service = await startWithAptTypes(machine);

      const properties = (service.getSnapshot().context as any).data.properties;
      // Only the declared property; no phantom checkboxes added
      expect(properties).toEqual({ location: '/backup' });

      service.stop();
    });

    it('load service does not overwrite false with false (idempotent)', async () => {
      const aptTask = {
        id: 'apt-task-explicit-false',
        enabled: true,
        name: 'Rebuild APT Metadata',
        typeId: 'repository.apt.rebuild.metadata',
        typeName: 'Repository - Rebuild APT repository metadata',
        status: 'WAITING' as const,
        statusDescription: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
        runnable: true,
        stoppable: false,
        properties: {
          repositoryName: '*',
          rebuildAptMetadataFullRebuild: 'false',
          resetProxyMetadata: 'false',
        },
        schedule: 'manual' as const,
        startDate: null,
        recurringDays: [],
        cronExpression: '',
        timeZoneOffset: '',
      };

      const machine = createTaskFormMachine('apt-task-explicit-false', aptTask);
      const service = await startWithAptTypes(machine);

      const properties = (service.getSnapshot().context as any).data.properties;
      expect(properties.rebuildAptMetadataFullRebuild).toBe('false');
      expect(properties.resetProxyMetadata).toBe('false');

      service.stop();
    });
  });

  describe('validateTask — Execute date range', () => {
    const base = (over: any) => ({
      typeId: EXECUTE_RECONCILE_PLAN_TYPE_ID,
      name: 'Repair - Execute Data Repair Plan', enabled: true, schedule: 'manual',
      properties: { taskScope: 'dates', ...over },
    }) as any;

    it('rejects end before start for the Execute task', () => {
      const errors: any = validateTask(base({ reconcileStartDate: '06/10/2026', reconcileEndDate: '06/01/2026' }));
      expect(errors.properties?.reconcileEndDate).toMatch(/on or after start date/);
    });

    it('accepts end on or after start', () => {
      const errors: any = validateTask(base({ reconcileStartDate: '06/01/2026', reconcileEndDate: '06/10/2026' }));
      expect(errors.properties?.reconcileEndDate).toBeUndefined();
    });
  });

describe('edit mode', () => {
  it('loads task data and enters correct schedule sub-state', async () => {
      const preloadedTask = {
        id: 'task-123',
        enabled: true,
        name: 'Daily Cleanup',
        typeId: 'repository.cleanup',
        typeName: 'Admin - Cleanup repositories',
        status: 'WAITING' as const,
        statusDescription: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
        runnable: true,
        stoppable: false,
        properties: {},
        schedule: 'daily' as const,
        startDate: new Date('2025-01-15T03:00:00'),
        recurringDays: [],
        cronExpression: '',
        timeZoneOffset: '+00:00',
      };

      const machine = createTaskFormMachine('task-123', preloadedTask);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'daily' })).toBe(true);
      expect(state.context.data.name).toBe('Daily Cleanup');
      expect(state.context.data.typeId).toBe('repository.cleanup');
      expect(state.context.data.schedule).toBe('daily');

      service.stop();
    });

    it('loads task with weekly schedule correctly', async () => {
      const preloadedTask = {
        id: 'task-456',
        enabled: true,
        name: 'Weekly Backup',
        typeId: 'db.backup',
        typeName: 'Admin - Export databases',
        status: 'WAITING' as const,
        statusDescription: '',
        nextRun: null,
        lastRun: null,
        lastRunResult: null,
        runnable: true,
        stoppable: false,
        properties: { location: '/backup' },
        schedule: 'weekly' as const,
        startDate: new Date('2025-01-15T02:00:00'),
        recurringDays: [2, 4, 6],
        cronExpression: '',
        timeZoneOffset: '-05:00',
      };

      const machine = createTaskFormMachine('task-456', preloadedTask);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.matches({ editing: 'weekly' })).toBe(true);
      expect(state.context.data.recurringDays).toEqual([2, 4, 6]);
      expect(state.context.data.properties.location).toBe('/backup');

      service.stop();
    });
  });

  describe('nexus.cloud.blobstore.removal quirks', () => {
    const CLOUD_BLOBSTORE_REMOVAL_TASK_TYPE = {
      type: 'nexus.cloud.blobstore.removal',
      name: 'Cloud - Remove blob store',
      enabled: true,
      alertEmail: null,
      notificationCondition: 'FAILURE',
      frequency: { schedule: 'once' },
      properties: { blobstoreName: '' },
    };

    const TASK_TYPES_WITH_CLOUD = [...MOCK_TASK_TYPES, CLOUD_BLOBSTORE_REMOVAL_TASK_TYPE];

    async function startWithCloudTypes(machine: ReturnType<typeof createTaskFormMachine>) {
      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(TASK_TYPES_WITH_CLOUD);
        return Promise.resolve([]);
      });
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));
      return service;
    }

    it('clears blobstoreName property for display when loading cloud removal task', async () => {
      const taskWithBlobstore = {
        id: 'cloud-removal-task',
        enabled: true,
        name: 'Remove deleted blob store',
        type: 'nexus.cloud.blobstore.removal',
        currentState: 'WAITING',
        schedule: 'once',
        startDate: null,
        nextRun: new Date('2026-06-27T10:00:00Z'),
        properties: { blobstoreName: 'deleted-blob-store' }, // <-- persisted value
        alertEmail: null,
        notificationCondition: 'FAILURE',
        message: '',
        lastRun: null,
        lastRunResult: null,
      };

      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(TASK_TYPES_WITH_CLOUD);
        return Promise.resolve(taskWithBlobstore);
      });

      const machine = createTaskFormMachine('cloud-removal-task');
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const ctx = service.getSnapshot().context as any;
      // The displayed value should be cleared to avoid showing orphan blob store
      expect(ctx.data.properties.blobstoreName).toBe('');
      // The original task should still carry the persisted value
      expect(ctx.task.properties.blobstoreName).toBe('deleted-blob-store');

      service.stop();
    });

    it('falls back to nextRun when startDate is null for cloud removal task', async () => {
      const nextRunDate = new Date('2026-06-27T10:00:00Z');
      const taskWithoutStartDate = {
        id: 'cloud-removal-task',
        enabled: true,
        name: 'Remove deleted blob store',
        type: 'nexus.cloud.blobstore.removal',
        currentState: 'WAITING',
        schedule: 'once',
        startDate: null, // <-- missing
        nextRun: nextRunDate, // <-- should be used as fallback
        properties: { blobstoreName: 'old-blob-store' },
        alertEmail: null,
        notificationCondition: 'FAILURE',
        message: '',
        lastRun: null,
        lastRunResult: null,
      };

      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(TASK_TYPES_WITH_CLOUD);
        return Promise.resolve(taskWithoutStartDate);
      });

      const machine = createTaskFormMachine('cloud-removal-task');
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const ctx = service.getSnapshot().context as any;
      // startDate should fall back to nextRun
      expect(ctx.data.startDate).toBeTruthy();
      expect(ctx.data.startDate.getTime()).toBe(nextRunDate.getTime());
      // startTime is extracted using local time (getHours/getMinutes), so derive expected value the same way
      const expectedTime = `${String(nextRunDate.getHours()).padStart(2, '0')}:${String(nextRunDate.getMinutes()).padStart(2, '0')}`;
      expect(ctx.data.startTime).toBe(expectedTime);

      service.stop();
    });

    it('does not fall back to nextRun for non-cloud tasks (regression guard)', async () => {
      const nextRunDate = new Date('2026-06-27T10:00:00Z');
      const regularTask = {
        id: 'regular-task',
        enabled: true,
        name: 'Regular Task',
        type: 'repository.cleanup',
        currentState: 'WAITING',
        schedule: 'once',
        startDate: null,
        nextRun: nextRunDate,
        properties: { repositoryName: 'maven-releases' },
        alertEmail: null,
        notificationCondition: 'FAILURE',
        message: '',
        lastRun: null,
        lastRunResult: null,
      };

      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(TASK_TYPES_WITH_CLOUD);
        return Promise.resolve(regularTask);
      });

      const machine = createTaskFormMachine('regular-task');
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const ctx = service.getSnapshot().context as any;
      // startDate should remain null (no fallback)
      expect(ctx.data.startDate).toBeNull();

      service.stop();
    });

    it('blocks Save when blobstoreName is empty for cloud removal task', async () => {
      const taskWithEmptyBlobstore = {
        id: 'cloud-removal-task',
        enabled: true,
        name: 'Remove deleted blob store',
        type: 'nexus.cloud.blobstore.removal',
        currentState: 'WAITING',
        schedule: 'once',
        startDate: new Date('2026-06-27T10:00:00Z'),
        nextRun: new Date('2026-06-27T10:00:00Z'),
        properties: { blobstoreName: '' }, // <-- empty triggers validation
        alertEmail: null,
        notificationCondition: 'FAILURE',
        message: '',
        lastRun: null,
        lastRunResult: null,
      };

      restClient.get.mockImplementation((url: string) => {
        if (url.includes('templates')) return Promise.resolve(TASK_TYPES_WITH_CLOUD);
        return Promise.resolve(taskWithEmptyBlobstore);
      });

      const machine = createTaskFormMachine('cloud-removal-task');
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      service.send({ type: 'UPDATE', name: 'name', value: 'Updated Name' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      const errors = state.context.validationErrors as any;
      // Validation should fail because blobstoreName is required (not exempted)
      expect(errors.properties?.blobstoreName).toContain('required');

      service.stop();
    });
  });
});

describe('create mode — Execute Data Repair Plan pre-fetches Plan task fields on type selection', () => {
  it('populates blobstoreName/repositoryName/taskScope when EXECUTE_RECONCILE_PLAN_TYPE_ID is selected', async () => {
    restClient.get.mockImplementation((url: string) => {
      if (url.includes('templates')) {
        return Promise.resolve(MOCK_TASK_TYPES);
      }
      // GET /v1/tasks (list) — Plan task is on the first page
      if (url === '/service/rest/v1/tasks') {
        return Promise.resolve({
          items: [
            {
              type: 'blobstore.planReconciliation',
              properties: {
                blobstoreName: 'default',
                repositoryName: 'maven-central',
                taskScope: 'duration',
                sinceDays: 'null',
                sinceHours: 'null',
                sinceMinutes: '30',
              },
            },
          ],
        });
      }
      return Promise.resolve({});
    });

    // Create mode: no taskId
    const machine = createTaskFormMachine(undefined);
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('editing'));

    // Before type selection — fields are empty
    expect(service.getSnapshot().context.data.typeId).toBe('');

    // Select the Execute task type
    service.send({ type: 'TASK_TYPE_CHANGE', value: EXECUTE_RECONCILE_PLAN_TYPE_ID } as any);

    const props = service.getSnapshot().context.data.properties;
    expect(props.blobstoreName).toBe('default');
    expect(props.repositoryName).toBe('maven-central');
    expect(props.taskScope).toBe('dates');
    expect(props.reconcileStartDate).toBeTruthy();
    expect(props.reconcileEndDate).toBeTruthy();

    service.stop();
  });

  it('leaves fields empty when no Plan task exists', async () => {
    restClient.get.mockImplementation((url: string) => {
      if (url.includes('templates')) return Promise.resolve(MOCK_TASK_TYPES);
      // No Plan task in the list
      return Promise.resolve({ items: [] });
    });

    const machine = createTaskFormMachine(undefined);
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('editing'));

    service.send({ type: 'TASK_TYPE_CHANGE', value: EXECUTE_RECONCILE_PLAN_TYPE_ID } as any);

    const props = service.getSnapshot().context.data.properties;
    expect(props.blobstoreName).toBeUndefined();
    expect(props.repositoryName).toBeUndefined();

    service.stop();
  });
});

describe('load — Execute Data Repair Plan derives blob store / repository from the Plan task', () => {
  it('synthesizes blobstoreName/repositoryName/taskScope from the sibling Data Repair Plan task', async () => {
    restClient.get.mockImplementation((url: string) => {
      if (url.includes('templates')) {
        return Promise.resolve(MOCK_TASK_TYPES);
      }
      if (url.endsWith('/tasks')) {
        // The list endpoint carries per-item properties; the Plan task has the real selection.
        return Promise.resolve({
          items: [
            {
              type: 'blobstore.planReconciliation',
              properties: {
                blobstoreName: 'default',
                repositoryName: 'maven-central',
                taskScope: 'duration',
                sinceDays: 'null',
                sinceHours: 'null',
                sinceMinutes: '30',
              },
            },
          ],
        });
      }
      // GET /v1/tasks/{id}: the Execute task itself stores no blob store / repository (only planIds).
      return Promise.resolve({
        id: 'exec-1',
        type: EXECUTE_RECONCILE_PLAN_TYPE_ID,
        name: 'Repair - Execute Data Repair Plan',
        enabled: true,
        currentState: 'WAITING',
        properties: {},
        schedule: 'manual',
      });
    });

    const machine = createTaskFormMachine('exec-1');
    const service = interpret(machine).start();
    await waitFor(service, (state) => state.matches('editing'));

    const props = service.getSnapshot().context.data.properties;
    expect(props.blobstoreName).toBe('default');
    expect(props.repositoryName).toBe('maven-central');
    expect(props.taskScope).toBe('dates');
    // Plan used a duration → a computed start/end date range is present.
    expect(props.reconcileStartDate).toBeTruthy();
    expect(props.reconcileEndDate).toBeTruthy();

    service.stop();
  });
});
