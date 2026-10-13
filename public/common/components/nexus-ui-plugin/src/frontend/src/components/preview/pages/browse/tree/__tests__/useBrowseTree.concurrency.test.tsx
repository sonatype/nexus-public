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
 * Regression tests for concurrency in the browse tree:
 *   - a late response for repository A must never touch repository B's state
 *   - auto-expansion must be bounded and cancellable, and must never claim it
 *     reached the branch point when it stopped for safety
 *   - deep-link path expansion must request each level exactly once
 *
 * Every request is deferred here so the *ordering* of responses is under test
 * control; the shared mockData fixtures are untouched.
 */

jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn((response: any) => response.data.result.data),
  },
}));

jest.mock('../../../../shared', () => ({
  EmptyState: ({ title }: { title: string }) => <div>{title}</div>,
}));

jest.mock('../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    EXT: { BROWSE: { ACTION: 'coreui_Browse', METHODS: { READ: 'read' } } },
  },
}));

import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, renderHook, act, screen, waitFor } from '@testing-library/react';

import { BrowseTree } from '../BrowseTree';

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

function ids(nodes: readonly any[]): string[] {
  return nodes.map((n) => n.node.id);
}

interface PendingRequest {
  readonly repositoryName: string;
  readonly node: string;
  settle: (nodes: BrowseNode[]) => void;
  fail: (message: string) => void;
}

/** Every request is captured un-settled so response order is under test control. */
let pending: PendingRequest[] = [];

function useDeferredTransport(): void {
  pending = [];
  mockRequest.mockImplementation(
    (_a: string, _m: string, { data }: any) =>
      new Promise((resolve, reject) => {
        pending.push({
          repositoryName: data[0].repositoryName,
          node: data[0].node,
          settle: (nodes) => resolve({ data: { result: { success: true, data: nodes } } }),
          fail: (message) => reject(new Error(message)),
        });
      }),
  );
}

/** Take the first still-unsettled request matching repo + node. */
function take(repositoryName: string, node: string): PendingRequest {
  const index = pending.findIndex((r) => r.repositoryName === repositoryName && r.node === node);
  if (index === -1) {
    throw new Error(
      `no pending request for ${repositoryName}:${node} (have ${pending
        .map((r) => `${r.repositoryName}:${r.node}`)
        .join(', ')})`,
    );
  }
  return pending.splice(index, 1)[0];
}

async function flush(): Promise<void> {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

/** Auto-resolving transport driven by a child map, recording every request. */
function useMap(map: Record<string, BrowseNode[]>, log: string[]): void {
  mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => {
    const nodeId = data[0].node;
    log.push(nodeId);
    return { data: { result: { success: true, data: map[nodeId] ?? [] } } };
  });
}

beforeEach(() => {
  jest.clearAllMocks();
  // JSDOM implements no layout, so scrollIntoView is absent; BrowseTree calls it
  // after selection. Stubbing it keeps the scroll side effect out of the way of
  // the request-ordering assertions below.
  Element.prototype.scrollIntoView = jest.fn();
});

