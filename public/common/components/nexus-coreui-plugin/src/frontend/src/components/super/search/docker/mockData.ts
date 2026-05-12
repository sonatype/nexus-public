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
  DockerResult,
  DockerSearchResponse,
  DockerSuggestion,
  DockerSuggestResponse,
  DockerDetail,
  DockerTag,
} from './docker.types';

/**
 * Mock data for Docker Search development.
 * Remove this file when backend is ready.
 */

export const mockResults: DockerResult[] = [
  {
    id: 'docker:nginx',
    imageName: 'library/nginx',
    displayName: 'nginx',
    latestTag: '1.25.3',
    tagsCount: 147,
    size: '187 MB',
    lastUpdated: '2024-01-15T10:30:00Z',
    repository: 'docker-hosted',
  },
  {
    id: 'docker:ubuntu',
    imageName: 'library/ubuntu',
    displayName: 'ubuntu',
    latestTag: '24.04',
    tagsCount: 89,
    size: '77 MB',
    lastUpdated: '2024-02-01T14:22:00Z',
    repository: 'docker-hosted',
  },
  {
    id: 'docker:redis',
    imageName: 'library/redis',
    displayName: 'redis',
    latestTag: '7.2.4',
    tagsCount: 234,
    size: '138 MB',
    lastUpdated: '2024-01-20T09:15:00Z',
    repository: 'docker-hosted',
  },
  {
    id: 'docker:postgres',
    imageName: 'library/postgres',
    displayName: 'postgres',
    latestTag: '16.2',
    tagsCount: 312,
    size: '425 MB',
    lastUpdated: '2024-01-18T11:45:00Z',
    repository: 'docker-hosted',
  },
  {
    id: 'docker:node',
    imageName: 'library/node',
    displayName: 'node',
    latestTag: '21.6.0',
    tagsCount: 456,
    size: '1.1 GB',
    lastUpdated: '2024-02-05T16:30:00Z',
    repository: 'docker-proxy',
  },
  {
    id: 'docker:python',
    imageName: 'library/python',
    displayName: 'python',
    latestTag: '3.12.1',
    tagsCount: 389,
    size: '1.0 GB',
    lastUpdated: '2024-01-25T08:00:00Z',
    repository: 'docker-proxy',
  },
  {
    id: 'docker:alpine',
    imageName: 'library/alpine',
    displayName: 'alpine',
    latestTag: '3.19.1',
    tagsCount: 78,
    size: '7.8 MB',
    lastUpdated: '2024-01-30T12:15:00Z',
    repository: 'docker-hosted',
  },
  {
    id: 'docker:mysql',
    imageName: 'library/mysql',
    displayName: 'mysql',
    latestTag: '8.3.0',
    tagsCount: 267,
    size: '578 MB',
    lastUpdated: '2024-01-22T15:20:00Z',
    repository: 'docker-hosted',
  },
  {
    id: 'docker:mongo',
    imageName: 'library/mongo',
    displayName: 'mongo',
    latestTag: '7.0.5',
    tagsCount: 198,
    size: '743 MB',
    lastUpdated: '2024-01-28T10:00:00Z',
    repository: 'docker-proxy',
  },
  {
    id: 'docker:openjdk',
    imageName: 'library/openjdk',
    displayName: 'openjdk',
    latestTag: '21-slim',
    tagsCount: 156,
    size: '398 MB',
    lastUpdated: '2024-01-10T14:45:00Z',
    repository: 'docker-hosted',
  },
];

export const mockSuggestions: DockerSuggestion[] = [
  {
    id: 'docker:nginx',
    displayText: 'nginx',
    highlights: [[0, 5]],
  },
  {
    id: 'docker:node',
    displayText: 'node',
    highlights: [[0, 4]],
  },
  {
    id: 'docker:postgres',
    displayText: 'postgres',
    highlights: [[0, 8]],
  },
];

