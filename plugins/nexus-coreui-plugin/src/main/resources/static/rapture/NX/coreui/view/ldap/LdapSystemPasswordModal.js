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
/*global Ext*/

/**
 * Modal to retrieve the 'System Password' of the LDAP server
 *
 * @since 3.24
 */
Ext.define('NX.coreui.view.ldap.LdapSystemPasswordModal', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-ldapserver-systempassword-modal',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      ui: 'nx-inset',
      closable: true,
      width: NX.view.ModalDialog.SMALL_MODAL,

      title: NX.I18n.render(me, 'Title'),

      items: {
        xtype: 'form',
        defaultType: 'textfield',
        defaults: {
          anchor: '100%'
        },
        items: [
          {
            xtype: 'nx-password',
            name: 'authPassword',
            itemId: 'authPassword',
            fieldLabel: NX.I18n.render(me, 'Password_FieldLabel'),
            inputType: 'password',
            helpText: NX.I18n.render(me, 'Password_HelpText'),
            allowBlank: false
          }
        ],

        buttonAlign: 'left',
        buttons: [
          { text: NX.I18n.render(me, 'Button_OK'), action: 'ok', scope: me, formBind: true, bindToEnter: true, ui: 'nx-primary' },
          { text: NX.I18n.render(me, 'Button_Cancel'), handler: me.close, scope: me }
        ]
      }
    });

    me.callParent();
  },
});
