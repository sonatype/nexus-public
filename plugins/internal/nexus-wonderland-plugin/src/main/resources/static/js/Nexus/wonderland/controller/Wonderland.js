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
/*global NX, Ext, Sonatype, Nexus*/

/**
 * Wonderland controller.
 *
 * @since 2.7
 */
NX.define('Nexus.wonderland.controller.Wonderland', {
  extend: 'Nexus.controller.Controller',

  requires: [
    'Nexus.siesta'
  ],

  init: function () {
    var me = this;

    me.control({
      '#nx-wonderland-view-authenticate': {
        'validate-credentials': me.validateCredentials
      }
    });
  },

  /**
   * @private
   *
   * @param {AuthenticateWindow} authwin
   * @param {String} username
   * @param {String} password
   */
  validateCredentials: function (authwin, username, password) {
    var me = this, mask;

    mask = new Ext.LoadMask(authwin.getEl(), {
      msg: 'Authenticating'
    });
    mask.show();

    Ext.Ajax.request({
      url: Nexus.siesta.basePath + '/wonderland/authenticate',
      method: 'POST',
      suppressStatus: [
        403 // Used to signal from the server unauthenticated state, don't display an error box for this response
      ],

      jsonData: {
        u: Sonatype.utils.base64.encode(username),
        p: Sonatype.utils.base64.encode(password)
      },

      scope: me,

      callback: function () {
        mask.hide();
      },

      success: function (response) {
        me.logDebug('Authenticated');

        var authTicket = Ext.decode(response.responseText);
        me.logDebug('Ticket: ' + authTicket.t);

        // fire event and close window
        authwin.fireEvent('authenticated', me, authTicket.t);
        authwin.close();
      },

      failure: function (response) {
        if (response.status === 403) {
          authwin.markInvalid();
        }
      }
    });
  }
});