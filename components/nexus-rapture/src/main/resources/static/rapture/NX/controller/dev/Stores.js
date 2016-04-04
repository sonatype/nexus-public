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
 * Stores developer panel controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.dev.Stores', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.data.StoreManager'
  ],

  refs: [
    {
      ref: 'stores',
      selector: 'nx-dev-stores'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'nx-dev-stores combobox': {
          change: me.onStoreSelected
        },
        'nx-dev-stores button[action=load]': {
          click: me.loadStore
        },
        'nx-dev-stores button[action=clear]': {
          click: me.clearStore
        }
      }
    });
  },

  /**
   * @private
   */
  onStoreSelected: function (combobox) {
    var storeId = combobox.getValue(),
        panel = this.getStores(),
        grid = panel.down('grid'),
        store, columns = [];

    if (storeId) {
      store = Ext.data.StoreManager.get(storeId);
      if (store) {
        Ext.each(store.model.getFields(), function (field) {
          columns.push({
            text: field.name,
            dataIndex: field.name,
            renderer: function(value) {
              if (Ext.isObject(value) || Ext.isArray(value)) {
                try {
                  return Ext.encode(value);
                }
                catch (e) {
                  console.error('Failed to encode value:', value, e);
                }
              }
              return value;
            }
          });
        });
        panel.removeAll(true);
        panel.add({
          xtype: 'grid',
          autoScroll: true,
          store: store,
          columns: columns
        });
      }
    }
  },

  /**
   * @private
   */
  loadStore: function () {
    var panel = this.getStores(),
        grid = panel.down('grid');

    if (grid) {
      grid.getStore().load();
    }
  },

  /**
   * @private
   */
  clearStore: function () {
    var panel = this.getStores(),
        grid = panel.down('grid');

    if (grid) {
      grid.getStore().removeAll();
    }
  }

});
