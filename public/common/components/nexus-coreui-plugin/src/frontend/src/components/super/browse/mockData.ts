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
 * Mock data for Browse/Preview UI development.
 *
 * Enable with ?mock in the URL (dev mode only). Remove ?mock before committing.
 * To remove entirely: delete this file and the isMockMode() branches in:
 * - useRepositoryListServer.ts
 * - useBrowseTree.ts (or fetchChildren wrapper)
 * - BrowsePage.tsx (allReposForFilters)
 * - search/unified/useUnifiedSearch.ts
 * - search/unified/useRepositories.ts
 */

import type {
  RepositoryReference,
  RepositoryPageResponse,
  ComponentXO,
  AssetXO,
} from './browse.types';
import type { BrowseNode } from './tree/browse-tree.types';

const baseUrl = 'http://localhost:8081/repository';

export const MOCK_REPOSITORIES: RepositoryReference[] = [
  { name: 'maven-releases', type: 'hosted', format: 'maven2', status: { online: true }, url: `${baseUrl}/maven-releases` },
  { name: 'maven-central', type: 'proxy', format: 'maven2', status: { online: true }, url: `${baseUrl}/maven-central` },
  { name: 'npm-hosted', type: 'hosted', format: 'npm', status: { online: true }, url: `${baseUrl}/npm-hosted` },
  { name: 'nuget-hosted', type: 'hosted', format: 'nuget', status: { online: true }, url: `${baseUrl}/nuget-hosted` },
  { name: 'docker-hosted', type: 'hosted', format: 'docker', status: { online: true }, url: `${baseUrl}/docker-hosted` },
  { name: 'pypi-hosted', type: 'hosted', format: 'pypi', status: { online: true }, url: `${baseUrl}/pypi-hosted` },
  { name: 'raw-hosted', type: 'hosted', format: 'raw', status: { online: true }, url: `${baseUrl}/raw-hosted` },
  { name: 'apt-hosted', type: 'hosted', format: 'apt', status: { online: true }, url: `${baseUrl}/apt-hosted` },
  { name: 'cargo-hosted', type: 'hosted', format: 'cargo', status: { online: true }, url: `${baseUrl}/cargo-hosted` },
  { name: 'cocoapods-hosted', type: 'hosted', format: 'cocoapods', status: { online: true }, url: `${baseUrl}/cocoapods-hosted` },
  { name: 'composer-hosted', type: 'hosted', format: 'composer', status: { online: true }, url: `${baseUrl}/composer-hosted` },
  { name: 'conan-hosted', type: 'hosted', format: 'conan', status: { online: true }, url: `${baseUrl}/conan-hosted` },
  { name: 'conda-hosted', type: 'hosted', format: 'conda', status: { online: true }, url: `${baseUrl}/conda-hosted` },
  { name: 'go-hosted', type: 'hosted', format: 'go', status: { online: true }, url: `${baseUrl}/go-hosted` },
  { name: 'helm-hosted', type: 'hosted', format: 'helm', status: { online: true }, url: `${baseUrl}/helm-hosted` },
  { name: 'rubygems-hosted', type: 'hosted', format: 'rubygems', status: { online: true }, url: `${baseUrl}/rubygems-hosted` },
  { name: 'yum-hosted', type: 'hosted', format: 'yum', status: { online: true }, url: `${baseUrl}/yum-hosted` },
  { name: 'huggingface-hosted', type: 'hosted', format: 'huggingface', status: { online: true }, url: `${baseUrl}/huggingface-hosted` },
  { name: 'gitlfs-hosted', type: 'hosted', format: 'gitlfs', status: { online: true }, url: `${baseUrl}/gitlfs-hosted` },
  { name: 'p2-hosted', type: 'hosted', format: 'p2', status: { online: true }, url: `${baseUrl}/p2-hosted` },
  { name: 'r-hosted', type: 'hosted', format: 'r', status: { online: true }, url: `${baseUrl}/r-hosted` },
  { name: 'swift-hosted', type: 'hosted', format: 'swift', status: { online: true }, url: `${baseUrl}/swift-hosted` },
  { name: 'terraform-hosted', type: 'hosted', format: 'terraform', status: { online: true }, url: `${baseUrl}/terraform-hosted` },
];

