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
import { ExtAPIUtils, APIConstants } from '@sonatype/nexus-ui-plugin';
import type {
  BrowseNode,
  BrowseTreeState,
  TreeNodeState,
  UseBrowseTreeResult,
  BrowseTreeActions,
} from './browse-tree.types';
import { isMockMode } from '@/config/previewFeatureFlags';
import { getMockBrowseNodes } from '../mockData';

const { EXT: { BROWSE: { ACTION, METHODS: { READ } } } } = APIConstants;

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
 * Get the parent node ID from a node ID (path-like).
 */
function getParentId(nodeId: string): string | null {
  const lastSlash = nodeId.lastIndexOf('/');
  if (lastSlash <= 0) return null;
  return nodeId.substring(0, lastSlash);
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

/** Initial expand depth to match Heritage UI (2–3 levels visible when browsing into a repo). */
const INITIAL_EXPAND_DEPTH = 2;

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
  });

  const isInitialPathExpandedRef = useRef(false);
  const prevRepoRef = useRef(repositoryName);

  // Reset auto-expand flag when repository changes
  if (prevRepoRef.current !== repositoryName) {
    prevRepoRef.current = repositoryName;
    isInitialPathExpandedRef.current = false;
  }

  // Ref to always have the latest nodes (avoids stale closures in async operations)
  const nodesRef = useRef(state.nodes);
  nodesRef.current = state.nodes;

  /**
   * Load root level nodes.
   */
  const loadRoot = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: undefined }));

    try {
      const children = await fetchChildren(repositoryName, '/');
      setState((prev) => ({
        ...prev,
        nodes: nodesToState(children),
        loading: false,
      }));
      return children;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error loading browse tree';
      setState((prev) => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      return [];
    }
  }, [repositoryName]);

  /**
   * Auto-expand single-child folder chains from a starting node.
   * Fetches children iteratively, expanding each level, until hitting
   * a node with multiple children or a leaf. This makes the tree
   * show content immediately like the Default UI.
   *
   * Uses immutable state updates via updateNodeInTree to avoid
   * mutation bugs and race conditions with other state updates.
   */
  const autoExpandSingleChildren = useCallback(async (startNodeId?: string) => {
    if (!startNodeId) return;

    const startNode = findNodeById(nodesRef.current, startNodeId);
    if (!startNode || startNode.node.leaf) return;

    let nodeId: string | null = startNodeId;
    const MAX_AUTO_EXPAND = 50;
    const expansions: Array<{ expandId: string; expandChildren: TreeNodeState[] }> = [];

    for (let depth = 0; depth < MAX_AUTO_EXPAND && nodeId; depth++) {
      try {
        const children = await fetchChildren(repositoryName, nodeId);
        const childStates = nodesToState(children);

        expansions.push({ expandId: nodeId, expandChildren: childStates });

        if (childStates.length === 1 && !childStates[0].node.leaf) {
          nodeId = childStates[0].node.id;
        } else {
          nodeId = null;
        }
      } catch {
        break;
      }
    }

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
  }, [repositoryName]);

  /**
   * Toggle expansion of a node.
   * On first expand, loads children from API.
   * Auto-expands single child folders by fetching the entire chain
   * and applying all expansions in one immutable state update.
   */
  const toggle = useCallback(async (nodeId: string): Promise<void> => {
    // Read from ref to avoid stale closure issues
    const currentNode = findNodeById(nodesRef.current, nodeId);
    if (!currentNode) return;

    // If collapsing, just toggle
    if (currentNode.expanded) {
      setState((prev) => ({
        ...prev,
        nodes: updateNodeInTree(prev.nodes, nodeId, (n) => ({
          ...n,
          expanded: false,
        })),
      }));
      return;
    }

    // If already has children loaded, expand and auto-expand single-child chains.
    // When a cached child hasn't loaded ITS children yet, fetch deeper levels
    // so the tree matches the Default UI's cascading actor expansion depth.
    if (currentNode.children !== null) {
      let walkNode: TreeNodeState | null = currentNode;
      const pendingExpands: string[] = [nodeId];

      while (
        walkNode?.children &&
        walkNode.children.length === 1 &&
        !walkNode.children[0].node.leaf
      ) {
        pendingExpands.push(walkNode.children[0].node.id);

        if (walkNode.children[0].children === null) {
          // Child hasn't loaded yet -- fall through to the fetch path
          // to continue the single-child chain deeper
          setState((prev) => {
            let newNodes = prev.nodes;
            for (const id of pendingExpands) {
              newNodes = updateNodeInTree(newNodes, id, (n) => ({
                ...n,
                expanded: true,
              }));
            }
            return { ...prev, nodes: newNodes };
          });

          // Fetch deeper levels for the unloaded child
          await autoExpandSingleChildren(walkNode.children[0].node.id);
          return;
        }

        walkNode = walkNode.children[0];
      }

      setState((prev) => {
        let newNodes = prev.nodes;
        for (const id of pendingExpands) {
          newNodes = updateNodeInTree(newNodes, id, (n) => ({
            ...n,
            expanded: true,
          }));
        }
        return { ...prev, nodes: newNodes };
      });
      return;
    }

    // Load children for the first time
    setState((prev) => ({
      ...prev,
      nodes: updateNodeInTree(prev.nodes, nodeId, (n) => ({
        ...n,
        loading: true,
        error: undefined,
      })),
    }));

    try {
      const children = await fetchChildren(repositoryName, currentNode.node.id);
      const childStates = nodesToState(children);

      // Build auto-expand chain: keep fetching single non-leaf children (capped at MAX_AUTO_EXPAND for depth fix)
      const MAX_AUTO_EXPAND = 50;
      const expansions: Array<{ expandId: string; expandChildren: TreeNodeState[] }> = [
        { expandId: nodeId, expandChildren: childStates },
      ];

      let deepChildren = childStates;
      let depth = 0;
      while (depth < MAX_AUTO_EXPAND && deepChildren.length === 1 && !deepChildren[0].node.leaf) {
        depth++;
        try {
          const nextChildren = await fetchChildren(repositoryName, deepChildren[0].node.id);
          const nextChildStates = nodesToState(nextChildren);
          expansions.push({ expandId: deepChildren[0].node.id, expandChildren: nextChildStates });
          deepChildren = nextChildStates;
        } catch {
          break;
        }
      }

      // Apply all expansions in one immutable state update (browse tree depth fix)
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
    } catch (err) {
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
  }, [repositoryName, autoExpandSingleChildren]);

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
   * Expand the tree to a specific path.
   * GitHub-style: Fetch all needed children in one go, then apply all expansions in ONE setState.
   * No waiting, no polling - let React handle the render cycle.
   */
  const expandToPath = useCallback(async (path: string): Promise<void> => {
    if (!path) return;

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

    if (process.env.NODE_ENV !== 'production') {
      console.debug('[BrowseTree:expandToPath] path=%s parts=%o rootNodes=%d',
        path, pathParts, workingNodes.length);
    }

    for (let i = 0; i < pathParts.length; i++) {
      const part = pathParts[i];
      currentPath = currentPath ? `${currentPath}/${part}` : part;

      let node = findNodeByIdFlexible(workingNodes, currentPath);
      let matchType = node ? 'exact' : 'none';

      // Fallback: version-aware fuzzy match for formats like NPM where the
      // tree uses tarball names (name-version.tgz) instead of version folders.
      if (!node && i > 0) {
        const parentPath = pathParts.slice(0, i).join('/');
        const parentNode = findNodeByIdFlexible(workingNodes, parentPath);
        const searchIn = parentNode?.children ?? workingNodes;
        node = findNodeByVersionFuzzy(searchIn, parentPath, part);
        if (node) matchType = 'fuzzy';
      }

      if (process.env.NODE_ENV !== 'production') {
        console.debug('[BrowseTree:expandToPath] [%d/%d] segment=%s lookup=%s match=%s resolved=%s',
          i + 1, pathParts.length, part, currentPath,
          matchType, node?.node.id ?? '(none)');
      }

      if (!node) {
        break;
      }
      currentPath = node.node.id;

      // If node is a folder and not yet expanded, fetch its children
      if (!node.node.leaf && !node.expanded && node.children === null) {
        try {
          const children = await fetchChildren(repositoryName, currentPath);
          const childStates = nodesToState(children);
          expansions.push({ expandId: currentPath, expandChildren: childStates });

          // Update working tree so next iteration can find children
          workingNodes = updateNodeInTree(workingNodes, currentPath, (n) => ({
            ...n,
            children: childStates,
            expanded: true,
            loading: false,
          }));
        } catch (err) {
          break;
        }
      }
    }

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
    if (process.env.NODE_ENV !== 'production') {
      console.debug('[BrowseTree:expandToPath] selecting=%s expansions=%d', finalNodeId, expansions.length);
    }

    // Select the final node (use currentPath which tracks the actual node id after encoding resolution)
    select(finalNodeId);
  }, [repositoryName, select]);

  /**
   * Move focus to the next visible node.
   */
  const focusNext = useCallback((): void => {
    const visibleIds = getVisibleNodeIds(state.nodes);
    const currentIndex = state.focusedNodeId
      ? visibleIds.indexOf(state.focusedNodeId)
      : -1;

    const nextIndex = currentIndex < visibleIds.length - 1 ? currentIndex + 1 : currentIndex;
    if (nextIndex >= 0 && nextIndex < visibleIds.length) {
      setState((prev) => ({ ...prev, focusedNodeId: visibleIds[nextIndex] }));
    }
  }, [state.nodes, state.focusedNodeId]);

  /**
   * Move focus to the previous visible node.
   */
  const focusPrevious = useCallback((): void => {
    const visibleIds = getVisibleNodeIds(state.nodes);
    const currentIndex = state.focusedNodeId
      ? visibleIds.indexOf(state.focusedNodeId)
      : visibleIds.length;

    const prevIndex = currentIndex > 0 ? currentIndex - 1 : 0;
    if (prevIndex >= 0 && prevIndex < visibleIds.length) {
      setState((prev) => ({ ...prev, focusedNodeId: visibleIds[prevIndex] }));
    }
  }, [state.nodes, state.focusedNodeId]);

  /**
   * Move focus to the parent node.
   */
  const focusParent = useCallback((): void => {
    if (!state.focusedNodeId) return;

    const parentId = getParentId(state.focusedNodeId);
    if (parentId) {
      setState((prev) => ({ ...prev, focusedNodeId: parentId }));
    }
  }, [state.focusedNodeId]);

  /**
   * Move focus to the first child node.
   */
  const focusFirstChild = useCallback((): void => {
    if (!state.focusedNodeId) return;

    const currentNode = findNodeById(state.nodes, state.focusedNodeId);
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

  // Handle initial path expansion after root is loaded.
  // Only expand when deep-linking (initialPath present); otherwise all folders start collapsed.
  useEffect(() => {
    if (
      !isInitialPathExpandedRef.current &&
      state.nodes.length > 0 &&
      !state.loading
    ) {
      isInitialPathExpandedRef.current = true;

      if (initialPath) {
        expandToPath(initialPath);
      }
      // No auto-expand when there's no initialPath — folders start collapsed
    }
  }, [initialPath, state.nodes, state.loading, expandToPath]);

  /**
   * Remove a node from the tree by ID, preserving the expanded state of all other nodes.
   * After removal, reloads the parent folder's children from the API to reflect server-side state.
   */
  const removeNode = useCallback(async (nodeId: string): Promise<void> => {
    // Find the parent path so we can reload its children
    const parentId = getParentId(nodeId);

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

    setState((prev) => ({
      ...prev,
      nodes: removeFromNodes(prev.nodes),
      // Clear selection if the deleted node was selected
      selectedNodeId: prev.selectedNodeId === nodeId ? undefined : prev.selectedNodeId,
      focusedNodeId: prev.focusedNodeId === nodeId ? undefined : prev.focusedNodeId,
    }));

    // Reload the parent folder's children from the API to reflect server-side state
    if (parentId) {
      try {
        const children = await fetchChildren(repositoryName, parentId);
        const childStates = nodesToState(children);
        setState((prev) => ({
          ...prev,
          nodes: updateNodeInTree(prev.nodes, parentId, (n) => ({
            ...n,
            children: childStates,
          })),
        }));
      } catch {
        // Parent reload failed — the optimistic removal is still valid
      }
    } else {
      // Deleted a root-level node — reload root
      try {
        const children = await fetchChildren(repositoryName, '/');
        setState((prev) => ({
          ...prev,
          nodes: nodesToState(children),
        }));
      } catch {
        // Root reload failed — the optimistic removal is still valid
      }
    }
  }, [repositoryName]);

  const actions: BrowseTreeActions = useMemo(
    () => ({
      toggle,
      select,
      refresh,
      expandToPath,
      removeNode,
      focusNext,
      focusPrevious,
      focusParent,
      focusFirstChild,
    }),
    [toggle, select, refresh, expandToPath, removeNode, focusNext, focusPrevious, focusParent, focusFirstChild]
  );

  return { state, actions };
}

export default useBrowseTree;

