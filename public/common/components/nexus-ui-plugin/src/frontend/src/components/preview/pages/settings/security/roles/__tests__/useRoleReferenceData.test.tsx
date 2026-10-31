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
import { useRoleReferenceData } from '../useRoleReferenceData';
import { useRolesApi } from '../useRolesApi';

jest.mock('../useRolesApi');

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const CURRENT_ROLE_ID = 'current';

describe('useRoleReferenceData', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches privileges + roles on mount and returns both', async () => {
    mockUseRolesApi.mockReturnValue({
      fetchPrivilegeReferences: jest
        .fn()
        .mockResolvedValue([{ id: 'p1', name: 'p1', description: 'first' }]),
      fetchRoleReferences: jest
        .fn()
        .mockResolvedValue([{ id: 'other', name: 'Other Role' }]),
    } as any);

    const { result } = renderHook(() => useRoleReferenceData(CURRENT_ROLE_ID));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.allPrivileges).toEqual([{ id: 'p1', name: 'p1', description: 'first' }]);
    expect(result.current.allRoles).toEqual([{ id: 'other', name: 'Other Role' }]);
    expect(result.current.error).toBeNull();
  });

  it('excludes the current role from allRoles to prevent self-nesting', async () => {
    mockUseRolesApi.mockReturnValue({
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue([
        { id: CURRENT_ROLE_ID, name: 'Current Role' },
        { id: 'other', name: 'Other Role' },
      ]),
    } as any);

    const { result } = renderHook(() => useRoleReferenceData(CURRENT_ROLE_ID));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.allRoles.map((r) => r.id)).toEqual(['other']);
  });

  it('deduplicates roles by id (same role can appear from multiple sources)', async () => {
    mockUseRolesApi.mockReturnValue({
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      fetchRoleReferences: jest.fn().mockResolvedValue([
        { id: 'duped', name: 'Duped' },
        { id: 'duped', name: 'Duped (from LDAP)' },
        { id: 'unique', name: 'Unique' },
      ]),
    } as any);

    const { result } = renderHook(() => useRoleReferenceData(CURRENT_ROLE_ID));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.allRoles.map((r) => r.id).sort()).toEqual(['duped', 'unique']);
  });

  it('reports fetch errors', async () => {
    mockUseRolesApi.mockReturnValue({
      fetchPrivilegeReferences: jest.fn().mockRejectedValue(new Error('privs down')),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
    } as any);

    const { result } = renderHook(() => useRoleReferenceData(CURRENT_ROLE_ID));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('privs down');
    expect(result.current.allPrivileges).toEqual([]);
    expect(result.current.allRoles).toEqual([]);
  });
});
