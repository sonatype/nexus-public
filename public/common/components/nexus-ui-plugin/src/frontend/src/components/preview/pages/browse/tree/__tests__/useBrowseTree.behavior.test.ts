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
 * Regression tests for the browse tree state model:
 *   - auto-expansion of single-child folder chains
 *   - preservation of an explicit user collapse
 *   - expand/collapse as distinct actions (keyboard Left/Right must not toggle)
 *   - structural (not string-derived) parent focus
 *
 * These use a purpose-built child map so the shared mockData fixtures — and the
 * suites that depend on them — are left untouched.
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
import { AUTO_EXPAND_MAX_LEVELS, useBrowseTree } from '../useBrowseTree';

const mockRequest = ExtAPIUtils.extAPIRequest as jest.Mock;

function folder(id: string, text = id.split('/').pop() as string): BrowseNode {
  return { id, text, type: 'folder', leaf: false, componentId: null, assetId: null, packageUrl: null };
}

function asset(id: string, text = id.split('/').pop() as string): BrowseNode {
  return { id, text, type: 'asset', leaf: true, componentId: null, assetId: `a-${id}`, packageUrl: null };
}

/**
 * An expandable container the browse API types as `asset` rather than `folder`.
 *
 * npm (and other formats) model package and version containers this way: the
 * node is `leaf: false` and renders in the tree with a disclosure control, but
 * its `type` is `'asset'`. Real fixture: `npm-internal` returns
 * `%40sonatype/publishtohosted` as `{type: 'asset', leaf: false}`.
 */
function container(id: string, text = id.split('/').pop() as string): BrowseNode {
  return { id, text, type: 'asset', leaf: false, componentId: null, assetId: null, packageUrl: null };
}

/** Chain: root -> v2 -> library -> {alpha, beta}. Branch point is `library`. */
const CHAIN_MAP: Record<string, BrowseNode[]> = {
  '/': [folder('v2')],
  v2: [folder('v2/library')],
  'v2/library': [folder('v2/library/alpha'), folder('v2/library/beta')],
  'v2/library/alpha': [asset('v2/library/alpha/a.jar')],
  'v2/library/beta': [asset('v2/library/beta/b.jar')],
};

/**
 * Chain whose middle levels are non-leaf `asset` containers rather than
 * `folder`s: scope -> package -> {1.0.0, 2.0.0}. The branch point is `package`;
 * nothing before it branches, so every level above it must open.
 */
const MIXED_CHAIN_MAP: Record<string, BrowseNode[]> = {
  '/': [folder('%40scope')],
  '%40scope': [container('%40scope/package')],
  '%40scope/package': [container('%40scope/package/1.0.0'), container('%40scope/package/2.0.0')],
};

/** Root branches immediately — nothing may auto-expand. */
const BRANCHED_MAP: Record<string, BrowseNode[]> = {
  '/': [folder('one'), folder('two')],
  one: [folder('one/only')],
  'one/only': [asset('one/only/x.jar')],
  two: [asset('two/y.jar')],
};

function useMap(map: Record<string, BrowseNode[]>, failFor?: string) {
  mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => {
    const nodeId = data[0].node;
    if (failFor && nodeId === failFor) {
      throw new Error('Boom');
    }
    return { data: { result: { success: true, data: map[nodeId] ?? [] } } };
  });
}

function ids(nodes: readonly any[]): string[] {
  return nodes.map((n) => n.node.id);
}

