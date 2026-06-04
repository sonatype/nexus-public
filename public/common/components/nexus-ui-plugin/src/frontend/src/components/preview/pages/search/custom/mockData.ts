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

import type {
  CustomFilter,
  CustomSearchResult,
  CustomSearchResponse,
} from './custom.types';

/**
 * Mock data for Custom Search development.
 * Remove this file when backend is ready.
 */

export const mockResults: CustomSearchResult[] = [
  {
    id: 'maven-1',
    repository: 'maven-central',
    format: 'maven2',
    group: 'org.apache.commons',
    name: 'commons-lang3',
    version: '3.14.0',
    lastModified: '2024-01-15T10:30:00Z',
  },
  {
    id: 'maven-2',
    repository: 'maven-central',
    format: 'maven2',
    group: 'com.google.guava',
    name: 'guava',
    version: '33.0.0-jre',
    lastModified: '2024-02-01T14:22:00Z',
  },
  {
    id: 'npm-1',
    repository: 'npm-proxy',
    format: 'npm',
    group: '@angular',
    name: 'core',
    version: '17.1.0',
    lastModified: '2024-01-20T09:15:00Z',
    tags: ['latest', 'stable'],
  },
  {
    id: 'npm-2',
    repository: 'npm-proxy',
    format: 'npm',
    name: 'lodash',
    version: '4.17.21',
    lastModified: '2023-10-15T08:00:00Z',
    tags: ['latest'],
  },
  {
    id: 'docker-1',
    repository: 'docker-hosted',
    format: 'docker',
    name: 'nginx',
    version: 'latest',
    lastModified: '2024-02-10T16:45:00Z',
    tags: ['latest', '1.25.3'],
  },
  {
    id: 'docker-2',
    repository: 'docker-hosted',
    format: 'docker',
    name: 'redis',
    version: '7.2.4',
    lastModified: '2024-02-05T11:00:00Z',
    tags: ['7.2.4', '7', 'latest'],
  },
  {
    id: 'nuget-1',
    repository: 'nuget-proxy',
    format: 'nuget',
    name: 'Newtonsoft.Json',
    version: '13.0.3',
    lastModified: '2023-06-30T12:00:00Z',
  },
  {
    id: 'maven-3',
    repository: 'maven-central',
    format: 'maven2',
    group: 'org.springframework',
    name: 'spring-core',
    version: '6.1.3',
    lastModified: '2024-01-25T10:00:00Z',
  },
  {
    id: 'maven-4',
    repository: 'maven-releases',
    format: 'maven2',
    group: 'com.mycompany',
    name: 'my-library',
    version: '2.0.0',
    lastModified: '2024-02-08T09:30:00Z',
  },
  {
    id: 'npm-3',
    repository: 'npm-proxy',
    format: 'npm',
    group: '@types',
    name: 'react',
    version: '18.2.48',
    lastModified: '2024-01-18T14:00:00Z',
    tags: ['latest'],
  },
  {
    id: 'docker-3',
    repository: 'docker-proxy',
    format: 'docker',
    name: 'postgres',
    version: '16.1',
    lastModified: '2024-01-10T08:00:00Z',
    tags: ['16.1', '16', 'latest'],
  },
  {
    id: 'maven-5',
    repository: 'maven-central',
    format: 'maven2',
    group: 'org.slf4j',
    name: 'slf4j-api',
    version: '2.0.11',
    lastModified: '2024-01-10T08:30:00Z',
  },
];

/**
 * Apply a single filter to a result.
 */
function matchesFilter(result: CustomSearchResult, filter: CustomFilter): boolean {
  const value = filter.value.toLowerCase();

  let fieldValue: string | undefined;

  switch (filter.field) {
    case 'format':
      fieldValue = result.format;
      break;
    case 'repository':
      fieldValue = result.repository;
      break;
    case 'group':
      fieldValue = result.group;
      break;
    case 'name':
      fieldValue = result.name;
      break;
    case 'version':
      fieldValue = result.version;
      break;
    case 'tag':
      // Special handling for tags - check if any tag matches
      if (!result.tags) return false;
      return result.tags.some((tag) => {
        const tagLower = tag.toLowerCase();
        switch (filter.operator) {
          case 'equals':
            return tagLower === value;
          case 'contains':
            return tagLower.includes(value);
          case 'startsWith':
            return tagLower.startsWith(value);
          case 'endsWith':
            return tagLower.endsWith(value);
          default:
            return false;
        }
      });
    case 'keyword':
      // Search across multiple fields
      const searchText = [
        result.format,
        result.repository,
        result.group,
        result.name,
        result.version,
        ...(result.tags || []),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      switch (filter.operator) {
        case 'equals':
          return searchText.includes(value);
        case 'contains':
          return searchText.includes(value);
        case 'startsWith':
          return searchText.includes(value);
        case 'endsWith':
          return searchText.includes(value);
        default:
          return false;
      }
    default:
      return false;
  }

  if (!fieldValue) return false;
  fieldValue = fieldValue.toLowerCase();

  switch (filter.operator) {
    case 'equals':
      return fieldValue === value;
    case 'contains':
      return fieldValue.includes(value);
    case 'startsWith':
      return fieldValue.startsWith(value);
    case 'endsWith':
      return fieldValue.endsWith(value);
    default:
      return false;
  }
}

/**
 * Simulates a custom search API call with mock data.
 * Filters results based on dynamic filter criteria.
 */
export async function mockCustomSearchApi(params: {
  filters: readonly CustomFilter[];
  continuationToken?: string;
  limit?: number;
}): Promise<CustomSearchResponse> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockResults];

  // Apply all filters (AND logic)
  if (params.filters.length > 0) {
    for (const filter of params.filters) {
      if (filter.value.trim()) {
        filtered = filtered.filter((result) => matchesFilter(result, filter));
      }
    }
  }

  // Pagination simulation
  const limit = params.limit ?? 50;
  const startIndex = params.continuationToken
    ? parseInt(params.continuationToken, 10)
    : 0;
  const endIndex = startIndex + limit;
  const paginatedResults = filtered.slice(startIndex, endIndex);
  const hasMore = endIndex < filtered.length;

  return {
    items: paginatedResults,
    totalCount: filtered.length,
    continuationToken: hasMore ? String(endIndex) : undefined,
  };
}


