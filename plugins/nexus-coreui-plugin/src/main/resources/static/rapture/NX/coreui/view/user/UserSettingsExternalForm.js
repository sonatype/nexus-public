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
 * External user "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.user.UserSettingsExternalForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-user-settings-external-form',
  requires: [
    'NX.Conditions',
    'NX.Dialogs',
    'NX.I18n'
  ],

  api: {
    submit: 'NX.direct.coreui_User.updateRoleMappings'
  },
  settingsFormSuccessMessage: function(data) {
    return 'User role mappings updated: ' + data['userId'];
  },

  editableMarker: NX.I18n.get('User_UserSettingsForm_Update_Error'),

  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:users:update'),
        NX.Conditions.formHasRecord('nx-coreui-user-settings-external-form', function(model) {
          return model.get('external');
        })
    );

    me.items = [
      {
        name: 'userId',
        itemId: 'userId',
        readOnly: true,
        fieldLabel: NX.I18n.get('User_UserSettingsForm_ID_FieldLabel'),
        helpText: NX.I18n.get('User_UserSettingsForm_ID_HelpText'),
        allowBlank: true
      },
      { name: 'realm', xtype: 'hiddenfield' },
      {
        name: 'firstName',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_FirstName_FieldLabel'),
        allowBlank: true,
        readOnly: true,
        submitValue: false
      },
      {
        name: 'lastName',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_LastName_FieldLabel'),
        allowBlank: true,
        readOnly: true,
        submitValue: false
      },
      {
        name: 'email',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_Email_FieldLabel'),
        helpText: NX.I18n.get('User_UserSettingsForm_Email_HelpText'),
        allowBlank: true,
        readOnly: true,
        submitValue: false
      },
      {
        xtype: 'combo',
        name: 'status',
        fieldLabel: NX.I18n.get('User_UserSettingsForm_Status_FieldLabel'),
        editable: false,
        store: [
          ['active', NX.I18n.get('User_UserSettingsForm_Status_ActiveItem')],
          ['disabled', NX.I18n.get('User_UserSettingsForm_Status_DisabledItem')]
        ],
        queryMode: 'local',
        allowBlank: true,
        readOnly: true,
        cls: 'nx-combo-disabled',
        submitValue: false
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
        delimiter: null,
        allowBlank: true
      },
      {
        xtype: 'textarea',
        name: 'externalRoles',
        itemId: 'externalRoles',
        fieldLabel: NX.I18n.get('User_UserSettingsExternalForm_ExternalRoles_FieldLabel'),
        helpText: NX.I18n.get('User_UserSettingsExternalForm_ExternalRoles_HelpText'),
        allowBlank: true,
        readOnly: true,
        submitValue: false
      }
    ];

    me.callParent(arguments);

    Ext.override(me.down('#externalRoles'), {
      /**
       * @override
       * Join external roles using '\n' so they are shown as a role / line.
       */
      setValue: function(value) {
        var formattedValue = value;
        if (Ext.isArray(formattedValue)) {
          formattedValue = [formattedValue.join('\n')];
        }
        this.callParent(formattedValue);
      }
    });

    Ext.override(me.down('#roles'), {
      /**
       * @override
       * Block removal of external roles.
       */
      moveRec: function(add, recs) {
        var externalRoles = me.getRecord().get('externalRoles'),
            canRemove = true;

        if (!add && externalRoles) {
          Ext.Array.each(Ext.Array.from(recs), function(roleModel) {
            if (Ext.Array.contains(externalRoles, roleModel.get('id'))) {
              canRemove = false;
              NX.Dialogs.showInfo(
                  NX.I18n.get('User_UserSettingsExternalForm_Remove_Error'),
                  'External mapped role "' + roleModel.get('name')
                      + '" cannot be removed because is assigned to user by external source'
              );
            }
            return canRemove;
          });
        }

        if (canRemove) {
          this.callParent(arguments);
        }
      }
    });
  }

});