/** Node id for level `i` of a synthetic lone-folder chain: `lvl0/lvl1/...`. */
function chainId(level: number): string {
  return Array.from({ length: level + 1 }, (_, i) => `lvl${i}`).join('/');
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

beforeEach(() => {
  jest.clearAllMocks();
});

describe('auto-expansion of single-child folder chains', () => {
  it('expands from the root down to the first branch point on initial browse', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(1);
    });

    await waitFor(() => {
      expect(result.current.state.nodes[0].expanded).toBe(true);
    });

    const v2 = result.current.state.nodes[0];
    expect(ids(v2.children!)).toEqual(['v2/library']);

    const library = v2.children![0];
    expect(library.expanded).toBe(true);
    expect(ids(library.children!)).toEqual(['v2/library/alpha', 'v2/library/beta']);

    // Stops AT the branch point: neither branch child is expanded.
    expect(library.children![0].expanded).toBe(false);
    expect(library.children![1].expanded).toBe(false);
  });

  it('keeps descending through single non-leaf children the API types as assets, not folders', async () => {
    // Regression: `npm-internal` in a live instance returns
    // root -> %40sonatype (folder) -> %40sonatype/publishtohosted
    // ({type: 'asset', leaf: false}) -> one .tgz. Expansion stopped at the
    // package container even though a single expandable child is not a branch
    // point, so the tree opened one level and halted.
    useMap(MIXED_CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('npm-internal'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(1);
    });
    await waitFor(() => {
      expect(result.current.state.nodes[0].expanded).toBe(true);
    });

    // Both levels above the branch point open...
    await waitFor(() => {
      expect(findState(result.current.state.nodes, '%40scope/package')?.expanded).toBe(true);
    });

    const pkg = findState(result.current.state.nodes, '%40scope/package');
    expect(ids(pkg.children!)).toEqual(['%40scope/package/1.0.0', '%40scope/package/2.0.0']);
    // ...and expansion stops at the branch point itself.
    expect(pkg.children![0].expanded).toBe(false);
    expect(pkg.children![1].expanded).toBe(false);
    expect(result.current.state.autoExpandStopReason).toBe('branch-point');
  });

  it('follows a lone-folder chain to the branch point at the deepest supported depth', async () => {
    // Auto-expansion must reach "the first branch point", and it does
    // so for any chain within AUTO_EXPAND_MAX_LEVELS. A bound is still required,
    // because each level is a sequential round trip and a chain of unique ids is
    // not a cycle: without one, a single repository selection can issue
    // unboundedly many requests. DEEP sits exactly on the bound so the deepest
    // supported chain is proven to open completely, and a walk that stops *at*
    // the bound reports 'safety-limit' rather than claiming the branch point
    // (covered in useBrowseTree.concurrency.test.tsx).
    const DEEP = AUTO_EXPAND_MAX_LEVELS;
    const deepMap: Record<string, BrowseNode[]> = { '/': [folder('lvl0')] };
    for (let i = 0; i < DEEP - 1; i++) {
      deepMap[chainId(i)] = [folder(chainId(i + 1))];
    }
    // The final level branches, which is the only thing that may stop the walk.
    deepMap[chainId(DEEP - 1)] = [folder(`${chainId(DEEP - 1)}/alpha`), folder(`${chainId(DEEP - 1)}/beta`)];
    useMap(deepMap);

    const { result } = renderHook(() => useBrowseTree('deep-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    // Walk the rendered tree and prove every one of the DEEP levels is open.
    await waitFor(() => {
      let cursor = result.current.state.nodes[0];
      let depth = 0;
      while (cursor?.expanded && cursor.children?.length === 1) {
        cursor = cursor.children[0];
        depth++;
      }
      expect(cursor?.node.id).toBe(chainId(DEEP - 1));
      expect(depth).toBe(DEEP - 1);
    });

    const deepest = findState(result.current.state.nodes, chainId(DEEP - 1));
    expect(deepest?.expanded).toBe(true);
    expect(ids(deepest!.children!)).toEqual([
      `${chainId(DEEP - 1)}/alpha`,
      `${chainId(DEEP - 1)}/beta`,
    ]);
    // The branch children themselves stay closed.
    expect(deepest!.children!.every((c) => !c.expanded)).toBe(true);
    // The walk really reached the branch point; it did not stop for safety.
    expect(result.current.state.autoExpandStopReason).toBe('branch-point');
  });

  it('terminates instead of looping when the server reports a cyclic chain', async () => {
    // Cycle protection is independent of the depth bound and fires first:
    // `loop` claims itself as its own only child, which would recurse forever
    // without a visited-id guard.
    useMap({
      '/': [folder('loop')],
      loop: [folder('loop')],
    });

    const { result } = renderHook(() => useBrowseTree('cyclic-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(1);
    });
    await act(async () => {
      await Promise.resolve();
    });

    // Root listing + exactly one expansion of `loop`; the repeat is refused.
    const requested = mockRequest.mock.calls.map((c) => c[2].data[0].node);
    expect(requested.filter((id: string) => id === 'loop')).toHaveLength(1);
    expect(requested).toEqual([...new Set(requested)]);
  });

  it('does not auto-expand when the root already branches', async () => {
    useMap(BRANCHED_MAP);
    const { result } = renderHook(() => useBrowseTree('maven-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });

    // Give any stray async expansion a chance to land before asserting.
    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.state.nodes.every((n) => !n.expanded)).toBe(true);
    expect(mockRequest).toHaveBeenCalledTimes(1);
  });

  it('does not request a child more than once while walking the chain', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    const requested = mockRequest.mock.calls.map((c) => c[2].data[0].node);
    expect(requested).toEqual([...new Set(requested)]);
  });

  it('stops cleanly when a child request fails mid-chain', async () => {
    useMap(CHAIN_MAP, 'v2/library');
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    const library = result.current.state.nodes[0].children![0];
    expect(library.expanded).toBe(false);
    expect(library.children).toBeNull();
  });

  it('does not auto-select or auto-focus a node while expanding', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    expect(result.current.state.selectedNodeId).toBeUndefined();
    expect(result.current.state.focusedNodeId).toBeUndefined();
  });

  it('re-runs auto-expansion against the new repository, not stale nodes', async () => {
    useMap(BRANCHED_MAP);
    const { result, rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'maven-hosted' },
    });

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });

    useMap(CHAIN_MAP);
    rerender({ repo: 'docker-hosted' });

    await waitFor(() => {
      expect(ids(result.current.state.nodes)).toEqual(['v2']);
    });
    await waitFor(() => {
      expect(result.current.state.nodes[0].expanded).toBe(true);
    });
  });
});

