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
 * Helpers to interact with Icon controller.
 *
 * @since 3.0
 */
Ext.define('NX.Icons', {
  singleton: true,
  requires: [
    'NX.util.Url'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Helper to get the CSS class for a named icon with optional variant.
   *
   * @public
   */
  cls: function (name, variant) {
    var cls = 'nx-icon-' + name;
    if (variant) {
      cls += '-' + variant;
    }
    return cls;
  },

  /**
   * Helper to get html text for a named icon with variant.
   *
   * @public
   */
  img: function(name, variant) {
    return Ext.DomHelper.markup({
      tag: 'img',
      src: Ext.BLANK_IMAGE_URL,
      cls: this.cls(name, variant)
    });
  },

  /**
   * Helper to URL for an icon name + variant + optional extension.
   *
   * @param name
   * @param [variant]
   * @param [ext] The file extension to use, png if not set.
   * @returns {string}
   */
  url: function(name, variant, ext) {
    var file = name;

    if (ext === undefined) {
      ext = 'png';
    }
    file += '.' + ext;

    return this.url2(file, variant);
  },

  /**
   * Helper to get a URL for an icon file + optional variant.
   *
   * @param file
   * @param [variant]
   * @returns {string}
   */
  url2: function(file, variant) {
    var url = NX.util.Url.baseUrl + '/static/rapture/resources/icons/';
    if (variant) {
      url += variant + '/';
    }
    url += file;
    return url;
  }

});
