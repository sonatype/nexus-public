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
import { useUserTreePreview } from '../useUserTreePreview';
import { User } from '../types';

const mockCombinedRoleTree = jest.fn();

jest.mock('../../roles/useRoleTree', () => ({
  useCombinedRoleTree: (...args: unknown[]) => mockCombinedRoleTree(...args),
}));

const roleTreeReturn = (tree: unknown[] = []) => ({
  tree,
  loading: false,
  error: null,
  toggleExpand: jest.fn(),
  expandAll: jest.fn(),
  collapseAll: jest.fn(),
});

const localUser: User = {
  userId: 'jsmith',
  realm: 'default',
  source: 'default',
  firstName: 'John',
  lastName: 'Smith',
  emailAddress: 'js@example.com',
  status: 'active',
  roles: ['nx-admin'],
};

const externalUser: User = {
  userId: 'ldap-user',
  realm: 'LDAP',
  source: 'LDAP',
  firstName: 'Ldap',
  lastName: 'User',
  emailAddress: 'ldap@example.com',
  status: 'active',
  roles: [],
};

describe('useUserTreePreview', () => {
  beforeEach(() => {
    mockCombinedRoleTree.mockReset();
  });

  it('returns an empty tree when the combined role tree is empty', () => {
    mockCombinedRoleTree.mockReturnValue(roleTreeReturn([]));
    const { result } = renderHook(() =>
      useUserTreePreview(['nx-admin'], localUser),
    );
    expect(result.current.tree).toEqual([]);
  });

  it('wraps the combined role tree under a synthetic user root when user is provided', () => {
    const roleNode = {
      id: 'role:nx-admin',
      entityId: 'nx-admin',
      name: 'Admin',
      type: 'role',
      inherited: false,
      expanded: false,
      isVisible: true,
      children: [],
    };
    mockCombinedRoleTree.mockReturnValue(roleTreeReturn([roleNode]));
    const { result } = renderHook(() =>
      useUserTreePreview(['nx-admin'], localUser),
    );
    expect(result.current.tree).toHaveLength(1);
    const root = result.current.tree[0];
    expect(root.id).toBe('user:jsmith:default');
    expect(root.name).toBe('User: jsmith (Local)');
    expect(root.children).toEqual([roleNode]);
  });

  it('labels the user root with the external source name for external users', () => {
    const roleNode = {
      id: 'role:x',
      entityId: 'x',
      name: 'X',
      type: 'role',
      inherited: false,
      expanded: false,
      isVisible: true,
      children: [],
    };
    mockCombinedRoleTree.mockReturnValue(roleTreeReturn([roleNode]));
    const { result } = renderHook(() =>
      useUserTreePreview(['x'], externalUser),
    );
    expect(result.current.tree[0].name).toBe('User: ldap-user (LDAP)');
    expect(result.current.tree[0].id).toBe('user:ldap-user:LDAP');
  });

  it('returns the bare combined role tree when no user is provided (create mode)', () => {
    const roleNode = {
      id: 'role:a',
      entityId: 'a',
      name: 'A',
      type: 'role',
      inherited: false,
      expanded: false,
      isVisible: true,
      children: [],
    };
    mockCombinedRoleTree.mockReturnValue(roleTreeReturn([roleNode]));
    const { result } = renderHook(() => useUserTreePreview(['a']));
    expect(result.current.tree).toEqual([roleNode]);
  });

  it('passes the current search term through to useCombinedRoleTree', () => {
    mockCombinedRoleTree.mockReturnValue(roleTreeReturn([]));
    const { result } = renderHook(() =>
      useUserTreePreview(['nx-admin'], localUser),
    );
    act(() => {
      result.current.setSearchTerm('admin');
    });
    const lastCall =
      mockCombinedRoleTree.mock.calls[mockCombinedRoleTree.mock.calls.length - 1];
    expect(lastCall[0]).toEqual(['nx-admin']);
    expect(lastCall[1]).toEqual({ searchTerm: 'admin' });
  });

  it('proxies loading and error from useCombinedRoleTree', () => {
    mockCombinedRoleTree.mockReturnValue({
      tree: [],
      loading: true,
      error: 'boom',
      toggleExpand: jest.fn(),
      expandAll: jest.fn(),
      collapseAll: jest.fn(),
    });
    const { result } = renderHook(() =>
      useUserTreePreview(['nx-admin'], localUser),
    );
    expect(result.current.loading).toBe(true);
    expect(result.current.error).toBe('boom');
  });
});
