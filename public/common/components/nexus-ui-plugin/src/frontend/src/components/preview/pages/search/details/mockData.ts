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

import type { GADetail, GAVersion, GARepository, GAAsset } from '../core';

const BASE_URL = 'http://localhost:8081/repository';

/**
 * Mock data for GA Detail views development.
 * Import types from ../core - do not duplicate.
 *
 * maven-core (maven2:org.apache.maven:maven-core) has full mock data for all tabs.
 */

// =============================================================================
// MAVEN-CORE - Full mock for Overview, Versions, Repositories, Files, Security
// =============================================================================

const MAVEN_CORE_VERSIONS: readonly GAVersion[] = [
  { version: '3.9.6', status: 'recommended', lastUpdated: '2024-01-15T10:00:00Z', repositories: ['maven-central', 'maven-releases'] },
  { version: '3.9.5', status: 'none', lastUpdated: '2023-11-20T09:00:00Z', repositories: ['maven-central'] },
  { version: '3.9.4', status: 'none', lastUpdated: '2023-08-15T14:00:00Z', repositories: ['maven-central'] },
  { version: '3.9.3', status: 'not-recommended', lastUpdated: '2023-05-01T11:00:00Z', repositories: ['maven-central'], statusReason: 'Security vulnerability CVE-2023-1234' },
  { version: '3.9.2', status: 'none', lastUpdated: '2023-02-10T08:00:00Z', repositories: ['maven-central'] },
  { version: '3.9.1', status: 'none', lastUpdated: '2022-11-15T12:00:00Z', repositories: ['maven-central'] },
  { version: '3.9.0', status: 'none', lastUpdated: '2022-08-01T10:00:00Z', repositories: ['maven-central'] },
  { version: '3.8.8', status: 'none', lastUpdated: '2022-05-20T09:00:00Z', repositories: ['maven-central'] },
  { version: '3.8.7', status: 'none', lastUpdated: '2022-02-14T14:00:00Z', repositories: ['maven-central'] },
  { version: '3.8.6', status: 'none', lastUpdated: '2021-11-01T11:00:00Z', repositories: ['maven-central'] },
];

const MAVEN_CORE_REPOSITORIES: readonly GARepository[] = [
  { name: 'maven-central', format: 'maven2', type: 'proxy', versionsCount: 10 },
  { name: 'maven-releases', format: 'maven2', type: 'hosted', versionsCount: 1 },
  { name: 'maven-public', format: 'maven2', type: 'group', versionsCount: 10 },
];

const MAVEN_CORE_ASSETS_3_9_6: readonly GAAsset[] = [
  {
    id: 'asset-maven-core-pom',
    repository: 'maven-central',
    path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.pom',
    downloadUrl: `${BASE_URL}/maven-central/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.pom`,
    format: 'pom',
    extension: 'pom',
    size: 45678,
    contentType: 'application/xml',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'a1b2c3d4e5f6', sha256: 'a1b2c3d4e5f6a1b2c3d4', md5: 'a1b2c3d4' },
  },
  {
    id: 'asset-maven-core-jar',
    repository: 'maven-central',
    path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.jar',
    downloadUrl: `${BASE_URL}/maven-central/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6.jar`,
    format: 'jar',
    extension: 'jar',
    size: 1234567,
    contentType: 'application/java-archive',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'b2c3d4e5f6a1', sha256: 'b2c3d4e5f6a1b2c3d4', md5: 'b2c3d4e5' },
  },
  {
    id: 'asset-maven-core-sources',
    repository: 'maven-central',
    path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-sources.jar',
    downloadUrl: `${BASE_URL}/maven-central/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-sources.jar`,
    format: 'jar',
    classifier: 'sources',
    extension: 'jar',
    size: 987654,
    contentType: 'application/java-archive',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'c3d4e5f6a1b2', sha256: 'c3d4e5f6a1b2c3d4', md5: 'c3d4e5f6' },
  },
  {
    id: 'asset-maven-core-javadoc',
    repository: 'maven-central',
    path: 'org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-javadoc.jar',
    downloadUrl: `${BASE_URL}/maven-central/org/apache/maven/maven-core/3.9.6/maven-core-3.9.6-javadoc.jar`,
    format: 'jar',
    classifier: 'javadoc',
    extension: 'jar',
    size: 2345678,
    contentType: 'application/java-archive',
    lastModified: '2024-01-15T10:00:00Z',
    checksums: { sha1: 'd4e5f6a1b2c3', sha256: 'd4e5f6a1b2c3d4', md5: 'd4e5f6a1' },
  },
];

const MAVEN_CORE_DETAIL: GADetail = {
  gaId: 'maven2:org.apache.maven:maven-core',
  format: 'maven',
  displayName: 'maven-core',
  description: 'Maven Core is the core of the Maven build system. It contains the default implementation for building Maven projects.',
  projectUrl: 'https://maven.apache.org/ref/current/maven-core/',
  license: 'Apache-2.0',
  repositories: MAVEN_CORE_REPOSITORIES,
  versions: MAVEN_CORE_VERSIONS,
};

// =============================================================================
// FALLBACK - Generic mock for other components
// =============================================================================

export const mockVersions: readonly GAVersion[] = MAVEN_CORE_VERSIONS;
export const mockRepositories: readonly GARepository[] = MAVEN_CORE_REPOSITORIES;
export const mockDetail: GADetail = MAVEN_CORE_DETAIL;
export const mockAssets: readonly GAAsset[] = MAVEN_CORE_ASSETS_3_9_6;

/**
 * Check if gaId refers to maven-core (handles maven/maven2 format).
 */
function isMavenCore(gaId: string): boolean {
  const normalized = gaId.replace(/^maven2?:/, 'maven:');
  return normalized === 'maven:org.apache.maven:maven-core';
}

/**
 * Simulate API delay for mock data.
 */
export function withDelay<T>(data: T, delayMs = 300): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), delayMs);
  });
}

/**
 * Get mock detail data.
 * Returns full maven-core mock when gaId matches; otherwise generic mock.
 */
export function getMockDetail(gaId: string): GADetail {
  if (isMavenCore(gaId)) {
    return { ...MAVEN_CORE_DETAIL, gaId };
  }
  return {
    ...MAVEN_CORE_DETAIL,
    gaId,
    displayName: gaId.split(':').pop() || 'unknown',
  };
}

/**
 * Get mock assets for a version.
 * Returns maven-core 3.9.6 assets when gaId matches; otherwise generic assets.
 */
export function getMockAssets(gaId: string, version: string): readonly GAAsset[] {
  if (isMavenCore(gaId) && version === '3.9.6') {
    return MAVEN_CORE_ASSETS_3_9_6;
  }
  if (isMavenCore(gaId)) {
    return MAVEN_CORE_ASSETS_3_9_6.map((a) => ({
      ...a,
      id: `${a.id}-${version}`,
      path: a.path.replace(/3\.9\.6/g, version),
      downloadUrl: a.downloadUrl.replace(/3\.9\.6/g, version),
    }));
  }
  return mockAssets;
}

