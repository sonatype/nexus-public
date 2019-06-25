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
 * Blobstore "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.blobstore.BlobstoreSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-blobstore-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  initComponent: function() {
    var me = this;

    me.settingsFormSuccessMessage = function(data) {
      return NX.I18n.get('Blobstore_BlobstoreSettingsForm_Update_Success') + data['name'];
    };

    me.editableMarker = NX.I18n.get('Blobstore_BlobstoreSettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:blobstores:update'),
        NX.Conditions.formHasRecord('nx-coreui-blobstore-settings-form', function(model) {
          var blobstoreTypeModel = NX.getApplication().getStore('BlobstoreType').getById(model.data.type);
          return blobstoreTypeModel.data.isModifiable;
        })
    );

    me.items = [
      {
        xtype: 'combo',
        name: 'type',
        itemId: 'type',
        fieldLabel: NX.I18n.get('Blobstore_BlobstoreAdd_Type_FieldLabel'),
        emptyText: NX.I18n.get('Blobstore_BlobstoreAdd_Type_EmptyText'),
        editable: false,
        store: 'BlobstoreType',
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
          var blobstoreTypeModel = NX.getApplication().getStore('BlobstoreType').getById(value);
          settingsFieldSet.importProperties(null, blobstoreTypeModel.get('formFields'), me.editableCondition);

          me.remove('blobstore-custom-form');
          var customFormName = blobstoreTypeModel.get('customFormName');
          if (customFormName) {
            me.add({
              id: 'blobstore-custom-form',
              xtype: customFormName
            });
          }
        }
      },
      {
        xtype: 'textfield',
        name: 'name',
        itemId: 'name',
        fieldLabel: NX.I18n.get('Blobstore_BlobstoreSettingsForm_Name_FieldLabel'),
        readOnly: true
      },
      {
        xtype: 'checkbox',
        name: 'isQuotaEnabled',
        itemId: 'isQuotaEnabled',
        boxLabel: NX.I18n.get('Blobstore_BlobstoreSettingsForm_EnableSoftQuota_FieldLabel'),
        readOnly: true,
        listeners: {
          change: function(checkbox, newValue) {
            me.toggleQuotaParamVisibility(newValue);
          }
        }
      },
      {
        xtype: 'combo',
        name: 'quotaType',
        itemId: 'quotaType',
        fieldLabel: NX.I18n.get('Blobstore_BlobstoreSettingsForm_QuotaType_FieldLabel'),
        editable: false,
        store: 'BlobStoreQuotaType',
        queryMode: 'local',
        displayField: 'name',
        valueField: 'id',
        readOnly: true
      },
      {
        xtype: 'numberfield',
        name: 'quotaLimit',
        itemId: 'quotaLimit',
        fieldLabel: NX.I18n.get('Blobstore_BlobstoreSettingsForm_QuotaLimit_FieldLabel'),
        minValue: 1,
        allowDecimals: false,
        allowExponential: true,
        readOnly: true
      },
      {
        xtype: 'nx-coreui-formfield-settingsfieldset',
        delimiter: null,
        disableSort: true
      }
    ];

    me.callParent();

    var isQuotaEnabledField = me.down('#isQuotaEnabled');
    me.toggleQuotaParamVisibility(isQuotaEnabledField !== null && isQuotaEnabledField.value);

    //map repository attributes raw map structure to/from a flattened representation
    Ext.override(me.getForm(), {
      getValues: function() {
        var values = this.callParent(arguments);
        var type = values['type'].toLowerCase();
        values.attributes = {};
        values.attributes[type] = me.down('nx-coreui-formfield-settingsfieldset').exportProperties(values);
        var customForm = me.down('#blobstore-custom-form');
        if (customForm) {
          Ext.Object.merge(values.attributes[type], customForm.exportProperties(values));
        }
        return values;
      },
      setValues: function(values) {
        var attrs = values['attributes'];
        var type = values['type'].toLowerCase();
        for (var prop in attrs[type]) {
          if (attrs[type].hasOwnProperty(prop)) {
            values["property_" + prop] = attrs[type][prop];
          }
        }
        this.callParent(arguments);

        if (type === 'group') {
          me.filterGroupStore(values['name'], values);
        }
      }
    });
  },
  /**
   * @private
   */
  toggleQuotaParamVisibility: function(value) {
    var me = this,
        form = me.up('form');
    var quotaTypeField = me.down('#quotaType');
    quotaTypeField.setVisible(value);
    quotaTypeField.setDisabled(!value);

    var quotaLimitField = me.down('#quotaLimit');
    quotaLimitField.setVisible(value);
    quotaLimitField.setDisabled(!value);

    if (form && form.rendered) {
      form.isValid();
    }
  },
  /**
   * @private
   */
  filterGroupStore: function(selectedBlobStoreName, values) {
    var me = this,
        members = me.down('nx-coreui-formfield-settingsfieldset').down('nx-itemselector'),
        membersValue = values['property_members'];

    if (!members || !membersValue || members.name !== 'property_members') {
      return;
    }

    members.getStore().load({
      params: {
        filter: [{property: 'blobStoreName', value: selectedBlobStoreName}]
      },
      callback: function() {
        members.suspendEvents();
        members.setValue(membersValue);
        members.validateValue();
        members.resumeEvents();
      }
    });
  },

  setEditable: function(editable) {
    var me = this;

    if (editable || !NX.Permissions.check('nexus:blobstores:update')) {
      me.quotaFieldsReadOnly(false);
      NX.getApplication().getController('Blobstores').showWarning(
          NX.I18n.format('Blobstore_BlobstoreFeature_Editing_Enabled_Message'));
      me.callParent(arguments);
    }
    else {
      me.quotaFieldsReadOnly(true);

      //if the form has any fields
      if (me.items != null) {
        //then grab all of the dynamically generated fields and make them non-editable
        var itemsToDisable = me.getChildItemsToDisable().filter(function(item) {
          return item.ownerCt.xtype === 'nx-coreui-formfield-settingsfieldset' || item.ownerCt.id === 'blobstore-custom-form';
        });

        me.setItemsEditable(false, itemsToDisable);
      }
    }
  },

  quotaFieldsReadOnly: function(readOnly) {
    var me = this;
    if (me.items) {
      Ext.Array.forEach(['#isQuotaEnabled', '#quotaType', '#quotaLimit'], function(f) {
        me.down(f).setReadOnly(readOnly);
      });
    }
  }
});
