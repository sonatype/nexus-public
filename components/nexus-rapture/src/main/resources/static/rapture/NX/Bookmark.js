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
 * A bookmark.
 *
 * @since 3.0
 */
Ext.define('NX.Bookmark', {

  config: {
    /**
     * Bookmark token.
     *
     * @cfg {String}
     */
    token: undefined
  },

  /**
   * @private
   */
  segments: undefined,

  /**
   * @constructor
   */
  constructor: function (config) {
    this.initConfig(config);
  },

  /**
   * Validates token to be a String and calculates segments.
   *
   * @private
   * @param token to apply
   * @returns {String} token
   */
  applyToken: function (token) {
    var me = this;
    if (token && !Ext.isString(token)) {
      throw Ext.Error.raise('Invalid token');
    }
    if (token && (token.trim().length === 0)) {
      token = undefined;
    }
    // avoid nulls
    if (!token) {
      token = undefined;
    }
    me.segments = [];
    if (token) {
      me.segments = token.split(':');
    }
    return token;
  },

  /**
   * @public
   * @param {Number} index of segment
   * @returns {String} segment at index if defined
   */
  getSegment: function (index) {
    return this.segments[index];
  },

  /**
   * @public
   * @returns {Array} list of all segments in this bookmarks
   */
  getSegments: function() {
    return this.segments;
  },

  /**
   * Appends a segment to current segment.
   *
   * @param {String/String[]} segments to append
   * @returns {NX.Bookmark} itself
   */
  appendSegments: function (segments) {
    var me = this;

    if (!segments) {
      throw Ext.Error.raise('Invalid segment: ' + segment);
    }
    if (!Ext.isArray(segments)) {
      segments = [segments];
    }
    Ext.each(segments, function (segment) {
      if (!segment || !Ext.isString(segment)) {
        throw Ext.Error.raise('Invalid segment: ' + segment);
      }
      me.segments.push(segment);
    });

    me.setToken(me.segments.join(':'));

    return me;
  }

});
