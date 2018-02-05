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
 * List of all known icons.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.Icons', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-dev-icons',

  title: 'Icons',
  store: 'Icon',
  emptyText: 'No icons',

  viewConfig: {
    deferEmptyText: false
  },

  columns: [
    { text: 'cls', dataIndex: 'cls', width: 200 },
    { text: 'name', dataIndex: 'name' },
    { text: 'file', dataIndex: 'file' },
    { text: 'variant', dataIndex: 'variant', width: 50 },
    { text: 'size', xtype: 'templatecolumn', tpl: '{height}x{width}', width: 80 },
    { text: 'url', xtype: 'templatecolumn', tpl: '<a href="{url}" target="_blank">{url}</a>', flex: 1 },
    { text: 'img src', xtype: 'templatecolumn', tpl: '<img src="{url}"/>' },
    {
      xtype: 'nx-iconcolumn',
      text: 'img class',
      dataIndex: 'cls',
      iconCls: function(value) {
        return value;
      }
    }
  ],

  plugins: [
    { ptype: 'rowediting', clicksToEdit: 1 },
    'gridfilterbox'
  ]

});