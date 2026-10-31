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

import { humanizePropertyKey, restTemplateToTaskType, RestTaskTemplate, mapRestStateToStatus, deriveExecutePlanProperties } from '../taskTransformers';

// NEXUS-53525: a single shared normalization of the backend `currentState` enum,
// used by BOTH useTasksApi.fetchTask (initial load + polling) and the form
// machine's fetchTask — so a page load and a poll can never disagree on status.
describe('mapRestStateToStatus (shared normalization)', () => {
  it('collapses the whole running group to RUNNING', () => {
    expect(mapRestStateToStatus('RUNNING')).toBe('RUNNING');
    expect(mapRestStateToStatus('RUNNING_STARTING')).toBe('RUNNING');
    expect(mapRestStateToStatus('RUNNING_BLOCKED')).toBe('RUNNING');
    expect(mapRestStateToStatus('RUNNING_CANCELED')).toBe('RUNNING');
  });

  it('strips a progress suffix and still maps to RUNNING', () => {
    expect(mapRestStateToStatus('RUNNING: 42 of 100')).toBe('RUNNING');
    expect(mapRestStateToStatus('RUNNING: 7%')).toBe('RUNNING');
  });

  it('maps WAITING and the DONE-group terminal states', () => {
    expect(mapRestStateToStatus('WAITING')).toBe('WAITING');
    expect(mapRestStateToStatus('OK')).toBe('OK');
    expect(mapRestStateToStatus('DONE')).toBe('OK');
    expect(mapRestStateToStatus('FAILED')).toBe('FAILED');
    expect(mapRestStateToStatus('CANCELED')).toBe('CANCELED');
    expect(mapRestStateToStatus('INTERRUPTED')).toBe('INTERRUPTED');
  });

  it('defaults empty/unknown states to WAITING', () => {
    expect(mapRestStateToStatus('')).toBe('WAITING');
    expect(mapRestStateToStatus('SOMETHING_ELSE')).toBe('WAITING');
  });
});

function makeTemplate(overrides: Partial<RestTaskTemplate>): RestTaskTemplate {
  return {
    type: 'test.task',
    name: 'Test Task',
    enabled: true,
    notificationCondition: 'FAILURE',
    properties: {},
    ...overrides,
  };
}

describe('humanizePropertyKey', () => {
  it('converts camelCase to spaced Title Case', () => {
    expect(humanizePropertyKey('lastUsed')).toBe('Last Used');
    expect(humanizePropertyKey('repositoryName')).toBe('Repository Name');
  });

  it('uppercases the first letter of single-word ids', () => {
    expect(humanizePropertyKey('age')).toBe('Age');
  });
});

