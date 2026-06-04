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
import { isValidIP, normalizeIPv6, validateAndNormalize } from '../utils/ipValidation';

describe('isValidIP', () => {
  describe('IPv4 validation', () => {
    it('accepts valid IPv4 addresses', () => {
      expect(isValidIP('192.168.1.1')).toBe(true);
      expect(isValidIP('10.0.0.1')).toBe(true);
      expect(isValidIP('0.0.0.0')).toBe(true);
      expect(isValidIP('255.255.255.255')).toBe(true);
      expect(isValidIP('127.0.0.1')).toBe(true);
    });

    it('rejects invalid IPv4 addresses', () => {
      expect(isValidIP('256.1.1.1')).toBe(false);
      expect(isValidIP('192.168.1')).toBe(false);
      expect(isValidIP('192.168.1.1.1')).toBe(false);
      expect(isValidIP('192.168.-1.1')).toBe(false);
      expect(isValidIP('192.168.1.256')).toBe(false);
    });

    it('rejects IPv4 addresses with leading zeros', () => {
      expect(isValidIP('192.168.001.001')).toBe(false);
      expect(isValidIP('192.168.01.1')).toBe(false);
      expect(isValidIP('01.02.03.04')).toBe(false);
      expect(isValidIP('010.0.0.1')).toBe(false);
      expect(isValidIP('192.168.1.01')).toBe(false);
    });

    it('accepts single zero octets (not leading zeros)', () => {
      expect(isValidIP('0.0.0.0')).toBe(true);
      expect(isValidIP('10.0.0.1')).toBe(true);
      expect(isValidIP('192.168.0.1')).toBe(true);
    });

    it('handles edge cases', () => {
      expect(isValidIP('')).toBe(false);
      expect(isValidIP(null)).toBe(false);
      expect(isValidIP(undefined)).toBe(false);
      expect(isValidIP('   ')).toBe(false);
      expect(isValidIP('not-an-ip')).toBe(false);
    });
  });

  describe('IPv4 CIDR validation', () => {
    it('accepts valid IPv4 CIDR notation', () => {
      expect(isValidIP('192.168.1.0/24')).toBe(true);
      expect(isValidIP('10.0.0.0/8')).toBe(true);
      expect(isValidIP('192.168.1.1/32')).toBe(true);
      expect(isValidIP('0.0.0.0/0')).toBe(true);
      expect(isValidIP('172.16.0.0/12')).toBe(true);
    });

    it('rejects invalid IPv4 CIDR notation', () => {
      expect(isValidIP('192.168.1.0/33')).toBe(false);
      expect(isValidIP('192.168.1.0/-1')).toBe(false);
      expect(isValidIP('256.1.1.1/24')).toBe(false);
      expect(isValidIP('192.168.1.0/')).toBe(false);
      expect(isValidIP('192.168.1.0/abc')).toBe(false);
    });
  });

  describe('IPv6 validation', () => {
    it('accepts full IPv6 addresses', () => {
      expect(isValidIP('2001:0db8:0000:0000:0000:0000:0000:0001')).toBe(true);
      expect(isValidIP('2001:0db8:85a3:0000:0000:8a2e:0370:7334')).toBe(true);
    });

    it('accepts compressed IPv6 addresses', () => {
      expect(isValidIP('2001:db8::1')).toBe(true);
      expect(isValidIP('2001:db8:85a3::8a2e:370:7334')).toBe(true);
      expect(isValidIP('2001:db8::8a2e:370:7334')).toBe(true);
    });

    it('accepts special IPv6 addresses', () => {
      expect(isValidIP('::1')).toBe(true); // localhost
      expect(isValidIP('::')).toBe(true); // all zeros
      expect(isValidIP('fe80::1')).toBe(true); // link-local
      expect(isValidIP('ff02::1')).toBe(true); // multicast
    });

    it('rejects invalid IPv6 addresses', () => {
      expect(isValidIP('2001:db8:::1')).toBe(false); // too many colons
      expect(isValidIP('2001:db8::1::')).toBe(false); // multiple compressions
      expect(isValidIP('2001:db8:gggg::1')).toBe(false); // invalid hex
      expect(isValidIP('2001:db8:0:0:0:0:0:0:1')).toBe(false); // too many groups
    });
  });

  describe('IPv6 CIDR validation', () => {
    it('accepts valid IPv6 CIDR notation', () => {
      expect(isValidIP('2001:db8::/32')).toBe(true);
      expect(isValidIP('2001:db8:85a3::/48')).toBe(true);
      expect(isValidIP('::1/128')).toBe(true);
      expect(isValidIP('::/0')).toBe(true);
    });

    it('rejects invalid IPv6 CIDR notation', () => {
      expect(isValidIP('2001:db8::/129')).toBe(false); // CIDR > 128
      expect(isValidIP('2001:db8::/-1')).toBe(false); // negative CIDR
      expect(isValidIP('2001:db8::/')).toBe(false); // missing CIDR
      expect(isValidIP('2001:db8:gggg::/32')).toBe(false); // invalid IP part
    });
  });

  describe('IPv4-mapped IPv6 validation', () => {
    it('accepts valid IPv4-mapped IPv6 addresses', () => {
      expect(isValidIP('::ffff:192.168.1.1')).toBe(true);
      expect(isValidIP('::FFFF:192.168.1.1')).toBe(true); // case insensitive
      expect(isValidIP('::ffff:10.0.0.1')).toBe(true);
    });

    it('rejects invalid IPv4-mapped IPv6 addresses', () => {
      expect(isValidIP('::ffff:256.1.1.1')).toBe(false);
      expect(isValidIP('::ffff:192.168.1')).toBe(false);
    });
  });

  describe('whitespace handling', () => {
    it('trims whitespace before validation', () => {
      expect(isValidIP('  192.168.1.1  ')).toBe(true);
      expect(isValidIP('  2001:db8::1  ')).toBe(true);
      expect(isValidIP('\t192.168.1.0/24\n')).toBe(true);
    });
  });

  describe('IPv4 CIDR with leading zeros', () => {
    it('rejects IPv4 CIDR with leading zeros in IP part', () => {
      expect(isValidIP('192.168.001.0/24')).toBe(false);
      expect(isValidIP('010.0.0.0/8')).toBe(false);
    });
  });

  describe('IPv4-mapped IPv6 with leading zeros', () => {
    it('rejects IPv4-mapped IPv6 with leading zeros in IPv4 part', () => {
      expect(isValidIP('::ffff:192.168.001.1')).toBe(false);
      expect(isValidIP('::ffff:010.0.0.1')).toBe(false);
    });
  });
});

