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
 * LDAP Server "Connection" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerConnectionForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-ldapserver-connection-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],
  // isAddPage variable uses for define, is user add new LDAP configuration or edit an old one.
  isAddPage: false,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = { xtype: 'nx-coreui-ldapserver-connection-fieldset' };

    me.editableMarker = NX.I18n.get('Ldap_LdapServerConnectionForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.isPermitted('nexus:ldap:update');

    me.callParent();

    me.getDockedItems('toolbar[dock="bottom"]')[0].add(
        { xtype: 'button', text: NX.I18n.get('Ldap_LdapServerConnectionForm_VerifyConnection_Button'), action: 'verifyconnection' }
    );

    NX.Conditions.formIs(me, function(form) {
      return !form.isDisabled() && form.isValid();
    }).on({
      satisfied: function() {
        this.enable();
      },
      unsatisfied: function() {
        this.disable();
      },
      scope: me.down('button[action=verifyconnection]')
    });
  }

});
