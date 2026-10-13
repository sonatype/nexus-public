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
import {
  createRoleDetailEditMachine,
  isPending,
  type RoleDetailEditContext,
  type RoleDetailEditMachineOptions,
} from '../roleDetailEditMachine';
import type { Role } from '../types';

const ROLE_ID = 'test-role';
const NEW_NAME = 'Updated Role Name';
const NEW_DESCRIPTION = 'Updated description';
const SAVE_ERROR_MESSAGE = 'Backend rejected the update';

const baseRole: Role = {
  id: ROLE_ID,
  version: '1',
  source: 'Default',
  name: 'Original Role',
  description: 'Original description',
  readOnly: false,
  privileges: ['nx-search-read'],
  roles: ['nx-anonymous'],
};

function buildOptions(overrides: Partial<RoleDetailEditMachineOptions> = {}): RoleDetailEditMachineOptions {
  return {
    initialRole: baseRole,
    save: jest.fn().mockResolvedValue(baseRole),
    onSaved: jest.fn(),
    ...overrides,
  };
}

function waitForState(
  service: ReturnType<typeof interpret>,
  matcher: (state: { matches: (value: unknown) => boolean }) => boolean,
): Promise<void> {
  return new Promise((resolve) => {
    if (matcher(service.state as never)) {
      resolve();
      return;
    }
    const subscription = service.subscribe((state) => {
      if (matcher(state)) {
        subscription.unsubscribe();
        resolve();
      }
    });
  });
}

