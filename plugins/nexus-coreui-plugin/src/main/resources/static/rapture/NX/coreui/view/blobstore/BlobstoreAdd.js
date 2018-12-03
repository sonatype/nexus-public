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
 * Add blobstore window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.blobstore.BlobstoreAdd', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-blobstore-add',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  defaultFocus: 'type',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    // cache the default work directory path
    NX.direct.coreui_Blobstore.defaultWorkDirectory(function(response) {
      if (Ext.isObject(response) && response.success) {
        me.defaultWorkDirectory = response.data.path;
        me.fileSeparator = response.data.fileSeparator;
      }
    });

    me.settingsForm = {
      xtype: 'nx-coreui-blobstore-settings-form',
      api: {
        submit: 'NX.direct.coreui_Blobstore.create'
      },
      settingsFormSuccessMessage: function(data) {
        return NX.I18n.get('Blobstore_BlobstoreAdd_Create_Success') + data['name'];
      },
      editableCondition: NX.Conditions.isPermitted('nexus:blobstores:create'),
      editableMarker: NX.I18n.get('Blobstore_BlobstoreAdd_Create_Error'),

      buttons: [
        { text: NX.I18n.get('Blobstore_BlobstoreList_New_Button'), action: 'add', formBind: true, ui: 'nx-primary' },
        { text: NX.I18n.get('Add_Cancel_Button'), action: 'back' }
      ]
    };

    me.callParent();

    var typeCombo = me.down('#type');
    typeCombo.setReadOnly(false);
    typeCombo.on({
      beforerender: function() {
        var me = this;
        me.setValue(me.getStore('BlobstoreType').first().data.id);
        typeCombo.resetOriginalValue();
      }
    });

    var nameField = me.down('#name');
    nameField.setReadOnly(false);
    // onChange listener to pre-populate the path
    nameField.on({
      change: function(f, newName, oldName) {
        var pathField = f.up().query('[name=property_path]')[0],
            wd = me.defaultWorkDirectory,
            oldPath = wd + me.fileSeparator + oldName;
        if (pathField && (!pathField.getValue() || pathField.getValue() === oldPath)) {
          pathField.setValue(wd + me.fileSeparator + newName);
        }
      }
    });

    // Enable quota fields
    Ext.Array.forEach(['#isQuotaEnabled', '#quotaType', '#quotaLimit'], function(f) { me.down(f).setReadOnly(false); });
  }
});
