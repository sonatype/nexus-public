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
 * LDAP Server "User & Group" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerUserAndGroupForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-ldapserver-userandgroup-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  items: { xtype: 'nx-coreui-ldapserver-userandgroup-fieldset' },

  editableMarker: NX.I18n.get('Ldap_LdapServerConnectionForm_Update_Error'),

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.isPermitted('nexus:ldap:update');

    me.callParent(arguments);

    me.getDockedItems('toolbar[dock="bottom"]')[0].add(
        { xtype: 'button', text: NX.I18n.get('Ldap_LdapServerUserAndGroupForm_VerifyGroupMapping_Button'), formBind: true, action: 'verifyusermapping' },
        { xtype: 'button', text: NX.I18n.get('Ldap_LdapServerUserAndGroupForm_VerifyLogin_Button'), formBind: true, action: 'verifylogin' }
    );
  },

  /**
   * @override
   * Additionally, marks invalid properties.
   */
  markInvalid: function(errors) {
    this.down('nx-coreui-ldapserver-userandgroup-fieldset').markInvalid(errors);
  }

});
