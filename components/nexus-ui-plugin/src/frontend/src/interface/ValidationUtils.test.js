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
import ValidationUtils from './ValidationUtils';

describe('ValidationUtils', () => {
  describe('isInRange', () => {
    it('ignores null', () => {
      expect(ValidationUtils.isInRange({value: null})).toBeNull();
    });
    it('ignores undefined', () => {
      expect(ValidationUtils.isInRange({value: undefined})).toBeNull();
    });
    it('ignores empty string', () => {
      expect(ValidationUtils.isInRange({value: ''})).toBeNull();
    });
    it('handles numbers equal to the max range', () => {
      expect(ValidationUtils.isInRange({value: 10, max: 10})).toBeNull();
    });
    it('handles numbers equal to the min range', () => {
      expect(ValidationUtils.isInRange({value: 0, min: 0})).toBeNull();
    });
    it('handles a string number in the range', () => {
      expect(ValidationUtils.isInRange({value: '5', min: 0, max: 10})).toBeNull();
    });
    it('rejects numbers greater than the max', () => {
      expect(ValidationUtils.isInRange({value: 1, max: 0})).toBe('The maximum value for this field is 0');
    });
    it('rejects numbers less than the min', () => {
      expect(ValidationUtils.isInRange({value: 0, min: 1})).toBe('The minimum value for this field is 1');
    });
    it('rejects non-numeric values', () => {
      expect(ValidationUtils.isInRange({value: '1xx', min: 0})).toBe('This field must contain a numeric value');
    });
    it('rejects decimal values when requested', () => {
      expect(ValidationUtils.isInRange({value: '1.0', max: 2, allowDecimals: false}))
          .toBe('This field must not contain decimal values');
    });
    it('allows non decimal values', () => {
      expect(ValidationUtils.isInRange({value: '1', max: 2, allowDecimals: false})).toBeNull();
    });
  });
  describe('isEmail', () => {
    it('rejects null', () => {
      expect(ValidationUtils.isEmail(null)).toBeFalsy();
    });
    it('rejects undefined', () => {
      expect(ValidationUtils.isEmail(undefined)).toBeFalsy();
    });
    it('rejects invalid emails', () => {
      expect(ValidationUtils.isEmail('invalid')).toBeFalsy();
      expect(ValidationUtils.isEmail('invalid@')).toBeFalsy();
      expect(ValidationUtils.isEmail('invalid@email.')).toBeFalsy();
      expect(ValidationUtils.isEmail('invalid@email..com')).toBeFalsy();
      expect(ValidationUtils.isEmail('@email.com')).toBeFalsy();
      expect(ValidationUtils.isEmail('@email')).toBeFalsy();
      expect(ValidationUtils.isEmail('@email.')).toBeFalsy();
    });
    it('allows valid email', () => {
      expect(ValidationUtils.isEmail('valid@email')).toBeTruthy();
      expect(ValidationUtils.isEmail('valid@email.com')).toBeTruthy();
      expect(ValidationUtils.isEmail('valid@email.s')).toBeTruthy();
    });
  });
});
