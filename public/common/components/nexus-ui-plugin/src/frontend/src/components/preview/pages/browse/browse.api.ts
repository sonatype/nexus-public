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
 * - REST: most methods use REST API
 *   - fetchRepositories, fetchComponent, deleteComponent, deleteAsset
 *   - fetchBrowseNodes (GET /v1/repositories/{name}/browse)
 *   - deleteFolder (DELETE /v1/repositories/{name}/browse)
 * - ExtDirect: fetchAsset uses coreui_Component.readAsset to receive the full asset
 *   attributes bag (firewall, content, format facets) — REST v1 strips everything
 *   except the format facet, so firewall data never reaches the Preview UI via REST.
 */

import { restClient, parseApiError, ENDPOINTS, urlBuilder, encodeRepositoryItemId } from '../../../../interface/api';
import ExtAPIUtils from '../../../../interface/ExtAPIUtils';
import { isMockMode } from '../../config/featureFlags';
import { getMockAsset, getMockComponent } from './mockData';
import type {
  BrowseNode,
  RepositoryReference,
  ComponentXO,
  AssetXO,
  TreeLoadParams,
} from './browse.types';

// =============================================================================
// TYPES
// =============================================================================

/**
 * Toast API injected by the Preview UI's ToastProvider.
 */
interface NexusToast {
  success(message: string): void;
  error(message: string): void;
}

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
  };
}

/**
 * Shape of AssetXO returned by ExtDirect coreui_Component.readAsset.
 * `name` holds the asset path; `attributes` is the full backing bag including
 * the `checksum` map and every facet (firewall, content, format-specific).
 */
interface ExtDirectAssetXO {
  id: string;
  name: string;
  format: string;
  repositoryName: string;
  contentType?: string;
  size?: number;
  blobCreated?: string | number | null;
  blobUpdated?: string | number | null;
  lastDownloaded?: string | number | null;
  blobRef?: string | null;
  componentId?: string | null;
  createdBy?: string | null;
  createdByIp?: string | null;
  attributes?: Record<string, unknown>;
  registryUrl?: string | null;
}

/**
 * Convert ExtDirect AssetXO to the UI's AssetXO shape.
 * Pulls `checksum` out of the attributes bag to a top-level field so existing
 * components (which render checksum as a separate section) keep working.
 */
