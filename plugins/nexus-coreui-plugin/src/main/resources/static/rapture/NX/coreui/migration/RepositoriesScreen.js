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
    'NX.coreui.migration.SupportedSelectionModel',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title'),

      description: NX.I18n.render(me, 'Description'),

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
            header: NX.I18n.render(me, 'Repository_Column'),
            dataIndex: 'name',
            flex: 1
          },
          {
            header: NX.I18n.render(me, 'Type_Column'),
            dataIndex: 'type',
            width: 70
          },
          {
            header: NX.I18n.render(me, 'Format_Column'),
            dataIndex: 'format'
          },
          {
            header: NX.I18n.render(me, 'Supported_Column'),
            dataIndex: 'supported',
            width: 90
          },
          {
            header: NX.I18n.render(me, 'Status_Column'),
            dataIndex: 'status',
            flex: 1,
            renderer: function (value, meta) {
              var truncated = Ext.String.ellipsis(value, 40, false);
              if (truncated !== value) {
                meta.tdAttr = Ext.String.format('data-qtip="{0}"', Ext.String.htmlEncode(value));
              }
              return truncated;
            }
          },
          {
            header: NX.I18n.render(me, 'Blobstore_Column'),
            dataIndex: 'blobStore'
          },
          {
            header: NX.I18n.render(me, 'Method_Column'),
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
                tooltip: NX.I18n.render(me, 'Action_Tooltip'),
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

    me.down('grid').settingsForm = true;

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
  },

  /**
   * Returns the state of the screen form
   *
   * @return {boolean}
   */
  isDirty: function() {
    return this.getGrid().isDirty();
  }
});