describe('explicit user collapse is preserved', () => {
  it('records the collapse and does not let auto-expansion re-open it', async () => {
    useMap(CHAIN_MAP);
    const { result, rerender } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    await act(async () => {
      result.current.actions.collapse('v2');
    });
    expect(result.current.state.nodes[0].expanded).toBe(false);
    expect(result.current.state.userCollapsedNodeIds).toContain('v2');

    rerender();
    await act(async () => {
      await Promise.resolve();
    });
    expect(result.current.state.nodes[0].expanded).toBe(false);
  });

  it('does not cascade a collapsed descendant back open when the parent is re-expanded', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    await act(async () => {
      result.current.actions.collapse('v2/library');
    });
    await act(async () => {
      result.current.actions.collapse('v2');
    });
    await act(async () => {
      await result.current.actions.expand('v2');
    });

    expect(result.current.state.nodes[0].expanded).toBe(true);
    expect(result.current.state.nodes[0].children![0].expanded).toBe(false);
  });

  it('an explicit expand clears the collapse record for that node', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });
    await act(async () => {
      result.current.actions.collapse('v2');
    });
    await act(async () => {
      await result.current.actions.expand('v2');
    });
    expect(result.current.state.userCollapsedNodeIds).not.toContain('v2');
  });

  it('clears user-collapse tracking on refresh', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });
    await act(async () => {
      result.current.actions.collapse('v2');
    });
    expect(result.current.state.userCollapsedNodeIds).toContain('v2');

    await act(async () => {
      await result.current.actions.refresh();
    });
    expect(result.current.state.userCollapsedNodeIds).toEqual([]);
  });
});

describe('expand and collapse are distinct actions', () => {
  it('expand on an already-expanded node leaves it expanded', async () => {
    useMap(CHAIN_MAP);
    const { result } = renderHook(() => useBrowseTree('docker-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes[0]?.expanded).toBe(true);
    });

    await act(async () => {
      await result.current.actions.expand('v2');
    });
    expect(result.current.state.nodes[0].expanded).toBe(true);
  });

  it('collapse on an already-collapsed node is a no-op', async () => {
    useMap(BRANCHED_MAP);
    const { result } = renderHook(() => useBrowseTree('maven-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });
    await act(async () => {
      result.current.actions.collapse('one');
    });
    expect(result.current.state.nodes[0].expanded).toBe(false);
  });
});

describe('focus model', () => {
  it('focusNode moves focus without changing the selection', async () => {
    useMap(BRANCHED_MAP);
    const { result } = renderHook(() => useBrowseTree('maven-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });

    act(() => {
      result.current.actions.select('one');
    });
    act(() => {
      result.current.actions.focusNode('two');
    });

    expect(result.current.state.focusedNodeId).toBe('two');
    expect(result.current.state.selectedNodeId).toBe('one');
  });

  it('focusParent uses tree structure, not string path math', async () => {
    // `alpha` sits under `v2/library`; naive lastIndexOf('/') math also yields
    // that, so assert the harder case: a child whose id is NOT a path suffix
    // of its parent still resolves to the structural parent.
    const ODD_MAP: Record<string, BrowseNode[]> = {
      '/': [folder('parent-a'), folder('parent-b')],
      'parent-a': [
        { ...folder('unrelated-child'), text: 'child' },
        { ...folder('unrelated-sibling'), text: 'sibling' },
      ],
    };
    useMap(ODD_MAP);
    const { result } = renderHook(() => useBrowseTree('maven-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });
    await act(async () => {
      await result.current.actions.expand('parent-a');
    });
    act(() => {
      result.current.actions.focusNode('unrelated-child');
    });
    act(() => {
      result.current.actions.focusParent();
    });

    expect(result.current.state.focusedNodeId).toBe('parent-a');
  });

  it('does not move focus when the traversal origin is no longer visible', async () => {
    // A stale origin (node removed, or hidden by a collapse) has no neighbour to
    // move to. Falling through to index 0 / the last row would teleport focus to
    // the opposite end of the tree instead of leaving it alone.
    useMap(BRANCHED_MAP);
    const { result } = renderHook(() => useBrowseTree('maven-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });
    act(() => {
      result.current.actions.focusNode('two');
    });

    act(() => {
      result.current.actions.focusNext('gone');
    });
    expect(result.current.state.focusedNodeId).toBe('two');

    act(() => {
      result.current.actions.focusPrevious('gone');
    });
    expect(result.current.state.focusedNodeId).toBe('two');
  });

  it('collapsing an ancestor moves focus out of the subtree it hides', async () => {
    useMap(BRANCHED_MAP);
    const { result } = renderHook(() => useBrowseTree('maven-hosted'));

    await waitFor(() => {
      expect(result.current.state.nodes).toHaveLength(2);
    });
    await act(async () => {
      await result.current.actions.expand('one');
    });
    const child = result.current.state.nodes[0].children?.[0]?.node.id as string;
    expect(child).toBeDefined();

    act(() => {
      result.current.actions.focusNode(child);
    });
    act(() => {
      result.current.actions.collapse('one');
    });

    expect(result.current.state.focusedNodeId).toBe('one');
  });
});
