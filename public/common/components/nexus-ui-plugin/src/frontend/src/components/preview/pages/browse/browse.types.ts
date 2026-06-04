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
 * Browse Module - Type Definitions
 *
 * Central type definitions for the Browse functionality.
 * All agents should import types from here.
 */

// =============================================================================
// BROWSE NODE TYPES
// =============================================================================

/**
 * Type of node in the browse tree.
 */
export type NodeType = 'folder' | 'component' | 'asset';

/**
 * A node in the browse tree.
 * Returned by coreui_Browse.read API.
 */
export interface BrowseNode {
  /** URL-encoded path (e.g., "org/apache/maven") */
  id: string;
  /** Type of node */
  type: NodeType;
  /** Display name */
  text: string;
  /** true = no children possible */
  leaf: boolean;
  /** Component ID if this node represents a component */
  componentId?: string;
  /** Asset ID if this node represents an asset */
  assetId?: string;
  /** Package URL (purl) if available */
  packageUrl?: string;
}

// =============================================================================
// REPOSITORY TYPES
// =============================================================================

/**
 * Repository type.
 */
export type RepositoryType = 'proxy' | 'hosted' | 'group';

/**
 * Repository online/offline status.
 */
export interface RepositoryStatus {
  online: boolean;
  description?: string;
  reason?: string;
}

/**
 * Repository reference for the browse list.
 * Returned by coreui_Repository.readReferences API.
 */
export interface RepositoryReference {
  name: string;
  type: RepositoryType;
  format: string;
  status: RepositoryStatus;
  url: string;
}

// =============================================================================
// COMPONENT & ASSET TYPES
// =============================================================================

/**
 * Component data.
 * Returned by coreui_Component.readComponent API.
 */
export interface ComponentXO {
  id: string;
  repositoryName: string;
  format: string;
  group?: string;
  name: string;
  version: string;
  // Additional fields that may be present
  lastModified?: string;
  blobCreated?: string;
}

/**
 * Asset data.
 * Returned by coreui_Component.readAsset API.
 */
export interface AssetXO {
  id: string;
  name: string;
  format: string;
  repositoryName: string;
  downloadUrl: string;
  path: string;
  contentType: string;
  size: number;
  lastModified: string;
  blobCreated?: string;
  lastDownloaded?: string;
  checksums?: Record<string, string>;
  componentId?: string;
}

// =============================================================================
// API REQUEST/RESPONSE TYPES
// =============================================================================

/**
 * Parameters for loading tree nodes.
 */
export interface TreeLoadParams {
  repositoryName: string;
  /** "/" for root, or path like "/org/apache" */
  node: string;
}

/**
 * Generic API response wrapper.
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

/**
 * ExtDirect response structure.
 */
export interface ExtDirectResponse<T> {
  data: {
    result: T;
    success: boolean;
  };
}

// =============================================================================
// COMPONENT PROP TYPES
// =============================================================================

/**
 * Props for BrowseTree component.
 */
export interface BrowseTreeProps {
  /** Repository to browse */
  repositoryName: string;
  /** Initial path to expand to (for deep linking) */
  initialPath?: string;
  /** Callback when a node is selected */
  onSelectNode: (node: BrowseNode) => void;
  /** Currently selected node ID */
  selectedNodeId?: string;
}

/**
 * Props for RepositoryList component.
 */
export interface RepositoryListProps {
  /** Callback when a repository is selected */
  onSelectRepository: (repoName: string) => void;
  /** Currently selected repository */
  selectedRepository?: string;
}

/**
 * Props for DetailPanel component.
 */
export interface DetailPanelProps {
  /** Selected node (null = show empty state) */
  node: BrowseNode | null;
  /** Repository name for API calls */
  repositoryName: string;
  /** Callback after successful delete */
  onDeleted?: () => void;
}

/**
 * Props for DeleteDialog component.
 */
export type DeleteType = 'component' | 'asset' | 'folder';

export interface DeleteDialogProps {
  /** Type of item being deleted */
  type: DeleteType;
  /** Name of item being deleted */
  name: string;
  /** Callback to execute delete */
  onConfirm: () => Promise<void>;
  /** Disable the trigger button */
  disabled?: boolean;
}

/**
 * Props for CopyUrlButton component.
 */
export interface CopyUrlButtonProps {
  /** URL to copy */
  url: string;
  /** Tooltip text */
  tooltipText?: string;
  /** Success message for toast */
  successMessage?: string;
}

/**
 * Props for NodeIcon component.
 */
export interface NodeIconProps {
  /** Node type */
  type: NodeType;
  /** Icon size in pixels */
  size?: number;
  /** Additional CSS class */
  className?: string;
}

