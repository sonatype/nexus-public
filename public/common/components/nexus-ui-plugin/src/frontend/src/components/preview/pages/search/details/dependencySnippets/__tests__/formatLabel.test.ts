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

import { getFormatLabel } from '../formatLabel';

describe('getFormatLabel', () => {
  it('maps the raw "maven" format to the "Maven" label', () => {
    expect(getFormatLabel('maven')).toBe('Maven');
  });

  it('maps the "maven2" registry key to the "Maven" label', () => {
    expect(getFormatLabel('maven2')).toBe('Maven');
  });

  it('returns the shared human-readable labels for known formats', () => {
    expect(getFormatLabel('npm')).toBe('npm');
    expect(getFormatLabel('nuget')).toBe('NuGet');
    expect(getFormatLabel('pypi')).toBe('PyPI');
    expect(getFormatLabel('oci')).toBe('OCI');
    expect(getFormatLabel('rubygems')).toBe('RubyGems');
  });

  it('is case-insensitive, resolving aliases and labels for upper-case formats', () => {
    expect(getFormatLabel('Maven')).toBe('Maven');
    expect(getFormatLabel('MAVEN')).toBe('Maven');
    expect(getFormatLabel('NPM')).toBe('npm');
  });

  it('falls back to the raw format id when unknown', () => {
    expect(getFormatLabel('somenewformat')).toBe('somenewformat');
  });
});
