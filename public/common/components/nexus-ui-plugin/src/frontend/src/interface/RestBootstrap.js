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
 * REST Bootstrap - Phase 2: React-First Shell
 *
 * Fetches auth, permissions, and application state via REST endpoints
 * and seeds window.NX globals BEFORE React renders. This allows
 * ExtJS.useUser(), ExtJS.checkPermission(), ExtJS.state().getValue()
 * to return real data immediately without waiting for ExtJS to boot.
 *
 * When ExtJS eventually loads (~3-5 seconds later), it overwrites
 * window.NX.State and window.NX.Permissions with its own implementations.
 * The React hooks auto-update via ExtJS store event subscriptions.
 *
 * Endpoints used:
 *   GET  /service/extdirect/poll/rapture_State_get  - User, edition, version, state values
 *   POST /service/extdirect                          - rapture_Security.getPermissions
 */

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const STATE_POLL_URL = '/service/extdirect/poll/rapture_State_get';
const EXTDIRECT_URL = '/service/extdirect';
const CSRF_COOKIE_NAME = 'NX-ANTI-CSRF-TOKEN';

// ---------------------------------------------------------------------------
// CSRF Token (same logic as ExtJS Application.js)
// ---------------------------------------------------------------------------

function getCookie(name) {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(
    new RegExp('(?:^|;\\s*)' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '=([^;]*)')
  );
  return match ? decodeURIComponent(match[1]) : null;
}

function setCookie(name, value) {
  if (typeof document !== 'undefined') {
    document.cookie = `${name}=${encodeURIComponent(value)}; path=/`;
  }
}

function ensureCSRFToken() {
  let token = getCookie(CSRF_COOKIE_NAME);
  if (!token) {
    token = Math.random().toString();
    setCookie(CSRF_COOKIE_NAME, token);
  }
  return token;
}

// ---------------------------------------------------------------------------
// Shiro WildcardPermission Matching
// Ported from NX.Permissions.implies() in nexus-rapture
// ---------------------------------------------------------------------------

function implies(granted, expected) {
  const grantedParts = granted.split(':');
  const expectedParts = expected.split(':');

  for (let i = 0; i < expectedParts.length; i++) {
    if (i >= grantedParts.length) return true;

    const gSubParts = grantedParts[i].split(',');
    const eSubParts = expectedParts[i].split(',');

    if (gSubParts.includes('*')) continue;

    if (!eSubParts.every((sub) => gSubParts.includes(sub))) {
      return false;
    }
  }

  for (let i = expectedParts.length; i < grantedParts.length; i++) {
    if (!grantedParts[i].split(',').includes('*')) {
      return false;
    }
  }

  return true;
}

// ---------------------------------------------------------------------------
// Permission Check with Cache
// ---------------------------------------------------------------------------

let _permCache = new Map();
let _permSet = null;

function checkPermission(permissions, permission) {
  if (permissions !== _permSet) {
    _permCache = new Map();
    _permSet = permissions;
  }

  const lower = permission.toLowerCase();
  const cached = _permCache.get(lower);
  if (cached !== undefined) return cached;

  let result = false;
  if (permissions.has(lower)) {
    result = true;
  } else {
    for (const granted of permissions) {
      if (implies(granted, lower)) {
        result = true;
        break;
      }
    }
  }

  _permCache.set(lower, result);
  return result;
}

// ---------------------------------------------------------------------------
// REST Fetchers
// ---------------------------------------------------------------------------

/**
 * Fetch auth state and all application state values from the polling endpoint.
 *
 * Returns { user, edition, version, stateValues } or null on failure.
 */
