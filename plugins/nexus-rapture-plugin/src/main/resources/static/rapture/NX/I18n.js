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
/*global Ext, NX*/

/**
 * I18n helper.
 *
 * @since 3.0
 */
Ext.define('NX.I18n', {
  singleton: true,
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * @private
   * @property {Object}
   */
  keys: {},

  /**
   * @public
   * @param {Object} keys
   */
  register: function(keys) {
    Ext.apply(this.keys, keys);
  },

  /**
   * @public
   * @param {String} key
   */
  get: function(key) {
    var text = this.keys[key];
    if (text === null || text === undefined) {
      this.logWarn('Missing I18n key:', key);
      return 'MISSING_I18N:' + key;
    }
    return text;
  },

  /**
   * @public
   * @param {String} key
   * @param {Object...} values
   * @returns {String}
   */
  format: function(key) {
    var text = this.get(key);
    if (text) {
      var params = Array.prototype.slice.call(arguments);
      // replace first element with text
      params.shift();
      params.unshift(text);
      text = Ext.String.format.apply(this, params);
    }
    return text;
  }
});