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
 * Developer Conditions grid.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.Conditions', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-dev-conditions',

  title: 'Conditions',
  store: 'NX.store.dev.Condition',
  emptyText: 'No condition',
  viewConfig: {
    deferEmptyText: false
  },

  columns: [
    { text: 'id', dataIndex: 'id', flex: 1 },
    { text: 'condition', dataIndex: 'condition', flex: 3 },
    {
      xtype: 'nx-iconcolumn',
      text: 'satisfied',
      dataIndex: 'satisfied',
      width: 80,
      align: 'center',
      iconVariant: 'x16',
      iconName: function (value) {
        return value ? 'permission-granted' : 'permission-denied';
      }
    }
  ],

  plugins: [
    'gridfilterbox'
  ],

  tbar : [
    { xtype: 'checkbox', itemId: 'showSatisfied', boxLabel: 'Show Satisfied', value: true },
    { xtype: 'checkbox', itemId: 'showUnsatisfied', boxLabel: 'Show Unsatisfied', value: true }
  ]

});