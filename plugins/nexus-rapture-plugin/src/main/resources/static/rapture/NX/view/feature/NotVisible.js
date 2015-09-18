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
 * Panel shown in case a bookmarked feature cannot be shown (403 like).
 *
 * @since 3.0
 */
Ext.define('NX.view.feature.NotVisible', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-feature-notvisible',

  layout: {
    type: 'vbox',
    align: 'center',
    pack: 'center'
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {
        xtype: 'label',
        text: me.text,
        style: {
          'color': '#000000',
          'font-size': '20px',
          'font-weight': 'bold',
          'text-align': 'center',
          'padding': '20px'
        }
      }
    ];

    me.callParent(arguments);
  }

});