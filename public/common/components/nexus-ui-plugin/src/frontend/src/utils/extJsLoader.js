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
 * ExtJS Loader Utility
 *
 * Phase 1: React Shell - Provides lazy loading mechanism for ExtJS.
 * Allows React to start immediately while ExtJS loads in the background.
 */

let extJSLoadCallbacks = [];
let extJSLoaded = false;

/**
 * Checks if ExtJS is loaded and ready.
 * @returns {boolean}
 */
export function isExtJSLoaded() {
  if (typeof window === 'undefined') {
    return false;
  }

  // Check that ExtJS is fully initialized with application and controllers
  try {
    return extJSLoaded || (
      window.Ext &&
      window.Ext.isReady &&
      window.NX &&
      typeof window.NX.getApplication === 'function' &&
      window.NX.getApplication() !== null &&
      window.NX.getApplication() !== undefined &&
      typeof window.NX.getApplication().getController === 'function'
    );
  } catch {
    // If any check throws, ExtJS is not fully ready
    return false;
  }
}

/**
 * Registers a callback to be called when ExtJS is loaded.
 * If ExtJS is already loaded, the callback is called immediately.
 * @param {Function} callback
 */
export function onExtJSLoad(callback) {
  if (isExtJSLoaded()) {
    callback();
    return;
  }

  extJSLoadCallbacks.push(callback);
}

/**
 * Marks ExtJS as loaded and triggers all registered callbacks.
 * This is called automatically when ExtJS finishes loading.
 */
function notifyExtJSLoaded() {
  extJSLoaded = true;
  extJSLoadCallbacks.forEach(callback => {
    try {
      callback();
    } catch (error) {
      console.error('Error in ExtJS load callback:', error);
    }
  });
  extJSLoadCallbacks = [];
}

/**
 * Loads ExtJS asynchronously.
 * In Phase 1, ExtJS is still bundled and loads automatically,
 * this function just waits for it to be ready.
 * @returns {Promise<void>}
 */
export function loadExtJS() {
  return new Promise((resolve, reject) => {
    if (isExtJSLoaded()) {
      resolve();
      return;
    }

    onExtJSLoad(() => {
      resolve();
    });

    // Timeout after 30 seconds
    setTimeout(() => {
      if (!isExtJSLoaded()) {
        reject(new Error('ExtJS failed to load within 30 seconds'));
      }
    }, 30000);
  });
}

// Listen for ExtJS ready event
// Skip initialization in test environment
if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'test') {
  // Suppress noisy ExtJS debug chatter while keeping other console output.
  if (!window.__nxExtJsDebugPatched) {
    window.__nxExtJsDebugPatched = true;
    
    // Patterns to suppress - ExtJS internal chatter that clutters the console
    const suppressPatterns = [
      'NX.app.Loader',
      'NX.app.Application',
      'evaluating transition',
      'Redirecting to login',
      'managed controllers',
      'Configured error handling',
      'Configured Ext.Direct',
      'Configured state provider',
      'responsive plugin is deprecated',
    ];
    
    const shouldSuppress = (firstArg) =>
      typeof firstArg === 'string' && 
      suppressPatterns.some(pattern => firstArg.includes(pattern));

    const originalDebug = console.debug?.bind(console);
    console.debug = (...args) => {
      if (shouldSuppress(args[0])) {
        return;
      }
      originalDebug && originalDebug(...args);
    };

    const originalInfo = console.info?.bind(console);
    console.info = (...args) => {
      if (shouldSuppress(args[0])) {
        return;
      }
      originalInfo && originalInfo(...args);
    };

    const originalLog = console.log?.bind(console);
    console.log = (...args) => {
      if (shouldSuppress(args[0])) {
        return;
      }
      originalLog && originalLog(...args);
    };

    const originalWarn = console.warn?.bind(console);
    console.warn = (...args) => {
      if (shouldSuppress(args[0])) {
        return;
      }
      originalWarn && originalWarn(...args);
    };
  }

  // Poll for ExtJS application to be fully initialized
  // Note: We use polling instead of Ext.onReady because onReady only means
  // the ExtJS DOM is ready, not that NX.getApplication() is functional
  const checkInterval = setInterval(() => {
    try {
      if (window.Ext &&
          window.Ext.isReady &&
          window.NX &&
          typeof window.NX.getApplication === 'function' &&
          window.NX.getApplication() &&
          typeof window.NX.getApplication().getController === 'function') {
        clearInterval(checkInterval);
        notifyExtJSLoaded();
      }
    } catch {
      // Keep polling if any check fails
    }
  }, 100);

  // Clear interval after 30 seconds
  setTimeout(() => {
    clearInterval(checkInterval);
    if (!extJSLoaded) {
      console.error('[ExtJS Loader] ExtJS failed to fully initialize within 30 seconds');
    }
  }, 30000);
}
