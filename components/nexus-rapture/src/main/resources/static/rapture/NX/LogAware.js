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
 * Adds logging support helpers to objects.
 *
 * @since 3.0
 */
Ext.define('NX.LogAware', {
  requires: [
    'NX.Log'
  ],

  /**
   * Log a message at the given level.
   *
   * @param {String} level
   * @param {Array} args
   */
  log: function (level, args) {
    //<if debug>
    NX.Log.recordEvent(level, Ext.getClassName(this), args);
    //</if>
  },

  /**
   * Log a trace message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  logTrace: function () {
    //<if debug>
    this.log('trace', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * Log a debug message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  logDebug: function () {
    //<if debug>
    this.log('debug', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * Log an info message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  logInfo: function () {
    //<if debug>
    this.log('info', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * Log a warn message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  logWarn: function () {
    //<if debug>
    this.log('warn', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * Log an error message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  logError: function () {
    //<if debug>
    this.log('error', Array.prototype.slice.call(arguments));
    //</if>
  }
});