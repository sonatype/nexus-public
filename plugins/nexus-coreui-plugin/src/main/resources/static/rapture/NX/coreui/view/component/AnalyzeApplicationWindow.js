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
/*global Ext*/

/**
 * Analyze application window.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.view.component.AnalyzeApplicationWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-component-analyze-window',
  requires: [
    'NX.I18n'
  ],
  ui: 'nx-inset',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.setWidth(NX.view.ModalDialog.LARGE_MODAL);

    me.title = NX.I18n.get('AnalyzeApplicationWindow_Title');

    me.items = {
      xtype: 'form',
      items: [
        {
          xtype: 'label',
          html: NX.I18n.get('AnalyzeApplicationWindow_Form_Html')
        },
        {
          xtype: 'nx-email',
          fieldLabel: NX.I18n.get('AnalyzeApplicationWindow_Form_Email_FieldLabel'),
          helpText: NX.I18n.get('AnalyzeApplicationWindow_Form_Email_HelpText'),
          name: 'emailAddress',
          allowBlank: false
        },
        {
          xtype: 'textfield',
          fieldLabel: NX.I18n.get('AnalyzeApplicationWindow_Form_Password_FieldLabel'),
          helpText: NX.I18n.get('AnalyzeApplicationWindow_Form_Password_HelpText'),
          name: 'password',
          inputType: 'password',
          allowBlank: false
        },
        {
          xtype: 'textfield',
          fieldLabel: NX.I18n.get('AnalyzeApplicationWindow_Form_ProprietaryPackages_FieldLabel'),
          helpText: NX.I18n.get('AnalyzeApplicationWindow_Form_ProprietaryPackages_HelpText'),
          name: 'proprietaryPackages'
        },
        {
          xtype: 'textfield',
          fieldLabel: NX.I18n.get('AnalyzeApplicationWindow_Form_Label_FieldLabel'),
          helpText: NX.I18n.get('AnalyzeApplicationWindow_Form_Label_HelpText'),
          name: 'reportLabel'
        },
        {
          xtype: 'combo',
          name: 'asset',
          itemId: 'asset',
          fieldLabel: NX.I18n.get('AnalyzeApplicationWindow_Form_Asset_FieldLabel'),
          helpText: NX.I18n.get('AnalyzeApplicationWindow_Form_Asset_HelpText'),
          emptyText: NX.I18n.get('AnalyzeApplicationWindow_Form_Asset_EmptyText'),
          editable: false,
          store: Ext.create('Ext.data.ArrayStore', {
            fields: ['value', 'display']
          }),
          valueField: 'value',
          displayField: 'display',
          queryMode: 'local',
          hidden: true
        }
      ],
      buttonAlign: 'left',
      buttons: [
        { text: NX.I18n.get('AnalyzeApplicationWindow_Analyze_Button'), action: 'analyze', formBind: true, ui: 'nx-primary' },
        { text: NX.I18n.get('AnalyzeApplicationWindow_Cancel_Button'), handler: function () {
          this.up('window').close();
        }}
      ]
    };

    me.callParent();
  }

});
