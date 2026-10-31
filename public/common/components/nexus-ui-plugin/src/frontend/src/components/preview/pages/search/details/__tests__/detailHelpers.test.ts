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

import {
  buildBrowseTarget,
  buildComponentPath,
  buildMavenPath,
  buildPurl,
  getMavenCentralUrl,
  parseGaCoordinates,
  PREVIEW_BROWSE_REPO_PATH_STATE,
  PREVIEW_BROWSE_REPO_STATE,
} from '../detailHelpers';

describe('parseGaCoordinates', () => {
  it('splits a full format:group:name id into its three parts', () => {
    expect(parseGaCoordinates('maven:org.apache.commons:commons-lang3')).toEqual({
      format: 'maven',
      group: 'org.apache.commons',
      name: 'commons-lang3',
    });
  });

  it('keeps the whole remainder as the name when it contains extra colons', () => {
    expect(parseGaCoordinates('maven:com.esd:my:weird:name')).toEqual({
      format: 'maven',
      group: 'com.esd',
      name: 'my:weird:name',
    });
  });

  it('treats a two-part id as format + name with no group', () => {
    expect(parseGaCoordinates('npm:lodash')).toEqual({ format: 'npm', group: '', name: 'lodash' });
  });

  it('treats a single-token id as the name with no format or group', () => {
    expect(parseGaCoordinates('lodash')).toEqual({ format: '', group: '', name: 'lodash' });
  });
});

describe('buildPurl', () => {
  it('builds a purl keeping the full package name (including colons) after the format', () => {
    expect(buildPurl('maven:org.apache.commons:commons-lang3', '3.14.0')).toBe(
      'pkg:maven/org.apache.commons:commons-lang3@3.14.0',
    );
  });

  it('builds a purl for a format:name id with no group', () => {
    expect(buildPurl('npm:lodash', '4.17.21')).toBe('pkg:npm/lodash@4.17.21');
  });

  it('returns an empty string when the id has no format separator', () => {
    expect(buildPurl('lodash', '1.0.0')).toBe('');
  });
});

describe('buildMavenPath', () => {
  it('turns the group dots into slashes and appends artifact + version', () => {
    expect(buildMavenPath('maven:org.apache.commons:commons-lang3', '3.14.0')).toBe(
      'org/apache/commons/commons-lang3/3.14.0',
    );
  });

  it('returns null when the id lacks a group segment', () => {
    expect(buildMavenPath('npm:lodash', '1.0.0')).toBeNull();
  });
});

describe('buildComponentPath', () => {
  it('builds a Maven repository path for maven and maven2', () => {
    expect(buildComponentPath('maven:org.apache.commons:commons-lang3', '3.14.0')).toBe(
      'org/apache/commons/commons-lang3/3.14.0',
    );
    expect(buildComponentPath('maven2:org.apache.commons:commons-lang3', '3.14.0')).toBe(
      'org/apache/commons/commons-lang3/3.14.0',
    );
  });

  it('builds npm, pypi and nuget registry paths', () => {
    expect(buildComponentPath('npm:lodash', '4.17.21')).toBe('lodash/4.17.21');
    expect(buildComponentPath('pypi:requests', '2.31.0')).toBe('requests/2.31.0');
    expect(buildComponentPath('nuget:Newtonsoft.Json', '13.0.3')).toBe('Newtonsoft.Json/13.0.3');
  });

  it('restores the @scope/name form for a scoped npm package', () => {
    // A scoped package's gaId is npm:scope:name (npm strips the leading @ from the namespace), so
    // the path must be rebuilt as @scope/name/version — not scope:name/version, which points at a
    // Browse node that does not exist.
    expect(buildComponentPath('npm:babel:core', '7.0.0')).toBe('@babel/core/7.0.0');
  });

  it('returns null for an unsupported format', () => {
    expect(buildComponentPath('docker:library/nginx', '1.25')).toBeNull();
  });

  it('returns null when the version is missing', () => {
    expect(buildComponentPath('npm:lodash', '')).toBeNull();
  });
});

describe('getMavenCentralUrl', () => {
  const expected =
    'https://central.sonatype.com/artifact/org.apache.commons/commons-lang3';

  it('builds the Maven Central URL for the "maven" format', () => {
    expect(getMavenCentralUrl('maven:org.apache.commons:commons-lang3')).toBe(expected);
  });

  it('builds the Maven Central URL for the "maven2" format', () => {
    expect(getMavenCentralUrl('maven2:org.apache.commons:commons-lang3')).toBe(expected);
  });

  it('is case-insensitive on the format', () => {
    expect(getMavenCentralUrl('MAVEN2:org.apache.commons:commons-lang3')).toBe(expected);
  });

  it('returns null for non-Maven formats', () => {
    expect(getMavenCentralUrl('npm:lodash')).toBeNull();
    expect(getMavenCentralUrl('pypi:requests')).toBeNull();
  });
});

describe('buildBrowseTarget', () => {
  it('targets the new Preview Browse node state when a path is given', () => {
    expect(
      buildBrowseTarget(
        'maven-central',
        'org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
      ),
    ).toEqual({
      state: PREVIEW_BROWSE_REPO_PATH_STATE,
      params: {
        repoName: 'maven-central',
        path: 'org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
        tab: 'summary',
      },
    });
  });

  it('percent-encodes each path segment so a decode-once route round-trips it', () => {
    // BrowsePage decodes the raw route param once, so the target must carry the encoded form.
    // A bare % (100%.txt) would otherwise throw on decode; a literal %20 would decode to a
    // different path. Separators stay as '/', only the segments are encoded.
    expect(buildBrowseTarget('raw-hosted', 'dir/100%.txt')).toEqual({
      state: PREVIEW_BROWSE_REPO_PATH_STATE,
      params: { repoName: 'raw-hosted', path: 'dir/100%25.txt', tab: 'summary' },
    });
    expect(buildBrowseTarget('raw-hosted', 'a/report%20final.pdf')?.params.path).toBe(
      'a/report%2520final.pdf',
    );
  });

  it('targets the repository root when no path is given', () => {
    expect(buildBrowseTarget('raw-hosted')).toEqual({
      state: PREVIEW_BROWSE_REPO_STATE,
      params: { repoName: 'raw-hosted' },
    });
    // An empty path is treated as no path, not as a node under the repo.
    expect(buildBrowseTarget('raw-hosted', '')).toEqual({
      state: PREVIEW_BROWSE_REPO_STATE,
      params: { repoName: 'raw-hosted' },
    });
  });

  it('never targets the Classic UI route', () => {
    const target = buildBrowseTarget('maven-central', 'org/foo');
    expect(target?.state.startsWith('preview.browse')).toBe(true);
  });

  it('returns null when there is no repository to browse to', () => {
    expect(buildBrowseTarget('')).toBeNull();
    expect(buildBrowseTarget('', 'org/foo')).toBeNull();
  });
});
