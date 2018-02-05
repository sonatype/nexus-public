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
 * Security realms settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.security.RealmSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-security-realm-settings',
  requires: [
    'NX.Conditions',
    'NX.ext.form.field.ItemSelector',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.settingsForm = [
      // basic settings
      {
        xtype: 'nx-settingsform',
        settingsFormSuccessMessage: NX.I18n.get('Security_RealmSettings_Update_Success'),
        api: {
          load: 'NX.direct.coreui_RealmSettings.read',
          submit: 'NX.direct.coreui_RealmSettings.update'
        },
        editableCondition: NX.Conditions.isPermitted('nexus:settings:update'),
        editableMarker: NX.I18n.get('Security_RealmSettings_Update_Error'),

        items: [
          {
            xtype: 'nx-itemselector',
            name: 'realms',
            fieldLabel: 'Active realms',
            buttons: ['up', 'add', 'remove', 'down'],
            fromTitle: NX.I18n.get('Security_RealmSettings_Available_FromTitle'),
            toTitle: NX.I18n.get('Security_RealmSettings_Available_ToTitle'),
            store: 'RealmType',
            valueField: 'id',
            displayField: 'name',
            delimiter: null
          }
        ]
      }
    ];

    me.callParent();
  }
});
