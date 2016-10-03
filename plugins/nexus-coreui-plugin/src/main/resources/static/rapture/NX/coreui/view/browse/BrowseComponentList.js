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
 * Browse components grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.browse.BrowseComponentList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-browse-component-list',
  requires: [
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-browse-component-list',

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      // Mark grid as health check columns target
      healthCheckColumnsTarget: true,
      // Mark grid as a component list
      componentList: true,

      store: 'Component',

      // Prevent the store from automatically loading
      loadStore: Ext.emptyFn,

      selModel: {
        pruneRemoved: false
      },

      viewConfig: {
        emptyText: NX.I18n.get('Browse_BrowseComponentList_EmptyText_View'),
        emptyTextFilter: NX.I18n.get('Browse_BrowseComponentList_EmptyText_Filter'),
        deferEmptyText: false
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          dataIndex: 'id',
          width: 36,
          iconVariant: 'x16',
          iconName: function() {
            return 'browse-component';
          }
        },
        {
          text: NX.I18n.get('Browse_BrowseComponentList_Name_Column'),
          dataIndex: 'name',
          stateId: 'name',
          flex: 3
        },
        {
          text: NX.I18n.get('Browse_BrowseComponentList_Group_Column'),
          dataIndex: 'group',
          stateId: 'group',
          flex: 4,
          renderer: NX.ext.grid.column.Renderers.optionalData
        },
        {
          text: NX.I18n.get('Browse_BrowseComponentList_Version_Column'),
          dataIndex: 'version',
          stateId: 'version',
          flex: 1,
          renderer: NX.ext.grid.column.Renderers.optionalData
        }
      ],

      plugins: [
        {ptype: 'remotegridfilterbox'}
      ]
    });

    this.callParent();
  }

});
