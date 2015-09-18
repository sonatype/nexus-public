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
 * LDAP Server feature panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerFeature', {
  extend: 'NX.view.drilldown.Drilldown',
  alias: 'widget.nx-coreui-ldapserver-feature',
  requires: [
    'NX.I18n'
  ],

  iconName: 'ldapserver-default',

  masters: [
    { xtype: 'nx-coreui-ldapserver-list' }
  ],

  tabs: [
    { xtype: 'nx-coreui-ldapserver-connection', title: NX.I18n.get('Ldap_LdapServerFeature_Connection_Title'), weight: 10 },
    { xtype: 'nx-coreui-ldapserver-userandgroup', title: NX.I18n.get('Ldap_LdapServerFeature_UserAndGroup_Title'), weight: 20 }
  ],

  actions: [
    { xtype: 'button', text: NX.I18n.get('Ldap_LdapServerFeature_Delete_Button'), glyph: 'xf056@FontAwesome' /* fa-minus-circle */, action: 'delete', disabled: true }
  ]
});
