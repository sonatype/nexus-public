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

import type { GAResult, GASearchResponse, GASuggestion } from '../core';

/**
 * Mock data for GA Search development.
 * Remove this file when backend is ready.
 */

export const mockResults: GAResult[] = [
  {
    gaId: 'maven:org.apache.commons:commons-lang3',
    format: 'maven',
    displayName: 'commons-lang3',
    namespace: 'org.apache.commons',
    latestVersion: '3.14.0',
    versionsCount: 47,
    repositoriesCount: 2,
    license: 'Apache-2.0',
    lastUpdated: '2024-01-15T10:30:00Z',
  },
  {
    gaId: 'maven:com.google.guava:guava',
    format: 'maven',
    displayName: 'guava',
    namespace: 'com.google.guava',
    latestVersion: '33.0.0-jre',
    versionsCount: 156,
    repositoriesCount: 3,
    license: 'Apache-2.0',
    lastUpdated: '2024-02-01T14:22:00Z',
  },
  {
    gaId: 'maven:org.springframework:spring-core',
    format: 'maven',
    displayName: 'spring-core',
    namespace: 'org.springframework',
    latestVersion: '6.1.3',
    versionsCount: 145,
    repositoriesCount: 2,
    license: 'Apache-2.0',
    lastUpdated: '2024-01-20T09:15:00Z',
  },
  {
    gaId: 'maven:org.springframework:spring-context',
    format: 'maven',
    displayName: 'spring-context',
    namespace: 'org.springframework',
    latestVersion: '6.1.3',
    versionsCount: 142,
    repositoriesCount: 2,
    license: 'Apache-2.0',
    lastUpdated: '2024-01-20T09:15:00Z',
  },
  {
    gaId: 'maven:org.springframework.boot:spring-boot',
    format: 'maven',
    displayName: 'spring-boot',
    namespace: 'org.springframework.boot',
    latestVersion: '3.2.2',
    versionsCount: 89,
    repositoriesCount: 2,
    license: 'Apache-2.0',
    lastUpdated: '2024-02-05T11:00:00Z',
  },
  {
    gaId: 'maven:com.fasterxml.jackson.core:jackson-databind',
    format: 'maven',
    displayName: 'jackson-databind',
    namespace: 'com.fasterxml.jackson.core',
    latestVersion: '2.16.1',
    versionsCount: 167,
    repositoriesCount: 3,
    license: 'Apache-2.0',
    lastUpdated: '2024-01-25T16:45:00Z',
  },
  {
    gaId: 'maven:org.slf4j:slf4j-api',
    format: 'maven',
    displayName: 'slf4j-api',
    namespace: 'org.slf4j',
    latestVersion: '2.0.11',
    versionsCount: 78,
    repositoriesCount: 2,
    license: 'MIT',
    lastUpdated: '2024-01-10T08:30:00Z',
  },
  {
    gaId: 'maven:ch.qos.logback:logback-classic',
    format: 'maven',
    displayName: 'logback-classic',
    namespace: 'ch.qos.logback',
    latestVersion: '1.4.14',
    versionsCount: 56,
    repositoriesCount: 2,
    license: 'EPL-1.0',
    lastUpdated: '2023-12-20T10:00:00Z',
  },
  {
    gaId: 'maven:org.projectlombok:lombok',
    format: 'maven',
    displayName: 'lombok',
    namespace: 'org.projectlombok',
    latestVersion: '1.18.30',
    versionsCount: 34,
    repositoriesCount: 1,
    license: 'MIT',
    lastUpdated: '2023-11-15T14:30:00Z',
  },
  {
    gaId: 'maven:junit:junit',
    format: 'maven',
    displayName: 'junit',
    namespace: 'junit',
    latestVersion: '4.13.2',
    versionsCount: 23,
    repositoriesCount: 2,
    license: 'EPL-1.0',
    lastUpdated: '2021-02-13T12:00:00Z',
  },
];

export const mockSuggestions: GASuggestion[] = [
  {
    gaId: 'maven:org.apache.commons:commons-lang3',
    displayText: 'org.apache.commons:commons-lang3',
    highlights: [[19, 32]],
  },
  {
    gaId: 'maven:com.google.guava:guava',
    displayText: 'com.google.guava:guava',
    highlights: [[17, 22]],
  },
  {
    gaId: 'maven:org.springframework:spring-core',
    displayText: 'org.springframework:spring-core',
    highlights: [[20, 31]],
  },
];

/**
 * Simulates a search API call with mock data.
 * Filters results based on query, groupId, and artifactId.
 */
export async function mockSearchApi(params: {
  query?: string;
  groupId?: string;
  artifactId?: string;
  repository?: string;
  continuationToken?: string;
  limit?: number;
}): Promise<GASearchResponse> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockResults];

  // Filter by query (searches displayName and namespace)
  if (params.query) {
    const q = params.query.toLowerCase();
    filtered = filtered.filter(
      (r) =>
        r.displayName.toLowerCase().includes(q) ||
        r.namespace.toLowerCase().includes(q)
    );
  }

  // Filter by groupId
  if (params.groupId) {
    const g = params.groupId.toLowerCase();
    filtered = filtered.filter((r) =>
      r.namespace.toLowerCase().includes(g)
    );
  }

  // Filter by artifactId
  if (params.artifactId) {
    const a = params.artifactId.toLowerCase();
    filtered = filtered.filter((r) =>
      r.displayName.toLowerCase().includes(a)
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
}): Promise<{ suggestions: GASuggestion[] }> {
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
        r.namespace.toLowerCase().includes(q)
    )
    .slice(0, limit)
    .map((r) => ({
      gaId: r.gaId,
      displayText: `${r.namespace}:${r.displayName}`,
      highlights: [] as [number, number][],
    }));

  return { suggestions: filtered };
}

