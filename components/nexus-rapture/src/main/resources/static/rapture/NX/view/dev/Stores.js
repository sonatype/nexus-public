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
Ext.define('NX.view.dev.Stores', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-dev-stores',
  requires: [
    'Ext.data.Store',
    'Ext.data.StoreManager'
  ],

  title: 'Stores',
  layout: 'fit',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      items: [
        {
          xtype: 'label',
          text: 'No store selected',
          padding: '10 10 10 10'
        }
      ],

      tbar: [
        {
          xtype: 'combo',
          name: 'storeId',
          width: 300,
          emptyText: 'select a store',
          queryMode: 'local',
          displayField: 'id',
          valueField: 'id',
          trigger2Cls: 'x-form-search-trigger',
          onTrigger2Click: function () {
            this.getStore().load();
          },
          store: Ext.create('Ext.data.Store', {
            fields: ['id'],
            data: Ext.data.StoreManager,
            proxy: {
              type: 'memory',
              reader: {
                type: 'json',
                read: function (data) {
                  var stores = [];

                  data.each(function (store) {
                    stores.push({
                      id: store.storeId
                    });
                  });

                  return this.readRecords(stores);
                }
              }
            },
            sorters: {property: 'id', direction: 'ASC'}
          })
        },
        {
          xtype: 'button',
          text: 'Load store',
          action: 'load',
          glyph: 'xf0ab@FontAwesome' /* fa-arrow-circle-down */
        },
        {
          xtype: 'button',
          text: 'Clear store',
          action: 'clear',
          glyph: 'xf12d@FontAwesome' /* fa-eraser */
        }
      ]
    });

    me.callParent();
  }

});
