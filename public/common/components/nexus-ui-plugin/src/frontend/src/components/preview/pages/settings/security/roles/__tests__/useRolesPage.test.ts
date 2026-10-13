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

import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { ToastProvider } from '../../../../../shared/Toast';
import { BASE_PATH, useRolesPage } from '../useRolesPage';
import { useRolesApi } from '../useRolesApi';
import { ExtJS } from '../../../../../../../interface/ExtJS';
import type { Role } from '../types';
import {
  getHashHistory,
  installHashStub,
  setInitialHash,
  uninstallHashStub,
} from './testUtils/hashStub';

jest.mock('../useRolesApi');
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
    usePermission: jest.fn((getValue: () => boolean) => getValue()),
    useUser: jest.fn(() => ({ id: 'admin' })),
  },
}));

const ROLE_ID = 'test-role';
const TRANSIENT_ERROR_MESSAGE = 'Network error';

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

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;
const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

function mockApi(overrides: Partial<ReturnType<typeof useRolesApi>>) {
  mockUseRolesApi.mockReturnValue({
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchRoles: jest.fn().mockResolvedValue([]),
    fetchRoleReferences: jest.fn().mockResolvedValue([]),
    fetchRoleSources: jest.fn().mockResolvedValue([]),
    fetchRolesFromSource: jest.fn().mockResolvedValue([]),
    fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
    findRole: jest.fn().mockResolvedValue(mockRole),
    createRole: jest.fn(),
    updateRole: jest.fn(),
    deleteRole: jest.fn(),
    ...overrides,
  } as unknown as ReturnType<typeof useRolesApi>);
}

function wrapper({ children }: { children: React.ReactNode }) {
  return React.createElement(ToastProvider, null, children);
}

// parseRoute unit tests moved to rolesPageMachine.test.ts, which is where parseRoute is
// now defined and exported. This file focuses on the hook's projection over the machine.

describe('useRolesPage', () => {
  beforeAll(() => {
    installHashStub();
  });

  afterAll(() => {
    uninstallHashStub();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    setInitialHash(`#${BASE_PATH}/${ROLE_ID}`);
  });

  it('should populate role and derive canEdit=true for an editable Default role with update permission', async () => {
    mockApi({ findRole: jest.fn().mockResolvedValue(mockRole) });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.role).toEqual(mockRole));
    expect(result.current.roleCanEdit).toBe(true);
    expect(result.current.roleCanDelete).toBe(true);
    expect(result.current.routeState.viewMode).toBe('detail');
  });

  it('should derive canEdit=false when role.readOnly is true even with update permission', async () => {
    const readOnlyRole = { ...mockRole, readOnly: true };
    mockApi({ findRole: jest.fn().mockResolvedValue(readOnlyRole) });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.role).toEqual(readOnlyRole));
    expect(result.current.roleCanEdit).toBe(false);
  });

  it('should set errorType=notFound and navigate back when findRole returns null', async () => {
    mockApi({ findRole: jest.fn().mockResolvedValue(null) });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.roleLoadErrorType).toBe('notFound'));
    expect(getHashHistory()).toContain(BASE_PATH);
  });

  it('should set errorType=forbidden and navigate back when findRole throws a 403', async () => {
    const forbiddenError = Object.assign(new Error('Forbidden'), { status: 403 });
    mockApi({ findRole: jest.fn().mockRejectedValue(forbiddenError) });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.roleLoadErrorType).toBe('forbidden'));
    expect(getHashHistory()).toContain(BASE_PATH);
  });

  it('should set errorType=transient and STAY on detail when findRole throws a non-403 non-404 error', async () => {
    const transientError = Object.assign(new Error(TRANSIENT_ERROR_MESSAGE), { status: 500 });
    mockApi({ findRole: jest.fn().mockRejectedValue(transientError) });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.roleLoadErrorType).toBe('transient'));
    // Transient errors keep the user on the detail route so a retry is possible.
    expect(getHashHistory()).toHaveLength(0);
    expect(result.current.role).toBeNull();
  });

  it('should clear the previous role state at the start of a new fetch (N1 regression guard)', async () => {
    // Two successive role loads. Assert that between the initial resolution and the retry
    // resolution, `role` is nulled so the view cannot briefly render stale data.
    const OTHER_ROLE_ID = 'other-role';
    const otherRole: Role = { ...mockRole, id: OTHER_ROLE_ID, name: 'Other Role' };
    const findRole = jest.fn(async (roleId: string) => (roleId === ROLE_ID ? mockRole : otherRole));
    mockApi({ findRole });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.role).toEqual(mockRole));

    // Simulate hash navigation to a different role. The hook's hash listener updates
    // routeState which re-fires the fetch effect. setInitialHash bypasses history
    // recording since we want to observe the hook's downstream nav, not this setup step.
    setInitialHash(`#${BASE_PATH}/${OTHER_ROLE_ID}`);
    act(() => {
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });

    // Between routeState update and the new fetch resolving, role must have been reset.
    expect(result.current.role).toBeNull();

    await waitFor(() => expect(result.current.role).toEqual(otherRole));
  });

  it('should re-fetch when retryRoleLoad is invoked and clear the error on success', async () => {
    let callCount = 0;
    const findRole = jest.fn(async () => {
      callCount += 1;
      if (callCount === 1) {
        throw Object.assign(new Error(TRANSIENT_ERROR_MESSAGE), { status: 500 });
      }
      return mockRole;
    });
    mockApi({ findRole });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.roleLoadErrorType).toBe('transient'));

    act(() => {
      result.current.retryRoleLoad();
    });

    await waitFor(() => expect(result.current.role).toEqual(mockRole));
    expect(result.current.roleLoadErrorType).toBeNull();
    expect(findRole).toHaveBeenCalledTimes(2);
  });

  it('should call deleteRole and navigate back on handleDeleteConfirm', async () => {
    const deleteRole = jest.fn().mockResolvedValue(undefined);
    mockApi({ findRole: jest.fn().mockResolvedValue(mockRole), deleteRole });

    const { result } = renderHook(() => useRolesPage(), { wrapper });

    await waitFor(() => expect(result.current.role).toEqual(mockRole));

    await act(async () => {
      await result.current.handleDeleteConfirm();
    });

    expect(deleteRole).toHaveBeenCalledWith(ROLE_ID);
    expect(getHashHistory()).toContain(BASE_PATH);
  });
});
