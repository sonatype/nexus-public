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

import type { NuGetResult, NuGetSearchResponse, NuGetDetail, NuGetVersion } from './nuget.types';

/**
 * Mock NuGet package data for development.
 */

export const mockNuGetResults: NuGetResult[] = [
  {
    id: 'nuget:Newtonsoft.Json',
    packageId: 'Newtonsoft.Json',
    displayName: 'Newtonsoft.Json',
    latestVersion: '13.0.3',
    versionsCount: 89,
    repositoriesCount: 2,
    description: 'Json.NET is a popular high-performance JSON framework for .NET',
    authors: ['James Newton-King'],
    projectUrl: 'https://www.newtonsoft.com/json',
    license: 'MIT',
    tags: ['json', 'serialization'],
    lastUpdated: '2023-03-08T10:00:00Z',
    totalDownloads: 3200000000,
  },
  {
    id: 'nuget:Microsoft.Extensions.DependencyInjection',
    packageId: 'Microsoft.Extensions.DependencyInjection',
    displayName: 'Microsoft.Extensions.DependencyInjection',
    latestVersion: '8.0.0',
    versionsCount: 45,
    repositoriesCount: 1,
    description: 'Default implementation of dependency injection for Microsoft.Extensions.DependencyInjection',
    authors: ['Microsoft'],
    projectUrl: 'https://dot.net/',
    license: 'MIT',
    tags: ['di', 'dependency-injection', 'ioc'],
    lastUpdated: '2023-11-14T12:00:00Z',
    totalDownloads: 980000000,
  },
  {
    id: 'nuget:Serilog',
    packageId: 'Serilog',
    displayName: 'Serilog',
    latestVersion: '3.1.1',
    versionsCount: 67,
    repositoriesCount: 2,
    description: 'Simple .NET logging with fully-structured events',
    authors: ['Serilog Contributors'],
    projectUrl: 'https://serilog.net/',
    license: 'Apache-2.0',
    tags: ['logging', 'structured-logging', 'serilog'],
    lastUpdated: '2023-11-28T09:30:00Z',
    totalDownloads: 450000000,
  },
  {
    id: 'nuget:AutoMapper',
    packageId: 'AutoMapper',
    displayName: 'AutoMapper',
    latestVersion: '13.0.1',
    versionsCount: 78,
    repositoriesCount: 1,
    description: 'A convention-based object-object mapper',
    authors: ['Jimmy Bogard'],
    projectUrl: 'https://automapper.org/',
    license: 'MIT',
    tags: ['mapper', 'mapping', 'object-mapper'],
    lastUpdated: '2024-01-10T14:20:00Z',
    totalDownloads: 380000000,
  },
  {
    id: 'nuget:FluentValidation',
    packageId: 'FluentValidation',
    displayName: 'FluentValidation',
    latestVersion: '11.9.0',
    versionsCount: 56,
    repositoriesCount: 1,
    description: 'A validation library for .NET that uses a fluent interface to construct validation rules',
    authors: ['Jeremy Skinner'],
    projectUrl: 'https://fluentvalidation.net/',
    license: 'Apache-2.0',
    tags: ['validation', 'fluent', 'validators'],
    lastUpdated: '2023-12-05T11:00:00Z',
    totalDownloads: 290000000,
  },
  {
    id: 'nuget:Dapper',
    packageId: 'Dapper',
    displayName: 'Dapper',
    latestVersion: '2.1.28',
    versionsCount: 34,
    repositoriesCount: 2,
    description: 'A simple object mapper for .NET',
    authors: ['Sam Saffron', 'Marc Gravell', 'Nick Craver'],
    projectUrl: 'https://github.com/DapperLib/Dapper',
    license: 'Apache-2.0',
    tags: ['orm', 'sql', 'micro-orm', 'dapper'],
    lastUpdated: '2024-01-15T08:45:00Z',
    totalDownloads: 210000000,
  },
  {
    id: 'nuget:xunit',
    packageId: 'xunit',
    displayName: 'xunit',
    latestVersion: '2.6.6',
    versionsCount: 42,
    repositoriesCount: 1,
    description: 'xUnit.net is a developer testing framework',
    authors: ['James Newkirk', 'Brad Wilson'],
    projectUrl: 'https://xunit.net/',
    license: 'Apache-2.0',
    tags: ['testing', 'unit-testing', 'xunit'],
    lastUpdated: '2024-01-08T16:30:00Z',
    totalDownloads: 320000000,
  },
  {
    id: 'nuget:Moq',
    packageId: 'Moq',
    displayName: 'Moq',
    latestVersion: '4.20.70',
    versionsCount: 98,
    repositoriesCount: 1,
    description: 'The most popular mocking library for .NET',
    authors: ['Daniel Cazzulino'],
    projectUrl: 'https://github.com/moq/moq4',
    license: 'BSD-3-Clause',
    tags: ['mocking', 'testing', 'tdd', 'moq'],
    lastUpdated: '2023-12-18T10:00:00Z',
    totalDownloads: 185000000,
  },
  {
    id: 'nuget:Polly',
    packageId: 'Polly',
    displayName: 'Polly',
    latestVersion: '8.2.1',
    versionsCount: 45,
    repositoriesCount: 1,
    description: 'Polly is a .NET resilience and transient-fault-handling library',
    authors: ['Michael Wolfenden', 'App vNext'],
    projectUrl: 'https://github.com/App-vNext/Polly',
    license: 'BSD-3-Clause',
    tags: ['resilience', 'retry', 'circuit-breaker', 'fault-handling'],
    lastUpdated: '2024-01-20T09:15:00Z',
    totalDownloads: 165000000,
  },
  {
    id: 'nuget:MediatR',
    packageId: 'MediatR',
    displayName: 'MediatR',
    latestVersion: '12.2.0',
    versionsCount: 38,
    repositoriesCount: 1,
    description: 'Simple, unambitious mediator implementation in .NET',
    authors: ['Jimmy Bogard'],
    projectUrl: 'https://github.com/jbogard/MediatR',
    license: 'Apache-2.0',
    tags: ['mediator', 'cqrs', 'messaging'],
    lastUpdated: '2024-01-05T13:45:00Z',
    totalDownloads: 145000000,
  },
];

