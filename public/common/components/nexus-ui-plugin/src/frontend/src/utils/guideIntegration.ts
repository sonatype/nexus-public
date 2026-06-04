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
 * Guide Deep Research Integration (y87u)
 *
 * Utilities for building Guide component URLs for deep research.
 * Guide provides detailed component intelligence including security advisories,
 * license info, usage statistics, and ecosystem-specific metadata.
 */

const GUIDE_BASE_URL = 'https://guide.sonatype.com';

/**
 * Ecosystem name mapping from Nexus format to Guide ecosystem identifier.
 */
const ECOSYSTEM_MAP: Record<string, string> = {
  maven2: 'maven',
  /** GA search / browse paths sometimes use `maven` instead of `maven2`. */
  maven: 'maven',
  npm: 'npm',
  nuget: 'nuget',
  pypi: 'pypi',
  rubygems: 'gem',
  golang: 'golang',
  cargo: 'cargo',
  cocoapods: 'cocoapods',
  composer: 'composer',
  conan: 'conan',
};

/**
 * Build a Guide component URL for deep research.
 *
 * @param ecosystem - Nexus repository format (e.g., 'npm', 'maven2', 'pypi')
 * @param packageName - Component package name (e.g., 'lodash', 'org.apache.commons:commons-lang3')
 * @param version - Component version (e.g., '4.17.21')
 * @returns Guide URL or null if ecosystem is unsupported
 *
 * @example
 * buildGuideComponentUrl('npm', 'lodash', '4.17.21')
 * // => 'https://guide.sonatype.com/component/npm/lodash/4.17.21'
 *
 * buildGuideComponentUrl('maven2', 'org.apache.commons:commons-lang3', '3.12.0')
 * // => 'https://guide.sonatype.com/component/maven/org.apache.commons%3Acommons-lang3/3.12.0'
 *
 * buildGuideComponentUrl('docker', 'nginx', 'latest')
 * // => null (docker not supported)
 */
/**
 * Derive the Guide "package" path segment from a Nexus GA id (`format:…` coordinates).
 *
 * Matches npm path / purl rules: npm scoped ids are `npm:@scope:name`
 * (three colon segments), which map to `@scope/name` for Guide — not `@scope:name`.
 */
export function guidePackageNameFromGaId(gaId: string): string | null {
  const parts = gaId.split(':');
  if (parts.length < 2) return null;
  const format = parts[0].toLowerCase();
  if (format === 'npm') {
    if (parts.length === 2) return parts[1] || null;
    if (parts.length >= 3 && parts[1].startsWith('@')) {
      return `${parts[1]}/${parts.slice(2).join('/')}`;
    }
  }
  const rest = parts.slice(1).join(':');
  return rest || null;
}

/**
 * Build a Guide component URL from a Nexus GA id (`format:group:name` or `format:name`)
 * and version.
 */
export function buildGuideComponentUrlFromGaId(
  gaId: string,
  version: string,
  referrer?: string
): string | null {
  const parts = gaId.split(':');
  if (parts.length < 2 || !version) return null;
  const ecosystem = parts[0].toLowerCase();
  const packageName = guidePackageNameFromGaId(gaId);
  if (!packageName) return null;
  return buildGuideComponentUrl(ecosystem, packageName, version, referrer);
}

export function buildGuideComponentUrl(
  ecosystem: string,
  packageName: string,
  version: string,
  referrer?: string
): string | null {
  const guideEcosystem = ECOSYSTEM_MAP[ecosystem.toLowerCase()];
  if (!guideEcosystem) {
    return null;
  }

  const encodedPackage = encodeURIComponent(packageName);
  const encodedVersion = encodeURIComponent(version);

  let url = `${GUIDE_BASE_URL}/component/${guideEcosystem}/${encodedPackage}/${encodedVersion}`;
  if (referrer) {
    url += `?referrer=${encodeURIComponent(referrer)}`;
  }
  return url;
}

/**
 * Check if Guide integration is available for a given ecosystem.
 */
export function isGuideSupported(ecosystem: string): boolean {
  return ecosystem.toLowerCase() in ECOSYSTEM_MAP;
}

/** True when {@link buildGuideComponentUrlFromGaId} can produce a URL for this gaId. */
export function isGuideSupportedForGaId(gaId: string): boolean {
  const parts = gaId.split(':');
  if (parts.length < 2) return false;
  return isGuideSupported(parts[0]);
}

/**
 * Extract package and version from Maven coordinates.
 * Maven coordinates use format: groupId:artifactId
 *
 * @example
 * parseMavenCoordinates('org.apache.commons:commons-lang3')
 * // => { group: 'org.apache.commons', artifact: 'commons-lang3', combined: 'org.apache.commons:commons-lang3' }
 */
export function parseMavenCoordinates(coords: string): {
  group: string;
  artifact: string;
  combined: string;
} | null {
  const parts = coords.split(':');
  if (parts.length !== 2) return null;
  return {
    group: parts[0],
    artifact: parts[1],
    combined: coords,
  };
}
