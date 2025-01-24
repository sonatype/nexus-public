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
 * Settings FieldSet.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.formfield.SettingsFieldSet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-formfield-settingsfieldset',

  requires: [
    'NX.coreui.view.formfield.factory.FormfieldCheckboxFactory',
    'NX.coreui.view.formfield.factory.FormfieldComboFactory',
    'NX.coreui.view.formfield.factory.FormfieldDateFieldFactory',
    'NX.coreui.view.formfield.factory.FormfieldItemselectFactory',
    'NX.coreui.view.formfield.factory.FormfieldNumberFieldFactory',
    'NX.coreui.view.formfield.factory.FormfieldTextAreaFactory',
    'NX.coreui.view.formfield.factory.FormfieldTextFieldFactory',
    'NX.coreui.view.formfield.factory.FormfieldUrlFactory',
    'NX.coreui.view.formfield.factory.FormfieldStaticInfoFactory',
    'NX.coreui.view.formfield.factory.FormfieldTaskScopeFactory',
    'NX.coreui.view.formfield.factory.FormfieldPanelMessageFactory'
  ],

  mixins: {
    logAware: 'NX.LogAware'
  },

  plugins: {
    responsive:true
  },
  responsiveConfig: {
    'width <= 1366': {
      width: 600
    },
    'width <= 1600': {
      width: 800
    },
    'width > 1600' : {
      width: 1000
    }
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      autoHeight: false,
      autoScroll: true,
      collapsed: false,
      items: []
    });

    me.callParent();
  },

  /**
   * @property
   */
  formFields: undefined,

  /**
   * Renders form fields.
   * @param {Array} formFields form fields to rendered
   */
  setFormFields: function (formFields) {
    var me = this,
        item;

    me.formFields = formFields;

    me.removeAll();

    if (me.formFields) {
      Ext.Array.each(me.formFields, function (formField) {
        var factory = Ext.ClassManager.getByAlias('nx.formfield.factory.' + formField.type);
        if (!factory) {
          me.logWarn('Missing factory for form-field type:', formField.type);
          factory = Ext.ClassManager.getByAlias('nx.formfield.factory.string');
        }
        if (factory) {
          var config = {
            requiresPermission: true,
            name: 'property_' + formField.id,
            factory: factory,
            delimiter: me.delimiter,
            listeners: {
              afterrender: {
                fn: function() {
                  // fixes an issue with hidden validation errors when the error is added before the field is rendered
                  this.validate();
                }
              }
            }
          };
          if (Ext.isDefined(me.delimiter)) {
            config.delimiter = me.delimiter;
          }
          item = Ext.apply(factory.create(formField, me.disableSort), config);
          me.configureListeners(formField, item);
          me.add(item);
        }
      });
    }
    me.up('form').isValid();
  },

  /**
   * Exports properties.
   * @returns {Object} properties object
   */
  exportProperties: function (values) {
    var me = this,
        properties = {},
        value;

    if (me.formFields) {
      Ext.Array.each(me.formFields, function (formField) {
        value = values['property_' + formField.id];
        if (Ext.isDefined(value) && value !== null) {
          if (Ext.isArray(value)) {
            properties[formField.id] = value;
          } else {
            properties[formField.id] = String(value);
          }
          delete values['property_' + formField.id];
        }
        else {
          properties[formField.id] = null;  
        }
      });
    }

    return properties;
  },

  /**
   * Imports properties.
   * @param {Object} properties to import
   * @param {Array} formFields to import
   */
  importProperties: function (properties, formFields, editableCondition) {
    var me = this,
        form = me.up('form').getForm(),
        data = {};

    // recreate settings only when we have different form fields  (compare json encoded objects)
    if (Ext.encode(me.formFields) !== Ext.encode(formFields)) {
      me.setFormFields(formFields);

      if (Ext.isDefined(editableCondition) && !editableCondition.isSatisfied()) {
        me.up('form').setEditable(false);
      }
    }

    // avoid resetting initial values of fields when creating a new record (properties will be null)
    if (properties) {
      if (me.formFields) {
        Ext.Array.each(me.formFields, function(formField) {
          data['property_' + formField.id] = '';
        });
      }

      Ext.Object.each(properties, function(key, value) {
        data['property_' + key] = value;
      });

      form.setValues(data);
    }
    me.fireEvent("propertiesimported", me, properties);
  },

  /**
   * Mark fields in this form invalid in bulk.
   * @param {Object/Object[]/Ext.data.Errors} errors
   * Either an array in the form `[{id:'fieldId', msg:'The message'}, ...]`,
   * an object hash of `{id: msg, id2: msg2}`, or a {@link Ext.data.Errors} object.
   */
  markInvalid: function (errors) {
    var form = this.up('form').getForm(),
        remainingMessages = [],
        key, marked, field;

    if (Ext.isDefined(errors)) {
      for (key in errors) {
        if (errors.hasOwnProperty(key)) {
          marked = false;
          if (form) {
            field = form.findField('property_' + key);
            if (!field) {
              field = form.findField(key);
            }
            if (field) {
              marked = true;
              field.markInvalid(errors[key]);
            }
          }
          if (!marked) {
            remainingMessages.push(errors[key]);
          }
        }
      }
    }

    if (remainingMessages.length > 0) {
      NX.Messages.warning(remainingMessages.join('\n'));
    }
  },
  /**
   * Associates field listeners with their corresponding handlers defined in attributes.listeners configuration.
   * @param formField Field configuration.
   * @param item Instance of the field.
   */
  configureListeners: function(formField, item) {
    const me = this;

    if(formField.attributes["listeners"]) {
      const listeners = formField.attributes["listeners"];

      Ext.Array.each(Object.keys(listeners),function (key) {
        const listener = me[listeners[key]];

        if(listener) {
          item.on(key, me[listeners[key]]);
        }
      })
    }
  },
  /**
   * Filters the repositoryName field to display only repositories associated with the blob store selected in the blobStoreName field.
   * @param selector Instance of the blobStoreName ItemSelector
   * @param newValue The blob stores selected.
   */
  filterRepositoryBySelectedBlobstore: function(selector, newValue) {
    const repositoryNameItems = selector.up('nx-coreui-formfield-settingsfieldset').query("nx-itemselector[name=property_repositoryName]")
    const selectedBlobStores = Ext.Array.filter(newValue.split(','), function(value) { return value !== "" });
    const filter = function (record) {
      return selectedBlobStores.length === 0
          || Ext.Array.contains(selectedBlobStores, record.get('blobStoreName'));
    };

    if(repositoryNameItems) {
      Ext.Array.each(repositoryNameItems, function(repositoryNameItem) {
        const filterField = repositoryNameItem.fromField.down('textfield');
        var store = repositoryNameItem.store;
        const newSelectedRepositories = repositoryNameItem.value && repositoryNameItem.value.length > 0
            ? Ext.Array.filter(repositoryNameItem.getRecordsForValue(repositoryNameItem.value), filter)
            : [];

        if(store.remoteFilter) {
          store = Ext.create('Ext.data.ChainedStore', { source: store });
          repositoryNameItem.store = store;
        }

        filterField.setValue('');
        store.clearFilter();
        store.filterBy(filter);

        repositoryNameItem.populateFromStore(store);
        repositoryNameItem.setValue(Ext.Array.map(newSelectedRepositories, function(record) { return record.get("name"); }));
      })
    }
  }

});
