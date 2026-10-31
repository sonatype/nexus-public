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

import { act, renderHook } from '@testing-library/react';

import type { User } from '../../../security/users/types';
import type { MergedApiEndpoint } from '../../utils/mergeSwaggerPermissions';
import {
  useEndpointAccess,
  type RoleWithAccessSummary,
  type UseEndpointAccessResult,
  type UserWithAccessSummary,
} from '../../hooks/useEndpointAccess';
import { useWhoHasAccessTab } from '../useWhoHasAccessTab';

jest.mock('../../hooks/useEndpointAccess');

const mockedUseEndpointAccess = useEndpointAccess as jest.MockedFunction<typeof useEndpointAccess>;

function baseRow(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/foo',
    fullPath: '/service/rest/v1/foo',
    tag: 'Test',
    permission: {
      httpMethod: 'GET',
      pathPattern: '/service/rest/v1/foo',
      permissions: [{ permission: 'nexus:test:read', logical: 'AND' }],
      description: null,
      tag: null,
      authenticated: true,
    },
    ...overrides,
  };
}

function makeUser(overrides: Partial<User> = {}): User {
  return {
    userId: 'alice',
    realm: 'default',
    source: 'default',
    firstName: 'Alice',
    lastName: 'Smith',
    emailAddress: 'alice@example.com',
    status: 'active',
    roles: ['role-a'],
    readOnly: false,
    ...overrides,
  };
}

function makeUserWithAccess(user: User, grantingRoleIds: string[] = ['role-a']): UserWithAccessSummary {
  return { user, grantingRoleIds };
}

function makeRole(id: string, name: string): RoleWithAccessSummary {
  return {
    role: {
      id,
      version: '1',
      source: 'default',
      name,
      description: '',
      readOnly: false,
      privileges: [],
      roles: [],
    },
    userCount: 0,
  };
}

const idleAccess: UseEndpointAccessResult = {
  loading: false,
  error: null,
  qualifyingRoles: [],
  usersWithAccess: [],
  noMappedPermissions: false,
  refetch: jest.fn(),
};

