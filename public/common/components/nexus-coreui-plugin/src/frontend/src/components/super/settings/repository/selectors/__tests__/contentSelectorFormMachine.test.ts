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
import { createContentSelectorFormMachine } from '../contentSelectorFormMachine';
import { CONTENT_SELECTOR_TYPE } from '../types';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    ENDPOINTS: {
      CONTENT_SELECTORS: '/service/rest/v1/security/content-selectors',
    },
    restClient: {
      get: jest.fn().mockResolvedValue(null),
    },
  };
});

const { restClient } = jest.requireMock('@sonatype/nexus-ui-plugin');

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createContentSelectorFormMachine>,
  loadData?: Record<string, unknown>
) {
  // Mock the API response for the load service
  restClient.get.mockImplementation((url: string) => {
    if (loadData && url.includes('content-selectors')) {
      return Promise.resolve(loadData);
    }
    return Promise.resolve(null);
  });

  const service = interpret(machine).start();

  // Wait for loading to complete
  await waitFor(service, (state) => state.matches('editing'));

  return service;
}

describe('contentSelectorFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('initializes with empty form data in create mode', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('');
      expect(state.context.data.type).toBe(CONTENT_SELECTOR_TYPE);
      expect(state.context.data.description).toBe('');
      expect(state.context.data.expression).toBe('');
      expect(state.context.selector).toBeNull();

      service.stop();
    });

    it('has no type variants (no editingConfig)', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      // Should be in 'editing' state directly, not a compound sub-state
      expect(state.matches('editing')).toBe(true);

      service.stop();
    });
  });

  describe('edit mode', () => {
    const preloadedSelector = {
      name: 'test-selector',
      type: 'csel',
      description: 'Test description',
      expression: 'format == "maven2"',
    };

    it('loads preloaded selector data', async () => {
      const machine = createContentSelectorFormMachine('test-selector', preloadedSelector);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('test-selector');
      expect(state.context.data.type).toBe('csel');
      expect(state.context.data.description).toBe('Test description');
      expect(state.context.data.expression).toBe('format == "maven2"');
      expect(state.context.selector).toEqual(preloadedSelector);

      service.stop();
    });

    it('fetches selector by name when no preloaded data', async () => {
      const fetchedSelector = {
        name: 'remote-selector',
        type: 'csel',
        description: 'Fetched from API',
        expression: 'path =^ "/org"',
      };

      const machine = createContentSelectorFormMachine('remote-selector');
      const service = await startAndLoad(machine, fetchedSelector);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('remote-selector');
      expect(state.context.data.expression).toBe('path =^ "/org"');
      expect(restClient.get).toHaveBeenCalledWith(
        '/service/rest/v1/security/content-selectors/remote-selector'
      );

      service.stop();
    });
  });

  describe('validation', () => {
    it('validates name is required', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Try to submit with empty name
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      // Should stay in editing (validation failed)
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();

      service.stop();
    });

    it('validates name format (letters, digits, underscores, hyphens, periods)', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'invalid name!' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toContain('letters');

      service.stop();
    });

    it('accepts valid name characters', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'valid-name_1.0' } as any);
      service.send({ type: 'UPDATE', name: 'expression', value: 'format == "maven2"' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toBeUndefined();

      service.stop();
    });

    it('validates expression is required', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-selector' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.expression).toBe('Expression is required');

      service.stop();
    });

    it('validates expression with blocking errors', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set valid name and expression content
      service.send({ type: 'UPDATE', name: 'name', value: 'test-selector' } as any);
      service.send({ type: 'UPDATE', name: 'expression', value: 'format ==' } as any);

      // Report blocking errors from CSEL editor
      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: true,
      } as any);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.expression).toBe('Expression has syntax errors');

      service.stop();
    });

    it('clears expression validation errors when CSEL editor reports no blocking errors', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-selector' } as any);
      service.send({ type: 'UPDATE', name: 'expression', value: 'format == "maven2"' } as any);

      // First report blocking errors
      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: true,
      } as any);

      expect(service.getSnapshot().context.validationErrors.expression).toBe(
        'Expression has syntax errors'
      );

      // Then clear them
      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: false,
      } as any);

      expect(service.getSnapshot().context.validationErrors.expression).toBeUndefined();

      service.stop();
    });

    it('passes validation with valid name and expression', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'test-selector' } as any);
      service.send({ type: 'UPDATE', name: 'expression', value: 'format == "maven2"' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.name).toBeUndefined();
      expect(state.context.validationErrors.expression).toBeUndefined();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates name field', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-selector' } as any);

      const state = service.getSnapshot();
      expect(state.context.data.name).toBe('new-selector');

      service.stop();
    });

    it('updates expression field', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({
        type: 'UPDATE',
        name: 'expression',
        value: 'path =^ "/org"',
      } as any);

      const state = service.getSnapshot();
      expect(state.context.data.expression).toBe('path =^ "/org"');

      service.stop();
    });

    it('updates description field', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({
        type: 'UPDATE',
        name: 'description',
        value: 'My content selector',
      } as any);

      const state = service.getSnapshot();
      expect(state.context.data.description).toBe('My content selector');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-selector' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'name', value: 'new-selector' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('');

      service.stop();
    });
  });

  describe('UPDATE_EXPRESSION_VALIDATION event', () => {
    it('stores expression blocking errors in context', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: true,
      } as any);

      expect(service.getSnapshot().context.expressionHasBlockingErrors).toBe(true);

      service.stop();
    });

    it('clears expression blocking errors in context', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set blocking errors
      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: true,
      } as any);
      expect(service.getSnapshot().context.expressionHasBlockingErrors).toBe(true);

      // Clear them
      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: false,
      } as any);
      expect(service.getSnapshot().context.expressionHasBlockingErrors).toBe(false);

      service.stop();
    });

    it('re-validates after expression validation update', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set expression content and blocking errors
      service.send({ type: 'UPDATE', name: 'expression', value: 'invalid' } as any);
      service.send({
        type: 'UPDATE_EXPRESSION_VALIDATION',
        hasBlockingErrors: true,
      } as any);

      expect(service.getSnapshot().context.validationErrors.expression).toBe(
        'Expression has syntax errors'
      );

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions to validating then saving with valid data', async () => {
      const mockSave = jest.fn().mockResolvedValue(undefined);

      const machine = createContentSelectorFormMachine(undefined);
      // Override save service
      const service = interpret(
        machine.withConfig({
          services: {
            ...machine.options.services,
            save: mockSave,
          },
        })
      ).start();

      // Mock load
      restClient.get.mockResolvedValue(null);
      await waitFor(service, (state) => state.matches('editing'));

      // Fill valid data
      service.send({ type: 'UPDATE', name: 'name', value: 'test-selector' } as any);
      service.send({
        type: 'UPDATE',
        name: 'expression',
        value: 'format == "maven2"',
      } as any);

      // Submit
      service.send({ type: 'SUBMIT' } as any);

      // Should transition through validating to saving
      await waitFor(service, (state) =>
        state.matches('saving') || state.matches('saved')
      );

      expect(mockSave).toHaveBeenCalled();

      service.stop();
    });

    it('stays in editing when validation fails on submit', async () => {
      const machine = createContentSelectorFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Submit with empty fields
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.name).toBeTruthy();
      expect(state.context.validationErrors.expression).toBeTruthy();

      service.stop();
    });
  });

  describe('load errors', () => {
    it('transitions to loadError when load fails', async () => {
      restClient.get.mockRejectedValue(new Error('Network error'));

      const machine = createContentSelectorFormMachine('missing-selector');
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));

      expect(service.getSnapshot().matches('loadError')).toBe(true);

      service.stop();
    });

    it('retries loading on RETRY event', async () => {
      restClient.get.mockRejectedValueOnce(new Error('Network error'));

      const machine = createContentSelectorFormMachine('retry-selector');
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));

      // Now make the load succeed
      restClient.get.mockResolvedValueOnce({
        name: 'retry-selector',
        type: 'csel',
        description: '',
        expression: 'format == "raw"',
      });

      service.send({ type: 'RETRY' } as any);

      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().context.data.name).toBe('retry-selector');

      service.stop();
    });
  });
});
