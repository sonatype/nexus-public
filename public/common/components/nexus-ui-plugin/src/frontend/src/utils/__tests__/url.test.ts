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

import { ensureTrailingSlash } from '../url';

describe('ensureTrailingSlash', () => {
  it('appends a trailing slash when the URL does not end with one', () => {
    expect(ensureTrailingSlash('http://example.com')).toBe('http://example.com/');
  });

  it('returns the URL unchanged when it already ends with a slash', () => {
    expect(ensureTrailingSlash('http://example.com/')).toBe('http://example.com/');
  });

  it('handles a plain path without trailing slash', () => {
    expect(ensureTrailingSlash('/api/v1')).toBe('/api/v1/');
  });

  it('handles a plain path that already has a trailing slash', () => {
    expect(ensureTrailingSlash('/api/v1/')).toBe('/api/v1/');
  });

  it('handles an empty string by appending a slash', () => {
    expect(ensureTrailingSlash('')).toBe('/');
  });
});