/**
 * Mock browse tree nodes by repository and path.
 * Key: `${repositoryName}:${path}` e.g. "maven-releases:/" or "maven-releases:/org/apache"
 */
export const MOCK_BROWSE_NODES: Record<string, BrowseNode[]> = {
  'maven-releases:/': [
    { id: 'org', text: 'org', type: 'folder', leaf: false },
    { id: 'com', text: 'com', type: 'folder', leaf: false },
  ],
  'maven-releases:/org': [
    { id: 'org/apache', text: 'apache', type: 'folder', leaf: false },
    { id: 'org/sonatype', text: 'sonatype', type: 'folder', leaf: false },
  ],
  'maven-releases:/org/apache': [
    { id: 'org/apache/maven', text: 'maven', type: 'folder', leaf: false },
  ],
  'maven-releases:/org/apache/maven': [
    {
      id: 'org/apache/maven/plugins',
      text: 'maven-plugins-3.9.6.pom',
      type: 'asset',
      leaf: true,
      assetId: 'asset-1',
    },
    {
      id: 'org/apache/maven/maven-core',
      text: 'maven-core',
      type: 'component',
      leaf: false,
      componentId: 'comp-1',
    },
  ],
  'maven-releases:/com': [
    { id: 'com/example', text: 'example', type: 'folder', leaf: false },
  ],
  'maven-releases:/com/example': [
    {
      id: 'com/example/demo',
      text: 'demo-1.0.0.jar',
      type: 'asset',
      leaf: true,
      assetId: 'asset-2',
    },
  ],
  'npm-hosted:/': [
    { id: '@sonatype', text: '@sonatype', type: 'folder', leaf: false },
    { id: 'lodash', text: 'lodash', type: 'folder', leaf: false },
  ],
  'npm-hosted:/@sonatype': [
    { id: '@sonatype/nexus-ui', text: 'nexus-ui', type: 'folder', leaf: false },
  ],
  'npm-hosted:/@sonatype/nexus-ui': [
    {
      id: '@sonatype/nexus-ui/package.json',
      text: 'package.json',
      type: 'asset',
      leaf: true,
      assetId: 'asset-npm-1',
    },
  ],
  'npm-hosted:/lodash': [
    {
      id: 'lodash/4.17.21',
      text: '4.17.21',
      type: 'component',
      leaf: false,
      componentId: 'comp-lodash',
    },
  ],
  'maven-central:/': [
    { id: 'org', text: 'org', type: 'folder', leaf: false },
  ],
  'maven-central:/org': [
    { id: 'org/apache', text: 'apache', type: 'folder', leaf: false },
  ],
  'nuget-hosted:/': [
    { id: 'Newtonsoft.Json', text: 'Newtonsoft.Json', type: 'folder', leaf: false },
  ],
  'nuget-hosted:/Newtonsoft.Json': [
    {
      id: 'Newtonsoft.Json/13.0.3',
      text: '13.0.3',
      type: 'component',
      leaf: false,
      componentId: 'comp-nuget-1',
    },
  ],
  'docker-hosted:/': [
    { id: 'alpine', text: 'alpine', type: 'folder', leaf: false },
  ],
  'docker-hosted:/alpine': [
    {
      id: 'alpine/latest',
      text: 'latest',
      type: 'asset',
      leaf: true,
      assetId: 'asset-docker-1',
    },
  ],
  // Repos below use fallback root (sample folder) - add explicit root + sample path for consistency
  'pypi-hosted:/': [
    { id: 'requests', text: 'requests', type: 'folder', leaf: false },
    { id: 'numpy', text: 'numpy', type: 'folder', leaf: false },
  ],
  'pypi-hosted:/requests': [
    {
      id: 'requests/2.31.0',
      text: '2.31.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-pypi-1',
    },
  ],
  'pypi-hosted:/numpy': [
    {
      id: 'numpy/1.26.0',
      text: '1.26.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-pypi-2',
    },
  ],
  'raw-hosted:/': [
    { id: 'docs', text: 'docs', type: 'folder', leaf: false },
    { id: 'readme.txt', text: 'readme.txt', type: 'asset', leaf: true, assetId: 'asset-raw-1' },
  ],
  'raw-hosted:/docs': [
    { id: 'docs/guide.pdf', text: 'guide.pdf', type: 'asset', leaf: true, assetId: 'asset-raw-2' },
  ],
  'apt-hosted:/': [
    { id: 'pool', text: 'pool', type: 'folder', leaf: false },
  ],
  'apt-hosted:/pool': [
    { id: 'pool/main', text: 'main', type: 'folder', leaf: false },
  ],
  'apt-hosted:/pool/main': [
    {
      id: 'pool/main/curl',
      text: 'curl_7.88.1',
      type: 'asset',
      leaf: true,
      assetId: 'asset-apt-1',
    },
  ],
  'cargo-hosted:/': [
    { id: 'serde', text: 'serde', type: 'folder', leaf: false },
  ],
  'cargo-hosted:/serde': [
    {
      id: 'serde/1.0.190',
      text: '1.0.190',
      type: 'component',
      leaf: false,
      componentId: 'comp-cargo-1',
    },
  ],
  'cocoapods-hosted:/': [
    { id: 'Alamofire', text: 'Alamofire', type: 'folder', leaf: false },
  ],
  'cocoapods-hosted:/Alamofire': [
    {
      id: 'Alamofire/5.8.0',
      text: '5.8.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-cocoapods-1',
    },
  ],
  'composer-hosted:/': [
    { id: 'monolog', text: 'monolog', type: 'folder', leaf: false },
  ],
  'composer-hosted:/monolog': [
    {
      id: 'monolog/3.5.0',
      text: '3.5.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-composer-1',
    },
  ],
  'conan-hosted:/': [
    { id: 'zlib', text: 'zlib', type: 'folder', leaf: false },
  ],
  'conan-hosted:/zlib': [
    {
      id: 'zlib/1.2.13',
      text: '1.2.13',
      type: 'component',
      leaf: false,
      componentId: 'comp-conan-1',
    },
  ],
  'conda-hosted:/': [
    { id: 'numpy', text: 'numpy', type: 'folder', leaf: false },
  ],
  'conda-hosted:/numpy': [
    {
      id: 'numpy/1.26.0',
      text: '1.26.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-conda-1',
    },
  ],
  'go-hosted:/': [
    { id: 'github.com', text: 'github.com', type: 'folder', leaf: false },
  ],
  'go-hosted:/github.com': [
    { id: 'github.com/gin-gonic', text: 'gin-gonic', type: 'folder', leaf: false },
  ],
  'go-hosted:/github.com/gin-gonic': [
    {
      id: 'github.com/gin-gonic/gin',
      text: 'gin',
      type: 'component',
      leaf: false,
      componentId: 'comp-go-1',
    },
  ],
  'helm-hosted:/': [
    { id: 'nginx-ingress', text: 'nginx-ingress', type: 'folder', leaf: false },
  ],
  'helm-hosted:/nginx-ingress': [
    {
      id: 'nginx-ingress/1.0.0',
      text: '1.0.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-helm-1',
    },
  ],
  'rubygems-hosted:/': [
    { id: 'rails', text: 'rails', type: 'folder', leaf: false },
  ],
  'rubygems-hosted:/rails': [
    {
      id: 'rails/7.1.0',
      text: '7.1.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-rubygems-1',
    },
  ],
  'yum-hosted:/': [
    { id: 'nginx', text: 'nginx', type: 'folder', leaf: false },
  ],
  'yum-hosted:/nginx': [
    {
      id: 'nginx/1.24.0',
      text: '1.24.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-yum-1',
    },
  ],
  'huggingface-hosted:/': [
    { id: 'bert-base-uncased', text: 'bert-base-uncased', type: 'folder', leaf: false },
  ],
  'huggingface-hosted:/bert-base-uncased': [
    {
      id: 'bert-base-uncased/model.bin',
      text: 'model.bin',
      type: 'asset',
      leaf: true,
      assetId: 'asset-hf-1',
    },
  ],
  'gitlfs-hosted:/': [
    { id: 'models', text: 'models', type: 'folder', leaf: false },
  ],
  'gitlfs-hosted:/models': [
    {
      id: 'models/model.bin',
      text: 'model.bin',
      type: 'asset',
      leaf: true,
      assetId: 'asset-gitlfs-1',
    },
  ],
  'p2-hosted:/': [
    { id: 'org.eclipse.jdt', text: 'org.eclipse.jdt', type: 'folder', leaf: false },
  ],
  'p2-hosted:/org.eclipse.jdt': [
    {
      id: 'org.eclipse.jdt/3.30.0',
      text: '3.30.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-p2-1',
    },
  ],
  'r-hosted:/': [
    { id: 'ggplot2', text: 'ggplot2', type: 'folder', leaf: false },
  ],
  'r-hosted:/ggplot2': [
    {
      id: 'ggplot2/3.4.4',
      text: '3.4.4',
      type: 'component',
      leaf: false,
      componentId: 'comp-r-1',
    },
  ],
  'swift-hosted:/': [
    { id: 'Alamofire', text: 'Alamofire', type: 'folder', leaf: false },
  ],
  'swift-hosted:/Alamofire': [
    {
      id: 'Alamofire/5.8.0',
      text: '5.8.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-swift-1',
    },
  ],
  'terraform-hosted:/': [
    { id: 'aws', text: 'aws', type: 'folder', leaf: false },
  ],
  'terraform-hosted:/aws': [
    {
      id: 'aws/4.0.0',
      text: '4.0.0',
      type: 'component',
      leaf: false,
      componentId: 'comp-terraform-1',
    },
  ],
};

