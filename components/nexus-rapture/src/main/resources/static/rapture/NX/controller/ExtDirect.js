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
 * Watches over Ext.Direct communication.
 *
 * @since 3.0
 */
Ext.define('NX.controller.ExtDirect', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Security',
    'NX.Messages',
    'NX.I18n'
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      direct: {
        '*': {
          beforecallback: me.checkResponse
        }
      }
    });
  },

  /**
   * Checks Ext.Direct response and automatically show warning messages if an error occurred.
   * If response specifies that authentication is required, will show the sign-in window.
   *
   * @private
   */
  checkResponse: function(provider, transaction) {
    var result = transaction.result,
        message;

    // FIXME: Anything that does logging here can cause Ext.Direct log event remoting to spin out of control

    if (Ext.isDefined(result)) {
      if (Ext.isDefined(result.success) && result.success === false) {

        if (Ext.isDefined(result.authenticationRequired) && result.authenticationRequired === true) {
          message = result.message;
          NX.Security.askToAuthenticate();
        }
        else if (Ext.isDefined(result.message)) {
          message = result.message;
        }
        else if (Ext.isDefined(result.messages)) {
          message = Ext.Array.from(result.messages).join('<br/>');
        }
      }

      if (Ext.isDefined(transaction.serverException)) {
        message = transaction.serverException.exception.message;
      }
    }
    else {
      message = NX.I18n.get('User_ConnectFailure_Message');
    }

    if (message) {
      NX.Messages.add({text: message, type: 'warning'});
    }

    // HACK: disabled for now as this causes problems remoting LogEvents
    ////<if debug>
    //var logMsg = transaction.action + ':' + transaction.method + " -> " + (message ? 'Failed: ' + message : 'OK');
    //if (Ext.isDefined(result) && result.errors) {
    //  logMsg += (' Errors: ' + Ext.encode(result.errors));
    //}
    //this.logDebug(logMsg);
    ////</if>
  }

});