describe('useWhoHasAccessTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseEndpointAccess.mockReturnValue(idleAccess);
  });

  describe('isAnonymous', () => {
    it('is false when permission is null (unmapped endpoint)', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({ row: baseRow({ permission: null }), active: true })
      );
      expect(result.current.isAnonymous).toBe(false);
    });

    it('is false when the endpoint requires authentication', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({ row: baseRow(), active: true })
      );
      expect(result.current.isAnonymous).toBe(false);
    });

    it('is true when the endpoint allows unauthenticated access', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({
          row: baseRow({
            permission: {
              httpMethod: 'GET',
              pathPattern: '/service/rest/v1/foo',
              permissions: [],
              description: null,
              tag: null,
              authenticated: false,
            },
          }),
          active: true,
        })
      );
      expect(result.current.isAnonymous).toBe(true);
    });
  });

  describe('requirementCount and requirementMode', () => {
    it('reports 0 requirements and defaults mode to ALL when permission is null', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({ row: baseRow({ permission: null }), active: true })
      );
      expect(result.current.requirementCount).toBe(0);
      expect(result.current.requirementMode).toBe('ALL');
    });

    it('reports 0 requirements and ALL when permissions array is empty', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({
          row: baseRow({
            permission: {
              httpMethod: 'GET',
              pathPattern: '/service/rest/v1/foo',
              permissions: [],
              description: null,
              tag: null,
              authenticated: true,
            },
          }),
          active: true,
        })
      );
      expect(result.current.requirementCount).toBe(0);
      expect(result.current.requirementMode).toBe('ALL');
    });

    it('reports 1 requirement and ALL for a single AND requirement', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({ row: baseRow(), active: true })
      );
      expect(result.current.requirementCount).toBe(1);
      expect(result.current.requirementMode).toBe('ALL');
    });

    it('reports N requirements and ALL when the first requirement is AND', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({
          row: baseRow({
            permission: {
              httpMethod: 'GET',
              pathPattern: '/service/rest/v1/foo',
              permissions: [
                { permission: 'a', logical: 'AND' },
                { permission: 'b', logical: 'AND' },
                { permission: 'c', logical: 'AND' },
              ],
              description: null,
              tag: null,
              authenticated: true,
            },
          }),
          active: true,
        })
      );
      expect(result.current.requirementCount).toBe(3);
      expect(result.current.requirementMode).toBe('ALL');
    });

    it('reports ANY when the first requirement is OR', () => {
      const { result } = renderHook(() =>
        useWhoHasAccessTab({
          row: baseRow({
            permission: {
              httpMethod: 'GET',
              pathPattern: '/service/rest/v1/foo',
              permissions: [
                { permission: 'a', logical: 'OR' },
                { permission: 'b', logical: 'OR' },
              ],
              description: null,
              tag: null,
              authenticated: true,
            },
          }),
          active: true,
        })
      );
      expect(result.current.requirementCount).toBe(2);
      expect(result.current.requirementMode).toBe('ANY');
    });
  });

  describe('filteredUsers', () => {
    const users: UserWithAccessSummary[] = [
      makeUserWithAccess(makeUser({ userId: 'alice', firstName: 'Alice', lastName: 'Anderson', emailAddress: 'alice@acme.com' })),
      makeUserWithAccess(makeUser({ userId: 'bob', firstName: 'Bob', lastName: 'Bailey', emailAddress: 'bob@example.org' })),
      makeUserWithAccess(makeUser({ userId: 'carol', firstName: 'Carol', lastName: 'Chen', emailAddress: 'carol@acme.com' })),
    ];

    beforeEach(() => {
      mockedUseEndpointAccess.mockReturnValue({ ...idleAccess, usersWithAccess: users });
    });

    it('returns every user when the filter is empty', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      expect(result.current.filteredUsers).toHaveLength(3);
    });

    it('returns every user when the filter is whitespace only', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('   '));
      expect(result.current.filteredUsers).toHaveLength(3);
    });

    it('matches on userId case-insensitively', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('ALICE'));
      expect(result.current.filteredUsers.map((u) => u.user.userId)).toEqual(['alice']);
    });

    it('matches on firstName', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('bob'));
      expect(result.current.filteredUsers.map((u) => u.user.userId)).toEqual(['bob']);
    });

    it('matches on lastName', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('chen'));
      expect(result.current.filteredUsers.map((u) => u.user.userId)).toEqual(['carol']);
    });

    it('matches on emailAddress', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('acme.com'));
      expect(result.current.filteredUsers.map((u) => u.user.userId).sort()).toEqual(['alice', 'carol']);
    });

    it('returns an empty list when nothing matches', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('nobody'));
      expect(result.current.filteredUsers).toEqual([]);
    });

    it('trims whitespace around the query before matching', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      act(() => result.current.setUserFilter('   bob   '));
      expect(result.current.filteredUsers.map((u) => u.user.userId)).toEqual(['bob']);
    });
  });

  describe('roleLabelById', () => {
    it('is empty when there are no qualifying roles', () => {
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      expect(result.current.roleLabelById.size).toBe(0);
    });

    it('maps each qualifying role id to its display name', () => {
      mockedUseEndpointAccess.mockReturnValue({
        ...idleAccess,
        qualifyingRoles: [makeRole('role-a', 'Auditors'), makeRole('role-b', 'Admins')],
      });
      const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
      expect(result.current.roleLabelById.get('role-a')).toBe('Auditors');
      expect(result.current.roleLabelById.get('role-b')).toBe('Admins');
    });
  });

  it('passes-through loading/error/noMappedPermissions/qualifyingRoles from useEndpointAccess', () => {
    const qualifying = [makeRole('role-a', 'Auditors')];
    mockedUseEndpointAccess.mockReturnValue({
      ...idleAccess,
      loading: true,
      error: 'boom',
      noMappedPermissions: true,
      qualifyingRoles: qualifying,
    });
    const { result } = renderHook(() => useWhoHasAccessTab({ row: baseRow(), active: true }));
    expect(result.current.loading).toBe(true);
    expect(result.current.error).toBe('boom');
    expect(result.current.noMappedPermissions).toBe(true);
    expect(result.current.qualifyingRoles).toBe(qualifying);
  });
});
