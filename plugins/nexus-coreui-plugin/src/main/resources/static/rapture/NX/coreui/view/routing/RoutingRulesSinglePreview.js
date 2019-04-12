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
 * Single Routing Rule preview section
 *
 * @since 3.16
 */
Ext.define('NX.coreui.view.routing.RoutingRulesSinglePreview', {
  extend: 'Ext.form.Panel',
  alias: 'widget.nx-coreui-routing-rules-single-preview',
  requires: [
    'NX.I18n'
  ],

  ui: 'nx-subsection',
  frame: true,

  buttonAlign: 'left',

  initComponent: function() {

    this.items = [
      {
        xtype: 'fieldset',
        itemId: 'wrapper',
        cls: 'nx-form-section nx-routing-rules-single-preview-field-wrapper',
        title: NX.I18n.get('RoutingRules_SinglePreview_Title'),
        items: [
          {
            xtype: 'fieldcontainer',
            cls: 'nx-routing-rules-test-path-container',
            itemId: 'pathContainer',
            fieldLabel: NX.I18n.get('RoutingRules_SinglePreview_Path_Label'),
            helpText: NX.I18n.get('RoutingRules_SinglePreview_Path_HelpText'),
            layout: 'hbox',
            maxWidth: 800,
            items: [
              {
                xtype: 'label',
                cls: 'nx-routing-rules-path-prefix',
                text: '/'
              },
              {
                xtype: 'textfield',
                name: 'path',
                itemId: 'path',
                allowBlank: true,
                maxWidth: 786,
                width: 'calc(100% - 14px)',
                listeners: {
                  blur: this.onPathBlur.bind(this),
                  focus: this.onPathFocus.bind(this),
                  change: this.hideTestResult.bind(this)
                }
              }
            ]
          },
          {
            xtype: 'label',
            cls: 'nx-routing-rules-test-result',
            itemId: 'testResult',
            hidden: true
          }
        ]
      }
    ];

    this.buttons = [
      {
        text: NX.I18n.get('RoutingRules_SinglePreview_Test_Button'),
        tooltip: NX.I18n.get('RoutingRules_SinglePreview_Test_Button_Tooltip'),
        action: 'test',
        bindToEnter: true
      }
    ];

    this.callParent();
  },

  onPathBlur: function(pathTextfield) {
    var pathContainer = pathTextfield.up('#pathContainer');
    pathContainer.removeCls('nx-textfield-focused');
  },

  onPathFocus: function(pathTextfield) {
    var pathContainer = pathTextfield.up('#pathContainer');
    pathContainer.addCls('nx-textfield-focused');
  },

  setTestResult: function(isAllowed) {
    var resultLabel = this.down('#testResult'),
        wrapper = this.down('#wrapper');
    if (isAllowed) {
      resultLabel.setHtml(NX.I18n.get('RoutingRules_SinglePreview_Allowed_Html'));
      wrapper.removeCls('blocked');
      wrapper.addCls('allowed');
    }
    else {
      resultLabel.setHtml(NX.I18n.get('RoutingRules_SinglePreview_Blocked_Html'));
      wrapper.addCls('blocked');
      wrapper.removeCls('allowed');
    }
    resultLabel.show();
  },

  hideTestResult: function() {
    var resultLabel = this.down('#testResult'),
        wrapper = this.down('#wrapper');
    resultLabel.hide();
    wrapper.removeCls('blocked');
    wrapper.removeCls('allowed');
  }

});