/**
 * Get mock browse nodes for a repository and path.
 * Returns empty array if no mock data exists (allows graceful fallback).
 * For unknown repos, returns a placeholder root so the tree isn't empty.
 */
export function getMockBrowseNodes(repositoryName: string, path: string): BrowseNode[] {
  // Tree passes nodeId: "/" for root, "org" or "org/apache" for expand (no leading slash)
  const normalizedPath =
    path === '' || path === '/' ? '/' : path.startsWith('/') ? path : `/${path}`;
  const key = `${repositoryName}:${normalizedPath}`;
  const nodes = MOCK_BROWSE_NODES[key];
  if (nodes) return nodes;
  // Fallback: if it's root and we have the repo in our list, show a placeholder
  if (normalizedPath === '/' && MOCK_REPOSITORIES.some((r) => r.name === repositoryName)) {
    return [{ id: 'sample', text: 'sample (mock)', type: 'folder', leaf: false }];
  }
  if (path === 'sample' || path === '/sample') {
    return [{ id: 'sample/readme', text: 'readme.txt', type: 'asset', leaf: true, assetId: 'mock-asset' }];
  }
  return [];
}

/**
 * Mock asset/component data for detail panel when in mock mode.
 */
export function getMockAsset(assetId: string, repositoryName: string): AssetXO | null {
  const mockAssets: Record<string, Partial<AssetXO>> = {
    'asset-1': {
      id: assetId,
      name: 'maven-plugins-3.9.6.pom',
      format: 'maven2',
      repositoryName,
      path: 'org/apache/maven/maven-plugins-3.9.6.pom',
      downloadUrl: `${baseUrl}/${repositoryName}/org/apache/maven/maven-plugins-3.9.6.pom`,
      contentType: 'application/xml',
      size: 12345,
      lastModified: '2024-01-15T10:00:00Z',
    },
    'asset-2': {
      id: assetId,
      name: 'demo-1.0.0.jar',
      format: 'maven2',
      repositoryName,
      path: 'com/example/demo-1.0.0.jar',
      downloadUrl: `${baseUrl}/${repositoryName}/com/example/demo-1.0.0.jar`,
      contentType: 'application/java-archive',
      size: 54321,
      lastModified: '2024-02-20T14:30:00Z',
    },
    'asset-npm-1': {
      id: assetId,
      name: 'package.json',
      format: 'npm',
      repositoryName,
      path: '@sonatype/nexus-ui/package.json',
      downloadUrl: `${baseUrl}/${repositoryName}/@sonatype/nexus-ui/package.json`,
      contentType: 'application/json',
      size: 1024,
      lastModified: '2024-03-01T09:00:00Z',
    },
    'asset-docker-1': {
      id: assetId,
      name: 'latest',
      format: 'docker',
      repositoryName,
      path: 'alpine/latest',
      downloadUrl: `${baseUrl}/${repositoryName}/alpine/latest`,
      contentType: 'application/octet-stream',
      size: 0,
      lastModified: '2024-01-01T00:00:00Z',
    },
    'mock-asset': {
      id: assetId,
      name: 'readme.txt',
      format: 'raw',
      repositoryName,
      path: 'sample/readme.txt',
      downloadUrl: `${baseUrl}/${repositoryName}/sample/readme.txt`,
      contentType: 'text/plain',
      size: 256,
      lastModified: '2024-01-01T00:00:00Z',
    },
  };
  const data = mockAssets[assetId];
  if (!data) return null;
  return { ...data } as AssetXO;
}

