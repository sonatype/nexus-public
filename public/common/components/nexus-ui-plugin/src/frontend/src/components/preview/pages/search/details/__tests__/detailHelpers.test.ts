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
  buildComponentPath,
  buildMavenPath,
  buildPurl,
  getMavenCentralUrl,
  parseGaCoordinates,
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