describe('normalizeIPv6', () => {
  describe('compresses zero sequences', () => {
    it('compresses full form to canonical form', () => {
      expect(normalizeIPv6('2001:0db8:0000:0000:0000:0000:0000:0001')).toBe('2001:db8::1');
      expect(normalizeIPv6('2001:0db8:0000:0000:0000:0000:0000:0000')).toBe('2001:db8::');
    });

    it('removes leading zeros from groups', () => {
      expect(normalizeIPv6('2001:0db8:0001:0000:0000:0000:0000:0001')).toBe('2001:db8:1::1');
    });

    it('handles already compressed addresses', () => {
      expect(normalizeIPv6('2001:db8::1')).toBe('2001:db8::1');
      expect(normalizeIPv6('::')).toBe('::');
      expect(normalizeIPv6('::1')).toBe('::1');
    });

    it('compresses the longest zero sequence', () => {
      expect(normalizeIPv6('2001:db8:0:0:1:0:0:1')).toBe('2001:db8::1:0:0:1');
      expect(normalizeIPv6('2001:0:0:0:0:0:0:1')).toBe('2001::1');
    });

    it('handles IPv4-mapped IPv6', () => {
      expect(normalizeIPv6('::ffff:192.168.1.1')).toBe('::ffff:192.168.1.1');
    });
  });

  describe('lowercase conversion', () => {
    it('converts uppercase hex to lowercase', () => {
      expect(normalizeIPv6('2001:DB8::1')).toBe('2001:db8::1');
      expect(normalizeIPv6('2001:0DB8:ABCD::1')).toBe('2001:db8:abcd::1');
    });
  });

  describe('edge cases', () => {
    it('handles null and undefined', () => {
      expect(normalizeIPv6(null)).toBe(null);
      expect(normalizeIPv6(undefined)).toBe(undefined);
    });

    it('handles all zeros', () => {
      expect(normalizeIPv6('0000:0000:0000:0000:0000:0000:0000:0000')).toBe('::');
    });

    it('handles addresses with single zero groups', () => {
      expect(normalizeIPv6('2001:db8:0:1:0:1:0:1')).toBe('2001:db8:0:1:0:1:0:1');
    });
  });
});

describe('validateAndNormalize', () => {
  describe('IPv4 addresses', () => {
    it('returns valid result for valid IPv4', () => {
      const result = validateAndNormalize('192.168.1.1');
      expect(result.isValid).toBe(true);
      expect(result.normalized).toBe('192.168.1.1');
      expect(result.error).toBeNull();
    });

    it('returns invalid result for invalid IPv4', () => {
      const result = validateAndNormalize('256.1.1.1');
      expect(result.isValid).toBe(false);
      expect(result.normalized).toBeNull();
      expect(result.error).toBe('Invalid IP address format');
    });

    it('rejects IPv4 with leading zeros', () => {
      const result = validateAndNormalize('192.168.001.1');
      expect(result.isValid).toBe(false);
    });
  });

  describe('IPv6 addresses', () => {
    it('normalizes IPv6 addresses', () => {
      const result = validateAndNormalize('2001:0db8:0000:0000:0000:0000:0000:0001');
      expect(result.isValid).toBe(true);
      expect(result.normalized).toBe('2001:db8::1');
    });

    it('normalizes already compressed IPv6', () => {
      const result = validateAndNormalize('2001:db8::1');
      expect(result.isValid).toBe(true);
      expect(result.normalized).toBe('2001:db8::1');
    });
  });

  describe('CIDR notation', () => {
    it('handles IPv4 CIDR', () => {
      const result = validateAndNormalize('192.168.0.0/24');
      expect(result.isValid).toBe(true);
      expect(result.normalized).toBe('192.168.0.0/24');
    });

    it('normalizes IPv6 CIDR', () => {
      const result = validateAndNormalize('2001:0db8::/32');
      expect(result.isValid).toBe(true);
      expect(result.normalized).toBe('2001:db8::/32');
    });
  });

  describe('whitespace handling', () => {
    it('trims whitespace', () => {
      const result = validateAndNormalize('  192.168.1.1  ');
      expect(result.isValid).toBe(true);
      expect(result.normalized).toBe('192.168.1.1');
    });
  });
});
