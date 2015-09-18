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
 * Security anonymous settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.security.AnonymousSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-security-anonymous-settings',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  initComponent: function () {
    var me = this;

    me.settingsForm = [
      {
        xtype: 'nx-settingsform',
        settingsFormSuccessMessage: NX.I18n.get('Security_AnonymousSettings_Update_Success'),
        api: {
          load: 'NX.direct.coreui_AnonymousSettings.read',
          submit: 'NX.direct.coreui_AnonymousSettings.update'
        },
        editableCondition: NX.Conditions.isPermitted('nexus:settings:update'),
        editableMarker: NX.I18n.get('Security_AnonymousSettings_Update_Error'),
        items: [
          {
            xtype: 'checkbox',
            name: 'enabled',
            value: true,
            boxLabel: NX.I18n.get('Security_AnonymousSettings_Allow_BoxLabel')
          },
          {
            xtype: 'textfield',
            name: 'userId',
            fieldLabel: NX.I18n.get('Security_AnonymousSettings_Username_FieldLabel'),
            allowBlank: false
          },
          {
            xtype: 'combo',
            name: 'realmName',
            fieldLabel: NX.I18n.get('Security_AnonymousSettings_Realm_FieldLabel'),
            queryMode: 'local',
            displayField: 'name',
            valueField: 'id',
            store: 'RealmType',
            editable: false
          }
        ]
      }
    ];

    me.callParent(arguments);
  }
});
