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
import {
  createCleanupPolicyFormMachine,
  validateCleanupPolicy,
} from '../cleanupPolicyFormMachine';
import { EMPTY_CLEANUP_POLICY, RELEASE_TYPES, type CleanupPolicy, type CleanupPolicyFormData, type FormatCriteria } from '../types';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
  };
});

/**
 * Sample format criteria for testing
 */
const SAMPLE_FORMAT_CRITERIA: FormatCriteria[] = [
  {
    id: 'maven2',
    name: 'Maven2',
    availableCriteria: ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex'],
  },
  {
    id: 'npm',
    name: 'npm',
    availableCriteria: ['lastBlobUpdated', 'lastDownloaded', 'regex'],
  },
  {
    id: 'docker',
    name: 'Docker',
    availableCriteria: ['lastBlobUpdated', 'lastDownloaded', 'regex'],
  },
  {
    id: 'raw',
    name: 'Raw',
    availableCriteria: ['lastBlobUpdated', 'lastDownloaded', 'regex'],
  },
];

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createCleanupPolicyFormMachine>
) {
  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

/**
 * Build a preloaded cleanup policy for edit mode tests
 */
function buildPreloadedPolicy(overrides: Partial<CleanupPolicy> = {}): CleanupPolicy {
  return {
    name: 'maven-cleanup',
    format: 'maven2',
    notes: 'Clean old snapshots',
    criteriaLastBlobUpdated: 30,
    criteriaLastDownloaded: null,
    criteriaReleaseType: 'PRERELEASES',
    criteriaAssetRegex: null,
    retain: null,
    sortBy: null,
    inUseCount: 2,
    ...overrides,
  };
}

describe('cleanupPolicyFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('initializes with empty form data', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('');
      expect(state.context.data.format).toBe('');
      expect(state.context.data.notes).toBe('');
      expect(state.context.data.criteriaLastBlobUpdated).toBeNull();

      service.stop();
    });

    it('initializes all criteria as disabled', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.criteriaEnabled.lastBlobUpdated).toBe(false);
      expect(state.context.criteriaEnabled.lastDownloaded).toBe(false);
      expect(state.context.criteriaEnabled.assetRegex).toBe(false);
      expect(state.context.criteriaEnabled.retain).toBe(false);

      service.stop();
    });

    it('stores format criteria reference data', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.formatCriteria).toHaveLength(4);
      expect(state.context.formatCriteria[0].id).toBe('maven2');

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads preloaded policy data into form', async () => {
      const policy = buildPreloadedPolicy();
      const machine = createCleanupPolicyFormMachine(
        'maven-cleanup',
        policy,
        SAMPLE_FORMAT_CRITERIA
      );
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('maven-cleanup');
      expect(state.context.data.format).toBe('maven2');
      expect(state.context.data.notes).toBe('Clean old snapshots');
      expect(state.context.data.criteriaLastBlobUpdated).toBe(30);
      expect(state.context.data.criteriaReleaseType).toBe('PRERELEASES');

      service.stop();
    });

    it('enables criteria flags based on preloaded policy values', async () => {
      const policy = buildPreloadedPolicy({
        criteriaLastBlobUpdated: 30,
        criteriaLastDownloaded: 14,
        criteriaAssetRegex: '.*-SNAPSHOT.*',
      });
      const machine = createCleanupPolicyFormMachine(
        'maven-cleanup',
        policy,
        SAMPLE_FORMAT_CRITERIA
      );
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.criteriaEnabled.lastBlobUpdated).toBe(true);
      expect(state.context.criteriaEnabled.lastDownloaded).toBe(true);
      expect(state.context.criteriaEnabled.assetRegex).toBe(true);
      expect(state.context.criteriaEnabled.retain).toBe(false);

      service.stop();
    });

    it('preserves policy reference in context', async () => {
      const policy = buildPreloadedPolicy();
      const machine = createCleanupPolicyFormMachine(
        'maven-cleanup',
        policy,
        SAMPLE_FORMAT_CRITERIA
      );
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.policy).not.toBeNull();
      expect(service.getSnapshot().context.policy?.name).toBe('maven-cleanup');

      service.stop();
    });
  });

  describe('basic validation', () => {
    it('validates name is required', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates name format (alphanumeric, hyphens, underscores, dots)', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'invalid name!' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toContain('letters');

      service.stop();
    });

    it('validates format is required', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-policy' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.format).toBeTruthy();

      service.stop();
    });

    it('validates at least one criterion is selected when format is set', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-policy' } as any);
      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.criteriaSelected).toBeTruthy();

      service.stop();
    });
  });

  describe('FORMAT_CHANGE event', () => {
    it('changes format and resets all criteria', async () => {
      const policy = buildPreloadedPolicy({
        criteriaLastBlobUpdated: 30,
        criteriaAssetRegex: '.*-SNAPSHOT.*',
      });
      const machine = createCleanupPolicyFormMachine(
        'test',
        policy,
        SAMPLE_FORMAT_CRITERIA
      );
      const service = await startAndLoad(machine);

      // Verify initial state
      expect(service.getSnapshot().context.data.format).toBe('maven2');
      expect(service.getSnapshot().context.criteriaEnabled.lastBlobUpdated).toBe(true);

      // Change format
      service.send({ type: 'FORMAT_CHANGE', value: 'npm' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.format).toBe('npm');
      expect(state.context.data.criteriaLastBlobUpdated).toBeNull();
      expect(state.context.data.criteriaLastDownloaded).toBeNull();
      expect(state.context.data.criteriaAssetRegex).toBeNull();
      expect(state.context.data.criteriaReleaseType).toBeNull();
      expect(state.context.data.retain).toBeNull();

      // All criteria should be disabled
      expect(state.context.criteriaEnabled.lastBlobUpdated).toBe(false);
      expect(state.context.criteriaEnabled.lastDownloaded).toBe(false);
      expect(state.context.criteriaEnabled.assetRegex).toBe(false);
      expect(state.context.criteriaEnabled.retain).toBe(false);

      service.stop();
    });
  });

  describe('TOGGLE_CRITERIA event', () => {
    it('enables a criteria checkbox', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: true,
      } as any);

      const state = service.getSnapshot();
      expect(state.context.criteriaEnabled.lastBlobUpdated).toBe(true);

      service.stop();
    });

    it('disables a criteria checkbox and clears its value', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);

      // Enable and set value
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: true,
      } as any);
      service.send({
        type: 'UPDATE',
        name: 'criteriaLastBlobUpdated',
        value: 30,
      } as any);

      expect(service.getSnapshot().context.data.criteriaLastBlobUpdated).toBe(30);

      // Disable
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: false,
      } as any);

      const state = service.getSnapshot();
      expect(state.context.criteriaEnabled.lastBlobUpdated).toBe(false);
      expect(state.context.data.criteriaLastBlobUpdated).toBeNull();

      service.stop();
    });

    it('disabling assetRegex clears the regex value', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'assetRegex',
        enabled: true,
      } as any);
      service.send({
        type: 'UPDATE',
        name: 'criteriaAssetRegex',
        value: '.*-SNAPSHOT.*',
      } as any);

      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'assetRegex',
        enabled: false,
      } as any);

      expect(service.getSnapshot().context.data.criteriaAssetRegex).toBeNull();

      service.stop();
    });

    it('disabling retain clears retain and sortBy values', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'retain',
        enabled: true,
      } as any);
      service.send({ type: 'UPDATE', name: 'retain', value: 5 } as any);

      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'retain',
        enabled: false,
      } as any);

      const state = service.getSnapshot();
      expect(state.context.data.retain).toBeNull();
      expect(state.context.data.sortBy).toBeNull();

      service.stop();
    });

    it('enabling retain sets default sortBy for maven2', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'retain',
        enabled: true,
      } as any);

      expect(service.getSnapshot().context.data.sortBy).toBe('version');

      service.stop();
    });

    it('enabling retain sets default sortBy for docker', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'docker' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'retain',
        enabled: true,
      } as any);

      expect(service.getSnapshot().context.data.sortBy).toBe('date');

      service.stop();
    });

    it('disabling last other criterion on docker clears retain state', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'docker' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: true,
      } as any);
      service.send({ type: 'UPDATE', name: 'criteriaLastBlobUpdated', value: 30 } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'retain',
        enabled: true,
      } as any);
      service.send({ type: 'UPDATE', name: 'retain', value: 5 } as any);

      expect(service.getSnapshot().context.criteriaEnabled.retain).toBe(true);
      expect(service.getSnapshot().context.data.retain).toBe(5);

      // Disable the only other criterion — retain should be cleared
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: false,
      } as any);

      const state = service.getSnapshot();
      expect(state.context.criteriaEnabled.retain).toBe(false);
      expect(state.context.data.retain).toBeNull();
      expect(state.context.data.sortBy).toBeNull();

      service.stop();
    });
  });

  describe('RELEASE_TYPE_CHANGE event', () => {
    it('updates release type in form data', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'RELEASE_TYPE_CHANGE',
        value: RELEASE_TYPES.RELEASES.id,
      } as any);

      expect(service.getSnapshot().context.data.criteriaReleaseType).toBe(
        RELEASE_TYPES.RELEASES.id
      );

      service.stop();
    });

    it('disables retain when release type is not RELEASES', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);

      // Set to RELEASES first (enables retain eligibility)
      service.send({
        type: 'RELEASE_TYPE_CHANGE',
        value: RELEASE_TYPES.RELEASES.id,
      } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'retain',
        enabled: true,
      } as any);
      service.send({ type: 'UPDATE', name: 'retain', value: 5 } as any);

      expect(service.getSnapshot().context.criteriaEnabled.retain).toBe(true);

      // Change to PRERELEASES (should disable retain)
      service.send({
        type: 'RELEASE_TYPE_CHANGE',
        value: RELEASE_TYPES.PRERELEASES.id,
      } as any);

      const state = service.getSnapshot();
      expect(state.context.criteriaEnabled.retain).toBe(false);
      expect(state.context.data.retain).toBeNull();
      expect(state.context.data.sortBy).toBeNull();

      service.stop();
    });

    it('clears release type when empty string', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'RELEASE_TYPE_CHANGE',
        value: RELEASE_TYPES.RELEASES.id,
      } as any);

      service.send({ type: 'RELEASE_TYPE_CHANGE', value: '' } as any);

      expect(service.getSnapshot().context.data.criteriaReleaseType).toBeNull();

      service.stop();
    });
  });

  describe('criteria validation', () => {
    it('validates lastBlobUpdated when enabled', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-policy' } as any);
      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: true,
      } as any);
      // Don't set a value
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.criteriaLastBlobUpdated).toBeTruthy();

      service.stop();
    });

    it('validates assetRegex pattern when enabled', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-policy' } as any);
      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'assetRegex',
        enabled: true,
      } as any);
      service.send({
        type: 'UPDATE',
        name: 'criteriaAssetRegex',
        value: '[invalid',
      } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.criteriaAssetRegex).toBeTruthy();

      service.stop();
    });

    it('does not validate disabled criteria', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-policy' } as any);
      service.send({ type: 'FORMAT_CHANGE', value: 'maven2' } as any);

      // Enable one criterion so the "at least one" check passes
      service.send({
        type: 'TOGGLE_CRITERIA',
        criteria: 'lastBlobUpdated',
        enabled: true,
      } as any);
      service.send({
        type: 'UPDATE',
        name: 'criteriaLastBlobUpdated',
        value: 30,
      } as any);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      // Disabled criteria should not have errors
      expect(state.context.validationErrors.criteriaLastDownloaded).toBeFalsy();
      expect(state.context.validationErrors.criteriaAssetRegex).toBeFalsy();
      expect(state.context.validationErrors.retain).toBeFalsy();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates fields via UPDATE event', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-policy' } as any);
      service.send({ type: 'UPDATE', name: 'notes', value: 'Test notes' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('my-policy');
      expect(state.context.data.notes).toBe('Test notes');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-policy' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createCleanupPolicyFormMachine(undefined, null, SAMPLE_FORMAT_CRITERIA);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'changed' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });
  });

  describe('validateCleanupPolicy function', () => {
    const defaultEnabled = {
      lastBlobUpdated: false,
      lastDownloaded: false,
      assetRegex: false,
      retain: false,
    };

    it('returns no errors for valid data with enabled criteria', () => {
      const data: CleanupPolicyFormData = {
        name: 'valid-policy',
        format: 'maven2',
        notes: '',
        criteriaLastBlobUpdated: 30,
        criteriaLastDownloaded: null,
        criteriaReleaseType: null,
        criteriaAssetRegex: null,
        retain: null,
        sortBy: null,
      };
      const enabled = { ...defaultEnabled, lastBlobUpdated: true };
      const errors = validateCleanupPolicy(data, enabled, SAMPLE_FORMAT_CRITERIA);
      const activeErrors = Object.keys(errors).filter((k) => errors[k]);
      expect(activeErrors).toHaveLength(0);
    });

    it('returns error for empty name', () => {
      const data = { ...EMPTY_CLEANUP_POLICY, format: 'maven2' };
      const enabled = { ...defaultEnabled, lastBlobUpdated: true };
      const errors = validateCleanupPolicy(data, enabled, SAMPLE_FORMAT_CRITERIA);
      expect(errors.name).toBeTruthy();
    });

    it('returns error for missing format', () => {
      const data = { ...EMPTY_CLEANUP_POLICY, name: 'test' };
      const errors = validateCleanupPolicy(data, defaultEnabled, SAMPLE_FORMAT_CRITERIA);
      expect(errors.format).toBeTruthy();
    });

    it('returns error for notes exceeding max length', () => {
      const data = {
        ...EMPTY_CLEANUP_POLICY,
        name: 'test',
        format: 'maven2',
        notes: 'a'.repeat(401),
      };
      const enabled = { ...defaultEnabled, lastBlobUpdated: true };
      const errors = validateCleanupPolicy(data, enabled, SAMPLE_FORMAT_CRITERIA);
      expect(errors.notes).toBeTruthy();
    });

    it('returns error for invalid regex pattern', () => {
      const data = {
        ...EMPTY_CLEANUP_POLICY,
        name: 'test',
        format: 'maven2',
        criteriaAssetRegex: '[invalid',
      };
      const enabled = { ...defaultEnabled, assetRegex: true };
      const errors = validateCleanupPolicy(data, enabled, SAMPLE_FORMAT_CRITERIA);
      expect(errors.criteriaAssetRegex).toBeTruthy();
    });

    it('returns error for invalid criteria number (out of range)', () => {
      const data = {
        ...EMPTY_CLEANUP_POLICY,
        name: 'test',
        format: 'maven2',
        criteriaLastBlobUpdated: 99999,
      };
      const enabled = { ...defaultEnabled, lastBlobUpdated: true };
      const errors = validateCleanupPolicy(data, enabled, SAMPLE_FORMAT_CRITERIA);
      expect(errors.criteriaLastBlobUpdated).toBeTruthy();
    });
  });
});
