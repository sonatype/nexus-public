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
 * Support Zip panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.SupportZip', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-support-supportzip',
  requires: [
    'NX.Conditions',
    'NX.I18n',
    'NX.State'
  ],
  listeners: {
    afterrender: function () {
      if (!NX.State.isClustered()) {
        this.query('button[action=hazips]')[0].setVisible(false);
      }
    }
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        uiSettings = NX.State.getValue('uiSettings', {});

    me.settingsForm = {
      xtype: 'nx-settingsform',
      settingsFormSubmitMessage: NX.I18n.get('Support_SupportZip_Creating_Message'),
      settingsFormSuccessMessage: NX.I18n.get('Support_SupportZip_Create_Success'),
      timeout: uiSettings['longRequestTimeout'],
      isDirty: function() { return false; }, // form is never saved, so never dirty
      api: {
        submit: 'NX.direct.atlas_SupportZip.create'
      },
      editableCondition: NX.Conditions.isPermitted('nexus:atlas:create'),
      editableMarker: NX.I18n.get('SupportZip_Permission_Error'),
      items: [
        {
          xtype: 'label',
          html: NX.I18n.get('SupportZip_HelpText')
        },
        {
          xtype: 'checkboxgroup',
          fieldLabel: NX.I18n.get('Support_SupportZip_Contents_FieldLabel'),
          columns: 1,
          allowBlank: false,
          items: [
            {
              xtype: 'checkbox',
              name: 'systemInformation',
              boxLabel: NX.I18n.get('Support_SupportZip_Report_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'threadDump',
              boxLabel: NX.I18n.get('Support_SupportZip_Dump_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'configuration',
              boxLabel: NX.I18n.get('Support_SupportZip_Configuration_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'security',
              boxLabel: NX.I18n.get('Support_SupportZip_Security_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'log',
              boxLabel: NX.I18n.get('Support_SupportZip_LogFiles_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'taskLog',
              boxLabel: NX.I18n.get('Support_SupportZip_TaskLogFiles_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'auditLog',
              boxLabel: NX.I18n.get('Support_SupportZip_AuditLogFiles_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'metrics',
              boxLabel: NX.I18n.get('Support_SupportZip_Metrics_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'jmx',
              boxLabel: NX.I18n.get('Support_SupportZip_JMX_BoxLabel'),
              checked: true
            }
          ]
        },
        {
          xtype: 'checkboxgroup',
          fieldLabel: NX.I18n.get('Support_SupportZip_Options_FieldLabel'),
          allowBlank: true,
          columns: 1,
          items: [
            {
              xtype: 'checkbox',
              name: 'limitFileSizes',
              boxLabel: NX.I18n.get('Support_SupportZip_Included_BoxLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'limitZipSize',
              boxLabel: NX.I18n.get('Support_SupportZip_Max_BoxLabel'),
              checked: true
            }
          ]
        }
      ],

      buttonAlign: 'left',

      buttons: [
        {
          text: NX.I18n.get('Support_SupportZip_Create_Button'),
          formBind: true,
          glyph: 'xf019@FontAwesome' /* fa-download */,
          action: 'submit',
          ui: 'nx-primary'
        },
        {
          text: NX.I18n.get('Support_HA_SupportZip_Create_Button'),
          formBind: true,
          glyph: 'xf019@FontAwesome' /* fa-download */,
          action: 'hazips',
          ui: 'nx-primary',
          disabled: !NX.State.isClustered()
        }
      ]
    };

    me.callParent();
  }
});
