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
/*global Ext*/

/**
 * Extension of Ext.ux.form.ItemSelector to allow better control over button configurations.
 *
 * @since 3.0
 */
Ext.define('NX.ext.form.field.ItemSelector', {
  extend: 'Ext.ux.form.ItemSelector',
  alias: 'widget.nx-itemselector',
  requires: [
    'Ext.ux.form.MultiSelect',
    'NX.I18n'
  ],

  plugins: {
    responsive: true
  },
  responsiveConfig: {
    'width <= 1366': {
      maxWidth: 600
    },
    'width <= 1600': {
      maxWidth: 800
    },
    'width > 1600' : {
      maxWidth: 1000
    }
  },
  height: 300,
  width: '100%',

  disabledCls: 'nx-itemselector-disabled',
  invalidCls: 'nx-invalid',

  maskOnDisable: false,
  selectionPlaceholder: null,

  /**
   * Override super *private* impl so we can control the button configuration.
   *
   * @override
   * @private
   */
  createButtons: function () {
    var me = this,
        buttons = me.callSuper();

    if (!me.hideNavIcons) {
      var i = 0;
      Ext.Array.forEach(me.buttons, function (name) {
        me.customizeButton(name, buttons[i++]);
      });
    }

    return buttons;
  },

  /**
   * Replace iconCls with glyph.
   *
   * @private
   *
   * @param name
   * @param button
   */
  customizeButton: function (name, button) {
    var icons = {
      top: 'x-fa fa-angle-double-up',
      up: 'x-fa fa-angle-up',
      add: 'x-fa fa-angle-right',
      remove: 'x-fa fa-angle-left',
      addAll: 'x-fa fa-angle-double-right',
      removeAll: 'x-fa fa-angle-double-left',
      down: 'x-fa fa-angle-down',
      bottom: 'x-fa fa-angle-double-down'
    };

    button.iconCls = icons[name];
  },

  createList: function (title) {
    var me = this,
        store = Ext.getStore(me.store),
        tbar, listener;

    // only create filter box for from field
    if (!me.fromField) {
      tbar = {
        xtype: 'nx-searchbox',
        cls: ['nx-searchbox', 'nx-filterbox'],
        iconClass: 'fa-filter',
        emptyText: NX.I18n.get('Form_Field_ItemSelector_Empty'),
        searchDelay: 200,
        listeners: {
          search: me.onSearch,
          searchcleared: me.onSearchCleared,
          scope: me
        }
      };
    }

    listener = store.onAfter('load', function() {
      if (me.fromField && me.fromField.boundList && me.fromField.boundList.getMaskTarget()) {
        me.fromField.boundList.mask();
        if (!me.fromField.boundList.disabled) {
          me.fromField.boundList.unmask();
        }
      }
    }, me, { destroyable: true });

    me.on('destroy', listener.destroy, listener);

    return Ext.create('Ext.ux.form.MultiSelect', {
      // We don't want the multiselects themselves to act like fields,
      // so override these methods to prevent them from including
      // any of their values
      submitValue: false,
      isDirty: Ext.emptyFn,
      getSubmitData: function () {
        return null;
      },
      getModelData: function () {
        return null;
      },
      cls: 'nx-multiselect',
      flex: 1,
      dragGroup: me.ddGroup,
      dropGroup: me.ddGroup,
      title: title,
      store: {
        model: store.model,
        sorters: store.getSorters().items,
        data: []
      },
      displayField: me.displayField,
      valueField: me.valueField,
      disabled: me.disabled,
      listeners: {
        boundList: {
          scope: me,
          itemdblclick: me.onItemDblClick,
          drop: me.syncValue
        }
      },
      tbar: tbar
    });
  },

  onAddAllBtnClick:function() {
    var me = this, items = me.fromField.getStore().getData().items;
    while (items.length > 0) {
      me.moveRec(true, items[0])
    }
  },

  onRemoveAllBtnClick:function() {
    var me = this, items = me.toField.getStore().getData().items;
    while (items.length > 0) {
      me.moveRec(false, items[0])
    }
  },

  /**
   * Ext.ux.form.ItemSelector defers setting value if store is not loaded,
   * which messes up the logic in Ext.form.Basic.setValues()
   * when Ext.form.Basic.trackResetOnLoad is true.
   *
   * @override
   */
  setValue: function(value) {
    if (this.store) {
      if (this.valueAsString) {
        if (Array.isArray(value)) {
          this.callParent(arguments);
        }
        else {
          this.callParent(value ? [value.split(',')] : undefined);
        }
      }
      else {
        this.callParent(arguments);
      }
    }

    // HACK: force original value to reset, to prevent always dirty forms when store has not loaded when form initially sets values.
    this.resetOriginalValue();
  },

  getValue: function() {
    const me = this,
        valueField = me.valueField,
        parentValue = this.callParent();
    var result = parentValue;

    if(Array.isArray(parentValue)) {
      result = Ext.Array.filter(parentValue, function(item) {
        if (me.selectionPlaceholder) {
          return me.selectionPlaceholder[valueField] !== item;
        }
        return true
      });
    }

    if (this.valueAsString) {
      return result.toString();
    }
    else {
      return result;
    }
  },

  // HACK: avoid exceptions when the store is reloaded
  populateFromStore: function (store) {
    var me = this,
        fromStore = me.fromField.store;

    if (fromStore) {
      fromStore.removeAll();
    }
    me.callParent(arguments);
  },

  /**
   * @private
   */
  onSearch: function (searchbox, value) {
    var me = this;

    me.fromField.store.filter({ id: 'filter', filterFn: function (model) {
      var stringValue = model.get(me.displayField);
      if (stringValue) {
        stringValue = stringValue.toString();
        return stringValue.toLowerCase().indexOf(value.toLowerCase()) !== -1;
      }
      return false;
    }});
  },

  /**
   * @private
   */
  onSearchCleared: function () {
    this.fromField.store.clearFilter();
  },

  // HACK: Looks like original item selector forgot to unbind from store which results in NPEs in #populateFromStore
  onDestroy: function () {
    var me = this;

    if (me.store) {
      me.store.un('load', me.populateFromStore, me);
    }
    this.callParent();
  },

  getRecordsForValue: function () {
    var me = this;
    if (!me.store) {
      return [];
    }
    return this.callParent(arguments);
  },

  onEnable: function() {
    this.callParent(arguments);
    Ext.each(this.query('boundlist'), function(list) {
      list.unmask();
    });
  },

  onDisable: function() {
    this.callParent(arguments);
    Ext.each(this.query('boundlist'), function(list) {
      list.mask();
    });
  },
  getSelections: function(list) {
    const me = this,
        valueField = me.valueField,
        selected = this.callParent(arguments);

    if(list === me.toField.boundList && me.selectionPlaceholder) {
      return Ext.Array.filter(selected, function (item) {
        return item.data[valueField] !== me.selectionPlaceholder[valueField];
      });
    } else {
      return selected;
    }
  }
});
