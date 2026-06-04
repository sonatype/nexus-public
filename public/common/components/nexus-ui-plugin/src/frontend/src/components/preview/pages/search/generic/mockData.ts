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

import type { GenericResult, GenericSearchFilters, GenericSearchResponse } from './generic.types';

/**
 * Mock data for generic search development.
 */
export const mockGenericResults: GenericResult[] = [
  {
    id: 'maven:org.apache.commons:commons-lang3:3.12.0',
    format: 'maven2',
    repository: 'maven-central',
    group: 'org.apache.commons',
    name: 'commons-lang3',
    version: '3.12.0',
    displayName: 'org.apache.commons:commons-lang3',
    assets: [
      {
        id: 'asset-1',
        path: 'org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
        downloadUrl: '/repository/maven-central/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar',
      },
    ],
  },
  {
    id: 'npm:react:19.1.0',
    format: 'npm',
    repository: 'npm-proxy-v1',
    group: null,
    name: 'react',
    version: '19.1.0',
    displayName: 'react',
    assets: [
      {
        id: 'asset-2',
        path: 'react/-/react-19.1.0.tgz',
        downloadUrl: '/repository/npm-proxy-v1/react/-/react-19.1.0.tgz',
      },
    ],
  },
  {
    id: 'npm:@types/react:19.0.0',
    format: 'npm',
    repository: 'npm-proxy-v1',
    group: '@types',
    name: 'react',
    version: '19.0.0',
    displayName: '@types/react',
    assets: [
      {
        id: 'asset-3',
        path: '@types/react/-/react-19.0.0.tgz',
        downloadUrl: '/repository/npm-proxy-v1/@types/react/-/react-19.0.0.tgz',
      },
    ],
  },
  {
    id: 'nuget:Newtonsoft.Json:13.0.3',
    format: 'nuget',
    repository: 'nuget.org-proxy',
    group: null,
    name: 'Newtonsoft.Json',
    version: '13.0.3',
    displayName: 'Newtonsoft.Json',
    assets: [
      {
        id: 'asset-4',
        path: 'Newtonsoft.Json/13.0.3/newtonsoft.json.13.0.3.nupkg',
        downloadUrl: '/repository/nuget.org-proxy/Newtonsoft.Json/13.0.3/newtonsoft.json.13.0.3.nupkg',
      },
    ],
  },
  {
    id: 'docker:library/nginx:latest',
    format: 'docker',
    repository: 'docker-proxy-v1',
    group: 'library',
    name: 'nginx',
    version: 'latest',
    displayName: 'library/nginx',
    assets: [
      {
        id: 'asset-5',
        path: 'v2/library/nginx/manifests/latest',
        downloadUrl: '/repository/docker-proxy-v1/v2/library/nginx/manifests/latest',
      },
    ],
  },
  {
    id: 'pypi:requests:2.31.0',
    format: 'pypi',
    repository: 'pypi-proxy',
    group: null,
    name: 'requests',
    version: '2.31.0',
    displayName: 'requests',
    assets: [
      {
        id: 'asset-6',
        path: 'packages/requests/2.31.0/requests-2.31.0.tar.gz',
        downloadUrl: '/repository/pypi-proxy/packages/requests/2.31.0/requests-2.31.0.tar.gz',
      },
    ],
  },
  {
    id: 'maven:com.google.guava:guava:32.1.2-jre',
    format: 'maven2',
    repository: 'maven-central',
    group: 'com.google.guava',
    name: 'guava',
    version: '32.1.2-jre',
    displayName: 'com.google.guava:guava',
    assets: [
      {
        id: 'asset-7',
        path: 'com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar',
        downloadUrl: '/repository/maven-central/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar',
      },
    ],
  },
  {
    id: 'npm:lodash:4.17.21',
    format: 'npm',
    repository: 'npm-proxy-v1',
    group: null,
    name: 'lodash',
    version: '4.17.21',
    displayName: 'lodash',
    assets: [
      {
        id: 'asset-8',
        path: 'lodash/-/lodash-4.17.21.tgz',
        downloadUrl: '/repository/npm-proxy-v1/lodash/-/lodash-4.17.21.tgz',
      },
    ],
  },
  {
    id: 'helm:bitnami/redis:17.3.14',
    format: 'helm',
    repository: 'helm-proxy',
    group: 'bitnami',
    name: 'redis',
    version: '17.3.14',
    displayName: 'bitnami/redis',
    assets: [
      {
        id: 'asset-9',
        path: 'charts/redis-17.3.14.tgz',
        downloadUrl: '/repository/helm-proxy/charts/redis-17.3.14.tgz',
      },
    ],
  },
  {
    id: 'go:github.com/gin-gonic/gin:v1.9.1',
    format: 'go',
    repository: 'go-proxy',
    group: 'github.com/gin-gonic',
    name: 'gin',
    version: 'v1.9.1',
    displayName: 'github.com/gin-gonic/gin',
    assets: [
      {
        id: 'asset-10',
        path: 'github.com/gin-gonic/gin/@v/v1.9.1.zip',
        downloadUrl: '/repository/go-proxy/github.com/gin-gonic/gin/@v/v1.9.1.zip',
      },
    ],
  },
];

/**
 * Mock search API for development.
 */
export async function mockGenericSearchApi(
  filters: GenericSearchFilters
): Promise<GenericSearchResponse> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  let results = [...mockGenericResults];

  // Apply filters
  if (filters.q) {
    const query = filters.q.toLowerCase();
    results = results.filter(
      (r) =>
        r.name.toLowerCase().includes(query) ||
        r.displayName.toLowerCase().includes(query) ||
        (r.group && r.group.toLowerCase().includes(query))
    );
  }

  if (filters.format) {
    results = results.filter((r) => r.format === filters.format);
  }

  if (filters.repository) {
    results = results.filter((r) => r.repository === filters.repository);
  }

  if (filters.group) {
    const group = filters.group.toLowerCase();
    results = results.filter((r) => r.group && r.group.toLowerCase().includes(group));
  }

  if (filters.name) {
    const name = filters.name.toLowerCase();
    results = results.filter((r) => r.name.toLowerCase().includes(name));
  }

  if (filters.version) {
    results = results.filter((r) => r.version.includes(filters.version!));
  }

  return {
    items: results,
    totalCount: results.length,
    continuationToken: undefined,
  };
}


