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

// Shared test helper for stubbing window.location.hash. Consolidates the direct-mutation
// pattern (`window.location.hash = '...'`) that had been scattered across the roles suite
// and lets tests observe hash assignments made by the code under test.
//
// Usage:
//   beforeAll(() => installHashStub());
//   afterAll(() => uninstallHashStub());
//   beforeEach(() => setInitialHash('#preview/admin/security/roles/test-role'));
//   ...
//   expect(getHashHistory()).toContain('preview/admin/security/roles');
//
// The stub replaces the entire window.location object because JSDOM defines `hash` as
// non-configurable on the real Location object.

interface LocationStub {
  hash: string;
}

let currentHash = '';
let hashHistory: string[] = [];
let originalLocation: Location | undefined;
let dispatchOnAssign = false;

const locationStub: LocationStub = {
  get hash(): string {
    return currentHash;
  },
  set hash(value: string) {
    // Record the raw assigned value in history (so tests can assert on the path the code
    // under test navigated to, without caring about the '#' prefix). Store the normalized
    // value so getters mirror browser behavior: `window.location.hash = 'foo'` reads as '#foo'.
    hashHistory.push(value);
    currentHash = value === '' ? '' : value.startsWith('#') ? value : `#${value}`;
    if (dispatchOnAssign) {
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    }
  },
};

export interface InstallHashStubOptions {
  initialHash?: string;
  /**
   * When true, assignments to `window.location.hash` synchronously fire a
   * `hashchange` event so the code under test's listener wakes up. Defaults to false
   * so callers can decide when to fire the event manually.
   */
  dispatchOnAssign?: boolean;
}

export function installHashStub(options: InstallHashStubOptions = {}): void {
  originalLocation = window.location;
  dispatchOnAssign = options.dispatchOnAssign ?? false;
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: locationStub,
  });
  currentHash = options.initialHash ?? '';
  hashHistory = [];
}

export function uninstallHashStub(): void {
  if (originalLocation !== undefined) {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
    originalLocation = undefined;
  }
  currentHash = '';
  hashHistory = [];
  dispatchOnAssign = false;
}

/**
 * Reset the current hash to a known value without recording it in the history.
 * Use in beforeEach to set the starting URL for each test.
 */
export function setInitialHash(hash: string): void {
  currentHash = hash;
  hashHistory = [];
}

/**
 * Returns a copy of every value that has been assigned to `window.location.hash`
 * since the stub was installed or `setInitialHash` was called.
 */
export function getHashHistory(): string[] {
  return [...hashHistory];
}

/**
 * Read the current hash value directly. Prefer assertions on `getHashHistory()`
 * when checking that the code under test navigated somewhere.
 */
export function getCurrentHash(): string {
  return currentHash;
}
