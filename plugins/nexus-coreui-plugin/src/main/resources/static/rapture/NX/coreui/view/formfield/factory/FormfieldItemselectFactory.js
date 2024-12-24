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
 * 'itemselect' factory.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.view.formfield.factory.FormfieldItemselectFactory', {
  singleton: true,
  alias: [
    'nx.formfield.factory.itemselect'
  ],
  requires: [
    'Ext.data.Store',
    'Ext.data.SortTypes',
    'NX.ext.form.field.ItemSelector'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Create control.
   */
  create: function (formField, disableSort) {
    const me = this;
    var filters,
        attributes = formField['attributes'] || {},
        idMapping = formField['idMapping'] || 'id',
        nameMapping = formField['nameMapping'] || 'name',
        itemConfig = {
          xtype: 'nx-itemselector',
          fieldLabel: formField.label,
          helpText: formField.helpText,
          name: formField.id,
          valueField: idMapping,
          displayField: nameMapping,
          width:600,

          itemCls: formField.required ? 'required-field' : '',
          allowBlank: !formField.required,
          delimiter: ',',

          // initialValue is null, but expected to be undefined for not set
          value: formField.initialValue ? formField.initialValue : undefined,

          listeners: {
            afterrender: function() {
              var value = this.getValue();
              if (formField.required && value === undefined || (Ext.isArray(value) && value.length === 0)) {
                // HACK: default blank behavior is not properly rendering required field error, forcing error to display
                this.markInvalid(this.blankText);
              }
            }
          }
        };

    if (attributes['buttons']) {
      itemConfig.buttons = attributes['buttons'];
    }
    if (attributes['fromTitle']) {
      itemConfig.fromTitle = attributes['fromTitle'];
    }
    if (attributes['toTitle']) {
      itemConfig.toTitle = attributes['toTitle'];
    }
    if (attributes['valueAsString']) {
      itemConfig.valueAsString = attributes['valueAsString'];
    }
    if (attributes['selectionPlaceholderText']) {
      const placeholder = {};

      placeholder[idMapping] = attributes['selectionPlaceholderText'];
      placeholder[nameMapping] = attributes['selectionPlaceholderText'];
      itemConfig.selectionPlaceholder = placeholder;
      itemConfig.listeners = {
        afterrender: function (itemSelector) {
          const settings = itemSelector.up('nx-coreui-formfield-settingsfieldset');

          itemSelector.on('change', me.selectionPlaceholderUpdater);
          if(settings) {
            settings.on('propertiesimported', function() { me.selectionPlaceholderUpdater(itemSelector) });
          }
          me.selectionPlaceholderUpdater(itemSelector);
        }
      };
    }

    if (formField['storeApi']) {
      if (formField['storeFilters']) {
        filters = [];
        Ext.Object.each(formField['storeFilters'], function (key, value) {
          filters.push({ property: key, value: value });
        });
      }

      var args = {
        proxy: {
          type: 'direct',
          api: {
            read: 'NX.direct.' + formField['storeApi']
          },
          reader: {
            type: 'json',
            rootProperty: 'data',
            idProperty: idMapping,
            successProperty: 'success'
          }
        },

        fields: [
          { name: 'id', mapping: idMapping },
          { name: 'name', mapping: nameMapping, sortType: Ext.data.SortTypes.asUCString }
        ],

        filters: filters,
        sortOnLoad: true,
        sorters: { property: nameMapping, direction: 'ASC' },
        remoteFilter: true,
        autoLoad: true
      };

      if (disableSort) {
        delete args.sortOnLoad;
        delete args.sorters;
      }
      itemConfig.store = Ext.create('Ext.data.Store', args);
    }

    return Ext.create('NX.ext.form.field.ItemSelector', itemConfig);
  },

  selectionPlaceholderUpdater: function (itemSelector) {
    const placeholderRecord = itemSelector.selectionPlaceholder;

    if(placeholderRecord) {
      const toField = itemSelector.toField, store = toField.getStore(), valueField = itemSelector.valueField;
      const selectedValues = Ext.Array.filter(itemSelector.getValue().split(','), function(selection) {
        return selection !== "" && selection !== placeholderRecord[valueField];
      });
      if (selectedValues.length === 0) {
        if (!store.findRecord(valueField, placeholderRecord[valueField])) {
          store.add(placeholderRecord);
          toField.setStore(store);
        }
      }
      else {
        const placeholder = store.findRecord(valueField, placeholderRecord[valueField]);
        if (placeholder) {
          store.remove(placeholder);
          toField.setStore(store);
        }
      }
    }
  }

});
