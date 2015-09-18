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
 * SMTP System Settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.system.SmtpSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-system-smtp-settings',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.settingsForm = [
      {
        xtype: 'nx-settingsform',
        settingsFormSuccessMessage: NX.I18n.get('System_SmtpSettings_Update_Success'),
        api: {
          load: 'NX.direct.coreui_Email.read',
          submit: 'NX.direct.coreui_Email.update'
        },
        editableCondition: NX.Conditions.isPermitted('nexus:settings:update'),
        editableMarker: NX.I18n.get('System_SmtpSettings_Update_Error'),

        items: [
          {
            xtype: 'checkbox',
            name: 'enabled',
            fieldLabel: NX.I18n.get('System_SmtpSettings_Enabled_FieldLabel')
          },
          {
            xtype: 'textfield',
            name: 'host',
            fieldLabel: NX.I18n.get('System_SmtpSettings_Host_FieldLabel')
          },
          {
            xtype: 'numberfield',
            name: 'port',
            fieldLabel: NX.I18n.get('System_SmtpSettings_Port_FieldLabel'),
            minValue: 1,
            maxValue: 65536,
            allowDecimals: false,
            allowExponential: false
          },
          {
            xtype: 'nx-email',
            name: 'fromAddress',
            fieldLabel: NX.I18n.get('System_SmtpSettings_FromAddress_FieldLabel')
          },
          {
            xtype: 'textfield',
            name: 'subjectPrefix',
            fieldLabel: NX.I18n.get('System_SmtpSettings_SubjectPrefix_FieldLabel'),
            allowBlank: true
          },
          {
            xtype: 'textfield',
            name: 'username',
            fieldLabel: NX.I18n.get('System_SmtpSettings_Username_FieldLabel'),
            allowBlank: true
          },
          {
            xtype: 'nx-password',
            name: 'password',
            fieldLabel: NX.I18n.get('System_SmtpSettings_Password_FieldLabel'),
            allowBlank: true
          }
        ]
      }
    ];

    me.callParent(arguments);

    me.down('nx-settingsform').getDockedItems('toolbar[dock="bottom"]')[0].add({
      xtype: 'button',
      text: NX.I18n.get('System_SmtpSettings_VerifyServer_Button'),
      formBind: true,
      action: 'verify',
      glyph: 'xf003@FontAwesome' /* fa-envelope-o */
    });
  }
});
