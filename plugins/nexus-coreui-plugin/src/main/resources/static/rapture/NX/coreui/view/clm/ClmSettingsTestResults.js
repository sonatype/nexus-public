/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * CLM test results window (shows a list of applications).
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.clm.ClmSettingsTestResults', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-clm-settings-testresults',
  requires: [
    'Ext.data.JsonStore',
    'NX.I18n'
  ],

  /**
   * @cfg json array of applications (as returned by checking the connection)
   */
  applications: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.title = NX.I18n.get('Clm_SettingsTestResults_Title');

    me.layout = 'fit';
    me.closable = true;
    me.autoShow = true;
    me.modal = true;
    me.constrain = true;

    me.buttonAlign = 'left';
    me.buttons = [
      { text: NX.I18n.get('Button_Close'), handler: function () {
        this.up('window').close();
      }}
    ];

    me.items = {
      xtype: 'grid',
      emptyText: NX.I18n.get('Clm_SettingsTestResults_EmptyText'),
      columns: [
        { header: NX.I18n.get('Clm_SettingsTestResults_Id_Header'), dataIndex: 'id', flex: 1, renderer: Ext.htmlEncode },
        { header: NX.I18n.get('Clm_SettingsTestResults_Name_Header'), dataIndex: 'name', flex: 1, renderer: Ext.htmlEncode }
      ],
      store: Ext.create('Ext.data.JsonStore', {
        fields: ['id', 'name'],
        data: me.applications
      })
    };

    me.width = NX.view.ModalDialog.LARGE_MODAL;
    me.maxHeight = me.height = Ext.getBody().getViewSize().height - 100;

    me.callParent();
    me.center();
  }

});
