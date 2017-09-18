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
 * Component/Asset tree panel
 *
 * @since 3.6
 */
Ext.define('NX.coreui.view.browse.ComponentAssetTree', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-component-asset-tree',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;
    var store = me.store || 'ComponentAssetTree';
    Ext.apply(me, {
      layout: {
        type: 'hbox',
        align: 'stretch'
      },
      items: [{
        xtype: 'treepanel',
        flex: 3,
        store: store,
        rootVisible: false,
        // Prevent the store from automatically loading
        loadStore: Ext.emptyFn,

        viewConfig: {
          emptyText: NX.I18n.get('Component_Asset_Tree_EmptyText_View'),
          deferEmptyText: false
        }
      }],
      dockedItems: [{
        xtype: 'nx-actions',
        dock: 'top',
        items: [{
            xtype: 'label',
            itemId: 'nx-coreui-component-asset-tree-html-view',
            html: NX.util.Url.asLink("", NX.I18n.get('Component_Asset_Tree_Html_View'), '_blank')
          },
          '->',
          {
            xtype: 'nx-searchbox',
            emptyText: NX.I18n.get('Grid_Plugin_FilterBox_Empty'),
            searchDelay: 200,
            width: 200
          },{
            xtype: 'label',
            itemId: 'nx-coreui-component-asset-tree-advanced-search',
            html: '<a href="#browse/search">Advanced search...</a>'
          }]
      }]
    });

    me.callParent();
  }

});
