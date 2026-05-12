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

import type { AptResult, AptSearchResponse, AptDetail, AptSearchFilters } from './apt.types';

/**
 * Mock Apt package data for development and testing.
 */
export const mockAptResults: AptResult[] = [
  {
    id: 'apt:nginx',
    name: 'nginx',
    displayName: 'nginx',
    latestVersion: '1.24.0-1',
    versionsCount: 45,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'High performance web server and reverse proxy',
    maintainer: 'Debian Nginx Maintainers <pkg-nginx-maintainers@lists.alioth.debian.org>',
    section: 'httpd',
    priority: 'optional',
    installedSize: 1536000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'apt:curl',
    name: 'curl',
    displayName: 'curl',
    latestVersion: '7.88.1-10',
    versionsCount: 89,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Command line tool for transferring data with URL syntax',
    maintainer: 'Debian Curl Maintainers <debian-curl-pkg@lists.launchpad.net>',
    section: 'web',
    priority: 'standard',
    installedSize: 512000,
    repositoriesCount: 3,
    lastUpdated: '2024-01-18T14:22:00Z',
  },
  {
    id: 'apt:git',
    name: 'git',
    displayName: 'git',
    latestVersion: '2.39.2-1.1',
    versionsCount: 156,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Fast, scalable, distributed revision control system',
    maintainer: 'Jonathan Nieder <jrnieder@gmail.com>',
    section: 'vcs',
    priority: 'optional',
    installedSize: 45056000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T09:15:00Z',
  },
  {
    id: 'apt:vim',
    name: 'vim',
    displayName: 'vim',
    latestVersion: '9.0.1378-2',
    versionsCount: 234,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Vi IMproved - enhanced vi editor',
    maintainer: 'Debian Vim Maintainers <pkg-vim-maintainers@lists.alioth.debian.org>',
    section: 'editors',
    priority: 'optional',
    installedSize: 3584000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-10T16:45:00Z',
  },
  {
    id: 'apt:openssh-server',
    name: 'openssh-server',
    displayName: 'openssh-server',
    latestVersion: '9.2p1-2',
    versionsCount: 78,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Secure shell (SSH) server for secure access from remote machines',
    maintainer: 'Debian OpenSSH Maintainers <debian-ssh@lists.debian.org>',
    section: 'net',
    priority: 'optional',
    installedSize: 1024000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-08T14:00:00Z',
  },
  {
    id: 'apt:python3',
    name: 'python3',
    displayName: 'python3',
    latestVersion: '3.11.2-1',
    versionsCount: 312,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Interactive high-level object-oriented language (default version)',
    maintainer: 'Matthias Klose <doko@debian.org>',
    section: 'python',
    priority: 'standard',
    installedSize: 82000,
    repositoriesCount: 3,
    lastUpdated: '2024-01-19T11:00:00Z',
  },
  {
    id: 'apt:nodejs',
    name: 'nodejs',
    displayName: 'nodejs',
    latestVersion: '18.19.0-1',
    versionsCount: 89,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Event-based server-side JavaScript engine',
    maintainer: 'Debian Javascript Maintainers <pkg-javascript-devel@lists.alioth.debian.org>',
    section: 'web',
    priority: 'optional',
    installedSize: 122880000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-17T08:30:00Z',
  },
  {
    id: 'apt:postgresql-15',
    name: 'postgresql-15',
    displayName: 'postgresql-15',
    latestVersion: '15.5-1',
    versionsCount: 45,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Object-relational SQL database, version 15 server',
    maintainer: 'Debian PostgreSQL Maintainers <pkg-postgresql-public@lists.alioth.debian.org>',
    section: 'database',
    priority: 'optional',
    installedSize: 65536000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-12T10:00:00Z',
  },
  {
    id: 'apt:docker-ce',
    name: 'docker-ce',
    displayName: 'docker-ce',
    latestVersion: '24.0.7-1',
    versionsCount: 156,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'stable',
    description: 'Docker: the open-source application container engine',
    maintainer: 'Docker, Inc.',
    section: 'admin',
    priority: 'optional',
    installedSize: 102400000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-05T12:00:00Z',
  },
  {
    id: 'apt:build-essential',
    name: 'build-essential',
    displayName: 'build-essential',
    latestVersion: '12.9',
    versionsCount: 34,
    architecture: 'amd64',
    distribution: 'bookworm',
    component: 'main',
    description: 'Informational list of build-essential packages',
    maintainer: 'Matthias Klose <doko@debian.org>',
    section: 'devel',
    priority: 'optional',
    installedSize: 20000,
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T08:00:00Z',
  },
];

/**
 * Mock Apt detail data.
 */
export const mockAptDetail: AptDetail = {
  id: 'apt:nginx',
  name: 'nginx',
  displayName: 'nginx',
  description: `Nginx ("engine X") is a high-performance HTTP and reverse proxy server, as well as a mail proxy server.

Nginx is known for its high performance, stability, rich feature set, simple configuration, and low resource consumption.

This package provides the nginx web server.`,
  maintainer: 'Debian Nginx Maintainers <pkg-nginx-maintainers@lists.alioth.debian.org>',
  section: 'httpd',
  priority: 'optional',
  homepage: 'https://nginx.org',
  versions: [
    { version: '1.24.0-1', architecture: 'amd64', distribution: 'bookworm', component: 'main', published: '2024-01-20T10:30:00Z', repository: 'apt-hosted' },
    { version: '1.22.1-1', architecture: 'amd64', distribution: 'bullseye', component: 'main', published: '2023-11-15T10:00:00Z', repository: 'apt-hosted' },
    { version: '1.22.0-1', architecture: 'amd64', distribution: 'bullseye', component: 'main', published: '2023-06-22T09:00:00Z', repository: 'apt-proxy' },
    { version: '1.18.0-6', architecture: 'amd64', distribution: 'buster', component: 'main', published: '2022-04-10T08:00:00Z', repository: 'apt-proxy' },
  ],
  repositories: ['apt-hosted', 'apt-proxy', 'apt-group'],
  depends: ['libc6 (>= 2.28)', 'libcrypt1 (>= 1:4.1.0)', 'libpcre2-8-0 (>= 10.22)', 'libssl3 (>= 3.0.0)', 'zlib1g (>= 1:1.1.4)'],
  recommends: ['nginx-doc'],
  suggests: ['nginx-extras'],
};

/**
 * Simulates Apt search API call with mock data.
 */
export async function mockAptSearchApi(filters: AptSearchFilters): Promise<AptSearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockAptResults];

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
    const a = filters.architecture.toLowerCase();
    filtered = filtered.filter((r) =>
      r.architecture.toLowerCase() === a
    );
  }

  // Filter by distribution
  if (filters.distribution) {
    const d = filters.distribution.toLowerCase();
    filtered = filtered.filter((r) =>
      r.distribution?.toLowerCase().includes(d)
    );
  }

  // Filter by component
  if (filters.component) {
    const c = filters.component.toLowerCase();
    filtered = filtered.filter((r) =>
      r.component?.toLowerCase() === c
    );
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates Apt detail API call.
 */
export async function mockAptDetailApi(id: string): Promise<AptDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockAptDetail;
}


