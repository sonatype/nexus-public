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
 * Header styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Header', {
  extend: 'NX.view.dev.styles.StyleSection',
  requires: [
    'Ext.XTemplate',
    'NX.State',
    'NX.I18n'
  ],

  title: 'Header',
  layout: {
    type: 'hbox',
    defaultMargins: {top: 0, right: 4, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this,
        items = [],
        logo = [];

    logo.push({ xtype: 'label', text: NX.I18n.get('Header_Panel_Logo_Text'), cls: 'nx-header-productname' });

    // HACK remove this
    var logoOnly = false;

    if (!logoOnly) {
      logo.push(
          {
            xtype: 'label',
            text: NX.State.getEdition() + ' ' + NX.State.getVersion(),
            cls: 'nx-header-productversion',
            style: {
              'padding-left': '8px'
            }
          }
      )
    }

    items.push({ xtype: 'nx-header-logo' });
    items.push({ xtype: 'container', items: logo });

    if (!logoOnly) {
      items.push(
          ' ', ' ', // 2x pad
          {
            xtype: 'nx-header-mode',
            items: {
              xtype: 'button',
              ui: 'nx-header',
              cls: 'nx-modebutton',
              height: 39,
              width: 39,
              pressed: true,
              toggleGroup: 'examplemode',
              allowDepress: false,
              title: 'Browse',
              tooltip: NX.I18n.get('Header_BrowseMode_Tooltip'),
              glyph: 'xf1b2@FontAwesome' /* fa-cube */
            }
          },
          {
            xtype: 'nx-header-mode',
            items: {
              xtype: 'button',
              ui: 'nx-header',
              cls: 'nx-modebutton',
              height: 39,
              width: 39,
              toggleGroup: 'examplemode',
              allowDepress: false,
              title: 'Administration',
              tooltip: NX.I18n.get('Header_AdminMode_Tooltip'),
              glyph: 'xf013@FontAwesome' /* fa-gear */
            }
          },
          ' ',
          {
            xtype: 'nx-searchbox',
            cls: 'nx-quicksearch',
            width: 200,
            emptyText: NX.I18n.get('Header_QuickSearch_Empty'),
            inputAttrTpl: "data-qtip='" + NX.I18n.get('Header_QuickSearch_Tooltip') + "'" // field tooltip
          },
          '->',
          //{
          //  xtype: 'button',
          //  ui: 'nx-header',
          //  glyph: 'xf0f3@FontAwesome',
          //  tooltip: 'Toggle messages display'
          //},
          {
            xtype: 'button',
            ui: 'nx-header',
            tooltip: NX.I18n.get('Header_Refresh_Tooltip'),
            glyph: 'xf021@FontAwesome' // fa-refresh
          },
          {
            xtype: 'button',
            ui: 'nx-header',
            tooltip: NX.I18n.get('Header_Help_Tooltip'),
            glyph: 'xf059@FontAwesome', // fa-question-circle
            arrowCls: '', // hide the menu button arrow
            menu: [
              {
                text: 'Menu item 1'
              },
              '-',
              {
                text: 'Menu item 2'
              },
              {
                text: 'Menu item 3'
              }
            ]
          },
          {
            xtype: 'nx-header-mode',
            items: {
              xtype: 'button',
              ui: 'nx-header',
              cls: 'nx-modebutton',
              height: 39,
              toggleGroup: 'examplemode',
              allowDepress: false,
              title: 'User',
              text: 'admin',
              tooltip: NX.I18n.get('User_Tooltip'),
              glyph: 'xf007@FontAwesome'
            }
          },
          {
            xtype: 'button',
            ui: 'nx-header',
            text: NX.I18n.get('Header_SignIn_Text'),
            tooltip: NX.I18n.get('Header_SignIn_Tooltip'),
            glyph: 'xf090@FontAwesome'
          },
          {
            xtype: 'button',
            ui: 'nx-header',
            text: NX.I18n.get('Header_SignOut_Text'),
            tooltip: NX.I18n.get('Header_SignOut_Text'),
            hidden: true,
            glyph: 'xf08b@FontAwesome'
          }
      );
    }

    me.items = [
      {
        xtype: 'toolbar',

        // set height to ensure we have uniform size and not depend on what is in the toolbar
        height: 40,

        style: {
          backgroundColor: '#000000'
        },
        anchor: '100%',
        padding: "0 0 0 16px",

        defaults: {
          scale: 'medium'
        },

        items: items
      }
    ];

    me.callParent();
  }
});
