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
 * Upload Component View
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.upload.UploadComponent', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nx-coreui-upload-component',
    requires: [
      'NX.I18n'
    ],

    /**
     * @override
     */
    initComponent: function() {
      var me = this;
      me.store = 'UploadDefinition';

      me.callParent();
    },

    ui: 'nx-inset',

    loadRecord: function(uploadDefinition, repository) {
      var me = this;

      me.uploadDefinition = uploadDefinition;
      me.repository = repository;

      me.removeAll(true);

      me.add({
        xtype: 'form',
        ui: 'nx-subsection',
        frame: true,

        api: {
          submit: 'NX.direct.coreui_Upload.doUpload'
        },

        items: [{
            xtype: 'panel',
            itemId: 'nx-coreui-upload-component-assets',
            title: NX.I18n.get('FeatureGroups_Upload_Asset_Form_Title'),
            ui: 'nx-subsection',
            frame: true,
            items: [me.createRow(true)],
            cls: 'nx-coreui-upload-component-assets'
        },{
            xtype: 'button',
            itemId: 'nx-coreui-upload-component-add-asset',
            text: NX.I18n.get('FeatureGroups_Upload_Asset_Form_Add_Asset_Button'),
            action: 'add_asset',
            hidden: !me.uploadDefinition.get('multipleUpload')
        },{
            xtype: 'panel',
            ui: 'nx-subsection',
            frame: true,
            title: NX.I18n.get('FeatureGroups_Upload_Component_Form_Title'),
            items: me.uploadDefinition.get('componentFields').map(me.createFieldWithLabel, this),
            style: {
                marginTop: '5px'
            },
            hidden: me.uploadDefinition.get('componentFields').length === 0
        },{
            xtype: 'hidden',
            name: 'repositoryName',
            value: repository.get('name')
        }],

        buttonAlign: 'left',
        buttons: [
          {
            text: NX.I18n.get('FeatureGroups_Upload_Form_Upload_Button'),
            action: 'upload',
            formBind: true,
            ui: 'nx-primary'
          }, {
            text: NX.I18n.get('FeatureGroups_Upload_Form_Discard_Button'),
            action: 'discard'
          }
        ],
        dockedItems: [{
          xtype: 'nx-actions',
          dock: 'bottom',
          items: [{
            xtype: 'panel',
            itemId: 'nx-coreui-upload-success-message'
          }]
        }]
      });
      me.doLayout();
    },

    addAssetRow: function() {
      var me = this,
          assetPanel = me.down('#nx-coreui-upload-component-assets');

      if (assetPanel) {
        var row = me.createRow(false),
            fields = row.items,
            suffix = assetPanel.items.items.length / (1 + fields.length);

        fields.forEach(function(field) {
          field.name += suffix;
        });

        assetPanel.add(row);
      }
    },

    createRow: function(firstRow) {
      var me = this,
          row = {
              xtype: 'panel',
              layout: 'column',
              style: {
                  marginBottom: '5px'
              },
              items: [{
                  xtype: 'fileuploadfield',
                  allowBlank: false,
                  submitValue: true,
                  buttonText: NX.I18n.get('FeatureGroups_Upload_Form_Browse_Button'),
                  buttonConfig: {
                      glyph: 'xf016@FontAwesome' /* fa-file-o */
                  },
                  fieldLabel: firstRow ? NX.I18n.get('FeatureGroups_Upload_Asset_Form_File_Label') : undefined,
                  name: 'file',
                  width: '300px'
              }]
          };

      var assetFields = me.uploadDefinition.get('assetFields');

      assetFields.forEach(function(assetField) {
          row.items.push(me.createField(assetField, !firstRow));
      });

      if (!firstRow) {
          row.items.push({
              xtype: 'button',
              text: NX.I18n.get('FeatureGroups_Upload_Asset_Form_Remove_Button'),
              action: 'remove_upload_asset'
          });
      }

      return row;
    },

    createFieldWithLabel: function (field) {
      var me = this;
      return me.createField(field);
    },

    createField: function (field, hideLabel) {
      var widget = {
        allowBlank: field.optional,
        name: field.name,
        fieldLabel: hideLabel ? undefined : field.name,
        width: '100px',
        style: {
            marginLeft: '5px'
        }
      };

      if (field.type === 'STRING') {
        widget.xtype = 'textfield';
      }
      else if (field.type === 'BOOLEAN') {
        widget.xtype = 'checkbox';
      }
      return widget;
    }
  });
