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

import { humanizePropertyKey, restTemplateToTaskType, RestTaskTemplate } from '../taskTransformers';

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
});
