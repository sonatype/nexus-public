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
 * Support Request controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SupportRequest', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.State',
    'NX.Permissions',
    'NX.Windows',
    'NX.I18n'
  ],

  views: [
    'support.SupportRequest'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Support Request',
      text: NX.I18n.get('SupportRequest_Text'),
      description: NX.I18n.get('SupportRequest_Description'),
      view: { xtype: 'nx-coreui-support-supportrequest' },
      iconConfig: {
        file: 'premium_support.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        // only show if edition is not OSS (ie. PRO or trial) and we have perms
        return NX.State.getEdition() !== 'OSS' &&
            NX.Permissions.check('nexus:atlas:create');
      }
    }, me);

    me.listen({
      component: {
        'nx-coreui-support-supportrequest button[action=makerequest]': {
          click: me.makeRequest
        }
      }
    });
  },

  /**
   * @private
   * Open sonatype support in a new browser window/tab.
   */
  makeRequest: function () {
    NX.Windows.open('https://links.sonatype.com/products/nexus/pro/support-request');
  }

});
