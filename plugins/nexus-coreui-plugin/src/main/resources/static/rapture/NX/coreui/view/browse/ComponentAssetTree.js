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
 * Component/Asset tree panel
 *
 * @since 3.6
 */
Ext.define('NX.coreui.view.browse.ComponentAssetTree', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-component-asset-tree',
  requires: [
    'NX.I18n',
    'NX.State',
    'NX.coreui.view.component.ComponentInfo',
    'NX.coreui.view.component.ComponentAssetInfo',
    'NX.coreui.view.component.ComponentFolderInfo'
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
        ariaRole: 'grid',
        flex: 3,
        store: store,
        rootVisible: false,
        // Prevent the store from automatically loading
        loadStore: Ext.emptyFn,
        animate: Ext.enableFx && !!NX.State.getValue('animateDuration'),
        viewConfig: {
          emptyText: NX.I18n.get('Component_Asset_Tree_EmptyText_View'),
          deferEmptyText: false
        }
      },
      {
        xtype: 'nx-coreui-component-componentinfo',
        iconCls: 'nx-icon-tree-component-x16',
        flex: 2,
        visible: false
      },
      {
        xtype: 'nx-coreui-component-componentassetinfo',
        flex: 2,
        iconCls: 'nx-icon-tree-asset-x16',
        visible: false
      },
      {
        xtype: 'nx-coreui-component-componentfolderinfo',
        iconCls: 'nx-icon-tree-folder-x16',
        flex: 2,
        visible: false
      }],
      dockedItems: [{
        xtype: 'panel',
        items: [{
          xtype: 'panel',
          itemId: 'info',
          ui: 'nx-drilldown-message',
          cls: 'nx-drilldown-info',
          iconCls: NX.Icons.cls('drilldown-info', 'x16'),
          hidden: true
        },
        {
          xtype: 'panel',
          itemId: 'warning',
          ui: 'nx-drilldown-message',
          cls: 'nx-drilldown-warning',
          iconCls: NX.Icons.cls('drilldown-warning', 'x16'),
          hidden: true
        },
        {
          xtype: 'nx-actions',
          dock: 'top',
          items: [{
            xtype: 'button',
            text: NX.I18n.get('Component_Asset_Tree_Upload_Component'),
            iconCls: 'x-fa fa-upload',
            action: 'upload',
            hidden: true
          },
          {
            xtype: 'label',
            itemId: 'nx-coreui-component-asset-tree-html-view',
            html: NX.util.Url.asLink("", NX.I18n.get('Component_Asset_Tree_Html_View'), '_blank')
          },
          '->',
          {
            xtype: 'label',
            itemId: 'nx-coreui-component-asset-tree-advanced-search',
            html: '<a href="#browse/search">Advanced search...</a>'
          }]
        }]
      }]
    });

    me.callParent();
  }

});
