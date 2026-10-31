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

import { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { ExtAPIUtils } from '../../../../../interface/ExtAPIUtils';
import { APIConstants } from '../../../../../constants/APIConstants';
import type {
  AutoExpandStopReason,
  BrowseNode,
  BrowseTreeState,
  TreeNodeState,
  UseBrowseTreeResult,
  BrowseTreeActions,
} from './browse-tree.types';
import { isMockMode } from '../../../config/featureFlags';
import { getMockBrowseNodes } from '../mockData';

const { EXT: { BROWSE: { ACTION, METHODS: { READ } } } } = APIConstants;

/**
 * Maximum number of levels a single auto-expansion walk will open.
 *
 * Auto-expansion of a lone-folder chain is inherently sequential — each level's
 * children have to arrive before we know whether to descend — so an unbounded
 * walk turns one repository selection into an unbounded series of round trips.
 * A visited-id set stops *cycles*, but a chain of unique ids (deeply nested
 * group folders, or generated content) is not a cycle and would keep going.
 *
 * 50 is far beyond any real layout (Maven group paths bottom out around ten
 * segments) while keeping the worst case bounded. Reaching it is reported as
 * {@link AutoExpandStopReason} `'safety-limit'`, never as `'branch-point'`, so
 * a bounded stop is never presented as a completed walk.
 */
export const AUTO_EXPAND_MAX_LEVELS = 50;

/** Result of one {@link collectSingleChildChain} walk. */
interface ChainWalk {
  readonly expansions: Expansion[];
  readonly stopReason: AutoExpandStopReason;
}

/**
 * Fetch children for a node from the API.
 * When ?mock is in URL (dev only), returns mock browse tree data.
 */
async function fetchChildren(repositoryName: string, nodeId: string): Promise<BrowseNode[]> {
  if (isMockMode()) {
    return Promise.resolve(getMockBrowseNodes(repositoryName, nodeId));
  }
  const response = await ExtAPIUtils.extAPIRequest(ACTION, READ, {
    data: [{ repositoryName, node: nodeId }],
  });
  return ExtAPIUtils.checkForErrorAndExtract(response);
}

/**
 * Detect backend error nodes: the browse API sometimes returns
 * nodes whose text is a raw Java exception instead of a real name
 * (e.g. "Cannot invoke …getName() because repository is null").
 */
const ERROR_NODE_PATTERNS = [
  'Cannot invoke',
  'NullPointerException',
  'IllegalStateException',
  'because repository is null',
  'Exception:',
];

function isErrorNode(node: BrowseNode): boolean {
  if (!node || !node.id) return true;
  if (!node.text) return true;
  const text = node.text;
  return ERROR_NODE_PATTERNS.some((p) => text.includes(p));
}

/**
 * Convert BrowseNode array to TreeNodeState array,
 * silently dropping error/placeholder nodes from the backend (bug s0h6).
 */
function nodesToState(nodes: BrowseNode[]): TreeNodeState[] {
  return nodes
    .filter((node) => !isErrorNode(node))
    .map((node) => ({
      node,
      expanded: false,
      children: null,
      loading: false,
    }));
}

/**
 * Fold a freshly fetched level into the level already held in state.
 *
 * A re-read after a delete must reflect what the server now holds *without*
 * discarding what the user already opened: replacing the level with
 * {@link nodesToState} would reset every surviving sibling to
 * `expanded: false, children: null`, collapsing subtrees that auto-expansion or
 * the user had opened — a delete undoing auto-expansion.
 *
 * Surviving nodes therefore keep their `expanded` flag and loaded `children`
 * while taking the server's fresh payload; nodes the server no longer lists are
 * dropped, and nodes it has added arrive collapsed.
 */
function mergeFetchedLevel(
  previous: readonly TreeNodeState[],
  fetched: BrowseNode[]
): TreeNodeState[] {
  const existing = new Map(previous.map((state) => [state.node.id, state]));
  return nodesToState(fetched).map((incoming) => {
    const prior = existing.get(incoming.node.id);
    return prior ? { ...prior, node: incoming.node, loading: false } : incoming;
  });
}


/** One fetched level of a chain expansion, applied as a single state update. */
type Expansion = { expandId: string; expandChildren: TreeNodeState[] };

/**
 * Whether a node may participate in single-child auto-expansion.
 *
 * `!leaf` is the same predicate the tree itself uses to decide whether a node
 * is expandable (see `BrowseTreeNode`'s `aria-expanded` and toggle handling),
 * so auto-expansion covers exactly the nodes the user could have opened by
 * hand. `type` is deliberately *not* consulted: formats model intermediate
 * containers with whatever type suits them -- npm returns package and version
 * containers as `{type: 'asset', leaf: false}` -- and a container holding a
 * single child is not a branch point regardless of its type.
 */
function isAutoExpandable(node: BrowseNode): boolean {
  return !node.leaf;
}

/**
 * Recursively find a node in the tree by ID.
 */
function findNodeById(nodes: readonly TreeNodeState[], nodeId: string): TreeNodeState | null {
  for (const node of nodes) {
    if (node.node.id === nodeId) {
      return node;
    }
    if (node.children) {
      const found = findNodeById(node.children, nodeId);
      if (found) return found;
    }
  }
  return null;
}

/**
 * Find the structural parent of a node by walking the loaded tree.
 *
 * Preferred over deriving the parent from the node id: ids are opaque server
 * strings and `lastIndexOf('/')` breaks for formats whose child ids are not
 * path-suffixes of their parent (Docker tags, npm tarballs, Go `@v` paths).
 */
function findParentNodeId(
  nodes: readonly TreeNodeState[],
  nodeId: string,
  parentId: string | null = null,
): string | null {
  for (const node of nodes) {
    if (node.node.id === nodeId) {
      return parentId;
    }
    if (node.children) {
      const found = findParentNodeId(node.children, nodeId, node.node.id);
      if (found !== null) return found;
    }
  }
  return null;
}

/**
 * Safely decode a URI component, returning the original string on failure.
 */
function safeDecodeURIComponent(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

/**
 * Find a node by ID, tolerating URL-encoding mismatches.
 * Tries the original id, then encoded, then decoded variants so that
 * deep-link paths with raw `@` match server-encoded `%40` (and vice-versa).
 *
 * The 4th fallback (decoded comparison) handles characters that Java's
 * URLEncoder.encode() encodes but JavaScript's encodeURIComponent() does NOT:
 *   `!` → `%21`, `'` → `%27`, `(` → `%28`, `)` → `%29`, `*` → `%2A`, `+` → `%2B`
 * For example, Go module paths like `github.com/!data!dog/...` are stored in
 * the tree as `github.com/%21data%21dog/...`. Decoding both sides normalizes
 * the comparison.
 */
function findNodeByIdFlexible(
  nodes: readonly TreeNodeState[],
  nodeId: string,
): TreeNodeState | null {
  let found = findNodeById(nodes, nodeId);
  if (found) return found;

  try {
    found = findNodeById(nodes, encodeURIComponent(nodeId));
    if (found) return found;
  } catch { /* ignore */ }

  try {
    found = findNodeById(nodes, decodeURIComponent(nodeId));
    if (found) return found;
  } catch { /* ignore */ }

  // Decoded comparison: decode BOTH the search ID and each tree node ID,
  // then compare.  This handles Java/JS encoding asymmetry for `!`, `+`, etc.
  found = findNodeByDecodedComparison(nodes, nodeId);
  if (found) return found;

  return null;
}

/**
 * Recursively search nodes comparing decoded IDs.
 * Both the search ID and each node's ID are decoded before comparison,
 * normalizing encoding differences between Java URLEncoder and JS encodeURIComponent.
 */
function findNodeByDecodedComparison(
  nodes: readonly TreeNodeState[],
  nodeId: string,
): TreeNodeState | null {
  const decodedSearch = safeDecodeURIComponent(nodeId);
  for (const node of nodes) {
    if (safeDecodeURIComponent(node.node.id) === decodedSearch) {
      return node;
    }
    if (node.children) {
      const found = findNodeByDecodedComparison(node.children, nodeId);
      if (found) return found;
    }
  }
  return null;
}

/**
 * Fuzzy-match a node among direct children when the exact ID doesn't match.
 *
 * Covers cross-format mismatches between search-constructed paths and browse
 * tree node IDs generated by format-specific BrowseNodeGenerators:
 *
 *   NPM:   `name/version` → `name/-/name-version.tgz`  (NpmBrowseNodeGenerator
 *          filters the `-` directory but the tgz ID retains it)
 *   NuGet: `name/version` → `name/version/id-version.nupkg`
 *   Docker:`manifests` renamed to `tags` in display tree
 *
 * Only searches the provided nodes (not recursive) to avoid false positives.
 */
function findNodeByVersionFuzzy(
  nodes: readonly TreeNodeState[],
  parentPath: string,
  segment: string,
): TreeNodeState | null {
  // 1. NPM / tarball archives  (name-version.tgz / .tar.gz)
  const tgzSuffix = `-${segment}.tgz`;
  const tarSuffix = `-${segment}.tar.gz`;
  for (const node of nodes) {
    const id = node.node.id;
    if (id.endsWith(tgzSuffix) || id.endsWith(tarSuffix)) {
      return node;
    }
  }

  // 2. NuGet packages  (id-version.nupkg)
  const nupkgSuffix = `-${segment}.nupkg`;
  for (const node of nodes) {
    if (node.node.id.endsWith(nupkgSuffix)) {
      return node;
    }
  }

  // 3. Display-text match (e.g. version folders, Docker tags renamed from manifests)
  for (const node of nodes) {
    if (node.node.text === segment) {
      return node;
    }
  }

  // 4. Docker: search path may use `manifests` but tree displays `tags`
  if (segment === 'manifests') {
    for (const node of nodes) {
      if (node.node.text === 'tags') {
        return node;
      }
    }
  }

  return null;
}

/**
 * Get all visible node IDs in order (for keyboard navigation).
 */
function getVisibleNodeIds(nodes: readonly TreeNodeState[], result: string[] = []): string[] {
  for (const node of nodes) {
    result.push(node.node.id);
    if (node.expanded && node.children) {
      getVisibleNodeIds(node.children, result);
    }
  }
  return result;
}

/**
 * Pick the node that should hold focus after `lostId` stops being a visible row.
 *
 * Deleting the focused row must not drop DOM focus to `<body>`: that loses the
 * user's place and forces them to Tab back in and re-descend the whole branch,
 * which on a deep tree is the same usability failure keyboard navigation exists
 * to prevent. The fallback is deterministic — the next surviving visible row,
 * else the nearest preceding one (which is the parent when the
 * deleted node was an only child) — and returns undefined only when nothing is
 * left to focus.
 *
 * `lostId` is the node that *held focus*, which is not necessarily the node that
 * was deleted: deleting a folder also removes its descendants, and one of those
 * may have owned the roving tab stop.
 *
 * `before` and `after` are the visible orders either side of the removal, so the
 * returned id is guaranteed to still be present and visible.
 */
function focusFallbackAfterRemoval(
  before: readonly TreeNodeState[],
  after: readonly TreeNodeState[],
  lostId: string
): string | undefined {
  const previousOrder = getVisibleNodeIds(before);
  const index = previousOrder.indexOf(lostId);
  if (index === -1) return undefined;

  const survivors = new Set(getVisibleNodeIds(after));
  for (let i = index + 1; i < previousOrder.length; i++) {
    if (survivors.has(previousOrder[i])) return previousOrder[i];
  }
  for (let i = index - 1; i >= 0; i--) {
    if (survivors.has(previousOrder[i])) return previousOrder[i];
  }
  return undefined;
}

/**
 * Recursively update a node in the tree.
 */
function updateNodeInTree(
  nodes: readonly TreeNodeState[],
  nodeId: string,
  updater: (node: TreeNodeState) => TreeNodeState
): TreeNodeState[] {
  return nodes.map((node) => {
    if (node.node.id === nodeId) {
      return updater(node);
    }
    if (node.children) {
      return {
        ...node,
        children: updateNodeInTree(node.children, nodeId, updater),
      };
    }
    return node;
  });
}

/**
 * Hook for managing browse tree state with lazy loading.
 *
 * Features:
 * - Lazy loading of children on first expand
 * - Auto-expand single child folders
 * - Initial depth expansion (2 levels) to match Heritage UI
 * - Deep linking support (expandToPath)
 * - Keyboard navigation
 */
export function useBrowseTree(
  repositoryName: string,
  initialPath?: string
): UseBrowseTreeResult {
  const [state, setState] = useState<BrowseTreeState>({
    repositoryName,
    nodes: [],
    loading: true,
    focusedNodeId: undefined,
    selectedNodeId: undefined,
    userCollapsedNodeIds: [],
  });

  const isInitialPathExpandedRef = useRef(false);
  const prevRepoRef = useRef(repositoryName);

  /**
   * Generation of the currently-relevant set of requests.
   *
   * `ExtAPIUtils.extAPIRequest` takes no axios config, so there is no
   * `AbortSignal` to hand the transport: an in-flight HTTP request cannot be
   * cancelled. Cancellation is therefore implemented one level up, at the
   * *sequence* level — a multi-step walk checks the generation between steps and
   * stops issuing further requests — and combined with this latest-request
   * generation guard, which discards the *result* of any request belonging to a
   * superseded generation. Together they guarantee that a late response for
   * repository A can neither continue fetching nor write to state after the tree
   * has moved to repository B.
   */
  const generationRef = useRef(0);

  // Reset auto-expand flag and supersede every in-flight request when the
  // repository changes. Done during render, before any effect for the new
  // repository runs, so nothing from the previous one can slip through.
  if (prevRepoRef.current !== repositoryName) {
    prevRepoRef.current = repositoryName;
    isInitialPathExpandedRef.current = false;
    generationRef.current += 1;
  }

  /** Whether `gen` is still the latest generation (i.e. results may be used). */
  const isCurrent = useCallback((gen: number): boolean => generationRef.current === gen, []);

  /**
   * Generation of the most recently requested path walk.
   *
   * {@link generationRef} above is bumped only when the *repository* changes, so
   * it cannot separate two walks within one repository — and those race, because
   * a walk fetches one level at a time and a later, shallower path can finish
   * first. Both walks end by selecting their final node, so without this the
   * slower older walk would have the last word and pull the selection, the URL
   * and the detail panel back to the path the user already left.
   */
  const pathGenerationRef = useRef(0);

  // Ref to always have the latest nodes (avoids stale closures in async operations)
  const nodesRef = useRef(state.nodes);
  nodesRef.current = state.nodes;

  // Ref mirror of the explicit-collapse set, so the async chain walk can honour
  // a collapse the user performed while children were still in flight.
  const userCollapsedRef = useRef(state.userCollapsedNodeIds);
  userCollapsedRef.current = state.userCollapsedNodeIds;

  /**
   * In-flight {@link expandToPath} walks, keyed `${repositoryName}::${path}`, so
   * concurrent callers for the same target join one walk instead of duplicating
   * every request along the path.
   */
  const inFlightPathsRef = useRef<Map<string, Promise<void>>>(new Map());

  /**
   * Load root level nodes.
   */
  const loadRoot = useCallback(async () => {
    // Any request still in flight belongs to a previous root load; supersede it
    // so its response cannot land on top of this one.
    generationRef.current += 1;
    const gen = generationRef.current;

    // Switching repository must drop the previous repository's nodes, selection
    // and collapse history immediately, so nothing downstream (auto-expansion,
    // detail panel, delete gating) can act on stale data.
    setState((prev) =>
      prev.repositoryName === repositoryName
        ? { ...prev, loading: true, error: undefined }
        : {
            ...prev,
            nodes: [],
            loading: true,
            error: undefined,
            focusedNodeId: undefined,
            selectedNodeId: undefined,
            userCollapsedNodeIds: [],
            autoExpandStopReason: undefined,
          }
    );

    try {
      const children = await fetchChildren(repositoryName, '/');
      // A superseded load must not publish nodes, clear `loading`, or reset the
      // collapse history for whatever repository is current now.
      if (!isCurrent(gen)) return [];
      setState((prev) => ({
        ...prev,
        repositoryName,
        nodes: nodesToState(children),
        loading: false,
        error: undefined,
        userCollapsedNodeIds: [],
      }));
      return children;
    } catch (err) {
      // Likewise a superseded failure is not the current repository's error.
      if (!isCurrent(gen)) return [];
      const errorMessage = err instanceof Error ? err.message : 'Unknown error loading browse tree';
      setState((prev) => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      return [];
    }
  }, [repositoryName, isCurrent]);

  /**
   * Walk a chain of single-child containers, fetching each level, and return the
   * expansions to apply.
   *
   * Shared by the mount-time auto-expansion (open collapsed
   * single-child folder chains up to the first branch point) and by `expand`,
   * so both agree on the stop condition. The chain stops at the first node that
   * is not "exactly one expandable child":
   *
   * - zero children (empty directory)      -> stop
   * - one leaf child                       -> stop (nothing to open)
   * - one folder + one file                -> stop (2 children = branch point)
   * - more than one child                  -> stop (branch point)
   * - the user explicitly collapsed it     -> stop (never re-open)
   *
   * The first fetch is allowed to throw so the caller can surface a node-level
   * error; a failure deeper in the chain just ends the walk, leaving the levels
   * already collected intact.
   *
   * Safety and cancellation model — the walk is bounded three ways, and reports
   * which one stopped it so a bounded stop is never mistaken for a completed one:
   *
   * - a visited-id set ends cyclic server data          -> `'cycle'`
   * - {@link AUTO_EXPAND_MAX_LEVELS} bounds a chain of
   *   unique ids, which is not a cycle and would
   *   otherwise issue unboundedly many sequential
   *   requests                                          -> `'safety-limit'`
   * - the generation guard ends the walk as soon as the
   *   repository changes or the tree reloads, so no
   *   further requests are issued for a tree nobody is
   *   looking at any more                               -> `'aborted'`
   *
   * Only `'branch-point'` means auto-expansion actually reached the first
   * branching folder.
   */
  const collectSingleChildChain = useCallback(
    async (startNodeId: string, gen: number): Promise<ChainWalk> => {
      const expansions: Expansion[] = [];
      const visited = new Set<string>();
      let nodeId: string | null = startNodeId;
      let first = true;

      while (nodeId) {
        // Cycle guard: a node that has already been expanded in this walk means
        // the server data loops back on itself. Stop rather than fetch forever.
        if (visited.has(nodeId)) return { expansions, stopReason: 'cycle' };
        // Depth guard: bound the number of sequential round trips one selection
        // can trigger. The levels already collected are still applied.
        if (expansions.length >= AUTO_EXPAND_MAX_LEVELS) {
          return { expansions, stopReason: 'safety-limit' };
        }
        // Sequence-level cancellation: stop before spending another request on a
        // tree that has been superseded.
        if (!isCurrent(gen)) return { expansions, stopReason: 'aborted' };
        visited.add(nodeId);

        let childStates: TreeNodeState[];
        try {
          childStates = nodesToState(await fetchChildren(repositoryName, nodeId));
        } catch (err) {
          if (first) throw err;
          return { expansions, stopReason: 'error' };
        }
        // The response arrived after the tree moved on — discard it rather than
        // descending further or applying it.
        if (!isCurrent(gen)) return { expansions, stopReason: 'aborted' };
        first = false;

        expansions.push({ expandId: nodeId, expandChildren: childStates });

        const onlyChild = childStates.length === 1 ? childStates[0] : undefined;
        nodeId =
          onlyChild &&
          isAutoExpandable(onlyChild.node) &&
          !userCollapsedRef.current.includes(onlyChild.node.id)
            ? onlyChild.node.id
            : null;
      }

      return { expansions, stopReason: 'branch-point' };
    },
    [repositoryName, isCurrent]
  );

  /**
   * Apply a set of fetched expansions in a single immutable state update, and
   * drop the expanded ids from the explicit-collapse set.
   */
  const applyExpansions = useCallback((expansions: readonly Expansion[], gen: number): void => {
    if (expansions.length === 0) return;
    // A superseded walk must not graft its children onto the current tree: node
    // ids can collide across repositories, so this is not merely wasted work.
    if (!isCurrent(gen)) return;
    setState((prev) => {
      let newNodes = prev.nodes;
      for (const { expandId, expandChildren } of expansions) {
        newNodes = updateNodeInTree(newNodes, expandId, (n) => ({
          ...n,
          children: expandChildren,
          expanded: true,
          loading: false,
          error: undefined,
        }));
      }
      const expandedIds = new Set(expansions.map((e) => e.expandId));
      return {
        ...prev,
        nodes: newNodes,
        userCollapsedNodeIds: prev.userCollapsedNodeIds.filter((id) => !expandedIds.has(id)),
      };
    });
  }, [isCurrent]);

  /**
   * Auto-expand the single-child folder chain hanging off the root.
   *
   * Runs once per repository after the root children land, and only when the
   * root itself has exactly one auto-expandable folder — so a repository that
   * branches at the top level stays fully collapsed and costs no extra request.
   * Deliberately touches neither `selectedNodeId` nor `focusedNodeId`: expansion
   * must not steal focus or override a user's selection while children load.
   */
  const autoExpandRootChain = useCallback(async (): Promise<void> => {
    const roots = nodesRef.current;
    if (roots.length !== 1) return;

    const only = roots[0];
    if (!isAutoExpandable(only.node)) return;
    if (userCollapsedRef.current.includes(only.node.id)) return;
    // Already opened (e.g. by a user click that raced the effect) - nothing to do.
    if (only.expanded) return;

    const gen = generationRef.current;
    try {
      const { expansions, stopReason } = await collectSingleChildChain(only.node.id, gen);
      applyExpansions(expansions, gen);
      if (isCurrent(gen)) {
        setState((prev) => ({ ...prev, autoExpandStopReason: stopReason }));
      }
    } catch {
      // Root chain is best-effort: a failed first fetch leaves the tree
      // collapsed rather than showing an error the user did not ask for.
      if (isCurrent(gen)) {
        setState((prev) => ({ ...prev, autoExpandStopReason: 'error' }));
      }
    }
  }, [applyExpansions, collectSingleChildChain, isCurrent]);

  /**
   * Expand a node, loading its children on first open.
   *
   * Distinct from {@link toggle} so keyboard ArrowRight can expand without ever
   * collapsing. Expanding a node whose children are already
   * cached opens *only* that node: descendants keep whatever expanded state they
   * had, so a branch the user deliberately closed is not cascaded back open.
   */
  const expand = useCallback(
    async (nodeId: string): Promise<void> => {
      const currentNode = findNodeById(nodesRef.current, nodeId);
      if (!currentNode || currentNode.node.leaf) return;

      // Already open: nothing to load, but the node is no longer user-collapsed.
      if (currentNode.expanded) {
        setState((prev) => ({
          ...prev,
          userCollapsedNodeIds: prev.userCollapsedNodeIds.filter((id) => id !== nodeId),
        }));
        return;
      }

      // Children cached from a previous open - reveal them without refetching.
      if (currentNode.children !== null) {
        setState((prev) => ({
          ...prev,
          nodes: updateNodeInTree(prev.nodes, nodeId, (n) => ({ ...n, expanded: true })),
          userCollapsedNodeIds: prev.userCollapsedNodeIds.filter((id) => id !== nodeId),
        }));
        return;
      }

      setState((prev) => ({
        ...prev,
        nodes: updateNodeInTree(prev.nodes, nodeId, (n) => ({
          ...n,
          loading: true,
          error: undefined,
        })),
      }));

      const gen = generationRef.current;
      try {
        const { expansions, stopReason } = await collectSingleChildChain(nodeId, gen);
        applyExpansions(expansions, gen);
        if (isCurrent(gen)) {
          setState((prev) => ({ ...prev, autoExpandStopReason: stopReason }));
        }
      } catch (err) {
        if (!isCurrent(gen)) return;
        const errorMessage = err instanceof Error ? err.message : 'Unknown error loading children';
        setState((prev) => ({
          ...prev,
          nodes: updateNodeInTree(prev.nodes, nodeId, (n) => ({
            ...n,
            loading: false,
            error: errorMessage,
          })),
        }));
      }
    },
    [applyExpansions, collectSingleChildChain, isCurrent]
  );

  /**
   * Collapse a node and record the collapse as explicit user intent.
   *
   * The recorded id is what stops auto-expansion from re-opening a branch the
   * user deliberately closed. Cached children are kept so re-expanding costs no
   * request.
   */
  const collapse = useCallback((nodeId: string): void => {
    const currentNode = findNodeById(nodesRef.current, nodeId);
    if (!currentNode || !currentNode.expanded) return;

    setState((prev) => {
      const nodes = updateNodeInTree(prev.nodes, nodeId, (n) => ({ ...n, expanded: false }));

      // Collapsing unmounts the whole subtree. The tree carries a single roving
      // tab stop, so if it was on a node inside that subtree the tree would be
      // left with no tabbable row at all — Tab would skip it and only a mouse
      // could recover it. Move it up to the node the user just collapsed, which
      // is where the user's attention already is.
      const focusStillVisible =
        !prev.focusedNodeId || getVisibleNodeIds(nodes).includes(prev.focusedNodeId);

      return {
        ...prev,
        nodes,
        focusedNodeId: focusStillVisible ? prev.focusedNodeId : nodeId,
        userCollapsedNodeIds: prev.userCollapsedNodeIds.includes(nodeId)
          ? prev.userCollapsedNodeIds
          : [...prev.userCollapsedNodeIds, nodeId],
      };
    });
  }, []);

  /**
   * Toggle expansion of a node (chevron click, double-click, Enter on a folder).
   * Thin wrapper over {@link expand} / {@link collapse} so all three entry
   * points share one state model.
   */
  const toggle = useCallback(
    async (nodeId: string): Promise<void> => {
      const currentNode = findNodeById(nodesRef.current, nodeId);
      if (!currentNode) return;
      if (currentNode.expanded) {
        collapse(nodeId);
        return;
      }
      await expand(nodeId);
    },
    [collapse, expand]
  );

  /**
   * Select a node.
   */
  const select = useCallback((nodeId: string): void => {
    setState((prev) => ({
      ...prev,
      selectedNodeId: nodeId,
      focusedNodeId: nodeId,
    }));
  }, []);

  /**
   * Refresh the tree.
   */
  const refresh = useCallback(async (): Promise<void> => {
    await loadRoot();
  }, [loadRoot]);

  /**
   * One deep-link path walk. Always go through {@link expandToPath}, which adds
   * the in-flight join; calling this directly duplicates concurrent walks.
   */
  const expandToPathUncached = useCallback(async (path: string): Promise<void> => {
    const gen = generationRef.current;
    pathGenerationRef.current += 1;
    const pathGen = pathGenerationRef.current;

    // Filter out segments that browse node generators strip from the display
    // tree.  Both NPM and Docker filter the literal "-" directory; keeping it
    // in the path would cause the lookup to fail and abort the expansion.
    const FILTERED_SEGMENTS = new Set(['-']);
    const pathParts = path.split('/').filter((s) => s !== '' && !FILTERED_SEGMENTS.has(s));

    // Build list of all nodes that need expanding along the path
    const expansions: Array<{ expandId: string; expandChildren: TreeNodeState[] }> = [];
    let currentPath = '';

    // Maintain a working tree that gets updated as we fetch children
    // so we can find nodes in previously fetched children
    let workingNodes = nodesRef.current;

    for (let i = 0; i < pathParts.length; i++) {
      const part = pathParts[i];
      currentPath = currentPath ? `${currentPath}/${part}` : part;

      let node = findNodeByIdFlexible(workingNodes, currentPath);

      // Fallback: version-aware fuzzy match for formats like NPM where the
      // tree uses tarball names (name-version.tgz) instead of version folders.
      if (!node && i > 0) {
        const parentPath = pathParts.slice(0, i).join('/');
        const parentNode = findNodeByIdFlexible(workingNodes, parentPath);
        const searchIn = parentNode?.children ?? workingNodes;
        node = findNodeByVersionFuzzy(searchIn, parentPath, part);
      }

      if (!node) {
        break;
      }
      currentPath = node.node.id;

      // If node is a folder and not yet expanded, fetch its children
      if (!node.node.leaf && !node.expanded && node.children === null) {
        // Sequence-level cancellation: a path walk for a superseded repository
        // stops instead of fetching the rest of the path.
        if (!isCurrent(gen)) return;
        try {
          const children = await fetchChildren(repositoryName, currentPath);
          if (!isCurrent(gen)) return;
          const childStates = nodesToState(children);
          expansions.push({ expandId: currentPath, expandChildren: childStates });

          // Update working tree so next iteration can find children
          workingNodes = updateNodeInTree(workingNodes, currentPath, (n) => ({
            ...n,
            children: childStates,
            expanded: true,
            loading: false,
          }));
        } catch (_err) {
          break;
        }
      }
    }

    // A superseded walk must touch neither the tree nor the selection: `select`
    // moves both selection and focus, so a late path expansion would otherwise
    // yank the user out of the repository they just switched to.
    if (!isCurrent(gen)) return;

    // Apply ALL expansions in ONE setState (GitHub style - no waiting between)
    if (expansions.length > 0) {
      setState((prev) => {
        let newNodes = prev.nodes;
        for (const { expandId, expandChildren } of expansions) {
          newNodes = updateNodeInTree(newNodes, expandId, (n) => ({
            ...n,
            children: expandChildren,
            expanded: true,
            loading: false,
          }));
        }
        return { ...prev, nodes: newNodes };
      });
    }

    const finalNodeId = currentPath || path;

    // A newer path was requested while this walk was fetching: its selection is
    // the one the user asked for, so this walk stops here. The expansions above
    // are kept — they are correct children either way, and discarding them would
    // only force the newer walk to refetch the levels the two paths share.
    if (pathGenerationRef.current !== pathGen) return;

    // Select the final node (use currentPath which tracks the actual node id after encoding resolution)
    select(finalNodeId);
  }, [repositoryName, select, isCurrent]);

  /**
   * Expand the tree to a specific path.
   * GitHub-style: Fetch all needed children in one go, then apply all expansions in ONE setState.
   * No waiting, no polling - let React handle the render cycle.
   *
   * Two independent owners request the initial deep link — this hook's own
   * post-root effect and BrowseTree's `initialPath` effect — and they can race,
   * because neither has applied its expansions yet when the other starts. Left
   * alone that walks and fetches every path level twice. The in-flight map below
   * makes the second caller join the first walk instead, so each level is
   * requested exactly once; the entry is keyed by repository *and* path and is
   * cleared when the walk settles, so a genuinely new path still expands.
   */
  const expandToPath = useCallback(async (path: string): Promise<void> => {
    if (!path) return;

    const inFlightKey = `${repositoryName}::${path}`;
    const joined = inFlightPathsRef.current.get(inFlightKey);
    if (joined) return joined;

    const walk = expandToPathUncached(path);
    inFlightPathsRef.current.set(inFlightKey, walk);
    try {
      await walk;
    } finally {
      inFlightPathsRef.current.delete(inFlightKey);
    }
  }, [repositoryName, expandToPathUncached]);

  /**
   * Move focus to the next visible node.
   */
  const focusNext = useCallback((fromNodeId?: string): void => {
    const visibleIds = getVisibleNodeIds(state.nodes);
    const origin = fromNodeId ?? state.focusedNodeId;
    const currentIndex = origin ? visibleIds.indexOf(origin) : -1;

    // An origin that is not in the visible list (stale id, node removed or just
    // hidden by a collapse) must not silently teleport focus to the first or last
    // row: there is no meaningful neighbour to move to, so nothing moves.
    if (origin && currentIndex === -1) return;

    const nextIndex = currentIndex < visibleIds.length - 1 ? currentIndex + 1 : currentIndex;
    if (nextIndex >= 0 && nextIndex < visibleIds.length) {
      setState((prev) => ({ ...prev, focusedNodeId: visibleIds[nextIndex] }));
    }
  }, [state.nodes, state.focusedNodeId]);

  /**
   * Move focus to the previous visible node.
   */
  const focusPrevious = useCallback((fromNodeId?: string): void => {
    const visibleIds = getVisibleNodeIds(state.nodes);
    const origin = fromNodeId ?? state.focusedNodeId;
    const currentIndex = origin ? visibleIds.indexOf(origin) : visibleIds.length;

    // An origin that is not in the visible list (stale id, node removed or just
    // hidden by a collapse) must not silently teleport focus to the first or last
    // row: there is no meaningful neighbour to move to, so nothing moves.
    if (origin && currentIndex === -1) return;

    const prevIndex = currentIndex > 0 ? currentIndex - 1 : 0;
    if (prevIndex >= 0 && prevIndex < visibleIds.length) {
      setState((prev) => ({ ...prev, focusedNodeId: visibleIds[prevIndex] }));
    }
  }, [state.nodes, state.focusedNodeId]);

  /**
   * Move focus to the parent node.
   *
   * Resolved from the loaded tree rather than from the node id, because child
   * ids are not reliably path-suffixes of their parent id across formats.
   */
  const focusParent = useCallback((fromNodeId?: string): void => {
    const origin = fromNodeId ?? state.focusedNodeId;
    if (!origin) return;

    const parentId = findParentNodeId(state.nodes, origin);
    if (parentId) {
      setState((prev) => ({ ...prev, focusedNodeId: parentId }));
    }
  }, [state.nodes, state.focusedNodeId]);

  /**
   * Move focus to a specific node without changing the selection.
   *
   * Focus and selection are coupled on {@link select} (clicking a node both
   * selects it and takes the roving tab stop), but arrow-key traversal must move
   * focus alone so browsing the tree does not churn the detail panel.
   */
  const focusNode = useCallback((nodeId: string): void => {
    setState((prev) => ({ ...prev, focusedNodeId: nodeId }));
  }, []);

  /**
   * Move focus to the first child node.
   */
  const focusFirstChild = useCallback((fromNodeId?: string): void => {
    const origin = fromNodeId ?? state.focusedNodeId;
    if (!origin) return;

    const currentNode = findNodeById(state.nodes, origin);
    if (currentNode?.expanded && currentNode.children && currentNode.children.length > 0) {
      setState((prev) => ({
        ...prev,
        focusedNodeId: currentNode.children![0].node.id,
      }));
    }
  }, [state.nodes, state.focusedNodeId]);

  // Load root on mount
  useEffect(() => {
    loadRoot();
  }, [loadRoot]);

  // Handle expansion after root children land: deep-link path when one was
  // requested, otherwise the single-child folder chain.
  //
  // Guarded on state.repositoryName so this never runs against the previous
  // repository's nodes after a repository switch, and on the ref so it runs once
  // per repository rather than on every rerender.
  useEffect(() => {
    if (isInitialPathExpandedRef.current) return;
    if (state.repositoryName !== repositoryName) return;
    if (state.loading || state.nodes.length === 0) return;

    isInitialPathExpandedRef.current = true;

    if (initialPath) {
      expandToPath(initialPath);
      return;
    }
    autoExpandRootChain();
  }, [
    initialPath,
    state.nodes,
    state.loading,
    state.repositoryName,
    repositoryName,
    expandToPath,
    autoExpandRootChain,
  ]);

  /**
   * Re-read one level of the tree from the server and fold it into state.
   *
   * `parentId` is `null` for the root level. Surviving siblings keep their
   * expansion and loaded children (see {@link mergeFetchedLevel}); a selection or
   * focus left pointing at a node the server no longer lists is cleared or moved
   * to a neighbour rather than left dangling.
   *
   * Generation-guarded like every other fetch here, so a late response cannot
   * write the previous repository's contents into the tree after a switch.
   * A failed re-read is a no-op: the tree keeps showing what
   * it already had.
   */
  const reloadLevel = useCallback(
    async (parentId: string | null, gen: number): Promise<void> => {
      let children: BrowseNode[];
      try {
        children = await fetchChildren(repositoryName, parentId ?? '/');
      } catch {
        return;
      }
      if (!isCurrent(gen)) return;

      setState((prev) => {
        const nodes = parentId
          ? updateNodeInTree(prev.nodes, parentId, (n) => ({
              ...n,
              children: mergeFetchedLevel(n.children ?? [], children),
              loading: false,
            }))
          : mergeFetchedLevel(prev.nodes, children);

        return {
          ...prev,
          nodes,
          selectedNodeId:
            prev.selectedNodeId && !findNodeById(nodes, prev.selectedNodeId)
              ? undefined
              : prev.selectedNodeId,
          focusedNodeId:
            prev.focusedNodeId && !findNodeById(nodes, prev.focusedNodeId)
              ? focusFallbackAfterRemoval(prev.nodes, nodes, prev.focusedNodeId)
              : prev.focusedNodeId,
        };
      });
    },
    [repositoryName, isCurrent]
  );

  /**
   * Re-read the level containing `nodeId` without removing anything.
   *
   * Used when a delete was *rejected*: the request may still have partially
   * succeeded server-side — a cloud/APT delete can remove the asset and then fail
   * updating format metadata — so the tree has to converge on what the server
   * actually holds. Because the merged level comes from the server's own listing,
   * the node stays if the server still has it and disappears if it does not, and
   * nothing is dropped optimistically either way. The caller does not navigate:
   * a rejected delete leaves the user on the node they tried to delete.
   */
  const reconcileNode = useCallback(
    async (nodeId: string): Promise<void> => {
      const parentId = findParentNodeId(nodesRef.current, nodeId);
      await reloadLevel(parentId, generationRef.current);
    },
    [reloadLevel]
  );

  /**
   * Remove a node from the tree by ID, preserving the expanded state of all other
   * nodes, then re-read the containing level so the tree reflects server-side
   * state — including a delete that did less than it reported.
   */
  const removeNode = useCallback(
    async (nodeId: string): Promise<void> => {
      // Resolve the level to re-read *before* the node leaves the tree, and
      // resolve it structurally: node ids are opaque server strings, so slicing
      // at the last `/` names a node that does not exist for every format whose
      // child ids are not path-suffixes of their parent's (npm tarballs served
      // from `-/`, Docker `sha256:` digests, Go `@v` paths). `null` means the
      // node sat at the root level.
      const parentId = findParentNodeId(nodesRef.current, nodeId);

      // Remove the node in-place from the tree
      const removeFromNodes = (nodes: readonly TreeNodeState[]): TreeNodeState[] => {
        const filtered = nodes.filter((n) => n.node.id !== nodeId);
        return filtered.map((n) => {
          if (n.children) {
            return { ...n, children: removeFromNodes(n.children) };
          }
          return n;
        });
      };

      setState((prev) => {
        const remaining = removeFromNodes(prev.nodes);
        const stillVisible = new Set(getVisibleNodeIds(remaining));
        return {
          ...prev,
          nodes: remaining,
          // Clear selection if the deleted node was selected
          selectedNodeId: prev.selectedNodeId === nodeId ? undefined : prev.selectedNodeId,
          // Keyboard focus moves to a surviving neighbour rather than vanishing.
          // Keyed on whether the *focused* node survived, not on whether it was
          // the one deleted: selection and focus diverge (click a folder, then
          // arrow into a child), and deleting the folder takes that child with
          // it. A focused id with no rendered row leaves the whole tree
          // untabbable, because the roving tab stop follows `focusedNodeId`.
          focusedNodeId:
            prev.focusedNodeId && !stillVisible.has(prev.focusedNodeId)
              ? focusFallbackAfterRemoval(prev.nodes, remaining, prev.focusedNodeId)
              : prev.focusedNodeId,
        };
      });

      const gen = generationRef.current;
      if (!isCurrent(gen)) return;
      await reloadLevel(parentId, gen);
    },
    [isCurrent, reloadLevel]
  );

  const actions: BrowseTreeActions = useMemo(
    () => ({
      toggle,
      expand,
      collapse,
      select,
      refresh,
      expandToPath,
      removeNode,
      reconcileNode,
      focusNext,
      focusPrevious,
      focusParent,
      focusFirstChild,
      focusNode,
    }),
    [
      toggle,
      expand,
      collapse,
      select,
      refresh,
      expandToPath,
      removeNode,
      reconcileNode,
      focusNext,
      focusPrevious,
      focusParent,
      focusFirstChild,
      focusNode,
    ]
  );

  return { state, actions };
}

export default useBrowseTree;

