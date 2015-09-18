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
 * List of all known features.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.Features', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-dev-features',

  title: 'Features',
  store: 'Feature',
  emptyText: 'No features',

  columns: [
    { text: 'mode', dataIndex: 'mode', editor: 'textfield' },
    { text: 'path', dataIndex: 'path', editor: 'textfield', flex: 1 },
    { text: 'bookmark', dataIndex: 'bookmark', editor: 'textfield', flex: 1 },
    { text: 'weight', dataIndex: 'weight', width: 80, editor: 'textfield' },
    { text: 'view', dataIndex: 'view', editor: 'textfield', hidden: true },
    { text: 'help keyword', dataIndex: 'helpKeyword', editor: 'textfield', flex: 1 },
    { text: 'description', dataIndex: 'description', editor: 'textfield', flex: 1 },
    { text: 'iconName', dataIndex: 'iconName', editor: 'textfield' },
    {
      xtype: 'nx-iconcolumn',
      dataIndex: 'iconName',
      width: 48,
      iconVariant: 'x16'
    },
    {
      xtype: 'nx-iconcolumn',
      dataIndex: 'iconName',
      width: 48,
      iconVariant: 'x32'
    }
  ],

  plugins: [
    { ptype: 'rowediting', clicksToEdit: 1 },
    'gridfilterbox'
  ],

  viewConfig: {
    deferEmptyText: false,
    markDirty: false
  }
});