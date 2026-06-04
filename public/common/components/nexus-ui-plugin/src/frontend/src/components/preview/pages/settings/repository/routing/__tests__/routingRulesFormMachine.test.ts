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
import { createRoutingRulesFormMachine } from '../routingRulesFormMachine';

// Mock the API interface at the path the source uses
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: jest.fn().mockResolvedValue(null),
    post: jest.fn().mockResolvedValue(undefined),
    put: jest.fn().mockResolvedValue(undefined),
    delete: jest.fn().mockResolvedValue(undefined),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

const MOCK_RULE = {
  id: 'block-sources',
  name: 'block-sources',
  description: 'Block source downloads',
  mode: 'BLOCK' as const,
  matchers: ['^/com/example/.*', '^/org/private/.*'],
  assignedRepositoryCount: 0,
  assignedRepositoryNames: [],
};

/**
 * Helper: start machine and wait for it to reach editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createRoutingRulesFormMachine>,
  ruleData?: Record<string, unknown>
) {
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('routing-rules/')) {
      return Promise.resolve(ruleData || MOCK_RULE);
    }
    return Promise.resolve(null);
  });

  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('routingRulesFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      restClient.get.mockResolvedValue(null);
      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to BLOCK mode with one empty matcher', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('');
      expect(state.context.data.description).toBe('');
      expect(state.context.data.mode).toBe('BLOCK');
      expect(state.context.data.matchers).toEqual(['']);

      service.stop();
    });

    it('has no routing rule in context', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.routingRule).toBeNull();

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads routing rule data from API', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('block-sources');
      expect(state.context.data.description).toBe('Block source downloads');
      expect(state.context.data.mode).toBe('BLOCK');
      expect(state.context.data.matchers).toEqual(['^/com/example/.*', '^/org/private/.*']);

      service.stop();
    });

    it('loads preloaded routing rule without API call', async () => {
      const preloadedRule = { ...MOCK_RULE, description: 'Preloaded' };
      const machine = createRoutingRulesFormMachine('block-sources', preloadedRule);

      restClient.get.mockResolvedValue(null);

      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      const state = service.getSnapshot();
      expect(state.context.data.description).toBe('Preloaded');
      expect(state.context.routingRule).toEqual(preloadedRule);

      service.stop();
    });

    it('transitions to loadError on API failure', async () => {
      restClient.get.mockRejectedValue(new Error('Not found'));

      const machine = createRoutingRulesFormMachine('nonexistent');
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().matches('loadError')).toBe(true);
      expect(service.getSnapshot().context.loadError).toBeTruthy();

      service.stop();
    });
  });

  describe('validation', () => {
    it('validates name is required', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBe('Name is required');

      service.stop();
    });

    it('validates name format - must start with letter', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: '123-bad' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['.*'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toContain('must start with a letter');

      service.stop();
    });

    it('validates name format - no special characters', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'bad name!' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['.*'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('accepts valid name with letters, digits, hyphens, underscores', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-rule_123' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['.*'] } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toBeFalsy();

      service.stop();
    });

    it('validates at least one matcher is required', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-rule' } as any);
      // Matchers default to [''] - all empty
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.matchers).toBe('At least one matcher is required');

      service.stop();
    });

    it('validates matcher regex patterns', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-rule' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['[invalid'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.matchers).toContain('Invalid regex pattern');

      service.stop();
    });

    it('passes validation with valid data', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-rule' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['^/com/.*'] } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toBeFalsy();
      expect(state.context.validationErrors.matchers).toBeFalsy();

      service.stop();
    });

    it('validates mode is required', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-rule' } as any);
      service.send({ type: 'UPDATE', name: 'mode', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['.*'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.mode).toBe('Mode is required');

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates name field', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-rule' } as any);

      expect(service.getSnapshot().context.data.name).toBe('new-rule');

      service.stop();
    });

    it('updates description field', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'description', value: 'A test rule' } as any);

      expect(service.getSnapshot().context.data.description).toBe('A test rule');

      service.stop();
    });

    it('updates mode field', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'mode', value: 'ALLOW' } as any);

      expect(service.getSnapshot().context.data.mode).toBe('ALLOW');

      service.stop();
    });

    it('updates matchers array', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'matchers', value: ['^/a/.*', '^/b/.*'] } as any);

      expect(service.getSnapshot().context.data.matchers).toEqual(['^/a/.*', '^/b/.*']);

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'description', value: 'Changed' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'description', value: 'Changed' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.description).toBe('Block source downloads');

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions to saving on valid SUBMIT', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'my-rule' } as any);
      service.send({ type: 'UPDATE', name: 'matchers', value: ['^/com/.*'] } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(
        state.matches('saving') || state.matches('validating') || state.matches('saved')
      ).toBe(true);

      service.stop();
    });

    it('stays in editing when validation fails', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Submit with empty name and empty matchers
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();
      expect(state.context.validationErrors.matchers).toBeTruthy();

      service.stop();
    });
  });

  describe('delete flow', () => {
    it('supports delete in edit mode', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      // Send DELETE event
      service.send({ type: 'DELETE' } as any);

      const state = service.getSnapshot();
      expect(state.matches('confirmingDelete')).toBe(true);

      service.stop();
    });

    it('transitions to deleting on CONFIRM_DELETE', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      service.send({ type: 'DELETE' } as any);
      service.send({ type: 'CONFIRM_DELETE' } as any);

      const state = service.getSnapshot();
      expect(
        state.matches('deleting') || state.matches('deleted')
      ).toBe(true);

      service.stop();
    });

    it('returns to editing on CANCEL_DELETE', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      service.send({ type: 'DELETE' } as any);
      expect(service.getSnapshot().matches('confirmingDelete')).toBe(true);

      service.send({ type: 'CANCEL_DELETE' } as any);
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });
  });

  describe('cancel flow', () => {
    it('cancels directly when pristine', async () => {
      const machine = createRoutingRulesFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CANCEL' } as any);

      expect(service.getSnapshot().matches('cancelled')).toBe(true);

      service.stop();
    });

    it('shows confirmation when dirty', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      // Make dirty
      service.send({ type: 'UPDATE', name: 'description', value: 'changed' } as any);
      service.send({ type: 'CANCEL' } as any);

      expect(service.getSnapshot().matches('confirmingCancel')).toBe(true);

      service.stop();
    });

    it('confirms cancel discards changes', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'description', value: 'changed' } as any);
      service.send({ type: 'CANCEL' } as any);
      service.send({ type: 'CONFIRM_CANCEL' } as any);

      expect(service.getSnapshot().matches('cancelled')).toBe(true);

      service.stop();
    });

    it('STAY returns to editing', async () => {
      const machine = createRoutingRulesFormMachine('block-sources');
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'description', value: 'changed' } as any);
      service.send({ type: 'CANCEL' } as any);
      service.send({ type: 'STAY' } as any);

      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });
  });
});
