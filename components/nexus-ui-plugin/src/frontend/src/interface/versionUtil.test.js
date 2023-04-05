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
import { getVersionMajorMinor } from './versionUtil';

describe('versionUtil', function() {
  describe('getVersionMajorMinor', function() {
    it('returns the input if passed null or undefined', function() {
      expect(getVersionMajorMinor(null)).toBe(null);
      expect(getVersionMajorMinor(undefined)).toBe(undefined);
    });

    it('returns the empty string if passed the empty string', function() {
      expect(getVersionMajorMinor('')).toBe('');
    });

    it('returns the entire input string if the input does not contain a "." character', function() {
      expect(getVersionMajorMinor('asdof#adfovn(aklndkjn,adionaoiqweqqnf894h-+!@#$%^&*(<>/?|\\\'":;…'))
          .toBe('asdof#adfovn(aklndkjn,adionaoiqweqqnf894h-+!@#$%^&*(<>/?|\\\'":;…');
    });

    it('returns the entire input string if the string contains exactly one "." character', function() {
      expect(getVersionMajorMinor('asdof#adfovn(aklndkjn,ad.ionaoiqweqqnf894h-+!@#$%^&*(<>/?|\\\'":;…'))
          .toBe('asdof#adfovn(aklndkjn,ad.ionaoiqweqqnf894h-+!@#$%^&*(<>/?|\\\'":;…');
    });

    it('returns the prefix of the input string up to but not including the second "." character', function() {
      expect(getVersionMajorMinor('1.2.')).toBe('1.2');
      expect(getVersionMajorMinor('1.2.3')).toBe('1.2');
      expect(getVersionMajorMinor('1-a.2-b.3-c')).toBe('1-a.2-b');
      expect(getVersionMajorMinor('1.2.3-SNAPSHOT')).toBe('1.2');
      expect(getVersionMajorMinor('aa.bb.cc-SNAPSHOT')).toBe('aa.bb');
      expect(getVersionMajorMinor('aa..cc-SNAPSHOT')).toBe('aa.');
    });
  });
});
