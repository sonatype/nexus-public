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
 * User search box.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.user.UserSearchBox', {
  extend: 'Ext.form.field.Text',
  alias: 'widget.nx-coreui-user-searchbox',
  requires: [
    'Ext.util.KeyNav',
    'NX.I18n'
  ],

  triggers: {
    clear: {
      cls: 'nx-form-fa-times-circle-trigger',
      handler: 'clearSearch',
      hidden: true
    },
    search: {
      cls: 'x-form-search-trigger',
      handler: 'doSearch'
    }
  },

  keyMap: {
    ESC: 'clearSearch',
    ENTER: 'doSearch'
  },

  listeners: {
    change: 'valueChanged'
  },

  width: 320,
  submitValue: false,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.emptyText = NX.I18n.get('User_UserList_Filter_EmptyText');

    me.callParent();
  },

  /**
   * @private
   */
  doSearch: function() {
    var value = this.getValue();
    this.search(value);
  },

  /**
   * Search for value and fires a 'search' event.
   *
   * @public
   * @param value to search for
   */
  search: function(value) {
    var me = this;

    if (value !== me.getValue()) {
      me.setValue(value);
    }
    me.fireEvent('search', me, value);
  },

  /**
   * Clears the search.
   *
   * @public
   */
  clearSearch: function() {
    var me = this;

    if (me.getValue()) {
      me.setValue(undefined);
    }
    me.fireEvent('searchcleared', me);
  },

  valueChanged: function() {
    var clearTrigger = this.getTrigger('clear');
    if (this.getValue()) {
      clearTrigger.show();
    }
    else {
      clearTrigger.hide();
    }
  }

});
