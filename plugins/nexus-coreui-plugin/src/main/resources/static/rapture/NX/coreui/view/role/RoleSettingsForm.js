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
 * Role "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.role.RoleSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-role-settings-form',
  requires: [
    'NX.Conditions',
    'NX.coreui.store.Role',
    'NX.I18n',
    'NX.util.Validator'
  ],

  api: {
    submit: 'NX.direct.coreui_Role.update'
  },

  initComponent: function() {
    var me = this,
        idField,
        roleStore = Ext.create('NX.coreui.store.Role');

    roleStore.load();

    me.settingsFormSuccessMessage = me.settingsFormSuccessMessage || function(data) {
      return NX.I18n.get('Role_RoleSettingsForm_Update_Success') + Ext.String.htmlEncode(data['name']);
    };

    me.editableMarker = NX.I18n.get('Role_RoleSettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:roles:update'),
        NX.Conditions.formHasRecord('nx-coreui-role-settings-form', function(model) {
          return !model.get('readOnly');
        })
    );

    if (me.source) {
      idField = {
        xtype: 'combo',
        name: 'id',
        itemId: 'id',
        fieldLabel: NX.I18n.get('Role_RoleSettingsForm_MappedRole_FieldLabel'),
        emptyText: NX.I18n.get('Role_RoleSettingsForm_MappedRole_EmptyText'),
        editable: true,
        store: 'RoleBySource',
        queryMode: 'local',
        displayField: 'name',
        valueField: 'id',
        vtype: 'nx-trim'
      };
    }
    else {
      idField = {
        name: 'id',
        itemId: 'id',
        readOnly: true,
        fieldLabel: NX.I18n.get('Role_RoleSettingsForm_RoleID_FieldLabel'),
        vtype: 'nx-trim'
      };
    }

    me.items = [
      {
        xtype: 'hiddenfield',
        name: 'version'
      },
      {
        xtype: 'hiddenfield',
        name: 'source',
        value: me.source || 'default'
      },
      idField,
      {
        name: 'name',
        fieldLabel: NX.I18n.get('Role_RoleSettingsForm_Name_FieldLabel'),
        transformRawValue: Ext.htmlDecode
      },
      {
        name: 'description',
        allowBlank: true,
        fieldLabel: NX.I18n.get('Role_RoleSettingsForm_Description_FieldLabel'),
        transformRawValue: Ext.htmlDecode
      },
      {
        xtype: 'nx-itemselector',
        name: 'privileges',
        itemId: 'privileges',
        fieldLabel: NX.I18n.get('Role_RoleSettingsForm_Privileges_FieldLabel'),
        allowBlank: true,
        buttons: ['add', 'remove'],
        fromTitle: NX.I18n.get('Role_RoleSettingsForm_Privileges_FromTitle'),
        toTitle: NX.I18n.get('Role_RoleSettingsForm_Privileges_ToTitle'),
        store: 'PrivilegeReference',
        valueField: 'id',
        displayField: 'name',
        delimiter: null
      },
      {
        xtype: 'nx-itemselector',
        name: 'roles',
        itemId: 'roles',
        fieldLabel: NX.I18n.get('Role_RoleSettingsForm_Roles_FieldLabel'),
        allowBlank: true,
        buttons: ['add', 'remove'],
        fromTitle: NX.I18n.get('Role_RoleSettingsForm_Roles_FromTitle'),
        toTitle: NX.I18n.get('Role_RoleSettingsForm_Roles_ToTitle'),
        store: roleStore,
        valueField: 'id',
        displayField: 'name',
        delimiter: null
      }
    ];

    me.callParent();

    NX.Conditions.formIs(me, function(form) {
      return !form.isDisabled();
    }).on({
      satisfied: function() {
        Ext.each(me.query('nx-itemselector'), function(it) {
          it.show();
        });
      },
      unsatisfied: function() {
        Ext.each(me.query('nx-itemselector'), function(it) {
          it.hide();
        });
      },
      scope: me
    });
  }
});
