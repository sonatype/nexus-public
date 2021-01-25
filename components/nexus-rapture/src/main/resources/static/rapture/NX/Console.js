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
 * Console helper.
 *
 * @since 3.0
 */
Ext.define('NX.Console', {
  singleton: true,

  /**
   * @private
   */
  console: undefined,

  /**
   * Disable all application console output.
   *
   * @public
   * @property {Boolean}
   */
  disable: false,

  /**
   * Set to true to enable console 'trace'.
   *
   * @public
   * @property {Boolean}
   */
  traceEnabled: false,

  /**
   * Set to false to disable console 'debug'.
   *
   * @public
   * @property {Boolean}
   */
  debugEnabled: true,

  /**
   * Set up the console environment.
   */
  constructor: function () {
    this.console = NX.global.console || {};

    // apply default empty functions to console if missing
    Ext.applyIf(this.console, {
      log: Ext.emptyFn,
      info: Ext.emptyFn,
      warn: Ext.emptyFn,
      error: Ext.emptyFn
    });

    // use ?debug to enable
    this.debugEnabled = NX.global.location.href.search("[?&]debug") > -1;

    // use ?debug&trace to enable
    this.traceEnabled = NX.global.location.href.search("[?&]trace") > -1;
  },

  /**
   * Output a message to console at given level.
   *
   * @public
   * @param {String} level
   * @param {Array} args
   */
  log: function (level, args) {
    var c = this.console;
    switch (level) {
      case 'trace':
        if (this.traceEnabled) {
          c.log.apply(c, args);
        }
        break;

      case 'debug':
        if (this.debugEnabled) {
          c.log.apply(c, args);
        }
        break;

      case 'info':
        c.info.apply(c, args);
        break;

      case 'warn':
        c.warn.apply(c, args);
        break;

      case 'error':
        c.error.apply(c, args);
        break;
    }
  },

  /**
   * Outputs a trace message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  trace: function() {
    this.log('trace', Array.prototype.slice.call(arguments));
  },

  /**
   * Outputs a debug message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  debug: function () {
    this.log('debug', Array.prototype.slice.call(arguments));
  },

  /**
   * Outputs an info message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  info: function () {
    this.log('info', Array.prototype.slice.call(arguments));
  },

  /**
   * Outputs a warn message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  warn: function () {
    this.log('warn', Array.prototype.slice.call(arguments));
  },

  /**
   * Outputs an error message.
   *
   * @public
   * @param {String/Object/Array} message
   */
  error: function () {
    this.log('error', Array.prototype.slice.call(arguments));
  },

  /**
   * Helper to record event details to console.
   *
   * @internal
   * @param event
   */
  recordEvent: function(event) {
    this.log(event.level, [event.level, event.logger, event.message.join(' ')]);
  }
});