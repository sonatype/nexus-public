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
 * Browse Detail Types
 *
 * Type definitions for the Browse detail panel components.
 * These types mirror the ExtJS models in:
 * - NX.coreui.model.Component
 * - NX.coreui.model.Asset
 */

/**
 * Node type in the browse tree.
 */
export type BrowseNodeType = 'folder' | 'component' | 'asset';

/**
 * Represents a node in the browse tree.
 * A node can be a folder, component, or asset.
 */
export interface BrowseNode {
  /** Unique identifier for the node */
  readonly id: string;
  /** Display text for the node */
  readonly text: string;
  /** Node type (folder, component, or asset) */
  readonly type: BrowseNodeType;
  /** Whether this is a leaf node (no children) */
  readonly leaf: boolean;
  /** Component ID if this is a component or asset node */
  readonly componentId?: string;
  /** Asset ID if this is an asset node */
  readonly assetId?: string;
}

/**
 * Component transfer object - matches NX.coreui.model.Component.
 * Represents a component (e.g., a Maven artifact, npm package) in the repository.
 */
export interface ComponentXO {
  /** Unique identifier for the component */
  readonly id: string;
  /** Grouping key used for identification */
  readonly groupingKey?: string;
  /** Repository name */
  readonly repositoryName: string;
  /** Component group (e.g., Maven groupId) */
  readonly group: string | null;
  /** Component name (e.g., Maven artifactId) */
  readonly name: string;
  /** Component version */
  readonly version: string | null;
  /** Repository format (e.g., maven2, npm, docker) */
  readonly format: string;
}

/**
 * Asset attributes - format-specific metadata.
 */
export interface AssetAttributes {
  /** Checksum information */
  readonly checksum?: {
    readonly sha1?: string;
    readonly sha256?: string;
    readonly sha512?: string;
    readonly md5?: string;
  };
  /** Maven-specific attributes */
  readonly maven2?: {
    readonly groupId?: string;
    readonly artifactId?: string;
    readonly version?: string;
    readonly baseVersion?: string;
    readonly extension?: string;
    readonly classifier?: string;
  };
  /** npm-specific attributes */
  readonly npm?: {
    readonly name?: string;
    readonly version?: string;
  };
  /** Docker-specific attributes */
  readonly docker?: {
    readonly imageName?: string;
    readonly imageTag?: string;
  };
  /** Content attributes */
  readonly content?: {
    readonly last_modified?: string;
    readonly etag?: string;
  };
  /** Any other format-specific attributes */
  readonly [key: string]: unknown;
}

/**
 * Asset transfer object - matches NX.coreui.model.Asset.
 * Represents a file/blob in the repository.
 */
export interface AssetXO {
  /** Unique identifier for the asset */
  readonly id: string;
  /** Asset name (usually the filename) */
  readonly name: string;
  /** Repository format */
  readonly format: string;
  /** MIME content type */
  readonly contentType: string;
  /** File size in bytes */
  readonly size: number;
  /** Repository name */
  readonly repositoryName: string;
  /** Containing repository name (for group repositories) */
  readonly containingRepositoryName?: string;
  /** ISO 8601 timestamp when blob was created */
  readonly blobCreated: string | null;
  /** ISO 8601 timestamp when blob was last updated */
  readonly blobUpdated: string | null;
  /** ISO 8601 timestamp when asset was last downloaded */
  readonly lastDownloaded: string | null;
  /** Number of times asset has been downloaded */
  readonly downloadCount?: number;
  /** Blob reference identifier */
  readonly blobRef: string | null;
  /** Parent component ID */
  readonly componentId: string | null;
  /** User who uploaded the asset */
  readonly createdBy: string | null;
  /** IP address of uploader */
  readonly createdByIp: string | null;
  /** Format-specific attributes */
  readonly attributes?: AssetAttributes;
}

/**
 * Folder information for folder nodes.
 */
export interface FolderInfo {
  /** Full path of the folder */
  readonly path: string;
  /** Folder name (last segment of path) */
  readonly folderName: string;
  /** Repository name */
  readonly repositoryName: string;
}

/**
 * Props for the ComponentDetailPanel component.
 */
export interface ComponentDetailPanelProps {
  /** Component data to display */
  component: ComponentXO;
  /** Callback when delete is requested */
  onDelete?: () => void;
  /** Whether delete is allowed */
  canDelete?: boolean;
}

/**
 * Props for the AssetDetailPanel component.
 */
export interface AssetDetailPanelProps {
  /** Asset data to display */
  asset: AssetXO;
  /** Parent component data (optional) */
  component?: ComponentXO;
  /** Callback when delete is requested */
  onDelete?: () => void;
  /** Whether delete is allowed */
  canDelete?: boolean;
  /** Callback when download is requested */
  onDownload?: () => void;
}

/**
 * Props for the FolderDetailPanel component.
 */
export interface FolderDetailPanelProps {
  /** Folder information */
  folder: FolderInfo;
  /** Callback when delete is requested */
  onDelete?: () => void;
  /** Whether delete is allowed */
  canDelete?: boolean;
}

/**
 * Union type for selected item in the detail panel.
 */
export type DetailPanelSelection =
  | { type: 'component'; data: ComponentXO }
  | { type: 'asset'; data: AssetXO; component?: ComponentXO }
  | { type: 'folder'; data: FolderInfo }
  | null;

/**
 * Props for the main DetailPanel component.
 */
export interface DetailPanelProps {
  /** Currently selected item */
  selection: DetailPanelSelection;
  /** Callback when delete is requested */
  onDelete?: (type: 'component' | 'asset' | 'folder', id: string) => void;
  /** Callback when download is requested (for assets) */
  onDownload?: (assetId: string) => void;
  /** Whether delete operations are allowed */
  canDelete?: boolean;
}

/**
 * UI strings for detail panel components.
 */
export interface DetailPanelStrings {
  /** Component panel strings */
  readonly component: {
    readonly title: string;
    readonly repository: string;
    readonly format: string;
    readonly group: string;
    readonly name: string;
    readonly version: string;
    readonly deleteButton: string;
    readonly deleteConfirmTitle: string;
    readonly deleteConfirmMessage: string;
  };
  /** Asset panel strings */
  readonly asset: {
    readonly summaryTitle: string;
    readonly attributesTitle: string;
    readonly repository: string;
    readonly format: string;
    readonly group: string;
    readonly name: string;
    readonly version: string;
    readonly path: string;
    readonly contentType: string;
    readonly fileSize: string;
    readonly blobCreated: string;
    readonly blobUpdated: string;
    readonly downloadCount: string;
    readonly lastDownloaded: string;
    readonly locallyCached: string;
    readonly blobRef: string;
    readonly containingRepository: string;
    readonly uploadedBy: string;
    readonly uploadedFromIp: string;
    readonly deleteButton: string;
    readonly downloadButton: string;
    readonly deleteConfirmTitle: string;
  };
  /** Folder panel strings */
  readonly folder: {
    readonly title: string;
    readonly path: string;
    readonly deleteButton: string;
    readonly deleteConfirmTitle: string;
    readonly deleteConfirmMessage: string;
  };
  /** Empty state */
  readonly emptyState: string;
}

