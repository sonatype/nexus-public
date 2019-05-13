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
 * @since 3.next
 */
Ext.define('NX.onboarding.view.ChangeAdminPasswordScreen', {
  extend: 'NX.wizard.Screen',
  alias: 'widget.nx-onboarding-change-admin-password-screen',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title'),

      buttons: ['back', 'next'],

      fields: [{
        xtype: 'form',
        defaults: {
          anchor: '100%'
        },
        items: [
          {
            xtype: 'nx-password',
            name: 'password',
            itemId: 'password',
            fieldLabel: NX.I18n.get('User_UserChangePassword_Password_FieldLabel'),
            allowBlank: false,
            listeners: {
              change: function(){
                var me = this;
                me.up('form').down('#verifyPassword').validate();
              }
            }
          },
          {
            xtype: 'nx-password',
            itemId: 'verifyPassword',
            fieldLabel: NX.I18n.get('User_UserChangePassword_PasswordConfirm_FieldLabel'),
            allowBlank: false,
            submitValue: false,
            validator: function () {
              var me = this;
              return (me.up('form').down('#password').getValue() === me.getValue()) ? true : NX.I18n.get('User_UserChangePassword_NoMatch_Error');
            }
          }
        ]
      }]
    });

    me.callParent();
  }

});
