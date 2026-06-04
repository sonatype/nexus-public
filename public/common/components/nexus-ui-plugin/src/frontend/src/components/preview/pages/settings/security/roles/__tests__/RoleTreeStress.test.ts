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

import { renderHook, waitFor, act } from '@testing-library/react';
import { useRoleTree } from '../useRoleTree';
import { useRolesApi } from '../useRolesApi';
import { usePrivilegesApi } from '../../privileges/usePrivilegesApi';
import { useContentSelectorsApi } from '../../../repository/selectors/useContentSelectorsApi';

// Mock the APIs
jest.mock('../useRolesApi');
jest.mock('../../privileges/usePrivilegesApi');
jest.mock('../../../repository/selectors/useContentSelectorsApi');

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;
const mockUsePrivilegesApi = usePrivilegesApi as jest.MockedFunction<typeof usePrivilegesApi>;
const mockUseContentSelectorsApi = useContentSelectorsApi as jest.MockedFunction<typeof useContentSelectorsApi>;

const scenarios = {
  diamond: {
    roles: [
      { id: 'set-role-top', name: 'Set Role Top', roles: ['set-role-left', 'set-role-right'], privileges: [], source: 'default', readOnly: false, description: 'Top', version: '1' },
      { id: 'set-role-left', name: 'Set Role Left', roles: [], privileges: ['set-base-privilege'], source: 'default', readOnly: false, description: 'Left', version: '1' },
      { id: 'set-role-right', name: 'Set Role Right', roles: [], privileges: ['set-base-privilege'], source: 'default', readOnly: false, description: 'Right', version: '1' },
    ],
    privileges: [
      { id: 'set-base-privilege', name: 'set-base-privilege', type: 'repository-view', properties: { format: 'maven2', repository: 'set-wide-group-mega', actions: 'browse,read' }, description: 'Base', permission: 'nexus:repository-view:maven2:set-wide-group-mega:browse,read', readOnly: false, version: '1' }
    ],
    selectors: []
  },
  selectorMaze: {
    roles: [
      { id: 'set-role-selector-master', name: 'Set Role Selector Master', roles: [], privileges: ['set-priv-csel-1', 'set-priv-csel-2', 'set-priv-csel-3'], source: 'default', readOnly: false, description: 'Master', version: '1' }
    ],
    privileges: [
      { id: 'set-priv-csel-1', name: 'set-priv-csel-1', type: 'repository-content-selector', properties: { format: 'maven2', repository: '*', actions: 'browse,read', contentSelector: 'set-csel-1' }, description: 'Priv 1', permission: '...', readOnly: false, version: '1' },
      { id: 'set-priv-csel-2', name: 'set-priv-csel-2', type: 'repository-content-selector', properties: { format: 'maven2', repository: '*', actions: 'browse,read', contentSelector: 'set-csel-2' }, description: 'Priv 2', permission: '...', readOnly: false, version: '1' },
      { id: 'set-priv-csel-3', name: 'set-priv-csel-3', type: 'repository-content-selector', properties: { format: 'maven2', repository: '*', actions: 'browse,read', contentSelector: 'set-csel-3' }, description: 'Priv 3', permission: '...', readOnly: false, version: '1' }
    ],
    selectors: [
      { name: 'set-csel-1', type: 'csel', description: 'CSEL 1', expression: 'format == "maven2" and path =^ "/org/set1"' },
      { name: 'set-csel-2', type: 'csel', description: 'CSEL 2', expression: 'format == "maven2" and path =^ "/org/set2"' },
      { name: 'set-csel-3', type: 'csel', description: 'CSEL 3', expression: 'format == "maven2" and path =^ "/org/set3"' }
    ]
  }
};

describe('useRoleTree Stress Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should handle Diamond Inheritance (Scenario C)', async () => {
    const { diamond } = scenarios;
    mockUseRolesApi.mockReturnValue({ fetchRoles: jest.fn().mockResolvedValue(diamond.roles) } as any);
    mockUsePrivilegesApi.mockReturnValue({ fetchPrivileges: jest.fn().mockResolvedValue({ data: diamond.privileges, total: 1 }) } as any);
    mockUseContentSelectorsApi.mockReturnValue({ fetchContentSelectors: jest.fn().mockResolvedValue(diamond.selectors) } as any);

    const { result } = renderHook(() => useRoleTree('set-role-top'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    // Verify tree structure
    expect(result.current.tree).toHaveLength(1);
    const root = result.current.tree[0];
    expect(root.entityId).toBe('set-role-top');
    expect(root.children).toHaveLength(2); // set-role-left and set-role-right

    // Verify diamond inheritance resolution in effective privileges
    // Both paths lead to the same privilege, but it should only appear once in the flattened list.
    expect(result.current.effectivePrivileges).toHaveLength(1);
    expect(result.current.effectivePrivileges[0].id).toBe('set-base-privilege');
  });

  it('should handle Selector Maze (Scenario D)', async () => {
    const { selectorMaze } = scenarios;
    mockUseRolesApi.mockReturnValue({ fetchRoles: jest.fn().mockResolvedValue(selectorMaze.roles) } as any);
    mockUsePrivilegesApi.mockReturnValue({ fetchPrivileges: jest.fn().mockResolvedValue({ data: selectorMaze.privileges, total: 3 }) } as any);
    mockUseContentSelectorsApi.mockReturnValue({ fetchContentSelectors: jest.fn().mockResolvedValue(selectorMaze.selectors) } as any);

    const { result } = renderHook(() => useRoleTree('set-role-selector-master'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    // Verify tree structure
    expect(result.current.tree).toHaveLength(1);
    const root = result.current.tree[0];
    expect(root.children).toHaveLength(3); // 3 privileges

    // Expand one privilege to see the content selector child
    act(() => {
      result.current.toggleExpand(root.children![0].id);
    });
    
    // Check if content selector is a child of the privilege
    await waitFor(() => {
      const updatedPriv1 = result.current.tree[0].children![0];
      expect(updatedPriv1.expanded).toBe(true);
    });

    const priv1 = result.current.tree[0].children![0];
    expect(priv1.children).toHaveLength(1);
    expect(priv1.children![0].type).toBe('content-selector');
    expect(priv1.children![0].csel).toBe('format == "maven2" and path =^ "/org/set1"');
  });

  it('should detect and prevent infinite loops in circular references', async () => {
    const circularRoles = [
      { id: 'role-a', name: 'Role A', roles: ['role-b'], privileges: [], source: 'default', readOnly: false, version: '1' },
      { id: 'role-b', name: 'Role B', roles: ['role-a'], privileges: [], source: 'default', readOnly: false, version: '1' },
    ];
    
    mockUseRolesApi.mockReturnValue({ fetchRoles: jest.fn().mockResolvedValue(circularRoles) } as any);
    mockUsePrivilegesApi.mockReturnValue({ fetchPrivileges: jest.fn().mockResolvedValue({ data: [], total: 0 }) } as any);
    mockUseContentSelectorsApi.mockReturnValue({ fetchContentSelectors: jest.fn().mockResolvedValue([]) } as any);

    const { result } = renderHook(() => useRoleTree('role-a'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    // role-a -> role-b -> role-a (circular)
    const root = result.current.tree[0];
    const roleB = root.children![0];
    expect(roleB.entityId).toBe('role-b');
    expect(roleB.children).toHaveLength(1);
    
    const roleACircular = roleB.children![0];
    expect(roleACircular.entityId).toBe('role-a');
    expect(roleACircular.isCircular).toBe(true);
    expect(roleACircular.children).toHaveLength(0); // Recursion should stop
  });
});
