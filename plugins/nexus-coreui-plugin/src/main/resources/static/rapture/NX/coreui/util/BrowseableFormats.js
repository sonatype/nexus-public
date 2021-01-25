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
/*global Ext*/

/**
 * BrowseableFormats helper.
 *
 * @since 3.2.1
 */
Ext.define('NX.coreui.util.BrowseableFormats', {
  singleton: true,
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Array of visible formats.
   *
   * @private
   */
  formats: undefined,

  /**
   * @public
   * @returns {boolean} True, if formats had been set (loaded from server)
   */
  available: function() {
    return Ext.isDefined(this.formats);
  },

  /**
   * Sets formats.
   *
   * @public
   * @param {Object} formats
   */
  setFormats: function(formats) {
    var me = this;

    // defensive copy
    me.formats = Ext.clone(formats);

    //<if debug>
    me.logDebug('BrowseableFormats installed');
    //</if>
  },

  /**
   * Resets all formats.
   *
   * @public
   */
  resetFormats: function() {
    var me = this;

    //<if debug>
    me.logDebug('Resetting formats');
    //</if>

    delete me.formats;
  },

  /**
   * Check if the format is in the current list of formats.
   *
   * @public
   * @param {String} format
   * @returns {boolean} True if format is in the list of visible formats.
   */
  check: function(format) {
    var me = this;

    if (!me.available()) {
      return false;
    }

    return Ext.Array.contains(me.formats, format);
  }
});
