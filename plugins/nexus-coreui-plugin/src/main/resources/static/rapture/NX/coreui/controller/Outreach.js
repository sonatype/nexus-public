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
 * Outreach controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Outreach', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.util.Url',
    'NX.State',
    'NX.Permissions'
  ],

  refs: [
    { ref: 'welcomePage', selector: 'nx-dashboard-welcome' }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.refreshOutreachContent
        },
        '#State': {
          userchanged: me.refreshOutreachContent
        }
      },
      component: {
        'nx-dashboard-welcome': {
          afterrender: me.refreshOutreachContent
        }
      }
    });
  },

  /**
   * @private
   * Add/Remove outreach content to/from welcome page, if outreach content is available.
   */
  refreshOutreachContent: function () {
    var me = this,
        welcomePage = me.getWelcomePage();

    if (welcomePage) {
      NX.direct.outreach_Outreach.readStatus(function (response) {
        if (Ext.isObject(response) && response.success) {
          var user = NX.State.getUser(),
              usertype,
              url;

          if (user) {
            usertype = user.administrator ? 'admin' : 'normal';
          }
          else {
            usertype = 'anonymous';
          }

          url = NX.util.Url.urlOf('service/outreach/?version=' + NX.State.getVersion() +
              '&versionMm=' + NX.State.getVersionMajorMinor() +
              '&edition=' + NX.State.getEdition() +
              '&usertype=' + usertype);

          var outreachContent = welcomePage.down('#outreach');
          if (outreachContent) {
            welcomePage.remove(outreachContent, true);
          }

          // add the outreach iframe to the welcome view
          welcomePage.add({
            xtype: 'uxiframe',
            itemId: 'outreach',
            anchor: '100%',
            width: '100%',
            flex: 1,
            border: false,
            frame: false,
            hidden: true,
            src: url,
            listeners: {
              load: function () {
                var iframe = this;
                // if the outreach content has loaded properly, show it
                if (iframe.getWin().iframeLoaded) {
                  iframe.show();
                }
                else {
                  // else complain and leave it hidden
                  //<if debug>
                  me.logDebug('Outreach iframe did not load: ' + url);
                  //</if>
                }
              }
            }
          });
        }
      });
    }
  }

});
