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
 * Tags API (Preview UI)
 *
 * REST-backed tag operations used by the XState machines and pages:
 * - fetchTagsFiltered: GET /service/rest/internal/ui/tags/filtered
 * - fetchTagDetail:    GET /service/rest/v1/tags/{name}
 * - createTag:         POST /service/rest/v1/tags
 */

import { restClient, parseApiError, urlBuilder } from '../../../../interface/api';

import type {
  TagDetail,
  TagsFilters,
  TagSortField,
  SortDirection,
} from './tags.types';

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
 * Parameters for fetching filtered tags.
 */
export interface FetchTagsFilteredParams {
  filters: TagsFilters;
  sortField: TagSortField;
  sortDirection: SortDirection;
  page: number;
  pageSize: number;
}

/**
 * Response from fetching filtered tags.
 */
export interface FetchTagsFilteredResponse {
  items: Array<{
    name: string;
    attributes: Record<string, unknown> | null;
    firstCreated: string | null;
    lastUpdated: string | null;
    componentCount: number;
  }>;
  totalCount: number;
}

/**
 * Fetch filtered tags with pagination (used by XState machine).
 *
 * Uses REST API: GET /service/rest/internal/ui/tags/filtered
 *
 * @param params - Query parameters for filtering
 * @returns Promise resolving to filtered tags and total count
 */
export async function fetchTagsFiltered(
  params: FetchTagsFilteredParams
): Promise<FetchTagsFilteredResponse> {
  const { filters, sortField, sortDirection, page, pageSize } = params;

  const searchParams = new URLSearchParams();

  if (filters.nameFilter) {
    searchParams.append('nameFilter', filters.nameFilter);
  }

  filters.componentCountRanges.forEach((range) => {
    searchParams.append('componentCountRanges', range);
  });

  filters.activityDays.forEach((days) => {
    searchParams.append('activityDays', String(days));
  });

  searchParams.append('sortField', sortField);
  searchParams.append('sortDirection', sortDirection);
  searchParams.append('page', String(page));
  searchParams.append('pageSize', String(pageSize));

  const data = await restClient.get<{
    items: FetchTagsFilteredResponse['items'];
    totalCount: number;
  }>(`/service/rest/internal/ui/tags/filtered?${searchParams.toString()}`);

  return {
    items: data.items,
    totalCount: data.totalCount,
  };
}
