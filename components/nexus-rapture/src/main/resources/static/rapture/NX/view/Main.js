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
 * Main uber mode panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.Main', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-main',
  requires: [
    'NX.I18n',
    'NX.Icons',
    'NX.view.header.QuickSearch',
    'Ext.button.Button'
  ],

  layout: 'border',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {
        xtype: 'panel',
        layout: {
          type: 'vbox',
          align: 'stretch'
        },
        items: [
          {
            xtype: 'nx-header-panel'
          }
        ],
        region: 'north',
        collapsible: false
      },

      {
        xtype: 'nx-feature-menu',
        region: 'west',
        border: false,
        resizable: true,
        resizeHandles: 'e'
      },

      {
        xtype: 'nx-feature-content',
        region: 'center',
        border: true
      },

      {
        xtype: 'nx-footer',
        region: 'south',
        hidden: true
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

    me.down('nx-header-panel>toolbar').add([
      // 2x pad
      ' ', ' ',
      {
        xtype: 'nx-header-mode',
        name: 'browse',
        title: NX.I18n.get('Header_BrowseMode_Title'),
        tooltip: NX.I18n.get('Header_BrowseMode_Tooltip'),
        glyph: 'xf1b2@FontAwesome', /* fa-cube */
        autoHide: true,
        collapseMenu: true
      },
      {
        xtype: 'nx-header-mode',
        name: 'admin',
        title: NX.I18n.get('Header_AdminMode_Title'),
        tooltip: NX.I18n.get('Header_AdminMode_Tooltip'),
        glyph: 'xf013@FontAwesome', /* fa-gear */
        autoHide: true,
        collapseMenu: false
      },
      ' ',
      {xtype: 'nx-header-quicksearch', hidden: true},
      '->',
      {
        id: 'nx-health-check-warnings',
        xtype: 'button',
        name: 'metric-health',
        tooltip: NX.I18n.get('Header_Health_Tooltip'),
        glyph: 'xf058@FontAwesome', /* fa-check-circle */
        autoHide: true,
        hidden: true,
        collapseMenu: false,
        ui: 'nx-mode',
        cls: ['nx-health-button-green', 'nx-modebutton'],
        onClick: function() {
          NX.Bookmarks.navigateTo(NX.Bookmarks.fromToken('admin/support/status'));
        }
      },
      {xtype: 'nx-header-refresh', ui: 'nx-header'},
      {xtype: 'nx-header-help', ui: 'nx-header'},
      {
        xtype: 'nx-header-mode',
        name: 'user',
        title: NX.I18n.get('Header_UserMode_Title'),
        glyph: 'xf007@FontAwesome', // fa-user
        autoHide: false,
        collapseMenu: false
      },
      {xtype: 'nx-header-signin', ui: 'nx-header'},
      {xtype: 'nx-header-signout', ui: 'nx-header'}
    ]);
  }

});
