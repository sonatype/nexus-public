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
import { useRepositoryTree } from '../useRepositoryTree';
import { useRepositoriesApi } from '../useRepositoriesApi';

jest.mock('../useRepositoriesApi');

const mockUseRepositoriesApi = useRepositoriesApi as jest.MockedFunction<typeof useRepositoriesApi>;

describe('useRepositoryTree Stress Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should handle Deep Tower (Scenario A - 7 levels deep)', async () => {
    // Mock 7 levels of group nesting: L1 -> L2 -> L3 -> L4 -> L5 -> L6 -> hosted-leaf
    const repos = [
      { name: 'set-tower-group-L1', type: 'group', format: 'maven2', attributes: { group: { memberNames: ['set-tower-group-L2'] } }, status: { online: true }, online: true },
      { name: 'set-tower-group-L2', type: 'group', format: 'maven2', attributes: { group: { memberNames: ['set-tower-group-L3'] } }, status: { online: true }, online: true },
      { name: 'set-tower-group-L3', type: 'group', format: 'maven2', attributes: { group: { memberNames: ['set-tower-group-L4'] } }, status: { online: true }, online: true },
      { name: 'set-tower-group-L4', type: 'group', format: 'maven2', attributes: { group: { memberNames: ['set-tower-group-L5'] } }, status: { online: true }, online: true },
      { name: 'set-tower-group-L5', type: 'group', format: 'maven2', attributes: { group: { memberNames: ['set-tower-group-L6'] } }, status: { online: true }, online: true },
      { name: 'set-tower-group-L6', type: 'group', format: 'maven2', attributes: { group: { memberNames: ['hosted-leaf'] } }, status: { online: true }, online: true },
      { name: 'hosted-leaf', type: 'hosted', format: 'maven2', attributes: { storage: { blobStoreName: 'default' } }, status: { online: true }, online: true },
    ];

    mockUseRepositoriesApi.mockReturnValue({ fetchRepositories: jest.fn().mockResolvedValue(repos) } as any);

    const { result } = renderHook(() => useRepositoryTree('set-tower-group-L1'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    // Verify deep nesting
    let current = result.current.tree[0];
    for (let i = 1; i <= 6; i++) {
      expect(current.name).toBe(`set-tower-group-L${i}`);
      expect(current.children).toHaveLength(1);
      current = current.children![0];
    }
    expect(current.name).toBe('hosted-leaf');
  });

  it('should handle Wide Fan (Scenario B - 60 members)', async () => {
    // Mock group with 60 members
    const memberNames = Array.from({ length: 60 }, (_, i) => `set-wide-member-${i + 1}`);
    const members = memberNames.map(name => ({
      name, type: 'hosted', format: 'maven2', attributes: { storage: { blobStoreName: 'default' } }, status: { online: true }, online: true
    }));
    const group = {
      name: 'set-wide-group-mega', type: 'group', format: 'maven2', attributes: { group: { memberNames } }, status: { online: true }, online: true
    };

    mockUseRepositoriesApi.mockReturnValue({ fetchRepositories: jest.fn().mockResolvedValue([group, ...members]) } as any);

    const { result } = renderHook(() => useRepositoryTree('set-wide-group-mega'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.tree[0].children).toHaveLength(60);
    expect(result.current.tree[0].children![59].name).toBe('set-wide-member-60');
  });

  it('should detect circular repository dependencies', async () => {
    const repos = [
      { name: 'group-a', type: 'group', attributes: { group: { memberNames: ['group-b'] } }, status: { online: true }, online: true },
      { name: 'group-b', type: 'group', attributes: { group: { memberNames: ['group-a'] } }, status: { online: true }, online: true },
    ];

    mockUseRepositoriesApi.mockReturnValue({ fetchRepositories: jest.fn().mockResolvedValue(repos) } as any);

    const { result } = renderHook(() => useRepositoryTree('group-a'));

    await waitFor(() => expect(result.current.loading).toBe(false));

    const root = result.current.tree[0];
    const groupB = root.children![0];
    expect(groupB.name).toBe('group-b');
    expect(groupB.children).toHaveLength(1);
    
    const groupACircular = groupB.children![0];
    expect(groupACircular.name).toBe('group-a');
    expect(groupACircular.isCircular).toBe(true);
    expect(groupACircular.children).toBeUndefined();
  });
});
