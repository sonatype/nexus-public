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
 * Log Viewer panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.logging.LogViewer', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-log-viewer',
  requires: [
    'NX.I18n'
  ],

  layout: 'fit',

  items: {
    xtype: 'textarea',
    cls: 'nx-log-viewer-field nx-monospace-field',
    readOnly: true,
    hideLabel: true,
    emptyText: NX.I18n.get('Logging_LogViewer_EmptyText'),
    inputAttrTpl: 'wrap="off"'
  },

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions nx-borderless',
    items: [
      {
        xtype: 'button',
        text: NX.I18n.get('Logging_LogViewer_Download_Button'),
        glyph: 'xf019@FontAwesome' /* fa-download */,
        action: 'download'
      },
      '-',
      {
        xtype: 'button',
        text: NX.I18n.get('Logging_LogViewer_Mark_Button'),
        glyph: 'xf11e@FontAwesome' /* fa-flag-checkered */,
        action: 'mark',
        disabled: true
      },
      '->',
      {
        xtype: 'label',
        text: NX.I18n.get('Logging_LogViewer_Refresh_Text')
      },
      {
        xtype: 'combo',
        itemId: 'refreshPeriod',
        width: 140,
        editable: false,
        value: 0,
        store: [
          [0, NX.I18n.get('Logging_LogViewer_Refresh_ManualItem')],
          [20, NX.I18n.get('Logging_LogViewer_Refresh_20SecondsItem')],
          [60, NX.I18n.get('Logging_LogViewer_Refresh_MinuteItem')],
          [120, NX.I18n.get('Logging_LogViewer_Refresh_2MinutesItem')],
          [300, NX.I18n.get('Logging_LogViewer_Refresh_5MinutesItem')]
        ],
        queryMode: 'local'
      },
      {
        xtype: 'combo',
        itemId: 'refreshSize',
        width: 120,
        editable: false,
        value: 25,
        store: [
          [25, NX.I18n.get('Logging_LogViewer_Last25KBItem')],
          [50, NX.I18n.get('Logging_LogViewer_Last50KBItem')],
          [100, NX.I18n.get('Logging_LogViewer_Last100KBItem')]
        ],
        queryMode: 'local'
      }
    ]
  }]

});
