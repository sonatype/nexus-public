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
 * Capability type data store.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilityTypeStore', {
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
      url: Nexus.siesta.basePath + '/capabilities/types',
      id: 'id',

      fields: [
        { name: 'id' },
        { name: 'name' },
        { name: 'about' },
        { name: 'formFields' }
      ],

      sortInfo: {
        field: 'id',
        direction: 'ASC'
      },

      listeners: {
        load: {
          fn: function () {
            this.logDebug('Loaded ' + self.getCount() + ' types');
          },
          scope: self
        }
      }
    });

    self.constructor.superclass.constructor.call(self, config);
  },

  /**
   * Returns capability type given its ID.
   */
  getTypeById: function (id) {
    var record = this.getById(id);

    if (record) {
      return record.data;
    }
  }

});