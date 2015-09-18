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
 * Welcome dashboard.
 *
 * @since 3.0
 */
Ext.define('NX.view.dashboard.Welcome', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-dashboard-welcome',
  requires: [
    'NX.util.Url'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },

  style: {
    "background-color": "white"
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      //NOTE: Using custom welcome icon for milestones
      //{
      //  xtype: 'image',
      //  cls: 'nx-icon-nexus-x100',
      //  style: {
      //    margin: '100px 0 0 0'
      //  }
      //},
      {
        xtype: 'image',
        src: NX.util.Url.urlOf('/static/rapture/resources/images/nexus-milestone.png'), // 160x130
        height: 130,
        width: 160,
        autoEl: 'div',
        style: {
          'text-align': 'center',
          'vertical-align': 'middle',
          margin: '100px 0 0 0'
        }
      }
    ];

    me.callParent();
  }
});