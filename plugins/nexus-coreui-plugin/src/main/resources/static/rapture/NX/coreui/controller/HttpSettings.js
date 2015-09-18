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
 * HTTP System Settings controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.HttpSettings', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.I18n'
  ],

  views: [
    'system.HttpSettings'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/System/HTTP',
      text: NX.I18n.get('HttpSettings_Text'),
      description: NX.I18n.get('HttpSettings_Description'),
      view: { xtype: 'nx-coreui-system-http-settings' },
      iconConfig: {
        file: 'lorry.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, me);

    me.listen({
      component: {
        'nx-coreui-system-http-settings checkbox[name=httpEnabled]': {
          change: me.onHttpEnabledChanged
        }
      }
    });
  },

  /**
   * Enable HTTPS proxy settings only when HTTP proxy settings are enabled.
   *
   * @private
   */
  onHttpEnabledChanged: function (httpEnabled) {
    var form = httpEnabled.up('form'),
        httpsProxy = form.down('#httpsProxy'),
        nonProxyHosts = form.down('#nonProxyHosts');

    if (!httpEnabled.getValue()) {
      httpsProxy.collapse();
      httpsProxy.disable();
      nonProxyHosts.hide();
      nonProxyHosts.disable();
    }
    else if (httpsProxy.isDisabled()) {
      httpsProxy.enable();
      nonProxyHosts.enable();
      nonProxyHosts.show();
    }
  }

});
