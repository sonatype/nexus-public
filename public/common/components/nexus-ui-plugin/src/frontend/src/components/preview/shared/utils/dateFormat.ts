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

/**
 * The placeholder rendered wherever a timestamp is missing or unparseable. An em dash, matching
 * the empty-value convention already used across the Preview UI's detail rows and tables.
 */
export const NO_DATE = '—';

/**
 * Timezone rule (NEXUS-54427): timestamps render in the **viewer's own timezone**, with that
 * timezone spelled out as a short GMT offset (`GMT-5`, `GMT+5:30`, `GMT` for UTC).
 *
 * Local-with-a-label rather than UTC because it is the compatibility-preserving choice: every
 * affected surface already formatted through `toLocaleDateString`, i.e. already in local time.
 * Switching to UTC would silently shift every displayed date for most of the world — the label is
 * what was missing, not the zone. It also matches the rest of the product: `DateUtils`
 * (`interface/DateUtils.js`) formats with `timeZoneName: 'longOffset'` in local time, and the task
 * scheduler labels schedules with `Intl.DateTimeFormat().resolvedOptions().timeZone`.
 *
 * Offset arithmetic and daylight saving are delegated entirely to `Intl.DateTimeFormat`; nothing
 * here computes or concatenates an offset by hand, so half-hour zones and DST transitions are
 * correct by construction.
 *
 * Locale is pinned to `en-US` — the same locale `DateUtils` pins and the locale the Preview UI's
 * `UIStrings` are written in. A fixed locale also keeps the field order unambiguous (`Mar 15,
 * 2024` can never be read as a day-first date) and keeps output stable in tests and screenshots.
 *
 * Purely client-side, so Pro, Cloud and Community Edition all render identically: there is no
 * server-side branch and no edition-specific code path.
 */
const FORMAT_OPTIONS: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  // Seconds are deliberately omitted: these are "last updated"/"last modified" fields where
  // second-level precision carries no meaning and only lengthens an already-long string.
  hour12: false,
  timeZoneName: 'shortOffset',
};

export interface FormatDateTimeOptions {
  /**
   * IANA timezone name (`'UTC'`, `'America/Bogota'`). Defaults to the viewer's own timezone, which
   * is the product behavior; the override exists so tests can assert exact output without
   * depending on the machine running them. An unknown zone falls back to {@link NO_DATE} rather
   * than throwing.
   */
  timeZone?: string;
}

/**
 * The single date/time formatter for Search and Component Details.
 *
 * Renders an ISO 8601 timestamp as date + hour + minute + timezone, e.g.
 * `Mar 15, 2024, 14:30 GMT-4`. Anything missing or unparseable — `null`, `undefined`, `''`,
 * `'   '`, `'not-a-date'` — returns {@link NO_DATE}. It never throws and can never emit the string
 * `Invalid Date`.
 *
 * Presentation only. Sorting, filtering and CSV export keep using the raw timestamp.
 */
export function formatDateTime(
  value: string | null | undefined,
  options: FormatDateTimeOptions = {},
): string {
  // Whitespace is rejected before the Date is constructed rather than left to `new Date('   ')`.
  // A whitespace-only string is not a well-formed ISO 8601 date, so what an engine makes of it is
  // implementation-defined (ECMA-262 21.4.3.2): V8 happens to yield an Invalid Date today, but
  // another runtime is free to resolve it to the epoch, which would render Jan 1, 1970 instead of
  // the placeholder. The `typeof` guard is for callers reaching this from untyped JavaScript with
  // something other than a string — trimming a non-string would throw.
  if (!value || (typeof value === 'string' && !value.trim())) {
    return NO_DATE;
  }

  // The Date constructor can throw for certain non-string values (Symbol, BigInt) that escape
  // the type system at runtime. Guard the construction so the "never throws" contract holds.
  let parsed: Date;
  try {
    parsed = new Date(value);
  } catch {
    return NO_DATE;
  }
  // `new Date('')` and `new Date('nonsense')` throw nothing — they produce an Invalid Date whose
  // format()/toLocaleString() is the literal text "Invalid Date". This check is what keeps that
  // string off the screen.
  if (Number.isNaN(parsed.getTime())) {
    return NO_DATE;
  }

  try {
    return new Intl.DateTimeFormat('en-US', {
      ...FORMAT_OPTIONS,
      // Passing `timeZone: undefined` is deliberate, not a leftover: `Intl` reads an absent or
      // undefined `timeZone` as "use the runtime's own zone", which is exactly the product
      // behavior. Do not "simplify" this into a conditional spread — it would change nothing but
      // obscure that the no-override path is the intended default.
      timeZone: options.timeZone,
    }).format(parsed);
  } catch {
    // Only reachable via an invalid `timeZone` (RangeError). A malformed override should degrade
    // to the placeholder, never take the surrounding tab down.
    return NO_DATE;
  }
}
