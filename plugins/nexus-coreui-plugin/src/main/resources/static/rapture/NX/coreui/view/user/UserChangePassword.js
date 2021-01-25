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
 * Chnage password window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.user.UserChangePassword', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-user-changepassword',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  /**
   * @cfg userId to change password for
   */
  userId: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.ui = 'nx-inset';
    me.title = NX.I18n.get('User_UserChangePassword_Title');

    me.setWidth(NX.view.ModalDialog.SMALL_MODAL);

    me.items = {
      xtype: 'form',
      editableCondition: NX.Conditions.isPermitted('nexus:userschangepw:create'),
      editableMarker: NX.I18n.get('User_UserChangePassword_NoPermission_Error'),
      defaults: {
        anchor: '100%',
        // allow cancel to be clicked w/o validating field to be non-blank
        validateOnBlur: false
      },
      items: [
        {
          xtype: 'nx-password',
          name: 'password',
          itemId: 'password',
          fieldLabel: NX.I18n.get('User_UserChangePassword_Password_FieldLabel'),
          allowBlank: false
        },
        {
          xtype: 'nx-password',
          fieldLabel: NX.I18n.get('User_UserChangePassword_PasswordConfirm_FieldLabel'),
          allowBlank: false,
          submitValue: false,
          validator: function () {
            var me = this;
            return (me.up('form').down('#password').getValue() === me.getValue()) ? true : NX.I18n.get('User_UserChangePassword_NoMatch_Error');
          }
        }
      ],

      buttonAlign: 'left',
      buttons: [
        { text: NX.I18n.get('User_UserChangePassword_Submit_Button'), action: 'changepassword', formBind: true, bindToEnter: true, ui: 'nx-primary' },
        { text: NX.I18n.get('User_UserChangePassword_Cancel_Button'), handler: function () {
          this.up('window').close();
        }}
      ]
    };

    me.maxHeight = Ext.getBody().getViewSize().height - 100;

    me.on({
      resize: function() {
        me.down('#password').focus();
      },
      single: true
    });

    me.callParent();
  }
});
