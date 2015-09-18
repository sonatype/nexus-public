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
 * User "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.user.UserSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-user-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  api: {
    submit: 'NX.direct.coreui_User.update'
  },
  settingsFormSuccessMessage: function(data) {
    return NX.I18n.get('User_UserSettingsForm_Update_Success') + data['userId'];
  },

  editableMarker: NX.I18n.get('User_UserSettingsForm_Update_Error'),

  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:users:update'),
        NX.Conditions.formHasRecord('nx-coreui-user-settings-form', function(model) {
          return !model.get('external');
        })
    );

    me.items = [
      {
        name: 'userId',
        itemId: 'userId',
        readOnly: true,
        fieldLabel: NX.I18n.get('User_UserSettingsForm_ID_FieldLabel'),
        helpText: NX.I18n.get('User_UserSettingsForm_ID_HelpText')
      },
      { name: 'version', xtype: 'hiddenfield' },
      {
        name: 'firstName',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_FirstName_FieldLabel')
      },
      {
        name: 'lastName',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_LastName_FieldLabel')
      },
      {
        xtype: 'nx-email',
        name: 'email',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_Email_FieldLabel'),
        helpText: NX.I18n.get('User_UserSettingsForm_Email_HelpText')
      },
      {
        xtype: 'combo',
        name: 'status',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_Status_FieldLabel'),
        emptyText: NX.I18n.get('User_UserSettingsForm_Status_EmptyText'),
        allowBlank: false,
        editable: false,
        store: [
          ['active', NX.I18n.get('User_UserSettingsForm_Status_ActiveItem')],
          ['disabled', NX.I18n.get('User_UserSettingsForm_Status_DisabledItem')]
        ],
        queryMode: 'local'
      },
      {
        xtype: 'nx-itemselector',
        name: 'roles',
        itemId: 'roles',
        fieldLabel: NX.I18n.get('User_UserSettingsExternalForm_Roles_FieldLabel'),
        buttons: ['add', 'remove'],
        fromTitle: NX.I18n.get('User_UserSettingsExternalForm_Roles_FromTitle'),
        toTitle: NX.I18n.get('User_UserSettingsExternalForm_Roles_ToTitle'),
        store: 'Role',
        valueField: 'id',
        displayField: 'name',
        delimiter: null
      }
    ];

    me.callParent(arguments);
  }

});
