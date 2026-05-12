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
import UIStrings from '../constants/UIStrings';
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

  describe('validateLength', () => {
    it('allows null', () => {
      expect(ValidationUtils.validateLength(null, 10)).toBeNull();
    });
    it('allows undefined', () => {
      expect(ValidationUtils.validateLength(undefined, 10)).toBeNull();
    });
    it('allows empty string', () => {
      expect(ValidationUtils.validateLength('', 10)).toBeNull();
    });
    it('rejects value longer than length', () => {
      expect(ValidationUtils.validateLength('12345678901', 10)).toBe(UIStrings.ERROR.MAX_CHARS(10));
    });
    it('allows value equal to length', () => {
      expect(ValidationUtils.validateLength('1234567890', 10)).toBeNull();
    });
    it('allows value shorter than length', () => {
      expect(ValidationUtils.validateLength('123456789', 10)).toBeNull();
    });
  });

  describe('isBlank', () => {
    it('returns true for null', () => {
      expect(ValidationUtils.isBlank(null)).toBe(true);
    });

    it('returns true for undefined', () => {
      expect(ValidationUtils.isBlank(undefined)).toBe(true);
    });

    it('returns true for empty string', () => {
      expect(ValidationUtils.isBlank('')).toBe(true);
    });

    it('returns true for whitespace only', () => {
      expect(ValidationUtils.isBlank('   ')).toBe(true);
      expect(ValidationUtils.isBlank('\t')).toBe(true);
      expect(ValidationUtils.isBlank('\n')).toBe(true);
    });

    it('returns false for non-blank string', () => {
      expect(ValidationUtils.isBlank('text')).toBe(false);
      expect(ValidationUtils.isBlank(' text ')).toBe(false);
    });
  });

  describe('notBlank', () => {
    it('returns false for blank strings', () => {
      expect(ValidationUtils.notBlank('')).toBe(false);
      expect(ValidationUtils.notBlank(null)).toBe(false);
      expect(ValidationUtils.notBlank('  ')).toBe(false);
    });

    it('returns true for non-blank strings', () => {
      expect(ValidationUtils.notBlank('text')).toBe(true);
      expect(ValidationUtils.notBlank('a')).toBe(true);
    });
  });

  describe('isUri', () => {
    it('returns true for valid URIs', () => {
      expect(ValidationUtils.isUri('http://example.com')).toBe(true);
      expect(ValidationUtils.isUri('https://example.com')).toBe(true);
      expect(ValidationUtils.isUri('ftp://files.com')).toBe(true);
      expect(ValidationUtils.isUri('file:///path/to/file')).toBe(true);
    });

    it('returns false for invalid URIs', () => {
      expect(ValidationUtils.isUri('not a uri')).toBe(false);
      expect(ValidationUtils.isUri('example.com')).toBe(false);
      // null/undefined return the falsy input value, not boolean false
      expect(ValidationUtils.isUri(null)).toBeNull();
      expect(ValidationUtils.isUri(undefined)).toBeUndefined();
    });
  });

  describe('notUri', () => {
    it('returns false for valid URIs', () => {
      expect(ValidationUtils.notUri('http://example.com')).toBe(false);
    });

    it('returns true for invalid URIs', () => {
      expect(ValidationUtils.notUri('not a uri')).toBe(true);
      expect(ValidationUtils.notUri(null)).toBe(true);
    });
  });

  describe('isUrl', () => {
    it('returns true for valid HTTP URLs', () => {
      expect(ValidationUtils.isUrl('http://example.com')).toBe(true);
      expect(ValidationUtils.isUrl('https://example.com')).toBe(true);
      expect(ValidationUtils.isUrl('http://example.com:8080')).toBe(true);
      expect(ValidationUtils.isUrl('http://example.com/path')).toBe(true);
    });

    it('returns false for invalid URLs', () => {
      expect(ValidationUtils.isUrl('ftp://example.com')).toBe(false);
      expect(ValidationUtils.isUrl('not a url')).toBe(false);
      expect(ValidationUtils.isUrl(null)).toBe(false);
      expect(ValidationUtils.isUrl('')).toBe(false);
    });

    it('returns false for port 0', () => {
      expect(ValidationUtils.isUrl('http://example.com:0')).toBe(false);
    });

    it('returns false for invalid hostname', () => {
      expect(ValidationUtils.isUrl('http://fo£o.bar')).toBe(false);
    });
  });

  describe('notUrl', () => {
    it('returns false for valid URLs', () => {
      expect(ValidationUtils.notUrl('http://example.com')).toBe(false);
    });

    it('returns true for invalid URLs', () => {
      expect(ValidationUtils.notUrl('not a url')).toBe(true);
    });
  });

  describe('isSecureUrl', () => {
    it('returns true for HTTPS URLs', () => {
      expect(ValidationUtils.isSecureUrl('https://example.com')).toBe(true);
      expect(ValidationUtils.isSecureUrl('ldaps://example.com')).toBe(true);
    });

    it('returns false for non-secure URLs', () => {
      expect(ValidationUtils.isSecureUrl('http://example.com')).toBe(false);
      expect(ValidationUtils.isSecureUrl('ftp://example.com')).toBe(false);
      // null returns null (falsy) instead of false
      expect(ValidationUtils.isSecureUrl(null)).toBeNull();
    });
  });

  describe('notSecureUrl', () => {
    it('returns false for secure URLs', () => {
      expect(ValidationUtils.notSecureUrl('https://example.com')).toBe(false);
    });

    it('returns true for non-secure URLs', () => {
      expect(ValidationUtils.notSecureUrl('http://example.com')).toBe(true);
    });
  });

  describe('isHost', () => {
    it('returns true for valid hostnames', () => {
      expect(ValidationUtils.isHost('example.com')).toBe(true);
      expect(ValidationUtils.isHost('sub.example.com')).toBe(true);
      expect(ValidationUtils.isHost('192.168.1.1')).toBe(true);
      expect(ValidationUtils.isHost('localhost')).toBe(true);
    });

    it('returns true for IPv6 addresses', () => {
      expect(ValidationUtils.isHost('[2001:db8::1]')).toBe(true);
    });

    it('returns true for hostname with port', () => {
      expect(ValidationUtils.isHost('example.com:8080')).toBe(true);
    });

    it('returns false for invalid hostnames', () => {
      // null/undefined/empty return the falsy input value, not boolean false
      expect(ValidationUtils.isHost(null)).toBeNull();
      expect(ValidationUtils.isHost(undefined)).toBeUndefined();
      expect(ValidationUtils.isHost('')).toBe('');
    });
  });

  describe('hasWhiteSpace', () => {
    it('returns true for strings with whitespace', () => {
      expect(ValidationUtils.hasWhiteSpace('hello world')).toBe(true);
      expect(ValidationUtils.hasWhiteSpace('hello\tworld')).toBe(true);
      expect(ValidationUtils.hasWhiteSpace('hello\nworld')).toBe(true);
    });

    it('returns false for strings without whitespace', () => {
      expect(ValidationUtils.hasWhiteSpace('helloworld')).toBe(false);
      expect(ValidationUtils.hasWhiteSpace('hello-world')).toBe(false);
    });
  });

  describe('validateLength', () => {
    it('returns null for valid length', () => {
      expect(ValidationUtils.validateLength('test', 10)).toBeNull();
      expect(ValidationUtils.validateLength('test', 4)).toBeNull();
    });

    it('returns error for exceeding length', () => {
      const error = ValidationUtils.validateLength('toolong', 5);
      expect(error).toBeTruthy();
      expect(error).toContain('5');
    });
  });

  describe('validateNameField', () => {
    it('returns null for valid names', () => {
      expect(ValidationUtils.validateNameField('validName')).toBeNull();
      expect(ValidationUtils.validateNameField('valid-name')).toBeNull();
      expect(ValidationUtils.validateNameField('valid_name')).toBeNull();
    });

    it('returns error for blank names', () => {
      expect(ValidationUtils.validateNameField('')).toBeTruthy();
      expect(ValidationUtils.validateNameField(null)).toBeTruthy();
    });

    it('returns error for invalid characters', () => {
      expect(ValidationUtils.validateNameField('invalid name')).toBeTruthy();
      expect(ValidationUtils.validateNameField('invalid@name')).toBeTruthy();
    });
  });

  describe('validateName', () => {
    it('returns null for valid names', () => {
      expect(ValidationUtils.validateName('valid')).toBeNull();
      expect(ValidationUtils.validateName('valid-name')).toBeNull();
      expect(ValidationUtils.validateName('valid_name')).toBeNull();
      expect(ValidationUtils.validateName('valid123')).toBeNull();
    });

    it('returns error for invalid names', () => {
      expect(ValidationUtils.validateName('invalid name')).toBeTruthy();
      expect(ValidationUtils.validateName('invalid@name')).toBeTruthy();
    });
  });

  describe('validateNotBlank', () => {
    it('returns undefined for non-blank values', () => {
      expect(ValidationUtils.validateNotBlank('value')).toBeUndefined();
    });

    it('returns error for blank values', () => {
      expect(ValidationUtils.validateNotBlank('')).toBeTruthy();
      expect(ValidationUtils.validateNotBlank(null)).toBeTruthy();
      expect(ValidationUtils.validateNotBlank('  ')).toBeTruthy();
    });
  });

  describe('validateHost', () => {
    it('returns undefined for valid hosts', () => {
      expect(ValidationUtils.validateHost('example.com')).toBeUndefined();
      expect(ValidationUtils.validateHost('192.168.1.1')).toBeUndefined();
    });

    it('returns error for invalid hosts', () => {
      expect(ValidationUtils.validateHost('not valid')).toBeTruthy();
      expect(ValidationUtils.validateHost('')).toBeTruthy();
    });
  });

  describe('validateIsUri', () => {
    it('returns undefined for valid URIs', () => {
      expect(ValidationUtils.validateIsUri('http://example.com')).toBeUndefined();
      expect(ValidationUtils.validateIsUri('https://example.com')).toBeUndefined();
    });

    it('returns error for invalid URIs', () => {
      expect(ValidationUtils.validateIsUri('not a uri')).toBeTruthy();
    });
  });

  describe('validateIsUrl', () => {
    it('returns undefined for valid URLs', () => {
      expect(ValidationUtils.validateIsUrl('http://example.com')).toBeUndefined();
    });

    it('returns error for invalid URLs', () => {
      expect(ValidationUtils.validateIsUrl('not a url')).toBeTruthy();
    });
  });

  describe('validateLeadingOrTrailingSpace', () => {
    it('returns undefined for strings without leading/trailing spaces', () => {
      expect(ValidationUtils.validateLeadingOrTrailingSpace('valid')).toBeUndefined();
      expect(ValidationUtils.validateLeadingOrTrailingSpace('valid string')).toBeUndefined();
    });

    it('returns error for leading spaces', () => {
      expect(ValidationUtils.validateLeadingOrTrailingSpace(' leading')).toBeTruthy();
    });

    it('returns error for trailing spaces', () => {
      expect(ValidationUtils.validateLeadingOrTrailingSpace('trailing ')).toBeTruthy();
    });

    it('returns error for both leading and trailing spaces', () => {
      expect(ValidationUtils.validateLeadingOrTrailingSpace(' both ')).toBeTruthy();
    });
  });

  describe('validateWhiteSpace', () => {
    it('returns undefined for strings without whitespace', () => {
      expect(ValidationUtils.validateWhiteSpace('nowhitespace')).toBeUndefined();
    });

    it('returns error for strings with whitespace', () => {
      expect(ValidationUtils.validateWhiteSpace('with space')).toBeTruthy();
      expect(ValidationUtils.validateWhiteSpace('with\ttab')).toBeTruthy();
    });
  });

  describe('isName', () => {
    it('returns truthy for valid names', () => {
      expect(ValidationUtils.isName('validName')).toBeTruthy();
      expect(ValidationUtils.isName('valid-name')).toBeTruthy();
      expect(ValidationUtils.isName('valid_name')).toBeTruthy();
      expect(ValidationUtils.isName('valid.name')).toBeTruthy();
      expect(ValidationUtils.isName('name123')).toBeTruthy();
    });

    it('returns falsy for invalid names', () => {
      expect(ValidationUtils.isName('invalid name')).toBeFalsy();
      expect(ValidationUtils.isName('invalid@name')).toBeFalsy();
      expect(ValidationUtils.isName('')).toBeFalsy();
      expect(ValidationUtils.isName(null)).toBeFalsy();
    });

    it('rejects names starting with period', () => {
      expect(ValidationUtils.isName('.invalid')).toBeFalsy();
    });
  });

  describe('validateEmail', () => {
    it('returns undefined for valid emails', () => {
      expect(ValidationUtils.validateEmail('valid@email.com')).toBeUndefined();
    });

    it('returns error for invalid emails', () => {
      expect(ValidationUtils.validateEmail('invalid')).toBeTruthy();
      expect(ValidationUtils.validateEmail('invalid@')).toBeTruthy();
    });
  });

  describe('isInvalid', () => {
    it('returns false for null', () => {
      expect(ValidationUtils.isInvalid(null)).toBe(false);
    });

    it('returns false for undefined', () => {
      expect(ValidationUtils.isInvalid(undefined)).toBe(false);
    });

    it('returns false for empty errors object', () => {
      expect(ValidationUtils.isInvalid({})).toBe(false);
    });

    it('returns false for errors object with null values', () => {
      expect(ValidationUtils.isInvalid({field1: null, field2: null})).toBe(false);
    });

    it('returns true for errors object with error messages', () => {
      expect(ValidationUtils.isInvalid({field1: 'error'})).toBe(true);
    });

    it('returns true for nested errors', () => {
      expect(ValidationUtils.isInvalid({nested: {field: 'error'}})).toBe(true);
    });

    it('handles mixed valid and invalid fields', () => {
      expect(ValidationUtils.isInvalid({field1: null, field2: 'error'})).toBe(true);
    });
  });

  describe('validatePasswordsMatch', () => {
    it('returns undefined when passwords match', () => {
      expect(ValidationUtils.validatePasswordsMatch('pass123', 'pass123')).toBeUndefined();
    });

    it('returns error when passwords do not match', () => {
      expect(ValidationUtils.validatePasswordsMatch('pass123', 'pass456')).toBeTruthy();
    });

    it('handles empty strings', () => {
      expect(ValidationUtils.validatePasswordsMatch('', '')).toBeUndefined();
    });
  });

  describe('isValidLoggerName', () => {
    it('returns true for valid logger names', () => {
      expect(ValidationUtils.isValidLoggerName('com.example.Logger')).toBe(true);
      expect(ValidationUtils.isValidLoggerName('Logger')).toBe(true);
      expect(ValidationUtils.isValidLoggerName('my.app.service')).toBe(true);
    });

    it('returns false for blank names', () => {
      expect(ValidationUtils.isValidLoggerName('')).toBe(false);
      expect(ValidationUtils.isValidLoggerName(null)).toBe(false);
      expect(ValidationUtils.isValidLoggerName('  ')).toBe(false);
    });

    it('returns false for names with invalid characters', () => {
      expect(ValidationUtils.isValidLoggerName('logger<name')).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger>name')).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger&name')).toBe(false);
      expect(ValidationUtils.isValidLoggerName("logger'name")).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger"name')).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger/name')).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger\nname')).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger\rname')).toBe(false);
      expect(ValidationUtils.isValidLoggerName('logger\tname')).toBe(false);
    });
  });

  describe('validateLoggerName', () => {
    it('returns null for valid logger names', () => {
      expect(ValidationUtils.validateLoggerName('com.example.Logger')).toBeNull();
      expect(ValidationUtils.validateLoggerName('Logger')).toBeNull();
    });

    it('returns error for blank names', () => {
      expect(ValidationUtils.validateLoggerName('')).toBeTruthy();
      expect(ValidationUtils.validateLoggerName(null)).toBeTruthy();
    });

    it('returns error for invalid characters', () => {
      expect(ValidationUtils.validateLoggerName('logger<name')).toBeTruthy();
      expect(ValidationUtils.validateLoggerName('logger&name')).toBeTruthy();
    });
  });
});