export function getMockComponent(componentId: string, repositoryName: string): ComponentXO | null {
  const mockComponents: Record<string, Partial<ComponentXO>> = {
    'comp-1': {
      id: componentId,
      repositoryName,
      format: 'maven2',
      group: 'org.apache.maven',
      name: 'maven-core',
      version: '3.9.6',
    },
    'comp-lodash': {
      id: componentId,
      repositoryName,
      format: 'npm',
      name: 'lodash',
      version: '4.17.21',
    },
    'comp-nuget-1': {
      id: componentId,
      repositoryName,
      format: 'nuget',
      name: 'Newtonsoft.Json',
      version: '13.0.3',
    },
  };
  const data = mockComponents[componentId];
  if (!data) return null;
  return { ...data } as ComponentXO;
}

/**
 * Build a mock RepositoryPageResponse for the filtered API.
 */
export function getMockRepositoryPageResponse(
  params: { page?: number; pageSize?: number; formats?: string; nameFilter?: string } = {}
): RepositoryPageResponse {
  const { page = 1, pageSize = 50, formats, nameFilter } = params;
  let repos = [...MOCK_REPOSITORIES];

  if (formats) {
    const formatList = formats.split(',').map((f) => f.trim().toLowerCase());
    repos = repos.filter((r) => formatList.includes(r.format.toLowerCase()));
  }
  if (nameFilter) {
    const filter = nameFilter.toLowerCase();
    repos = repos.filter((r) => r.name.toLowerCase().includes(filter));
  }

  const totalCount = repos.length;
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const start = (page - 1) * pageSize;
  const data = repos.slice(start, start + pageSize);

  return {
    data,
    totalCount,
    page,
    pageSize,
    totalPages,
    hasNextPage: page < totalPages,
    hasPreviousPage: page > 1,
  };
}