describe('stale repository responses', () => {
  it('discards a late root response for the previous repository', async () => {
    useDeferredTransport();

    const { result, rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'repo-a' },
    });

    const rootA = take('repo-a', '/');
    rerender({ repo: 'repo-b' });
    const rootB = take('repo-b', '/');

    // repo-b lands first, then repo-a's abandoned request finally answers.
    rootB.settle([folder('b-one'), folder('b-two')]);
    await flush();
    rootA.settle([folder('a-one'), folder('a-two')]);
    await flush();

    expect(result.current.state.repositoryName).toBe('repo-b');
    expect(ids(result.current.state.nodes)).toEqual(['b-one', 'b-two']);
    expect(result.current.state.loading).toBe(false);
  });

  it('does not surface the previous repository failure as the new repository error', async () => {
    useDeferredTransport();

    const { result, rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'repo-a' },
    });

    const rootA = take('repo-a', '/');
    rerender({ repo: 'repo-b' });
    const rootB = take('repo-b', '/');

    rootB.settle([folder('b-one'), folder('b-two')]);
    await flush();
    rootA.fail('repo-a exploded');
    await flush();

    expect(result.current.state.error).toBeUndefined();
    expect(result.current.state.loading).toBe(false);
    expect(ids(result.current.state.nodes)).toEqual(['b-one', 'b-two']);
  });

  it('does not apply the previous repository auto-expansion to the new repository', async () => {
    useDeferredTransport();

    const { result, rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'repo-a' },
    });

    // repo-a has a lone folder, so auto-expansion starts fetching its children.
    take('repo-a', '/').settle([folder('shared')]);
    await flush();
    const chainA = take('repo-a', 'shared');

    rerender({ repo: 'repo-b' });
    // repo-b happens to have a node with the same id but different contents.
    take('repo-b', '/').settle([folder('shared'), folder('b-other')]);
    await flush();

    chainA.settle([asset('shared/from-repo-a.jar')]);
    await flush();

    const shared = result.current.state.nodes.find((n) => n.node.id === 'shared');
    expect(shared?.expanded).toBe(false);
    expect(shared?.children).toBeNull();
  });

  it('does not let a stale deep-link expansion select or focus a node in the new repository', async () => {
    useDeferredTransport();

    const { result, rerender } = renderHook(
      ({ repo, path }) => useBrowseTree(repo, path),
      { initialProps: { repo: 'repo-a', path: 'shared/deep' } },
    );

    take('repo-a', '/').settle([folder('shared'), folder('a-other')]);
    await flush();
    const pathA = take('repo-a', 'shared');

    rerender({ repo: 'repo-b', path: 'shared/deep' });
    take('repo-b', '/').settle([folder('shared'), folder('b-other')]);
    await flush();

    pathA.settle([folder('shared/deep')]);
    await flush();

    expect(result.current.state.selectedNodeId).toBeUndefined();
    expect(result.current.state.focusedNodeId).toBeUndefined();
  });

  it('discards the post-delete root reload of the previous repository', async () => {
    // removeNode reloads from the server after an optimistic removal. For a
    // root-level node that reload replaces `nodes` wholesale, so if the user
    // deletes and immediately switches repository, an unguarded late response
    // repopulates the tree with the *previous* repository's contents.
    useDeferredTransport();

    const { result, rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'repo-a' },
    });

    take('repo-a', '/').settle([folder('a-one'), folder('a-two')]);
    await flush();

    // Delete a root-level node; the confirming reload is left in flight.
    await act(async () => {
      result.current.actions.removeNode('a-one');
    });
    const reloadA = take('repo-a', '/');

    rerender({ repo: 'repo-b' });
    take('repo-b', '/').settle([folder('b-one')]);
    await flush();

    reloadA.settle([folder('a-two')]);
    await flush();

    expect(result.current.state.repositoryName).toBe('repo-b');
    expect(ids(result.current.state.nodes)).toEqual(['b-one']);
  });

  it('discards the post-delete parent reload of the previous repository', async () => {
    useDeferredTransport();

    const { result, rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'repo-a' },
    });

    // repo-a branches at the root so nothing auto-expands, then open `shared`.
    take('repo-a', '/').settle([folder('shared'), folder('a-other')]);
    await flush();
    await act(async () => {
      result.current.actions.expand('shared');
    });
    take('repo-a', 'shared').settle([asset('shared/one.jar'), asset('shared/two.jar')]);
    await flush();

    await act(async () => {
      result.current.actions.removeNode('shared/one.jar');
    });
    const reloadA = take('repo-a', 'shared');

    // repo-b happens to have a node with the same id but different contents.
    rerender({ repo: 'repo-b' });
    take('repo-b', '/').settle([folder('shared'), folder('b-other')]);
    await flush();

    reloadA.settle([asset('shared/two.jar')]);
    await flush();

    const shared = result.current.state.nodes.find((n) => n.node.id === 'shared');
    expect(shared?.children).toBeNull();
  });
});

describe('bounded auto-expansion', () => {
  /** Chain node id for level `i`: `lvl0`, `lvl0/lvl1`, ... */
  function chainId(level: number): string {
    return Array.from({ length: level + 1 }, (_, i) => `lvl${i}`).join('/');
  }

  /** A lone-folder chain of `depth` unique ids that never branches. */
  function endlessChain(depth: number): Record<string, BrowseNode[]> {
    const map: Record<string, BrowseNode[]> = { '/': [folder(chainId(0))] };
    for (let i = 0; i < depth; i++) {
      map[chainId(i)] = [folder(chainId(i + 1))];
    }
    return map;
  }

  it('stops after the documented safety limit instead of walking an unbounded chain', async () => {
    const log: string[] = [];
    useMap(endlessChain(AUTO_EXPAND_MAX_LEVELS * 3), log);

    const { result } = renderHook(() => useBrowseTree('repo'));

    await waitFor(() => expect(result.current.state.autoExpandStopReason).toBeDefined());

    // One root read plus at most one read per auto-expanded level.
    expect(log.length).toBeLessThanOrEqual(AUTO_EXPAND_MAX_LEVELS + 1);
  });

  it('reports a safety stop as such rather than as reaching the branch point', async () => {
    const log: string[] = [];
    useMap(endlessChain(AUTO_EXPAND_MAX_LEVELS * 3), log);

    const { result } = renderHook(() => useBrowseTree('repo'));

    await waitFor(() => expect(result.current.state.autoExpandStopReason).toBe('safety-limit'));
  });

  it('reports reaching the branch point when the chain genuinely branches', async () => {
    const log: string[] = [];
    useMap(
      {
        '/': [folder('v2')],
        v2: [folder('v2/library')],
        'v2/library': [folder('v2/library/alpha'), folder('v2/library/beta')],
      },
      log,
    );

    const { result } = renderHook(() => useBrowseTree('repo'));

    await waitFor(() => expect(result.current.state.autoExpandStopReason).toBe('branch-point'));
  });

  it('abandons an in-progress chain walk when the repository changes', async () => {
    useDeferredTransport();

    const { rerender } = renderHook(({ repo }) => useBrowseTree(repo), {
      initialProps: { repo: 'repo-a' },
    });

    take('repo-a', '/').settle([folder('lvl0')]);
    await flush();
    const level0 = take('repo-a', 'lvl0');

    rerender({ repo: 'repo-b' });
    take('repo-b', '/').settle([folder('b-one'), folder('b-two')]);
    await flush();

    // Answering the abandoned level must not queue the next level for repo-a.
    level0.settle([folder('lvl0/lvl1')]);
    await flush();

    expect(pending.filter((r) => r.repositoryName === 'repo-a')).toEqual([]);
  });
});

