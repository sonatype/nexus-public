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

import {
  parseRetryAfter,
  resolveDefaultLandingRoute,
  isPreviewHash,
  isClassicLandingHash,
  returnToTargetsPreview,
  returnToTargetsClassicLanding,
  PREVIEW_WELCOME_ROUTE,
  CLASSIC_WELCOME_ROUTE,
  USER_REQUESTED_LEGACY_KEY,
} from './loginUtils';

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

describe('resolveDefaultLandingRoute', () => {
  it('exposes the expected constant values', () => {
    expect(PREVIEW_WELCOME_ROUTE).toBe('preview.browse.welcome');
    expect(CLASSIC_WELCOME_ROUTE).toBe('browse.welcome');
    expect(USER_REQUESTED_LEGACY_KEY).toBe('user_requested_legacy');
  });

  it('returns preview when default+access and legacy not requested', () => {
    expect(resolveDefaultLandingRoute({
      defaultToPreviewUi: true,
      canAccessPreviewUi: true,
      userRequestedLegacy: false,
    })).toBe(PREVIEW_WELCOME_ROUTE);
  });

  it('returns classic when defaultToPreviewUi is off', () => {
    expect(resolveDefaultLandingRoute({
      defaultToPreviewUi: false,
      canAccessPreviewUi: true,
      userRequestedLegacy: false,
    })).toBe(CLASSIC_WELCOME_ROUTE);
  });

  it('returns classic when the user cannot access preview', () => {
    expect(resolveDefaultLandingRoute({
      defaultToPreviewUi: true,
      canAccessPreviewUi: false,
      userRequestedLegacy: false,
    })).toBe(CLASSIC_WELCOME_ROUTE);
  });

  it('returns classic when the user explicitly requested legacy', () => {
    expect(resolveDefaultLandingRoute({
      defaultToPreviewUi: true,
      canAccessPreviewUi: true,
      userRequestedLegacy: true,
    })).toBe(CLASSIC_WELCOME_ROUTE);
  });

  it('returns preview when userRequestedLegacy is omitted (treated as false)', () => {
    expect(resolveDefaultLandingRoute({
      defaultToPreviewUi: true,
      canAccessPreviewUi: true,
    })).toBe(PREVIEW_WELCOME_ROUTE);
  });
});

describe('isPreviewHash', () => {
  it('returns true for preview hashes', () => {
    expect(isPreviewHash('#preview/browse/welcome')).toBe(true);
    expect(isPreviewHash('#/preview/browse/welcome')).toBe(true);
    expect(isPreviewHash('#preview')).toBe(true);
    expect(isPreviewHash('preview/browse/welcome')).toBe(true);
  });

  it('returns false for classic hashes and empty values', () => {
    expect(isPreviewHash('#browse/welcome')).toBe(false);
    expect(isPreviewHash('#/browse/welcome')).toBe(false);
    expect(isPreviewHash('#admin/repository/repositories')).toBe(false);
    expect(isPreviewHash('')).toBe(false);
    expect(isPreviewHash(null)).toBe(false);
    expect(isPreviewHash(undefined)).toBe(false);
  });
});

describe('returnToTargetsPreview', () => {
  it('returns true for a Base64-encoded preview returnTo', () => {
    expect(returnToTargetsPreview(btoa('#preview/browse/welcome'))).toBe(true);
    expect(returnToTargetsPreview(btoa('#/preview/browse/welcome'))).toBe(true);
  });

  it('returns false for a Base64-encoded classic returnTo', () => {
    expect(returnToTargetsPreview(btoa('#browse/welcome'))).toBe(false);
    expect(returnToTargetsPreview(btoa('#admin/repository/repositories'))).toBe(false);
  });

  it('handles raw (non-Base64) hash values', () => {
    expect(returnToTargetsPreview('#preview/browse/welcome')).toBe(true);
    expect(returnToTargetsPreview('#admin/repository/repositories')).toBe(false);
  });

  it('returns false for empty values', () => {
    expect(returnToTargetsPreview('')).toBe(false);
    expect(returnToTargetsPreview(null)).toBe(false);
    expect(returnToTargetsPreview(undefined)).toBe(false);
  });
});

describe('isClassicLandingHash', () => {
  it('returns true for Classic landing pages and empty values', () => {
    expect(isClassicLandingHash('#browse/welcome')).toBe(true);
    expect(isClassicLandingHash('#/browse/welcome')).toBe(true);
    expect(isClassicLandingHash('#')).toBe(true);
    expect(isClassicLandingHash('#/')).toBe(true);
    expect(isClassicLandingHash('')).toBe(true);
    expect(isClassicLandingHash(null)).toBe(true);
    expect(isClassicLandingHash(undefined)).toBe(true);
  });

  it('returns false for Classic deep links and Preview hashes', () => {
    expect(isClassicLandingHash('#admin/security/users')).toBe(false);
    expect(isClassicLandingHash('#browse/browse')).toBe(false);
    expect(isClassicLandingHash('#preview/browse/welcome')).toBe(false);
  });
});

describe('returnToTargetsClassicLanding', () => {
  it('returns true for a missing returnTo (default applies)', () => {
    expect(returnToTargetsClassicLanding('')).toBe(true);
    expect(returnToTargetsClassicLanding(null)).toBe(true);
    expect(returnToTargetsClassicLanding(undefined)).toBe(true);
  });

  it('returns true for a Base64-encoded Classic landing returnTo', () => {
    expect(returnToTargetsClassicLanding(btoa('#browse/welcome'))).toBe(true);
    expect(returnToTargetsClassicLanding(btoa('#/'))).toBe(true);
  });

  it('returns false for a Classic deep link (preserved)', () => {
    expect(returnToTargetsClassicLanding(btoa('#admin/security/users'))).toBe(false);
    expect(returnToTargetsClassicLanding('#admin/security/users')).toBe(false);
  });

  it('returns false for a Preview returnTo (preserved)', () => {
    expect(returnToTargetsClassicLanding(btoa('#preview/browse/welcome'))).toBe(false);
  });
});