async function fetchAuthAndState() {
  const csrfToken = ensureCSRFToken();

  const response = await fetch(STATE_POLL_URL, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      [CSRF_COOKIE_NAME]: csrfToken,
      'X-Nexus-UI': 'true',
    },
  });

  if (!response.ok) {
    return null;
  }

  const result = await response.json();
  const data = result?.data?.data;

  if (!data || typeof data !== 'object') {
    return null;
  }

  // Unwrap StateValueXO wrappers: { hash, value } → value
  const stateValues = {};
  for (const [key, wrapper] of Object.entries(data)) {
    if (wrapper && typeof wrapper === 'object' && 'value' in wrapper) {
      stateValues[key] = wrapper.value;
    } else {
      stateValues[key] = wrapper;
    }
  }

  // Extract user
  const userValue = stateValues.user || null;
  const user = userValue
    ? {
        id: userValue.id,
        authenticated: userValue.authenticated,
        administrator: userValue.administrator,
        authenticatedRealms: userValue.authenticatedRealms || [],
      }
    : null;

  // Extract edition and version from status
  const statusValue = stateValues.status || {};
  let edition = statusValue.edition || null;
  const version = statusValue.version || null;

  // Fallback: try license for edition
  if (!edition && stateValues.license?.edition) {
    edition = stateValues.license.edition;
  }

  // Default to PRO if authenticated but edition unknown
  if (!edition && user?.authenticated) {
    edition = 'PRO';
  }

  return { user, edition, version, stateValues };
}

/**
 * Fetch permissions via ExtDirect RPC.
 *
 * Returns a Set of lowercase permission strings, or empty Set on failure.
 */
async function fetchPermissions() {
  const csrfToken = ensureCSRFToken();

  try {
    const response = await fetch(EXTDIRECT_URL, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        [CSRF_COOKIE_NAME]: csrfToken,
        'X-Nexus-UI': 'true',
      },
      body: JSON.stringify({
        action: 'rapture_Security',
        method: 'getPermissions',
        data: null,
        type: 'rpc',
        tid: Date.now(),
      }),
    });

    if (!response.ok) {
      return new Set();
    }

    const result = await response.json();
    const data = result?.result?.data;

    if (Array.isArray(data)) {
      return new Set(data.map((p) => p.id.toLowerCase()));
    }
  } catch (e) {
    console.warn('[RestBootstrap] Failed to fetch permissions:', e);
  }

  return new Set();
}

// ---------------------------------------------------------------------------
// Seed window.NX Globals
// ---------------------------------------------------------------------------

/**
 * Store REST-fetched data in a SEPARATE namespace (window.__nxRestBootstrap).
 *
 * IMPORTANT: We do NOT touch window.NX.State or window.NX.Permissions.
 * Those are owned by ExtJS. Overwriting them breaks ExtJS initialization
 * (e.g., NX.State.setBrowserSupported is not a function).
 *
 * Instead, ExtJS.js fallbacks are enhanced to read from __nxRestBootstrap
 * when the real NX.State/NX.Permissions aren't available yet.
 */
function seedRestData(authState, permissions) {
  if (typeof window === 'undefined') return;

  const { user, edition, version, stateValues } = authState;

  // Ensure status is in stateValues
  if (!stateValues.status) {
    stateValues.status = { edition, version };
  }

  // Store in separate namespace - ExtJS.js reads from here as fallback
  window.__nxRestBootstrap = {
    user,
    edition,
    version,
    stateValues,
    permissions,
    checkPermission: (permission) => checkPermission(permissions, permission),
  };

  console.info(
    `[RestBootstrap] Stored REST data: user=${user?.id || 'anonymous'}, edition=${edition}, permissions=${permissions.size}`
  );
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Bootstrap the application from REST endpoints.
 *
 * Fetches auth state and permissions in parallel, then seeds window.NX globals.
 * Returns a promise that resolves when seeding is complete.
 *
 * On failure (e.g., 401 not authenticated), seeds with empty/anonymous state
 * so the app can still render (and redirect to login if needed).
 */
export async function bootstrapFromREST() {
  try {
    const [authState, permissions] = await Promise.all([
      fetchAuthAndState(),
      fetchPermissions(),
    ]);

    if (authState) {
      seedRestData(authState, permissions);
    } else {
      seedRestData(
        { user: null, edition: null, version: null, stateValues: {} },
        new Set()
      );
    }
  } catch (error) {
    console.error('[RestBootstrap] Bootstrap failed:', error);
    seedRestData(
      { user: null, edition: null, version: null, stateValues: {} },
      new Set()
    );
  }
}
