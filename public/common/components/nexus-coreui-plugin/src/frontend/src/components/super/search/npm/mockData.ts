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

import type { NpmResult, NpmSearchResponse, NpmDetail, NpmSearchFilters } from './npm.types';

/**
 * Mock npm package data for development.
 */
export const mockNpmResults: NpmResult[] = [
  {
    id: 'npm:@angular/core',
    scope: '@angular',
    name: 'core',
    displayName: '@angular/core',
    latestVersion: '17.1.0',
    versionsCount: 892,
    description: 'Angular - the core framework',
    author: 'angular',
    license: 'MIT',
    keywords: ['angular', 'framework', 'typescript'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'npm:@angular/common',
    scope: '@angular',
    name: 'common',
    displayName: '@angular/common',
    latestVersion: '17.1.0',
    versionsCount: 890,
    description: 'Angular - commonly needed directives and services',
    author: 'angular',
    license: 'MIT',
    keywords: ['angular', 'common'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'npm:react',
    scope: '',
    name: 'react',
    displayName: 'react',
    latestVersion: '18.2.0',
    versionsCount: 1245,
    description: 'React is a JavaScript library for building user interfaces.',
    author: 'facebook',
    license: 'MIT',
    keywords: ['react', 'ui', 'frontend'],
    repositoriesCount: 3,
    lastUpdated: '2024-01-15T14:22:00Z',
  },
  {
    id: 'npm:react-dom',
    scope: '',
    name: 'react-dom',
    displayName: 'react-dom',
    latestVersion: '18.2.0',
    versionsCount: 1230,
    description: 'React package for working with the DOM.',
    author: 'facebook',
    license: 'MIT',
    keywords: ['react', 'dom'],
    repositoriesCount: 3,
    lastUpdated: '2024-01-15T14:22:00Z',
  },
  {
    id: 'npm:@types/node',
    scope: '@types',
    name: 'node',
    displayName: '@types/node',
    latestVersion: '20.11.5',
    versionsCount: 567,
    description: 'TypeScript definitions for Node.js',
    author: 'DefinitelyTyped',
    license: 'MIT',
    keywords: ['typescript', 'types', 'node'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-18T09:15:00Z',
  },
  {
    id: 'npm:@types/react',
    scope: '@types',
    name: 'react',
    displayName: '@types/react',
    latestVersion: '18.2.48',
    versionsCount: 445,
    description: 'TypeScript definitions for React',
    author: 'DefinitelyTyped',
    license: 'MIT',
    keywords: ['typescript', 'types', 'react'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-19T11:00:00Z',
  },
  {
    id: 'npm:lodash',
    scope: '',
    name: 'lodash',
    displayName: 'lodash',
    latestVersion: '4.17.21',
    versionsCount: 234,
    description: 'Lodash modular utilities.',
    author: 'jdalton',
    license: 'MIT',
    keywords: ['modules', 'stdlib', 'util'],
    repositoriesCount: 2,
    lastUpdated: '2023-02-15T08:30:00Z',
  },
  {
    id: 'npm:axios',
    scope: '',
    name: 'axios',
    displayName: 'axios',
    latestVersion: '1.6.5',
    versionsCount: 178,
    description: 'Promise based HTTP client for the browser and node.js',
    author: 'axios',
    license: 'MIT',
    keywords: ['xhr', 'http', 'ajax', 'promise'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-10T16:45:00Z',
  },
  {
    id: 'npm:express',
    scope: '',
    name: 'express',
    displayName: 'express',
    latestVersion: '4.18.2',
    versionsCount: 289,
    description: 'Fast, unopinionated, minimalist web framework',
    author: 'dougwilson',
    license: 'MIT',
    keywords: ['express', 'framework', 'web', 'rest', 'api'],
    repositoriesCount: 2,
    lastUpdated: '2023-10-20T12:00:00Z',
  },
  {
    id: 'npm:typescript',
    scope: '',
    name: 'typescript',
    displayName: 'typescript',
    latestVersion: '5.3.3',
    versionsCount: 456,
    description: 'TypeScript is a language for application scale JavaScript development',
    author: 'Microsoft',
    license: 'Apache-2.0',
    keywords: ['typescript', 'language', 'javascript'],
    repositoriesCount: 3,
    lastUpdated: '2024-01-08T14:00:00Z',
  },
];

/**
 * Mock npm detail data.
 */
export const mockNpmDetail: NpmDetail = {
  id: 'npm:react',
  scope: '',
  name: 'react',
  displayName: 'react',
  description: 'React is a JavaScript library for building user interfaces.',
  author: 'facebook',
  license: 'MIT',
  homepage: 'https://react.dev/',
  repositoryUrl: 'https://github.com/facebook/react',
  keywords: ['react', 'ui', 'frontend', 'javascript'],
  versions: [
    { version: '18.2.0', tags: ['latest'], published: '2024-01-15T14:22:00Z', repository: 'npm-hosted' },
    { version: '18.1.0', tags: [], published: '2023-06-10T10:00:00Z', repository: 'npm-hosted' },
    { version: '18.0.0', tags: [], published: '2022-03-29T09:00:00Z', repository: 'npm-hosted' },
    { version: '17.0.2', tags: [], published: '2021-03-22T08:00:00Z', repository: 'npm-proxy' },
    { version: '19.0.0-beta.0', tags: ['next', 'beta'], published: '2024-01-20T12:00:00Z', repository: 'npm-hosted' },
  ],
  repositories: ['npm-hosted', 'npm-proxy', 'npm-group'],
};

/**
 * Simulates npm search API call with mock data.
 */
export async function mockNpmSearchApi(filters: NpmSearchFilters): Promise<NpmSearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockNpmResults];

  // Filter by scope
  if (filters.scope) {
    const s = filters.scope.toLowerCase().replace('@', '');
    filtered = filtered.filter((r) =>
      r.scope.toLowerCase().replace('@', '').includes(s)
    );
  }

  // Filter by name
  if (filters.name) {
    const n = filters.name.toLowerCase();
    filtered = filtered.filter((r) =>
      r.name.toLowerCase().includes(n) || r.displayName.toLowerCase().includes(n)
    );
  }

  // Filter by version (exact match)
  if (filters.version) {
    filtered = filtered.filter((r) => r.latestVersion === filters.version);
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates npm detail API call.
 */
export async function mockNpmDetailApi(id: string): Promise<NpmDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockNpmDetail;
}

