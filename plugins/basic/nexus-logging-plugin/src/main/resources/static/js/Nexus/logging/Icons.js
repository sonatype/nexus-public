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
 * Container for icons used by logging.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.Icons', {
  extend: 'Nexus.util.IconContainer',
  singleton: true,

  /**
   * @constructor
   */
  constructor: function () {
    var me = this;

    // helper to build an icon config with variants, where variants live in directories, foo.png x16 -> x16/foo.png
    function iconConfig(fileName, variants) {
      var config = {};
      if (variants === undefined) {
        variants = ['x32', 'x16'];
      }
      Ext.each(variants, function (variant) {
        config[variant] = variant + '/' + fileName;
      });
      return config;
    }

    me.constructor.superclass.constructor.call(me, {
      stylePrefix: 'nx-logging-icon-',

      icons: {
        arrow_refresh: 'arrow_refresh.png',
        download: iconConfig('download.png'),
        book: iconConfig('book.png'),
        book_add: iconConfig('book_add.png'),
        book_delete: iconConfig('book_delete.png'),
        bookmark_red: iconConfig('bookmark_red.png'),
        arrow_undo_orange: iconConfig('arrow_undo_orange.png'),

        logging: '@book',
        logger: '@book',

        loggers_refresh: '@arrow_refresh',
        loggers_add: '@book_add',
        loggers_remove: '@book_delete',
        loggers_reset: '@arrow_undo_orange',

        log_refresh: '@arrow_refresh',
        log_download: '@download',
        log_mark: '@bookmark_red'
      }
    });
  }

});