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
import { useRoleModalContent } from '../useRoleModalContent';
import { useRolesApi } from '../useRolesApi';
import type { Role } from '../types';

jest.mock('../useRolesApi');

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const ROLE_NAME = 'test-role';
const OTHER_ROLE_NAME = 'other-role';
const TRANSIENT_ERROR_MESSAGE = 'Network error';

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

const mockOtherRole: Role = { ...mockRole, id: OTHER_ROLE_NAME, name: 'Other Role' };

function buildFindRole(implementation: (roleId: string) => Promise<Role | null>) {
  return jest.fn(implementation);
}

function mockApi(findRoleImpl: (roleId: string) => Promise<Role | null>) {
  const findRole = buildFindRole(findRoleImpl);
  mockUseRolesApi.mockReturnValue({
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchRoles: jest.fn().mockResolvedValue([]),
    fetchRoleReferences: jest.fn().mockResolvedValue([]),
    fetchRoleSources: jest.fn().mockResolvedValue([]),
    fetchRolesFromSource: jest.fn().mockResolvedValue([]),
    fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
    findRole,
    createRole: jest.fn(),
    updateRole: jest.fn(),
    deleteRole: jest.fn(),
  } as unknown as ReturnType<typeof useRolesApi>);
  return findRole;
}

describe('useRoleModalContent', () => {
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('should populate role and clear loading on successful fetch', async () => {
    mockApi(() => Promise.resolve(mockRole));
    const { result } = renderHook(() => useRoleModalContent(ROLE_NAME));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.role).toEqual(mockRole);
    expect(result.current.error).toBeUndefined();
    expect(result.current.errorType).toBeNull();
  });

  it('should set errorType=notFound when findRole returns null', async () => {
    mockApi(() => Promise.resolve(null));
    const { result } = renderHook(() => useRoleModalContent(ROLE_NAME));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.role).toBeNull();
    expect(result.current.errorType).toBe('notFound');
    expect(result.current.error).toBe('Role not found');
  });

  it('should set errorType=transient and log to console when findRole throws', async () => {
    const thrownError = new Error(TRANSIENT_ERROR_MESSAGE);
    mockApi(() => Promise.reject(thrownError));

    const { result } = renderHook(() => useRoleModalContent(ROLE_NAME));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.role).toBeNull();
    expect(result.current.errorType).toBe('transient');
    expect(result.current.error).toBe(TRANSIENT_ERROR_MESSAGE);
    expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load role:', thrownError);
  });

  // Stale-role protection was moved to the machine's `loading` state entry action
  // (see roleModalContentMachine.test.ts: 'should clear stale role state at the entry
  // to loading'). SecurityEntityModalContext keys the RoleModalContent mount by roleName
  // so the caller-side contract for switching roles is a remount, not a prop update -
  // this test verifies the caller-side contract by re-mounting.
  it('should reset state on remount with a new roleName', async () => {
    const findRole = jest.fn(async (roleId: string) => (roleId === ROLE_NAME ? mockRole : mockOtherRole));
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole,
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    } as unknown as ReturnType<typeof useRolesApi>);

    const first = renderHook(() => useRoleModalContent(ROLE_NAME));
    await waitFor(() => expect(first.result.current.role).toEqual(mockRole));
    first.unmount();

    const second = renderHook(() => useRoleModalContent(OTHER_ROLE_NAME));
    // On fresh mount, the new machine starts in `loading` with cleared role.
    expect(second.result.current.role).toBeNull();
    expect(second.result.current.loading).toBe(true);
    await waitFor(() => expect(second.result.current.role).toEqual(mockOtherRole));
  });

  it('should re-fetch and clear the error when retry is called', async () => {
    let callCount = 0;
    const findRole = jest.fn(async () => {
      callCount += 1;
      if (callCount === 1) throw new Error(TRANSIENT_ERROR_MESSAGE);
      return mockRole;
    });
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRoles: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue([]),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole,
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    } as unknown as ReturnType<typeof useRolesApi>);

    const { result } = renderHook(() => useRoleModalContent(ROLE_NAME));

    await waitFor(() => expect(result.current.errorType).toBe('transient'));

    act(() => {
      result.current.retry();
    });

    await waitFor(() => expect(result.current.role).toEqual(mockRole));
    expect(result.current.errorType).toBeNull();
    expect(result.current.error).toBeUndefined();
    expect(findRole).toHaveBeenCalledTimes(2);
  });

  it('should not update state after unmount', async () => {
    let resolveFn: (value: Role | null) => void = () => {};
    mockApi(
      () =>
        new Promise<Role | null>((resolve) => {
          resolveFn = resolve;
        }),
    );

    const { result, unmount } = renderHook(() => useRoleModalContent(ROLE_NAME));
    unmount();

    // Resolve after unmount - should NOT trigger a "state update on unmounted component" warning.
    await act(async () => {
      resolveFn(mockRole);
    });
    expect(result.current.role).toBeNull();
    expect(result.current.loading).toBe(true);
  });
});
