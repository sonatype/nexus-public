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

/**
 * Regression tests for the post-delete reconciliation path.
 *
 * Deleting is the only tree action that has to re-read server state, and it must
 * do so without destroying what auto-expansion and focus handling already
 * established:
 *   - the level to re-read is found *structurally*, not by string-slicing the
 *     node id (child ids are not reliably path-suffixes of their parent);
 *   - merging the re-read level must preserve the expanded state and loaded
 *     children of the surviving siblings, at every depth including the root;
 *   - a *rejected* delete must reconcile the level without removing anything
 *     optimistically, so a partially-succeeded delete still converges on what
 *     the server actually holds;
 *   - the roving tab stop must land on a row that still exists, so the tree stays
 *     reachable by keyboard after a delete.
 */

jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn((response: any) => response.data.result.data),
  },
}));

jest.mock('../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    EXT: { BROWSE: { ACTION: 'coreui_Browse', METHODS: { READ: 'read' } } },
  },
}));

import { renderHook, act, waitFor } from '@testing-library/react';

import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';
import type { BrowseNode } from '../browse-tree.types';
import { useBrowseTree } from '../useBrowseTree';

const mockRequest = ExtAPIUtils.extAPIRequest as jest.Mock;

function folder(id: string, text = id.split('/').pop() as string): BrowseNode {
  return { id, text, type: 'folder', leaf: false, componentId: null, assetId: null, packageUrl: null };
}

function asset(id: string, text = id.split('/').pop() as string): BrowseNode {
  return { id, text, type: 'asset', leaf: true, componentId: null, assetId: `a-${id}`, packageUrl: null };
}

/** Expandable container the browse API types as `asset` (npm scopes/packages/versions). */
function container(id: string, text = id.split('/').pop() as string): BrowseNode {
  return { id, text, type: 'asset', leaf: false, componentId: null, assetId: null, packageUrl: null };
}

/**
 * A repository whose child ids are *not* path-suffixes of their parent's id.
 *
 * npm serves tarballs from a sibling `-` path, so the asset under version
 * `%40scope/pkg/1.0.0` is `%40scope/pkg/-/pkg-1.0.0.tgz`. String-slicing that id
 * at the last `/` yields `%40scope/pkg/-`, which is not a node in the tree.
 * Docker (`sha256:` digests) and Go (`@v` paths) are shaped the same way.
 */
const TARBALL = '%40scope/pkg/-/pkg-1.0.0.tgz';
const VERSION = '%40scope/pkg/1.0.0';

function oddMap(): Record<string, BrowseNode[]> {
  return {
    '/': [container('%40scope'), folder('other')],
    '%40scope': [container('%40scope/pkg')],
    '%40scope/pkg': [container(VERSION)],
    [VERSION]: [asset(TARBALL, 'pkg-1.0.0.tgz')],
    other: [asset('other/x.jar')],
  };
}

/** Root branches, so nothing auto-expands and every level is opened explicitly. */
function branchedMap(): Record<string, BrowseNode[]> {
  return {
    '/': [folder('dirA'), asset('doomed-root.jar')],
    dirA: [folder('dirA/sub'), asset('dirA/doomed.jar')],
    'dirA/sub': [asset('dirA/sub/kept.jar')],
  };
}

let requested: string[] = [];

function useMap(map: Record<string, BrowseNode[]>) {
  mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => {
    const nodeId = data[0].node;
    requested.push(nodeId);
    return { data: { result: { success: true, data: map[nodeId] ?? [] } } };
  });
}

/** Depth-first lookup of a node's state by id. */
function findState(nodes: readonly any[], id: string): any {
  for (const n of nodes) {
    if (n.node.id === id) return n;
    const found = n.children ? findState(n.children, id) : null;
    if (found) return found;
  }
  return null;
}

function ids(nodes: readonly any[]): string[] {
  return nodes.map((n) => n.node.id);
}

beforeEach(() => {
  jest.clearAllMocks();
  requested = [];
});

