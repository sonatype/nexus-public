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
import UnitUtil from './UnitUtil';

describe('UnitUtil', () => {
  describe('bytesToMegaBytes', () => {
    it('returns null for null input', () => {
      expect(UnitUtil.bytesToMegaBytes(null))
      .toBeNull();
    });
    it('returns undefined for undefined inout', () => {
      expect(UnitUtil.bytesToMegaBytes(undefined))
      .toBeUndefined();
    });
    it('returns empty string for empty string input', () => {
      expect(UnitUtil.bytesToMegaBytes(''))
      .toBe('');
    });
    it('returns 0 for 0 input', () => {
      expect(UnitUtil.bytesToMegaBytes(0))
      .toBe(0);
    });
    it('returns 1 megabyte for 1048576 bytes', () => {
      expect(UnitUtil.bytesToMegaBytes(1048576))
      .toBe(1);
    });
    // 1 Terabyte = 1048576 Megabytes
    it('returns 1048576(1 Terabyte) megabytes for 1099511627776 bytes', () => {
      expect(UnitUtil.bytesToMegaBytes(1099511627776))
      .toBe(1048576);
    });
    it('returns 1048576(1 Terabyte) megabytes for string input 1099511627776 bytes', () => {
      expect(UnitUtil.bytesToMegaBytes('1099511627776'))
      .toBe(1048576);
    });
  });
  describe('megaBytesToBytes', () => {
    it('returns null for null input', () => {
      expect(UnitUtil.megaBytesToBytes(null))
      .toBeNull();
    });
    it('returns undefined for undefined inout', () => {
      expect(UnitUtil.megaBytesToBytes(undefined))
      .toBeUndefined();
    });
    it('returns empty string for empty string input', () => {
      expect(UnitUtil.megaBytesToBytes(''))
      .toBe('');
    });
    it('returns 0 for 0 input', () => {
      expect(UnitUtil.megaBytesToBytes(0))
      .toBe(0);
    });
    it('returns 1048576 bytes for 1 megabyte', () => {
      expect(UnitUtil.megaBytesToBytes(1))
      .toBe(1048576);
    });
    // 1 Terabyte = 1048576 Megabytes
    it('returns 1099511627776(1 Terabyte) for 1048576 megabyte', () => {
      expect(UnitUtil.megaBytesToBytes(1048576))
      .toBe(1099511627776);
    });
    it('returns 1099511627776(1 Terabyte) for string input 1048576 megabyte', () => {
      expect(UnitUtil.megaBytesToBytes('1048576'))
      .toBe(1099511627776);
    });
  });
});
