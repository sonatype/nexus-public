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
import DateUtils from './DateUtils';

describe('DateUtils', () => {
  let consoleDebugSpy;

  beforeEach(() => {
    consoleDebugSpy = jest.spyOn(console, 'debug').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleDebugSpy.mockRestore();
  });

  describe('timestampToString', () => {
    it('converts valid timestamp to date string', () => {
      // Use a mid-day UTC timestamp to avoid timezone boundary issues
      const timestamp = 1672574400000; // 2023-01-01 12:00:00 UTC
      const result = DateUtils.timestampToString(timestamp);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      // Check that it contains a valid date format (any year is fine due to timezone)
      expect(result).toMatch(/\w+ \w+ \d+ \d{4}/);
    });

    it('handles zero timestamp', () => {
      const result = DateUtils.timestampToString(0);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      // Zero timestamp is Jan 1 1970 UTC, may show Dec 31 1969 in some timezones
      expect(result).toMatch(/196\d|1970/);
    });

    it('handles negative timestamp', () => {
      const timestamp = -86400000; // 1969-12-31 UTC
      const result = DateUtils.timestampToString(timestamp);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      expect(result).toContain('1969');
    });

    it('handles current date timestamp', () => {
      const timestamp = Date.now();
      const result = DateUtils.timestampToString(timestamp);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns Invalid Date string for invalid timestamp', () => {
      const invalidValue = 'invalid';
      const result = DateUtils.timestampToString(invalidValue);

      // new Date('invalid').toString() returns "Invalid Date", doesn't throw
      expect(result).toBe('Invalid Date');
    });

    it('returns Invalid Date string for undefined', () => {
      const result = DateUtils.timestampToString(undefined);

      // new Date(undefined).toString() returns "Invalid Date"
      expect(result).toBe('Invalid Date');
    });
  });

  describe('prettyDate', () => {
    it('formats valid timestamp to pretty date string', () => {
      // Use mid-day UTC to avoid timezone boundary issues
      const timestamp = 1672574400000; // 2023-01-01 12:00:00 UTC
      const result = DateUtils.prettyDate(timestamp);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      expect(result).toMatch(/\w{3}, \w{3} \d{1,2}, \d{4}/); // e.g., "Sun, Jan 1, 2023"
    });

    it('formats date with correct format pattern', () => {
      // Use mid-day UTC to ensure same day in most timezones
      const timestamp = 1672574400000; // 2023-01-01 12:00:00 UTC
      const result = DateUtils.prettyDate(timestamp);

      // Check format: "Ddd, Mmm D, YYYY"
      expect(result).toMatch(/\w{3}, \w{3} \d{1,2}, \d{4}/);
    });

    it('handles zero timestamp', () => {
      const result = DateUtils.prettyDate(0);

      expect(result).toBeTruthy();
      // Zero timestamp is Jan 1 1970 UTC, may show Dec 31 1969 in western timezones
      expect(result).toMatch(/196\d|1970/);
    });

    it('handles negative timestamp', () => {
      const timestamp = -86400000; // 1969-12-31 UTC
      const result = DateUtils.prettyDate(timestamp);

      expect(result).toBeTruthy();
      expect(result).toContain('1969');
      expect(result).toContain('Dec');
    });

    it('formats different months correctly', () => {
      // Use mid-day UTC to avoid timezone boundary
      const julyTimestamp = 1688212800000; // 2023-07-01 12:00:00 UTC
      const result = DateUtils.prettyDate(julyTimestamp);

      expect(result).toContain('2023');
      // Could be Jun 30 or Jul 1 depending on timezone, check for either
      expect(result).toMatch(/Jun|Jul/);
    });

    it('returns Invalid Date for invalid timestamp', () => {
      const invalidValue = 'not a timestamp';
      const result = DateUtils.prettyDate(invalidValue);

      // new Date('invalid').toLocaleDateString() returns "Invalid Date"
      expect(result).toBe('Invalid Date');
    });

    it('handles null input as epoch', () => {
      const result = DateUtils.prettyDate(null);

      // new Date(null) creates epoch date (Jan 1 1970 UTC)
      expect(result).toBeTruthy();
      expect(result).toMatch(/196\d|1970/);
    });
  });

  describe('prettyDateTime', () => {
    it('formats valid date to pretty datetime string', () => {
      const date = new Date('2023-01-01T12:30:45Z');
      const result = DateUtils.prettyDateTime(date);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      expect(result).toContain('2023');
    });

    it('includes time zone information', () => {
      const date = new Date('2023-06-15T14:30:00Z');
      const result = DateUtils.prettyDateTime(date);

      expect(result).toBeTruthy();
      // Should contain some time zone indicator
      expect(result).toMatch(/GMT|UTC|[+-]\d{2}:\d{2}/);
    });

    it('uses 24-hour format', () => {
      const date = new Date('2023-01-01T14:30:00Z');
      const result = DateUtils.prettyDateTime(date);

      expect(result).toBeTruthy();
      // Should not contain AM/PM indicators
      expect(result).not.toMatch(/AM|PM/i);
    });

    it('handles different dates', () => {
      const date1 = new Date('2020-03-15T10:00:00Z');
      const result1 = DateUtils.prettyDateTime(date1);

      const date2 = new Date('2024-12-25T18:45:30Z');
      const result2 = DateUtils.prettyDateTime(date2);

      expect(result1).toBeTruthy();
      expect(result2).toBeTruthy();
      expect(result1).not.toBe(result2);
    });

    it('handles invalid date objects', () => {
      const invalidDate = new Date('invalid');
      const result = DateUtils.prettyDateTime(invalidDate);

      // Invalid dates still produce a result, but may contain "Invalid Date"
      expect(result).toBeTruthy();
    });

    it('handles non-date input', () => {
      const result = DateUtils.prettyDateTime('not a date');

      // String input will still be processed by toLocaleString
      expect(result).toBeTruthy();
    });

    it('handles null input', () => {
      const result = DateUtils.prettyDateTime(null);

      // null.toLocaleString() throws, so catch block returns null
      expect(result).toBeNull();
    });
  });

  describe('prettyDateTimeLong', () => {
    it('formats valid date to long datetime string', () => {
      const date = new Date('2023-01-01T12:30:45Z');
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      expect(result).toContain('2023');
    });

    it('includes weekday in long format', () => {
      const date = new Date('2023-01-01T12:00:00Z'); // Sunday
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      expect(result).toMatch(/Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday/);
    });

    it('includes month in long format', () => {
      const date = new Date('2023-06-15T12:00:00Z');
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      expect(result).toContain('June');
    });

    it('includes time components', () => {
      const date = new Date('2023-01-15T14:30:45Z');
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      // Should contain time separators
      expect(result).toMatch(/:/);
    });

    it('uses 24-hour format', () => {
      const date = new Date('2023-01-01T15:30:00Z');
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      // Should not contain AM/PM indicators
      expect(result).not.toMatch(/AM|PM/i);
    });

    it('includes timezone information', () => {
      const date = new Date('2023-06-15T14:30:00Z');
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      // Should contain some time zone indicator
      expect(result).toMatch(/GMT|UTC|[+-]\d{2}:\d{2}/);
    });

    it('formats different dates distinctly', () => {
      const date1 = new Date('2020-01-01T10:00:00Z');
      const result1 = DateUtils.prettyDateTimeLong(date1);

      const date2 = new Date('2024-12-31T22:30:15Z');
      const result2 = DateUtils.prettyDateTimeLong(date2);

      expect(result1).toBeTruthy();
      expect(result2).toBeTruthy();
      expect(result1).not.toBe(result2);
    });

    it('includes seconds in output', () => {
      const date = new Date('2023-06-15T14:30:45Z');
      const result = DateUtils.prettyDateTimeLong(date);

      expect(result).toBeTruthy();
      // Result should have multiple colons for hours:minutes:seconds
      const colonCount = (result.match(/:/g) || []).length;
      expect(colonCount).toBeGreaterThanOrEqual(2);
    });

    it('handles invalid date objects', () => {
      const invalidDate = new Date('not valid');
      const result = DateUtils.prettyDateTimeLong(invalidDate);

      // Invalid dates still produce a result, but may contain "Invalid Date"
      expect(result).toBeTruthy();
    });

    it('handles non-date input', () => {
      const result = DateUtils.prettyDateTimeLong('invalid');

      // String input will still be processed by toLocaleString
      expect(result).toBeTruthy();
    });

    it('handles null input', () => {
      const result = DateUtils.prettyDateTimeLong(null);

      // null.toLocaleString() throws, so catch block returns null
      expect(result).toBeNull();
    });

    it('handles undefined input', () => {
      const result = DateUtils.prettyDateTimeLong(undefined);

      // undefined.toLocaleString() throws, so catch block returns null
      expect(result).toBeNull();
    });
  });
});
