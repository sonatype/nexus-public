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
  extend: 'Ext.container.Container',
  alias: 'widget.nx-unsupported-browser',
  requires: [
    'NX.I18n',
    'NX.Icons'
  ],

  cls: 'nx-unsupported-browser',
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
        xtype: 'container',
        region: 'center',
        layout: {
          type: 'vbox',
          align: 'center',
          pack: 'center'
        },
        items: [
          {
            xtype: 'label',
            cls: 'title',
            text: NX.I18n.get('UnsupportedBrowser_Title')
          },
          {
            xtype: 'label',
            cls: 'description',
            text: NX.I18n.get('UnsupportedBrowser_Alternatives_Text')
          },
          {
            xtype: 'container',
            cls: 'icons',
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
