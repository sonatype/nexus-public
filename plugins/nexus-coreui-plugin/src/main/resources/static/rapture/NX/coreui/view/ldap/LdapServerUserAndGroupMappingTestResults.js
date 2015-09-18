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
 * LDAP Server User & Group test results window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerUserAndGroupMappingTestResults', {
  extend: 'Ext.window.Window',
  alias: 'widget.nx-coreui-ldapserver-userandgroup-testresults',
  requires: [
    'Ext.data.JsonStore',
    'NX.I18n'
  ],

  title: NX.I18n.get('Ldap_LdapServerUserAndGroupMappingTestResults_Title'),

  layout: 'fit',
  autoShow: true,
  modal: true,
  constrain: true,
  width: 630,

  buttonAlign: 'left',
  buttons: [
    { text: NX.I18n.get('Ldap_LdapServerUserAndGroupMappingTestResults_Close_Button'), handler: function () {
      this.up('window').close();
    }}
  ],

  /**
   * @cfg json array of users (as returned by checking the user mapping)
   */
  mappedUsers: undefined,

  initComponent: function () {
    var me = this;

    me.items = {
      xtype: 'grid',
      columns: [
        { header: NX.I18n.get('Ldap_LdapServerUserAndGroupMappingTestResults_ID_Header'), dataIndex: 'username', flex: 1 },
        { header: NX.I18n.get('Ldap_LdapServerUserAndGroupMappingTestResults_Name_Header'), dataIndex: 'realName', flex: 1 },
        { header: NX.I18n.get('Ldap_LdapServerUserAndGroupMappingTestResults_Email_Header'), dataIndex: 'email', width: 250 },
        { header: NX.I18n.get('Ldap_LdapServerUserAndGroupMappingTestResults_Roles_Header'), dataIndex: 'membership', flex: 3 }
      ],
      store: Ext.create('Ext.data.JsonStore', {
        fields: ['username', 'realName', 'email', 'membership'],
        data: me.mappedUsers
      })
    };

    me.maxHeight = Ext.getBody().getViewSize().height - 100;

    me.callParent(arguments);
  }

});
