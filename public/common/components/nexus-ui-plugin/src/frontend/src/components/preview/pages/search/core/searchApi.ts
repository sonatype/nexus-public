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

import Axios from 'axios';
import type { GAResult, GASearchResponse } from './search.types';

/**
 * Search API Response from existing /service/rest/v1/search endpoint.
 */
interface RawSearchResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

/**
 * Raw search item from the API.
 */
interface RawSearchItem {
  id: string;
  repository: string;
  format: string;
  group: string;
  name: string;
  version: string;
  assets: Array<{
    id: string;
    path: string;
    downloadUrl: string;
    checksum: {
      sha1?: string;
      sha256?: string;
      md5?: string;
    };
    contentType?: string;
    lastModified?: string;
  }>;
}

/**
 * Search parameters for the API.
 */
export interface SearchApiParams {
  format?: string;
  query?: string;
  groupId?: string;
  artifactId?: string;
  repository?: string;
  continuationToken?: string;
}

/**
 * Call the existing search API.
 */
async function fetchSearchResults(params: SearchApiParams): Promise<RawSearchResponse> {
  const queryParams = new URLSearchParams();
  
  if (params.format) {
    queryParams.set('format', params.format);
  }
  if (params.query) {
    queryParams.set('q', params.query);
  }
  if (params.groupId) {
    queryParams.set('maven.groupId', params.groupId);
  }
  if (params.artifactId) {
    queryParams.set('maven.artifactId', params.artifactId);
  }
  if (params.repository) {
    queryParams.set('repository', params.repository);
  }
  if (params.continuationToken) {
    queryParams.set('continuationToken', params.continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);
  return response.data;
}

/**
 * Aggregate raw search results by Group:Artifact (GA).
 * This is client-side aggregation since the existing API returns per-version results.
 */
function aggregateByGA(items: RawSearchItem[]): Map<string, GAResult> {
  const gaMap = new Map<string, GAResult>();

  for (const item of items) {
    const gaId = `maven:${item.group}:${item.name}`;
    
    const existing = gaMap.get(gaId);
    if (existing) {
      // Update existing GA entry
      existing.versionsCount += 1;
      if (!existing.repositories.includes(item.repository)) {
        existing.repositories.push(item.repository);
      }
      // Track latest version (simple string comparison for now)
      if (item.version > existing.latestVersion) {
        existing.latestVersion = item.version;
      }
    } else {
      // Create new GA entry
      // displayName = artifactId (name), namespace = groupId (group)
      gaMap.set(gaId, {
        id: gaId,
        gaId,
        format: 'maven',
        namespace: item.group,
        displayName: item.name,  // artifactId for display
        latestVersion: item.version,
        versionsCount: 1,
        repositoriesCount: 1,
        repositories: [item.repository],
        lastUpdated: new Date().toISOString(),
      });
    }
  }

  // Update repositoriesCount for all entries
  for (const ga of gaMap.values()) {
    ga.repositoriesCount = ga.repositories.length;
  }

  return gaMap;
}

/**
 * Search for Maven artifacts and aggregate by GA.
 * Uses the existing /service/rest/v1/search API.
 */
export async function searchMavenGA(params: SearchApiParams): Promise<GASearchResponse> {
  const response = await fetchSearchResults({
    ...params,
    format: 'maven2',
  });

  const gaMap = aggregateByGA(response.items);
  const results = Array.from(gaMap.values());

  // Sort by relevance (number of versions) descending
  results.sort((a, b) => b.versionsCount - a.versionsCount);

  return {
    items: results,
    totalCount: results.length,
    continuationToken: response.continuationToken,
  };
}

/**
 * Generic search function for any format.
 * Can be extended for npm, nuget, docker, etc.
 */
export async function searchComponents(
  format: string,
  params: SearchApiParams
): Promise<RawSearchResponse> {
  return fetchSearchResults({
    ...params,
    format,
  });
}

export default {
  searchMavenGA,
  searchComponents,
  fetchSearchResults,
};

