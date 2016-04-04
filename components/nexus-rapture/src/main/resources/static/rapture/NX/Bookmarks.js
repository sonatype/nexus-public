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
 * Helpers to interact with **{@link NX.controller.Bookmarking}** controller.
 *
 * @since 3.0
 */
Ext.define('NX.Bookmarks', {
  singleton: true,
  requires: [
    'NX.Bookmark'
  ],

  /**
   * @private
   * @returns {NX.controller.Bookmarking}
   */
  controller: function () {
    return NX.getApplication().getBookmarkingController();
  },

  /**
   * @see NX.controller.Bookmarking#getBookmark
   */
  getBookmark: function () {
    return this.controller().getBookmark();
  },

  /**
   * @see NX.controller.Bookmarking#bookmark
   */
  bookmark: function (bookmark, caller) {
    return this.controller().bookmark(bookmark, caller);
  },

  /**
   * @see NX.controller.Bookmarking#navigateTo
   */
  navigateTo: function (bookmark, caller) {
    return this.controller().navigateTo(bookmark, caller);
  },

  /**
   * Creates a new bookmark.
   *
   * @public
   * @param {String} token bookmark token
   * @returns {NX.Bookmark} created bookmark
   */
  fromToken: function (token) {
    return Ext.create('NX.Bookmark', { token: token });
  },

  /**
   * Creates a new bookmark from provided segments.
   *
   * @public
   * @param {String[]} segments bookmark segments
   * @returns {NX.Bookmark} created bookmark
   */
  fromSegments: function (segments) {
    var token;
    if (Ext.isDefined(segments)) {
      token = Ext.Array.from(segments).join(':');
    }
    return Ext.create('NX.Bookmark', { token: token });
  },

  /**
   * Encodes the value suitable to be used as a bookmark token.
   * (eliminate spaces and lower case)
   *
   * @param value to be encoded
   * @returns {String} encoded value
   */
  encode: function (value) {
    if (!Ext.isString(value)) {
      throw Ext.Error.raise('Value to be encoded must be a String');
    }
    return value.replace(/\s/g, '');
  }

});