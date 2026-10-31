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

import { deriveFileType, UNKNOWN_FILE_TYPE } from '../fileType';

describe('deriveFileType', () => {
  describe('paths carrying a real extension (BDD-002, BDD-006)', () => {
    it.each([
      ['maven jar', 'org/sonatype/test/foo/1.0/foo-1.0.jar', 'maven2', 'jar'],
      ['maven pom', 'org/sonatype/test/foo/1.0/foo-1.0.pom', 'maven2', 'pom'],
      ['npm tarball', 'foo/-/foo-1.0.0.tgz', 'npm', 'tgz'],
      ['scoped npm tarball', '@scope/foo/-/foo-1.0.0.tgz', 'npm', 'tgz'],
      ['pypi wheel', 'packages/f/foo/foo-1.0-py3-none-any.whl', 'pypi', 'whl'],
      ['yum rpm', 'packages/foo-1.0-1.x86_64.rpm', 'yum', 'rpm'],
      ['uppercase extension normalised', 'foo/1.0/FOO-1.0.JAR', 'maven2', 'jar'],
      // Compound extensions show their last segment only — the behaviour paths with real
      // extensions already had, so no currently-correct row changes.
      ['tarball keeps last segment', 'downloads/foo-1.0.tar.gz', 'raw', 'gz'],
      ['nuget symbol package', '/Foo/1.0.0/Foo.1.0.0.symbols.nupkg', 'nuget', 'nupkg'],
      // A real extension wins over the format label: the path is authoritative when it has one.
      ['nuget proxy content path', '/v3/content/foo/1.0.0/foo.1.0.0.nupkg', 'nuget', 'nupkg'],
      ['go module zip', 'github.com/foo/bar/@v/v1.2.3.zip', 'go', 'zip'],
      ['go module info', 'github.com/foo/bar/@v/v1.2.3.info', 'go', 'info'],
    ])('%s', (_label, path, format, expected) => {
      expect(deriveFileType({ path, format })).toBe(expected);
    });
  });

  describe('extensionless paths fall back to a format label (BDD-001, BDD-003, BDD-004)', () => {
    it.each([
      // The reported defect: the last dot lands inside the version, so the naive split
      // produced "16343". NEXUS-54612.
      ['nuget package', '/System.Net.Http/2.0.20126.16343', 'nuget', 'nupkg'],
      ['nuget package, no dots at all', '/Foo/1', 'nuget', 'nupkg'],
      ['cargo crate', '/crates/serde/1.0.219/download', 'cargo', 'crate'],
      ['docker manifest', '/v2/foo/manifests/sha256:abc123', 'docker', 'manifest'],
      ['docker manifest by tag', '/v2/foo/manifests/latest', 'docker', 'manifest'],
      ['docker blob', '/v2/foo/blobs/sha256:abc123', 'docker', 'blob'],
      // OCI emits the same two segments (OciFacetUtils#MANIFESTS_SEGMENT / #BLOBS_SEGMENT).
      ['oci manifest', '/v2/foo/manifests/sha256:abc123', 'oci', 'manifest'],
      ['oci manifest by tag', '/v2/foo/manifests/latest', 'oci', 'manifest'],
      ['oci shared blob', '/v2/-/blobs/sha256:abc123', 'oci', 'blob'],
      ['terraform module', '/v1/modules/ns/name/aws/1.0.0', 'terraform', 'module'],
      ['terraform module, trailing slash', '/v1/modules/ns/name/aws/1.0.0/', 'terraform', 'module'],
      ['go version list', 'github.com/foo/bar/@v/list', 'go', 'metadata'],
      ['go latest', 'github.com/foo/bar/@latest', 'go', 'metadata'],
    ])('%s', (_label, path, format, expected) => {
      expect(deriveFileType({ path, format })).toBe(expected);
    });
  });

  describe('container paths outrank a dotted reference', () => {
    // DockerFacetUtils#VALID_TAG is `^[\w][\w.-]{0,127}$`, so a tag may end in a
    // letter-bearing chunk after a dot. Deriving the extension first turned those into
    // 'release' / 'final' — the shape of the path has to win for these formats.
    it.each([
      ['docker tag ending in .RELEASE', '/v2/foo/manifests/1.0.0.RELEASE', 'docker', 'manifest'],
      ['oci tag ending in .RELEASE', '/v2/foo/manifests/1.0.0.RELEASE', 'oci', 'manifest'],
      ['docker tag ending in .final', '/v2/foo/manifests/v2.1.final', 'docker', 'manifest'],
      ['docker blob path with a dotted digest', '/v2/foo/blobs/sha256:ab.cd', 'docker', 'blob'],
    ])('%s', (_label, path, format, expected) => {
      expect(deriveFileType({ path, format })).toBe(expected);
    });

    it('still uses the extension for a container path that is neither manifest nor blob', () => {
      expect(deriveFileType({ path: '/v2/foo/other/bar.json', format: 'docker' })).toBe('json');
    });

    it.each([['docker'], ['oci']])(
      '%s falls through to the unknown type when the path has no usable shape or extension',
      (format) => {
        expect(deriveFileType({ path: '/v2/foo/tags/list', format })).toBe(UNKNOWN_FILE_TYPE);
      },
    );

    it('matches the container formats case-insensitively', () => {
      expect(deriveFileType({ path: '/v2/foo/manifests/1.0.0.RELEASE', format: 'OCI' })).toBe(
        'manifest',
      );
    });
  });

  describe('no valid extension and no format label (BDD-005)', () => {
    it.each([
      ['extensionless raw file', 'docs/README', 'raw'],
      ['dotfile', 'config/.npmignore', 'raw'],
      ['numeric trailing segment', 'foo/bar/1.2.3', 'raw'],
      ['digest-style segment', 'objects/sha256:abc123', 'gitlfs'],
      ['empty path', '', 'raw'],
      ['path is only slashes', '///', 'raw'],
      ['unknown format, extensionless', '/some/opaque/path', 'somethingnew'],
    ])('%s', (_label, path, format) => {
      expect(deriveFileType({ path, format })).toBe(UNKNOWN_FILE_TYPE);
    });
  });

  describe('extension validation rejects non-extension trailing segments', () => {
    it('rejects a purely numeric segment', () => {
      expect(deriveFileType({ path: 'foo/2.0.20126.16343', format: 'raw' })).toBe(
        UNKNOWN_FILE_TYPE,
      );
    });

    it('accepts a digit-leading extension that contains a letter', () => {
      // '7z' is a real extension; the guard rejects segments with no letters at all rather
      // than segments that merely start with a digit.
      expect(deriveFileType({ path: 'downloads/bar.7z', format: 'raw' })).toBe('7z');
    });

    it('rejects a segment containing no letters', () => {
      expect(deriveFileType({ path: 'foo/bar.123', format: 'raw' })).toBe(UNKNOWN_FILE_TYPE);
    });

    it('rejects a segment with punctuation', () => {
      expect(deriveFileType({ path: 'foo/bar.1-0', format: 'raw' })).toBe(UNKNOWN_FILE_TYPE);
    });

    it('rejects an implausibly long segment', () => {
      expect(deriveFileType({ path: 'foo/bar.thisisnotanextension', format: 'raw' })).toBe(
        UNKNOWN_FILE_TYPE,
      );
    });

    it('scopes the search to the filename, never the directories', () => {
      // The old code split the whole path, so a dotted directory poisoned an extensionless
      // filename. Scoping to the last segment is what makes cargo's "download" resolvable.
      expect(deriveFileType({ path: 'github.com/foo/bar/download', format: 'cargo' })).toBe(
        'crate',
      );
    });

    it('never falls back to jar', () => {
      // componentVersionDetailApi used to default to 'jar', mislabelling every extensionless
      // asset in every non-maven format.
      expect(deriveFileType({ path: '/Foo/1.0.0', format: 'raw' })).not.toBe('jar');
    });
  });

  describe('tolerates a missing format', () => {
    it.each([
      ['undefined format with a real extension', 'foo/bar.jar', undefined, 'jar'],
      ['null format with a real extension', 'foo/bar.jar', null, 'jar'],
      ['empty format with a real extension', 'foo/bar.jar', '', 'jar'],
    ])('%s', (_label, path, format, expected) => {
      expect(deriveFileType({ path, format })).toBe(expected);
    });

    it('returns the unknown marker when neither path nor format can answer', () => {
      expect(deriveFileType({ path: 'foo/bar', format: undefined })).toBe(UNKNOWN_FILE_TYPE);
    });
  });

  describe('format matching is case-insensitive', () => {
    it.each([
      ['upper', 'NuGet'],
      ['mixed', 'NUGET'],
    ])('%s', (_label, format) => {
      expect(deriveFileType({ path: '/Foo/1.0.0', format })).toBe('nupkg');
    });
  });
});