describe('restTemplateToTaskType — shared between CREATE (useTasksApi) and EDIT (tasksFormMachine)', () => {
  it('preserves descriptor field order via Object.entries on a LinkedHashMap-serialized payload', () => {
    // PurgeUnusedTaskDescriptor declares repositoryName before lastUsed. With the backend
    // LinkedHashMap fix, the JSON keeps that order, and Object.entries iterates it
    // verbatim — so the wizard renders Repository above Last Used in BOTH create and edit.
    const result = restTemplateToTaskType(
      makeTemplate({
        type: 'repository.purge-unused',
        name: 'Repository - Delete unused components',
        properties: { repositoryName: '', lastUsed: '1' },
      }),
    );
    const ids = (result.formFields ?? []).map((f) => f.id);
    expect(ids).toEqual(['repositoryName', 'lastUsed']);
  });

  it('applies TASK_FIELD_UI metadata to known fields (label/type/required/initialValue)', () => {
    const result = restTemplateToTaskType(
      makeTemplate({
        type: 'repository.purge-unused',
        properties: { repositoryName: '', lastUsed: '7' },
      }),
    );
    const repo = result.formFields!.find((f) => f.id === 'repositoryName')!;
    const lastUsed = result.formFields!.find((f) => f.id === 'lastUsed')!;

    // Repository: type=repo (combobox), required=true (default), label uses metadata
    expect(repo.type).toBe('repo');
    expect(repo.required).toBe(true);
    expect(repo.label).toBe('Repository *');

    // lastUsed: numeric, label from metadata
    expect(lastUsed.type).toBe('number');
    expect(lastUsed.label).toBe('Last Used (days) *');
    // Backend value wins over the metadata placeholder
    expect(lastUsed.initialValue).toBe('7');
  });

  it('renders external.metadata.repository.format as optional text input (not a repo combobox)', () => {
    // Regression: when a task is loaded for EDIT, the previous bare-bones machine
    // mapper auto-detected this id as type=repo and forced required=true. The shared
    // transformer must respect TASK_FIELD_UI so EDIT and CREATE behave identically.
    const result = restTemplateToTaskType(
      makeTemplate({
        type: 'external.blobstore.metadata',
        properties: { repositoryName: 'maven-central', 'external.metadata.repository.format': '' },
      }),
    );
    const formatField = result.formFields!.find(
      (f) => f.id === 'external.metadata.repository.format',
    )!;

    expect(formatField.type).toBe('string');
    expect(formatField.required).toBe(false);
    // No trailing asterisk because it's optional
    expect(formatField.label).toBe('Repository format');
  });

  it('does not leak meta.placeholder into initialValue when the backend sends an empty string', () => {
    // Regression: previous fallback `value || meta.placeholder || ''` made hint text
    // like "e.g. maven2" become the actual stored value when the descriptor declared
    // no withInitialValue() (e.g. ExternalMetadataTask's Repository format field).
    const result = restTemplateToTaskType(
      makeTemplate({
        type: 'external.blobstore.metadata',
        properties: { 'external.metadata.repository.format': '' },
      }),
    );
    const formatField = result.formFields!.find(
      (f) => f.id === 'external.metadata.repository.format',
    )!;
    expect(formatField.initialValue).toBe('');
  });

  it('preserves concurrentRun=false (used by EDIT to restrict schedule options)', () => {
    const result = restTemplateToTaskType(
      makeTemplate({
        type: 'repository.move',
        concurrentRun: false,
      }),
    );
    expect(result.concurrentRun).toBe(false);
  });

  it('returns formFields=undefined when properties is empty', () => {
    // The form machine reads `selectedTaskType.formFields?.length > 0` — both null and
    // undefined are falsy, but undefined matches the TaskType.formFields?: optional shape.
    const result = restTemplateToTaskType(makeTemplate({ properties: {} }));
    expect(result.formFields).toBeUndefined();
  });

  it('hides server-managed fields flagged hidden in TASK_FIELD_UI', () => {
    // moveInitialBlobstore is set by the backend at runtime — never user-editable.
    const result = restTemplateToTaskType(
      makeTemplate({
        type: 'repository.move',
        properties: {
          moveRepositoryName: '',
          moveTargetBlobstore: '',
          moveInitialBlobstore: 'default',
        },
      }),
    );
    const ids = (result.formFields ?? []).map((f) => f.id);
    expect(ids).toContain('moveRepositoryName');
    expect(ids).toContain('moveTargetBlobstore');
    expect(ids).not.toContain('moveInitialBlobstore');
  });

  describe('NEXUS-53360 ScriptTask + H2BackupTask field mapping', () => {
    it('maps script fields: source=text (not checkbox), language=string, multinode=optional checkbox', () => {
      // Mirrors the /v1/tasks/templates payload for the `script` type: language has the
      // descriptor's default value, source is empty (MANDATORY, no withInitialValue), and
      // multinode is the clustered checkbox.
      const result = restTemplateToTaskType(
        makeTemplate({
          type: 'script',
          name: 'Admin - Execute script',
          properties: { language: 'groovy', source: '', multinode: 'false' },
        }),
      );
      const source = result.formFields!.find((f) => f.id === 'source')!;
      const language = result.formFields!.find((f) => f.id === 'language')!;
      const multinode = result.formFields!.find((f) => f.id === 'multinode')!;

      // The key regression: an empty value must NOT downgrade source to a checkbox.
      expect(source.type).toBe('text');
      expect(source.type).not.toBe('checkbox');
      expect(source.required).toBe(true);

      expect(language.type).toBe('string');
      expect(language.required).toBe(true);
      expect(language.initialValue).toBe('groovy');

      // Checkboxes are never user-required, regardless of the value sent.
      expect(multinode.type).toBe('checkbox');
      expect(multinode.required).toBe(false);
    });

    it('maps the H2BackupTask location field as a required string path', () => {
      const result = restTemplateToTaskType(
        makeTemplate({
          type: 'h2.backup.task',
          name: 'Admin - Backup H2 Database',
          properties: { location: '' },
        }),
      );
      const location = result.formFields!.find((f) => f.id === 'location')!;
      expect(location.type).toBe('string');
      expect(location.required).toBe(true);
    });
  });

  describe('Data Repair Plan template (blobstore.planReconciliation)', () => {
    // Mirrors TaskTemplateXO.toTaskTemplateXO for the self-hosted descriptor: every form field id
    // becomes a property key set to its initial value as a string.
    const selfHostedTemplate = () =>
      makeTemplate({
        type: 'blobstore.planReconciliation',
        name: 'Repair - Data Repair Plan',
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
      });

    it('types the banners as alertBanner and never marks them required', () => {
      const result = restTemplateToTaskType(selfHostedTemplate());
      const top = result.formFields!.find((f) => f.id === 'topAlertBanner')!;
      const bottom = result.formFields!.find((f) => f.id === 'bottomAlertBanner')!;
      expect(top.type).toBe('alertBanner');
      expect(top.required).toBe(false);
      expect(bottom.type).toBe('alertBanner');
      expect(bottom.required).toBe(false);
    });

    it('keeps taskScope typed as a radio without the spurious heuristic required flag', () => {
      const result = restTemplateToTaskType(selfHostedTemplate());
      const taskScope = result.formFields!.find((f) => f.id === 'taskScope')!;
      expect(taskScope.type).toBe('taskScope');
      // taskScope IS required by the descriptor, but it always carries a value so it never blocks.
      expect(taskScope.required).toBe(true);
      expect(taskScope.initialValue).toBe('duration');
    });

    it('filters out the hidden name template field from the rendered form', () => {
      const result = restTemplateToTaskType(selfHostedTemplate());
      expect(result.formFields!.find((f) => f.id === 'name')).toBeUndefined();
    });

    it('treats the optional timespan and selector fields as not required', () => {
      const result = restTemplateToTaskType(selfHostedTemplate());
      const byId = (id: string) => result.formFields!.find((f) => f.id === id)!;
      expect(byId('onlyNotify').required).toBe(false);
      expect(byId('blobstoreName').required).toBe(false);
      expect(byId('repositoryName').required).toBe(false);
      expect(byId('sinceMinutes').required).toBe(false);
      expect(byId('reconcileStartDate').required).toBe(false);
      expect(byId('reconcileStartDate').type).toBe('date');
    });
  });
});

