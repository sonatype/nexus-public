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
 * Message controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Message', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Icons',
    'Ext.window.Toast'
  ],

  /**
   * @protected
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'message-default': {
        file: 'bell.png',
        variants: ['x16', 'x32'],
        preload: true
      },
      'message-primary': {
        file: 'information.png',
        variants: ['x16', 'x32'],
        preload: true
      },
      'message-danger': {
        file: 'exclamation.png',
        variants: ['x16', 'x32'],
        preload: true
      },
      'message-warning': {
        file: 'warning.png',
        variants: ['x16', 'x32'],
        preload: true
      },
      'message-success': {
        file: 'accept.png',
        variants: ['x16', 'x32'],
        preload: true
      }
    });
  },

  /**
   * Internal customization of {@link NX.view.message.Notification} window options.
   *
   * At the moment, mainly intended for use by functional tests that need to
   * override default settings.
   *
   * @internal
   * @property {Object}
   */
  windowOptions: {},

  /**
   * @public
   * @param {object} message
   * @param {string} message.type
   * @param {string} message.text
   */
  addMessage: function (message) {
    if (!message.type) {
      message.type = 'default';
    }

    message.timestamp = new Date();

    // show transient message notification
    if (!this.messageExists(message)) {
      Ext.toast({
        ui: 'nx-message-' + message.type,
        iconCls: NX.Icons.cls('message-' + message.type, 'x16'),
        html: message.text,
        title: Ext.String.capitalize(message.type),
        align: 'tr',
        closable: true,
        anchor: Ext.ComponentQuery.query('nx-feature-content')[0],
        stickOnClick: true,
        minWidth: 150,
        maxWidth: 400,
        autoCloseDelay: NX.State.getValue('messageDuration', 5000),
        slideInDuration: NX.State.getValue('animateDuration', 800),
        slideBackDuration: NX.State.getValue('animateDuration', 500),
        hideDuration: NX.State.getValue('animateDuration', 500),
        slideInAnimation: 'elasticIn',
        slideBackAnimation: 'elasticIn',
        ariaRole: 'alert'
      });
    }
  },

  /**
   * Query to see if the message is already displayed so that we can prevent duplicates.
   * @private
   * @param message
   */
  messageExists: function (message) {
    var existingMessage = Ext.Array.filter(Ext.ComponentQuery.query('toast'), function (foundMessage) {
          return Ext.String.trim(foundMessage.config.html) === message.text;
        }
    );
    return existingMessage.length > 0;
  }
});
