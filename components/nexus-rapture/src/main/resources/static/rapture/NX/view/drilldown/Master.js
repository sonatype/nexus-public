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
 * Master/Detail tabs.
 *
 * @since 3.0
 */
Ext.define('NX.view.drilldown.Master', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-drilldown-master',
  requires: [
    'NX.I18n'
  ],

  maskElement: 'body',

  cls: 'nx-drilldown-master',
  rowLines: false,

  /**
   * @private
   */
  initComponent: function() {
    var me = this,
        hasAffordance = me.columns.some(function(column) {
          return column.cls === 'nx-drilldown-affordance';
        });

    if (!hasAffordance) {
      me.columns.push({
        width: 28,
        hideable: false,
        sortable: false,
        menuDisabled: true,
        resizable: false,
        draggable: false,
        cls: 'nx-drilldown-affordance',

        defaultRenderer: function() {
          return Ext.DomHelper.markup({
            tag: 'span',
            cls: 'x-fa fa-angle-right'
          });
        }
      });
    }

    me.callParent();

    me.on('render', this.loadStore, this);
  },

  loadStore: function() {
    this.getStore().load();
  },

  pushColumn: function(newColumn) {
    var columns = this.getColumns(),
        hasAffordance = columns.some(function(column) {
          return column.cls === 'nx-drilldown-affordance';
        });

    return this.getHeaderContainer().insert(hasAffordance ? columns.length - 1 : columns.length, newColumn);
  }
});
