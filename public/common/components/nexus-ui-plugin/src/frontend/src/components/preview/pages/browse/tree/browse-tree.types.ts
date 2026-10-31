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
 * Browse Tree Types
 *
 * Types for the Browse Tree component - a lazy-loading tree that displays
 * repository contents with expand/collapse, deep linking, and keyboard navigation.
 */

/**
 * Type of node in the browse tree.
 */
export type BrowseNodeType = 'folder' | 'component' | 'asset';

/**
 * A node in the browse tree as returned by the API.
 */
export interface BrowseNode {
  /** Unique identifier for the node (path-like, e.g., "folder1/component1/asset1.txt") */
  readonly id: string;
  /** Display text for the node */
  readonly text: string;
  /** Type of node */
  readonly type: BrowseNodeType;
  /** Whether this is a leaf node (no children) */
  readonly leaf: boolean;
  /** Component ID if this is a component or asset */
  readonly componentId?: string | null;
  /** Asset ID if this is an asset */
  readonly assetId?: string | null;
  /** Package URL if available */
  readonly packageUrl?: string | null;
}

/**
 * State of a tree node with children and loading info.
 */
export interface TreeNodeState {
  /** The node data */
  readonly node: BrowseNode;
  /** Whether the node is expanded */
  readonly expanded: boolean;
  /** Child nodes (null if not loaded, empty array if loaded but no children) */
  readonly children: readonly TreeNodeState[] | null;
  /** Whether children are currently loading */
  readonly loading: boolean;
  /** Error message if loading failed */
  readonly error?: string;
}

/**
 * Why an auto-expansion walk stopped.
 *
 * Distinguishing these matters because only `branch-point` means the walk
 * actually reached the first branching folder; the others mean it gave up early
 * and the branch point may still lie below the deepest opened node.
 */
export type AutoExpandStopReason =
  /** The chain genuinely branched (or ended) — auto-expansion is complete. */
  | 'branch-point'
  /** {@link AUTO_EXPAND_MAX_LEVELS} reached; deeper levels were NOT opened. */
  | 'safety-limit'
  /** The server returned a chain that loops back on itself. */
  | 'cycle'
  /** A fetch below the first level failed; levels above it are still applied. */
  | 'error'
  /** The repository changed (or the tree reloaded) mid-walk. */
  | 'aborted';

/**
 * State for the entire browse tree.
 */
export interface BrowseTreeState {
  /** Repository name being browsed */
  readonly repositoryName: string;
  /** Root level nodes */
  readonly nodes: readonly TreeNodeState[];
  /** Whether the tree is loading */
  readonly loading: boolean;
  /** Error message if loading failed */
  readonly error?: string;
  /** Currently focused node ID (for keyboard navigation) */
  readonly focusedNodeId?: string;
  /** Currently selected node ID */
  readonly selectedNodeId?: string;
  /**
   * IDs of nodes the user explicitly collapsed.
   *
   * Auto-expansion of single-child folder chains must never re-open a branch the
   * user deliberately closed, so every collapse records the node here and every
   * explicit expand removes it again.
   */
  readonly userCollapsedNodeIds: readonly string[];
  /**
   * Outcome of the most recent auto-expansion walk, or undefined when none has
   * finished for the current repository.
   *
   * Surfaced on state so a bounded stop is never mistaken for having reached the
   * branch point: anything other than `'branch-point'` means auto-expansion is
   * incomplete and the user must keep expanding manually.
   */
  readonly autoExpandStopReason?: AutoExpandStopReason;
}

/**
 * Props for the BrowseTree component.
 */
export interface BrowseTreeProps {
  /** Repository name to browse */
  repositoryName: string;
  /** Initial path to expand to (for deep linking / bookmarking) */
  initialPath?: string;
  /** Callback when a node is selected */
  onSelect?: (node: BrowseNode) => void;
  /** Base URL for node links */
  baseUrl?: string;
  /** Custom class name */
  className?: string;
  /** Show search/filter input above tree (default: true) */
  showSearch?: boolean;
}

/**
 * Props for the BrowseTreeNode component.
 */
