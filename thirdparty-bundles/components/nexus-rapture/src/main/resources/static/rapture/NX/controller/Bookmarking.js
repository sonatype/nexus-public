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

/**
 * Bookmarking controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Bookmarking', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.History',
    'NX.Bookmark',
    'NX.Bookmarks'
  ],

  /**
   * If this controller had been launched. Becomes true after onLaunch() method is called by ExtJS.
   */
  launched: false,

  /**
   * @override
   */
  init: function () {
    var me = this;

    // The only requirement for this to work is that you must have a hidden field and
    // an iframe available in the page with ids corresponding to Ext.History.fieldId
    // and Ext.History.iframeId.  See history.html for an example.
    Ext.History.useTopWindow = false;
    Ext.History.init();

    me.bindToHistory();

    me.addEvents(
        /**
         * Fires when user navigates to a new bookmark.
         *
         * @event navigate
         * @param {String} bookmark value
         */
        'navigate'
    );
  },

  /**
   * @public
   * @returns {NX.Bookmark} current bookmark
   */
  getBookmark: function () {
    return NX.Bookmarks.fromToken(Ext.History.bookmark || Ext.History.getToken());
  },

  /**
   * Sets bookmark to a specified value.
   *
   * @public
   * @param {NX.Bookmark} bookmark new bookmark
   * @param {Object} [caller] whom is asking to bookmark
   */
  bookmark: function (bookmark, caller) {
    var me = this,
        oldValue = me.getBookmark().getToken();

    if (!me.launched) {
      return;
    }

    if (bookmark && oldValue !== bookmark.getToken()) {
      //<if debug>
      me.logDebug('Bookmark:', bookmark.getToken(), (caller ? '(' + caller.self.getName() + ')' : ''));
      //</if>

      Ext.History.bookmark = bookmark.getToken();
      Ext.History.add(bookmark.getToken());
    }
  },

  /**
   * Sets bookmark to a specified value and navigates to it.
   *
   * @public
   * @param {NX.Bookmark} bookmark to navigate to
   * @param {Object} [caller] whom is asking to navigate
   */
  navigateTo: function (bookmark, caller) {
    var me = this;

    if (!me.launched) {
      return;
    }

    if (bookmark) {
      //<if debug>
      me.logDebug('Navigate to:', bookmark.getToken(), (caller ? '(' + caller.self.getName() + ')' : ''));
      //</if>

      me.bookmark(bookmark, caller);
      me.fireEvent('navigate', bookmark);
    }
  },

  /**
   * Navigate to current bookmark.
   *
   * @override
   */
  onLaunch: function () {
    var me = this;

    me.launched = true;

    me.navigateTo(me.getBookmark(), me);
  },

  /**
   * Sets bookmark to a specified value and navigates to it.
   *
   * @private
   * @param {String} token to navigate to
   */
  onNavigate: function (token) {
    var me = this;

    if (token !== Ext.History.bookmark) {
      delete Ext.History.bookmark;
      me.navigateTo(NX.Bookmarks.fromToken(token), me);
    }
  },

  /**
   * Start listening to **{@link Ext.History}** change events.
   *
   * @private
   */
  bindToHistory: function () {
    var me = this;

    Ext.History.on('change', me.onNavigate, me);
  }

});
