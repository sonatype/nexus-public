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
 * System Information controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SysInfo', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.util.Url',
    'NX.util.DownloadHelper',
    'NX.Messages',
    'NX.I18n'
  ],

  views: [
    'support.SysInfo'
  ],
  refs: [
    {
      ref: 'sysInfo',
      selector: 'nx-coreui-support-sysinfo'
    },
    {
      ref: 'content',
      selector: 'nx-feature-content'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/System Information',
      text: NX.I18n.get('SysInfo_Title'),
      description: NX.I18n.get('SysInfo_Description'),
      view: { xtype: 'nx-coreui-support-sysinfo' },
      iconConfig: {
        file: 'globe_place.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:atlas:read');
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.load
        }
      },
      component: {
        'nx-coreui-support-sysinfo': {
          afterrender: me.load
        },
        'nx-coreui-support-sysinfo button[action=download]': {
          'click': me.download
        }
      }
    });
  },

  /**
   * Load system information panel.
   *
   * @private
   */
  load: function () {
    var me = this,
        panel = me.getSysInfo();

    if (panel) {
      //<if debug>
      me.logDebug('Refreshing sysinfo');
      //</if>

      me.getContent().getEl().mask(NX.I18n.get('SysInfo_Load_Mask'));
      NX.direct.atlas_SystemInformation.read(function (response) {
        me.getContent().getEl().unmask();
        if (Ext.isObject(response) && response.success) {
          panel.setInfo(response.data);
        }
      });
    }
  },

  /**
   * Download system information report.
   *
   * @private
   */
  download: function () {
    NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf('service/siesta/atlas/system-information'));
  }
});
