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

const MAX_HANDOFF_AGE_MS = 5000;

// Kept on window because the production build inlines a copy of this module into each lazy-loaded page chunk. The
// coreui and cloudui copies share this key on purpose: the stored shape is identical.
const SLOT_KEY = '__nxIqServerJustSaved';

/**
 * Stores only what the Connected page reads, so the username and password never reach the window slot.
 */
export function setJustSavedIqConfig(config) {
  if (!config) {
    clearJustSavedIqConfig();
    return;
  }
  window[SLOT_KEY] = {
    value: {enabled: config.enabled, url: config.url, licensedSolutions: config.licensedSolutions},
    at: Date.now()
  };
}

/**
 * Reads the handoff without clearing it, so it is safe to call during a render that React may discard.
 */
export function peekJustSavedIqConfig() {
  const slot = window[SLOT_KEY];
  if (!slot) {
    return null;
  }
  if (Date.now() - slot.at > MAX_HANDOFF_AGE_MS) {
    clearJustSavedIqConfig();
    return null;
  }
  return slot.value;
}

export function clearJustSavedIqConfig() {
  delete window[SLOT_KEY];
}
