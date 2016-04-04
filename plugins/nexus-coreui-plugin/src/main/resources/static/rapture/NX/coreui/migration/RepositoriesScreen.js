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
 * Migration repository selection screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.RepositoriesScreen', {
  extend: 'NX.wizard.GridScreen',
  alias: 'widget.nx-coreui-migration-repositories',
  requires: [
    'NX.Icons',
    'NX.ext.grid.column.Action',
    'NX.coreui.migration.SupportedSelectionModel'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: 'Repositories',

      description: '<p>Select the repositories to be migrated.<br/>' +
      'Customize advanced configuration of the migration per-repository as needed.</p>',

      grid: {
        xtype: 'grid',

        viewConfig: {
          stripeRows: true,
          getRowClass: function (record) {
            if (!record.get('supported')) {
              return 'nx-disabled-row';
            }
          }
        },

        columns: [
          {
            header: 'Repository',
            dataIndex: 'repository',
            flex: 1
          },
          {
            header: 'Type',
            dataIndex: 'type',
            width: 70
          },
          {
            header: 'Format',
            dataIndex: 'format'
          },
          {
            header: 'Supported',
            dataIndex: 'supported',
            width: 90
          },
          {
            header: 'Status',
            dataIndex: 'status',
            flex: 1
          },
          {
            header: 'Destination',
            dataIndex: 'blobStore'
          },
          {
            header: 'Method',
            dataIndex: 'ingestMethod',
            width: 80,
            renderer: function (value) {
              // TODO: i18n
              return value.toLowerCase();
            }
          },
          {
            xtype: 'nx-actioncolumn',
            width: 32,
            menuDisabled: true,
            items: [
              {
                action: 'customize',
                iconCls: NX.Icons.cls('migration-customize', 'x16'),
                tooltip: 'Customize repository options',
                isDisabled: function(view, ri, ci, item, record) {
                  return !record.get('supported');
                }
              }
            ]
          }
        ],

        selModel: Ext.create('NX.coreui.migration.SupportedSelectionModel'),

        store: 'NX.coreui.migration.RepositoryStore'
      },

      fields: [
        {
          // hidden field to provide form validity support for grid
          xtype: 'hidden',
          isValid: function() {
            // valid if we have at least 1 selection
            return me.getSelectionModel().getCount() !== 0;
          }
        }
      ],

      buttons: ['back', 'next', 'cancel']
    });

    me.callParent();

    // update hidden field to when grid selection changes
    me.getSelectionModel().on('selectionchange', function(selModel, selected, opts) {
      me.down('hidden').setValue(selected.length);
    });
  },

  /**
   * @return {Ext.grid.Panel}
   */
  getGrid: function() {
    return this.down('grid');
  },

  /**
   * @return {Ext.selection.Model}
   */
  getSelectionModel: function() {
    return this.getGrid().getSelectionModel();
  }
});
