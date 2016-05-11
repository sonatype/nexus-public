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
 * License Users controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.LicenseUsers', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.util.DownloadHelper',
    'NX.util.Url',
    'NX.I18n'
  ],

  stores: [
    'LicenseUser'
  ],
  views: [
    'licensing.LicenseUserList'
  ],
  refs: [
    {
      ref: 'list',
      selector: 'nx-coreui-licensing-licenseuser-list'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'licenseuser-default': {
        file: 'ssl_certificates.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/System/Licensing/License Users',
      text: NX.I18n.get('LicenseUsers_Title'),
      description: NX.I18n.get('LicenseUsers_Description'),
      view: { xtype: 'nx-coreui-licensing-licenseuser-list' },
      iconConfig: {
        file: 'ssl_tls_manager.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:licensing:read');
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.load
        }
      },
      component: {
        'nx-coreui-licensing-licenseuser-list': {
          afterrender: me.load
        },
        'nx-coreui-licensing-licenseuser-list button[action=download]': {
          click: me.download
        }
      }
    });
  },

  /**
   * @private
   * Load active user store.
   */
  load: function () {
    var list = this.getList();

    if (list) {
      list.getStore().load();
    }
  },

  /**
   * @private
   * Download active users in CSV format.
   */
  download: function () {
    NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf('service/siesta/licensing/csv_access_data'));
  }

});
