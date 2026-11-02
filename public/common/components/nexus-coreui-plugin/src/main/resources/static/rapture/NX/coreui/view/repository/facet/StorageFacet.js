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
 * Configuration for repository storage facet.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.StorageFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-storage-facet',
  requires: [
    'NX.I18n'
  ],

  /**
   * @cfg Boolean
   * Set whether or not strict content type validation is enabled for storage, defaults to true.
   */
  strictContentTypeValidation: true,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;


    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('Repository_Facet_StorageFacet_Title'),

        defaults: {
          allowBlank: false,
          itemCls: 'required-field'
        },

        items: [
          {
            xtype: 'combo',
            name: 'attributes.storage.blobStoreName',
            itemId: 'blobStoreName',
            fieldLabel: NX.I18n.get('Repository_Facet_StorageFacet_BlobStore_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_StorageFacet_BlobStore_HelpText'),
            emptyText: NX.I18n.get('Repository_Facet_StorageFacet_BlobStore_EmptyText'),
            editable: false,
            store: 'BlobstoreNames',
            queryMode: 'local',
            displayField: 'name',
            valueField: 'name',
            readOnlyOnUpdate: true,
            listeners: {
              afterrender: function (combo) {
                if (!combo.getValue()) {
                  var store = combo.getStore();
                  if (store.getTotalCount() === 1) {
                    var value = store.getAt(0).get('name');
                    combo.originalValue = value;
                    combo.setValue(value);
                  }
                }
              }
            }
          },
          {
            xtype: 'checkbox',
            name: 'attributes.storage.strictContentTypeValidation',
            itemId: 'strictContentTypeValidation',
            fieldLabel: NX.I18n.get('Repository_Facet_StorageFacet_ContentTypeValidation_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_StorageFacet_ContentTypeValidation_HelpText'),
            value: me.strictContentTypeValidation
          }
        ]
      }
    ];

    if (NX.State.getValue('datastores')) {
      me.items[0].items.unshift(
          {
            xtype: 'hiddenfield',
            name: 'attributes.storage.dataStoreName',
            itemId: 'dataStoreName',
            editable: false,
            readOnlyOnUpdate: true,
            hidden: true,
            value: 'nexus'
          });
    }

    if (NX.State.isEnterpriseEdition()) {
      me.items[0].items.push(
          {
            xtype: 'checkbox',
            name: 'attributes.storage.globalDistribution',
            itemId: 'globalDistribution',
            fieldLabel: NX.I18n.get('Repository_Facet_StorageFacet_GlobalDistribution_FieldLabel'),
            helpText: NX.I18n.get('Repository_Facet_StorageFacet_GlobalDistribution_HelpText'),
            value: false,
            hidden: true,
            listeners: {
              afterrender: function (checkbox) {
                var facet = checkbox.up('nx-coreui-repository-storage-facet'),
                    combo = facet.down('#blobStoreName'),
                    sync = function () {
                      facet.syncGlobalDistribution(checkbox, combo);
                    };

                checkbox.mon(combo, 'change', sync);
                checkbox.mon(combo.getStore(), 'load', sync);
                sync();
              }
            }
          });
    }

    me.callParent();
  },

  /**
   * Shows the global distribution checkbox only while the selected blob store has CloudFront or S3
   * Transfer Acceleration configured, clearing it otherwise. On a create form, seeds it from the
   * capability default each time a different blob store is selected.
   *
   * @private
   * @param {Ext.form.field.Checkbox} checkbox global distribution checkbox
   * @param {Ext.form.field.ComboBox} combo blob store selector
   */
  syncGlobalDistribution: function(checkbox, combo) {
    var blobStoreName = combo.getValue(),
        record = combo.getStore().findRecord('name', blobStoreName),
        enabled = Boolean(record && record.get('globalDistributionEnabled')),
        cdnDefault;

    if (!enabled) {
      checkbox.setValue(false);
    }
    else if (checkbox.cdnDefaultBlobStore !== blobStoreName && checkbox.up('nx-coreui-repository-add')) {
      checkbox.cdnDefaultBlobStore = blobStoreName;
      cdnDefault = Boolean(NX.State.getValue('cdnDefaultForNewRepositories', false));
      checkbox.originalValue = cdnDefault;
      checkbox.setValue(cdnDefault);
    }
    checkbox.setVisible(enabled);
  }
});