describe('deep-link path expansion', () => {
  const DEEP_LINK_TREE: Record<string, BrowseNode[]> = {
    '/': [folder('org'), folder('com')],
    org: [folder('org/acme')],
    'org/acme': [folder('org/acme/1.0')],
    'org/acme/1.0': [asset('org/acme/1.0/acme-1.0.jar')],
  };

  it('requests each path level once even though the hook and the tree both ask', async () => {
    const log: string[] = [];
    useMap(DEEP_LINK_TREE, log);

    // The real component: useBrowseTree's post-root effect and BrowseTree's own
    // initialPath effect both fire expandToPath for the same target.
    render(
      <Theme>
        <BrowseTree repositoryName="repo" initialPath="org/acme/1.0" />
      </Theme>,
    );

    await waitFor(() =>
      expect(screen.getByTestId('tree-node-org/acme/1.0/acme-1.0.jar')).toBeInTheDocument(),
    );

    const duplicates = log.filter((id, i) => log.indexOf(id) !== i);
    expect(duplicates).toEqual([]);
  });

  it('lets the newest path win when an earlier deep walk is still fetching', async () => {
    // Search navigation changes initialPath while a walk is still fetching. Both
    // walks end by selecting their own final node, so if the abandoned one gets
    // the last word it drags the selection, the URL and the detail panel back to
    // the path the user already left.
    useDeferredTransport();

    const { rerender } = render(
      <Theme>
        <BrowseTree repositoryName="repo" initialPath="org/acme/1.0" />
      </Theme>,
    );

    await act(async () => {
      take('repo', '/').settle([folder('org')]);
    });

    // The deep walk is now blocked on its first level.
    const deepWalkLevel = take('repo', 'org');

    rerender(
      <Theme>
        <BrowseTree repositoryName="repo" initialPath="org" />
      </Theme>,
    );
    await flush();

    await act(async () => {
      take('repo', 'org').settle([folder('org/acme')]);
    });
    await waitFor(() =>
      expect(screen.getByTestId('tree-node-org')).toHaveAttribute('aria-selected', 'true'),
    );

    // The superseded walk finally answers, one level at a time.
    await act(async () => {
      deepWalkLevel.settle([folder('org/acme')]);
    });
    await act(async () => {
      take('repo', 'org/acme').settle([folder('org/acme/1.0')]);
    });
    await act(async () => {
      take('repo', 'org/acme/1.0').settle([asset('org/acme/1.0/acme-1.0.jar')]);
    });
    await flush();

    expect(screen.getByTestId('tree-node-org')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByTestId('tree-node-org/acme/1.0')).toHaveAttribute(
      'aria-selected',
      'false',
    );
  });

  it('expands a genuinely new path after an earlier walk settled', async () => {
    const log: string[] = [];
    useMap(DEEP_LINK_TREE, log);

    const { rerender } = render(
      <Theme>
        <BrowseTree repositoryName="repo" initialPath="org" />
      </Theme>,
    );

    await waitFor(() => expect(screen.getByTestId('tree-node-org/acme')).toBeInTheDocument());

    rerender(
      <Theme>
        <BrowseTree repositoryName="repo" initialPath="org/acme/1.0" />
      </Theme>,
    );

    await waitFor(() =>
      expect(screen.getByTestId('tree-node-org/acme/1.0/acme-1.0.jar')).toBeInTheDocument(),
    );
  });
});
