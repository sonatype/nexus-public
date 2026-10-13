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
import { useRoleEffectivePrivileges } from '../useRoleEffectivePrivileges';
import { useRolesApi } from '../useRolesApi';
import { usePrivilegesApi } from '../../privileges/usePrivilegesApi';
import type { Role } from '../types';
import type { Privilege } from '../../privileges/types';

jest.mock('../useRolesApi');
jest.mock('../../privileges/usePrivilegesApi');

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;
const mockUsePrivilegesApi = usePrivilegesApi as jest.MockedFunction<typeof usePrivilegesApi>;

const TARGET_ROLE_ID = 'target';
const NESTED_ROLE_ID = 'nested';
const CYCLE_ROLE_ID = 'cycle';

function makePrivilege(id: string): Privilege {
  return {
    id,
    name: id,
    description: `desc-${id}`,
    type: 'application',
    readOnly: false,
    properties: {},
  } as any;
}

function makeRole(id: string, privileges: string[], roles: string[]): Role {
  return {
    id,
    version: '1',
    source: 'Default',
    name: id,
    description: '',
    readOnly: false,
    privileges,
    roles,
  };
}

function mockApis(privileges: Privilege[], roles: Role[]) {
  mockUseRolesApi.mockReturnValue({
    fetchRoles: jest.fn().mockResolvedValue(roles),
  } as any);
  mockUsePrivilegesApi.mockReturnValue({
    fetchPrivileges: jest.fn().mockResolvedValue({ data: privileges }),
  } as any);
}

describe('useRoleEffectivePrivileges', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns an empty privileges array when snapshot is null (derivation short-circuits)', async () => {
    mockApis([makePrivilege('p1')], [makeRole(TARGET_ROLE_ID, ['p1'], [])]);

    const { result } = renderHook(() => useRoleEffectivePrivileges(null));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.privileges).toEqual([]);
  });

  it('returns direct privileges when snapshot has no contained roles', async () => {
    const privileges = [makePrivilege('p1'), makePrivilege('p2')];
    const persistedRole = makeRole(TARGET_ROLE_ID, ['p1'], []);
    mockApis(privileges, [persistedRole]);

    const { result } = renderHook(() =>
      useRoleEffectivePrivileges({
        id: TARGET_ROLE_ID,
        privileges: ['p1', 'p2'],
        roles: [],
      }),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    // Both privileges come through even though persisted role only has p1 -
    // the snapshot (not the persisted role) drives the walk. AC4.
    expect(result.current.privileges.map((p) => p.id).sort()).toEqual(['p1', 'p2']);
  });

  it('walks contained roles recursively and attributes granted-by', async () => {
    const privileges = [makePrivilege('p1'), makePrivilege('p2')];
    const nestedRole = makeRole(NESTED_ROLE_ID, ['p2'], []);
    const persistedRole = makeRole(TARGET_ROLE_ID, ['p1'], [NESTED_ROLE_ID]);
    mockApis(privileges, [persistedRole, nestedRole]);

    const { result } = renderHook(() =>
      useRoleEffectivePrivileges({
        id: TARGET_ROLE_ID,
        privileges: ['p1'],
        roles: [NESTED_ROLE_ID],
      }),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    const byId = new Map(result.current.privileges.map((p) => [p.id, p]));
    expect(byId.get('p1')?.grantedByRoleIds).toEqual([TARGET_ROLE_ID]);
    expect(byId.get('p2')?.grantedByRoleIds).toEqual([NESTED_ROLE_ID]);
  });

  it('reflects pending changes to snapshot roles without re-fetching', async () => {
    const privileges = [makePrivilege('p1'), makePrivilege('p2')];
    const nestedRole = makeRole(NESTED_ROLE_ID, ['p2'], []);
    const persistedRole = makeRole(TARGET_ROLE_ID, ['p1'], []); // persisted has no nested
    mockApis(privileges, [persistedRole, nestedRole]);

    // Snapshot pretends the user has just added nested via the picker
    const { result } = renderHook(() =>
      useRoleEffectivePrivileges({
        id: TARGET_ROLE_ID,
        privileges: ['p1'],
        roles: [NESTED_ROLE_ID],
      }),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.privileges.map((p) => p.id).sort()).toEqual(['p1', 'p2']);
  });

  it('handles cycles without infinite recursion', async () => {
    const privileges = [makePrivilege('p1')];
    // A contains B; B contains A. Both grant p1.
    const roleA = makeRole(TARGET_ROLE_ID, ['p1'], [CYCLE_ROLE_ID]);
    const roleB = makeRole(CYCLE_ROLE_ID, ['p1'], [TARGET_ROLE_ID]);
    mockApis(privileges, [roleA, roleB]);

    const { result } = renderHook(() =>
      useRoleEffectivePrivileges({
        id: TARGET_ROLE_ID,
        privileges: ['p1'],
        roles: [CYCLE_ROLE_ID],
      }),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    // p1 is granted by both roles; dedupe via grantedByRoleIds. No infinite loop.
    expect(result.current.privileges).toHaveLength(1);
    expect(result.current.privileges[0].grantedByRoleIds.sort()).toEqual(
      [TARGET_ROLE_ID, CYCLE_ROLE_ID].sort(),
    );
  });

  it('reports a fetch error', async () => {
    mockUseRolesApi.mockReturnValue({
      fetchRoles: jest.fn().mockRejectedValue(new Error('down')),
    } as any);
    mockUsePrivilegesApi.mockReturnValue({
      fetchPrivileges: jest.fn().mockResolvedValue({ data: [] }),
    } as any);

    const { result } = renderHook(() =>
      useRoleEffectivePrivileges({
        id: TARGET_ROLE_ID,
        privileges: [],
        roles: [],
      }),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('down');
    expect(result.current.privileges).toEqual([]);
  });

  it('surfaces direct snapshot privileges when the fetched roles collection is empty', async () => {
    mockApis([makePrivilege('p1')], []);

    const { result } = renderHook(() =>
      useRoleEffectivePrivileges({
        id: TARGET_ROLE_ID,
        privileges: ['p1'],
        roles: [],
      }),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.privileges.map((p) => p.id)).toEqual(['p1']);
  });
});
