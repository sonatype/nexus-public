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
 * Helpers to interact with Message controller.
 *
 * @since 3.0
 */
Ext.define('NX.Messages', {
  singleton: true,

  /**
   * Add a new custom message.
   *
   * @public
   * @param {Object} message
   * @param {boolean} indicates that the message is already encoded
   */
  add: function (message, encoded) {
    message.text = encoded ? message.text : Ext.util.Format.htmlEncode(message.text);
    NX.getApplication().getMessageController().addMessage(message);
  },

  /**
   * Check if a message with a matching type already exists
   * @public
   * @param {Object} message
   */
  messageExists: function(message) {
    return NX.getApplication().getMessageController().messageExists(message);
  },

  //
  // High-level helpers
  //

  /**
   * Add a notice message.
   *
   * @public
   * @param {string} message
   * @param {boolean} indicates that the message is already encoded
   */
  notice: function (message, encoded) {
    this.add({type: 'default', text: message}, encoded);
  },

  /**
   * Add an info message.
   *
   * @public
   * @param {string} message
   * @param {boolean} indicates that the message is already encoded
   */
  info: function (message, encoded) {
    this.add({type: 'primary', text: message}, encoded);
  },

  /**
   * Add an error message.
   *
   * @public
   * @param {string} message
   * @param {boolean} indicates that the message is already encoded
   */
  error: function (message, encoded) {
    this.add({type: 'danger', text: message}, encoded);
  },

  /**
   * Add a warning message.
   *
   * @public
   * @param {string} message
   * @param {boolean} indicates that the message is already encoded
   */
  warning: function (message, encoded) {
    this.add({type: 'warning', text: message}, encoded);
  },

  /**
   * Add a success message.
   *
   * @public
   * @param {string} message
   * @param {boolean} indicates that the message is already encoded
   */
  success: function (message, encoded) {
    this.add({type: 'success', text: message}, encoded);
  }
});
