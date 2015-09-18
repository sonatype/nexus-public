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

  api: {
    submit: 'NX.direct.coreui_Blobstore.update'
  },
  settingsFormSuccessMessage: function(data) {
    return NX.I18n.get('Blobstore_BlobstoreSettingsForm_Update_Success') + data['name'];
  },

  editableMarker: NX.I18n.get('Blobstore_BlobstoreSettingsForm_Update_Error'),

  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.never();

    me.items = [
      {
        xtype: 'textfield',
        name: 'name',
        itemId: 'name',
        fieldLabel: NX.I18n.get('Blobstore_BlobstoreSettingsForm_Name_FieldLabel'),
        readOnly: true
      },
      {
        xtype: 'textarea',
        name: 'attributes',
        fieldLabel: NX.I18n.get('Blobstore_BlobstoreSettingsForm_Attributes_FieldLabel'),
        height: 300,
        allowBlank: false,
        cls: 'nx-monospace-field'
      }
    ];

    me.callParent(arguments);
  }
});
