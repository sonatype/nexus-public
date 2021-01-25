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
   * @private
   * @property {Object}
   */
  bundles: {},

  /**
   * @public
   * @param {Object} strings
   */
  register: function (strings) {
    Ext.apply(this.keys, strings.keys);
    Ext.apply(this.bundles, strings.bundles);
  },

  /**
   * Resolves a string from a key.
   *
   * If the key begins with '@' then the remainder is assumed to be a reference to another key and will be resolved.
   *
   * @public
   * @param {String} key
   */
  get: function (key) {
    var text = this.keys[key];
    if (text === null || text === undefined) {
      this.logWarn('Missing I18n key:', key);
      return 'MISSING_I18N:' + key;
    }
    // resolve references
    if (text.charAt(0) === '@') {
      return this.get(text.substring(1, text.length));
    }
    else {
      return text;
    }
  },

  /**
   * @public
   * @param {String} key
   * @param {Object...} values
   * @returns {String}
   */
  format: function (key) {
    var text = this.get(key);
    if (text) {
      var params = Array.prototype.slice.call(arguments);
      // replace first element with text
      params.shift();
      params.unshift(text);
      text = Ext.String.format.apply(this, params);
    }
    return text;
  },

  /**
   * Render a bundle string.
   *
   * @param {Object/String} bundle
   * @param {String} key
   * @param {Object...} [params]
   * @returns {String}
   */
  render: function (bundle, key) {
    var resources, text, params;

    // resolve bundle
    if (Ext.isObject(bundle)) {
      bundle = Ext.getClassName(bundle);
    }

    //<if debug>
    this.logTrace('Resolving bundle:', bundle, 'key:', key);
    //</if>

    resources = this.bundles[bundle];
    if (resources === undefined) {
      this.logWarn('Missing I18n bundle:', bundle);
      return 'MISSING_I18N:' + bundle + ':' + key;
    }

    // resolve text
    text = resources[key];
    if (text === undefined) {
      // handle bundle extension
      var extend = resources['$extend'];
      if (extend !== undefined) {
        params = Array.prototype.slice.call(arguments, 1);
        params.unshift(extend);
        return this.render.apply(this, params);
      }

      this.logWarn('Missing I18n bundle key:', bundle, ':', key);
      return 'MISSING_I18N:' + bundle + ':' + key;
    }

    // resolve references
    if (text.charAt(0) === '@') {
      if (text.indexOf(':') !== -1) {
        // bundle ref <bundle>:[key]
        var items = text.substring(1, text.length).split(':', 2);

        // default to given key if ref missing key
        if (items[1] === '') {
          items[1] = key;
        }

        params = Array.prototype.slice.call(arguments, 2);
        params.unshift(items[1]);
        params.unshift(items[0]);
        return this.render.apply(this, params);
      }
      else {
        // key ref <key>
        text = this.get(text.substring(1, text.length));
      }
    }

    // optionally format with parameters
    if (arguments.length > 2) {
      params = Array.prototype.slice.call(arguments, 2);
      params.unshift(text);
      text = Ext.String.format.apply(this, params);
    }

    return text;
  }
});
