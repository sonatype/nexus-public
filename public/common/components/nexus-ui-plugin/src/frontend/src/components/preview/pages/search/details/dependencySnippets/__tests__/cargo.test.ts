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

import { generate } from '../cargo';
import type { SnippetComponentModel } from '../types';

describe('cargo dependency snippet generator', () => {
  const component: SnippetComponentModel = {
    format: 'cargo',
    group: '',
    name: 'serde',
    version: '1.0.197',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(component).map((s) => s.displayName)).toEqual(['cargo', 'manual']);
  });

  it('produces snippet text matching Classic', () => {
    const s: Record<string, string> = {};
    for (const g of generate(component)) s[g.displayName] = g.snippetText;
    expect(s['cargo']).toBe('cargo add serde@1.0.197');
    expect(s['manual']).toBe('serde = "1.0.197"');
  });

  it('returns no snippets when the version is missing', () => {
    expect(generate({ ...component, version: '' })).toEqual([]);
  });
});
