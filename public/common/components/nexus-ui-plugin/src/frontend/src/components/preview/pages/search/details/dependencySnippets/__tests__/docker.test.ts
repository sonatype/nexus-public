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

import { generate } from '../docker';
import type { SnippetAssetModel, SnippetComponentModel } from '../types';

function byName(component: SnippetComponentModel, asset?: SnippetAssetModel) {
  const map: Record<string, string> = {};
  for (const s of generate(component, asset)) map[s.displayName] = s.snippetText;
  return map;
}

describe('docker dependency snippet generator', () => {
  const component: SnippetComponentModel = {
    format: 'docker',
    group: '',
    name: 'library/nginx',
    version: '1.25',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(component).map((s) => s.displayName)).toEqual([
      'Docker',
      'Dockerfile',
      'Compose',
      'Kubernetes',
    ]);
  });

  it('produces snippet text matching Classic without a registry URL', () => {
    const s = byName(component);
    expect(s['Docker']).toBe('docker pull library/nginx:1.25');
    expect(s['Dockerfile']).toBe('FROM library/nginx:1.25');
    expect(s['Compose']).toBe('services:\n  nginx:\n    image: library/nginx:1.25');
    expect(s['Kubernetes']).toBe(
      'spec:\n  containers:\n  - name: nginx\n    image: library/nginx:1.25',
    );
  });

  it('prefixes every image reference with the asset registry URL when present', () => {
    const s = byName(component, { registryUrl: 'localhost:8081' });
    // All references to the image must point at the private registry; a bare name would
    // resolve to Docker Hub instead.
    expect(s['Docker']).toBe('docker pull localhost:8081/library/nginx:1.25');
    expect(s['Dockerfile']).toBe('FROM localhost:8081/library/nginx:1.25');
    expect(s['Compose']).toBe('services:\n  nginx:\n    image: localhost:8081/library/nginx:1.25');
    expect(s['Kubernetes']).toBe(
      'spec:\n  containers:\n  - name: nginx\n    image: localhost:8081/library/nginx:1.25',
    );
  });
});