export interface BrowseTreeNodeProps {
  /** The node state */
  nodeState: TreeNodeState;
  /** Repository name (for URL generation) */
  repositoryName: string;
  /** Depth level in the tree (0 = root) */
  depth: number;
  /** Callback when node is toggled */
  onToggle: (nodeId: string) => void;
  /** Callback when node is selected */
  onSelect?: (node: BrowseNode) => void;
  /**
   * Explicit focus override. Optional because the tree does not pass it: focus is
   * derived from {@link focusedNodeId} so that nested nodes resolve it the same
   * way root-level ones do.
   */
  isFocused?: boolean;
  /**
   * Node that currently owns keyboard focus, or undefined while the user has not
   * yet driven focus into the tree. Passed down the recursion so nested nodes can
   * receive DOM focus — without it only root-level nodes are reachable.
   */
  focusedNodeId?: string;
  /**
   * Node that holds the roving tab stop. Defaults to the first visible node so
   * the tree is reachable with a single Tab press; owning the tab stop does *not*
   * imply DOM focus, which only {@link focusedNodeId} grants.
   */
  tabStopNodeId?: string;
  /** Currently selected node ID (for folder icon: FolderOpen when expanded or selected) */
  selectedNodeId?: string;
  /** Callback for keyboard navigation */
  onKeyDown: (event: React.KeyboardEvent, nodeId: string) => void;
  /** Base URL for node links */
  baseUrl: string;
  /** Number of siblings at this level (for aria-setsize) */
  siblingCount?: number;
  /** 1-based position among siblings (for aria-posinset) */
  siblingIndex?: number;
}

/**
 * Actions for the useBrowseTree hook.
 */
export interface BrowseTreeActions {
  /** Toggle node expansion */
  toggle: (nodeId: string) => Promise<void>;
  /** Expand a node (no-op when already expanded); auto-expands single-child chains */
  expand: (nodeId: string) => Promise<void>;
  /** Collapse a node (no-op when already collapsed); records an explicit user collapse */
  collapse: (nodeId: string) => void;
  /** Select a node */
  select: (nodeId: string) => void;
  /** Refresh the tree */
  refresh: () => Promise<void>;
  /** Expand to a specific path (for deep linking) */
  expandToPath: (path: string) => Promise<void>;
  /** Remove a node from the tree, preserving expanded state of all other nodes */
  removeNode: (nodeId: string) => Promise<void>;
  /**
   * Re-read the level containing a node from the server without removing
   * anything, dropping the node only if the server no longer lists it.
   *
   * The rejected-delete counterpart to {@link removeNode}: a delete that failed
   * may still have partially succeeded, so the tree reconciles with the server
   * instead of either trusting the error or dropping the node optimistically.
   */
  reconcileNode: (nodeId: string) => Promise<void>;
  /**
   * Navigate focus to the next visible node.
   *
   * `fromNodeId` is the traversal origin; pass the node that received the key
   * event so navigation works before focus has ever been set (the first visible
   * node holds the tab stop implicitly, so it can receive keys while
   * `focusedNodeId` is still undefined).
   */
  focusNext: (fromNodeId?: string) => void;
  /** Navigate focus to the previous visible node; see {@link focusNext} for `fromNodeId` */
  focusPrevious: (fromNodeId?: string) => void;
  /** Navigate focus to the parent node; see {@link focusNext} for `fromNodeId` */
  focusParent: (fromNodeId?: string) => void;
  /** Navigate focus to the first child node; see {@link focusNext} for `fromNodeId` */
  focusFirstChild: (fromNodeId?: string) => void;
  /** Move focus to a specific node without changing the selection */
  focusNode: (nodeId: string) => void;
}

/**
 * Return type of the useBrowseTree hook.
 */
export interface UseBrowseTreeResult {
  /** Current tree state */
  state: BrowseTreeState;
  /** Actions to manipulate the tree */
  actions: BrowseTreeActions;
}

/**
 * API response type for fetching children.
 */
export interface FetchChildrenResponse {
  /** Array of child nodes */
  readonly data: BrowseNode[];
}

/**
 * Strings used in the browse tree UI.
 */
export interface BrowseTreeStrings {
  /** Title for the browse tree section */
  readonly title: string;
  /** Empty message when no content */
  readonly emptyMessage: string;
  /** Loading message */
  readonly loadingMessage: string;
  /** Error message prefix */
  readonly errorPrefix: string;
  /** Retry button text */
  readonly retryButton: string;
  /** Expand button label (for screen readers) */
  readonly expandLabel: string;
  /** Collapse button label (for screen readers) */
  readonly collapseLabel: string;
}

/**
 * Ref handle for BrowseTree component.
 * Exposes methods that parent components can call imperatively.
 */
export interface BrowseTreeRef {
  /** Expand the tree to show a specific path and scroll it into view */
  expandToPath: (path: string) => Promise<void>;
  /** Scroll a node into view by its ID */
  scrollToNode: (nodeId: string) => void;
  /** Remove a node from the tree, preserving expanded state of all other nodes */
  removeNode: (nodeId: string) => Promise<void>;
  /** Re-read the level containing a node, dropping it only if the server has */
  reconcileNode: (nodeId: string) => Promise<void>;
}

