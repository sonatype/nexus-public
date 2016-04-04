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
 * Toolbar styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Toolbars', {
  extend: 'NX.view.dev.styles.StyleSection',

  title: 'Toolbars',

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    function toolbar(scale) {
      var obj = {
        xtype: 'toolbar',
        items: [
          'text',
          {
            xtype: 'button',
            text: 'plain'
          },
          {
            xtype: 'button',
            text: 'with glyph',
            glyph: 'xf1b2@FontAwesome'
          },
          {
            xtype: 'button',
            text: 'with icon',
            iconCls: 'nx-icon-help-kb-x16'
          },
          ' ', // spacer
          {
            xtype: 'button',
            text: 'button menu',
            menu: [
              { text: 'plain' },
              { text: 'with glyph', glyph: 'xf059@FontAwesome' },
              { text: 'with icon', iconCls: 'nx-icon-help-kb-x16'}
            ]
          },
          '-', // sep
          {
            xtype: 'splitbutton',
            text: 'split button',
            menu: Ext.widget('menu', {
              items: [
                {text: 'Item 1'},
                {text: 'Item 2'}
              ]
            })
          },
          {
            xtype: 'button',
            enableToggle: true,
            pressed: true,
            text: 'toggle button'
          },
          '->', // spring
          {
            xtype: 'nx-searchbox',
            width: 200
          }
        ]
      };

      if (scale) {
        Ext.apply(obj, {
          defaults: {
            scale: scale
          }
        });
      }

      return obj;
    }

    me.items = [
      me.label('default'),
      toolbar(undefined),
      me.label('scale: medium'),
      toolbar('medium'),
      me.label('scale: large'),
      toolbar('large')
    ];

    me.callParent();
  }
});