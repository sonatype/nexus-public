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
import { restClient } from '../../../../../../../interface/api';
import {
  useStructureComponentSearch,
  getAllMemberRepos,
} from '../useStructureComponentSearch';
import type { RepositoryTreeNode } from '../useRepositoryTree';

jest.mock('../../../../../../../interface/api');

describe('getAllMemberRepos', () => {
  it('returns empty set for empty tree', () => {
    expect(getAllMemberRepos([])).toEqual(new Set());
  });

  it('returns leaf repo names (hosted and proxy) only', () => {
    const tree: RepositoryTreeNode[] = [
      {
        id: 'g1',
        name: 'g1',
        type: 'group',
        format: 'maven2',
        status: 'online',
        online: true,
        children: [
          {
            id: 'g1::h1',
            name: 'hosted-repo',
            type: 'hosted',
            format: 'maven2',
            status: 'online',
            online: true,
            blobStore: 'default',
          },
          {
            id: 'g1::p1',
            name: 'proxy-repo',
            type: 'proxy',
            format: 'maven2',
            status: 'online',
            online: true,
            remoteUrl: 'https://example.com',
          },
        ],
      },
    ];
    const result = getAllMemberRepos(tree);
    expect(result).toEqual(new Set(['hosted-repo', 'proxy-repo']));
  });

  it('flattens nested groups to leaf repos', () => {
    const tree: RepositoryTreeNode[] = [
      {
        id: 'outer',
        name: 'outer',
        type: 'group',
        format: 'maven2',
        status: 'online',
        online: true,
        children: [
          {
            id: 'outer::inner',
            name: 'inner',
            type: 'group',
            format: 'maven2',
            status: 'online',
            online: true,
            children: [
              {
                id: 'outer::inner::leaf',
                name: 'leaf-repo',
                type: 'hosted',
                format: 'maven2',
                status: 'online',
                online: true,
                blobStore: 'default',
              },
            ],
          },
        ],
      },
    ];
    const result = getAllMemberRepos(tree);
    expect(result).toEqual(new Set(['leaf-repo']));
  });
});

describe('useStructureComponentSearch', () => {
  const memberRepos = new Set(['my-repo', 'other-repo']);
  const tree: RepositoryTreeNode[] = [];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns empty repos and no loading for empty query', () => {
    const { result } = renderHook(() =>
      useStructureComponentSearch('group1', '', memberRepos, tree)
    );
    expect(result.current.reposWithMatches).toEqual(new Set());
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('debounces and calls search API, intersects with member repos', async () => {
    (restClient.get as jest.Mock).mockResolvedValue({
      items: [
        { repository: 'my-repo' },
        { repository: 'external-repo' },
        { repository: 'other-repo' },
      ],
    });

    const { result } = renderHook(() =>
      useStructureComponentSearch('group1', 'foo', memberRepos, tree)
    );

    await waitFor(
      () => {
        expect(result.current.loading).toBe(false);
        expect(result.current.reposWithMatches.size).toBeGreaterThan(0);
      },
      { timeout: 600 }
    );

    expect(restClient.get).toHaveBeenCalledWith('/service/rest/v1/search', {
      params: { q: 'foo', repository: 'group1' },
    });
    expect(result.current.reposWithMatches).toEqual(
      new Set(['my-repo', 'other-repo'])
    );
    expect(result.current.reposWithMatches.has('external-repo')).toBe(false);
  });

  it('clears repos when query becomes empty', async () => {
    (restClient.get as jest.Mock).mockResolvedValue({
      items: [{ repository: 'my-repo' }],
    });

    const { result, rerender } = renderHook(
      ({ query }) =>
        useStructureComponentSearch('group1', query, memberRepos, tree),
      { initialProps: { query: 'foo' } }
    );

    await waitFor(
      () => {
        expect(result.current.reposWithMatches.size).toBeGreaterThan(0);
      },
      { timeout: 600 }
    );

    rerender({ query: '' });

    await waitFor(() => {
      expect(result.current.reposWithMatches).toEqual(new Set());
    });
  });

  it('sets error on API failure', async () => {
    (restClient.get as jest.Mock).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() =>
      useStructureComponentSearch('group1', 'foo', memberRepos, tree)
    );

    await waitFor(
      () => {
        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBe('Network error');
        expect(result.current.reposWithMatches).toEqual(new Set());
      },
      { timeout: 600 }
    );
  });
});
