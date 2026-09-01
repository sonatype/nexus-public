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

import { renderHook, waitFor } from '@testing-library/react';

import { usePrivilegeProfile } from '../usePrivilegeProfile';

// Mock the three API hooks the profile hook composes.
const mockFindPrivilege = jest.fn();
const mockFetchRoles = jest.fn();
const mockFetchUsers = jest.fn();

jest.mock('../usePrivilegesApi', () => ({
  usePrivilegesApi: () => ({ findPrivilege: mockFindPrivilege }),
}));
jest.mock('../../roles/useRolesApi', () => ({
  useRolesApi: () => ({ fetchRoles: mockFetchRoles }),
}));
jest.mock('../../users/useUsersApi', () => ({
  useUsersApi: () => ({ fetchUsers: mockFetchUsers }),
}));

const PRIVILEGE = {
  id: 'nx-all',
  name: 'nx-all',
  type: 'wildcard',
  description: 'All permissions',
  readOnly: true,
  permission: 'nexus:*',
  properties: { pattern: 'nexus:*' },
};

describe('usePrivilegeProfile', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFindPrivilege.mockResolvedValue(PRIVILEGE);
    mockFetchRoles.mockResolvedValue([]);
    mockFetchUsers.mockResolvedValue([]);
  });

  it('loads the privilege plus roles/users when all reads succeed', async () => {
    const role = { id: 'r1', name: 'Role 1', privileges: ['nx-all'], roles: [] };
    const user = { userId: 'u1', roles: ['r1'] };
    mockFetchRoles.mockResolvedValue([role]);
    mockFetchUsers.mockResolvedValue([user]);

    const { result } = renderHook(() => usePrivilegeProfile('nx-all'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.privilege).toEqual(PRIVILEGE);
    expect(result.current.rolesUsing).toEqual([role]);
    expect(result.current.usersWithAccess).toEqual([user]);
    expect(result.current.error).toBeNull();
  });

  // NEXUS-54212: a user with only nexus:privileges:read (no roles:read/users:read) must
  // still be able to view the privilege. The roles/users 403s degrade to empty tabs
  // instead of failing the whole profile.
  it('still renders the privilege when fetchRoles/fetchUsers are denied (403)', async () => {
    const denied = new Error('You do not have permission to perform this action.');
    mockFetchRoles.mockRejectedValue(denied);
    mockFetchUsers.mockRejectedValue(denied);

    const { result } = renderHook(() => usePrivilegeProfile('nx-all'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.privilege).toEqual(PRIVILEGE);
    expect(result.current.rolesUsing).toEqual([]);
    expect(result.current.usersWithAccess).toEqual([]);
    // The privilege loaded successfully, so no error is surfaced.
    expect(result.current.error).toBeNull();
  });

  it('surfaces an error only when the privilege itself fails to load', async () => {
    mockFindPrivilege.mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => usePrivilegeProfile('nx-all'));

    await waitFor(() => expect(result.current.error).toBe('boom'));
    expect(result.current.privilege).toBeNull();
  });

  it('reports "Privilege not found" when the privilege does not exist', async () => {
    mockFindPrivilege.mockResolvedValue(null);

    const { result } = renderHook(() => usePrivilegeProfile('missing'));

    await waitFor(() => expect(result.current.error).toBe('Privilege not found'));
    expect(result.current.privilege).toBeNull();
  });
});
