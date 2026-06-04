/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import {parseRetryAfter} from './loginUtils';

describe('parseRetryAfter', () => {
  it('returns null for absent header', () => {
    expect(parseRetryAfter(null)).toBeNull();
    expect(parseRetryAfter(undefined)).toBeNull();
    expect(parseRetryAfter('')).toBeNull();
  });

  it('parses a delay-seconds integer string', () => {
    expect(parseRetryAfter('30')).toBe(30);
    expect(parseRetryAfter('120')).toBe(120);
  });

  it('returns at least 1 for zero or negative delay-seconds', () => {
    expect(parseRetryAfter('0')).toBe(1);
    expect(parseRetryAfter('-5')).toBe(1);
  });

  it('parses a future HTTP-date string', () => {
    const future = new Date(Date.now() + 60000).toUTCString();
    const result = parseRetryAfter(future);
    expect(result).toBeGreaterThan(0);
    expect(result).toBeLessThanOrEqual(60);
  });

  it('returns null for an unparseable value', () => {
    expect(parseRetryAfter('not-a-date-or-number')).toBeNull();
  });

  it('returns null for a past HTTP-date string', () => {
    const past = new Date(Date.now() - 60000).toUTCString();
    expect(parseRetryAfter(past)).toBeNull();
  });
});
