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
 * Browse assets grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.browse.BrowseAssetList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-browse-asset-list',
  requires: [
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-browse-asset-list',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;
    var store = me.store || 'Asset';
    Ext.apply(me, {
      store: store,

      // Marker for source of targets to be shown in container assets
      assetContainerSource: true,

      // Prevent the store from automatically loading
      loadStore: Ext.emptyFn,

      selModel: {
        pruneRemoved: false
      },

      viewConfig: {
        emptyText: NX.I18n.get('Browse_BrowseAssetList_EmptyText_View'),
        emptyTextFilter: NX.I18n.get('Browse_BrowseAssetList_EmptyText_Filter'),
        deferEmptyText: false
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          dataIndex: 'contentType',
          width: 36,
          iconVariant: 'x16',
          iconNamePrefix: 'asset-type-',
          iconName: function(value) {
            var assetType;

            if (value) {
              assetType = value.replace('/', '-');
              if (NX.getApplication().getIconController().findIcon('asset-type-' + assetType, 'x16')) {
                return assetType;
              }
            }
            return 'default';
          }
        },
        {
          text: NX.I18n.get('Browse_BrowseAssetList_Name_Column'),
          dataIndex: 'name',
          stateId: 'name',
          flex: 1
        }
      ],

      tbar: {
        xtype: 'nx-actions',
        items: [
          '->',
          {
            xtype: 'nx-searchbox',
            itemId: 'filter',
            emptyText: NX.I18n.get('Grid_Plugin_FilterBox_Empty'),
            width: 200
          }
        ]
      },

      plugins: {
        ptype: 'bufferedrenderer',
        trailingBufferZone: 20,
        leadingBufferZone: 50
      }
    });

    me.callParent();
  }

});
