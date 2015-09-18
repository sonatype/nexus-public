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
    'NX.Icons'
  ],

  views: [
    'message.Notification'
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
   */
  addMessage: function (message) {
    if (!message.type) {
      message.type = 'default';
    }

    message.timestamp = new Date();

    // show transient message notification
    var cfg = Ext.clone(this.windowOptions);
    this.getView('message.Notification').create(Ext.apply(cfg, {
      ui: 'nx-message-' + message.type,
      iconCls: NX.Icons.cls('message-' + message.type, 'x16'),
      title: Ext.String.capitalize(message.type),
      html: message.text
    }));
  }
});
