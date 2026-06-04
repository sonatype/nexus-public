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

import type { GolangResult, GolangSearchResponse, GolangDetail, GolangSearchFilters } from './golang.types';

/**
 * Mock Go module data for development.
 */
export const mockGolangResults: GolangResult[] = [
  {
    id: 'go:github.com/gin-gonic/gin',
    module: 'github.com/gin-gonic/gin',
    latestVersion: 'v1.9.1',
    versionsCount: 45,
    description: 'Gin is a HTTP web framework written in Go',
    license: 'MIT',
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T10:30:00Z',
  },
  {
    id: 'go:github.com/gorilla/mux',
    module: 'github.com/gorilla/mux',
    latestVersion: 'v1.8.1',
    versionsCount: 32,
    description: 'A powerful HTTP router and URL matcher for building Go web servers',
    license: 'BSD-3-Clause',
    repositoriesCount: 2,
    lastUpdated: '2024-01-10T14:22:00Z',
  },
  {
    id: 'go:github.com/spf13/cobra',
    module: 'github.com/spf13/cobra',
    latestVersion: 'v1.8.0',
    versionsCount: 28,
    description: 'A Commander for modern Go CLI interactions',
    license: 'Apache-2.0',
    repositoriesCount: 2,
    lastUpdated: '2024-01-08T09:15:00Z',
  },
  {
    id: 'go:github.com/spf13/viper',
    module: 'github.com/spf13/viper',
    latestVersion: 'v1.18.2',
    versionsCount: 35,
    description: 'Go configuration with fangs',
    license: 'MIT',
    repositoriesCount: 2,
    lastUpdated: '2024-01-12T11:00:00Z',
  },
  {
    id: 'go:github.com/stretchr/testify',
    module: 'github.com/stretchr/testify',
    latestVersion: 'v1.8.4',
    versionsCount: 42,
    description: 'A toolkit with common assertions and mocks',
    license: 'MIT',
    repositoriesCount: 3,
    lastUpdated: '2024-01-05T08:30:00Z',
  },
  {
    id: 'go:go.uber.org/zap',
    module: 'go.uber.org/zap',
    latestVersion: 'v1.26.0',
    versionsCount: 24,
    description: 'Blazing fast, structured, leveled logging in Go',
    license: 'MIT',
    repositoriesCount: 2,
    lastUpdated: '2024-01-03T16:45:00Z',
  },
  {
    id: 'go:github.com/sirupsen/logrus',
    module: 'github.com/sirupsen/logrus',
    latestVersion: 'v1.9.3',
    versionsCount: 38,
    description: 'Structured, pluggable logging for Go',
    license: 'MIT',
    repositoriesCount: 2,
    lastUpdated: '2023-12-20T12:00:00Z',
  },
  {
    id: 'go:github.com/go-chi/chi/v5',
    module: 'github.com/go-chi/chi/v5',
    latestVersion: 'v5.0.11',
    versionsCount: 18,
    description: 'lightweight, idiomatic and composable router for building Go HTTP services',
    license: 'MIT',
    repositoriesCount: 2,
    lastUpdated: '2024-01-18T14:00:00Z',
  },
  {
    id: 'go:github.com/labstack/echo/v4',
    module: 'github.com/labstack/echo/v4',
    latestVersion: 'v4.11.4',
    versionsCount: 52,
    description: 'High performance, minimalist Go web framework',
    license: 'MIT',
    repositoriesCount: 2,
    lastUpdated: '2024-01-16T10:00:00Z',
  },
  {
    id: 'go:github.com/go-gorm/gorm',
    module: 'github.com/go-gorm/gorm',
    latestVersion: 'v1.25.6',
    versionsCount: 67,
    description: 'The fantastic ORM library for Golang',
    license: 'MIT',
    repositoriesCount: 3,
    lastUpdated: '2024-01-20T08:00:00Z',
  },
];

/**
 * Mock Go module detail data.
 */
export const mockGolangDetail: GolangDetail = {
  id: 'go:github.com/gin-gonic/gin',
  module: 'github.com/gin-gonic/gin',
  description: 'Gin is a HTTP web framework written in Go (Golang). It features a Martini-like API with much better performance.',
  license: 'MIT',
  homepage: 'https://gin-gonic.com/',
  repositoryUrl: 'https://github.com/gin-gonic/gin',
  versions: [
    { version: 'v1.9.1', published: '2024-01-15T10:30:00Z', repository: 'go-hosted' },
    { version: 'v1.9.0', published: '2023-06-10T10:00:00Z', repository: 'go-hosted' },
    { version: 'v1.8.2', published: '2023-03-29T09:00:00Z', repository: 'go-hosted' },
    { version: 'v1.8.1', published: '2022-09-22T08:00:00Z', repository: 'go-proxy' },
    { version: 'v1.8.0', published: '2022-06-15T12:00:00Z', repository: 'go-proxy' },
  ],
  repositories: ['go-hosted', 'go-proxy', 'go-group'],
};

/**
 * Simulates Go module search API call with mock data.
 */
export async function mockGolangSearchApi(filters: GolangSearchFilters): Promise<GolangSearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockGolangResults];

  // Filter by module path
  if (filters.module) {
    const m = filters.module.toLowerCase();
    filtered = filtered.filter((r) =>
      r.module.toLowerCase().includes(m)
    );
  }

  // Filter by version (exact match)
  if (filters.version) {
    filtered = filtered.filter((r) => r.latestVersion === filters.version);
  }

  // Filter by keyword
  if (filters.keyword) {
    const k = filters.keyword.toLowerCase();
    filtered = filtered.filter((r) =>
      r.module.toLowerCase().includes(k) ||
      (r.description && r.description.toLowerCase().includes(k))
    );
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates Go module detail API call.
 */
export async function mockGolangDetailApi(id: string): Promise<GolangDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockGolangDetail;
}


