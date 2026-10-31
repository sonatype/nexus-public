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
import { loggingConfigMachine } from '../LoggingConfigMachine';

function makeMachine(
  serviceOverrides: {
    deleteLogger?: () => Promise<void>;
    resetAll?: () => Promise<void>;
  } = {}
) {
  return loggingConfigMachine.withConfig({
    services: {
      deleteLogger: serviceOverrides.deleteLogger ?? (async () => undefined),
      resetAll: serviceOverrides.resetAll ?? (async () => undefined),
    },
  });
}

describe('loggingConfigMachine', () => {
  it('starts in list state', () => {
    const service = interpret(makeMachine());
    service.start();

    expect(service.getSnapshot().matches('list')).toBe(true);
    expect(service.getSnapshot().context.selectedLogger).toBeNull();
    expect(service.getSnapshot().context.refreshKey).toBe(0);

    service.stop();
  });

  describe('list state transitions', () => {
    it('SELECT transitions to editing and sets selectedLogger', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'SELECT', name: 'org.sonatype' });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('editing')).toBe(true);
      expect(snapshot.context.selectedLogger).toBe('org.sonatype');

      service.stop();
    });

    it('CREATE transitions to creating state', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'CREATE' });

      expect(service.getSnapshot().matches('creating')).toBe(true);

      service.stop();
    });

    it('RESET_ALL_CLICK transitions to confirmResetAll state', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'RESET_ALL_CLICK' });

      expect(service.getSnapshot().matches('confirmResetAll')).toBe(true);

      service.stop();
    });

    it('CLEAR_ERROR clears error in list state', () => {
      const machine = loggingConfigMachine.withContext({
        ...loggingConfigMachine.context,
        error: 'Some error',
      }).withConfig({
        services: {
          deleteLogger: async () => undefined,
          resetAll: async () => undefined,
        },
      });
      const service = interpret(machine);
      service.start();

      service.send({ type: 'CLEAR_ERROR' });

      expect(service.getSnapshot().context.error).toBeNull();

      service.stop();
    });
  });

  describe('creating state', () => {
    it('BACK from creating transitions to list and clears selectedLogger', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'CREATE' });
      service.send({ type: 'BACK' });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('list')).toBe(true);
      expect(snapshot.context.selectedLogger).toBeNull();

      service.stop();
    });

    it('SAVE from creating transitions to list and increments refreshKey', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'CREATE' });
      service.send({ type: 'SAVE' });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('list')).toBe(true);
      expect(snapshot.context.refreshKey).toBe(1);

      service.stop();
    });
  });

  describe('editing state', () => {
    it('BACK from editing transitions to list and clears selectedLogger', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'SELECT', name: 'org.sonatype' });
      service.send({ type: 'BACK' });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('list')).toBe(true);
      expect(snapshot.context.selectedLogger).toBeNull();

      service.stop();
    });

    it('SAVE from editing transitions to list and increments refreshKey', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'SELECT', name: 'org.sonatype' });
      service.send({ type: 'SAVE' });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('list')).toBe(true);
      expect(snapshot.context.refreshKey).toBe(1);

      service.stop();
    });

    it('DELETE_CLICK from editing transitions to confirmDelete', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'SELECT', name: 'org.sonatype' });
      service.send({ type: 'DELETE_CLICK' });

      expect(service.getSnapshot().matches('confirmDelete')).toBe(true);

      service.stop();
    });
  });

  describe('confirmDelete state', () => {
    it('CANCEL_DELETE from confirmDelete transitions back to editing', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'SELECT', name: 'org.sonatype' });
      service.send({ type: 'DELETE_CLICK' });
      service.send({ type: 'CANCEL_DELETE' });

      const snapshot = service.getSnapshot();
      expect(snapshot.matches('editing')).toBe(true);
      expect(snapshot.context.selectedLogger).toBe('org.sonatype');

      service.stop();
    });

    it('CONFIRM_DELETE from confirmDelete transitions to deleting', () => {
      // Use a slow delete so we can observe the deleting state
      const machine = makeMachine({ deleteLogger: () => new Promise(() => {}) });
      const service = interpret(machine);
      service.start();

      service.send({ type: 'SELECT', name: 'org.sonatype' });
      service.send({ type: 'DELETE_CLICK' });
      service.send({ type: 'CONFIRM_DELETE' });

      expect(service.getSnapshot().matches('deleting')).toBe(true);

      service.stop();
    });
  });

  describe('deleting state', () => {
    it('successful delete transitions to list and increments refreshKey', (done) => {
      const service = interpret(makeMachine());

      service.onTransition((state) => {
        if (state.matches('deleting')) {
          // Let the service run
        }
        if (state.matches('list') && state.context.refreshKey > 0) {
          expect(state.context.selectedLogger).toBeNull();
          expect(state.context.refreshKey).toBe(1);
          service.stop();
          done();
        }
      });

      service.start();
      service.send({ type: 'SELECT', name: 'org.sonatype' });
      service.send({ type: 'DELETE_CLICK' });
      service.send({ type: 'CONFIRM_DELETE' });
    });

    it('failed delete transitions to editing and sets error', (done) => {
      const machine = makeMachine({
        deleteLogger: async () => { throw new Error('Delete failed'); },
      });
      const service = interpret(machine);
      let inEditingWithLogger = false;

      service.onTransition((state) => {
        if (state.matches('editing') && state.context.selectedLogger === 'org.sonatype' && !inEditingWithLogger) {
          inEditingWithLogger = true;
          service.send({ type: 'DELETE_CLICK' });
          service.send({ type: 'CONFIRM_DELETE' });
        }
        if (state.matches('editing') && inEditingWithLogger && state.context.error) {
          expect(state.context.error).toBe('Delete failed');
          service.stop();
          done();
        }
      });

      service.start();
      service.send({ type: 'SELECT', name: 'org.sonatype' });
    });
  });

  describe('confirmResetAll state', () => {
    it('CANCEL_RESET_ALL transitions back to list', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'RESET_ALL_CLICK' });
      service.send({ type: 'CANCEL_RESET_ALL' });

      expect(service.getSnapshot().matches('list')).toBe(true);

      service.stop();
    });

    it('CONFIRM_RESET_ALL transitions to resettingAll', () => {
      const machine = makeMachine({ resetAll: () => new Promise(() => {}) });
      const service = interpret(machine);
      service.start();

      service.send({ type: 'RESET_ALL_CLICK' });
      service.send({ type: 'CONFIRM_RESET_ALL' });

      expect(service.getSnapshot().matches('resettingAll')).toBe(true);

      service.stop();
    });
  });

  describe('resettingAll state', () => {
    it('successful resetAll transitions to list and increments refreshKey', (done) => {
      const service = interpret(makeMachine());

      service.onTransition((state) => {
        if (state.matches('list') && state.context.refreshKey > 0) {
          expect(state.context.refreshKey).toBe(1);
          service.stop();
          done();
        }
      });

      service.start();
      service.send({ type: 'RESET_ALL_CLICK' });
      service.send({ type: 'CONFIRM_RESET_ALL' });
    });

    it('failed resetAll transitions to list and sets error', (done) => {
      const machine = makeMachine({
        resetAll: async () => { throw new Error('Reset failed'); },
      });
      const service = interpret(machine);
      let resetSent = false;

      service.onTransition((state) => {
        if (state.matches('list') && resetSent && state.context.error) {
          expect(state.context.error).toBe('Reset failed');
          service.stop();
          done();
        }
      });

      service.start();
      service.send({ type: 'RESET_ALL_CLICK' });
      resetSent = true;
      service.send({ type: 'CONFIRM_RESET_ALL' });
    });
  });

  describe('refreshKey increments', () => {
    it('increments refreshKey on each SAVE from creating', () => {
      const service = interpret(makeMachine());
      service.start();

      service.send({ type: 'CREATE' });
      service.send({ type: 'SAVE' });
      expect(service.getSnapshot().context.refreshKey).toBe(1);

      service.send({ type: 'CREATE' });
      service.send({ type: 'SAVE' });
      expect(service.getSnapshot().context.refreshKey).toBe(2);

      service.stop();
    });
  });
});
