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
 * S3 Blobstore custom "Settings" panel.
 *
 * @since 3.17
 */
Ext.define('NX.s3blobstore.view.BlobstoreSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-blobstore-settings-s3',
  requires: [
    'Ext.data.Store',
    'NX.I18n'
  ],

  settingsForm: [
    {
      xtype: 'combo',
      name: 'property_region',
      fieldLabel: NX.I18n.get('S3Blobstore_Region_FieldLabel'),
      helpText: NX.I18n.get('S3Blobstore_Region_HelpText'),
      itemCls: 'required-field',
      displayField: 'name',
      valueField: 'id',
      editable: false,
      forceSelection: true,
      queryMode: 'local',
      triggerAction: 'all',
      emptyText: 'Select...',
      selectOnFocus: false,
      allowBlank: false,
      listeners: {
        added: function() {
          var me = this;
          me.getStore().load();
        },
        afterrender: function() {
          var me = this;
          if (!me.getValue()) {
            me.setValue('DEFAULT');
          }
        }
      },
      store: 'NX.s3blobstore.store.S3Region'
    },
    {
      xtype:'textfield',
      name: 'property_bucket',
      fieldLabel: NX.I18n.get('S3Blobstore_Bucket_FieldLabel'),
      helpText: NX.I18n.get('S3Blobstore_Bucket_HelpText'),
      allowBlank: false
    },
    {
      xtype:'textfield',
      name: 'property_prefix',
      fieldLabel: NX.I18n.get('S3Blobstore_Prefix_FieldLabel'),
      helpText: NX.I18n.get('S3Blobstore_Prefix_HelpText'),
      allowBlank: true
    },
    {
      xtype:'numberfield',
      name: 'property_expiration',
      fieldLabel: NX.I18n.get('S3Blobstore_Expiration_FieldLabel'),
      helpText: NX.I18n.get('S3Blobstore_Expiration_HelpText'),
      value: Number('3'),
      minValue: Number('-1'),
      allowBlank: false
    },
    {
      xtype: 'nx-optionalfieldset',
      title: NX.I18n.get('S3Blobstore_Authentication_Title'),
      checkboxToggle: true,
      checkboxName: 'authEnabled',
      collapsed: true,
      items: [
        {
          xtype:'textfield',
          name: 'property_accessKeyId',
          fieldLabel: NX.I18n.get('S3Blobstore_Authentication_AccessKeyId'),
          allowBlank: false
        },
        {
          xtype: 'textfield',
          inputType: 'password',
          name: 'property_secretAccessKey',
          fieldLabel: NX.I18n.get('S3Blobstore_Authentication_SecretAccessKey'),
          allowBlank: false
        },
        {
          xtype: 'textfield',
          name: 'property_assumeRole',
          fieldLabel: NX.I18n.get('S3Blobstore_Authentication_AssumeRoleArn'),
          allowBlank: true
        },
        {
          xtype: 'textfield',
          name: 'property_sessionToken',
          fieldLabel: NX.I18n.get('S3Blobstore_Authentication_SessionToken'),
          allowBlank: true
        }
      ]
    },
    {
      xtype: 'nx-optionalfieldset',
      title: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_Title'),
      checkboxToggle: true,
      checkboxName: 'advancedConnectionSettingsEnabled',
      collapsed: true,
      listeners: {
        expand: function() {
          var me = this;
          var signerType = me.down('[name=property_signertype]');
          if (!signerType.getValue()) {
            signerType.setValue('DEFAULT');
          }
        }
      },
      items: [
        {
          xtype:'textfield',
          name: 'property_endpoint',
          fieldLabel: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_EndPointUrl_FieldLabel'),
          helpText: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_EndPointUrl_HelpText'),
          allowBlank: true
        },
        {
          xtype: 'combo',
          name: 'property_signertype',
          fieldLabel: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_SignatureVersion_FieldLabel'),
          helpText: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_SignatureVersion_HelpText'),
          itemCls: 'required-field',
          displayField: 'name',
          valueField: 'id',
          editable: false,
          forceSelection: true,
          queryMode: 'local',
          triggerAction: 'all',
          emptyText: 'Select...',
          selectOnFocus: false,
          allowBlank: true,
          listeners: {
            added: function() {
              var me = this;
              me.getStore().load();
            }
          },
          store: 'NX.s3blobstore.store.S3SignerType'
        },
        {
          xtype: 'checkbox',
          name: 'property_forcepathstyle',
          fieldLabel: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_PathStyleAccess_FieldLabel'),
          helpText: NX.I18n.get('S3Blobstore_AdvancedConnectionSettings_PathStyleAccess_HelpText')
        }
      ]
    }
  ],

  exportProperties: function(values) {
    var properties = {};
    Ext.Object.each(values, function(key, value) {
      if (key.startsWith('property_')) {
        properties[key.replace('property_', '')] = String(value);
      }
    });
    return properties;
  }
  
});
