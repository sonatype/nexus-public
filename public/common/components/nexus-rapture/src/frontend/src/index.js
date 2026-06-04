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
// Suppress noisy NX.app.Loader console chatter early so debug logs stay readable.
if (typeof window !== 'undefined' && !window.__nxExtJsDebugPatchedRapture) {
  window.__nxExtJsDebugPatchedRapture = true;
  const shouldSuppressLoader = (args) =>
    args.some((arg) => typeof arg === 'string' && arg.includes('NX.app.Loader'));

  const wrap = (originalFn) => (...args) => {
    if (shouldSuppressLoader(args)) {
      return;
    }
    originalFn && originalFn(...args);
  };

  console.debug = wrap(console.debug?.bind(console));
  console.info = wrap(console.info?.bind(console));
  console.log = wrap(console.log?.bind(console));
  console.warn = wrap(console.warn?.bind(console));
  console.error = wrap(console.error?.bind(console));

  // Also silence NX.Log (used by NX.app.Loader) once it appears.
  const trySilenceNxLog = () => {
    const nxLog = window.NX && window.NX.Log;
    if (!nxLog || nxLog.__silencedForLoader) {
      return false;
    }
    ['logInfo', 'info', 'logDebug', 'debug', 'logWarn', 'warn', 'logError', 'error'].forEach((fn) => {
      if (typeof nxLog[fn] === 'function') {
        nxLog[fn] = () => {};
      }
    });
    nxLog.__silencedForLoader = true;
    return true;
  };

  const nxLogPoll = setInterval(() => {
    if (trySilenceNxLog()) {
      clearInterval(nxLogPoll);
    }
  }, 50);

  // Stop polling after 10s to avoid runaway timers.
  setTimeout(() => clearInterval(nxLogPoll), 10000);
}

console.info('[verify] rapture bundle from NEXUS_RESOURCE_DIRS loaded');

import exposeDependencies from './exposeDependencies';
import configureDebug from './configureDebug.js';

exposeDependencies();
configureDebug();
