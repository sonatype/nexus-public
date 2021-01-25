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
/*global Ext, NX*/

/**
 * Clipboard related utils.
 *
 * @since 3.15
 */
Ext.define('NX.util.Clipboard', {
  singleton: true,

  /**
   * Copy specified text to clipboard
   *
   * @public
   * @param {String} text
   */
  copyToClipboard: function(text) {
    var textarea;
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
    } else if (window.clipboardData && window.clipboardData.setData) {
      window.clipboardData.setData("Text", text);
    } else {
      textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.left = '-99999px';
      textarea.style.height = '1em';
      textarea.style.width = '1em';
      document.body.appendChild(textarea);
      textarea.select();

      try {
        document.execCommand('copy');
      } catch (err) {
        console.error('Unable to copy text to clipboard: ', err);
      }

      document.body.removeChild(textarea);
    }
  }
});
