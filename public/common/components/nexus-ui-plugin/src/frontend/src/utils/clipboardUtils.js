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
 * Copy text to clipboard with fallback support for browsers/contexts
 * where Clipboard API is not available.
 *
 * This function attempts to copy text using two methods:
 * 1. Modern Clipboard API (navigator.clipboard) - requires HTTPS or localhost
 * 2. execCommand fallback - works in insecure contexts (HTTP)
 *
 * @param {string} text - The text to copy to clipboard
 * @returns {Promise<boolean>} - Resolves to true on success, false on failure
 *
 * @remarks
 * The execCommand method is deprecated but remains the only option for HTTP contexts.
 * For best security and performance, deploy Nexus over HTTPS.
 */
export async function copyToClipboard(text) {
  // Validate input to prevent copying invalid values
  if (text == null || text === '') {
    console.warn('copyToClipboard called with empty/null text:', text);
    return false;
  }

  // Modern Clipboard API (requires HTTPS or localhost)
  if (navigator.clipboard && navigator.clipboard.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch (err) {
      console.warn('Clipboard API failed, trying fallback:', err);
      // Fall through to fallback methods
    }
  }

  // Fallback: execCommand with textarea (works in HTTP contexts)
  const textarea = document.createElement('textarea');
  try {
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-99999px';
    textarea.style.height = '1em';
    textarea.style.width = '1em';
    textarea.setAttribute('readonly', '');
    document.body.appendChild(textarea);
    textarea.select();

    const successful = document.execCommand('copy');
    return successful;
  } catch (err) {
    console.error('All clipboard methods failed:', err);
    return false;
  } finally {
    // Always clean up the textarea, even if execCommand throws
    document.body.removeChild(textarea);
  }
}

