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
import { createRoleModalContentMachine } from '../roleModalContentMachine';
import type { Role } from '../types';

const ROLE_NAME = 'test-role';
const NOT_FOUND_MESSAGE = 'Role not found';
const TRANSIENT_ERROR_MESSAGE = 'Network error';
const FALLBACK_ERROR_MESSAGE = 'Failed to load role';

const mockRole: Role = {
  id: ROLE_NAME,
  version: '1',
  source: 'Default',
  name: 'Test Role',
  description: 'Test',
  readOnly: false,
  privileges: [],
  roles: [],
};

function waitForState(
  service: ReturnType<typeof interpret>,
  matcher: (state: { matches: (value: string) => boolean }) => boolean,
): Promise<void> {
  return new Promise((resolve) => {
    const subscription = service.subscribe((state) => {
      if (matcher(state)) {
        subscription.unsubscribe();
        resolve();
      }
    });
  });
}

describe('roleModalContentMachine', () => {
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('should start in the loading state with cleared role and error', () => {
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: () => new Promise(() => {}),
    });
    const service = interpret(machine).start();

    expect(service.state.matches('loading')).toBe(true);
    expect(service.state.context.role).toBeNull();
    expect(service.state.context.error).toBeNull();

    service.stop();
  });

  it('should transition to loaded and populate role on successful fetch', async () => {
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => mockRole,
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('loaded'));

    expect(service.state.context.role).toEqual(mockRole);
    expect(service.state.context.error).toBeNull();

    service.stop();
  });

  it('should transition to notFound when findRole returns null', async () => {
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => null,
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('notFound'));

    expect(service.state.context.role).toBeNull();
    expect(service.state.context.error).toBe(NOT_FOUND_MESSAGE);

    service.stop();
  });

  it('should transition to error and log to console when findRole throws', async () => {
    const thrownError = new Error(TRANSIENT_ERROR_MESSAGE);
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => {
        throw thrownError;
      },
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('error'));

    expect(service.state.context.role).toBeNull();
    expect(service.state.context.error).toBe(TRANSIENT_ERROR_MESSAGE);
    expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load role:', thrownError);

    service.stop();
  });

  it('should fall back to a generic message when the thrown error has no message', async () => {
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => {
        throw null;
      },
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('error'));

    expect(service.state.context.error).toBe(FALLBACK_ERROR_MESSAGE);

    service.stop();
  });

  it('should re-enter loading and re-invoke findRole on RETRY from error', async () => {
    let callCount = 0;
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => {
        callCount += 1;
        if (callCount === 1) throw new Error(TRANSIENT_ERROR_MESSAGE);
        return mockRole;
      },
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('error'));
    expect(callCount).toBe(1);

    service.send({ type: 'RETRY' });
    expect(service.state.matches('loading')).toBe(true);
    expect(service.state.context.role).toBeNull();
    expect(service.state.context.error).toBeNull();

    await waitForState(service, (state) => state.matches('loaded'));

    expect(service.state.context.role).toEqual(mockRole);
    expect(callCount).toBe(2);

    service.stop();
  });

  it('should NOT respond to RETRY events from non-error states', async () => {
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => mockRole,
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('loaded'));

    service.send({ type: 'RETRY' });
    // RETRY is only wired into the `error` state; from `loaded` it is a no-op.
    expect(service.state.matches('loaded')).toBe(true);

    service.stop();
  });

  it('should clear stale role state at the entry to loading (retry clears previous data)', async () => {
    let resolveFn: ((role: Role | null) => void) = () => {};
    let callCount = 0;
    const machine = createRoleModalContentMachine({
      roleName: ROLE_NAME,
      findRole: async () => {
        callCount += 1;
        if (callCount === 1) throw new Error(TRANSIENT_ERROR_MESSAGE);
        return new Promise<Role | null>((resolve) => {
          resolveFn = resolve;
        });
      },
    });
    const service = interpret(machine).start();

    await waitForState(service, (state) => state.matches('error'));

    service.send({ type: 'RETRY' });
    // Immediately after RETRY, role has been cleared even though the new fetch is pending.
    expect(service.state.matches('loading')).toBe(true);
    expect(service.state.context.role).toBeNull();
    expect(service.state.context.error).toBeNull();

    resolveFn(mockRole);
    await waitForState(service, (state) => state.matches('loaded'));

    expect(service.state.context.role).toEqual(mockRole);
    service.stop();
  });
});
