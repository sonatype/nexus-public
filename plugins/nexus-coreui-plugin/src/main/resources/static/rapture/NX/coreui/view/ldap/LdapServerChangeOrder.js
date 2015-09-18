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
 * Change LDAP Server ordering window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerChangeOrder', {
  extend: 'NX.view.ChangeOrderWindow',
  alias: 'widget.nx-coreui-ldapserver-changeorder',
  requires: [
    'NX.coreui.store.LdapServer',
    'NX.I18n'
  ],

  title: NX.I18n.get('Ldap_LdapServerChangeOrder_Title'),

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.store = Ext.create('NX.coreui.store.LdapServer', {
      sortOnLoad: true,
      sorters: { property: 'order', direction: 'ASC' }
    });
    me.store.load();

    me.callParent(arguments);
  }

});
