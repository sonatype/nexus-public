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
 * Derives the "Type" shown for an asset in the component detail Files tab.
 *
 * Both asset mappers used to do `path.split('.').pop()`, which is wrong for every format
 * whose asset path carries no extension: the last dot then lands inside a version or a
 * directory. NuGet packages are stored at `/{id}/{version}` (NugetContentFacetImpl), so
 * `/System.Net.Http/2.0.20126.16343` rendered as the file type "16343" — NEXUS-54612.
 *
 * The path is authoritative when it actually carries an extension; otherwise the
 * repository format supplies the canonical type for its artifact.
 */

/** Shown when neither the path nor the format can name a type. An em dash, not an empty cell. */
export const UNKNOWN_FILE_TYPE = '—';

/**
 * Longest trailing segment still treated as an extension. Real extensions are short
 * ('nupkg' is 5, 'symbols' never appears alone); anything longer is a word from the
 * filename that happens to follow a dot.
 */
const MAX_EXTENSION_LENGTH = 10;

/**
 * A candidate extension: alphanumeric, and containing at least one letter so version
 * fragments ('16343', '219', '0') are rejected while real digit-leading extensions
 * ('7z') are kept. Punctuation is excluded, which is what rejects semver prereleases
 * such as '0-beta1'.
 */
const EXTENSION_PATTERN = new RegExp(`^(?=.*[a-z])[a-z0-9]{1,${MAX_EXTENSION_LENGTH}}$`, 'i');

/**
 * Canonical type per format, used only when the path carries no usable extension.
 *
 * Deliberately covers just the formats whose extensionless asset paths were confirmed to
 * misrender (NEXUS-54612); every other format either carries a real extension or falls
 * through to UNKNOWN_FILE_TYPE rather than to a guess.
 */
const FORMAT_FILE_TYPES: Readonly<Record<string, string>> = {
  // Regular packages live at /{id}/{version} with the .nupkg implied.
  nuget: 'nupkg',
  // Crates are served from /crates/{name}/{version}/download.
  cargo: 'crate',
  // /v1/modules/{namespace}/{name}/{provider}/{version}
  terraform: 'module',
  // Version-list and @latest metadata assets carry no extension; the .zip/.info/.mod
  // assets do, so they never reach here.
  go: 'metadata',
};

/**
 * Formats whose assets are addressed by the OCI distribution path shape rather than by
 * filename. Both emit the same two segments — docker via DockerFacetUtils#blobPath, OCI via
 * OciFacetUtils#MANIFESTS_SEGMENT / #BLOBS_SEGMENT.
 */
const CONTAINER_FORMATS: ReadonlySet<string> = new Set(['docker', 'oci']);

/**
 * Manifest or blob, read from the path shape: `/v2/{name}/manifests/{reference}` and
 * `/v2/{name}/blobs/{digest}`. Null when the path is neither, leaving the caller to fall
 * back to the ordinary extension handling.
 */
function containerFileType(path: string): string | null {
  if (path.includes('/manifests/')) {
    return 'manifest';
  }
  if (path.includes('/blobs/')) {
    return 'blob';
  }
  return null;
}

/** Last non-empty path segment. Trailing slashes are stripped: terraform emits them. */
function fileNameOf(path: string): string {
  const segments = path.split('/').filter((segment) => segment !== '');
  return segments.length > 0 ? segments[segments.length - 1] : '';
}

/**
 * Extension of a filename, or null when it has none worth showing.
 *
 * Scoped to the filename rather than the whole path, so a dotted directory
 * ('github.com/...') cannot supply an extension the file does not have. A leading dot is
 * not a separator: '.npmignore' is a name, not an extension.
 */
function extensionOf(fileName: string): string | null {
  const lastDot = fileName.lastIndexOf('.');
  if (lastDot <= 0) {
    return null;
  }
  const candidate = fileName.slice(lastDot + 1);
  return EXTENSION_PATTERN.test(candidate) ? candidate.toLowerCase() : null;
}

/**
 * The file type to display for one asset.
 *
 * @param path the asset's full repository path
 * @param format the asset's repository format ('nuget', 'maven2', …). Optional: callers
 *   read it straight off the API response, where it can be absent.
 */
export function deriveFileType({
  path,
  format,
}: {
  readonly path: string;
  readonly format?: string | null;
}): string {
  const safePath = path || '';
  const normalisedFormat = (format || '').toLowerCase();

  // Container paths are resolved from their shape *before* any extension is considered: a
  // docker tag may contain dots (DockerFacetUtils#VALID_TAG is `^[\w][\w.-]{0,127}$`), so
  // '/v2/foo/manifests/1.0.0.RELEASE' would otherwise render 'release' — the same defect
  // class this helper exists to fix.
  if (CONTAINER_FORMATS.has(normalisedFormat)) {
    const containerType = containerFileType(safePath);
    if (containerType) {
      return containerType;
    }
  }

  const extension = extensionOf(fileNameOf(safePath));
  if (extension) {
    return extension;
  }

  return FORMAT_FILE_TYPES[normalisedFormat] || UNKNOWN_FILE_TYPE;
}

export default deriveFileType;