/**
 * Props for FormatBadge component.
 */
export interface FormatBadgeProps {
  /** Repository format (e.g., "maven2", "npm") */
  format: string;
}

/**
 * Props for StatusIndicator component.
 */
export interface StatusIndicatorProps {
  /** Repository status */
  status: RepositoryStatus;
  /** Show text label */
  showLabel?: boolean;
}

// =============================================================================
// TREE STATE TYPES
// =============================================================================

/**
 * State for the browse tree.
 */
export interface TreeState {
  /** Map of parent ID to children nodes */
  nodes: Map<string, BrowseNode[]>;
  /** Set of expanded node IDs */
  expandedNodes: Set<string>;
  /** Set of node IDs currently loading */
  loadingNodes: Set<string>;
  /** Currently selected node ID */
  selectedNodeId: string | null;
  /** Error message if any */
  error: string | null;
}

/**
 * Result of useBrowseTree hook.
 */
export interface UseBrowseTreeResult {
  /** Root level nodes */
  rootNodes: BrowseNode[];
  /** Loading state for initial load */
  loading: boolean;
  /** Check if a node is expanded */
  isExpanded: (nodeId: string) => boolean;
  /** Check if a node is loading children */
  isLoading: (nodeId: string) => boolean;
  /** Get children of a node */
  getChildren: (nodeId: string) => BrowseNode[];
  /** Toggle node expand/collapse */
  toggleNode: (nodeId: string) => Promise<void>;
  /** Select a node */
  selectNode: (node: BrowseNode) => void;
  /** Expand tree to a specific path */
  expandToPath: (path: string) => Promise<void>;
  /** Retry loading a node */
  retry: (nodeId: string) => void;
  /** Error message */
  error: string | null;
}

// =============================================================================
// REPOSITORY LIST STATE TYPES
// =============================================================================

/**
 * Sort direction.
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Sortable fields in repository list.
 */
export type RepositorySortField = 'name' | 'type' | 'format' | 'status';

/**
 * Server-side filter parameters for repository list.
 */
export interface RepositoryFilterParams {
  /** Comma-separated list of formats */
  formats?: string;
  /** Comma-separated list of types */
  types?: string;
  /** Comma-separated list of statuses (online/offline) */
  statuses?: string;
  /** Name filter (substring match) */
  nameFilter?: string;
  /** Field to sort by */
  sortField?: RepositorySortField;
  /** Sort direction */
  sortDirection?: SortDirection;
  /** Page number (1-indexed) */
  page?: number;
  /** Page size (max 200) */
  pageSize?: number;
}

/**
 * Paginated response from server-side filtered repository list.
 */
export interface RepositoryPageResponse {
  /** Repository data for current page */
  data: RepositoryReference[];
  /** Total count of matching repositories */
  totalCount: number;
  /** Current page number */
  page: number;
  /** Page size */
  pageSize: number;
  /** Total number of pages */
  totalPages: number;
  /** Has more pages */
  hasNextPage: boolean;
  /** Has previous pages */
  hasPreviousPage: boolean;
}

/**
 * Result of useRepositoryList hook (with server-side filtering).
 */
export interface UseRepositoryListResult {
  /** Repository list (current page) */
  repositories: RepositoryReference[];
  /** Loading state */
  loading: boolean;
  /** Error message */
  error: string | null;
  /** Current filter parameters */
  filterParams: RepositoryFilterParams;
  /** Update filter parameters (triggers server fetch) */
  setFilterParams: (params: Partial<RepositoryFilterParams>) => void;
  /** Total count of matching repositories */
  totalCount: number;
  /** Current page */
  page: number;
  /** Total pages */
  totalPages: number;
  /** Go to next page */
  nextPage: () => void;
  /** Go to previous page */
  previousPage: () => void;
  /** Go to specific page */
  goToPage: (page: number) => void;
  /** Retry loading */
  retry: () => void;
}

// =============================================================================
// DETAIL PANEL STATE TYPES
// =============================================================================

/**
 * Result of useComponentDetail hook.
 */
export interface UseComponentDetailResult {
  /** Component data */
  component: ComponentXO | null;
  /** Loading state */
  loading: boolean;
  /** Error message */
  error: string | null;
  /** Retry loading */
  retry: () => void;
}

/**
 * Result of useAssetDetail hook.
 */
export interface UseAssetDetailResult {
  /** Asset data */
  asset: AssetXO | null;
  /** Loading state */
  loading: boolean;
  /** Error message */
  error: string | null;
  /** Retry loading */
  retry: () => void;
}
