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
 * A store filter box.
 *
 * @since 2.7
 */
NX.define('Nexus.ext.StoreFilterBox', {
  extend: 'Nexus.ext.SearchBox',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  xtype: 'nx-store-filter-box',

  /**
   * @cfg {Ext.data.Store} store that should filtered
   */
  filteredStore: undefined,

  /**
   * @cfg {Array} array of field ids that should be used for filtering
   */
  filteredFields: undefined,

  /**
   * @cfg {Function} to be used for filtering (defaults to string contains)
   */
  filterFn: function (valueToBeMatched, filterValue) {
    var stringValue;
    if (valueToBeMatched) {
      stringValue = valueToBeMatched.toString();
      if (stringValue) {
        return stringValue.toLowerCase().indexOf(filterValue.toLowerCase()) != -1;
      }
    }
    return false;
  },

  /**
   * @override
   */
  initComponent: function () {
    var self = this;

    Nexus.ext.StoreFilterBox.superclass.initComponent.call(self, arguments);

    self.addEvents(

        /**
         * Fires before filtering the store.
         * @event beforeFiltering
         * @param {String} search value
         */
        'beforefiltering'
    );

    self.bindToStore(self.filteredStore);

    self.on('destroy', function () {
      self.unbindFromStore(self.filteredStore);
    });
    self.on('search', function () {
      self.filterStore();
    });
    self.on('searchcleared', function () {
      self.filterStore();
    });
  },

  /**
   * Filters the store on current search box value.
   */
  filterStore: function () {
    var self = this,
        filterValue = self.getSearchValue();

    self.filteredStore.clearFilter();
    if (filterValue) {
      self.logDebug(
          'Filtering ' + self.filteredStore + ' on [' + filterValue + '] using fields: ' + self.filteredFields
      );
      self.fireEvent('beforefiltering');
      self.filteredStore.filterBy(function (record) {
        for (var i = 0; i < self.filteredFields.length; i++) {
          var filteredField = self.filteredFields[i];
          if (filteredField) {
            if (self.matches(filterValue, record, filteredField, record.data[filteredField])) {
              return true;
            }
          }
        }
        return false;
      }, self);
    }
    else {
      self.logDebug('Filtering cleared on ' + self.filteredStore);
    }
  },

  /**
   * @protected
   * Returns true if the field value is defined and matches the filtering function.
   * @param filterValue to match
   * @param record record that was used to extract the value to be matched
   * @param fieldName filter field name that was used to extract the value to be matched
   * @param fieldValue to me matched
   */
  matches: function (filterValue, record, fieldName, fieldValue) {
    var self = this;
    return self.filterFn(fieldValue, filterValue);
  },

  /**
   * Unbinds form current store and register itself to provided store.
   * @param store to register itself to
   */
  reconfigureStore: function (store, filteredFields) {
    var self = this;
    if (self.filteredStore !== store) {
      self.unbindFromStore(self.filteredStore);
      self.bindToStore(store);
    }
    self.filteredFields = filteredFields;
    self.filterStore();
  },

  /**
   * @private
   * Remove itself as listener from provided store.
   * @param store to remove itself from
   */
  unbindFromStore: function (store) {
    var self = this;
    if (store) {
      self.logDebug('Unbinding from store ' + self.filteredStore);
      store.removeListener('load', self.filterStore, self);
    }
  },

  /**
   * @private
   * Register itself as listener of load events on provided store.
   * @param store to register itself to
   */
  bindToStore: function (store) {
    var self = this;
    self.filteredStore = store;
    if (store) {
      self.logDebug('Binding to store ' + self.filteredStore);
      store.on('load', self.filterStore, self);
    }
  }

});