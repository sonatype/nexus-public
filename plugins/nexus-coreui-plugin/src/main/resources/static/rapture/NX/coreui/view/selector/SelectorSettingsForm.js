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
 * Selector "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.selector.SelectorSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-selector-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  api: {
    submit: 'NX.direct.coreui_Selector.update'
  },

  initComponent: function() {
    var me = this;

    me.settingsFormSuccessMessage = me.settingsFormSuccessMessage || function(data) {
          return NX.I18n.format('Selector_SelectorSettingsForm_Update_Success', data['name']);
        };

    me.editableMarker = me.editableMarker || NX.I18n.get('Selector_SelectorSettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.isPermitted('nexus:selectors:update');

    me.items = [
      {
        xtype: 'hiddenfield',
        name: 'id'
      },
      {
        xtype: 'hiddenfield',
        name: 'type',
        value: 'jexl'
      },
      {
        xtype: 'fieldcontainer',
        items: {
          xtype: 'fieldset',
          cls: 'nx-form-section',
          title: NX.I18n.get('Selector_SelectorSettingsForm_SelectorID_Title'),
          defaults: {
            xtype: 'textfield',
            allowBlank: false
          },
          items: [
            {
              name: 'name',
              itemId: 'name',
              fieldLabel: NX.I18n.get('Selector_SelectorSettingsForm_Name_FieldLabel'),
              readOnly: true
            },
            {
              name: 'description',
              allowBlank: true,
              fieldLabel: NX.I18n.get('Selector_SelectorSettingsForm_Description_FieldLabel')
            }
          ]
        }
      },
      {
        xtype: 'fieldcontainer',
        items: {
          xtype: 'fieldset',
          cls: 'nx-form-section',
          title: NX.I18n.get('Selector_SelectorSettingsForm_Specification_Title'),
          defaults: {
            xtype: 'textfield',
            allowBlank: false
          },
          items: {
            xtype: 'textareafield',
            name: 'expression',
            itemId: 'expression',
            fieldLabel: NX.I18n.get('Selector_SelectorSettingsForm_Expression_FieldLabel'),
            helpText: NX.I18n.get('Selector_SelectorSettingsForm_Expression_HelpText'),
            afterBodyEl: NX.I18n.get('Selector_SelectorSettingsForm_Expression_AfterBodyEl')
          }
        }
      }
    ];

    me.callParent();
  }

});
