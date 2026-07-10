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
import { assign } from 'xstate';
import { createFormMachine } from '../createFormMachine';

interface TestFormData {
  name: string;
  value: number;
}

describe('createFormMachine', () => {
  describe('default behavior (stayEditableAfterSave: false)', () => {
    it('reaches saved state after successful save', async () => {
      const machine = createFormMachine<TestFormData>({
        id: 'test-form',
        context: {
          data: { name: 'test', value: 1 },
        },
        actions: {
          validate: assign(() => ({ validationErrors: {} })),
        },
      });

      const service = interpret(
        machine.withConfig({
          services: {
            save: async () => Promise.resolve({}),
          },
        })
      ).start();

      // Machine starts in editing since no load service
      expect(service.getSnapshot().matches('editing')).toBe(true);

      // Make a change to dirty the form
      service.send({ type: 'UPDATE', name: 'name', value: 'modified' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      // Submit
      service.send({ type: 'SUBMIT' } as any);

      // Should reach saved state (not editing)
      await waitFor(service, (state) => state.matches('saved'));
      expect(service.getSnapshot().matches('saved')).toBe(true);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('modified');

      service.stop();
    });

    it('saved state is final - no further events accepted', async () => {
      const machine = createFormMachine<TestFormData>({
        id: 'test-form',
        context: {
          data: { name: 'test', value: 1 },
        },
        actions: {
          validate: assign(() => ({ validationErrors: {} })),
        },
      });

      const service = interpret(
        machine.withConfig({
          services: {
            save: async () => Promise.resolve({}),
          },
        })
      ).start();

      // Dirty and submit
      service.send({ type: 'UPDATE', name: 'name', value: 'modified' } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('saved'));

      // Try to update - should be ignored since saved is final
      service.send({ type: 'UPDATE', name: 'name', value: 'another' } as any);

      // State should still be saved and data unchanged
      expect(service.getSnapshot().matches('saved')).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('modified');

      service.stop();
    });
  });

  describe('stayEditableAfterSave: true', () => {
    it('transitions directly to editing after save (bypasses saved)', async () => {
      const machine = createFormMachine<TestFormData>({
        id: 'test-form-stay-editable',
        context: {
          data: { name: 'test', value: 1 },
        },
        stayEditableAfterSave: true,
        actions: {
          validate: assign(() => ({ validationErrors: {} })),
        },
      });

      const service = interpret(
        machine.withConfig({
          services: {
            save: async () => Promise.resolve({}),
          },
        })
      ).start();

      expect(service.getSnapshot().matches('editing')).toBe(true);

      // Dirty and submit
      service.send({ type: 'UPDATE', name: 'name', value: 'modified' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);
      service.send({ type: 'SUBMIT' } as any);

      // Should reach editing with isPristine true (never hit saved state)
      await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);
      expect(service.getSnapshot().matches('editing')).toBe(true);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.name).toBe('modified');

      // Can continue editing after save
      service.send({ type: 'UPDATE', name: 'name', value: 'another' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);
      expect(service.getSnapshot().context.data.name).toBe('another');

      service.stop();
    });
  });
});
