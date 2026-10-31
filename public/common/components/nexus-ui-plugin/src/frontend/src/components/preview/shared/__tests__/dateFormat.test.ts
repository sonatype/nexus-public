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
import { formatDateTime, NO_DATE } from '../utils/dateFormat';

/*
 * Every assertion on exact output passes an explicit `timeZone`. Nothing here may depend on the
 * timezone of the machine running the suite — jest.config.js pins no TZ, so a test that leans on
 * the ambient zone passes in Bogota and fails in Berlin.
 */
const INSTANT = '2024-03-15T18:30:00Z';

describe('formatDateTime', () => {
  describe('valid timestamps', () => {
    it('renders date, hour, minute and timezone', () => {
      expect(formatDateTime(INSTANT, { timeZone: 'UTC' })).toBe('Mar 15, 2024, 18:30 GMT');
    });

    it('includes the calendar date', () => {
      expect(formatDateTime(INSTANT, { timeZone: 'America/Bogota' })).toContain('Mar 15, 2024');
    });

    it('includes hour and minute, and omits seconds', () => {
      const formatted = formatDateTime(INSTANT, { timeZone: 'America/Bogota' });
      expect(formatted).toContain('13:30');
      expect(formatted).not.toMatch(/13:30:\d\d/);
    });

    it('labels the timezone as a GMT offset', () => {
      expect(formatDateTime(INSTANT, { timeZone: 'America/Bogota' })).toContain('GMT-5');
    });

    it('renders a 24-hour clock rather than AM/PM', () => {
      expect(formatDateTime(INSTANT, { timeZone: 'America/Bogota' })).not.toMatch(/[AP]M/);
    });

    it('accepts a timestamp with no explicit UTC designator', () => {
      // Some payloads carry a local-style ISO string. It must still format, not fall back to a dash.
      expect(formatDateTime('2024-03-15T18:30:00', { timeZone: 'UTC' })).not.toBe(NO_DATE);
    });
  });

  describe('timezone conversion', () => {
    // A single instant, four zones: the offset label must always agree with the wall clock beside it.
    it.each([
      ['UTC', 'Mar 15, 2024, 18:30 GMT'],
      ['America/Bogota', 'Mar 15, 2024, 13:30 GMT-5'],
      ['Europe/Berlin', 'Mar 15, 2024, 19:30 GMT+1'],
      ['Asia/Kolkata', 'Mar 16, 2024, 00:00 GMT+5:30'],
    ])('formats %s as %s', (timeZone, expected) => {
      expect(formatDateTime(INSTANT, { timeZone })).toBe(expected);
    });

    // Kolkata is +05:30 — a whole-hour assumption anywhere in the implementation shows up here.
    it('handles a zone whose offset is not a whole number of hours', () => {
      expect(formatDateTime('2024-03-15T00:15:00Z', { timeZone: 'Asia/Kolkata' })).toBe(
        'Mar 15, 2024, 05:45 GMT+5:30',
      );
    });

    // Day-boundary case: 23:30 UTC is already the next calendar day in Kolkata and still the
    // previous one in Honolulu. The displayed date must follow the zone, not the raw string.
    it('shifts the displayed calendar date across a day boundary', () => {
      const nearMidnight = '2024-03-15T23:30:00Z';
      expect(formatDateTime(nearMidnight, { timeZone: 'Asia/Kolkata' })).toBe(
        'Mar 16, 2024, 05:00 GMT+5:30',
      );
      expect(formatDateTime(nearMidnight, { timeZone: 'Pacific/Honolulu' })).toBe(
        'Mar 15, 2024, 13:30 GMT-10',
      );
    });

    // Daylight saving is Intl's job, not ours: the same zone reports a different offset in July
    // than in January, and neither is computed by hand.
    it('reports the daylight-saving offset in effect at that instant', () => {
      expect(formatDateTime('2024-01-15T18:30:00Z', { timeZone: 'America/New_York' })).toBe(
        'Jan 15, 2024, 13:30 GMT-5',
      );
      expect(formatDateTime('2024-07-15T18:30:00Z', { timeZone: 'America/New_York' })).toBe(
        'Jul 15, 2024, 14:30 GMT-4',
      );
    });

    it('defaults to the viewer timezone when none is given', () => {
      const resolved = new Intl.DateTimeFormat().resolvedOptions().timeZone;
      expect(formatDateTime(INSTANT)).toBe(formatDateTime(INSTANT, { timeZone: resolved }));
    });
  });

  describe('missing and invalid input', () => {
    it.each([
      ['null', null],
      ['undefined', undefined],
      ['an empty string', ''],
      ['a whitespace string', '   '],
      ['an unparseable string', 'not-a-date'],
      ['an out-of-range date', '2024-13-45T99:99:99Z'],
      // Not '2024-' or other truncated ISO fragments: V8's fallback parser resolves those to a
      // real instant (Jan 1 2024), so they are valid input as far as the formatter is concerned.
    ])('returns the dash placeholder for %s', (_label, value) => {
      expect(formatDateTime(value as string | null | undefined)).toBe(NO_DATE);
    });

    it.each([
      ['null', null],
      ['undefined', undefined],
      ['an empty string', ''],
      ['an unparseable string', 'not-a-date'],
    ])('never throws for %s', (_label, value) => {
      expect(() => formatDateTime(value as string | null | undefined)).not.toThrow();
    });

    it.each([null, undefined, '', '   ', 'not-a-date', '2024-13-45T99:99:99Z', 'NaN'])(
      'never renders the literal text "Invalid Date" (%s)',
      (value) => {
        expect(formatDateTime(value as string | null | undefined)).not.toContain('Invalid Date');
      },
    );

    /*
     * `Date` is outside the declared input type, but nothing stops untyped data from reaching the
     * formatter at runtime. An Invalid Date object must degrade to the placeholder, exactly as an
     * unparseable string does.
     */
    it('returns the dash placeholder for an invalid Date object passed at runtime', () => {
      const invalidDate = new Date('nonsense') as unknown as string;
      expect(formatDateTime(invalidDate)).toBe(NO_DATE);
    });

    it('returns the dash placeholder rather than throwing on an unknown timezone', () => {
      expect(formatDateTime(INSTANT, { timeZone: 'Mars/Olympus_Mons' })).toBe(NO_DATE);
    });

    /*
     * Symbol and BigInt cannot be used as dates — the Date constructor throws TypeError for them.
     * They are outside the declared input type, but nothing stops untyped code from passing them
     * at runtime. The "never throws" contract requires they degrade to the placeholder.
     */
    it('returns the dash placeholder for a Symbol passed at runtime', () => {
      const sym = Symbol('test') as unknown as string;
      expect(formatDateTime(sym)).toBe(NO_DATE);
    });

    it('returns the dash placeholder for a BigInt passed at runtime', () => {
      const bigint = 1n as unknown as string;
      expect(formatDateTime(bigint)).toBe(NO_DATE);
    });

    it('never throws for a Symbol passed at runtime', () => {
      const sym = Symbol('test') as unknown as string;
      expect(() => formatDateTime(sym)).not.toThrow();
    });

    it('never throws for a BigInt passed at runtime', () => {
      const bigint = 1n as unknown as string;
      expect(() => formatDateTime(bigint)).not.toThrow();
    });
  });

  it('exposes an em dash as the placeholder', () => {
    expect(NO_DATE).toBe('—');
  });
});
