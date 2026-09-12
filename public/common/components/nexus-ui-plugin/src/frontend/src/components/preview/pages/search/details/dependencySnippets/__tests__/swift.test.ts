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

import { generate } from '../swift';
import type { SnippetComponentModel } from '../types';

describe('swift dependency snippet generator', () => {
  const component: SnippetComponentModel = {
    format: 'swift',
    group: '',
    name: 'apple.swift-collections',
    version: '1.1.0',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(component).map((s) => s.displayName)).toEqual(['Package.swift']);
  });

  it('produces snippet text matching Classic', () => {
    const [snippet] = generate(component);
    expect(snippet.description).toBe('Add the following dependency to your Package.swift:');
    expect(snippet.snippetText).toBe('.package(id: "apple.swift-collections", from: "1.1.0")');
  });
});
