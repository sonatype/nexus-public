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
 * Browse Detail Panel Components
 *
 * Components for displaying detailed information about selected items
 * in the browse tree (components, assets, and folders).
 */

// Main container component
export { DetailPanel, default } from './DetailPanel';

// Individual detail panels
export { ComponentDetailPanel } from './ComponentDetailPanel';
export { AssetDetailPanel } from './AssetDetailPanel';
export { FolderDetailPanel } from './FolderDetailPanel';

// Types
export type {
  BrowseNode,
  BrowseNodeType,
  ComponentXO,
  AssetXO,
  AssetAttributes,
  FolderInfo,
  DetailPanelSelection,
  DetailPanelProps,
  ComponentDetailPanelProps,
  AssetDetailPanelProps,
  FolderDetailPanelProps,
  DetailPanelStrings,
} from './detail.types';

// Utilities
export {
  formatFileSize,
  formatDate,
  formatRelativeDate,
  getAssetDownloadUrl,
  truncateText,
  getFilenameFromPath,
  isAssetCached,
  getLastDownloadedDisplay,
} from './detail.utils';

