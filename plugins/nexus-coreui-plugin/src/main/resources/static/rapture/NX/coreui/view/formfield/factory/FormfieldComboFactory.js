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

/*global NX, Ext, Sonatype*/

/**
 * 'combo' factory.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.formfield.factory.FormfieldComboFactory', {
  singleton: true,
  alias: [
    'nx.formfield.factory.combo',
    'nx.formfield.factory.combobox',
    'nx.formfield.factory.repo',
    'nx.formfield.factory.repo-or-group',
    'nx.formfield.factory.repo-target'
  ],
  requires: [
    'Ext.data.Store',
    'Ext.form.ComboBox',
    'NX.util.Url'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Creates a combo.
   * @param formField form field to create combo for
   * @returns {*} created combo (never null)
   */
  create: function (formField) {
    var ST = Ext.data.SortTypes,
        item, filters,
        itemConfig = {
          xtype: 'combo',
          fieldLabel: formField.label,
          itemCls: formField.required ? 'required-field' : '',
          helpText: formField.helpText,
          name: formField.id,
          displayField: 'name',
          valueField: 'id',
          editable: false,
          forceSelection: true,
          queryMode: 'local',
          triggerAction: 'all',
          emptyText: 'Select...',
          selectOnFocus: false,
          allowBlank: !formField.required
        };

    if (formField.initialValue) {
      itemConfig.value = formField.initialValue;
    }
    if (formField['storeApi']) {
      if (formField['storeFilters']) {
        filters = [];
        Ext.Object.each(formField['storeFilters'], function (key, value) {
          filters.push({ property: key, value: value });
        });
      }
      itemConfig.store = Ext.create('Ext.data.Store', {
        proxy: {
          type: 'direct',
          api: {
            read: 'NX.direct.' + formField['storeApi']
          },
          reader: {
            type: 'json',
            root: 'data',
            idProperty: formField['idMapping'] || 'id',
            successProperty: 'success'
          }
        },

        fields: [
          { name: 'id', mapping: formField['idMapping'] || 'id' },
          { name: 'name', mapping: formField['nameMapping'] || 'name', sortType: ST.asUCString }
        ],

        filters: filters,
        sortOnLoad: true,
        sorters: { property: 'name', direction: 'ASC' },
        remoteFilter: true,
        autoLoad: true
      });
    }
    item = Ext.create('Ext.form.ComboBox', itemConfig);
    Ext.override(item, {
      /**
       * Avoid value being set to null by combobox in case that store was not already loaded.
       */
      setValue: function (value) {
        var me = this;
        me.callParent(arguments);
        if (me.getValue() === null) {
          me.value = value;
        }
      }
    });
    return item;
  }

});