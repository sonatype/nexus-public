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
 * A search box.
 *
 * @since 3.0
 */
Ext.define('NX.ext.SearchBox', {
  extend: 'Ext.form.field.Text',
  alias: 'widget.nx-searchbox',
  requires: [
    'Ext.util.KeyNav'
  ],

  emptyText: 'search',
  submitValue: false,

  /**
   * Number of milliseconds to trigger searching.
   *
   * @cfg {Number}
   */
  searchDelay: 1000,

  triggers: {
    clear: {
      cls: 'nx-form-fa-times-circle-trigger',
      handler: function() {
        this.clearSearch();
      }
    }
  },

  listeners: {
    change: 'onValueChange',
    keypress: 'updateTriggerVisibility'
  },

  maskOnDisable: false,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      checkChangeBuffer: me.searchDelay,
      ariaLabel: me.emptyText
    });

    me.callParent(arguments);
  },

  /**
   * @override
   */
  initEvents: function () {
    var me = this;

    me.callParent();

    me.keyNav = new Ext.util.KeyNav({
      target: me.inputEl,
      esc: {
        handler: me.clearSearch,
        scope: me,
        defaultEventAction: false
      },
      enter: {
        handler: me.onEnter,
        scope: me,
        defaultEventAction: false
      },
      scope: me,
      forceKeyDown: true
    });
  },

  /**
   * Search on ENTER.
   *
   * @private
   */
  onEnter: function () {
    var me = this;

    //me.lastValue is used to check for changes in the delayed checkchanges task, so we fake it out here
    //otherwise, the onValueChange will get triggered regardless when timeout occurs
    //(causing undesired page transition if page is changed prior to this delayed check)
    me.lastValue = me.getValue();
    me.search(me.lastValue);
    me.resetOriginalValue();
  },

  /**
   * Trigger search.
   *
   * @private
   */
  onValueChange: function (trigger, value) {
    var me = this;

    if (value) {
      me.search(value);
    }
    else {
      me.clearSearch();
    }
    me.resetOriginalValue();
  },

  /**
   * Search for value and fires a 'search' event.
   *
   * @public
   * @param value to search for
   */
  search: function (value) {
    var me = this;

    if (value !== me.getValue()) {
      me.setValue(value);
    }
    else {
      if (me.fireEvent('beforesearch', me)) {
        me.fireEvent('search', me, value);
      }
    }
  },

  /**
   * Clears the search and fires a 'searchcleared' event.
   *
   * @public
   */
  clearSearch: function () {
    var me = this;

    if (me.getValue()) {
      me.setValue(undefined);
    }
    me.fireEvent('searchcleared', me);
  }

});
