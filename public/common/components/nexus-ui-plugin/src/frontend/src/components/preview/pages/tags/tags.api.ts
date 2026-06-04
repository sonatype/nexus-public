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
 * Tags API
 *
 * Migration Status (AgentDev3):
 * - fetchTags: ✅ REST (GET /v1/tags)
 * - fetchFilteredTags: ✅ REST (GET /internal/ui/tags/filtered)
 * - fetchTagDetail: ✅ REST (GET /v1/tags/{name})
 *
 * All methods migrated to REST API using restClient.
 */

import { restClient, parseApiError, urlBuilder } from '../../../../interface/api';

import type {
  Tag,
  TagDetail,
  TagPageResponse,
  TagsFilters,
  TagSortField,
  SortDirection,
} from './tags.types';

/**
 * Fetch all tags from the server using REST API.
 *
 * Uses REST API: GET /service/rest/v1/tags
 *
 * @returns Promise resolving to array of tags
 */
export async function fetchTags(): Promise<Tag[]> {
  try {
    const url = urlBuilder.tags.list();
    const response = await restClient.get<Tag[]>(url);
    return Array.isArray(response) ? response : [];
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    console.error('Failed to fetch tags:', err);
    throw new Error(apiError.message || 'Failed to load tags');
  }
}

/**
 * Fetch filtered tags with component counts using REST API.
 *
 * Uses REST API: GET /service/rest/internal/ui/tags/filtered
 *
 * @param filters - Filter criteria
 * @param sortField - Field to sort by
 * @param sortDirection - Sort direction
 * @param page - Page number (0-indexed)
 * @param pageSize - Items per page
 * @returns Promise resolving to paginated tag response
 */
export async function fetchFilteredTags(
  filters: TagsFilters,
  sortField: TagSortField,
  sortDirection: SortDirection,
  page: number,
  pageSize: number
): Promise<TagPageResponse> {
  try {
    const params: Record<string, string> = {
      sortField,
      sortDirection,
      page: String(page),
      pageSize: String(pageSize),
    };

    if (filters.nameFilter) {
      params.nameFilter = filters.nameFilter;
    }

    if (filters.componentCounts.length > 0) {
      params.componentCounts = filters.componentCounts.join(',');
    }

    if (filters.activityDays.length > 0) {
      params.activityDays = filters.activityDays.join(',');
    }

    const baseUrl = urlBuilder.tags.filtered();
    const queryString = new URLSearchParams(params).toString();
    const url = `${baseUrl}?${queryString}`;

    const response = await restClient.get<TagPageResponse>(url);
    return response;
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    console.error('Failed to fetch filtered tags:', err);
    throw new Error(apiError.message || 'Failed to load filtered tags');
  }
}

/**
 * Fetch details for a specific tag using REST API.
 *
 * Uses REST API: GET /service/rest/v1/tags/{tagName}
 *
 * @param tagName - Name of the tag to fetch
 * @returns Promise resolving to tag details
 */
export async function fetchTagDetail(tagName: string): Promise<TagDetail> {
  try {
    const url = urlBuilder.tags.get(tagName);
    const response = await restClient.get<TagDetail>(url);
    return response;
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    console.error('Failed to fetch tag detail:', err);
    throw new Error(apiError.message || 'Failed to load tag details');
  }
}

/**
 * Create a new tag using REST API.
 *
 * Uses REST API: POST /service/rest/v1/tags
 *
 * @param name - Name for the new tag
 * @param attributes - Optional key-value attributes
 * @returns Promise resolving to the created tag
 */
export async function createTag(
  name: string,
  attributes: Record<string, string> = {}
): Promise<TagDetail> {
  try {
    const url = urlBuilder.tags.list();
    const response = await restClient.post<TagDetail>(url, { name, attributes });
    return response;
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    console.error('Failed to create tag:', err);
    throw new Error(apiError.message || 'Failed to create tag');
  }
}

/**
 * Delete a tag using REST API.
 *
 * Uses REST API: DELETE /service/rest/v1/tags/{tagName}
 *
 * @param tagName - Name of the tag to delete
 * @returns Promise resolving when tag is deleted
 */
export async function deleteTag(tagName: string): Promise<void> {
  try {
    const url = urlBuilder.tags.delete(tagName);
    await restClient.delete(url);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    console.error('Failed to delete tag:', err);
    throw new Error(apiError.message || 'Failed to delete tag');
  }
}
