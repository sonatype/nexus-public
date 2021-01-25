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
 * Privilege grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.privilege.PrivilegeList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-privilege-list',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-privilege-list',

  // filter will install into toolbar, ensure its properly styled for drilldown
  tbar: {
    xtype: 'nx-actions'
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    Ext.apply(me, {
      store: 'Privilege',

      columns: [
        {
          xtype: 'nx-iconcolumn',
          dataIndex: 'type',
          width: 36,
          iconVariant: 'x16',
          iconNamePrefix: 'privilege-'
        },

        // NOTE: Not including ID here as for user-created privileges these are random strings

        { header: NX.I18n.get('Privilege_PrivilegeList_Name_Header'), dataIndex: 'name', stateId: 'name', flex: 2 },

        {
          header: NX.I18n.get('Privilege_PrivilegeList_Description_Header'),
          dataIndex: 'description',
          stateId: 'description',
          flex: 4
        },

        { header: NX.I18n.get('Privilege_PrivilegeList_Type_Header'), dataIndex: 'type', stateId: 'type', flex: 1 },

        {
          header: NX.I18n.get('Privilege_PrivilegeList_Permission_Header'),
          dataIndex: 'permission',
          stateId: 'permission',
          flex: 2
        }
      ],

      viewConfig: {
        emptyText: NX.I18n.get('Privilege_PrivilegeList_EmptyText'),
        emptyTextFilter: NX.I18n.get('Privilege_PrivilegeList_Filter_EmptyText'),
        deferEmptyText: false
      },

      selModel: {
        pruneRemoved: false
      },

      dockedItems: [{
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.get('Privilege_PrivilegeList_New_Button'),
            action: 'new',
            disabled: true,
            iconCls: 'x-fa fa-plus-circle'
          }
        ]
      }],

      plugins: [
        {ptype: 'remotegridfilterbox'}
      ]
    });

    me.callParent();
  }
});
