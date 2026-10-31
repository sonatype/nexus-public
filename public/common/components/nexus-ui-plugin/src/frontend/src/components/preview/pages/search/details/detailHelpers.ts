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

import { BROWSE_ROUTE_STATES } from '../../browse/browse.routes';

/**
 * Parse a gaId into its `format`, `group`, and `name` segments.
 *
 * A gaId is `format:group:name`, `format:name`, or a bare token. Any colons beyond the
 * group separator stay part of the name, so ids whose name legitimately contains a colon
 * are preserved rather than truncated.
 */
export function parseGaCoordinates(gaId: string): { format: string; group: string; name: string } {
  const parts = gaId.split(':');
  if (parts.length >= 3) {
    return { format: parts[0], group: parts[1], name: parts.slice(2).join(':') };
  }
  if (parts.length === 2) {
    return { format: parts[0], group: '', name: parts[1] };
  }
  return { format: '', group: '', name: gaId };
}

/**
 * Build a Package URL (purl) from gaId and version.
 * Format: pkg:{format}/{pkgName}@{version}
 * Example: pkg:maven/org.apache.commons:commons-lang3@3.14.0
 */
export function buildPurl(gaId: string, version: string): string {
  const parts = gaId.split(':');
  if (parts.length < 2) return '';
  const format = parts[0];
  const pkgName = parts.slice(1).join(':');
  return `pkg:${format}/${pkgName}@${version}`;
}

/**
 * Build Maven repository path from gaId and version.
 * Format: {groupId}/{artifactId}/{version} with dots in groupId replaced by slashes.
 * Example: org/apache/commons/commons-lang3/3.14.0
 */
export function buildMavenPath(gaId: string, version: string): string | null {
  const parts = gaId.split(':');
  if (parts.length < 3) return null;
  const [, groupId, artifactId] = parts;
  return `${groupId.replace(/\./g, '/')}/${artifactId}/${version}`;
}

/**
 * Build npm package path.
 * gaId: npm:name or npm:scope:name
 * Path: @scope/name or name (registry path format)
 */
function buildNpmPath(gaId: string, version: string): string | null {
  const parts = gaId.split(':');
  if (parts.length < 2) return null;
  const format = parts[0]?.toLowerCase();
  if (format !== 'npm') return null;
  // A scoped package arrives as npm:scope:name because npm stores the scope as the component
  // namespace with the leading @ stripped. Restore it as @scope/name so the path matches the
  // real Browse node; joining with ':' (or dropping the @) would target a node that doesn't
  // exist, and a non-null wrong path defeats buildBrowseTarget's repository-root fallback.
  if (parts.length >= 3) {
    return `@${parts[1]}/${parts.slice(2).join(':')}/${version}`;
  }
  const name = parts[1];
  if (!name) return null;
  return `${name}/${version}`;
}

/**
 * Build PyPI package path.
 * gaId: pypi:name
 * Path: name/version/
 */
function buildPypiPath(gaId: string, version: string): string | null {
  const parts = gaId.split(':');
  if (parts.length < 2) return null;
  const format = parts[0]?.toLowerCase();
  if (format !== 'pypi') return null;
  const name = parts[1];
  if (!name) return null;
  return `${name}/${version}`;
}

/**
 * Build NuGet package path.
 * gaId: nuget:packageId
 * Path: packageId/version/
 */
function buildNugetPath(gaId: string, version: string): string | null {
  const parts = gaId.split(':');
  if (parts.length < 2) return null;
  const format = parts[0]?.toLowerCase();
  if (format !== 'nuget') return null;
  const packageId = parts[1];
  if (!packageId) return null;
  return `${packageId}/${version}`;
}

/**
 * Build component path for formats that support it.
 * Returns the repository/registry path for copying.
 * Supported: maven, maven2, npm, pypi, nuget.
 */
export function buildComponentPath(gaId: string, version: string): string | null {
  const parts = gaId.split(':');
  if (parts.length < 2 || !version) return null;
  const format = parts[0]?.toLowerCase();

  switch (format) {
    case 'maven':
    case 'maven2':
      return buildMavenPath(gaId, version);
    case 'npm':
      return buildNpmPath(gaId, version);
    case 'pypi':
      return buildPypiPath(gaId, version);
    case 'nuget':
      return buildNugetPath(gaId, version);
    default:
      return null;
  }
}

/**
 * Get Maven Central (central.sonatype.com) URL for a Maven component.
 * Returns null for non-Maven formats.
 */
export function getMavenCentralUrl(gaId: string): string | null {
  const parts = gaId.split(':');
  const format = parts[0]?.toLowerCase();
  if (parts.length < 3 || (format !== 'maven' && format !== 'maven2')) return null;
  const groupId = parts[1];
  const artifactId = parts[2];
  return `https://central.sonatype.com/artifact/${encodeURIComponent(groupId)}/${encodeURIComponent(artifactId)}`;
}

// =============================================================================
// BROWSE DEEP-LINKING (NEXUS-54430)
// =============================================================================

// Route-state names come from the Browse feature's single source of truth
// ({@link BROWSE_ROUTE_STATES}) rather than being re-declared here — the two must stay in lockstep
// for the deep link to resolve, and a duplicated literal would drift silently on a rename.

/** UI-Router state for the Preview/Nexus One Browse tree at a repository root. */
export const PREVIEW_BROWSE_REPO_STATE = BROWSE_ROUTE_STATES.repo;
/** UI-Router state for a specific node (folder/asset) within the Browse tree. */
export const PREVIEW_BROWSE_REPO_PATH_STATE = BROWSE_ROUTE_STATES.path;

/** A router navigation target: the state name plus its params. */
export interface BrowseTarget {
  readonly state: string;
  readonly params: Record<string, string>;
}

/**
 * Build the router target for the new Preview / Nexus One Browse experience.
 *
 * Deliberately returns a UI-Router state + params rather than a hash string: the same state
 * (`preview.browse.browse.*`) is registered in every edition, and the router resolves it to the
 * edition's own URL — `#preview/browse/...` on Cloud, `#browse/...` on Self-Hosted/CE. Building a
 * hash by hand would hard-code one edition's prefix and, as before this ticket (NEXUS-54430),
 * drop the user out of the new UI into the Classic UI.
 *
 * `path` is the browse-node path, which equals an asset's `path` and a component's registry path
 * (see {@link buildComponentPath}). When absent — an unsupported format, or a versionless
 * component with no computable path — the target is the repository root instead.
 *
 * The incoming `path` is a *decoded* content path, but the Browse route param is `raw` and
 * `BrowsePage` decodes it once (matching the encoded node ids the tree emits). So the segments are
 * percent-encoded here — separators kept as `/` — to align with that contract: without it a name
 * containing a bare `%` (e.g. `100%.txt`) throws on decode and blanks the page, and one containing
 * a valid escape (e.g. a literal `report%20final.pdf`) decodes to a different path that the tree
 * never matches.
 *
 * Returns null when there is no repository to browse to, so callers can hide the affordance.
 */
export function buildBrowseTarget(
  repository: string,
  path?: string | null,
): BrowseTarget | null {
  if (!repository) return null;
  if (path) {
    const encodedPath = path.split('/').map(encodeURIComponent).join('/');
    return {
      state: PREVIEW_BROWSE_REPO_PATH_STATE,
      params: { repoName: repository, path: encodedPath, tab: 'summary' },
    };
  }
  return {
    state: PREVIEW_BROWSE_REPO_STATE,
    params: { repoName: repository },
  };
}
