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
 * A grid plugins that adds filtering capabilities.
 *
 * @since 3.0
 */
Ext.define('NX.ext.grid.plugin.Filtering', {
  extend: 'Ext.AbstractPlugin',
  alias: 'plugin.gridfiltering',
  requires: [
    'Ext.util.Filter'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * @private {String} current filtering value
   */
  filterValue: undefined,

  /**
   * @private {Ext.data.Store} store that should filtered
   */
  filteredStore: undefined,

  /**
   * @private {Array} array of field ids that should be used for filtering
   */
  filteredFields: undefined,

  /**
   * @cfg {Function} to be used for filtering (defaults to String contains)
   */
  filterFn: function (valueToBeMatched, filterValue) {
    var stringValue;
    if (valueToBeMatched) {
      stringValue = valueToBeMatched.toString();
      if (stringValue) {
        return stringValue.toLowerCase().indexOf(filterValue.toLowerCase()) !== -1;
      }
    }
    return false;
  },

  /**
   * Returns true if the field value is defined and matches the filtering function.
   *
   * @protected
   * @param filterValue to match
   * @param record record that was used to extract the value to be matched
   * @param fieldName filter field name that was used to extract the value to be matched
   * @param fieldValue to me matched
   */
  matches: function (filterValue, record, fieldName, fieldValue) {
    return this.filterFn(fieldValue, filterValue);
  },

  /**
   * Filters on specified value.
   *
   * @public
   * @param value to filter upon
   */
  filter: function (value) {
    var me = this;

    me.filterValue = value;
    me.applyFilter();
  },

  /**
   * Clears filter.
   *
   * @public
   */
  clearFilter: function () {
    var me = this;

    if (me.filterValue) {
      me.filterValue = undefined;
      me.applyFilter();
    }
  },

  /**
   * Filters on current filter value.
   *
   * @public
   */
  applyFilter: function () {
    var me = this,
        remoteFilter = me.filteredStore.remoteFilter,
        filters = me.filteredStore.filters.items;

    // HACK: when remote filter is on store will not be locally filtered, so we have to trick ExtJS into doing local
    // filtering by setting remoteFilter to false and temporary remove the other filters (will add them back after local
    // filtering is performed)
    if (remoteFilter) {
      me.filteredStore.filters.clear();
      me.filteredStore.remoteFilter = false;
    }
    if (me.filterValue) {
      //<if debug>
      me.logTrace('Filtering', me.filteredStore.self.getName(), 'on [', me.filterValue, '] using fields:', me.filteredFields);
      //</if>

      me.filteredStore.filter(me.filteringFilter);
    }
    else {
      me.filteredStore.removeFilter(me.filteringFilter);

      //<if debug>
      me.logTrace('Filtering cleared on', me.filteredStore.self.getName());
      //</if>
    }
    if (remoteFilter) {
      me.filteredStore.remoteFilter = remoteFilter;
      if (filters) {
        me.filteredStore.filters.add(filters);
      }
    }
  },

  /**
   * Clear filter value if store is filtered from outside.
   *
   * @private
   */
  syncFilterValue: function (store, filters) {
    var me = this,
        filteringFilterRemoved = true;

    if (filters) {
      Ext.Array.each(filters, function (filter) {
        if (filter.id === me.filteringFilter.id) {
          filteringFilterRemoved = false;
          return false;
        }
        return true;
      });
    }

    if (me.filterValue && filteringFilterRemoved) {
      me.clearFilter();
      me.grid.fireEvent('filteringautocleared');
    }
  },

  /**
   * Bind plugin to grid.
   *
   * @private
   * @param grid to bind to
   */
  init: function (grid) {
    var me = this;

    me.grid = grid;
    grid.filterable = true;
    grid.filter = Ext.Function.bind(me.filter, me);
    grid.clearFilter = Ext.Function.bind(me.clearFilter, me);

    me.filteringFilter = Ext.create('Ext.util.Filter', {
      id: 'filteringPlugin',
      filterFn: function (record) {
        for (var i = 0; i < me.filteredFields.length; i++) {
          var filteredField = me.filteredFields[i];
          if (filteredField) {
            if (me.matches(me.filterValue, record, filteredField, record.data[filteredField])) {
              return true;
            }
          }
        }
        return false;
      }
    });

    grid.mon(grid, {
      reconfigure: me.onReconfigure,
      scope: me,
      beforerender: {
        fn: me.onBeforeRender,
        single: true,
        scope: me
      }
    });
  },

  /**
   * Handles configuration of grid.
   *
   * @private
   * @param grid that was reconfigure
   */
  onBeforeRender: function (grid) {
    this.onReconfigure(grid, grid.getStore(), grid.columns);
  },

  /**
   * Handles reconfiguration of grid.
   *
   * @private
   * @param grid that was reconfigured
   * @param store new store
   * @param columns new columns
   */
  onReconfigure: function (grid, store, columns) {
    var me = this,
        store = store || me.grid.getStore();

    //<if debug>
    me.logTrace('Grid', grid.id, 'reconfigured; binding to new store');
    //</if>

    me.reconfigureStore(store, me.extractColumnsWithDataIndex(columns));
  },

  /**
   * Unbinds from current store and register itself to provided store.
   *
   * @private
   * @param store to register itself to
   * @param filteredFields fields to be used while filtering
   */
  reconfigureStore: function (store, filteredFields) {
    var me = this;
    if (me.filteredStore !== store) {
      me.unbindFromStore(me.filteredStore);
      me.bindToStore(store);
    }
    me.filteredFields = filteredFields;
    me.applyFilter();
  },

  /**
   * Register itself as listener of load events on provided store.
   *
   * @private
   * @param store to register itself to
   */
  bindToStore: function (store) {
    var me = this;
    me.filteredStore = store;
    if (store) {
      //<if debug>
      me.logTrace('Binding to store', me.filteredStore.self.getName());
      //</if>

      me.grid.mon(store, 'load', me.applyFilter, me);
      me.grid.mon(store, 'filterchange', me.syncFilterValue, me);
    }
  },

  /**
   * Remove itself as listener from provided store.
   *
   * @private
   * @param store to remove itself from
   */
  unbindFromStore: function (store) {
    var me = this;
    if (store) {
      //<if debug>
      me.logTrace('Unbinding from store', me.filteredStore.self.getName());
      //</if>

      me.grid.mun(store, 'load', me.applyFilter, me);
      me.grid.mun(store, 'filterchange', me.syncFilterValue, me);
    }
  },

  /**
   * Returns the dataIndex property of all grid columns.
   *
   * @private
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
