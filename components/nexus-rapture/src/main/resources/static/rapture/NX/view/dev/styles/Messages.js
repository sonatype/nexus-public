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
 * Message notification styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Messages', {
  extend: 'NX.view.dev.styles.StyleSection',
  requires: [
    'NX.Icons'
  ],

  title: 'Messages',
  layout: {
    type: 'hbox',
    defaultMargins: {top: 0, right: 4, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    function message(type) {
      var style = 'nx-message-' + type;
      var icon = NX.Icons.cls('message-' + type, 'x16');
      return {
        xtype: 'window',
        ui: style,
        iconCls: icon,
        title: type,
        html: "ui: '" + style + "'",
        hidden: false,
        collapsible: false,
        floating: false,
        closable: false,
        draggable: false,
        resizable: false,
        width: 200
      };
    }

    me.items = [
      message('default'),
      message('primary'),
      message('danger'),
      message('warning'),
      message('success')
    ];

    me.callParent();
  }
});