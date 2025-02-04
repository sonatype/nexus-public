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
 * @since 3.77
 */
Ext.define('NX.onboarding.step.CommunityEulaStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.onboarding.view.CommunityEulaScreen'
  ],

  config: {
    screen: 'NX.onboarding.view.CommunityEulaScreen',
    enabled: true
  },

  /**
   * @override
   */
  init: function () {
    const me = this;

    me.control({
      'button[action=next]': {
        click: me.agree
      },
      'button[action=back]': {
        click: me.moveBack
      }
    });
  },

  agree: function() {
    const me = this;

    var response = Ext.Ajax.request({
      url: NX.util.Url.relativePath + '/service/rest/v1/system/eula',
      method: 'GET',
      headers: {'Content-Type': 'application/json'}
    });
    response.then(function success(response) {
      var getResponse = JSON.parse(response.responseText);
      getResponse.accepted = true;

      var postOptions = {
        url: NX.util.Url.relativePath + '/service/rest/v1/system/eula',
        method: 'POST',
        jsonData: getResponse,
        success: function() {
          me.moveNext();
        },
        failure: function(response) {
          var message;

          try {
            message = JSON.parse(response.responseText);

            if (Array.isArray(message)) {
              message = message.map(function(e) { return e.message; }).join('\n');
            }
          } catch (e) {
            message = response.statusText;
          }

          NX.Messages.error(message);
        }
      };

      Ext.Ajax.request(postOptions);
    }, function error(response) {
      var message;

      try {
        message = JSON.parse(response.responseText);

        if (Array.isArray(message)) {
          message = message.map(function(e) { return e.message; }).join('\n');
        }
      } catch (e) {
        message = response.statusText;
      }

      NX.Messages.error(message);
    });
  }
});
