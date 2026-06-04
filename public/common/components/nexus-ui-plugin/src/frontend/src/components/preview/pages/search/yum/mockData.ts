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

import type { YumResult, YumSearchResponse, YumDetail, YumSearchFilters } from './yum.types';

/**
 * Mock Yum/RPM package data for development and testing.
 */
export const mockYumResults: YumResult[] = [
  {
    id: 'yum:nginx',
    name: 'nginx',
    displayName: 'nginx-1.24.0-1.el9.x86_64',
    latestVersion: '1.24.0',
    release: '1.el9',
    architecture: 'x86_64',
    versionsCount: 15,
    summary: 'A high performance web server and reverse proxy server',
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'yum:httpd',
    name: 'httpd',
    displayName: 'httpd-2.4.57-5.el9.x86_64',
    latestVersion: '2.4.57',
    release: '5.el9',
    architecture: 'x86_64',
    versionsCount: 42,
    summary: 'Apache HTTP Server',
    repositoriesCount: 3,
    lastUpdated: '2024-01-15T14:22:00Z',
  },
  {
    id: 'yum:vim-enhanced',
    name: 'vim-enhanced',
    displayName: 'vim-enhanced-9.0.2081-1.el9.x86_64',
    latestVersion: '9.0.2081',
    release: '1.el9',
    architecture: 'x86_64',
    versionsCount: 28,
    summary: 'A version of the VIM editor which includes recent enhancements',
    repositoriesCount: 2,
    lastUpdated: '2024-01-18T09:15:00Z',
  },
  {
    id: 'yum:kernel',
    name: 'kernel',
    displayName: 'kernel-5.14.0-362.el9.x86_64',
    latestVersion: '5.14.0',
    release: '362.el9',
    architecture: 'x86_64',
    versionsCount: 156,
    summary: 'The Linux kernel',
    repositoriesCount: 2,
    lastUpdated: '2024-01-10T16:45:00Z',
  },
  {
    id: 'yum:python3',
    name: 'python3',
    displayName: 'python3-3.9.18-1.el9.x86_64',
    latestVersion: '3.9.18',
    release: '1.el9',
    architecture: 'x86_64',
    versionsCount: 34,
    summary: 'Python 3 interpreter',
    repositoriesCount: 2,
    lastUpdated: '2024-01-08T14:00:00Z',
  },
  {
    id: 'yum:gcc',
    name: 'gcc',
    displayName: 'gcc-11.4.1-2.el9.x86_64',
    latestVersion: '11.4.1',
    release: '2.el9',
    architecture: 'x86_64',
    versionsCount: 45,
    summary: 'Various compilers (C, C++, Objective-C, ...)',
    repositoriesCount: 2,
    lastUpdated: '2024-01-19T11:00:00Z',
  },
  {
    id: 'yum:docker-ce',
    name: 'docker-ce',
    displayName: 'docker-ce-24.0.7-1.el9.x86_64',
    latestVersion: '24.0.7',
    release: '1.el9',
    architecture: 'x86_64',
    versionsCount: 67,
    summary: 'The open-source application container engine',
    repositoriesCount: 1,
    lastUpdated: '2024-01-20T08:30:00Z',
  },
  {
    id: 'yum:openssl',
    name: 'openssl',
    displayName: 'openssl-3.0.7-24.el9.x86_64',
    latestVersion: '3.0.7',
    release: '24.el9',
    architecture: 'x86_64',
    versionsCount: 89,
    summary: 'Utilities from the general purpose cryptography library',
    repositoriesCount: 2,
    lastUpdated: '2024-01-05T12:00:00Z',
  },
  {
    id: 'yum:systemd',
    name: 'systemd',
    displayName: 'systemd-252-18.el9.x86_64',
    latestVersion: '252',
    release: '18.el9',
    architecture: 'x86_64',
    versionsCount: 78,
    summary: 'System and Service Manager',
    repositoriesCount: 2,
    lastUpdated: '2024-01-12T10:00:00Z',
  },
  {
    id: 'yum:bash',
    name: 'bash',
    displayName: 'bash-5.1.8-6.el9.x86_64',
    latestVersion: '5.1.8',
    release: '6.el9',
    architecture: 'x86_64',
    versionsCount: 23,
    summary: 'The GNU Bourne Again shell',
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T08:00:00Z',
  },
];

/**
 * Mock Yum detail data.
 */
export const mockYumDetail: YumDetail = {
  id: 'yum:nginx',
  name: 'nginx',
  displayName: 'nginx',
  summary: 'A high performance web server and reverse proxy server',
  description: `Nginx is a web server and a reverse proxy server for HTTP, SMTP, POP3 and IMAP 
protocols, with a strong focus on high concurrency, performance and low memory usage.

Features include:
- HTTP/2, WebSocket, FastCGI, uwsgi, SCGI support
- SSL/TLS support with SNI
- Name-based and IP-based virtual servers
- HTTP load balancing with health checks
- Mail proxy server with SSL, STARTTLS, STLS`,
  license: 'BSD',
  url: 'https://nginx.org',
  vendor: 'Nginx, Inc.',
  group: 'System Environment/Daemons',
  versions: [
    { version: '1.24.0', release: '1.el9', versionRelease: '1.24.0-1.el9', architecture: 'x86_64', published: '2024-01-20T10:30:00Z', repository: 'yum-hosted' },
    { version: '1.22.1', release: '1.el9', versionRelease: '1.22.1-1.el9', architecture: 'x86_64', published: '2023-11-15T10:00:00Z', repository: 'yum-hosted' },
    { version: '1.20.2', release: '1.el8', versionRelease: '1.20.2-1.el8', architecture: 'x86_64', published: '2023-08-22T09:00:00Z', repository: 'yum-proxy' },
    { version: '1.18.0', release: '2.el8', versionRelease: '1.18.0-2.el8', architecture: 'x86_64', published: '2022-04-10T08:00:00Z', repository: 'yum-proxy' },
    { version: '1.24.0', release: '1.el9', versionRelease: '1.24.0-1.el9', architecture: 'noarch', published: '2024-01-20T10:30:00Z', repository: 'yum-hosted', downloadUrl: '/repository/yum-hosted/nginx-1.24.0-1.el9.noarch.rpm' },
  ],
  repositories: ['yum-hosted', 'yum-proxy', 'yum-group'],
};

/**
 * Simulates Yum search API call with mock data.
 */
export async function mockYumSearchApi(filters: YumSearchFilters): Promise<YumSearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockYumResults];

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

  // Filter by architecture
  if (filters.architecture) {
    const arch = filters.architecture.toLowerCase();
    filtered = filtered.filter((r) => r.architecture.toLowerCase() === arch);
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates Yum detail API call.
 */
export async function mockYumDetailApi(id: string): Promise<YumDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockYumDetail;
}


