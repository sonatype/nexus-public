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
 * Browse Actions Types
 *
 * Type definitions for the Browse action components:
 * - DeleteDialog - Confirmation dialog for delete operations
 * - CopyUrlButton - Copy URL to clipboard
 * - useActions - Delete API hook
 */

/**
 * Type of item being deleted.
 */
export type DeleteItemType = 'component' | 'asset' | 'folder';

/**
 * Information about the item to be deleted.
 */
export interface DeleteItemInfo {
  /** Type of item being deleted */
  readonly type: DeleteItemType;
  /** Unique identifier for the item */
  readonly id: string;
  /** Display name for the item */
  readonly name: string;
  /** Repository name the item belongs to */
  readonly repositoryName?: string;
}

/**
 * Props for the DeleteDialog component.
 */
export interface DeleteDialogProps {
  /** Whether the dialog is open */
  open: boolean;
  /** Callback when the dialog open state changes */
  onOpenChange: (open: boolean) => void;
  /** Item to be deleted */
  item: DeleteItemInfo | null;
  /** Callback when delete is confirmed */
  onConfirm: () => void;
  /** Whether the delete operation is in progress */
  isDeleting?: boolean;
}

/**
 * Props for the CopyUrlButton component.
 */
export interface CopyUrlButtonProps {
  /** URL to copy to clipboard */
  url: string;
  /** Optional tooltip text (default: "Copy URL to Clipboard") */
  tooltipText?: string;
  /** Optional success message (default: "URL Copied to Clipboard") */
  successMessage?: string;
  /** Size of the button */
  size?: 'small' | 'medium' | 'large';
  /** Additional CSS class names */
  className?: string;
  /** Whether the button is disabled */
  disabled?: boolean;
}

/**
 * Result of a delete operation.
 */
export interface DeleteResult {
  /** Whether the delete was successful */
  success: boolean;
  /** Error message if the delete failed */
  error?: string;
}

/**
 * Options for the useActions hook.
 */
export interface UseActionsOptions {
  /** Callback when delete succeeds */
  onDeleteSuccess?: (item: DeleteItemInfo) => void;
  /** Callback when delete fails */
  onDeleteError?: (item: DeleteItemInfo, error: string) => void;
}

/**
 * Return value of the useActions hook.
 */
export interface UseActionsReturn {
  /** Delete a component by ID */
  deleteComponent: (componentId: string, repositoryName?: string) => Promise<DeleteResult>;
  /** Delete an asset by ID */
  deleteAsset: (assetId: string, repositoryName?: string) => Promise<DeleteResult>;
  /** Delete a folder by path */
  deleteFolder: (path: string, repositoryName: string) => Promise<DeleteResult>;
  /** Whether a delete operation is in progress */
  isDeleting: boolean;
  /** The item currently being deleted (if any) */
  deletingItem: DeleteItemInfo | null;
}

/**
 * API endpoints for delete operations.
 */
export const DELETE_ENDPOINTS = {
  /** Delete a component: DELETE /service/rest/v1/components/{id} */
  COMPONENT: '/service/rest/v1/components',
  /** Delete an asset: DELETE /service/rest/v1/assets/{id} */
  ASSET: '/service/rest/v1/assets',
} as const;

/**
 * UI strings for the action components.
 */
export interface ActionStrings {
  /** Delete dialog strings */
  readonly deleteDialog: {
    readonly componentTitle: string;
    readonly assetTitle: string;
    readonly folderTitle: string;
    readonly componentMessage: (name: string) => string;
    readonly assetMessage: (name: string) => string;
    readonly folderMessage: (name: string) => string;
    readonly confirmButton: string;
    readonly cancelButton: string;
    readonly deletingButton: string;
  };
  /** Copy URL button strings */
  readonly copyUrl: {
    readonly tooltipText: string;
    readonly successMessage: string;
    readonly errorMessage: string;
  };
  /** Error messages */
  readonly errors: {
    readonly deleteComponentFailed: string;
    readonly deleteAssetFailed: string;
    readonly deleteFolderFailed: string;
    readonly networkError: string;
  };
}

/**
 * Default UI strings for action components.
 */
export const ACTION_STRINGS: ActionStrings = {
  deleteDialog: {
    componentTitle: 'Delete Component',
    assetTitle: 'Delete Asset',
    folderTitle: 'Delete Folder',
    componentMessage: (name: string) =>
      `Are you sure you want to delete component "${name}"? This action cannot be undone.`,
    assetMessage: (name: string) =>
      `Are you sure you want to delete asset "${name}"? This action cannot be undone.`,
    folderMessage: (name: string) =>
      `Are you sure you want to delete folder "${name}" and all its contents? This action cannot be undone.`,
    confirmButton: 'Delete',
    cancelButton: 'Cancel',
    deletingButton: 'Deleting...',
  },
  copyUrl: {
    tooltipText: 'Copy URL to Clipboard',
    successMessage: 'URL Copied to Clipboard',
    errorMessage: 'Failed to copy URL',
  },
  errors: {
    deleteComponentFailed: 'Failed to delete component',
    deleteAssetFailed: 'Failed to delete asset',
    deleteFolderFailed: 'Failed to delete folder',
    networkError: 'Network error occurred',
  },
};

