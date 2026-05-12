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
  /** Whether this node is focused (for keyboard navigation) */
  isFocused: boolean;
  /** Currently selected node ID (for folder icon: FolderOpen when expanded or selected) */
  selectedNodeId?: string;
  /** Callback for keyboard navigation */
  onKeyDown: (event: React.KeyboardEvent, nodeId: string) => void;
  /** Base URL for node links */
  baseUrl: string;
}

/**
 * Actions for the useBrowseTree hook.
 */
export interface BrowseTreeActions {
  /** Toggle node expansion */
  toggle: (nodeId: string) => Promise<void>;
  /** Select a node */
  select: (nodeId: string) => void;
  /** Refresh the tree */
  refresh: () => Promise<void>;
  /** Expand to a specific path (for deep linking) */
  expandToPath: (path: string) => Promise<void>;
  /** Remove a node from the tree, preserving expanded state of all other nodes */
  removeNode: (nodeId: string) => Promise<void>;
  /** Navigate focus to the next visible node */
  focusNext: () => void;
  /** Navigate focus to the previous visible node */
  focusPrevious: () => void;
  /** Navigate focus to the parent node */
  focusParent: () => void;
  /** Navigate focus to the first child node */
  focusFirstChild: () => void;
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
}

