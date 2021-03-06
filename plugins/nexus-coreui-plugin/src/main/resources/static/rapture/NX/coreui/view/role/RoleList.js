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
 * Role grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.role.RoleList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-role-list',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-role-list',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.store = 'Role';

    me.columns = [
      {
        xtype: 'nx-iconcolumn',
        width: 36,
        iconVariant: 'x16',
        iconName: function () {
          return 'role-default';
        }
      },
      {header: NX.I18n.get('Role_RoleList_Name_Header'), dataIndex: 'name', stateId: 'name', flex: 1},
      {header: NX.I18n.get('Role_RoleList_Source_Header'), dataIndex: 'source', stateId: 'source'},
      {header: NX.I18n.get('Role_RoleList_Description_Header'), dataIndex: 'description', stateId: 'description', flex: 1}
    ];

    me.viewConfig = {
      emptyText: NX.I18n.get('Role_RoleList_EmptyText'),
      deferEmptyText: false
    };

    me.plugins = [
      { ptype: 'gridfilterbox', emptyText: NX.I18n.get('Role_RoleList_Filter_EmptyText') }
    ];

    me.dockedItems = [{
      xtype: 'nx-actions',
      items: [
        { xtype: 'button', text: NX.I18n.get('Role_RoleList_New_Button'), iconCls: 'x-fa fa-plus-circle', action: 'new', disabled: true,
          menu: [
            { text: NX.I18n.get('Role_RoleList_New_NexusRoleItem'), action: 'newrole', iconCls: NX.Icons.cls('role-default', 'x16') }
          ]
        }
      ]
    }];

    me.callParent();
  }
});
