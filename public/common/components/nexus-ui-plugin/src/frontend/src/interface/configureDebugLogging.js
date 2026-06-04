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

const SUPPRESS_PATTERNS = [
  'NX.app.Loader',
  'NX.app.Application',
  'evaluating transition',
  'Redirecting to login',
  'managed controllers',
  'Configured error handling',
  'Configured Ext.Direct',
  'Configured state provider',
  'responsive plugin is deprecated',
  '[verify]',
];

/**
 * Patches console methods to suppress noisy ExtJS internal chatter during boot.
 * Safe to call multiple times — guarded by a window flag.
 */
export default function configureDebugLogging() {
  if (typeof window === 'undefined' || window.__nxExtJsDebugPatchedGlobal) {
    return;
  }
  window.__nxExtJsDebugPatchedGlobal = true;

  const shouldSuppress = (args) =>
    args.some((arg) => typeof arg === 'string' &&
      SUPPRESS_PATTERNS.some(pattern => arg.includes(pattern)));

  const wrap = (originalFn) => (...args) => {
    if (shouldSuppress(args)) {
      return;
    }
    originalFn && originalFn(...args);
  };

  console.debug = wrap(console.debug?.bind(console));
  console.info = wrap(console.info?.bind(console));
  console.log = wrap(console.log?.bind(console));
  console.warn = wrap(console.warn?.bind(console));
}
