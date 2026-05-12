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
 * Browse Module - API Layer
 *
 * MIGRATION STATUS:
 * - REST: ALL methods now use REST API
 *   - fetchRepositories, fetchComponent, fetchAsset, deleteComponent, deleteAsset
 *   - fetchBrowseNodes (NEW: GET /v1/repositories/{name}/browse)
 *   - deleteFolder (NEW: DELETE /v1/repositories/{name}/browse)
 */

import { restClient, parseApiError, ENDPOINTS, urlBuilder, encodeRepositoryItemId } from '@/utils/api';
import { isMockMode } from '@/config/previewFeatureFlags';
import { getMockAsset, getMockComponent } from './mockData';
import type {
  BrowseNode,
  RepositoryReference,
  ComponentXO,
  AssetXO,
  TreeLoadParams,
} from './browse.types';

// =============================================================================
// REPOSITORY API (REST)
// =============================================================================

/**
 * REST API repository shape
 */
interface RestRepository {
  name: string;
  format: string;
  type: string;
  url?: string;
  online?: boolean;
}

/**
 * Fetch all browseable repositories using REST API.
 */
export async function fetchRepositories(): Promise<RepositoryReference[]> {
  try {
    const repos = await restClient.get<RestRepository[]>(ENDPOINTS.REPOSITORIES);
    // Map to reference format
    return repos.map((r) => ({
      id: r.name,
      name: r.name,
      format: r.format,
      type: r.type,
    }));
  } catch (err: unknown) {
    console.error('Failed to fetch repositories:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

// =============================================================================
// BROWSE TREE API (REST)
// =============================================================================

/**
 * REST API browse node response shape
 */
interface RestBrowseNode {
  id: string;
  text: string;
  type: 'folder' | 'component' | 'asset';
  leaf: boolean;
  componentId?: string | null;
  assetId?: string | null;
  packageUrl?: string | null;
}

/**
 * Convert REST browse node to BrowseNode shape
 */
function restToBrowseNode(rest: RestBrowseNode): BrowseNode {
  return {
    id: rest.id,
    text: rest.text,
    type: rest.type,
    leaf: rest.leaf,
    componentId: rest.componentId || undefined,
    assetId: rest.assetId || undefined,
    packageUrl: rest.packageUrl || undefined,
  };
}

/**
 * Fetch children nodes for a path in a repository using REST API.
 *
 * @param params - Repository name and node path
 * @returns Array of child nodes
 */
export async function fetchBrowseNodes(params: TreeLoadParams): Promise<BrowseNode[]> {
  try {
    const { repositoryName, node } = params;
    const url = ENDPOINTS.REPOSITORY_BROWSE(repositoryName);
    const queryUrl = urlBuilder.query(url, { path: node || '/' });
    const nodes = await restClient.get<RestBrowseNode[]>(queryUrl);
    return Array.isArray(nodes) ? nodes.map(restToBrowseNode) : [];
  } catch (err: unknown) {
    console.error('Failed to fetch browse nodes:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

// =============================================================================
// COMPONENT API (REST)
// =============================================================================

/**
 * REST API component shape
 */
interface RestComponent {
  id: string;
  repository: string;
  format: string;
  group?: string;
  name: string;
  version?: string;
  assets?: RestAsset[];
}

/**
 * REST API asset shape
 */
interface RestAsset {
  id: string;
  repository: string;
  format: string;
  path: string;
  downloadUrl?: string;
  checksum?: {
    sha1?: string;
    sha256?: string;
    sha512?: string;
    md5?: string;
  };
  contentType?: string;
  lastModified?: string;
  blobCreated?: string;
  lastDownloaded?: string;
  fileSize?: number;
}

/**
 * Convert REST component to ComponentXO format
 */
function restToComponentXO(rest: RestComponent): ComponentXO {
  return {
    id: rest.id,
    repositoryName: rest.repository,
    format: rest.format,
    group: rest.group || '',
    name: rest.name,
    version: rest.version || '',
    assets: rest.assets?.map(restToAssetXO) || [],
  };
}

/**
 * Convert REST asset to AssetXO format
 */
function restToAssetXO(rest: RestAsset): AssetXO {
  return {
    id: rest.id,
    repositoryName: rest.repository,
    format: rest.format,
    name: rest.path,
    path: rest.path,
    downloadUrl: rest.downloadUrl,
    checksum: rest.checksum,
    contentType: rest.contentType,
    lastModified: rest.lastModified,
    blobCreated: rest.blobCreated,
    lastDownloaded: rest.lastDownloaded,
    fileSize: rest.fileSize,
  };
}

/**
 * Fetch component details using REST API.
 *
 * @param componentId - Raw component ID from browse tree
 * @param repositoryName - Repository name (required to encode ID for REST API)
 * @returns Component data
 */
export async function fetchComponent(componentId: string, repositoryName: string): Promise<ComponentXO> {
  try {
    if (isMockMode()) {
      const mock = getMockComponent(componentId, repositoryName);
      if (mock) return mock;
    }
    // REST API expects base64(repositoryName:rawId) format
    const encodedId = encodeRepositoryItemId(repositoryName, componentId);
    const url = urlBuilder.components.get(encodedId);
    const rest = await restClient.get<RestComponent>(url);
    return restToComponentXO(rest);
  } catch (err: unknown) {
    console.error('Failed to fetch component:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

/**
 * Fetch asset details using REST API.
 *
 * @param assetId - Raw asset ID from browse tree
 * @param repositoryName - Repository name (required to encode ID for REST API)
 * @returns Asset data
 */
export async function fetchAsset(assetId: string, repositoryName: string): Promise<AssetXO> {
  try {
    if (isMockMode()) {
      const mock = getMockAsset(assetId, repositoryName);
      if (mock) return mock;
    }
    // REST API expects base64(repositoryName:rawId) format
    const encodedId = encodeRepositoryItemId(repositoryName, assetId);
    const url = urlBuilder.assets.get(encodedId);
    const rest = await restClient.get<RestAsset>(url);
    return restToAssetXO(rest);
  } catch (err: unknown) {
    console.error('Failed to fetch asset:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

// =============================================================================
// DELETE API (REST for component/asset, ExtDirect for folder)
// =============================================================================

/**
 * Delete a component using REST API.
 *
 * @param componentData - Component to delete (must include repositoryName)
 * @returns Array of deleted node IDs (returns [componentId] for REST)
 */
export async function deleteComponent(componentData: ComponentXO): Promise<string[]> {
  try {
    // REST API expects base64(repositoryName:rawId) format
    const encodedId = encodeRepositoryItemId(componentData.repositoryName, componentData.id);
    const url = urlBuilder.components.delete(encodedId);
    await restClient.delete(url);
    return [componentData.id];
  } catch (err: unknown) {
    console.error('Failed to delete component:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

/**
 * Delete an asset using REST API.
 *
 * @param assetId - Raw asset ID from browse tree
 * @param repositoryName - Repository name (required to encode ID for REST API)
 */
export async function deleteAsset(
  assetId: string,
  repositoryName: string,
  alreadyEncoded = false
): Promise<void> {
  try {
    const encodedId = alreadyEncoded ? assetId : encodeRepositoryItemId(repositoryName, assetId);
    const url = urlBuilder.assets.delete(encodedId);
    await restClient.delete(url);
  } catch (err: unknown) {
    console.error('Failed to delete asset:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

/**
 * Delete a folder and all its contents using REST API.
 *
 * @param path - Folder path (URL-encoded)
 * @param repositoryName - Repository name
 */
export async function deleteFolder(path: string, repositoryName: string): Promise<void> {
  try {
    const url = ENDPOINTS.REPOSITORY_BROWSE(repositoryName);
    const queryUrl = urlBuilder.query(url, { path });
    await restClient.delete(queryUrl);
  } catch (err: unknown) {
    console.error('Failed to delete folder:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

// =============================================================================
// IN-REPOSITORY SEARCH API (REST)
// =============================================================================

/**
 * Search result item from the search API
 */
export interface SearchResultItem {
  id: string;
  repository: string;
  format: string;
  group?: string;
  name: string;
  version?: string;
  path?: string;
}

/**
 * Search response from the API
 */
interface SearchResponse {
  items: SearchResultItem[];
  continuationToken?: string;
}

/**
 * Search within a specific repository.
 *
 * @param repositoryName - Repository to search within
 * @param query - Search query string
 * @param limit - Maximum results to return (default 20)
 * @returns Array of search results
 */
export async function searchInRepository(
  repositoryName: string,
  query: string,
  limit: number = 20
): Promise<SearchResultItem[]> {
  try {
    const url = urlBuilder.query(ENDPOINTS.SEARCH, {
      repository: repositoryName,
      q: query,
      limit,
    });
    const response = await restClient.get<SearchResponse>(url);
    return response.items || [];
  } catch (err: unknown) {
    console.error('Failed to search repository:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Check if user has permission to delete components/assets.
 */
export function canDelete(): boolean {
  // @ts-expect-error - ExtJS is globally available
  return window.NX?.Permissions?.check('nexus:component:delete') ?? false;
}

/**
 * Show a success toast message.
 */
export function showSuccessMessage(message: string): void {
  // Use Radix toast if in Preview UI mode and provider is available
  const isPreviewUI = typeof window !== 'undefined' && window.location.hash.includes('#preview/');
  const nexusToast = typeof window !== 'undefined' ? (window as any).__nexusToast : null;

  if (isPreviewUI && nexusToast) {
    nexusToast.success(message);
  } else {
    // @ts-expect-error - ExtJS is globally available
    window.NX?.Messages?.success(message);
  }
}

/**
 * Show an error toast message.
 */
export function showErrorMessage(message: string): void {
  // Use Radix toast if in Preview UI mode and provider is available
  const isPreviewUI = typeof window !== 'undefined' && window.location.hash.includes('#preview/');
  const nexusToast = typeof window !== 'undefined' ? (window as any).__nexusToast : null;

  if (isPreviewUI && nexusToast) {
    nexusToast.error(message);
  } else {
    // @ts-expect-error - ExtJS is globally available
    window.NX?.Messages?.error(message);
  }
}
