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
 * LDAP Server grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-ldapserver-list',
  requires: [
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-ldapserver-list',

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      store: 'LdapServer',

      columns: [
        {
          xtype: 'nx-iconcolumn',
          width: 36,
          iconVariant: 'x16',
          iconName: function () {
            return 'ldapserver-default';
          }
        },
        { header: NX.I18n.get('Ldap_LdapServerList_Order_Header'), dataIndex: 'order', stateId: 'order', width: 80 },
        { header: NX.I18n.get('Ldap_LdapServerList_Name_Header'), dataIndex: 'name', stateId: 'name', flex: 1, renderer: Ext.htmlEncode },
        { header: NX.I18n.get('Ldap_LdapServerList_URL_Header'), dataIndex: 'url', stateId: 'url', flex: 1, renderer: Ext.htmlEncode }
      ],

      viewConfig: {
        emptyText: NX.I18n.get('Ldap_LdapServerList_EmptyText'),
        deferEmptyText: false
      },

      dockedItems: [{
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.get('Ldap_LdapServerList_New_Button'),
            iconCls: 'x-fa fa-plus-circle',
            action: 'new',
            disabled: true
          },
          {
            xtype: 'button',
            text: NX.I18n.get('Ldap_LdapServerList_ChangeOrder_Button'),
            iconCls: 'x-fa fa-sort-numeric-up',
            action: 'changeorder',
            disabled: true
          },
          {
            xtype: 'button',
            text: NX.I18n.get('Ldap_LdapServerList_ClearCache_Button'),
            iconCls: 'x-fa fa-trash',
            action: 'clearcache',
            disabled: true
          }
        ]
      }],

      plugins: [
        { ptype: 'gridfilterbox', emptyText: NX.I18n.get('Ldap_LdapServerList_Filter_EmptyText') }
      ]
    });

    this.callParent();
  }

});
