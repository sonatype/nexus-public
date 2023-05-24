/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Search results grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.search.SearchResultList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-search-result-list',
  requires: [
    'NX.I18n'
  ],

  allowClearSort: true,

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      // Mark grid as health check columns target
      healthCheckColumnsTarget: true,
      // Mark grid as a component list
      componentList: true,

      store: 'SearchResult',

      // Prevent the store from automatically loading
      loadStore: Ext.emptyFn,

      viewConfig: {
        emptyText: NX.I18n.get('Search_SearchResultList_EmptyText'),
        deferEmptyText: false
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          width: 36,
          iconVariant: 'x16',
          iconName: function () {
            return 'search-component';
          }
        },
        {
          text: NX.I18n.get('Search_SearchResultList_Name_Header'),
          dataIndex: 'name',
          stateId: 'name',
          flex: 3,
          renderer: Ext.htmlEncode
        },
        {
          header: NX.I18n.get('Search_SearchResultList_Group_Header'),
          dataIndex: 'group',
          stateId: 'group',
          flex: 4,
          renderer: NX.ext.grid.column.Renderers.optionalData
        },
        {
          header: NX.I18n.get('Search_SearchResultList_Last_Updated_Header'),
          dataIndex: 'lastBlobUpdated',
          stateId: 'lastBlobUpdated',
          flex: 2,
          renderer: NX.ext.grid.column.Renderers.formattedDate
        },
        {
          header: NX.I18n.get('Search_SearchResultList_Version_Header'),
          dataIndex: 'version',
          stateId: 'version',
          flex: 1,
          renderer: NX.ext.grid.column.Renderers.optionalData
        },
        {
          header: NX.I18n.get('Search_SearchResultList_Format_Header'),
          dataIndex: 'format',
          stateId: 'format',
          width: 100,
          renderer: Ext.htmlEncode
        },
        {
          header: NX.I18n.get('Search_SearchResultList_Repository_Header'),
          dataIndex: 'repositoryName',
          stateId: 'repositoryName',
          flex: 2,
          renderer: Ext.htmlEncode
        }
      ],

      // Show all results up to the page size
      trailingBufferZone: 300,
      leadingBufferZone: 300
    });

    this.callParent();
  }

});
