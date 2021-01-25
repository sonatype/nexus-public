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
 * Helpers to show dialog boxes.
 *
 * @since 3.0
 */
Ext.define('NX.Dialogs', {
  singleton: true,
  requires: [
    'NX.I18n'
  ],

  /**
   * Show information dialog.
   *
   * @public
   */
  showInfo: function (title, message, options, htmlEncoded) {
    options = options || {};

    if (!htmlEncoded) {
      message = Ext.htmlEncode(message);
      title = title ? Ext.htmlEncode(title) : title;
    }

    // set default configuration
    Ext.applyIf(options, {
      title: title || NX.I18n.get('Dialogs_Info_Title'),
      msg: message,
      buttons: Ext.Msg.OK,
      icon: Ext.MessageBox.INFO,
      closable: true
    });

    Ext.Msg.show(options);
  },

  /**
   * Show error dialog.
   *
   * @public
   */
  showError: function (title, message, options, htmlEncoded) {
    options = options || {};

    if (!htmlEncoded) {
      message = Ext.htmlEncode(message);
      title = title ? Ext.htmlEncode(title) : title;
    }

    // set default configuration
    Ext.applyIf(options, {
      title: title || NX.I18n.get('Dialogs_Error_Title'),
      msg: message || NX.I18n.get('Dialogs_Error_Message'),
      buttons: Ext.Msg.OK,
      icon: Ext.MessageBox.ERROR,
      closable: true
    });

    Ext.Msg.show(options);
  },

  /**
   * Show confirmation dialog.
   *
   * @public
   */
  askConfirmation: function (title, message, onYesFn, options, htmlEncoded) {
    options = options || {};

    if (!htmlEncoded) {
      message = Ext.htmlEncode(message);
      title = title ? Ext.htmlEncode(title) : title;
    }

    Ext.Msg.show({
      title: title,
      msg: message,
      buttons: Ext.Msg.YESNO,
      icon: Ext.MessageBox.QUESTION,
      closable: false,
      animEl: options.animEl,
      buttonText: options.buttonText,
      fn: function (buttonName) {
        if (buttonName === 'yes' || buttonName === 'ok') {
          if (Ext.isDefined(onYesFn)) {
            onYesFn.call(options.scope);
          }
        }
        else {
          if (Ext.isDefined(options.onNoFn)) {
            options.onNoFn.call(options.scope);
          }
        }
      }
    });
  }

});
