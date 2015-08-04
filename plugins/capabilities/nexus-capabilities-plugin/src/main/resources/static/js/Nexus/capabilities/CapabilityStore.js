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
 * Capability data store.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilityStore', {
  extend: 'Ext.data.JsonStore',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: ['Nexus.siesta'],

  /**
   * @constructor
   */
  constructor: function (config) {
    var self = this,
        ST = Ext.data.SortTypes;

    config = config || {};

    Ext.apply(config, {
      url: Nexus.siesta.basePath + '/capabilities',
      id: 'capability.id',

      fields: [
        { name: 'id', mapping: 'capability.id' },
        { name: 'description', sortType: ST.asUCString },
        { name: 'notes', mapping: 'capability.notes', sortType: ST.asUCString },
        { name: 'enabled', mapping: 'capability.enabled' },
        { name: 'active' },
        { name: 'error' },
        { name: 'typeName' },
        { name: 'typeId', mapping: 'capability.typeId'},
        { name: 'stateDescription' },
        { name: 'status' },
        { name: 'properties', mapping: 'capability.properties' },
        { name: '$capability', mapping: 'capability' },
        { name: '$tags',
          convert: function (newValue, record) {
            return self.convertTags(record.tags);
          }
        }
      ],

      sortInfo: {
        field: 'typeName',
        direction: 'ASC'
      },

      listeners: {
        load: {
          fn: function () {
            self.tagKeys = self.calculateTagKeys();
            this.logDebug('Loaded ' + self.getCount() + ' capabilities');
          },
          scope: self
        }
      }
    });

    self.constructor.superclass.constructor.call(self, config);
  },

  /**
   * Returns the url of a capability given its ID.
   */
  urlOf: function (id) {
    return this.url + '/' + id;
  },

  /**
   * Calculates an array of all available tag keys.
   * @private
   */
  calculateTagKeys: function () {
    var self = this,
        allTagKeys = [];

    self.each(function (record) {
      if (record.data.$tags) {
        for (var key in record.data.$tags) {
          if (allTagKeys.indexOf(key) < 0) {
            allTagKeys.push(key);
          }
        }
      }
    });

    return allTagKeys.sort();
  },

  /**
   * Converts tags collection to an map like object.
   * @private
   */
  convertTags: function (recordTags) {
    var tags = {};

    if (recordTags) {
      Ext.each(recordTags, function (tag) {
        tags[tag.key] = tag.value;
      });
      return tags;
    }
  },

  sameTagKeysAs: function (another) {
    return JSON.stringify(this.tagKeys) === JSON.stringify(another);
  }

});