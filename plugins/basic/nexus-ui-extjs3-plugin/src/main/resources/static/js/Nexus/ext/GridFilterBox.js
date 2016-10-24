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
/*global NX, Ext, Nexus*/

/**
 * A grid filter box.
 *
 * @since 2.7
 */
NX.define('Nexus.ext.GridFilterBox', {
  extend: 'Nexus.ext.StoreFilterBox',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  xtype: 'nx-grid-filter-box',

  /**
   * @cfg {Ext.grid.GridPanel} grid that should filtered
   */
  filteredGrid: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var self = this;

    Ext.apply(self, {
      filteredStore: self.findGridStore(self.filteredGrid),
      filteredFields: self.findGridColumns(self.filteredGrid)
    });

    Nexus.ext.GridFilterBox.superclass.initComponent.call(self, arguments);

    self.on('render', function () {
      self.logDebug('Binding to grid ' + self.filteredGrid);
      self.filteredGrid.on('reconfigure', self.onGridReconfigured, self);
    });

    self.on('destroy', function () {
      self.logDebug('Unbinding from grid ' + self.filteredGrid);
      self.filteredGrid.removeListener('reconfigure', self.onGridReconfigured, self);
    });
    self.on('beforeFiltering', function () {
      if (self.filteredGrid.view.emptyTextBackup) {
        self.filteredGrid.view.emptyText = self.filteredGrid.view.emptyTextBackup;
      }
      if (self.filteredGrid.view.emptyTextWhileFiltering && self.filteredStore.getCount() > 0) {
        if (!self.filteredGrid.view.emptyTextBackup) {
          self.filteredGrid.view.emptyTextBackup = self.filteredGrid.view.emptyText;
        }
        self.filteredGrid.view.emptyText = self.filteredGrid.view.emptyTextWhileFiltering.replaceAll(
            '{criteria}', Ext.util.Format.htmlEncode(self.getSearchValue())
        );
      }
    });
    self.on('searchcleared', function () {
      if (self.filteredGrid.view.emptyTextBackup) {
        self.filteredGrid.view.emptyText = self.filteredGrid.view.emptyTextBackup;
      }
    });
  },

  /**
   * @private
   * Handles reconfiguration of grid.
   * @param grid that was reconfigured
   * @param store new store
   * @param columnModel new column model
   */
  onGridReconfigured: function (grid, store, columnModel) {
    var self = this;
    self.logDebug('Grid ' + self.filteredGrid + ' reconfigured, binding to new store');
    self.reconfigureStore(store, self.extractColumnsWithDataIndexFromModel(columnModel));
  },

  /**
   * @private
   * Finds the data store of a grid.
   * @param grid to find store of
   * @returns {Ext.data.Store} store or undefined if store could not be found
   */
  findGridStore: function (grid) {
    return grid.getStore() || grid.gridStore || grid.store || grid.ds;
  },

  /**
   * @private
   * Find the columns of a grid.
   * @param grid to find columns of
   * @returns {Array} columns or undefined if columns could not be determined
   */
  findGridColumns: function (grid) {
    var self = this,
        colModel = grid.getColumnModel() || grid.cm || grid.colModel,
        columns;

    if (colModel) {
      columns = self.extractColumnsWithDataIndexFromModel(colModel);
    }
    if (!columns) {
      columns = self.extractColumnsWithDataIndex(grid.columns);
    }
    return columns;
  },

  /**
   * @private
   * Returns the dataIndex property of all grid columns.
   * @returns {Array} of fields names
   */
  extractColumnsWithDataIndexFromModel: function (columnModel) {
    var self = this,
        columns;

    if (columnModel) {
      columns = columnModel.getColumnsBy(function () {
        return true;
      });
      return self.extractColumnsWithDataIndex(columns);
    }
  },

  /**
   * @private
   * Returns the dataIndex property of all grid columns.
   * @returns {Array} of fields names
   */
  extractColumnsWithDataIndex: function (columns) {
    var filterFieldNames = [];

    if (columns) {
      Ext.each(columns, function (column) {
        if (column.dataIndex) {
          filterFieldNames.push(column.dataIndex);
        }
      });
    }

    if (filterFieldNames.length > 0) {
      return filterFieldNames;
    }
  }

});
