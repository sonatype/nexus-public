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
 * A {@link Ext.grid.column.Column} which renders its value as a link.
 *
 * @since 3.0
 */
Ext.define('NX.ext.grid.column.CopyLink', {
  extend: 'Ext.grid.column.Column',
  alias: ['widget.nx-copylinkcolumn'],
  requires: [
    'NX.util.Url'
  ],

  stateId: 'copylink',

  constructor: function () {
    var me = this;

    me.listeners = {
      click: function() {
        // Prevent drilldown from triggering
        return false;
      }
    };

    me.callParent(arguments);
  },

  /**
   * Renders value as a link.
   */
  defaultRenderer: function (value, _, record) {
    if (value) {
      value = value.replace(/\$baseUrl/, NX.util.Url.baseUrl);
      return NX.util.Url.asCopyWidget(value, record.data.format);
    }
    return undefined;
  },

  /**
   * @protected
   */
  target: function (value) {
    return value;
  }

});
