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
import { useAssignedUsers } from '../useAssignedUsers';
import { useUsersApi } from '../../users/useUsersApi';
import type { User } from '../../users/types';

jest.mock('../../users/useUsersApi');

const mockUseUsersApi = useUsersApi as jest.MockedFunction<typeof useUsersApi>;

function makeUser(userId: string, roles: string[]): User {
  return {
    userId,
    realm: 'default',
    source: 'default',
    firstName: 'First',
    lastName: 'Last',
    emailAddress: `${userId}@example.com`,
    status: 'active',
    roles,
  };
}

describe('useAssignedUsers', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns only users whose roles array includes the target roleId', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn().mockResolvedValue([
        makeUser('alice', ['admin', 'target']),
        makeUser('bob', ['other']),
        makeUser('carol', ['target', 'yet-another']),
      ]),
    } as any);

    const { result } = renderHook(() => useAssignedUsers('target'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.users.map((u) => u.userId).sort()).toEqual(['alice', 'carol']);
    expect(result.current.error).toBeNull();
  });

  it('returns an empty array when no user has the role', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn().mockResolvedValue([makeUser('a', ['other'])]),
    } as any);

    const { result } = renderHook(() => useAssignedUsers('missing-role'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.users).toEqual([]);
  });

  it('returns an empty users array when roleId is null (filter short-circuits)', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest
        .fn()
        .mockResolvedValue([makeUser('alice', ['target'])]),
    } as any);

    const { result } = renderHook(() => useAssignedUsers(null));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.users).toEqual([]);
  });

  it('reports fetch errors', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn().mockRejectedValue(new Error('down')),
    } as any);

    const { result } = renderHook(() => useAssignedUsers('r'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('down');
  });
});