// =============================================================================
// SEARCH MOCK (for UnifiedSearchPage)
// =============================================================================

/**
 * Raw search item shape matching /service/rest/v1/search API response.
 */
export interface MockRawSearchItem {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string;
  assets: Array<{
    id: string;
    path: string;
    downloadUrl: string;
    contentType?: string;
    lastModified?: string;
  }>;
}

const MOCK_SEARCH_ITEMS: MockRawSearchItem[] = [
  {
    id: 'maven2:org.apache.maven:maven-core:3.9.6',
    repository: 'maven-releases',
    format: 'maven2',
    group: 'org.apache.maven',
    name: 'maven-core',
    version: '3.9.6',
    assets: [
      {
        id: 'asset-maven-1',
        path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.pom',
        downloadUrl: `${baseUrl}/maven-releases/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.pom`,
        contentType: 'application/xml',
        lastModified: '2024-01-15T10:00:00Z',
      },
    ],
  },
  {
    id: 'npm:lodash:4.17.21',
    repository: 'npm-hosted',
    format: 'npm',
    group: null,
    name: 'lodash',
    version: '4.17.21',
    assets: [
      {
        id: 'asset-npm-1',
        path: 'lodash/-/lodash-4.17.21.tgz',
        downloadUrl: `${baseUrl}/npm-hosted/lodash/-/lodash-4.17.21.tgz`,
        contentType: 'application/gzip',
        lastModified: '2024-02-10T14:00:00Z',
      },
    ],
  },
  {
    id: 'nuget:Newtonsoft.Json:13.0.3',
    repository: 'nuget-hosted',
    format: 'nuget',
    group: null,
    name: 'Newtonsoft.Json',
    version: '13.0.3',
    assets: [
      {
        id: 'asset-nuget-1',
        path: 'newtonsoft.json/13.0.3/newtonsoft.json.13.0.3.nupkg',
        downloadUrl: `${baseUrl}/nuget-hosted/newtonsoft.json/13.0.3/newtonsoft.json.13.0.3.nupkg`,
        contentType: 'application/zip',
        lastModified: '2024-03-01T09:00:00Z',
      },
    ],
  },
  {
    id: 'docker:alpine:latest',
    repository: 'docker-hosted',
    format: 'docker',
    group: null,
    name: 'alpine',
    version: 'latest',
    assets: [
      {
        id: 'asset-docker-1',
        path: 'alpine/latest',
        downloadUrl: `${baseUrl}/docker-hosted/alpine/latest`,
        contentType: 'application/octet-stream',
        lastModified: '2024-01-01T00:00:00Z',
      },
    ],
  },
  {
    id: 'maven2:com.example:demo:1.0.0',
    repository: 'maven-releases',
    format: 'maven2',
    group: 'com.example',
    name: 'demo',
    version: '1.0.0',
    assets: [
      {
        id: 'asset-maven-2',
        path: 'com/example/demo/1.0.0/demo-1.0.0.jar',
        downloadUrl: `${baseUrl}/maven-releases/com/example/demo/1.0.0/demo-1.0.0.jar`,
        contentType: 'application/java-archive',
        lastModified: '2024-02-20T14:30:00Z',
      },
    ],
  },
  {
    id: 'pypi:requests:2.31.0',
    repository: 'pypi-hosted',
    format: 'pypi',
    group: null,
    name: 'requests',
    version: '2.31.0',
    assets: [{ id: 'asset-pypi-1', path: 'requests-2.31.0-py3-none-any.whl', downloadUrl: `${baseUrl}/pypi-hosted/requests-2.31.0-py3-none-any.whl`, contentType: 'application/zip', lastModified: '2024-01-10T12:00:00Z' }],
  },
  {
    id: 'raw:docs:readme:1.0',
    repository: 'raw-hosted',
    format: 'raw',
    group: null,
    name: 'readme.txt',
    version: '1.0',
    assets: [{ id: 'asset-raw-1', path: 'docs/readme.txt', downloadUrl: `${baseUrl}/raw-hosted/docs/readme.txt`, contentType: 'text/plain', lastModified: '2024-01-05T09:00:00Z' }],
  },
  {
    id: 'apt:curl:7.88.1',
    repository: 'apt-hosted',
    format: 'apt',
    group: null,
    name: 'curl',
    version: '7.88.1',
    assets: [{ id: 'asset-apt-1', path: 'pool/main/c/curl/curl_7.88.1_amd64.deb', downloadUrl: `${baseUrl}/apt-hosted/pool/main/c/curl/curl_7.88.1_amd64.deb`, contentType: 'application/vnd.debian.binary-package', lastModified: '2024-02-01T10:00:00Z' }],
  },
  {
    id: 'cargo:serde:1.0.195',
    repository: 'cargo-hosted',
    format: 'cargo',
    group: null,
    name: 'serde',
    version: '1.0.195',
    assets: [{ id: 'asset-cargo-1', path: 'serde-1.0.195.crate', downloadUrl: `${baseUrl}/cargo-hosted/serde-1.0.195.crate`, contentType: 'application/gzip', lastModified: '2024-03-01T14:00:00Z' }],
  },
  {
    id: 'cocoapods:Alamofire:5.8.0',
    repository: 'cocoapods-hosted',
    format: 'cocoapods',
    group: null,
    name: 'Alamofire',
    version: '5.8.0',
    assets: [{ id: 'asset-cocoapods-1', path: 'Alamofire/5.8.0/Alamofire.podspec', downloadUrl: `${baseUrl}/cocoapods-hosted/Alamofire/5.8.0/Alamofire.podspec`, contentType: 'text/x-ruby', lastModified: '2024-02-15T11:00:00Z' }],
  },
  {
    id: 'composer:monolog:monolog:3.5.0',
    repository: 'composer-hosted',
    format: 'composer',
    group: 'monolog',
    name: 'monolog',
    version: '3.5.0',
    assets: [{ id: 'asset-composer-1', path: 'monolog/monolog/3.5.0/monolog-3.5.0.zip', downloadUrl: `${baseUrl}/composer-hosted/monolog/monolog/3.5.0/monolog-3.5.0.zip`, contentType: 'application/zip', lastModified: '2024-01-20T08:00:00Z' }],
  },
  {
    id: 'conan:zlib:1.3.1',
    repository: 'conan-hosted',
    format: 'conan',
    group: null,
    name: 'zlib',
    version: '1.3.1',
    assets: [{ id: 'asset-conan-1', path: 'zlib/1.3.1/conanfile.py', downloadUrl: `${baseUrl}/conan-hosted/zlib/1.3.1/conanfile.py`, contentType: 'text/x-python', lastModified: '2024-02-25T16:00:00Z' }],
  },
  {
    id: 'conda:numpy:1.26.4',
    repository: 'conda-hosted',
    format: 'conda',
    group: null,
    name: 'numpy',
    version: '1.26.4',
    assets: [{ id: 'asset-conda-1', path: 'numpy-1.26.4-py310.conda', downloadUrl: `${baseUrl}/conda-hosted/numpy-1.26.4-py310.conda`, contentType: 'application/octet-stream', lastModified: '2024-03-05T09:00:00Z' }],
  },
  {
    id: 'go:github.com/gin-gonic/gin:v1.9.1',
    repository: 'go-hosted',
    format: 'go',
    group: null,
    name: 'github.com/gin-gonic/gin',
    version: 'v1.9.1',
    assets: [{ id: 'asset-go-1', path: 'github.com/gin-gonic/gin/@v/v1.9.1.zip', downloadUrl: `${baseUrl}/go-hosted/github.com/gin-gonic/gin/@v/v1.9.1.zip`, contentType: 'application/zip', lastModified: '2024-02-10T13:00:00Z' }],
  },
  {
    id: 'helm:nginx-ingress:4.8.0',
    repository: 'helm-hosted',
    format: 'helm',
    group: null,
    name: 'nginx-ingress',
    version: '4.8.0',
    assets: [{ id: 'asset-helm-1', path: 'nginx-ingress-4.8.0.tgz', downloadUrl: `${baseUrl}/helm-hosted/nginx-ingress-4.8.0.tgz`, contentType: 'application/gzip', lastModified: '2024-01-30T10:00:00Z' }],
  },
  {
    id: 'rubygems:rails:7.1.3',
    repository: 'rubygems-hosted',
    format: 'rubygems',
    group: null,
    name: 'rails',
    version: '7.1.3',
    assets: [{ id: 'asset-rubygems-1', path: 'gems/rails-7.1.3.gem', downloadUrl: `${baseUrl}/rubygems-hosted/gems/rails-7.1.3.gem`, contentType: 'application/octet-stream', lastModified: '2024-02-20T14:00:00Z' }],
  },
  {
    id: 'yum:nginx:1.24.0',
    repository: 'yum-hosted',
    format: 'yum',
    group: null,
    name: 'nginx',
    version: '1.24.0',
    assets: [{ id: 'asset-yum-1', path: 'nginx-1.24.0-1.el7.x86_64.rpm', downloadUrl: `${baseUrl}/yum-hosted/nginx-1.24.0-1.el7.x86_64.rpm`, contentType: 'application/x-rpm', lastModified: '2024-02-05T11:00:00Z' }],
  },
  {
    id: 'huggingface:bert-base-uncased:1.0',
    repository: 'huggingface-hosted',
    format: 'huggingface',
    group: null,
    name: 'bert-base-uncased',
    version: '1.0',
    assets: [{ id: 'asset-hf-1', path: 'bert-base-uncased/config.json', downloadUrl: `${baseUrl}/huggingface-hosted/bert-base-uncased/config.json`, contentType: 'application/json', lastModified: '2024-03-01T12:00:00Z' }],
  },
  {
    id: 'gitlfs:file:abc123',
    repository: 'gitlfs-hosted',
    format: 'gitlfs',
    group: null,
    name: 'model.bin',
    version: 'abc123',
    assets: [{ id: 'asset-gitlfs-1', path: 'objects/ab/c1/model.bin', downloadUrl: `${baseUrl}/gitlfs-hosted/objects/ab/c1/model.bin`, contentType: 'application/octet-stream', lastModified: '2024-01-15T09:00:00Z' }],
  },
  {
    id: 'p2:org.eclipse.jdt:3.31.0',
    repository: 'p2-hosted',
    format: 'p2',
    group: null,
    name: 'org.eclipse.jdt',
    version: '3.31.0',
    assets: [{ id: 'asset-p2-1', path: 'plugins/org.eclipse.jdt_3.31.0.jar', downloadUrl: `${baseUrl}/p2-hosted/plugins/org.eclipse.jdt_3.31.0.jar`, contentType: 'application/java-archive', lastModified: '2024-02-28T10:00:00Z' }],
  },
  {
    id: 'r:ggplot2:3.4.4',
    repository: 'r-hosted',
    format: 'r',
    group: null,
    name: 'ggplot2',
    version: '3.4.4',
    assets: [{ id: 'asset-r-1', path: 'ggplot2_3.4.4.tar.gz', downloadUrl: `${baseUrl}/r-hosted/ggplot2_3.4.4.tar.gz`, contentType: 'application/gzip', lastModified: '2024-02-12T15:00:00Z' }],
  },
  {
    id: 'swift:Alamofire:5.8.1',
    repository: 'swift-hosted',
    format: 'swift',
    group: null,
    name: 'Alamofire',
    version: '5.8.1',
    assets: [{ id: 'asset-swift-1', path: 'Alamofire/5.8.1/Alamofire.swiftpackage', downloadUrl: `${baseUrl}/swift-hosted/Alamofire/5.8.1/Alamofire.swiftpackage`, contentType: 'application/zip', lastModified: '2024-03-02T11:00:00Z' }],
  },
  {
    id: 'terraform:hashicorp:aws:5.30.0',
    repository: 'terraform-hosted',
    format: 'terraform',
    group: 'hashicorp',
    name: 'aws',
    version: '5.30.0',
    assets: [{ id: 'asset-terraform-1', path: 'hashicorp/aws/5.30.0/aws-5.30.0.zip', downloadUrl: `${baseUrl}/terraform-hosted/hashicorp/aws/5.30.0/aws-5.30.0.zip`, contentType: 'application/zip', lastModified: '2024-02-18T14:00:00Z' }],
  },
];

