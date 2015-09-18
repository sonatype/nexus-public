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
 * Unsupported browser uber mode panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.UnsupportedBrowser', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-unsupported-browser',
  requires: [
    'NX.I18n',
    'NX.Icons'
  ],

  layout: 'border',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {
        xtype: 'nx-header-panel',
        region: 'north',
        collapsible: false
      },

      {
        xtype: 'panel',
        region: 'center',
        layout: {
          type: 'vbox',
          align: 'center',
          pack: 'center'
        },
        items: [
          {
            xtype: 'label',
            text: NX.I18n.get('UnsupportedBrowser_Title'),
            // TODO replace style with UI
            style: {
              'color': '#000000',
              'font-size': '20px',
              'font-weight': 'bold',
              'text-align': 'center',
              'padding': '20px'
            }
          },
          {
            xtype: 'label',
            text: NX.I18n.get('UnsupportedBrowser_Alternatives_Text'),
            // TODO replace style with UI
            style: {
              'font-size': '10px'
            }
          },
          {
            xtype: 'panel',
            padding: '20 0 50 0',
            layout: {
              type: 'hbox'
            },
            items: [
              { xtype: 'image', width: 72, height: 72, src: NX.Icons.url('chrome', 'x72') },
              { xtype: 'image', width: 72, height: 72, src: NX.Icons.url('firefox', 'x72') },
              { xtype: 'image', width: 72, height: 72, src: NX.Icons.url('ie', 'x72') },
              { xtype: 'image', width: 72, height: 72, src: NX.Icons.url('opera', 'x72') },
              { xtype: 'image', width: 72, height: 72, src: NX.Icons.url('safari', 'x72') }
            ]
          },
          { xtype: 'button', text: NX.I18n.get('UnsupportedBrowser_Continue_Button'), action: 'continue' }
        ]
      },
      {
        xtype: 'nx-footer',
        region: 'south',
        hidden: false
      },
      {
        xtype: 'nx-dev-panel',
        region: 'south',
        collapsible: true,
        collapsed: true,
        resizable: true,
        resizeHandles: 'n',

        // keep initial constraints to prevent huge panels
        height: 300,

        // default to hidden, only show if debug enabled
        hidden: true
      }
    ];

    me.callParent();
  }

});
