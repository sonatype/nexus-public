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
 * License User grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.licensing.LicenseUserList', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-licensing-licenseuser-list',
  requires: [
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-licensing-licenseuser-list',

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      store: 'LicenseUser',

      columns: [
        {
          xtype: 'nx-iconcolumn',
          width: 36,
          iconVariant: 'x16',
          iconName: function () {
            return 'licenseuser-default';
          }
        },
        { header: NX.I18n.get('Licensing_LicenseUserList_IP_Header'), dataIndex: 'ip', stateId: 'ip', flex: 1 },
        { header: NX.I18n.get('Licensing_LicenseUserList_Date_Header'), dataIndex: 'timestamp', stateId: 'timestamp', flex: 1 },
        { header: NX.I18n.get('Licensing_LicenseUserList_User_Header'), dataIndex: 'userId', stateId: 'userId', flex: 1 },
        { header: NX.I18n.get('Licensing_LicenseUserList_Agent_Header'), dataIndex: 'userAgent', stateId: 'userAgent', flex: 2 }
      ],

      viewConfig: {
        emptyText: NX.I18n.get('Licensing_LicenseUserList_EmptyText'),
        deferEmptyText: false
      },

      dockedItems: [{
        xtype: 'nx-actions',
        items: {
          xtype: 'button',
          text: NX.I18n.get('Licensing_LicenseUserList_Download_Button'),
          glyph: 'xf019@FontAwesome' /* fa-download */,
          action: 'download'
        }
      }],

      plugins: [{ ptype: 'gridfilterbox', emptyText: 'No license user matched criteria "$filter"' }]
    });

    this.callParent();
  }

});
