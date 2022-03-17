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
/*global NX, Ext, Nexus*/

/**
 * A search box.
 *
 * @since 2.7
 */
NX.define('Nexus.ext.SearchBox', {
  extend: 'Ext.Container',

  requires: [ 'Nexus.util.Icons' ],

  xtype: 'nx-search-box',

  /**
   * @cfg {Number} delay between keystrokes and actual filtering (defaults to 200).
   */
  searchDelay: 200,

  /**
   * @override
   */
  initComponent: function () {
    var self = this,
        icons = Nexus.util.Icons;

    self.searchField = NX.create('Ext.form.TextField', {
      enableKeyEvents: true,
      cls: 'nx-searchbox',
      style: {
        paddingLeft: '22px',
        paddingRight: '22px'
      },
      listeners: {
        keyup: {
          fn: function (field, e) {
            clearTimeout(self.searchTimeout);
            if (e.keyCode === 27) {
              self.clearSearch();
            }
            else {
              self.searchTimeout = function () {
                if (self.searchField.getValue() && self.searchField.getValue().length > 0) {
                  self.fireEvent('search', self.searchField.getValue());
                }
                else {
                  self.fireEvent('searchcleared');
                }
              }.defer(self.searchDelay, self);
            }
          },
          scope: self
        }
      }
    });

    self.clearButton = NX.create('Ext.Button', {
      xtype: 'button',
      iconCls: icons.get('glyph_circle_remove').cls,
      scope: self,
      handleMouseEvents: false,
      hidden: true,
      handler: self.clearSearch,
      style: {
        position: 'absolute',
        top: '3px',
        right: '3px',
        opacity: 0.5
      }
    });

    Ext.apply(self, {
      layout: 'fit',
      items: [
        self.searchField,
        self.clearButton,
        {
          xtype: 'button',
          // FIXME: Pick one of these icons
          //iconCls: icons.get('glyph_filter').cls,
          iconCls: icons.get('glyph_search').cls,
          disabled: true,
          scope: self,
          style: {
            position: 'absolute',
            top: '3px',
            right: (self.width - 20) + 'px',
            opacity: 0.25
          }
        }
      ]
    });

    self.addEvents(

        /**
         * Fires when a search value is available.
         * @event search
         * @param {String} search value
         */
        'search',

        /**
         * Fires when search has been cleared (no value available).
         * @event searchcleared
         */
        'searchcleared'
    );

    Nexus.ext.SearchBox.superclass.initComponent.call(self, arguments);

    self.on('search', function (value) {
      if (value && value.length > 0) {
        self.clearButton.show();
      }
    });

    self.on('searchcleared', function () {
      self.clearButton.hide();
    });
  },

  /**
   * Clears the search.
   */
  clearSearch: function () {
    var self = this;

    self.searchField.setValue(undefined);
    self.fireEvent('searchcleared');
  },

  /**
   * Sets teh value of search box and performs the search.
   */
  search: function (value) {
    var self = this;

    self.searchField.setValue(value);
    self.fireEvent('search');
  },

  /**
   * Returns the current search value or undefined if search is undefined or empty.
   */
  getSearchValue: function () {
    var self = this;
    if (self.searchField.getValue() && self.searchField.getValue().length > 0) {
      return self.searchField.getValue();
    }
    return undefined;
  }

});