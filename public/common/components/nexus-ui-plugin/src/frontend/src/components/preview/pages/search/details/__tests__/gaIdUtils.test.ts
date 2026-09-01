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

import { parseGaId } from '../gaIdUtils';

describe('parseGaId', () => {
  it('splits a namespaced identifier into format, group and name', () => {
    expect(parseGaId('maven2:org.test:artifact')).toEqual({
      format: 'maven2',
      group: 'org.test',
      name: 'artifact',
    });
  });

  it('reads the second segment as the name for formats without a namespace', () => {
    expect(parseGaId('npm:lodash')).toEqual({ format: 'npm', group: '', name: 'lodash' });
  });

  it('returns a lone segment as the name with no format, since the two are indistinguishable', () => {
    expect(parseGaId('lodash')).toEqual({ format: '', group: '', name: 'lodash' });
  });

  it('keeps only the first three segments when a name contains colons', () => {
    // Scoped npm names arrive as e.g. npm:@scope:pkg; anything beyond the third segment is
    // dropped rather than folded into the name.
    expect(parseGaId('npm:@scope:pkg:extra')).toEqual({
      format: 'npm',
      group: '@scope',
      name: 'pkg',
    });
  });

  it('treats the empty string as an empty name rather than throwing', () => {
    expect(parseGaId('')).toEqual({ format: '', group: '', name: '' });
  });
});
