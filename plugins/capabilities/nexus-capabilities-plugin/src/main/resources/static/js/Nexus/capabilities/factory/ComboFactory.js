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
/*global NX, Ext, Sonatype*/

/**
 * 'combo' factory.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.factory.ComboFactory', {

  singleton: true,

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  supports: ['combo', 'combobox', 'repo', 'repo-or-group', 'repo-target'],

  /**
   * Map of stores / store url.
   * @private
   */
  stores: {},

  /**
   * Creates a combo.
   * @param formField capability type form field to create combo for
   * @returns {*} created combo (never null)
   */
  create: function (formField) {
    var self = this,
        ST = Ext.data.SortTypes,
        store,
        item = NX.create('Ext.form.ComboBox', {
          xtype: 'combo',
          fieldLabel: formField.label,
          itemCls: formField.required ? 'required-field' : '',
          helpText: formField.helpText,
          name: formField.id,
          displayField: 'name',
          valueField: 'id',
          editable: false,
          forceSelection: true,
          mode: 'local',
          triggerAction: 'all',
          emptyText: 'Select...',
          selectOnFocus: true,
          allowBlank: formField.required ? false : true,
          anchor: '96%'
        });

    if (formField.initialValue) {
      item.value = formField.initialValue;
    }
    if (formField.storePath) {
      store = self.stores[formField.storePath];
      if (!store) {
        store = NX.create('Ext.data.JsonStore', {
          url: Sonatype.config.contextPath + formField.storePath,
          id: formField.idMapping || 'id',
          root: formField.storeRoot,

          fields: [
            { name: 'id', mapping: formField.idMapping || 'id' },
            { name: 'name', mapping: formField.nameMapping || 'name', sortType: ST.asUCString }
          ],

          sortInfo: {
            field: 'name',
            direction: 'ASC'
          },

          autoLoad: true
        });
        self.stores[formField.storePath] = store;
        self.logDebug("Caching store for " + store.url);
      }
      item.store = store;
    }
    return item;
  },

  /**
   * Evicts all cached stores (they will be recreated on demand).
   */
  evictCache: function () {
    var self = this;

    self.logDebug('Evicted all cached stores');
    self.stores = {};
  }

});