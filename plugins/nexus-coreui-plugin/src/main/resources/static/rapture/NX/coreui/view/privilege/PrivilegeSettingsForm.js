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
 * Privilege "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.privilege.PrivilegeSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-privilege-settings-form',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    Ext.apply(me, {
          items: [
            {
              xtype: 'hiddenfield',
              name: 'id'
            },
            {
              xtype: 'hiddenfield',
              name: 'version'
            },
            {
              xtype: 'hiddenfield',
              name: 'type'
            },
            {
              xtype: 'textfield',
              fieldLabel: NX.I18n.get('Privilege_PrivilegeSettingsForm_Name_FieldLabel'),
              name: 'name',
              itemId: 'name',
              allowBlank: false,
              readOnly: true
            },
            {
              xtype: 'textfield',
              fieldLabel: NX.I18n.get('Privilege_PrivilegeSettingsForm_Description_FieldLabel'),
              name: 'description',
              allowBlank: true,
              transformRawValue: Ext.htmlDecode
            },
            {
              xtype: 'nx-coreui-formfield-settingsfieldset'
            }
          ],

          settingsFormSuccessMessage: me.settingsFormSuccessMessage || function(data) {
            return NX.I18n.format('Privilege_PrivilegeSettingsForm_Update_Success', data['name']);
          },

          editableMarker: me.editableMarker || NX.I18n.get('Privilege_PrivilegeSettingsForm_Update_Error'),

          editableCondition: me.editableCondition || NX.Conditions.and(
              NX.Conditions.isPermitted('nexus:privileges:update'),
              NX.Conditions.formHasRecord('nx-coreui-privilege-settings-form', function(model) {
                return !model.get('readOnly');
              })
          )
        }
    );

    me.callParent();
  },

  /**
   * @override
   * Imports privilege into settings field set.
   * @param {NX.coreui.model.Capability} model capability model
   */
  loadRecord: function(model) {
    var me = this,
        store = NX.getApplication().getStore('PrivilegeType'),
        privilegeTypeModel = store.getById(model.get('type')),
        settingsFieldSet = me.down('nx-coreui-formfield-settingsfieldset');

    me.model = model;

    if (privilegeTypeModel) {
      settingsFieldSet.importProperties(model.get('properties'), privilegeTypeModel.get('formFields'), me.editableCondition);
    }
    else {
      store.on({
        load: function() {
          privilegeTypeModel = store.getById(model.get('type'));
          if (me.model === model && privilegeTypeModel) {
            settingsFieldSet.importProperties(model.get('properties'), privilegeTypeModel.get('formFields'),
                me.editableCondition);
          }
        },
        single: true
      });
    }
    me.callParent(arguments);
  },

  /**
   * @override
   * Exports privilege from settings field set.
   * @returns {Object} form values
   */
  getValues: function() {
    var me = this,
        values = me.getForm().getFieldValues(),
        privilege = {
          id: values.id,
          name: values.name,
          description: values.description,
          version: values.version,
          type: values.type,
          readOnly: values.readOnly,
          properties: {}
        };

    Ext.apply(privilege.properties, me.down('nx-coreui-formfield-settingsfieldset').exportProperties(values));
    return privilege;
  },

  /**
   * Mark fields in this form invalid in bulk.
   * @param {Object/Object[]/Ext.data.Errors} errors
   * Either an array in the form `[{id:'fieldId', msg:'The message'}, ...]`,
   * an object hash of `{id: msg, id2: msg2}`, or a {@link Ext.data.Errors} object.
   */
  markInvalid: function(errors) {
    this.down('nx-coreui-formfield-settingsfieldset').markInvalid(errors);
  }

});
