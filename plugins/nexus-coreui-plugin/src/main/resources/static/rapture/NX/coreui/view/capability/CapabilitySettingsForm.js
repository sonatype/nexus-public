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
 * Capability "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.capability.CapabilitySettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-capability-settings-form',
  requires: [
    'NX.I18n'
  ],

  items: [
    {
      xtype: 'hiddenfield',
      name: 'id'
    },
    {
      xtype: 'hiddenfield',
      name: 'typeId'
    },
    {
      xtype: 'hiddenfield',
      name: 'notes'
    },
    {
      xtype: 'checkbox',
      fieldLabel: NX.I18n.get('Capability_CapabilitySettingsForm_Enabled_FieldLabel'),
      name: 'enabled',
      allowBlank: false,
      checked: true,
      editable: true
    },
    { xtype: 'nx-coreui-formfield-settingsfieldset' }
  ],

  editableMarker: NX.I18n.get('Capability_CapabilityAdd_Create_Error'),

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.isPermitted('nexus:capabilities:update');

    me.callParent(arguments);
  },

  /**
   * @override
   * Imports capability into settings field set.
   * @param {NX.coreui.model.Capability} model capability model
   */
  loadRecord: function(model) {
    var me = this,
        capabilityTypeModel = NX.getApplication().getStore('CapabilityType').getById(model.get('typeId')),
        settingsFieldSet = me.down('nx-coreui-formfield-settingsfieldset');

    me.callParent(arguments);
    if (capabilityTypeModel) {
      settingsFieldSet.importProperties(model.get('properties'), capabilityTypeModel.get('formFields'));
    }
  },

  /**
   * @override
   * Exports capability from settings field set.
   * @returns {Object} form values
   */
  getValues: function() {
    var me = this,
        values = me.getForm().getFieldValues(),
        capability = {
          id: values.id,
          typeId: values.typeId,
          notes: values.notes,
          enabled: values.enabled,
          properties: {}
        };

    Ext.apply(capability.properties, me.down('nx-coreui-formfield-settingsfieldset').exportProperties(values));
    return capability;
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
