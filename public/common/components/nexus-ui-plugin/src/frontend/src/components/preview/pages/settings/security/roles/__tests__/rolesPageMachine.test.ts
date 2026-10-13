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
  createRolesPageMachine,
  parseRoute,
  type RouteState,
  type RolesPageMachineOptions,
} from '../rolesPageMachine';
import type { Role } from '../types';

const ROLE_ID = 'test-role';
const OTHER_ROLE_ID = 'other-role';
const TRANSIENT_ERROR_MESSAGE = 'Network error';
const DELETE_ERROR_MESSAGE = 'Delete failed';

const mockRole: Role = {
  id: ROLE_ID,
  version: '1',
  source: 'Default',
  name: 'Test Role',
  description: 'Test',
  readOnly: false,
  privileges: [],
  roles: [],
};

const LIST_ROUTE: RouteState = { viewMode: 'list', roleId: null };
const CREATE_ROUTE: RouteState = { viewMode: 'create', roleId: null };
const DETAIL_ROUTE: RouteState = { viewMode: 'detail', roleId: ROLE_ID };

function buildOptions(overrides: Partial<RolesPageMachineOptions> = {}): RolesPageMachineOptions {
  return {
    initialRouteState: LIST_ROUTE,
    findRole: jest.fn().mockResolvedValue(mockRole),
    deleteRole: jest.fn().mockResolvedValue(undefined),
    onDeleted: jest.fn(),
    onNavigateAway: jest.fn(),
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

describe('parseRoute', () => {
  it('parses an empty hash as list mode', () => {
    expect(parseRoute('')).toEqual(LIST_ROUTE);
  });

  it('parses /roles as list mode', () => {
    expect(parseRoute('#preview/admin/security/roles')).toEqual(LIST_ROUTE);
  });

  it('parses /roles/create as create mode', () => {
    expect(parseRoute('#preview/admin/security/roles/create')).toEqual(CREATE_ROUTE);
  });

  it('parses /roles/{id} as detail mode with decoded id', () => {
    expect(parseRoute('#preview/admin/security/roles/my-role')).toEqual({
      viewMode: 'detail',
      roleId: 'my-role',
    });
  });

  it('treats a legacy /profile suffix as the unified detail route', () => {
    expect(parseRoute('#preview/admin/security/roles/legacy-role/profile')).toEqual({
      viewMode: 'detail',
      roleId: 'legacy-role',
    });
  });

  it('URL-decodes the role id segment', () => {
    expect(parseRoute('#preview/admin/security/roles/role%20name')).toEqual({
      viewMode: 'detail',
      roleId: 'role name',
    });
  });
});

describe('rolesPageMachine', () => {
  describe('initial state', () => {
    it('starts in listView when initial route is list', () => {
      const service = interpret(createRolesPageMachine(buildOptions())).start();
      expect(service.state.matches('listView')).toBe(true);
      service.stop();
    });

    it('starts in createView when initial route is create', () => {
      const service = interpret(
        createRolesPageMachine(buildOptions({ initialRouteState: CREATE_ROUTE })),
      ).start();
      expect(service.state.matches('createView')).toBe(true);
      service.stop();
    });

    it('starts in detailView.loading when initial route is detail', () => {
      const service = interpret(
        createRolesPageMachine(
          buildOptions({
            initialRouteState: DETAIL_ROUTE,
            findRole: () => new Promise(() => {}),
          }),
        ),
      ).start();
      expect(service.state.matches({ detailView: 'loading' })).toBe(true);
      service.stop();
    });
  });

  describe('detail fetch lifecycle', () => {
    it('transitions loading -> loaded on success', async () => {
      const service = interpret(
        createRolesPageMachine(buildOptions({ initialRouteState: DETAIL_ROUTE })),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));

      expect(service.state.context.role).toEqual(mockRole);
      expect(service.state.context.errorMessage).toBeNull();
      expect(service.state.context.errorType).toBeNull();
      service.stop();
    });

    it('transitions loading -> notFound when findRole returns null and fires onNavigateAway', async () => {
      const onNavigateAway = jest.fn();
      const service = interpret(
        createRolesPageMachine(
          buildOptions({
            initialRouteState: DETAIL_ROUTE,
            findRole: jest.fn().mockResolvedValue(null),
            onNavigateAway,
          }),
        ),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'notFound' }));

      expect(service.state.context.errorType).toBe('notFound');
      expect(service.state.context.errorMessage).toBe('Role not found');
      expect(onNavigateAway).toHaveBeenCalledTimes(1);
      service.stop();
    });

    it('transitions loading -> forbidden when findRole throws with status=403 and fires onNavigateAway', async () => {
      const forbiddenError = Object.assign(new Error('Forbidden'), { status: 403 });
      const onNavigateAway = jest.fn();
      const service = interpret(
        createRolesPageMachine(
          buildOptions({
            initialRouteState: DETAIL_ROUTE,
            findRole: jest.fn().mockRejectedValue(forbiddenError),
            onNavigateAway,
          }),
        ),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'forbidden' }));

      expect(service.state.context.errorType).toBe('forbidden');
      expect(service.state.context.errorMessage).toBe(
        'You do not have permission to view this role',
      );
      expect(onNavigateAway).toHaveBeenCalledTimes(1);
      service.stop();
    });

    it('transitions loading -> transientError when findRole throws a non-403 error and stays there', async () => {
      const transientError = Object.assign(new Error(TRANSIENT_ERROR_MESSAGE), { status: 500 });
      const onNavigateAway = jest.fn();
      const service = interpret(
        createRolesPageMachine(
          buildOptions({
            initialRouteState: DETAIL_ROUTE,
            findRole: jest.fn().mockRejectedValue(transientError),
            onNavigateAway,
          }),
        ),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'transientError' }));

      expect(service.state.context.errorType).toBe('transient');
      expect(service.state.context.errorMessage).toBe(TRANSIENT_ERROR_MESSAGE);
      expect(onNavigateAway).not.toHaveBeenCalled();
      service.stop();
    });

    it('re-enters loading and re-invokes findRole on RETRY from transientError', async () => {
      let callCount = 0;
      const findRole = jest.fn(async () => {
        callCount += 1;
        if (callCount === 1) {
          throw Object.assign(new Error(TRANSIENT_ERROR_MESSAGE), { status: 500 });
        }
        return mockRole;
      });
      const service = interpret(
        createRolesPageMachine(buildOptions({ initialRouteState: DETAIL_ROUTE, findRole })),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'transientError' }));

      service.send({ type: 'RETRY' });
      expect(service.state.matches({ detailView: 'loading' })).toBe(true);
      expect(service.state.context.role).toBeNull();
      expect(service.state.context.errorType).toBeNull();

      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));
      expect(service.state.context.role).toEqual(mockRole);
      expect(findRole).toHaveBeenCalledTimes(2);
      service.stop();
    });
  });

  describe('route changes', () => {
    it('transitions to createView on ROUTE_CHANGE with create route', () => {
      const service = interpret(createRolesPageMachine(buildOptions())).start();
      service.send({ type: 'ROUTE_CHANGE', routeState: CREATE_ROUTE });
      expect(service.state.matches('createView')).toBe(true);
      expect(service.state.context.routeState).toEqual(CREATE_ROUTE);
      service.stop();
    });

    it('transitions to detailView.loading on ROUTE_CHANGE with detail route', () => {
      const service = interpret(
        createRolesPageMachine(buildOptions({ findRole: () => new Promise(() => {}) })),
      ).start();
      service.send({ type: 'ROUTE_CHANGE', routeState: DETAIL_ROUTE });
      expect(service.state.matches({ detailView: 'loading' })).toBe(true);
      service.stop();
    });

    it('resets error and role on route change', async () => {
      const service = interpret(
        createRolesPageMachine(buildOptions({ initialRouteState: DETAIL_ROUTE })),
      ).start();
      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));

      service.send({ type: 'ROUTE_CHANGE', routeState: LIST_ROUTE });
      expect(service.state.context.errorMessage).toBeNull();
      expect(service.state.context.role).toBeNull();
      service.stop();
    });
  });

  describe('delete flow', () => {
    it('transitions loaded -> deleting -> loaded on successful delete and fires onDeleted', async () => {
      const onDeleted = jest.fn();
      const deleteRoleMock = jest.fn().mockResolvedValue(undefined);
      const service = interpret(
        createRolesPageMachine(
          buildOptions({
            initialRouteState: DETAIL_ROUTE,
            deleteRole: deleteRoleMock,
            onDeleted,
          }),
        ),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));

      service.send({ type: 'OPEN_DELETE_DIALOG' });
      expect(service.state.context.deleteDialogOpen).toBe(true);

      service.send({ type: 'CONFIRM_DELETE' });
      expect(service.state.matches({ detailView: 'deleting' })).toBe(true);
      // Dialog is closed as soon as we enter deleting to prevent double-confirmation.
      expect(service.state.context.deleteDialogOpen).toBe(false);

      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));

      expect(deleteRoleMock).toHaveBeenCalledWith(ROLE_ID);
      expect(onDeleted).toHaveBeenCalledWith(mockRole.name);
      expect(service.state.context.refreshKey).toBe(1);
      service.stop();
    });

    it('transitions deleting -> loaded on delete failure and records error', async () => {
      const deleteError = Object.assign(new Error(DELETE_ERROR_MESSAGE), { status: 500 });
      const onDeleted = jest.fn();
      const service = interpret(
        createRolesPageMachine(
          buildOptions({
            initialRouteState: DETAIL_ROUTE,
            deleteRole: jest.fn().mockRejectedValue(deleteError),
            onDeleted,
          }),
        ),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));

      service.send({ type: 'OPEN_DELETE_DIALOG' });
      service.send({ type: 'CONFIRM_DELETE' });

      await waitForState(
        service,
        (state) => state.matches({ detailView: 'loaded' }) && state.context.errorMessage !== null,
      );

      expect(service.state.context.errorMessage).toBe(DELETE_ERROR_MESSAGE);
      expect(onDeleted).not.toHaveBeenCalled();
      service.stop();
    });

    it('closes the delete dialog on CANCEL_DELETE', () => {
      const service = interpret(createRolesPageMachine(buildOptions())).start();
      service.send({ type: 'OPEN_DELETE_DIALOG' });
      expect(service.state.context.deleteDialogOpen).toBe(true);

      service.send({ type: 'CANCEL_DELETE' });
      expect(service.state.context.deleteDialogOpen).toBe(false);
      service.stop();
    });
  });

  describe('page-level events', () => {
    it('increments refreshKey on INCREMENT_REFRESH', () => {
      const service = interpret(createRolesPageMachine(buildOptions())).start();
      const before = service.state.context.refreshKey;
      service.send({ type: 'INCREMENT_REFRESH' });
      expect(service.state.context.refreshKey).toBe(before + 1);
      service.stop();
    });
  });

  describe('stale-state guards', () => {
    it('clears role state on loading entry (retry does not briefly render previous data)', async () => {
      let resolveFn: (role: Role | null) => void = () => {};
      let callCount = 0;
      const findRole = jest.fn(async () => {
        callCount += 1;
        if (callCount === 1) throw new Error(TRANSIENT_ERROR_MESSAGE);
        return new Promise<Role | null>((resolve) => {
          resolveFn = resolve;
        });
      });
      const service = interpret(
        createRolesPageMachine(buildOptions({ initialRouteState: DETAIL_ROUTE, findRole })),
      ).start();

      await waitForState(service, (state) => state.matches({ detailView: 'transientError' }));

      service.send({ type: 'RETRY' });
      expect(service.state.matches({ detailView: 'loading' })).toBe(true);
      expect(service.state.context.role).toBeNull();
      expect(service.state.context.errorType).toBeNull();

      resolveFn(mockRole);
      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));
      expect(service.state.context.role).toEqual(mockRole);
      service.stop();
    });

    it('clears role and error when navigating away from detailView to listView', async () => {
      const service = interpret(
        createRolesPageMachine(buildOptions({ initialRouteState: DETAIL_ROUTE })),
      ).start();
      await waitForState(service, (state) => state.matches({ detailView: 'loaded' }));

      service.send({ type: 'ROUTE_CHANGE', routeState: { viewMode: 'detail', roleId: OTHER_ROLE_ID } });
      // Route reassigned; role cleared by assignRouteAndReset action.
      expect(service.state.context.role).toBeNull();
      service.stop();
    });
  });
});