/**
 * Repositories for search useRepositories hook (name, format, type, url).
 */
export function getMockRepositoriesForSearch(): Array<{ name: string; format: string; type: string; url?: string }> {
  return MOCK_REPOSITORIES.map((r) => ({
    name: r.name,
    format: r.format,
    type: r.type,
    url: r.url,
  }));
}

/**
 * Total count of mock components when unfiltered (no query, format, or repository filter).
 * Used for the Components header counter in mock mode.
 */
export function getMockSearchTotalCount(): number {
  return MOCK_SEARCH_ITEMS.length;
}

/**
 * Mock search results for UnifiedSearchPage.
 * Filters by format when specified; returns items matching query (substring) when provided.
 */
export function getMockSearchResults(
  query: string,
  format?: string,
  repositoryFilter?: string
): { items: MockRawSearchItem[]; continuationToken?: string } {
  let items = [...MOCK_SEARCH_ITEMS];

  if (format && format !== 'all') {
    const apiFormat = format === 'maven' ? 'maven2' : format;
    items = items.filter((i) => i.format.toLowerCase() === apiFormat.toLowerCase());
  }
  if (repositoryFilter) {
    items = items.filter((i) => i.repository === repositoryFilter);
  }
  if (query.trim()) {
    const q = query.trim().toLowerCase();
    items = items.filter(
      (i) =>
        i.name.toLowerCase().includes(q) ||
        (i.group && i.group.toLowerCase().includes(q)) ||
        i.version.toLowerCase().includes(q)
    );
  }

  return { items, continuationToken: undefined };
}
