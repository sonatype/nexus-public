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
 * Assets grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.AssetList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-component-asset-list',
  requires: [
    'NX.I18n'
  ],
  mixins: {
    componentUtils: 'NX.coreui.mixin.ComponentUtils'
  },
  /**
   * Currently shown component model.
   */
  componentModel: undefined,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    Ext.apply(me, {
      store: 'ComponentAsset',

      cls: 'nx-hr',

      // Marker for source of targets to be shown in container assets
      assetContainerSource: true,

      // Prevent the store from automatically loading
      loadStore: Ext.emptyFn,

      allowDeselect: true,

      viewConfig: {
        emptyText: 'No assets found',
        deferEmptyText: false
      },

      columns: [
        {
          xtype: 'nx-iconcolumn',
          dataIndex: 'name',
          width: 36,
          iconVariant: 'x16',
          iconName: function(value) {
            var icon = me.mixins.componentUtils.getIconForAssetName(value);
            if (icon) {
              var iconName = icon.get('name');
              if (iconName) {
                return iconName;
              }
            }
            return 'default';
          }
        },
        {
          text: NX.I18n.get('SearchResultAssetList_Name_Header'),
          dataIndex: 'name',
          flex: 2.5,
          renderer: Ext.htmlEncode
        }
      ]
    });

    me.callParent();
  },

  /**
   * @public
   *
   * Sets owning component.
   * @param {NX.coreui.model.Component} componentModel owning component
   */
  setComponentModel: function(componentModel) {
    var me = this;

    me.componentModel = componentModel;
    me.fireEvent('updated', me, me.componentModel);
  }

});
