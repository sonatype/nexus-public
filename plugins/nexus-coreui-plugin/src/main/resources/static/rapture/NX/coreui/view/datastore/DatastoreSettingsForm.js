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
 * Datastore "Settings" form.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.datastore.DatastoreSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-datastore-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  initComponent: function() {
    var me = this;

    me.settingsFormSuccessMessage = function(data) {
      return NX.I18n.get('Datastore_DatastoreSettingsForm_Update_Success') + data['name'];
    };

    me.editableMarker = NX.I18n.get('Datastore_DatastoreSettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:datastores:update'),
        NX.Conditions.formHasRecord('nx-coreui-datastore-settings-form', function(model) {
          var datastoreSourceModel = NX.getApplication().getStore('DatastoreSource').getById(model.data.source);
          return datastoreSourceModel.data.isModifiable;
        })
    );

    me.items = [
      {
        xtype: 'combo',
        name: 'source',
        itemId: 'source',
        fieldLabel: NX.I18n.get('Datastore_DatastoreAdd_Source_FieldLabel'),
        emptyText: NX.I18n.get('Datastore_DatastoreAdd_Source_EmptyText'),
        editable: false,
        store: 'DatastoreSource',
        queryMode: 'local',
        displayField: 'name',
        valueField: 'id',
        readOnly: true,
        onChange: function(newValue, oldValue) {
          var comboxBox = this;
          var value = newValue || oldValue;
          if (!newValue) {
            comboxBox.setValue(value);
            comboxBox.originalValue = value;
            comboxBox.validate();
          }
        }
      },
      {
        xtype: 'combo',
        name: 'type',
        itemId: 'type',
        fieldLabel: NX.I18n.get('Datastore_DatastoreAdd_Type_FieldLabel'),
        emptyText: NX.I18n.get('Datastore_DatastoreAdd_Type_EmptyText'),
        editable: false,
        store: 'DatastoreType',
        queryMode: 'local',
        displayField: 'name',
        valueField: 'id',
        readOnly: true,
        onChange: function(newValue, oldValue) {
          var comboxBox = this;
          var settingsFieldSet = me.down('nx-coreui-formfield-settingsfieldset');
          var value = newValue || oldValue;
          if (!newValue) {
            comboxBox.setValue(value);
            comboxBox.originalValue = value;
            comboxBox.validate();
          }
          var datastoreTypeModel = NX.getApplication().getStore('DatastoreType').getById(value);
          settingsFieldSet.importProperties(null, datastoreTypeModel.get('formFields'), me.editableCondition);
        }
      },
      {
        xtype: 'textfield',
        name: 'name',
        itemId: 'name',
        fieldLabel: NX.I18n.get('Datastore_DatastoreSettingsForm_Name_FieldLabel'),
        readOnly: true
      },
      {
        xtype: 'nx-coreui-formfield-settingsfieldset',
        delimiter: null,
        disableSort: true
      }
    ];

    me.callParent();

    //map attributes raw map structure to/from a flattened representation
    Ext.override(me.getForm(), {
      getValues: function() {
        var values = this.callParent(arguments);
        values.attributes = me.down('nx-coreui-formfield-settingsfieldset').exportProperties(values);
        return values;
      },
      setValues: function(values) {
        var attrs = values['attributes'];
        for (var prop in attrs) {
          if (attrs.hasOwnProperty(prop)) {
            values["property_" + prop] = attrs[prop];
          }
        }
        this.callParent(arguments);
      }
    });
  },
  setEditable: function(editable) {
    var me = this;

    if (editable || !NX.Permissions.check('nexus:datastores:update')) {
      NX.getApplication().getController('Datastores').showWarning(
          NX.I18n.format('Datastore_DatastoreFeature_Editing_Enabled_Message'));
      me.callParent(arguments);
    }
    else {
      //if the form has any fields
      if (me.items != null) {
        //then grab all of the dynamically generated fields and make them non-editable
        var itemsToDisable = me.getChildItemsToDisable().filter(function(item) {
          return item.ownerCt.xtype === 'nx-coreui-formfield-settingsfieldset';
        });

        me.setItemsEditable(false, itemsToDisable);
      }
    }
  }
});
