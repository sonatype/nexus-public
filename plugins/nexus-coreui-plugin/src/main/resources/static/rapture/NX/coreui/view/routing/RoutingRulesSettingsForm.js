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
 * Routing Rules "Settings" form.
 *
 * @since 3.16
 */
Ext.define('NX.coreui.view.routing.RoutingRulesSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-routing-rules-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  initComponent: function() {
    var me = this;

    me.settingsFormSuccessMessage = me.settingsFormSuccessMessage || function(data) {
          return NX.I18n.format('RoutingRules_SettingsForm_Update_Success', data['name']);
        };

    me.editableMarker = me.editableMarker || NX.I18n.get('RoutingRules_SettingsForm_Update_Error');

    me.editableCondition = me.editableCondition || NX.Conditions.watchState('routingRules');

    me.items = [
      {
        xtype: 'fieldcontainer',
        items: {
          xtype: 'fieldset',
          cls: 'nx-form-section',
          defaults: {
            xtype: 'textfield',
            allowBlank: false
          },
          items: [
            {
              name: 'name',
              itemId: 'name',
              fieldLabel: NX.I18n.get('RoutingRules_Name_Label')
            },
            {
              name: 'description',
              itemId: 'description',
              fieldLabel: NX.I18n.get('RoutingRules_Description_Label'),
              allowBlank: true
            },
            {
              xtype: 'fieldcontainer',
              itemId: 'modeContainer',
              fieldLabel: NX.I18n.get('RoutingRules_Mode_Label'),
              layout: 'hbox',
              items: [
                {
                  xtype: 'combo',
                  name: 'mode',
                  editable: false,
                  width: '80px',
                  store: [
                    ['BLOCK', NX.I18n.get('RoutingRules_Mode_Block_Text')],
                    ['ALLOW', NX.I18n.get('RoutingRules_Mode_Allow_Text')]
                  ],
                  value: 'BLOCK'
                },
                {
                  xtype: 'label',
                  text: NX.I18n.get('RoutingRules_Mode_Common_Text'),
                  cls: 'nx-mode-common-text'
                }
              ]
            },
            {
              xtype: 'fieldset',
              cls: 'nx-coreui-routing-rules-settings-form-matchers',
              itemId: 'nx-coreui-routing-rules-settings-form-matchers',
              items: [
                {
                  xtype: 'label',
                  cls: 'nx-matchers-label',
                  text: NX.I18n.get('RoutingRules_Matchers_Label')
                },
                {
                  xtype: 'label',
                  cls: 'nx-matchers-help-text',
                  text: NX.I18n.get('RoutingRules_Matchers_Description')
                }
              ]
            },
            {
              xtype: 'button',
              glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
              text: NX.I18n.get('RoutingRules_Matchers_Add_Button'),
              tooltip: NX.I18n.get('RoutingRules_Matchers_Add_Button'),
              handler: me.onAddMatcherClick.bind(this)
            }
          ]
        }
      }
    ];

    me.callParent();
  },

  /**
   * @private
   */
  getMatchersSection: function() {
    return this.down('#nx-coreui-routing-rules-settings-form-matchers');
  },

  getMatcherFields: function() {
    var matchersSection = this.getMatchersSection();
    return matchersSection.query('textfield');
  },

  addMatcherRow: function(value) {
    var matchersSection = this.getMatchersSection(),
        index = this.getMatcherFields().length,
        row = {
          xtype: 'panel',
          cls: 'nx-repeated-row',
          layout: 'column',
          items: [
            {
              xtype: 'textfield',
              allowBlank: false,
              name: 'matchers[' + index + ']',
              width: 'calc(100% - 36px)',
              value: value
            },
            {
              xtype: 'button',
              cls: 'nx-matcher-remove-button',
              glyph: 'xf1f8@FontAwesome' /* fa-trash */,
              tooltip: NX.I18n.get('RoutingRules_Matchers_Remove_Button'),
              handler: this.onRemoveMatcherClick.bind(this)
            }
          ]
        };

    matchersSection.add(row);
    this.setFirstRemoveButtonState();
  },

  onAddMatcherClick: function() {
    var textFields, lastTextField;

    this.addMatcherRow();

    textFields = this.getMatcherFields();
    lastTextField = textFields[textFields.length - 1];

    // show "This field is required" message immediately to prevent Add
    // button from moving when multiple empty matchers are added
    lastTextField.validate();

    lastTextField.focus();
  },

  onRemoveMatcherClick: function(button) {
    var matchersSection = this.getMatchersSection(),
        row = button.up();

    // hack to get form to detect a state change and enable the action buttons
    matchersSection.add({
      xtype: 'hiddenfield',
      isDirty: function() {
        return true;
      }
    });
    this.fireEvent('dirtychange', this.getForm());

    matchersSection.remove(row);

    this.setFirstRemoveButtonState();

    // rename matcher fields to keep index order consistent
    this.getMatcherFields().forEach(function(matcherField, index) {
      var newName = 'matchers[' + index + ']';
      window.document.getElementById(matcherField.getInputId()).name = newName;
      matcherField.name = newName;
    }, this);
  },

  setFirstRemoveButtonState: function() {
    var matchersSection = this.getMatchersSection(),
        removeButtons = matchersSection.query('button[cls=nx-matcher-remove-button]');

    if (removeButtons.length > 1) {
      removeButtons[0].enable();
    }
    else {
      removeButtons[0].disable();
    }
  },

  resetMatchersSection: function() {
    var matchersSection = this.getMatchersSection(),
        matcherRows = this.items && this.query('panel') || [],
        dirtyHiddenFields = this.items && this.query('hiddenfield') || [];

    dirtyHiddenFields.forEach(function(field) {
      matchersSection.remove(field);
    });

    matcherRows.forEach(function(row) {
      matchersSection.remove(row);
    });
  }

});