describe('removeNode re-reads the structurally correct level', () => {
  it('reloads the real parent when child ids are not path-suffixes of it', async () => {
    const map = oddMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('npm-internal'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    for (const level of ['%40scope', '%40scope/pkg', VERSION]) {
      await act(async () => {
        await result.current.actions.expand(level);
      });
    }
    await waitFor(() => expect(findState(result.current.state.nodes, TARBALL)).not.toBeNull());

    // The server accepted the delete.
    map[VERSION] = [];
    requested = [];
    await act(async () => {
      await result.current.actions.removeNode(TARBALL);
    });

    // The level that has to be re-read is the tarball's *parent in the tree*,
    // not `%40scope/pkg/-`, which the id's last path segment suggests.
    expect(requested).toContain(VERSION);
    expect(requested).not.toContain('%40scope/pkg/-');
    expect(findState(result.current.state.nodes, TARBALL)).toBeNull();
    expect(findState(result.current.state.nodes, VERSION).children).toEqual([]);
  });
});

describe('post-delete reload preserves expansion elsewhere in the tree', () => {
  it('keeps a sibling subtree expanded after deleting a non-root node', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    await act(async () => {
      await result.current.actions.expand('dirA/sub');
    });
    await waitFor(() => expect(findState(result.current.state.nodes, 'dirA/sub').expanded).toBe(true));

    map.dirA = [folder('dirA/sub')];
    await act(async () => {
      await result.current.actions.removeNode('dirA/doomed.jar');
    });

    const sub = findState(result.current.state.nodes, 'dirA/sub');
    expect(sub).not.toBeNull();
    // The re-read of `dirA` must refresh the *level*, not reset the sibling that
    // the user (or auto-expansion) had already opened.
    expect(sub.expanded).toBe(true);
    expect(ids(sub.children ?? [])).toEqual(['dirA/sub/kept.jar']);
  });

  it('keeps the tree expanded after deleting a root-level node', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    await act(async () => {
      await result.current.actions.expand('dirA/sub');
    });
    await waitFor(() => expect(findState(result.current.state.nodes, 'dirA/sub').expanded).toBe(true));

    map['/'] = [folder('dirA')];
    await act(async () => {
      await result.current.actions.removeNode('doomed-root.jar');
    });

    expect(findState(result.current.state.nodes, 'doomed-root.jar')).toBeNull();
    const dirA = findState(result.current.state.nodes, 'dirA');
    expect(dirA.expanded).toBe(true);
    expect(findState(result.current.state.nodes, 'dirA/sub').expanded).toBe(true);
    expect(ids(findState(result.current.state.nodes, 'dirA/sub').children ?? [])).toEqual([
      'dirA/sub/kept.jar',
    ]);
  });
});

describe('reconcileNode re-reads a level without removing anything', () => {
  it('leaves the node in place when the server still holds it', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    await act(async () => {
      await result.current.actions.expand('dirA/sub');
    });

    requested = [];
    await act(async () => {
      await result.current.actions.reconcileNode('dirA/doomed.jar');
    });

    expect(requested).toEqual(['dirA']);
    expect(findState(result.current.state.nodes, 'dirA/doomed.jar')).not.toBeNull();
    expect(findState(result.current.state.nodes, 'dirA/sub').expanded).toBe(true);
  });

  it('drops the node when the server reports it gone (partially succeeded delete)', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    await act(async () => {
      await result.current.actions.select('dirA/doomed.jar');
    });

    // Cloud/APT: the asset was removed but a metadata facet failed, so the
    // request is reported as an error while the server no longer lists the node.
    map.dirA = [folder('dirA/sub')];
    await act(async () => {
      await result.current.actions.reconcileNode('dirA/doomed.jar');
    });

    expect(findState(result.current.state.nodes, 'dirA/doomed.jar')).toBeNull();
    expect(result.current.state.selectedNodeId).toBeUndefined();
    expect(findState(result.current.state.nodes, 'dirA/sub')).not.toBeNull();
  });
});

describe('the tab stop survives a delete', () => {
  /**
   * The roving tab stop follows `focusedNodeId`, so an id with no rendered row
   * leaves every treeitem at `tabIndex={-1}` and the tree unreachable by Tab.
   * Focus and selection diverge routinely — clicking a folder selects it, then
   * ArrowDown moves focus to a child — and deleting the folder removes that
   * child too, which is how focus ends up pointing at a node that is gone.
   */
  it('moves focus off a descendant that the delete removed with its parent', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    await act(async () => {
      await result.current.actions.expand('dirA/sub');
    });

    // Selection sits on the folder about to be deleted; focus has moved into it.
    await act(async () => {
      await result.current.actions.select('dirA/sub');
    });
    act(() => {
      result.current.actions.focusNode('dirA/sub/kept.jar');
    });
    expect(result.current.state.focusedNodeId).toBe('dirA/sub/kept.jar');

    map.dirA = [asset('dirA/doomed.jar')];
    await act(async () => {
      await result.current.actions.removeNode('dirA/sub');
    });

    const focused = result.current.state.focusedNodeId;
    expect(focused).not.toBe('dirA/sub/kept.jar');
    // Whatever it is, it has to be a node the tree still renders.
    expect(findState(result.current.state.nodes, focused as string)).not.toBeNull();
    // Deterministically the next surviving visible row.
    expect(focused).toBe('dirA/doomed.jar');
  });

  it('moves focus off the deleted node itself', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    act(() => {
      result.current.actions.focusNode('dirA/doomed.jar');
    });

    map.dirA = [folder('dirA/sub')];
    await act(async () => {
      await result.current.actions.removeNode('dirA/doomed.jar');
    });

    expect(result.current.state.focusedNodeId).not.toBe('dirA/doomed.jar');
    expect(findState(result.current.state.nodes, result.current.state.focusedNodeId as string)).not.toBeNull();
  });

  it('leaves focus alone when the delete happens elsewhere in the tree', async () => {
    const map = branchedMap();
    useMap(map);
    const { result } = renderHook(() => useBrowseTree('maven-releases'));

    await waitFor(() => expect(result.current.state.nodes).toHaveLength(2));
    await act(async () => {
      await result.current.actions.expand('dirA');
    });
    act(() => {
      result.current.actions.focusNode('dirA/sub');
    });

    map['/'] = [folder('dirA')];
    await act(async () => {
      await result.current.actions.removeNode('doomed-root.jar');
    });

    expect(result.current.state.focusedNodeId).toBe('dirA/sub');
  });
});
