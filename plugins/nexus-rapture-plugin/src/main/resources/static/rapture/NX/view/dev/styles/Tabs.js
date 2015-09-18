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
 * Tab styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Tabs', {
  extend: 'NX.view.dev.styles.StyleSection',

  title: 'Tabs',
  layout: {
    type: 'vbox',
    defaultMargins: {top: 4, right: 0, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    function tabStyle(name) {
      var proto = {
        xtype: 'tabpanel',
        width: 400,
        height: 80,
        activeTab: 0,
        ui: name,
        items: [
          { title: 'Settings', items: { xtype: 'panel', html: 'A simple tab', ui: 'nx-inset' } },
          { title: 'Routing', items: { xtype: 'panel', html: 'Another one', ui: 'nx-inset' } },
          { title: 'Smart Proxy', items: { xtype: 'panel', html: 'Yet another', ui: 'nx-inset' } },
          { title: 'Health Check', items: { xtype: 'panel', html: 'And one more', ui: 'nx-inset' } }
        ]
      };

      return {
        xtype: 'container',
        layout: {
          type: 'vbox',
          defaultMargins: {top: 4, right: 0, bottom: 0, left: 0}
        },

        items: [
          me.label('ui: ' + name),
          Ext.clone(proto),
          me.label('ui: ' + name + '; plain: true'),
          Ext.apply(proto, { plain: true })
        ]
      }
    }

    me.items = [
      tabStyle('default'),
      tabStyle('nx-light')
    ];

    me.callParent();
  }
});