describe('roleDetailEditMachine', () => {
  describe('initial state', () => {
    it('starts in viewing with committed equal to the initial role', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      expect(service.state.matches('viewing')).toBe(true);
      expect(service.state.context.committed.name).toBe(baseRole.name);
      expect(service.state.context.committed.privileges).toEqual(baseRole.privileges);
      service.stop();
    });

    it('coerces missing name and description to empty strings so bound inputs stay controlled', () => {
      const sparseRole = { ...baseRole, name: undefined, description: undefined } as unknown as Role;
      const service = interpret(
        createRoleDetailEditMachine(buildOptions({ initialRole: sparseRole })),
      ).start();
      expect(service.state.context.committed.name).toBe('');
      expect(service.state.context.committed.description).toBe('');
      expect(service.state.context.pending.name).toBe('');
      expect(service.state.context.pending.description).toBe('');
      service.stop();
    });

    it('starts with pending identical to committed (isPending=false)', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      expect(isPending(service.state.context)).toBe(false);
      service.stop();
    });
  });

  describe('field updates', () => {
    it('UPDATE_NAME writes to pending but leaves committed alone', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      expect(service.state.context.pending.name).toBe(NEW_NAME);
      expect(service.state.context.committed.name).toBe(baseRole.name);
      expect(isPending(service.state.context)).toBe(true);
      service.stop();
    });

    it('UPDATE_DESCRIPTION marks pending', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      service.send({ type: 'UPDATE_DESCRIPTION', value: NEW_DESCRIPTION });
      expect(service.state.context.pending.description).toBe(NEW_DESCRIPTION);
      expect(isPending(service.state.context)).toBe(true);
      service.stop();
    });

    it('SET_PRIVILEGES replaces the privileges array', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      service.send({ type: 'SET_PRIVILEGES', value: ['a', 'b'] });
      expect(service.state.context.pending.privileges).toEqual(['a', 'b']);
      expect(isPending(service.state.context)).toBe(true);
      service.stop();
    });

    it('SET_ROLES replaces the contained-roles array', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      service.send({ type: 'SET_ROLES', value: ['role-a'] });
      expect(service.state.context.pending.roles).toEqual(['role-a']);
      expect(isPending(service.state.context)).toBe(true);
      service.stop();
    });

    it('reordering the same privileges is NOT considered dirty (order-insensitive)', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      service.send({ type: 'SET_PRIVILEGES', value: ['nx-search-read'] });
      // Committed had ['nx-search-read']; pending now has same set.
      expect(isPending(service.state.context)).toBe(false);
      service.stop();
    });
  });

  describe('cancel', () => {
    it('CANCEL rewinds pending back to committed', () => {
      const service = interpret(createRoleDetailEditMachine(buildOptions())).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SET_PRIVILEGES', value: ['brand-new'] });
      expect(isPending(service.state.context)).toBe(true);
      service.send({ type: 'CANCEL' });
      expect(isPending(service.state.context)).toBe(false);
      expect(service.state.context.pending.name).toBe(baseRole.name);
      expect(service.state.context.pending.privileges).toEqual(baseRole.privileges);
      service.stop();
    });

    it('CANCEL also clears any lingering saveError', () => {
      const service = interpret(
        createRoleDetailEditMachine(
          buildOptions({
            save: jest.fn().mockRejectedValue(new Error(SAVE_ERROR_MESSAGE)),
          }),
        ),
      ).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SAVE' });
      return waitForState(service, (state) => state.matches('viewing')).then(() => {
        expect(service.state.context.saveError).toBe(SAVE_ERROR_MESSAGE);
        service.send({ type: 'CANCEL' });
        expect(service.state.context.saveError).toBeNull();
        service.stop();
      });
    });
  });

  describe('save', () => {
    it('SAVE with no pending changes is a no-op (guarded)', () => {
      const saveSpy = jest.fn().mockResolvedValue(baseRole);
      const service = interpret(createRoleDetailEditMachine(buildOptions({ save: saveSpy }))).start();
      service.send({ type: 'SAVE' });
      expect(saveSpy).not.toHaveBeenCalled();
      expect(service.state.matches('viewing')).toBe(true);
      service.stop();
    });

    it('SAVE with pending changes transitions to saving and invokes save with pending', () => {
      let capturedPending: any = null;
      const saveSpy = jest.fn().mockImplementation((data: any) => {
        capturedPending = data;
        return new Promise(() => {}); // never resolves - lets us assert mid-flight state
      });
      const service = interpret(createRoleDetailEditMachine(buildOptions({ save: saveSpy }))).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SAVE' });
      expect(service.state.matches('saving')).toBe(true);
      expect(capturedPending.name).toBe(NEW_NAME);
      expect(capturedPending.privileges).toEqual(baseRole.privileges);
      service.stop();
    });

    it('successful save transitions saving -> viewing, commits pending, fires onSaved with the fresh role', async () => {
      const freshRole: Role = { ...baseRole, name: NEW_NAME, version: '2' };
      const saveSpy = jest.fn().mockResolvedValue(freshRole);
      const onSaved = jest.fn();
      const service = interpret(
        createRoleDetailEditMachine(buildOptions({ save: saveSpy, onSaved })),
      ).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SAVE' });
      await waitForState(service, (state) => state.matches('viewing'));

      expect(service.state.context.committed.name).toBe(NEW_NAME);
      expect(service.state.context.pending.name).toBe(NEW_NAME);
      expect(isPending(service.state.context)).toBe(false);
      expect(onSaved).toHaveBeenCalledWith(freshRole);
      service.stop();
    });

    it('successful save with sparse response coerces missing name and description to empty strings', async () => {
      const sparseFreshRole = { ...baseRole, name: undefined, description: undefined } as unknown as Role;
      const saveSpy = jest.fn().mockResolvedValue(sparseFreshRole);
      const service = interpret(
        createRoleDetailEditMachine(buildOptions({ save: saveSpy })),
      ).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SAVE' });
      await waitForState(service, (state) => state.matches('viewing'));

      expect(service.state.context.pending.name).toBe('');
      expect(service.state.context.pending.description).toBe('');
      service.stop();
    });

    it('failed save transitions saving -> viewing with saveError, pending unchanged', async () => {
      const saveSpy = jest.fn().mockRejectedValue(new Error(SAVE_ERROR_MESSAGE));
      const onSaved = jest.fn();
      const service = interpret(
        createRoleDetailEditMachine(buildOptions({ save: saveSpy, onSaved })),
      ).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SAVE' });
      await waitForState(
        service,
        (state) => state.matches('viewing') && (state as any).context.saveError !== null,
      );

      expect(service.state.context.saveError).toBe(SAVE_ERROR_MESSAGE);
      // Pending stays dirty so the user can adjust and retry.
      expect(service.state.context.pending.name).toBe(NEW_NAME);
      expect(isPending(service.state.context)).toBe(true);
      expect(onSaved).not.toHaveBeenCalled();
      service.stop();
    });

    it('starting a new SAVE clears any previous saveError', async () => {
      let calls = 0;
      const saveSpy = jest.fn().mockImplementation(() => {
        calls += 1;
        if (calls === 1) return Promise.reject(new Error(SAVE_ERROR_MESSAGE));
        return new Promise(() => {}); // never resolves - lets us assert the state right after the transition
      });
      const service = interpret(createRoleDetailEditMachine(buildOptions({ save: saveSpy }))).start();
      service.send({ type: 'UPDATE_NAME', value: NEW_NAME });
      service.send({ type: 'SAVE' });
      await waitForState(
        service,
        (state) => state.matches('viewing') && (state as any).context.saveError !== null,
      );
      expect(service.state.context.saveError).toBe(SAVE_ERROR_MESSAGE);

      service.send({ type: 'SAVE' });
      expect(service.state.matches('saving')).toBe(true);
      expect(service.state.context.saveError).toBeNull();
      service.stop();
    });
  });

  describe('isPending', () => {
    const base: RoleDetailEditContext = {
      committed: {
        id: 'r',
        name: 'Same',
        description: 'Same',
        privileges: ['a', 'b'],
        roles: ['x'],
      },
      pending: {
        id: 'r',
        name: 'Same',
        description: 'Same',
        privileges: ['a', 'b'],
        roles: ['x'],
      },
      saveError: null,
    };

    it('returns false when all editable fields match', () => {
      expect(isPending(base)).toBe(false);
    });

    it('returns true on name diff', () => {
      expect(isPending({ ...base, pending: { ...base.pending, name: 'Other' } })).toBe(true);
    });

    it('returns true on description diff', () => {
      expect(isPending({ ...base, pending: { ...base.pending, description: 'Other' } })).toBe(true);
    });

    it('returns true when a privilege was removed', () => {
      expect(isPending({ ...base, pending: { ...base.pending, privileges: ['a'] } })).toBe(true);
    });

    it('returns true when a contained role was added', () => {
      expect(isPending({ ...base, pending: { ...base.pending, roles: ['x', 'y'] } })).toBe(true);
    });

    it('returns false when privileges are reordered but the set is the same', () => {
      expect(isPending({ ...base, pending: { ...base.pending, privileges: ['b', 'a'] } })).toBe(false);
    });
  });
});