export const mockNuGetVersions: NuGetVersion[] = [
  {
    version: '13.0.3',
    isPrerelease: false,
    downloads: 450000000,
    published: '2023-03-08T10:00:00Z',
    targetFrameworks: ['net6.0', 'net7.0', 'net8.0', 'netstandard2.0', 'netstandard2.1'],
  },
  {
    version: '13.0.2',
    isPrerelease: false,
    downloads: 320000000,
    published: '2022-12-15T10:00:00Z',
    targetFrameworks: ['net6.0', 'net7.0', 'netstandard2.0', 'netstandard2.1'],
  },
  {
    version: '13.0.2-beta2',
    isPrerelease: true,
    downloads: 15000,
    published: '2022-11-01T10:00:00Z',
    targetFrameworks: ['net6.0', 'net7.0', 'netstandard2.0'],
  },
  {
    version: '13.0.1',
    isPrerelease: false,
    downloads: 280000000,
    published: '2021-06-09T10:00:00Z',
    targetFrameworks: ['net6.0', 'netstandard2.0', 'netstandard2.1'],
  },
  {
    version: '12.0.3',
    isPrerelease: false,
    downloads: 520000000,
    published: '2020-03-22T10:00:00Z',
    targetFrameworks: ['netstandard2.0', 'net45', 'net40'],
  },
];

export const mockNuGetDetail: NuGetDetail = {
  packageId: 'Newtonsoft.Json',
  displayName: 'Newtonsoft.Json',
  description: 'Json.NET is a popular high-performance JSON framework for .NET',
  authors: ['James Newton-King'],
  projectUrl: 'https://www.newtonsoft.com/json',
  licenseUrl: 'https://licenses.nuget.org/MIT',
  license: 'MIT',
  iconUrl: 'https://www.newtonsoft.com/content/images/nugeticon.png',
  tags: ['json', 'serialization', 'deserialize', 'serializer'],
  versions: mockNuGetVersions,
  repositories: ['nuget-proxy', 'nuget-hosted'],
};

/**
 * Simulates a NuGet search API call with mock data.
 */
export async function mockNuGetSearchApi(params: {
  query?: string;
  packageId?: string;
  version?: string;
  prerelease?: boolean;
  targetFramework?: string;
  continuationToken?: string;
  limit?: number;
}): Promise<NuGetSearchResponse> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockNuGetResults];

  // Filter by query
  if (params.query) {
    const q = params.query.toLowerCase();
    filtered = filtered.filter(
      (r) =>
        r.packageId.toLowerCase().includes(q) ||
        r.displayName.toLowerCase().includes(q) ||
        r.description?.toLowerCase().includes(q) ||
        r.tags?.some((t) => t.toLowerCase().includes(q))
    );
  }

  // Filter by packageId
  if (params.packageId) {
    const pid = params.packageId.toLowerCase();
    filtered = filtered.filter((r) =>
      r.packageId.toLowerCase().includes(pid)
    );
  }

  // Pagination
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
 * Get mock detail for a package.
 */
export async function mockNuGetDetailApi(packageId: string): Promise<NuGetDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));
  
  const result = mockNuGetResults.find((r) => r.packageId === packageId);
  
  if (result) {
    return {
      packageId: result.packageId,
      displayName: result.displayName,
      description: result.description,
      authors: result.authors ? [...result.authors] : undefined,
      projectUrl: result.projectUrl,
      license: result.license,
      tags: result.tags ? [...result.tags] : undefined,
      versions: mockNuGetVersions,
      repositories: ['nuget-proxy'],
    };
  }
  
  return mockNuGetDetail;
}

