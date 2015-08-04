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
/*global NX, Ext, Sonatype, Nexus*/

/**
 * Controller.
 *
 * @since 2.7
 */
NX.define('Nexus.controller.Controller', {
  mixins: [
    'Nexus.LogAwareMixin'
  ],

  control: function (config) {
    var me = this;
    Ext.iterate(config, function (key) {
      if (key.startsWith('#')) {
        // closure to register control events on given object
        function register(obj) {
          var events = config['#' + obj.id];

          Ext.iterate(events, function (event) {
            obj.on(event, events[event], me);
            me.logDebug('Registered for event "' + event + '" on ' + obj.id);
          });
        }

        var id = key.substring(1),
            obj = Ext.ComponentMgr.get(id);

        // If component already exists, register events on it
        if (Ext.isObject(obj)) {
          register(obj);
        }
        else {
          // else when the component is created we will register events then
          Ext.ComponentMgr.onAvailable(id, register, me);
        }
      }
    });
  },

  /**
   * Parses an error message out of an Ajax response.
   * @param response Ajax response
   * @param {String} [defaultMessage] to be used if a message could not be extracted from response
   * @returns {String} parsed message
   */
  parseExceptionMessage: function (response, defaultMessage) {
    var message;
    if (response.siestaError) {
      message = response.siestaError.message;
    }
    if (Ext.isEmpty(message) && response.responseText) {
      message = Sonatype.utils.parseHTMLErrorMessage(response.responseText);
    }
    if (Ext.isEmpty(message)) {
      message = response.statusText;
    }
    if (Ext.isEmpty(message)) {
      message = defaultMessage;
    }
    if (Ext.isEmpty(message)) {
      message = 'Could not be determined';
    }
    return message;
  }

});