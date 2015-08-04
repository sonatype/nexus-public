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
 * Capability grouping data store.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilitiesGridStore', {
  extend: 'Ext.data.GroupingStore',

  mixins: [
    'Nexus.LogAwareMixin',
    'Nexus.capabilities.Icons',
    'Nexus.capabilities.CapabilityStore'
  ],

  /**
   * @constructor
   */
  constructor: function (config) {
    var self = this,
        config = config || {},
        ST = Ext.data.SortTypes,
        fields = [
          { name: 'id' },
          { name: 'description', sortType: ST.asUCString },
          { name: 'notes', sortType: ST.asUCString },
          { name: 'enabled' },
          { name: 'active' },
          { name: 'error' },
          { name: 'typeName' },
          { name: 'typeId'},
          { name: 'stateDescription' },
          { name: 'status' },
          { name: 'properties' },
          { name: '$capability' }
        ];

    self.tagKeys = config.capabilityStore.tagKeys;

    if (self.tagKeys) {
      Ext.each(self.tagKeys, function (key) {
        fields.push({ name: 'tag$' + key, sortType: ST.asUCString });
      });
    }

    Ext.apply(config, {
      reader: NX.create('Ext.data.JsonReader', {
        fields: fields
      }),

      autoDestroy: true,

      sortInfo: {
        field: 'typeName',
        direction: 'ASC'
      }
    });

    self.constructor.superclass.constructor.call(self, config);

    self.loadCapabilities();
  },

  /**
   * Loads all capabilities from configured capability store into this store.
   */
  loadCapabilities: function () {
    var self = this,
        recordType = this.recordType,
        newRecords = [];

    self.capabilityStore.each(function (record) {
      var newRecord,
          overrideConfig = {};

      if (record.data.$tags) {
        for (var key in record.data.$tags) {
          overrideConfig['tag$' + key] = record.data.$tags[key];
        }
      }
      newRecord = new recordType(Ext.apply(Ext.apply({}, record.data), overrideConfig), record.id);
      newRecords.push(newRecord);
    });

    self.loadRecords({records: newRecords}, {});
  },

  /**
   * Returns the column model based on discovered tags.
   */
  getColumnModel: function () {
    var self = this,
        icons = Nexus.capabilities.Icons,
        columns = [];

    columns.push(
        {
          width: 30,
          resizable: false,
          sortable: false,
          fixed: true,
          hideable: false,
          menuDisabled: true,
          renderer: function (value, metaData, record) {
            return icons.iconFor(record.data).img;
          }
        },
        {
          id: 'typeName',
          width: 175,
          header: 'Type',
          dataIndex: 'typeName',
          sortable: true
        }
    );

    if (self.tagKeys) {
      Ext.each(self.tagKeys, function (key) {
        columns.push(
            {
              id: 'tag$' + key,
              width: 100,
              header: key,
              dataIndex: 'tag$' + key,
              sortable: true
            }
        );
      });
    }

    columns.push(
        {
          id: 'description',
          width: 250,
          header: 'Description',
          dataIndex: 'description',
          sortable: true
        },
        {
          id: 'notes',
          width: 175,
          header: 'Notes',
          dataIndex: 'notes',
          sortable: true
        }
    );

    return NX.create('Ext.grid.ColumnModel', {columns: columns});
  },

  /**
   * Extract tag key from name. Returns null if name is not a tag name.
   */
  getTagKeyFrom: function (name) {
    if (name.startsWith('tag$')) {
      return name.substring('tag$'.length);
    }
  }

});