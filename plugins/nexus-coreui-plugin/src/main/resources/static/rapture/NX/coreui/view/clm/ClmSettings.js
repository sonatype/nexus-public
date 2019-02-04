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
 * CLM settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.clm.ClmSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-clm-settings',
  requires: [
    'NX.Conditions',
    'NX.I18n',
    'NX.ext.button.Button'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.settingsForm = {
      xtype: 'nx-settingsform',
      settingsFormSuccessMessage: 'IQ Server Settings $action',
      api: {
        load: 'NX.direct.clm_CLM.read',
        submit: 'NX.direct.clm_CLM.update'
      },
      editableCondition: NX.Conditions.isPermitted('nexus:settings:update'),
      editableMarker: NX.I18n.get('Clm_ClmSettings_Permission_Error'),
      items: [
        {
          xtype: 'label',
          html: NX.I18n.get('ClmSettings_Html')
        },
        {
          xtype: 'checkbox',
          name: 'enabled',
          fieldLabel: NX.I18n.get('ClmSettings_Enable_FieldLabel'),
          helpText: NX.I18n.get('ClmSettings_Enable_HelpText')
        },
        {
          xtype: 'nx-url',
          name: 'url',
          fieldLabel: NX.I18n.get('ClmSettings_URL_FieldLabel'),
          helpText: NX.I18n.get('ClmSettings_URL_HelpText'),
          emptyText: NX.I18n.get('ClmSettings_URL_EmptyText')
        },
        {
          xtype: 'combo',
          name: 'authenticationType',
          fieldLabel: NX.I18n.get('ClmSettings_AuthenticationType_FieldLabel'),
          editable: false,
          store: [
            ['USER', NX.I18n.get('ClmSettings_AuthenticationType_User')],
            ['PKI', NX.I18n.get('ClmSettings_AuthenticationType_Pki')]
          ],
          listeners: {
            'change': function(combo) {
              var userAuthFields = this.up('form').down('#userAuthFields');

              if(combo.getValue() === 'USER') {
                userAuthFields.show();
                userAuthFields.enable();
              }
              else {
                userAuthFields.hide();
                userAuthFields.disable();
              }
            }
          }
        },
        {
          xtype: 'fieldcontainer',
          itemId: 'userAuthFields',
          hidden: true,
          items: [
            {
              xtype:'textfield',
              name: 'username',
              fieldLabel: NX.I18n.get('ClmSettings_Username_FieldLabel'),
              helpText: NX.I18n.get('ClmSettings_Username_HelpText'),
              emptyText: NX.I18n.get('ClmSettings_Username_EmptyText'),
              allowBlank: false,
              inputAttrTpl: 'autocomplete="new-username"'
            },
            {
              xtype: 'nx-password',
              name: 'password',
              fieldLabel: NX.I18n.get('ClmSettings_Password_FieldLabel'),
              helpText: NX.I18n.get('ClmSettings_Password_HelpText'),
              emptyText: NX.I18n.get('ClmSettings_Password_EmptyText'),
              allowBlank: false,
              inputAttrTpl: 'autocomplete="new-password"'
            }
          ]
        },
        {
          xtype: 'numberfield',
          name: 'timeout',
          fieldLabel: NX.I18n.get('ClmSettings_ConnectionTimeout_FieldLabel'),
          helpText: NX.I18n.get('ClmSettings_ConnectionTimeout_HelpText'),
          emptyText: NX.I18n.get('ClmSettings_ConnectionTimeout_EmptyText'),
          allowBlank: true,
          allowDecimals: false,
          allowExponential: false,
          minValue: 1,
          maxValue: 3600
        },
        {
          xtype: 'textareafield',
          name: 'properties',
          fieldLabel: NX.I18n.get('ClmSettings_Properties_FieldLabel'),
          helpText: NX.I18n.get('ClmSettings_Properties_HelpText'),
          emptyText: NX.I18n.get('ClmSettings_Properties_EmptyText'),
          allowBlank: true
        }
      ]
    };

    me.dockedItems = {
      xtype: 'nx-actions',
      dock: 'top',
      items: [
        {
          xtype: 'nx-button',
          action: 'open',
          text: NX.I18n.get('Clm_Dashboard_Description'),
          glyph: 'xf08e@FontAwesome' /* fa-external-link */
        }
      ]
    };

    me.callParent();

    me.down('nx-settingsform').getDockedItems('toolbar[dock="bottom"]')[0].add({
      xtype: 'button', text: NX.I18n.get('ClmSettings_Properties_Verify_Button'), formBind: true, action: 'verify'
    });
  }

});
