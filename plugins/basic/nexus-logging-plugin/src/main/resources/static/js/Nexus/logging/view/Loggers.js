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
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Loggers grid.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.view.Loggers', {
  extend: 'Ext.grid.EditorGridPanel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.logging.Icons',
    'Nexus.logging.store.Logger',
    'Nexus.logging.view.LoggerLevel'
  ],

  xtype: 'nx-logging-view-loggers',

  title: 'Loggers',

  stripeRows: true,
  border: false,
  autoScroll: true,
  clicksToEdit: 1,

  viewConfig: {
    emptyText: 'No loggers defined.',
    emptyTextWhileFiltering: 'No loggers matched criteria: {criteria}',
    deferEmptyText: false
  },

  loadMask: {
    msg: 'Loading...',
    msgCls: 'loading-indicator'
  },

  autoExpandColumn: 'name',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.logging.Icons,
        sp = Sonatype.lib.Permissions;

    Ext.apply(me, {
      store: NX.create('Nexus.logging.store.Logger'),

      columns: [
        {
          width: 30,
          resizable: false,
          sortable: false,
          fixed: true,
          hideable: false,
          menuDisabled: true,
          renderer: function (value, metaData, record) {
            return icons.get('logger').img;
          }
        },
        {
          id: 'name',
          header: 'Name',
          dataIndex: 'name',
          sortable: true
        },
        {
          id: 'level',
          header: 'Level',
          dataIndex: 'level',
          sortable: true,
          width: 80,
          tooltip: 'Double click to edit',
          editor: {
            xtype: 'nx-logging-combo-logger-level',
            listeners: {
              select: function () {
                // Automatically save when logger level is selected to avoid need for user to press enter or click away
                // in order for value to be saved
                me.stopEditing(false);
              }
            }
          }
        }
      ],

      tbar: [
        {
          id: 'nx-logging-button-refresh-loggers',
          text: 'Refresh',
          tooltip: 'Refresh loggers',
          iconCls: icons.get('loggers_refresh').cls
        },
        {
          id: 'nx-logging-button-add-logger',
          text: 'Add',
          tooltip: 'Add new logger',
          iconCls: icons.get('loggers_add').cls,
          disabled: !sp.checkPermission('nexus:logconfig', sp.EDIT)
        },
        {
          id: 'nx-logging-button-remove-loggers',
          text: 'Remove',
          tooltip: 'Remove selected logger',
          iconCls: icons.get('loggers_remove').cls,
          disabled: true
        },
        '-',
        {
          id: 'nx-logging-button-reset-loggers',
          text: 'Reset',
          tooltip: 'Reset loggers to their default levels',
          iconCls: icons.get('loggers_reset').cls,
          disabled: !sp.checkPermission('nexus:logconfig', sp.EDIT)
        },
        '->',
        {
          xtype: 'nx-grid-filter-box',
          filteredGrid: me
        }
      ]
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  }
});