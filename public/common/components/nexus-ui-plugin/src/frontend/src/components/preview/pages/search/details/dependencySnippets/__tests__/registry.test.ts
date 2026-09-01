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

import { getDependencySnippets, normalizeFormatKey } from '../registry';

const maven = { format: 'maven', group: 'org.apache.commons', name: 'commons-lang3', version: '3.14.0' };

describe('dependency snippet registry', () => {
  it('normalizes the "maven" format to the maven2 generator', () => {
    const snippets = getDependencySnippets('maven', maven);
    expect(snippets.map((s) => s.displayName)).toContain('Apache Maven');
    expect(snippets[0].snippetText).toContain('<groupId>org.apache.commons</groupId>');
  });

  it('resolves the raw "maven2" format the same way', () => {
    const viaMaven = getDependencySnippets('maven', maven);
    const viaMaven2 = getDependencySnippets('maven2', { ...maven, format: 'maven2' });
    expect(viaMaven2).toEqual(viaMaven);
  });

  it('dispatches each known format to its generator', () => {
    expect(getDependencySnippets('npm', { format: 'npm', group: '', name: 'lodash', version: '4.17.21' })[0].displayName).toBe('npm');
    expect(getDependencySnippets('pypi', { format: 'pypi', group: '', name: 'requests', version: '2.31.0' })[0].displayName).toBe('pip');
    expect(getDependencySnippets('rubygems', { format: 'rubygems', group: '', name: 'rails', version: '7.1.3' })[0].displayName).toBe('Install');
  });

  it('is case-insensitive on the format key', () => {
    expect(getDependencySnippets('NPM', { format: 'NPM', group: '', name: 'lodash', version: '1.0.0' })[0].displayName).toBe('npm');
  });

  it('returns an empty array for an unknown format', () => {
    expect(getDependencySnippets('conan', { format: 'conan', group: '', name: 'x', version: '1' })).toEqual([]);
  });
});

describe('normalizeFormatKey', () => {
  it('maps "maven" to the "maven2" registry key', () => {
    expect(normalizeFormatKey('maven')).toBe('maven2');
  });

  it('lowercases and passes through other formats unchanged', () => {
    expect(normalizeFormatKey('maven2')).toBe('maven2');
    expect(normalizeFormatKey('NPM')).toBe('npm');
    expect(normalizeFormatKey('pypi')).toBe('pypi');
  });
});
