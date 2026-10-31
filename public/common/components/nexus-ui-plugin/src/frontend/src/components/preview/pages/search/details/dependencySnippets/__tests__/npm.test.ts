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

import { generate } from '../npm';
import type { SnippetComponentModel } from '../types';

function byName(component: SnippetComponentModel) {
  const map: Record<string, string> = {};
  for (const s of generate(component)) {
    map[s.displayName] = s.snippetText;
  }
  return map;
}

describe('npm dependency snippet generator', () => {
  const unscoped: SnippetComponentModel = {
    format: 'npm',
    group: '',
    name: 'lodash',
    version: '4.17.21',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(unscoped).map((s) => s.displayName)).toEqual(['npm', 'Yarn', 'package.json']);
  });

  it('produces unscoped package snippets matching Classic', () => {
    const s = byName(unscoped);
    expect(s['npm']).toBe('npm install lodash@4.17.21');
    expect(s['Yarn']).toBe('yarn add lodash@4.17.21');
    expect(s['package.json']).toBe('"lodash": "4.17.21"');
  });

  it('prefixes the scope with @scope/ for scoped packages', () => {
    const s = byName({ format: 'npm', group: 'angular', name: 'core', version: '17.0.0' });
    expect(s['npm']).toBe('npm install @angular/core@17.0.0');
    expect(s['Yarn']).toBe('yarn add @angular/core@17.0.0');
    expect(s['package.json']).toBe('"@angular/core": "17.0.0"');
  });
});