export const mockTags: DockerTag[] = [
  {
    name: 'latest',
    digest: 'sha256:abc123def456...',
    sizeBytes: 196083712,
    size: '187 MB',
    pushedAt: '2024-01-15T10:30:00Z',
    os: 'linux',
    architecture: 'amd64',
  },
  {
    name: '1.25.3',
    digest: 'sha256:abc123def456...',
    sizeBytes: 196083712,
    size: '187 MB',
    pushedAt: '2024-01-15T10:30:00Z',
    os: 'linux',
    architecture: 'amd64',
  },
  {
    name: '1.25.3-alpine',
    digest: 'sha256:bcd234efg567...',
    sizeBytes: 45287424,
    size: '43 MB',
    pushedAt: '2024-01-15T09:00:00Z',
    os: 'linux',
    architecture: 'amd64',
  },
  {
    name: '1.25.2',
    digest: 'sha256:cde345fgh678...',
    sizeBytes: 195035136,
    size: '186 MB',
    pushedAt: '2024-01-10T08:30:00Z',
    os: 'linux',
    architecture: 'amd64',
  },
  {
    name: '1.24-alpine',
    digest: 'sha256:def456ghi789...',
    sizeBytes: 44040192,
    size: '42 MB',
    pushedAt: '2023-12-20T14:15:00Z',
    os: 'linux',
    architecture: 'amd64',
  },
];

export const mockDetail: DockerDetail = {
  id: 'docker:nginx',
  imageName: 'library/nginx',
  displayName: 'nginx',
  description: 'Official build of Nginx.',
  tags: mockTags,
  tagsCount: 147,
  repository: 'docker-hosted',
  lastUpdated: '2024-01-15T10:30:00Z',
};

/**
 * Simulates a search API call with mock data.
 * Filters results based on query, imageName, tag, and digest.
 */
export async function mockSearchApi(params: {
  query?: string;
  imageName?: string;
  tag?: string;
  digest?: string;
  repository?: string;
  continuationToken?: string;
  limit?: number;
}): Promise<DockerSearchResponse> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockResults];

  // Filter by query (searches displayName and imageName)
  if (params.query) {
    const q = params.query.toLowerCase();
    filtered = filtered.filter(
      (r) =>
        r.displayName.toLowerCase().includes(q) ||
        r.imageName.toLowerCase().includes(q)
    );
  }

  // Filter by imageName
  if (params.imageName) {
    const name = params.imageName.toLowerCase();
    filtered = filtered.filter(
      (r) =>
        r.imageName.toLowerCase().includes(name) ||
        r.displayName.toLowerCase().includes(name)
    );
  }

  // Filter by tag
  if (params.tag) {
    const t = params.tag.toLowerCase();
    filtered = filtered.filter((r) =>
      r.latestTag.toLowerCase().includes(t)
    );
  }

  // Filter by repository
  if (params.repository) {
    const repo = params.repository.toLowerCase();
    filtered = filtered.filter((r) =>
      r.repository?.toLowerCase().includes(repo)
    );
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

/**
 * Simulates suggest API call with mock data.
 */
export async function mockSuggestApi(params: {
  query: string;
  limit?: number;
}): Promise<DockerSuggestResponse> {
  await new Promise((resolve) => setTimeout(resolve, 100));

  if (params.query.length < 2) {
    return { suggestions: [] };
  }

  const q = params.query.toLowerCase();
  const limit = params.limit ?? 10;

  const filtered = mockResults
    .filter(
      (r) =>
        r.displayName.toLowerCase().includes(q) ||
        r.imageName.toLowerCase().includes(q)
    )
    .slice(0, limit)
    .map((r) => ({
      id: r.id,
      displayText: r.displayName,
      highlights: [] as [number, number][],
    }));

  return { suggestions: filtered };
}

/**
 * Simulates detail API call with mock data.
 */
export async function mockDetailApi(id: string): Promise<DockerDetail | null> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  const result = mockResults.find((r) => r.id === id);
  if (!result) {
    return null;
  }

  return {
    id: result.id,
    imageName: result.imageName,
    displayName: result.displayName,
    description: `Official ${result.displayName} image.`,
    tags: mockTags,
    tagsCount: result.tagsCount,
    repository: result.repository ?? 'docker-hosted',
    lastUpdated: result.lastUpdated,
  };
}


