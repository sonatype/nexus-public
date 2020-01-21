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
 * Add datastore window.
 *
 * @since 3.19
 */
Ext.define('NX.coreui.view.datastore.DatastoreAdd', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-datastore-add',
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

    me.settingsForm = {
      xtype: 'nx-coreui-datastore-settings-form',
      api: {
        submit: 'NX.direct.coreui_Datastore.create'
      },
      settingsFormSuccessMessage: function(data) {
        return NX.I18n.get('Datastore_DatastoreAdd_Create_Success') + data['name'];
      },
      editableCondition: NX.Conditions.isPermitted('nexus:datastores:create'),
      editableMarker: NX.I18n.get('Datastore_DatastoreAdd_Create_Error'),

      buttons: [
        { text: NX.I18n.get('Datastore_DatastoreList_New_Button'), action: 'add', formBind: true, ui: 'nx-primary' },
        { text: NX.I18n.get('Add_Cancel_Button'), action: 'back' }
      ]
    };

    me.callParent();

    var sourceCombo = me.down('#source');
    sourceCombo.setReadOnly(false);
    sourceCombo.on({
      beforerender: function() {
        var me = this;
        me.setStore('ModifiableDatastoreSource');
        me.setValue(me.getStore('ModifiableDatastoreSource').first().data.id);
        sourceCombo.resetOriginalValue();
      }
    });

    var typeCombo = me.down('#type');
    typeCombo.setReadOnly(false);
    typeCombo.on({
      beforerender: function() {
        var me = this;
        me.setValue(me.getStore('DatastoreType').first().data.id);
        typeCombo.resetOriginalValue();
      }
    });

    var nameField = me.down('#name');
    nameField.setReadOnly(false);
  }
});