describe('deriveExecutePlanProperties (Execute Data Repair Plan ← Plan task)', () => {
  // UTC noon so the date arithmetic is deterministic regardless of the test machine's timezone.
  const NOW = new Date('2026-06-24T12:00:00Z');

  it('copies blob store / repository and forces taskScope to dates', () => {
    const r = deriveExecutePlanProperties(
      { blobstoreName: 'default', repositoryName: 'maven-central', taskScope: 'duration', sinceMinutes: '30' },
      NOW
    );
    expect(r.blobstoreName).toBe('default');
    expect(r.repositoryName).toBe('maven-central');
    expect(r.taskScope).toBe('dates');
  });

  it('computes now-duration … now when the plan used a duration (days)', () => {
    const r = deriveExecutePlanProperties(
      { blobstoreName: 'default', repositoryName: 'maven-central', sinceDays: '1', sinceHours: '0', sinceMinutes: '0' },
      NOW
    );
    expect(r.reconcileEndDate).toBe('06/24/2026');
    expect(r.reconcileStartDate).toBe('06/23/2026');
  });

  it('treats non-numeric duration parts (e.g. "null") as 0', () => {
    const r = deriveExecutePlanProperties(
      { blobstoreName: 'b', repositoryName: 'r', sinceDays: 'null', sinceHours: 'null', sinceMinutes: '30' },
      NOW
    );
    // 30 minutes before UTC noon is the same day
    expect(r.reconcileStartDate).toBe('06/24/2026');
    expect(r.reconcileEndDate).toBe('06/24/2026');
  });

  it('copies explicit plan dates when present (no compute)', () => {
    const r = deriveExecutePlanProperties(
      { blobstoreName: 'b', repositoryName: 'r', reconcileStartDate: '01/02/2026', reconcileEndDate: '03/04/2026' },
      NOW
    );
    expect(r.reconcileStartDate).toBe('01/02/2026');
    expect(r.reconcileEndDate).toBe('03/04/2026');
  });

  it('returns {} for a missing plan and drops empty repository key', () => {
    expect(deriveExecutePlanProperties(null, NOW)).toEqual({});
    const r = deriveExecutePlanProperties({ blobstoreName: '', repositoryName: '', sinceMinutes: '0' }, NOW);
    expect(r.repositoryName).toBeUndefined();
    expect(r.taskScope).toBe('dates');
  });

  it('defaults blobstoreName to "(All Blob Stores)" when plan has no specific blob store', () => {
    const withEmpty = deriveExecutePlanProperties({ blobstoreName: '', repositoryName: '', sinceMinutes: '0' }, NOW);
    expect(withEmpty.blobstoreName).toBe('(All Blob Stores)');

    const withUndefined = deriveExecutePlanProperties({ repositoryName: 'r', reconcileStartDate: '01/01/2026', reconcileEndDate: '01/31/2026' }, NOW);
    expect(withUndefined.blobstoreName).toBe('(All Blob Stores)');

    const withSentinel = deriveExecutePlanProperties({ blobstoreName: '(All Blob Stores)', repositoryName: '', sinceMinutes: '0' }, NOW);
    expect(withSentinel.blobstoreName).toBe('(All Blob Stores)');
  });
});

