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
 * Global logging helper.
 *
 * @since 3.0
 */
Ext.define('NX.Log', {
  singleton: true,
  requires: [
    'NX.Console'
  ],

  /**
   * Reference to attached logging controller.
   *
   * @private
   * @property {NX.controller.Logging}
   */
  controller: undefined,

  /**
   * Queue of events logged before controller is attached.
   * This is deleted upon attachment after events are passed to the controller.
   *
   * @private
   */
  eventQueue: [],

  /**
   * Attach to the logging controller.
   *
   * @internal
   * @param {NX.controller.Logging} controller
   */
  attach: function (controller) {
    var me = this;
    me.controller = controller;

    // reply queued events and clear
    Ext.each(me.eventQueue, function (event) {
      me.controller.recordEvent(event);
    });
    delete me.eventQueue;
  },

  /**
   * Record a log event.
   *
   * @public
   * @param {String} level
   * @param {String} logger
   * @param {String/Array} message
   */
  recordEvent: function (level, logger, message) {
    var me = this,
        event = {
          timestamp: Date.now(),
          level: level,
          logger: logger,
          message: message
        };

    // if controller is attached, delegate to record the event
    if (me.controller) {
      me.controller.recordEvent(event);
    }
    else {
      // else queue the event and emit to console
      me.eventQueue.push(event);
      NX.Console.recordEvent(event);
    }
  }
});