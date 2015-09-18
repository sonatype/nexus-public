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

  config: {
    stateful: true,
    stateId: 'nx-coreui-privilege-list'
  },

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
    deferEmptyText: false
  },

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions nx-borderless'
  }],

  plugins: [
    { ptype: 'gridfilterbox', emptyText: NX.I18n.get('Privilege_PrivilegeList_Filter_EmptyText') }
  ]
});
