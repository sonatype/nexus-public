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

import { useState, useCallback } from 'react';
import { restClient, parseApiError } from '../../../../../interface/api';
import { useToast } from '../../../shared';

import type {
  DeleteItemInfo,
  DeleteResult,
  UseActionsOptions,
  UseActionsReturn,
} from './actions.types';
import { DELETE_ENDPOINTS, ACTION_STRINGS } from './actions.types';

/**
 * Extract error message from the REST response error.
 */
function getErrorMessage(error: unknown): string {
  const parsed = parseApiError(error);
  return parsed.message || ACTION_STRINGS.errors.networkError;
}

/**
 * useActions provides API methods for delete operations on components, assets, and folders.
 *
 * This hook manages:
 * - Delete API calls for components and assets
 * - Loading state during operations
 * - Success/error callbacks
 * - ExtJS toast notifications
 *
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { deleteComponent, deleteAsset, isDeleting } = useActions({
 *     onDeleteSuccess: (item) => {
 *       console.log('Deleted:', item.name);
 *       refreshList();
 *     },
 *     onDeleteError: (item, error) => {
 *       console.error('Failed to delete:', item.name, error);
 *     },
 *   });
 *
 *   const handleDeleteComponent = async () => {
 *     await deleteComponent('component-123', 'maven-releases');
 *   };
 *
 *   return (
 *     <button onClick={handleDeleteComponent} disabled={isDeleting}>
 *       Delete
 *     </button>
 *   );
 * }
 * ```
 */
export function useActions(options: UseActionsOptions = {}): UseActionsReturn {
  const { onDeleteSuccess, onDeleteError } = options;
  const toast = useToast();

  const [isDeleting, setIsDeleting] = useState(false);
  const [deletingItem, setDeletingItem] = useState<DeleteItemInfo | null>(null);

  /**
   * Delete a component by ID.
   *
   * @param componentId - The component ID to delete
   * @param repositoryName - Optional repository name for context
   * @returns Promise resolving to delete result
   */
  const deleteComponent = useCallback(
    async (componentId: string, repositoryName?: string): Promise<DeleteResult> => {
      const item: DeleteItemInfo = {
        type: 'component',
        id: componentId,
        name: componentId, // Will be overwritten with actual name by caller if known
        repositoryName,
      };

      setIsDeleting(true);
      setDeletingItem(item);

      try {
        await restClient.delete(`${DELETE_ENDPOINTS.COMPONENT}/${encodeURIComponent(componentId)}`);

        setIsDeleting(false);
        setDeletingItem(null);

        if (onDeleteSuccess) {
          onDeleteSuccess(item);
        }

        return { success: true };
      } catch (error) {
        const errorMessage = getErrorMessage(error);

        setIsDeleting(false);
        setDeletingItem(null);

        toast.error(`${ACTION_STRINGS.errors.deleteComponentFailed}: ${errorMessage}`);

        if (onDeleteError) {
          onDeleteError(item, errorMessage);
        }

        return { success: false, error: errorMessage };
      }
    },
    [onDeleteSuccess, onDeleteError, toast]
  );

  /**
   * Delete an asset by ID.
   *
   * @param assetId - The asset ID to delete
   * @param repositoryName - Optional repository name for context
   * @returns Promise resolving to delete result
   */
  const deleteAsset = useCallback(
    async (assetId: string, repositoryName?: string): Promise<DeleteResult> => {
      const item: DeleteItemInfo = {
        type: 'asset',
        id: assetId,
        name: assetId, // Will be overwritten with actual name by caller if known
        repositoryName,
      };

      setIsDeleting(true);
      setDeletingItem(item);

      try {
        await restClient.delete(`${DELETE_ENDPOINTS.ASSET}/${encodeURIComponent(assetId)}`);

        setIsDeleting(false);
        setDeletingItem(null);

        if (onDeleteSuccess) {
          onDeleteSuccess(item);
        }

        return { success: true };
      } catch (error) {
        const errorMessage = getErrorMessage(error);

        setIsDeleting(false);
        setDeletingItem(null);

        toast.error(`${ACTION_STRINGS.errors.deleteAssetFailed}: ${errorMessage}`);

        if (onDeleteError) {
          onDeleteError(item, errorMessage);
        }

        return { success: false, error: errorMessage };
      }
    },
    [onDeleteSuccess, onDeleteError, toast]
  );

  /**
   * Delete a folder by path.
   *
   * Note: Folder deletion may not be directly supported by the REST API.
   * This method provides the interface for future implementation or
   * may need to recursively delete contained assets.
   *
   * @param path - The folder path to delete
   * @param repositoryName - The repository name containing the folder
   * @returns Promise resolving to delete result
   */
  const deleteFolder = useCallback(
    async (path: string, repositoryName: string): Promise<DeleteResult> => {
      const item: DeleteItemInfo = {
        type: 'folder',
        id: path,
        name: path.split('/').pop() || path,
        repositoryName,
      };

      setIsDeleting(true);
      setDeletingItem(item);

      try {
        // node.id from the browse API has URL-encoded segments (e.g., @v → %40v).
        // Decode first to avoid double-encoding when encodeURIComponent re-encodes.
        const decodedPath = decodeURIComponent(path);
        await restClient.delete(`/service/rest/v1/repositories/${repositoryName}/browse?path=${encodeURIComponent(decodedPath)}`);

        setIsDeleting(false);
        setDeletingItem(null);

        if (onDeleteSuccess) {
          onDeleteSuccess(item);
        }

        return { success: true };
      } catch (error) {
        const errorMessage = getErrorMessage(error);

        setIsDeleting(false);
        setDeletingItem(null);

        toast.error(`${ACTION_STRINGS.errors.deleteFolderFailed}: ${errorMessage}`);

        if (onDeleteError) {
          onDeleteError(item, errorMessage);
        }

        return { success: false, error: errorMessage };
      }
    },
    [onDeleteSuccess, onDeleteError, toast]
  );

  return {
    deleteComponent,
    deleteAsset,
    deleteFolder,
    isDeleting,
    deletingItem,
  };
}

export default useActions;
