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

import { useState, useEffect, useCallback } from 'react';
import Axios from 'axios';
import { ExtAPIUtils } from '../../../../../interface/ExtAPIUtils';
import { ExtJS } from '../../../../../interface/ExtJS';
import { useToast } from '../../../shared';

import type {
  AssetDetailData,
  ComponentDetailData,
  ComponentTag,
  LifecycleData,
  UseAssetDetailParams,
  UseAssetDetailState,
  UseAssetDetailActions,
} from './asset-detail.types';

/**
 * Decode base64 asset ID.
 * Handles URL-encoded base64 where special characters may be percent-encoded
 * and + characters may have been converted to spaces.
 */
function decodeAssetId(encodedId: string): string {
  try {
    // First, URL-decode any percent-encoded characters (%3D, %2B, %2F, etc.)
    const urlDecoded = decodeURIComponent(encodedId);
    // URL parsing converts + to space, so reverse that before base64 decoding
    const normalizedBase64 = urlDecoded.replace(/ /g, '+');
    return atob(normalizedBase64);
  } catch {
    return encodedId;
  }
}

/**
 * Custom hook for fetching and managing asset detail data.
 *
 * Features:
 * - Fetches asset metadata
 * - Fetches associated component data
 * - Fetches component tags
 * - Provides add/remove tag functionality
 * - Error handling with retry
 */
export function useAssetDetail({
  repositoryName,
  assetId,
  componentId,
}: UseAssetDetailParams): UseAssetDetailState & UseAssetDetailActions {
  // State
  const [asset, setAsset] = useState<AssetDetailData | null>(null);
  const [component, setComponent] = useState<ComponentDetailData | null>(null);
  const [tags, setTags] = useState<ComponentTag[]>([]);
  const [lifecycle, setLifecycle] = useState<LifecycleData | null>(null);
  const [loading, setLoading] = useState(true);
  const [tagsLoading, setTagsLoading] = useState(false);
  const [lifecycleLoading, setLifecycleLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const toast = useToast();

  /**
   * Fetch asset data from API.
   */
  const fetchAssetData = useCallback(async () => {
    if (!repositoryName || !assetId) return;

    setLoading(true);
    setError(null);

    try {
      // Decode the asset ID
      const decodedAssetId = decodeAssetId(assetId);

      // Fetch asset via ExtDirect API - positional params: assetId, repositoryName
      const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'readAsset', [
        decodedAssetId,
        repositoryName,
      ]);

      const result = ExtAPIUtils.checkForError(response);

      if (result?.success && result?.data) {
        const assetData = result.data;
        
        setAsset({
          id: assetData.id || assetId,
          name: assetData.name || assetData.path?.split('/').pop() || '',
          path: assetData.path || assetData.name || '',
          format: assetData.format || '',
          contentType: assetData.contentType,
          size: assetData.size,
          blobCreated: assetData.blobCreated,
          blobUpdated: assetData.blobUpdated,
          lastDownloaded: assetData.lastDownloaded,
          locallyCached: assetData.locallyCached,
          blobRef: assetData.blobRef,
          uploader: assetData.uploader,
          uploaderIp: assetData.uploaderIp,
          downloadUrl: assetData.downloadUrl || `/repository/${repositoryName}/${assetData.path}`,
          checksum: assetData.checksum,
          attributes: assetData.attributes,
        });

        // If we have component info, set it
        if (assetData.componentId || componentId) {
          setComponent({
            id: assetData.componentId || componentId || '',
            name: assetData.componentName || assetData.name,
            group: assetData.componentGroup,
            version: assetData.componentVersion,
            format: assetData.format || '',
            repository: repositoryName,
          });
        }
      } else {
        throw new Error(result?.message || 'Failed to load asset');
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load asset details';
      setError(message);
      console.error('Error fetching asset:', err);
    } finally {
      setLoading(false);
    }
  }, [repositoryName, assetId, componentId]);

  /**
   * Fetch tags for the component.
   */
  const fetchTags = useCallback(async () => {
    if (!component?.id && !componentId) return;

    setTagsLoading(true);

    try {
      const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'readComponentTags', [{
        componentId: component?.id || componentId,
      }]);

      const result = ExtAPIUtils.checkForError(response);

      if (result?.success && result?.data) {
        const tagData = Array.isArray(result.data) ? result.data : [];
        setTags(tagData.map((t: { name?: string; id?: string }) => ({
          name: t.name || t.id || '',
        })));
      }
    } catch (err) {
      console.error('Error fetching tags:', err);
      // Don't set error - tags are optional
    } finally {
      setTagsLoading(false);
    }
  }, [component?.id, componentId]);

  /**
   * Add a tag to the component.
   */
  const addTag = useCallback(async (tagName: string) => {
    if (!component?.id && !componentId) {
      toast.error('Cannot add tag: No component associated');
      return;
    }

    try {
      const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'addTag', [{
        componentId: component?.id || componentId,
        tagName,
      }]);

      const result = ExtAPIUtils.checkForError(response);

      if (result?.success) {
        toast.success(`Tag "${tagName}" added`);
        // Refresh tags
        await fetchTags();
      } else {
        throw new Error(result?.message || 'Failed to add tag');
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to add tag';
      toast.error(message);
    }
  }, [component?.id, componentId, fetchTags, toast]);

  /**
   * Remove a tag from the component.
   */
  const removeTag = useCallback(async (tagName: string) => {
    if (!component?.id && !componentId) {
      toast.error('Cannot remove tag: No component associated');
      return;
    }

    try {
      const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'removeTag', [{
        componentId: component?.id || componentId,
        tagName,
      }]);

      const result = ExtAPIUtils.checkForError(response);

      if (result?.success) {
        toast.success(`Tag "${tagName}" removed`);
        // Refresh tags
        await fetchTags();
      } else {
        throw new Error(result?.message || 'Failed to remove tag');
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to remove tag';
      toast.error(message);
    }
  }, [component?.id, componentId, fetchTags, toast]);

  /**
   * Refetch all data.
   */
  const refetch = useCallback(async () => {
    await fetchAssetData();
  }, [fetchAssetData]);

  // Initial fetch
  useEffect(() => {
    fetchAssetData();
  }, [fetchAssetData]);

  // Fetch tags when component is loaded
  useEffect(() => {
    if (component?.id || componentId) {
      fetchTags();
    }
  }, [component?.id, componentId, fetchTags]);

  return {
    asset,
    component,
    tags,
    lifecycle,
    loading,
    tagsLoading,
    lifecycleLoading,
    error,
    refetch,
    addTag,
    removeTag,
  };
}

