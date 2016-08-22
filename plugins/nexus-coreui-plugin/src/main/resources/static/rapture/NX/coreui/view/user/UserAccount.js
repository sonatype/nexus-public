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
 * User account settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.user.UserAccount', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-user-account',
  requires: [
    'NX.I18n'
  ],

  initComponent: function () {
    var me = this;

    me.settingsForm = [
      {
        xtype: 'nx-settingsform',
        settingsFormSuccessMessage: NX.I18n.get('User_UserAccount_Update_Success'),
        api: {
          load: 'NX.direct.coreui_User.readAccount',
          submit: 'NX.direct.coreui_User.updateAccount'
        },
        editableMarker: NX.I18n.get('User_UserAccount_Update_Error'),
        items: [
          {
            name: 'userId',
            itemId: 'userId',
            readOnly: true,
            fieldLabel: NX.I18n.get('User_UserAccount_ID_FieldLabel'),
            helpText: NX.I18n.get('User_UserAccount_ID_HelpText')
          },
          {
            name: 'firstName',
            fieldLabel: NX.I18n.get('User_UserAccount_First_FieldLabel')
          },
          {
            name: 'lastName',
            fieldLabel: NX.I18n.get('User_UserAccount_Last_FieldLabel')
          },
          {
            xtype: 'nx-email',
            name: 'email',
            fieldLabel: NX.I18n.get('User_UserAccount_Email_FieldLabel')
          },
          {
            xtype: 'hidden',
            name: 'external',
            listeners: {
              change: function(field, value) {
                var form = field.up('nx-settingsform'),
                    external = value && value.trim().toLowerCase() === 'true'; // hidden fields values are always strings

                form.getForm().external = external;
                form.setEditable(!external);
              }
            }
          }
        ]
      }
    ];

    me.callParent();

    me.down('nx-settingsform').getDockedItems('toolbar[dock="bottom"]')[0].add({
      xtype: 'button', text: NX.I18n.get('User_UserAccount_Password_Button'), action: 'changepassword', glyph: 'xf023@FontAwesome' /* fa-lock */, disabled: true
    });

    // do not perform any validity check if we have an external user
    Ext.override(me.down('nx-settingsform').getForm(), {
      isValid: function() {
        if (this.external) {
          this.clearInvalid();
          return true;
        }
        else {
          return this.callParent();
        }
      }
    });
  }

});