describe('restTemplateToTaskType — malware.remediator banners + selector (NEXUS-53359)', () => {
  // Field order mirrors MalwareRemediatorTaskDescriptor.getFormFields(); the REST
  // template serializes each as fieldId -> "" (no value, no type).
  const malwareTemplate = makeTemplate({
    type: 'malware.remediator',
    name: 'Automatic Malware Management',
    properties: {
      malwareRemediatorTaskRequirements: '',
      repositoryName: '',
      enableMalwareCleanup: '',
      enableMalwareCleanupMessage: '',
    },
  });

  it('keeps descriptor field order', () => {
    const ids = (restTemplateToTaskType(malwareTemplate).formFields ?? []).map((f) => f.id);
    expect(ids).toEqual([
      'malwareRemediatorTaskRequirements',
      'repositoryName',
      'enableMalwareCleanup',
      'enableMalwareCleanupMessage',
    ]);
  });

  it('maps the two PanelMessage fields to alertBanner and the others to repo/checkbox', () => {
    const byId = Object.fromEntries(
      (restTemplateToTaskType(malwareTemplate).formFields ?? []).map((f) => [f.id, f]),
    );
    expect(byId.malwareRemediatorTaskRequirements.type).toBe('alertBanner');
    expect(byId.repositoryName.type).toBe('repo');
    expect(byId.enableMalwareCleanup.type).toBe('checkbox');
    expect(byId.enableMalwareCleanupMessage.type).toBe('alertBanner');
  });

  it('does not misdetect the empty-valued message fields as checkboxes', () => {
    const byId = Object.fromEntries(
      (restTemplateToTaskType(malwareTemplate).formFields ?? []).map((f) => [f.id, f]),
    );
    expect(byId.malwareRemediatorTaskRequirements.type).not.toBe('checkbox');
    expect(byId.enableMalwareCleanupMessage.type).not.toBe('checkbox');
  });
});
