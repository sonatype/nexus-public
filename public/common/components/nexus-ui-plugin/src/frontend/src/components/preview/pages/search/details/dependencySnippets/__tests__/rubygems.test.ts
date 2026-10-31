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

import { generate } from '../rubygems';
import type { SnippetComponentModel } from '../types';

describe('rubygems dependency snippet generator', () => {
  const component: SnippetComponentModel = {
    format: 'rubygems',
    group: '',
    name: 'rails',
    version: '7.1.3',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(component).map((s) => s.displayName)).toEqual(['Install', 'Gemfile']);
  });

  it('produces snippet text matching Classic', () => {
    const s: Record<string, string> = {};
    for (const g of generate(component)) s[g.displayName] = g.snippetText;
    expect(s['Install']).toBe('gem install rails -v 7.1.3');
    expect(s['Gemfile']).toBe("gem 'rails', '~> 7.1.3'");
  });
});
