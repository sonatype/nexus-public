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
 * Loggers grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.logging.LoggerList', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-logger-list',
  requires: [
    'NX.I18n'
  ],

  config: {
    stateful: true,
    stateId: 'nx-coreui-logger-list'
  },
  
  store: 'Logger',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function () {
        return 'logger-default';
      }
    },
    { header: NX.I18n.get('Logging_LoggerList_Name_Header'), dataIndex: 'name', stateId: 'name', hideable: false, flex: 1 },
    {
      header: NX.I18n.get('Logging_LoggerList_Level_Header'),
      dataIndex: 'level',
      stateId: 'level',
      hideable: false,
      editor: {
        xtype: 'combo',
        editable: false,
        store: [
          ['TRACE', NX.I18n.get('Logging_LoggerList_Level_TraceItem')],
          ['DEBUG', NX.I18n.get('Logging_LoggerList_Level_DebugItem')],
          ['INFO', NX.I18n.get('Logging_LoggerList_Level_InfoItem')],
          ['WARN', NX.I18n.get('Logging_LoggerList_Level_WarnItem')],
          ['ERROR', NX.I18n.get('Logging_LoggerList_Level_ErrorItem')],
          ['OFF', NX.I18n.get('Logging_LoggerList_Level_OffItem')],
          ['DEFAULT', NX.I18n.get('Logging_LoggerList_Level_DefaultItem')]
        ],
        queryMode: 'local'
      }
    }
  ],

  viewConfig: {
    emptyText: NX.I18n.get('Logging_LoggerList_EmptyText'),
    deferEmptyText: false
  },

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions nx-borderless',
    items: [
      {
        xtype: 'button',
        text: NX.I18n.get('Logging_LoggerList_New_Button'),
        glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
        action: 'new',
        disabled: true
      },
      {
        xtype: 'button',
        text: NX.I18n.get('Logging_LoggerList_Delete_Button'),
        glyph: 'xf056@FontAwesome' /* fa-minus-circle */,
        action: 'delete',
        disabled: true
      },
      '-',
      {
        xtype: 'button',
        text: NX.I18n.get('Logging_LoggerList_Reset_Button'),
        glyph: 'xf0e2@FontAwesome' /* fa-undo */,
        action: 'reset',
        disabled: true
      }
    ]
  }],

  plugins: [
    { pluginId: 'editor', ptype: 'rowediting', clicksToEdit: 1, errorSummary: false },
    { ptype: 'gridfilterbox', emptyText: NX.I18n.get('Logging_LoggerList_Filter_EmptyText') }
  ]

});
