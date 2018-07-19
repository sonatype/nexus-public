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
 * An field that allows ordering records in a store.
 *
 * @since 3.0
 */
Ext.define('NX.ext.form.field.ItemOrderer', {
  extend: 'Ext.ux.form.MultiSelect',
  alias: 'widget.nx-itemorderer',
  requires: [
    'Ext.button.Button',
    'Ext.ux.form.MultiSelect'
  ],

  /**
   * @cfg {Boolean} [hideNavIcons=false] True to hide the navigation icons
   */
  hideNavIcons: false,

  /**
   * @cfg {Array} buttons Defines the set of buttons that should be displayed on the right of MultiSelect field.
   * Defaults to <tt>['top', 'up', 'down', 'bottom']</tt>. These names are used to look up the button text labels in
   * {@link #buttonsText} and the glyph in {@link #buttonsGlyph}.
   * This can be overridden with a custom Array to change which buttons are displayed or their order.
   */
  buttons: ['top', 'up', 'down', 'bottom'],

  /**
   * @cfg {Object} buttonsText The tooltips for the {@link #buttons}.
   * Labels for buttons.
   */
  buttonsText: {
    top: 'Move to Top',
    up: 'Move Up',
    down: 'Move Down',
    bottom: 'Move to Bottom'
  },

  /**
   * @cfg {Object} buttonsGlyph The glyphs for the {@link #buttons}.
   * Glyphs for buttons.
   */
  buttonsGlyph: {
    top: 'xf102@FontAwesome' /* fa-angle-double-up */,
    up: 'xf106@FontAwesome' /* fa-angle-up */,
    down: 'xf107@FontAwesome' /* fa-angle-down */,
    bottom: 'xf103@FontAwesome' /* fa-angle-double-down */
  },

  layout: {
    type: 'hbox',
    align: 'stretch'
  },

  initComponent: function () {
    var me = this;

    me.ddGroup = me.id + '-dd';
    me.callParent();

    // bindStore must be called after the orderField has been created because
    // it copies records from our configured Store into the orderField's Store
    me.bindStore(me.store);
  },

  setupItems: function () {
    var me = this;

    me.orderField = Ext.create('Ext.ux.form.MultiSelect', {
      // We don't want the multiselects themselves to act like fields,
      // so override these methods to prevent them from including
      // any of their values
      submitValue: false,
      getSubmitData: function () {
        return null;
      },
      getModelData: function () {
        return null;
      },
      flex: 1,
      dragGroup: me.ddGroup,
      dropGroup: me.ddGroup,
      title: me.title,
      store: {
        model: me.store.model,
        data: []
      },
      displayField: me.displayField,
      valueField: me.valueField,
      disabled: me.disabled,
      listeners: {
        boundList: {
          scope: me,
          drop: me.syncValue
        }
      }
    });

    return [
      me.orderField,
      {
        xtype: 'container',
        margins: '0 4',
        layout: {
          type: 'vbox',
          pack: 'center'
        },
        items: me.createButtons()
      }
    ];
  },

  createButtons: function () {
    var me = this,
        buttons = [];

    if (!me.hideNavIcons) {
      Ext.Array.forEach(me.buttons, function (name) {
        buttons.push({
          xtype: 'button',
          tooltip: me.buttonsText[name],
          glyph: me.buttonsGlyph[name],
          handler: me['on' + Ext.String.capitalize(name) + 'BtnClick'],
          navBtn: true,
          scope: me,
          margin: '4 0 0 0'
        });
      });
    }
    return buttons;
  },

  /**
   * Get the selected records from the specified list.
   *
   * Records will be returned *in store order*, not in order of selection.
   * @param {Ext.view.BoundList} list The list to read selections from.
   * @return {Ext.data.Model[]} The selected records in store order.
   *
   */
  getSelections: function (list) {
    var store = list.getStore();

    return Ext.Array.sort(list.getSelectionModel().getSelection(), function (a, b) {
      a = store.indexOf(a);
      b = store.indexOf(b);

      if (a < b) {
        return -1;
      }
      else if (a > b) {
        return 1;
      }
      return 0;
    });
  },

  onTopBtnClick: function () {
    var me = this,
        list = me.orderField.boundList,
        store = list.getStore(),
        selected = me.getSelections(list);

    store.suspendEvents();
    store.remove(selected, true);
    store.insert(0, selected);
    store.resumeEvents();
    list.refresh();
    me.syncValue();
    list.getSelectionModel().select(selected);
  },

  onBottomBtnClick: function () {
    var me = this,
        list = me.orderField.boundList,
        store = list.getStore(),
        selected = me.getSelections(list);

    store.suspendEvents();
    store.remove(selected, true);
    store.add(selected);
    store.resumeEvents();
    list.refresh();
    me.syncValue();
    list.getSelectionModel().select(selected);
  },

  onUpBtnClick: function () {
    var me = this,
        list = me.orderField.boundList,
        store = list.getStore(),
        selected = me.getSelections(list),
        rec,
        i = 0,
        len = selected.length,
        index = 0;

    // Move each selection up by one place if possible
    store.suspendEvents();
    for (; i < len; ++i, index++) {
      rec = selected[i];
      index = Math.max(index, store.indexOf(rec) - 1);
      store.remove(rec, true);
      store.insert(index, rec);
    }
    store.resumeEvents();
    list.refresh();
    me.syncValue();
    list.getSelectionModel().select(selected);
  },

  onDownBtnClick: function () {
    var me = this,
        list = me.orderField.boundList,
        store = list.getStore(),
        selected = me.getSelections(list),
        rec,
        i = selected.length - 1,
        index = store.getCount() - 1;

    // Move each selection down by one place if possible
    store.suspendEvents();
    for (; i > -1; --i, index--) {
      rec = selected[i];
      index = Math.min(index, store.indexOf(rec) + 1);
      store.remove(rec, true);
      store.insert(index, rec);
    }
    store.resumeEvents();
    list.refresh();
    me.syncValue();
    list.getSelectionModel().select(selected);
  },

  syncValue: function () {
    var me = this;
    me.mixins.field.setValue.call(me, me.setupValue(me.orderField.store.getRange()));
  },

  setValue: function (value) {
    // do nothing as we always show all records, unselected
  },

  onBindStore: function (store) {
    var me = this;

    if (me.orderField) {
      if (store.getCount()) {
        me.populateStore(store);
      }
      else {
        me.store.on('load', me.populateStore, me);
      }
    }
  },

  populateStore: function (store) {
    var me = this,
        orderStore = me.orderField.store;

    me.storePopulated = true;

    orderStore.removeAll();
    orderStore.add(store.getRange());
    me.syncValue();

    orderStore.fireEvent('load', orderStore);
  },

  onEnable: function () {
    var me = this;

    me.callParent();
    me.orderField.enable();

    Ext.Array.forEach(me.query('[navBtn]'), function (btn) {
      btn.enable();
    });
  },

  onDisable: function () {
    var me = this;

    me.callParent();
    me.orderField.disable();

    Ext.Array.forEach(me.query('[navBtn]'), function (btn) {
      btn.disable();
    });
  },

  onDestroy: function () {
    var me = this;

    if (me.store) {
      me.store.un('load', me.populateStore, me);
    }
    me.bindStore(null);
    me.callParent();
  }

});
