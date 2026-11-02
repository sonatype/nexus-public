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

import { formatDateTime, NO_DATE } from '../../../../shared';
import type { Waiver } from './waiverTypes';

/** Table placeholder. A hyphen, not an em dash — see the table standard. */
export const NO_VALUE = '-';

export type WaiverExpirationBand = 'expired-or-soon' | 'approaching' | 'comfortable' | 'never';

export const WAIVER_BAND_COLOR: Record<WaiverExpirationBand, 'red' | 'orange' | 'green' | 'gray'> = {
  'expired-or-soon': 'red',
  approaching: 'orange',
  comfortable: 'green',
  never: 'gray',
};

/** Midnight UTC of the calendar day containing `millis`. */
function utcDayStart(millis: number): number {
  const d = new Date(millis);
  return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate());
}

/**
 * Bands a waiver's expiry for badge colouring.
 *
 * Adapted from the prototype's `getWaiverExpirationBand`, NOT ported verbatim: that version
 * regex-tests a `YYYY-MM-DD` string, and our `expiryTime` is epoch millis. A number coerces to
 * "1755624907259", fails the regex, and returns 'never' — which would render every dated waiver
 * as a gray "Never" badge, plausibly and silently.
 *
 * Compares whole UTC days, the same basis the backend validates renewals on, so a waiver the API
 * considers expiring tomorrow can never render as expiring today.
 */
export function expirationBand(expiryTime: number | null, now: Date): WaiverExpirationBand {
  if (expiryTime === null || expiryTime === undefined) {
    return 'never';
  }
  if (!Number.isFinite(expiryTime)) {
    return 'never';
  }
  const days = Math.round((utcDayStart(expiryTime) - utcDayStart(now.getTime())) / 86_400_000);
  if (days <= 7) {
    return 'expired-or-soon';
  }
  if (days <= 30) {
    return 'approaching';
  }
  return 'comfortable';
}

/**
 * Wire value → display label for the Scope column.
 *
 * Seven values, not five: `frontend.md` corrects the design doc, and `root_organization` is the
 * common case on a real instance rather than an edge one. A naive concatenation would render
 * "repository_manager - x" and visibly contradict Firewall's own data model.
 */
const SCOPE_LABELS: Record<string, string> = {
  application: 'Application',
  organization: 'Organization',
  repository: 'Repository',
  repository_manager: 'Repository Manager',
  repository_container: 'All Repositories',
  root_organization: 'Root Organization',
  all_repositories: 'All Repositories',
};

export function scopeLabel(type: string | null, name: string | null): string {
  if (!type) {
    return NO_VALUE;
  }
  const label = SCOPE_LABELS[type.toLowerCase()] ?? type;
  return name ? `${label} - ${name}` : label;
}

/**
 * Component identity for display.
 *
 * `displayName` arrives already composed server-side, so unlike Quarantine no client-side
 * composition is normally needed. The fallback exists for waivers with no component identifier.
 */
export function componentDisplay(waiver: Waiver): string {
  if (waiver.displayName) {
    return waiver.displayName;
  }
  const { ecosystem, groupId, artifactId, version } = waiver;
  if (groupId && artifactId) {
    const coords = `${groupId}:${artifactId}${version ? `@${version}` : ''}`;
    return ecosystem ? `${ecosystem}/${coords}` : coords;
  }
  return NO_VALUE;
}

/** Three states: null means no upgrade INFORMATION, false means checked and none available. */
export function upgradeLabel(value: boolean | null): string {
  if (value === null || value === undefined) {
    return NO_VALUE;
  }
  return value ? 'Available' : 'None';
}

/**
 * Formats epoch millis for display.
 *
 * `formatDateTime` declares `string | null | undefined`. It survives a number at runtime, but the
 * signature is wrong and there is no `tsc` here to catch it — so convert at this boundary rather
 * than widening a shared signature.
 */
export function toDisplayDate(millis: number | null): string {
  if (!millis || !Number.isFinite(millis)) {
    return NO_VALUE;
  }
  const formatted = formatDateTime(new Date(millis).toISOString());
  return formatted === NO_DATE ? NO_VALUE : formatted;
}

/**
 * The earliest date a renewal may specify, as a `<input type="date">` value.
 *
 * Derived from the UTC date, not the local one: IQ compares whole UTC days and requires strictly
 * after today, so a user east of UTC offered a local tomorrow can pick a date IQ still counts as
 * today and rejects with 400.
 */
export function utcTomorrowInputValue(now: Date): string {
  const tomorrow = new Date(utcDayStart(now.getTime()) + 86_400_000);
  return tomorrow.toISOString().slice(0, 10);
}

/** UTC calendar date as YYYY-MM-DD, so the badge text cannot drift with the viewer's timezone. */
export function toIsoDate(millis: number | null): string {
  if (!millis || !Number.isFinite(millis)) {
    return NO_VALUE;
  }
  return new Date(millis).toISOString().slice(0, 10);
}
