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
 * Date format related utils.
 *
 * @since 3.0
 */
Ext.define('NX.util.DateFormat', {
  singleton: true,

  mixins: [
    'NX.LogAware'
  ],

  /**
   * @private
   */
  defaultPatterns: {
    date: {
      'short': 'Y-M-d',       // 2013-Mar-06
      'long': 'l, F d, Y'     // Wednesday, March 06, 2013
    },

    time: {
      'short': 'H:i:s',                   // 15:49:57
      'long': 'H:i:s T (\\G\\M\\TO)'      // 15:49:57-0700 PST (GMT-0700)
    },

    datetime: {
      'short': 'Y-M-d H:i:s',                     // 2013-Mar-06 15:49:57
      'long': 'l, F d, Y H:i:s T (\\G\\M\\TO)'    // Wednesday, March 06, 2013 15:50:19 PDT (GMT-0700)
    }
  },

  /**
   * Return the date format object for the given name.
   *
   * Date format objects currently have a 'short' and 'long' variants.
   *
   *      @example
   *      var longDatetimePattern = NX.util.DateFormat.forName('datetime')['long'];
   *      var shortDatePattern = NX.util.DateFormat.forName('date')['short'];
   *
   * @public
   * @param name
   * @return {*} Date format object.
   */
  forName: function (name) {
    var format = this.defaultPatterns[name];

    // if no format, complain and return the full ISO-8601 format
    if (!name) {
      this.logWarn('Missing named format:', name);
      return 'c';
    }

    // TODO: Eventually let this customizable by user, for now its hardcoded

    return format;
  },

  /**
   * Formats the passed timestamp using the specified format pattern.
   *
   * @public
   * @param {Number} value The value to format converted to a date by the Javascript's built-in Date#parse method.
   * @param {String} [format] Any valid date format string. Defaults to {@link Ext.Date#defaultFormat}.
   * @return {String} The formatted date string
   */
  timestamp: function (value, format) {
    format = format || NX.util.DateFormat.forName('datetime')['long'];
    return value ? Ext.util.Format.date(new Date(value), format) : undefined;
  },

  /**
   * Returns a timestamp rendering function that can be reused to apply a date format multiple times efficiently.
   *
   * @public
   * @param {String} format Any valid date format string. Defaults to {@link Ext.Date#defaultFormat}.
   * @return {Function} The date formatting function
   */
  timestampRenderer: function (format) {
    return function (value) {
      return NX.util.DateFormat.timestamp(value, format);
    };
  },

  /**
   * @public
   * @returns {String} time zone
   */
  getTimeZone: function () {
    var me = this;

    if (!me.timeZone) {
      me.timeZone = new Date().toTimeString();
      me.timeZone = me.timeZone.substring(me.timeZone.indexOf(" "));
    }

    return me.timeZone;
  }

});