function extDirectToAssetXO(ext: ExtDirectAssetXO, repositoryName: string): AssetXO {
  const attributes = ext.attributes ?? {};
  const checksum =
    typeof attributes.checksum === 'object' && attributes.checksum !== null
      ? (attributes.checksum as Record<string, string>)
      : undefined;

  // Construct download URL with proper encoding for special characters
  // Note: repositoryName should be URL-safe (no encoding needed for typical names)
  // but path segments may contain spaces, unicode, or special chars
  const path = ext.name;
  const pathSegments = path.replace(/^\//, '').split('/').map(encodeURIComponent);
  const downloadUrl = `/repository/${encodeURIComponent(repositoryName)}/${pathSegments.join('/')}`;

  // Convert timestamp to ISO string.
  // ExtDirect returns timestamps as either:
  // - ISO string (e.g., "2026-06-11T10:30:00.000Z") - returned as-is
  // - Unix timestamp in milliseconds (e.g., 1749649800000) - converted to ISO
  // Note: If timestamps appear as dates in 1970, the backend is sending seconds
  // instead of milliseconds - this would require multiplying by 1000.
  const toIso = (v: string | number | null | undefined): string | undefined =>
    v == null ? undefined : typeof v === 'number' ? new Date(v).toISOString() : v;

  return {
    id: ext.id,
    repositoryName: ext.repositoryName ?? repositoryName,
    format: ext.format,
    name: path,
    path,
    downloadUrl,
    checksum,
    contentType: ext.contentType,
    blobCreated: toIso(ext.blobCreated),
    blobUpdated: toIso(ext.blobUpdated),
    lastDownloaded: toIso(ext.lastDownloaded),
    size: ext.size,
    blobRef: ext.blobRef ?? undefined,
    componentId: ext.componentId ?? undefined,
    createdBy: ext.createdBy ?? undefined,
    createdByIp: ext.createdByIp ?? undefined,
    attributes,
    registryUrl: ext.registryUrl ?? undefined,
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
 * Fetch asset details via ExtDirect coreui_Component.readAsset.
 *
 * Uses ExtDirect (matching Classic UI) instead of REST v1 because the REST endpoint's
 * AssetXOBuilder.getExpandedAttributes only emits the format facet — firewall, content,
 * and other facet bags are stripped before the response is returned. ExtDirect returns
 * the full asset.attributes().backing() map, which the Attributes tab needs to render
 * Firewall and other dynamic facets at parity with Classic UI (NEXUS-52920).
 *
 * @param assetId - Raw asset ID from browse tree node
 * @param repositoryName - Repository name
 * @returns Asset data with full attributes bag
 */
export async function fetchAsset(assetId: string, repositoryName: string): Promise<AssetXO> {
  try {
    if (isMockMode()) {
      const mock = getMockAsset(assetId, repositoryName);
      if (mock) return mock;
    }
    const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'readAsset', {
      data: [assetId, repositoryName],
    });
    // checkForErrorAndExtract throws on { success: false } / exception, otherwise returns result.data
    const data = ExtAPIUtils.checkForErrorAndExtract(response) as ExtDirectAssetXO | undefined;
    if (!data) {
      throw new Error('Asset not found');
    }
    return extDirectToAssetXO(data, repositoryName);
  } catch (err: unknown) {
    console.error('Failed to fetch asset:', err);
    const apiError = parseApiError(err);
    throw new Error(apiError.message);
  }
}

// =============================================================================
// DELETE PERMISSION PREFLIGHT (ExtDirect)
// =============================================================================

/**
 * Preflight: can the current user delete this asset?
 *
 * Calls coreui_Component.canDeleteAsset — the same @DirectMethod Classic UI's
 * mixin/ComponentUtils.js uses. Server resolves against the concrete
 * repository/format/asset (including content-selector grants), so users with
 * specific-scope delete permissions are not falsely denied — unlike a
 * client-side ExtJS.checkPermission wildcard check, which returns false against
 * specific-scope grants (Shiro WildcardPermission asymmetry). See NEXUS-53861.
 *
 * Returns false on any error: hidden is the safe default.
 */
export async function canDeleteAsset(assetId: string, repositoryName: string): Promise<boolean> {
  try {
    const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'canDeleteAsset', {
      data: [assetId, repositoryName],
    });
    return ExtAPIUtils.checkForErrorAndExtract(response) === true;
  } catch (err: unknown) {
    console.error('Failed to check asset delete permission:', err);
    return false;
  }
}

/**
 * Preflight: can the current user delete this component?
 *
 * Server signature accepts a JSON-stringified ComponentXO (matches Classic UI's
 * payload from NX.direct.coreui_Component.canDeleteComponent).
 */
export async function canDeleteComponent(component: ComponentXO): Promise<boolean> {
  try {
    const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'canDeleteComponent', {
      data: [JSON.stringify(component)],
    });
    return ExtAPIUtils.checkForErrorAndExtract(response) === true;
  } catch (err: unknown) {
    console.error('Failed to check component delete permission:', err);
    return false;
  }
}

/**
 * Preflight: can the current user delete this folder?
 */
export async function canDeleteFolder(path: string, repositoryName: string): Promise<boolean> {
  try {
    const response = await ExtAPIUtils.extAPIRequest('coreui_Component', 'canDeleteFolder', {
      data: [path, repositoryName],
    });
    return ExtAPIUtils.checkForErrorAndExtract(response) === true;
  } catch (err: unknown) {
    console.error('Failed to check folder delete permission:', err);
    return false;
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
  const nexusToast = typeof window !== 'undefined' ? (window as { __nexusToast?: NexusToast }).__nexusToast : null;

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
  const nexusToast = typeof window !== 'undefined' ? (window as { __nexusToast?: NexusToast }).__nexusToast : null;

  if (isPreviewUI && nexusToast) {
    nexusToast.error(message);
  } else {
    // @ts-expect-error - ExtJS is globally available
    window.NX?.Messages?.error(message);
  }
}
