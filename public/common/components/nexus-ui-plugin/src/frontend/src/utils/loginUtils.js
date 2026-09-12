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

/**
 * Parses a Retry-After header value (RFC 9110 §10.2.3).
 * Accepts either a delay-seconds integer or an HTTP-date string.
 * Returns null if the header is absent or unparseable.
 */
export function parseRetryAfter(header) {
  if (!header) return null;
  const seconds = Number(header);
  if (!isNaN(seconds)) return Math.max(1, seconds);
  const delta = Math.ceil((new Date(header) - Date.now()) / 1000);
  return isFinite(delta) && delta > 0 ? delta : null;
}

/**
 * sessionStorage key set when a user explicitly switches to the Classic (Heritage) UI.
 * Mirrors the key used by the global header's "Switch to Classic UI" control.
 */
export const USER_REQUESTED_LEGACY_KEY = 'user_requested_legacy';

/** ui-router state name for the Nexus One (Preview) UI dashboard. */
export const PREVIEW_WELCOME_ROUTE = 'preview.browse.welcome';

/** ui-router state name for the Classic UI dashboard. */
export const CLASSIC_WELCOME_ROUTE = 'browse.welcome';

/** URL hash form of the Preview welcome route, as the SSO SAML/OIDC backends expect (Base64-decoded). */
export const PREVIEW_WELCOME_HASH = '#preview/browse/welcome';

/**
 * Resolves which welcome/dashboard route a user should land on when there is no
 * explicit returnTo target. Lands on the Preview dashboard only when the admin has
 * enabled "Default to Nexus One UI", the user is allowed into Preview, and the user
 * has not explicitly chosen the Classic UI in this browser session.
 *
 * @param {Object} opts
 * @param {boolean} opts.defaultToPreviewUi - the defaultToPreviewUi admin setting
 * @param {boolean} opts.canAccessPreviewUi - whether this user may use Preview
 *   (loggedInEnabled when authenticated, anonymousEnabled when anonymous)
 * @param {boolean} opts.userRequestedLegacy - user opted into Classic this session
 * @return {string} a ui-router state name
 */
export function resolveDefaultLandingRoute({ defaultToPreviewUi, canAccessPreviewUi, userRequestedLegacy }) {
  if (defaultToPreviewUi && canAccessPreviewUi && !userRequestedLegacy) {
    return PREVIEW_WELCOME_ROUTE;
  }
  return CLASSIC_WELCOME_ROUTE;
}

/**
 * True when a decoded returnTo hash/path targets the Preview UI
 * (e.g. '#preview/browse/welcome' or '#/preview/...').
 *
 * @param {string} hash - a URL hash or path (with or without a leading '#'/'/')
 * @return {boolean}
 */
export function isPreviewHash(hash) {
  if (!hash) {
    return false;
  }
  const path = String(hash).replace(/^#/, '').replace(/^\//, '');
  return path === 'preview' || path.startsWith('preview/');
}

/**
 * True when a returnTo param targets the Preview UI. Accepts either the Base64-encoded
 * '#...' hash produced by the login redirects, or a raw '#...'/'/...' value. Used to
 * decide whether a fresh login should honor the returnTo or override a Classic
 * destination with the Preview dashboard (NEXUS-53957).
 *
 * @param {string} returnToParam - Base64-encoded or raw returnTo value
 * @return {boolean}
 */
/**
 * True when a decoded returnTo hash/path targets a Classic landing page (the dashboard or
 * app root) — the only Classic destinations the "Default to Nexus One UI" soft default
 * overrides. Classic deep links are preserved. A missing hash counts as a landing page (no
 * explicit destination → the default applies). Mirrors the isLandingPage gate in
 * GlobalHeaderRadix so login and the header share one soft-default contract (NEXUS-53957).
 *
 * @param {string} hash - a URL hash or path (with or without a leading '#'/'/')
 * @return {boolean}
 */
export function isClassicLandingHash(hash) {
  if (!hash) {
    return true;
  }
  const path = String(hash).replace(/^#/, '').replace(/^\//, '');
  return path === '' || path === 'browse/welcome';
}

/**
 * Decodes a returnTo param (Base64-encoded '#...' hash, or a raw '#...'/'/...' value) to its
 * hash/path form. Returns '' when there is no returnTo.
 */
function decodeReturnTo(returnToParam) {
  if (!returnToParam) {
    return '';
  }
  let value = String(returnToParam);
  try {
    const decoded = atob(value);
    if (decoded.startsWith('#') || decoded.startsWith('/')) {
      value = decoded;
    }
  }
  catch {
    // Not Base64 — treat the original value as a raw hash/path.
  }
  return value;
}

/**
 * True when a returnTo param targets the Preview UI. Accepts Base64-encoded or raw values.
 */
export function returnToTargetsPreview(returnToParam) {
  return isPreviewHash(decodeReturnTo(returnToParam));
}

/**
 * True when a returnTo param is missing or targets a Classic landing page. Accepts
 * Base64-encoded or raw values. Used to decide whether a fresh login should override the
 * returnTo with the Preview dashboard (landing pages only) while preserving Classic deep
 * links (NEXUS-53957).
 */
export function returnToTargetsClassicLanding(returnToParam) {
  return isClassicLandingHash(decodeReturnTo(returnToParam));
}
